package algo;

import java.util.NoSuchElementException;

public class LinkedList<T> {

    private static class Node<T> {
        T value;
        Node<T> prev, next;
        Node(T value) { this.value = value; }
    }

    private Node<T> head;
    private Node<T> tail;
    private int size;

    public long comparisons = 0;
    public long accesses = 0;

    public int size() { return size; }
    public boolean isEmpty() { return size == 0; }

    public void resetCounters() {
        comparisons = 0;
        accesses = 0;
    }

    private void checkIndexForAccess(int index) {
        if (index < 0 || index >= size)
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
    }

    private void checkIndexForInsert(int index) {
        if (index < 0 || index > size)
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
    }

    private Node<T> nodeAt(int index) {
        Node<T> cur;
        if (index < size / 2) {
            cur = head;
            for (int i = 0; i < index; i++) { cur = cur.next; accesses++; }
        } else {
            cur = tail;
            for (int i = size - 1; i > index; i--) { cur = cur.prev; accesses++; }
        }
        accesses++;
        return cur;
    }

    public void add(T x) {
        Node<T> node = new Node<>(x);
        accesses++;
        if (tail == null) {
            head = tail = node;
        } else {
            tail.next = node;
            node.prev = tail;
            tail = node;
        }
        size++;
    }

    public void add(int index, T x) {
        checkIndexForInsert(index);
        if (index == size) { add(x); return; }
        if (index == 0) {
            Node<T> node = new Node<>(x);
            accesses++;
            node.next = head;
            if (head != null) head.prev = node;
            head = node;
            if (tail == null) tail = node;
            size++;
            return;
        }
        Node<T> at = nodeAt(index);
        Node<T> node = new Node<>(x);
        accesses++;
        Node<T> before = at.prev;
        node.prev = before;
        node.next = at;
        before.next = node;
        at.prev = node;
        size++;
    }

    public T remove(int index) {
        checkIndexForAccess(index);
        Node<T> target = nodeAt(index);
        T value = target.value;
        Node<T> before = target.prev;
        Node<T> after = target.next;
        if (before != null) before.next = after; else head = after;
        if (after != null) after.prev = before; else tail = before;
        size--;
        return value;
    }

    public T get(int index) {
        checkIndexForAccess(index);
        return nodeAt(index).value;
    }

    public boolean contains(T x) {
        Node<T> cur = head;
        while (cur != null) {
            comparisons++;
            if (cur.value == null ? x == null : cur.value.equals(x)) return true;
            cur = cur.next;
        }
        return false;
    }

    public T peekFirst() {
        if (head == null) throw new NoSuchElementException();
        return head.value;
    }
}