package io.github.kusoroadeolu.cleap;

import java.util.Arrays;

// A bounded max heap.
/*
* This heap has the following methods:
*   find-max  find a maximum item of a max-heap, respectively (a.k.a. peek)
*   insert: adding a new key to the heap (a.k.a., push[4])
*   extract-max : returns the node of maximum value from a max heap after removing it from the heap (a.k.a., pop[5])
* */
public class BoundedSequentialHeap<T extends Comparable<T>> implements Heap<T>{
    private final Object[] tree;
    private final int capacity;
    private int size;

    //Array size should always be 2 ^ n - 1
    public BoundedSequentialHeap(int initialCap) {
        int pow2 = 1 << (32 - Integer.numberOfLeadingZeros(initialCap - 1));
        tree = new Object[capacity = (pow2 - 1)];
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
    @SuppressWarnings("unchecked")
    @Override
    public boolean insert(T t) {
        if (size - 1 == capacity) return false;
        int currIdx = size++;
        tree[currIdx] = t;
        int pIdx = (currIdx - 1) / 2;
        T parent;

        //Swim up
        while (pIdx > -1) {
            parent = (T) tree[pIdx];
            if (t.compareTo(parent) <= 0) return true;
            else {
                tree[pIdx] = t;
                tree[currIdx] = parent;
                currIdx = pIdx;
                pIdx =  (currIdx - 1) / 2;
            } //Sift up
        }

        return false;
    }

    @SuppressWarnings("unchecked")
    @Override
    public T findMax(){
        return (T) tree[0];
    }

    @SuppressWarnings("unchecked")
    @Override
    public T extractMax(){
        int pIdx = 0;
        var val = (T) tree[pIdx]; //Null the tree head
        tree[pIdx] = null;

        int cIdx1 = childIndex(pIdx, 1);
        int cIdx2 = childIndex(pIdx, 2);

        //Sift down
        while (cIdx1 <= size) {
            T child1 = (T) tree[cIdx1];
            T child2 = (T) tree[cIdx2];
            if (child2 == null || child1.compareTo(child2) >= 1) {
                tree[pIdx] = child1;
                tree[cIdx1] = null;
                pIdx = cIdx1;
            } else {
                tree[pIdx] = child2;
                tree[cIdx2] = null;
                pIdx = cIdx2;
            }

            cIdx1 = childIndex(pIdx, 1);
            cIdx2 = childIndex(pIdx, 2);
        }

        return val;
    }


    private int childIndex(int parentIdx, int by) {
        return parentIdx * 2 + by;
    }

    @Override
    public String toString() {
        return Arrays.toString(tree);
    }

    static void main() {
        BoundedSequentialHeap<Integer> heap = new BoundedSequentialHeap<>(8);
        heap.insert(30);
        heap.insert(50);
        heap.insert(100);
        heap.insert(70);
        heap.insert(200);
        heap.insert(300);
        IO.println(heap);
        heap.extractMax();
        IO.println(heap);
        heap.extractMax();
        IO.println(heap);
    }

}
