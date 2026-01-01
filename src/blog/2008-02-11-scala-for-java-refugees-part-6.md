{%
laika.title = "Scala for Java Refugees Part 6: Getting Over Java"
laika.metadata.date = "2008-02-11"
%}


# Scala for Java Refugees Part 6: Getting Over Java

Thus follows the sixth and final installment of my prolific "[Scala for Java Refugees](<http://www.codecommit.com/blog/scala/scala-for-java-refugees-part-1>)" series.  After this post, I will continue to write about Scala as the spirit moves, but I don't think I'll do another full-length _series_ focused entirely on this one topic.  It's a surprisingly exhausting thing to do, tying yourself to a single subject for so long.  ( _insert piteous moans from my keyboard_ )  Anyway, enough of my whining...

To be honest, I've been looking forward to this article from day one of the series.  This is the article where we get to open the door on all sorts of wonderful Scala-specific goodies.  So far, the focus has been mostly on areas where Scala's semantics more-or-less parity Java's.  In this article, we'll look at some of the many ways in which Scala surpasses its lineage.  It's time to get over that old girlfriend of yours and join me in the new tomorrow!

### Class Extensions

There has been some chit-chat around the Java communal fireplace talking about adding class extensions to Java 7.  The basic idea is that classes need not have fixed members, but that methods can be weaved into the class or instance depending on imports.  This is similar to the concept of "open classes" supported by highly dynamic languages like Ruby:

```ruby
class String
  def print_self
    puts self
  end
end

"Daniel Spiewak".print_self    # prints my name
```

This funky looking sample is actually adding a new method to the _String_ class (the one already defined by the language) and making it available to all instances of _String_ in the defining scope.  Actually, once this class definition executes, the _print_self_ method will be available to **all** instances of _String_ within any scope, but let's not be confusing.

Obviously, Java class extensions have to be a bit more controlled.  Things are statically typed, and what's more there are some hard and fast rules about namespaces and fully-qualified class names.  The compiler will actually prevent me from creating a class with the same fully qualified name as another.  The main proposal seems to be some sort of variation on static imports, with the use case being things like the _Collections.sort(List)_ method.

As one would expect from a language not tied to such heavy legacy baggage, Scala has managed to solve the problem of class extensions in a very elegant (and type-safe) way.  Actually, the solution took a lot of inspiration from C#, but that's not important right now.  The ultimate answer to the problem of class extensions is...[implicit type conversions](<http://technically.us/code/x/the-awesomeness-of-scala-is-implicit>).

Scala allows you to define methods which take a value of a certain type in as a parameter and return a value of a different type as the result.  This in and of itself isn't so unique until you add the real magic.  By declaring this method to be _implicit_ (which is a modifier keyword), you tell the compiler to automatically use this conversion method in situations where a value of type _A_ is called for but a value of type _B_ was passed.

Maybe an example would clear this up:

```scala
implicit def str2int(str:String):Int = Integer.parseInt(str)
def addTwo(a:Int, b:Int) = a + b

addTwo("123", 456)
```

Notice the type "error" in the final line.  With this call, we are passing a _String_ value to a method which is expecting an _Int._   Normally this is frowned upon, but before the compiler throws a fit, it takes one last look around and discovers the _str2int_ method.  This method takes a _String_ , returns an _Int_ and most importantly is declared _implicit_.  The compiler makes the assumption that however this method works, it somehow converts arbitrary _String_ values into _Int_ s.  With this bit of knowledge in hand, it is able to implicitly insert the method call to _str2int_ into the output binary, causing this sample to compile and return _579_ as the final value.

Now if that were all implicit type conversions were capable of, they would still be pretty amazing.  But fortunately for us, the cleverness doesn't end there.  The Scala compiler is also capable of intelligently finding the type you need given the context; more intelligently than just relying on assignment or method parameter type.  This is where implicit type conversions become the enabling factor for extension methods.

Let's imagine that I want to duplicate my Ruby example above in pure Scala.  My end goal is to "add" the _printSelf()_ method to the _String_ class.  This method should be usable from any _String_ instances within the enclosing scope (so enabling the literal/call syntax we had in Ruby).  To accomplish these ends, we're going to need two things: a composing class containing the extension method and an implicit type conversion.  Observe:

```scala
class MyString(str:String) {
  def printSelf() {
    println(str)
  }
}

implicit def str2mystring(str:String) = new MyString(str)

"Daniel Spiewak".printSelf()
```

I'd call that powerful.  Here the compiler sees that _String_ does not declare the _printSelf()_ method.  Once again, before it blows up it looks around for an implicit type conversion which might yield something with a _printSelf()_ method.  Conveniently enough, there is just such a conversion method declared in scope.  The compiler adds the call, and we're none-the-wiser.  As far as we're concerned, we just called the _printSelf()_ method on a _String_ literal, when actually we were invoking the method on a composing instance which wraps around _String_.

Implicit conversion methods are just that, fully-functional methods.  There are no limitations (that I know of) on what you can do in these methods as opposed to "normal" methods.  This allows for conversions of arbitrary complexity (though most are usually quite simple).  Oh, and you should note that the compiler checks for conversions solely based on type information, the name is not significant.  The convention I used here ( _typea2typeb_ ) is just that, a convention.  You can call your implicit conversion methods whatever you feel like.

### Operator Overloading

Moving right along in our whirl-wind tour of random Scala coolness, we come to the forgotten island of operator overloading.  Founded by mathematicians accustomed to dealing with different operational semantics for identical operators, operator overloading was abandoned years ago by the developer community after the fiasco that was/is C++.  Until I saw Scala, I had assumed that the technique had gone the way of lazy evaluation (another Scala feature) and pointer arithmetic. 

Some languages like Ruby support operator overloading in a very limited way, but even they tend to discourage it for all but the most hard-core use cases.  Scala on the other hand is really much closer to how mathematicians envisioned operator overloading in Turing-complete languages.  The distinction is simple: in Scala, method names can contain arbitrary symbols.

This may seem like a trivial point, but it turns out to be very powerful.  One of the leading problems with operator overloading in languages like C++ and Ruby is that you cannot define new operators.  You have a limited set of operators with hard-coded call semantics (less-so in Ruby).  These operators may be overloaded within carefully defined boundaries, but that's all.  Neither Ruby nor C++ succeed in elevating operator overloading to the level of a generally useful technique.

Scala avoids this trap by lifting the restriction against arbitrary operators.  In Scala, you can call your operators whatever you want because there is no special logic for dealing with them hard-coded into the compiler.  Little things like * precedence over + and so on **are** hard-coded, but the important stuff remains flexible.

So let's imagine that I wanted to define an insertion operator in Scala similar to the infamous << in C++.  I could go about it in this way:

```scala
import java.io.PrintStream

implicit def ps2richps(ps:PrintStream) = new RichPrintStream(ps)
class RichPrintStream(ps:PrintStream) {
  // method with a symbolic name
  def <<(a:Any) = {
    ps.print(a.toString())
    ps.flush()
    ps
  }
}

val endl = '\n'

System.out << "Daniel" << ' ' << "Spiewak" << endl
```

Wow!  We can actually write code as ugly as C++ even in fancy, new-fangled languages!  Obviously we have an implicit type conversion here (see above if you weren't paying attention the first time).  More interesting is the _< <(Any)_ method declared within the _RichPrintStream_ class.  This is actually a proper method.  There's no magic associated with it nor any funky limitations to bite you in the behind when you least expect it.

Looking down a bit further in the code, we see the "nicely" chained _PrintStream_ invocations using the _< <(Any)_ method and the implicit conversion from _PrintStream_ to _RichPrintStream_.  It may not look like it, but these are actually method calls just like the block-standard _var.method(params)_ syntax.  The line could just as easily have looked like this:

```scala
System.out.<<("Daniel").<<(' ').<<("Spiewak").<<(endl)
```

Of course, I'm not sure _why_ we would prefer the second syntax as opposed to the first.  This just illustrates the flexible nature of Scala's invocation syntax.  You can actually extend this concept to other methods.  For example:

```scala
class Factory {
  def construct(str:String) = "Boo: " + str
}

val fac = new Factory()

fac construct "Daniel"
// is the same as...
fac.construct("Daniel")
```

With methods which only take a single parameter, Scala allows the developer to replace the . with a space and omit the parentheses, enabling the operator syntax shown in our insertion operator example.  This syntax is used in other places in the Scala API, such as constructing _Range_ instances:

```scala
val firstTen:Range = 0 to 9
```

Here again, _to(Int)_ is a vanilla method declared inside a class (there's actually some more implicit type conversions here, but you get the drift).

### Tuples

Mathematics defines a structure such that 2 or more values are contained in an ordered list of _n_ dimension (where _n_ is the number of values in the "list").  This construct is called an [_n_ -tuple](<http://en.wikipedia.org/wiki/N-tuple>) (or just "tuple").  This is obviously a construct which is easily emulated in code through the use of an array or similar.  However the syntax for such constructions has always been bulky and unweildy, eliminating raw tuples from the stock toolset of most developers.  Shame, really.

Tuples are fundamentally a way of pairing discrete pieces of data in some sort of meaningful way.  Theoretically, they can be applied to many different scenarios such as returning multiple values from a method or examining key-value pairs from a map as a single, composite entity.  Really the only thing preventing programmers from exploiting the power of such simple constructs is the lack of an equivalently simple syntax.  At least, until now...

```scala
val keyValue = ("S123 Phoney Ln", "Daniel Spiewak")

println(keyValue._1)   // S123 Phoney Ln
println(keyValue._2)   // Daniel Spiewak
```

In this example, _keyValue_ is a so-called **2-tuple.**   Scala declares such values to be of type _(String, String)_.  And yes, that is an actual type which you can use in declarations, type parameters, even class inheritance.  Under the surface, the _(String, String)_ type syntax is actually a bit of syntax sugar wrapping around the _Tuple2[String, String]_ type.  In fact, there are 22 different " _n-_ tuple types" declared by Scala, one for each value of _n_ up to (and including) 22.

Tuples don't have to be all the same type either.  Here are a few tuples mapping between integer literals and their _String_ literal equivalents:

```scala
val tuple1 = (1, "1")
val tuple2 = (2, "2")
val tuple3 = (3, "3")

val (i, str) = tuple1

println(i)     // 1
println(str)   // "1"
```

What ho!  The simultaneous declaration of values _i_ and _str_ is another bit of Scala syntax sugar one can use in dealing with tuples.  As expected, _i_ gets the first value in the tuple, _str_ gets the second.  Scala's type-inference mechanism kicks in here and infers _i_ to be of type _Int_ and _str_ to be of type _String.   _This is inferred from the type of the _tuple1_ value, which is _(Int, String)_.

So what are they good for?  Well it turns out Scala allows you to put tuples to good use in a lot of ways.  For example, returning multiple values from a method:

```scala
class Circle {
  private val radius = 3

  def center():(Int, Int) = {
    var x = 0
    var y = 0
    // ...
    (x, y)
  }
}
```

Scala has no need for clumsy wrappers like Java's _[Point](<http://java.sun.com/javase/6/docs/api/java/awt/Point.html>)_ class.  Effectively, the _center()_ method is returning two separate values, paired using a tuple.  This example also showcases how we can use the tuple type syntax to specify explicit types for methods, variables and such.

The Map API can also benefit from a little tuple love.  After all, what are maps but effective sets of key-value tuples?  This next example shows tuples in two places, both the map iterator and the _Map()_ initialization syntax:

```scala
val tastiness = Map("Apple" -> 5, "Pear" -> 3, "Orange" -> 8, "Mango" -> 7, "Pineapple" -> 8)

println("On a scale from 1-10:")
tastiness.foreach { tuple:(String, Int) =>
  val (fruit, value) = tuple

  println("    " + fruit + " : " + value)
}
```

Remember our old friend _foreach_?  (think back to the [first article](<http://www.codecommit.com/blog/scala/scala-for-java-refugees-part-1>))  We'll look at the semantics of this a bit more in a second, but the important thing to focus on here is the map initialization syntax.  Scala defines _Map_ as an [object](<http://www.codecommit.com/blog/scala/scala-for-java-refugees-part-3-methods-and-statics>) with an _[apply()](<http://www.codecommit.com/blog/scala/diving-into-scala>)_ method taking a varargs array of tuples.  Got that?

The declaration for the object with just this method might look like this:

```scala
object Map {
  def apply[A,B](tuples:(A, B)*):Map[A,B] = {
    val back = new HashMap[A,B]
    tuples.foreach(back.+=)    // iterate over the tuple Array and add to back
    back
  }
}
```

The _apply()_ method is going to return a new _Map_ parameterized against whatever the type of the specified tuples happens to be (in this case _String_ and _Int_ ).  The * character on the end of the parameter type just specifies the parameter as varargs, similar to Java's "..." notation.

So all the way back to our _tastiness_ example, the first line could be read as: _declare a new value_ tastiness _and assign it the return value from the expression_ Map.apply(...) _where the parameter is an array of tuples._   The overloaded -> operator is just another way of declaring a tuple in code, similar to the _(valueA, valueB)_ syntax we saw earlier.

### Higher-Order Functions

Contrary to popular opinion, the term "higher-order function" doesn't refer to some sort of elitist club to which you must gain entrance before you can understand.  I know it may **seem** that way sometimes, but trust me when I say that higher-order functions are really quite easy and surprisingly useful.

Taking a few steps back (so to speak), it's worth pointing out that any Java developer with a modicum of experience has employed the patterns allowed by higher-order functions, knowingly or unknowingly.  For example, this is how you declare listeners on a _JButton_ using Swing:

```java
JButton button = new JButton("Push Me");
button.addActionListener(new ActionListener() {
    public void actionPerformed(ActionEvent e) {
        System.out.println("You pushed me!");
    }
});
add(button);
```

This example passes an instance of an anonymous inner class to the _addActionListener()_ method.  The sole purpose of this inner class is to encapsulate the _actionPerformed(ActionEvent)_ method in an object which can be passed around.  Effectively, this pattern is a form of higher-order function.  _addActionListener()_ accepts a single argument (called a **functional** ) which is itself a function delegate encapsulating a block of statements (in this case, one _println()_ ).

Of course, this isn't really a higher-order function since Java doesn't allow functional values.  You can't just pass a method to another method and expect something to happen (other than a compiler error).  This sort of anonymous inner class delegate instance pattern is really like a distant cousin to proper functionals.

Let's assume for one blissful moment that we could rewrite Swing to take full advantage of Scala's syntax.  Let's pretend that we changed the _addActionListener()_ method so that it actually would accept a true functional as the parameter, rather than this _ActionListener_ garbage.  The above example could then condense down to something like this:

```scala
val button = new JButton("Push Me")
button.addActionListener((e:ActionEvent) => {
  println("You pushed me!")
})
add(button)
```

Instead of a bulky anonymous inner class wrapping around our block of statements, we pass an **anonymous method** (a method without a name declared in-place similar to anonymous inner classes).  This method takes a single parameter of type _ActionEvent_ and when called performs a simple _println()_.  It is effectively the same as the Java example, except with one tenth the boiler-plate.

We can actually condense this example down even farther.  We can take advantage of some of the flexibility in Scala's syntax when dealing with function parameters and remove some of those nasty parentheses (after all, it's Scala, not LISP):

```scala
val button = new JButton("Push Me")
button.addActionListener { e:ActionEvent =>
  println("You pushed me!")
}
add(button)
```

Concise and intuitive, with no nasty surprises like only being able to access final variables (Scala anonymous methods can access any variable/value within its enclosing scope).  In fact, what we have here is currently the focus of a great deal of controversy within the Java language community.  This, dear friends, is a [closure](<http://en.wikipedia.org/wiki/Closure_\(computer_science\)>).

Wikipedia's definition falls a little bit short in terms of clarity, so let me summarize: a closure is exactly what it looks like, a block of code embedded within an enclosing block which logically represents a function (or method, the terms are roughly analogous).  This is the type of construct which people like [Neal Gafter](<http://gafter.blogspot.com/>) are pushing for inclusion into Java 7.  This addition would enable code similar to the above Scala example to be written in pure Java.

Most of the closures proposals though have a single, overwhelming point of opposition: cryptic syntax.  As I've said many times, Java is tied to a great deal of legacy baggage, especially syntactically.  This baggage prevents it from evolving naturally beyond a certain point.  Scala on the other hand has virtually no history, thus the designers were able to create a clean, well-considered syntax which reflects the needs of most developers.  You've seen how Scala allows you to declare and pass functionals, but what about the receiving end?  Does the syntax bulk up under the surface?

Here's a simple example which iterates over an array, calling a functional for each element:

```scala
def iterate(array:Array[String], fun:(String)=>Unit) = {
  for (i <- 0 to (array.length - 1)) {    // anti-idiom array iteration
    fun(array(i))
  }
}

val a = Array("Daniel", "Chris", "Joseph", "Renee")
iterate(a, (s:String) => println(s))
```

See?  The syntax is so natural you almost miss it.  Starting at the top, we look at the type of the _fun_ parameter and we see the _(type1, ...)=>returnType_ syntax which indicates a functional type.  In this case, _fun_ will be a functional which takes a single parameter of type _String_ and returns _Unit_ (effectively _void_ , so anything at all).  Two lines down in the function, we see the syntax for actually invoking the functional.  _fun_ is treated just as if it were a method available within the scope, the call syntax is identical.  Veterans of the C/C++ dark-ages will recognize this syntax as being reminiscent of how function pointers were handled back-in-the-day.  The difference is, no memory leaks to worry about, and no over-verbosity introduced by too many star symbols.

At the bottom of the example, we see another (slightly different) syntax for specifying an anonymous method.  In this case, the method is just a single expression, so we don't need all the cruft entailed by a proper block.  So we drop the braces altogether and instead write the method on a single line, declaring parameters and handling them within.

We're not done though.  Scala provides still more flexibility in the syntax for these higher-order function things.  In the _iterate_ invocation, we're creating an entire anonymous method just to make another call to the _println(String)_ method.  Considering _println(String)_ is itself a method which takes a _String_ and returns _Unit_ , one would think we could compress this down a bit.  As it turns out, we can:

```scala
iterate(a, println)
```

By omitting the parentheses and just specifying the method name, we're telling the Scala compiler that we want to use _println_ as a functional value, passing it to the _iterate_ method.  Thus instead of creating a new method just to handle a single set of calls, we pass in an old method which already does what we want.  This is a pattern commonly seen in C and C++.  In fact, the syntax for passing a function as a functional value is precisely the same.  Seems that some things never change...

Now there is one outstanding dilemma here that the attentive will have picked up on: what about _println()_ (accepting no parameters)?  Of course Scala allows zero-arg method invocations to optionally omit the parameters for brevity's sake.  What's to prevent the compiler from assuming that instead of wanting the value of _println(String)_ as a functional, perhaps we actually want the **return value** of _println()_.  Well the answer is that the Scala compiler is very smart.  It has no trouble with this particular sample in differentiating between the different cases and choosing the unambiguous answer.

But assuming that the compiler couldn't figure it out, there's still a syntax to force the compiler to accept a method name as a functional rather than an actual invocation (Scala calls these "partially applied functions"):

```scala
iterate(a, println _)
```

That dangling underscore there is not a weird typo introduced by WordPress.  No, it's actually a weird construct introduced by Martin Odersky.  This underscore (preceded by a method name and a non-optional space) tells the compiler to look at _println_ as a functional, rather than a method to be invoked.  Whenever you're in doubt about whether you're semantically passing a functional or a return value, try throwing in the underscore suffix.  If you can't figure it out, the compiler probably can't either.

I could go on talking about higher-order functions for days (and many people have), but I think I'll just close with one final note.  A lot of features throughout the Scala API are designed as higher-order functions.  _foreach(),_ the standard mechanism for iterating over any _Iterable_ , is an excellent example of this:

```scala
val people = Array("Daniel", "Chris", "Joseph", "Renee")

people.foreach { name:String =>
  println("Person: " + name)
}
```

This is the idiomatic way to loop through an array in Scala.  In fact, as I said this is the idiom for looping through _anything_ in Scala which is potentially "loopable".  As you can now see, this is in fact a higher-order function taking an anonymous method as a parameter which it then calls once for each element in the array.  This makes sense from a logical standpoint.  After all, which is more "componentized": manually managing a loop over a range of values, or asking the array for each value in turn?

### So Long, Farewell...

That about wraps it up for my introductory series on Scala.  I certainly hope this set of articles was sufficient information to get you on your feet in this tremendously powerful new language.

If you're like me, something like this series will only whet your appetite (or dampen your spirits to the point of manic despair).  I strongly suggest you read Alex Blewitt's [excellent introduction](<http://alblue.blogspot.com/search/label/scala>) to Scala (if you haven't already).  Much of the material he talks about was covered in an article in this series, but he provides a different perspective and a degree of insight which is valuable in learning a new language.  There is also a [wiki for the Scala language](<http://scala.sygneca.com/>).  It has a frustrating lack of information on some (seemingly arbitrary) topics, but it can often be a source of explanation and usage examples that cannot be found elsewhere.

On a more "hard core" level, I have found the [scaladoc API](<http://www.scala-lang.org/docu/files/api/index.html>) for the Scala runtime to be an invaluable resource in my own projects.  Finally, when all else fails, there's always the [official Scala documentation](<http://www.scala-lang.org/docu/index.html>).  Included with this package is the (very heavy) [Scala tour](<http://www.scala-lang.org/intro/index.html>), which doesn't seem to be linked from anywhere except the Nabble mailing-list archive.

I leave you with this parting thought:  You've seen Scala, how it works, the benefits it can bring and the total transparency of its interop with Java.  If you haven't at least tried this language first hand, trust me, you're missing out.