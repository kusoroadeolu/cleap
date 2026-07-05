package io.github.kusoroadeolu.cleap.stress;

import io.github.kusoroadeolu.cleap.Heap;
import io.github.kusoroadeolu.cleap.bounded.LBBoundedPQ;
import io.github.kusoroadeolu.cleap.experimental.OptimisticConcurrentHeap;
import org.openjdk.jcstress.annotations.*;
import org.openjdk.jcstress.infra.results.II_Result;
import org.openjdk.jcstress.infra.results.I_Result;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.openjdk.jcstress.annotations.Expect.ACCEPTABLE;
import static org.openjdk.jcstress.annotations.Expect.ACCEPTABLE_INTERESTING;

public class LBPQStress {
    @JCStressTest
    @Outcome(id = "1", expect = Expect.ACCEPTABLE, desc = "Invariant maintained")
    @Outcome(id = "0", expect = Expect.FORBIDDEN, desc = "Invariant violated")
    @State
    //Assert no writes are lost
    public static class NoLostWrites {
        private LBBoundedPQ<Integer> heap;
        private Set<Integer> seen;
        private Queue<Integer> queue;


        public NoLostWrites() {
            this.heap = new LBBoundedPQ<>(10, 1);
            seen = ConcurrentHashMap.newKeySet();
            queue = new ConcurrentLinkedQueue<>();
        }

        @Actor
        public void poller(){
            var x = heap.poll();
            if (x != null) seen.add(x);
        }

        @Actor
        public void poller2(){
            var x = heap.poll();
            if (x != null) seen.add(x);

        }

        @Actor
        public void adder(){
            doWork();
        }


        void doWork() {
            for (int i = 0; i < 20; ++i) {
                int x = ThreadLocalRandom.current().nextInt(50);
                boolean added = heap.add(x);
                if (added) queue.add(x);
            }
        }


        @Arbiter
        public void arbiter(I_Result res) {
            Set<Integer> all = new HashSet<>(heap.toList());
            all.addAll(seen);

            Set<Integer> added = new HashSet<>(queue);
            if (all.size() != added.size()) {
                res.r1 = 0;
                return;
            }

            for (Integer i : all) {
                if (!added.contains(i)) {
                    res.r1 = 0;
                    return;
                }
            }

            res.r1 = 1;
            queue.clear();
            seen.clear();
        }

    }

    @JCStressTest
    @Outcome(id = {"1, 1"}, expect = Expect.ACCEPTABLE, desc = "Invariant maintained")
    @State
    //If a merge is necessary, both values do not return null
    public static class MergeNoNull {
        private LBBoundedPQ<Integer> heap;


        public MergeNoNull() {
            this.heap = new LBBoundedPQ<>(100, 1);
            for (int i = 0; i < 15; ++i) {
                heap.add(ThreadLocalRandom.current().nextInt(100));
            }
        }

        //One thread should trigger a merge, so one thread should at least valid value

        @Actor
        public void poller(II_Result r){
            var x = heap.poll();
            if (x == null) r.r1 = 0;
            else r.r1 = 1;
        }

        @Actor
        public void poller2(II_Result r){
            var x = heap.poll();
            if (x == null) r.r2 = 0;
            else r.r2 = 1;
        }
    }

    @JCStressTest(Mode.Termination)
    @Outcome(id = "TERMINATED", expect = ACCEPTABLE,             desc = "Gracefully finished")
    @Outcome(id = "STALE",      expect = ACCEPTABLE_INTERESTING, desc = "Test is stuck")
    @State
    //If a merge is necessary, both values do not return null
    public static class Misc {
        private AtomicBoolean a = new AtomicBoolean(false);
        private AtomicBoolean b = new AtomicBoolean(false);

        //One thread should trigger a merge, so one thread should at least valid value
        @Signal
        public void writer() {
            a.setPlain(true);
            b.setRelease(true);
        }

        @Actor
        public void reader() {
            while (!a.getAcquire());
        }

    }
}
