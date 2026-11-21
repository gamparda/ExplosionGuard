package com.sunglogbag81.explosionguard;

import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import io.papermc.paper.event.entity.EntityBreakEvent;

/**
 * Blocks already survive explosions; this does the same for the things players place that are entities:
 * paintings, item frames (and the item in them), cushions, leash knots, armor stands, boats, minecarts and dropped items.
 * Mobs and players still take explosion damage.
 */
final class EntityShield implements Listener {
    static boolean explosion(EntityDamageEvent.DamageCause cause) {
        return cause == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION || cause == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION;
    }

    /** Things a player built or dropped, as opposed to creatures. Armor stands count as living but are decoration. */
    static boolean shielded(Entity entity) {
        return entity instanceof ArmorStand || !(entity instanceof LivingEntity);
    }

    private static boolean overworld(Entity entity) {
        return entity.getWorld().getEnvironment() == World.Environment.NORMAL;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHangingBreak(HangingBreakEvent event) {
        if (event.getCause() == HangingBreakEvent.RemoveCause.EXPLOSION && overworld(event.getEntity())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (explosion(event.getCause()) && shielded(event.getEntity()) && overworld(event.getEntity())) event.setCancelled(true);
    }

    /** Cushions and other attached decorations that are not hangings report explosions here instead. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityBreak(EntityBreakEvent event) {
        if (event.getCause() == EntityBreakEvent.RemoveCause.EXPLOSION && overworld(event.getEntity())) event.setCancelled(true);
    }
}
