package io.github.kusoroadeolu.cleap.stress;

import io.github.kusoroadeolu.cleap.HOHConcurrentHeap;
import io.github.kusoroadeolu.cleap.Heap;
import io.github.kusoroadeolu.cleap.OptimisticConcurrentHeap;
import org.openjdk.jcstress.annotations.*;
import org.openjdk.jcstress.infra.results.I_Result;

import java.util.concurrent.ThreadLocalRandom;

public class OpHeapStressTest {

    @JCStressTest
    @Outcome(id = "1", expect = Expect.ACCEPTABLE, desc = "Invariant maintained")
    @Outcome(id = "0", expect = Expect.FORBIDDEN, desc = "Invariant violated")
    @State
     public static class HeapifyInvariantTest {
        private Heap<Integer> heap;

        public HeapifyInvariantTest() {
            this.heap = new OptimisticConcurrentHeap<>();
        }

        @Actor
        public void inserter(){
            heap.insert((ThreadLocalRandom.current().nextInt() % 3) + 1);
        }

        @Actor
        public void deleter(){
            heap.poll();
        }


        @Arbiter
        public void arbiter(I_Result res) {
            res.r1 = 1;
        }

    }
}
