package io.github.kusoroadeolu.cleap.stress;

import io.github.kusoroadeolu.cleap.Heap;
import io.github.kusoroadeolu.cleap.OptimisticConcurrentHeap;
import org.openjdk.jcstress.annotations.*;
import org.openjdk.jcstress.infra.results.I_Result;

import java.util.List;

public class OpHeapStress {

    @JCStressTest
    @Outcome(id = "1", expect = Expect.ACCEPTABLE, desc = "Invariant maintained")
    @Outcome(id = "0", expect = Expect.FORBIDDEN, desc = "Invariant violated")
    @State
    //Assert deleted nodes are never inserted
     public static class DeletedNodeInvariant {
        private Heap<Integer> heap;


        public DeletedNodeInvariant() {
            this.heap = new OptimisticConcurrentHeap<>(List.of(1, 2, 3));
        }

        @Actor
        public void poller(){
            heap.poll();
        }


        @Arbiter
        public void arbiter(I_Result res) {
            heap.add(0);
            if (heap.peek() != 3) res.r1 = 1;
            else res.r1 = 0;
        }

        }


    @JCStressTest
    @Outcome(id = "0", expect = Expect.ACCEPTABLE, desc = "Invariant maintained")
    @State
    //Assert size isn't incremented or decremented if nothing is in the actual priority queue
    public static class EmptyHeapInvariant {
        private Heap<Integer> heap;


        public EmptyHeapInvariant() {
            this.heap = new OptimisticConcurrentHeap<>(List.of(1, 2, 3));
        }

        @Actor
        public void poller(){
            heap.poll();
        }


        @Arbiter
        public void arbiter(I_Result res) {
            res.r1 = heap.size();
        }

    }

//    @JCStressTest
//    @Outcome(id = "0", expect = Expect.ACCEPTABLE, desc = "Invariant maintained")
//    @State
//    //Assert size is incremented on inserts and deleted nodes are skipped
//    public static class HeapSizeInvariant {
//        private Heap<Integer> heap;
//
//
//        public HeapSizeInvariant() {
//            this.heap = new OptimisticConcurrentHeap<>();
//        }
//
//        @Actor
//        public void poller(){
//            heap.poll();
//        }
//
//        @Actor
//        public void inserter(){
//            heap.add(0);
//        }
//
//
//        @Arbiter
//        public void arbiter(I_Result res) {
//            res.r1 = heap.size(); //Should always be 3, since we're the only i
//        }
//
//    }

}
