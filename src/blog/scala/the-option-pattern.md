{%
laika.title = "The \"Option\" Pattern"
laika.metadata.date = "2008-04-07"
%}


# The "Option" Pattern

As I've gotten to know Scala better, I've begun to appreciate its simple power in ways which have caught me by surprise.  When I picked up the language, I was expecting to be wowed by things like type inference and a more concise syntax.  I wasn't expecting to fall in love with `Option`.

### The Option Monad

In case you don't know, `Option` is a class in the Scala core libraries.  It is what object-oriented developers would call "a simple container".  It simply wraps around an instance of some type as specified in its type parameter.  A simple application would be in a naive integer division function:

```scala
def div(a:Int)(b:Int):Option[Int] = if (b <= 0) None
  else if (a < b) Some(0)
  else Some(1 + div(a - b)(b).get)
```

Pretty straightforward stuff.  This method repeatedly subtracts the dividend by the divisor until it is strictly less than the divisor.  Of course, the fact that I wrote this as a pure function using currying, recursion and complex expressions obfuscates the meaning somewhat, but you get the drift.  What's really interesting here is the use of `Option` to encapsulate the result.  Here's how we could use this method to perform some useful(?) calculations:

```scala
div(25)(5)     // => Some(5)
div(150)(2)    // => Some(75)
div(13)(4)     // => Some(3)
```

Nothing earth-shattering in the mathematical realm, but it provides a useful illustration.  Each return value is wrapped in an instance of class `Some`, which is a subclass of `Option`.  This doesn't seem very useful until we consider what happens when we try to divide values which break the algorithm:

```scala
div(13)(0)     // => None
div(25)(-5)    // => None
```

Instead of getting an integer result wrapped in an enclosing class, we get an instance of a totally different class which doesn't appear to encapsulate any value at all.  `None` is still a subclass of `Option`, but unlike `Some` it does not represent any specific value.  In fact, it would be more accurate to say that it represents the _absence_ of a value.  This makes a lot of sense seeing as there really is no sane value for the first computation, and the second is simply incomputable with the given algorithm.

Retrieving a value from an instance of `Option` can be done in one of two ways.  The first technique is demonstrated in the `div` method itself (calling the no-args `get` method).  This is nice because it's terse, but it's not really the preferred way of doing things.  After all, what happens if the value in question is actually an instance of `None`?  (the answer is: Scala throws an exception)  This really doesn't seem all that compelling as a means of encapsulating return values.  That is why pattern matching is more frequently employed:

```scala
div(13)(0) match {
  case Some(x) => println(x)
  case None => println("Problems")
}
// => prints "Problems"

div(25)(5) match {
  case Some(x) => println(x)
  case None => println("Problems")
}
// => prints "5"
```

Pattern matching allows us to deconstruct `Option` values in a type-safe manner without the risk of trying to access a value which really isn't there.  Granted, the pattern matching syntax is a bit more verbose than just calling a `get` polymorphically, but it's more about the principle of the thing.  It's easy to see how this could be quite elegant in a non-trivial example.

#### Compared to `null`

This is very similar to a common pattern in C++ and Java.  Often times a method needs to return either a value or nothing, depending on various conditions.  More importantly, some internal state may be uninitialized, so a common "default" value for this state would be `null`.  Consider the following lazy initialization:

```java
public class Example {
    private String value;

    public String getValue() {
        if (value == null) {
             value = queryDatabase();
        }

        return value;
    }
}

// ...
Example ex = new Example();
System.out.println(ex.getValue());
```

Well, that's all well and good, but there's two problems with this code.  Number one, there's always the potential for stray null pointer exceptions.  This is certainly less of a concern in Java than it was back in the days of C and C++, but they still can be annoying.  However, let's just assume that we're all good programmers and we always check potentially-`null` values prior to use, there's still the problem of primitive types.  Let's change our example just a bit to see where this causes issues:

```java
public class Example {
    private int value;

    public int getValue() {
        if (value == ???) {    // er...?
             value = queryDatabase();
        }

        return value;
    }
}

// ...
Example ex = new Example();
System.out.println(ex.getValue());
```

If you're following along at home, your compiler will probably complain at this point saying that what you just wrote was not valid Java.  If your compiler _didn't_ complain, then you have some more serious issues that need to be addressed.

Primitive values cannot be valued as `null` because they are _true primitives_ (an `int` is actually a bona-fide integer value sitting in a register somewhere at the hardware level).  This too is a holdover from the days of C and C++, but it's something we have to deal with.  One of the consequences of this is that there is no reasonable "non-value" for primitive types.  Many people have tried clever little tricks to get around this, but most of them lead to horrible and strange results:

```java
public class Example {
    private Integer value = null;

    public int getValue() {
        // forgot to init...

        return value;
    }
}
```

This code will fail at runtime with a `NullPointerException` oddly originating from the return statement in `getValue()`.  I can't tell you how many times I've spent hours sifting through code I thought was perfectly safe before finally isolating a stray null value which the compiler happily attempted to autobox.

It's worth briefly mentioning that a common "non-value" for integers is something negative, but this breaks down when you can have legitimate values which fall into that range.  In short, there's really no silver bullet within the Java language, so we have to turn elsewhere for inspiration.

### Option in Java

I was actually working on an algorithm recently which required just such a solution.  In this case, the primitive value was a `boolean`, so there wasn't even a _conventional_ non-value to jump to.  I hemmed and hawed for a while before eventually deciding to implement a simple `Option` monad within Java.  The rest of the API is remarkably functional for something written in Java (immutable state everywhere), so I figured that a few monadic types would feel right at home.  Here's what I came up with:

```java
public interface Option<T> {
    public T get();
}

public final class Some<T> implements Option<T> {
    private final T value;
    
    public Some(T value) {
        this.value = value;
    }

    public T get() {
        return value;
    }
}

public final class None<T> implements Option<T> {
    
    public None() {}
    
    public T get() {
        throw new UnsupportedOperationException("Cannot resolve value on None");
    }
}
```

The usage for this code looks like this:

```java
public class Example {
    private Option<Boolean> value = new None<Boolean>();

    public boolean getValue() {
        if (value instanceof None) {
            value = queryDatabase();
        }

        return value.get();
    }
}
```

Once again, Java has demonstrated how needlessly verbose and annoying its syntax can be.  In case you were wondering, the generics are necessary on `None` primarily because Java has such a poor type system.  Effectively, `null` is an untyped value which may be assigned to any class type.  Java has no concept of a [Nothing](<http://www.scala-lang.org/docu/files/api/scala/Nothing.html>) type which is a subtype of anything.  Thus, there's no way to provide a default parameterization for `None` and the developer must specify.

Now this is certainly not the cleanest API we could have written and it's definitely not a very good demonstration of how monads can be applied to Java, but it gets the job done.  If you're interested, there's a lot of good information [out there](<http://blog.pretheory.com/arch/2008/02/the_maybe_monad_in_ruby.php>) on how do do [something like this](<http://weblog.raganwald.com/2008/01/objectandand-objectme-in-ruby.html>) better.  The point was not to create a pure monad though, the point was to create something that solved the problem at hand.

### Conclusion

Once you start thinking about structuring your code to use `Option` in languages which have built-in support for it, you'll find yourself dreaming about such patterns in other, less fortunate languages.  It's really sort of bizarre how much this little device can open your mind to new possibilities.  Take my code, and give it a try in your project.  Better yet, implement something on your own which solves the problem more elegantly!  The stodgy old Java "best practices" could use a little fresh air.

**P.S.** Yes, I know that the original implementation of this was actually the `Maybe` monad in Haskell.  I picked `Option` instead mainly because a) I like the name better, and b) it's Scala, so it's far more approachable than Haskell.