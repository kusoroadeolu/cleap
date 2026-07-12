package io.github.kusoroadeolu.cleap.bounded;

import org.junit.jupiter.api.Test;

import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.*;

class ElimLBBoundedPQTest {
    @Test
    void mergeFlow_onDelete_shouldBeFilledProperly() {
        var pq = new CombiningLBBoundedPQ<>(10);
        pq.add(5); pq.add(1); pq.add(3);
        assertEquals(1, pq.poll());
    }


    @Test
    void onDeleteWithEmptyPQShouldReturnNull() {
        var pq = new CombiningLBBoundedPQ<>(1);
        assertEquals(null, pq.poll());
    }

    @Test
    void onDeleteWithNoEmptyPQShouldReturnNull() {
        var pq = new CombiningLBBoundedPQ<>(2);
        pq.add(1);
        assertEquals(1, pq.poll());
    }

    @Test
    void onInsertThenDeleteEnsure9ValuesInDA() {
        var pq = new CombiningLBBoundedPQ<Integer>(100);
        for (int i = 0; i < 100; ++i) {
            pq.add(ThreadLocalRandom.current().nextInt(1_000));
        }

        Integer polled = pq.poll();
        Integer polled1 = pq.poll();
        assertEquals(8, pq.deleteArray().size());
        assertNotNull(polled1);
    }

    @Test
    void onInsertThenDeleteEnsureNoNullsInDA() {
        var pq = new CombiningLBBoundedPQ<Integer>(100);
        for (int i = 0; i < 100; ++i) {
            pq.add(ThreadLocalRandom.current().nextInt(1_000));
        }

        pq.poll();
        Object[] oa = pq.deleteArray().items;
        for (int i = 0; i < 10; ++i) {
            assertNotNull(oa[i]);
        }
    }

}