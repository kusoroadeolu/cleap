package io.github.kusoroadeolu.cleap.jmh;

import io.github.kusoroadeolu.cleap.PriorityQueue;
import io.github.kusoroadeolu.cleap.experimental.LockedPQ;
import io.github.kusoroadeolu.cleap.latest.MpmcEpochPQ;
import io.github.kusoroadeolu.cleap.latest.PaddedArenaEpochPQ;
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
public class DeleteMinLatencyBench {

    private PriorityQueue<Integer> queue;

    @Param({"LOCK", "MPMC_EPO", "EPO", "PADDED_EPO"})
    private String type;

    @Setup
    public void setup() {
        int cap = 65536;
        queue = switch (type) {
            case "LOCK" -> new LockedPQ<>(cap);
            case "PADDED_EPO" -> new PaddedArenaEpochPQ<>(cap);
            case "MPMC_EPO" -> new MpmcEpochPQ<>(cap);
            default -> throw new RuntimeException();
        };

        int to = cap / 2;
        for (int i = 0; i < to; ++i) {
            queue.add(ThreadLocalRandom.current().nextInt(1_000_000));
        }
    }

    @Group("ratio_6_2")
    @GroupThreads(6)
    @Benchmark
    public void insert_6_2(Blackhole bh) {
        bh.consume(queue.add(ThreadLocalRandom.current().nextInt(1_000_000)));
    }

    @Group("ratio_6_2")
    @GroupThreads(2)
    @Benchmark
    public void deleteMin_6_2(Blackhole bh) {
        bh.consume(queue.poll());
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
                    .include(DeleteMinLatencyBench.class.getSimpleName())
                    .build();
            new org.openjdk.jmh.runner.Runner(options).run();
        }
    }

    /*
    * Benchmark                                                  (type)    Mode      Cnt       Score   Error  Units
DeleteMinLatencyBench.ratio_4_4                              LOCK  sample  4683532       3.263 ± 0.161  us/op
DeleteMinLatencyBench.ratio_4_4:deleteMin_4_4                LOCK  sample  2421393       3.876 ± 0.286  us/op
DeleteMinLatencyBench.ratio_4_4:deleteMin_4_4:p0.00          LOCK  sample                  ≈ 0          us/op
DeleteMinLatencyBench.ratio_4_4:deleteMin_4_4:p0.50          LOCK  sample                0.200          us/op
DeleteMinLatencyBench.ratio_4_4:deleteMin_4_4:p0.90          LOCK  sample                0.500          us/op
DeleteMinLatencyBench.ratio_4_4:deleteMin_4_4:p0.95          LOCK  sample                1.000          us/op
DeleteMinLatencyBench.ratio_4_4:deleteMin_4_4:p0.99          LOCK  sample              105.984          us/op
DeleteMinLatencyBench.ratio_4_4:deleteMin_4_4:p0.999         LOCK  sample              164.864          us/op
DeleteMinLatencyBench.ratio_4_4:deleteMin_4_4:p0.9999        LOCK  sample              317.952          us/op
DeleteMinLatencyBench.ratio_4_4:deleteMin_4_4:p1.00          LOCK  sample           161742.848          us/op
*
DeleteMinLatencyBench.ratio_4_4:insert_4_4                   LOCK  sample  2262139       2.607 ± 0.129  us/op
DeleteMinLatencyBench.ratio_4_4:insert_4_4:p0.00             LOCK  sample                  ≈ 0          us/op
DeleteMinLatencyBench.ratio_4_4:insert_4_4:p0.50             LOCK  sample                0.100          us/op
DeleteMinLatencyBench.ratio_4_4:insert_4_4:p0.90             LOCK  sample                0.200          us/op
DeleteMinLatencyBench.ratio_4_4:insert_4_4:p0.95             LOCK  sample                0.200          us/op
DeleteMinLatencyBench.ratio_4_4:insert_4_4:p0.99             LOCK  sample               99.200          us/op
DeleteMinLatencyBench.ratio_4_4:insert_4_4:p0.999            LOCK  sample              156.672          us/op
DeleteMinLatencyBench.ratio_4_4:insert_4_4:p0.9999           LOCK  sample              270.738          us/op
DeleteMinLatencyBench.ratio_4_4:insert_4_4:p1.00             LOCK  sample            59768.832          us/op
*
DeleteMinLatencyBench.ratio_4_4:p0.00                        LOCK  sample                  ≈ 0          us/op
DeleteMinLatencyBench.ratio_4_4:p0.50                        LOCK  sample                0.200          us/op
DeleteMinLatencyBench.ratio_4_4:p0.90                        LOCK  sample                0.300          us/op
DeleteMinLatencyBench.ratio_4_4:p0.95                        LOCK  sample                0.900          us/op
DeleteMinLatencyBench.ratio_4_4:p0.99                        LOCK  sample              103.168          us/op
DeleteMinLatencyBench.ratio_4_4:p0.999                       LOCK  sample              161.144          us/op
DeleteMinLatencyBench.ratio_4_4:p0.9999                      LOCK  sample              289.792          us/op
DeleteMinLatencyBench.ratio_4_4:p1.00                        LOCK  sample           161742.848          us/op
*
DeleteMinLatencyBench.ratio_4_4                          MPMC_EPO  sample  4473956       1.506 ± 0.098  us/op
DeleteMinLatencyBench.ratio_4_4:deleteMin_4_4            MPMC_EPO  sample  2177329       2.948 ± 0.197  us/op
DeleteMinLatencyBench.ratio_4_4:deleteMin_4_4:p0.00      MPMC_EPO  sample                  ≈ 0          us/op
DeleteMinLatencyBench.ratio_4_4:deleteMin_4_4:p0.50      MPMC_EPO  sample                0.200          us/op
DeleteMinLatencyBench.ratio_4_4:deleteMin_4_4:p0.90      MPMC_EPO  sample                0.800          us/op
DeleteMinLatencyBench.ratio_4_4:deleteMin_4_4:p0.95      MPMC_EPO  sample                1.200          us/op
DeleteMinLatencyBench.ratio_4_4:deleteMin_4_4:p0.99      MPMC_EPO  sample               12.992          us/op
DeleteMinLatencyBench.ratio_4_4:deleteMin_4_4:p0.999     MPMC_EPO  sample              252.416          us/op
DeleteMinLatencyBench.ratio_4_4:deleteMin_4_4:p0.9999    MPMC_EPO  sample             2397.254          us/op
DeleteMinLatencyBench.ratio_4_4:deleteMin_4_4:p1.00      MPMC_EPO  sample            46596.096          us/op
*
DeleteMinLatencyBench.ratio_4_4:insert_4_4               MPMC_EPO  sample  2296627       0.138 ± 0.033  us/op
DeleteMinLatencyBench.ratio_4_4:insert_4_4:p0.00         MPMC_EPO  sample                  ≈ 0          us/op
DeleteMinLatencyBench.ratio_4_4:insert_4_4:p0.50         MPMC_EPO  sample                  ≈ 0          us/op
DeleteMinLatencyBench.ratio_4_4:insert_4_4:p0.90         MPMC_EPO  sample                0.100          us/op
DeleteMinLatencyBench.ratio_4_4:insert_4_4:p0.95         MPMC_EPO  sample                0.100          us/op
DeleteMinLatencyBench.ratio_4_4:insert_4_4:p0.99         MPMC_EPO  sample                0.400          us/op
DeleteMinLatencyBench.ratio_4_4:insert_4_4:p0.999        MPMC_EPO  sample                3.600          us/op
DeleteMinLatencyBench.ratio_4_4:insert_4_4:p0.9999       MPMC_EPO  sample              108.760          us/op
DeleteMinLatencyBench.ratio_4_4:insert_4_4:p1.00         MPMC_EPO  sample            10043.392          us/op
*
DeleteMinLatencyBench.ratio_4_4:p0.00                    MPMC_EPO  sample                  ≈ 0          us/op
DeleteMinLatencyBench.ratio_4_4:p0.50                    MPMC_EPO  sample                0.100          us/op
DeleteMinLatencyBench.ratio_4_4:p0.90                    MPMC_EPO  sample                0.500          us/op
DeleteMinLatencyBench.ratio_4_4:p0.95                    MPMC_EPO  sample                0.800          us/op
DeleteMinLatencyBench.ratio_4_4:p0.99                    MPMC_EPO  sample                2.200          us/op
DeleteMinLatencyBench.ratio_4_4:p0.999                   MPMC_EPO  sample              206.080          us/op
DeleteMinLatencyBench.ratio_4_4:p0.9999                  MPMC_EPO  sample             1214.891          us/op
DeleteMinLatencyBench.ratio_4_4:p1.00                    MPMC_EPO  sample            46596.096          us/op
*
DeleteMinLatencyBench.ratio_4_4                        PADDED_EPO  sample  5089634       1.410 ± 0.088  us/op
DeleteMinLatencyBench.ratio_4_4:deleteMin_4_4          PADDED_EPO  sample  2393112       2.856 ± 0.184  us/op
DeleteMinLatencyBench.ratio_4_4:deleteMin_4_4:p0.00    PADDED_EPO  sample                  ≈ 0          us/op
DeleteMinLatencyBench.ratio_4_4:deleteMin_4_4:p0.50    PADDED_EPO  sample                0.100          us/op
DeleteMinLatencyBench.ratio_4_4:deleteMin_4_4:p0.90    PADDED_EPO  sample                0.200          us/op
DeleteMinLatencyBench.ratio_4_4:deleteMin_4_4:p0.95    PADDED_EPO  sample                2.000          us/op
DeleteMinLatencyBench.ratio_4_4:deleteMin_4_4:p0.99    PADDED_EPO  sample                6.600          us/op
DeleteMinLatencyBench.ratio_4_4:deleteMin_4_4:p0.999   PADDED_EPO  sample              319.488          us/op
DeleteMinLatencyBench.ratio_4_4:deleteMin_4_4:p0.9999  PADDED_EPO  sample             2271.001          us/op
DeleteMinLatencyBench.ratio_4_4:deleteMin_4_4:p1.00    PADDED_EPO  sample            38207.488          us/op
*
DeleteMinLatencyBench.ratio_4_4:insert_4_4             PADDED_EPO  sample  2696522       0.127 ± 0.031  us/op
DeleteMinLatencyBench.ratio_4_4:insert_4_4:p0.00       PADDED_EPO  sample                  ≈ 0          us/op
DeleteMinLatencyBench.ratio_4_4:insert_4_4:p0.50       PADDED_EPO  sample                  ≈ 0          us/op
DeleteMinLatencyBench.ratio_4_4:insert_4_4:p0.90       PADDED_EPO  sample                0.100          us/op
DeleteMinLatencyBench.ratio_4_4:insert_4_4:p0.95       PADDED_EPO  sample                0.100          us/op
DeleteMinLatencyBench.ratio_4_4:insert_4_4:p0.99       PADDED_EPO  sample                0.300          us/op
DeleteMinLatencyBench.ratio_4_4:insert_4_4:p0.999      PADDED_EPO  sample                2.500          us/op
DeleteMinLatencyBench.ratio_4_4:insert_4_4:p0.9999     PADDED_EPO  sample               86.957          us/op
DeleteMinLatencyBench.ratio_4_4:insert_4_4:p1.00       PADDED_EPO  sample            17137.664          us/op
*
DeleteMinLatencyBench.ratio_4_4:p0.00                  PADDED_EPO  sample                  ≈ 0          us/op
DeleteMinLatencyBench.ratio_4_4:p0.50                  PADDED_EPO  sample                  ≈ 0          us/op
DeleteMinLatencyBench.ratio_4_4:p0.90                  PADDED_EPO  sample                0.100          us/op
DeleteMinLatencyBench.ratio_4_4:p0.95                  PADDED_EPO  sample                0.200          us/op
DeleteMinLatencyBench.ratio_4_4:p0.99                  PADDED_EPO  sample                2.100          us/op
DeleteMinLatencyBench.ratio_4_4:p0.999                 PADDED_EPO  sample              201.984          us/op
DeleteMinLatencyBench.ratio_4_4:p0.9999                PADDED_EPO  sample             1624.064          us/op
DeleteMinLatencyBench.ratio_4_4:p1.00                  PADDED_EPO  sample            38207.488          us/op
*
*
* ------------------------------------------------------------------------------------------------------------
*
DeleteMinLatencyBench.ratio_6_2                              LOCK  sample  5127447       2.052 ± 0.047  us/op
DeleteMinLatencyBench.ratio_6_2:deleteMin_6_2                LOCK  sample  1160111       3.341 ± 0.179  us/op
DeleteMinLatencyBench.ratio_6_2:deleteMin_6_2:p0.00          LOCK  sample                  ≈ 0          us/op
DeleteMinLatencyBench.ratio_6_2:deleteMin_6_2:p0.50          LOCK  sample                0.200          us/op
DeleteMinLatencyBench.ratio_6_2:deleteMin_6_2:p0.90          LOCK  sample                0.500          us/op
DeleteMinLatencyBench.ratio_6_2:deleteMin_6_2:p0.95          LOCK  sample                1.000          us/op
DeleteMinLatencyBench.ratio_6_2:deleteMin_6_2:p0.99          LOCK  sample               91.392          us/op
DeleteMinLatencyBench.ratio_6_2:deleteMin_6_2:p0.999         LOCK  sample              143.616          us/op
DeleteMinLatencyBench.ratio_6_2:deleteMin_6_2:p0.9999        LOCK  sample              316.364          us/op
DeleteMinLatencyBench.ratio_6_2:deleteMin_6_2:p1.00          LOCK  sample            48496.640          us/op
DeleteMinLatencyBench.ratio_6_2:insert_6_2                   LOCK  sample  3967336       1.676 ± 0.030  us/op
DeleteMinLatencyBench.ratio_6_2:insert_6_2:p0.00             LOCK  sample                  ≈ 0          us/op
DeleteMinLatencyBench.ratio_6_2:insert_6_2:p0.50             LOCK  sample                0.100          us/op
DeleteMinLatencyBench.ratio_6_2:insert_6_2:p0.90             LOCK  sample                0.100          us/op
DeleteMinLatencyBench.ratio_6_2:insert_6_2:p0.95             LOCK  sample                0.200          us/op
DeleteMinLatencyBench.ratio_6_2:insert_6_2:p0.99             LOCK  sample               79.616          us/op
DeleteMinLatencyBench.ratio_6_2:insert_6_2:p0.999            LOCK  sample              125.696          us/op
DeleteMinLatencyBench.ratio_6_2:insert_6_2:p0.9999           LOCK  sample              222.720          us/op
DeleteMinLatencyBench.ratio_6_2:insert_6_2:p1.00             LOCK  sample            10780.672          us/op
DeleteMinLatencyBench.ratio_6_2:p0.00                        LOCK  sample                  ≈ 0          us/op
DeleteMinLatencyBench.ratio_6_2:p0.50                        LOCK  sample                0.100          us/op
DeleteMinLatencyBench.ratio_6_2:p0.90                        LOCK  sample                0.300          us/op
DeleteMinLatencyBench.ratio_6_2:p0.95                        LOCK  sample                0.300          us/op
DeleteMinLatencyBench.ratio_6_2:p0.99                        LOCK  sample               83.456          us/op
DeleteMinLatencyBench.ratio_6_2:p0.999                       LOCK  sample              130.176          us/op
DeleteMinLatencyBench.ratio_6_2:p0.9999                      LOCK  sample              235.776          us/op
DeleteMinLatencyBench.ratio_6_2:p1.00                        LOCK  sample            48496.640          us/op
DeleteMinLatencyBench.ratio_6_2                          MPMC_EPO  sample  4112243       0.530 ± 0.030  us/op
DeleteMinLatencyBench.ratio_6_2:deleteMin_6_2            MPMC_EPO  sample  1141299       1.556 ± 0.097  us/op
DeleteMinLatencyBench.ratio_6_2:deleteMin_6_2:p0.00      MPMC_EPO  sample                  ≈ 0          us/op
DeleteMinLatencyBench.ratio_6_2:deleteMin_6_2:p0.50      MPMC_EPO  sample                0.100          us/op
DeleteMinLatencyBench.ratio_6_2:deleteMin_6_2:p0.90      MPMC_EPO  sample                0.300          us/op
DeleteMinLatencyBench.ratio_6_2:deleteMin_6_2:p0.95      MPMC_EPO  sample                0.600          us/op
DeleteMinLatencyBench.ratio_6_2:deleteMin_6_2:p0.99      MPMC_EPO  sample                1.700          us/op
DeleteMinLatencyBench.ratio_6_2:deleteMin_6_2:p0.999     MPMC_EPO  sample              206.080          us/op
DeleteMinLatencyBench.ratio_6_2:deleteMin_6_2:p0.9999    MPMC_EPO  sample             1567.437          us/op
DeleteMinLatencyBench.ratio_6_2:deleteMin_6_2:p1.00      MPMC_EPO  sample            10059.776          us/op
DeleteMinLatencyBench.ratio_6_2:insert_6_2               MPMC_EPO  sample  2970944       0.136 ± 0.018  us/op
DeleteMinLatencyBench.ratio_6_2:insert_6_2:p0.00         MPMC_EPO  sample                  ≈ 0          us/op
DeleteMinLatencyBench.ratio_6_2:insert_6_2:p0.50         MPMC_EPO  sample                  ≈ 0          us/op
DeleteMinLatencyBench.ratio_6_2:insert_6_2:p0.90         MPMC_EPO  sample                0.100          us/op
DeleteMinLatencyBench.ratio_6_2:insert_6_2:p0.95         MPMC_EPO  sample                0.100          us/op
DeleteMinLatencyBench.ratio_6_2:insert_6_2:p0.99         MPMC_EPO  sample                0.700          us/op
DeleteMinLatencyBench.ratio_6_2:insert_6_2:p0.999        MPMC_EPO  sample                4.496          us/op
DeleteMinLatencyBench.ratio_6_2:insert_6_2:p0.9999       MPMC_EPO  sample               73.960          us/op
DeleteMinLatencyBench.ratio_6_2:insert_6_2:p1.00         MPMC_EPO  sample             2740.224          us/op
DeleteMinLatencyBench.ratio_6_2:p0.00                    MPMC_EPO  sample                  ≈ 0          us/op
DeleteMinLatencyBench.ratio_6_2:p0.50                    MPMC_EPO  sample                0.100          us/op
DeleteMinLatencyBench.ratio_6_2:p0.90                    MPMC_EPO  sample                0.100          us/op
DeleteMinLatencyBench.ratio_6_2:p0.95                    MPMC_EPO  sample                0.300          us/op
DeleteMinLatencyBench.ratio_6_2:p0.99                    MPMC_EPO  sample                1.000          us/op
DeleteMinLatencyBench.ratio_6_2:p0.999                   MPMC_EPO  sample              182.784          us/op
DeleteMinLatencyBench.ratio_6_2:p0.9999                  MPMC_EPO  sample              494.989          us/op
DeleteMinLatencyBench.ratio_6_2:p1.00                    MPMC_EPO  sample            10059.776          us/op
DeleteMinLatencyBench.ratio_6_2                        PADDED_EPO  sample  4600737       0.425 ± 0.024  us/op
DeleteMinLatencyBench.ratio_6_2:deleteMin_6_2          PADDED_EPO  sample  1233080       1.279 ± 0.081  us/op
DeleteMinLatencyBench.ratio_6_2:deleteMin_6_2:p0.00    PADDED_EPO  sample                  ≈ 0          us/op
DeleteMinLatencyBench.ratio_6_2:deleteMin_6_2:p0.50    PADDED_EPO  sample                0.100          us/op
DeleteMinLatencyBench.ratio_6_2:deleteMin_6_2:p0.90    PADDED_EPO  sample                0.200          us/op
DeleteMinLatencyBench.ratio_6_2:deleteMin_6_2:p0.95    PADDED_EPO  sample                0.200          us/op
DeleteMinLatencyBench.ratio_6_2:deleteMin_6_2:p0.99    PADDED_EPO  sample                2.100          us/op
DeleteMinLatencyBench.ratio_6_2:deleteMin_6_2:p0.999   PADDED_EPO  sample              196.352          us/op
DeleteMinLatencyBench.ratio_6_2:deleteMin_6_2:p0.9999  PADDED_EPO  sample             1135.378          us/op
DeleteMinLatencyBench.ratio_6_2:deleteMin_6_2:p1.00    PADDED_EPO  sample             9158.656          us/op
DeleteMinLatencyBench.ratio_6_2:insert_6_2             PADDED_EPO  sample  3367657       0.113 ± 0.015  us/op
DeleteMinLatencyBench.ratio_6_2:insert_6_2:p0.00       PADDED_EPO  sample                  ≈ 0          us/op
DeleteMinLatencyBench.ratio_6_2:insert_6_2:p0.50       PADDED_EPO  sample                  ≈ 0          us/op
DeleteMinLatencyBench.ratio_6_2:insert_6_2:p0.90       PADDED_EPO  sample                0.100          us/op
DeleteMinLatencyBench.ratio_6_2:insert_6_2:p0.95       PADDED_EPO  sample                0.100          us/op
DeleteMinLatencyBench.ratio_6_2:insert_6_2:p0.99       PADDED_EPO  sample                0.500          us/op
DeleteMinLatencyBench.ratio_6_2:insert_6_2:p0.999      PADDED_EPO  sample                5.000          us/op
DeleteMinLatencyBench.ratio_6_2:insert_6_2:p0.9999     PADDED_EPO  sample               52.190          us/op
DeleteMinLatencyBench.ratio_6_2:insert_6_2:p1.00       PADDED_EPO  sample             4079.616          us/op
DeleteMinLatencyBench.ratio_6_2:p0.00                  PADDED_EPO  sample                  ≈ 0          us/op
DeleteMinLatencyBench.ratio_6_2:p0.50                  PADDED_EPO  sample                  ≈ 0          us/op
DeleteMinLatencyBench.ratio_6_2:p0.90                  PADDED_EPO  sample                0.100          us/op
DeleteMinLatencyBench.ratio_6_2:p0.95                  PADDED_EPO  sample                0.100          us/op
DeleteMinLatencyBench.ratio_6_2:p0.99                  PADDED_EPO  sample                1.900          us/op
DeleteMinLatencyBench.ratio_6_2:p0.999                 PADDED_EPO  sample              170.496          us/op
DeleteMinLatencyBench.ratio_6_2:p0.9999                PADDED_EPO  sample              353.242          us/op
DeleteMinLatencyBench.ratio_6_2:p1.00                  PADDED_EPO  sample             9158.656          us/op
    *
    * */
}