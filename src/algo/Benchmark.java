package algo;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Random;

public class Benchmark {

    static final int[] SIZES = {100, 1_000, 10_000, 100_000};
    static final int REPEATS = 5;
    static final long SEED = 42L;
    static final String OUT_DIR = "results/tables/";

    public static void main(String[] args) throws IOException {
        new java.io.File(OUT_DIR).mkdirs();
        workload1_randomAccess();
        workload2_search();
        workload3_insertionRemoval();
        workload4_priorityProcessing();
        System.out.println("Done. CSVs in " + OUT_DIR);
    }

    interface Runnable5 { void run(); }

    static double timeAverageNanos(Runnable5 body) {
        long total = 0;
        for (int r = 0; r < REPEATS; r++) {
            long start = System.nanoTime();
            body.run();
            total += (System.nanoTime() - start);
        }
        return total / (double) REPEATS;
    }

    static Integer[] randomInts(Random rnd, int n, int bound) {
        Integer[] arr = new Integer[n];
        for (int i = 0; i < n; i++) arr[i] = rnd.nextInt(bound);
        return arr;
    }

    static void workload1_randomAccess() throws IOException {
        try (PrintWriter out = new PrintWriter(new FileWriter(OUT_DIR + "workload1_random_access.csv"))) {
            out.println("structure,n,avg_time_ns,accesses,theoretical");
            for (int n : SIZES) {
                Random rnd = new Random(SEED);
                Integer[] values = randomInts(rnd, n, 1_000_000);
                int m = 10_000;
                int[] indices = new int[m];
                for (int i = 0; i < m; i++) indices[i] = rnd.nextInt(n);

                DynamicArray<Integer> arr = new DynamicArray<>(n);
                for (Integer v : values) arr.add(v);
                double t1 = timeAverageNanos(() -> { for (int idx : indices) arr.get(idx); });
                out.println("DynamicArray," + n + "," + t1 + "," + m + ",O(1)");

                LinkedList<Integer> list = new LinkedList<>();
                for (Integer v : values) list.add(v);
                double t2 = timeAverageNanos(() -> { for (int idx : indices) list.get(idx); });
                out.println("LinkedList," + n + "," + t2 + "," + m + ",O(n)");
            }
        }
    }

    static void workload2_search() throws IOException {
        try (PrintWriter out = new PrintWriter(new FileWriter(OUT_DIR + "workload2_search.csv"))) {
            out.println("structure,n,avg_time_ns,comparisons,theoretical");
            for (int n : SIZES) {
                Random rnd = new Random(SEED);
                Integer[] values = randomInts(rnd, n, 1_000_000);
                int m = 1_000;
                Integer[] queries = new Integer[m];
                for (int i = 0; i < m; i++)
                    queries[i] = (rnd.nextBoolean()) ? values[rnd.nextInt(n)] : -1 - rnd.nextInt(1_000_000);

                DynamicArray<Integer> arr = new DynamicArray<>(n);
                for (Integer v : values) arr.add(v);
                arr.resetCounters();
                double t1 = timeAverageNanos(() -> { for (Integer q : queries) arr.contains(q); });
                out.println("DynamicArray," + n + "," + t1 + "," + (arr.comparisons / REPEATS) + ",O(n)");

                LinkedList<Integer> list = new LinkedList<>();
                for (Integer v : values) list.add(v);
                list.resetCounters();
                double t2 = timeAverageNanos(() -> { for (Integer q : queries) list.contains(q); });
                out.println("LinkedList," + n + "," + t2 + "," + (list.comparisons / REPEATS) + ",O(n)");
            }
        }
    }

    static void workload3_insertionRemoval() throws IOException {
        try (PrintWriter out = new PrintWriter(new FileWriter(OUT_DIR + "workload3_insertion_removal.csv"))) {
            out.println("structure,n,position,operation,avg_time_ns,theoretical");
            for (int n : SIZES) {
                Random rnd = new Random(SEED);
                Integer[] values = randomInts(rnd, n, 1_000_000);
                int m = 1_000;
                runInsertRemove(out, "DynamicArray", n, values, m, true);
                runInsertRemove(out, "LinkedList", n, values, m, false);
            }
        }
    }

    interface StatefulRun { double run(); }

    static double timeAverageStateful(StatefulRun body) {
        double total = 0;
        for (int r = 0; r < REPEATS; r++) total += body.run();
        return total / REPEATS;
    }

    static void runInsertRemove(PrintWriter out, String name, int n, Integer[] values, int m, boolean isArray) {
        int[] positions = {0, n / 2};
        String[] labels = {"begin", "middle"};

        for (int p = 0; p < positions.length; p++) {
            int pos = positions[p];

            if (isArray) {
                double t = timeAverageStateful(() -> {
                    DynamicArray<Integer> a2 = new DynamicArray<>(n + m);
                    for (Integer v : values) a2.add(v);
                    long start = System.nanoTime();
                    for (int i = 0; i < m; i++) a2.add(pos, i);
                    return (double) (System.nanoTime() - start);
                });
                out.println(name + "," + n + "," + labels[p] + ",insert," + t + ",O(n)");

                double t2 = timeAverageStateful(() -> {
                    DynamicArray<Integer> a2 = new DynamicArray<>(n);
                    for (Integer v : values) a2.add(v);
                    long start = System.nanoTime();
                    for (int i = 0; i < m && a2.size() > 0; i++) a2.remove(Math.min(pos, a2.size() - 1));
                    return (double) (System.nanoTime() - start);
                });
                out.println(name + "," + n + "," + labels[p] + ",remove," + t2 + ",O(n)");
            } else {
                String theo = (pos == 0) ? "O(1)" : "O(n)";
                double t = timeAverageStateful(() -> {
                    LinkedList<Integer> l2 = new LinkedList<>();
                    for (Integer v : values) l2.add(v);
                    long start = System.nanoTime();
                    for (int i = 0; i < m; i++) l2.add(pos, i);
                    return (double) (System.nanoTime() - start);
                });
                out.println(name + "," + n + "," + labels[p] + ",insert," + t + "," + theo);

                double t2 = timeAverageStateful(() -> {
                    LinkedList<Integer> l2 = new LinkedList<>();
                    for (Integer v : values) l2.add(v);
                    long start = System.nanoTime();
                    for (int i = 0; i < m && l2.size() > 0; i++) l2.remove(Math.min(pos, l2.size() - 1));
                    return (double) (System.nanoTime() - start);
                });
                out.println(name + "," + n + "," + labels[p] + ",remove," + t2 + "," + theo);
            }
        }
    }

    static void workload4_priorityProcessing() throws IOException {
        try (PrintWriter out = new PrintWriter(new FileWriter(OUT_DIR + "workload4_priority_processing.csv"))) {
            out.println("n,avg_insert_time_ns,avg_extract_time_ns,insert_comparisons,extract_comparisons,order_ok");
            for (int n : SIZES) {
                Random rnd = new Random(SEED);
                Integer[] values = randomInts(rnd, n, 1_000_000);
                double totalInsert = 0, totalExtract = 0;
                long insertComp = 0, extractComp = 0;
                boolean orderOk = true;

                for (int r = 0; r < REPEATS; r++) {
                    MinHeap<Integer> heap = new MinHeap<>(Math.max(16, n));
                    long start = System.nanoTime();
                    for (Integer v : values) heap.insert(v);
                    totalInsert += (System.nanoTime() - start);
                    insertComp += heap.comparisons;

                    heap.resetCounters();
                    int prev = Integer.MIN_VALUE;
                    start = System.nanoTime();
                    for (int i = 0; i < n; i++) {
                        int v = heap.extractMin();
                        if (v < prev) orderOk = false;
                        prev = v;
                    }
                    totalExtract += (System.nanoTime() - start);
                    extractComp += heap.comparisons;
                }

                out.println(n + "," + (totalInsert / REPEATS) + "," + (totalExtract / REPEATS) + ","
                        + (insertComp / REPEATS) + "," + (extractComp / REPEATS) + "," + orderOk);
            }
        }
    }
}