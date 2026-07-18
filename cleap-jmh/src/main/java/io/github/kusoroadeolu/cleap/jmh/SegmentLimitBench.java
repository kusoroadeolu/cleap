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
@Fork(2)
public class SegmentLimitBench {
    private PriorityQueue<Integer> queue;

    @Param({"PADDED_EPO"})
    private String type;

    @Param ({"512", "2048", "4096"})
    private String segmentLimit;

    @Setup
    public void setup() {
        int cap = 65536;
        queue = switch (type) {
            case "PADDED_EPO" -> new PaddedArenaGenerationPQ<>(cap, Long.parseLong(segmentLimit));
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

    static class BenchRunner {
        static void main() throws RunnerException {
            Options options = new OptionsBuilder()
                    .include(SegmentLimitBench.class.getSimpleName())
                    .build();
            new org.openjdk.jmh.runner.Runner(options).run();
        }
    }

/*
* Benchmark                                          (segmentLimit)      (type)    Mode      Cnt      Score   Error  Units
SegmentLimitBench.ratio_6_2                                   512  PADDED_EPO  sample  5543570      0.273 ± 0.022  us/op
SegmentLimitBench.ratio_6_2:deleteMin_6_2                     512  PADDED_EPO  sample  1201722      0.998 ± 0.079  us/op
SegmentLimitBench.ratio_6_2:deleteMin_6_2:p0.00               512  PADDED_EPO  sample                 ≈ 0          us/op
SegmentLimitBench.ratio_6_2:deleteMin_6_2:p0.50               512  PADDED_EPO  sample               0.100          us/op
SegmentLimitBench.ratio_6_2:deleteMin_6_2:p0.90               512  PADDED_EPO  sample               0.200          us/op
SegmentLimitBench.ratio_6_2:deleteMin_6_2:p0.95               512  PADDED_EPO  sample               0.300          us/op
SegmentLimitBench.ratio_6_2:deleteMin_6_2:p0.99               512  PADDED_EPO  sample               2.200          us/op
SegmentLimitBench.ratio_6_2:deleteMin_6_2:p0.999              512  PADDED_EPO  sample             114.816          us/op
SegmentLimitBench.ratio_6_2:deleteMin_6_2:p0.9999             512  PADDED_EPO  sample             188.663          us/op
SegmentLimitBench.ratio_6_2:deleteMin_6_2:p1.00               512  PADDED_EPO  sample           14499.840          us/op
SegmentLimitBench.ratio_6_2:insert_6_2                        512  PADDED_EPO  sample  4341848      0.072 ± 0.018  us/op
SegmentLimitBench.ratio_6_2:insert_6_2:p0.00                  512  PADDED_EPO  sample                 ≈ 0          us/op
SegmentLimitBench.ratio_6_2:insert_6_2:p0.50                  512  PADDED_EPO  sample                 ≈ 0          us/op
SegmentLimitBench.ratio_6_2:insert_6_2:p0.90                  512  PADDED_EPO  sample               0.100          us/op
SegmentLimitBench.ratio_6_2:insert_6_2:p0.95                  512  PADDED_EPO  sample               0.100          us/op
SegmentLimitBench.ratio_6_2:insert_6_2:p0.99                  512  PADDED_EPO  sample               0.300          us/op
SegmentLimitBench.ratio_6_2:insert_6_2:p0.999                 512  PADDED_EPO  sample               4.200          us/op
SegmentLimitBench.ratio_6_2:insert_6_2:p0.9999                512  PADDED_EPO  sample              14.288          us/op
SegmentLimitBench.ratio_6_2:insert_6_2:p1.00                  512  PADDED_EPO  sample           19693.568          us/op
SegmentLimitBench.ratio_6_2:p0.00                             512  PADDED_EPO  sample                 ≈ 0          us/op
SegmentLimitBench.ratio_6_2:p0.50                             512  PADDED_EPO  sample                 ≈ 0          us/op
SegmentLimitBench.ratio_6_2:p0.90                             512  PADDED_EPO  sample               0.100          us/op
SegmentLimitBench.ratio_6_2:p0.95                             512  PADDED_EPO  sample               0.100          us/op
SegmentLimitBench.ratio_6_2:p0.99                             512  PADDED_EPO  sample               1.300          us/op
SegmentLimitBench.ratio_6_2:p0.999                            512  PADDED_EPO  sample             103.040          us/op
SegmentLimitBench.ratio_6_2:p0.9999                           512  PADDED_EPO  sample             122.496          us/op
SegmentLimitBench.ratio_6_2:p1.00                             512  PADDED_EPO  sample           19693.568          us/op
SegmentLimitBench.ratio_6_2                                  2048  PADDED_EPO  sample  5472304      0.401 ± 0.028  us/op
SegmentLimitBench.ratio_6_2:deleteMin_6_2                    2048  PADDED_EPO  sample  1070168      1.768 ± 0.113  us/op
SegmentLimitBench.ratio_6_2:deleteMin_6_2:p0.00              2048  PADDED_EPO  sample                 ≈ 0          us/op
SegmentLimitBench.ratio_6_2:deleteMin_6_2:p0.50              2048  PADDED_EPO  sample               0.100          us/op
SegmentLimitBench.ratio_6_2:deleteMin_6_2:p0.90              2048  PADDED_EPO  sample               0.200          us/op
SegmentLimitBench.ratio_6_2:deleteMin_6_2:p0.95              2048  PADDED_EPO  sample               0.200          us/op
SegmentLimitBench.ratio_6_2:deleteMin_6_2:p0.99              2048  PADDED_EPO  sample               2.100          us/op
SegmentLimitBench.ratio_6_2:deleteMin_6_2:p0.999             2048  PADDED_EPO  sample             518.144          us/op
SegmentLimitBench.ratio_6_2:deleteMin_6_2:p0.9999            2048  PADDED_EPO  sample             596.992          us/op
SegmentLimitBench.ratio_6_2:deleteMin_6_2:p1.00              2048  PADDED_EPO  sample           12156.928          us/op
SegmentLimitBench.ratio_6_2:insert_6_2                       2048  PADDED_EPO  sample  4402136      0.069 ± 0.021  us/op
SegmentLimitBench.ratio_6_2:insert_6_2:p0.00                 2048  PADDED_EPO  sample                 ≈ 0          us/op
SegmentLimitBench.ratio_6_2:insert_6_2:p0.50                 2048  PADDED_EPO  sample                 ≈ 0          us/op
SegmentLimitBench.ratio_6_2:insert_6_2:p0.90                 2048  PADDED_EPO  sample               0.100          us/op
SegmentLimitBench.ratio_6_2:insert_6_2:p0.95                 2048  PADDED_EPO  sample               0.100          us/op
SegmentLimitBench.ratio_6_2:insert_6_2:p0.99                 2048  PADDED_EPO  sample               0.300          us/op
SegmentLimitBench.ratio_6_2:insert_6_2:p0.999                2048  PADDED_EPO  sample               3.700          us/op
SegmentLimitBench.ratio_6_2:insert_6_2:p0.9999               2048  PADDED_EPO  sample              14.288          us/op
SegmentLimitBench.ratio_6_2:insert_6_2:p1.00                 2048  PADDED_EPO  sample           20611.072          us/op
SegmentLimitBench.ratio_6_2:p0.00                            2048  PADDED_EPO  sample                 ≈ 0          us/op
SegmentLimitBench.ratio_6_2:p0.50                            2048  PADDED_EPO  sample                 ≈ 0          us/op
SegmentLimitBench.ratio_6_2:p0.90                            2048  PADDED_EPO  sample               0.100          us/op
SegmentLimitBench.ratio_6_2:p0.95                            2048  PADDED_EPO  sample               0.100          us/op
SegmentLimitBench.ratio_6_2:p0.99                            2048  PADDED_EPO  sample               0.700          us/op
SegmentLimitBench.ratio_6_2:p0.999                           2048  PADDED_EPO  sample               7.200          us/op
SegmentLimitBench.ratio_6_2:p0.9999                          2048  PADDED_EPO  sample             532.480          us/op
SegmentLimitBench.ratio_6_2:p1.00                            2048  PADDED_EPO  sample           20611.072          us/op
SegmentLimitBench.ratio_6_2                                  4096  PADDED_EPO  sample  5280239      0.461 ± 0.034  us/op
SegmentLimitBench.ratio_6_2:deleteMin_6_2                    4096  PADDED_EPO  sample   992837      2.161 ± 0.174  us/op
SegmentLimitBench.ratio_6_2:deleteMin_6_2:p0.00              4096  PADDED_EPO  sample                 ≈ 0          us/op
SegmentLimitBench.ratio_6_2:deleteMin_6_2:p0.50              4096  PADDED_EPO  sample               0.100          us/op
SegmentLimitBench.ratio_6_2:deleteMin_6_2:p0.90              4096  PADDED_EPO  sample               0.200          us/op
SegmentLimitBench.ratio_6_2:deleteMin_6_2:p0.95              4096  PADDED_EPO  sample               0.200          us/op
SegmentLimitBench.ratio_6_2:deleteMin_6_2:p0.99              4096  PADDED_EPO  sample               2.100          us/op
SegmentLimitBench.ratio_6_2:deleteMin_6_2:p0.999             4096  PADDED_EPO  sample            1085.440          us/op
SegmentLimitBench.ratio_6_2:deleteMin_6_2:p0.9999            4096  PADDED_EPO  sample            1197.499          us/op
SegmentLimitBench.ratio_6_2:deleteMin_6_2:p1.00              4096  PADDED_EPO  sample            9142.272          us/op
SegmentLimitBench.ratio_6_2:insert_6_2                       4096  PADDED_EPO  sample  4287402      0.068 ± 0.011  us/op
SegmentLimitBench.ratio_6_2:insert_6_2:p0.00                 4096  PADDED_EPO  sample                 ≈ 0          us/op
SegmentLimitBench.ratio_6_2:insert_6_2:p0.50                 4096  PADDED_EPO  sample                 ≈ 0          us/op
SegmentLimitBench.ratio_6_2:insert_6_2:p0.90                 4096  PADDED_EPO  sample               0.100          us/op
SegmentLimitBench.ratio_6_2:insert_6_2:p0.95                 4096  PADDED_EPO  sample               0.100          us/op
SegmentLimitBench.ratio_6_2:insert_6_2:p0.99                 4096  PADDED_EPO  sample               0.300          us/op
SegmentLimitBench.ratio_6_2:insert_6_2:p0.999                4096  PADDED_EPO  sample               3.700          us/op
SegmentLimitBench.ratio_6_2:insert_6_2:p0.9999               4096  PADDED_EPO  sample              14.400          us/op
SegmentLimitBench.ratio_6_2:insert_6_2:p1.00                 4096  PADDED_EPO  sample           10108.928          us/op
SegmentLimitBench.ratio_6_2:p0.00                            4096  PADDED_EPO  sample                 ≈ 0          us/op
SegmentLimitBench.ratio_6_2:p0.50                            4096  PADDED_EPO  sample                 ≈ 0          us/op
SegmentLimitBench.ratio_6_2:p0.90                            4096  PADDED_EPO  sample               0.100          us/op
SegmentLimitBench.ratio_6_2:p0.95                            4096  PADDED_EPO  sample               0.100          us/op
SegmentLimitBench.ratio_6_2:p0.99                            4096  PADDED_EPO  sample               0.600          us/op
SegmentLimitBench.ratio_6_2:p0.999                           4096  PADDED_EPO  sample               5.096          us/op
SegmentLimitBench.ratio_6_2:p0.9999                          4096  PADDED_EPO  sample            1124.352          us/op
SegmentLimitBench.ratio_6_2:p1.00                            4096  PADDED_EPO  sample           10108.928          us/op
* */

/* 1024
* DeleteMinLatencyBench.ratio_6_2                        PADDED_EPO  sample  5168325      0.349 ± 0.017  us/op
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
