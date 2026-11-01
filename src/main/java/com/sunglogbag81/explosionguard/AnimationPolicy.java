package com.sunglogbag81.explosionguard;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.TileState;
import org.bukkit.block.data.type.Slab;
import org.bukkit.util.BoundingBox;
import java.util.Collection;
import java.util.EnumSet;

final class AnimationPolicy {
    private static final EnumSet<Material> SPECIAL_PHYSICS=EnumSet.of(Material.SLIME_BLOCK,Material.HONEY_BLOCK,
            Material.ICE,Material.PACKED_ICE,Material.BLUE_ICE,Material.FROSTED_ICE,
            Material.SOUL_SAND,Material.SOUL_SOIL,Material.HAY_BLOCK,Material.MAGMA_BLOCK);
    private AnimationPolicy(){}
    static boolean eligible(Block block) {
        if(block.isEmpty()||block.getType()==Material.BARRIER||block.getType()==Material.TNT)return false;
        // Matching collision shape alone does not preserve bouncing, friction or ground effects.
        if(SPECIAL_PHYSICS.contains(block.getType()))return false;
        if(block.getState(false) instanceof TileState||block.getBlockData() instanceof Slab)return false;
        return fullCube(block.getCollisionShape().getBoundingBoxes());
    }
    static boolean fullCube(Collection<BoundingBox> boxes) {
        if(boxes.size()!=1)return false;
        var box=boxes.iterator().next();
        return equal(box.getMinX(),0)&&equal(box.getMinY(),0)&&equal(box.getMinZ(),0)
                &&equal(box.getMaxX(),1)&&equal(box.getMaxY(),1)&&equal(box.getMaxZ(),1);
    }
    private static boolean equal(double one,double two){return Math.abs(one-two)<1e-7;}
}
