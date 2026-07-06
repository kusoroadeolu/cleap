package io.github.kusoroadeolu.cleap.bounded;

import io.github.kusoroadeolu.cleap.Heap;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Arrays;
import java.util.Comparator;
import java.util.PriorityQueue;

public class OrderedBoundedPQ<T> implements Heap<T> {
    final FixedPriorityQueue<T> pq;
    final Object lock;
    final int capacity;
    final int maxDaCapacity;
    DeleteArray<T> deleteArray;

    public OrderedBoundedPQ(int capacity) {
        this.pq = new FixedPriorityQueue<>(capacity);
        lock = new Object();
        this.capacity = capacity;
        this.maxDaCapacity = Math.max(1, (int) (0.1 * capacity));
        deleteArray = new DeleteArray<>(0);
    }

    @Override
    public boolean add(T t) {
        synchronized (lock) {
            var da = deleteArray;
            int size = (int) I_INDEX.get(da);

            int dSize = (int) D_INDEX.getAcquire(da);
            if ((dSize + size) >= capacity) return false;
            pq.add(t, size);
            I_INDEX.setRelease(da, 1);
            return true;
        }
    }

    @Override
    public T peek() {
        return null;
    }

    @Override
    public T poll() {
        DeleteArray<T> da = null;

            for (;;) {
                da = deleteArray;
                var s = loState(da);

                if (s != State.NONE) {
                    if (s == State.MERGED) continue;
                    while (loState(da) == State.MERGING) Thread.onSpinWait();
                    continue;
                }

                int idx = (int) I_INDEX.getAcquire(da);
                int daCap = da.capacity;

                if (daCap == 0 && idx == 0) return null;

                int dIdx = (int) D_INDEX.getAndAdd(da, 1);

                if (dIdx < daCap) return da.valueAt(dIdx);

                else if (idx == 0) return null;

                if ((s = loState(da)) != State.NONE || !casState(s, State.MERGING, da)) {
                    if (s == State.MERGED) continue;
                    while (loState(da) == State.MERGING) Thread.onSpinWait();
                    continue;
                }


            try {
                synchronized (lock) {

                    int iIdx = (int) I_INDEX.get(da); //value at this idx is always null (so this is always the size of the queue)
                    var pq = this.pq;

                    int newDaCap = Math.min(iIdx, maxDaCapacity);
                    Object[] dq = new Object[newDaCap];


                    for (int i = 0; i < newDaCap; ++i) {
                        dq[i] = pq.poll(--iIdx);
                    }

                    var newDa = new DeleteArray<T>(dq, iIdx, 1);
                    deleteArray = newDa;
                    return (T) dq[0];
                }

            }finally {
                STATE.setRelease(da, State.MERGED);
            }


            }

    }

    @Override
    public int size() {
        return 0;
    }


    State loState(DeleteArray<T> da) {
        return (State) STATE.getAcquire(da);
    }

    boolean casState(State a, State b, DeleteArray<T> da) {
        return STATE.compareAndSet(da, a, b);
    }


    public static class DeleteArray<T> {
        final Object[] items;
        final int capacity;
        volatile State state = State.NONE;
        volatile int deleteIndex = 0;
        volatile int insertIndex = 0;

        public DeleteArray(int capacity) {
            if (capacity == 0) items = null;
            else items = new Object[capacity];
            this.capacity = capacity;
        }

        public DeleteArray(Object[] o, int startIIdx, int startDIdx) {
            items = o;
            this.capacity = o.length;
            I_INDEX.set(this, startIIdx);
            D_INDEX.set(this, startDIdx);
        }


        T valueAt(int index) {
            return (T) items[index];
        }

        @Override
        public String toString() {
            return "DeleteArray{" +
                    "items=" + Arrays.toString(items) +
                    ", capacity=" + capacity +
                    ", deleteIndex=" + deleteIndex +
                    ", insertIndex=" + insertIndex +
                    '}';
        }
    }

    enum State {MERGING, NONE, MERGED}

    record Status (State state) {}


    private static final VarHandle D_INDEX;
    private static final VarHandle STATE;
    private static final VarHandle I_INDEX;


    static {
        var l = MethodHandles.lookup();
        try{
            D_INDEX = l.findVarHandle(DeleteArray.class, "deleteIndex", int.class);
            I_INDEX = l.findVarHandle(DeleteArray.class, "insertIndex", int.class);
            STATE = l.findVarHandle(DeleteArray.class, "state", State.class);
        }catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static class FixedPriorityQueue<T> {
        Object[] queue;
        int size;

        public FixedPriorityQueue(int capacity) {
            this.queue = new Object[capacity];
        }

        public void add(T t, int size) {
            int i = size;
            siftUpComparable(i, t, queue);
            this.size++;
        }

        public T poll(int size) {
            final Object[] es;
            final T result;

            if ((result = (T) ((es = queue)[0])) != null) {
                final int n;
                final T x = (T) es[(n = size)];
                this.size--;
                es[n] = null;
                if (n > 0) {
                    siftDownComparable(0, x, es, n);
                }
            }
            return result;
        }

        private static <T> void siftDownComparable(int k, T x, Object[] es, int n) {

                Comparable<? super T> key = (Comparable<? super T>)x;
                int half = n >>> 1;           // loop while a non-leaf
                while (k < half) {
                    int child = (k << 1) + 1; // assume left child is least
                    Object c = es[child];
                    int right = child + 1;
                    if (right < n && ((Comparable<? super T>) c).compareTo((T) es[right]) > 0)
                        c = es[child = right];
                    if (key.compareTo((T) c) <= 0)
                        break;
                    es[k] = c;
                    k = child;
                }
                es[k] = key;

        }

        void siftUpComparable(int k, T x, Object[] es) {
            Comparable<? super T> key = (Comparable<? super T>) x;
            while (k > 0) {
                int parent = (k - 1) >>> 1;
                Object e = es[parent];
                if (e == null || key.compareTo((T) e) >= 0)
                    break;
                es[k] = e;
                k = parent;
            }
            es[k] = key;
        }
    }
}
