package algo;

import java.util.NoSuchElementException;

public class DynamicArray<T> {

    private Object[] data;
    private int size;

    public long comparisons = 0;
    public long accesses = 0;

    public DynamicArray() {
        this(16);
    }

    public DynamicArray(int initialCapacity) {
        if (initialCapacity < 1) initialCapacity = 1;
        data = new Object[initialCapacity];
        size = 0;
    }

    public int size() { return size; }
    public boolean isEmpty() { return size == 0; }

    public void resetCounters() {
        comparisons = 0;
        accesses = 0;
    }

    private void ensureCapacity(int minCapacity) {
        if (minCapacity <= data.length) return;
        int newCapacity = data.length;
        while (newCapacity < minCapacity) newCapacity *= 2;
        Object[] newData = new Object[newCapacity];
        for (int i = 0; i < size; i++) {
            newData[i] = data[i];
            accesses++;
        }
        data = newData;
    }

    private void checkIndexForAccess(int index) {
        if (index < 0 || index >= size)
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
    }

    private void checkIndexForInsert(int index) {
        if (index < 0 || index > size)
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
    }

    public void add(T x) {
        ensureCapacity(size + 1);
        data[size] = x;
        accesses++;
        size++;
    }

    public void add(int index, T x) {
        checkIndexForInsert(index);
        ensureCapacity(size + 1);
        for (int i = size; i > index; i--) {
            data[i] = data[i - 1];
            accesses++;
        }
        data[index] = x;
        accesses++;
        size++;
    }

    @SuppressWarnings("unchecked")
    public T remove(int index) {
        checkIndexForAccess(index);
        T removed = (T) data[index];
        accesses++;
        for (int i = index; i < size - 1; i++) {
            data[i] = data[i + 1];
            accesses++;
        }
        data[size - 1] = null;
        size--;
        return removed;
    }

    @SuppressWarnings("unchecked")
    public T get(int index) {
        checkIndexForAccess(index);
        accesses++;
        return (T) data[index];
    }

    public boolean contains(T x) {
        for (int i = 0; i < size; i++) {
            comparisons++;
            Object el = data[i];
            if (el == null ? x == null : el.equals(x)) return true;
        }
        return false;
    }

    public T peekFirst() {
        if (size == 0) throw new NoSuchElementException();
        return get(0);
    }
}