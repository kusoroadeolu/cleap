package io.github.kusoroadeolu.cleap.latest;

import java.util.Objects;

public class EpochPQ<E> extends SpinLockRPad<E>{

    public EpochPQ(int capacity) {
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

                if (pIndex >= pLimit) return false; //no slots available
                else soProducerLimit(pLimit);
            }


            if (casProducerIndex(pIndex, pIndex + 1)) break;
        }


        soElem(buffer, Utils.offset(pIndex, mask), e);
        return true;
    }


    //Todo add merge logic
    public E poll() {
        long cIndex = lpConsumerIndex();
        var offset = Utils.offset(cIndex, mask);
        var buffer = this.buffer;
        E elem = lvElem(buffer, offset);

        if (elem == null) {
            if (cIndex == lvProducerIndex()) return null;
            else {
                for (;;) {
                    if ((elem = lvElem(buffer, offset)) == null) Thread.onSpinWait();
                    else break;
                }
            }
        }

        spElem(buffer, offset, null);
        soConsumerIndex(cIndex + 1);
        return elem;
    }
}
