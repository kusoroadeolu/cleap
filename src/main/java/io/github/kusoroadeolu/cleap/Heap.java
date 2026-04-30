package io.github.kusoroadeolu.cleap;

public interface Heap<T extends Comparable<T>> {
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
    *
    * Rather than looping through, we could just use the size
    *
    * */
    @SuppressWarnings("unchecked")
    boolean insert(T t);

    @SuppressWarnings("unchecked")
    T findMax();

    @SuppressWarnings("unchecked")
    T extractMax();
}
