{%
laika.title = "Scala for Java Refugees Part 3: Methods and Statics"
laika.metadata.date = "2008-01-21"
%}


# Scala for Java Refugees Part 3: Methods and Statics

In this series, we've already [laid the foundations](<http://www.codecommit.com/blog/scala/scala-for-java-refugees-part-1>) for Scala's syntax as well as gotten a feel for how some of its [object-oriented constructs](<http://www.codecommit.com/blog/scala/scala-for-java-refugees-part-2>) work.  We haven't really looked at any of these subjects in-depth however.  Most of our effort has been focused on high-level, flash bang overviews that just get you tasting your way into the language.  This post will go into greater depth regarding method syntax, touch a bit on scopes and attempt to cover how static members work in Scala.  We will also touch on a few gotchas due to "missing" imperative instructions.

### Methods Galore

Scala isn't called a "functional language" just because it's [Turing complete](<http://en.wikipedia.org/wiki/Turing_completeness>).  Scala has a very powerful and flexible syntax as it relates to methods, both declaration and invocation.  We've already seen some basic samples:

```scala
class Person {
  def firstName() = {
    var back:String = ...   // read from a database
    back
  }
}
```

Fairly straightforward.  But this doesn't really give you the full picture.  In Java for example, you can create methods with different visibilities, modifiers and (oddly enough) return types.  Does Scala support all of this flexibility?

The answer is a qualified "yes".  Scala does allow for different visibilities on not just methods, but any members.  For example:

```scala
class Person {
  private var name = "Daniel Spiewak"
  val ssn = 1234567890    // public constant field

  def firstName() = splitName()(0)   // public method

  private def splitName() = name.split(" ")    // private method

  protected def guessAge() = {
    import Math._
    round(random * 20)
  }
}
```

At the risk of going on a tangent, it's worth pointing out the (seemingly) out of place _import_ statement within the _guessAge()_ method.  I mentioned in the first post that Scala's import is far more powerful than Java's.  One of its many charms is imparting to the power to import into a specific scope.  The _import_ statement within _guessAge()_ is much like a Java _static import_ statement which only provides access to the _Math_ members within the _guessAge()_ method.  So we couldn't just make a call to _round()_ from within the _splitName()_ method.  Rubyists can think of it much like the _include_ statement without all of the hassle (it's not actually including, it's importing and so eliminating the need for fully-qualified names).

Scala access modifiers are also quite a bit more powerful than Java's.  For example, _protected_ by default limits access to only subclasses, unlike Java which also allows access to other classes in the same package.  More importantly though, Scala allows the developer to more explicitly specify the scope of the access modifier.  This is accomplished using the _modifier\[package\]_ notation.  For example:

```scala
package com.codecommit.mypackage

class MyClass {
  private[mypackage] def myMethod = "test"
}
```

In this example, _myMethod_ is access-restricted to both the enclosing class _and_ the enclosing package.  Essentially, this is how the Java-style package private modifier can be emulated using Scala.  The _protected_ modifier also allows such visibility qualifiers.  The one restriction here is that the package specified must be an enclosing package.  In the above example, specifying _private\[com.codecommit.mypackage\]_ is perfectly valid, but _private\[scala.collection.immutable\]_ would not be correct.

So with the exception of package-private, visibilities work about the same in Scala as they do in Java, both in syntax and function.  Modifiers are really where things get interesting.  Scala has far fewer method modifiers than Java does, primarily because it doesn't _need_ so many.  For example, Scala supports the _final_ modifier, but it doesn't support _abstract, native_ or _synchronized:_

```scala
abstract class Person {
  private var age = 0

  def firstName():String
  final def lastName() = "Spiewak"

  def incrementAge() = {
    synchronized {
      age += 1
    }
  }

  @native
  def hardDriveName():String
}
```

If we were in Java, we would write the above like this:

```java
public abstract class Person {
    private int age = 0;

    public abstract String firstName();

    public final String lastName() {
        return "Spiewak";
    }

    public synchronized void incrementAge() {
        age += 1;
    }

    public native String hardDriveAge();
}
```

Yes, I know it's more "Scala-esque" to use actors rather than _synchronized()_ , but one step at a time.

You see how Scala keeps with its theme of making the common things concise?  Think about it, almost every method you declare is public, so why should you have to say so explicitly?  Likewise, it just makes sense that methods without a body should be implicitly abstract (unless they're native).

One very important type-saving feature that you should see in the example above is that Scala doesn't _force_ you to declare the return type for your methods.  Once again the type inference mechanism can come into play and the return type will be inferred.  The exception to this is if the method can return at different points in the execution flow (so if it has an explicit return statement).  In this case, Scala forces you to declare the return type to ensure unambiguous behavior.

You should also notice that none of the Scala methods actually include a _return_ statement.  This of course seems odd as judging by the Java translation, _lastName()_ should return a _String_.  Well it turns out that Scala carries a useful shortcut for method returns: the last statement in an expression, be it a scope, a closure or a method becomes its return value.  This convention is also found in languages like Ruby and Haskell.  This example illustrates:

```scala
def name() = {
  val name = new StringBuilder("Daniel")
  name.append(" Spiewak");
  name.toString()
}

val s = name()
println(s)    // prints "Daniel Spiewak"
```

Again, in this example the return type of the method is inferred (as _String_ ).  We could just as easily have written the _name()_ method as follows, it just would have been less concise.

```scala
def name():String = {
  val name = new StringBuilder("Daniel")
  name.append(" Spiewak");
  return name.toString()
}
```

This "returnless" form becomes extremely important when dealing with anonymous methods (closures).  Obviously you can't return from a closure, you can only _yield,_ however the principle is the same.  Since closures are often used to reduce code bulk and make certain algorithms more concise, it only makes sense that their return syntax would be as compact as possible:

```scala
val arr = Array(1, 2, 3, 4, 5)
val sum = arr.reduceLeft((a:Int, b:Int) => a + b)

println(sum)    // 15
```

In this example we're passing an anonymous method to the _reduceLeft()_ method within _Array_.  This method just calls its parameter function repeatedly for each value pair, passing them as the _a_ and _b_ parameters.  Here's the key part though: our anonymous method adds the two parameters and yields the result back to _reduceLeft()_.  Again, no _return_ statement (or actually, as a closure it would be _yield_ ).  Also, we don't explicitly specify the return type for the closure, it is inferred from our last (and only) statement.

### Method Overriding

A very important concept in object-oriented programming is method overriding, where a subclass redefines a method declared in a superclass.  Java's syntax looks like this:

```java
public class Fruit {
    public int getWorth() {
        return 5;
    }
}

public class Apple extends Fruit {
    @Override
    public int getWorth() {
        return 1;
    }
}
```

Technically, the _@Override_ annotation is optional, but it's still good practice to use it.  This gives you the compile-time assurance that you actually are overriding a method from a superclass.  In principle, a method declared in a subclass overrides any method in the superclass declared with the exact same signature.  At first glance this seems great, less syntax right?  The problem is when you start dealing with APIs where you're uncertain if you got the overriding method signature right.  You could just as easily overload the method rather than overriding it, leading to totally different functionality and sometimes hard-to-trace bugs.  This is where _@Override_ comes in.

Scala actually has a bigger problem with method overriding than just signature verification: multiple inheritance.  Multiple inheritance is when one class inherits from more than one superclass.  C++ had this feature years ago, effectively demonstrating how [horrible it can really be](<http://en.wikipedia.org/wiki/Diamond_problem>).  When Gosling laid out the initial spec for Java, multiple inheritance was one of the things specifically avoided.  This is good for simplicity, but it's sometimes constraining on the power-end of life.  Interfaces are great and all, but sometimes they just don't cut it.

The key to avoiding ambiguities in the inheritance hierarchy is explicitly stating that a method _must_ override a superclass method.  If that method has the same signature as a superclass method but doesn't _override_ it, a compile error is thrown.  Add to that significant ordering in the _extends/with_ clauses, and you get a workable multiple-inheritance scheme.  But I'm getting ahead of myself...

Here's the _Fruit_ example, translated into Scala:

```scala
class Fruit {
  def worth() = 5
}

class Apple extends Fruit {
  override def worth() = 1
}
```

Notice that in Scala, _override_ is actually a keyword.  It is a mandatory method modifier for any method with a signature which conflicts with another method in a superclass.  Thus overriding in Scala isn't implicit (as in Java, Ruby, C++, etc), but explicitly declared.  This little construct completely solves the problems associated with multiple inheritance in Scala.  We'll get into _trait_ s and multiple inheritance in more detail in a future article.

Often times when you override a method, you need to call back to the superclass method.  A good example of this would be extending a Swing component:

```scala
class StrikeLabel(text:String) extends JLabel(text) {
  def this() = this("")

  override def paintComponent(g:Graphics):Unit = {
    super.paintComponent(g)

    g.setColor(Color.RED)
    g.drawLine(1, getHeight() / 2, getWidth() - 1, getHeight() / 2)
  }
}
```

This component is just a rack-standard _JLabel_ with a red line drawn through its center.  Not a very useful component, but it demonstrates a pattern we see used a lot in Java: delegating to the superclass implementation.  We don't want to actually implement all of the logic necessary to paint the text on the _Graphics_ context with the appropriate font and such.  That work has already been done for us in _JLabel._   Thus we ask _JLabel_ to paint itself, then paint our _StrikeLabel_ -specific logic on top.

As you see in the example, the syntax for making this superclass delegation is almost precisely the same as it is in Java.  Effectively, _super_ is a special private value (much like _this_ ) which contains an internal instance of the superclass.  We can use the value just like _super_ in Java to access methods and values directly on the superclass, bypassing our overriding.

That little bit of extra syntax in the _extends_ clause is how you call to a superclass constructor.  In this case, we're taking the _text_ parameter passed to the default constructor of the _StrikeLabel_ class and passing it on to the constructor in _JLabel_.  In Java you do the same thing like this:

```java
public class StrikeLabel extends JLabel {
    public StrikeLabel(String text) {
        super(text);
    }

    public StrikeLabel() {
        this("");
    }
}
```

This may seem just a bit odd at first glance, but actually provides a nice syntactical way to ensure that the call to the super constructor is _always_ the first statement in the constructor.  In Java, this is of course compile-checked, but there's nothing intuitively obvious in the syntax _preventing_ you from calling to the super constructor farther down in the implementation.  In Scala, calling the super constructor and calling a superclass method implementation are totally different operations, syntactically.  This leads to a more intuitive flow in understanding why one can be invoked arbitrarily and the other must be called prior to anything else.

### Scala's Sort-of Statics

Scala is a very interesting language in that it eschews many of the syntax constructs that developers from a Java background might find essential.  This ranges from little things like flexible constructor overloading, to more complex things like a complete _lack_ of static member support.

So in Java, static members are just normal class members with a different modifier.  They are accessed outside of the context of a proper instance using the classname as a qualifier:

```java
public class Utilities {
    public static final String APP_NAME = "Test App";

    public static void loadImages() {
        // ...
    }

    public static EntityManager createManager() {
        // ...
    }
}

System.out.println(Utilities.APP_NAME);

Utilities.loadImages();
EntityManager manager = Utilities.createManager();
```

Scala does support this type of syntax, but under the surface it is quite a bit different.  For one thing, you don't use the _static_ modifier.  Instead, you declare all of the "static" members within a special type of class which acts as an only-static container.  This type of class is called _object._

```scala
object Utilities {
  val APP_NAME = "Test App"

  def loadImages() = {
    // ...
  }

  def createManager():EntityManager = {
    // ...
  }
}

println(Utilities.APP_NAME)

Utilities.loadImages()
val manager = Utilities.createManager()
```

The syntax to use these "statics" is the same, but things are quite a bit different in the implementation.  It turns out that _object_ actually represents a singleton class.  _Utilities_ is in fact both the classname and the value name to access this singleton instance.  Nothing in the example above is static, it just seems like it is due to the way the syntax works.  If we port the above class directly to Java, this is what it might look like:

```java
public class Utilities {
    private static Utilities instance;

    public final String APP_NAME = "Test App";

    private Utilities() {}

    public void loadImages() { 
        // ...
    }

    public EntityManager createManager() {
        // ...
    }

    public static synchronized Utilities getInstance() {
        if (instance == null) {
            instance = new Utilities();
        }

        return instance;
    }
}

// ...
```

So Scala provides a special syntax which basically gives us a singleton for free, without all of the crazy syntax involved in declaring it.  This is a really elegant solution to the problems with proper statics.  Since Scala doesn't actually have static members, we no longer have to worry about mixing scopes, access qualifiers, etc.  It all just works nicely.

But what about mixing static and instance members?  Java allows us to do this quite easily since _static_ is a qualifier, but Scala requires "static" members to be declared in a special singleton class.  In Java, we can do this:

```java
public class Person {
    public String getName() {
        return "Daniel";
    }

    public static Person createPerson() {
        return new Person();
    }
}
```

The solution in Scala is to declare both an _object_ and a _class_ of the same name, placing the "static" members in the _object_ and the instance members in the _class_.  To be honest, this seems extremely strange to me and is really the only downside to Scala's singleton syntax:

```scala
object Person {
  def createPerson() = new Person()
}

class Person {
  def name() = "Daniel"
}
```

The syntax for using this _object/class_ combination is exactly the same as it would be in Java had we mixed static and instance members.  The Scala compiler is able to distinguish between references to _Person_ the _object_ and references to _Person_ the _class._   For example, the compiler knows that we can't create a new instance of an _object_ , since it's a singleton.  Therefore we must be referring to the _class_ _Person_ in the _createPerson()_ method.  Likewise, if a call was made to _Person.createPerson()_ , the compiler is more than capable of deducing that it must be a reference to the _object Person_ as there is no way to access a method directly upon a class.  It's all perfectly logical and consistent, it just strikes the eye funny when you look at it.

### Conclusion

And so ends our two part, whirlwind tour of Scala's object-oriented constructs, methods and statics.  There are of course trivialities along the way which we haven't covered, but those are easy enough to learn now that you have the basics.  The more interesting syntax is still to come though.  For one thing, we've barely scratched the surface of all of the things that you can do with methods.  They don't call it a "functional" language for nothing!  But in keeping with our goal to represent the imperative side of the Scala language, we'll save that for later.

In the next article, we'll look at [pattern matching and exception handling](<http://www.codecommit.com/blog/scala/scala-for-java-refugees-part-4>), two (surprisingly) related concepts.