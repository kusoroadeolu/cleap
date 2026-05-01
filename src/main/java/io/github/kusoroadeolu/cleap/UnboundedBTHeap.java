package io.github.kusoroadeolu.cleap;


// An unbounded node based heap

import java.util.Objects;

/*
* A binary tree based heap
*
* */
public class UnboundedBTHeap<T extends Comparable<T>> implements Heap<T> {

    private Node<T> head; //Starts as null
    private int size;

    static class Node<T extends Comparable<T>>{
        T value;
        Node<T> left;
        Node<T> right;

        public Node(T value) {
            this.value = Objects.requireNonNull(value);
        }

        public void setLeft(Node<T> left) {
            this.left = left;
        }

        public void setRight(Node<T> right) {
            this.right = right;
        }

        public int compareTo(Node<T> node) {
            return value.compareTo(node.value);
        }

        public Node<T> next() {
            if (left == null) return null;
            else if (right == null) return null;
            else if (left.compareTo(right) > 0) return left;
            else return right;
        }

        public Node<T> maxNext() {
            if (left == null) {
                return right;
            }

            if (right == null) {
                return left;
            }

            else if (left.compareTo(right) > 0) return left;
            else return right;
        }

        @Override
        public String toString() {
            return "Node[" +
                    "value=" + value +
                    ", left=" + left +
                    ", right=" + right +
                    ']';
        }
    }


    /*
    * For inserts
    * We start from the head, choosing based on comparison if we should select left or right
    *
    * We load prev as null and next as the head
    *
    * if next == null && prev == null -> no head, set head = node
    * if next == null, we've reached the end of the node, we then check if left or right is null, and we set prev.left/right = node
    * elif node.compareNext > 0, we set node.left = next
    *   if prev == null, that means we've only jumped one node and we're at the head, therefore, head = node
    *   else we compare next = prev.left/prev.right , then we set prev.left/prev.right -> node
    * */
    @Override
    public boolean insert(T t) {
        Node<T> node = new Node<>(t);
        Node<T> prev = null;
        Node<T> next = head;
        if (next == null) { //If there's no head
            head = node;
            return true;
        }

        while (true) {
            if (next == null) { //If we reach the end of this node, we check which node is null
                if (prev.left == null) prev.setLeft(node);
                else prev.setRight(node);
                return true;
            } else if (node.compareTo(next) > 0) {

                node.setLeft(next);
                if (prev == null) head = node;
                else {
                    if (next == prev.left) prev.setLeft(node);
                    else prev.setRight(node);
                }
                 //If prev is still null, that mean's we are the head

                return true;
            }

            prev = next;
            next = prev.next();
        }

    }


    @Override
    public T peek() {
        if (head == null) return null;
        else return head.value;
    }

    @Override
    public T head() {
        if (head == null) return null;
        T val = head.value;
        head = mergeNodes(head.left, head.right);
        --size;
        return val;
    }

    private Node<T> mergeNodes(Node<T> a, Node<T> b) {
        if (a == null) return b;
        if (b == null) return a;

        if (a.compareTo(b) >= 0) {
            a.right = mergeNodes(a.right, b);
            return a;
        } else {
            b.right = mergeNodes(b.right, a);
            return b;
        }
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public int capacity() {
        return Integer.MAX_VALUE;
    }

    @Override
    public String toString() {
        return head == null ? null : head.toString();

    }
}


