package io.github.kusoroadeolu.cleap.dualarray;

import io.github.kusoroadeolu.cleap.PriorityQueue;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Arrays;

public class OrderedBoundedPQ<T> implements PriorityQueue<T> {
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
            int size = da.lpIIndex();
            int dSize = Math.min(da.capacity, da.lvDIndex());
            int sum = dSize + size;

            if (sum >= capacity) return false;

            pq.add(t, size);
            da.incrementIIndex();
            return true;
        }
    }

    @Override
    public T poll() {
        DeleteArray<T> da;
            for (;;) {
                da = loDArr();
                var s = loState(da);

                if (s != State.NONE) {
                    if (s == State.MERGED) continue;
                    while (loState(da) == State.MERGING) Thread.onSpinWait();
                    continue;
                }

                int idx = da.lvIIndex();
                int daCap = da.capacity;

                if (daCap == 0 && idx == 0) return null; //initially empty

                int dIdx = da.incrementDIndex(); //get then incr

                if (dIdx < daCap) return da.valueAt(dIdx);

                else if (da.lvIIndex() == 0) return null; //overshot, still empty

                if ((s = loState(da)) != State.NONE || !casState(s, State.MERGING, da)) {
                    if (s == State.MERGED) continue;
                    while (loState(da) == State.MERGING) Thread.onSpinWait();
                    continue;
                }


                try {
                    synchronized (lock) {
                        int iIdx = da.lpIIndex(); //value at this idx is always null (so this is always the size of the queue)
                        var pq = this.pq;

                        int newDaCap = Math.min(iIdx, maxDaCapacity);
                        Object[] dq = new Object[newDaCap];


                        for (int i = 0; i < newDaCap; ++i) {
                            dq[i] = pq.poll(--iIdx);
                        }

                        var newDa = new DeleteArray<T>(dq, iIdx, 1);
                        soDarr(newDa);
                        return (T) dq[0];
                    }

                }finally {
                    da.state = State.MERGED;
                }
            }

    }

    public void soDarr(DeleteArray<T> da) {
        D_ARR.setRelease(this, da);
    }

    @Override
    public int size() {
        synchronized (lock) {
            var da = deleteArray;
            return da.insertIndex + (capacity - da.deleteIndex);
        }
    }


    State loState(DeleteArray<T> da) {
        return (State) STATE.getAcquire(da);
    }

    DeleteArray<T> loDArr() {
        return (DeleteArray<T>) D_ARR.getAcquire(this);
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


        void incrementIIndex() {
            I_INDEX.getAndAddRelease(this, 1);
        }

        int lvIIndex() {
            return (int) I_INDEX.getVolatile(this);
        }

        int lvDIndex() {
            return (int) D_INDEX.getVolatile(this);
        }

        int lpIIndex() {
            return (int) I_INDEX.get(this);
        }

        int incrementDIndex() {
            return (int) D_INDEX.getAndAdd(this, 1);
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


    private static final VarHandle D_INDEX;
    private static final VarHandle STATE;
    private static final VarHandle I_INDEX;
    private static final VarHandle D_ARR;


    static {
        var l = MethodHandles.lookup();
        try{
            D_ARR =  l.findVarHandle(OrderedBoundedPQ.class, "deleteArray", DeleteArray.class);
            D_INDEX = l.findVarHandle(DeleteArray.class, "deleteIndex", int.class);
            I_INDEX = l.findVarHandle(DeleteArray.class, "insertIndex", int.class);
            STATE = l.findVarHandle(DeleteArray.class, "state", State.class);
        }catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static class FixedPriorityQueue<T> {
        final Object[] queue;

        public FixedPriorityQueue(int capacity) {
            this.queue = new Object[capacity];
        }

        public void add(T t, int size) {
            int i = size;
            siftUpComparable(i, t, queue);
        }

        public T poll(int size) {
            final Object[] es;
            final T result;

            if ((result = (T) ((es = queue)[0])) != null) {
                final int n;
                final T x = (T) es[(n = size)];
                es[n] = null;
                if (n > 0) {
                    siftDownComparable(0, x, es, n);
                }
            }
            return result;

        }

        private static <T> void siftDownComparable(int k, T x, Object[] es, int n) {
                Comparable<? super T> key = (Comparable<? super T>) x;
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
                if (key.compareTo((T) e) >= 0)
                    break;
                es[k] = e;
                k = parent;
            }
            es[k] = key;
        }
    }

}
