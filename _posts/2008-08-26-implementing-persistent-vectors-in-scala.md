---
categories:
- Scala
date: '2008-08-26 00:00:00 '
layout: post
title: Implementing Persistent Vectors in Scala
wordpress_id: 258
wordpress_path: /scala/implementing-persistent-vectors-in-scala
---

Oh yeah, we're really getting into Digg-friendly titles now, aren't we?

The topic of persistent vectors is one of those odd backwaters of functional programming that few dare to approach. The basic idea behind it all is to try to devise an immutable data structure which has the performance characteristics of a mutable vector. What this means for practical application is that you shouldn't have to deal with _O(n)_ efficiency on random access like you do with `List`(s). Instead, accessing arbitrary indexes should be constant time ( _O(1)_ ), as should computing its length. Additionally, modifying an arbitrary index should be reasonably efficient - as close to constant-time as possible.

Of course, the word "modifying" is a trifle inaccurate as it implies a mutable data structure. One of the primary qualifications for a purely functional vector is that it is completely immutable. Any changes to the vector result in the creation of a _new_ vector, rather than modifying the old. Basically, it's an immutable data structure just like any other, but one which retains the brutal efficiency of its mutable counterpart.

Unfortunately, this turns out to be a very tough nut to crack. A number of researchers have attempted different strategies for solving the problem, none of which have been entirely successful. Rich Hickey, the creator of [Clojure](<http://www.clojure.org>), has a [brilliant presentation](<http://blip.tv/file/812787>) that essentially describes the solution I have chosen. For the impatient, the good stuff starts at about 36:00 and lasts for about ten minutes. I'll elaborate on the problems with functional vectors a bit more in a second, but first a bit of motivational propaganda...

### Thou Shalt Not Mutate

There is a single principle which should be drilled into skulls of all programmers everywhere: mutable data structures are bad news. Don't get me wrong, I love `ArrayList` as much as the Java addict, but such structures cause serious problems, particularly where concurrency is concerned. We can consider the trivial example where two threads are attempting to populate an array simultaneously:

```java private String[] names = new String[6]; private int index = 0; public static void main(String[] args) { Thread thread1 = new Thread() { public void run() { names[index++] = "Daniel"; names[index++] = "Chris"; names[index++] = "Joseph"; } }; Thread thread2 = new Thread() { public void run() { names[index++] = "Renee"; names[index++] = "Bethany"; names[index++] = "Grace"; } }; thread1.start(); thread2.start(); thread1.join(); thread2.join(); for (String name : names) { System.out.println(name); } } ``` 

What does this snippet print? I don't know. It's actually indeterminate. Now we can _guess_ that on most machines the result will be essentially interleaved between the two threads, but there is no way to guarantee this. Part of the reason for this is the fact that arrays are mutable. As such, they enable (and indeed, encourage) certain patterns which are highly destructive when employed asynchronously.

However, concurrency is not even the only motivation for immutable data structures. Consider the following example:

```java List names = ... for (String name : names) { names.add(name.toUpperCase()); } ``` 

I'm sure all of us have done something like this, most likely by accident. The result is (of course) a `ConcurrentModificationException` caused by the fact that we are attempting to add to a `List` _while_ we are iterating over its contents. I know that the first time I was faced with this error message I became extremely confused. After all, no threads are being employed, so why is this a problem?

Iterators are extremely dependent on the internal state of their data structure. Anyone who has ever implemented an iterator for a linked list or (even better) a tree will attest to this fact. This means that generally speaking, there is no way for an iterator to _guarantee_ correctness if that structure is changing out from underneath it (so to speak). Things may be fine in a linear structure like a list, but as soon as you get into anything non-linear like a tree or a hash table it becomes difficult to even define what the "correct" behavior should be. Think about it; should the iterator backtrack to hit the missing elements? Should this backtracking include elements which have already been consumed? What if the order changes dramatically and pre-consumed elements are now _ahead_ of the current index? There are a whole host of problems associated with iterating over mutable data structures, and so rather than vainly attempt to solve these issues in a sane and consistent manner, JDK collections simply throw an exception.

All of this becomes moot once you start using immutable data structures. There is no way to modify a structure while iterating over it because there is no way to modify the structure _at all!_ Concurrency is not an issue because without any mutable state to require locking, every thread can operate simultaneously on the structure. Not only is it thread safe, but it is unsynchronized _and_ thread safe. Immutable data structures retain all of the asynchronous throughput of non-locking implementations without any of the race conditions and non-determinacy which usually results.

#### A Brief Note on Idioms

At this point, the question you must be asking yourself is: "So if the data structure is immutable, what good is it?" The answer is "for reading". Data structures spend most of their lives being read and traversed by other code. Immutable data structures can be read in exactly the same fashion as mutable ones. The trick of course is constructing that data structure in the first place. After all, if the data structure is completely immutable, where does it come from? A simple example from a prior article is sufficient to demonstrate both aspects:

```scala def toSet[T](list: List[T]): Set[T] = list match { case hd :: tail => hd + toSet(tail) case Nil => Set[T]() } ``` 

This is neither the most concise nor the most efficient way to accomplish this task. The only purpose served by this example is to illustrate that it _is_ very possible to build up immutable data structures without undue syntactic overhead. You'll notice that every time we want to "change" a data structure - either removing from the list or adding to the set - we use a function call and either pass or return the modified structure. In essence, the state is kept entirely on the stack, with each new version of the data structure in question becoming the "changed" version from the previous operation.

This idiom is actually quite powerful and can be applied to even more esoteric (and less uniformly iterative) operations. As long as you are willing to let execution jump from one function to the next, it becomes extremely natural to deal with such immutability. In fact, you start to think of immutable data structures as if they were in fact mutable, simply due to the fact that you are idiomatically "modifying" them at each step. Note that this pattern of modifying data between functions is critical to actor-based programming and any seriously concurrent application design.

### Problems Abound

For the sake of argument, let's assume that my pitiful arguments have convinced you to lay aside your heathen mutating ways and follow the path of functional enlightenment. Let's also assume that you're consumed with the desire to create an application which tracks the status of an arbitrary number of buttons. These buttons may be pressed in any order regardless of what other buttons are already pressed. Following the path of immutability and making use of the patron saint of persistent data structures (`List`), you might come up with the following solution:

```scala class ButtonStrip private (buttons: List[Boolean]) { def this(num: Int) = { this((0 until num).foldLeft(List[Boolean]()) { (list, i) => false :: list }) } def status(index: Int) = buttons(index) def push(index: Int) = modify(index, true) def unpush(index: Int) = modify(index, false) /** * Modify buttons list and return a new ButtonStrip with the results. */ private def modify(index: Int, status: Boolean) = { val (_, back) = (buttons :\ (buttons.length - 1, List[Boolean]())) { (tuple, button) => val (i, total) = tuple (if (i == index) status else button) :: total } new ButtonStrip(back) } } ``` 

This is a horrible mess of barely-idiomatic functional code. It's difficult to read and nearly impossible to maintain; but it's purely immutable! This is _not_ how you want your code to look. In fact, this is an excellent example of what David MacIver would call "[bad functional code](<http://www.drmaciver.com/2008/08/functional-code-not-equal-good-code>)".

Perhaps even worse than the readability (or lack thereof) is the inefficiency of this code. It's _terribly_ slow for just about any sort of operation. Granted, we can imagine this only being used with a list of buttons of limited length, but it's the principle of the thing. The fact is that we are relying on a number of operations which are extremely inefficient with lists, most prominently `length` and `apply()` (accessing an arbitrary index). Not only that, but we're recreating the _entire_ list every time we change the status of a single button, something which is bad for any number of reasons.

What we really need here is a random-access structure, something which allows us to access and "change" any index with some degree of efficiency. Likely the most intuitive thing to do here would be to just use a good ol' array of `Boolean`(s) and make a new copy of this array any time we need to change something. Unfortunately, this is almost as bad as our copying the list every time. Normally, when you use an immutable data structure, modifications do _not_ require copying large amounts of data. Our `toSet` example from above uses almost zero data copying under the surface, due to the way that `Set` and `List` are implemented.

Specifically, `Set` and `List` are _persistent_ data structures. This doesn't mean that they live on a filesystem. Rather, the term "persistent" refers to the fact that each instance of the collection may share significant structure with another instance. For example, prepending an element onto an immutable list yields a new list which consists of the new element and a tail which is _precisely_ the original list. Thus, each list contains its predecessor (if you will) within itself. `List` is an example of a _fully persistent_ data structure; not everything can be so efficient. `Set` and `Map` for example are usually implemented as some sort of tree structure, and so insertions require some data copying (specifically, the parent nodes). However, this copying is minimized by the nature of the structure. This notion of persistence in the data structure works precisely because these structures are immutable. If you could change an element in a persistent data structure it would likely result in the modification of that same element in totally disparate instances of that structure across the entire runtime (not a pleasant outcome).

So `List` is persistent, arrays are not. Even if we treat arrays as being completely immutable, the overhead of copying a potentially huge array on each write operation is rather daunting. What we need is some sort of data structure with the properties of an array (random access, arbitrary modification) with the persistent properties of a list. As I said before, this turns out to be a _very_ difficult problem.

### Partitioned Tries

One solution to this problem which provides a compromise between these two worlds is that of a partitioned [trie](<http://en.wikipedia.org/wiki/Trie>) (pronounced "try" by all that is sane and holy). In essence, a partitioned trie is a tree of vectors with a very high branching factor (the number of children per node). Each vector is itself a tree of vectors and so on. Note that these are not not like the [binary search trees](<http://en.wikipedia.org/wiki/Binary_search_tree>) that every had to create back in college, partitioned tries can potentially have _dozens_ of branches per node. As it turns out, it is this unusual property which makes these structures so efficient.

To get a handle on this concept, let's imagine that we have a trie with a branching factor of 3 (much smaller than it should be, but it's easier to draw). Into this vector we will insert the following data:

**  Index **

| 

**Data**  
  
---|---  
  
`0`

| 

Daniel  
  
`7`

| 

Chris  
  
`2`

| 

Joseph  
  
`4`

| 

Renee  
  
`1`

| 

Bethany  
  
`3`

| 

Grace  
  
`9`

| 

Karen  
  
`13`

| 

Larry  
  
`5`

| 

Moya  
  
After all the jumbling necessary to make this structure work, the result will look like the following:

![image](/assets/images/blog/wp-content/uploads/2008/08/image.png)

It's hard to see where the "trie" part of this comes into play, so bear with me. The important thing to notice here is the access times for indexes 0-2: it's _O(1)._ This is of course a tree, so not all nodes will be one step away from the root, but the point is that we have achieved constant-time access for at least _some_ of the nodes. Mathematically speaking, the worst-case access time for any index _n_ is _O( log 3(n) )._ Not too horrible, but we can do better.

First though, we have to lay some groundwork. I said that the structures we were working with are _tries_ rather than normal _trees_. A trie implies that the key for each element is encoded in the path from the root to that node. So far, it just appears that we have built a fancy tree with numeric keys and a higher branching factor than "normal". This would be true if all we were given is the above diagram, but consider the slightly modified version below:

![image](/assets/images/blog/wp-content/uploads/2008/08/image1.png)

Structurally, nothing has changed, but most of the edges have been renumbered. It is now a bit more apparent that each node has three branches numbered from 0 to 2. Also, with a little more thought, we can put the rest of the pieces together.

Consider the "Moya" node. In our input table, this bit of data has an index of **5**. To find its "index" in our renumbered trie, we follow the edges from the root down to the destination node, arriving at a value of **12**. However, remember that each node has only 3 branches. Intuitively, we should be thinking about base-3 math somewhere about now. And indeed, converting **12** into base-3 yields a final value of **5** , indicating that the index of the node is indeed encoded in the path from the root based on _column_. By the way, this works on any node (try it yourself). The path to "Karen" is **100** , which converted into base-3 becomes **9** , the input index of the element.

This is all fine and dandy, but we haven't really solved our original problem yet: how to achieve constant-time access to arbitrary indexes in a persistent structure. To really approach a solution to our problem, we must increase the branching factor substantially. Rather than working with a branching factor of 3 (and thus, _O( log 3(n) )_ efficiency), let's dial the branching factor up to 32 and see what happens.

The result is completely undiagramable; but it does actually provide constant time access for indexes between 0 and 31. If we were to take our example data set and input it into our revised trie, the result would be a single layer of nodes, numbered at exactly their logical index value. In the worst case, the efficiency of our more complicated trie is _O( log 32(n) )_. Generally speaking, we can infer (correctly) that for any branching factor _b_ and any index _n_ , the lookup efficiency will be precisely _log b(n)_. As we increase the branching factor, the read-efficiency of our trie increases _exponentially_. To put a branching factor of 32 into perspective, this means that the algorithmic complexity of accessing index 1,000,000 would only be 3.986! That's incredibly small, especially given the sheer magnitude of the index in question. It's not _technically_ constant time, but it's so incredibly small for all conceivable indexes that we can just pretend that it is. As Rich Hickey says:

> ...when it's a small enough value, I really don't care about it.

So that takes care of the reading end of life, but what about writing? After all, if all we needed was constant time lookups, we could just use an array. What we really need to take care to do is ensure that modifications are also as fast as we can make them, and that's where the tricky part comes in.

We can think of an array as a partitioned trie with a branching factor of ![infinity](/assets/images/blog/wp-content/uploads/2008/08/image2.png). When we modify an array immutably, we must copy _every_ element from the original array into a new one which will contain the modification. This contrasts with `List` \- effectively, a partitioned trie with a branching factor of 1 - which in the best case (prepending) requires _none_ of the elements to be copied. Our 32-trie is obviously somewhere in between. As I said previously, the partitioned trie doesn't really solve the problem of copying, it just compromises on efficiency somewhat (the difference between 1 and 3.986).

The truth is that to modify a partitioned trie, every node in the target _sub-trie_ must be copied into a new subtrie, which then forces the copying of its level and so-on recursively until the root is reached. Note that the contents of the nodes are not being copied, just the nodes themselves (a shallow copy). Thus, if we return to our example 3-trie from above and attempt to insert a value at index 12, we will have to copy the "Larry" node along with our new node to form the children of a copy of the "Renee" node. Once this is done, the "Grace" and "Moya" nodes must also be copied along with the new "Renee" to form the children of a new "Bethany". Finally, the "Daniel" and "Joseph" nodes are copied along with the new "Bethany" to form the children of a new root, which is returned as the modified trie. That sounds like a lot of copying, but consider how much went untouched. We never copied the "Karen" or the "Chris" nodes, they just came over with their parent's copies. Instead of copying 100% of the nodes (as we would have had it been an array), we have only copied 80%. Considering that this was an example contrived to force the maximum copying possible, that's pretty good!

Actually, we can do even better than this by storing the children of each node within an array (we would have to do this anyway for constant-time access). Thus, only the array and the modified nodes need be copied, the other nodes can remain untouched. Using this strategy, we further reduce the copying from 80% to 30%. Suddenly, the advantages of this approach are becoming apparent.

Now of course, the higher the branching factor, the larger the arrays in question and so the less efficient the inserts. However, insertion is always going to be more efficient than straight-up arrays so long as the inserted index is greater than the branching factor. Considering that most vectors have more than 32 elements, I think that's a pretty safe bet.

### Implementation

I bet you thought I was going to get to this section _first_. Foolish reader...

Once we have all this theoretical ground-work, the implementation just falls into place. We start out with a `Vector` class parameterized _covariantly_ on its element type. Covariant type parameterization just means that a vector with type `Vector[B]` is a subtype of `Vector[A]` whenever `B` is a subtype of `A`. `List` works this way as well, as do most immutable collections, but as it turns out, this sort of parameterization is unsafe (meaning it could lead to a breakdown in the type system) when used with mutable collections. This is part of why generics in Java are strictly _invariant_.

Coming back down to Earth (sort of), we consider for our design that the `Vector` class will represent the partitioned trie. Since each child node in the trie is a trie unto itself, it is only logical to have each of the nodes also represented by `Vector`. Thus, a `Vector` must have three things:

  * `data`
  * `length`
  * `branches`

Put into code, this looks like the following:

```scala class Vector[+T] private (private val data: Option[T], val length: Int, private val branches: Seq[Vector[T]]) extends RandomAccessSeq[T] { def this() = this(None, 0, new Array[Vector[T]](BRANCHING_FACTOR)) // ... } ``` 

`RandomAccessSeq` is a parent class in the Scala collections API which allows our vector to be treated just like any other collection in the library. You'll notice that we're hiding the default constructor and providing a no-args public constructor which instantiates the default. This only makes sense as all of those fields are implementation-specific and should not be exposed in the public API. It's also worth noting that the branches field is typed as `Seq[Vector[T]]` rather than `Array[Vector[T]]`. This is a bit of a type-system hack to get around the fact that `Array` is parameterized invariantly (as a mutable sequence) whereas `Seq` is not.

With this initial design decision, the rest of the implementation follows quite naturally. The only trick is the ability to convert an index given in base-10 to the relevant base-32 (where 32 is the branching factor) values to be handled at each level. After far more pen-and-paper experimentation than I would like to admit, I finally arrived at the following solution to this problem:

```scala def computePath(total: Int, base: Int): List[Int] = { if (total < 0) { throw new IndexOutOfBoundsException(total.toString) } else if (total < base) List(total) else { var num = total var digits = 0 while (num >= base) { num /= base digits += 1 } val rem = total % (Math.pow(base, digits)).toInt val subPath = computePath(rem, base) num :: (0 until (digits - subPath.length)).foldRight(subPath) { (i, path) => 0 :: path } } } ``` 

As a brief explanation, if our branching factor is 10 and the input index (`total`) is 20017, the result of this recursive function will be `List(2, 0, 0, 1, 7)`. The final step in the method (dealing with ranges and folding and such) is required to solve the problem of leading zeroes dropping off of subsequent path values and thus corrupting the final coordinates in the trie.

The final step in our implementation (assuming that we've got the rest correct) is to implement some of the utility methods common to all collections. Just for demonstration, this is the implementation of the `map` function. It also happens to be a nice, convenient example of _good_ functional code. :-)

```scala override def map[A](f: (T)=>A): Vector[A] = { val newBranches = branches map { vec => if (vec == null) null else vec map f } new Vector(data map f, length, newBranches) } ``` 

#### Properties

Before moving on from this section, it's worth noting that that our implementation of the vector concept has some rather bizarre properties not held by conventional, mutable vectors. For one thing, it has a logically infinite size. What I mean by this is it is possible to address any positive integral index within the vector without being concerned about resize penalties. In fact, the only addresses which throw an `IndexOutOfBoundsException` are negative. The length of the vector is defined to be the maximum index which contains a valid element. This actually mirrors the semantics of Ruby's `Array` class, which also allows placement at any positive index. Interestingly enough, the efficiency of addressing arbitrary indexes is actually worst-case much better in the persistent trie than it is in a conventional amortized array-based vector.

### Vectored Buttons

Since we now have an immutable data structure with efficient random-access, we may as well rewrite our previous example of the button strip using this structure. Not only is the result far more efficient, but it is also intensely cleaner and easier to read:

```scala class ButtonStrip private (buttons: Vector[Boolean]) { def this(num: Int) = this(new Vector[Boolean]) // no worries about length def status(index: Int) = buttons(index) def push(index: Int) = new ButtonStrip(buttons(index) = true) def unpush(index: Int) = new ButtonStrip(buttons(index) = false) } ``` 

You'll notice that the `update` method is in fact defined for `Vector`, but rather than returning `Unit` it returns the modified `Vector`. Interestingly enough, we don't need to worry about length allocation or anything bizarre like that due to the properties of the persistent vector (infinite length). Just like arrays, a `Vector` is pre-populated with the default values for its type. In the case of most types, this is `null`. However, for `Int`, the default value is `0`; for `Boolean`, the default is `false`. We exploit this property when we simply return the value of dereferencing the vector based on _any_ index. Thus, our `ButtonStrip` class actually manages a strip of infinite length.

### Conclusion

The truth is that we didn't even go as far as we could have in terms of optimization. Clojure has an implementation of a bit-partitioned hash trie which is basically the same thing (conceptually) as the persistent vector implemented in this article. However, there are some important differences.

Rather than jumping through strange mathematical hoops and hacky iteration to develop a "path" to the final node placement, Clojure's partitioned trie uses bit masking to simply choose the trailing five bits of the index. If this node is taken, then the index is right-shifted and the next five bits are masked off. This is far more efficient in path calculation, but it has a number of interesting consequences on the final shape of the trie. Firstly, the average depth of the tree for random input is less, usually by around 1 level (on average). This means that the array copying on insert must occur less frequently, but must copy more references at each step. Literally, the trie is more dense. This probably leads to superior performance. Unfortunately, it also requires that the index for each value be stored along with the node, requiring more memory. Also, this sort of "bucket trie" (to coin a phrase) is a little less efficient in lookups in the average case. Not significantly so, but the difference is there. Finally, this masking technique requires that the branching factor be a multiple of 2. This isn't such a bad thing, but it does impose a minor restriction on the flexibility of the trie.

Most importantly though, Clojure's trie uses _two_ arrays to contain the children: one for the head and one for the tail. This is a phenomenally clever optimization which reduces the amount of array copying by 50% across the board. Rather than having a single array of length 32, it has two arrays of length 16 maintained logically "end to end". The correct array is chosen based on the index and recursively from there on down the line. Naturally, this is substantially more efficient in terms of runtime and heap cost on write.

At the end of the day though, these differences are all fairly academic. Just having an idiomatic partitioned trie for Scala is a fairly significant step toward functional programming relevance in the modern industry. With this structure it is possible to still maintain lightning-fast lookup times and decent insertion times without sacrificing the critical benefits of immutable data structures. Hopefully structures like this one (hopefully, even better implementations) will eventually find their way into the standard Scala library so that all may benefit from their stream-lined efficiency.

  * [Vector.scala](<http://www.codecommit.com/blog/misc/implementing-persistent-vectors-in-scala/Vector.scala>)
  * [VectorSpecs.scala](<http://www.codecommit.com/blog/misc/implementing-persistent-vectors-in-scala/VectorSpecs.scala>) ([ScalaCheck](<http://scalacheck.googlecode.com>) rocks)

### Update

The implementation of `Vector` which I present in this article is inherently much less efficient than the one Rich Hickey created for Clojure.  I finally broke down and created a line-for-line port from Clojure's Java implementation of `PersistentVector` into a Scala class.  I _strongly_ suggest that you use this (much faster) implementation, rather than my own flawed efforts.  :-)  You can download the improved `Vector` here: [Vector.scala](<http://www.codecommit.com/blog/misc/implementing-persistent-vectors-in-scala/final/Vector.scala>).