package io.github.kusoroadeolu.cleap.latest;


import io.github.kusoroadeolu.cleap.PriorityQueue;

import java.lang.invoke.VarHandle;
import java.util.Arrays;

import static io.github.kusoroadeolu.cleap.latest.Utils.*;


class SequencedArrayRPad<E> extends SequencedArray<E> {

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

    public SequencedArrayRPad(int capacity) {
        super(capacity);
    }
}

class MpmcProducerIndexField<E> extends SequencedArrayRPad<E> {
    long producerIndex;
    static final VarHandle P_INDEX = fieldOffset(MpmcProducerIndexField.class, "producerIndex", long.class);

    public MpmcProducerIndexField(int capacity) {
        super(capacity);
    }

    public long lvProducerIndex() {
        return (long) P_INDEX.getVolatile(this);
    }

    public boolean casProducerIndex(long seen, long newIndex) {
        return P_INDEX.compareAndSet(this, seen, newIndex);
    }
}

class MpmcProducerIndexRPad<E> extends MpmcProducerIndexField<E> {
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

    public MpmcProducerIndexRPad(int capacity) {
        super(capacity);
    }
}

class MpmcConsumerIndexField<E> extends MpmcProducerIndexRPad<E> {

    long consumerIndex;

    static final VarHandle C_INDEX = fieldOffset(MpmcConsumerIndexField.class, "consumerIndex", long.class);

    public MpmcConsumerIndexField(int capacity) {
        super(capacity);
    }

    public boolean casConsumerIndex(long seen, long to) {
        return C_INDEX.compareAndSet(this, seen, to);
    }

    public long lvConsumerIndex() {
        return (long) C_INDEX.getVolatile(this);
    }

    public void soConsumerIndex(long index) {
        C_INDEX.setRelease(this, index);
    }
}

class MpmcConsumerIndexLPad<E> extends MpmcConsumerIndexField<E> {
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

    public MpmcConsumerIndexLPad(int capacity) {
        super(capacity);
    }
}

class MpmcMergeLimitField<E> extends MpmcConsumerIndexLPad<E> {
    final long maxMergeLimit;


    public MpmcMergeLimitField(int capacity) {
        super(capacity);
        maxMergeLimit = Utils.mergeLimit(this.mask + 1);

    }



    public long lpMaxMergeLimit() {
        return maxMergeLimit;
    }
}

class MpmcMergeLimitRPad<E> extends MpmcMergeLimitField<E> {
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


    public MpmcMergeLimitRPad(int capacity) {
        super(capacity);
    }
}

class MpmcStatusField<E> extends MpmcMergeLimitRPad<E> {

    volatile Status status = new Status(0);
    static final VarHandle STATUS = fieldOffset(MpmcStatusField.class, "status", Status.class);


    public MpmcStatusField(int capacity) {
        super(capacity);
    }

    Status lvStatus() {
        return status;
    }

    void svStatus(Status status) {
        STATUS.setVolatile(this, status);
    }


    static class Status {
        final long sortedIndex;
        volatile State state = State.NONE;
        static final VarHandle STATE = fieldOffset(Status.class, "state", State.class);

        Status(long sortedIndex) {
            this.sortedIndex = sortedIndex;
        }

        boolean casState(State seen, State to) {
            return STATE.compareAndSet(this, seen, to);
        }

        State lvState() {
            return state;
        }

        State loState() {
            return (State) STATE.getAcquire(this);
        }

        void soState(State state) {
            STATE.setRelease(this, state);
        }

        @Override
        public String toString() {
            return "Status{" +
                    "sortedIndex=" + sortedIndex +
                    ", state=" + state +
                    '}';
        }
    }

    enum State {NONE, MERGING, MERGED}
}

class StatusFieldRPad<E> extends MpmcStatusField<E> {
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

    public StatusFieldRPad(int capacity) {
        super(capacity);
    }
}



public class MpmcEpochPQ<E> extends StatusFieldRPad<E> implements PriorityQueue<E> {

    public MpmcEpochPQ(int capacity) {
        super(capacity);
    }

    @Override
    public boolean add(E e) {
        var buffer = this.buffer;
        var sequence = this.sequence;
        var mask = this.mask;

        long pIndex;
        long seq;
        long cIndex = Long.MIN_VALUE;
        int offset;

        do{
            pIndex = lvProducerIndex();
            offset = Utils.offset(pIndex, mask);
            seq = lvSequence(sequence, offset);


            if (seq < pIndex) { //lagging consumer yet to update seq or no consumer
                long available = pIndex - (mask + 1);
                if (available >= cIndex && available >= (cIndex = lvConsumerIndex())) return false;
                else seq = pIndex + 1;
            }

        } while (seq > pIndex || !casProducerIndex(pIndex, pIndex + 1));

        spElem(buffer, Utils.offset(pIndex, mask), e);
        soSequence(sequence, offset, pIndex + 1);
        return true;
    }

    @Override
    public E poll() {
        var buffer = this.buffer;
        var sequence = this.sequence;
        var mask = this.mask;

        long cIndex;
        long seq;
        long expected;
        long pIndex = -1;
        int offset;
        long sortedIndex;
        Status s = lvStatus();

        for (;;) {
            cIndex = lvConsumerIndex();
            offset = Utils.offset(cIndex , mask);
            seq = lvSequence(sequence, offset);
            expected = cIndex + 1; //seq at this offset should be exactly +1 of the offset value
            sortedIndex = s.sortedIndex;

            if (cIndex >= sortedIndex) { // '>' status is stale, == 'try merge' status
                if (cIndex == (pIndex = lvProducerIndex())) return null;

                if (s.lvState() == State.NONE && s.casState(State.NONE, State.MERGING)) {
                    long sIndex = merge(cIndex, pIndex, mask, buffer, sequence);
                    Status newS = new Status(sIndex);
                    svStatus(newS);
                    s.soState(State.MERGED);
                    s = newS;
                    continue;
                } else while (s.loState() != State.MERGED){
                    Thread.onSpinWait();
                }

                s = lvStatus();
                continue;
            }

            if (seq < expected) { //producer might not have updated seq yet or queue is empty
                if (cIndex >= pIndex && cIndex == (pIndex = lvProducerIndex())) return null;
                else seq = expected + 1;
            }


            if (seq == expected && casConsumerIndex(cIndex, cIndex + 1)) break;
        }

        int elemOffset = Utils.offset(cIndex, mask);
        E elem = lpElem(buffer, elemOffset);
        spElem(buffer, elemOffset, null);
        soSequence(sequence, offset, cIndex + mask + 1);
        return elem;
    }

    @Override
    public int size() {
        return (int) (lvProducerIndex() - lvConsumerIndex());
    }

    long merge(long cIndex, long pIndex, long mask ,Object[] buffer, long[] sequence) {
        long mmg = Math.min(pIndex, cIndex + maxMergeLimit);
        int diff = (int) (mmg - cIndex);

        if (diff == 1) return mmg;

        E[] array = (E[]) new Object[diff];
        long j = cIndex;
        for (int i = 0; i < diff; ++i) {
            long expected = j + 1;
            int offset = offset(j, mask);
            long seq = lvSequence(sequence, offset);
            if (seq != expected) {
                while (lvSequence(sequence, offset) != expected) {
                    Thread.onSpinWait();
                }
            }

            array[i] = lpElem(buffer, offset);
            j++;
        }

        Arrays.sort(array);

        j = cIndex;
        for (int i = 0; i < diff; ++i) {
            int offset = offset(j++, mask);
            spElem(buffer, offset, array[i]);
        }
        return mmg;
    }
}
