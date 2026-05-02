package io.github.kusoroadeolu.cleap;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

//Sanity tests
class HeapTest {
    Heap<Integer> heap;

    @ParameterizedTest
    @ValueSource(strings = {"ub", "b", "op"})
    void insertOnEmpty_headShouldEqualValue(String type){
        heap = getHeap(type);

        heap.insert(1);
        assertEquals(1, heap.peek());
    }


    @ParameterizedTest
    @ValueSource(strings = {"ub", "b", "op"})
    void onInsert_higherPriority_shouldSiftUp(String type){
        heap = getHeap(type);

        heap.insert(1);
        heap.insert(2);
        heap.insert(3);
         assertEquals(3, heap.peek());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ub", "b", "op"})
    void onExtractMax_shouldSiftUpGreaterChild(String type){
        heap = getHeap(type);

        heap.insert(1);
        heap.insert(2);
        heap.insert(3);
        heap.poll();

        assertEquals(2, heap.peek()); //Should be 2
    }

    @ParameterizedTest
    @ValueSource(strings = {"ub", "b", "op"})
    void onExtractMax_shouldReturnNull_ifEmpty(String type){
        heap = getHeap(type);
        heap.poll();
        assertNull( heap.peek()); //Should be 2
    }

    @ParameterizedTest
    @ValueSource(strings = {"ub", "b", "op"})
    void sizeShouldReturnZeroIfEmpty(String type){
        heap = getHeap(type);

        assertEquals(0, heap.size()); //Should be 2
    }

    Heap<Integer> getHeap(String type) {
       return switch (type) {
            case "ub" -> new UnboundedBTHeap<>();
            case "b" -> new BoundedArrayHeap<>(3);
            case "op" -> new StagedConcurrentHeap<>();
            default -> throw new IllegalArgumentException();
        };
    }
}