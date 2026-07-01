package io.github.kusoroadeolu.cleap;


import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Comparator;
import java.util.Objects;

/*
A lock free ordered linked singly linked list set
States - marked (linearization point for removal), null key (means the pred node has been logically fully deleted), next pointer marked as a tombstone (node is going to be unlinked, don'item cas to it's next ptr)
The next node ideas were borrowed from fraser's thesis and the JDK Skip list map

The 2 main ideas here are helping and ownership checks for a node's next pointer using a cas
The main issue during physical unlinking is the issue of lost writes; mid deletion, a thread could insert a node, to a deleted node's next pointer
To solve this, we could approach this using stamped nodes through atomic stamped reference, to ensure a cas to a node's next pointer fails if a thread marks the stamp of the bit

However, this incurs high memory overhead when the workload is insert heavy(as we keep unneeded bits for nodes at rest), plus the api of atomic stamped ref is pretty annoying to use

To solve this, during deletions, we instead use a tombstone based approach in which during deletions, a thread cas's a dummy node to the next ref of a node, any thread that tries to cas to
that node's next ref, will see the dummy tombstone and instead backoff

This also allows for helping, a thread which encounters a deleted node, can help attach its tombstone and unlink it

// A(pred) - B(marked) - C(dummy) - R
// A(pred) - R (curr)


Duplicates are allowed in this list
*/

/*
* A - B - C - D - E
*
*
* */

/**
 * @author kusoroadeolu
 * */
public class LeaderList<T>{
    private final Node<WQNode<T>> left;
    private final Node<WQNode<T>> right;
    private final Comparator<T> comparator;

    public LeaderList(Comparator<T> comparator) {
        this.left = new SentinelNode<>();
        this.right = new SentinelNode<>();
        left.next = right;
        this.comparator = comparator;
    }

    public LeaderList() {
        this(null);
    }

    Node<WQNode<T>> insert(T t, int segment) {
        Objects.requireNonNull(t);
        var l = left;
        var r = right;
        Node<WQNode<T>> node = null;
        var comp = comparator;
        restartFromLeft: for (; ;) {
            var pred = l;
            var curr = pred.loNext();
            for (;;) {
                if (curr.isDummy()) continue restartFromLeft; // We need to restart from left, here, we could keep traversing forward ideally, if pred < item

                var s = curr.status;

                if (s == Status.MARKED) { //If curr is marked or unlinked try to help unlink
                    //Ensure a volatile read, hb guarantee that marking happens before physical deletion of a node
                    curr = helpUnlink(pred, curr); //Only shift curr
                    continue;
                }

                int res = compare(t, comp, curr, l, r);

                if (res > 0) pred = curr;
                else {
                    //Ensure we immediately set curr = next; backed by volatile write
                    //Otherwise if we fail, move backwards, don'item change pred, two things could've happened, pred was deleted (its dummy tombstone was introduced) or a new node greater than pred was added (this node could be > or < us)
                    //However since we're always > pred, we don'item need to restart from left
                    if (node == null) node = new Node<>(new WQNode<>(t, segment)); //Lazily initialize
                    node.spNext(curr);
                    if (pred.casNext(curr, node)) {
                        return node; //Linearization point
                    }
                }

                curr = pred.loNext();
            }
        }

    }


    /*
    * A B C D
    * c(v:1 s:2) a(v:2 tbd) b(v:3 tbd) d(v:5 s:2)
    * */
    Node<WQNode<T>> poll() {
        var l = left;
        var r = right;
        restartFromLeft: for (; ;) {
            var pred = l;
            var curr = l.loNext();

            for (;;) {
                if (curr.isDummy()) continue restartFromLeft;

                var s = curr.status;

                if (curr == r) return null;
                else if (s == null && curr.casMarked()) {
                    helpUnlink(pred, curr);
                    return curr;
                }

                pred = curr; curr = curr.loNext();
            }
        }
    }

    /*
    A - D
    b - c - a - d

    * */

    /*
    * Pretty strict but this is to ensure given a zero idx set of p [1 - 5]
    * a given thread item will never delete all values (2 - 5) at any point a value 1 has been inserted, it will retry and see one immediately its inserted
    * */
    public WQNode<T> removeFirstValidNode() {
        var l = left;
        var r = right;

        restartFromLeft: for (; ;) {
            var pred = l;
            var curr = pred.loNext();

            for (;;) {
                if (curr.isDummy()) //If we find a moving node, it means we should wait to prevent deleting a node fir
                    continue restartFromLeft; //If we find a dummy node, restart from left

                var s = curr.status;

                if (s == Status.MARKED) {
                    helpUnlink(pred, curr);
                    continue restartFromLeft;
                }

                if (curr == r) return null;
                else {
                    boolean removed = curr.casMarked();
                    helpUnlink(pred, curr);
                    if (removed) return curr.item;
                    else continue restartFromLeft;
                }
            }
        }
    }

    //To be inserted, to be removed
    //Returns the next lowest priority value in the list

    /*
    * a b c d
    *
    * given 4 values a b c d
    * from the strict delete method, if c and d have been deleted, that means there exist no scenario, where a and b exist if c and d are deleted
    * if a or b were inserted before c or d (since this is method protected by a lock, the scenario is impossible)
    * */
    MoveResult<T> insertAndReturnLargestSegmentNode(T toBeInserted, T toBeRemoved, int segment) {
        var startNode = insert(Objects.requireNonNull(toBeInserted) ,segment);

        var l = left;
        var r = right;
        var cmp = comparator;

        restartFromLeft: for (; ;) {
            var pred = startNode;
            var curr = pred.loNext();
            Node<WQNode<T>> newLlv = startNode;

            for (;;) {
                if (curr.isDummy())
                    continue restartFromLeft; //If we find a dummy node, restart from left

                var s = curr.status;
                var currSegment = curr.item.segment;

                int res = compare(toBeRemoved, cmp, curr, l, r);

                if (res < 0) return new MoveResult<>(newLlv, false);

                if (res == 0 && currSegment == segment) {
                    boolean marked = curr.casMarked();
                    helpUnlink(pred, curr);
                    return new MoveResult<>(newLlv, marked);
                }

                if (s == Status.MARKED) {
                    curr = helpUnlink(pred, curr);
                    continue;
                }


                if (curr.item.segment() == segment) newLlv = curr;


                pred = curr; curr = pred.loNext();
            }
        }
    }

    Node<WQNode<T>> findListLargest(Node<WQNode<T>> start, int segment) {
        var l = start == null ? left : start;
        var r = right;
        var newLlv = start;
        for (; ;) {
            var pred = l;
            var curr = pred.loNext();

            for (;;) {
                if (curr == r) {
                    return curr == left ? null : newLlv;
                }

                if (!curr.isDummy() && curr.item.segment == segment) newLlv = curr;

                pred = curr; curr = pred.loNext();
            }
        }
    }

    public void removeTBR(Node<WQNode<T>> seen) {
        seen.status = Status.MARKED;
        T t = seen.item.t();

        var l = left;
        var r = right;
        var cmp = comparator;
        restartFromLeft: for (; ;) {
            var pred = l;
            var curr = pred.loNext();

            for (;;) {

                if (!curr.isDummy() && compare(t, cmp , curr , l, r) < 0) return;

                if (curr == seen) {
                    if (pred.isDummy() || pred.isMarked()) continue restartFromLeft;
                    if (curr.loNext().isDummy()) return;
                    helpUnlink(pred, curr);
                    return;
                }


                pred = curr; curr = pred.loNext();
            }
        }
    }

    //Returns the next undead node
    private Node<WQNode<T>> helpUnlink(Node<WQNode<T>> pred, Node<WQNode<T>> curr) {
        Node<WQNode<T>> n;
        Node<WQNode<T>> d = null;

        for (;;) {
            n = curr.loNext();
            if (n.isDummy()) {
                n = n.loNext();
                break;
            } else {
                if (d == null) d = new Node<>(null, Status.MARKED);
                d.spNext(n);
                if (curr.casNext(n, d)) break; //Swap curr's next to a dummy node
            }
        }

        pred.casNext(curr, n); //try to link. failure is alright, another node has unlinked this , all we need is the new, probably unmarked (at this point) curr node
        return n;
    }


    int compare(T t, Comparator<T> cmp ,Node<WQNode<T>> curr, Node<WQNode<T>> l, Node<WQNode<T>> r) {
        return cmp == null ? compareWithoutComparator(t, curr, l, r) : compareWithComparator(t, cmp ,curr, l, r);
    }

    private int compareWithoutComparator(T t, Node<WQNode<T>> curr, Node<WQNode<T>> l, Node<WQNode<T>> r) {
        if (curr == r) return -1;       // right sentinel, stop
        if (curr == l) return 1; //cant happen
        return ((Comparable<T>) t).compareTo(curr.item.t());
    }

    private int compareWithComparator(T t, Comparator<T> comparator ,Node<WQNode<T>> curr, Node<WQNode<T>> l, Node<WQNode<T>> r) {
        if (curr == r) return -1;       // right sentinel, stop
        if (curr == l) return 1; //cant happen
        return comparator.compare(t, curr.item.t());
    }

    public void clear() {
        Node<WQNode<T>> seen;
        do {
            seen = left.loNext();
        }while (!left.casNext(seen, right));
    }


    static class Node<T> {
        final T item;
        volatile Status status;
        volatile Node<T> next;

        public Node(T item) {
            this.item = item;
        }

        public Node(T item, Status status) {
            this.item = item;
            this.status = status;
        }

        boolean isDummy() {
            return item == null;
        }

        public Node<T> loNext(){
            return (Node<T>) NEXT.getAcquire(this);
        }

        public boolean casNext(Node<T> seen, Node<T> ours) {
            return NEXT.compareAndSet(this, seen, ours);
        }

        public boolean isMarked(){
            return (Status) STATUS.getVolatile(this) == Status.MARKED;
        }


        public void spNext(Node<T> next) {
            NEXT.set(this, next);
        }

        Node(T item, Status status , Node<T> next) {
            this.item = item;
            this.status = status;
            NEXT.set(this, next); //Backed by a volatile write
        }

        public boolean casMarked() {
            return STATUS.compareAndSet(this, null, Status.MARKED);
        }

        @Override
        public String toString() {
            return item.toString() + " -> " + next.toString();
        }
    }

    //Left sentinel node
    private static class SentinelNode<T> extends Node<T> {


        public SentinelNode(T t, Status b , Node<T> next) {
            super(t, b ,next);
        }

        public SentinelNode() {
            this(null, null ,null);
        }

        @Override
        boolean isDummy() {
            return false;
        }

        @Override
        public String toString() {
            var n = next == null ? next : next.toString();
            return "SentinelNode -> " + n;
        }


    }


    @Override
    public String toString() {
        var l = left;
        var curr = l.loNext();
        var sb = new StringBuilder();
        while (curr != right) {
            sb.append(curr).append(" ");
            curr = curr.loNext();
        }
        return sb.toString();
    }

    private static final VarHandle NEXT;
    private static final VarHandle STATUS;

    static {
        var l = MethodHandles.lookup();
        try {
            NEXT = l.findVarHandle(Node.class, "next", Node.class);
            STATUS = l.findVarHandle(Node.class, "status", Status.class);
        }catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    enum Status {
        MARKED //logically added but need to move a node downwards
    }

    record WQNode<T> (T t, int segment) implements Comparable<T>{
        @Override
        public int compareTo(T o) {
            return ((Comparable<T>) t).compareTo(o);
        }
    }

    record MoveResult<T>(Node<WQNode<T>> item, boolean marked) {

    }
}