package com.sunglogbag81.explosionguard;

import java.util.*;
import java.util.function.Predicate;

/** Only the immediate exposed faces are revealed, never a complete chunk of real ores. */
final class OverlayView {
    private OverlayView(){}
    static Set<MaskQueue.Key> exposed(Collection<MaskQueue.Mask> masks,Predicate<MaskQueue.Mask> valid) {
        var result=new LinkedHashSet<MaskQueue.Key>();
        for(var mask:masks)if(valid.test(mask)) {
            result.add(mask.key());result.addAll(mask.key().neighbors());
        }
        return result;
    }
    static Set<MaskQueue.Chunk> chunks(Collection<MaskQueue.Mask> masks) {
        var result=new LinkedHashSet<MaskQueue.Chunk>();
        for(var key:exposed(masks,ignored->true))result.add(key.chunk());
        return result;
    }
    static List<MaskQueue.Mask> touching(MaskQueue queue,UUID world,long chunk,long now) {
        int x=(int)chunk,z=(int)(chunk>>32);
        var result=new ArrayList<MaskQueue.Mask>();
        for(int[] offset:new int[][]{{0,0},{1,0},{-1,0},{0,1},{0,-1}}) {
            long neighbor=((long)(x+offset[0])&0xffffffffL)|((long)(z+offset[1])<<32);
            // Expired masks still waiting for their animation turn remain visible until removed.
            for(var mask:queue.inChunk(world,neighbor,Long.MIN_VALUE)) {
                if(mask.key().chunkKey()==chunk||mask.key().neighbors().stream().anyMatch(key->key.chunkKey()==chunk))result.add(mask);
            }
        }
        return result;
    }
}
