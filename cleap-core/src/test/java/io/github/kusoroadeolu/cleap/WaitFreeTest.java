package io.github.kusoroadeolu.cleap;

import org.jetbrains.lincheck.Lincheck;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class WaitFreeTest {



    @Test
    public void stressTest() {
        Lincheck.runConcurrentTest(() -> {
            final Heap<Integer> heap = new WaitFreeHeap<>(3);
            Thread t1 = new Thread(() -> heap.add(1));
            Thread t2 = new Thread(() -> heap.add(2));
            Thread t3 = new Thread(() -> heap.add(3));

            t1.start();
            t2.start();
            t3.start();

            try {
                t1.join();
                t2.join();
                t3.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }


            Assertions.assertEquals(3, heap.peek());
        });
    }

}
