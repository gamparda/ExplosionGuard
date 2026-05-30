package com.sunglogbag81.explosionguard;

import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Prevents overworld explosions from breaking blocks without cancelling the explosion itself.
 *
 * <p>Do not cancel the explosion events: cancelling can suppress normal explosion
 * behaviour. Clearing the affected block list only removes block damage, while
 * entity/player damage and knockback are still handled by the server. Nether and
 * End explosions are left untouched.</p>
 */
public final class ExplosionGuardPlugin extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("ExplosionGuard enabled: overworld explosion block damage is disabled; nether/end remain enabled.");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onEntityExplode(EntityExplodeEvent event) {
        if (isOverworld(event.getLocation().getWorld())) event.blockList().clear();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBlockExplode(BlockExplodeEvent event) {
        if (isOverworld(event.getBlock().getWorld())) event.blockList().clear();
    }

    private boolean isOverworld(World world) {
        return world != null && world.getEnvironment() == World.Environment.NORMAL;
    }
}
