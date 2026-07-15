package io.github.kusoroadeolu.cleap.latest;

import io.github.kusoroadeolu.cleap.Heap;
import io.github.kusoroadeolu.cleap.latest.PaddedArenaEpochPQ.ArenaObject;

import java.lang.invoke.VarHandle;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

import static io.github.kusoroadeolu.cleap.latest.Utils.*;


class CircularArrayRPad<E> extends CircularArray<E> {

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

    public CircularArrayRPad(int capacity) {
        super(capacity);
    }
}

class ProducerLimitField<E> extends CircularArrayRPad<E> {
    long producerLimit;
    static final VarHandle P_LIMIT = fieldOffset(ProducerLimitField.class, "producerLimit", long.class);

    public ProducerLimitField(int capacity) {
        super(capacity);
    }

    public void soProducerLimit(long limit) {
        P_LIMIT.setRelease(this, limit);
    }

    public long lvProducerLimit() {
        return (long) P_LIMIT.getVolatile(this);
    }
}


class ProducerRPad<E> extends ProducerLimitField<E> {
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

    public ProducerRPad(int capacity) {
        super(capacity);
    }
}

class ProducerIndexField<E> extends ProducerRPad<E> {
    long producerIndex;
    static final VarHandle P_INDEX = fieldOffset(ProducerIndexField.class, "producerIndex", long.class);

    public ProducerIndexField(int capacity) {
        super(capacity);
    }

    public long lvProducerIndex() {
        return (long) P_INDEX.getVolatile(this);
    }

    public boolean casProducerIndex(long seen, long newIndex) {
        return P_INDEX.compareAndSet(this, seen, newIndex);
    }
}

class ProducerIndexRPad<E> extends ProducerIndexField<E> {
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

    public ProducerIndexRPad(int capacity) {
        super(capacity);
    }
}

class ConsumerIndexField<E> extends ProducerIndexRPad<E> {

    long consumerIndex;

    static final VarHandle C_INDEX = fieldOffset(ConsumerIndexField.class, "consumerIndex", long.class);

    public ConsumerIndexField(int capacity) {
        super(capacity);
    }

    public long lpConsumerIndex() {
        return consumerIndex;
    }

    public long lvConsumerIndex() {
        return (long) C_INDEX.getVolatile(this);
    }

    public void soConsumerIndex(long index) {
        C_INDEX.setRelease(this, index);
    }
}

class ConsumerIndexLPad<E> extends ConsumerIndexField<E> {
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

    public ConsumerIndexLPad(int capacity) {
        super(capacity);
    }
}

class MergeLimitField<E> extends ConsumerIndexLPad<E> {
    final long maxMergeLimit;
    long sortedIndex;

    public MergeLimitField(int capacity) {
        super(capacity);
        maxMergeLimit = Utils.mergeLimit(this.mask + 1);

    }

    public void spSortedIndex(long sIndex) {
        sortedIndex = sIndex;
    }

    public long lpMaxMergeLimit() {
        return maxMergeLimit;
    }

    public long lpSortedIndex() {
        return sortedIndex;
    }
}

class MergeLimitRPad<E> extends MergeLimitField<E> {
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


    public MergeLimitRPad(int capacity) {
        super(capacity);
    }
}

class SharedConsumerFields<E> extends MergeLimitRPad<E> {

    int state;
    final ArenaObject[] arena;
    static final VarHandle STATE = fieldOffset(SharedConsumerFields.class, "state", int.class);

    static final int ARENA_SIZE = NCPU;
    static final int MASK = ARENA_SIZE - 1;
    static final Object WAITER = new Object();
    static final Object AWAIT = new Object();
    static final Object NONE = new Object();
    static final int SPINS_PER_SLOT = 200;
    static final int MAX_SPINS = NCPU * SPINS_PER_SLOT;
    static final int BACKOFF_SPINS = 40;


    public SharedConsumerFields(int capacity) {
        super(capacity);
        int size = arenaSize();
        arena = new ArenaObject[size];
        for (int i = 0; i < size; ++i) {
            arena[i] = new ArenaObject();
        }
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
}

class SharedConsumerFieldsRPad<E> extends SharedConsumerFields<E> {
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

    public SharedConsumerFieldsRPad(int capacity) {
        super(capacity);
    }
}


public class PaddedArenaEpochPQ<E> extends SharedConsumerFieldsRPad<E> implements Heap<E> {

    public PaddedArenaEpochPQ(int capacity) {
        super(capacity);
    }

    public boolean offer(final E e) {
        Objects.requireNonNull(e);

        var buffer = this.buffer;
        var mask = this.mask;

        long pLimit = lvProducerLimit();
        long pIndex;
        long cIndex;

        for (;;) {
            pIndex = lvProducerIndex(); //could use an acquire read here
            if (pIndex >= pLimit) {
                cIndex = lvConsumerIndex();
                pLimit = cIndex + mask + 1; //Available slots in the buffer rn

                if (pIndex >= pLimit) {
                    return false; //no slots available
                }
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
                        ArenaObject o = arena[i];
                        if (o.loValue() == WAITER && o.casValue(WAITER, AWAIT)) {
                            E value = doPoll();
                            o.soValue(value == null ? NONE : value);
                        }
                    }
                    return elem;
                }finally {
                    release();
                }
            }


            int start = ThreadLocalRandom.current().nextInt();
            int arenaSize = arenaSize();
            inner: for (int step = 0, totalSpins = 0; (step < arenaSize) && (totalSpins < MAX_SPINS); step++) {
                int index = (step + start) & MASK;
                var arenaObject = arena[index];
                var seen = arenaObject.loValue();
                if (seen == null && arenaObject.casValue(null, WAITER)) {
                    int spins = 0;
                    for (int backoffSpins = 0; ;) {
                        seen = arenaObject.loValue();
                        if (seen != WAITER) {
                            Object elem;
                            while ((elem = arenaObject.loValue()) == AWAIT) Thread.onSpinWait();
                            arenaObject.soValue(null);
                            return elem == NONE ? null : (E) elem;
                        } else if ((spins >= SPINS_PER_SLOT) && arenaObject.casValue(WAITER, null)) {
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
        return 0;
    }

    E doPoll() {
        long cIndex = lpConsumerIndex();
        long sIndex = lpSortedIndex();
        long mask = this.mask;
        var buffer = this.buffer;
        E elem;
        if (cIndex == sIndex) { //If we've reached the end of the sorted index
            long pIndex = lvProducerIndex();
            if (pIndex == cIndex) return null;
            long newIndex = merge(cIndex, pIndex, mask, buffer);

            if (newIndex == -1) {
                var offset = offset(cIndex, mask);
                while ((elem = lvElem(buffer, offset)) == null) Thread.onSpinWait();

                spSortedIndex(sIndex + 1);
                soConsumerIndex(cIndex + 1);
                return elem;
            }

            spSortedIndex(newIndex);
        }

        var offset = offset(cIndex, mask);
        elem = lpElem(buffer, offset);
        spElem(buffer, offset, null);
        soConsumerIndex(cIndex + 1);
        return elem;
    }

    long merge(long cIndex, long pIndex, long mask ,Object[] buffer) {
        long mmg = Math.min(pIndex, cIndex + maxMergeLimit);

        int diff = (int) (mmg - cIndex);
        if (diff == 1) return -1;

        E[] array = (E[]) new Object[diff];
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

        Arrays.sort(array);

        j = cIndex;
        for (int i = 0; i < diff; ++i) {
            int offset = offset(j++, mask);
            spElem(buffer, offset, array[i]);
        }

        return mmg;
    }


    @Override
    public E peek() {
        return null;
    }

    public String toString() {
        return Arrays.toString(buffer);
    }


    static class ArenaObjectLPad {
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

    }

    static class ArenaObjectField extends ArenaObjectLPad{
        Object value;
        static final VarHandle VALUE = Utils.fieldOffset(ArenaObjectField.class, "value", Object.class);

        public boolean casValue(Object seen, Object to) {
            return VALUE.compareAndSet(this, seen, to);
        }

        public void soValue(Object value) {
            VALUE.setRelease(this, value);
        }

        public Object loValue() {
            return VALUE.getAcquire(this);
        }
    }

    static class ArenaObject extends ArenaObjectField{
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
    }
}
