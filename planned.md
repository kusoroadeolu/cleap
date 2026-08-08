## What I've already built
1. Two (probably novel) bounded priority queues

## Plans for my unbounded concurrent priority queues
1. A priority queue similar to that from the Intel TBB library
2. PIPQ (from the paper). Ive tried to implement it before but got bored halfway lol
3. Multiqueue & MultiBucketQueue hybrid (a multiqueue that upgrades into a multibucket queue when the underlying priority queues get too large, this will be the last thing i tackle though)
4. Multiqueue with work stealing (a multiqueue which segments steal from other segments when empty)


## Right now I'm deciding
1. mechanisms to choose which segment to insert into (what spread function should i use)
2. how to prevent segment skew
3. how to alleviate contention past segmentation

