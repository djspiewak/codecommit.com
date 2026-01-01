---
categories:
- Scala
date: '2008-11-17 01:00:00 '
layout: post
title: Infinite Lists for the Finitely Patient
wordpress_id: 273
wordpress_path: /scala/infinite-lists-for-the-finitely-patient
---

Functional programming has a lot of weird and abstract concepts.  Monads of course are the poster child for all that is strange and confusing in functional languages, but there are other examples.  At first glance, it seems that the concept of an infinite list would be just as bizarre and incomprehensible as anything else in the paradigm, but really things aren't as bad as all that.  In fact, even the die-hard imperative programmer can likely benefit from the patterns and techniques enabled by this obtuse construct.

For the first part of the article, I'm going to use a lot of examples involving numbers.  This is just because I'm lazy and only want to introduce one concept at a time.  Everyone understands the idea that there are an infinite amount of integers starting from 0 and counting upward.  Please bear with me all the way to the end, I promise I will give some more convincing motivation for infinite lists along the way.

If you're already familiar with the semantics of infinite lists, feel free to skip to the section answering that all-important question: [Why Do We Care?](<#motivation>)

### Syntax

Before we dive into the topic of infinite lists and how they are practically applicable, it's important to understand the underlying mechanics and (critically) how the API works in practice.  I'll be using Scala to illustrate.  Any language which supports lambdas would actually suffice, but Scala's unique blend of the [functional and the object-oriented](<http://www.codecommit.com/blog/scala/is-scala-not-functional-enough>) can be very useful in illustrating infinite lists in an imperative context.  Also, for the duration of the article, I will be making use of the following implicit conversion:

```scala
class RichStream[A](str: =>Stream[A]) {
  def ::(hd: A) = Stream.cons(hd, str)
}

implicit def streamToRichStream[A](str: =>Stream[A]) = new RichStream(str)
```

The only thing you need to know about the above is that it _dramatically_ simplifies the syntax required when working with infinite lists in Scala.  Since the focus of this article is infinite lists and not implicit conversions, we will just take the above as magic incantations and leave it at that.

The primary medium for working with infinite lists in Scala is the `scala.Stream` class.  This class is not magic, you could write it yourself in plain-old-Scala without too much trouble.  `Stream` inherits from the all-encompassing `scala.Iterable` trait, meaning that familiar methods like `map`, `foldLeft` and `foreach` are all available for use with an infinite list as well as a finite one.

In terms of mechanics, infinite lists are very similar to finite ones: composed of a _head_ —which is some data of type `A`—and a _tail_ —which is another `Stream[A]`.  The critical distinction which allows infinite lists to be infinite is the fact that this tail is _lazily evaluated_.  That means that the tail as a value is not available until you ask for it.  This is a profound idea with far reaching consequences, particularly in languages which take it to its greatest extreme (Haskell).

Just like with a normal, finite `List`, we build instances of `Stream` using a _cons cell_.  This is basically the in-code representation of head/tail concept.  Normally, this "cons for `Stream`s" is handled by the singleton method `Stream.cons` (the equivalent of [Clojure's `lazy-cons`](<http://clojure.org/api#toc277>)).  However, thanks to our magic implicit conversion noted above, we can make use of the same right-associative `::` operator that we use with regular every-day finite `List`s.  For example, we can easy create an infinite list of the natural numbers (from 0 to ∞):

```scala
def from(n: Int): Stream[Int] = n :: from(n + 1)

val nats = from(0)
```

In this example, `from` is what is known as a _generator function_.  That is to say, it takes an input and transforms it directly into some output structure.  Well, actually that's only part of the definition, but the rest of it gets a little weird...

One very important property of `from` which should immediately jump out at you is the fact that it is infinitely recursive.  It takes a number, invokes this mysterious `::` operator on that value and then recursively calls back to itself.  There is no conditional guard, no base case, just an endless series of calls.  Intuitively, this should lead to non-termination at runtime...except that it doesn't.  Remember what I said about the lazily-evaluated tail?  This is where that idea really begins to take effect.  The `from` function is _not_ infinitely recursive; at least, not right away.

Because the tail of a `Stream` is lazy, there is no need to actually invoke `from(n + 1)` until that particular element of the list (or one which comes after it) is required.  To prove this intuition, we can add a side-effect to the `from` function so that we can trace the execution:

```scala
def from(n: Int): Stream[Int] = {
  println("Requesting n = " + n)
  n :: from(n + 1)
}

val nats = from(0)
```

This code will print exactly (and only) the following:

```
Requesting n = 0
```

The invocation `from(1)` is never processed because it does not _need_ to be.  We can force further evaluation of the list by requesting a later element:

```scala
nats(4)      // get the fifth natural number (4)
```

This will print the following:

```
Requesting n = 1
  Requesting n = 2
  Requesting n = 3
  Requesting n = 4
```

Notice that we don't evaluate for `n = 0` a second time.  Infinite lists are lazy, but not inefficient.  Each element is evaluated exactly once, and only as necessary.  You can exploit this property to do some really amazing things (such as the most concise Fibonacci sequence you will ever see).

As mentioned previously, all of the standard utility methods available on collections are also usable with infinite lists.  For example, it might be moderately interesting to get a list of all multiples of 5.  We could create a new generator function (e.g. `fromBy5`) which only returned multiples of five, but there is a much easier technique we could apply.  We already have a list of all natural numbers, why not make use of that?  If we multiplied _every_ natural number by 5, the result would be identical to the infinite list of the multiples of 5.  If you haven't taken calculus, this statement probably sounds a little weird.  After all, wouldn't the list of all natural numbers multiplied by 5 be _bigger_ than just the list of the multiples of five?

Actually, the answer to this question is "no", and we could prove that mathematically if it weren't entirely besides the point.  The really interesting thing to focus on is the technique suggested: take the infinite list of natural numbers (which we already have) and multiply each element by 5, resulting in another infinite list.  If you're at all familiar with the higher-order processing of finite collections, you should be thinking `map`:

```scala
val fives = nats map { _ * 5 }
```

For those of you not "in the know", the ever-cryptic `{ _ * 5 }` syntax actually means the following: `{ x => x * 5 }`.  The advantage of the former over the latter is merely brevity.

The definition of the `map` method goes something like this:

> For every element within the target collection, apply the specified function value and store the result in the corresponding location of a new collection.

"For every element..."  That phrase should be raising huge red flags.  It implies that `map` is trying to iterate over the entire `nats` collection—a collection of infinite size.  Once again, we seem to have created a non-terminating expression.

Fortunately, the laziness of infinite lists comes to our rescue.  Just like `::` doesn't evaluate the tail until it is absolutely required to do so, `map` doesn't actually invoke the function it is passed until an element is retrieved from the mapped list.  Once again making use of our `println`-enabled `from` method, we can test when the elements are being retrieved:

```scala
val nats = from(0)
val fives = nats map { _ * 5 }

println("Querying list...")
fives(4)                      // get the fifth multiple of five (20)
```

When this code executes, it will print the following:

```
Requesting n = 0
  Querying list...
  Requesting n = 1
  Requesting n = 2
  Requesting n = 3
  Requesting n = 4
```

Surprised?  It helps to remember this simple fact: _everything_ about an infinite list is lazy.  Nothing is evaluated until it absolutely must.

So, that means we can do something like the following, right?

```scala
for (n <- fives) {
  println(n)
}
```

If we were to execute this snippet, the result would be as follows:

```
Requesting n = 0
  0
  Requesting n = 1
  5
  Requesting n = 2
  10
  Requesting n = 3
  15
  Requesting n = 4
  20
  Requesting n = 5
  25
  Requesting n = 6
  30
  .
  .
  .
```

In other words, this evaluation really is non-terminating.  This is because there is no way for the laziness of the list to help us.  We're trying to iterate over every element in the list, retrieving that element for evaluation _right away_.  In this sort of situation, evaluation cannot be deferred, and thus the program will never end.

As it turns out, there are a number of operations on infinite lists which cannot be done lazily.  The `foreach` method is one of these, as are `foldLeft`, `foldRight`, `reduce`(`Left` and `Right`), `length`, etc.  As a general rule of thumb, virtually any operation on an infinite list which produces _another_ infinite list can be performed lazily.  Otherwise, if a result is of an immediate type (such as `Int` or even `Array`), the evaluation absolutely must be performed at that exact point in the code.  More formally: any operation on an infinite list which is [_catamorphic_](<http://en.wikipedia.org/wiki/Catamorphism>) (meaning that it takes the whole collection and reduces it to a single value) will force the evaluation of the collection in its entirety.

So if it's impossible to iterate, fold or get the length of an infinite list, of what use could they possibly be?  It is true that infinite lists in and of themselves would be too "high-minded" for any practical application, but that's what the `take` method is designed to fix.  The `Stream#take(Int)` method is a way of imposing a narrow window on an infinite list.  Rather than looking at every single natural number from 0 to infinity, we just look at the numbers from 0 to 9.  Put into code:

```scala
val nats = from(0)
val digits = nats.take(10)

for (d <- digits) {
  println(d)
}
```

This will print (exactly):

```
0
  1
  2
  3
  4
  5
  6
  7
  8
  9
```

The beauty of this is that we have created an infinite list, taken exactly what we needed and ignored the rest.  Most of the time, this is how you will use the streams which you create.  Note that the `take` method still does not force evaluation of the list.  If we go back to our fancy `println`ified version of `from`, we can see that evaluation is still being performed lazily, just limited to an upper bound of 9 (because we took the first 10 elements, starting from 0):

```
Requesting n = 0
  0
  Requesting n = 1
  1
  Requesting n = 2
  2
  Requesting n = 3
  3
  Requesting n = 4
  4
  Requesting n = 5
  5
  Requesting n = 6
  6
  Requesting n = 7
  7
  Requesting n = 8
  8
  Requesting n = 9
  9
```

### Why Do We Care?

Thus far, we have looked at a few examples of infinite lists, all of them having to do with numbers.  This list is surprisingly useful (as we will see in a moment), but it is far from the only infinite list we can create.  This section will explore some slightly more down-to-earth applications for infinite lists, including use-cases which everyone (even the dirty-scumbag imperative lovers) can relate to.  :-)

**Conceptually, infinite lists are a way of flattening recursion and packaging it up in a form which can be manipulated in a less clumsy fashion.**   Got that?  Good.  Now, read it again.

Let's consider the simple (and extremely common) case in C/C++ where we must iterate over all of the elements of an array.  I'm using C++ here because Java has the enhanced `for`-loop which completely blows my example out of the proverbial water:

```cpp
char *names[6] = { "Daniel", "Chris", "Joseph", "Renee", "Bethany", "Grace" };
int len = 6;

for (int i = 0; i < len; ++i)
{
    cout << names[i] << endl;
}
```

We've all written code like this.  In fact, most of us have probably written it so often that the prefix "`for (int i = 0; `" has become part of our permanent muscle memory.  However, look at what we have done here.  Strictly speaking, we're not looping over the the array of `names` at all.  We are actually looping over every successive integer between 0 and 6.

Replicating these semantics in a functional programming language is actually a bit tricky (assuming that we rule out higher-order solutions like `foreach`).  Ignoring for the moment the issues associated with side-effects and I/O, we have a somewhat more serious issue to worry about: functional languages don't have any loops.  Of course, Scala does (`while` and `do`/`while`), but for the sake of argument we will ignore this fact.

The idiom for "looping" in functional languages is to use recursion.  Instead of maintaining a mutable counter (`i`) which changes value at each iteration, we will define a `loop` method which takes a parameter `i`, processes the `i`the element of an array and then recursively calls itself, passing `i + 1`.  In code, this looks like the following:

```scala
val names = Array("Daniel", "Chris", "Joseph", "Renee", "Bethany", "Grace")

def loop(i: Int) {
  if (i < names.length) {
    println(names(i))
    loop(i + 1)
  }
}
loop(0)
```

Just in case you were confused as to why some people dislike functional languages, the above is "it".  Scala will compile the above into a form which is actually just as efficient as the earlier C++ example, but that's not really the point.  The problem is that this code is horribly ugly.  The underlying meaning (looping over the array) is completely obscured behind a fog of syntax and method dispatch.  You can easily imagine how a more complex looping example (particularly with nested loops) might get unmaintainably messy.

This is where infinite lists swoop in and save the day.  Remember when I said that infinite lists serve to bottle up recursion in a form that we can swallow?  (if you don't remember, scroll back up and read it again)  We can make use of the fact that we have already defined an infinite list of all integers starting at 0.  All we have to do is just `take` exactly the number we need (`names.length`) and then use a `for`-comprehension to iterate over the result:

```scala
for (i <- nats.take(names.length)) {
  println(names(i))
}
```

If you actually unroll the evaluation semantics of the above, you will find that we're still using recursion, it's just being handled behind the scenes in the `from` method.  This is what I meant when I said that infinite lists "flatten recursion".

Note that the above idiom is so common-place that Scala actually defines a specialization of infinite lists just to represent ranges of integers.  Oddly enough, this class is called "`Range`".  We can create a range by using the `until` method on a starting integer, passing it an end-point:

```scala
for (i <- 0 until names.length) {
  println(names(i))
}
```

This way, we don't have to define a new infinite list of `nats` every time we need to iterate over a range of numbers.

Of course, iteration over an integer range is a fairly trite example.  Everyone reading this article should know that there are better ways of accomplishing what we just did.  For example, the higher-order method `foreach` is capable of printing the `names` array in a far more concise fashion:

```scala
names foreach println
```

Let's try something a little more complex.  Imagine that we have to loop through 10 random integers between -100 and 100.  Just to make this really fun, let's say that we have to perform this as an inner and an outer loop, printing out all combinations of the _same_ 10 random numbers.

If it weren't for the restriction that the 10 random numbers need be the same between both loops, an imperative programmer might just do something like the following:

```java
int a = 0;
for (int i1 = 0; i1 < 10; i1++) {
    a = // generate random number

    int b = 0;
    for (int i2 = 0; i2 < 10; i2++) {
        b = // generate random number
        System.out.println("(" + a + ", " + b + ")");
    }
}
```

This code is really nasty.  Not only that, but it doesn't do what we want since the random numbers generated for the inner loop will be different from the ones generated for the outer.  We could technically fiddle with shared seeds and the like, but even that isn't a really elegant solution.

Using infinite lists, the solution is surprisingly simple.  All we need to do is define an infinite list of random numbers between -100 and 100, then take the first 10 values from this list and perform standard nested iteration using a `for`-comprehension.  It's much easier to write in Scala than it is to say:

```scala
def randomGen: Stream[Int] = (Math.random * 200).toInt - 100 :: randomGen
val random = randomGen

for (a <- random.take(10); b <- random.take(10)) {
  println("(" + a + ", " + b + ")")
}
```

When this executes, it will print the following sequence:

```
(68, 68)
  (68, -83)
  (68, -79)
  (68, -78)
  (68, -18)
  (68, -80)
  (68, 29)
  (68, 10)
  (68, -63)
  (68, 3)
  (-83, 68)
  (-83, -83)
  (-83, -79)
  (-83, -78)
  .
  .
  .
```

Pretty nifty, eh?  You'll notice that this time our generator function didn't need to take a parameter.  This dramatically simplified the syntax involved in generating the infinite list.  However, despite the fact that we no longer require a parameter for our generator, we still need to assign the list to a value.  Consider these two (incorrect!) definitions for `random`:

```scala
def random: Stream[Int] = (Math.random * 200).toInt - 100 :: random
// or even...
val random: Stream[Int] = (Math.random * 200).toInt - 100 :: random
```

The first definition for `random` (as a function) will indeed work, producing an infinite list of random numbers.  However, that list will not be repeatable.  In other words, consecutive invocations of `random` (such as in our nested loop example) will produce different lists.  Swinging to the other end of the scale, the value definition for `random` will actually produce an infinite list of exactly the same number (e.g. `42, 42, 42, 42, 42, 42, ...`).  The reason for this is left as an exercise for the reader.

Let's consider one last example.  This time, we'll look at something a little less "functional" in nature.  Imagine that you have defined a database schema which looks like the following:

`people`  
---  
**id** | `INTEGER`  
**firstName** | `VARCHAR`  
**lastName** | `VARCHAR`  
**parentID** | `INTEGER`  
  
This is a fairly simple parent/child schema with each row having a reference to its parent.  It's not difficult to imagine a scenario where one might want to traverse the "ancestry" of a row in the table, perhaps looking for some specific criterion or just for enumerative purposes.  This could be done in Java (and other languages) by maintaining a mutable "`person`" variable containing the current row in the database.  This variable could be modified iteratively based on successive queries determining its corresponding `parentID`.  However, as you can imagine, the code would be messy and prone to subtle errors.

The better approach in this situation is actually to make use of an infinite list.  While it is true that the list could not possibly be infinite given a finite database, the concept of a lazily-constructed collection can still simplify the issue.  We would construct the list using the following generator method:

```scala
def ancestors(id: Int) = {
  val conn: Connection = ...
  
  try {
    val res = conn.executeQuery("SELECT parentID FROM people WHERE id = ?")

    if (res.next()) {
      val parentID = res.getInt(1)
      parentID :: ancestors(parentID)
    } else Stream.empty
  } finally {
    conn.close()
  }
}
```

One item of note regarding this function is the use of the `Stream.empty` constant.  This is precisely analogous to the `Nil` object used in constructing conventional lists: it simply marks the end of the linked list.  Because every person will have a finite ancestry, we aren't actually exploiting the infinite potential (no pun intended) of streams.  In fact, we are only using the data structure as a tool to facilitate lazy evaluation.

The nice thing about representing the database traversal in the given fashion is the ability to treat the results as a fully-realized collection even before the queries have actually been run.  For example, we can generate a list of all people in the parentage of person 42, and then restrict this list to only people with an odd `id`.  I have no idea why this would be useful, but here it is:

```scala
val parents = ancestors(42)
val fewerParents = parents filter { _ % 2 == 1 }

for (id <- fewerParents) {
  println(nameForPerson(id))       // get a person's name and print
}
```

All of the logic associated with traversing the database tree is nicely tucked away within the `ancestors` function.  This separation of logic allows for much cleaner code at times, hence reducing the potential for silly mistakes.

### Conclusion

Infinite lists can be a surprisingly powerful tool, even in the arsenal of someone who is still devoted to the Dark Side of imperative programming.  Our last example looked at a scenario where an infinite list could be useful, despite the obviously side-effecting nature of the problem.  Similarly, the example of a list of random numbers shows how infinite lists can even be used to solve problems which are extremely difficult to solve using classical, imperative techniques.

To summarize: infinite lists are not just a beautiful mathematical formalism stemming from the forbidding forests of Haskell, they can also be a useful device when approaching down-to-earth problems like complex iteration.  The next time you find yourself writing unpleasant code to loop over complex data sets, stop and consider whether or not a stream might be a better solution.