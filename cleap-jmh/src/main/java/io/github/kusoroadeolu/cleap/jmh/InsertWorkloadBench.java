package io.github.kusoroadeolu.cleap.jmh;

import io.github.kusoroadeolu.cleap.OptimisticConcurrentHeap;
import io.github.kusoroadeolu.cleap.PIPQ;
import io.github.kusoroadeolu.cleap.StagedConcurrentHeap;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.profile.JavaFlightRecorderProfiler;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 10, time = 1)
@Measurement(iterations = 10, time = 1)
@Fork(3)
public class InsertWorkloadBench {
    private Queue<Integer> queue;


    /*
    * STA uses the previous insert logic of "trying to acquire the lock" after CAS to head
    * OPT uses the new insert logic of embedding state into the node and only acquiring the lock if the node's next pointer == null
    * Benchmark                         (type)   Mode  Cnt  Score   Error   Units
        InsertWorkloadBench.eightThreads     JDK  thrpt   30  4.242 ± 1.476  ops/us
        InsertWorkloadBench.eightThreads     OPT  thrpt   30  4.096 ± 1.297  ops/us
        InsertWorkloadBench.eightThreads     STA  thrpt   30  3.803 ± 0.944  ops/us
        InsertWorkloadBench.fourThreads      JDK  thrpt   30  4.516 ± 1.245  ops/us
        InsertWorkloadBench.fourThreads      OPT  thrpt   30  4.238 ± 1.151  ops/us
        InsertWorkloadBench.fourThreads      STA  thrpt   30  2.983 ± 0.750  ops/us
        Here we can see that the null -> acquire lock actually has better thrpt across all thread counts
    * */
    @Param({"PIPQ"}) //JDK, Optimistic
    private String type;

    @Setup
    public void setup() {
        queue = switch (type) {
            case "JDK" -> new PriorityBlockingQueue<>();
            case "PIPQ" -> new PIPQ<>();
            default -> throw new RuntimeException();
        };
    }

    @TearDown(Level.Iteration)
    public void after() {
        queue.clear();
        //System.out.println(((PIPQ<?>)queue).bound()) ;
    }


    @Threads(4)
    @Benchmark
    public void fourThreads(Blackhole bh) {
        doWork(bh);
    }

    @Threads(8)
    @Benchmark
    public void eightThreads(Blackhole bh) {
        doWork(bh);
    }


    private void doWork(Blackhole bh) {
        bh.consume(queue.offer(ThreadLocalRandom.current().nextInt(10_000)));
    }

    static class BenchRunner {
        static void main() throws RunnerException {
            Options options = new OptionsBuilder()
                    .include(InsertWorkloadBench.class.getSimpleName())
                    //.addProfiler(JavaFlightRecorderProfiler.class, "dir=C:\\jfr-sl")
                    .build();
            new org.openjdk.jmh.runner.Runner(options).run();        }
    }
}
