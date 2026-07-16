package io.github.kusoroadeolu.cleap.latest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MpmcEpochPQTest {

    @Test
    void pollOnEmptyQueueReturnsNull() {
        var queue = new MpmcEpochPQ<Integer>(8);
        assertNull(queue.poll());
    }

    @Test
    void addThenPollSingleElement() {
        var queue = new MpmcEpochPQ<Integer>(8);
        assertTrue(queue.add(42));
        assertEquals(42, queue.poll());
        assertNull(queue.poll());
    }

    @Test
    void capacityIsRoundedUpToPowerOfTwo() {
        // capacity 5 -> rounds to 8
        var queue = new MpmcEpochPQ<Integer>(5);
        for (int i = 0; i < 8; i++) {
            assertTrue(queue.add(i), "add should succeed for element " + i);
        }
        assertFalse(queue.add(99), "queue should be full at rounded capacity (8)");
    }

    @Test
    void addReturnsFalseWhenQueueIsFull() {
        var queue = new MpmcEpochPQ<Integer>(4); // rounds to 4
        assertTrue(queue.add(1));
        assertTrue(queue.add(2));
        assertTrue(queue.add(3));
        assertTrue(queue.add(4));
        assertFalse(queue.add(5));
    }

    @Test
    void pollReturnsElementsInSortedOrder() {
        var queue = new MpmcEpochPQ<Integer>(8);
        int[] values = {5, 3, 4, 1, 2};
        for (int v : values) {
            assertTrue(queue.add(v));
        }

        System.out.println(queue);
        Integer prev = queue.poll();
        System.out.println(queue);
        assertNotNull(prev);

        for (int i = 1; i < values.length; i++) {
            Integer next = queue.poll();

            assertNotNull(next); //3-5, 1-4, 2
            assertTrue(next >= prev, "expected sorted (non-decreasing) order, got " + prev + " then " + next);
            prev = next;
        }
        assertNull(queue.poll());
    }

    @Test
    void queueCanBeRefilledAfterBeingDrained() {
        var queue = new MpmcEpochPQ<Integer>(4);

        assertTrue(queue.add(1));
        assertTrue(queue.add(2));
        assertEquals(1, queue.poll());
        assertEquals(2, queue.poll());
        assertNull(queue.poll());

        // queue should be usable again after being fully drained
        assertTrue(queue.add(10));
        assertTrue(queue.add(20));
        assertEquals(10, queue.poll());
        assertEquals(20, queue.poll());
    }

    @Test
    void wraparoundAcrossMultipleFillDrainCycles() {
        var queue = new MpmcEpochPQ<Integer>(4); // small capacity to force wraparound quickly

        for (int cycle = 0; cycle < 5; cycle++) {
            assertTrue(queue.add(cycle * 10 + 1));
            assertTrue(queue.add(cycle * 10 + 2));
            assertTrue(queue.add(cycle * 10 + 3));

            assertEquals(cycle * 10 + 1, queue.poll());
            assertEquals(cycle * 10 + 2, queue.poll());
            assertEquals(cycle * 10 + 3, queue.poll());
            assertNull(queue.poll());
        }
    }

    @Test
    void singleElementPollDoesNotTriggerMerge() {
        // diff == 1 in merge() should take the -1 shortcut path without sorting
        var queue = new MpmcEpochPQ<Integer>(8);
        assertTrue(queue.add(7));
        assertEquals(7, queue.poll());
    }
}