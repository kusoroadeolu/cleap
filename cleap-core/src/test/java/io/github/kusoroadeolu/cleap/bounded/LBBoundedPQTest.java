package io.github.kusoroadeolu.cleap.bounded;

import io.github.kusoroadeolu.cleap.bounded.LBBoundedPQ.DeleteArray.Status;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.*;

class LBBoundedPQTest {

    @Test
    void onDeleteWithEmptyPQShouldReturnNull() {
        var pq = new LBBoundedPQ<>(1);
        assertEquals(null, pq.poll());
    }

    @Test
    void onDeleteWithNoEmptyPQShouldReturnNull() {
        var pq = new LBBoundedPQ<>(2);
        pq.add(1);
        assertEquals(1, pq.poll());
    }

    @Test
    void onInsertThenDeleteEnsure9ValuesInDA() {
        var pq = new LBBoundedPQ<Integer>(100);
        for (int i = 0; i < 100; ++i) {
            pq.add(ThreadLocalRandom.current().nextInt(1_000));
        }

        Integer polled = pq.poll();
        assertEquals(9, pq.deleteArray().size());
    }

    @Test
    void onInsertThenDeleteEnsureNoNullsInDA() {
        var pq = new LBBoundedPQ<Integer>(100);
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