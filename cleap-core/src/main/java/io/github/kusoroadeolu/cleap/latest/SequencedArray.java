package io.github.kusoroadeolu.cleap.latest;

import java.lang.invoke.VarHandle;
import java.util.Arrays;

import static io.github.kusoroadeolu.cleap.latest.Utils.roundToPowerOfTwo;

class SequencedArrayLPad {
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
}

public class SequencedArray<E> extends SequencedArrayLPad {

    final Object[] buffer;
    final long[] sequence; //Tracks offsets that are free for a consumer or producer to claim
    final long mask;
    static final VarHandle BUFFER = Utils.objectArrayOffset();
    static final VarHandle SEQUENCE = Utils.longArrayOffset();


    public SequencedArray(int capacity) {
        int actualCapacity = roundToPowerOfTwo(capacity);
        mask = actualCapacity - 1;
        buffer = new Object[actualCapacity];
        sequence = new long[actualCapacity];

        for (int i = 0; i < actualCapacity; ++i) {
            SEQUENCE.setRelease(sequence, i, i);
        }
    }

    public void soElem(Object[] buf, int index , Object o) {
        BUFFER.setRelease(buf, index, o);
    }

    public void spElem(Object[] buf, int index , Object o) {
        BUFFER.set(buf, index, o);
    }

    public E lvElem(Object[] buf, int index) {
        return (E) BUFFER.getVolatile(buf, index);
    }

    public long lvSequence(long[] sequence, int index) {
        return (long) SEQUENCE.getVolatile(sequence, index);
    }

    public void soSequence(long[] sequence, int index, long value) {
        SEQUENCE.setRelease(sequence, index, value);
    }

    public E lpElem(Object[] buf, int index) {
        return (E) BUFFER.get(buf, index);
    }

    public String toString() {
        return Arrays.toString(buffer);
    }
}
