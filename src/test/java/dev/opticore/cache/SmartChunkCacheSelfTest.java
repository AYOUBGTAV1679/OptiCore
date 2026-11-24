package dev.opticore.cache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/** Dependency-free regression and concurrency tests. */
public final class SmartChunkCacheSelfTest {
    static int passed, failed;
    static final class E implements CacheEntry {
        final long size; final AtomicInteger released = new AtomicInteger(); final boolean throwsOnRelease;
        E(long size) { this(size, false); }
        E(long size, boolean throwsOnRelease) { this.size = size; this.throwsOnRelease = throwsOnRelease; }
        public long sizeBytes() { return size; }
        public void release() { released.incrementAndGet(); if (throwsOnRelease) throw new IllegalStateException("boom"); }
    }
    static RenderCacheKey k(int x, int z, int section) { return new RenderCacheKey("overworld", x, z, section); }
    static void check(String name, boolean ok) {
        if (ok) { passed++; System.out.println("PASS  " + name); }
        else { failed++; System.out.println("FAIL  " + name); }
    }
    public static void main(String[] args) throws Exception {
        hitMiss(); keyDependencyIdentity(); boundaryInvalidationTargets(); lruOrder(); getRefreshesRecency(); oversizeRejected(); shrinkEvicts();
        invalidateChunk(); resourceReload(); settingsChange(); replaceAccounting();
        releaseThrows(); nullArgumentsRejected(); clearReleasesAll(); concurrency();
        System.out.println("\n" + passed + " passed, " + failed + " failed");
        if (failed != 0) throw new AssertionError("Cache tests failed");
    }
    static void hitMiss() {
        var c = new SmartChunkCache<E>(100); var e = new E(10);
        check("empty lookup is a miss", c.get(k(0,0,0)) == null);
        c.put(k(0,0,0), e);
        check("stored lookup is a hit", c.get(k(0,0,0)) == e);
        check("hit/miss counters", c.stats().hits() == 1 && c.stats().misses() == 1);
    }
    static void keyDependencyIdentity() {
        var base = new RenderCacheKey("overworld", 2, 3, 4, 11L, 21L, 31L);
        var changedNeighbor = new RenderCacheKey("overworld", 2, 3, 4, 12L, 21L, 31L);
        var changedLight = new RenderCacheKey("overworld", 2, 3, 4, 11L, 22L, 31L);
        var changedSettings = new RenderCacheKey("overworld", 2, 3, 4, 11L, 21L, 32L);
        check("mesh key changes when neighbor geometry dependencies change",
                !base.equals(changedNeighbor));
        check("mesh key changes when lighting dependencies change",
                !base.equals(changedLight));
        check("mesh key changes when render settings change",
                !base.equals(changedSettings));
        check("dependency variants still invalidate as the same chunk",
                base.inChunk("overworld", 2, 3) && changedNeighbor.inChunk("overworld", 2, 3));
    }
    static void boundaryInvalidationTargets() {
        var interior = ChunkBoundaryInvalidation.affectedByBlockEdit(17, 34);
        check("interior block edit invalidates only its own chunk",
                interior.equals(List.of(new ChunkBoundaryInvalidation.ChunkCoordinate(1, 2))));

        var corner = ChunkBoundaryInvalidation.affectedByBlockEdit(0, 15);
        check("positive chunk corner invalidates current and both face neighbors",
                corner.equals(List.of(
                        new ChunkBoundaryInvalidation.ChunkCoordinate(0, 0),
                        new ChunkBoundaryInvalidation.ChunkCoordinate(-1, 0),
                        new ChunkBoundaryInvalidation.ChunkCoordinate(0, 1))));

        var negative = ChunkBoundaryInvalidation.affectedByBlockEdit(-1, -16);
        check("negative world coordinates use floor-based chunk boundaries",
                negative.equals(List.of(
                        new ChunkBoundaryInvalidation.ChunkCoordinate(-1, -1),
                        new ChunkBoundaryInvalidation.ChunkCoordinate(0, -1),
                        new ChunkBoundaryInvalidation.ChunkCoordinate(-1, -2))));
    }
    static void lruOrder() {
        var c = new SmartChunkCache<E>(30); E a=new E(10), b=new E(10), d=new E(10), f=new E(10);
        c.put(k(0,0,0),a); c.put(k(1,0,0),b); c.put(k(2,0,0),d); c.put(k(3,0,0),f);
        check("least-recently-used entry evicted and released", c.get(k(0,0,0))==null && a.released.get()==1);
        check("newer entries remain", c.get(k(1,0,0))==b && c.get(k(2,0,0))==d && c.get(k(3,0,0))==f);
        check("memory bound respected", c.currentBytes()==30 && c.currentBytes()<=c.maxBytes());
    }
    static void getRefreshesRecency() {
        var c=new SmartChunkCache<E>(30); E a=new E(10),b=new E(10),d=new E(10),f=new E(10);
        c.put(k(0,0,0),a); c.put(k(1,0,0),b); c.put(k(2,0,0),d); c.get(k(0,0,0)); c.put(k(3,0,0),f);
        check("read refreshes LRU order", c.get(k(0,0,0))==a && c.get(k(1,0,0))==null && b.released.get()==1);
    }
    static void oversizeRejected() {
        var c=new SmartChunkCache<E>(50); E big=new E(51);
        check("oversized value rejected", !c.put(k(0,0,0),big));
        check("rejected value released once", big.released.get()==1 && c.size()==0 && c.stats().rejected()==1);
    }
    static void shrinkEvicts() {
        var c=new SmartChunkCache<E>(100);
        for(int i=0;i<10;i++) c.put(k(i,0,0),new E(10));
        c.setMaxBytes(35);
        check("shrinking limit evicts to bound", c.currentBytes()<=35 && c.size()==3);
        check("shrink preserves newest entry", c.get(k(9,0,0))!=null && c.get(k(0,0,0))==null);
    }
    static void invalidateChunk() {
        var c=new SmartChunkCache<E>(1000); E a=new E(1),b=new E(1),o=new E(1),n=new E(1);
        c.put(k(5,5,0),a); c.put(k(5,5,1),b); c.put(k(5,6,0),o);
        c.put(new RenderCacheKey("nether",5,5,0),n);
        int removed=c.invalidateChunk("overworld",5,5);
        check("chunk invalidation removes its sections", removed==2 && c.get(k(5,5,0))==null && c.get(k(5,5,1))==null);
        check("neighbour and other dimension retained", c.get(k(5,6,0))==o && c.get(new RenderCacheKey("nether",5,5,0))==n);
        check("invalidated bytes and release accounting", a.released.get()==1 && b.released.get()==1 && c.currentBytes()==2);
    }
    static void resourceReload() {
        var c=new SmartChunkCache<E>(1000); E a=new E(5); c.put(k(0,0,0),a); c.onResourcesReloaded();
        check("resource reload makes old entry stale", c.get(k(0,0,0))==null && a.released.get()==1 && c.currentBytes()==0);
        E b=new E(5); c.put(k(0,0,0),b);
        check("new entry valid after resource reload", c.get(k(0,0,0))==b);
    }
    static void settingsChange() {
        var c=new SmartChunkCache<E>(1000); E a=new E(5),b=new E(5);
        c.put(k(0,0,0),a); c.put(k(1,0,0),b); c.onRenderSettingsChanged();
        check("purge frees entries stale after settings change", c.purgeStale()==2 && a.released.get()==1 && b.released.get()==1 && c.size()==0);
    }
    static void replaceAccounting() {
        var c=new SmartChunkCache<E>(100); E a=new E(40),b=new E(10);
        c.put(k(0,0,0),a); c.put(k(0,0,0),b);
        check("replacement releases old value and updates bytes", a.released.get()==1 && b.released.get()==0 && c.currentBytes()==10 && c.size()==1);
        c.put(k(0,0,0),b);
        check("re-put same instance does not release it", b.released.get()==0 && c.currentBytes()==10);
    }
    static void releaseThrows() {
        var c=new SmartChunkCache<E>(10); E bad=new E(10,true),ok=new E(10);
        c.put(k(0,0,0),bad); c.put(k(1,0,0),ok);
        check("release exception contained and counted", c.get(k(1,0,0))==ok && c.stats().releaseFailures()==1 && c.currentBytes()==10);
    }
    static void nullArgumentsRejected() {
        var c=new SmartChunkCache<E>(10); boolean a=false,b=false,d=false;
        try { c.get(null); } catch(NullPointerException expected) { a=true; }
        try { c.put(null,new E(1)); } catch(NullPointerException expected) { b=true; }
        try { c.put(k(0,0,0),null); } catch(NullPointerException expected) { d=true; }
        check("null keys and values rejected", a&&b&&d&&c.size()==0);
    }
    static void clearReleasesAll() {
        var c=new SmartChunkCache<E>(1000); List<E> all=new ArrayList<>();
        for(int i=0;i<20;i++){ E e=new E(3); all.add(e); c.put(k(i,0,0),e); }
        c.clear(); c.clear();
        check("clear releases each value once and is idempotent", all.stream().allMatch(e->e.released.get()==1) && c.size()==0 && c.currentBytes()==0);
    }
    static void concurrency() throws Exception {
        final long max=2000; var c=new SmartChunkCache<E>(max);
        List<E> all=Collections.synchronizedList(new ArrayList<>());
        ExecutorService pool=Executors.newFixedThreadPool(8); AtomicLong violations=new AtomicLong();
        List<Future<?>> futures=new ArrayList<>();
        for(int t=0;t<8;t++){
            futures.add(pool.submit(()->{
                ThreadLocalRandom r=ThreadLocalRandom.current();
                for(int i=0;i<60000;i++){
                    int op=r.nextInt(100); var key=k(r.nextInt(40),r.nextInt(40),r.nextInt(4));
                    if(op<45){ E e=new E(1+r.nextInt(120)); all.add(e); c.put(key,e); }
                    else if(op<85)c.get(key);
                    else if(op<90)c.invalidateChunk("overworld",r.nextInt(40),r.nextInt(40));
                    else if(op<93)c.onRenderSettingsChanged();
                    else if(op<95)c.onResourcesReloaded();
                    else if(op<97)c.purgeStale();
                    else if(op<99)c.setMaxBytes(max-r.nextInt(500));
                    if(c.currentBytes()>max)violations.incrementAndGet();
                }
            }));
        }
        for(var f:futures)f.get(120,TimeUnit.SECONDS);
        pool.shutdown();
        check("concurrent workload never exceeds configured maximum",violations.get()==0);
        long live=c.currentBytes(); int size=c.size(); c.clear();
        int bad=0; for(E e:all)if(e.released.get()!=1)bad++;
        check("concurrent workload releases each value exactly once (bad="+bad+", total="+all.size()+")",bad==0);
        check("cache empty after concurrent clear",c.size()==0&&c.currentBytes()==0);
        System.out.println("      (stress: "+all.size()+" entries; final live="+size+"/"+live+" bytes; stats="+c.stats()+")");
    }
}