{%
laika.title = "More Persistent Vectors: Performance Analysis"
laika.metadata.date = "2008-09-01"
%}


# More Persistent Vectors: Performance Analysis

In my [previous post](<http://www.codecommit.com/blog/scala/implementing-persistent-vectors-in-scala>), I introduced the concept of "persistent vectors" and walked through one implementation of the idea. When I actually pushed the post out, I was pretty happy with my code, but it seems I still have much to learn. :-) A number of very smart people replied, suggesting ways that the implementation could be cleaned up and improved. Among these intrepid commenters was [David MacIver](<http://www.drmaciver.com/>), who correctly pointed out the similarities between my persistent vector and his `IntMap` class (coming in Scala 2.7.2). Needless to say, my interest was piqued, so over the course of last week, I spent a fair amount of time going over his implementation as well as the implementations proposed by researchers in the past.

I also took the time to create a proper performance test suite for my vector implementation, one which could compare with other conceptually-similar implementations in a controlled and repeatable environment. The results were both interesting and instructive, so I figured I would take the time to share them here.

Essentially, the performance suite runs through six tests, each of which designed to illuminate either a strength or a weakness in the underlying implementation. These tests are run against four different classes:

  * `Vector` (my persistent implementation) 
  * `ArrayBuffer` (from `scala.collection.mutable`) 
  * `IntMap` (David MacIver) 
  * `Map`

The addition of the last test target was more curiosity than anything else. I wanted to see just how improved was `IntMap` over `Map` for integer keys. The results turned out to be rather surprising:

**** | **Time** |  | **Memory**  
---|---|---|---  
**** | **Vector** | **ArrayBuffer** | **IntMap** | **Map** |  | **Vector** | **ArrayBuffer** | **IntMap** | **Map**  
**Fill Sequential** | 190.51ms | 15.39ms | 37.15ms | 115.14ms |  | 67.11 MB | 3.93 MB | 22.29 MB | 12 MB  
**Fill Random** | 392.98ms | 2028.43ms | 128.35ms | 103.3ms |  | 64.97 MB | 513.59 MB | 39.89 MB | 10.94 MB  
**Read Sequential** | 28.01ms | 3.83ms | 23.21ms | 35.24ms |  | 6.67 MB | 0.02 KB | 0.02 KB | 3.37 MB  
**Read Random** | 92.8ms | 11.14ms | 54.81ms | 63.8ms |  | 8.01 MB | 0.02 KB | 0.02 KB | 2.01 MB  
**Reverse** | 0.02ms | 0.01ms | - | - |  | 0.09 KB | 0.04 KB | - | -  
**Compute Length** | 0.01ms | 0.01ms | 5.11ms | 0.3ms |  | 0.02 KB | 0.02 KB | 0.02 KB | 2.23 MB  
  
As you can see, `IntMap` triumphed over the other immutable data structures (including `Vector`) at just about every turn. To understand why this is surprising, we need to spend a little time examining the theoretical properties of the two primary implementations: `IntMap` and `Vector`.

### Patricia Tries

I already spent a fair bit of time explaining the concept of partitioned tries in the previous article, so I'm not going to reiterate all of that information here. In a nutshell, the implementation of `Vector` is based upon the concept of a trie with an extremely high branching factor where the path to each node encodes its index. Unlike `List`, the data structure is not fully persistent, meaning that some data copying must take place upon insert. Specifically, a new array of branches must be allocated for the specific parent node of the inserted value and so on recursively on to the root. The advantage to this partially-persistent implementation is that we can take advantage of the constant access time afforded by the use of arrays under the surface. The unfortunate truth is that fully persistent data structures do not offer constant access time (at least, none that I know of), and thus are generally unsuitable for implementing random-access vectors.

The idea for this implementation comes originally from Phil Bagwell (interestingly enough, a researcher at LAMP, Martin Odersky's research department at EPFL) in a paper entitled "[Ideal Hash Trees](<http://lamp.epfl.ch/papers/idealhashtrees.pdf>)". His original concept though was for a more efficient hash table data structure, not necessarily with immutability as a requirement. This concept was then adapted by Rich Hickey for his language, Clojure. Finally, I expanded upon Clojure's persistent vectors somewhat by changing their trie distribution from little- to big-endian and wrote up the result in Scala. There are some other minor differences between Hickey's design and my own, but the data structures are essentially identical.

Like my `Vector` implementation, the idea for `IntMap` began its life [as a research paper](<http://citeseer.ist.psu.edu/okasaki98fast.html>), this time by Chris Okaski and Andrew Gill. This paper is quite an interesting read if you've got a spare afternoon, although it does make use of SML rather than Scala as a base language. In summary, the idea was to create an efficient, persistent map structure for integer keys. Superficially, this sounds quite similar to Hickey's modification of Bagwell's concept, but there are many important distinctions below the surface.

At the extremely lowest level, `IntMap` actually makes use of a structure known as a "[Patricia trie](<http://en.wikipedia.org/wiki/Patricia_trie>)" with a fixed branching factor of two. Much like `Vector`, `IntMap` encodes the key (index) of the data node within its path. This encoding is based on the individual bits of the index. However, unlike `Vector`, the ordering is little-endian. Also, to avoid needlessly growing trees to absurd depths, linear common sub-keys are merged into a single prefix node. This is what differentiates Patricia tries. To illustrate, if our branching factor were 10 and we were to store at indexes 1234 and 2234, the common prefix "234" would be represented in a single node, rather than three separate nodes trivially linked to one-another.

This use of a low branching factor in the trie is extremely useful when performing insertions. Specifically, more of the trie structure is preserved untouched from one modification to another. Literally, `IntMap` is _more_ persistent than `Vector`. While this is great for writes, it does make read times a little less efficient. Specifically, `IntMap` reads are worst-case _O( log 2(k) )_, where _k_ is the index in question. For random data input, the average case is reduced somewhat by the prefix collapsing, but this should not be too significant.

By contrast, `Vector` uses an extremely high branching factor (by default) and so offers read efficiency which is _O( log b(k) )_, where _k_ is the index and _b_ is the branching factor. Due to the practical limitations imposed on integer length, this translates into an upper-bound of _O(7),_ which is (for all intents and purposes) constant. Unfortunately, this analysis does not seem to be born-out by the performance data.

### Possible Explanation

The only answer I can think of to explain the disparity between `IntMap` and `Vector` (both in time and in space utilization) is the use of a `List[Int]` in `Vector` to find the target node in the data structure. This `List` is required primarily because I wanted the data distribution in the trie to be optimized for sequential access, therefore requiring the trie encoding to be big-endian on the index rather than little-endian. The unfortunate truth is there's no clean mathematical method (that I know of) which would allow the deconstruction of a number based on its most significant value in an arbitrary base. In fact, the implementation of `computePath` (as suggested by Jules) actually cheats and makes use of the fact that persistent `List`(s) are constructed from the tail-end:

```scala
@inline
def computePath(total: Int, base: Int) = {
  if (total < 0) {
    throw new IndexOutOfBoundsException(total.toString)
  } else {
    var back: List[Int] = Nil
    var num = total
    
    do {
      back = (num % base) :: back
      num /= base
    } while (num > 0)
    
    back
  }
}
```

As efficient as this method is on most modern processors, it can never be as fast as a simple bit-masking operation. Not only that, but it requires the creation of massive numbers of small, immutable objects (cons cells). I believe that it is this instantiation overhead which is eating up the extra memory on reads and killing the overall performance.

Unfortunately, I can't seem to conceive a better way of doing big-endian data distribution. So are there any clever mathy people out there who have a brilliant way of deconstructing the index head-first rather than from the tail end? If I could do that, then I could remove the `List` entirely from the implementation and rely instead on in-place calculations. Maybe then I could catch up with David's blisteringly fast `IntMap`. :-)

  * [Vector.scala](<http://www.codecommit.com/blog/misc/more-persistent-vectors-performance-analysis/Vector.scala>) (modified since last time)
  * [VectorPerfTest.scala](<http://www.codecommit.com/blog/misc/more-persistent-vectors-performance-analysis/VectorPerfTest.scala>)