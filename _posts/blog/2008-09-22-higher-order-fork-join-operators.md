---
categories:
- Scala
date: '2008-09-22 02:00:49 '
layout: post
title: Higher-Order Fork/Join Operators
wordpress_id: 263
wordpress_path: /scala/higher-order-fork-join-operators
---

I think we can all agree that concurrency is a problem. Not really a problem as in "lets get rid of it", but more the type of problem that really smart people spend their entire lives trying to solve. Over the years, many different solutions have been proposed, some of them low-level, some more abstract. However, despite their differences, a common thread runs through all of these ideas: each of them attempts to ease the pain of decomposing operations in a reorderable fashion.

Surprisingly, the word "ordering" is not often heard in conjunction with parallelism. Most of the time, people are thinking in terms of server/client or broker/investor. If you really deconstruct the issue though, there is actually a deeper question underlying all concurrency: what operations do _not_ depend upon each other in a sequential fashion? As soon as we identify these critical operations, we're one step closer to being able to effectively optimize a particular algorithm with respect to asynchronous processing.

By the way, I really will get to fork/join a little later, but I wanted to be sure that I had laid a solid groundwork for the road ahead. Without understanding some of the fundamental theory behind fork/join, it will be impossible to see how it can be applied effectively to your next project.

### Factorial

One of the odd things about computer science is a depressing lack of imaginative examples. Not being one to break with tradition, I've decided to kick off our little quest with a little time spent in the well-trodden foothills of factorial. This should help us to establish some terminology (which I'm arbitrarily assigning for the purposes of this article) as well as the basic concepts involved. A simple recursive implementation (in Scala of course) would look like this:

```scala def fac(n: Int): Int = if (n < 1) 1 else fac(n - 1) * n ``` 

For each number, this function performs a number of discrete operations. First, it checks to see if the index is less than 1. If so, then the function returns immediately, otherwise it proceeds on a separate course. This is a _branching_ operation. Since the "`true` branch" is uninteresting, we will focus on the case when the index is in fact greater than 1. In this case, we have three critical operations which must be performed. They are as follows (temporary names are fictitious):

  * Subtract `1` from `n` and store the value in some temporary `$t1`
  * Dispatch to function `fac` passing the value from `$t1`
  * Multiply result from dispatch with `n` and return 

All this may seem extremely pedantic but please, bear with me. Consider these operations very carefully in the topological sense. What we're trying to see here is if one (or more) of these operations may be ordered above (or below) one of the others. For example, could we perhaps dispatch to `fac` _after_ multiplying and returning? Or could we perform the subtraction operation after the dispatch?

The answer is quite obviously "of course not". There is no way we can change the ordering in this expression because each step depends entirely upon the result from the previous. As far as our attempts to parallelize are concerned, these three operations are completely _atomic_ , meaning that they form an inseparable computation.

![image](/assets/images/blog/wp-content/uploads/2008/09/image.png)

Since we've drilled down as far as we can possibly go in our implementation and so identified the most atomic computation, let's move out one step and see if we can find anything with promise. Stepping back through our execution sequence leads us directly to the branching operation identified previously. Remember that our goal is to identify operations which can be shuffled around in the execution order without affecting the semantics. (does this feel like pipeline optimization to anyone else?) Unfortunately, here too we are at an impasse. We might try moving an atomic computation from one of the branches out before the branching operation, but then we could conceivably do the wrong thing. Since our function uses recursion, this sort of reordering would be very dangerous indeed.

The truth is that for factorial, there are absolutely no operations which can be moved around without something going wrong. Because of this property, we are forced to conclude that the _entire_ factorial operation is atomic, not just its `false` branch. Unfortunately, this means that there is no way to effectively transform this function into some sort of asynchronous variant. That's not to say that you couldn't calculate factorial of two separate numbers concurrently, but there is no way to modify _this_ implementation of the factorial function in a parallel fashion[1](<http://www.codecommit.com/blog/scala/higher-order-fork-join-operators#comment-4060>). This is truly the defining factor of atomic computations: it may be possible to reorder a series of atomic computations, but such a reordering cannot affect the internals of these computations. Within the "atom", the order is fixed.

So what does reordering have to do with concurrency? Everything, as it turns out. In order to implement an asynchronous algorithm, it is necessary to identify the parts of the algorithm which can be executed in parallel. In order for one computation to be executed concurrently with another, neither must rely upon the other being at any particular stage in its evaluation. That is to say, in order to execute computation **A** at the same time as computation **B** , the _ordering_ of these two computations must be irrelevant. Providing that both computations complete prior to some computation **C** (which presumably depends upon the results of **A** and **B** ), the aggregate semantics of the algorithm should remain unaffected. You could prove this, but I really don't feel like it and frankly, I don't think anyone reading this will care. :-)

### Fibonacci

Now that we have some simple analysis on factorial under our belt, let's try something a little tougher. The Fibonacci series is another of those classic computer science examples. Curiously enough, the implementation used by every known textbook to explain recursion is actually one of the _worst_ possible ways to implement the calculation. Wikipedia has an excellent description of why this is, but suffice it to say that the intuitive approach is very, very bad (efficiency wise).

However, the "good" implementations used to calculate the _n_ th number of the Fibonacci series just aren't as concise or easily recognized. Also, they're fairly efficient in their own rights and thus see far less benefit from parallelization at the end of the day. So rather than taking the high road, we're going to just bull straight ahead and use the first algorithm which comes to mind:

```scala def fib(n: Int): Int = if (n < 2) n else fib(n - 1) + fib(n - 2) ``` 

Like factorial, this function makes an excellent poster child for the syntactic wonders of functional programming. Despite its big-O properties, one cannot help but stop and appreciate the concise beauty of this single line of code.

As is common in simple recursion, our function begins with a conditional. We have a simple branching operation testing once again for a range ( _n < 2_), with a base case returning _n_ directly. It is easy to see how the "`true` branch" is atomic as it consists of but one operation. We've already made a hand-wavy argument that branches themselves should not be dissected and moved around outside of the conditional, so it would seem that our only hope rests with the recursive "`false` branch". In words, we have the following operations:

  * Subtract `1` from `n` and store the value in temporary `$t1`
  * Dispatch to function `fib` passing the value from `$t1`; store the value in `$t1`
  * Subtract `2` from `n` and store the value in temporary `$t2`
  * Dispatch to function `fib` passing the value from `$t2`; store the value in `$t2`
  * Add values `$t1` and `$t2` and return 

Ah, this looks promising! We have two "blocks" of operations which look almost identical. Printed redundancy should _always_ be a red flag to developers, regardless of the form. Printed redundancy should _always_ be a red flag to developers, regardless of the form. In this case though, we don't want to extract the duplicate functionality into a separate function, that would be absurd. Rather, we need to observe something about these two operations, specifically: they do not depend on one-another. It doesn't matter whether or not we have already computed the value of `fib(n - 1)`, we can still go ahead and compute `fib(n - 2)` and the result will be exactly the same. We're going to get into trouble again as soon as we get to the addition operation, but as long as both dispatches occur before the final aggregation of results, we should be in the clear!

![image](/assets/images/blog/wp-content/uploads/2008/09/image1.png)

Because it does not matter in which order these computations occur, we are able to safely parallelize without fear of subtle semantic errors cropping up at unexpected (and of course, unrepeatable) full-board demonstrations. Armed with the assurance which only comes from heady, unrealistic trivial algorithm analysis, we can start planning our attack.

### Threads Considered Insane

Being a good Java developer (despite the fact that we're using Scala), the very first thing which should come to mind when thinking of concurrency is the concept of a "thread". I'm not going to go into any detail as to what threads are or how they work since they really are concurrency 101. Suffice it to say though that threads are the absolute lowest-level mechanism we could possibly use (at least on this platform). Here we are, Fibonacci a-la `Thread`:

```scala def fib(n: Int): Int = { if (n < 2) n else { var t1 = 0 var t2 = 0 val thread1 = new Thread { override def run() { t1 = fib(n - 1) } } val thread2 = new Thread { override def run() { t2 = fib(n - 2) } } thread1.start() thread2.start() thread1.join() thread2.join() t1 + t2 } } ``` 

I can't even begin to count all of the things that are wrong with this code. For starters, it's ugly. Gone is that attractive one-liner that compelled us to pause and marvel. In its place we have a 25 line monster with no apparent virtues. The intent of the algorithm has been completely obscured, lost in a maze of ceremony. But the worst flaw of all is the fact that this design will actually require ( _n - 2)!_ threads. So to calculate the 10th Fibonacci number, we will need to create, start and destroy 40,320 `Thread` instances! That is a truly frightening value.

At first blush, it seems that we can alleviate at least _some_ of the insanity by using a thread pool. After all, can't we just reuse some of these threads rather than throwing them away each time? Unfortunately, this well-intentioned approach doesn't quite suffice. It turns out that we can't really pool very many threads due to the fact that we're utilizing a thread in `fib` to recursively call itself and then _wait for the result_. Thus, the "parent" dispatch is still holding a resource when the "child" attempts to obtain an allocation. Granted, we have reduced the number of required threads to a mere _2n - 4_ , but with a fixed size thread pool (the most common configuration), we're still going to run into starvation almost immediately. Apocalisp has [a more in-depth article](<http://apocalisp.wordpress.com/2008/09/02/a-better-future/>) explaining why this is the case.

### Something a Little Better...

For the moment, it looks like we have run into an insurmountable obstacle. Rather than mash our brains out trying to come up with a solution, let's move on and conceptualize how we _might_ want things to work, at least in syntax.

Clearly threads are not the answer. A better approach might be to deal with computational values using indirection. If we could somehow kick off a task asynchronously and then keep a "pointer" to the final result of that task (which has not yet completed), we could later come back to that result and retrieve it, blocking only if necessary. It just so happens that the Java 5 Concurrency API introduced a series of classes which fulfill this wish: (what a coincidence!)

```scala def fib(n: Int): Future[Int] = { if (n < 2) future(n) else { val t1 = future { fib(n - 1).get() } val t2 = future { fib(n - 2).get() } future { t1.get() + t2.get() } } } def future[A](f: =>A) = exec.submit(new Callable[A] { def call = f }) ``` 

I'm assuming that a variable called `exec` is defined within the enclosing scope and is of type `ExecutorService`. The helper method is just syntax, the real essence of the example is what we're doing with `Future`. You'll notice that this is much shorter than our threaded version. It still bears a passing resemblance to that horrific creature of yesteryear, but yet remains far enough removed as to be legible. We still have our issue of thread starvation to content with, but at least the syntax is getting better.

Along those lines, we should begin to notice a pattern emerging from the chaos: in both implementations so far we have started by asynchronously computing two values which are assigned to their respective variables, we then block and then merge the result via addition. Do you see the commonality? We start by _forking_ our reorderable computations and finish by _joining_ the results according to some function. This right here is the very essence of fork/join. If you understand this one concept, then everything else falls into place.

Now that we have identified a common pattern, we can work to make it more syntactically palatable. If indeed fork/join is all about merging asynchronous computations based on a given function, then we can invent a bit of syntax sugar which should make the Fibonacci function more concise and more readable. To differentiate ourselves from `Future`, we will call our result "`Promise`" (catchy, ain't it?).

```scala def fib(n: Int): Promise[Int] = { if (n < 2) promise(n) else { { (_:Int) + (_:Int) } =<< fib(n - 1) =<< fib(n - 2) } } ``` 

At first glance, it seems that all we have done is reduce a formerly-comprehensible series of verbose constructs to a very concise (but unreadable) equivalent. We can still make out our recursive calls as well as the construction of the base case, but our comprehension stops there. Perhaps this would be a bit more understandable:

```scala val add = { (a: Int, b: Int) => a + b } def fib(n: Int): Promise[Int] = { if (n < 2) promise(n) else { add =<< fib(n - 1) =<< fib(n - 2) } } ``` 

The only reason to use an anonymous method assigned to a value (`add`) rather than a first-class method is the Scala compiler treats the two differently in subtle ways. Technically, I _could_ use a method and arrive at the same semantic outcome, but we would need a little more syntax to make it happen (specifically, an underscore).

This should be starting to make some more sense. What we have here is a _literal_ expression of fork/join: given a function which can _join_ two integers, _fork_ a concurrent "process" (not in the literal sense) for each argument and reduce. The final result of the expression is a new instance of `Promise`. As with `Future`, this operation is non-blocking and very fast. Since the arguments themselves are passed in as instances of `Promise`, we literally don't need to wait for anything. We have now successfully transformed our original `fib` function into a non-blocking version. The only thing left is a little bit of syntax to "unwrap" the final result:

```scala val num = fib(20) num() // 6765 ``` 

Incidentally, the `=<<` operator was not chosen arbitrarily, its resemblance to the "bind" operator in Haskell is quite intentional. That is not to say that the operation itself is monadic in any sense of the word, but it does bear a _conceptual_ relation to the idea of "binding multiple operations together". The operator is inverted because the bind operation is effectively happening in reverse. Rather than starting with a monad value and then successively binding with other monads and finally mapping on the tail value (as Scala does it), we are starting with the map function and then working our way "backwards" from the tail to the head (as it were). None of the monadic laws apply, but this particular concurrency abstraction should tickle the same region of your brain.

### An End to Starvation

I half-promised a little while back that we would eventually solve the issue of thread starvation in the implementation. As mentioned, this particular issue was the central focus of an article on Apocalisp a few weeks back. For full details, I will again [refer you to the original](<http://apocalisp.wordpress.com/2008/09/02/a-better-future/>). In a nutshell though, it looks like this:

  * Operation A dispatches operations B and C, instructing them to send the result _back_ to A once they are finished 
  * Operation A releases the thread 
  * Operation B executes and sends the result back to A 
  * Operation C executes and sends the result back to A 
  * Operation A combines the two results and sends the final result back to whoever dispatched it 
  * _...and so on, recursively_

Rather than stopping the world (or at least, our little thread) while we wait for a sub-operation to complete, we just tell it to give us the result as soon as it's done and we move on. The whole thing is based around the idea of asynchronous message passing. The first person to say "actors" gets a gold star.

Every `Promise` is an actor, capable of evaluating its calculation and sending the result wherever we need it. The `=<<` builds up a "partially-applied asynchronous function" based on the original function value we specified (`add`), binding each `Promise` in turn to a successive argument (a nice side-benefit of this is compile-time type checking for argument binding). Once the final argument is bound, a full-fledged `Promise` emerges with the ability to _receive_ result messages from the argument `Promise`(s). Once every value is available, the results are aggregated in a single collection and then passed _in order_ to the function. The final result is returned and subsequently passed back to any pending actors. It's a classic actor pattern actually: don't block, just tell someone else to call you as soon as they are ready.

With this strategy, it is actually possible to execute the whole shebang in a single thread! This is because we never actually _need_ to be executing anything in parallel, everything is based on the queuing of messages. Of course, a single-threaded execution model would completely ruin the entire exercise, so we will just trust that Scala's actor library will choose the correct size for its internal thread pool and distribute tasks accordingly.

### Conclusion

In case you hadn't already guessed, I've actually gone and implemented this idea. What I have presented here is a bit of a distillation of the "long think" I've had regarding this concept and how it could be done. The only important item that I've left out is what Doug Lea calls the "granularity control". Basically, it's a threshold which describes the point at which the benefits of executing a task asynchronously (using fork/join) are outweighed by the overhead involved. This threshold can be seen in my [benchmark of the library](<http://www.codecommit.com/blog/misc/higher-order-fork-join-operators/PromiseDemo.scala>). Performance numbers look something like this (on a dual-core, 2 Ghz 32-bit processor):

**Calculate`fib(45)`**  
---  
Sequential Time | 14682.403901 ms  
Parallel Time | 7515.882423 ms  
Sequential Memory | 0.023438 KB  
Parallel Memory | 3131.548828 KB  
  
For the mathematically challenged, the results show that the parallel execution using `Promise` was **95.351698%** faster than the same operation run sequentially. That's _almost_ linear with the number of cores! Accounting for the overhead imposed by actors, I would expect that the impact on performance would approach linearity as the number of cores increases.

Fork/join isn't the answer to the worlds concurrency problems, but it certainly is a step in the right direction. Also, it's hardly a new approach, but it has generally remained shrouded in a murky cloud of academic trappings and formalisms. As time progresses and our industry-wide quest for better concurrency becomes all the more urgent, I hope that we will begin to see more experimentation into improving the outward appearance of this powerful design pattern.

  * Download [concurrent-0.1.0.jar](<http://www.codecommit.com/blog/misc/higher-order-fork-join-operators/concurrent-0.1.0.jar>) ([sources](<http://www.codecommit.com/blog/misc/higher-order-fork-join-operators/concurrent-source.zip>))
    * Requires [collections-0.1.0.jar](<http://www.codecommit.com/blog/misc/higher-order-fork-join-operators/collections-0.1.0.jar>) (for the [persistent vector implementation](<http://www.codecommit.com/blog/scala/implementing-persistent-vectors-in-scala>))