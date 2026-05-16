package com.sunglogbag81.explosionguard;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Prevents explosions from breaking blocks without cancelling the explosion itself.
 *
 * <p>Do not cancel the explosion events: cancelling can suppress normal explosion
 * behaviour. Clearing the affected block list only removes block damage, while
 * entity/player damage and knockback are still handled by the server.</p>
 */
public final class ExplosionGuardPlugin extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("ExplosionGuard enabled: block damage is disabled, explosion damage remains enabled.");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onEntityExplode(EntityExplodeEvent event) {
        event.blockList().clear();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBlockExplode(BlockExplodeEvent event) {
        event.blockList().clear();
    }
}
