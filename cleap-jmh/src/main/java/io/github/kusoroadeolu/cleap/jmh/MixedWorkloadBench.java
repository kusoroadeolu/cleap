package io.github.kusoroadeolu.cleap.jmh;

import io.github.kusoroadeolu.cleap.OptimisticConcurrentHeap;
import io.github.kusoroadeolu.cleap.StagedConcurrentHeap;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.Queue;
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
MixedWorkloadBench.eightThreads     JDK  thrpt   30  7.723 ± 1.190  ops/us
MixedWorkloadBench.eightThreads     OPT  thrpt   30  5.217 ± 0.090  ops/us
MixedWorkloadBench.eightThreads     STA  thrpt   30  4.886 ± 0.075  ops/us
MixedWorkloadBench.fourThreads      JDK  thrpt   30  8.411 ± 1.023  ops/us
MixedWorkloadBench.fourThreads      OPT  thrpt   30  5.494 ± 0.151  ops/us
MixedWorkloadBench.fourThreads      STA  thrpt   30  5.097 ± 0.127  ops/us
MixedWorkloadBench.twoThreads       JDK  thrpt   30  7.627 ± 0.939  ops/us
MixedWorkloadBench.twoThreads       OPT  thrpt   30  5.802 ± 0.040  ops/us
MixedWorkloadBench.twoThreads       STA  thrpt   30  5.290 ± 0.120  ops/us
* Initial benchmarks. We aren't too far off from the JDK's implementation. Right now, while I haven't profiled this, I do believe try locks add an extra layer of contention,
since immediately after the CAS operation all threads race for the lock immediately
* Rather than this, let's encode state into the stack itself, if we cas and our next node pointer == null, we are fit to take the lock otherwise we are not

When we hold the lock and detach the head, we atomically set the head to null and immediately make it visible, so we can never have a lost write by a node


enchmark                        (type)   Mode  Cnt  Score   Error   Units
MixedWorkloadBench.eightThreads     OPT  thrpt   30  4.409 ± 0.549  ops/us
MixedWorkloadBench.eightThreads     STA  thrpt   30  4.334 ± 0.226  ops/us
MixedWorkloadBench.fourThreads      OPT  thrpt   30  5.931 ± 0.165  ops/us
MixedWorkloadBench.fourThreads      STA  thrpt   30  4.557 ± 0.098  ops/us
MixedWorkloadBench.twoThreads       OPT  thrpt   30  5.111 ± 0.168  ops/us
MixedWorkloadBench.twoThreads       STA  thrpt   30  3.801 ± 0.117  ops/us

So I tested the idea. In theory the idea seems smart until you realize both pollers and inserts are now waiting on the same lock. Take a scenario where we have 4 inserters and 4 pollers
We've basically increased the wait count by 1. I extra waiter. Persay an inserted gets starved of the lock for a while, other inserts who's next pointer are not null are accumulating more nodes on the stack for the waiting inserter to deal with
Unlike the previous algo where the inserter just dips

After trying to use my fast path optimization for OPT (where we read the stack on polls outside the lock) and recheck once we reacquire the lock, rereading the stack if we have a dead node. Looks like its worse at two and 4 threads but better at 8 threads. Doesn;t beat the JDK impl though
Benchmark                         Mode  Cnt  Score   Error   Units
MixedWorkloadBench.eightThreads  thrpt   30  5.720 ± 0.153  ops/us
MixedWorkloadBench.fourThreads   thrpt   30  5.476 ± 0.209  ops/us
MixedWorkloadBench.twoThreads    thrpt   30  5.650 ± 0.126  ops/us

I'll go back to my initial implementation for now
* * */

public class MixedWorkloadBench {
    private Queue<Integer> queue;

    //@Param({"OPT", "STA"}) //JDK, Optimistic, Staged
    private String type;

    @State(Scope.Thread)
    public static class ThreadState {
        boolean insert = true;
    }

    @Setup
    public void setup() {
        queue = new OptimisticConcurrentHeap<>();

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