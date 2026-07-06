package io.github.kusoroadeolu.cleap.bounded;

import io.github.kusoroadeolu.cleap.Heap;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Arrays;

public class OrderedBoundedPQ<T> implements Heap<T> {
    Object[] queue;
    final Object lock;
    final int capacity;
    final int maxDaCapacity;
    DeleteArray<T> deleteArray;
    volatile State state = State.NONE;

    public OrderedBoundedPQ(int capacity) {
        this.queue = new Object[capacity];
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
            var q = queue;
            if ((dSize + size) == capacity) return false;
            siftUpComparable(size, t, q);
            I_INDEX.getAndAdd(da, 1);
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
        try {

            for (;;) {
                var s = STATE.getAcquire(this);
                da = deleteArray;

                if (s != State.NONE) {
                    while (STATE.getAcquire(this) == State.MERGING) Thread.onSpinWait();
                    continue;
                }

                int idx = (int) I_INDEX.getAcquire(da);
                int daCap = da.capacity;

                if (daCap == 0 && idx == 0) return null;

                int dIdx = (int) D_INDEX.getAndAdd(da, 1);

                if (dIdx < daCap) return da.valueAt(dIdx);

                else if (idx == 0) return null;

                if (STATE.getAcquire(this) != State.NONE || !STATE.compareAndSet(this, State.NONE, State.MERGING)) {
                    while (STATE.getAcquire(this) == State.MERGING) Thread.onSpinWait();
                    continue;
                }


                synchronized (lock) {
                    int iIdx = (int) I_INDEX.get(da);
                    Object[] iq = queue;
                    int newDaCap = Math.min(iIdx, maxDaCapacity);
                    Object[] dq = new Object[newDaCap];
                    Object[] newIq = new Object[capacity];
                    for (int i = 0, j = 0; i < iIdx; ++i) {
                        if (i < newDaCap) dq[i] = iq[i];
                        else newIq[j++] = iq[i];
                    }

                    var newDa = new DeleteArray<T>(dq, iIdx - newDaCap, 1);
                    queue = newIq;
                    deleteArray = newDa;
                    STATE.setRelease(this, State.NONE);
                    return (T) dq[0];
                }

            }
        }catch (RuntimeException e) {
            throw new RuntimeException(da.toString());
        }
    }

    @Override
    public int size() {
        return 0;
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


    public static class DeleteArray<T> {
        final Object[] items;
        final int capacity;
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

    enum State {NONE, MERGING}


    private static final VarHandle D_INDEX;
    private static final VarHandle STATE;
    private static final VarHandle I_INDEX;


    static {
        var l = MethodHandles.lookup();
        try{
            D_INDEX = l.findVarHandle(DeleteArray.class, "deleteIndex", int.class);
            I_INDEX = l.findVarHandle(DeleteArray.class, "insertIndex", int.class);
            STATE = l.findVarHandle(OrderedBoundedPQ.class, "state", State.class);
        }catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
