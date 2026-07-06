package io.github.kusoroadeolu.cleap.jmh;

import io.github.kusoroadeolu.cleap.Heap;
import io.github.kusoroadeolu.cleap.bounded.ElimLBBoundedPQ;
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

@BenchmarkMode(Mode.SampleTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 10, time = 1)
@Measurement(iterations = 10, time = 1)
@Fork(3)
public class HeavyPollBench {
    private Heap<Integer> queue;

    @Param({"LB"})
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
            case "LB" -> new LBBoundedPQ<>(10000);
            case "ELB" -> new OrderedBoundedPQ<>(10000);

            default -> throw new RuntimeException();
        };

        for (int i = 0; i < 1000; ++i) queue.add(ThreadLocalRandom.current().nextInt(1_000_000));

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
        int next = ts.nextInt();
        if (next >= 0 && next < 79) bh.consume(queue.poll());
       else bh.consume(queue.add(ThreadLocalRandom.current().nextInt(1_000_000)));

    }

    static class BenchRunner {
        static void main() throws RunnerException {
            Options options = new OptionsBuilder()
                    .include(HeavyPollBench.class.getSimpleName())
                    .addProfiler(JavaFlightRecorderProfiler.class, "dir=C:\\jfr-sl")
                    .build();
            new org.openjdk.jmh.runner.Runner(options).run();
        }

    }
}
