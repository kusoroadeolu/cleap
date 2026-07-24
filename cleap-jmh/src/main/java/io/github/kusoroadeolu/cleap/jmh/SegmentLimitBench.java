package io.github.kusoroadeolu.cleap.jmh;

import io.github.kusoroadeolu.cleap.PriorityQueue;
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
@Fork(3)
public class SegmentLimitBench {
    private PriorityQueue<Integer> queue;

    @Param({"PADDED-GEN"})
    private String type;

    @Param ({"128", "256", "512" ,"2048", "4096"})
    private String segmentLimit;

    @Setup
    public void setup() {
        int cap = 65536;
        queue = switch (type) {
            case "PADDED-GEN" -> new PaddedArenaGenerationPQ<>(cap, Long.parseLong(segmentLimit));
            default -> throw new RuntimeException();
        };

        int to = cap / 2;
        for (int i = 0; i < to; ++i) {
            queue.add(ThreadLocalRandom.current().nextInt(1_000_000));
        }
    }

    @Group("ratio_4_4")
    @GroupThreads(4)
    @Benchmark
    public void insert_4_4(Blackhole bh) {
        bh.consume(queue.add(ThreadLocalRandom.current().nextInt(1_000_000)));
    }

    @Group("ratio_4_4")
    @GroupThreads(4)
    @Benchmark
    public void deleteMin_4_4(Blackhole bh) {
        bh.consume(queue.poll());
    }

    static class BenchRunner {
        static void main() throws RunnerException {
            Options options = new OptionsBuilder()
                    .include(SegmentLimitBench.class.getSimpleName())
                    .build();
            new org.openjdk.jmh.runner.Runner(options).run();
        }
    }

}
