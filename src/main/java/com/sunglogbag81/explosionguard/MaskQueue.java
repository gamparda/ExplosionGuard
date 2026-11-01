package com.sunglogbag81.explosionguard;

import java.util.*;

/** Only client overlays live here. The world is never replaced with barriers. */
final class MaskQueue {
    record Key(UUID world,int x,int y,int z) {
        Key offset(int dx,int dy,int dz){return new Key(world,x+dx,y+dy,z+dz);}
        List<Key> neighbors(){return List.of(offset(1,0,0),offset(-1,0,0),offset(0,1,0),offset(0,-1,0),offset(0,0,1),offset(0,0,-1));}
        long chunkKey(){return ((long)(x>>4)&0xffffffffL)|((long)(z>>4)<<32);}
        Chunk chunk(){return new Chunk(world,chunkKey());}
    }
    record Chunk(UUID world,long key){}
    record Candidate(Key key,String data,double depth){Candidate(Key key,String data){this(key,data,0);}}
    record Mask(Key key,String data,long created,long expires,double depth){}
    private final Map<Key,Mask> active=new HashMap<>();
    private final Map<Chunk,Set<Key>> chunks=new HashMap<>();
    private final PriorityQueue<Mask> due=new PriorityQueue<>(Comparator.comparingLong(Mask::expires)
            .thenComparing(Comparator.comparingDouble(Mask::depth).reversed()).thenComparingInt(mask->mask.key().y())
            .thenComparingInt(mask->mask.key().x()).thenComparingInt(mask->mask.key().z()));

    List<Mask> add(Collection<Candidate> candidates,long now,long delay,long lifetime,int limit) {
        var accepted=new ArrayList<Mask>();
        for(var candidate:candidates) {
            var old=active.get(candidate.key());
            if(old==null&&active.size()>=limit)continue;
            long created=old!=null&&old.data().equals(candidate.data())?old.created():now;
            long expires=Math.min(now+delay,created+lifetime);
            var mask=new Mask(candidate.key(),candidate.data(),created,expires,candidate.depth());
            active.put(mask.key(),mask);due.add(mask);accepted.add(mask);
            chunks.computeIfAbsent(mask.key().chunk(),ignored->new HashSet<>()).add(mask.key());
        }
        // Repeated explosions must not grow the stale deadline heap indefinitely.
        if(due.size()>active.size()*2+100){due.clear();due.addAll(active.values());}
        return accepted;
    }
    List<Mask> expire(long now,int budget) {
        var result=new ArrayList<Mask>();
        while(!due.isEmpty()&&due.peek().expires()<=now&&result.size()<budget) {
            var mask=due.remove();if(active.get(mask.key())!=mask)continue;
            remove(mask);result.add(mask);
        }
        return result;
    }
    private void remove(Mask mask) {
        active.remove(mask.key());var group=chunks.get(mask.key().chunk());
        if(group!=null){group.remove(mask.key());if(group.isEmpty())chunks.remove(mask.key().chunk());}
    }
    List<Mask> inChunk(UUID world,long key,long now) {
        var keys=chunks.get(new Chunk(world,key));if(keys==null)return List.of();
        return keys.stream().map(active::get).filter(Objects::nonNull).filter(mask->mask.expires()>now).toList();
    }
    Mask get(Key key){return active.get(key);}
    List<Mask> drain(){var values=List.copyOf(active.values());active.clear();chunks.clear();due.clear();return values;}
    int size(){return active.size();}
}
