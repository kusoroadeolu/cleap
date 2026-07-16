package io.github.kusoroadeolu.cleap.dualarray;

import io.github.kusoroadeolu.cleap.PriorityQueue;
import io.github.kusoroadeolu.cleap.dualarray.LBBoundedPQ.DeleteArray.Status;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static io.github.kusoroadeolu.cleap.dualarray.LBBoundedPQ.DeleteArray.State.*;

public class LBBoundedPQ<T> implements PriorityQueue<T> {
    private final InsertArray<T> insertArray;
    private final Comparator<T> nullReverseComparator; //Packs the bottom of an array with nulls
    private DeleteArray<T> deleteArray;
    private final int capacity;
    private final int maxDaCapacity;
    private final int slack;

    public LBBoundedPQ(int capacity) {
        this.capacity = capacity;
        this.maxDaCapacity = Math.max(1, (int) (0.1 * capacity));
        this.insertArray = new InsertArray<>(capacity);
        this.deleteArray = new DeleteArray<>(0);
        this.slack = Math.max(1, (int) (0.1 * maxDaCapacity));
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

    @Override
    public boolean add(T t) {
        var ia = insertArray;
        var lock = ia.rwLock.readLock();
        int capacity = this.capacity;
        lock.lock();
        try {
            var da = deleteArray;
            int iIndex = da.loIIndex();

            if (iIndex == capacity) return false;

            Status daStatus;

            for (;;) {
                //70, 30
                //71, 30 -0
                daStatus = da.status;
                int daIndex = daStatus.dIndex; // (0, 1, 2, 3), if index is on 2, that means value at 2 hasn't been consumed yet
                int daSize = da.capacity; //10 - 5 = 5
                int next = iIndex + 1;
                int total = iIndex + (daSize - daIndex);

                if (total >= capacity) return false;
                else if (I_INDEX.compareAndSet(da, iIndex, next)) break; //linearization point an item has been inserted into the array
                else iIndex = da.loIIndex(); //re-read with acquire, write to array can't be reordered as the write is dependent on index
            }


            ia.items[iIndex] = t;
            if (da.capacity == 0 || daStatus.state != DeleteArray.State.NONE) return true; //nothing in the D.A or da is freezing, we'll get merged soon
            var lastDaIndex = da.capacity - 1;
            int prio = compare(t, da.items[lastDaIndex]);
            if (prio < 0) SLACK_COUNT.getAndAdd(da, 1); //Writes to items is made visible by da status read
            return true;
        }finally {
            lock.unlock();
        }
    }

    @Override
    public T poll() {
        outer: for (;;) {
                DeleteArray<T> da = (DeleteArray<T>) D_ARR.getAcquire(this);
                int daCapacity;
                int seenIndex;
                int daIndex;
                Status daStatus;
                var ia = insertArray;

                for (;;) {
                    daStatus = da.status; //Volatile read (is merged, we'll always see the new delete array)

                    daCapacity = da.capacity;
                    var state = daStatus.state;

                    if (state == MERGING) {
                        for (;;) {
                            if (da.status.state == MERGED) continue outer;
                            Thread.onSpinWait();
                        }
                    } else if (state == MERGED) continue outer;

                    seenIndex = da.loIIndex();
                    daIndex = daStatus.dIndex;
                    boolean isEmpty = daIndex == daCapacity;

                    if (isEmpty && seenIndex == 0) return null; //DA and IA are empty (will never be zero in the presence of merging deletes)

                    boolean shouldMerge = isEmpty || da.slackCount >= slack;
                    // if , reassign daStatus if the cas succeeds

                    Status s;
                    if (shouldMerge) {
                        if (da.casStatus(daStatus, (s = new Status(daIndex, MERGING)))) {
                            daStatus = s;
                            break;
                        } else {
                            for (;;) {
                                if (da.status.state == MERGED) continue outer;
                                Thread.onSpinWait();
                            }
                        }

                    } else if (da.casStatus(daStatus, new Status(daIndex + 1, NONE))) {
                        return da.items[daIndex];
                    }
                }

                return merge(ia, da, daStatus);
            }


    }


    T merge(InsertArray<T> ia,  DeleteArray<T> da, Status daStatus) {
        var lock = ia.rwLock.writeLock();
        lock.lock();
        try {
            var iItems = ia.items;
            var dItems = da.items;
            var cmp = nullReverseComparator;
            var maxDaCapacity = this.maxDaCapacity;
            int iIndex = da.lpIndex(); //start index for insert array (val at this index is always null)
            int dIndex = daStatus.dIndex; //start index for delete array
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
                --iIndex;
            }

            I_INDEX.set(newDa, (iIndex = Math.max(0, iIndex)));

            T item = newDa.items[0];
            //Writes to this array
            //For inserts backed by exclusive lock, otherwise for deletes and inserts, backed by set release
            //However, they can't modify this as it is prevented by the merging flag
            D_ARR.setRelease(this, newDa);
            da.status = new Status(iIndex, MERGED);
            return item;
        }finally {
            lock.unlock();
        }
    }


    public List<T> drain() {
        var da = (DeleteArray<T>) D_ARR.getAcquire(this);
        var ia = insertArray;
        List<T> list = new ArrayList<>();

        ia.rwLock.readLock().lock();
        try {
            for (int i = da.status.dIndex; i < da.capacity; ++i) {
                list.add(da.items[i]);
            }

            int upto = da.iIndex;
            for (int i = 0; i < upto; ++i) {
                list.add(ia.items[i]);
            }

        }finally {
            ia.rwLock.readLock().unlock();
        }

        return list;
    }


    public int slackCount() {
        return deleteArray().slackCount;
    }

    InsertArray<T> insertArray() {
        return insertArray;
    }

    DeleteArray<T> deleteArray() {
        return deleteArray;
    }

    int compare(T a, T b) {
        return ((Comparable<T>) a).compareTo(b);
    }

    @Override
    public int size() {
        for (;;) {
            var da = (DeleteArray<T>) D_ARR.getVolatile(this);
            var s = da.status;
            if (s.state == MERGED) continue;
            return da.loIIndex() + (da.size() - da.status.dIndex);
        }
    }

    @Override
    public void clear() {
        var ia = insertArray;
        ia.rwLock.writeLock().lock();
        try {
            for (;;) {
                if (poll() == null) break;
            }
        }finally {
            ia.rwLock.writeLock().unlock();
        }
    }

    static class DeleteArray<T> {
        final T[] items;
        final int capacity;
        volatile Status status = new Status(0, NONE);
        volatile int slackCount;
        volatile int iIndex;

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
            return capacity - status.dIndex;
        }

        boolean casStatus(Status s, Status newS) {
            return STATUS.compareAndSet(this, s, newS);
        }

        int loIIndex() {
            return (int) I_INDEX.getAcquire(this);
        }

        int lpIndex() {
            return (int) I_INDEX.get(this);
        }

        static class Status {
            private final int dIndex;
            private final State state;

            public Status(int dIndex, State state) {
                this.dIndex = dIndex;
                this.state = state;
            }

            @Override
            public String toString() {
                return "Status{" +
                        "index=" + dIndex +
                        ", state=" + state +
                        '}';
            }
        }

        enum State {NONE, MERGING, MERGED}
    }

    static class InsertArray<T> {
        private final T[] items;
        private final ReadWriteLock rwLock;

        public InsertArray(int capacity) {
            this.items = (T[]) new Object[capacity];
            this.rwLock = new ReentrantReadWriteLock();
        }
    }

    private static final VarHandle I_INDEX;
    private static final VarHandle STATUS;
    private static final VarHandle D_ARR;
    private static final VarHandle SLACK_COUNT;

    static {
        var l = MethodHandles.lookup();
        try{
            I_INDEX = l.findVarHandle(DeleteArray.class, "iIndex", int.class);
            STATUS = l.findVarHandle(DeleteArray.class, "status", Status.class);
            D_ARR = l.findVarHandle(LBBoundedPQ.class, "deleteArray", DeleteArray.class);
            SLACK_COUNT = l.findVarHandle(DeleteArray.class, "slackCount", int.class);
        }catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
