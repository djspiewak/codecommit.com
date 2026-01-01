{%
laika.title = "Implementing Groovy's Elvis Operator in Scala"
laika.metadata.date = "2008-07-07"
%}


# Implementing Groovy's Elvis Operator in Scala

Groovy has an interesting shortening of the ternary operator that it rather fancifully titles "the [Elvis Operator](<http://groovy.codehaus.org/Operators#Operators-ElvisOperator\(%3F%3A\\\)>)".  This operator is hardly unique to Groovy - C# has had it since 2.0 in the form of the [Null Coalescing Operator](<http://blog.devstone.com/Aaron/archive/2006/01/02/1404.aspx>) \- but that doesn't mean that it is not a language feature worth learning from.  Surprisingly (for a C-derivative language), Scala entirely lacks any sort of ternary operator.  However, the language syntax is more than flexible enough to implement something similar without ever having to dip into the compiler.

But before we go there, it is worth examining what this operator does and how it works in languages which already have it.  In essence, it is just a bit of syntax sugar, allowing you to easily check if a value is `null` and provide a value in the case that it is.  For example:

```groovy
firstName = "Daniel"
lastName = null

println firstName ?: "Chris"
println lastName ?: "Spiewak"
```

This profound snippet really demonstrates about all there is to the Elvis operator.  The result is as follows:

```
Daniel
Spiewak
```

Not terribly exciting.  Essentially, what we have is a binary operator which evaluates the left expression and tests to see if it is `null`.  In the case of `firstName`, this is false, so the right expression (in this case, `"Chris"`) is never evaluated.  However, `lastName` _is_ `null`, which means that we have to evaluate the right expression and return its value, rather than `null`.  It's all just so much syntax sugar that can be expressed equivalently in any language with a conditional operator (in this case, Java):

```java
String firstName = "Daniel";
String lastName = null;

System.out.println((firstName == null) ? "Chris" : firstName);
System.out.println((lastName == null) ? "Spiewak" : lastName);
```

A bit verbose, don't you think?  Of course, this isn't really a fair comparison, since Groovy is a far more concise language than Java.  Let's see how the above would render in a real man's language like Scala:

```scala
val firstName = "Daniel"
val lastName: String = null

println(if (firstName == null) "Chris" else firstName)
println(if (lastName == null) "Spiewak" else lastName)
```

Better, but still a little clumsy.  The truth of the matter is that we're forced to do this sort of null checking all the time (well, maybe a little less in Scala) and the constructs for doing so are woefully inadequate.  Thus, the motivation for the Elvis operator.

### Getting Things Started

Like all good programmers should, we're going to start with a runnable specification for every behavior desired from the operator.  I've [written before](<http://www.codecommit.com/blog/java/the-brilliance-of-bdd>) about the [excellent Specs framework](<http://code.google.com/p/specs/>), so that's what we'll use:

```scala
"elvis operator" should {
  "use predicate when not null" in {
    "success" ?: "failure" mustEqual "success"
  }
  
  "use alternative when null" in {
    val test: String = null
    test ?: "success" mustEqual "success"
  }
  
  "type correctly" in {		// if it compiles, then we're fine
    val str: String = "success" ?: "failure"
    val i: Int = 123 ?: 321
    
    str mustEqual "success"
    i mustEqual 123
  }
  
  "infer join of types" in {    // must compile
    val res: CharSequence = "success" ?: new java.lang.StringBuilder("failure")	
    res mustEqual "success"
  }
  
  "only eval alternative when null" in {
    var a = "success"
    def alt = {
      a = "failure"
      a
    }
    
    "non-null" ?: alt
    a mustEqual "success"
  }
}
```

Fairly straightforward stuff.  I imagine that this specification for the operator is a bit more involved than the one used in the Groovy compiler, due to the fact that Scala is a statically typed language and thus requires a bit more effort to ensure that everything is working properly.  From this specification, we can infer three core properties of the operator:

  1. Basic behavior when `null`/not-`null`
  2. The result type should be the _unification_ of the static types of the left and right operands 
  3. The right operand should only be evaluated when the left is `null`

The first property is fairly easy to understand; it is intuitive in the definition of the operator.  All this means is that the value of the operator expression is dependent on the value of the left operand.  When not `null`, the expression value is equal to the value of the left operand.  If the left operand is `null`, then the expression is valued equivalent to the right operand.  This is just formally expressing what we spent the first section of the article describing.

Ignoring the second and third properties, we can actually attempt an implementation.  For the moment, we will just assume that the left and right operands must be of _exactly_ the same type, otherwise the operator will be inapplicable.  So, without further ado, implementation enters stage right:

```scala
implicit def elvisOperator[T](alt: T) = new {
  def ?:(pred: T) = if (pred == null) alt else pred
}
```

Notice the use of the anonymous inner class to carry the actual operator?  This is a fairly common trick in Scala to avoid the definition of a full-blown class just for the sake of adding a method to an existing type.  To break down what's going on here, we have defined an implicit type conversion from _any_ type `T` to our anonymous inner class.  This conversion will be inserted by the compiler whenever we invoke the `?:` operator on an expression.

Sharp-eyed developers will notice something a little odd about the way this code is structured.  In fact, if you look closely, it seems that we evaluate the _right_ operand and use its value if non-null (otherwise left), which is exactly the opposite of what our specification defines.  For a normal operator, this observation would be quite correct.  However, Scala defines the associatively of operators based on the trailing symbol.  In this case, because our trailing symbol is a colon (`:`), the operator itself will be right-associative.  Thus, the following expression:

```scala
check ?: alternate
```

...is transformed by the compiler into the following:

```scala
alternate.?:(check)
```

This is how right-associative operators function, by performing method calls on the _right_ operand.  Thus, we need to define our implicit conversion such that the `?:` method will be defined for the right operand, taking the left operand as a parameter.  We'll see a bit later on how this can cause trouble, but for now, let's continue with the specification.

### A Little Type Theory

The second property is a little tougher.  Type unification is one of those pesky issues that plague statically typed languages and are simply irrelevant in those with dynamic type systems.  The issue arises from the following question: what happens if the left and right operands are of different types?  In Groovy, this is a non-issue because the value of the expression is simply dynamically typed according to the runtime type of the operand which is chosen.  However, Scala requires static type information, which means that we need to ensure that the static type of the expression is sound for _either_ the left or the right operand (since Scala does not have non-nullable types).  The best way to do this is to compute the least upper bound of the two types, an operation which is also known as minimal unification.  Consider the following hierarchy:

![image](/assets/images/blog/wp-content/uploads/2008/07/image1.png)

Now imagine that the left operand is of static type `Apple`, while the right operand is of static type `Pear`.  We need to find a static type which is safe for _both_ of these.  Intuitively, this type would be `Fruit`, since it is a common superclass of both `Apple` and `Pear`.  Regardless of which expression is chosen at runtime, we will be able to polymorphically treat the value as a value of type `Fruit`.  The intuition in this case is quite correct.  In fact, it actually has a rigorous mathematical proof...which I won't go into.  ( _queue sighs of relief_ )

One additional example should serve to really drive the point home.  Consider the scenario where the left operand has type `Vegitable` and the right operand has type `Apple`.  This is a bit trickier, but it recursively boils down to the same case.  The only common superclass between these two types is `Object`, due to the fact that the hierarchies are disjoint.

This operation is fairly easy to perform by hand given the full type hierarchy.  For that matter, it isn't very difficult to write an algorithm which can efficiently compute the minimal unification of two types.  Unfortunately, we don't have that luxury here.  We cannot simply write code which is executed at compile time to determine type information, we must make use of the existing Scala type system in order to "trick" the compiler into inferring things for us.  We do this by making use of lower-bounds on type parameters.  With this in mind, we can (finally) make a first attempt at a well-typed implementation of the operator:

```scala
implicit def elvisOperator[T](alt: T) = new {
  def ?:[A >: T](pred: A) = if (pred == null) alt else pred
}
```

The only thing we have changed is the type of the `pred` variable from `T` to a new type parameter, `A`.  This new type parameter is defined by the lower-bound T.  Translated into English, the type expression reads something like the following:

> Accept parameter `pred` of some type `A` which is a super-type of `T`.

The real magic of the expression is that pred need not be _exactly_ of type A; it could also be a subtype.  Thus, A is some generic supertype which encompasses both the types of the left and the right operands.

### Fancy Parameter Types

This allows us to move onto the third property: only evaluate the right operand if the left is `null`.  This is the normal behavior for conditional expressions.  After all, you wouldn't want your code performing an expensive operation (such as grabbing data from a server somewhere) just to throw away the result because a different branch of the conditional was chosen.  Actually, the bigger issue with ignoring this property (as we have done so far) is that the right operand may actually have side-effects.  Scala isn't a pure functional language, so evaluating expressions that we don't need (or worse, that the developer isn't expecting) can have extremely dire consequences.

Unfortunately, at first glance, there doesn't really seem to be a way to avoid this evaluation.  After all, we need to invoke the `?:` method on _something_.  We could try using a left-associative operator instead (such as C#'s `??` operator), but even that wouldn't fully solve the problem as we would still need to pass the right operand as a parameter.  In short, it seems like we're stuck.

The good news is that Scala's designers chose to adopt an age-old construct known as "pass-by-name parameters".  This technique dates all the way back to ALGOL (possibly even further).  In fact, it's so old and obscure that I've actually had professors tell me that it has been completely abandoned in favor of the more conventional pass-by-value (what Java, C#, Scala and most languages use) and pass-by-reference (which is available in C++).  Pass-by-name parameters are very much like normal parameters in that they are used to copy values from a calling scope into the method in question.  However, unlike normal parameters, they are evaluated on an as-needed basis.  This means that a pass-by-name parameter will only be evaluated if its value is required within the method called.  For example:

```scala
def doSomething(a: =>Int) = 1 + 2
def createInteger() = {
  println("Made integer")
  42
}

println("In the beginning...")
doSomething(createInteger())
println("...at the end")
```

Counter to our first intuition, this will print the following:

```
In the beginning...
...at the end
```

In other words, the `createInteger` method is never called!  This is because the value of the pass-by-name parameter in the `doSomething` method is never accessed, meaning that the value of the expression is not needed.  The `a` parameter is denoted pass-by-name by the `=>` notation (just in case you were wondering).  We can apply this to our implementation by changing the parameter of the implicit conversion from pass-by-value to pass-by-name:

```scala
implicit def elvisOperator[T](alt: =>T) = new {
  def ?:[A >: T](pred: A) = if (pred == null) alt else pred
}
```

The language-level implementation of the `if`/`else` conditional expression will ensure that the `alt` parameter is only accessed iff the value of `pred` is `null`, meaning we have finally satisfied all three properties.  We can check this by compiling and running our specification from earlier:

```
Specification "TernarySpecs"
  elvis operator should
  + use predicate when not null
  + use alternative when null
  + type correctly
  + infer join of types
  + only eval alternative when null

Total for specification "TernarySpecs":
Finished in 0 second, 78 ms
5 examples, 6 assertions, 0 failure, 0 error
```

### Conclusion

We now have a working implementation of Groovy's Elvis operator within Scala and we never had to move beyond simple API design.  Truly, one of Scala's greatest strengths is its ability to expression extremely complex constructs within the confines of the language.  This makes it uniquely well-suited to hosting internal domain-specific languages.  Using techniques similar to the ones I have outlined in this article, it is possible to define operations which would require compiler-level implementation in most languages.

The full source (such as it is) for the Elvis operator in Scala is available for download, along with a bonus implementation of C#'s `??` syntax (just in case you prefer it).  The implementation differs slightly due to the fact that `??` is a left-associative operator, but the single-use (unchained) semantics are identical.  Enjoy!

  * Download [implementation sources](<http://www.codecommit.com/blog/misc/scala_ternary.zip>)