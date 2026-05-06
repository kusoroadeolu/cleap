package io.github.kusoroadeolu.cleap;



import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

///Based on this [paper](https://dada.cs.washington.edu/research/tr/1994/12/UW-CSE-94-12-07.pdf)
/// This bounded max heap
///
///
/// For both modification operations: `add` and `poll`,
/// they are divided into a **preliminary** phase and a **sifting** phase (sifting always occurs from the top to bottom)
///
/// The preliminary phase is always protected by a shared state 0 -> 1 _for a thread to start a prelim phase for its operation_, 1 -> 0 _for a thread to end its prelim phase for its operation_
///
/// ## Poll
/// Assuming no inactive operations are encountered
/// - Preliminary phase: Start your prelim phase, store the head of the heap, read the bottom most value of the heap, mark it as inactive
///  and swap the head of the heap to the bottom most value, continue till idx, end
/// - Sifting phase: We sift the new value downwards
///
/// ## Add/Offer
/// Assuming no inactive operations are encountered
/// - Preliminary phase: If start your prelim phase,
///     <br> if
///         <br> &ensp;size == capacity, end your prelim phase and return false
///     <br> else
///         <br> &ensp;create a new entry at idx == size with an insert swap operation pointing to the root then incr size,
///     <br> end your prelim phase
/// otherwise
///     Help the thread
///
public class WaitFreeHeap<T extends Comparable<T>> implements Heap<T>{
    private final int capacity;
    private final HeapEntry<T>[] entries;
    private final AtomicReference<HeapStatus<T>> heapStatus;
    /*
     * Try acquire heap entry. If we can't acquire heap entry. Check heap entry, if the operation is an insert, help the insert operation
     *
     * */

    @SuppressWarnings("unchecked")
    public WaitFreeHeap(int capacity) {
        int pow2 = 1 << (32 - Integer.numberOfLeadingZeros(capacity - 1));
        entries = (HeapEntry<T>[]) new HeapEntry[capacity = (pow2 - 1)];
        this.capacity = capacity;
        this.heapStatus = new AtomicReference<>(new HeapStatus<>(null, HeapStatus.ADD, -1 ,0, -1));
        for (int i = 0; i < capacity; ++i) {
            entries[i] = new HeapEntry<>(null, i);
        }

    }

    void applyAddPreliminaryPhase(HeapEntry<T>[] es, HeapStatus<T> status) {
        var e = es[status.idxAt];
        var ar = e.value;

        //If the entry value null, we try to cas from null
        ar.compareAndSet(null, status.value); //We use a cas here because at this ;

        var root =  es[0];
        e.opId.compareAndSet(-1, status.opId);
        if (status.idxAt > 0) e.trackingPtr.compareAndSet(null, root.fix()); //try swap from null to root. In the case someone has already set this to root, we don't want to overwrite
        status.loApply();
    }

    @Override
    public boolean add(T t) {
        Objects.requireNonNull(t);
        var h = heapStatus;
        HeapStatus<T> current = h.get();
        HeapEntry<T>[] es = entries;
        HeapStatus<T> ours = current.fix(t, HeapStatus.ADD);
        for (; ;) {
            if (!h.compareAndSet(current, ours)) { //If current has changed
                current = h.get();
                if (current.idxAt >= capacity) return false; //We can't add either so just leave

                //If heap status thread needs help
                if (!current.isApplied()) applyAddPreliminaryPhase(es, current);
                ours = current.fix(t, HeapStatus.ADD);
            }else {

                if (ours.idxAt >= capacity) {
                    h.set(current); //Set to the old value
                    return false;
                }

                if (!ours.isApplied()) applyAddPreliminaryPhase(es, ours);
                break;
            }
        }

        int idx = ours.idxAt;
        HeapEntry<T> entry = es[idx];
        addSift(es, entry, ours.opId);
        return true;
    }


    //Dummy logic for now, will change later
    ClonedHeapEntry<T> nextChild(HeapEntry<T>[] es, int idx, int leafIdx){
        if (idx + 1 >= capacity) return null;  // check the incremented value
        return es[++idx].fix();
    }

    private void addSift(HeapEntry<T>[] es, HeapEntry<T> ours, int opId) {
        for (;;) {
            if(help(es, ours, opId ,false)) break;
            //If we're helping and o is already available just return
        }
    }

    boolean help(HeapEntry<T>[] es, HeapEntry<T> ours, int opId, boolean help){
        var ourCloned = ours.fix();
        if (ourCloned.idx == 0) return true; //If we're the parent just return

        var oStatus = ourCloned.status;
        var parentCloned = ourCloned.entry();

        if (parentCloned == null || ourCloned.opId != opId) return true;

        var pStatus = parentCloned.status();


        if (help && oStatus == HeapEntry.AVAILABLE) return false;

        //If we didnt swap this, retry

        HeapEntry<T> parent = es[parentCloned.idx];
        var pValue = parentCloned.value();
        var oValue = ourCloned.value();



        if (pStatus == HeapEntry.AVAILABLE) {
            if(oValue.compareTo(pValue) > 0) { //If we can swap our parent, we can swap ourselves, otherwise we can retry
                if ((ours.casStatus(HeapEntry.AVAILABLE, HeapEntry.NEEDS_HELP)) || oStatus == HeapEntry.NEEDS_HELP) {
                    parent.value.compareAndSet(pValue, oValue);
                    ours.casStatus(HeapEntry.NEEDS_HELP, HeapEntry.PARENT_SWAPPED); //Retry

                    //We need to re-read parent
                    swapDesc(es,  ours, ourCloned,  parentCloned, pValue, oValue);
                }else if (oStatus == HeapEntry.PARENT_SWAPPED) swapDesc(es, ours, ourCloned, parentCloned, pValue, oValue);

            } else {
                var next = nextChild(es, parentCloned.idx, -1);
                if (next == null || next.idx() >= ourCloned.idx()){
                    ours.opId.compareAndSet(ourCloned.opId, -1);
                    return !help;

                }
                ours.trackingPtr.compareAndSet(parentCloned, next);
            }
            //If we fail to swap parent
        } else if (pStatus == HeapEntry.PARENT_SWAPPED) {
            swapDesc(es, parent ,parentCloned, parentCloned.entry(), parentCloned.entry().value(), parentCloned.value());
            ours.trackingPtr.compareAndSet(parentCloned, parent.fix());
        } else {
            help(es, parent,  parentCloned.opId(),true);
            ours.trackingPtr.compareAndSet(parentCloned, parent.fix());
        }

        return false;
    }

    private void swapDesc(HeapEntry<T>[] es, HeapEntry<T> ours ,ClonedHeapEntry<T> ourCloned, ClonedHeapEntry<T> parentCloned, T pValue, T oValue) {
        ours.value.compareAndSet(oValue, pValue);
        var nextCloned = nextChild(es, parentCloned.idx, ourCloned.idx);
        ours.trackingPtr.compareAndSet(parentCloned, nextCloned); //Doesn't matter if we fail here, another thread mightve written here , we'll still re read the value fresh next iteration

        if (nextCloned == null || nextCloned.idx >= ourCloned.idx) {
            ours.opId.compareAndSet(ourCloned.opId, -1);
        }

        ours.casStatus(HeapEntry.PARENT_SWAPPED, HeapEntry.AVAILABLE); //Set available swap the child

    }




    private int parentIndex(int childIdx) {
        return (childIdx - 1 ) / 2;
    }

    private int childIdx(int parentIdx, int by) {
        return 2 * parentIdx + by;
    }

    @Override
    public T poll() {
        return null;
    }

    @Override
    public T peek() {
        return entries[0].value.get(); //For tests
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public int capacity() {
        return 0;
    }

    @Override
    public String toString() {
        var ls = Arrays.stream(entries).map(h -> h.value.get() + " " + h.status.get()).toList();
        return ls.toString();
    }

    static class HeapStatus<T> {
        final int size;
        final int idxAt;
        final int operation; //POLL, OFFER/ADD
        final int opId;
        final T value;
        final AtomicBoolean applied;
        static final int ADD = 1;
        static final int POLL = 2;


        public HeapStatus(T value, int operation, int opId ,int size, int idxAt) {
            this.value = value;
            this.operation = operation;
            this.size = size;
            this.idxAt = idxAt;
            this.opId = opId;
            this.applied = new AtomicBoolean(false);
        }

        public boolean isApplied() {
            return applied.getAcquire();
        }

        public HeapStatus<T> fix(T value, int operation) {
            int idx = operation == ADD ? (idxAt + 1) : idxAt;
            int size = operation == ADD ? this.size + 1 : this.size - 1;
            return new HeapStatus<>(value, operation, opId + 1, size, idx);
        }

        public void loApply() {
            applied.setRelease(true);
        }

        @Override
        public String toString() {
            return "HeapStatus{" +
                    "size=" + size +
                    ", idxAt=" + idxAt +
                    ", operation=" + operation +
                    ", opId=" + opId +
                    ", value=" + value +
                    ", applied=" + applied +
                    '}';
        }
    }



    static class HeapEntry<T> {
        final AtomicReference<T> value;
        final AtomicReference<ClonedHeapEntry<T>> trackingPtr; // the breadcrumb
        final AtomicInteger status;
        final int idx;
        final AtomicInteger opId;

        static final int AVAILABLE = 1;
        static final int NEEDS_HELP = 2; //Needs help
        static final int PARENT_SWAPPED = 3;
        static final int UNAVAILABLE = 4;

        public HeapEntry(ClonedHeapEntry<T> pointTo, int idx) { //All point to the root initially
            this.trackingPtr = new AtomicReference<>(pointTo);
            this.value = new AtomicReference<>();
            this.status = new AtomicInteger(AVAILABLE);
            this.idx = idx;
            this.opId = new AtomicInteger(-1);
        }

        public ClonedHeapEntry<T> fix() {
            return new ClonedHeapEntry<>(status.get(), value.get(), idx, opId.get(), trackingPtr.get());
        }

        boolean casStatus(int olds, int news) {
            return status.compareAndSet(olds, news);
        }

        ClonedHeapEntry<T> getClonedEntry(){
            return trackingPtr.get();
        }

        @Override
        public String toString() {
            return "HeapEntry[" +
                    "value=" + value +
                    ", status=" + status +
                    ", idx=" + idx +
                    ']';
        }
    }

    record ClonedHeapEntry<T>(int status, T value, int idx, int opId, ClonedHeapEntry<T> entry) {

    }

    //Our value -> value
    /*
     *   3 -> Parent swap op
     *  / \
     * 2   5 -> Child swap op
     *
     *
     * Child swap operation, if we havent swapped a child operation yet,
     * */

    static void main() throws InterruptedException {


    }
}