package io.github.kusoroadeolu.cleap.stress;

import io.github.kusoroadeolu.cleap.HOHConcurrentHeap;
import io.github.kusoroadeolu.cleap.Heap;
import org.openjdk.jcstress.annotations.*;
import org.openjdk.jcstress.infra.results.I_Result;

import java.util.concurrent.ThreadLocalRandom;

public class HOHStressTest {

    @JCStressTest
    @Outcome(id = "1", expect = Expect.ACCEPTABLE, desc = "Invariant maintained")
    @Outcome(id = "0", expect = Expect.FORBIDDEN, desc = "Invariant violated")
    @State
     public static class HeapifyInvariantTest {
        private Heap<Integer> heap;

        public HeapifyInvariantTest() {
            this.heap = new HOHConcurrentHeap<>(3);
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
