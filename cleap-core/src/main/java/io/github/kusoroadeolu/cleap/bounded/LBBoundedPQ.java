package io.github.kusoroadeolu.cleap.bounded;

import io.github.kusoroadeolu.cleap.Heap;
import io.github.kusoroadeolu.cleap.bounded.LBBoundedPQ.DeleteArray.Status;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static io.github.kusoroadeolu.cleap.bounded.LBBoundedPQ.DeleteArray.State.MERGING;
import static io.github.kusoroadeolu.cleap.bounded.LBBoundedPQ.DeleteArray.State.NONE;

public class LBBoundedPQ<T> implements Heap<T> {
    private final InsertArray<T> insertArray;
    private final Comparator<T> nullReverseComparator; //Packs the bottom of an array with nulls
    private volatile DeleteArray<T> deleteArray;
    private final int capacity;
    private final int maxDaCapacity;
    private final int slack;

    public LBBoundedPQ(int capacity) {
        this.capacity = capacity;
        this.maxDaCapacity = Math.max(1, (int) (0.1 * capacity));
        this.insertArray = new InsertArray<>(capacity);
        this.deleteArray = new DeleteArray<>(0);
        this.slack = 10;
        this.nullReverseComparator = (a, b) -> {
            if (b == null || a == null) return 0;
            return compare(b, a);
        };
    }

    public LBBoundedPQ(int capacity, int slack) {
        this.capacity = capacity;
        this.maxDaCapacity = Math.max(1, (int) (0.1 * capacity));
        this.insertArray = new InsertArray<>(capacity);
        this.deleteArray = new DeleteArray<>(0);
        this.slack = slack;
        this.nullReverseComparator = (a, b) -> {
            if (b == null || a == null) return 0;
            return compare(b, a);
        };
    }

    LBBoundedPQ(InsertArray<T> ia, DeleteArray<T> da) {
        this.capacity = ia.items.length;
        this.maxDaCapacity = Math.max(1, (int) (0.1 * capacity));
        this.insertArray = ia;
        this.deleteArray = da;
        this.slack = 10;
        this.nullReverseComparator = (a, b) -> {
            if (b == null || a == null) return 1;
            return compare(b, a);
        };
    }


    @Override
    public boolean add(T t) {
        var ia = insertArray;
        var lock = ia.rwLock.readLock();
        int capacity = this.capacity;
        lock.lock();
        try {
            int index = ia.loIndex();

            if (index == capacity) return false;
            var da = deleteArray;
            Status daStatus;

            for (;;) {
                //70, 30
                //71, 30 -0
                daStatus = da.status;
                int daIndex = daStatus.index; // (0, 1, 2, 3), if index is on 2, that means value at 2 hasn't been consumed yet
                int daSize = da.capacity; //10 - 5 = 5
                int next = index + 1;
                int total = index + (daSize - daIndex);

                if (total >= capacity)return false;
                else if (I_INDEX.compareAndSet(ia, index, next)) break;
                else index = ia.loIndex(); //re-read
            }


            ia.items[index] = t;
            if (da.capacity == 0 || daStatus.state != DeleteArray.State.NONE) return true; //nothing in the D.A or da is freezing, we'll get merged soon
            var lastDaIndex = da.capacity - 1;
            int prio = compare(t, da.items[lastDaIndex]);
            if (prio < 0) MERGE.getAndAdd(da, 1); //Writes to items is made visible by da status read
            return true;
        }finally {
            lock.unlock();
        }
    }

    @Override
    public T poll() {
        outer: for (;;) {
            var da = deleteArray;
            int daSize;
            int seenIndex;
            int daIndex;
            Status daStatus;
            var ia = insertArray;

            for (;;) {
                daStatus = da.status;
                daSize = da.capacity;

                if (isFreezingOrFrozen(daStatus.state)) {
                    for (;;) {
                        if (da != deleteArray) continue outer;
                        Thread.onSpinWait();
                    }
                }

                seenIndex = ia.loIndex();

                if (daSize == 0 && seenIndex == 0) return null; //Otherwise try to start a merge

                daIndex = daStatus.index;

                boolean isEmpty = daIndex == daSize;

                if (isEmpty && seenIndex == 0) return null;

                boolean shouldMerge = isEmpty || da.mergeCount >= slack;
                // if , reassign daStatus if the cas succeeds

                Status s;
                if (shouldMerge) {
                    if (da.casStatus(daStatus, (s = new Status(daIndex, MERGING)))) {
                        daStatus = s;
                        break;
                    } else {
                        for (;;) {
                            if (da != deleteArray) continue outer;
                            Thread.onSpinWait();
                        }
                    }

                } else if (da.casStatus(daStatus, new Status(daIndex + 1, NONE))) {
                    return da.items[daIndex];
                }
            }

            var lock = ia.rwLock.writeLock();

            lock.lock();
            try {
                var iItems = ia.items;
                var dItems = da.items;
                var cmp = nullReverseComparator;
                int iIndex = ia.loIndex(); //start index for insert array (val at this index is always null)
                int dIndex = daStatus.index; //start index for delete array
                int dSize = da.capacity;
                for (int i = dIndex; i < dSize; ++i) {
                    iItems[iIndex++] = dItems[dIndex];
                }

                Arrays.sort(iItems, cmp);

                /*
                Given this structure with a newDSize 100 and d arr newDSize 30
                2 possibilities:
                1. merged d and i arrays have a newDSize less than 30
                2. merged d and i arrays a newDSize greater than or equal 30

                iIndex  = 20; (start Index)
                dAliveSize = 5;

                after merge,
                currIIndex = 24 and currISize = 25
                currIIndex to Math.max(0, currIIndex - 30)


                iIndex  = 30; (start Index)
                dAliveSize = 5;

                after merge,
                currIIndex = 34 and currISize = 35
                currIIndex to Math.max(0, currIIndex - 30)
                * */

                int newDSize = Math.min(iIndex, maxDaCapacity);
                var newDa = new DeleteArray<T>(newDSize, new DeleteArray.Status(1, NONE));
                for (int i = iIndex - 1, j = 0; j < newDSize; --i, j++) {
                    var v = iItems[i];
                    newDa.items[j] = v;
                    iItems[i] = null;
                }

                T item = newDa.items[0];
                ia.index = Math.max(0, iIndex - maxDaCapacity);
                deleteArray = newDa;

                return item;
            }finally {
                lock.unlock();
            }
        }
    }


    public List<T> toList() {
        var da = deleteArray;
        var ia = insertArray;
        List<T> list = new ArrayList<>();

        ia.rwLock.readLock().lock();
        try {
            for (int i = da.status.index; i < da.capacity; ++i) {
                list.add(da.items[i]);
            }

            int upto = ia.index;
            for (int i = 0; i < upto; ++i) {
                list.add(ia.items[i]);
            }

        }finally {
            ia.rwLock.readLock().unlock();
        }

        return list;
    }

    InsertArray<T> insertArray() {
        return insertArray;
    }

    DeleteArray<T> deleteArray() {
        return deleteArray;
    }

    @Override
    public T peek() {
        return null;
    }

    int compare(T a, T b) {
        return ((Comparable<T>) a).compareTo(b);
    }

    boolean isFreezingOrFrozen(DeleteArray.State s) {
        return s == MERGING;
    }

    @Override
    public int size() {
        var da = deleteArray;
        return insertArray.loIndex() + (da.size() - da.status.index);
    }

    static class DeleteArray<T> {
        final T[] items;
        final int capacity;
        volatile Status status = new Status(0, NONE);
        volatile int mergeCount;

        DeleteArray(int capacity) {
            if (capacity == 0) items = null;
            else items = (T[]) new Object[capacity];
            this.capacity = capacity;

        }

        DeleteArray(int capacity, Status s) {
            this(capacity);
            STATUS.set(this, s);
        }

        int size() {
            return capacity - status.index;
        }

        boolean casStatus(Status s, Status newS) {
            return STATUS.compareAndSet(this, s, newS);
        }

        static class Status {
            private final int index;
            private final State state;

            public Status(int index, State state) {
                this.index = index;
                this.state = state;
            }

            @Override
            public String toString() {
                return "Status{" +
                        "index=" + index +
                        ", state=" + state +
                        '}';
            }
        }

        enum State {NONE, MERGING}
    }

    static class InsertArray<T> {
        private final T[] items;
        private volatile int index;
        private final ReadWriteLock rwLock;

        public InsertArray(int capacity) {
            this.items = (T[]) new Object[capacity];
            this.rwLock = new ReentrantReadWriteLock();
        }

        public InsertArray(T[] items, int index) {
            this.items = items;
            this.index = index;
            this.rwLock = new ReentrantReadWriteLock();
        }

        int loIndex() {
            return (int) I_INDEX.getAcquire(this);
        }
    }

    private static final VarHandle I_INDEX;
    private static final VarHandle STATUS;
    private static final VarHandle MERGE;

    static {
        var l = MethodHandles.lookup();
        try{
            I_INDEX = l.findVarHandle(InsertArray.class, "index", int.class);
            STATUS = l.findVarHandle(DeleteArray.class, "status", Status.class);
            MERGE = l.findVarHandle(DeleteArray.class, "mergeCount", int.class);
        }catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
