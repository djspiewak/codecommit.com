---
categories:
- Scala
date: '2008-07-28 00:00:00 '
layout: post
title: 'Scala Collections for the Easily Bored Part 2: One at a Time'
wordpress_id: 250
wordpress_path: /scala/scala-collections-for-the-easily-bored-part-2
---

As I [hinted previously](<http://www.codecommit.com/blog/scala/scala-collections-for-the-easily-bored-part-1>), this series is intended to delve into Scala's extensive collections API and the many ways in which it can make your life easier. Probably the most important operations you could ever perform on collections are those which examine each element, one at a time. After all, what's a more common array idiom than looping over all values?

In that vein, this article starts by looking at `foreach`, the imperative programmer's bread-and-butter when it comes to types like `Array` and `List`. But rather than just stopping there, we also will look at more powerful, higher-order operations like fold, `map` and the ever-mysterious: `flatMap`.

### Iterating

As I said above, looping over every item in a collection is probably the most heavily used operation in a programmer's repertoire. In fact, this pattern is so common in imperative languages that Java 5 saw the introduction of a special construct at the language level, just to make things a little easier. For example:

```java
String[] names = { "Daniel", "Chris", "Joseph" };
for (String name : names) {
    System.out.println(name);
}
```

This code should be old hat to anyone coming from a Java background. Under the surface, this code is compiled into a `while`-loop with an iterator and an increment operation.  The code steps through the array, assigning each successive element to `name`. Most statically typed languages have a construct similar to this. For example, C# offers the `foreach` statement, which is almost precisely similar to Java's enhanced for-loop, but with a slightly different syntax. However, some languages (like Ruby) idiomatically eschew loops and rely instead on higher-order methods. Translating the above into Ruby yields the following:

```ruby
names = ['Daniel', 'Chris', 'Joseph']
names.each do |name|
  puts name
end
```

In this case, we aren't using a loop of any kind, but rather creating a block (Ruby's name for a closure) which takes a single parameter and passes it to the built-in `puts` method. This block is then passed as an object to the `each` method of class `Array`, which calls the block once for each element in series. Certainly, there is a language construct which encapsulates this, but using the `each` method directly is considered more "Ruby".

The same approach is taken in Scala. Rather than define a special construct for iteration, Scala simply provides the syntactic tools needed to construct higher-order methods like Ruby's `each`. Every collection in Scala's library defines (or inherits) a `foreach` method (taking after its C# ancestry) which behaves precisely like Ruby's `each`. To show how, we will translate our example once more, this time into Scala:

```scala
val names = Array("Daniel", "Chris", "Joseph")
names.foreach { name =>
  println(name)
}
```

Here we define an anonymous method (Scala's name for a closure) which takes a single parameter. As in Ruby, this closure is passed to `foreach` and invoked for each array element. In this way, foreach is a a so-called "higher-order" method, due to the fact that it accepts a parameter which is itself another method. Scala's implementation of this concept is actually a bit more general than Ruby's, allowing us to shorten our example into the following:

```scala
val names = Array("Daniel", "Chris", "Joseph")
names.foreach(println)
```

This time, instead of creating an anonymous method to pass to `foreach`, we simply pass the `println` method itself. The only criterion that `foreach` imposes on its parameter is that it is a method which accepts a single parameter of type `String` (the element type of the array). Since we already have just such a method (`println`), there is no need to define another simply for encapsulation.

Unfortunately, despite its flexibility, foreach is not always the most syntactically concise way to iterate over a collection. There are times that we just want to use a syntax which is similar to the for-loops available in other languages. Well, fear not! Scala's for-comprehensions are more than capable of providing just such a syntax. Consider the example of imperatively summing the values of a list:

```scala
val nums = List(1, 2, 3, 4, 5)

var sum = 0
for (n <- nums) {
  sum += n
}
```

At compile-time, the above is actually translated into an equivalent call to `foreach`, passing the body of the loop as the anonymous method. This means that any class which correctly declares a `foreach` method may be used in a for-comprehension in this way, greatly reducing the syntactic overhead.

### Folding

Looping is nice, but sometimes there are situations where it is necessary to somehow combine or examine every element in a collection, producing a single value as a result. An example of this could be our previous example of summing a list. Using `foreach`, we had to make use of a mutable variable (`sum`) and produce the result as a side effect. This is fine for hybrid languages like Scala, but some languages actually lack mutable variables altogether. In the previous post, I mentioned ML, which is a pure-functional language (almost). Since pure-functional languages lack mutable state, the gods of computing needed to come up with some other way to accommodate this particular case. The solution is called "folding".

Folding a collection is literally the process of looking at each element in addition to a current accumulator and returning some value. To make things more clear, let's re-write our list summing example in a functional style:

```scala
val nums = List(1, 2, 3, 4, 5)
val sum = nums.foldLeft(0) { (total, n) =>
  total + n
}
```

It may seem a bit disguised, but this too is a loop, just like `foreach`. For each element in the list (starting from the left), the `foldLeft` method will call the anonymous method we passed to it, providing both the current element, as well as the total accumulated from the previous call. Since there was no previous call when the first element is processed, we must specify an initial value for `total` \- in this case, 0. Literally, the above can be flattened into the following:

```scala
val sum = 0 + 1 + 2 + 3 + 4 + 5
```

Of course, we would never want to hard-code a list in this fashion, but it serves as a sufficient illustration of the functionality. I know from experience that when you first discover fold it's difficult to see why anyone would _want_ to use a construct so limited. After all, doesn't it just serve to obscure the true meaning of the code? Well, take my word for it, fold is an almost indispensable tool...once you get to know it a little better. Try keeping an eye out for times in your own code where a fold might be useful instead of a conventional loop. You'll be surprised how often it can be used to solve a problem, sometimes one not even intuitively related to accumulation.

There's no special language-level syntax for fold, but Scala's powerful operator overloading mechanism has allowed the designers of the collections API to create a special operator of rather dubious readability. To illustrate, here's our "summing a list" example once more:

```scala
val nums = List(1, 2, 3, 4, 5)
(0 /: nums) {_+_}
```

Yeah, I can't read it either. This example is semantically equivalent to the previous fold, but its meaning is a bit obfuscated by a) the bizarre right-associative operator, and b) a mysterious cameo by Scala's ubiquitous underscore. While I don't have a problem using the [underscore syntax](<http://www.codecommit.com/blog/scala/quick-explanation-of-scalas-syntax>) in my own code, I don't think that the fold operator improves anything other than number of characters. I suppose it's a matter of taste though.

#### Reduce

Fold has a closely related operation in Scala called "reduce" which can be extremely helpful in merging the elements of a sequence where leading or trailing values might be a problem. Consider the ever-popular example of transforming a list of `String`(s) into a single, comma-delimited value:

```scala
val names = List("Daniel", "Chris", "Joseph")
val str = names.foldLeft("") { (acc, n) =>
  acc + ", " + n
}

println(str)
```

If you compile and run this code, you will actually arrive at a result which looks like the following:

```
, Daniel, Chris, Joseph
```

This is because folding a list requires a leading value, but this means that we have an extra separator running wild. We could try a `foldRight`, but this would merely move the same problem to the tail of the string. Interestingly enough, this problem of leading/trailing separators is hardly specific to folding. I can't tell you how many times I ran into this issue when constructing highly-imperative query synthesis algorithms for [ActiveObjects](<https://activeobjects.dev.java.net>).

The easiest way to solve this problem in Scala is to simply use a reduce, rather than a fold. As a rule, any collection which defines `foldLeft` will also define `reduceLeft` (and the corresponding right methods). Reduce distinguishes itself from fold in that it does _not_ require an initial value to "prime the sequence" as it were. Rather, it starts with the very first element in the sequence and moves on to the end. Thus, the following code will produce the desired result for our names example:

```scala
val names = List("Daniel", "Chris", "Joseph")
val str = names.reduceLeft[String] { (acc, n) =>
  acc + ", " + n
}

println(str)
```

There are of course a few small problems with this approach. Firstly, it is not as general as a fold. Reduce is designed primarily for the iterate/accumulate pattern, whereas fold may be applied to many problems (such as searching a list). Also, the reduce method will throw an exception if the target collection is empty. Finally, Scala's type inference isn't quite clever enough to figure out what's going on without the explicit `[String]` type parameter (since our result is of type `String`). Still, even with all these shortcomings, reduce can be a very powerful tool in the right hands.

### Mapping

We've seen how fold can be an extremely useful tool for applying a computation to each element in a collection and arriving at a single result, but what if we want to apply a method to every element in a collection in-place (as it were), creating a new collection of the same type with the modified elements? Coming from an imperative background, this probably sounds a little abstract, but like fold, map can be extremely useful in many common scenarios. Consider the example of transforming a list of `String`(s) into a list of `Int`(s):

```scala
val strs = List("1", "2", "3", "4", "5")
val nums = strs.map { s =>
  s.toInt
}

nums == List(1, 2, 3, 4, 5)   // true
```

The final expression in this snippet is just to illustrate what really happens to the list elements when `map` is called. Literally, the map method walks through each element in the list, calls the provided method and then stores the result in the corresponding index of a _new_ list. (list is immutable, remember?) If you think about it, this is very similar to looping with `foreach` except that at each iteration we produce a value which is saved for future use.

Another common application of this technique might be to cast an entire array from one type to another. I often make use of XMLRPC, which has the unfortunate property of stripping all type information from its composite types. Thus, I often find myself writing code like this:

```java
public void rpcMethod(Object[] params) {
    String[] strParams = new String[params.length];
    for (int i = 0; i < params.length; i++) {
        strParams[i] = (String) params[i];
    }
}
```

It's a lot of trouble to go through, but I really don't know any better way. We can't just cast the array to `String[]`, since the array itself is _not_ of type `String[]`, only its elements match that type. Java doesn't support higher-order operations such as `map`, but fortunately Scala does. We can translate the above into a functional style and gain tremendously in both readability and conciseness:

```scala
def rpcMethod(params: Array[Object]) {
  val strParams = params.map { _.asInstanceOf[String] }
}
```

For the sake of brevity, you'll notice that I made use of the underscore syntax as a placeholder for the anonymous method parameter. This syntax works remarkably well for short operations like the above, where all we need to do is take the input value and cast it to a new type.

As it turns out, mapping over a collection is a phenomenally common operation, perhaps even more so than fold. For that reason, the creators of Scala decided that it was worth adding a special syntax sugar built into the powerful for-comprehension mechanism. With a little bit of tweaking, we can transform our casting example into an arguably more readable form:

```scala
def rpcMethod(params: Array[Object]) {
  val strParams = for (p <- params) yield p.asInstanceOf[String]
}
```

At compile-time, these two forms are literally equivalent. In some ways it is a matter of taste as to which is better. I personally tend to favor directly calling `map` for simple, non-combinatorial operations like this, but to each his own.

### Binding

Actually, the name "bind" comes from Haskell. Scala's term for this operation is "flatMap" because the operation may be viewed as a combination of the `map` and `flatten` methods. Of all of the techniques discussed so far, this one probably has the richest theoretical implications. Coming straight from the menacing jungles of category theory and the perplexing wasteland of monads, `flatMap` is both intriguing and apparently useless (at first glance anyway).

Like `map`, `flatMap` walks through every element in a collection and applies a given function, saving the value for later use. However, unlike `map`, `flatMap` expects the return type of the specified function to be the same as the enclosing collection with an optionally different type parameter. If we look at this in terms of our number-converting example from previously, this means that our anonymous method must not return a value of type `Int`, but rather of type `List[Int]`. Once `flatMap` has all of the resultant `List[Int]` values, it _flattens_ them into a single list containing all of the elements from each of the inner lists.

Ok, that was utterly un-helpful. Maybe the method signature would be more illuminating:

```scala
class List[A] {   // don't try this at home
  def flatMap[B](f: (A)=>List[B]): List[B]
}
```

Other than [forcing order of evaluation](<http://sigfpe.blogspot.com/2006/08/you-could-have-invented-monads-and.html>), I can't personally think of too many common cases where this sort of operation is useful. However, one contrived example does spring to mind. Consider once more the example of converting a list of `String`(s) into a list of `Int`(s), but this time assume that some of the `String` elements might not nicely convert into integer values:

```scala
val strs = List("1", "two", "3", "four", "five")
val nums = strs.flatMap { s =>
  try {
    List(s.toInt)
  } catch {
    case _ => Nil
  }
}

nums == List(1, 3)    // true
```

Recall that in Scala, everything is an expression - including `try`/`catch` blocks - therefore, everything has a value. This code literally walks through the entire list and _tries_ to convert each element into an integer and wrap the result in a `List`. If the conversion fails (for whatever reason), an empty list is returned (`Nil`). Because we return an empty list for those elements which cannot be converted, `flatMap` literally resolves those results out of existence, leading to a `List` which only contains two `Int`(s). For the monadically uninclined among us, this is precisely the reason why `Nil` is referred to as the "zero" of the `List` monad. However, that's a topic for [an entirely different series](<http://james-iry.blogspot.com/2007/09/monads-are-elephants-part-1.html>)...

### Conclusion

Ok, so this article was a bit longer than I really wanted to run, but there's a lot of material to cover! Scala's collections framework shows how even operations steeped in mountains of theory can still prove useful in solving common problems. Now, every time I use collections in Java (or even Ruby), I find myself reaching for many of these same methods, only to find them either unavailable or less powerful than I would like. Scala provides a welcome refuge for all those of us who desire more powerful collection types in our daily programming.

[Be with us next time](<http://www.codecommit.com/blog/scala/scala-collections-for-the-easily-bored-part-3>) for `filter`, `forall`, `exists` and more!