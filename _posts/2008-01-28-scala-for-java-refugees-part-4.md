---
categories:
- Scala
date: '2008-01-28 01:00:34 '
layout: post
title: 'Scala for Java Refugees Part 4: Pattern Matching and Exceptions'
wordpress_id: 183
wordpress_path: /scala/scala-for-java-refugees-part-4
---

So far, we've been examining the [similarities between Scala and Java](<http://www.codecommit.com/blog/scala/scala-for-java-refugees-part-1>).  Areas where Scala syntax is so similar to Java's as to be almost identical.  Of course, this doesn't take full advantage of the language, but it allows developers to utilize the cleaner Scala syntax within the restrictions of a familiar environment.  From this point on, things are going to get a bit weird.

A number of the "more advanced" features in the Scala language are clear departures from Java.  They may mirror the corresponding Java feature, but in syntax the concepts are totally different.  Among these "different features" is the concept of pattern matching, something critical to the understanding of exceptions.

### Pattern Matching

As is oft parroted on the blogosphere, pattern matching in Scala is really a lot like Java's _switch/case_ construct.  So in Java, one might write something like this:

```java public boolean checkPrime(int number) { // checks if a number between 1 and 10 is prime switch (number) { case 1: return true; case 2: return true; case 3: return true; case 5: return true; case 7: return true; default: return false; } } ``` 

It's obviously an extremely naive prime number sieve (if you can call it that), but it demonstrates the example quite effectively.  This familiar usage of _switch/case_ is basically just a shortened _if/else_ block.  One could just as easily have written this as a series of _if_ statements, however it would have been less concise.  Interestingly enough, _switch/case_ is often discouraged in object-oriented languages in favor of cleaner architectures making use of polymorphism for the same result.  But I digress...

One of the major limitations of _switch/case_ in Java (and really any C derivative language) is that it can only be used on primitives.  You can't use _switch/case_ to test a _String_ , for example (a need which arises more often than one would think).  In fact, the most complex type testable within _switch/case_ is the _Enum_ , and even this is just being reduced to its ordinal values under the surface.  In short, _switch/case_ is a very crippled hold-over from the dark ages of _#include_ and _extern._

Fortunately, the designers of Scala chose not to include this "feature" in their new language.  Instead, they implemented a "new" (it's actually been around in other languages for a long time) concept called pattern matching.  At a basic level, it allows algorithms which are very similar to the _checkPrime(int)_ example:

```scala def checkPrime(number:Int):Boolean = { number match { case 1 => return true case 2 => return true case 3 => return true case 5 => return true case 7 => return true case _ => return false } } ``` 

Looks a lot like the Java example, right?  The biggest difference which likely jumps out at you is the lack of a _default_ statement.  Instead, we see the return of Scala's ubiquitous wildcard character, the underscore.  Literally read, this example means: match the value within number; in the case that it is 1, return true; in the case that it is 2, return true; ...; for any previously unmatched value, return false. 

Scala _case_ statements can't "overflow" into each-other (causing multiple matches) like Java's can, so even if we weren't returning values, the algorithm would still be safe.  In fact the method can be more concisely expressed as follows:

```scala def checkPrime(number:Int) = number match { case 1 => true case 2 => true case 3 => true case 5 => true case 7 => true case _ => false } ``` 

This works because the _match_ statement actually returns whatever value is returned from the matched _case_ statement.  This allows _match/case_ algorithms to be far more "functional" in nature, actually improving readability (as in the above example).

Now if this were the end of Scala's pattern matching capabilities, it would still be worth using.  However, Scala's capabilities go far beyond mere integer comparison.  For example, Scala's _match_ statements are not restricted to "primitive" types (which Scala doesn't have, incidentally).  You can just as easily perform pattern matching on strings or even more complex values.  For example, the _instanceof_ operation in Scala looks like this:

```scala var obj = performOperation() var cast:Color = obj match { case x:Color => x case _ => null } ``` 

In Java of course we would do something like this:

```java Object obj = performOperation(); Color cast = null; if (obj instanceof Color) { cast = (Color) obj; } ``` 

The Scala example may look a bit odd, but trust me when I say that it's really quite elegant.  In the example, we're actually performing pattern matching against the _type_ of the input.  If the type matches, then the case-local variable _x_ is populated with the value from _obj_ and assigned type _Color_.  This variable can then be returned from the _match,_ and thus assigned to _cast._   If no type is matched, just return _null._   Scala allows a great deal of power in its pattern matching in that it allows the implicit assignment of case-local variables based on the matches.  In the example given, _x_ is implicitly assigned iff (if and only if) the matched value is of the given type.

### Case Classes

Now type matching may be cool, but once again it fails to encompass the awesome power afforded by Scala's _match_ statement.  Not only is Scala capable of inspecting the type it is matching, but also values _within_ that type.  This probably doesn't make much sense, so once again we resort to code samples:

```scala case class Number(value:Int) def checkPrime(n:Number) = n match { case Number(1) => true case Number(2) => true case Number(3) => true case Number(5) => true case Number(7) => true case Number(_) => false } checkPrime(Number(12)) ``` 

The first statement is the key to the whole thing: defining a new _case class_ "Number" with a single property.  Case classes are a special type of class in Scala which can be matched directly by pattern matching.  The also have some other attributes such as a compiler-defined _toString()_ , a proper equivalency operation and so on.  You can also create a new instance of a case class without the _new_ operator (similar syntax to instantiation in C++).  But in every other respect, case classes can be treated identically to normal classes. 

Scalists (is that a word?) like to use case classes in situations where they need a "quick and dirty" class, due to the predefined operations and the conciseness of its instantiation syntax.  I personally don't care for this convention, mainly because case classes have some odd corners which can bite you when you least expect.  For one thing, case classes cannot extend other case classes (though they can extend normal classes and normal classes can inherit from case classes).  More importantly, case classes become implicitly abstract if they inherit an abstract member which is not implemented.  This can lead to some very strange looking compiler errors when attempting pattern matching on what you thought was a valid hierarchy.

Anyway, back to our example.  For each case, we're actually creating a new instance of _Number,_ each with a different _value._   This is where the significance of the case class instantiation syntax comes into play.  Scala then takes our new instance and compares it with the one being matched (and this is all type-checked by the way).  Scala sees that the instances are the same type, so it _introspects_ the two instances and compares the property values (in this case, just _value_ ).  Now this would seem to be massively inefficient, but Scala is able to do some clever things with case classes in pattern matching and everything turns out nicely.

Everything seems sort of intuitive until we reach the final case, which is using our friend the underscore.  Of course we could have just written this as the "any case" ( _case __ ) but I wanted to demonstrate wildcards in case classes.  This statement basically means "match objects of type _Number_ with any value".  This is the case which is matched by our _checkPrime(Number(12))_ invocation farther down.  Oh, and as a side note, if _null_ is passed to this function, Scala will throw a _MatchError,_ proving once again the loveliness of the Scala type system.

Of course, this sort of matching doesn't seem very useful.  All we did was encapsulate an _Int_ within a case class.  While this is cool for illustrative purposes, it would be grounds for execution if seen in a real code base.  This is where the power of inheritence meets case classes.

```scala class Color(val red:Int, val green:Int, val blue:Int) case class Red(r:Int) extends Color(r, 0, 0) case class Green(g:Int) extends Color(0, g, 0) case class Blue(b:Int) extends Color(0, 0, b) def printColor(c:Color) = c match { case Red(v) => println("Red: " + v) case Green(v) => println("Green: " + v) case Blue(v) => println("Blue: " + v) case col:Color => { print("R: " + col.red + ", ") print("G: " + col.green + ", ") println("B: " + col.blue) } case null => println("Invalid color") } printColor(Red(100)) printColor(Blue(220)) printColor(new Color(100, 200, 50)) printColor(null) ``` 

The output will look something like this:

``` Red: 100 Blue: 220 R: 100, G: 200, B: 50 Invalid color ``` 

There are a couple of important things about this example.  Firstly, you should notice that we're heavily using the feature in pattern matching which allows us to assign new values as part of the match.  In each of the specific color cases ( _Red, Green, Blue_ ) we're passing an undefined variable _v_ to the new case class instance.  This variable will be assigned the property value of the class if a match is made.  So for our first invocation, the matcher finds that we're looking at an instance of _Red_.  It then retrieves the property value from the instance ( _red_ ) and assigns that value to the local case parameter _x_.  This value is then usable within the definition of the case.

The second thing which should jump out at you is the use of polymorphic case classes.  Here we have several specific types of _Color_ which each take a single value as their property.  This value is then passed to the super-constructor (along with a number of literal values, depending on the color).  _Red, Green_ and _Blue_ are all case classes, _Color_ is not.  We can't just say _case Color(r, g, b) = >_ because _Color_ is just an ordinary class, it cannot be matched in such a fashion.

This pattern is also the first we have seen with a multi-line case.  Technically, this is still just a single expression (a scope) which itself contains multiple expressions, but you can still think of it like a _switch_ statement with multiple statements prior to a _break._

Finally, if nothing else applies, the instance will match _case null_ and print our "Invalid color" message.  You'll notice we did no explicit _null_ checking, we just let the matcher handle the ugly details for us.  Now isn't that clean?

Case classes are about the closest thing Scala has to Java's enumerations.  Oh Scala does have a type _Enumeration_ that you can do interesting things with, but idiomatically case classes are almost exclusively used in situations where enumerated values would be employed in Java.  This of course lends itself to greater flexibility (because case classes may contain values) and better "wow factor" when showing off your code.  :-)

### Exception Handling

Well I've been promising all along that I would somehow tie this in with exceptions and the time is now.  It turns out that Scala doesn't have exception handling, at least not in the way that Java does.  Instead, Scala allows you to _try/catch_ any exception in a single block and then perform pattern matching against it.

Let's take our _checkPrime()_ example.  It really only handles integers between 1 and 10, so it would be quite nice to test this precondition and throw an exception if it fails.  We can do this quite trivially:

```scala def checkPrime(n:Int) = { if (n < 1 || n > 10) { throw new IllegalArgumentException("Only values between 1 and 10") } n match { case 1 => true case 2 => true // ... } } ``` 

Not very interesting from a theoretical standpoint, but it's a solid piece of code.  You can see that Scala's exception throwing syntax is almost identical to Java's.  In fact, the only real difference between Scala's exception dispatch and Java's is that Scala does not have checked exceptions.  It's not very apparent in this example, but we could have just as easily thrown an instance of _java.lang.Exception_ or some other "checked" exception, without modifying our method signature.  Scala also does not _force_ you to catch exceptions, it trusts that you'll clue in when your program crashes.

So how do we invoke this method, pedantically watching for exceptions?  It turns out that the syntax looks surprisingly like pattern matching:

```scala try { checkPrime(12) } catch { case e:IllegalArgumentException => e.printStackTrace() } ``` 

You see the _catch_ clause is actually a _match_ statement under the surface on any exception thrown by the body of _try_.  The _catch_ runs through and matches against the various cases and finds our _IllegalArgumentException_ rule.  Once again, we're implicitly creating a new local variable within a pattern.

If we wanted to catch all exception types, we could resort to our old friend the underscore:

```scala try { checkPrime(12) } catch { case _ => println("Caught an exception!") } ``` 

This syntax seems to really only lend itself to a single _catch_ block, rather than Java's concept of a different _catch_ for each exception type.  But since _catch_ is really just a pattern matching block, it seems obvious that we don't really need more than one _catch_ block, we can just handle different exception types as different cases:

```scala import java.sql._ import java.net._ Connection conn = DriverManager.getConnection("jdbc:hsqldb:mem:testdb", "sa", "") try { PreparedStatement stmt = conn.prepareStatement("INSERT INTO urls (url) VALUES (?)") stmt.setObject(1, new URL("http://www.codecommit.com")) stmt.executeUpdate() stmt.close() } catch { case e:SQLException => println("Database error") case e:MalformedURLException => println("Bad URL") case e => { println("Some other exception type:") e.printStackTrace() } } finally { conn.close() } ``` 

Since Scala doesn't actually have checked exceptions, we really could get away with not catching the _MalformedURLException_ (since we know that it's correct).  However, it's worth showing the multiple-exception scenario just for the sake of example.

This example also shows use of the _finally_ block, something which should be quite familiar to any Java developers.  In Scala, the finally works precisely the way it does in Java, so there should be no concerns about odd behavior when designing code which requires it.

### Conclusion

Scala exceptions are fairly intuitive beasts.  They behave almost exactly like Java's (with the exception of being all unchecked), providing a familiarity and an interoperability with existing Java libraries.  The exception handling mechanism however demonstrates considerably more power, making use of Scala's built-in pattern matching capabilities and thus maintaining a far more internally consistent syntax.

The next article in our series will delve a little deeper into the hidden mysteries of Scala's type system: [traits and type parameters](<http://www.codecommit.com/blog/scala/scala-for-java-refugees-part-5>).