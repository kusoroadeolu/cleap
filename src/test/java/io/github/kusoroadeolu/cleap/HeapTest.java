package io.github.kusoroadeolu.cleap;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

//Sanity tests
class HeapTest {
    Heap<Integer> heap;

    @ParameterizedTest
    @ValueSource(strings = {"ub", "b"})
    void insertOnEmpty_headShouldEqualValue(String value){
        heap = switch (value) {
            case "ub" -> new UnboundedBTHeap<>();
            case "b" -> new BoundedArrayHeap<>(3);
            default -> throw new IllegalArgumentException();
        };

        heap.insert(1);
        assertEquals(1, heap.peek());
    }


    @ParameterizedTest
    @ValueSource(strings = {"ub", "b"})
    void onInsert_greaterValues_shouldSiftUp(String value){
        heap = switch (value) {
            case "ub" -> new UnboundedBTHeap<>();
            case "b" -> new BoundedArrayHeap<>(3);
            default -> throw new IllegalArgumentException();
        };

        heap.insert(1);
        heap.insert(2);
        heap.insert(3);
        assertEquals(3, heap.peek());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ub",  "b"})
    void onExtractMax_shouldSiftUpGreaterChild(String value){
        heap = switch (value) {
            case "ub" -> new UnboundedBTHeap<>();
            case "b" -> new BoundedArrayHeap<>(3);
            default -> throw new IllegalArgumentException();
        };

        heap.insert(1);
        heap.insert(2);
        heap.insert(3);
        heap.head();

        assertEquals(2, heap.peek()); //Should be 2
    }

    @ParameterizedTest
    @ValueSource(strings = {"ub",  "b"})
    void onExtractMax_shouldReturnNull_ifEmpty(String value){
        heap = switch (value) {
            case "ub" -> new UnboundedBTHeap<>();
            case "b" -> new BoundedArrayHeap<>(3);
            default -> throw new IllegalArgumentException();
        };

        heap.head();
        assertNull( heap.peek()); //Should be 2
    }

    @ParameterizedTest
    @ValueSource(strings = {"ub",  "b"})
    void sizeShouldReturnZeroIfEmpty(String value){
        heap = switch (value) {
            case "ub" -> new UnboundedBTHeap<>();
            case "b" -> new BoundedArrayHeap<>(3);
            default -> throw new IllegalArgumentException();
        };

        assertEquals(0, heap.size()); //Should be 2
    }


}