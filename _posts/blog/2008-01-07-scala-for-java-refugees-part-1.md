---
categories:
- Scala
date: '2008-01-07 01:00:57 '
layout: post
title: 'Scala for Java Refugees Part 1: main(String[])'
wordpress_id: 175
wordpress_path: /scala/scala-for-java-refugees-part-1
---

_This article is also available in[Spanish](<http://blog.santiagobasulto.com.ar/2011/11/22/scala-para-refugiados-de-java-parte-1/>) and [Bulgarian](<http://www.fatcow.com/edu/codecommit-java-bl/>)._

You know who you are.  You're the developer who picked up Java years ago, maybe as a second language and better alternative to C++, maybe as your first language coming into the industry.  You're comfortable with Java, you know its ins and outs, its moods.  It's like an old girlfriend; you may not feel the vibe anymore, but you know just how to rub it so it smiles.  In short, you're a craftsman, and Java is your workhorse tool.

You're starting to to become a bit pragmatic about your language choice though.  To put it mildly, the Java honeymoon is over.  While you can hardly find enough fault with the language to just walk away, you're certainly open-minded enough to consider alternatives.  You've heard of this new-fangled thing called Ruby - how could you _not_ have heard, given the sheer noise level produced by its followers.  You're impressed by the succinctness of its constructs and the power of its syntax, but you're not sold on the idea of using a scripting language to build your enterprise app.  Dynamic typing and TextMate are all well and good, but for the real-world iron horse you're going to need something with a bit more backbone.  As a good pragmatist, you stick with the tool that works: Java.

The good news is that there's light at the end of the tunnel.  There's a new language on the scene that's taking the developer world by storm.  Scala seems to offer everything you've been looking for in a language: static typing, compiled to bytecode (so you can run it on all those ancient Java-capable servers), a succinct and expressive syntax.  You've seen a few examples which have really caught your eye.  It looks the spitting image of Java, except with half the useless constructs thrown out.  No semi-colons, no _public static void_ method qualifiers; it even seems to have some sort of static type inference mechanism.

The only problem you have now is figuring out where to start.  You've tried looking on the Scala website, but what you found stopped you in your tracks.  Everything's so...functional.  Lamdas, high-order functions, immutable state, recursion out the wazoo.  Suddenly things are looking less promising.

Have no fear, ye refugee of Java EE grid iron, all is not lost.  True, Scala is a functional language, but it's also imperative and highly object oriented.  What does this mean?  It means that you don't _have_ to write code with the sole purpose of pleasing [Haskell Curry](<http://en.wikipedia.org/wiki/Haskell_Curry>).  You can write code that you can actually read a week from now.  You may even be able to show this code to your Java-loving coworkers and they just might understand it.  You don't _have_ to curry every function and avoid loops at all costs.  You _can_ write your Java applications in Scala.  You just need the right introduction.

### Introductions

If you're like me and can identify with the above, then this series is for you.  I've read a lot of articles and tutorials on Scala ([Alex Blewitt's series](<http://alblue.blogspot.com/2007/10/scala-introduction-to-scala-interpreter.html>) is highly recommended, especially if you're interested in the more functional side of life), but few of these tutorials have even attempted to make things easier for the run-of-the-mill Java developer to make the transition.  I personally have very little FP (Functional Programming) experience, so I don't think I could write an article about porting Scheme code to Scala even if I wanted to.  Instead, this series will focus on how Scala is just like Java, except better.

Did I mention [Alex's Scala introduction series](<http://alblue.blogspot.com/2007/10/scala-introduction-to-scala-interpreter.html>)?  Seriously, this is great reading, and not a bad introduction to Scala in and of itself.  Once you're done reading my ramblings, you should run over and read some of his more coherent stuff.   The more the merrier!

### Getting Started

```scala object HelloWorld extends Application { println("Hello, World!") } ``` 

Nothing like getting things rolling with a little code.  Notice the refreshing lack of mandatory semicolons.  We _can_ use them anyway, but they aren't required unless we need multiple statements on a single line.  This code sample does exactly what it looks like, it defines an application which (when run using the _scala_ interpreter) will print "Hello, World!" to stdout.  If you put this code into a file with a _.scala_ extension, you can then compile it using the _scalac_ compiler.  The result will be a single _.class_ file.  You could technically run the _.class_ using the _java_ interpreter, but you would have to mess with the classpath a bit.  The easiest thing to do is just use the _scala_ command like so:

```bash scalac hello.scala scala HelloWorld ``` 

Notice the name of the file in question?  Unlike Java, Scala doesn't force you to define all public classes individually in files of the same name.  Scala actually lets you define as many classes as you want per file (think C++ or Ruby).  It's still good practice to follow the Java naming convention though, so being good programmers we'll save our _HelloWorld_ example in a file called "HelloWorld.scala".

#### Editors

Just a brief note on your first few moments with Scala: using the right editor is key.  As you must have learned from your many years in the Java world, IDEs are your friend.  Scala, being a much younger language doesn't have very good IDE support yet.  It is a static, general purpose language like Java, so IDE support will be forthcoming.  For the moment however, you're stuck with a very limited set of options.

  * Eclipse (using one of two shoddy and unstable Scala plugins) 
  * Emacs 
  * IntelliJ (basically just syntax highlighting support) 
  * TextMate 
  * VIM 
  * jEdit 

There are a few other options available, but these are the biggies (to see a full list, look in the misc/scala-tool-support/ directory under the Scala installation root).  My personal recommendation is that you use jEdit or TextMate, though if you're feeling adventurous you're free to try one of the Eclipse plugins too.  Scala support in Eclipse has the advantage (at least with the [beta plugin](<http://osdir.com/ml/lang.scala/2006-02/msg00002.html>)) of features like semantic highlighting, code completion (of both Scala and imported Java classes) and other IDE-like features.  In my experience though, both Eclipse plugins were unstable to the point of unusable and as such, not worth the trouble.  Scala is a much cleaner language than Java, so it really does have less of a need for a super powerful IDE.  It would be nice, no question about that, but [not essential](<http://www.javaworld.com/javaworld/jw-12-2007/jw-12-outside-the-ide.html>).

### More Hello World

```scala object HelloWorld2 { def main(args:Array[String]) = { var greeting = "" for (i <\- 0 until args.length) { greeting += (args(i) + " ") } if (args.length > 0) greeting = greeting.substring(0, greeting.length - 1) println(greeting) } } ``` 

Save this in a new file (we'll call it "HelloWorld2.scala"), compile and run using the following commands:

```bash scalac HelloWorld2.scala scala HelloWorld2 Hello, World! ``` 

Once again, this prints "Hello, World!" to stdout.  This time we did things a little differently though.  Now we've got command line arguments coming into our app.  We define a variable of type _String_ , iterate over an array and then call a bit of string manipulation.  Fairly straightforward, but definitely more complex than the first example.  ( **Note:** Scala mavens will no doubt suggest the use of _Array#deepMkString(String)_ (similar to Ruby's _Array::join_ method) instead of iterating over the array.  This is the correct approach, but I wanted to illustrate a bit more of the language than just an obscure API feature).

The first thing to notice about this example is that we actually define a main method.  In the first example, we just extended _Application_ and declared everything in the default constructor.  This is nice and succinct, but it has two problems.  First, we can't parse command line args that way.  Second, such examples are extremely confusing to Scala newbies since it looks more than slightly magical.  Don't worry, I'll explain the magic behind our first example in time, but for now just take it on faith.

In the example, we define a main method that looks something like our old Java friend, _public static void main._   In fact, this is almost exactly the Scala analog of just that method signature in Java.  With this in mind, an experienced developer will be able to pick out a few things about the language just by inspection. 

First off, it looks like all methods are implicitly public.  This is somewhat correct.  Scala methods are public _by default_ , which means there's no _public_ method modifier ( _private_ and _protected_ are both defined).  It also looks like Scala methods are static by default.  This however, is not entirely correct.

Scala doesn't really have statics.  The sooner you recognize that, the easier the language will be for you.  Instead, it has a special syntax which allows you to easily define and use singleton classes (that's what _object_ means).  What we've really declared is a singleton class with an instance method, _main._   I'll cover this in more detail later, but for now just think of _object_ as a class with only static members.

Upon deeper inspection of our sample, we gain a bit of insight into both Scala array syntax, as well as an idea of how one can explicitly specify variable types.  Specifically, let's focus on the method declaration line.

```scala def main(args:Array[String]) = { ``` 

In this case, _args_ is a method parameter of type _Array[String]._   That is to say, _args_ is a string array.  In Scala, _Array_ is actually a class (a real class, not like Java arrays) that takes a type parameter defining the type of its elements.  The equivalent Java syntax (assuming Java had an _Array_ class) would be something like this:

```java public static void main(Array args) { ``` 

In Scala, variable type is specified using the _variable:Type_ syntax.  Thus, if I wanted to declare a variable that was explicitly of type _Int,_ it would be done like this:

```scala var myInteger:Int ``` 

If you look at the sample, we actually do declare a variable of type _String_.  However, we don't explicitly specify any type.  This is because we're taking advantage of Scala's type inference mechanism.  These two statements are semanticly equivalent:

```scala var greeting = "" ``` 

```scala var greeting:String = "" ``` 

In the first declaration, it's obvious to us that _greeting_ is a _String,_ thus the compiler is able to infer it for us.  Both _greeting_ s are staticly type checked, the second one is just 7 characters shorter.  :-)

Observant coders will also notice that we haven't declared a return type for our _main_ method.  That's because Scala can infer this for us as well.  If we really did want to say something explicitly, we could declare things like this:

```scala def main(args:Array[String]):Unit = { ``` 

Once again, the type antecedes the element, delimited by a colon.  As an aside, _Unit_ is the Scala type for I-really-don't-care-what-I-return situations.  Think of it like Java's _Object_ and _void_ types rolled into one.

### Iterating Over an Array

```scala var greeting = "" for (i <\- 0 until args.length) { greeting += (args(i) + " ") } ``` 

By the way, it's worth noting at this juncture that the convention for Scala indentation is in fact two spaces, rather than the tabs or the four space convention that's so common in Java.  Indentation isn't significant, so you can really do things however you want, but the other four billion, nine hundred, ninety-nine million, nine hundred, ninety-nine thousand, nine hundred and ninety-nine people in the world use the two space convention, so it's probably worth getting used to it.  The logic behind the convention is that deeply nested structure isn't a bad sign in Scala like it is in Java, thus the indentation can be more subtle.

This sample of code is a bit less intuitively obvious than the ones we've previously examined.  We start out by declaring a variable, _greeting_ of inferred type _String._   No hardship there.  The second line is using the rarely-seen Scala for loop, a little more type inference, method invocation on a "primitive" literal and a _Range_ instance.  Developers with Ruby experience will probably recognize this equivalent syntax:

```ruby for i in 0..(args.size - 1) greeting += args[i] + " " end ``` 

The crux of the Scala for loop is the _Range_ instance created in the _RichInt#until_ method.  We can break this syntax into separate statements like so:

```scala val range = 0.until(args.length) for (i <\- range) { ``` 

Oh, that's not a typo there declaring range using _val_ instead of _var._   Using _val_ , we're declaring a variable _range_ as a constant (in the Java sense, not like C/C++ _const_ ).  Think of it like a shorter form of Java's _final_ modifier.

Scala makes it possible to invoke methods using several different syntaxes.  In this case, we're seeing the _value methodName param_ syntax, which is literally equivalent to the _value.methodName(param)_ syntax.  Another important action which is taking place here is the implicit conversion of an _Int_ literal (0) to an instance of _[scala.runtime.RichInt](<http://www.scala-lang.org/docu/files/api/scala/runtime/RichInt.html>)_.  How this takes place isn't important right now, only that _RichInt_ is actually the class declaring the _until_ method, which returns an instance of _Range._

Once we have our _Range_ instance (regardless of how it is created), we pass the value into the magic _for_ syntax.  In the _for_ loop declaration, we're defining a new variable _i_ of inferred type _Int._   This variable contains the current value as we walk through the range [0, args.length) - including the lower bound but not the upper.

In short, the _for_ loop given is almost, but not quite equivalent to the following bit of Java:

```java for (int i = 0; i < args.length; i++) { ``` 

Obviously the Java syntax is explicitly defining the range tests, rather than using some sort of _Range_ object, but the point remains.  Fortunately, you almost never have to use this loop syntax in Scala, as we'll see in a bit.

The body of the loop is the one final bit of interesting code in our sample.  Obviously we're appending a value to our _greeting_ String.  What I'm sure struck you funny (I know it did me) is the fact that Scala array access is done with parentheses, not square brackets.  I suppose this makes some sense since Scala type parameters are specified using square brackets (rather than greater-than/less-than symbols), but it still looks a little odd.

To summarize, the upper sample in Scala is logically equivalent to the lower sample in Java:

```scala var greeting = "" for (i <\- 0 until args.length) { greeting += (args(i) + " ") } ``` 

```java String greeting = ""; for (int i = 0; i < args.length; i++) { greeting += args[i] + " "; } ``` 

### A Better Way to Iterate

In Java 5, we saw the introduction of the so-called for/each iterator syntax.  Thus, in Java we can do something like this:

```java for (String arg : args) { greeting += arg + " "; } ``` 

Much more concise.  Scala has a similar syntax defined as a high-order function - a function which takes another function as a parameter.  I'll touch on these more later, but for the moment you can take it as more magic fairie dust:

```scala args.foreach { arg => greeting += (arg + " ") } ``` 

Here we see that _foreach_ is a method of class _Array_ that takes a closure (anonymous function) as a parameter.  The _foreach_ method then calls that closure once for each element, passing the element as a parameter to the closure ( _arg_ ).  The _arg_ parameter has an inferred type of _String_ because we're iterating over an array of strings.

Now as we saw earlier, Scala methods can be called in different ways.  In this case we're calling _foreach_ omitting the parentheses for clarity.  We also could have written the sample like this:

```scala args.foreach(arg => { greeting += (arg + " ") }) ``` 

Scala actually defines an even _more_ concise way to define single-line closures.  We can omit the curly-braces altogether by moving the instruction into the method invocation:

```scala args.foreach(arg => greeting += (arg + " ")) ``` 

Not bad!  So our fully rewritten sample looks like this:

```scala object HelloWorld2 { def main(args:Array[String]) = { var greeting = "" args.foreach(arg => greeting += (arg + " ")) if (args.length > 0) greeting = greeting.substring(0, greeting.length - 1) println(greeting) } } ``` 

The syntax looks great, but what is it actually doing?  I don't know about you, but I hate having to use APIs that I don't know how to replicate myself.  With a bit of work, we can recreate the gist of the Scala _foreach_ method in pure Java.  Let's assume for a moment that Java had an _Array_ class.  In that _Array_ class, let's pretend there was a _foreach_ method which took a single instance as a parameter.  Defined in code, it might look like this:

```java public interface Callback { public void operate(T element); } public class Array { // ... public void foreach(Callback callback) { for (T e : contents) { // our data structure is called "contents" callback.operate(e); } } } ``` 

I could have defined _foreach_ recursively (as it is defined in Scala), but remember I'm trying to keep these explanations clear of the tangled morass that is FP.  :-)

Sticking with our goal to see an analog to the Scala _foreach_ , here's how we would use the above API in Java:

```java public class HelloWorld2 { public static void main(Array args) { final StringBuilder greeting = new StringBuilder(); args.foreach(new Callback() { public void operate(String element) { greeting.append(element).append(' '); } }); if (args.length() > 0) { greeting.setLength(greeting.length() - 1); } System.out.println(greeting.toString()); } } ``` 

Starting to see why Scala is legitimately appealing?  If you're like me, you just want Scala to be a more concise Java.  In this case, that's exactly what we've got.  No strange functional cruft, no immutable state.  Just solid, hard-working code.

### A Word About Built-in Types

Because Scala is built on the JVM, it inherits a lot of its core API directly from Java.  This means you can interact with Java APIs.  More than that, it means that _any_ code you write in Scala is using Java APIs and functions.  For example, our sample _HelloWorld2_ is using a string variable _greeting._   This variable is literally of type _java.lang.String_.  When you declare an integer variable (type _Int_ ) the compiler converts this to the Java primitive type _int._

It's also worth noting that there are a number of built-in implicit conversions (just like the _Int_ to _RichInt_ we saw earlier with the _Range_ creation).  For example, Scala's _Array[String]_ will be implicitly converted to a Java _String[]_ when passed to a method which accepts such values.  Even Scala type parameters are available and interoperable with Java generics (in the current development version of Scala and slated for inclusion in 2.6.2).  In short, when you use Scala you're really using Java, just with a different syntax.

### Conclusion

Scala doesn't have to be complex, needlessly academic or require a master's degree in CompSci.  Scala can be the language for the common man, the 9-5 developer who's working on that next enterprise web application.  It has real potential to provide the clear syntax Java never had without forsaking the power and stability of a trusted, first-class language.  Convinced?

Up next, classes, methods and properties: everything you need to know to get rolling with [object-oriented programming in Scala](<http://www.codecommit.com/blog/scala/scala-for-java-refugees-part-2>).