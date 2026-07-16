package io.github.kusoroadeolu.cleap.stress;

import io.github.kusoroadeolu.cleap.PriorityQueue;
import io.github.kusoroadeolu.cleap.experimental.OptimisticConcurrentPriorityQueue;
import org.openjdk.jcstress.annotations.*;
import org.openjdk.jcstress.infra.results.I_Result;

import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static org.openjdk.jcstress.annotations.Expect.ACCEPTABLE;
import static org.openjdk.jcstress.annotations.Expect.ACCEPTABLE_INTERESTING;

public class OpHeapStress {

    @JCStressTest
    @Outcome(id = "1", expect = Expect.ACCEPTABLE, desc = "Invariant maintained")
    @Outcome(id = "0", expect = Expect.FORBIDDEN, desc = "Invariant violated")
    @State
    //Assert deleted nodes are never inserted
     public static class DeletedNodeInvariant {
        private PriorityQueue<Integer> priorityQueue;


        public DeletedNodeInvariant() {
            this.priorityQueue = new OptimisticConcurrentPriorityQueue<>(List.of(1, 2, 3));
        }

        @Actor
        public void poller(){
            priorityQueue.poll();
        }


        @Arbiter
        public void arbiter(I_Result res) {
            priorityQueue.add(0);
            if (priorityQueue.peek() != 3) res.r1 = 1;
            else res.r1 = 0;
        }

    }


    @JCStressTest
    @Outcome(id = "0", expect = Expect.ACCEPTABLE, desc = "Invariant maintained")
    @State
    //Assert size isn't incremented or decremented if nothing is in the actual priority queue
    public static class EmptyHeapInvariant {
        private PriorityQueue<Integer> priorityQueue;


        public EmptyHeapInvariant() {
            this.priorityQueue = new OptimisticConcurrentPriorityQueue<>(List.of(1, 2, 3));
        }

        @Actor
        public void poller(){
            priorityQueue.poll();
        }


        @Arbiter
        public void arbiter(I_Result res) {
            res.r1 = priorityQueue.size();
        }

    }

    @JCStressTest(Mode.Termination)
    @Outcome(id = "TERMINATED", expect = ACCEPTABLE,             desc = "Gracefully finished")
    @Outcome(id = "STALE",      expect = ACCEPTABLE_INTERESTING, desc = "Test is stuck")
    @State
    //Assert deleted nodes are never inserted
    public static class RWVisibility {
        private ReadWriteLock rwLock = new ReentrantReadWriteLock();
        private boolean signal;

        @Signal
        public void readLocker() {
            rwLock.readLock().lock();
            try {
                signal = true;
            }finally {
                rwLock.readLock().unlock();
            }
        }

        @Actor
        public void writeLocker() {
            for (;;) {
                rwLock.writeLock().lock();
                try {
                    if (signal) break;
                }finally {
                    rwLock.writeLock().unlock();
                }
            }
        }

    }
}
