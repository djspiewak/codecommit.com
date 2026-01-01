{%
laika.title = "Is Scala Not \"Functional Enough\"?"
laika.metadata.date = "2008-10-20"
%}


# Is Scala Not "Functional Enough"?

In one of Rich Hickey's [excellent presentations introducing Clojure](<http://clojure.blip.tv/>), he mentions in passing that Scala "isn't really a functional language". He says that Java and Scala are both cut from the same mold, and because Scala doesn't _force_ immutability it really shouldn't qualify. These viewpoint is something I've been hearing a lot of from various sources, people talking about how F# is really the only mainstream functional language, or how once Erlang takes off it will leave Scala in the dust.

When I first heard this sentiment voiced by Rich, I brushed it off as a little odd and only slightly self-serving (after all, if you don't use Scala, there's a better chance you will use Clojure). Rich has his own opinions about a lot of things, but I have found with most that I can still understand his rationale, even if I don't agree. So, realizing that many of his other kooky ideas seemed to have some basis in reality, I decided to come back to his opinion on Scala and give it some deeper consideration.

The core of the argument made by Rich (and others) against Scala as a functional language goes something like this:

  * Mutable variables as first-class citizens of the language 
  * Uncontrolled side-effects (ties in with the first point) 
  * Mutable collections and other imperative libraries exist on equal footing 
  * Object-oriented structures (class inheritance, overloading, etc) 
  * Verbosity 

### Comparative Type Inference

If you're coming from Java-land, the final point may have caught you a bit by surprise. After all, Scala is _vastly_ more concise than Java, so how could anyone possibly claim that it is "too verbose"? Well, to answer that question, you have to compare Scala with the other side of the language jungle: the functional languages. Here's an explicitly-recursive function which sums a list of integers:

```scala
def sum(ls: List[Int]): Int = ls match {
  case hd :: tail => hd + sum(tail)
  case Nil => 0
}
```

That's not too bad. The use of pattern matching eliminates an entire class of runtime errors (selecting a non-existent element) and makes the code a lot cleaner than the equivalent Java. However, compare this with the same function ported directly to SML (a functional language:

```ml
fun sum nil = 0
  | sum (hd :: tail) = hd + sum tail
```

One thing you'll notice here is the complete lack of any type annotations. Like most static functional languages, ML (and derivatives) has a form of type inference called "[Hindley - Milner](<http://en.wikipedia.org/wiki/Type_inference#Hindley.E2.80.93Milner_type_inference_algorithm>)" (sometimes called "global type inference"). Rather than just looking at a single expression to infer a type (like Scala), Hindley - Milner looks at the _entire_ function and derives the most general (least restrictive) type which satisfies all expressions. This means that everything can be statically type-checked with almost no need to declare types explicitly.

"Now, wait!" (you say), "You would never write a function just to sum a list; you should be using a fold." That's true. So let's see how well these two languages do when the problem is solved in a more realistic fashion. Once again, Scala first:

```scala
def sum(ls: List[Int]) = ls.foldLeft(0) { _ + _ }
```

Let's see ML top that!

```ml
fun sum ls = foldl (op+) 0 ls
```

Then again, maybe we'll just quit while we're behind...

The fact is that Scala requires significantly more ceremony to accomplish some things which are _trivial_ in pure-functional languages like ML and Haskell. So while Scala may be a huge step up from Java and C++, it's still a far cry from being the quickest and most readable way of expressing things in a functional style.

One obvious solution to this would be to just add Hindley - Milner type inference to Scala. Well, this may be the "obvious" solution, but it doesn't work. Scala has an extremely powerful and complex type system, one with a number of properties which Hindley - Milner just can't handle. A full object-oriented inheritance hierarchy causes some serious problems with the "most general" inference of Hindley - Milner: just about everything becomes type `Any` (or close to it). Also, method overloading can lead to ambiguities in the inferred types. This is actually a problem even in the venerable Haskell, which imposes hard limitations on what functions can be in scope at any given point in time (so as to avoid two functions with the same name).

Simply put, Scala's design forbids any type inference (that I know of) more sophisticated than local expression-level. Don't get me wrong, it's still better than nothing, but a language with local type inference alone will never be as generally concise as a language with Hindley - Milner.

### Side Effects

One big ticket item in the litany of complaints against Scala is the admission of uncontrolled side effects. It's not hard to find an example which demonstrates this property:

```scala
val name = readLine
println("Hello, " + name)
```

This example alone shows how fundamental side-effects are within the Scala language. All we have done here is made two function calls, one of them passing a `String` and receiving _nothing_ as a result. From a mathematical standpoint, this code snippet is virtually a no-op. However, we all know that the `println` function has an additional side effect which involves sending text to standard out. Coming from Java, this makes perfect sense and it's probably hard to see why this would be considered a problem. However, coming from Haskell, what we just wrote was a complete abomination.

You see, Haskell says that no function should _ever_ have side effects unless they are explicitly declared using a special type constructor. In fact, this is one of the areas where monads have had a huge impact on Haskell's design. Consider the following Haskell equivalent:

```haskell
main :: IO ()
main = do
         name <- getLine
         putStrLn ("Hello, " ++ name)
```

Even if you don't know Haskell, the above should be pretty readable. The first line is the type declaration for the `main` "function" (it's actually a value, but why quibble). Haskell does have Hindley - Milner type inference, but I wanted to be extra-explicit. You'll notice that `main` is not of type `void` or `Unit` or anything similar, it is actually of type `IO` _parameterized_ with Haskell's form of `Unit`: `()`. This is an extremely important point: `IO` is a monad which represents an action with side-effects returning a value which matches its type parameter (in this case, `()`). The little dance we perform using `do`-notation is just a bit of syntax sugar allowing us to compose two other `IO` values together in a specific order. The `getLine` "function" is of type `IO String`, meaning that it somehow reads a `String` value by using side effects (in this case, reading from standard in). Similarly, `putStrLn` is a function of type `String -> IO ()`. This means that it takes a `String` as a parameter and uses it to perform some side effects, from which it obtains no result value. The do-notation takes these two monadic values and _composes_ them together, forming one big value of type `IO ()`.

Now, this may seem horribly over-complicated, especially when compared to the nice clean side effects that we have in Scala, but it's actually quite mathematically elegant. You see, the IO monad is how we represent actions with side effects. In fact, the _only_ (safe) way to have side effects in Haskell is to wrap them up inside monad instantiations like these. Haskell's type system allows you to actually identify and _control_ side effects so that they remain contained within discrete sections of your code base.

This may not sound so compelling, but remember that functional programming is all about eliminating side effects. You _compute_ your result, you don't just accidentally find yourself with a magic value at the end of a long run. The ability to work with side effects as packaged values just like any other constant is extremely powerful. More importantly, it is far closer to the "true" definition of functional programming than what we have in Scala.

### Conclusion

I hate to say it, but Rich Hickey and the others are quite right: Scala _isn't_ a terribly functional language. Variables, mutable data structures, side effects and constant type declarations all seem to conspire to remove that crown from Scala's proverbial brow. But let's not forget one thing: Scala wasn't _designed_ to be a functional language.

That may sound like heresy, but it's true. Scala was created primarily as an experiment in language design, specifically focusing on type systems. This is the one area where I think Scala excels far beyond the rest of the field. Scala makes it possible to model many problems in an abstract way and then leverage the type system to prove correctness _at compile time_. This is approach is both revolutionary and an extremely natural way to solve problems. The experience of using the type system in this fashion is a little difficult to describe (I'm still on the lookout for good examples), but trust me, you'll like it when you see it.

Scala's not really a functional language, and as [Cedric Beaust has pointed out](<http://beust.com/weblog/archives/000490.html>), it's not really the best object-oriented language either; so what is it good for? Scala sits in a strange middle ground between the two worlds of functional and object-oriented programming. While this does have some disadvantages like being forced to take second place in terms of type inference, it also lets you do some really interesting stuff like build a mutable `ListBuffer` with constant time conversion to an immutable `List`, or sometimes recognize the fact that [fold is not the universal solution](<http://www.drmaciver.com/2008/08/functional-code-not-equal-good-code/>). It's an experiment to be sure, but one which I think has yielded some very powerful, very useful results...just not many of a _purely_ functional nature.