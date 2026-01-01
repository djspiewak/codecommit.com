---
categories:
- Scala
date: '2008-03-17 01:00:42 '
layout: post
title: Function Currying in Scala
wordpress_id: 206
wordpress_path: /scala/function-currying-in-scala
---

As a hybrid-functional language, Scala supports many of the same techniques as languages like Haskell and LISP.  One of the least used and most misunderstood of these is that of function currying.  Furthermore, there are many articles talking about the various ways to use currying within languages like Ruby, Groovy and similar, but very few which actually discuss _why_ it's useful.  To that end, I present a quick run-down on how to curry methods in Scala, along with some idea of why you would want to.

### Defining Curried Functions

Conceptually, currying is a fairly simple idea.  Wikipedia [defines it](<http://en.wikipedia.org/wiki/Currying>) as follows:

> In computer science, currying, invented by Moses Schönfinkel and Gottlob Frege, is the technique of transforming a function that takes multiple arguments into a function that takes a single argument (the other arguments having been specified by the curry).

Mathematically,

f \colon (X \times Y) \to Z  
\mbox{curry}(f) \colon X \to (Y \to Z)

What this is saying is that the currying process transforms a function of two parameters into a function of one parameter which _returns_ a function of one parameter which itself returns the result.  In Scala, we can accomplish this like so:

```scala
def add(x:Int, y:Int) = x + y

add(1, 2)   // 3
add(7, 3)   // 10
```

And after currying:

```scala
def add(x:Int) = (y:Int) => x + y

add(1)(2)   // 3
add(7)(3)   // 10
```

In the first sample, the `add` method takes two parameters and returns the result of adding the two.  The second sample redefines the `add` method so that it takes only a single `Int` as a parameter and returns a functional (closure) as a result.  Our driver code then calls this functional, passing the second "parameter".  This functional computes the value and returns the final result.

Of course, it's not very apparent as to _why_ this is a good idea.  All we've really accomplished is eliminate the second parameter and move it into another function.  This is a big deal in languages like Haskell which only allow a single parameter, but Scala doesn't have this restriction.  For the moment, just assume with me that this is wonderful and profound as we move forward into a better implementation.

Our first example was nice, but it seems a little clumsy.  All we've done is add a ton of syntactic overhead to our `add` definition.  Fortunately, Scala has a nice syntax for defining curried functions which dates back to pure-functional languages like ML.  We can shorten our curried definition to something more like this:

```scala
def add(x:Int)(y:Int) = x + y

add(1)(2)   // 3
add(7)(3)   // 10
```

This is really just syntactic sugar, the compiler treats both definitions the same.

### Applied to Existing Methods

Currying isn't just limited to methods which you define.  Thanks to the power of a language with inner methods and closures, it is possible to define a method which takes a function of _n_ parameters and converts it to a curried function of order _n_.  In fact, this function is already defined within the `Function` singleton in the `scala` package.  We can use it to curry the first version of our `add` method:

```scala
def add(x:Int, y:Int) = x + y
val addCurried = Function.curried(add _)

add(1, 2)   // 3
addCurried(1)(2)   // 3
```

That mysterious underscore within the `curried` method invocation is actually a bit of Scala syntax which tells the compiler to treat `add` as a function value, rather than a method to be invoked.  Scala refers to this as a "partially applied function", though it doesn't really meet the strictest definition.  More on this later.

It is interesting (and important) to note that the `curried` method is only overloaded for methods of up to arity (number of parameters) 5.  I suppose that only a maniac would want to define a curried version of a function of any higher arity, but it's still odd to consider.

`Function` also provides utility methods which allow us to reverse the process.  For example, if we start with our second version of `add`, we may wish to generate a version which takes all parameters inline (in conventional fashion).  This can be done using the `uncurried` method:

```scala
def add(x:Int)(y:Int) = x + y
val addUncurried = Function.uncurried(add _)

add(3)(4)  // 7
addUncurried(3, 4)  // 7
```

As you can see, the process does invert quite nicely.  Notice that while this may appear to be dynamic programming ([note](<http://www.codecommit.com/blog/scala/function-currying-in-scala#comment-3307>)), it's actually quite static and valid from a compiler's standpoint.  Effectively, `addUncurried` is a closure defined within the `uncurried` method.  Because we assign it to a val, we can call it anywhere in scope (anywhere we can access the `addUncurried` variable).  The compiler will type-check everything here, including the uncurried functional and its parameter types.

### Partially Applied Functions

All of this is great, but it doesn't answer the question: "why do we care?"  After all, when you look at the differences, it just seems that we've slightly altered the syntax for passing parameters.  This doesn't even seem terribly mathematical (normally a trait of functional languages) since the notation for passing multiple variables is comma-delimited, not parentheses-separated.

It turns out that the best rationale for using currying has to do with general and specialized functions.  It would be nice to define a function for the general case, but allow users to specialize the function and then used the specialized version on different data sets.  Take the following code as an example:

```scala
def process[A](filter:A=>Boolean)(list:List[A]):List[A] = {
  lazy val recurse = process(filter) _

  list match {
    case head::tail => if (filter(head)) {
      head::recurse(tail)
    } else {
      recurse(tail)
    }

    case Nil => Nil
  }
}

val even = (a:Int) => a % 2 == 0

val numbersAsc = 1::2::3::4::5::Nil
val numbersDesc = 5::4::3::2::1::Nil

process(even)(numbersAsc)   // [2, 4]
process(even)(numbersDesc)  // [4, 2]
```

Aside from being a fair example of how imperative and functional programming blend well, this little ditty provides a convenient example with repeated code.  Astute observers will see that I could have easily done this using the `List#filter` method, but I wanted a curried function for an example.  Besides, I hadn't even used the cons operator once on this blog and I figured it was about time to start...

The bit of code which is redundant is on the last two lines: the repeated invocation of `process(even)`.  Not only is this redundant, but it's somewhat inefficient.  To see why, we have to partially apply the curried function to look at the result type:

```
scala> process(even) _
res0: (List[Int]) => List[Int] = <function>
```

There again is our old friend the underscore.  Remember that it just tells the compiler to treat the suffixed value as a functional, rather than a method to be evaluated.  Thus it seems that invoking `process(even)` is perfectly valid in isolation.  We don't have to specify the second parameter since there is none.  If the `process` method where not curried, there would be no way to do this (no way in most languages, we'll see a way in Scala a little farther down).  The result type of this invocation is the functional which would be invoked next in the series (with the second set of parentheses).  Remember that a curried function is one which takes a parameter and returns another function which takes a parameter and does more processing.  We're just accessing that intermediate function.

So if `process(even)` generates a functional, invoking it multiple times probably has some overhead.  To do so would be to create the intermediary functional twice.  Of course, the second problem is this is code-repeat,  something all good programmers cringe to see.  It turns out that we can solve both of these problems in one fell swoop:

```scala
//...

val even = (a:Int) => a % 2 == 0
val processEvens = process(even) _

val numbersAsc = 1::2::3::4::5::Nil
val numbersDesc = 5::4::3::2::1::Nil

processEvens(numbersAsc)   // [2, 4]
processEvens(numbersDesc)  // [4, 2]
```

Now we have a function `processEvens` of order 1 which _specifically_ processes through an "even" filter.  We have created a specialized version of `process` by taking advantage of the way currying works.  This turns out to be a very powerful technique, and while I don't have any non-trivial examples to share, I'm sure you can use your imagination.

### Partials without Currying

It's clear that partially applied functions can be very powerful, but they only seem to apply to a method which is already curried.  Of course, for a standard, arity _n_ method we can use the `curried` method to convert, but this gets clumsy.  It would be nice if we could use the partially applied function technique on _any_ method, even one which is not curried.  Enter underscore.

This little construct is probably the most powerful single character I've ever seen.  You can do so much with it in Scala, and yet it all seems to remain semantically consistent.  Scala allows us to use underscore within the invocation, partially applying the function by creating a new functional which takes the appropriate parameters.

```scala
def add(x:Int, y:Int, z:Int) = x + y + z

val addFive = add(5, _:Int, _:Int)
addFive(3, 1)    // 9
```

Here we have partially applied the `add` method, specializing on the value 5 for the first parameter.  `addFive` is actually a functional which takes two parameters of type `Int` and returns the result of passing the values of 5 and the two parameters in order to the `add` method.  This code is semantically equivalent to the following:

```scala
def add(x:Int, y:Int, z:Int) = x + y + z

val addFive = (a:Int, b:Int) => add(5, a, b)
addFive(3, 1)    // 9
```

The underscore just provides a convenient syntactic sugar for this.  The only unfortunate part is the fact that we must annotate the underscores with types.  If this were ML, Haskell or similar, the types of the underscores could easily be inferred and the annotations omitted.  It would be nice if Scala could do things like this, but I guess it can't be _entirely_ perfect.

### Conclusion

Hopefully this provides a fair introduction to the useful world of method currying.  As you can see, currying is not just an academic construct we were forced to demonstrate on college exams, it's a powerful technique with real-world applications.  By blending the functional and the object-oriented, Scala has managed to bring all the power of currying to the imperative world, a feat well worth utilizing on your next project.