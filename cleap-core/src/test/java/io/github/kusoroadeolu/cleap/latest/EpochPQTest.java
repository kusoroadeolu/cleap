package io.github.kusoroadeolu.cleap.latest;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Sequential (single-threaded) unit tests for PaddedArenaEpochPQ.
 *
 * Note: PaddedArenaEpochPQ.poll() has a lock-free "arena" fallback path meant for
 * concurrent contention. In a single-threaded test, acquire() will always
 * succeed on first try, so we only ever exercise the fast path. The arena
 * fallback is NOT covered here and needs separate concurrent tests.
 */
class EpochPQTest {

    private PaddedArenaEpochPQ<Integer> queue;

    @BeforeEach
    void setUp() {
        queue = new PaddedArenaEpochPQ<>(16);
    }

    // Fills the queue via offer() until it returns false, returning how many succeeded.
    private int fillToCapacity(PaddedArenaEpochPQ<Integer> q) {
        int count = 0;
        while (q.offer(count)) {
            count++;
        }
        return count;
    }

    @Test
    void offer_succeedsOnEmptyQueue() {
        assertTrue(queue.offer(1));
    }

    @Test
    void offer_returnsFalseWhenFull() {
        int capacity = fillToCapacity(queue);
        assertTrue(capacity > 0, "queue should accept at least one element before filling up");
        assertFalse(queue.offer(999), "offer should fail once the buffer is full");
    }

    @Test
    void offer_throwsNpeOnNull() {
        assertThrows(NullPointerException.class, () -> queue.offer(null));
    }

    @Test
    void add_delegatesToOffer() {
        assertTrue(queue.add(1));
        assertEquals(Integer.valueOf(1), queue.poll());
    }

    @Test
    void add_returnsFalseWhenFull() {
        fillToCapacity(queue);
        assertFalse(queue.add(999));
    }

    @Test
    void poll_returnsNullOnEmptyQueue() {
        assertNull(queue.poll());
    }

    @Test
    void poll_singleElement_returnsSameElement() {
        queue.offer(42);
        assertEquals(Integer.valueOf(42), queue.poll());
        assertNull(queue.poll());
    }

    @Test
    void poll_multipleElements_returnsSortedOrder() {
        int[] values = {5, 3, 1, 4, 2};
        for (int v : values) {
            queue.offer(v);
        }

        List<Integer> polled = new ArrayList<>();
        Integer e;
        while ((e = queue.poll()) != null) {
            polled.add(e);
        }

        assertEquals(List.of(1, 2, 3, 4, 5), polled);
    }

    @Test
    void poll_moreThanMergeLimit_stillReturnsAllSortedAcrossChunks() {
        // Use a larger queue so we can push past capacity/merge-limit boundaries
        // and verify chunked merging still produces a fully sorted drain.
        PaddedArenaEpochPQ<Integer> q = new PaddedArenaEpochPQ<>(64);
        int n = fillToCapacity(q);
        assertTrue(n > 0);

        List<Integer> expected = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            expected.add(i);
        }

        System.out.println(q);

        expected.sort(Integer::compareTo);

        List<Integer> polled = new ArrayList<>();
        Integer e;
        while ((e = q.poll()) != null) {
            polled.add(e);
        }

        assertEquals(expected, polled);
    }

    @Test
    void offerAfterFull_succeedsAfterPoll() {
        fillToCapacity(queue);
        assertFalse(queue.offer(999));

        Integer polled = queue.poll();
        assertNotNull(polled);

        assertTrue(queue.offer(999), "offer should succeed again after a slot is freed by poll");
    }

    @Test
    void pollDrainsExactlyOfferedCount() {
        int n = 5;
        for (int i = 0; i < n; i++) {
            queue.offer(i);
        }

        for (int i = 0; i < n; i++) {
            assertNotNull(queue.poll(), "expected a non-null element at index " + i);
        }

        assertNull(queue.poll(), "queue should be empty after draining all offered elements");
    }

    @Test
    void size_alwaysReturnsZero() {
        // Pinning current (stub) behavior: size() is not actually implemented.
        assertEquals(0, queue.size());
        queue.offer(1);
        assertEquals(0, queue.size());
    }

    @Test
    void peek_alwaysReturnsNull() {
        // Pinning current (stub) behavior: peek() is not actually implemented.
        assertNull(queue.peek());
        queue.offer(1);
        assertNull(queue.peek());
    }
}