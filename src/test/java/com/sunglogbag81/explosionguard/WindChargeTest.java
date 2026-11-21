package com.sunglogbag81.explosionguard;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.ExplosionResult;
import org.junit.jupiter.api.Test;

class WindChargeTest {
    @Test
    void onlyBlockBreakingExplosionsAreMasked() {
        assertTrue(ExplosionGuardPlugin.breaksBlocks(ExplosionResult.DESTROY));
        assertTrue(ExplosionGuardPlugin.breaksBlocks(ExplosionResult.DESTROY_WITH_DECAY));
        assertFalse(ExplosionGuardPlugin.breaksBlocks(ExplosionResult.TRIGGER_BLOCK)); // wind charge
        assertFalse(ExplosionGuardPlugin.breaksBlocks(ExplosionResult.KEEP));
    }
}
