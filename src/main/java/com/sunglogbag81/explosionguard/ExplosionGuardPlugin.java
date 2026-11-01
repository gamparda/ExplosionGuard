package com.sunglogbag81.explosionguard;

import io.papermc.paper.event.packet.PlayerChunkLoadEvent;
import io.papermc.paper.math.Position;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.*;
import java.util.function.BooleanSupplier;

/** World blocks stay intact; temporary barriers exist only in outgoing client updates. */
public final class ExplosionGuardPlugin extends JavaPlugin implements Listener {
    private final MaskQueue queue=new MaskQueue();
    private final Set<MaskQueue.Chunk> refreshes=new LinkedHashSet<>();
    private Settings settings;
    private ProtectionHook protection;
    private BlockData barrier;
    private long lastWarning;

    @Override public void onEnable() {
        try{settings=Settings.load(getDataFolder().toPath().resolve("config.yml"));}
        catch(Exception error){settings=Settings.defaults();getLogger().warning("설정 파일을 읽거나 저장할 수 없어 기본값으로 실행합니다: "+error);}
        protection=new ProtectionHook(this);barrier=Material.BARRIER.createBlockData();
        getServer().getPluginManager().registerEvents(this,this);
        getServer().getScheduler().runTaskTimer(this,()->{
            var restored=queue.expire(now(),Math.min(settings.restoreBudget(),settings.animationBudget()));
            restoreStep(restored);
            refreshes.addAll(OverlayView.chunks(restored));
            refreshPending(settings.refreshBudget(),true);
        },1,1);
        getLogger().info("ExplosionGuard 1.1.1: exposed faces use real blocks; restoration uses Paper anti-xray chunk refresh.");
    }
    @Override public void onDisable(){refreshes.addAll(OverlayView.chunks(queue.drain()));refreshPending(Integer.MAX_VALUE,false);}

    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void onEntityExplode(EntityExplodeEvent event) {
        if(event.getLocation().getWorld().getEnvironment()==World.Environment.NORMAL)capture(event.blockList(),event.getLocation(),event::isCancelled);
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void onBlockExplode(BlockExplodeEvent event) {
        if(event.getBlock().getWorld().getEnvironment()==World.Environment.NORMAL)capture(event.blockList(),event.getBlock().getLocation().add(.5,.5,.5),event::isCancelled);
    }
    private void capture(List<Block> affected,Location origin,BooleanSupplier cancelled) {
        var visible=List.copyOf(affected.subList(0,Math.min(affected.size(),settings.explosionLimit())));
        // Block destruction is disabled even if an optional visual/protection API fails below.
        affected.clear();
        var candidates=new ArrayList<MaskQueue.Candidate>();var particles=new ArrayList<MaskQueue.Candidate>();
        for(Block block:visible) {
            if(protection.protectedBlock(block)||block.getType().isAir())continue;
            var candidate=new MaskQueue.Candidate(key(block),block.getBlockData().getAsString(),block.getLocation().add(.5,.5,.5).distanceSquared(origin));
            if(particles.size()<settings.particleLimit())particles.add(candidate);
            if(AnimationPolicy.eligible(block))candidates.add(candidate);
        }
        // Keeping real blocks also keeps inventories, text, PDC, support geometry and drops safe.
        if(candidates.isEmpty()&&particles.isEmpty())return;
        getServer().getScheduler().runTask(this,()->{
            if(cancelled.getAsBoolean())return;
            long now=now();
            var valid=candidates.stream().filter(candidate->unchanged(candidate.key(),candidate.data()))
                    .filter(candidate->!protection.protectedBlock(block(candidate.key()))).toList();
            sendMasks(queue.add(valid,now,settings.delayMillis(),settings.lifetimeMillis(),settings.pendingLimit()),null);
            for(var particle:particles) {
                if(!unchanged(particle.key(),particle.data()))continue;
                Block block=block(particle.key());if(block==null||protection.protectedBlock(block))continue;
                block.getWorld().spawnParticle(Particle.BLOCK,block.getLocation().add(.5,.5,.5),4,.3,.3,.3,0,block.getBlockData());
            }
        });
    }
    private static MaskQueue.Key key(Block block){return new MaskQueue.Key(block.getWorld().getUID(),block.getX(),block.getY(),block.getZ());}
    private static long now(){return System.nanoTime()/1_000_000L;}
    private Block block(MaskQueue.Key key) {
        World world=Bukkit.getWorld(key.world());
        if(world==null||!world.isChunkLoaded(key.x()>>4,key.z()>>4))return null;
        return world.getBlockAt(key.x(),key.y(),key.z());
    }
    private boolean unchanged(MaskQueue.Key key,String data){Block block=block(key);return block!=null&&block.getBlockData().getAsString().equals(data);}
    private boolean active(MaskQueue.Mask mask){return mask!=null&&unchanged(mask.key(),mask.data())&&!protection.protectedBlock(block(mask.key()));}
    private void sendMasks(Collection<MaskQueue.Mask> masks,Player only){send(masks,only);}
    private void sendCurrent(Collection<MaskQueue.Mask> masks){refreshes.addAll(OverlayView.chunks(masks));}
    private void send(Collection<MaskQueue.Mask> masks,Player only) {
        if(masks.isEmpty())return;
        var exposed=OverlayView.exposed(masks,this::active);
        Collection<? extends Player> players=only==null?Bukkit.getOnlinePlayers():List.of(only);
        for(Player player:players) {
            if(!player.isOnline())continue;
            var updates=new HashMap<Position,BlockData>();var tiles=new ArrayList<TileState>();
            for(var key:exposed) {
                if(!player.getWorld().getUID().equals(key.world())||!player.isChunkSent(key.chunkKey()))continue;
                Block block=block(key);if(block==null)continue;
                // A neighbor can belong to another active explosion: never replace its barrier with a real wall.
                boolean hidden=active(queue.get(key));
                updates.put(Position.block(key.x(),key.y(),key.z()),hidden?barrier:block.getBlockData());
                if(!hidden&&block.getState(false) instanceof TileState tile)tiles.add(tile);
            }
            if(updates.isEmpty())continue;
            try {
                player.sendMultiBlockChange(updates);
                for(var tile:tiles)player.sendBlockUpdate(tile.getLocation(),tile);
            }catch(RuntimeException error){warn("표시 갱신 실패: "+error.getMessage());}
        }
    }
    private void restoreStep(Collection<MaskQueue.Mask> masks) {
        if(masks.isEmpty())return;
        // Only the next restoring blocks are sent as real states. The rest remain barriers.
        for(Player player:Bukkit.getOnlinePlayers()) {
            var updates=new HashMap<Position,BlockData>();var tiles=new ArrayList<TileState>();
            for(var mask:masks) {
                var key=mask.key();
                if(!player.getWorld().getUID().equals(key.world())||!player.isChunkSent(key.chunkKey()))continue;
                Block block=block(key);if(block==null)continue;
                updates.put(Position.block(key.x(),key.y(),key.z()),block.getBlockData());
                if(block.getState(false) instanceof TileState tile)tiles.add(tile);
            }
            if(updates.isEmpty())continue;
            try{player.sendMultiBlockChange(updates);for(var tile:tiles)player.sendBlockUpdate(tile.getLocation(),tile);}
            catch(RuntimeException error){warn("단계별 복구 표시 실패: "+error.getMessage());}
        }
    }
    @SuppressWarnings("deprecation") // Paper refreshChunk deliberately reapplies its per-player Anti-Xray controller.
    private void refreshPending(int budget,boolean reapply) {
        var iterator=refreshes.iterator();
        for(int count=0;count<budget&&iterator.hasNext();) {
            var chunk=iterator.next();
            // Do not erase another active animation, including faces reaching across a chunk border.
            if(reapply&&!OverlayView.touching(queue,chunk.world(),chunk.key(),Long.MIN_VALUE).isEmpty())continue;
            iterator.remove();count++;
            World world=Bukkit.getWorld(chunk.world());int x=(int)chunk.key(),z=(int)(chunk.key()>>32);
            if(world==null||!world.isChunkLoaded(x,z))continue;
            try {
                if(!world.refreshChunk(x,z)){refreshes.add(chunk);return;}
                // refreshChunk does not fire PlayerChunkLoadEvent. Queue overlays after the chunk packet.
                if(reapply)getServer().getScheduler().runTask(this,()->sendMasks(OverlayView.touching(queue,chunk.world(),chunk.key(),now()),null));
            }catch(RuntimeException error){refreshes.add(chunk);warn("Paper 청크 갱신 실패: "+error.getMessage());return;}
        }
    }
    private void warn(String message) {
        long now=System.currentTimeMillis();if(now-lastWarning<5000)return;
        lastWarning=now;getLogger().warning(message);
    }
    @EventHandler(priority=EventPriority.MONITOR)
    public void onChunkSent(PlayerChunkLoadEvent event) {
        if(queue.size()==0)return;
        var player=event.getPlayer();var world=event.getWorld().getUID();long chunk=event.getChunk().getChunkKey();
        getServer().getScheduler().runTask(this,()->sendMasks(OverlayView.touching(queue,world,chunk,now()),player));
    }
    @EventHandler(priority=EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        if(queue.size()==0)return;
        var player=event.getPlayer();
        getServer().getScheduler().runTaskLater(this,()->{
            if(!player.isOnline())return;
            for(long chunk:player.getSentChunkKeys())sendMasks(OverlayView.touching(queue,player.getWorld().getUID(),chunk,now()),player);
        },5);
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void onBreak(BlockBreakEvent event) {
        var mask=queue.get(key(event.getBlock()));
        if(mask!=null&&unchanged(mask.key(),mask.data())) {
            event.setCancelled(true);event.getPlayer().sendActionBar(Component.text("복구 연출 중입니다. 잠시 후 다시 시도하세요."));
        }
    }
    @Override public boolean onCommand(CommandSender sender,Command command,String label,String[] args) {
        if(!sender.hasPermission("explosionguard.admin")){sender.sendMessage("권한이 없습니다.");return true;}
        String sub=args.length==0?"status":args[0].toLowerCase(Locale.ROOT);
        switch(sub) {
            case "status" -> sender.sendMessage("ExplosionGuard: 화면 복구 대기 "+queue.size()+"개 / 청크 갱신 대기 "+refreshes.size()+"개 / 지연 "+settings.delayMillis()/1000+"초 / 월드 원본 유지");
            case "restore" -> {int count=queue.size();sendCurrent(queue.drain());sender.sendMessage("화면 표시 "+count+"개의 Paper 청크 원복을 요청했습니다.");}
            case "reload" -> {
                try {
                    Settings next=Settings.load(getDataFolder().toPath().resolve("config.yml"));
                    sendCurrent(queue.drain());settings=next;protection=new ProtectionHook(this);
                    sender.sendMessage("ExplosionGuard 설정을 다시 읽었습니다.");
                }catch(Exception error){sender.sendMessage("설정을 읽을 수 없습니다: "+error.getMessage());}
            }
            default -> sender.sendMessage("/"+label+" [status|restore|reload]");
        }
        return true;
    }
    @Override public List<String> onTabComplete(CommandSender sender,Command command,String alias,String[] args) {
        if(!sender.hasPermission("explosionguard.admin")||args.length!=1)return List.of();
        return List.of("status","restore","reload").stream().filter(value->value.startsWith(args[0].toLowerCase(Locale.ROOT))).toList();
    }
}
