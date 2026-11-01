package com.sunglogbag81.explosionguard;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class MaskQueueTest {
    final UUID world=UUID.randomUUID();
    MaskQueue.Candidate at(int x,int y,int z){return new MaskQueue.Candidate(new MaskQueue.Key(world,x,y,z),"minecraft:stone");}
    @Test void waitsAndRestoresBottomFirstWithinBudget(){
        var queue=new MaskQueue();queue.add(List.of(at(0,12,0),at(0,10,0),at(0,11,0)),0,100,300,10);
        assertTrue(queue.expire(99,10).isEmpty());assertEquals(10,queue.expire(100,1).getFirst().key().y());
        assertEquals(2,queue.size());assertEquals(2,queue.expire(100,10).size());assertEquals(0,queue.size());
    }
    @Test void repeatedExplosionDeduplicatesAndExtendsDeadline(){
        var queue=new MaskQueue();var candidate=at(1,1,1);queue.add(List.of(candidate),0,100,300,10);queue.add(List.of(candidate),80,100,300,10);
        assertEquals(1,queue.size());assertTrue(queue.expire(100,10).isEmpty());assertEquals(1,queue.expire(180,10).size());
    }
    @Test void continuousExplosionsCannotExtendTheSameMaskForever(){
        var queue=new MaskQueue();var candidate=at(1,1,1);
        for(int time=0;time<300;time+=20)queue.add(List.of(candidate),time,100,300,10);
        assertEquals(300,queue.get(candidate.key()).expires());assertEquals(1,queue.expire(300,10).size());
    }
    @Test void pendingLimitLeavesAdditionalWorldBlocksUnmasked(){
        var queue=new MaskQueue();assertEquals(2,queue.add(List.of(at(1,1,1),at(2,1,1),at(3,1,1)),0,100,300,2).size());
        assertNull(queue.get(at(3,1,1).key()));assertEquals(2,queue.size());
    }
    @Test void chunkIndexHandlesNegativeCoordinatesAndWorlds(){
        var queue=new MaskQueue();var candidate=at(-1,50,-17);queue.add(List.of(candidate),0,100,300,10);
        assertEquals((0xffffffffL)|(0xfffffffeL<<32),candidate.key().chunkKey());
        assertEquals(1,queue.inChunk(world,candidate.key().chunkKey(),99).size());
        assertTrue(queue.inChunk(UUID.randomUUID(),candidate.key().chunkKey(),99).isEmpty());
        assertTrue(queue.inChunk(world,candidate.key().chunkKey(),100).isEmpty());
    }
    @Test void manualRestoreAndDisableClearAllDeadlinesAndChunkIndexes(){
        var queue=new MaskQueue();var candidate=at(1,1,1);queue.add(List.of(candidate),0,100,300,10);
        assertEquals(1,queue.drain().size());assertEquals(0,queue.size());assertTrue(queue.expire(9999,10).isEmpty());
        assertTrue(queue.inChunk(world,candidate.key().chunkKey(),0).isEmpty());
    }
    @Test void changedWorldDataReplacesOldSnapshotWithoutUsingItsDeadline(){
        var queue=new MaskQueue();var candidate=at(1,1,1);queue.add(List.of(candidate),0,100,300,10);
        queue.add(List.of(new MaskQueue.Candidate(candidate.key(),"minecraft:gold_block")),90,100,300,10);
        assertTrue(queue.expire(100,10).isEmpty());assertEquals("minecraft:gold_block",queue.expire(190,1).getFirst().data());
    }
    @Test void repeatedExplosionsCompactStaleHeapEntries()throws Exception {
        var queue=new MaskQueue();for(int i=0;i<10000;i++)queue.add(List.of(at(1,1,1)),i,100,30000,10);
        var field=MaskQueue.class.getDeclaredField("due");field.setAccessible(true);
        assertTrue(((Queue<?>)field.get(queue)).size()<=102);assertEquals(1,queue.expire(10100,10).size());
    }
}
