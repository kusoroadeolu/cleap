package io.github.kusoroadeolu.cleap.jmh;

import io.github.kusoroadeolu.cleap.PriorityQueue;
import io.github.kusoroadeolu.cleap.dualarray.LBBoundedPQ;
import io.github.kusoroadeolu.cleap.dualarray.OrderedBoundedPQ;
import io.github.kusoroadeolu.cleap.experimental.LockedPQ;
import io.github.kusoroadeolu.cleap.latest.MpmcGenerationPQ;
import io.github.kusoroadeolu.cleap.latest.PaddedArenaGenerationPQ;
import io.github.kusoroadeolu.jmhpretty.JmhPrettyPrinter;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.nio.file.Path;
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

    @Param({"LBPQ", "LOCK", "OBQ"})
    private String type;

    @Setup
    public void setup() {
        int cap = 65536;
        queue = switch (type) {
            case "LBPQ" -> new LBBoundedPQ<>(cap);
            case "OBQ" -> new OrderedBoundedPQ<>(cap);
            case "MPMC-GEN" -> new MpmcGenerationPQ<>(cap);
            case "PADDED-GEN" -> new PaddedArenaGenerationPQ<>(cap);
            case "LOCK" -> new LockedPQ<>(cap);
            default -> throw new RuntimeException();
        };

        int to = cap / 2;
        for (int i = 0; i < to; ++i) {
            queue.add(ThreadLocalRandom.current().nextInt(1_000_000));
        }
    }
//
//    @Group("ratio_6_2")
//    @GroupThreads(6)
//    @Benchmark
//    public void insert_6_2(Blackhole bh) {
//        bh.consume(queue.add(ThreadLocalRandom.current().nextInt(1_000_000)));
//    }
//
//    @Group("ratio_6_2")
//    @GroupThreads(2)
//    @Benchmark
//    public void deleteMin_6_2(Blackhole bh) {
//        bh.consume(queue.poll());
//    }

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
                    .result("results.json")
                    .resultFormat(ResultFormatType.JSON)
                    .build();
            new org.openjdk.jmh.runner.Runner(options).run();
            JmhPrettyPrinter.builder().build().print(Path.of(Path.of(".").toAbsolutePath().toString(), "target", "results.json").toString());

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
DeleteMinLatencyBench.ratio_4_4                          MPMC-GEN  sample  4805020      1.099 ± 0.048  us/op
DeleteMinLatencyBench.ratio_4_4:deleteMin_4_4            MPMC-GEN  sample  2031101      2.502 ± 0.111  us/op
DeleteMinLatencyBench.ratio_4_4:deleteMin_4_4:p0.00      MPMC-GEN  sample                 ≈ 0          us/op
DeleteMinLatencyBench.ratio_4_4:deleteMin_4_4:p0.50      MPMC-GEN  sample               0.300          us/op
DeleteMinLatencyBench.ratio_4_4:deleteMin_4_4:p0.90      MPMC-GEN  sample               0.900          us/op
DeleteMinLatencyBench.ratio_4_4:deleteMin_4_4:p0.95      MPMC-GEN  sample               1.300          us/op
DeleteMinLatencyBench.ratio_4_4:deleteMin_4_4:p0.99      MPMC-GEN  sample               5.200          us/op
DeleteMinLatencyBench.ratio_4_4:deleteMin_4_4:p0.999     MPMC-GEN  sample             238.848          us/op
DeleteMinLatencyBench.ratio_4_4:deleteMin_4_4:p0.9999    MPMC-GEN  sample             759.469          us/op
DeleteMinLatencyBench.ratio_4_4:deleteMin_4_4:p1.00      MPMC-GEN  sample           20250.624          us/op
DeleteMinLatencyBench.ratio_4_4:insert_4_4               MPMC-GEN  sample  2773919      0.071 ± 0.019  us/op
DeleteMinLatencyBench.ratio_4_4:insert_4_4:p0.00         MPMC-GEN  sample                 ≈ 0          us/op
DeleteMinLatencyBench.ratio_4_4:insert_4_4:p0.50         MPMC-GEN  sample                 ≈ 0          us/op
DeleteMinLatencyBench.ratio_4_4:insert_4_4:p0.90         MPMC-GEN  sample               0.100          us/op
DeleteMinLatencyBench.ratio_4_4:insert_4_4:p0.95         MPMC-GEN  sample               0.100          us/op
DeleteMinLatencyBench.ratio_4_4:insert_4_4:p0.99         MPMC-GEN  sample               0.400          us/op
DeleteMinLatencyBench.ratio_4_4:insert_4_4:p0.999        MPMC-GEN  sample               1.800          us/op
DeleteMinLatencyBench.ratio_4_4:insert_4_4:p0.9999       MPMC-GEN  sample              14.192          us/op
DeleteMinLatencyBench.ratio_4_4:insert_4_4:p1.00         MPMC-GEN  sample           10469.376          us/op
DeleteMinLatencyBench.ratio_4_4:p0.00                    MPMC-GEN  sample                 ≈ 0          us/op
DeleteMinLatencyBench.ratio_4_4:p0.50                    MPMC-GEN  sample               0.100          us/op
DeleteMinLatencyBench.ratio_4_4:p0.90                    MPMC-GEN  sample               0.600          us/op
DeleteMinLatencyBench.ratio_4_4:p0.95                    MPMC-GEN  sample               0.900          us/op
DeleteMinLatencyBench.ratio_4_4:p0.99                    MPMC-GEN  sample               1.900          us/op
DeleteMinLatencyBench.ratio_4_4:p0.999                   MPMC-GEN  sample             227.840          us/op
DeleteMinLatencyBench.ratio_4_4:p0.9999                  MPMC-GEN  sample             275.968          us/op
DeleteMinLatencyBench.ratio_4_4:p1.00                    MPMC-GEN  sample           20250.624          us/op
DeleteMinLatencyBench.ratio_4_4                        PADDED-GEN  sample  5093995      0.888 ± 0.043  us/op
DeleteMinLatencyBench.ratio_4_4:deleteMin_4_4          PADDED-GEN  sample  2301297      1.886 ± 0.092  us/op
DeleteMinLatencyBench.ratio_4_4:deleteMin_4_4:p0.00    PADDED-GEN  sample                 ≈ 0          us/op
DeleteMinLatencyBench.ratio_4_4:deleteMin_4_4:p0.50    PADDED-GEN  sample               0.100          us/op
DeleteMinLatencyBench.ratio_4_4:deleteMin_4_4:p0.90    PADDED-GEN  sample               0.200          us/op
DeleteMinLatencyBench.ratio_4_4:deleteMin_4_4:p0.95    PADDED-GEN  sample               2.000          us/op
DeleteMinLatencyBench.ratio_4_4:deleteMin_4_4:p0.99    PADDED-GEN  sample               2.200          us/op
DeleteMinLatencyBench.ratio_4_4:deleteMin_4_4:p0.999   PADDED-GEN  sample             238.080          us/op
DeleteMinLatencyBench.ratio_4_4:deleteMin_4_4:p0.9999  PADDED-GEN  sample             342.907          us/op
DeleteMinLatencyBench.ratio_4_4:deleteMin_4_4:p1.00    PADDED-GEN  sample           19791.872          us/op
DeleteMinLatencyBench.ratio_4_4:insert_4_4             PADDED-GEN  sample  2792698      0.067 ± 0.017  us/op
DeleteMinLatencyBench.ratio_4_4:insert_4_4:p0.00       PADDED-GEN  sample                 ≈ 0          us/op
DeleteMinLatencyBench.ratio_4_4:insert_4_4:p0.50       PADDED-GEN  sample                 ≈ 0          us/op
DeleteMinLatencyBench.ratio_4_4:insert_4_4:p0.90       PADDED-GEN  sample               0.100          us/op
DeleteMinLatencyBench.ratio_4_4:insert_4_4:p0.95       PADDED-GEN  sample               0.100          us/op
DeleteMinLatencyBench.ratio_4_4:insert_4_4:p0.99       PADDED-GEN  sample               0.400          us/op
DeleteMinLatencyBench.ratio_4_4:insert_4_4:p0.999      PADDED-GEN  sample               1.600          us/op
DeleteMinLatencyBench.ratio_4_4:insert_4_4:p0.9999     PADDED-GEN  sample              14.192          us/op
DeleteMinLatencyBench.ratio_4_4:insert_4_4:p1.00       PADDED-GEN  sample           13565.952          us/op
DeleteMinLatencyBench.ratio_4_4:p0.00                  PADDED-GEN  sample                 ≈ 0          us/op
DeleteMinLatencyBench.ratio_4_4:p0.50                  PADDED-GEN  sample                 ≈ 0          us/op
DeleteMinLatencyBench.ratio_4_4:p0.90                  PADDED-GEN  sample               0.100          us/op
DeleteMinLatencyBench.ratio_4_4:p0.95                  PADDED-GEN  sample               0.200          us/op
DeleteMinLatencyBench.ratio_4_4:p0.99                  PADDED-GEN  sample               2.100          us/op
DeleteMinLatencyBench.ratio_4_4:p0.999                 PADDED-GEN  sample             226.304          us/op
DeleteMinLatencyBench.ratio_4_4:p0.9999                PADDED-GEN  sample             256.768          us/op
DeleteMinLatencyBench.ratio_4_4:p1.00                  PADDED-GEN  sample           19791.872          us/op
DeleteMinLatencyBench.ratio_6_2                          MPMC-GEN  sample  4543958      0.444 ± 0.025  us/op
DeleteMinLatencyBench.ratio_6_2:deleteMin_6_2            MPMC-GEN  sample  1026636      1.690 ± 0.079  us/op
DeleteMinLatencyBench.ratio_6_2:deleteMin_6_2:p0.00      MPMC-GEN  sample                 ≈ 0          us/op
DeleteMinLatencyBench.ratio_6_2:deleteMin_6_2:p0.50      MPMC-GEN  sample               0.100          us/op
DeleteMinLatencyBench.ratio_6_2:deleteMin_6_2:p0.90      MPMC-GEN  sample               0.400          us/op
DeleteMinLatencyBench.ratio_6_2:deleteMin_6_2:p0.95      MPMC-GEN  sample               0.700          us/op
DeleteMinLatencyBench.ratio_6_2:deleteMin_6_2:p0.99      MPMC-GEN  sample               1.600          us/op
DeleteMinLatencyBench.ratio_6_2:deleteMin_6_2:p0.999     MPMC-GEN  sample             252.672          us/op
DeleteMinLatencyBench.ratio_6_2:deleteMin_6_2:p0.9999    MPMC-GEN  sample             498.040          us/op
DeleteMinLatencyBench.ratio_6_2:deleteMin_6_2:p1.00      MPMC-GEN  sample            5996.544          us/op
DeleteMinLatencyBench.ratio_6_2:insert_6_2               MPMC-GEN  sample  3517322      0.080 ± 0.023  us/op
DeleteMinLatencyBench.ratio_6_2:insert_6_2:p0.00         MPMC-GEN  sample                 ≈ 0          us/op
DeleteMinLatencyBench.ratio_6_2:insert_6_2:p0.50         MPMC-GEN  sample                 ≈ 0          us/op
DeleteMinLatencyBench.ratio_6_2:insert_6_2:p0.90         MPMC-GEN  sample               0.100          us/op
DeleteMinLatencyBench.ratio_6_2:insert_6_2:p0.95         MPMC-GEN  sample               0.100          us/op
DeleteMinLatencyBench.ratio_6_2:insert_6_2:p0.99         MPMC-GEN  sample               0.500          us/op
DeleteMinLatencyBench.ratio_6_2:insert_6_2:p0.999        MPMC-GEN  sample               3.400          us/op
DeleteMinLatencyBench.ratio_6_2:insert_6_2:p0.9999       MPMC-GEN  sample              15.118          us/op
DeleteMinLatencyBench.ratio_6_2:insert_6_2:p1.00         MPMC-GEN  sample           19791.872          us/op
DeleteMinLatencyBench.ratio_6_2:p0.00                    MPMC-GEN  sample                 ≈ 0          us/op
DeleteMinLatencyBench.ratio_6_2:p0.50                    MPMC-GEN  sample               0.100          us/op
DeleteMinLatencyBench.ratio_6_2:p0.90                    MPMC-GEN  sample               0.100          us/op
DeleteMinLatencyBench.ratio_6_2:p0.95                    MPMC-GEN  sample               0.300          us/op
DeleteMinLatencyBench.ratio_6_2:p0.99                    MPMC-GEN  sample               1.000          us/op
DeleteMinLatencyBench.ratio_6_2:p0.999                   MPMC-GEN  sample             223.744          us/op
DeleteMinLatencyBench.ratio_6_2:p0.9999                  MPMC-GEN  sample             262.144          us/op
DeleteMinLatencyBench.ratio_6_2:p1.00                    MPMC-GEN  sample           19791.872          us/op
DeleteMinLatencyBench.ratio_6_2                        PADDED-GEN  sample  5168325      0.349 ± 0.017  us/op
DeleteMinLatencyBench.ratio_6_2:deleteMin_6_2          PADDED-GEN  sample  1146238      1.340 ± 0.064  us/op
DeleteMinLatencyBench.ratio_6_2:deleteMin_6_2:p0.00    PADDED-GEN  sample                 ≈ 0          us/op
DeleteMinLatencyBench.ratio_6_2:deleteMin_6_2:p0.50    PADDED-GEN  sample               0.100          us/op
DeleteMinLatencyBench.ratio_6_2:deleteMin_6_2:p0.90    PADDED-GEN  sample               0.200          us/op
DeleteMinLatencyBench.ratio_6_2:deleteMin_6_2:p0.95    PADDED-GEN  sample               0.200          us/op
DeleteMinLatencyBench.ratio_6_2:deleteMin_6_2:p0.99    PADDED-GEN  sample               2.100          us/op
DeleteMinLatencyBench.ratio_6_2:deleteMin_6_2:p0.999   PADDED-GEN  sample             246.784          us/op
DeleteMinLatencyBench.ratio_6_2:deleteMin_6_2:p0.9999  PADDED-GEN  sample             324.674          us/op
DeleteMinLatencyBench.ratio_6_2:deleteMin_6_2:p1.00    PADDED-GEN  sample            6258.688          us/op
DeleteMinLatencyBench.ratio_6_2:insert_6_2             PADDED-GEN  sample  4022087      0.066 ± 0.012  us/op
DeleteMinLatencyBench.ratio_6_2:insert_6_2:p0.00       PADDED-GEN  sample                 ≈ 0          us/op
DeleteMinLatencyBench.ratio_6_2:insert_6_2:p0.50       PADDED-GEN  sample                 ≈ 0          us/op
DeleteMinLatencyBench.ratio_6_2:insert_6_2:p0.90       PADDED-GEN  sample               0.100          us/op
DeleteMinLatencyBench.ratio_6_2:insert_6_2:p0.95       PADDED-GEN  sample               0.100          us/op
DeleteMinLatencyBench.ratio_6_2:insert_6_2:p0.99       PADDED-GEN  sample               0.400          us/op
DeleteMinLatencyBench.ratio_6_2:insert_6_2:p0.999      PADDED-GEN  sample               3.800          us/op
DeleteMinLatencyBench.ratio_6_2:insert_6_2:p0.9999     PADDED-GEN  sample              14.192          us/op
DeleteMinLatencyBench.ratio_6_2:insert_6_2:p1.00       PADDED-GEN  sample           13139.968          us/op
DeleteMinLatencyBench.ratio_6_2:p0.00                  PADDED-GEN  sample                 ≈ 0          us/op
DeleteMinLatencyBench.ratio_6_2:p0.50                  PADDED-GEN  sample                 ≈ 0          us/op
DeleteMinLatencyBench.ratio_6_2:p0.90                  PADDED-GEN  sample               0.100          us/op
DeleteMinLatencyBench.ratio_6_2:p0.95                  PADDED-GEN  sample               0.100          us/op
DeleteMinLatencyBench.ratio_6_2:p0.99                  PADDED-GEN  sample               1.400          us/op
DeleteMinLatencyBench.ratio_6_2:p0.999                 PADDED-GEN  sample             174.848          us/op
DeleteMinLatencyBench.ratio_6_2:p0.9999                PADDED-GEN  sample             255.531          us/op
DeleteMinLatencyBench.ratio_6_2:p1.00                  PADDED-GEN  sample           13139.968          us/op
* */


/*
Benchmark                                              (type)    Mode      Cnt      Score   Error  Units
DeleteMinLatencyBench.ratio_4_4                          LBPQ  sample  4143074      8.819 ± 0.170  us/op
DeleteMinLatencyBench.ratio_4_4:deleteMin_4_4            LBPQ  sample  2342160      1.432 ± 0.207  us/op
DeleteMinLatencyBench.ratio_4_4:deleteMin_4_4:p0.00      LBPQ  sample                 ≈ 0          us/op
DeleteMinLatencyBench.ratio_4_4:deleteMin_4_4:p0.50      LBPQ  sample                 ≈ 0          us/op
DeleteMinLatencyBench.ratio_4_4:deleteMin_4_4:p0.90      LBPQ  sample               0.100          us/op
DeleteMinLatencyBench.ratio_4_4:deleteMin_4_4:p0.95      LBPQ  sample               0.100          us/op
DeleteMinLatencyBench.ratio_4_4:deleteMin_4_4:p0.99      LBPQ  sample               4.800          us/op
DeleteMinLatencyBench.ratio_4_4:deleteMin_4_4:p0.999     LBPQ  sample             218.071          us/op
DeleteMinLatencyBench.ratio_4_4:deleteMin_4_4:p0.9999    LBPQ  sample            1001.029          us/op
DeleteMinLatencyBench.ratio_4_4:deleteMin_4_4:p1.00      LBPQ  sample           39518.208          us/op
DeleteMinLatencyBench.ratio_4_4:insert_4_4               LBPQ  sample  1800914     18.426 ± 0.283  us/op
DeleteMinLatencyBench.ratio_4_4:insert_4_4:p0.00         LBPQ  sample                 ≈ 0          us/op
DeleteMinLatencyBench.ratio_4_4:insert_4_4:p0.50         LBPQ  sample               0.400          us/op
DeleteMinLatencyBench.ratio_4_4:insert_4_4:p0.90         LBPQ  sample               1.100          us/op
DeleteMinLatencyBench.ratio_4_4:insert_4_4:p0.95         LBPQ  sample             211.456          us/op
DeleteMinLatencyBench.ratio_4_4:insert_4_4:p0.99         LBPQ  sample             304.640          us/op
DeleteMinLatencyBench.ratio_4_4:insert_4_4:p0.999        LBPQ  sample             427.520          us/op
DeleteMinLatencyBench.ratio_4_4:insert_4_4:p0.9999       LBPQ  sample            1623.877          us/op
DeleteMinLatencyBench.ratio_4_4:insert_4_4:p1.00         LBPQ  sample           54329.344          us/op
DeleteMinLatencyBench.ratio_4_4:p0.00                    LBPQ  sample                 ≈ 0          us/op
DeleteMinLatencyBench.ratio_4_4:p0.50                    LBPQ  sample               0.100          us/op
DeleteMinLatencyBench.ratio_4_4:p0.90                    LBPQ  sample               0.700          us/op
DeleteMinLatencyBench.ratio_4_4:p0.95                    LBPQ  sample               1.100          us/op
DeleteMinLatencyBench.ratio_4_4:p0.99                    LBPQ  sample             265.216          us/op
DeleteMinLatencyBench.ratio_4_4:p0.999                   LBPQ  sample             364.544          us/op
DeleteMinLatencyBench.ratio_4_4:p0.9999                  LBPQ  sample            1279.370          us/op
DeleteMinLatencyBench.ratio_4_4:p1.00                    LBPQ  sample           54329.344          us/op
DeleteMinLatencyBench.ratio_4_4                          LOCK  sample  5042318      2.538 ± 0.031  us/op
DeleteMinLatencyBench.ratio_4_4                           OBQ  sample  5057047      3.478 ± 0.064  us/op
DeleteMinLatencyBench.ratio_4_4:deleteMin_4_4             OBQ  sample  2626930      3.316 ± 0.094  us/op
DeleteMinLatencyBench.ratio_4_4:deleteMin_4_4:p0.00       OBQ  sample                 ≈ 0          us/op
DeleteMinLatencyBench.ratio_4_4:deleteMin_4_4:p0.50       OBQ  sample               0.200          us/op
DeleteMinLatencyBench.ratio_4_4:deleteMin_4_4:p0.90       OBQ  sample               0.300          us/op
DeleteMinLatencyBench.ratio_4_4:deleteMin_4_4:p0.95       OBQ  sample               0.600          us/op
DeleteMinLatencyBench.ratio_4_4:deleteMin_4_4:p0.99       OBQ  sample              87.128          us/op
DeleteMinLatencyBench.ratio_4_4:deleteMin_4_4:p0.999      OBQ  sample             523.776          us/op
DeleteMinLatencyBench.ratio_4_4:deleteMin_4_4:p0.9999     OBQ  sample            1710.080          us/op
DeleteMinLatencyBench.ratio_4_4:deleteMin_4_4:p1.00       OBQ  sample            9617.408          us/op
DeleteMinLatencyBench.ratio_4_4:insert_4_4                OBQ  sample  2430117      3.653 ± 0.086  us/op
DeleteMinLatencyBench.ratio_4_4:insert_4_4:p0.00          OBQ  sample                 ≈ 0          us/op
DeleteMinLatencyBench.ratio_4_4:insert_4_4:p0.50          OBQ  sample               0.200          us/op
DeleteMinLatencyBench.ratio_4_4:insert_4_4:p0.90          OBQ  sample               0.600          us/op
DeleteMinLatencyBench.ratio_4_4:insert_4_4:p0.95          OBQ  sample               1.000          us/op
DeleteMinLatencyBench.ratio_4_4:insert_4_4:p0.99          OBQ  sample             113.408          us/op
DeleteMinLatencyBench.ratio_4_4:insert_4_4:p0.999         OBQ  sample             472.064          us/op
DeleteMinLatencyBench.ratio_4_4:insert_4_4:p0.9999        OBQ  sample            1417.192          us/op
DeleteMinLatencyBench.ratio_4_4:insert_4_4:p1.00          OBQ  sample            9519.104          us/op
DeleteMinLatencyBench.ratio_4_4:p0.00                     OBQ  sample                 ≈ 0          us/op
DeleteMinLatencyBench.ratio_4_4:p0.50                     OBQ  sample               0.200          us/op
DeleteMinLatencyBench.ratio_4_4:p0.90                     OBQ  sample               0.400          us/op
DeleteMinLatencyBench.ratio_4_4:p0.95                     OBQ  sample               0.800          us/op
DeleteMinLatencyBench.ratio_4_4:p0.99                     OBQ  sample             102.784          us/op
DeleteMinLatencyBench.ratio_4_4:p0.999                    OBQ  sample             498.176          us/op
DeleteMinLatencyBench.ratio_4_4:p0.9999                   OBQ  sample            1572.864          us/op
DeleteMinLatencyBench.ratio_4_4:p1.00                     OBQ  sample            9617.408          us/op
DeleteMinLatencyBench.ratio_6_2                          LBPQ  sample  3620464     14.912 ± 0.165  us/op
DeleteMinLatencyBench.ratio_6_2:deleteMin_6_2            LBPQ  sample   967557      1.211 ± 0.252  us/op
DeleteMinLatencyBench.ratio_6_2:deleteMin_6_2:p0.00      LBPQ  sample                 ≈ 0          us/op
DeleteMinLatencyBench.ratio_6_2:deleteMin_6_2:p0.50      LBPQ  sample                 ≈ 0          us/op
DeleteMinLatencyBench.ratio_6_2:deleteMin_6_2:p0.90      LBPQ  sample               0.100          us/op
DeleteMinLatencyBench.ratio_6_2:deleteMin_6_2:p0.95      LBPQ  sample               0.100          us/op
DeleteMinLatencyBench.ratio_6_2:deleteMin_6_2:p0.99      LBPQ  sample               1.400          us/op
DeleteMinLatencyBench.ratio_6_2:deleteMin_6_2:p0.999     LBPQ  sample             177.664          us/op
DeleteMinLatencyBench.ratio_6_2:deleteMin_6_2:p0.9999    LBPQ  sample             775.668          us/op
DeleteMinLatencyBench.ratio_6_2:deleteMin_6_2:p1.00      LBPQ  sample           22478.848          us/op
DeleteMinLatencyBench.ratio_6_2:insert_6_2               LBPQ  sample  2652907     19.909 ± 0.205  us/op
DeleteMinLatencyBench.ratio_6_2:insert_6_2:p0.00         LBPQ  sample                 ≈ 0          us/op
DeleteMinLatencyBench.ratio_6_2:insert_6_2:p0.50         LBPQ  sample               0.500          us/op
DeleteMinLatencyBench.ratio_6_2:insert_6_2:p0.90         LBPQ  sample               1.200          us/op
DeleteMinLatencyBench.ratio_6_2:insert_6_2:p0.95         LBPQ  sample             162.816          us/op
DeleteMinLatencyBench.ratio_6_2:insert_6_2:p0.99         LBPQ  sample             448.000          us/op
DeleteMinLatencyBench.ratio_6_2:insert_6_2:p0.999        LBPQ  sample             551.936          us/op
DeleteMinLatencyBench.ratio_6_2:insert_6_2:p0.9999       LBPQ  sample            1738.156          us/op
DeleteMinLatencyBench.ratio_6_2:insert_6_2:p1.00         LBPQ  sample           16809.984          us/op
DeleteMinLatencyBench.ratio_6_2:p0.00                    LBPQ  sample                 ≈ 0          us/op
DeleteMinLatencyBench.ratio_6_2:p0.50                    LBPQ  sample               0.300          us/op
DeleteMinLatencyBench.ratio_6_2:p0.90                    LBPQ  sample               1.000          us/op
DeleteMinLatencyBench.ratio_6_2:p0.95                    LBPQ  sample               1.800          us/op
DeleteMinLatencyBench.ratio_6_2:p0.99                    LBPQ  sample             436.736          us/op
DeleteMinLatencyBench.ratio_6_2:p0.999                   LBPQ  sample             535.552          us/op
DeleteMinLatencyBench.ratio_6_2:p0.9999                  LBPQ  sample            1554.337          us/op
DeleteMinLatencyBench.ratio_6_2:p1.00                    LBPQ  sample           22478.848          us/op
DeleteMinLatencyBench.ratio_6_2                           OBQ  sample  6301064      5.108 ± 0.078  us/op
DeleteMinLatencyBench.ratio_6_2:deleteMin_6_2             OBQ  sample  1940607      2.180 ± 0.101  us/op
DeleteMinLatencyBench.ratio_6_2:deleteMin_6_2:p0.00       OBQ  sample                 ≈ 0          us/op
DeleteMinLatencyBench.ratio_6_2:deleteMin_6_2:p0.50       OBQ  sample               0.200          us/op
DeleteMinLatencyBench.ratio_6_2:deleteMin_6_2:p0.90       OBQ  sample               0.200          us/op
DeleteMinLatencyBench.ratio_6_2:deleteMin_6_2:p0.95       OBQ  sample               0.300          us/op
DeleteMinLatencyBench.ratio_6_2:deleteMin_6_2:p0.99       OBQ  sample               2.100          us/op
DeleteMinLatencyBench.ratio_6_2:deleteMin_6_2:p0.999      OBQ  sample             520.393          us/op
DeleteMinLatencyBench.ratio_6_2:deleteMin_6_2:p0.9999     OBQ  sample            1693.696          us/op
DeleteMinLatencyBench.ratio_6_2:deleteMin_6_2:p1.00       OBQ  sample           16400.384          us/op
DeleteMinLatencyBench.ratio_6_2:insert_6_2                OBQ  sample  4360457      6.411 ± 0.103  us/op
DeleteMinLatencyBench.ratio_6_2:insert_6_2:p0.00          OBQ  sample                 ≈ 0          us/op
DeleteMinLatencyBench.ratio_6_2:insert_6_2:p0.50          OBQ  sample               0.200          us/op
DeleteMinLatencyBench.ratio_6_2:insert_6_2:p0.90          OBQ  sample               0.500          us/op
DeleteMinLatencyBench.ratio_6_2:insert_6_2:p0.95          OBQ  sample               0.900          us/op
DeleteMinLatencyBench.ratio_6_2:insert_6_2:p0.99          OBQ  sample             196.608          us/op
DeleteMinLatencyBench.ratio_6_2:insert_6_2:p0.999         OBQ  sample             932.864          us/op
DeleteMinLatencyBench.ratio_6_2:insert_6_2:p0.9999        OBQ  sample            2170.880          us/op
DeleteMinLatencyBench.ratio_6_2:insert_6_2:p1.00          OBQ  sample           16367.616          us/op
DeleteMinLatencyBench.ratio_6_2:p0.00                     OBQ  sample                 ≈ 0          us/op
DeleteMinLatencyBench.ratio_6_2:p0.50                     OBQ  sample               0.200          us/op
DeleteMinLatencyBench.ratio_6_2:p0.90                     OBQ  sample               0.400          us/op
DeleteMinLatencyBench.ratio_6_2:p0.95                     OBQ  sample               0.600          us/op
DeleteMinLatencyBench.ratio_6_2:p0.99                     OBQ  sample             156.160          us/op
DeleteMinLatencyBench.ratio_6_2:p0.999                    OBQ  sample             831.488          us/op
DeleteMinLatencyBench.ratio_6_2:p0.9999                   OBQ  sample            2101.248          us/op
DeleteMinLatencyBench.ratio_6_2:p1.00                     OBQ  sample           16400.384          us/op
* */


}