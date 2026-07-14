# Brainstorming

## What am I building?
A bounded concurrent priority queue preferably using an array (as the main structure) which helps with spatial locality. 

## What is the scope of this project
Ideally I plan to make this production ready. However, for the method scope I plan to trim most of the methods to the essentials.
No batch methods and it doesn't have to implement the `Collection` interface

## What does bounded mean
Given a pq of an initial size k, the pq should not hold at any given time, any number of elements > k. When size == k, we reject new inserts

## What I want to optimize for
Low latency delete min operations under high write contention. However inserts should have decent or low latency/thrpt. I am also optimizing for a low memory footprint

## Consistency model
I'm aiming for linearizability. This can be weakened if needed. Communication among threads will be mainly done using causal consistency modes, though sequential consistency modes
might be needed in some places

## The issue with pq's
The main bottleneck of pqs is the serial nature of delete min operations as most threads will contend at the head of the queue. We alleviate
this through the use of supporting data structures

## The issue of bounded non linear structures
As per unbounded pq's which have greater room to allow for certain techniques to improve performance, unfortunately, bounded pqs 
do not allow that as the use of multiple datastructures which directly contribute to the size of the pq makes it harder to adhere to
its bounded invariants, more or so in the sense that we could possibly evict a value even though it was meant to be in the queue at some point in time

## Papers I've read
PIPQ, CBPQ, MultiQueue ,Hunt et al, Mounds heap

All these papers assume unbounded usage
### PIPQ

PIPQ leverages the idea from multi-queue by segmenting the core PQ into multiple PQs however we introduce a higher level, a linked list, above the segmented pq's which supports for deletion operations without acquiring locks on the segmented pq's. 
The main issue however is the use of shared state among insert/deleting threads and the shift up/down dance during insertion

### CBPQ
CBPQ uses a different approach. It uses a chunked linked list to manage the pq introducing a buffer chunk and immutable first chunk separate from the main list that handle deletions while the others handle insertions

### MultiQueue
A simple structure which just segments sequential pqs protected using a lock. Deletions are relaxed as a thread randomly chooses two segments and takes the highest priority value of both segments

### Hunt et al
Pretty old. This uses striped locking to handle concurrency in a single lock and some bit techniques which aren't too relevant. Im also optimizing for memory footprint 

### Mounds 
This uses an array of mound nodes. Each mound node contains an anchor and a concurrent list. The anchor allows for traversing inserts and deletes to know what nodes their values fall in
It also uses a status flag per node to indicate if the node obeys the mound invariant, if not, the structure is moundified. I'd have tried to implement this but they assume DCAS and there's also
the issue of mutable anchors in a bounded array, so there's that

## Off the table
1. Striped locking - the latency of just acquiring the lock defeats the latency it will take to compare two values (and possibly swap them if needed). Also it incurs a high memory footprint and is highly prone to deadlocks
2. A simple fixed array using a lock (PriorityBlockingQueue) already does it (though its unbounded)
3. Skip list based approaches

## Some of my current design ideas
### Segmented + Elimination

Higher level (Linked list) --- Supports mainly deletions (threads can add here from any segment once it's count reduces past a threshold)
          |  -------------------- (Elimination array to support delete and insert operations)
Lower level (Segmented PQs)


### Simple array and delete min (A minor version of CBPQ)
Base structure (array)
        |
Supporting structure (a smaller array(immutable) that serves delete operations using FAA) plus a Mini Buffer (which holds keys smaller that the current max in the supporting structure and supports rebuilds and eliminations)

**NOTE** By PQ/pq I am referring to a priority queue


## A bit more about the structures
Base Array - Handles insertions and helps with merging (protected by a RW lock - Reads for insertions, writes for merges)
Supporting Array - Handles deletes, immutable, deletes simply increment a FAA counter to delete a value

[//]: # (Buffer Array - Handles inserts whose values have higher prio than the lowest prio item in the deletion array, the values )

[//]: # (in this array are not logically in the pq until they're merged)

## Data structures

### Insertion array 
Initialized to the initial given capacity of the pq. Does not maintain priority order, just maintains FIFO ordering

- CAS monotonic counter: tracks the current position of a value to be incremented (prevents over bound)
- RW Lock: Read lock allows for concurrent insertions, Write lock allows for merges with the deletion array

### Delete array 
Null on initialization of the pq. Has a max capacity of the given cap of the pq * 0.30

- Status Flag: Contains the current status of the array: `NONE`, `FREEZING`, `FROZEN`.
- FAA monotonic counter: Tracks the current deletion point of the array 
- Size: Tracks the size of the array(its final upon construction), though it may be less than the length of the array

[//]: # (### Buffer array)

[//]: # (- Status Flag: Contains the current status of the array: `NONE`, `FREEZING`, `FROZEN`.)

[//]: # (- FAA monotonic counter: Tracks the current insertion point of the array)

[//]: # (- Size: Tracks the size of the array&#40;its final upon construction&#41;, though it may be less than the length of the array)

Note that the thread who inserts into the zero index position of the buffer will handle the merging with the deletion 
and insertion array

## Pseudocode
Note: D.A refers to the delete array, I.A refers to the insert array, capacity = total cap of the

### Insertion flow

Obtain R Lock
var V = value;
repeatedly
    var index = CAS Counter.get();
    var daFAAIndex = D.A.FAA counter index Comment: checking the D.A FAA size is ok here as we can accept loose bounding(the bounds might be less than but will never go above)
    var deleteSize = D.A == null ? 0 : (D.A.size - ) + 1  Comment: Note we don't need to check status here
    var totalPQSize = index + deleteSize + 1;
    if (totalPQSize >= capacity) return false
    tryAdvance -> try swap cas counter
    if(!tryAdvance) restart

if (index of {D.A.size - 1} at D.A array has a lower prio than V) 
    then insert to buffer and inform deletes about a merge
else insert to I.A array at index and return true
Finally release R Lock


### Delete Flow
var d = D.A
var i = Increment CAS counter
if (D.A is null or D.A.CAS index >= size) 
    if (Try obtain write lock for I.A.) Comment: When we obtain this lock, we need a way to tell other threads to backoff and wait for us to signal them
        if (I.A is empty), signal other threads that array is empty while holding the lock
        if (I.A has values), sort the I.A. array in reverse order,
            copy the last n values into the delete array ,
            publish the delete array with the index starting at 1 (we claim the zero value)
elif(D.A.Status is FROZEN) wait for a new D.A array to be published
else return value at I



## LATER THOUGHTS
- I ditched the buffer approach as it was harder to maintain correctness with the buffer included

### What I've Done So Far
So far I've managed to build 3 versions of what I described here
1. LBBoundedPQ - A relaxed priority queue that includes a delete min array and an insert array. The insert array is protected by a RW lock.
Inserts always acquire the read lock, so multiple inserts can occur concurrently. 
Inserts do not maintain priority order, rather they maintain fifo order using a cas based counter to claim positions in the array to insert into the array. Note the array cannot grow or shrink
A merge flag/counter is provided for inserts in the case an inserted value has a higher priority than the lowest priority item in the delete array

Deletes use a separate array-based structure which hold the highest priority items in the structure, to allow for deletes. To delete a value, a thread trys to claim an index in the delete array 
If the index claimed is >= than the size of the delete array or an insert has flagged for a merge(up to a specific slack count), the delete thread suspends all delete operations temporarily by indicating it is merging. At this time other delete threads will backoff
The deleting thread will then acquire the write lock for the insert array, sort the insert array(in reversed order), before extracting the needed number of elems from the insert array and rebuilding the new delete array

The indexes of the insert and delete arrays are monotonically increasing and can never decrease 

To ensure boundedness/fixed capacity in this pq, we allow for loose boundedness (in the sense at 2 points p a thread could see an index I for the insert array and later see a value D for the delete array)
We accept this caveat as remember the indexes in this structure are monotonically increasing.

Ideally the publication of a new delete array `happens before` the old delete array is marked as merged/dead


2. CombiningLBBoundedPQ - Similar to the LBBoundedPQ however, to solve the sequential nature of the poll operation we allow for combining.
A technique in which threads contend over mutual exclusion for a critical section. Threads which fail to acquire the mutex publish their 
work in a shared structure to allow the combiner to help do their work for them (in batches). 

The combining thread in this scenario handles the merging and index acquisition logic. If a merge is needed, the combining thread always merges before index acquistion

3. OrderedBoundedPQ - Similar to the LBBoundedPQ however, inserts are protected by an exclusive lock and the insert array always obeys the heap
invariant under the lock. The merge invariant for inserted values with higher prio than those in the delete array is non-existent here. This allows for deletes to take advantage of a FAA counter rather than a CAS based counter.

In the case of a merge; when the delete array is logically empty i.e. delete index == delete arr capacity/size, a thread sets the status of that array to merging, acquires the insert lock
and repeatedly polls the highest priority value from the insert array into the new delete array before making the new delete array visible


## THOUGHTS
The main issues so far is the fact that fixed capacity tightly couples the delete and insert array, which causes cache coherence traffic and copious amounts of false sharing
as they both depend on each other to maintain the bounded invariant. Also, through profiling, a lot of the hotpaths have been the memory accesses, which is never a good sign
as no amount of memory ordering optimization will increase performance to an actual meaningful level. This hints at the algorithm being suboptimal for the problem.

As at now, the best performing structures for my intended workload, 80% poll 20% insert (measured by latency) by a mile is a simple locked PQ followed by the ordered bounded PQ. The Combining and LB PQ are 
pretty suboptimal. However for insert heavy workloads 100% inserts, all these designs are pretty neck in neck as inserts are pretty cheap in most of them


## A simpler redesign (experimental)
So far, I've been rethinking some choices, a new design I've emerged with (not necessarily a new design in that sense) but one
that repurposes a well studied data structure. A MPMC Bounded Queue


We relax the invariants through logical deletion epochs (will be explained later). The heap invariant is not maintained as well

### Pseudocode
We rely on the MPMC as an abstraction for the pseudocode, though I'll implement it myself.

## Insert 
MPMC Insert

## Polls
Assuming a producer index, consumer index and a consumer capacity (the index of the array that we merged to)

repeatedly:
if consumer index == capacity
    if consumer index  == producer index, return empty
    otherwise  a thread tries to start a merge (this begins a logical epoch) by acquiring a simple spin lock (by casing the capacity to the new one if we fail we spin on the capacity)
    during a merge, we subset the array from consumer index (masked) to producer index or a capacity
    We copy this subset from the array into a new array, sort that array and recopy back into the original
    We then claim the first consumer index, before making the new capacity visible
otherwise
    MPMC poll


### Logical epochs
The idea of logical epochs is to solve the issue of later arriving higher priority values
We define that an epoch starts when the structure is initially made or after a merge and ends when a merge begins
We sort values by their epoch first, then their actual priority, so we trade perfect strictness for relaxed priority semantics
Epoch sizes are also bounded to prevent tail latency spikes

For example take an initial array of capacity 5 with these values

7 2 5 3 null - epoch (0)

A delete thread comes up and starts a merge and logically increases the epoch by 1. Values 7 to 3 are classified as being in the 0 epoch

Hence if a later arriving priority value like `1` comes later, it is seen as a lower priority value as elements are classified by their epoch
then actual priority. Relaxing strict correctness for perf