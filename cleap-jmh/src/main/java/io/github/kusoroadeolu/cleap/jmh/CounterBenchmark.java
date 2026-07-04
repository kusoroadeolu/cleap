package io.github.kusoroadeolu.cleap.jmh;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/*
* Just a quick benchmark against lock, cas and faa
* */
@BenchmarkMode(Mode.SampleTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 10, time = 1)
@Measurement(iterations = 10, time = 1)
@Fork(2)
public class CounterBenchmark {
    /*
    * Benchmark                          (type)   Mode  Cnt   Score   Error   Units
CounterBenchmark.counterBenchmark    LOCK  thrpt   20  47.527 ± 1.157  ops/us
CounterBenchmark.counterBenchmark     CAS  thrpt   20  12.125 ± 0.424  ops/us
CounterBenchmark.counterBenchmark     FAA  thrpt   20  42.420 ± 0.077  ops/us
    * */

    @Param({"LOCK", "CAS", "FAA"})//JDK, Optimistic
    private String type;

    private Counter counter;

    @Setup
    public void setup() {
        counter = switch (type) {
            case "LOCK" -> new LockCounter();
            case "CAS" -> new CASCounter();
            case "FAA" -> new FAACounter();
            default -> throw new RuntimeException();
        };
    }

    @TearDown(Level.Iteration)
    public void after() {
        counter.reset();
    }


    @Threads(8)
    @Benchmark
    public void counterBenchmark(Blackhole bh) {
        bh.consume(counter.increment());
    }



    interface Counter {
        int increment();
        void reset();
    }

    static class LockCounter implements Counter {
        final Lock lock = new ReentrantLock();
        int i;

        @Override
        public int increment() {
            var l = lock;
            l.lock();
            try {
                return i++;
            }finally {
                l.unlock();
            }

        }

        @Override
        public void reset() {
            var l = lock;
            l.lock();
            try {
                i = 0;
            }finally {
                l.unlock();
            }

        }
    }

    static class CASCounter implements Counter {
        final AtomicInteger i = new AtomicInteger();

        @Override
        public int increment() {
            var li = i;
            for (;;) {
                int seen = li.getAcquire();
                if (li.compareAndSet( seen, seen + 1)) return seen;
            }
        }

        @Override
        public void reset() {
            i.set(0);
        }
    }

    static class FAACounter implements Counter {
        final AtomicInteger i = new AtomicInteger();

        @Override
        public int increment() {
           return i.getAndIncrement();
        }

        @Override
        public void reset() {
            i.set(0);
        }
    }


    static class BenchRunner {
        static void main() throws RunnerException {
            Options options = new OptionsBuilder()
                    .include(CounterBenchmark.class.getSimpleName())
                    .build();
            new org.openjdk.jmh.runner.Runner(options).run();        }
    }
}
