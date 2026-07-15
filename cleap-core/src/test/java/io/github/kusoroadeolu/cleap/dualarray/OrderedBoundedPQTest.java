package io.github.kusoroadeolu.cleap.dualarray;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OrderedBoundedPQTest {

    @Test
    void mergeFlow() {
        var pq = new OrderedBoundedPQ<Integer>(10);
        pq.add(5); pq.add(1); pq.add(2); pq.add(7); pq.add(0);

        var q = pq.poll();
        System.out.println(pq.deleteArray);
        assertEquals(0, q);
    }
}