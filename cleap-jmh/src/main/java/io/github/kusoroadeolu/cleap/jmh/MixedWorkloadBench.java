package io.github.kusoroadeolu.cleap.jmh;

import io.github.kusoroadeolu.cleap.Heap;
import io.github.kusoroadeolu.cleap.dualarray.LockedPQ;
import io.github.kusoroadeolu.cleap.dualarray.OrderedBoundedPQ;
import io.github.kusoroadeolu.cleap.latest.EpochPQ;
import io.github.kusoroadeolu.cleap.latest.PaddedArenaEpochPQ;
import io.github.kusoroadeolu.cleap.latest.MpmcEpochPQ;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.SampleTime)
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

/*
* Benchmark                         (type)  Mode  Cnt  Score   Error  Units
InsertWorkloadBench.eightThreads    PIPQ  avgt   30  1.050 ± 0.064  us/op
InsertWorkloadBench.eightThreads     JDK  avgt   30  0.781 ± 0.046  us/op
InsertWorkloadBench.fourThreads     PIPQ  avgt   30  0.412 ± 0.019  us/op
InsertWorkloadBench.fourThreads      JDK  avgt   30  0.380 ± 0.022  us/op
MixedWorkloadBench.eightThreads     PIPQ  avgt   30  0.833 ± 0.009  us/op
MixedWorkloadBench.eightThreads      JDK  avgt   30  0.519 ± 0.014  us/op
MixedWorkloadBench.fourThreads      PIPQ  avgt   30  0.332 ± 0.004  us/op
MixedWorkloadBench.fourThreads       JDK  avgt   30  0.225 ± 0.006  us/op
*
*
* Benchmark                         (type)   Mode  Cnt   Score   Error   Units
InsertWorkloadBench.eightThreads    PIPQ  thrpt   30   7.916 ± 0.505  ops/us
InsertWorkloadBench.eightThreads     JDK  thrpt   30  10.678 ± 0.741  ops/us
InsertWorkloadBench.fourThreads     PIPQ  thrpt   30  10.096 ± 0.616  ops/us
InsertWorkloadBench.fourThreads      JDK  thrpt   30  10.472 ± 0.643  ops/us
MixedWorkloadBench.eightThreads     PIPQ  thrpt   30   9.630 ± 0.258  ops/us
MixedWorkloadBench.eightThreads      JDK  thrpt   30  15.488 ± 0.619  ops/us
MixedWorkloadBench.fourThreads      PIPQ  thrpt   30  12.080 ± 0.124  ops/us
MixedWorkloadBench.fourThreads       JDK  thrpt   30  18.096 ± 0.280  ops/us
* */

public class MixedWorkloadBench {
    private Heap<Integer> queue;

    @Param({"LOCK", "MPMC_EPO", "EPO", "PADDED_EPO"})
    private String type;

    @Param({"32768", "65536"})
    private String cap;

    @State(Scope.Thread)
    public static class ThreadState {
        boolean insert = true;
    }

    @Setup
    public void setup() {
        int cap = Integer.parseInt(this.cap);
        queue = switch (type) {
            case "LOCK" -> new LockedPQ<>(cap);
            case "PADDED_EPO" -> new PaddedArenaEpochPQ<>(cap);
            case "EPO" -> new EpochPQ<>(cap);
            case "MPMC_EPO" -> new MpmcEpochPQ<>(cap);
            case "OBQ" -> new OrderedBoundedPQ<>(cap);
            default -> throw new RuntimeException();
        };

        int to = cap / 2;
        for (int i = 0; i < to; ++i) queue.add(ThreadLocalRandom.current().nextInt(1_000_000));

    }

//    @TearDown(Level.Iteration)
//    public void after() {
//        queue.clear();
//    }

//    @Threads(4)
//    @Benchmark
//    public void fourThreads(Blackhole bh, ThreadState ts) {
//        doWork(bh, ts);
//    }

    @Threads(8)
    @Benchmark
    public void eightThreads(Blackhole bh, ThreadState ts) {
        doWork(bh, ts);
    }


    private void doWork(Blackhole bh, ThreadState ts) {
        boolean isInsert = ts.insert;
        ts.insert = !isInsert;
        bh.consume(isInsert
                ? queue.add(ThreadLocalRandom.current().nextInt(1_000_000))
                : queue.poll());
    }

    static class BenchRunner {
        static void main() throws RunnerException {
            Options options = new OptionsBuilder()
                    .include(MixedWorkloadBench.class.getSimpleName())
                    //.addProfiler(JavaFlightRecorderProfiler.class, "dir=C:\\jfr-sl")
                    .build();
            new org.openjdk.jmh.runner.Runner(options).run();        }
    }

}
