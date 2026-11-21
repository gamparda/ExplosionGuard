package com.sunglogbag81.explosionguard;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.GlowItemFrame;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Painting;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.junit.jupiter.api.Test;
import io.papermc.paper.event.entity.EntityBreakEvent;
import org.bukkit.entity.Cushion;

class EntityShieldTest {
    private final EntityShield shield = new EntityShield();

    private static <T extends Entity> T in(Class<T> type, World.Environment env) {
        T entity = mock(type);
        World world = mock(World.class);
        when(world.getEnvironment()).thenReturn(env);
        when(entity.getWorld()).thenReturn(world);
        return entity;
    }

    private static EntityDamageEvent damage(Entity entity, DamageCause cause) {
        EntityDamageEvent event = mock(EntityDamageEvent.class);
        when(event.getEntity()).thenReturn(entity);
        when(event.getCause()).thenReturn(cause);
        return event;
    }

    @Test
    void decorationsAndDropsAreShieldedCreaturesAreNot() {
        assertTrue(EntityShield.shielded(mock(Painting.class)));
        assertTrue(EntityShield.shielded(mock(ItemFrame.class)));
        assertTrue(EntityShield.shielded(mock(GlowItemFrame.class)));
        assertTrue(EntityShield.shielded(mock(ArmorStand.class)));
        assertTrue(EntityShield.shielded(mock(Minecart.class)));
        assertTrue(EntityShield.shielded(mock(Item.class)));
        assertFalse(EntityShield.shielded(mock(Zombie.class)));
        assertFalse(EntityShield.shielded(mock(Player.class)));
    }

    @Test
    void explosionDamageToAnItemFrameIsCancelled() {
        EntityDamageEvent event = damage(in(ItemFrame.class, World.Environment.NORMAL), DamageCause.ENTITY_EXPLOSION);
        shield.onDamage(event);
        verify(event).setCancelled(true);
        EntityDamageEvent tnt = damage(in(ArmorStand.class, World.Environment.NORMAL), DamageCause.BLOCK_EXPLOSION);
        shield.onDamage(tnt);
        verify(tnt).setCancelled(true);
    }

    @Test
    void playersStillBreakThingsAndMobsStillGetHurt() {
        EntityDamageEvent hit = damage(in(ItemFrame.class, World.Environment.NORMAL), DamageCause.ENTITY_ATTACK);
        shield.onDamage(hit);
        verify(hit, never()).setCancelled(true);
        EntityDamageEvent zombie = damage(in(Zombie.class, World.Environment.NORMAL), DamageCause.ENTITY_EXPLOSION);
        shield.onDamage(zombie);
        verify(zombie, never()).setCancelled(true);
    }

    @Test
    void onlyTheOverworldLikeTheBlockProtection() {
        EntityDamageEvent nether = damage(in(ItemFrame.class, World.Environment.NETHER), DamageCause.ENTITY_EXPLOSION);
        shield.onDamage(nether);
        verify(nether, never()).setCancelled(true);
    }

    @Test
    void paintingsSurviveExplosionsButNotOtherRemoval() {
        Painting painting = in(Painting.class, World.Environment.NORMAL);
        HangingBreakEvent boom = mock(HangingBreakEvent.class);
        when(boom.getEntity()).thenReturn(painting);
        when(boom.getCause()).thenReturn(HangingBreakEvent.RemoveCause.EXPLOSION);
        shield.onHangingBreak(boom);
        verify(boom).setCancelled(true);
        HangingBreakEvent physics = mock(HangingBreakEvent.class);
        when(physics.getEntity()).thenReturn(painting);
        when(physics.getCause()).thenReturn(HangingBreakEvent.RemoveCause.PHYSICS);
        shield.onHangingBreak(physics);
        verify(physics, never()).setCancelled(true);
    }

    @Test
    void cushionsSurviveExplosionsButPlayersCanStillBreakThem() {
        Cushion cushion = in(Cushion.class, World.Environment.NORMAL);
        EntityBreakEvent boom = mock(EntityBreakEvent.class);
        when(boom.getEntity()).thenReturn(cushion);
        when(boom.getCause()).thenReturn(EntityBreakEvent.RemoveCause.EXPLOSION);
        shield.onEntityBreak(boom);
        verify(boom).setCancelled(true);
        EntityBreakEvent hit = mock(EntityBreakEvent.class);
        when(hit.getEntity()).thenReturn(cushion);
        when(hit.getCause()).thenReturn(EntityBreakEvent.RemoveCause.ENTITY);
        shield.onEntityBreak(hit);
        verify(hit, never()).setCancelled(true);
    }
}
