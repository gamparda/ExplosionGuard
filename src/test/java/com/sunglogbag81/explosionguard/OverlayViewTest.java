package com.sunglogbag81.explosionguard;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class OverlayViewTest {
    final UUID world=UUID.randomUUID();
    MaskQueue.Candidate at(int x,int y,int z,double depth){return new MaskQueue.Candidate(new MaskQueue.Key(world,x,y,z),"minecraft:stone",depth);}
    @Test void revealOnlyTheMaskAndSixExposedNeighborsNotDeeperOres() {
        var queue=new MaskQueue();var masks=queue.add(List.of(at(15,-32,0,0)),0,100,300,100);
        var exposed=OverlayView.exposed(masks,ignored->true);
        assertEquals(7,exposed.size());assertTrue(exposed.contains(new MaskQueue.Key(world,16,-32,0)));
        assertFalse(exposed.contains(new MaskQueue.Key(world,17,-32,0)));
        assertTrue(OverlayView.exposed(masks,ignored->false).isEmpty());
    }
    @Test void chunkBordersRefreshBothSidesAndReapplyNeighborFaces() {
        var queue=new MaskQueue();var masks=queue.add(List.of(at(15,-32,0,0)),0,100,300,100);
        assertEquals(3,OverlayView.chunks(masks).size());
        assertEquals(1,OverlayView.touching(queue,world,1,50).size());
        assertTrue(OverlayView.touching(queue,world,2,50).isEmpty());
        assertTrue(OverlayView.touching(queue,UUID.randomUUID(),1,50).isEmpty());
    }
    @Test void delayedAnimationMasksSurviveChunkResendsPastDeadlineUntilTheirTurn() {
        var queue=new MaskQueue();queue.add(List.of(at(0,0,0,0),at(1,0,0,1)),0,100,300,100);
        queue.expire(100,1);
        assertEquals(1,OverlayView.touching(queue,world,0,500).size());
    }
    @Test void recoveryMovesFromDeepestDistanceTowardBlastCenterOneBlockPerTick() {
        var queue=new MaskQueue();queue.add(List.of(at(0,0,0,0),at(1,0,0,1),at(3,0,0,9),at(2,0,0,4)),0,100,300,100);
        for(int expected:new int[]{3,2,1,0}) {
            var step=queue.expire(100,1);assertEquals(1,step.size());assertEquals(expected,step.getFirst().key().x());
        }
    }
}
