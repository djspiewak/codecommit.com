{%
laika.title = "Scala Collections for the Easily Bored Part 1: A Tale of Two Flavors"
laika.metadata.date = "2008-07-21"
%}


# Scala Collections for the Easily Bored Part 1: A Tale of Two Flavors

One of the most obvious things to a Java developer first coming into Scala-land is the radically different Collections API included as part of the standard library. For the most part, we use the same frameworks and APIs in Scala as are available in Java. This is natural, thanks to the extremely tight integration between the two languages. So why is this one area such a startling departure from Scala's heritage?

The answer has to do with what the Scala language is syntactically capable of handling. Scala isn't just an object-oriented language, it is also highly functional. It is only natural that such an integral part of the core libraries would reflect this fact. Unfortunately, most developers fail to take full advantage of the power offered by the collections API. Despite the available power, most code written using Scala's collections tends to look a lot like Java in disguise.

I had actually planned on addressing this topic in a single article. However, Scala's collections are so vast and powerful (sounds like one of Roddenberry's [alien consciousness beings](<http://memory-alpha.org/en/wiki/Nagilum>)) that it really would overrun the limits of conventional blogging to attempt to cover it all in a single post. Despite the fact that I've been [creating](<http://www.codecommit.com/blog/scala/naive-text-parsing-in-scala>) [mammoth](<http://www.codecommit.com/blog/scala/formal-language-processing-in-scala>) [anthologies](<http://www.codecommit.com/blog/java/bencode-stream-parsing-in-java>) of late, I think it's probably better to break it into bite-sized chunks. First up: the confusing dual nature of Scala's collections API!

### A Tale of Two Flavors

The very first thing developers notice when looking at Scala's collections is a (seemingly) odd redundancy in the specification. Looking under the scala.collection package, we see not one, but _three_ separate sub-packages, each containing what seem to be reimplementations of the same types. For example, consider the following three traits:

  * [`scala.collection.immutable.Map`](<http://www.scala-lang.org/docu/files/api/scala/collection/immutable/Map.html>)
  * `[scala.collection.jcl.Map](<http://www.scala-lang.org/docu/files/api/scala/collection/jcl/Map.html>)`
  * `[scala.collection.mutable.Map](<http://www.scala-lang.org/docu/files/api/scala/collection/mutable/Map.html>)`

I don't know about you, but this confused the heck out of me the first time I really dug into Scala's standard API. Actually, it gets even worse when you discover that there is _also_ a trait (and companion object), `[scala.collection.Map](<http://www.scala-lang.org/docu/files/api/scala/collection/Map.html>)`, which is actually a super-trait of the three listed above. Seems like Dr. Odersky discovered the magic of separated namespaces and reacted like a two-year-old on espresso.

As it turns out, there's a very logical reason for having these separated and seemingly-redundant collections packages. Of the three, one of them can be discounted immediately as uninteresting. The `jcl` package contains collections, but they are merely wrappers for the corresponding Java collections. This allows more efficient transmutation between the Java collections API and Scala. It is almost _never_ necessary to use this package directly, since a number of implicit conversions are built into Scala to make the process essentially transparent.

Of course, this still leaves the `immutable` and `mutable` packages. This distinction traces back to some of Scala's functional roots. As you are likely aware, Scala supports both mutable and immutable variables, as denoted by the `var` and `val` keywords, respectively. While there are some significant differences at compile-time, conceptually, the only distinction between these two types is the former allows reassignment whereas the latter does not. Mutable variables are a common - and indeed, essential - feature in imperative languages (Java, C++, Ruby, etc). For example, here's how we would sum an array of integers in Java:

```java
int[] numbers = ...

int sum = 0;
for (int n : numbers) {
    sum += n;   // reassign sum to accumulate n
}
```

In this case, `sum` is a mutable variable which accumulates the total value of all numbers in the array, `numbers`. Theoretical disputes aside, this style of programming is simply impossible in certain languages. For example, SML provides no mechanism for declaring mutable variables. So if we want to sum the values in a list of ints, we have to do it in some other way (code provided for the curious):

```ml
fun sumList ls = foldl (op +) 0 ls
```

In Scala, both of these techniques are available to us. However, despite providing for mutable state, Scala does _encourage_ developers to avoid it. The reasoning behind this is that mutable state has a tendency to make code more difficult to reason about, making testing much harder. Also, as any experienced developer will tell you, mutable state _kills_ concurrency.

Not only does Scala encourage the use of immutable variables, it also encourages the use of immutable collections. This concept may seem a little bizarre to those of you coming from an imperative background (I know it did to me), but it actually works. While a map container which cannot be modified probably seems a little useless, it actually provides a startling degree of freedom from concerns like concurrent locking and unintended side-effects. With immutable collections, you are able to manipulate objects with perfect assuredness that no method call will "accidentally" alter its contents. How many times have you cloned a collection prior to returning it? Or how often have you dug through someone else's code just to assure yourself that it is safe to call a given method passing an instance of `List`? Immutable data structures completely solve that problem.

Naturally, immutable collections require very different patterns and idioms than those which are mutable. My ML code from above illustrates this to a very small degree, using a fold to traverse an immutable list. As a general rule, immutable data structures are only useful when being passed around via method calls. A common pattern is to build up a data structure recursively, creating a new instance with one more element at each invocation:

```scala
def toSet[T](list: List[T]) = {
  def traverse(list: List[T])(set: Set[T]): Set[T] = list match {
    case hd :: tail => traverse(tail)(set + hd)   // create a new Set, adding hd
    case Nil => set
  }

  traverse(list)(Set[T]())
}

val names = List("Daniel", "Chris", "Joseph", "Renee")
val nameSet = toSet(names)
```

At each recursive invocation of `traverse`, a new `Set` is created, based on the contents of the old one, `set`, but with one additional member, `hd`. At the same time, we are deconstructing an immutable `List`, `list`, selecting its first element and whatever remains at each step. Whenever you work with immutable data structures, you will see a lot of code which looks like this.

Of course, the natural question which comes to mind is, what about performance? If each invocation actually creates a brand new `Set` for _every_ recursive call, doesn't that require a lot of inefficient object copying and heap operations? Well, as it turns out, this isn't really the case. Yes, a new instance must be created at each turn, which is is a comparatively expensive operation on the JVM, but almost nothing is being copied. All of Scala's immutable data structures have a property known as _persistence_ , which means that you don't copy the data out of the old container when creating a new, you just have the new _reference_ the old and treat all of its contents as if they were its own. A linked list is a good example of this, since each node of a list contains exactly one element, as well as a reference to another node. If we think of each node as a representative of a list starting with itself and traversing to the end, then list suddenly becomes a _fully persistent_ data structure (since the new list contains a sub-list in its entirety and additive operations require no data copying). Rich Hickey, the creator of [Clojure](<http://clojure.org/>) (a Lisp dialect running on the JVM), has [an excellent presentation](<http://clojure.blip.tv/#819147>) which explains some of the hows and whys behind this technique (as well as some other interesting topics). Chapter 19 of _Programming in Scala_ (Odersky, Spoon & Venners) also has a good example of a persistent immutable queue.

### Conclusion

I happen to like immutable data, so most of this series uses the `scala.collection.immutable` package. However, there are certainly situations where mutable data structures are the only way to go, either because of system requirements or for performance reasons. Fortunately, Scala's mutable collections have an almost identical interface to its immutable collections. Thus, most of the in formation presented here is applicable to both branches of the library.

Now that we have laid a basic foundation regarding the fundamentals of Scala's collections framework, we can move onto more interesting things. [The next installment](<http://www.codecommit.com/blog/scala/scala-collections-for-the-easily-bored-part-2>) will deal with fold and map, the bread and butter of every collection.