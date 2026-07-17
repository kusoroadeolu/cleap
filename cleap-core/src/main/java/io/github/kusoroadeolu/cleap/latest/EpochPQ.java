package io.github.kusoroadeolu.cleap.latest;

import io.github.kusoroadeolu.cleap.PriorityQueue;

import java.lang.invoke.VarHandle;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReferenceArray;

import static io.github.kusoroadeolu.cleap.latest.Utils.*;


class EpochSharedConsumerFields<E> extends SegmentLimitRPad<E> {

    int state;
    final AtomicReferenceArray<Object> arena;
    static final VarHandle STATE = fieldOffset(EpochSharedConsumerFields.class, "state", int.class);

    static final int ARENA_SIZE = Utils.roundToPowerOfTwo(NCPU);
    static final int MASK = ARENA_SIZE - 1;
    static final Object WAITER = new Object();
    static final Object AWAIT = new Object();
    static final Object NONE = new Object();
    static final int SPINS_PER_SLOT = 200;
    static final int MAX_SPINS = Math.min(2500, ARENA_SIZE * SPINS_PER_SLOT);
    static final int BACKOFF_SPINS = 40;


    public EpochSharedConsumerFields(int capacity) {
        super(capacity);
        arena = new AtomicReferenceArray<>(arenaSize());
    }

    public boolean acquire() {
        return isFree() && (int) STATE.getAndAdd(this, 1) == 0;
    }

    public static int arenaSize() {
        return ARENA_SIZE;
    }

    public void release() {
        STATE.setRelease(this, 0);
    }

    public boolean isFree() {
        return (int) STATE.getAcquire(this) == 0;
    }

    public Object loArenaElem(AtomicReferenceArray<Object> arena, int index) {
        return arena.getAcquire(index);
    }

    public boolean casArenaElem(AtomicReferenceArray<Object> arena, int index, Object seen , Object to) {
        return arena.compareAndSet(index, seen, to);
    }

    public void soArenaElem(AtomicReferenceArray<Object> arena, int index, Object o) {
        arena.setRelease(index, o);
    }
}

class EpochSharedConsumerFieldsRPad<E> extends EpochSharedConsumerFields<E> {
    byte b000,b001,b002,b003,b004,b005,b006,b007;//  8b
    byte b010,b011,b012,b013,b014,b015,b016,b017;// 16b
    byte b020,b021,b022,b023,b024,b025,b026,b027;// 24b
    byte b030,b031,b032,b033,b034,b035,b036,b037;// 32b
    byte b040,b041,b042,b043,b044,b045,b046,b047;// 40b
    byte b050,b051,b052,b053,b054,b055,b056,b057;// 48b
    byte b060,b061,b062,b063,b064,b065,b066,b067;// 56b
    byte b070,b071,b072,b073,b074,b075,b076,b077;// 64b
    byte b100,b101,b102,b103,b104,b105,b106,b107;// 72b
    byte b110,b111,b112,b113,b114,b115,b116,b117;// 80b
    byte b120,b121,b122,b123,b124,b125,b126,b127;// 88b
    byte b130,b131,b132,b133,b134,b135,b136,b137;// 96b
    byte b140,b141,b142,b143,b144,b145,b146,b147;//104b
    byte b150,b151,b152,b153,b154,b155,b156,b157;//112b
    byte b160,b161,b162,b163,b164,b165,b166,b167;//120b
    byte b170,b171,b172,b173,b174,b175,b176,b177;//128b

    public EpochSharedConsumerFieldsRPad(int capacity) {
        super(capacity);
    }
}


public class EpochPQ<E> extends EpochSharedConsumerFieldsRPad<E> implements PriorityQueue<E> {

    public EpochPQ(int capacity) {
        super(capacity);
    }

    public boolean offer(final E e) {
        Objects.requireNonNull(e);

        var buffer = this.buffer;
        var mask = this.mask;
        long capacity = mask + 1;

        long pLimit = lvProducerLimit();
        long pIndex;
        long cIndex;

        for (;;) {
            pIndex = lvProducerIndex();
            if (pIndex >= pLimit) {
                cIndex = lvConsumerIndex();
                pLimit = cIndex + capacity; //Available slots in the buffer rn

                if (pIndex >= pLimit) return false; //no slots available
                //greater than check here is to take care of the race where a stalled / late arriving thread stores an old plimit
                else soProducerLimit(pLimit);
            }


            if (casProducerIndex(pIndex, pIndex + 1)) break;
        }

        int offset = offset(pIndex, mask);
        soElem(buffer, offset , e);
        return true;
    }


    @Override
    public boolean add(E e) {
        return offer(e);
    }

    public E poll() {
        var arena = this.arena;
        for (;;) {
            if (acquire()) {
                try {
                    E elem = doPoll();
                    for (int i = 0; i < arenaSize(); ++i) {
                        Object o = loArenaElem(arena, i);
                        if (o == WAITER && casArenaElem(arena, i, WAITER, AWAIT)) {
                            E value = doPoll();
                            soArenaElem(arena, i, value == null ? NONE : value);
                        }
                    }
                    return elem;
                }finally {
                    release();
                }
            }


            int start = ThreadLocalRandom.current().nextInt();
            int arenaSize = arenaSize();
            inner: for (int step = 0, totalSpins = 0; (step < arenaSize) && (totalSpins < MAX_SPINS) && isFree(); step++) {
                int index = (step + start) & MASK;
                var seen = loArenaElem(arena, index);
                if (seen == null && casArenaElem(arena, index, null, WAITER)) {
                    int spins = 0;
                    for (int backoffSpins = 0; ;) {
                        seen = loArenaElem(arena, index);
                        if (seen != WAITER) {
                            Object elem;
                            while ((elem = loArenaElem(arena, index)) == AWAIT) Thread.onSpinWait();
                            soArenaElem(arena, index, null);
                            return elem == NONE ? null : (E) elem;
                        } else if ((spins >= SPINS_PER_SLOT) && casArenaElem(arena, index, WAITER, null)) {
                            totalSpins += spins;
                            continue inner;
                        }

                        while (++backoffSpins <= BACKOFF_SPINS) Thread.onSpinWait(); //avoid repeated spins on index to prevent cache line thrashing

                        spins += backoffSpins;
                        backoffSpins = 0;
                    }
                }
            }

        }
    }


    @Override
    public int size() {
        return (int) (lvProducerIndex() - lvConsumerIndex());
    }

    E doPoll() {
        long cIndex = lpConsumerIndex();
        long sIndex = lpSegmentEndIndex();
        long mask = this.mask;
        var buffer = this.buffer;
        E elem;
        if (cIndex == sIndex) { //If we've reached the end of the sorted index
            long pIndex = lvProducerIndex();
            if (pIndex == cIndex) return null;
            long newIndex = segmentSort(cIndex, pIndex, mask, buffer);

            if (newIndex == -1) {
                var offset = offset(cIndex, mask);
                while ((elem = lvElem(buffer, offset)) == null) Thread.onSpinWait();

                spSegmentEndIndex(sIndex + 1);
                soConsumerIndex(cIndex + 1);
                return elem;
            }

            spSegmentEndIndex(newIndex);
        }

        var offset = offset(cIndex, mask);
        elem = lpElem(buffer, offset);
        spElem(buffer, offset, null);
        soConsumerIndex(cIndex + 1);
        return elem;
    }

    long segmentSort(long cIndex, long pIndex, long mask , Object[] buffer) {
        long mmg = Math.min(pIndex, cIndex + segmentLimit);

        int diff = (int) (mmg - cIndex);
        if (diff == 1) return -1;

        Object[] array = this.sortBuffer;

        long j = cIndex;
        for (int i = 0; i < diff; ++i) {
            int offset = offset(j++, mask);
            E elem = lvElem(buffer, offset);
            if (elem == null) {
                while ((elem = lvElem(buffer, offset)) == null) {
                    Thread.onSpinWait();
                }
            }

            array[i] = elem;
        }

        Arrays.sort(array, 0, diff);

        j = cIndex;
        for (int i = 0; i < diff; ++i) {
            int offset = offset(j++, mask);
            spElem(buffer, offset, array[i]);
            array[i] = null;
        }

        return mmg;
    }

    public String toString() {
        return Arrays.toString(buffer);
    }
}