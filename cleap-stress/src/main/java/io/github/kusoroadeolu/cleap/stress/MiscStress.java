package io.github.kusoroadeolu.cleap.stress;

import org.openjdk.jcstress.annotations.*;
import org.openjdk.jcstress.infra.results.LL_Result;

import java.util.concurrent.atomic.AtomicInteger;

public class MiscStress {
    @JCStressTest()
    @Outcome(id = {"1, 1", "0, 1"}, expect = Expect.ACCEPTABLE, desc = "Invariant maintained")
    @State
    //Basically ensure a thread never sees zero in the I_INDEX varhandle during a merge
    public static class SizeConsistency {
        final AtomicInteger i = new AtomicInteger(0);
        long j = 0;


        //One thread should trigger a merge, so one thread should at least valid value

        @Actor
        public void writer1(){
            j = 5;
            i.setRelease(1);
        }

        @Actor
        public void writer(){
            j = 6;
            i.setRelease(1);
        }

        @Actor
        public void reader(LL_Result r){
            r.r1 = i.getAcquire();
            r.r2 = j;
        }
    }
}
