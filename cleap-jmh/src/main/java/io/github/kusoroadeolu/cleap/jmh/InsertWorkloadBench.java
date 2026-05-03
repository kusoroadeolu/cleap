package io.github.kusoroadeolu.cleap.jmh;

import io.github.kusoroadeolu.cleap.OptimisticConcurrentHeap;
import io.github.kusoroadeolu.cleap.StagedConcurrentHeap;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.Queue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 10, time = 1)
@Measurement(iterations = 10, time = 1)
@Fork(3)
public class InsertWorkloadBench {
    private Queue<Integer> queue;


    /*
    * STA uses the previous insert logic of "trying to acquire the lock" after CAS to head
    * OPT uses the new insert logic of embedding state into the node and only acquiring the lock if the node's next pointer == null
    * Benchmark                         (type)   Mode  Cnt  Score   Error   Units
        InsertWorkloadBench.eightThreads     JDK  thrpt   30  4.242 ± 1.476  ops/us
        InsertWorkloadBench.eightThreads     OPT  thrpt   30  4.096 ± 1.297  ops/us
        InsertWorkloadBench.eightThreads     STA  thrpt   30  3.803 ± 0.944  ops/us
        InsertWorkloadBench.fourThreads      JDK  thrpt   30  4.516 ± 1.245  ops/us
        InsertWorkloadBench.fourThreads      OPT  thrpt   30  4.238 ± 1.151  ops/us
        InsertWorkloadBench.fourThreads      STA  thrpt   30  2.983 ± 0.750  ops/us
        InsertWorkloadBench.twoThreads       JDK  thrpt   30  3.410 ± 0.692  ops/us
        InsertWorkloadBench.twoThreads       OPT  thrpt   30  3.380 ± 0.847  ops/us
        InsertWorkloadBench.twoThreads       STA  thrpt   30  3.349 ± 0.928  ops/us
        Here we can see that the null -> acquire lock actually has better thrpt across all thread counts
    * */
    @Param({"JDK", "OPT", "STA"}) //JDK, Optimistic, Staged
    private String type;

    @State(Scope.Thread)
    public static class ThreadState {
        boolean insert = true;
        int count = 0;
    }

    @Setup
    public void setup() {
        queue = switch (type) {
            case "JDK" -> new PriorityBlockingQueue<>();
            case "OPT" -> new OptimisticConcurrentHeap<>();
            case "STA" -> new StagedConcurrentHeap<>();
            default -> throw new IllegalArgumentException();
        };

        for (int i = 0; i < 1000; i++) queue.offer(i);
    }

    @Threads(2)
    @Benchmark
    public void twoThreads(Blackhole bh, ThreadState ts) {
        doWork(bh, ts);
    }

    @Threads(4)
    @Benchmark
    public void fourThreads(Blackhole bh, ThreadState ts) {
        doWork(bh, ts);
    }

    @Threads(8)
    @Benchmark
    public void eightThreads(Blackhole bh, ThreadState ts) {
        doWork(bh, ts);
    }


//    @Threads(16)
//    @Benchmark
//    public void sixteenThreads(Blackhole bh, ThreadState ts) {
//        doWork(bh, ts);
//    }
//
//    @Threads(32)
//    @Benchmark
//    public void thirtyTwoThreads(Blackhole bh, ThreadState ts) {
//        doWork(bh, ts);
//    }

    private void doWork(Blackhole bh, ThreadState ts) {
        boolean isInsert = ts.insert;
        if (++ts.count == 100) { //Every 100 iterations poll
            isInsert = !isInsert;
            ts.count = 0;
        }
        bh.consume(isInsert
                ? queue.offer(ThreadLocalRandom.current().nextInt(10_000))
                : queue.poll());
    }
}
