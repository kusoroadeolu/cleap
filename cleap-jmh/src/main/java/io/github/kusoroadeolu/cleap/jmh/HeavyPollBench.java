package io.github.kusoroadeolu.cleap.jmh;

import io.github.kusoroadeolu.cleap.PriorityQueue;
import io.github.kusoroadeolu.cleap.dualarray.CombiningLBBoundedPQ;
import io.github.kusoroadeolu.cleap.dualarray.LBBoundedPQ;
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


@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 10, time = 1)
@Measurement(iterations = 10, time = 1)
@Fork(3)
public class HeavyPollBench {
    private PriorityQueue<Integer> queue;

    @Param({"OBQ", "ELB", "LB", "LOCK", "MPMC_EPO", "EPO", "PADDED_EPO"})
    private String type;

    @Param({"32768", "65536"})
    private String cap;

    @State(Scope.Thread)
    public static class ThreadState {
        int nextInt() {
            return ThreadLocalRandom.current().nextInt(100);
        }
    }

    @Setup
    public void setup() {
        int cap = Integer.parseInt(this.cap);
        queue = switch (type) {
            case "LOCK" -> new LockedPQ<>(cap);
            case "EPO" -> new EpochPQ<>(cap);
            case "PADDED_EPO" -> new PaddedArenaEpochPQ<>(cap);
            case "MPMC_EPO" -> new MpmcEpochPQ<>(cap);
            case "OBQ" -> new OrderedBoundedPQ<>(cap);
            case "ELB" -> new CombiningLBBoundedPQ<>(cap);
            case "LB" -> new LBBoundedPQ<>(cap);

            default -> throw new RuntimeException();
        };

        int to = cap / 2;
        for (int i = 0; i < to; ++i) queue.add(ThreadLocalRandom.current().nextInt(1_000_000));

    }



    @Threads(8)
    @Benchmark
    public void eightThreads(Blackhole bh, ThreadState ts) {
        doWork(bh, ts);
    }


    private void doWork(Blackhole bh, ThreadState ts) {
        int next = ts.nextInt();
         if (next >= 0 && next <= 79) bh.consume(queue.poll());
        else bh.consume(queue.add(ThreadLocalRandom.current().nextInt(1_000_000)));

    }

    static class BenchRunner {
        static void main() throws RunnerException {
            Options options = new OptionsBuilder()
                    .include(HeavyPollBench.class.getSimpleName())
                   // .addProfiler(JavaFlightRecorderProfiler.class, "dir=C:\\jfr-hp")
                    .build();
            new org.openjdk.jmh.runner.Runner(options).run();
        }

    }
}

/*
Benchmark                            (cap)  (type)    Mode      Cnt       Score   Error  Units
HeavyPollBench.eightThreads          32768     ELB  sample  4761801      30.652 ± 0.778  us/op
HeavyPollBench.eightThreads:p0.00    32768     ELB  sample                  ≈ 0          us/op
HeavyPollBench.eightThreads:p0.50    32768     ELB  sample                0.200          us/op
HeavyPollBench.eightThreads:p0.90    32768     ELB  sample               50.496          us/op
HeavyPollBench.eightThreads:p0.95    32768     ELB  sample               64.192          us/op
HeavyPollBench.eightThreads:p0.99    32768     ELB  sample              112.384          us/op
HeavyPollBench.eightThreads:p0.999   32768     ELB  sample             3175.211          us/op
HeavyPollBench.eightThreads:p0.9999  32768     ELB  sample            24903.680          us/op
HeavyPollBench.eightThreads:p1.00    32768     ELB  sample            92930.048          us/op
HeavyPollBench.eightThreads          32768      LB  sample  4940378      29.971 ± 0.946  us/op
HeavyPollBench.eightThreads:p0.00    32768      LB  sample                  ≈ 0          us/op
HeavyPollBench.eightThreads:p0.50    32768      LB  sample                0.100          us/op
HeavyPollBench.eightThreads:p0.90    32768      LB  sample               43.648          us/op
HeavyPollBench.eightThreads:p0.95    32768      LB  sample               57.792          us/op
HeavyPollBench.eightThreads:p0.99    32768      LB  sample              100.992          us/op
HeavyPollBench.eightThreads:p0.999   32768      LB  sample             3776.512          us/op
HeavyPollBench.eightThreads:p0.9999  32768      LB  sample            30015.488          us/op
HeavyPollBench.eightThreads:p1.00    32768      LB  sample           124518.400          us/op
HeavyPollBench.eightThreads          65536     ELB  sample  2990511      71.929 ± 4.267  us/op
HeavyPollBench.eightThreads:p0.00    65536     ELB  sample                  ≈ 0          us/op
HeavyPollBench.eightThreads:p0.50    65536     ELB  sample                0.100          us/op
HeavyPollBench.eightThreads:p0.90    65536     ELB  sample               70.656          us/op
HeavyPollBench.eightThreads:p0.95    65536     ELB  sample               96.256          us/op
HeavyPollBench.eightThreads:p0.99    65536     ELB  sample              168.448          us/op
HeavyPollBench.eightThreads:p0.999   65536     ELB  sample             4300.800          us/op
HeavyPollBench.eightThreads:p0.9999  65536     ELB  sample           113108.425          us/op
HeavyPollBench.eightThreads:p1.00    65536     ELB  sample           340262.912          us/op
HeavyPollBench.eightThreads          65536      LB  sample  4319483      38.636 ± 1.583  us/op
HeavyPollBench.eightThreads:p0.00    65536      LB  sample                  ≈ 0          us/op
HeavyPollBench.eightThreads:p0.50    65536      LB  sample                0.100          us/op
HeavyPollBench.eightThreads:p0.90    65536      LB  sample               55.040          us/op
HeavyPollBench.eightThreads:p0.95    65536      LB  sample               71.808          us/op
HeavyPollBench.eightThreads:p0.99    65536      LB  sample              124.928          us/op
HeavyPollBench.eightThreads:p0.999   65536      LB  sample             2781.184          us/op
HeavyPollBench.eightThreads:p0.9999  65536      LB  sample            53608.448          us/op
HeavyPollBench.eightThreads:p1.00    65536      LB  sample           180355.072          us/op
*/

/*
* Benchmark                            (cap)  (type)    Mode      Cnt      Score   Error  Units
HeavyPollBench.eightThreads          32768    LOCK  sample  5819544      1.262 ± 0.014  us/op
HeavyPollBench.eightThreads:p0.00    32768    LOCK  sample                 ≈ 0          us/op
HeavyPollBench.eightThreads:p0.50    32768    LOCK  sample               0.100          us/op
HeavyPollBench.eightThreads:p0.90    32768    LOCK  sample               0.100          us/op
HeavyPollBench.eightThreads:p0.95    32768    LOCK  sample               0.100          us/op
HeavyPollBench.eightThreads:p0.99    32768    LOCK  sample              54.848          us/op
HeavyPollBench.eightThreads:p0.999   32768    LOCK  sample              75.776          us/op
HeavyPollBench.eightThreads:p0.9999  32768    LOCK  sample             117.632          us/op
HeavyPollBench.eightThreads:p1.00    32768    LOCK  sample            3776.512          us/op


HeavyPollBench.eightThreads          65536    LOCK  sample  5743508      1.300 ± 0.014  us/op
HeavyPollBench.eightThreads:p0.00    65536    LOCK  sample                 ≈ 0          us/op
HeavyPollBench.eightThreads:p0.50    65536    LOCK  sample               0.100          us/op
HeavyPollBench.eightThreads:p0.90    65536    LOCK  sample               0.100          us/op
HeavyPollBench.eightThreads:p0.95    65536    LOCK  sample               0.100          us/op
HeavyPollBench.eightThreads:p0.99    65536    LOCK  sample              56.896          us/op
HeavyPollBench.eightThreads:p0.999   65536    LOCK  sample              78.720          us/op
HeavyPollBench.eightThreads:p0.9999  65536    LOCK  sample             111.488          us/op
HeavyPollBench.eightThreads:p1.00    65536    LOCK  sample            4227.072          us/op
*
*
*
Benchmark                            (cap)    (type)    Mode      Cnt      Score   Error  Units
HeavyPollBench.eightThreads          32768  MPMC_EPO  sample  6821941      0.599 ± 0.024  us/op
HeavyPollBench.eightThreads:p0.00    32768  MPMC_EPO  sample                 ≈ 0          us/op
HeavyPollBench.eightThreads:p0.50    32768  MPMC_EPO  sample               0.300          us/op
HeavyPollBench.eightThreads:p0.90    32768  MPMC_EPO  sample               1.300          us/op
HeavyPollBench.eightThreads:p0.95    32768  MPMC_EPO  sample               1.600          us/op
HeavyPollBench.eightThreads:p0.99    32768  MPMC_EPO  sample               2.600          us/op
HeavyPollBench.eightThreads:p0.999   32768  MPMC_EPO  sample               7.496          us/op
HeavyPollBench.eightThreads:p0.9999  32768  MPMC_EPO  sample              67.047          us/op
HeavyPollBench.eightThreads:p1.00    32768  MPMC_EPO  sample           18972.672          us/op
HeavyPollBench.eightThreads          65536  MPMC_EPO  sample  7089331      0.617 ± 0.024  us/op
HeavyPollBench.eightThreads:p0.00    65536  MPMC_EPO  sample                 ≈ 0          us/op
HeavyPollBench.eightThreads:p0.50    65536  MPMC_EPO  sample               0.300          us/op
HeavyPollBench.eightThreads:p0.90    65536  MPMC_EPO  sample               1.300          us/op
HeavyPollBench.eightThreads:p0.95    65536  MPMC_EPO  sample               1.700          us/op
HeavyPollBench.eightThreads:p0.99    65536  MPMC_EPO  sample               2.600          us/op
HeavyPollBench.eightThreads:p0.999   65536  MPMC_EPO  sample               7.496          us/op
HeavyPollBench.eightThreads:p0.9999  65536  MPMC_EPO  sample              63.002          us/op
HeavyPollBench.eightThreads:p1.00    65536  MPMC_EPO  sample           24281.088          us/op
* */


//RAW MPMC FIFO Queue
/*
Benchmark                            (cap)  (type)    Mode      Cnt      Score   Error  Units
HeavyPollBench.eightThreads          32768    MPMC  sample  7320727      0.379 ± 0.020  us/op
HeavyPollBench.eightThreads:p0.00    32768    MPMC  sample                 ≈ 0          us/op
HeavyPollBench.eightThreads:p0.50    32768    MPMC  sample               0.200          us/op
HeavyPollBench.eightThreads:p0.90    32768    MPMC  sample               0.700          us/op
HeavyPollBench.eightThreads:p0.95    32768    MPMC  sample               1.000          us/op
HeavyPollBench.eightThreads:p0.99    32768    MPMC  sample               1.500          us/op
HeavyPollBench.eightThreads:p0.999   32768    MPMC  sample               4.896          us/op
HeavyPollBench.eightThreads:p0.9999  32768    MPMC  sample              30.784          us/op
HeavyPollBench.eightThreads:p1.00    32768    MPMC  sample           18022.400          us/op
HeavyPollBench.eightThreads          65536    MPMC  sample  7347190      0.373 ± 0.021  us/op
HeavyPollBench.eightThreads:p0.00    65536    MPMC  sample                 ≈ 0          us/op
HeavyPollBench.eightThreads:p0.50    65536    MPMC  sample               0.200          us/op
HeavyPollBench.eightThreads:p0.90    65536    MPMC  sample               0.700          us/op
HeavyPollBench.eightThreads:p0.95    65536    MPMC  sample               1.000          us/op
HeavyPollBench.eightThreads:p0.99    65536    MPMC  sample               1.600          us/op
HeavyPollBench.eightThreads:p0.999   65536    MPMC  sample               4.896          us/op
HeavyPollBench.eightThreads:p0.9999  65536    MPMC  sample              25.184          us/op
HeavyPollBench.eightThreads:p1.00    65536    MPMC  sample           20086.784          us/op
* */

/*
* Benchmark                            (cap)      (type)    Mode      Cnt      Score   Error  Units
HeavyPollBench.eightThreads          32768  PADDED_EPO  sample  5592031      0.467 ± 0.027  us/op
HeavyPollBench.eightThreads:p0.00    32768  PADDED_EPO  sample                 ≈ 0          us/op
HeavyPollBench.eightThreads:p0.50    32768  PADDED_EPO  sample               0.100          us/op
HeavyPollBench.eightThreads:p0.90    32768  PADDED_EPO  sample               1.800          us/op
HeavyPollBench.eightThreads:p0.95    32768  PADDED_EPO  sample               1.800          us/op
HeavyPollBench.eightThreads:p0.99    32768  PADDED_EPO  sample               1.900          us/op
HeavyPollBench.eightThreads:p0.999   32768  PADDED_EPO  sample               8.000          us/op
HeavyPollBench.eightThreads:p0.9999  32768  PADDED_EPO  sample              76.646          us/op
HeavyPollBench.eightThreads:p1.00    32768  PADDED_EPO  sample           16056.320          us/op
HeavyPollBench.eightThreads          32768         EPO  sample  5910677      0.487 ± 0.024  us/op
HeavyPollBench.eightThreads:p0.00    32768         EPO  sample                 ≈ 0          us/op
HeavyPollBench.eightThreads:p0.50    32768         EPO  sample               0.100          us/op
HeavyPollBench.eightThreads:p0.90    32768         EPO  sample               1.800          us/op
HeavyPollBench.eightThreads:p0.95    32768         EPO  sample               1.800          us/op
HeavyPollBench.eightThreads:p0.99    32768         EPO  sample               1.900          us/op
HeavyPollBench.eightThreads:p0.999   32768         EPO  sample               8.127          us/op
HeavyPollBench.eightThreads:p0.9999  32768         EPO  sample              71.296          us/op
HeavyPollBench.eightThreads:p1.00    32768         EPO  sample           19365.888          us/op
HeavyPollBench.eightThreads          65536  PADDED_EPO  sample  5642111      0.464 ± 0.029  us/op
HeavyPollBench.eightThreads:p0.00    65536  PADDED_EPO  sample                 ≈ 0          us/op
HeavyPollBench.eightThreads:p0.50    65536  PADDED_EPO  sample               0.100          us/op
HeavyPollBench.eightThreads:p0.90    65536  PADDED_EPO  sample               1.700          us/op
HeavyPollBench.eightThreads:p0.95    65536  PADDED_EPO  sample               1.800          us/op
HeavyPollBench.eightThreads:p0.99    65536  PADDED_EPO  sample               1.800          us/op
HeavyPollBench.eightThreads:p0.999   65536  PADDED_EPO  sample               8.288          us/op
HeavyPollBench.eightThreads:p0.9999  65536  PADDED_EPO  sample              78.255          us/op
HeavyPollBench.eightThreads:p1.00    65536  PADDED_EPO  sample           21757.952          us/op
HeavyPollBench.eightThreads          65536         EPO  sample  5825476      0.538 ± 0.038  us/op
HeavyPollBench.eightThreads:p0.00    65536         EPO  sample                 ≈ 0          us/op
HeavyPollBench.eightThreads:p0.50    65536         EPO  sample               0.100          us/op
HeavyPollBench.eightThreads:p0.90    65536         EPO  sample               1.800          us/op
HeavyPollBench.eightThreads:p0.95    65536         EPO  sample               1.800          us/op
HeavyPollBench.eightThreads:p0.99    65536         EPO  sample               1.900          us/op
HeavyPollBench.eightThreads:p0.999   65536         EPO  sample               8.688          us/op
HeavyPollBench.eightThreads:p0.9999  65536         EPO  sample              99.733          us/op
HeavyPollBench.eightThreads:p1.00    65536         EPO  sample           29851.648          us/op
* */


/*
* Benchmark                    (cap)      (type)   Mode  Cnt   Score   Error   Units
HeavyPollBench.eightThreads  32768  PADDED_EPO  thrpt   30  28.407 ± 0.451  ops/us
HeavyPollBench.eightThreads  32768         EPO  thrpt   30  25.207 ± 0.324  ops/us
HeavyPollBench.eightThreads  65536  PADDED_EPO  thrpt   30  28.548 ± 0.441  ops/us
HeavyPollBench.eightThreads  65536         EPO  thrpt   30  25.329 ± 0.287  ops/us
HeavyPollBench.eightThreads  32768       OBQ  thrpt   30  13.710 ± 0.603  ops/us
HeavyPollBench.eightThreads  32768       ELB  thrpt   30   0.437 ± 0.036  ops/us
HeavyPollBench.eightThreads  32768        LB  thrpt   30   0.382 ± 0.036  ops/us
HeavyPollBench.eightThreads  32768      LOCK  thrpt   30  30.048 ± 0.719  ops/us
HeavyPollBench.eightThreads  32768  MPMC_EPO  thrpt   30  21.321 ± 0.947  ops/us
HeavyPollBench.eightThreads  65536       OBQ  thrpt   30  14.052 ± 0.480  ops/us
HeavyPollBench.eightThreads  65536       ELB  thrpt   30   0.285 ± 0.061  ops/us
HeavyPollBench.eightThreads  65536        LB  thrpt   30   0.304 ± 0.051  ops/us
HeavyPollBench.eightThreads  65536      LOCK  thrpt   30  30.991 ± 0.804  ops/us
HeavyPollBench.eightThreads  65536  MPMC_EPO  thrpt   30  21.525 ± 0.812  ops/us
* */