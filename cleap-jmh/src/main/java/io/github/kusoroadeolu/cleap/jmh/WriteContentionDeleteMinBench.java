package io.github.kusoroadeolu.cleap.jmh;

import io.github.kusoroadeolu.cleap.PriorityQueue;
import io.github.kusoroadeolu.cleap.experimental.LockedPQ;
import io.github.kusoroadeolu.cleap.latest.GenerationPQ;
import io.github.kusoroadeolu.cleap.latest.MpmcGenerationPQ;
import io.github.kusoroadeolu.cleap.latest.PaddedArenaGenerationPQ;
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
@Fork(2)
public class WriteContentionDeleteMinBench {

    private PriorityQueue<Integer> queue;

    @Param({"LOCK", "MPMC_EPO", "EPO", "PADDED_EPO"})
    private String type;

    @Setup
    public void setup() {
        int cap = 65536;
        queue = switch (type) {
            case "LOCK" -> new LockedPQ<>(cap);
            case "EPO" -> new GenerationPQ<>(cap);
            case "PADDED_EPO" -> new PaddedArenaGenerationPQ<>(cap);
            case "MPMC_EPO" -> new MpmcGenerationPQ<>(cap);
            default -> throw new RuntimeException();
        };

        int to = cap / 2;
        for (int i = 0; i < to; ++i) {
            queue.add(ThreadLocalRandom.current().nextInt(1_000_000));
        }
    }

    @AuxCounters(AuxCounters.Type.OPERATIONS)
    @State(Scope.Thread)
    public static class WriterCounters {
        public long writerSucceeded;
        public long writerRejected;

        @Setup(Level.Iteration)
        public void reset() {
            writerSucceeded = 0;
            writerRejected = 0;
        }
    }

    @AuxCounters(AuxCounters.Type.OPERATIONS)
    @State(Scope.Thread)
    public static class ReaderCounters {
        public long readerSucceeded;
        public long readerEmpty;

        @Setup(Level.Iteration)
        public void reset() {
            readerSucceeded = 0;
            readerEmpty = 0;
        }
    }


    @Group("ratio_6_2")
    @GroupThreads(6)
    @Benchmark
    public void insert_6_2(Blackhole bh, WriterCounters wc) {
        boolean added = queue.add(ThreadLocalRandom.current().nextInt(1_000_000));
        if (added) wc.writerSucceeded++; else wc.writerRejected++;
        bh.consume(added);
    }

    @Group("ratio_6_2")
    @GroupThreads(2)
    @Benchmark
    public void deleteMin_6_2(Blackhole bh, ReaderCounters rc) {
        Integer result = queue.poll();
        if (result == null) rc.readerEmpty++; else rc.readerSucceeded++;
        bh.consume(result);
    }


    @Group("ratio_4_4")
    @GroupThreads(4)
    @Benchmark
    public void insert_4_4(Blackhole bh, WriterCounters wc) {
        boolean added = queue.add(ThreadLocalRandom.current().nextInt(1_000_000));
        if (added) wc.writerSucceeded++; else wc.writerRejected++;
        bh.consume(added);
    }

    @Group("ratio_4_4")
    @GroupThreads(4)
    @Benchmark
    public void deleteMin_4_4(Blackhole bh, ReaderCounters rc) {
        Integer result = queue.poll();
        if (result == null) rc.readerEmpty++; else rc.readerSucceeded++;
        bh.consume(result);
    }

    static class BenchRunner {
        static void main() throws RunnerException {
            Options options = new OptionsBuilder()
                    .include(WriteContentionDeleteMinBench.class.getSimpleName())
                    .build();
            new org.openjdk.jmh.runner.Runner(options).run();
        }
    }
}