---
categories:
- Ruby
- Scala
date: '2008-03-24 01:00:52 '
layout: post
title: JRuby Interop DSL in Scala
wordpress_id: 215
wordpress_path: /ruby/jruby-interop-dsl-in-scala
---

JRuby is an amazing bit of programming.  It has managed to rise from its humble beginnings as a hobby project [on SourceForge](<http://sourceforge.net/projects/jruby>) to the most viable third-party Ruby implementation currently available.  As far as I am aware, JRuby is the only Ruby implementation other than MRI which is capable of running an unmodified Rails application.  But JRuby's innovation is not just limited to a rock-solid Ruby interpreter, it also provides tight integration between Java and Ruby.

There's a lot of material out there on how to replace Java with Ruby "glue code" in your application.  The so-called "[polyglot programming](<http://memeagora.blogspot.com/2006/12/polyglot-programming.html>)" technique states that we should embrace multiplicity of language in our applications.  Java may be very suitable for the core business logic of the application, but for actually driving the frontend UI, we may want to use something more expressive (like Ruby).  JRuby provides some powerful constructs which allow access to Java classes from within any Ruby application.  For example:

```ruby
require 'java'

JFrame = javax.swing.JFrame
JButton = javax.swing.JButton
JLabel = javax.swing.JLabel

BorderLayout = java.awt.BorderLayout

class MainWindow < JFrame
  def initialize
    super 'My Test Window'

    setSize(300, 200)
    setDefaultCloseOperation EXIT_ON_CLOSE
    
    label = JLabel.new('You pushed the button', JLabel::CENTER)
      label.visible = false
    add label

    button = JButton.new 'Push Me'
    button.add_action_listener do
      label.visible = true
    end
    add(button, BorderLayout::SOUTH)
  end
end

window = MainWindow.new
window.visible = true
```

![sshot-1](/assets/images/blog/wp-content/uploads/2008/03/sshot-1.png)  ![sshot-2](/assets/images/blog/wp-content/uploads/2008/03/sshot-2.png) 

Not a terribly complex example, but it illustrates some of the major advantages of JRuby.  Notice how clean and concise this code is.  It wouldn't have been much longer had I done this using Java, but it would certainly have been less readable.  Ruby is absolutely perfect for this sort of use case (driving a UI).

As I said though, there are a myriad of examples showing this sort of thing.  As such, it's not a very interesting topic for a posting.  What the masses have failed to cover, however, is how to accomplish the opposite: calling from Java into Ruby.

Likely the reason this topic has received less attention is because Java is the language will the veritable zoo of libraries and frameworks.  The amount of effort and research that has been put into Java simply dwarfs the comparative immaturity of the Ruby offerings.  Given the disparity, why would you even want to call into Ruby from Java?  This conclusion seems logical until one remembers that almost any application which uses Ruby for the frontend must actually pass flow control to Ruby at some point.  This means calling some sort of Ruby code.

### The Java Way

There is _some_ information [available](<http://wiki.jruby.org/wiki/Java_Integration>) on the JRuby Wiki.  The wiki article really should include the caveat that "some experimentation may be required."  Sufficient information is available, but it is neither intuitive nor convenient.  From Java, the syntax for executing an arbitrary Ruby statement looks like this:

```java
ScriptEngineManager m = new ScriptEngineManager();
ScriptEngine rubyEngine = m.getEngineByName("jruby");
ScriptContext context = engine.getContext();

context.setAttribute("label", new Integer(4), ScriptContext.ENGINE_SCOPE);

try {
    rubyEngine.eval("puts 2 + $label", context);
} catch (ScriptException e) {
    e.printStackTrace();
}
```

It's a typical Java API: over-bloated, over-designed and over-generic.  What would be really nice is to have a syntax for accessing Ruby objects that is as seamless as accessing Java from Ruby.  I want to be able to call Ruby methods and use Ruby classes with the same ease that I can use Java methods and classes.  In short, I want an internal DSL for Ruby.

Unfortunately, Java is a bit constrained in this regard.  Java's syntax is extremely rigid and does not lend itself well to DSL construction.  It's certainly [possible](<http://dschneller.blogspot.com/2007/08/building-xml-groovy-way-in-java.html>), but the result is usually less than satisfactory.  We could certainly construct an API around the the [Java Scripting API](<https://scripting.dev.java.net>) (JSR-233) which provides more high-level access (such as direct method calls and object wrappers), but it would be clunky and only a marginal improvement over the original.

The good news is there's another language tightly integrated with Java that has a far more flexible syntax.  Rather than building our JSR-233 wrapper in Java, we can avail ourselves of Scala's power and flexibility, hopefully arriving at a DSL which approaches native "feel" in its syntax.

### The Scala Way

Since we're attempting to construct a tightly-integrated API for language calls, the most effective route would be to apply techniques [already discussed](<http://www.codecommit.com/blog/ruby/xmlbuilder-a-ruby-dsl-case-study>) in the context of DSL design.  As always, we start with the syntax and allow it to drive the implementation:

```scala
// syntax.scala
import com.codecommit.scalaruby._

object Main extends Application with JRuby {
  require("test")
  
  associate('Person)(new Person(""))
  
  println("Received from multiply: " + 'multiply(123, 23))
  println("Functional test: " + funcTest('test_string))
  
  val me = new Person("Daniel Spiewak")
  println("Name1: " + me.name)
  println("Name2: " + (me->'name)())
  
  me.name = "Daniel"
  println("New Name: " + me.name)
  
  println("Person#toString(): " + me)
  
  val otherPerson = 'create_person().asInstanceOf[AnyRef]
  println("create_person type: " + otherPerson.getClass())
  println("create_person value: " + otherPerson.send[String]("name")())
  
  eval("puts 'Ruby integration is amazing'")
  
  def funcTest(fun:(Any*)=>String) = fun()
}

class Person(name:String) extends RubyClass('Person, name) {
  def name = send[String]("name")()
  def name_=(n:String) = 'name = n
}
```

And the associated Ruby code:

```ruby
# test.rb
class Person
  attr_reader :name
  attr_writer :name
  
  def initialize(name)
    @name = name
  end
  
  def to_s
    "Person: {name=#{name}}"
  end
end

def test_string
  'Daniel Spiewak'
end

def multiply(a, b)
  a * b
end

def create_person
  Person.new 'Test Person'
end
```

Obviously we're going to need some heavy implicit type conversions.  The important thing to note is that we don't see any residue of the Java Scripting API, it's all been encapsulated by our DSL.  We've taken an API which is oriented around single-call, low-level invocations and created a high-level wrapper framework which allows method calls, instantiation and even some form of type-checking.

Starting from the top, we see a call which should be familiar to Rubyists, the `require` statement.  In our framework, this method call is just a bit of syntactic sugar around a call to `eval(String)`.  This semantics are basically the same as within Ruby directly, with the exception of how Ruby source files are resolved.  Any script file on the CLASSPATH is fair game, in addition to the normal Ruby locations.  This allows us to easily embed Ruby scripts within application JARs, libraries and other Java distributables.

Moving down a bit further, we find a somewhat mysterious call to the [curried](<http://www.codecommit.com/blog/scala/function-currying-in-scala>) `associate(Symbol)(RubyObject)` method.  The purpose of this invocation will become more apparent later on.  Suffice it to say that this step is necessary to allow Scala class wrappers around existing Ruby classes.

On the next line of interest, we see for the first time how the framework allows for seamless Ruby method invocation.  Unlike Ruby, Scala doesn't allow us to simply handle calls to non-existent methods.  Because of this limitation, we have to be a bit more clever in how we structure the syntax.  In this case, we use [Scala symbols](<http://alblue.blogspot.com/2007/12/scala-introduction-to-scala-case.html>) to represent the method.  There doesn't seem to be a terribly good explanation of symbols in Scala, but there's [plenty of information](<http://glu.ttono.us/articles/2005/08/19/understanding-ruby-symbols>) regarding how they work in Ruby.  Since the concepts are virtually identical, techniques are cross-applicable.

The key to the whole "symbols as methods" idea is implicit type conversion.  The `JRuby` trait inherits a set of conversions which look something like this:

```scala
implicit def sym2Method[R](sym:Symbol):(Any*)=>R = send[R](sym2string(sym))
implicit def sym2MethodAssign[R](sym:Symbol) = new SpecialMethodAssign[R](sym)

private[scalaruby] class SpecialMethodAssign[R](sym:Symbol) {
  def intern_=(param:Any) = new RubyMethod[R](str2sym(sym2string(sym) + "="))(param)
}
```

Though we haven't looked at it yet, it is possible to infer the purpose of the `send(String)` method.  It's function is to prepare a call to a Ruby method without actually invoking it.  This distinction allows us to pass Ruby methods around as method parameters, just like standard Scala methods.  The method returned is actually an instance of class `RubyMethod[R]` (where `R` is the return type).  Scala allows classes to extend structural types like methods, allowing us to redefine the method invocation semantics for wrapped Ruby calls.

```scala
class RubyMethod[R](method:Symbol) extends ((Any*)=>R) {
  import JRuby.engine
  
  override def apply(params:Any*) = call(params.toArray)
  
  private[scalaruby] def call(params:Array[Any]):R = {
    val context = engine.getContext()
    val plist = new Array[String](params.length)
    
    for (i <- 0 until params.length) {
      plist(i) = "res" + i
      context.setAttribute(plist(i), JRuby.resolveValue(params(i)), ScriptContext.ENGINE_SCOPE)
      plist(i) = "$" + plist(i)
    }
    
    evaluate(() => if (plist.length > 0) {
      sym2string(method) + "(" + plist.reduceLeft[String](_ + ", " + _) + ")"
    } else {
      sym2string(method) + "()"
    })
  }
  
  protected def evaluate(invoke:()=>String):R = {
    val toRun = invoke()
    Logger.getLogger("com.codecommit.scalaruby").info(toRun)
    
    JRuby.handleExcept(JRuby.wrapValue[R](engine, engine.eval(toRun, engine.getContext())))
  }
}
```

The gist of this code is simply to assign every parameter value to an attribute in the Ruby runtime.  Attributes of `ENGINE_SCOPE` (as defined by JSR-233) are represented as global variables within Ruby.  These variables are named sequentially starting from zero.  (e.g. `$res0`, `$res1`, ...)  As you can imagine, this technique tends to be a bit of a concurrency killer.  To keep things simple, I decided to completely ignore the issues associated with asynchronous execution.  It is certainly possible to adapt the framework to function in a multi-threaded environment, but I didn't bother to do it.  (one of the perks of blogging is a license to extreme laziness)

Once these parameters are assigned, the method call is evaluated within the context of the runtime.  This is done by literally generating the corresponding Ruby code (done in the anonymous method) and then wrapping the return value in an instance of `RubyObject` (if necessary).  Note that the `send(String)` method does not actually kick-start this invocation process at all.  Rather, it creates an instance of `RubyMethod[R]` which corresponds to the method name.  This class extends `(Any*)=>R`, so it may be used in the normal "method fashion" - by appending parentheses which enclose parameters (if any).

### Supporting Cast

At this point, it's worth taking a moment to examine the specifics of the framework class hierarchy.  A number of classes exist to wrap around Ruby objects and methods.  We've already seen a few of them (`RubyMethod[R]` and `RubyObject`), but it's worth going into more detail as to their purpose and relation to one another. 

Note that these class names often conflict with existing classes in the JRuby implementation.  This odd coincidence is precipitated by the fact that the framework seems to deal with a lot of the same concepts as the JRuby runtime (go figure).  Rather than obfuscating my class naming to avoid conflict, I just assume that you will either make use of the enhanced Scala import feature (as I have in the implementation), or just avoid using the JRuby internal classes.

![image](/assets/images/blog/wp-content/uploads/2008/03/image7.png)

  * **RubyObject -** The root of the object hierarchy.  This abstract class is designed to encapsulate the core functionality of the generic object (roughly: `send`, `->` and `eval`) as well as containing all of the implicit type conversions.  Most of the syntax-defining magic happens here (more on this later). 
  * **JRuby -** This is the primary type interface between the developer and the framework.  Classes which wish to make use of Ruby integration must inherit from this trait.  This is where the `Logger` (for executed statements) is initialized and deactivated.  Within the corresponding `object`, all of the backend resources are managed.  This is where the actual `ScriptEngineManager` instance lives, as well as a set of utility methods to handle wrapping and unwrapping of framework-specific objects. 
  * **RubyWrapperObject -** This implementation of `RubyObject` is designed to wrap around instances which already exist within the Ruby interpreter.  For example, if a Ruby method returns an instance of `ActiveRecord::Base`, it will be represented in Scala by a corresponding instance of `RubyWrapperObject`.  Note that objects which are equivalent in the Ruby interpreter are not guaranteed to be _pointer-equivalent._   However, the `equals(Object)` method is well-defined within `RubyObject`, thus comparisons between `RubyObject` instances will return sane results.  The `==` method in Scala is defined in terms of `equals(Object)`, so existing code will behave rationally. 
  * **RubyClass -** With the exception of the `JRuby` trait, this is likely the only class within the framework which the developer will have to reference explicitly.  This class allows developer-defined Scala classes to wrap around existing Ruby classes, providing type-safe method calls and even extended functionality.  More on this feature later. 
  * **RubyMethod -** We've already seen how this serves as a wrapper around calls to Ruby methods.  However, its default implementation assumes that the method is defined in the global namespace.  This is impractical for many method calls (such as dispatch on an object). 
  * **RubyInstanceMethod -** This class solves the problem of object dispatch with `RubyMethod`.  All of the core functionality is identical to its superclass with the exception of the generated Ruby code.  Instead of just generating a method call passing parameters, this class will generate a method call on a given Ruby object.  Thus, this class depends upon `RubyWrapperObject` which maintains a reference to a corresponding Ruby instance.

### Alternative Dispatch

Not every method call is made on the enclosing scope.  Sometimes it is necessary to call a method in an object to which you have a reference.  For example, a method may return an instance of a some Ruby class.  This instance will be automatically wrapped by a Scala instance of `RubyWrappedObject`.  Since this Scala class doesn't actually define any methods which correspond to the Ruby class, it is necessary to once more utilize the "symbols as methods" trick in method dispatch.  There are two ways to call a method on an object like this: the `send[R](String)` method (where `R` is the return type), and the `->` (arrow) operator.

Using the arrow operator is a lot like normal method calls, except with symbols instead of method names.  Just like dispatch on the enclosing scope, the call is converted into an instance of `RubyMethod` (actually, an instance of `RubyInstanceMethod`) which can then be used as a standard Scala method.  The difference between using arrow and dispatching on the enclosing scope is the syntax must be a little more contrived.

Parentheses have the second-highest priority of all the Scala operators (the dot operator (.) has the highest).  This means that if we simply "follow our nose" where the syntax is concerned, we will arrive at an order of invocation which leads to an undesirable result.  Consider the following sample:

```scala
val obj = 'create_person()
obj->'name()
```

The first call is a standard dispatch on the enclosing scope.  The second call is what is interesting to us.  Reading this line naturally (at least to old C/C++ programmers) we would arrive at the following sequence of events:

  1. Get a reference to the `name` method from the instance contained within `obj`
  2. Invoke the method, passing no parameters

Unfortunately, this is not how the compiler sees things.  Because parentheses bind tighter than the arrow operator, it actually resolves the expression in the following way:

  1. Get a reference to the `name` method contained within _the enclosing scope_
  2. Invoke the method, passing no parameters 
  3. Invoke the `->` method on the instance within `obj`, passing the result of `name` as a parameter

This is obviously not what we wanted.  Unfortunately, there's no way to make the arrow operator bind tighter than parentheses.  This is a good thing from a language standpoint, but it causes problems for our syntax.

The solution is to enclose any "arrow dispatch" statement within parentheses so as to force the order of evaluation:

```scala
val obj = 'create_person()
(obj->'name)()
```

It looks a bit weird, but it's the only way Scala will allow this to work.  This call now evaluates properly, calling the `name` method on the `obj` instance, passing no parameters.

There's actually another problem associated with arrow dispatch in our DSL: Scala already has an implicit meaning for the arrow operator.  The following sample should look familiar to those of you who have worked with Scala in other applications:

```scala
val numbers = Map(1 -> "one", 2 -> "two", 3 -> "three")
```

By default, Scala defines the arrow operator as an alternative syntax for defining [2-tuples](<http://www.codecommit.com/blog/scala/scala-for-java-refugees-part-6>).  This is good for most things, but bad for us.  What we want is to define a new implicit type conversion which converts `Any` into a corresponding instance of `RubyWrappedObject`.  This would allow us to satisfy the syntax given above.  However, Scala's 2-tuple syntax already defines an implicit type conversion for the `Any` type which deals with the arrow operator.  Rather than examining the context to attempt to disambiguate, the Scala compiler simply gives up and prints an error stating that the implicit type conversions are ambiguous.  This poses a bit of a problem and nearly killed the arrow operator idea in design.

The solution is actually to override Scala's built-in conversion by defining our own conversion _with the same name and signature_ but which provides us with the option of using our own arrow operator definition.  The behavior we want is to allow normal use of the arrow operator when dealing with `Any -> Any`, but convert to `RubyWrappedObject` and dispatch when dealing with `Any -> Symbol`.  After a little digging through the Scala standard library, I arrived at the following solution (defined in `RubyObject`):

```scala
implicit def any2ArrowAssoc[A](a:A) = new SpecialArrowAssoc(a)

private[scalaruby] class SpecialArrowAssoc[A](a:A) extends ArrowAssoc(a) {
  def ->(sym:Symbol) = (a match {
    case obj:RubyObject => obj
    case other => new RubyWrapperObject(other)
  })->sym
}
```

Notice that we extend Scala's pre-existing `ArrowAssoc[A]` class (which handles the special 2-tuple syntax) and then overload the `->` method to work differently with symbols.  This code now does precisely what we need.  By introducing this extra layer of indirection, as well as by overriding Scala's existing conversion, we're able to support the arrow syntax as shown in the above examples.

#### Sending Messages

There is one final form of dispatch which allows typed return values: `send[R](String)`.  This is actually the method to which all the other dispatch forms delegate (as it is the most general).  This method is very similar to the Ruby `send` method which allows Smalltalk-style message passing on arbitrary objects.  The really important thing about this method though is that it will automatically cast the return value from the method to whatever type you specify, allowing you to define type-safe wrappers around existing Ruby methods in Scala:

```scala
def multiply(a:Int, b:Int) = send[Int]("multiply")(a, b)

val result:Int = multiply(123, 23)
```

`send` is effectively defined as a curried function since it takes a method name as a parameter and returns an instance of `RubyMethod` as a result.  This mimics the behavior of dispatch with symbol literals in that you can use `send` to generate type-safe partially-applied functions for corresponding Ruby methods.

Note that `send` could just as easily have taken a symbol as a parameter, rather than a string.  However, the metaphor throughout the DSL is "symbols as methods", thus string was used to avoid logical conflict.  Scala itself was perfectly happy passing symbol literals around in addition to treating them as methods.

### Class Wrapping

The final bit of code in the example now so far above us serves as a sample of how one might wrap an existing Ruby class within Scala.  `Person` is actually a class defined in Ruby (as you can see from the Ruby sources).  It has a read/write attribute, `name`, as well as an overridden `to_s` method.  `RubyObject` already contains the logic for handling calls to `toString()` and proxying them to Ruby's `to_s`, but the `name` attribute must be handled explicitly in code.

The goal is basically to provide a type-safe wrapper around the `Person` Ruby class.  We could just as easily dispatch on the automatically wrapped instance of `RubyWrappedObject` using either syntax described above, but an explicit wrapper is a bit nicer.  The compiler can check things for us, and we can even add methods to the class (at least, as far as Scala is concerned) in true Ruby "open class" style.  All that is necessary to accomplish this wrapper is to extend `RubyClass` and to define the delegating wrapper methods:

```scala
class Person(name:String) extends RubyClass('Person, name) {
  def name = send[String]("name")()
  def name_=(n:String) = 'name = n
}
```

We specify which Ruby class we are wrapping as the first parameter in the constructor for `RubyClass`.  The parameters which follow are passed directly to the constructor of the corresponding Ruby class.  This Ruby constructor is invoked automatically, instantiating the corresponding wrapped Ruby object in the background.  Notice that we specify the name of the Ruby class using a symbol.  This is the one place in the framework that we break with the "symbols as methods" metaphor.  The consequence is a nice, clean syntax for Ruby class wrapping.  Unfortunately, it also means that wrapping a class within a non-included namespace (e.g. `ActiveRecord::Base`) can be a little clunky.  The only way to do it is to explicitly invoke the `Symbol(String)` constructor.  (this is required because Scala symbols can only contain alpha-numerics and underscores)

Once we have our wrapped class signature, it's easy to define the delegate methods.  Scala encourages a blurring of field and method, similar to Ruby.  As such, it supports a very Ruby-esque syntax for accessor/mutator pairs.  This makes the wrapped syntax just a bit nicer.  For the accessor, we make a call to the `send` method, specifying the return type necessary for the wrapper.  The mutator allows us to be a bit more creative.

We don't really need type-safe return values for a mutator.  We would normally just set the return type as `Unit` and ignore the result.  Thus we can once again use the symbol dispatch syntax.  Notice that this time we're not directly treating a symbol as a method.  We're apparently _assigning_ a value to the symbol using the `=` operator (corresponds to the `operator=` assignment operator in C++).  This is possible through a separate implicit type conversion which generates a one-off utility instance:

```scala
private[scalaruby] class SpecialMethodAssign[R](sym:Symbol) {
  def intern_=(param:Any) = new RubyMethod[R](str2sym(sym2string(sym) + "="))(param)
}
```

As you can see, all this method does is generate a new symbol which includes the '=' character and returns the result of dispatching on the corresponding Ruby method.  Note that mutators in Ruby are defined as " _=_ ", thus appending "=" to the method name is the appropriate behavior.

#### Return Value Wrapping

There's actually a slight problem involved in allowing Scala wrappers around existing Ruby types.  Well, not so much a problem as an inconsistency.  The problem is simply this: if a Ruby method creates an instance of a Ruby class for which there is a Scala wrapper and returns this value through the framework into Scala, one would expect this value would be wrapped into an instance of the Scala wrapper.  If you look in the example far above, there is an example of this in the `create_person` method.  The method creates an instance of Ruby class `Person` and returns it as a result.

Somehow, the framework must identify that there is a corresponding Scala wrapper and then properly create an instance.  This actually poses something of a dilemma in two ways.  Number one, Scala has no equivalent to Ruby's `ObjectSpace`, so there's no way to get a comprehensive list of all classes which have been defined.  Even if we could get this list, the corresponding Ruby class is specified in the constructor parameters to `RubyClass`, so there's no way to obtain the information statically from outside the class.  Number two, we have to somehow create an instance of the Scala wrapper class _without_ creating a corresponding instance of the wrapped Ruby class (since we already have one).  This means we need some sort of override in the `RubyClass` constructor.

The best solution to all of these problems is to introduce the `associate` method.  The usage is demonstrated at the top of the example where we associate the `Person` Ruby class with the `Person` Scala wrapper class.  More specifically, we associate the Ruby class with a pass-by-name parameter which defines how to instantiate the Scala class.  This is an important distinction as it solves our second problem of instance creation.  The framework has no way of knowing what parameters must be passed to the Scala wrapper constructor, so the instantiation itself must be passed:

```scala
associate('Person)(new Person(""))
```

As I mentioned previously, this is a pass-by-name parameter which means that it will not be immediately evaluated, but rather on-demand somewhere in the body of `associate`.  The `associate` method actually takes this value and wraps it in an anonymous method which invokes the instantiation each time a value of Ruby type `Person` must be wrapped.  Just prior to invoking the constructor, an override is put in place within the `RubyClass` singleton object (not shown in the class hierarchy) to prevent the creation of a corresponding Ruby instance.  This is what allows the new instance of Scala class `Person` to correspond with an existing Ruby value.  Here again we're sacrificing concurrency for a hacky work-around to a complex problem.  Any sort of "proper" implementation would have to solve this problem in a more elegant way.

### It Never Ends!

This post, that is.  There's so much more I could ramble on about (I never even talked about how exceptions are handled), but this entry is already far too long.  Hopefully the material presented here only serves to whet your appetite for slicker JRuby-Scala integration and all the benefits it can bring.  I've packaged up the framework presented here as a downloadable archive.  The package includes the Ruby engine for the Java Scripting API as well as a `jar-complete` build made from the JRuby SVN.  The project _may_ work with JRuby 1.0, but I doubt it.  Anyway, JRuby 1.1 is due shortly, so why bother.  Remember that this is extremely untested and very experimental.  (I did warn you about the concurrency issues, right?)  If this is interesting to people, I may do a proper release into an OSS project somewhere.  For right now, I just don't have the time.  :-(

I hope this entry gives you an idea of what's involved in Scala DSL implementation, as well as an idea of where such a technique may be useful in your own projects.  After all, what would be better than _everyone_ being able to write their own Rails-killer and define highly fluid APIs!

  * Download [unnamed-scala-ruby.zip](<http://www.codecommit.com/blog/misc/unnamed-scala-ruby.zip>) (requires Java 6 and Scala 2.7.0)