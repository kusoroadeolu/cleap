package io.github.kusoroadeolu.cleap;

import io.github.kusoroadeolu.cleap.experimental.BoundedArrayPriorityQueue;
import io.github.kusoroadeolu.cleap.experimental.OptimisticConcurrentPriorityQueue;
import io.github.kusoroadeolu.cleap.experimental.UnboundedBTPriorityQueue;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

//Sanity tests
class HeapTest {
    PriorityQueue<Integer> priorityQueue;

    @ParameterizedTest
    @ValueSource(strings = {"ub", "b", "op"})
    void addOnEmpty_headShouldEqualValue(String type){
        priorityQueue = getHeap(type);

        priorityQueue.add(1);
        assertEquals(1, priorityQueue.peek());
    }


    @ParameterizedTest
    @ValueSource(strings = {"ub", "b", "op"})
    void onAdd_higherPriority_shouldSiftUp(String type){
        priorityQueue = getHeap(type);

        priorityQueue.add(1);
        priorityQueue.add(2);
        priorityQueue.add(3);
         assertEquals(3, priorityQueue.peek());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ub", "b", "op"})
    void onExtractMax_shouldSiftUpGreaterChild(String type){
        priorityQueue = getHeap(type);

        priorityQueue.add(1);
        priorityQueue.add(2);
        priorityQueue.add(3);
        priorityQueue.poll();

        assertEquals(2, priorityQueue.peek()); //Should be 2
    }

    @ParameterizedTest
    @ValueSource(strings = {"ub", "b", "op"})
    void onExtractMax_shouldReturnNull_ifEmpty(String type){
        priorityQueue = getHeap(type);
        priorityQueue.poll();
        assertNull( priorityQueue.peek()); //Should be 2
    }

    @ParameterizedTest
    @ValueSource(strings = {"ub", "b", "op"})
    void sizeShouldReturnZeroIfEmpty(String type){
        priorityQueue = getHeap(type);

        assertEquals(0, priorityQueue.size()); //Should be 2
    }

    PriorityQueue<Integer> getHeap(String type) {
       return switch (type) {
            case "ub" -> new UnboundedBTPriorityQueue<>();
            case "b" -> new BoundedArrayPriorityQueue<>(3);
            case "op" -> new OptimisticConcurrentPriorityQueue<>();
            default -> throw new IllegalArgumentException();
        };
    }
}