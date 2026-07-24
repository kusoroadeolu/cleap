# cleap

**cleap** is a Java library of **bounded, concurrent priority queues** designed for workloads that need low-latency `delete-min` (poll) operations under high write contention, while keeping inserts cheap. It targets linearizable consistency and is built around array-based structures for spatial locality rather than node-based heaps or skip lists.

> Status: experimental / research project. The API surface is intentionally minimal (`add`, `poll`, `size`, `clear`, `drain`) — no batch operations, and the `PriorityQueue<T>` interface does not extend `java.util.Collection`.

## Why

Classic priority queue designs (binary heaps guarded by a single lock, `PriorityBlockingQueue`, skip-list-based queues) serialize `delete-min` at the head of the structure, which becomes the dominant bottleneck once you scale up concurrent pollers. Most published concurrent PQ designs (PIPQ, CBPQ, MultiQueue, Mound heaps, Hunt et al.) also assume an **unbounded** structure, which gives them freedom (extra buffers, growable segments, etc.) that a fixed-capacity queue can't afford — boundedness couples insert and delete paths tightly, since you must always know the true size to reject inserts correctly.

cleap explores several designs for solving this specifically for **bounded** queues, trading strict heap-order correctness for various relaxed-but-linearizable guarantees in exchange for throughput and tail latency. The full design rationale, papers considered, and dead ends are documented in [`brainstorm.md`](./brainstorm.md).

## Modules

| Module | What's in it                                                                                                                      |
|---|-----------------------------------------------------------------------------------------------------------------------------------|
| `cleap-core` | The library itself; all queue implementations and the shared `PriorityQueue<T>` interface                                         |
| `cleap-jmh` | JMH microbenchmarks comparing implementations under different insert/poll ratios                                                  |
| `cleap-stress` | [jcstress](https://github.com/openjdk/jcstress) concurrency-correctness tests (linearizability / lost-update / lost-write checks) |

## Implementations

All implementations live in `cleap-core` under `io.github.kusoroadeolu.cleap` and share the `PriorityQueue<T>` interface (`add`, `poll`, `size`, `clear`, `drain`).

### `latest` package — current recommended designs

These are single-array MPMC/MPSC FIFO queues (derived from JCTools-style patterns) that trade strict heap ordering for **generation-based** relaxed priority ordering:

- **`PaddedArenaGenerationPQ`** — the current best performer. MPSC inserts via a padded producer index/limit; polls are serialized through a combiner with a cache-line-padded arena so waiting pollers don't false-share while spin-waiting for the combiner to deliver their result.
- **`GenerationPQ`** — same algorithm as `PaddedArenaGenerationPQ` but without per-slot arena padding (useful for isolating the cost of false sharing in benchmarks).
- **`MpmcGenerationPQ`** — a true MPMC variant (dual element/sequence array) where polls can proceed concurrently until a segment-sort is required, instead of being fully combiner-serialized.

**Generations**: a generation is a lazily-validated, time-bound range of logically inserted elements, capped at a tunable *segment limit*. Elements are ordered first by generation (earlier generation always wins), then by priority within a generation. This bounds the cost of any single sort operation and avoids the "later, higher-priority arrival must sort the whole structure" problem. See [`latest/Notes.md`](./cleap-core/src/main/java/io/github/kusoroadeolu/cleap/latest/Notes.md) for the full state machine and worked examples.

### `dualarray` package — insert-array / delete-array designs

An earlier design family: a FIFO **insert array** (protected by a `ReadWriteLock`, reads for concurrent inserts, write for merges) paired with a logically-immutable, growable-up-to-a-bound **delete array** that services polls independently until it needs refilling from the insert array (a "merge").

- **`LBBoundedPQ`** — insert array is unordered FIFO; delete array claims are CAS-based; a merge sorts the insert array and republishes the delete array.
- **`CombiningLBBoundedPQ`** — same core idea as `LBBoundedPQ`, but polls are combined through a lock-free combining arena so threads that lose the race publish their request and let the combiner batch-serve them, instead of blocking.
- **`OrderedBoundedPQ`** — inserts go through an exclusive lock and maintain the heap invariant directly (via sift-up/sift-down), so deletes can use a simple FAA counter with no insert-triggered merge condition.

Design tradeoffs and the merge/status state machine (`NONE` / `MERGING` / `MERGED`) for this family are documented in [`dualarray/Notes.md`](./cleap-core/src/main/java/io/github/kusoroadeolu/cleap/dualarray/Notes.md).

### `experimental` package

Prototypes and unbounded reference structures used for comparison, including a segmented worker/leader-list design (`PIPQ`), optimistic MPSC-stack-backed heaps (`OptimisticConcurrentPriorityQueue`, `StagedConcurrentPriorityQueue`), a plain `ReentrantLock`-guarded heap (`LockedPQ`) used as a baseline, and unbounded array/node heaps for reference.

## Usage

```java
import io.github.kusoroadeolu.cleap.PriorityQueue;
import io.github.kusoroadeolu.cleap.latest.PaddedArenaGenerationPQ;

PriorityQueue<Integer> queue = new PaddedArenaGenerationPQ<>(1024); // capacity rounds up to next power of two

queue.add(5);
queue.add(1);
queue.add(3);

Integer min = queue.poll();  // 1
int size = queue.size();
List<Integer> rest = queue.drain(); // polls everything remaining
```

`add` returns `false` once the queue is at capacity instead of blocking or throwing. `poll` returns `null` on an empty queue.

## Benchmarks
Run benchmarks:

```bash
cd cleap-jmh
mvn package
java -jar target/benchmark.jar
```

## Correctness testing

- **Unit tests** (`cleap-core/src/test`) cover basic queue semantics per implementation (empty poll, ordering, capacity rounding, wraparound, merge/segment-sort edge cases).
- **jcstress tests** (`cleap-stress`) target the properties that matter most for these designs under real interleavings: no lost writes, no null results when a merge/segment-sort is required, bounded-size invariants, and add/remove linearizability.

Run stress tests with:

```bash
cd cleap-stress
mvn package
java -jar target/jcstress.jar
```

## Building

This is a multi-module Maven project (Java 21):

```bash
mvn clean install        # builds cleap-core, cleap-jmh, cleap-stress
```

Run benchmarks:

```bash
cd cleap-jmh
mvn package
java -jar target/benchmark.jar
```

## Consistency model

All implementations target **linearizability** for `add`/`poll`, with the caveat that the `latest` package's generation ordering is an intentional, documented relaxation of strict priority order (see "Generations" above) traded for throughput an insert is still guaranteed to happen-before any poll that observes it, and generation exhaustion happens-before the start of the next generation.