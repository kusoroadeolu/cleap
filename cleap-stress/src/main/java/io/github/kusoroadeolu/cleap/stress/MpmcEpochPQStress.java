package io.github.kusoroadeolu.cleap.stress;

import io.github.kusoroadeolu.cleap.latest.MpmcEpochPQ;
import org.openjdk.jcstress.annotations.*;
import org.openjdk.jcstress.infra.results.II_Result;
import org.openjdk.jcstress.infra.results.I_Result;

import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadLocalRandom;

public class MpmcEpochPQStress {
    @JCStressTest
    @Outcome(id = "1", expect = Expect.ACCEPTABLE, desc = "Invariant maintained")
    @Outcome(id = "0", expect = Expect.FORBIDDEN, desc = "Invariant violated")
    @State
    //Assert no writes are lost
    public static class NoLostWrites {
        private MpmcEpochPQ<Integer> heap;
        private Set<Integer> seen;
        private Queue<Integer> queue;


        public NoLostWrites() {
            this.heap = new MpmcEpochPQ<>(8);
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
    @Outcome(id = "1, 1", expect = Expect.ACCEPTABLE, desc = "Invariant maintained")
    @State
    //At least one thread triggers a merge immediately, the other has to wait (so both should see concrete values)
    public static class MergeInvariant {
        private MpmcEpochPQ<Integer> heap;

        public MergeInvariant() {
            this.heap = new MpmcEpochPQ<>(8);
            heap.add(2); heap.add(1);
        }

        @Actor
        public void poller(II_Result r){
            var x = heap.poll();
            r.r1 = x == null ? 0 : 1;
        }

        @Actor
        public void poller2(II_Result r){
            var x = heap.poll();
            r.r2 = x == null ? 0 : 1;
        }

    }


    @JCStressTest
    @Outcome(id = "1, 1", expect = Expect.ACCEPTABLE, desc = "Invariant maintained")
    @State
    //At least one thread triggers a merge immediately, the other has to wait (so both should see concrete values)
    public static class EpochPriorityInvariant {
        private MpmcEpochPQ<Integer> heap;

        public EpochPriorityInvariant() {
            this.heap = new MpmcEpochPQ<>(8); //should see 3 before 1 (2 - 3 - 1)
            heap.add(3); heap.add(2); heap.add(1); //epoch len is 2, only 3 and 2 will be sorted in pq order
        }

        @Actor
        public void poller(II_Result r){
            var x = heap.poll();
            r.r1 = (x != 3 && x != 2) ? 0 : 1;
        }

        @Actor
        public void poller2(II_Result r){
            var x = heap.poll();
            r.r2 = (x != 3 && x != 2) ? 0 : 1;
        }

    }


    @JCStressTest
    @Outcome(id = "1", expect = Expect.ACCEPTABLE, desc = "Invariant maintained")
    @State
    public static class AddRemoveLinearizability {
        private MpmcEpochPQ<Integer> heap;

        public AddRemoveLinearizability() {
            this.heap = new MpmcEpochPQ<>(4); //should see 3 before 1 (2 - 3 - 1)
        }

        @Actor
        public void poller(I_Result r){
            var x = heap.poll();
            r.r1 = x == null ? 0 : 1;
        }

        @Actor
        public void adder(){
            heap.add(1); //will always succeed
        }

        @Arbiter
        public void arbiter(I_Result r) {
            List<Integer> ls = heap.drain();
            boolean contains = ls.contains(1);
            /*
            * Valid executions
            * add -> remove 1 - 1
            * remove -> add 0 - 1
            * */

            if (r.r1 == 1 && contains) r.r1 = 0;
            else r.r1 = 1;
        }

    }
}
