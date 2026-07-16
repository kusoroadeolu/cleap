package io.github.kusoroadeolu.cleap.stress;

import io.github.kusoroadeolu.cleap.dualarray.LBBoundedPQ;
import org.openjdk.jcstress.annotations.*;
import org.openjdk.jcstress.infra.results.II_Result;
import org.openjdk.jcstress.infra.results.I_Result;

import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadLocalRandom;

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
            Set<Integer> all = new HashSet<>(heap.drain());
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

    @JCStressTest()
    @Outcome(id = {"1, 1"}, expect = Expect.ACCEPTABLE, desc = "Invariant maintained")
    @State
    //Basically ensure a thread never sees zero in the I_INDEX varhandle during a merge
    public static class NoLostWritesTwo {
        private LBBoundedPQ<Integer> heap;


        public NoLostWritesTwo() {
            this.heap = new LBBoundedPQ<>(20, 1); //This should give a delete cap of 2
            for (int i = 0; i < 2; ++i) {
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


    @JCStressTest()
    @Outcome(id = {"1, 1", "0, 1"}, expect = Expect.ACCEPTABLE, desc = "Invariant maintained")
    @State
    //Basically ensure a thread never sees zero in the I_INDEX varhandle during a merge
    public static class SizeConsistency {
        private LBBoundedPQ<Integer> heap;


        public SizeConsistency() {
            this.heap = new LBBoundedPQ<>(20, 1); //This should give a delete cap of 2
        }

        //One thread should trigger a merge, so one thread should at least valid value

        @Actor
        public void adder(){
            heap.add(ThreadLocalRandom.current().nextInt());
        }

        @Actor
        public void adder1(){
            heap.add(ThreadLocalRandom.current().nextInt());
        }

        @Actor
        public void poller2(II_Result r){
            var x = heap.poll();
            r.r1 = x == null ? 0 : 1;
        }

        @Arbiter
        public void arbiter(II_Result r) {
            boolean isNull = r.r1 == 0;
            if (isNull && heap.size() < 2) r.r2 = 0;
            else r.r2 = 1;
        }
    }


    @JCStressTest()
    @Outcome(id = "1", expect = Expect.ACCEPTABLE, desc = "Invariant maintained")
    @State
    //Basically ensure a thread never sees zero in the I_INDEX varhandle during a merge
    public static class BoundedInvariant {
        private LBBoundedPQ<Integer> heap;


        public BoundedInvariant() {
            this.heap = new LBBoundedPQ<>(2, 1); //This should give a delete cap of 2
        }

        //One thread should trigger a merge, so one thread should at least valid value

        @Actor
        public void adder(){
            heap.add(ThreadLocalRandom.current().nextInt());
        }

        @Actor
        public void adder1(){
            heap.add(ThreadLocalRandom.current().nextInt());
        }
        @Actor
        public void adder2(){
            heap.add(ThreadLocalRandom.current().nextInt());
        }

        @Actor
        public void poller2(){
            heap.poll();

        }

        @Actor
        public void arbiter(I_Result r) {
            r.r1 = heap.size() > 2 ? 0 : 1;
        }
    }
}

