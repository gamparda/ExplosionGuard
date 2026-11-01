package com.sunglogbag81.explosionguard;
import org.bukkit.Material;
import org.bukkit.block.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Slab;
import org.bukkit.util.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class AnimationPolicyTest {
    Block cube(Material material){
        Block block=mock(Block.class);when(block.getType()).thenReturn(material);
        when(block.getState(false)).thenReturn(mock(BlockState.class));when(block.getBlockData()).thenReturn(mock(BlockData.class));
        var shape=mock(VoxelShape.class);when(shape.getBoundingBoxes()).thenReturn(List.of(new BoundingBox(0,0,0,1,1,1)));when(block.getCollisionShape()).thenReturn(shape);return block;
    }
    @Test void fullStoneCanBeMasked(){assertTrue(AnimationPolicy.eligible(cube(Material.STONE)));}
    @Test void inventoriesSignsAndOtherTileStatesStayOriginal(){
        for(Material material:List.of(Material.CHEST,Material.BARREL,Material.FURNACE,Material.SHULKER_BOX,Material.OAK_SIGN)) {
            var block=cube(material);when(block.getState(false)).thenReturn(mock(TileState.class));assertFalse(AnimationPolicy.eligible(block));
        }
    }
    @Test void slabsIncludingDoubleSlabsStayOriginal(){var block=cube(Material.STONE_SLAB);when(block.getBlockData()).thenReturn(mock(Slab.class));assertFalse(AnimationPolicy.eligible(block));}
    @Test void partialAndEmptyCollisionShapesAreNeverExpandedToFullCubes(){
        assertFalse(AnimationPolicy.fullCube(List.of(new BoundingBox(0,0,0,1,.5,1))));assertFalse(AnimationPolicy.fullCube(List.of()));
        assertFalse(AnimationPolicy.fullCube(List.of(new BoundingBox(0,0,0,.5,1,1),new BoundingBox(.5,0,0,1,.5,1))));
    }
    @Test void actualBarriersAndTntAreNotMasked(){assertFalse(AnimationPolicy.eligible(cube(Material.BARRIER)));assertFalse(AnimationPolicy.eligible(cube(Material.TNT)));}
    @Test void specialPhysicsBlocksKeepTheirNativeClientBehavior(){
        for(var material:List.of(Material.SLIME_BLOCK,Material.ICE,Material.PACKED_ICE,Material.BLUE_ICE,Material.SOUL_SOIL,Material.HAY_BLOCK,Material.MAGMA_BLOCK))assertFalse(AnimationPolicy.eligible(cube(material)));
    }
}
