package io.github.kusoroadeolu.cleap;

import java.util.Arrays;

//Same mech as a bounded array heap, but with a grow method if the array overflows
public class UnboundedArrayHeap<T extends Comparable<T>> implements Heap<T>{
    private Object[] tree;
    private int capacity;
    private int size;

    //Array size should always be 2 ^ n - 1
    public UnboundedArrayHeap(int initialCap) {
        int pow2 = 1 << (32 - Integer.numberOfLeadingZeros(initialCap - 1));
        tree = new Object[capacity = (pow2 - 1)];
    }

    @Override
    public boolean insert(T t) {
        if (size - 1 == capacity) grow();
        int childIdx = size++;
        tree[childIdx] = t;
        int pIdx = parentIndex(childIdx);
        T parent;

        while (pIdx > -1) {
            parent = valueAt(pIdx);
            if (t.compareTo(parent) <= 0) return true;
            else {
                tree[pIdx] = t;
                tree[childIdx] = parent;
                childIdx = pIdx;
                pIdx = parentIndex(childIdx);
            } //Sift up
        }

        return true;
    }

    void grow(){
        int newCapacity = 1 << (32 - Integer.numberOfLeadingZeros((capacity * 2) - 1));
        tree = Arrays.copyOf(tree, newCapacity);
        capacity = newCapacity;

    }

    @SuppressWarnings("unchecked")
    @Override
    public T peek(){
        return (T) tree[0];
    }

    @SuppressWarnings("unchecked")
    @Override
    public T poll(){
        int pIdx = 0;
        var val = (T) tree[pIdx]; //Null the tree head
        if (val != null) --size;

        tree[pIdx] = null;

        int cIdx1 = childIndex(pIdx, 1);
        int cIdx2 = childIndex(pIdx, 2);

        //Sift down
        while (cIdx1 <= size) {
            T child1 = valueAt(cIdx1);
            T child2 = valueAt(cIdx2);
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

    @SuppressWarnings("unchecked")
    T valueAt (int idx) {
        return (T) tree[idx];
    }


    private int childIndex(int parentIdx, int by) {
        return parentIdx * 2 + by;
    }

    private int parentIndex(int childIdx) {
        return (childIdx - 1 ) / 2;
    }

    @Override
    public String toString() {
        return Arrays.toString(tree);
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
