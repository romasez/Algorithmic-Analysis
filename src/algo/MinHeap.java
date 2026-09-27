package algo;

import java.util.NoSuchElementException;

public class MinHeap<T extends Comparable<T>> {

    private Object[] heap;
    private int size;

    public long comparisons = 0;
    public long accesses = 0;

    public MinHeap() { this(16); }

    public MinHeap(int initialCapacity) {
        if (initialCapacity < 1) initialCapacity = 1;
        heap = new Object[initialCapacity];
        size = 0;
    }

    public int size() { return size; }
    public boolean isEmpty() { return size == 0; }

    public void resetCounters() {
        comparisons = 0;
        accesses = 0;
    }

    private void ensureCapacity(int minCapacity) {
        if (minCapacity <= heap.length) return;
        int newCapacity = heap.length;
        while (newCapacity < minCapacity) newCapacity *= 2;
        Object[] newHeap = new Object[newCapacity];
        for (int i = 0; i < size; i++) { newHeap[i] = heap[i]; accesses++; }
        heap = newHeap;
    }

    @SuppressWarnings("unchecked")
    private T at(int i) { return (T) heap[i]; }

    private void swap(int i, int j) {
        Object tmp = heap[i];
        heap[i] = heap[j];
        heap[j] = tmp;
        accesses += 2;
    }

    private int parent(int i) { return (i - 1) / 2; }
    private int left(int i) { return 2 * i + 1; }
    private int right(int i) { return 2 * i + 2; }

    public void insert(T x) {
        ensureCapacity(size + 1);
        heap[size] = x;
        accesses++;
        int i = size;
        size++;
        while (i > 0) {
            int p = parent(i);
            comparisons++;
            if (at(p).compareTo(at(i)) <= 0) break;
            swap(i, p);
            i = p;
        }
    }

    public T peekMin() {
        if (size == 0) throw new NoSuchElementException("Heap is empty");
        accesses++;
        return at(0);
    }

    public T extractMin() {
        if (size == 0) throw new NoSuchElementException("Heap is empty");
        T min = at(0);
        accesses++;
        size--;
        heap[0] = heap[size];
        accesses++;
        heap[size] = null;
        int i = 0;
        while (true) {
            int l = left(i), r = right(i), smallest = i;
            if (l < size) {
                comparisons++;
                if (at(l).compareTo(at(smallest)) < 0) smallest = l;
            }
            if (r < size) {
                comparisons++;
                if (at(r).compareTo(at(smallest)) < 0) smallest = r;
            }
            if (smallest == i) break;
            swap(i, smallest);
            i = smallest;
        }
        return min;
    }
    /**
     * Package-private helper used only by tests to verify the min-heap
     * property holds for the entire array at this moment: every node's key
     * must be <= both of its children's keys.
     */
    boolean isValidMinHeap() {
        for (int i = 0; i < size; i++) {
            int l = left(i), r = right(i);
            if (l < size && at(i).compareTo(at(l)) > 0) return false;
            if (r < size && at(i).compareTo(at(r)) > 0) return false;
        }
        return true;
    }
}
