package io.github.kusoroadeolu.cleap.jmh;

import io.github.kusoroadeolu.cleap.Heap;
import io.github.kusoroadeolu.cleap.bounded.CombiningLBBoundedPQ;
import io.github.kusoroadeolu.cleap.bounded.LBBoundedPQ;
import io.github.kusoroadeolu.cleap.bounded.LockedPQ;
import io.github.kusoroadeolu.cleap.bounded.OrderedBoundedPQ;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.profile.JavaFlightRecorderProfiler;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/*
* Benchmark                    (type)   Mode  Cnt   Score   Error   Units
HeavyPollBench.eightThreads    LOCK  thrpt   30  32.505 ± 0.819  ops/us
HeavyPollBench.eightThreads     OBQ  thrpt   30  13.166 ± 0.619  ops/us
HeavyPollBench.eightThreads      LB  thrpt   30   0.982 ± 0.098  ops/us
HeavyPollBench.eightThreads     ELB  thrpt   30   0.917 ± 0.058  ops/us
* */



@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 10, time = 1)
@Measurement(iterations = 10, time = 1)
@Fork(3)
public class HeavyPollBench {
    private Heap<Integer> queue;

    @Param({ "LB", "ELB"})
    private String type;

    @State(Scope.Thread)
    public static class ThreadState {
        int nextInt() {
            return ThreadLocalRandom.current().nextInt(100);
        }
    }

    @Setup
    public void setup() {
        queue = switch (type) {
            case "LOCK" -> new LockedPQ<>(10000);
            case "LB" -> new LBBoundedPQ<>(10000, 10);
            case "ELB" -> new CombiningLBBoundedPQ<>(10000, 10);
            case "OBQ" -> new OrderedBoundedPQ<>(10000);


            default -> throw new RuntimeException();
        };

        for (int i = 0; i < 1000; ++i) queue.add(ThreadLocalRandom.current().nextInt(1_000_000));

    }


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
        int next = ts.nextInt();
        if (next >= 0 && next < 79) bh.consume(queue.poll());
       else bh.consume(queue.add(ThreadLocalRandom.current().nextInt(1_000_000)));

    }

    static class BenchRunner {
        static void main() throws RunnerException {
            Options options = new OptionsBuilder()
                    .include(HeavyPollBench.class.getSimpleName())
                    .addProfiler(JavaFlightRecorderProfiler.class, "dir=C:\\jfr-hp")
                    .build();
            new org.openjdk.jmh.runner.Runner(options).run();
        }

    }
}

/*
Benchmark                            (type)    Mode      Cnt      Score   Error  Units
HeavyPollBench.eightThreads            LOCK  sample  5960936      1.284 ± 0.017  us/op
HeavyPollBench.eightThreads:p0.00      LOCK  sample                 ≈ 0          us/op
HeavyPollBench.eightThreads:p0.50      LOCK  sample                 ≈ 0          us/op
HeavyPollBench.eightThreads:p0.90      LOCK  sample               0.100          us/op
HeavyPollBench.eightThreads:p0.95      LOCK  sample               0.100          us/op
HeavyPollBench.eightThreads:p0.99      LOCK  sample              55.552          us/op
HeavyPollBench.eightThreads:p0.999     LOCK  sample              78.720          us/op
HeavyPollBench.eightThreads:p0.9999    LOCK  sample             110.720          us/op
HeavyPollBench.eightThreads:p1.00      LOCK  sample            5734.400          us/op

HeavyPollBench.eightThreads              LB  sample  6245491     10.909 ± 0.155  us/op
HeavyPollBench.eightThreads:p0.00        LB  sample                 ≈ 0          us/op
HeavyPollBench.eightThreads:p0.50        LB  sample               0.400          us/op
HeavyPollBench.eightThreads:p0.90        LB  sample              30.272          us/op
HeavyPollBench.eightThreads:p0.95        LB  sample              40.256          us/op
HeavyPollBench.eightThreads:p0.99        LB  sample              62.656          us/op
HeavyPollBench.eightThreads:p0.999       LB  sample             375.296          us/op
HeavyPollBench.eightThreads:p0.9999      LB  sample            5701.632          us/op
HeavyPollBench.eightThreads:p1.00        LB  sample           21561.344          us/op

HeavyPollBench.eightThreads             ELB  sample  6485655      9.313 ± 0.119  us/op
HeavyPollBench.eightThreads:p0.00       ELB  sample                 ≈ 0          us/op
HeavyPollBench.eightThreads:p0.50       ELB  sample               0.400          us/op
HeavyPollBench.eightThreads:p0.90       ELB  sample              25.888          us/op
HeavyPollBench.eightThreads:p0.95       ELB  sample              34.944          us/op
HeavyPollBench.eightThreads:p0.99       ELB  sample              55.744          us/op
HeavyPollBench.eightThreads:p0.999      ELB  sample             141.312          us/op
HeavyPollBench.eightThreads:p0.9999     ELB  sample            3964.928          us/op
HeavyPollBench.eightThreads:p1.00       ELB  sample           27099.136          us/op
" these are the results


* "
Benchmark                            (type)    Mode      Cnt      Score   Error  Units
HeavyPollBench.eightThreads             OBQ  sample  5816405      1.955 ± 0.083  us/op
HeavyPollBench.eightThreads:p0.00       OBQ  sample                 ≈ 0          us/op
HeavyPollBench.eightThreads:p0.50       OBQ  sample               0.200          us/op
HeavyPollBench.eightThreads:p0.90       OBQ  sample               0.900          us/op
HeavyPollBench.eightThreads:p0.95       OBQ  sample               1.300          us/op
HeavyPollBench.eightThreads:p0.99       OBQ  sample              51.840          us/op
HeavyPollBench.eightThreads:p0.999      OBQ  sample             107.776          us/op
HeavyPollBench.eightThreads:p0.9999     OBQ  sample            1513.472          us/op
HeavyPollBench.eightThreads:p1.00       OBQ  sample           21561.344          us/op
* */

