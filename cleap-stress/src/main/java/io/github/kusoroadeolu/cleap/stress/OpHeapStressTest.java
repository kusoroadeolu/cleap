package io.github.kusoroadeolu.cleap.stress;

import io.github.kusoroadeolu.cleap.Heap;
import io.github.kusoroadeolu.cleap.StagedConcurrentHeap;
import org.openjdk.jcstress.annotations.*;
import org.openjdk.jcstress.infra.results.I_Result;

public class OpHeapStressTest {

    @JCStressTest
    @Outcome(id = "1", expect = Expect.ACCEPTABLE, desc = "Invariant maintained")
    @Outcome(id = "0", expect = Expect.FORBIDDEN, desc = "Invariant violated")
    @State
     public static class HeapifyInvariantTest {
        private Heap<Integer> heap;

        public HeapifyInvariantTest() {
            this.heap = new StagedConcurrentHeap<>();
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
