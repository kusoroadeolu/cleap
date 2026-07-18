package io.github.kusoroadeolu.cleap.jmh;

import io.github.kusoroadeolu.cleap.PriorityQueue;
import io.github.kusoroadeolu.cleap.experimental.LockedPQ;
import io.github.kusoroadeolu.cleap.latest.MpmcGenerationPQ;
import io.github.kusoroadeolu.cleap.latest.PaddedArenaGenerationPQ;
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
public class DeleteMinLatencyBench {

    private PriorityQueue<Integer> queue;

    @Param({"MPMC_EPO", "PADDED_EPO"})
    private String type;

    @Setup
    public void setup() {
        int cap = 65536;
        queue = switch (type) {
            case "LOCK" -> new LockedPQ<>(cap);
            case "PADDED_EPO" -> new PaddedArenaGenerationPQ<>(cap);
            case "MPMC_EPO" -> new MpmcGenerationPQ<>(cap);
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
                    .addProfiler(JavaFlightRecorderProfiler.class, "dir=C:\\jfr-hp")
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
*
DeleteMinLatencyBench.ratio_6_2:insert_6_2                   LOCK  sample  3967336       1.676 ± 0.030  us/op
DeleteMinLatencyBench.ratio_6_2:insert_6_2:p0.00             LOCK  sample                  ≈ 0          us/op
DeleteMinLatencyBench.ratio_6_2:insert_6_2:p0.50             LOCK  sample                0.100          us/op
DeleteMinLatencyBench.ratio_6_2:insert_6_2:p0.90             LOCK  sample                0.100          us/op
DeleteMinLatencyBench.ratio_6_2:insert_6_2:p0.95             LOCK  sample                0.200          us/op
DeleteMinLatencyBench.ratio_6_2:insert_6_2:p0.99             LOCK  sample               79.616          us/op
DeleteMinLatencyBench.ratio_6_2:insert_6_2:p0.999            LOCK  sample              125.696          us/op
DeleteMinLatencyBench.ratio_6_2:insert_6_2:p0.9999           LOCK  sample              222.720          us/op
DeleteMinLatencyBench.ratio_6_2:insert_6_2:p1.00             LOCK  sample            10780.672          us/op
*
DeleteMinLatencyBench.ratio_6_2:p0.00                        LOCK  sample                  ≈ 0          us/op
DeleteMinLatencyBench.ratio_6_2:p0.50                        LOCK  sample                0.100          us/op
DeleteMinLatencyBench.ratio_6_2:p0.90                        LOCK  sample                0.300          us/op
DeleteMinLatencyBench.ratio_6_2:p0.95                        LOCK  sample                0.300          us/op
DeleteMinLatencyBench.ratio_6_2:p0.99                        LOCK  sample               83.456          us/op
DeleteMinLatencyBench.ratio_6_2:p0.999                       LOCK  sample              130.176          us/op
DeleteMinLatencyBench.ratio_6_2:p0.9999                      LOCK  sample              235.776          us/op
DeleteMinLatencyBench.ratio_6_2:p1.00                        LOCK  sample            48496.640          us/op
*/


/*
* Benchmark                                                  (type)    Mode      Cnt      Score   Error  Units
DeleteMinLatencyBench.ratio_4_4                          MPMC_EPO  sample  4805020      1.099 ± 0.048  us/op
DeleteMinLatencyBench.ratio_4_4:deleteMin_4_4            MPMC_EPO  sample  2031101      2.502 ± 0.111  us/op
DeleteMinLatencyBench.ratio_4_4:deleteMin_4_4:p0.00      MPMC_EPO  sample                 ≈ 0          us/op
DeleteMinLatencyBench.ratio_4_4:deleteMin_4_4:p0.50      MPMC_EPO  sample               0.300          us/op
DeleteMinLatencyBench.ratio_4_4:deleteMin_4_4:p0.90      MPMC_EPO  sample               0.900          us/op
DeleteMinLatencyBench.ratio_4_4:deleteMin_4_4:p0.95      MPMC_EPO  sample               1.300          us/op
DeleteMinLatencyBench.ratio_4_4:deleteMin_4_4:p0.99      MPMC_EPO  sample               5.200          us/op
DeleteMinLatencyBench.ratio_4_4:deleteMin_4_4:p0.999     MPMC_EPO  sample             238.848          us/op
DeleteMinLatencyBench.ratio_4_4:deleteMin_4_4:p0.9999    MPMC_EPO  sample             759.469          us/op
DeleteMinLatencyBench.ratio_4_4:deleteMin_4_4:p1.00      MPMC_EPO  sample           20250.624          us/op
DeleteMinLatencyBench.ratio_4_4:insert_4_4               MPMC_EPO  sample  2773919      0.071 ± 0.019  us/op
DeleteMinLatencyBench.ratio_4_4:insert_4_4:p0.00         MPMC_EPO  sample                 ≈ 0          us/op
DeleteMinLatencyBench.ratio_4_4:insert_4_4:p0.50         MPMC_EPO  sample                 ≈ 0          us/op
DeleteMinLatencyBench.ratio_4_4:insert_4_4:p0.90         MPMC_EPO  sample               0.100          us/op
DeleteMinLatencyBench.ratio_4_4:insert_4_4:p0.95         MPMC_EPO  sample               0.100          us/op
DeleteMinLatencyBench.ratio_4_4:insert_4_4:p0.99         MPMC_EPO  sample               0.400          us/op
DeleteMinLatencyBench.ratio_4_4:insert_4_4:p0.999        MPMC_EPO  sample               1.800          us/op
DeleteMinLatencyBench.ratio_4_4:insert_4_4:p0.9999       MPMC_EPO  sample              14.192          us/op
DeleteMinLatencyBench.ratio_4_4:insert_4_4:p1.00         MPMC_EPO  sample           10469.376          us/op
DeleteMinLatencyBench.ratio_4_4:p0.00                    MPMC_EPO  sample                 ≈ 0          us/op
DeleteMinLatencyBench.ratio_4_4:p0.50                    MPMC_EPO  sample               0.100          us/op
DeleteMinLatencyBench.ratio_4_4:p0.90                    MPMC_EPO  sample               0.600          us/op
DeleteMinLatencyBench.ratio_4_4:p0.95                    MPMC_EPO  sample               0.900          us/op
DeleteMinLatencyBench.ratio_4_4:p0.99                    MPMC_EPO  sample               1.900          us/op
DeleteMinLatencyBench.ratio_4_4:p0.999                   MPMC_EPO  sample             227.840          us/op
DeleteMinLatencyBench.ratio_4_4:p0.9999                  MPMC_EPO  sample             275.968          us/op
DeleteMinLatencyBench.ratio_4_4:p1.00                    MPMC_EPO  sample           20250.624          us/op
DeleteMinLatencyBench.ratio_4_4                        PADDED_EPO  sample  5093995      0.888 ± 0.043  us/op
DeleteMinLatencyBench.ratio_4_4:deleteMin_4_4          PADDED_EPO  sample  2301297      1.886 ± 0.092  us/op
DeleteMinLatencyBench.ratio_4_4:deleteMin_4_4:p0.00    PADDED_EPO  sample                 ≈ 0          us/op
DeleteMinLatencyBench.ratio_4_4:deleteMin_4_4:p0.50    PADDED_EPO  sample               0.100          us/op
DeleteMinLatencyBench.ratio_4_4:deleteMin_4_4:p0.90    PADDED_EPO  sample               0.200          us/op
DeleteMinLatencyBench.ratio_4_4:deleteMin_4_4:p0.95    PADDED_EPO  sample               2.000          us/op
DeleteMinLatencyBench.ratio_4_4:deleteMin_4_4:p0.99    PADDED_EPO  sample               2.200          us/op
DeleteMinLatencyBench.ratio_4_4:deleteMin_4_4:p0.999   PADDED_EPO  sample             238.080          us/op
DeleteMinLatencyBench.ratio_4_4:deleteMin_4_4:p0.9999  PADDED_EPO  sample             342.907          us/op
DeleteMinLatencyBench.ratio_4_4:deleteMin_4_4:p1.00    PADDED_EPO  sample           19791.872          us/op
DeleteMinLatencyBench.ratio_4_4:insert_4_4             PADDED_EPO  sample  2792698      0.067 ± 0.017  us/op
DeleteMinLatencyBench.ratio_4_4:insert_4_4:p0.00       PADDED_EPO  sample                 ≈ 0          us/op
DeleteMinLatencyBench.ratio_4_4:insert_4_4:p0.50       PADDED_EPO  sample                 ≈ 0          us/op
DeleteMinLatencyBench.ratio_4_4:insert_4_4:p0.90       PADDED_EPO  sample               0.100          us/op
DeleteMinLatencyBench.ratio_4_4:insert_4_4:p0.95       PADDED_EPO  sample               0.100          us/op
DeleteMinLatencyBench.ratio_4_4:insert_4_4:p0.99       PADDED_EPO  sample               0.400          us/op
DeleteMinLatencyBench.ratio_4_4:insert_4_4:p0.999      PADDED_EPO  sample               1.600          us/op
DeleteMinLatencyBench.ratio_4_4:insert_4_4:p0.9999     PADDED_EPO  sample              14.192          us/op
DeleteMinLatencyBench.ratio_4_4:insert_4_4:p1.00       PADDED_EPO  sample           13565.952          us/op
DeleteMinLatencyBench.ratio_4_4:p0.00                  PADDED_EPO  sample                 ≈ 0          us/op
DeleteMinLatencyBench.ratio_4_4:p0.50                  PADDED_EPO  sample                 ≈ 0          us/op
DeleteMinLatencyBench.ratio_4_4:p0.90                  PADDED_EPO  sample               0.100          us/op
DeleteMinLatencyBench.ratio_4_4:p0.95                  PADDED_EPO  sample               0.200          us/op
DeleteMinLatencyBench.ratio_4_4:p0.99                  PADDED_EPO  sample               2.100          us/op
DeleteMinLatencyBench.ratio_4_4:p0.999                 PADDED_EPO  sample             226.304          us/op
DeleteMinLatencyBench.ratio_4_4:p0.9999                PADDED_EPO  sample             256.768          us/op
DeleteMinLatencyBench.ratio_4_4:p1.00                  PADDED_EPO  sample           19791.872          us/op
DeleteMinLatencyBench.ratio_6_2                          MPMC_EPO  sample  4543958      0.444 ± 0.025  us/op
DeleteMinLatencyBench.ratio_6_2:deleteMin_6_2            MPMC_EPO  sample  1026636      1.690 ± 0.079  us/op
DeleteMinLatencyBench.ratio_6_2:deleteMin_6_2:p0.00      MPMC_EPO  sample                 ≈ 0          us/op
DeleteMinLatencyBench.ratio_6_2:deleteMin_6_2:p0.50      MPMC_EPO  sample               0.100          us/op
DeleteMinLatencyBench.ratio_6_2:deleteMin_6_2:p0.90      MPMC_EPO  sample               0.400          us/op
DeleteMinLatencyBench.ratio_6_2:deleteMin_6_2:p0.95      MPMC_EPO  sample               0.700          us/op
DeleteMinLatencyBench.ratio_6_2:deleteMin_6_2:p0.99      MPMC_EPO  sample               1.600          us/op
DeleteMinLatencyBench.ratio_6_2:deleteMin_6_2:p0.999     MPMC_EPO  sample             252.672          us/op
DeleteMinLatencyBench.ratio_6_2:deleteMin_6_2:p0.9999    MPMC_EPO  sample             498.040          us/op
DeleteMinLatencyBench.ratio_6_2:deleteMin_6_2:p1.00      MPMC_EPO  sample            5996.544          us/op
DeleteMinLatencyBench.ratio_6_2:insert_6_2               MPMC_EPO  sample  3517322      0.080 ± 0.023  us/op
DeleteMinLatencyBench.ratio_6_2:insert_6_2:p0.00         MPMC_EPO  sample                 ≈ 0          us/op
DeleteMinLatencyBench.ratio_6_2:insert_6_2:p0.50         MPMC_EPO  sample                 ≈ 0          us/op
DeleteMinLatencyBench.ratio_6_2:insert_6_2:p0.90         MPMC_EPO  sample               0.100          us/op
DeleteMinLatencyBench.ratio_6_2:insert_6_2:p0.95         MPMC_EPO  sample               0.100          us/op
DeleteMinLatencyBench.ratio_6_2:insert_6_2:p0.99         MPMC_EPO  sample               0.500          us/op
DeleteMinLatencyBench.ratio_6_2:insert_6_2:p0.999        MPMC_EPO  sample               3.400          us/op
DeleteMinLatencyBench.ratio_6_2:insert_6_2:p0.9999       MPMC_EPO  sample              15.118          us/op
DeleteMinLatencyBench.ratio_6_2:insert_6_2:p1.00         MPMC_EPO  sample           19791.872          us/op
DeleteMinLatencyBench.ratio_6_2:p0.00                    MPMC_EPO  sample                 ≈ 0          us/op
DeleteMinLatencyBench.ratio_6_2:p0.50                    MPMC_EPO  sample               0.100          us/op
DeleteMinLatencyBench.ratio_6_2:p0.90                    MPMC_EPO  sample               0.100          us/op
DeleteMinLatencyBench.ratio_6_2:p0.95                    MPMC_EPO  sample               0.300          us/op
DeleteMinLatencyBench.ratio_6_2:p0.99                    MPMC_EPO  sample               1.000          us/op
DeleteMinLatencyBench.ratio_6_2:p0.999                   MPMC_EPO  sample             223.744          us/op
DeleteMinLatencyBench.ratio_6_2:p0.9999                  MPMC_EPO  sample             262.144          us/op
DeleteMinLatencyBench.ratio_6_2:p1.00                    MPMC_EPO  sample           19791.872          us/op
DeleteMinLatencyBench.ratio_6_2                        PADDED_EPO  sample  5168325      0.349 ± 0.017  us/op
DeleteMinLatencyBench.ratio_6_2:deleteMin_6_2          PADDED_EPO  sample  1146238      1.340 ± 0.064  us/op
DeleteMinLatencyBench.ratio_6_2:deleteMin_6_2:p0.00    PADDED_EPO  sample                 ≈ 0          us/op
DeleteMinLatencyBench.ratio_6_2:deleteMin_6_2:p0.50    PADDED_EPO  sample               0.100          us/op
DeleteMinLatencyBench.ratio_6_2:deleteMin_6_2:p0.90    PADDED_EPO  sample               0.200          us/op
DeleteMinLatencyBench.ratio_6_2:deleteMin_6_2:p0.95    PADDED_EPO  sample               0.200          us/op
DeleteMinLatencyBench.ratio_6_2:deleteMin_6_2:p0.99    PADDED_EPO  sample               2.100          us/op
DeleteMinLatencyBench.ratio_6_2:deleteMin_6_2:p0.999   PADDED_EPO  sample             246.784          us/op
DeleteMinLatencyBench.ratio_6_2:deleteMin_6_2:p0.9999  PADDED_EPO  sample             324.674          us/op
DeleteMinLatencyBench.ratio_6_2:deleteMin_6_2:p1.00    PADDED_EPO  sample            6258.688          us/op
DeleteMinLatencyBench.ratio_6_2:insert_6_2             PADDED_EPO  sample  4022087      0.066 ± 0.012  us/op
DeleteMinLatencyBench.ratio_6_2:insert_6_2:p0.00       PADDED_EPO  sample                 ≈ 0          us/op
DeleteMinLatencyBench.ratio_6_2:insert_6_2:p0.50       PADDED_EPO  sample                 ≈ 0          us/op
DeleteMinLatencyBench.ratio_6_2:insert_6_2:p0.90       PADDED_EPO  sample               0.100          us/op
DeleteMinLatencyBench.ratio_6_2:insert_6_2:p0.95       PADDED_EPO  sample               0.100          us/op
DeleteMinLatencyBench.ratio_6_2:insert_6_2:p0.99       PADDED_EPO  sample               0.400          us/op
DeleteMinLatencyBench.ratio_6_2:insert_6_2:p0.999      PADDED_EPO  sample               3.800          us/op
DeleteMinLatencyBench.ratio_6_2:insert_6_2:p0.9999     PADDED_EPO  sample              14.192          us/op
DeleteMinLatencyBench.ratio_6_2:insert_6_2:p1.00       PADDED_EPO  sample           13139.968          us/op
DeleteMinLatencyBench.ratio_6_2:p0.00                  PADDED_EPO  sample                 ≈ 0          us/op
DeleteMinLatencyBench.ratio_6_2:p0.50                  PADDED_EPO  sample                 ≈ 0          us/op
DeleteMinLatencyBench.ratio_6_2:p0.90                  PADDED_EPO  sample               0.100          us/op
DeleteMinLatencyBench.ratio_6_2:p0.95                  PADDED_EPO  sample               0.100          us/op
DeleteMinLatencyBench.ratio_6_2:p0.99                  PADDED_EPO  sample               1.400          us/op
DeleteMinLatencyBench.ratio_6_2:p0.999                 PADDED_EPO  sample             174.848          us/op
DeleteMinLatencyBench.ratio_6_2:p0.9999                PADDED_EPO  sample             255.531          us/op
DeleteMinLatencyBench.ratio_6_2:p1.00                  PADDED_EPO  sample           13139.968          us/op
* */

}