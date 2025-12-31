---
categories:
- Scala
date: '2008-01-09 01:24:45 '
layout: post
title: Quick Explanation of Scala's (_+_) Syntax
wordpress_id: 178
wordpress_path: /scala/quick-explanation-of-scalas-syntax
---

It seems every time I turn around, someone else is railing against Scala for having an enormously cryptic syntax, citing (_+_) as an example.  Aside from fact that it looks like an upside-down face, I think this little construct is probably catching more flack than it deserves.  To start with, let's get a bit of a context:

```scala val numbers = Array(1, 2, 3, 4, 5) val sum = numbers.reduceLeft[Int](_+_) println("The sum of the numbers one through five is " + sum) ``` 

Block out the second line for just one second.  The example is really pretty understandable.  The first line creates a new value (not a variable, so it's constant) of inferred type _Array[Int]_ and populating it with the results from the factory method _Array.apply[T](*T)_   (creates a new array for the given values).  The correspondent to the example in Java would be something like this:

```java final int[] numbers = {1, 2, 3, 4, 5}; int sum = 0; for (int num : numbers) { sum += num; } System.out.println("The sum of the numbers from one to five is " + sum); ``` 

You're probably already getting a bit of an idea on the meaning behind that magic (_+_) syntax.  Let's go a bit deeper...

In Scala, arrays, lists, buffers, maps and so on all mixin from the _Iterable_ trait.  This trait defines a number of methods (such as _foreach_ ) which are common to all such classes.  This is precisely why the syntax for iterating over an array is the same as the syntax for iterating over a list.  One of the methods defined in this trait is the high-order function _reduceLeft.   _This method essentially iterates over each sequential pair of values in the array and uses a function parameter to combine the values in some way.  This process is repeated recursively until only a single value remains, which is returned.

Linearized, the function looks something like this:

```scala val numbers = Array(1, 2, 3, 4, 5) var sum = numbers(0) sum += numbers(1) sum += numbers(2) sum += numbers(3) sum += numbers(4) ``` 

I mentioned that _reduceLeft_ uses a function parameter to perform the actual operation, and this is where the (_+_) syntax comes into play.  Obviously such an operation would be less useful if it hard-coded addition as the method for combination, thus the function parameter.  You'll find this is a common theme throughout the Scala core libraries: using function parameters to define discrete, repeated operations so as to make generic functions more flexible.  This pattern is also quite common in the C++ STL algorithms library.

(_+_) is actually a Scala shorthand for an anonymous function taking two parameters and returning the result obtained by invoking the + operator on the first passing the second.  Less precisely: it's addition.  Likewise, (_*_) would define multiplication.  With this in mind, we can rewrite our first example to use an explicitly defined closure, rather than the shorthand syntax:

```scala val numbers = Array(1, 2, 3, 4, 5) val sum = numbers.reduceLeft((a:Int, b:Int) => a + b) println("The sum of the numbers one through five is " + sum) ``` 

The examples are functionally equivalent.  The main advantage to this form is clarity.  Also, because we're now explicitly defining the anonymous function, we no longer need to specify the type parameter for the _reduceLeft_ method (it can be inferred by the compiler).  The disadvantage is that we've added an extra 20 characters and muddied our otherwise clean code snippet.

Another bit that is worth clarifying is that Scala does not limit this syntax to the addition operator.  For example, we could just as easily rewrite the example to reduce to the factorial of the array:

```scala val numbers = Array(1, 2, 3, 4, 5) val prod = numbers.reduceLeft[Int](_*_) println("The factorial of five is " + prod) ``` 

Notice we have changed the (_+_) construct to (_*_)  This is all that is required to change our reduction operation from a summation to a factorial.  Let's see Java match that!

Admittedly, the underscore syntax in Scala is a rather odd looking construct at first glance.  But once you get used to it, you can apply it to many complex and otherwise verbose operations without sacrificing clarity.  In fact, the only thing you sacrifice by using this syntax is your intern's ability to read and modify your code; and really, is that such a bad thing to be rid of?  :-)