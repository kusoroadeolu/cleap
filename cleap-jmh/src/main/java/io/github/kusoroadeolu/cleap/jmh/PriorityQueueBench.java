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

/*
Benchmark                        (type)   Mode  Cnt  Score   Error   Units
PriorityQueueBench.eightThreads     JDK  thrpt   30  7.723 ± 1.190  ops/us
PriorityQueueBench.eightThreads     OPT  thrpt   30  5.217 ± 0.090  ops/us
PriorityQueueBench.eightThreads     STA  thrpt   30  4.886 ± 0.075  ops/us
PriorityQueueBench.fourThreads      JDK  thrpt   30  8.411 ± 1.023  ops/us
PriorityQueueBench.fourThreads      OPT  thrpt   30  5.494 ± 0.151  ops/us
PriorityQueueBench.fourThreads      STA  thrpt   30  5.097 ± 0.127  ops/us
PriorityQueueBench.twoThreads       JDK  thrpt   30  7.627 ± 0.939  ops/us
PriorityQueueBench.twoThreads       OPT  thrpt   30  5.802 ± 0.040  ops/us
PriorityQueueBench.twoThreads       STA  thrpt   30  5.290 ± 0.120  ops/us
* Initial benchmarks. We aren't too far off from the J
*
* * */

public class PriorityQueueBench {
    private Queue<Integer> queue;

    @Param({"JDK", "OPT", "STA"}) //JDK, Optimistic, Staged
    private String type;

    @State(Scope.Thread)
    public static class ThreadState {
        boolean insert = true;
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
        ts.insert = !isInsert;
        bh.consume(isInsert
                ? queue.offer(ThreadLocalRandom.current().nextInt(10_000))
                : queue.poll());
    }
}