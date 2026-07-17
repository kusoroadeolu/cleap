## Notes & Implementation details
This package contains the successor to the dual-array relaxed priority queues. It consists of three main implementations
`EpochPQ`, `MpmcEpochPQ` and `PaddedArenaEpochPQ`. These implementations are built on top well studied mpsc and mpmc
single array FIFO queues. These priority queues only use a singular array to store elements compared to their predecessors 

The main contribution these relaxed priority queues provide are the **epoch** priority ordering & logical sorted segment semantics. 
An epoch (in this case) is a lazily bounded range of logically inserted elements relative to the queue capacity, capped at a logical segment size of N elements.

I use the word `logical` as different concurrent queue algorithms which assume different memory consistency models 
assume a logical insertion point

An epoch and its range, is determined at segment sort time; Epochs are logical in the sense that there 
is no actual physical mechanism to track epochs rather they are used to verify the semantics of the queue.

Elements in each of these priority queues are ordered based on two things:
1. The epoch they happened to be inserted in. Earlier epochs have greater priority than later epochs
2. The actual priority of the element inserted

Given two values A and B
Epoch A > Epoch B, then A will be delivered first
otherwise if Epoch A == Epoch B, 
    then if Priority A > Priority B, then A will be delivered first otherwise B will be delivered first

A more practical example:
    Given an array of unsigned integers capacity 8 with a logical segment size of 4 
    [4 7 2 1 -1 -1 -1 -1]
    On queue creation, Epoch 0, consists of the values 4 to 1
    If a later insert of a element `0` such that the queue becomes [4 7 2 1 0 -1 -1 -1], `0` will be
    regarded as if it was part of the epoch
    
When a delete min operation occurs, a segment sort operation will begin and determine the actual range of the epoch;  
after this a new epoch will begin and only the values which fall in the determined range of the epoch will be sorted.
a segment sort operation will not reoccur until all the values in the range of the epoch have been exhausted
Under concurrent executions, regardless of memory consistency models, the values in the range of an epoch
are solely determined by their indexes in the queue at segment sort time(which is serialized) under any interleaving.


### Implementations
Both `EpochPQ` and `PaddedArenaEpochPQ` are Mpsc fifo queues which use the same insertion algorithm. Threads race to claim an index in the queue
to insert their elements, only succeeding if their CAS succeeds, boundedness is enforced using a lazily updated producer 
limit (isolated on its own cache line) which an insert thread uses to determine if the queue is full

Polls are serialized through a combiner which handles segment sort operations while other threads contend on a shared array trying to claim different indices to allow the combiner to deliver values to them;
when an index is claimed, a thread spin waits up to a max spin count until they retry to become the combiner. The state transition for combiners and waiters communicating through arena indexes 
is as so

null -> WAITER   (a thread registered as waiting)
WAITER -> AWAIT  (the combiner has claimed you)
AWAIT -> result  (the combiner has written your value, or NONE if queue empty)
result -> null   (thread has read it and cleared the slot)

The major difference between both poll implementations is that each object may share a cache line with others in `EpochPQ` are not padded unlike in `PaddedArenaEpochPQ` where each 
object is isolated in their own cache line by padding pre allocated objects which threads can perform atomic actions on.


`MpmcEpochPQ` uses a dual array (element and sequence) Mpmc fifo queue to handle concurrent insertions and deletions. Insertions 
use the sequence array to maintain boundedness. Threads also race to claim an index in the queue
to insert their elements, only succeeding if their CAS succeeds. Unlike other queue implementations polls can continue concurrently
until a sort segment operation is needed. 

Sort segment operations are coordinated through a monotonic three state status, refreshed after 
- `NONE` - Indices can be claimed for polls in the sorted segment 
- `MERGING` - A thread is currently sorting the segment range of the current epoch. This is a stop the world operation for only delete operations. 
- The poll latency & thrpt of deletes is dependent on how fast a thread can perform this operation. Waiting threads spin wait on the status flag until a merged operation is published
- `MERGED` - A new epoch has started and the previous elements in the range of the last epoch have been sorted and can be claimed. 
The release of a new status flag `happens before` the merged write to the previous status's state


The base queue implementations were inspired from JCTools. All fields in these queue implementations have been thoughtfully
isolated on their own cache line based on their access patterns and who 'owns' them to prevent cache miss penalties 
from false sharing

Finally, as always, an insert of an element to any of these priority queues `happens before` a subsequent delete of that element
