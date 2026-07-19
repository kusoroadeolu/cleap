package io.github.kusoroadeolu.cleap.jmh;

import io.github.kusoroadeolu.cleap.PriorityQueue;
import io.github.kusoroadeolu.cleap.dualarray.LBBoundedPQ;
import io.github.kusoroadeolu.cleap.dualarray.OrderedBoundedPQ;
import io.github.kusoroadeolu.cleap.experimental.LockedPQ;
import io.github.kusoroadeolu.cleap.latest.MpmcGenerationPQ;
import io.github.kusoroadeolu.cleap.latest.PaddedArenaGenerationPQ;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 10, time = 1)
@Measurement(iterations = 10, time = 1)
@Fork(3)
public class DeleteMinThrptBench {

    private PriorityQueue<Integer> queue;


    @Param({"LBPQ", "OBQ", "MPMC_EPO", "PADDED_EPO", "LOCK"})
    private String type;

    @Setup
    public void setup() {
        int cap = 65536;
        queue = switch (type) {
            case "LBPQ" -> new LBBoundedPQ<>(cap);
            case "OBQ" -> new OrderedBoundedPQ<>(cap);
            case "MPMC_EPO" -> new MpmcGenerationPQ<>(cap);
            case "PADDED_EPO" -> new PaddedArenaGenerationPQ<>(cap);
            case "LOCK" -> new LockedPQ<>(cap);
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
                    .include(DeleteMinThrptBench.class.getSimpleName())
                    .build();
            new org.openjdk.jmh.runner.Runner(options).run();
        }
    }
}

/*
* DeleteMinThrptBench.ratio_4_4                        LBPQ  thrpt   20   27.747 ±  9.284  ops/us
DeleteMinThrptBench.ratio_4_4:deleteMin_4_4          LBPQ  thrpt   20   25.951 ±  9.382  ops/us
DeleteMinThrptBench.ratio_4_4:insert_4_4             LBPQ  thrpt   20    1.796 ±  0.106  ops/us
DeleteMinThrptBench.ratio_4_4:readerEmpty            LBPQ  thrpt   20   24.157 ±  9.481  ops/us
DeleteMinThrptBench.ratio_4_4:readerSucceeded        LBPQ  thrpt   20    1.794 ±  0.107  ops/us
DeleteMinThrptBench.ratio_4_4:writerRejected         LBPQ  thrpt   20    0.002 ±  0.006  ops/us
DeleteMinThrptBench.ratio_4_4:writerSucceeded        LBPQ  thrpt   20    1.795 ±  0.107  ops/us
DeleteMinThrptBench.ratio_4_4                         OBQ  thrpt   20   18.217 ±  0.141  ops/us
DeleteMinThrptBench.ratio_4_4:deleteMin_4_4           OBQ  thrpt   20    2.669 ±  0.056  ops/us
DeleteMinThrptBench.ratio_4_4:insert_4_4              OBQ  thrpt   20   15.548 ±  0.179  ops/us
DeleteMinThrptBench.ratio_4_4:readerEmpty             OBQ  thrpt   20    2.556 ±  0.038  ops/us
DeleteMinThrptBench.ratio_4_4:readerSucceeded         OBQ  thrpt   20    0.115 ±  0.023  ops/us
DeleteMinThrptBench.ratio_4_4:writerRejected          OBQ  thrpt   20      ≈ 0           ops/us
DeleteMinThrptBench.ratio_4_4:writerSucceeded         OBQ  thrpt   20   15.552 ±  0.178  ops/us
DeleteMinThrptBench.ratio_4_4                    MPMC_EPO  thrpt   20  202.122 ±  4.252  ops/us
DeleteMinThrptBench.ratio_4_4:deleteMin_4_4      MPMC_EPO  thrpt   20    3.262 ±  0.083  ops/us
DeleteMinThrptBench.ratio_4_4:insert_4_4         MPMC_EPO  thrpt   20  198.860 ±  4.215  ops/us
DeleteMinThrptBench.ratio_4_4:readerEmpty        MPMC_EPO  thrpt   20      ≈ 0           ops/us
DeleteMinThrptBench.ratio_4_4:readerSucceeded    MPMC_EPO  thrpt   20    3.274 ±  0.080  ops/us
DeleteMinThrptBench.ratio_4_4:writerRejected     MPMC_EPO  thrpt   20  196.023 ±  4.161  ops/us
DeleteMinThrptBench.ratio_4_4:writerSucceeded    MPMC_EPO  thrpt   20    3.272 ±  0.079  ops/us
DeleteMinThrptBench.ratio_4_4                  PADDED_EPO  thrpt   20  204.718 ±  6.054  ops/us
DeleteMinThrptBench.ratio_4_4:deleteMin_4_4    PADDED_EPO  thrpt   20    4.308 ±  0.221  ops/us
DeleteMinThrptBench.ratio_4_4:insert_4_4       PADDED_EPO  thrpt   20  200.410 ±  6.235  ops/us
DeleteMinThrptBench.ratio_4_4:readerEmpty      PADDED_EPO  thrpt   20      ≈ 0           ops/us
DeleteMinThrptBench.ratio_4_4:readerSucceeded  PADDED_EPO  thrpt   20    4.321 ±  0.220  ops/us
DeleteMinThrptBench.ratio_4_4:writerRejected   PADDED_EPO  thrpt   20  196.595 ±  6.517  ops/us
DeleteMinThrptBench.ratio_4_4:writerSucceeded  PADDED_EPO  thrpt   20    4.321 ±  0.219  ops/us
DeleteMinThrptBench.ratio_4_4                        LOCK  thrpt   20   10.589 ±  1.766  ops/us
DeleteMinThrptBench.ratio_4_4:deleteMin_4_4          LOCK  thrpt   20    5.611 ±  1.267  ops/us
DeleteMinThrptBench.ratio_4_4:insert_4_4             LOCK  thrpt   20    4.978 ±  0.548  ops/us
DeleteMinThrptBench.ratio_4_4:readerEmpty            LOCK  thrpt   20    0.758 ±  0.695  ops/us
DeleteMinThrptBench.ratio_4_4:readerSucceeded        LOCK  thrpt   20    4.853 ±  0.622  ops/us
DeleteMinThrptBench.ratio_4_4:writerRejected         LOCK  thrpt   20    0.125 ±  0.267  ops/us
DeleteMinThrptBench.ratio_4_4:writerSucceeded        LOCK  thrpt   20    4.854 ±  0.622  ops/us
DeleteMinThrptBench.ratio_6_2                        LBPQ  thrpt   20   53.578 ± 13.578  ops/us
DeleteMinThrptBench.ratio_6_2:deleteMin_6_2          LBPQ  thrpt   20   51.920 ± 13.721  ops/us
DeleteMinThrptBench.ratio_6_2:insert_6_2             LBPQ  thrpt   20    1.658 ±  0.224  ops/us
DeleteMinThrptBench.ratio_6_2:readerEmpty            LBPQ  thrpt   20   50.340 ± 13.704  ops/us
DeleteMinThrptBench.ratio_6_2:readerSucceeded        LBPQ  thrpt   20    1.582 ±  0.194  ops/us
DeleteMinThrptBench.ratio_6_2:writerRejected         LBPQ  thrpt   20    0.076 ±  0.248  ops/us
DeleteMinThrptBench.ratio_6_2:writerSucceeded        LBPQ  thrpt   20    1.582 ±  0.186  ops/us
DeleteMinThrptBench.ratio_6_2                         OBQ  thrpt   20   20.550 ±  6.498  ops/us
DeleteMinThrptBench.ratio_6_2:deleteMin_6_2           OBQ  thrpt   20    4.660 ±  7.481  ops/us
DeleteMinThrptBench.ratio_6_2:insert_6_2              OBQ  thrpt   20   15.891 ±  1.186  ops/us
DeleteMinThrptBench.ratio_6_2:readerEmpty             OBQ  thrpt   20    4.568 ±  7.514  ops/us
DeleteMinThrptBench.ratio_6_2:readerSucceeded         OBQ  thrpt   20    0.097 ±  0.041  ops/us
DeleteMinThrptBench.ratio_6_2:writerRejected          OBQ  thrpt   20    1.200 ±  3.219  ops/us
DeleteMinThrptBench.ratio_6_2:writerSucceeded         OBQ  thrpt   20   14.695 ±  4.251  ops/us
DeleteMinThrptBench.ratio_6_2                    MPMC_EPO  thrpt   20  236.137 ±  9.092  ops/us
DeleteMinThrptBench.ratio_6_2:deleteMin_6_2      MPMC_EPO  thrpt   20    3.514 ±  0.238  ops/us
DeleteMinThrptBench.ratio_6_2:insert_6_2         MPMC_EPO  thrpt   20  232.623 ±  9.214  ops/us
DeleteMinThrptBench.ratio_6_2:readerEmpty        MPMC_EPO  thrpt   20    0.041 ±  0.148  ops/us
DeleteMinThrptBench.ratio_6_2:readerSucceeded    MPMC_EPO  thrpt   20    3.500 ±  0.173  ops/us
DeleteMinThrptBench.ratio_6_2:writerRejected     MPMC_EPO  thrpt   20  230.467 ±  8.923  ops/us
DeleteMinThrptBench.ratio_6_2:writerSucceeded    MPMC_EPO  thrpt   20    3.498 ±  0.175  ops/us
DeleteMinThrptBench.ratio_6_2                  PADDED_EPO  thrpt   20  341.970 ± 13.155  ops/us
DeleteMinThrptBench.ratio_6_2:deleteMin_6_2    PADDED_EPO  thrpt   20    3.153 ±  0.158  ops/us
DeleteMinThrptBench.ratio_6_2:insert_6_2       PADDED_EPO  thrpt   20  338.817 ± 13.295  ops/us
DeleteMinThrptBench.ratio_6_2:readerEmpty      PADDED_EPO  thrpt   20      ≈ 0           ops/us
DeleteMinThrptBench.ratio_6_2:readerSucceeded  PADDED_EPO  thrpt   20    3.170 ±  0.164  ops/us
DeleteMinThrptBench.ratio_6_2:writerRejected   PADDED_EPO  thrpt   20  336.669 ± 13.339  ops/us
DeleteMinThrptBench.ratio_6_2:writerSucceeded  PADDED_EPO  thrpt   20    3.170 ±  0.163  ops/us
DeleteMinThrptBench.ratio_6_2                        LOCK  thrpt   20   14.214 ±  1.457  ops/us
DeleteMinThrptBench.ratio_6_2:deleteMin_6_2          LOCK  thrpt   20    2.290 ±  0.345  ops/us
DeleteMinThrptBench.ratio_6_2:insert_6_2             LOCK  thrpt   20   11.924 ±  1.629  ops/us
DeleteMinThrptBench.ratio_6_2:readerEmpty            LOCK  thrpt   20      ≈ 0           ops/us
DeleteMinThrptBench.ratio_6_2:readerSucceeded        LOCK  thrpt   20    2.290 ±  0.345  ops/us
DeleteMinThrptBench.ratio_6_2:writerRejected         LOCK  thrpt   20    9.635 ±  1.849  ops/us
DeleteMinThrptBench.ratio_6_2:writerSucceeded        LOCK  thrpt   20    2.290 ±  0.346  ops/us
* */