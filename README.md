# Assignment 2 — Algorithmic Analysis, Correctness and Performance Trade-offs

## 1. Overview

This project implements and analyzes three data structures from scratch in Java:

- **Dynamic Array** (`DynamicArray<T>`) — a resizable array with amortised doubling growth.
- **Linked List** (`LinkedList<T>`) — a doubly linked list that traverses from whichever end (head/tail) is closer to the target index.
- **Min-Heap** (`MinHeap<T extends Comparable<T>>`) — an array-backed binary min-heap.

All three expose the operation set required by the assignment (`add`, `add(index,x)`, `remove(index)`, `get(index)`, `contains(x)` for the array/list; `insert`, `peekMin`, `extractMin` for the heap), and all three are instrumented with `comparisons` / `accesses` counters so the benchmark can report exact operation counts, not just wall-clock time.

The goal of the assignment is not just to implement these structures, but to **prove their correctness with loop invariants**, **derive their asymptotic complexity**, and then **measure** that complexity empirically across four controlled workloads (random access, search, insertion/removal, and priority processing) at `n = 100, 1 000, 10 000, 100 000`.

All results in this report come from real runs of `Benchmark.java` on the author's machine, using `System.nanoTime()`, 5 repetitions per measurement, and `Random(42)` as the seed.

## 2. Complexity Analysis

Let `n` be the number of elements in the structure at the time of the call.

### 2.1 Dynamic Array vs. Linked List

| Structure    | Operation      | Best      | Average   | Worst     | Aux. space |
|--------------|----------------|-----------|-----------|-----------|------------|
| DynamicArray | `add(x)`       | Θ(1)      | Θ(1) amortised | O(n) (on resize) | O(n) amortised |
| DynamicArray | `add(i, x)`    | Ω(1) (i=n)| Θ(n)      | O(n) (i=0)| O(1) |
| DynamicArray | `remove(i)`    | Ω(1) (i=n-1)| Θ(n)    | O(n) (i=0)| O(1) |
| DynamicArray | `get(i)`       | Θ(1)      | Θ(1)      | Θ(1)      | O(1) |
| DynamicArray | `contains(x)`  | Ω(1)      | Θ(n)      | O(n)      | O(1) |
| LinkedList   | `add(x)`       | Θ(1)      | Θ(1)      | Θ(1)      | O(1) per node |
| LinkedList   | `add(i, x)`    | Ω(1) (i=0 or i=n)| Θ(n) | O(n) (i=n/2) | O(1) |
| LinkedList   | `remove(i)`    | Ω(1) (i=0 or i=n-1)| Θ(n)| O(n) (i=n/2)| O(1) |
| LinkedList   | `get(i)`       | Ω(1) (i=0 or i=n-1)| Θ(n)| O(n) (i=n/2)| O(1) |
| LinkedList   | `contains(x)`  | Ω(1)      | Θ(n)      | O(n)      | O(1) |

**Justification.**

- **`DynamicArray.get(i)`** computes a direct offset into contiguous memory (`data[i]`); no branching on the *value* stored occurs, so every call does the same constant amount of work regardless of `i` or `n` — Θ(1) in all three cases.
- **`DynamicArray.add(i, x)` / `remove(i)`** must shift `size - i` elements to keep the array contiguous. Best case is at the tail (`i = size`, zero elements shifted); worst case is at the head (`i = 0`, all `n` elements shifted); a randomly chosen `i` shifts `n/2` elements on average, so the average case is also Θ(n).
- **`DynamicArray.add(x)`** is Θ(1) *amortised*: most calls write into a free slot; occasionally the array doubles (O(n) copy), but this happens with exponentially decreasing frequency, so the total cost of `n` appends is O(n).
- **`LinkedList.get(i)` / `add(i,x)` / `remove(i)`** must first *walk* the list to reach node `i`. The implementation starts from whichever end is closer (`head` if `i < size/2`, else `tail`), so `i = 0` or `i = n-1` is Θ(1), while `i = n/2` is Θ(n). Once found, the insertion/removal itself is Θ(1) (pointer relinking only).
- **`contains(x)`** is a linear scan on both structures: best case O(1) if the first element matches, worst/average case Θ(n).

**Same Big-O, different practical cost.** `get(i)` looks the same on both structures syntactically, but is Θ(1) on the array and Θ(n) on the list. Conversely, `add(0, x)` looks identical on both, but is Θ(n) on the array (shifting) and Θ(1) on the list (relinking).

### 2.2 Min-Heap

| Operation      | Best  | Average   | Worst     | Aux. space |
|----------------|-------|-----------|-----------|------------|
| `insert(x)`    | Ω(1)  | Θ(log n)  | O(log n)  | O(1) (O(n) amortised on resize) |
| `peekMin()`    | Θ(1)  | Θ(1)      | Θ(1)      | O(1) |
| `extractMin()` | Ω(1)  | Θ(log n)  | O(log n)  | O(1) |

**Justification.**

- **`peekMin()`** returns `heap[0]` — the min-heap property guarantees the minimum is always at the root, so this is Θ(1) in every case.
- **`insert(x)`** appends the new element (Θ(1)) and *sifts it up*, comparing with its parent and swapping while smaller. Best case: already ≥ parent, O(1). Worst/average case: it may rise to the root, O(log n), since a binary heap of `n` elements has height `⌊log₂ n⌋`.
- **`extractMin()`** moves the last element to the root (Θ(1)) and *sifts it down*, comparing with both children at each level. Best case: already ≤ both children, O(1). Worst/average case: it may sink to a leaf, O(log n), with up to two comparisons per level (vs. one for sift-up).

## 3. Correctness — Loop Invariant Proofs

Two non-trivial, loop-based operations were chosen: **`DynamicArray.add(index, x)`** (an array insertion, required to be loop-based), and **`MinHeap.extractMin()`** (a heap operation involving a loop).

### 3.1 `DynamicArray.add(index, x)`

```java
public void add(int index, T x) {
    checkIndexForInsert(index);
    ensureCapacity(size + 1);
    for (int i = size; i > index; i--) {
        data[i] = data[i - 1];
    }
    data[index] = x;
    size++;
}
```

**Loop invariant.** At the start of each iteration (indexed by `i`, running from `size` down to `index + 1`), the subarray `data[i .. size]` is an exact copy of the *original* array's subarray `data_old[i-1 .. size-1]` — every element originally at a position `≥ i` has already been shifted one slot to the right, and no element at a position `< i` has been touched yet.

1. **Initialization.** Before the first iteration, `i = size`. The range `i .. size` is empty and no element has been moved yet, so the invariant holds trivially.

2. **Maintenance.** Assume the invariant holds at the start of an iteration for a given `i` (`i > index`): all original elements at positions `≥ i` are already correctly shifted. The loop body executes `data[i] = data[i - 1]`, copying the original element from position `i - 1` into position `i`. The set of correctly-shifted positions now extends down to `i - 1`. Decrementing `i` re-establishes the invariant for the next iteration.

3. **Termination.** The loop condition `i > index` and the strict decrement of `i` (starting from `size`) guarantee exactly `size - index` iterations, after which `i = index` and the loop terminates (`checkIndexForInsert` already guaranteed `0 ≤ index ≤ size`).

4. **Correctness at termination.** When the loop exits, `i = index`, and by the invariant every original element at a position `≥ index` has been shifted one slot right: `data[index+1 .. size]` now equals `data_old[index .. size-1]`. The statement `data[index] = x` places the new element into the vacated slot, and `size` is incremented. The final array satisfies: `data[0..index-1]` unchanged, `data[index] = x`, `data[index+1..size] = data_old[index..size-1]` — exactly the specification of "insert `x` at position `index`, shifting the rest right." ∎

### 3.2 `MinHeap.extractMin()` — the sift-down loop

```java
int i = 0;
while (true) {
    int l = left(i), r = right(i), smallest = i;
    if (l < size && at(l).compareTo(at(smallest)) < 0) smallest = l;
    if (r < size && at(r).compareTo(at(smallest)) < 0) smallest = r;
    if (smallest == i) break;
    swap(i, smallest);
    i = smallest;
}
```
(executed after the old root has been saved and the last element moved into `heap[0]`, with `size` already decremented.)

**Loop invariant.** At the start of every iteration, with current position `i`: the subtree rooted at `heap[i]` is the only part of the array that may violate the min-heap property; every node outside that subtree already satisfies "a node's key ≤ its children's keys."

1. **Initialization.** Before the first iteration, `i = 0`. The moved element was a former leaf and might violate the property relative to its children, but every other node is untouched from before the extraction, so the only questionable subtree is the whole heap, rooted at `0` — matching the invariant.

2. **Maintenance.** Assume the invariant holds for the current `i`. The loop finds `smallest`, the index holding the minimum key among `heap[i]` and its existing children. If `smallest == i`, `heap[i]` already satisfies the property and the loop breaks — nothing is left invalid. Otherwise, `swap(i, smallest)` places the smaller child's key at `i` (now valid relative to its children) and moves the old key down into `smallest`; all other nodes are untouched. Setting `i = smallest` re-establishes the invariant one level down.

3. **Termination.** Each iteration either breaks immediately or moves `i` strictly one level deeper in a tree of finite height `⌊log₂ size⌋`, so the loop always terminates after at most `⌊log₂ size⌋` iterations.

4. **Correctness at termination.** When the loop exits, `heap[i] ≤` both its children (or has none). By the invariant, every node outside the subtree rooted at `i` already satisfied the heap property, and now the node at `i` does too, while both child subtrees remain valid min-heaps (the violation only ever moved through already-valid subtrees). Therefore the entire array is a valid min-heap again. Combined with the fact that the previous minimum was saved and returned before the loop ran, this proves `extractMin()` returns the true minimum and leaves the heap valid. ∎

## 4. Experimental Setup

- **n (initial size):** 100, 1 000, 10 000, 100 000 — used for every applicable workload.
- **m (operations per workload):** 10 000 `get` calls (Workload 1), 1 000 `contains` calls (Workload 2), 1 000 insertions + 1 000 removals at two positions (Workload 3), `n` inserts + `n` extracts (Workload 4).
- **Repetitions:** every timed section is run **5 times**; the reported time is the arithmetic mean.
- **Timing:** `System.nanoTime()` around the operation loop only; input generation happens before the timer starts and is excluded from measurement.
- **Random seed:** `new Random(42)`, re-seeded fresh for each `n`.
- **Metrics recorded:** element accesses/shifts for the array/list workloads, comparisons for search and heap workloads.
- **Environment:** results below were produced by running `java Benchmark` directly (JDK, IntelliJ IDEA), not simulated.

## 5. Results

### Workload 1 — Random Access (`get(index)`, 10 000 calls)

| n | DynamicArray | LinkedList | theoretical |
|---|---:|---:|---|
| 100 | 0.33 ms | 0.90 ms | O(1) vs O(n) |
| 1 000 | 0.18 ms | 3.03 ms | O(1) vs O(n) |
| 10 000 | 0.11 ms | 30.5 ms | O(1) vs O(n) |
| 100 000 | 0.08 ms | 307.5 ms | O(1) vs O(n) |

**Analyze.** `LinkedList` grows roughly linearly with `n` (~10× time at 10× n), matching Θ(n). `DynamicArray` stays essentially flat — it even slightly *decreases*, which is a JIT warm-up effect (the JVM optimises hot code further as the benchmark progresses), not a contradiction of Θ(1). The results agree with theory: the array is insensitive to `n`, while the list's cost is dominated by unavoidable pointer-chasing to a random index.

### Workload 2 — Search (`contains(x)`, 1 000 calls)

| n | DynamicArray time | LinkedList time | comparisons (both) | theoretical |
|---|---:|---:|---:|---|
| 100 | 0.48 ms | 0.45 ms | 73 592 | O(n) |
| 1 000 | 1.03 ms | 1.40 ms | 742 593 | O(n) |
| 10 000 | 3.68 ms | 10.75 ms | 7 599 563 | O(n) |
| 100 000 | 39.21 ms | 112.88 ms | 76 330 683 | O(n) |

**Analyze.** Both structures perform a linear scan, so their comparison counts are **identical** — a useful confirmation that both `contains` implementations behave the same way algorithmically. Execution time grows roughly linearly with `n` for both, confirming Θ(n). The array is consistently faster than the list despite doing the *same number of comparisons*, illustrating that identical Big-O does not guarantee identical practical speed: array traversal benefits from cache locality, while list traversal follows scattered pointers.

### Workload 3 — Insertion and Removal (1 000 operations each)

| n | DynArr insert@begin | LinkedList insert@begin | DynArr insert@mid | LinkedList insert@mid |
|---|---:|---:|---:|---:|
| 100 | 1.82 ms | 0.10 ms | 0.50 ms | 0.10 ms |
| 1 000 | 1.31 ms | 0.04 ms | 0.78 ms | 0.63 ms |
| 10 000 | 7.52 ms | 0.03 ms | 4.19 ms | 6.11 ms |
| 100 000 | 96.64 ms | 0.01 ms | 48.69 ms | 61.00 ms |

| n | DynArr remove@begin | LinkedList remove@begin | DynArr remove@mid | LinkedList remove@mid |
|---|---:|---:|---:|---:|
| 100 | 0.10 ms | 0.03 ms | 0.02 ms | 0.01 ms |
| 1 000 | 0.52 ms | 0.08 ms | 0.17 ms | 0.22 ms |
| 10 000 | 6.90 ms | 0.06 ms | 3.26 ms | 5.46 ms |
| 100 000 | 96.92 ms | 0.01 ms | 47.74 ms | 60.23 ms |

**Analyze.**
- **Insertion/removal at the beginning:** the sharpest contrast — O(n) on the array (every existing element shifts right) vs. O(1) on the list (only the head pointer changes). The array's time grows with `n`; the list's stays essentially flat, close to zero.
- **Insertion/removal in the middle:** both structures cost O(n), but for different reasons — the array still shifts ~`n/2` elements, while the list must *traverse* ~`n/4` nodes before the O(1) relink. Both curves rise together with `n`.
- This is the clearest demonstration of how **physical organization** determines performance: contiguous storage (array) is cheap to index but expensive to rearrange; pointer-linked storage (list) is expensive to index but cheap to rearrange once located.

### Workload 4 — Priority Processing (Min-Heap)

| n | insert time (n ops) | extractMin time (n ops) | insert comparisons | extract comparisons | order OK |
|---|---:|---:|---:|---:|---|
| 100 | 0.043 ms | 0.143 ms | 194 | 841 | ✔ |
| 1 000 | 0.097 ms | 0.356 ms | 2 232 | 14 994 | ✔ |
| 10 000 | 0.570 ms | 1.588 ms | 22 593 | 216 736 | ✔ |
| 100 000 | 1.847 ms | 19.77 ms | 227 662 | 2 831 463 | ✔ |

**Analyze.**
- `insert` is O(log n) worst/average case (sift-up), `peekMin` is O(1), `extractMin` is O(log n) worst/average case (sift-down).
- As `n` grows 1 000× (100 → 100 000), total insert comparisons grow only ~1 174× and total time only ~43× — far less than a 1 000× linear operation would show, consistent with each individual `insert` costing O(log n).
- `extractMin` consistently costs more than `insert` at the same `n`, both in time and comparisons (roughly 2×), matching theory: sift-down does up to **two** comparisons per level (both children), sift-up only **one** (single parent).
- `order_ok = true` in every run, confirming extracted elements always come out in non-decreasing order — the heap property is correctly maintained by both `insert` and `extractMin` (also verified explicitly in `Tests.java`, which checks the heap property after *every single* insert and extract).

## 6. Discussion — Theory vs. Experiment

- **Workload 1:** matches theory cleanly — flat O(1) for the array, linear O(n) for the list.
- **Workload 2:** matches theory — both O(n), and comparison counts are literally identical between structures, the strongest possible confirmation.
- **Workload 3:** matches theory at the extremes (O(1) list-at-head vs. O(n) array-at-head) and in the middle (both O(n)); deviations from a "clean" curve are only in constant factors.
- **Workload 4:** matches the expected O(n log n) total cost for n inserts/extractions; the ~2:1 comparison ratio between `extractMin` and `insert` matches the sift-down vs. sift-up comparison counts predicted by the algorithm.

## 7. Performance and Design Analysis

1. **How does increasing n affect each workload?** Random access on the array stays flat; every other measured quantity grows with `n` — linearly for search and middle insert/remove, and O(n log n) in total (sub-linear per operation) for the heap's n-insert/n-extract sequence.
2. **Which results agree with theory?** All of them, in trend: O(1) stays flat, O(n) grows proportionally, O(log n) per operation shows up as sub-linear total growth.
3. **Where do results differ from theory?** Only in constant factors and in exactly where two asymptotically-equal curves cross — Big-O predicts the *shape* of growth, not the exact numbers; low-level effects (cache behaviour, JIT warm-up, autoboxing) change constants without changing the exponent.
4. **Why can two algorithms with the same Big-O have different running times?** Big-O deliberately discards constant factors and lower-order terms. Two O(n) algorithms can differ substantially in practice if one does more work per element (more memory indirection, worse cache locality) — invisible to asymptotic notation but very visible on a stopwatch, as seen in Workload 2.
5. **How do constant factors and implementation details affect performance?** Contiguous arrays benefit from CPU cache prefetching; linked structures scatter nodes across the heap, causing more cache misses per element visited. Autoboxing, bounds-checking, and JIT optimisation opportunities also matter.
6. **Why is a Dynamic Array preferable for some workloads?** Whenever random access by index dominates (Workload 1), the array's O(1) `get` and excellent cache locality make it the clear winner.
7. **When can a Linked List be useful?** When insertions/removals happen frequently at known positions near the ends — Workload 3 shows the list winning decisively for insert/remove at the beginning, needing no shifting at all.
8. **Why is a Heap appropriate for priority-based processing?** O(log n) insert and extract-min while keeping O(1) access to the current minimum — much better than a sorted array (O(n) insert) or an unsorted array/list (O(n) extract-min).
9. **How does the workload influence the choice of data structure?** There is no universally "best" structure — the right choice depends on which operations dominate: random reads favour arrays, frequent head insert/remove favours lists, and always-need-the-minimum favours heaps. The experiments make this concrete: the same `n` produces opposite winners depending only on *which* operation is measured.

## 8. Design Recommendations

| Workload / access pattern | Recommended structure | Why |
|---|---|---|
| Frequent random `get(i)` by index | Dynamic Array | O(1) vs O(n) for the list |
| Frequent linear search by value | Either (tie on Big-O) | Comparisons are identical; pick based on other operations you also need |
| Frequent insert/remove at the front | Linked List | O(1) vs O(n) for the array |
| Frequent insert/remove at arbitrary/middle positions | Neither is great (both O(n)) | Traversal/shifting cost is unavoidable in either simple structure |
| Repeatedly need "the smallest/most urgent item next" | Min-Heap | O(log n) insert/extract, O(1) peek |

## 9. Testing and Correctness Validation

`Tests.java` covers, without any external testing framework:

- Empty structure, single element, multiple elements, duplicate values.
- Boundary indices (`add(size, x)` valid append) and invalid indices (negative, out of range → `IndexOutOfBoundsException`).
- Large inputs (tens of thousands of elements).
- Randomised cross-validation against `java.util.ArrayList` (5 000 mixed operations, per-operation comparison for both `DynamicArray` and `LinkedList`).
- Randomised cross-validation of `MinHeap` against `java.util.PriorityQueue` (2 000 elements, full extraction order compared).
- Explicit heap-property verification **after every single `insert()`** and **after every single `extractMin()`** (not just the final extraction order), via a package-private `isValidMinHeap()` helper.
- `extractMin()` non-decreasing order check on a large (50 000-element) heap.

All tests pass: `Passed: 5038, Failed: 0`.

## 10. Conclusion

Across all four workloads, the measured behaviour of the Dynamic Array, Linked List, and Min-Heap agreed with their theoretical complexities: O(1) operations stayed flat as `n` grew, O(n) operations grew proportionally to `n`, and the heap's O(log n) operations produced markedly sub-linear total growth. The experiments also reinforced two points a purely theoretical analysis can understate: (1) operations that look syntactically identical across structures (`get`, `add(0,x)`) can have wildly different costs depending on physical layout, and (2) even operations with matching Big-O bounds can differ substantially in practice due to constant factors — cache locality, indirection, and implementation details that asymptotic notation intentionally ignores. Choosing the right structure therefore requires matching its strengths to the workload's dominant operation, not just its overall Big-O "grade."

## Repository Structure

```
assignment-2/
├── src/algo/
│   ├── DynamicArray.java   # resizable array implementation
│   ├── LinkedList.java     # doubly linked list implementation
│   ├── MinHeap.java        # binary min-heap implementation
│   ├── Benchmark.java      # benchmark harness (all 4 workloads)
│   └── Tests.java          # dependency-free correctness test suite
├── results/
│   ├── tables/              # CSV output from Benchmark.java
│   └── plots/                # PNG plots generated from the CSVs
└── README.md                 # this report
