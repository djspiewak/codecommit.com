---
categories:
- Scala
date: '2008-10-06 00:00:00 '
layout: post
title: Software Transactional Memory in Scala
wordpress_id: 266
wordpress_path: /scala/software-transactional-memory-in-scala
---

The fact is that there are a lot of problems that are hard to solve in a purely-functional style. That's not to say that no solution exists, but certain problems are very difficult to model without shared state. In such situations, a slightly different approach to concurrency must be considered. Actors are inapplicable, seeing as they are the embodiment of "shared-nothing" continuation passing, and [fork/join](<http://www.codecommit.com/blog/scala/higher-order-fork-join-operators>) doesn't really help us. Usually, when faced with the need for shared mutable state, most developers will resort to the old-fashioned technique of locking everything and controlling access to that state one thread at a time. The solution often goes something like this:

```java public class Container { private int value; private final ReadWriteLock lock = new ReentrantReadWriteLock(); public int getValue() { lock.readLock().lock(); try { return value; } finally { lock.readLock.unlock(); } } public void setValue(int value) { lock.writeLock().lock(); try { this.value = value; } finally { lock.writeLock.unlock(); } } } ``` 

Obviously, most scenarios which call for such locking are a bit more complex, but you get the picture. There are two very serious problems with locking:

  * Implementation rules are _ad hoc_
  * Throughput is reduced 

The first issue is obvious: there is nothing actually _preventing_ us from accessing `value` without locking. What's worse, it would be just as easy to lock, access `value` and then forget to unlock when we were done. There is no logical connection between `value` and its lock, the implementation rules are solely enforced by convention. Of course, we can avoid the problem of forgetting to unlock by using a locking strategy which is lexical in nature, but in that case our second issue (throughput) becomes even more pronounced:

```java private int value; private final Object lock = new Object(); public int getValue() { synchronized (lock) { return value; } } public void setValue(int value) { synchronized (lock) { this.value = value; } } ``` 

The problem here is that everything blocks everything else. If one thread wants to retrieve value, it literally stops the world while the other threads wait their turn. Data integrity is guaranteed, but this was accomplished only by creating a serial bottleneck on read/write access to value. On top of that, we still have no enforced correlation between the lock and its corresponding piece of data.

### "Glass is Half Full" Concurrency

What we have been doing here is called "pessimistic locking". We have literally started with the assumption that we are going to run into data contention issues. Our entire locking mechanism is designed with the worst case scenario in-mind: lots of threads trying to write to the same piece of data simultaneously. However, not _every_ concurrent system is going to have constant interference between threads. As it turns out, in practice data contention is the exception rather than the rule. Given this, it would be very nice if we could design a system which gave threads the "benefit of the doubt", assuming that they will _not_ conflict, while still somehow maintaining data integrity.

The answer to this is "optimistic locking". We turn the problem completely on its head. Instead of assuming that there will be problems and so locking everything preemptively, we assume that everything will be fine and just allow everyone free, non-blocking access. Of course, an "open-door policy" with regards to shared mutable state isn't enough in and of itself, we have to have some rule for dealing with contention issues, and some way of detecting those conflicts when they happen. Enter transactions...

The idea behind a purely-optimistic transactional memory model is that all write operations must occur within a transaction. Data can be read any time, but the memory model will ensure that the data is never in an "intermediate state" - or between writes. Let's consider the more complicated scenario of the transfer of funds from one bank account to another. The steps are as follows:

  1. Withdraw $500 from account **A**
  2. Deposit $500 into account **B**

Of course, this isn't really an algorithm, it's a high-level overview. We're really looking at something a bit more complicated than it would seem:

```scala def transfer(amount: Int, a: Account, b: Account) { a.balance = a.balance - amount b.balance = b.balance + amount } ``` 

I avoided using the `+=` and `-=` operators so as to illustrate the problem more explicitly. This operation has four separate operations on shared mutable state (the account balance). It's not too difficult to see how this could go terribly wrong in the case where two separate transfers are taking place simultaneously. For example, we could transfer $500 from account **A** into account **B** , while _at the same time_ we transfer $200 from account **B** into account **C**. Remember, we're not locking anything, so we run the danger that our concurrent execution order will be interleaved in the following fashion:

  1. `[Thread1]` Get balance of account **A**
  2. `[Thread1]` Set balance of account **A** to its former amount less 500 
  3. `[Thread1]` Get balance of account **B**
  4. `[Thread2]` Get balance of account **B**
  5. `[Thread1]` Set balance of account **B** to its former amount plus 500 
  6. `[Thread2]` Set balance of account **B** to its former amount less 200
  7. `[Thread2]` Get balance of account **C**
  8. `[Thread2]` Set balance of account **C** to its former amount plus 200 

The operation in red is the one we need to be concerned about. `Thread2` retrieved the balance of account **B** just prior to when it was modified by `Thread1`. This means that when `Thread2` calculates the new balance (less 200), it will be basing its result on a now-obsolete balance. When it sets the balance, the $500 that was transferred from account **A** will mysteriously vanish, the unfortunate victim of a common race condition.

In a transactional system, both transfers would be separately handled in their own transaction. Neither of them would modify the other's data until all operations are completed, at which point the transaction would commit and the data would become "live". Thus, when the `Thread1` adds $500 to account **B** , the actual balance of account **B** will remain constant at its original value (prior to the transaction). Once the transaction commits, both the balance of **A** and **B** will be updated essentially simultaneously. There will never be a point where $500 is discovered missing "in transit" between two accounts.

This is only half the equation though. The real genius of the transactional model is just prior to committing, a transaction _validates_ itself, ensuring that all of the data it worked with is still in the same state it was at the beginning of the transaction. If something has changed, then the transaction is invalidated and must be re-run automatically. Our issue from above can never happen because the `Thread2` transaction will attempt to validate itself, only to discover that the balance of account **B** has been changed in the meantime. Rather than yield to the race condition, the `Thread2` transaction will throw away its work and start from scratch. Assuming that nothing else is running, the second validation will be successful (since nothing will have changed concurrently) and the transaction will commit.

It all sounds much more complicated than it actually is. People familiar with modern databases like Oracle should already be comfortable working with optimistic transactional models. The technique is a little less common in software, but it can still be applied.

In order to make this work in a conventional application setting, we need to introduce a few more abstractions. There are several ways to go about this, but I have chosen to follow the model laid down by Rich Hickey in his implementation of Clojure's STM. In turn, Clojure seems to take a fair bit of inspiration from Haskell's STM monad, although it does not port over concepts like transaction composition and what Simon Payton Jones calls "choice". Basically, the design can be distilled as follows:

  * Each item of shared state must be stored in a _reference_
  * References can be read at any point, but they can only be modified within a transaction 
  * The data contained within a reference must itself be immutable, the reference simply allows you to switch the data it contains 
  * Within a transaction, reading a reference does not return its current value, but rather the value it had precisely when the transaction began. This allows data to change outside the transaction without disrupting its internal processing. 
  * Changes made to a reference inside a transaction are not world-visible until the transaction completes and is committed. Validation ensures that no data is lost during the commit. 
  * Transactions must _never_ contain side-effects as they may be executed multiple times 

With all that in mind, let's get to work on an implementation!

### References

Since these are the primitive building blocks of our STM, it seems logical that we should start here. At an extremely basic level, we will need a structure which looks something like the following:

```scala class Ref[T](private var value: T) { def get = value def :=(value: T) { this.value = value } } ``` 

This is the basic idea anyway. We want to be able to retrieve a value from a reference using `get`, and we want to be able to store a value in the reference using the `:=` operator (hat tip to all of you rabid Pascal fan-boys). Unfortunately, we haven't really accomplished anything here. Yes, we have a reference to wrap around a mutable piece of data, but there are no associated concurrency semantics. Remember the definition of a reference? We must _only_ be able to write to it within a transaction. Furthermore, the value returned from a reference within a transaction is not necessarily its current, world-visible state, but rather the data it had the moment the transaction began.

In order to accommodate these requirements, we will introduce the concept of a _context_. Both read and write access to a reference will require a context to be present. We will have one context for the entirety of world-visible state. Additionally, each transaction will have its own context. In that way, we can ensure that transaction modifications are kept local to itself prior to commit while at the same time preventing changes from other transactions from becoming visible after the transaction has started (potentially leading to data integrity problems). Our API has now evolved to something more like the following:

```scala class Ref[T](private var value: T) { // ??? def get(c: Context) = c.retrieve(this) def :=(value: T)(c: Context) { c.store(this)(value) } } ``` 

I left the constructor undefined because of the fairly obvious problem in this implementation: how does the `Context` get the values in the first place? For the moment, let's put that problem aside and deal with a more interesting one: limiting write access to within transactions. Recall that we're going to have two different kinds of contexts: a live context which is global, as well as a context which is local to the transaction. The requirement alone implies a way to _statically_ restrict reference mutability to within transactions: require a type of context _other_ than the live one. To this end, we will derive the following closed hierarchy. It is closed because there will be no other `Context` implementations, preventing over-zealous API extensions from fouling up our semantics.

![image](/assets/images/blog/wp-content/uploads/2008/10/image.png)

We will define `Context` to be an abstract class and `LiveContext` to be a singleton object. Each transaction will have its own `Transaction` context which it will use in both read _and_ write operations. `LiveContext` will only be used outside of a transaction, when there is no other context available. To enforce this, we will restrict the type of the `Context` taken by the reference assignment operator to only accept `Transaction`:

```scala def :=(value: T)(c: Transaction) { c.store(this)(value) } ``` 

With this in mind, we can start to envision what the syntax for reference operations will look like:

```scala def transfer(amount: Int, a: Ref[Int], b: Ref[Int])(c: Transaction) { a.:=(a.get(c) - amount)(c) b.:=(b.get(c) + amount)(c) } // ... val accountA = new Ref[Int](1500) val accountB = new Ref[Int](200) // somehow call `transfer` as a transaction println("Account A: " + accountA.get(LiveContext)) println("Account B: " + accountB.get(LiveContext)) ``` 

And just when our design was looking so nice too. We've succeeded in preventing _at compile time_ the modification of references outside of transactions, but we did it at the cost of a tyrannical syntax. Fortunately, Scala has a handy mechanism for cleaning up syntax such as this, one which should reduce the volume of annoying bulk by several orders of magnitude: implicits.

IScala makes it possible to mark parameters as accepting implicit values. These parameters in turn become implicit values themselves. When a method which accepts an implicit parameter of a specific type is called with an implicit value of the same type in scope, the parameter can be omitted entirely. Thus, by marking `LiveContext` as an implicit object and appropriately annotating the last parameter of `transfer` as well as the `Context` parameters of `Ref`'s accessor and mutator, we can eliminate almost all of the annoying bulk in the above example:

```scala implicit object LiveContext extends Context { ... } class Ref[T](value: T) { // ??? def get(implicit c: Context) = c.retrieve(this) def :=(value: T)(implicit c: Transfer) { c.store(this)(value) } } ``` 

With these extra modifiers in place, we can redo our transfer snippet to see how things look. To cut down on line length, we will also assume that `accountA` and `accountB` are references in some sort of global scope:

```scala val accountA = new Ref[Int](1500) val accountB = new Ref[Int](200) def transfer(amount: Int)(implicit t: Transaction) { accountA := accountA.get - amount accountB := accountB.get + amount } // somehow call `transfer` as a transaction println("Account A: " + accountA.get) println("Account B: " + accountB.get) ``` 

Pretty slick, and we aren't even finished yet! We can add an implicit conversion from `Ref[T]` to `T`, eliminating the need to call `get` anywhere in the code:

```scala implicit def refToValue[T](ref: Ref[T])(implicit c: Context) = { ref.get(c) } ``` 

...and the final example syntax:

```scala val accountA = new Ref[Int](1500) val accountB = new Ref[Int](200) def transfer(amount: Int)(implicit t: Transaction) { accountA := accountA - amount accountB := accountB + amount } // somehow call `transfer` as a transaction println("Account A: " + accountA) println("Account B: " + accountB) ``` 

With the exception of the `:=` syntax (which is unfortunately unavoidable), you would never be able to tell that references are being used rather than conventional vars. Even better, we have managed to preserve our static assurance that no reference may be modified outside of a transaction. If we were to attempt to call the `:=` method without an instance of `Transaction` on-hand, the Scala type checker would complain and our code would not compile. The we need to do to close the loop is to make sure that the only place a `Transaction` instance can be obtained is inside a transaction (seems logical).

### Atomic

For the moment, let's shelve all of the implementation requirements for the STM and instead focus on the API. We already have an almost elegant syntax for references, but we're still missing one final piece: initiating the transaction. Being incredibly imaginative and innately creative, I decided the best way to devise an API for this would be to hit Wikipedia. After all, why come up with something original when someone smarter has already solved the problem?

``` // Insert a node into a doubly-linked list atomically atomic { newNode->prev = node; newNode->next = node->next; node->next->prev = newNode; node->next = newNode; } atomic (queueSize > 0) { remove item from queue and use it } ``` 

**Credit:** [Software transactional memory # Proposed language support](<http://en.wikipedia.org/wiki/Software_transactional_memory#Proposed_language_support> "http://en.wikipedia.org/wiki/Software_transactional_memory#Proposed_language_support")

It's a fairly straightforward API, one which could be expressed in Scala as it stands if we really wanted to. Obviously the body of the first transaction is a little closer to C than Scala, but we can just take that as example pseudo-code and move on. The real core of the API is a single method, `atomic`, which takes a function value and executes that function as a transaction. If a conditional is provided, it acts as a guard. If the guard ever returns `false`, the transaction aborts and is not retried.

The only problem here is we haven't accounted for our implicit `Transaction` parameter. Intuitively, we could just add a parameter of type `Transaction` to the function value, but unfortunately Scala doesn't allow anonymous functions with implicit parameters. That leaves one of two options: either take the `Transaction` as a parameter to the anonymous function and then store it in an implicit value _within_ the function; or pass an actual method which takes an implicit parameter. In truth, either approach will work, but for the remainder of the article I will use the method-passing approach, rather than the separate assignment within the anonymous method.

Altogether, our API in action looks something like this:

```scala val accountA = new Ref[Int](1500) val accountB = new Ref[Int](200) def transfer(amount: Int)(implicit t: Transaction) { accountA := accountA - amount accountB := accountB + amount } atomic(transfer(500)(_)) // run transaction println("Account A: " + accountA) println("Account B: " + accountB) ``` 

Notice the extra `(_)` syntax in the call to atomic. (thanks, [Jesper](<http://jnordenberg.blogspot.com/>)!) This is required because transfer accepts an implicit parameter. Without it, Scala doesn't know whether we mean to call the method using some implicit value in scope or if we want the function value itself.

Remember this API. We'll come back to the implementation of this method after we have completed the rest of the framework. For now, let's move onto context...

### Context

Returning to the dark and mysterious internals of the implementation, we now come to the deep morass of `Context`. As it turns out, everything in the STM will revolve around this class and its two separate implementations. Recall that it is responsible for retrieving reference values, controlling what data is visible from within a transaction and what data is live. Generally speaking, we are going to have the following design with respect to where reference data is handled:

  * If in `LiveContext`, access the One True copy of the data. 
  * If in `Transaction`, access the One True copy _if and only if_ there is no transaction-local version. Once the reference has been read, cache the data and return that value from all future reads within the transaction. We're cheating a bit here since we aren't taking a snapshot of the world on transaction start, we're waiting for the first reference read. As long as we get our validation right, this should be ok. 

Logically, this is a simple default operation clouded by a fairly substantive special case (within a transaction after the reference has been read or written once). To handle the special case, it only makes sense that we use a map from reference to the data to which it corresponds in that transaction. Remember that the data within a transaction may be a bit behind the live copy.

We could use another map for the general case, but we can make things even simpler than that. Rather than having a global map from reference to value, we can just store the live values within the `Ref` objects themselves. They will still need to delegate to their context to retrieve and store that value, but the actual live version can be kept locally. This simplifies `LiveContext` tremendously:

```scala class Ref[T](private[stm] var value: T) { def get(implicit c: Context) = c.retrieve(this) def :=(value: T)(implicit t: Transaction) { t.store(this)(value) } } // ... sealed abstract class Context { def retrieve[T](ref: Ref[T]): T } implicit object LiveContext extends Context { def retrieve[T](ref: Ref[T]) = ref.value } ``` 

In this iteration, we have simplified `Context` a little bit, seeing as it is never actually used for storage. Notice that all we need to do in `LiveContext` is delegate right back to the reference. This may seem like a bit of superfluous indirection but it becomes absolutely essential once we start considering `Transaction`:

```scala import collection._ final class Transaction private[stm] () extends Context { private val world = mutable.Map[Ref[Any], Any]() def retrieve[T](ref: Ref[T]) = { val castRef = ref.asInstanceOf[Ref[Any]] if (!world.contains(castRef)) { world(castRef) = ref.value } world(castRef).asInstanceOf[T] } def store[T](ref: Ref[T])(value: T) { val castRef = ref.asInstanceOf[Ref[Any]] world(castRef) = value } } ``` 

This is the reason we needed to redirect through retrieve rather than just grabbing the value directly in `Ref`. Within a transaction, any dereferencing will polymorphically come to an instance of this class. The mutable map, world, handles the transaction-local cache of all values once they have been accessed. Thus, the reference can change after we have looked at it (when another transaction commits) and it doesn't affect the values local to our transaction. This technique is exceedingly powerful and in no small part responsible for the higher throughput made possible by the transactional model.

Incidentally, it is worth noting that within a transaction, references are not thread safe. Thus, if you start a transaction and then start manipulating references concurrently within that same transaction, bad things will happen. This isn't really a problem though because transactions are always designed to be single-threaded from start to finish. They are _used_ in multi-threaded situations, they do not _use_ multiple threads.

### Commitment

Now that we have the basic nuts and bolts of our STM framework, we need to start considering how we are going to commit transactions. The process is two fold: first, we validate all references either read or written to by the transaction, checking for anything which may have changed in the interim; and second, we copy all new data from our transaction-local cache into the live references. For the sake of simplicity, we will only allow one transaction to commit at a time. We could do a little better, but this should work just fine for the experimental stuff.

Validation is a toughy, but the second commit step is fairly easy to satisfy: just loop through a set of all writes and copy the changes into the corresponding `Ref`. The necessary changes are as follows:

```scala final class Transaction private[stm] () extends Context { private val world = mutable.Map[Ref[Any], Any]() private val writes = mutable.Set[Ref[Any]]() ... def store[T](ref: Ref[T])(v: T) { val castRef = ref.asInstanceOf[Ref[Any]] world(castRef) = v writes += castRef } def commit() = { CommitLock.synchronized { // TODO validate for (ref <\- writes) { ref.value = world(ref) } } true } } private[stm] object CommitLock ``` 

The `commit` method returns a `Boolean` value indicating whether or not the commit was successful. If a conflict was detected the method returns `false` and (presumably) the transaction will be tried again. Since we aren't doing any validation just yet, we will always return `true`. Note that we don't really have to lock anything related to the `Ref` instances in order to write their data. It is possible for another thread to be reading data from these same references at precisely the same time as we are writing to them. However, primitive memory operations are atomic by definition, meaning that we don't need to worry about data integrity on the level of a reference value.

Validation is actually the most important part of the transaction commit process and quite possibly the most important facet of the entire STM concept. Without it, there is nothing to prevent data integrity from breaking down and causing problems. (remember that $500 we lost?) Unfortunately, our system doesn't quite have the chops yet to support any sort of transaction validation.

In order to validate a `Ref`, we need to compare its state to the state it was in at the moment we read from it or wrote to it (meaning into the transaction-local cache). We can't just compare values, partially because equals isn't fast enough, but also because it doesn't provide a strong enough guarantee about whether or not data has changed. What we need is some value which we control which indicates deterministically the current state of a `Ref` and which can be used later to determine if that state has changed. In short, we need a revision number:

```scala class Ref[T](value: T) { private[stm] var contents = (value, 0) // init revision to 0 } ``` 

The revision numbers have to be controlled statically with thread-safety, so the best place for them should be in the `Transaction` singleton. `Transaction` (the companion object for `Transaction` the `Context`) also contains our implicit conversion as well as that enigmatic `atomic` method that we still have yet to implement.

```scala object Transaction { private var rev_ = 1 private val revLock = new AnyRef private def rev = revLock.synchronized { val back = rev_ rev_ += 1 back } ... } ``` 

Empowered by a revision increment system which is guaranteed to produce unique values for each invocation, we can expand upon our concept just a little bit. Each `Transaction` will have a unique revision number associated with it (maintained within the `rev` field). Assuming a transaction successfully commits, it will not only modify the value of the references in question but also their revision, which it will set to its own number.

This revision system can be used in validation of transaction commit. Whenever we read or write to a Ref for the first time, we will store its current revision number within the transaction. When it comes time to commit the transaction, we can loop over our revision map and compare with the actual revision of the reference in question. If all of the expected revisions match up with reality, the transaction checks out and we can go ahead and commit. Otherwise, we have to assume that a transaction operating concurrently modified a reference we used and committed after we started our own transaction. Once this is known, we can't simply commit over the other transaction's changes, throwing away all of that money. Our transaction _must_ be retried from scratch.

Now that we know how to validate, we can finally look at a completed version of commit (and supporting cast):

```scala final class Transaction private[stm] (val rev: Int) extends Context { private val world = mutable.Map[Ref[Any], Any]() private val writes = mutable.Set[Ref[Any]]() private val version = mutable.Map[Ref[Any], Int]() def retrieve[T](ref: Ref[T]) = { ... if (!world.contains(castRef)) { ... if (!version.contains(castRef)) { version(castRef) = castRef.contents._2 } } ... } def store[T](ref: Ref[T])(v: T) { ... if (!version.contains(castRef)) { version(castRef) = ref.contents._2 } ... } def commit() = { CommitLock.synchronized { val back = world.foldLeft(true) { (success, tuple) => val (ref, _) = tuple success && ref.contents._2 == version(ref) } if (back) { for (ref <\- writes) { ref.contents = (world(ref), rev) } } back } } } ``` 

It's a lot of code, but all fairly straightforward. The validation simply bears out our intuition: checking the revisions initially retrieved from the references with their current values. Here again we are making use of the fact that memory access is atomic. We don't need to worry about a revision changing out of sync with a value because both of them are encapsulated by a 2-tuple within the `Ref` itself. Meanwhile, the validation can be trusted because of the `CommitLock`: we don't need to worry about another transaction committing between our validation and when we actually get around to saving our values.

### Atomic 2.0

I said we would come back to this, and here we are! We never did implement the `atomic` method, which is a bit of a shame seeing as it is what is responsible for kicking off the entire transactional process. Not only that, but it creates the `Transaction` instance, ensures that the transaction gets committed once it has finished and it retries if that commit fails. Set in code, it looks something like this:

```scala def atomic[A](f: (Transaction)=>A): A = atomic(true)(f) def atomic[A](cond: =>Boolean)(f: (Transaction)=>A) = { def attemptTransact(): A = { if (cond) { val trans = new Transaction(rev) try { val result = f(trans) if (trans.commit()) result else attemptTransact() } catch { case _ => attemptTransact() // if exception, assume conflict and retry } } else null.asInstanceOf[A] } attemptTransact() } ``` 

The only weird thing here is the use of an internal function to control the transaction dispatch process. This is necessary because we need to emulate the pattern of a `do`/`while` loop without losing the ability to capture a return value. This is the one minor feature of the transaction API that we have designed which I haven't already discussed: the ability to return a value from a transaction. Practically speaking, this isn't needed _too_ often since the very purpose of a transaction is to modify references, but it is still a pattern worth keeping in hand.

You will notice that we have some generic catch-all exception handling going on here. Whenever a transaction throws an exception, we assume that it has failed and we try again. To be honest, I wrestled back and forth with this decision. After all, if a transaction comes across a `NullPointerException` on its first try, it's not likely to do any better the second time around, or the third, or the fourth, or the... On the other hand, there is a remote but very real possibility that data can briefly get into an inconsistent state within a transaction. To understand how, consider the following abstract scenario:

  1. Transaction **A** writes data to reference **A** and reference **B**
  2. Transaction **B** starts after transaction **A** but _before_ it has committed 
  3. Transaction **B** reads from reference **A**
  4. Transaction **A** commits 
  5. Transaction **B** reads from reference **B**
  6. Transaction **B** does the funky chicken and tries to commit 
  7. Transaction **B** fails validation and has to try again 
  8. _Everything works fine on the second time around_

Everything gets straightened out in the end due to the validation, but there is still this hairy moment within transaction **B** that we have to worry about. Within the transaction, we have some data from before transaction **A** committed, and some from afterward. Validation is never going to let this slip by, but while the transaction is still executing we can run into some fairly serious issues.

Imagine for example that reference **B** is changed in such a way that it throws an exception in transaction **B** unless it is paired with just the right value of reference **A**. Since we have an inconsistent pairing between references **A** and **B** within transaction **B** , we will get an exception caused directly by a breakdown in data integrity. Because this is a data integrity issue, we want this exception to trigger a redo in the transaction. However, we can't really predict such exceptions, so there's no way we can distinguish between a legitimate exception (which should be propagated outside the transaction) and a data integrity fault. In short, we're in trouble.

Given the way we designed the STM framework, I don't really see a nice, efficient way to avoid this problem. There are other approaches we could take which don't have this issue, but that would either require an entirely different implementation or a much smarter developer taking the lead. I would be interested to see how Clojure handles this case...

### A Less-Trivial Example

Now that we have this full STM framework, it might be interesting to put it to work. To that end, let's create a simple market simulation with three businesses and a hundred potential customers, each with their own accounts and separate balances. All of these entities will have their own thread operating on their behalf, making purchases, excepting refunds and keeping the Mafia off their back. Bear in mind that this is a _truly concurrent_ simulation with full thread semantics. We're not going to use actors or anything like that to reduce the number of threads involved, everything will operate in true parallel.

To make things a little more interesting, we will also associate a 7.5 % fee with each transfer, paid to a separate account. Also, each transfer will be logged presumably for later review. To make everything fun, we will have one final thread which monitors the entire market, summing up _every_ account and checking the total. The obvious concern is that some collision in access of the shared data will lead to the unexpected loss (or gain) of wealth. So long as the total market value remains constant, we can assume that shared state is being handled appropriately.

Let's start out by defining our accounts and basic structure. Bear in mind that `Account` is a type alias (similar to a C-style typedef) for `Ref[Long]`, it is not a separate class.

```scala object BankFrenzy { import Transaction._ type Account = Ref[Long] private val fees = new Account private val log: Ref[Vector[Transfer]] = new Ref(EmptyVector) def main(args: Array[String]) { val business1 = new Account(15000) val business2 = new Account(20000) val business3 = new Account(50000) val people = (0 until 100).foldLeft(Vector[Account]()) { (vec, i) => vec + new Account(1000) } } } ``` 

Remember that all data contained within a reference must be completely immutable, otherwise the STM framework cannot help you. If you can just change the underlying data within a reference at will, then transaction semantics are useless (since revision tracking breaks down). To that end, we will use my [port of Clojure's persistent vector](<http://www.codecommit.com/blog/misc/implementing-persistent-vectors-in-scala/final/Vector.scala>) to handle the transfer log; and just because it's fun, we will also use it to manage personal accounts.

Moving on, we should probably define some functions to operate on these accounts. Remember, they must take an implicit parameter of type Transaction, otherwise they will be prevented from modifying references.

```scala def transfer(amount: Long, from: Account, to: Account)(implicit t: Transaction) { log := log + Transfer(amount, from, to) val less = Math.round(amount * 0.075) from := from - amount to := to + (amount - less) fees := fees + less } def sum(portfolio: Vector[Account])(implicit t: Transaction) = { portfolio.foldRight(0:Long) { _ + _ } } ``` 

One thing worthy of attention here is the `sum` method: we aren't actually modifying any references in this method, so why bother putting it within a transaction? The answer is to enforce data integrity. We want to make sure that we see a truly consistent picture of the entire market, and the only way to be absolutely sure of that is to use the conceptual "snapshot of the world" maintained by a transactional log.

Also notice that the `sum` method does not store its result within a reference, it actually returns a value. This is one of the neat features of our STM implementation: it allows transactions to return values just like functions. This dramatically reduces the boilerplate which would normally be required to get the result of a calculation from within a transaction.

With this infrastructure in place, we can go ahead and create all of the threads we're going to need for the simulation:

```scala val market = people + business1 + business2 + business3 + fees var running = true val secActor = thread { while (running) { val total = atomic(sum(market)(_)) println("Market value: $" + total) sleep(10) } } val businessActor = thread { while (running) { atomic(transfer(250, business1, business2)(_)) // transfer rent sleep(200) } } val peopleActors = for { i <\- 0 until people.length val p = people(i) } yield thread { atomic(transfer(50, p, business3)(_)) // payoff the mob atomic(transfer(i * 10, p, business1)(_)) // purchase from business1 atomic(transfer(i * 3, business2, p)(_)) // refund from business2 } ``` 

This is assuming that we have already defined a utility method, `thread`, in the following fashion:

```scala def thread(f: =>Unit) = new Thread { override def run() { f } } ``` 

Just one of the many little tricks made possible by the staggering power of Scala's syntax.

There isn't much worthy of attention within our simulation threads, it's just a lot of concurrent operations running against some shared state. If we wanted to really have some fun, we could add `println` status messages to each thread, allowing us to try the simulation multiple times and watch the thread interleaving change from run to run. However, all we're really interested in with this simulation is the assurance that data integrity is maintained at all times. To see that, all we really need is to check the starting market value, the ending value and some period market auditing while the simulation is in progress:

```scala println("Starting market value: $" + atomic(sum(market)(_))) businessActor.start() secActor.start() for (pa <\- peopleActors) pa.start() for (pa <\- peopleActors) pa.join() running = false businessActor.join() secActor.join() println("Total fees: $" + fees) println("Final market value: $" + atomic(sum(market)(_))) ``` 

If we compile and run the simulation, the output will look something like this (on a dual-core, 2 Ghz processor):

``` Starting market value: $185000 Market value: $185000 Market value: $185000 Market value: $185000 Total fees: $5258 Final market value: $185000 ``` 

We could try the simulation a hundred times with different CPU loads and even on separate machines, the results will always be the same. While there may be a greater or lesser number of concurrent market audits during the simulation, the values retrieved each time will be the same. From this we can conclude one important fact: we have succeeded in designing an STM framework which preserves data integrity.

In simulations like this one with a high degree of contested data, STM may actually be _slower_ than a traditional, fine-grained locking strategy. However, just think for a moment about trying to write this simulation using Java's `ReentrantReadWriteLock` class. It would be nearly impossible to design such a system, let alone maintain it. There would always be the danger that we would accidentally get the locking in the wrong order, or forget to lock something before we access it. In short, such an effort would be extremely hazard prone, and far more verbose. Using our STM framework, the resulting code was clean and simple to understand. It's easy to see why techniques like this are really starting to catch on.

  * [Download BankFrenzy.scala](<http://www.codecommit.com/blog/misc/software-transactional-memory-in-scala/BankFrenzy.scala>) (just in time for the economic crisis, too!)

### Conclusion

Hopefully this has been an informative and enjoyable foray into the world of transactional memory systems. I will retroactively apologize for any areas where my facts are in err; I'm certainly quite new to all of these concepts. As per usual, the library described in this article is available for download. I was a little lax on the testing side of life, so you may want to vet the library a little bit before you trust it unsupervised in your data center.

There are a number of interesting aspects to STM that I didn't cover in this article, such as `retry` and the related function, `check` (implemented in the framework). Also left completely untouched is the monadic operation `orElse` which can be used to compose transactions in a "first try this, then try that on failure" sort of way.

STM is a very active research topic today with a lot of the brightest minds in the industry pondering ways to make it better. While it certainly doesn't solve all of the problems associated with concurrency, it does have the potential to simplify locking and produce better performance under some conditions. Definately a technology to watch as it develops!

  * [Download scala_stm full sources](<http://www.codecommit.com/blog/misc/software-transactional-memory-in-scala/scala_stm.zip>)