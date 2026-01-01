{%
laika.title = "Improving the STM: Multi-Version Concurrency Control"
laika.metadata.date = "2008-11-10"
%}


# Improving the STM: Multi-Version Concurrency Control

I wrote a post some time ago introducing a [software transactional memory (STM) framework for Scala](<http://www.codecommit.com/blog/scala/software-transactional-memory-in-scala>) based on implicit conversions.  This framework had some nice features, like a very clean syntax coupled with compile-time verification of transactional semantics, but it really wasn't ready for real-world use.  There was one very serious skeleton in the closet that I carefully avoided as I went through the presentation: non-deterministic behavior was still possible, even within a transaction.

### The Problem

Allow me to illustrate.  Imagine two transactions, each executing concurrently in a world with two reference cells.  One transaction reads from both cells, subtracts one from the other and then uses the result as a divisor.  The other transaction increments both cells in lock-step with each other.  Put into code, this would look something like the following:

```scala
val a = new Ref(1)
val b = new Ref(0)

def compute(x: Int)(implicit t: Transaction) = x / (a - b)

def interfere(implicit t: Transaction) {
  a := a + 1
  b := b + 1
}

atomic(compute(3)(_))         // in thread-1
...
atomic(interfere(_))          // in thread-2
```

Incidentally, I know that the whole `interfere(_)` syntax is a bit ugly.  Voice your opinion on RFE [#1492](<https://lampsvn.epfl.ch/trac/scala/ticket/1492>).

This series of computations, while useless, should work just fine under normal circumstances.  The STM is going to verify that neither transaction is stepping on the other _with any lasting effect_ , but unfortunately there are things that can go wrong before we get to "lasting".  It may not be obvious, but this code actually has a very real probability of throwing an `ArithmeticException` due to a division by zero.

This unfortunate circumstance arises when the "interfere" transaction commits after the "compute" transaction has read a value for `a` but _before_ it reads a value for `b`.  When this occurs, `compute` will obtain a value for `b` which is precisely the same as the value it has already read from `a`.  Now, `interfere` has also changed the global state of `a` to preserve its property of leading `b` by 1, but there is no way for `compute` to know that: it has already extracted a value for `a`, it's not going to check to ensure that it's still sacred.  What happens next is of course trivial: with `a` and `b` having the same value, the subtraction operation results in `0`, causing the division to fail.

Note that this issue does not indicate a failure in the _safeguards_ , just an incomplete specification for the STM.  If the reference fault had not resulted in an exception, the commit process for the "compute" transaction would have caught the problem and retried the transaction.  In other words, if the problem hadn't caused a catastrophic failure within the transaction, the STM would have gracefully recovered and no one would have been any the wiser.

When I created the STM implementation, I recognized that this case can indeed arise.  To compensate for this, I created a dumb catch-all within the transaction dispatch process.  This code literally swallows all exceptions coming from within the transaction, treating them as signals to abort and retry.  This works-around our exceptional-interferance issue from above, but it does bring consequences of its own:

```scala
def badBoy(implicit t: Transaction) {
  throw RuntimeException("Er...")
}
```

This transaction will _always_ throw an exception, regardless of the state of the references or any other transactions.  With the originally envisioned design, this exception would have simply propagated out from where the transaction was kicked off (wherever the `atomic` method was called) and life would have been happy.  Unfortunately, with our catch-all in place (required to solve the earlier problem) this code will fall into an infinite loop.

The problem is that the catch-all will field the `RuntimeException` and assume that it's a divine signal that trouble is on the way.  This assumption that data-integrity has been somehow compromised leads the STM to simply retry the transaction.  However, since the exception is thrown every time, the catch-all will just keep on swallowing the same exception, endlessly-retrying the same tired transaction.  Naturally, this is just a trivial example, but it's not difficult to imagine a block of code which really _does_ throw a legitimate exception, one which we would want to propagate back into user-land where we can effectively deal with the situation.  An infinite loop of failing transactions is not the right way to handle the problem.

Speaking of infinite loops, we still haven't really solved our interference problem from above.  In our first snippet, we created a data contention situation which could non-deterministically cause an exception to be thrown.  In order to solve this problem, we introduced a catch-all to swallow the exception and just keep right on trucking.  This "solution" not only introduces some undesired behavior (swallowing legitimate exceptions), but it also fails to really fix the issue.  Consider the following slight variation on our previous theme:

```scala
val a = new Ref(1)
val b = new Ref(0)

def maybeLoop(implicit t: Transaction) {
  while (a <= b) {
    // should never happen
  }
}

def interfere(implicit t: Transaction) {
  a := a + 1
  b := b + 1
}
```

In case it isn't obvious, the `while`-loop in `maybeLoop` should never actually execute.  We have defined references `a` and `b` to have values such that `a` is always strictly greater than `b`.  The only transaction which modifies these values preserves this property, and so by easy induction we can prove that the conditional "`a <= b`" is in fact a contradiction.

Unfortunately, our little hand-wavy proof doesn't take non-determinism into account.  Remember from the first example where the "interfere" transaction committed between the reads for `a` and `b`?  Earlier, this just raised an `ArithmeticException`.  However, in this case, such a conflict would actually cause `maybeLoop` to suddenly become unterminating!  Because of the way that this data contention falls out, we could end up with an infinite loop where we had expected no loop at all.

This is very, very bad for a number of reasons.  With our exception problem, we were just able to introduce a catch-all to get around the issue.  The same trick isn't going to work with an infinite loop.  Control is never returned to the transaction controller, so there is no way to inject any logic in this case.  What's (possibly) worse is there is no way for us to even _try_ to detect these sorts of situations.  This is actually a slight specialization of [the halting problem](<http://en.wikipedia.org/wiki/Halting_problem>).  Briefly: because any Turing Complete language must be capable of non-terminating execution, the introduction of non-deterministic evaluation into such a language can result in non-deterministic non-terminating execution.  Since it is impossible in general for one Turing Machine to determine whether or not another will halt on a given input, it must follow that it is also impossible to determine whether or not a non-deterministic Turing Machine will halt.  My logical form is atrocious, but I think you get the picture.

### The Solution

The point is that we have a problem, and cop-out solutions like a catch-all exception handler isn't going to solve it.  The only real answer[1](<#1>) would be to somehow ensure that when "interfere" commits, it doesn't actually change the values of `a` and `b` as visible to the other running transaction.  In other words, transactional commits should change the global state, but leave any transaction-local state untouched.  That way, in-progress transactions can work with the reference values which were in place at the start of the transaction, deterministically work their way down to the bottom and finally either commit or detect conflicts and retry.  With this solution, exceptions can be propagated out to user-land because every exception will be valid, none will ever be caused by a reference fault.

In a nutshell, this strategy is like giving each transaction its own "snapshot of the world".  From the moment the transaction starts, it remains entirely oblivious to any changes going on in the world outside.  It's as if each transaction got to stop the universe, do its work, commit and then allow things to pick up right where they left off.  However, instead of actually stopping the universe, each transaction will preserve all reference state from that exact moment and allow everyone else to keep right on rolling.  In technical terms, this is called MVCC: **M** ulti- **V** ersion **C** oncurrency **C** ontrol.

If we apply this technique to our first dilemma involving `compute` and `interfere`, we will find that even when `interfere` commits between the reads of `a` and `b`, there really is no issue.  Rather than getting a new value of `b` which is inconsistent with its already-retrieved old value of `a`, the "compute" transaction will just see the old values for _both_ `a` and `b`.  None of the changes to the reference world outside of `compute` are observed until after the transaction completes.  At that point, the STM will detect any reference faults (such as `interfere` causing interference) and if necessary, retry the transaction.

### The Implementation

This all sounds quite well-and-good, but how would we go about creating such a magical fairy land in the sky?  The most intuitive approach would be to copy _every_ live reference whenever we start a transaction.  This isn't really as bad as it sounds, seeing as any data contained within a reference must be immutable, so copies are literally free (just use the original data).  Unfortunately, that doesn't stop the entire process from being heinously inefficient.  It's easy to imagine a system with 100,000 references and a tiny little transaction which only uses 2 of them.  Do we really want to loop through 100,000 references just to preserve a pair of them?

Obviously, a different approach is needed.  That's where the committal process comes into play.  With the old design, when a transaction commits, it would first check for any reference faults, then if everything was dandy it would write the necessary changes to the global state for each reference.  In code it looked like this:

```scala
def commit() = {
  if (world.size > 0) {
    CommitLock.synchronized {
      val back = world.foldLeft(true) { (success, tuple) =>
        val (ref, _) = tuple
        success && ref.rev == version(ref)
      }
      
      if (back) {
        for (ref <- writes) {
          ref.contents = (world(ref), rev)
        }
      }
      
      back
    }
  } else true
}
```

MVCC is going to require this `commit` method to do a little extra legwork.  Rather than _just_ writing the changes into the global state, `commit` will need to loop through any active transaction, saving the old values _for only the modified references_ within each of these transaction's logs.  This dramatically reduces the amount of looping and saving which takes place without actually imposing too much extra overhead.  This change - combined with a separation of `reads` and `writes` within a transaction - actually looks like the following:

```scala
def preserve[T](ref: Ref[T]) {
  val castRef = ref.asInstanceOf[Ref[Any]]
  
  if (!world.contains(castRef)) {
    val (v, rev) = ref.contents
    
    world(castRef) = v
    version(castRef) = rev
  }
}

def commit() = {
  if (world.size > 0) {
    CommitLock.synchronized {
      val f = { ref: Ref[Any] => ref.rev == version(ref) }
      val back = reads.forall(f) && writes.forall(f)
      
      if (back) {
        for (ref <- writes) {
          for (t <- Transaction.active) {
            if (t != this) t.preserve(ref)       // preserve the old contents of t
          }
          
          ref.contents = (world(ref), rev)
        }
      }
      
      back
    }
    
  } else true
}
```

Amazingly enough, this tiny little change is all that is required to implement MVCC within our STM.  Well, of course I'm skipping the implementation of `Transaction.active` as well as the bitsy concurrency semantics required to make it all work, but you weren't really interested in any of that, were you?

For those who are keeping score, [Clojure](<http://clojure.org>) already has MVCC implemented within its hallmark STM.  As a point of interest, the implementation is subtly different from the one I have just outlined.  Instead of placing the old reference values within the local transaction logs, Clojure stores these contents as old versions of the `TVar` within the reference itself.  I'm not sure if I like this approach better or not, but it certainly seems to work well!  In either case, the semantics are essentially identical.

Final aside worthy of mention: you need not concern yourself that all of this extra copying is going to eat through heap space.  This is one of the huge benefits of immutable data: copies are free.  We aren't really copying the data, just maintaining a reference to its previous version.  Once the transaction commits, its log will go out of scope and that old reference will be eligible for garbage collection.  All in all, the addition of MVCC to our STM doesn't raise the overhead even the slightest bit.

### Conclusion

Realizing that my blog was not the most convenient way to distribute project files for a semi-serious library like the Scala STM, I decided to [put the project on GitHub](<http://github.com/djspiewak/scala-stm/tree/master>).  For those who hate following links, you can checkout, build and test (see below) the library using the following commands (assuming you have both Git and [Buildr](<http://incubator.apache.org/buildr/>) installed):

```
git clone git://github.com/djspiewak/scala-stm.git
  cd scala-stm
  buildr
```

In a flurry of activity besides the addition of MVCC, I have also added some reasonably-comprehensive BDD specs to ensure that the correctness of the implementation isn't just in my head.  Of course, all of the tests are probabilistic, but I think the library is finally to a point where they can be relied upon.  If you like, I'll even distribute a proper pre-built JAR.

STM is hardly in its infancy, but it is just now starting to catch on as a viable alternative to the classic locking techniques favored by concurrent programs everywhere.  Improvements like MVCC promise even better reliability and throughput, further advancing the goal of deterministic asynchronous programs which are easy to develop and even easier to reason correct.

[1](<#1-back>) There actually is another solution involving conflict detection on the fly, but it's a little inefficient and not really as subjectively elegant as MVCC.