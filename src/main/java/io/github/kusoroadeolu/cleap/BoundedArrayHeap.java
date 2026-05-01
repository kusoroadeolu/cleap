package io.github.kusoroadeolu.cleap;

import java.util.Arrays;

// A bounded non thread safe max heap.
/*
* This heap has the following methods:
*   find-max  find a maximum item of a max-heap, respectively (a.k.a. peek)
*   insert: adding a new key to the heap (a.k.a., push[4])
*   extract-max : returns the node of maximum value from a max heap after removing it from the heap (a.k.a., pop[5])
* */
public class BoundedArrayHeap<T extends Comparable<T>> implements Heap<T> {
    private final Object[] items;
    private final int capacity;
    private int size;

    //Array size should always be 2 ^ n - 1
    public BoundedArrayHeap(int capacity) {
        int pow2 = 1 << (32 - Integer.numberOfLeadingZeros(capacity - 1));
        items = new Object[this.capacity = (pow2 - 1)];
    }

    /*
    * Given an index i, the children of the elem at idx i will be = 2i + 1 and 2i + 2
    * if(size - 1 == capacity) return false
    * Given a value t = T and an idx i = 0
    * Starting with a queue of unvisited idx = 0
    * while(true) {
    *   val currIdx = queue.poll();
    *   if(tree[currIdx] == null) {
    *       tree[currIdx] = val
    *       we then recursivly swim up, comparing the currIdx to the parent, swapping if the val currentIdx > parent until it is false or we reach the root
    * }else if(t.compareTo(tree[currIdx]) <= 0) {
    *       queue.offer(2*currIdx + 1); queue.offer(2 * currIdx + 2);
    *
    *   }
    * }
    *
    * Rather than looping through, we could just insert at the size, then sift up
    *
    * */
    @Override
    public boolean insert(T t) {
        if (size - 1 == capacity) return false;
        int childIdx = size++;
        items[childIdx] = t;
        int pIdx = parentIndex(childIdx);
        T parent;

        //Swim up
        while (pIdx > -1) {
            parent = valueAt(pIdx);
            if (t.compareTo(parent) <= 0) return true;
            else {
                items[pIdx] = t;
                items[childIdx] = parent;
                childIdx = pIdx;
                pIdx = parentIndex(childIdx);
            } //Sift up
        }

        return true;
    }

    @SuppressWarnings("unchecked")
    @Override
    public T peek(){
        return (T) items[0];
    }

    @SuppressWarnings("unchecked")
    @Override
    public T head(){
        int pIdx = 0;
        var val = (T) items[pIdx]; //Null the tree head
        if (val != null) --size;

        items[pIdx] = null;

        int cIdx1 = childIndex(pIdx, 1);
        int cIdx2 = childIndex(pIdx, 2);

        //Sift down
        while (cIdx1 <= size) {
            T child1 = valueAt(cIdx1);
            T child2 = valueAt(cIdx2);
            if (child2 == null || child1.compareTo(child2) >= 1) {
                items[pIdx] = child1;
                items[cIdx1] = null;
                pIdx = cIdx1;
            } else {
                items[pIdx] = child2;
                items[cIdx2] = null;
                pIdx = cIdx2;
            }

            cIdx1 = childIndex(pIdx, 1);
            cIdx2 = childIndex(pIdx, 2);
        }

        return val;
    }

    @SuppressWarnings("unchecked")
    T valueAt (int idx) {
        return (T) items[idx];
    }


    private int childIndex(int parentIdx, int by) {
        return parentIdx * 2 + by;
    }

    private int parentIndex(int childIdx) {
        return (childIdx - 1 ) / 2;
    }

    @Override
    public String toString() {
        return Arrays.toString(items);
    }


    @Override
    public int size() {
        return size;
    }

    @Override
    public int capacity() {
        return capacity;
    }
}
