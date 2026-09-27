package algo;

import java.util.Random;
import java.util.Arrays;

public class Tests {

    static int passed = 0;
    static int failed = 0;

    static void check(boolean cond, String msg) {
        if (cond) passed++;
        else { failed++; System.out.println("FAIL: " + msg); }
    }

    public static void main(String[] args) {
        testDynamicArrayBasics();
        testDynamicArrayBoundaries();
        testLinkedListBasics();
        testLinkedListBoundaries();
        testAgainstJavaUtilList();
        testMinHeapBasics();
        testMinHeapAgainstPriorityQueue();
        testMinHeapLarge();

        System.out.println("\nPassed: " + passed + ", Failed: " + failed);
        if (failed > 0) System.exit(1);
    }

    static void testDynamicArrayBasics() {
        DynamicArray<Integer> a = new DynamicArray<>();
        check(a.isEmpty(), "empty at start");
        a.add(10); a.add(20);
        a.add(0, 5); // [5,10,20]
        check(a.get(0) == 5 && a.get(1) == 10 && a.get(2) == 20, "add(index,x) shifts correctly");
        int removed = a.remove(0);
        check(removed == 5 && a.get(0) == 10, "remove(0) shifts left");
        check(a.contains(20) && !a.contains(999), "contains works");
    }

    static void testDynamicArrayBoundaries() {
        DynamicArray<Integer> a = new DynamicArray<>();
        boolean threw = false;
        try { a.get(0); } catch (IndexOutOfBoundsException e) { threw = true; }
        check(threw, "get on empty throws");
        a.add(1); a.add(2); a.add(3);
        threw = false;
        try { a.add(4, 99); } catch (IndexOutOfBoundsException e) { threw = true; }
        check(threw, "add(size+1) throws");
        threw = false;
        try { a.add(3, 99); } catch (IndexOutOfBoundsException e) { threw = true; }
        check(!threw, "add(size) is a valid append");
    }

    static void testLinkedListBasics() {
        LinkedList<Integer> l = new LinkedList<>();
        l.add(10); l.add(20);
        l.add(0, 5);
        check(l.get(0) == 5 && l.get(1) == 10 && l.get(2) == 20, "linked list add(index,x)");
        int removed = l.remove(0);
        check(removed == 5 && l.get(0) == 10, "linked list remove(0)");
        check(l.contains(20) && !l.contains(999), "linked list contains");
    }

    static void testLinkedListBoundaries() {
        LinkedList<Integer> l = new LinkedList<>();
        boolean threw = false;
        try { l.get(0); } catch (IndexOutOfBoundsException e) { threw = true; }
        check(threw, "get on empty list throws");
    }

    static void testAgainstJavaUtilList() {
        Random rnd = new Random(42);
        java.util.List<Integer> ref = new java.util.ArrayList<>();
        DynamicArray<Integer> arr = new DynamicArray<>();
        LinkedList<Integer> list = new LinkedList<>();

        for (int op = 0; op < 5000; op++) {
            int kind = rnd.nextInt(4);
            if (kind == 0 || ref.isEmpty()) {
                int v = rnd.nextInt(1000);
                ref.add(v); arr.add(v); list.add(v);
            } else if (kind == 1) {
                int idx = rnd.nextInt(ref.size() + 1);
                int v = rnd.nextInt(1000);
                ref.add(idx, v); arr.add(idx, v); list.add(idx, v);
            } else if (kind == 2) {
                int idx = rnd.nextInt(ref.size());
                Integer expected = ref.remove(idx);
                Integer gotArr = arr.remove(idx);
                Integer gotList = list.remove(idx); // было пропущено!
                check(expected.equals(gotArr), "remove matches ArrayList (DynamicArray)");
                check(expected.equals(gotList), "remove matches ArrayList (LinkedList)");
            } else {
                int idx = rnd.nextInt(ref.size());
                check(ref.get(idx).equals(arr.get(idx)), "get matches ArrayList (DynamicArray)");
                check(ref.get(idx).equals(list.get(idx)), "get matches ArrayList (LinkedList)");
            }
        }
        check(ref.size() == arr.size() && ref.size() == list.size(), "final sizes match");
    }

    static void testMinHeapBasics() {
        MinHeap<Integer> h = new MinHeap<>();
        int[] values = {5, 3, 8, 1, 9, 2, 7, 1, 1};
        for (int v : values) h.insert(v);
        int[] sorted = values.clone();
        Arrays.sort(sorted);
        for (int expected : sorted) {
            check(h.extractMin() == expected, "extractMin returns non-decreasing order");
        }
        check(h.isEmpty(), "heap empty after draining");
    }

    static void testMinHeapAgainstPriorityQueue() {
        Random rnd = new Random(42);
        java.util.PriorityQueue<Integer> ref = new java.util.PriorityQueue<>();
        MinHeap<Integer> h = new MinHeap<>();
        for (int i = 0; i < 2000; i++) {
            int v = rnd.nextInt(100000);
            ref.add(v); h.insert(v);
        }
        boolean ok = true;
        while (!ref.isEmpty()) {
            if (!ref.poll().equals(h.extractMin())) ok = false;
        }
        check(ok, "MinHeap matches java.util.PriorityQueue");
    }

    static void testMinHeapLarge() {
        Random rnd = new Random(42);
        int n = 50_000;
        MinHeap<Integer> h = new MinHeap<>();
        for (int i = 0; i < n; i++) h.insert(rnd.nextInt());
        int prev = Integer.MIN_VALUE;
        boolean nonDecreasing = true;
        while (!h.isEmpty()) {
            int v = h.extractMin();
            if (v < prev) nonDecreasing = false;
            prev = v;
        }
        check(nonDecreasing, "large heap non-decreasing order");
    }
    static void testMinHeapPropertyAfterEachInsert() {
        Random rnd = new Random(42);
        MinHeap<Integer> h = new MinHeap<>();
        boolean allValid = true;
        for (int i = 0; i < 2000; i++) {
            h.insert(rnd.nextInt(100000));
            if (!h.isValidMinHeap()) allValid = false;
        }
        check(allValid, "heap property holds after every single insert()");
    }

    static void testMinHeapPropertyAfterEachExtract() {
        Random rnd = new Random(42);
        MinHeap<Integer> h = new MinHeap<>();
        for (int i = 0; i < 2000; i++) h.insert(rnd.nextInt(100000));

        boolean allValid = true;
        while (h.size() > 0) {
            h.extractMin();
            if (!h.isValidMinHeap()) allValid = false;
        }
        check(allValid, "heap property holds after every single extractMin()");
    }
}