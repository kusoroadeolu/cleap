## Notes & Implementation details
This package contains the successor to the dual-array relaxed priority queues. It consists of three main implementations
`GenerationPQ`, `MpmcGenerationPQ` and `PaddedArenaGenerationPQ`. These implementations are built on top well studied mpsc and mpmc
single array FIFO queues. These priority queues only use a singular array to store elements compared to their predecessors 


### Generations
The main contribution these relaxed priority queues provide is **generation** priority ordering & sorted segment semantics. 
A `generation` (in this case) is a time bound, lazily validated range of logically inserted elements.
The validated range (segment size) is relative to the queue capacity, capped at a segment limit of N elements.

A `segment limit` is the max possible range of elements a generation can enclose/hold. A generation is said to be bounded 
once its range has exceeded the segment limit

I use the word `logical` for insertions as different concurrent queue algorithms which assume different memory consistency models 
always assume a logical insertion point, whether it be linearizable, quiescent etc.  

However, the base FIFO must guarantee that once an index belongs to the validated range of the current generation, 
no subsequent enqueue can cause a logically later element to become part of that validated range (i.e. no overwrites)

A generation and its range, is determined at segment sort time; generations are logical in the sense that there 
is no actual physical mechanism to track generations rather they are used to verify the **priority** semantics of the queue.

Elements in each of these priority queues are ordered based on two things:
1. The generation they happened to be inserted in. Earlier generations automatically have greater priority than later generations
2. The actual priority of the element inserted

Given two values A and B
Generation A > Generation B, then A will be delivered first
otherwise if generation A == generation B, 
    then if Priority A > Priority B, then A will be delivered first otherwise B will be delivered first

A practical example:
    Given an array of unsigned integers capacity 8 with a segment limit of 4 
    [4 7 2 1 -1 -1 -1 -1]
    On queue creation, generation 0, consists of the values 4 to 1
    If a later insert of an element `0` such that the queue becomes [4 7 2 1 0 -1 -1 -1], `0` will be
    regarded as if it was part of a new generation 1

Another practical example
    Given an array of unsigned integers capacity 8 with a segment limit of 4
    [4 7 1 -1 -1 -1 -1 -1]
    On queue creation, generation 0, consists of the values 4 to 1
    If a later insert of a delete min operation occurs, the generation's range is lazily determined. 
    As there are only 3 elements in the queue at the time of this delete min operation. 
    The generation's actual range is determined to be 3 rather than 4 regardless of if a logical insertion interleaved with the delete op. 
    (we always assume the range of a generation is the segment limit, until validation occurs);  
    Only the values which fall in the validated range of the generation will be sorted.
    A segment sort operation will not reoccur until all the values in the validated range of the earliest validated generation have been exhausted

A new generation begins once a previous generation has been sorted OR the range of a generation has exceeded the segment limit

Under concurrent executions, regardless of memory consistency models, the values in the range of a generation
are solely determined by their indexes in the queue at segment sort time (which is serialized) regardless of any interleaving.


### Implementations
Both `GenerationPQ` and `PaddedArenaGenerationPQ` are Mpsc fifo queues which use the same insertion algorithm. Threads race to claim an index in the queue
to insert their elements, only succeeding if their CAS succeeds, boundedness is enforced using a lazily updated producer 
limit (isolated on its own cache line) which an insert thread uses to determine if the queue is full

Polls are serialized through a combiner which handles segment sort operations while other threads contend on a shared array trying to claim different indices to allow the combiner to deliver values to them;
when an index is claimed, a thread spin waits up to a max spin count until they retry to become the combiner. The state transition for combiners and waiters communicating through arena indexes 
is as so

null -> WAITER   (a thread registered as waiting)
WAITER -> AWAIT  (the combiner has claimed you)
AWAIT -> result  (the combiner has written your value, or NONE if queue empty)
result -> null   (thread has read it and cleared the slot)

The major difference between both poll implementations is that each object may share a cache line with others in `generationPQ` are not padded unlike in `PaddedArenagenerationPQ` where each 
object is isolated in their own cache line by padding pre allocated objects which threads can perform atomic actions on.


`MpmcGenerationPQ` uses a dual array (element and sequence) Mpmc fifo queue to handle concurrent insertions and deletions. Insertions 
use the sequence array to maintain boundedness. Threads also race to claim an index in the queue
to insert their elements, only succeeding if their CAS succeeds. Unlike other queue implementations polls can continue concurrently
until a sort segment operation is needed. 

Sort segment operations are coordinated through a monotonic three state status, refreshed after 
- `NONE` - Indices can be claimed for polls in the sorted segment 
- `SORTING` - A thread is currently sorting the segment range of the current generation. This is a stop the world operation for only delete operations. 
- The poll latency & thrpt of deletes is dependent on how fast a thread can perform this operation. Waiting threads spin wait on the status flag until a merged operation is published
- `SORTED` - A new generation has started and the previous elements in the range of the last generation have been sorted and can be claimed. 
The release of a new status flag `happens before` the merged write to the previous status's state


The base queue implementations were inspired from JCTools. All fields in these queue implementations have been thoughtfully
isolated on their own cache line based on their access patterns and who 'owns' them to prevent cache miss penalties 
from false sharing

Finally, as always
1. An insert of an element to any of these priority queues `happens before` a subsequent delete of that element
2. The exhaustion or bounding of a generation `happens before` the occurrence of a new generation
3. A segment sort of the elements in a generation's range `happens before` the deletion of any elements in that range