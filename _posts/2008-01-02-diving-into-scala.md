---
categories:
- Scala
date: '2008-01-02 01:00:30 '
layout: post
title: Diving into Scala
wordpress_id: 172
wordpress_path: /scala/diving-into-scala
---

I came to the realization this week that I really don't know enough Scala (like I say, just enough to be dangerous).  After all, most experts agree that this is the general purpose language of the future, and it may just be a more productive language of the present.  With that in mind, I think it's a fair conclusion that _no_ Java developer should be without at least a working knowledge of the language.

In truth, I've been putting off learning more about Scala.  Yes, I'm sold on its benefits and what it could become, but I'm [very comfortable in my cozy IDE](<http://www.javaworld.com/javaworld/jw-12-2007/jw-12-outside-the-ide.html>) and often it seems like there's no immediate reason to change.  More than that, I was slightly put-off by all the functional goodies.  I mean, currying?  Function parameters?  Lazy evaluation?  It's enough to make an upstanding imperative developer's hair stand on end.  And every time I thought I had worked up the courage to try a more in-depth experiment, I'd browse over to the [Scala site](<http://www.scala-lang.org>) and there would be that demonic monocharacter-variabled quicksort implementation staring me in the face.  A word of advise, guys: that's not the best example for how _readable_ and _intuitive_ Scala can be!

But in the end, my curiosity and dislike-for-anything-I-can't-understand won the day.  A while back, I wrote an extremely naive chess engine in Java.  It was horribly inefficient (working recursively, rather than using a more conventional graph search algorithm) and it barely worked for even simple analyses, but it was at the time a diverting weekend project.  Remembering this project set me thinking about how I could have done it better.  Obviously some redesign would be needed on the core algorithm.  An astounding amount of graph theory comes into play (no pun intended) in chess analysis.  Graph search algorithms are usually profoundly functional in nature, which got me thinking about Scala again.  Working with closures, cheap recursion and first class functions can make such algorithms far more expressive, easy-to-read and efficient.  So I updated my Scala runtime, dusted off the "scala" mode definition for jEdit and got to work.

I won't deny, I was somewhat leery of stepping out of my nice Eclipse environment.  Granted, I program in Ruby all the time and rarely use an IDE for it, but somehow that's different.  Scala is a peer to Java from a language standpoint, so mentally I expected my experience to be similar to the time I wrote Java code in Notepad.  Nothing could be farther from the truth.

Writing code in Scala was like a breath of fresh air.  To my surprise, the code I was turning out was _both_ more concise _and_ more readable than the equivalent Java (a rare combination).  Add to that the fact that much of the Scala core API is based on (or actually _is_ ) the Java lang libraries and I found myself almost oblivious to the lack of code completion, inline documentation and incremental compilation (almost).  I did find myself keeping a Scala interpreter running in a separate shell (a habit I picked up doing Ruby with IRB), as well as using Google frequently to find little tidbits.  There's no doubt that the experience would have been more fluid with a toolset of the JDT caliber, but I wasn't totally dependant on my life support anymore.

Things were flowing smoothly, and I was just starting to pick up steam when my passenger train derailed on a tiny piece of miss-placed syntactic jello: arrays.

In Scala, arrays are objects just like any other.  Yes, I'm sure you heard that when transitioning to Java, but Scala really carries the concept through to its full character (think Ruby arrays).  This is how you allocate an array of size 5 in Scala:

```scala
var array = new Array[String](5)
```

Or, if you just have a set of static values, you can use the factory method (ish, more on that later):

```scala
var names = Array("daniel", "chris", "joseph", "renee", "bethany", "grace")
```

It was at this point that my warning sirens began going off.  Any syntax which looks even remotely like C++ deserves extremely close scrutiny before it should be used.

The first invocation is fairly straightforward.  We're creating a new instance of class _Array_ parameterized on the _String_ type.  Scala has type parameterization similar to Java generics except significantly more powerful.  In fact, Scala type parameters are really closer to lightweight templates (without the sytactical baggage) than to generics.  The Javascript-style variable declaration is actually typesafe because the compiler statically infers the type from the assignment (yet another "well, duh!" feature that Scala handles beautifully).

The second syntax is a bit more complex.  It is best understood once a bit of groundwork is laid analyzing the rest of the array syntax:

```scala
var array = new Array[String](5)
array(0) = "daniel"
array(1) = "chris"
println(array(0))
```

No, that's not a typo. Scala uses parentheses to access array values, not the C-style square brackets which have become the standard.  By this time, the syntax was starting to look more and more like BASIC and the warning sirens had become full-blown air raid alerts.  My vision blurred, my knees got week and I desperately groped for a fresh slice of pizza.  Any language that reminds me of C++ and VB6 successively within the span of 30 seconds deserves not just scrutiny, but all-out quarantine.

It turns out that somewhere in the _Array_ class, there's a set of declarations that look like this:

```scala
class Array[T](size:Int) {
  def apply(index:Int):T = {
    // return the value from the underlying data structure
  }

  def update(index:Int, value:T):Unit {
    // set the value at the corresponding index
  }

  // ...
}
```

Ouch!  Seems the other shoe just dropped.  It turns out that the _apply_ and _update_ methods are special magic functions enabling the array access and modification syntax.  The same code in C++ might look like this:

```cpp
template <class T>
class Array {
public:
    Array(int length);

    T& operator()(int index);

private:
    // ...
};
```

For those of you who don't know, there's a reason that syntax passed into "bad practice" years ago.  In short: it's cryptic, relies heavily on magic syntax tricks (thus is quite fragile) and by its very nature redefines an operation that is understood to be basic to the language (parentheses).  It's the absolute worse that operator overloading has to offer.

But the fun doesn't stop there!  Remember I said the (sort of) array factory constructor would make more sense after analyzing arrays a bit more?  You probably know where I'm going with this...

```scala
object Array {
  def apply[T](values:*T) = values
}

class Array[T](length:Int) {
  // ...
}
```

Try not to think about it too much, it'll make your brain hurt.  Scala doesn't really have a static scope, so any class that needs to do things statically (like factories) has to use the singleton construct ( _object_ ).  Extend this concept just a bit and you can see how classes which must be _both_ instance _and_ static can run into some trouble.  Thus, Scala allows the definition of both the singleton and the class form of the same type.  In Java, the same concept might look like this:

```java
public class Array<T> {
    public Array(int length) {
        // ...
    }

    // ...

    public static <V> Array<V> createInstance(V... values) {
        // ...
    }
}
```

Obviously the correspondence isn't exact since Java array syntax can't be replicated by mere mortals, but you get the gist of it.  It seems this is one time when Scala syntax _isn't_ more concise or more intuitive than Java.

So what does this all mean for our array factory construct?  It means we're not actually looking at a method, at least, not a method named "Array" as we might reasonably expect.  The array factory construct can be written equivalently in this fashion:

```scala
var names = Array.apply("daniel", "chris", "joseph", "renee", "bethany", "grace")
```

Are you starting to see why overloading the parentheses operator is considered bad form in civilized lands?  The syntax may look nice, properly used, but it's a major pain in the behind if you have to read someone else's badly-designed code to understand what's going on if you can't rely on parentheses to be parentheses.

Things keep going from bad to worse when you consider all the trouble you can get into with method declarations.  Try this one on for size:

```scala
class Pawn {
  def getSymbol = "P"
}
```

Famously concise and readable...except it's not going to do what you probably expect it to.  To use this class, you might write something like this:

```scala
var pawn = new Pawn
println(pawn.getSymbol())
```

The above code will fail with a compiler error on line 2, saying that we passed the wrong number of arguments to the _getSymbol_ method.  Here's a heads-up to those of you designing the Scala syntax: this confuses the hell out of any newbie trying the language for the first time.  It turns out that declaring a method without parentheses means that it can never be invoked _with_ those parentheses.  Changing the invocation to the following alleviates the error:

```scala
println(pawn.getSymbol)
```

And just when you're beginning to make sense of all this, you remember that declaring a method _with_ parentheses means that it can be used both ways!  So the following code is all valid:

```scala
class Pawn {
  def getSymbol() = "P"
}

var pawn = new Pawn

// method 1
println(pawn.getSymbol())

// method 2
println(pawn.getSymbol)

// method 3
println(pawn getSymbol)
```

One final, related tidbit just in case you thought these were isolated examples.  It may come as a shock to those of you coming from C-derivative languages (such as Java), but Scala doesn't support constructor overloading.  Seriously, there's no way to define multiple constructors for a class in Scala.  Of course, like many other things Scala provides a way to _emulate_ the syntax, but it's far from pretty.  Let's suppose you want to be able to construct a _Person_ with either a full name, a first and last name or a first name, last name and age.

```scala
object Person {
  def apply(fullName:String) = {
    var names = fullName.split(' ')
    Person(names(0), names(1))
  }

  def apply(firstName:String, lastName:String) = Person(firstName, lastName, 20)
}

case class Person(firstName:String, lastName:String, age:Int) {
  def getFirstName() = firstName
  def getLastName() = lastName
  def getAge() = age
}

var person1 = Person("Daniel Spiewak")
var person2 = Person("Jonas", "Quinn")
var person3 = Person("Rebecca", "Valentine", 42)
```

_"Where are your Rebel friends now?!"_

Of course it all makes some sense from a theoretical standpoint, but in practice I can't see this being anything but clumsy and annoying (not to mention visually ambiguous.  Try to trace the code that creates _person1_ to see what I mean).

**Update:** As has been pointed out in the comments, Scala does support a primitive sort of constructor overloading (using _def this()_ ) which allows for simple delegate constructors (like I demonstrated above).  However, the fact remains that Scala's constructor overloading is neither as powerful nor as uniformly intuitive as the corresponding feature in Java.

### Conclusion

I really don't mean to be a pain.  After all, someone could write exactly the same sort of rant about any language.  Overall, I still think Scala is a big win over any imperative language I've ever seen.  The point I'm trying to get across is that there's no silver bullet.  No language is perfect (not even Scala).

Scala's at an interesting place right now.  When I work with it and when I read the buzz around the blogosphere, I get the same feeling as I did about Ruby 5 years ago.  Back then Ruby was a virtually unknown Asian language with some interesting dynamic constructs.  Now there's so much hype built up behind the language it could choke a mule.  People are rallying to its cause and breaking out the ACME flame-throwers to deal with any dissidents.  I would hate to see that happen to Scala.

Final word to Java developers everywhere who haven't tried Scala yet: you're missing it!!  Learn Scala.  Try a few applications.  Trust me, you won't regret it.  Just be sure to drink plenty of fluids, I'm sure you'll run into your share of oddball syntax corners.