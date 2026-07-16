## Notes & Implementation details
All the classes in this package use a dual array structure consisting of an insert array and delete array.
The insert array is a single fixed capacity array. In of itself, the insert array actually coordinates very little work
simply allowing threads to claim an index and insert values in the array

In `LBBoundedPQ` and `CombiningLBBoundedPQ`, the insert array is protected by a RW lock. Read locks are claimed by
inserting threads to allow inserts to proceed concurrently and the heap invariant is not maintained. However, in `OrderedBoundedPQ`
the heap invariant is maintained through a serialized array which heapifies values on insert.

The delete array is a logically immutable & growable (up to a bound) array which coordinates delete operations and allows deleting threads
to proceed without interfering with insert threads (up till a merge operation). `LBBoundedPQ` and `OrderedBoundedPQ`
use `CAS` and `FAA` operations respectively to allow threads to claim indices in the delete array before logical deletion.
`CombiningLBBoundedPQ` serializes delete ops through a combiner and a shared arena (allows threads to publish their requests for min values in the priority queue)

In the case of the delete array being logically empty, when the delete index >= delete array capacity, a merge is required.
A merge is a stop the world operation which is serialized through a single thread. A merge sorts the whole insert array for FIFO based arrays 
and then drains the top N values from the insert array into the new delete array to be published. A merge can also be triggered in the scenario 
that an element with a higher priority than the lowest priority element in the delete array is to be inserted into the insert array. This is 
however only true for `LBBoundedPQ` and `CombiningLBBoundedPQ`; a merge cannot be triggered for this reason in `OrderedBoundedPQ`.
Merges due to this scenario are communicated through a tunable **slack count**, an approximate indicator on how many stale values are allowed  
in the insert array before a merge is required.

Merges are coordinated in `LBBoundedPQ` and `OrderedBoundedPQ` through a monotonic three state status 
- `NONE` - Indices can be claimed in the delete array OR the delete array needs to merge but no delete operation is going
- `MERGING` - A thread is currently merging the delete array with the insert array. This is a stop the world operation and 
the slow path for all these structures. The poll latency & thrpt of these structures is dependent on how fast a thread
can perform this operation. Waiting threads spin wait on the status flag until a merged operation is published
- `MERGED` - This array is dead and a new delete array has been published. The release of a new delete array `happens before`
the merged write to the previous delete array

`CombiningLBBoundedPQ` merges are performed by the combining thread and are always performed first (if needed) before helping waiting threads with their operations. 
Waiting threads simply spin wait to be served their results

Boundedness is preserved in all structures by summing the count of the current values in the insert array and the remaining values in the delete array
before any insert operation is performed.

The progress guarantees of all these structures are blocking for primary operations. Fairness is not guaranteed hence a thread can be starved indefinitely
as long as there are elements in the structure and are at the mercy of the OS or whatever scheduler manages the lifecycle of threads

All these structures were derived from the CBPQ paper with major adjustments. Their performance is not that great for my intended workload
compared to the baseline of a single lock, however its worth writing about this. These structures are also highly prone to false sharing
as both delete and insert array structures are tightly coupled to each other to enforce boundedness.

Another variant I considered was a lock free variant of this, however, with the disappointing performance of these structures; `OrderedBoundedPQ` being the best,
I'd rather not waste time on a probably disappointing but harder lock free variant.

Finally, as always, an insert of an element to any of these priority queues `happens before` a subsequent delete of that element
