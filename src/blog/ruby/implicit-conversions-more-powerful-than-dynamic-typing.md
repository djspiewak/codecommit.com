{%
laika.title = "Implicit Conversions: More Powerful than Dynamic Typing?"
laika.metadata.date = "2008-09-15"
%}


# Implicit Conversions: More Powerful than Dynamic Typing?

One of the most surprising things I've ever read about Scala came in the form of a (mostly positive) review article. This article went to some lengths comparing Scala to Java, JRuby on Groovy, discussing many of its advantages and disadvantages relative to those languages. Everyone seems to be writing articles to this effect these days, so the comparison in and of itself was not surprising. What _was_ interesting was an off-hand comment discussing Scala's "dynamic typing" and how it aids in the development of domain specific languages.

Now this article had just finished a long-winded presentation of type inference and compilation steps, so I'm quite certain that the author was aware of Scala's type system. The more likely target of the "dynamic typing" remark would be Scala's implicit conversions mechanism. I have heard this language feature described many times as being a way of "dynamically" adding members to an existing class. While it would be incorrect to say that this feature constitutes a dynamic type system, it is true that it may be used to satisfy many of the same design patterns. Consider the facetious example of a string "reduction" method, one which produces an acronym based on the upper-case characters within the string:

```scala
val acronym = "Microsoft Certified Systems Engineer".reduce
println(acronym)            // MCSE
```

The immediate problem with this snippet is the fact that string literals are of type `java.lang.String`, a class which comes pre-defined by the language. The only way to ensure that the above syntax works properly is to "add" the `reduce` method to the `String` class separate from its definition. In a language such as Ruby or Groovy which have dynamic type systems, we could simply open the class definition and add a new method at runtime. However, in Scala we have to be a bit more tricky. We can't actually add methods to an existing class, but we can define a new class which contains the desired method. Once we have that, we can define an implicit conversion from our target class to our new class. The Scala compiler sees this and performs the appropriate magic behind the scenes. In code, it looks like this:

```scala
class MyRichString(str: String) {
  def reduce = str.toCharArray.foldLeft("") { (t, c) =>
    t + (if (c.isUpperCase) c.toString else "")
  }
}

implicit def str2MyRichString(str: String) = new MyRichString(str)
```

This contrasts quite dramatically with the Ruby implementation of the same concept via open classes (somewhat less-graciously known as "Monkey Patching"):

```ruby
class String
  def reduce
    arr = unpack('c*').select { |c| (65..90).include? c }
    arr.pack 'c*'
  end
end

puts 'HyperText Transfer Protocol'.reduce       # HTTP
```

No visible type conversion is taking place here, all we did is add a method to an existing class and trust that the runtime can figure out the rest. Indeed, for this application, we don't really need anything else. However, as anyone with experience implementing internal domain-specific languages will tell you, seldom is life as simple as adding a few methods to an existing class. Consider a more complicated scenario where we need to _overload_ the `<` operator on integers to operate on `String` values, returning `true` if the length of the string is less than the integer value, otherwise `false`. In Scala, we would once again make use of the implicit conversion mechanism, this time with an even more concise syntax:

```scala
implicit def lessThanOverload(i: Int) = new {
  def <(str: String) = str.length < i
}
```

In fact, we don't even need to go this far. It is possible to create an implicit conversion from `String` to `Int` defined on the length of the `String`. This would allow _existing_ method implementations within the `Int` class to operate upon `String` values:

```scala
implicit def str2Int(str: String) = str.length
```

As a matter of interest, this particular situation can be managed by one of the most convoluted and verbose languages on the market, C++:

```cpp
bool operator<(const int &i, const std::string &str)
{
    return str.length() < i;
}
```

Despite the seemingly-dynamic nature of the problem, the statically typed language camp seems well represented in terms of solutions. Ironically, this sort of problem is one which will be exceedingly difficult to solve in a language like Ruby. This is primarily because method overloading is an innately _static_ device. That's not to say that overloading is impossible in a dynamically typed language (Groovy), but it's not easy. To see why, let's consider the most natural implementation of our operator problem in Ruby:

```ruby
class Fixnum
  def <(str)
    str.size < self
  end
end
```

Intuitively, this may seem like the right way to approach the problem, but the results of such an implementation would be disastrous. At the very least, the first time anyone attempted to perform a `<` comparison targeting an integer, the interpreter will overflow the call stack. In fact, any time _any_ code uses the less-than operator on an instance of `Fixnum`, the interpreter will crash. The reason for this is the invocation of `<` upon `str.size` within our "overloaded" definition. This call creates a very tight recursive loop which will very quickly eat through all available stack frames. We can avoid this problem by reversing the comparison like so:

```ruby
class Fixnum
  def <(str)
    self >= str.size
  end
end
```

Now we don't have to worry about stack overflow, but in the process we have accidentally redefined integer-to-integer comparison in a very strange way:

```
irb(main):006:0> 123 < 'test'
=> true
irb(main):007:0> 123 < 123
=> true
```

Clearly, more effort is going to be required if we are to put to rest our little dilemma. As it turns out, the final solution is surprisingly ugly and verbose:

```ruby
class Fixnum
  alias_method :__old_less_than__, '<'.to_sym
  def <(target)
    if target.kind_of? String
      __old_less_than__ target.size
    else
      __old_less_than__ target
    end
  end
end
```

Whatever happened to Ruby as a "more elegant" language? The unfortunate truth is that in order to _emulate_ method overloading based on input type, we must hold onto the old method implementation while we implement a type-sensitive facade in its place. The `alias_method` invocation literally copies the old less-than operator implementation and provides us with a way of referencing it within our later redefinition. And what happens if someone _else_ happens to monkey patch `Fixnum` and (for whatever reason) uses the identifier "`__old_less_than__`"? Well, then we have problems. It's like the old days of Lisp macros and endless identifier collisions.

It is true that this was an example specifically contrived to make Ruby look bad. I could have implemented the overload using Groovy's meta-classes and been reasonably certain that everything would work out fine, but that's not the point. The point is that there are a surprising number of situations where static typing serves not only to check for errors but also to allow extension patterns which would be otherwise impossible (or very, very difficult). Dynamic typing isn't the panacea of extensibility that its proponents make it out to be, sometimes it isn't quite up to the task.

In fact (and this is where we come to my Digg-friendly point), I would submit that Scala (and to a lesser extent, C++) have created a mechanism for controlled extensibility which is _more_ powerful than Ruby's open classes design. That's not to say that there aren't situations which are easily solved using open classes and entirely intractable using only implicit conversions, but in my experience these scenarios are very rare. In fact, I believe that it is far more common to run against a problem like my contrived overload which is greatly simplified through the use of static typing.

Ironically enough, some of [Ruby's greatest pundits](<http://weblog.raganwald.com/>) are starting to come around to the belief that a more controlled and well-defined model of class extension is required. [ParseTree](<http://parsetree.rubyforge.org/>) is a Ruby framework which provides mechanisms for dynamically manipulating the AST of an expression prior to evaluation. Conceptually, it is very similar to Lisp's macros and peripherally related to .NET's expression trees (used in LINQ). ParseTree is used by a number of complex Ruby domain-specific languages, including [Ambition](<http://ambition.rubyforge.org/>), a fact which is extremely telling of how great the need is for just such a tool. Having myself attempted a domain-specific language for constructing queries, I can state categorically that to do such a thing solely on the basis of open classes would be nearly impossible. Even if successful, such a framework would be extremely volatile, sensitive to the slightest change in the Ruby core library, either caused by update or by _other_ packages injecting their own meddlesome implementations into runtime classes.

Lex Spoon (co-author of _Programming in Scala_ ) [once said](<http://www.infoq.com/presentations/jaoo-spoon-scala>) that any language which seriously targeted domain-specific languages would have to create some sort of implicit conversion mechanism. At the time, I was skeptical, convinced that Ruby (and similar) would always have the upper-hand in the area of class extension due to their dynamic treatment of modules and classes. However, after some serious dabbling in the field of internal domain-specific languages, I'm beginning to come 'round to his point of view. Implicit conversions are far from a weak imitation of Scala's dynamically typed "betters", they are a powerful and controlled way of extending types far beyond anything which can be easily accomplished through open classes.