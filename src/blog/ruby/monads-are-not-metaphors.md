{%
laika.title = "Monads Are Not Metaphors"
laika.metadata.date = "2010-12-27"
%}


# Monads Are Not Metaphors

_This article is also [available in Japanese](<http://eed3si9n.com/ja/monads-are-not-metaphors>)._

I am about to break a promise. Almost three years ago, I promised myself that I would _never_ write an article about monads. There are too many such articles already; so many, in fact, that people are often confused by the sheer proliferation. Everyone seems to have a different take on the subject, meaning that those attempting to learn the concept for the first time are stuck trying to reason out the commonalities between burritos, space suits, elephants and desert Bedouins.

I'm not going to add to this menagerie of confusing analogies. The fact is that none of these parallels are entirely accurate. None of them convey the whole picture, and some of them are blatantly misleading in important respects. You will _never_ come to understand monads by pondering Mexican food and the Final Frontier. The only way to understand monads is to see them for what they are: a mathematical construct.

### Math (or not)

Here's the thing about monads which is hard to grasp: monads are a pattern, not a specific type. Monads are a _shape_ , they are an abstract interface (not in the Java sense) more than they are a concrete data structure. As a result, _any_ example-driven tutorial is doomed to incompleteness and failure. The only way to really understand is to take a step back and look at what monads mean in the _abstract_ rather than the concrete. Take a look at the following Ruby snippet:

```ruby
def foo(bar)
  puts bar
  bar.size
end
```

Just as a quick Ruby refresher, we can rewrite this code in the following way:

```ruby
def foo(bar)
  puts bar; bar.size
end
```

Ruby has this neat convention (which is shared by most modern languages) which causes the final expression in a method to be turned into the implicit return statement. Thus, the `foo` method will take a parameter, print it to standard out and then return its `size`. Fairly simple, right?

Here's the puzzler: what is the semicolon (`;`) doing? It's tempting to say that it's just a separator, but theoretically speaking, there's something much more interesting going on here. Let's switch to Scala and add some Christmas trimmings:

```scala
def foo(bar: String) = {
  ({ () => println(bar) })()
  ({ () => bar.length })()
}
```

Just in case you're unfamiliar with Scala, I'd like to make it clear that we are _not_ required to enclose every statement inside its own lambda (anonymous function). I'm just doing that to make a point.

This function does exactly the same thing as the Ruby version. Well, the parameter is a bit more constrained since we require a `String` rather than accepting anything defines `size`, but moving past that… The major difference from what we had previously is that each statement is wrapped inside its own anonymous function, which we immediately call. We can again play the same semicolon trick that we used in Ruby. However, because the statements are actually functions, we can go a step further:

```scala
def foo(bar: String) = {
  ({ () => println(bar) } andThen { () => bar.length })()
}
```

(note: the `andThen` method isn't defined for functions of 0-arity, but we're going to pretend that it is and that it works the same as it does for functions of one argument. If it makes you feel better, you can pretend that these are both one-argument functions taking `Unit` as a parameter, the theoretical implications are the same, it just requires more syntax)

Notice that we haven't actually used the semicolon (although we could have). Instead, we're _combining_ two functions together and invoking them at the very end. The semantics we're using to do the combination are such that the first function will be evaluated, then its result (`()`) discarded and the second function evaluated with its result returned. For those following along at home, we could easily define `andThen` in the following way:

```scala
def funcSyntax[A](f1: () => A) = new {
  def andThen[B](f2: () => B) = f1(); f2()
}
```

In a way, we have defined a method which literally encapsulates the effect of the semicolon "operator", allowing us to apply it directly to functions, rather than dealing with it indirectly at the level of statements. That's kind of a cool thought, but the important point is that we are first executing the first function, discarding its result and then executing the second function, returning its result.

It should be clear that we could extend this to any number of functions. For example:

```scala
def foo(bar: String) = {
  ({ () => println("Executing foo") } andThen
   { () => println(bar) } andThen
   { () => bar.length })()
}
```

Still with me? Congratulations, you've seen your first monad.

### [You Could Have Invented Monads! (and maybe you already have)](<http://blog.sigfpe.com/2006/08/you-could-have-invented-monads-and.html>)

This certainly isn't a monad in the traditional sense, but if we worked at it, we could show that the monadic axioms do hold. The significant point here is what this monad is doing: combining one thing together with another in sequence. In fact, this is what _all_ monads do, deep down. You start out with Thing One, and you have a function which will (given One) will give you Thing Two. Monads let you combine Thing One and your function together, producing a final resultant Thing. Let's look at some more code:

```scala
case class Thing[+A](value: A)
```

This is about the simplest container imaginable (in fact, it is _precisely_ the simplest container imaginable, but that's not relevant now). We can wrap up values inside of `Thing`, but that's about it:

```scala
val a = Thing(1)
val b = Thing(2)
```

Now, let's switch into design mode for a moment. Imagine that we find ourselves writing a lot of code which looks like this:

```scala
def foo(i: Int) = Thing(i + 1)

val a = Thing(1)
val b = foo(a.value)        // => Thing(2)
```

We're starting with a `Thing`, and then we're using the value inside of that `Thing` to call a function which gives us a new `Thing`. If you think about it, this is actually a very common pattern. We have a value, and then we use that value to compute a new value. Mathematically, this is pretty much the same as the following:

```scala
def foo(i: Int) = i + 1

val a = 1
val b = foo(a)              // => 2
```

The only difference between these is that the first version wraps everything in `Thing`, while the second version is using "bare" values.

Now, let's extend our imagine just a bit and assume that we have a good reason for wrapping everything inside of `Thing`. There could of course be any number of reasons for this, but basically it boils down to the notion that `Thing` might have some extra logic which does interesting things with its value. Here's the question: can we come up with a nicer way of going from `a` to `b`? Basically, we want to encapsulate this pattern as a more general tool.

What we want is a function which pulls the value out of `Thing` and then calls another function with that value, returning the result of that function call (which will be a new `Thing`). Since we're good object-oriented programmers, we will define this as a method on class `Thing`:

```scala
case class Thing[+A](value: A) {
  def bind[B](f: A => Thing[B]) = f(value)
}
```

So if we have a `Thing`, we can pull its value out and use it to compute a new `Thing`, all in one convenient step:

```scala
def foo(i: Int) = Thing(i + 1)

val a = Thing(1)
val b = a bind foo          // => Thing(2)
```

Notice that this is a lot cleaner that our original version, while still performing exactly the same function. `Thing` is a monad.

#### The Monad Pattern

Any time you start with something which you pull apart and use to compute a new something of that same type, you have a monad. It's really as simple as that. If it sounds like I'm describing almost all of your code, then good, that means you're starting to catch on. Monads are everywhere. And by "everywhere", I do mean _everywhere_.

To understand why this is, let's look at what it is that makes `Thing` a monad:

```scala
val a = Thing(1)
```

The first thing is that I can wrap up a value inside of a new `Thing`. Object-oriented developers might call this a "constructor". Monads call it "the `unit` function". Haskell calls it "`return`" (maybe we shouldn't try to figure out that one just yet). Whatever you call it though, it comes to the same thing. We have a function of type `A => Thing`; a function which takes some value and wraps it up inside a new `Thing`.

```scala
a bind { i => Thing(i + 1) }
```

We also have this fancy `bind` function, which digs inside our `Thing` and allows a function which we supply to use that value to create a new `Thing`. Scala calls this function "`flatMap`". Haskell calls it "`>>=`". Again, the name doesn't matter. What's interesting here is the fact that `bind` is how you combine two things together in sequence. **We start with one thing and use its value to compute a new thing.**

It's as simple as that! If you're like me, then you're likely asking the following question: if it's so simple, what's all the fuss about? Why not just call this the "using one thing to compute another" pattern? Well, for one thing, that's far too verbose. For another, monads were first defined by mathematicians, and mathematicians _love_ to name things. Mathematics is all about finding patterns, and it's hard to find patterns if you can't affix labels once you've found them.

### More Examples

I said that monads are everywhere (they are!), but we've only looked at two examples so far. Let's see a few more.

#### Option

This might be the most famous monad of all, quite possibly because it's one of the easiest to understand and by far the easiest to motivate. Consider the following code:

```scala
def firstName(id: Int): String = ...    // fetch from database
def lastName(id: Int): String = ...

def fullName(id: Int): String = {
  val fname = firstName(id)
  if (fname != null) {
    val lname = lastName(id)
    if (lname != null)
      fname + " " + lname
    else
      null
  } else {
    null
  }
}
```

Here again, we have a fairly common pattern. We have two functions (`firstName` and `lastName`) which are responsible for producing some data which may or may not be available. If the data is available, then it is returned. Otherwise, the result of these functions will be `null`. We then use these functions to do something interest (in this case, compute a full name). Unfortunately, the fact that `firstName` and `lastName` may or may not produce a useful value needs to be handled explicitly with a set of nested `if`s.

At first blush, it seems this is the best that we can do. However, if you look very closely, you can find the monad pattern buried in this code. It's a little more complicated than last time, but it's still there. Let's try wrapping everything in `Thing` to make it clear:

```scala
def firstName(id: Int): Thing[String] = ...    // fetch from database
def lastName(id: Int): Thing[String] = ...

def fullName(id: Int): Thing[String] = {
  firstName(id) bind { fname =>
    if (fname != null) {
      lastName(id) bind { lname =>
        if (lname != null)
          Thing(fname + " " + lname)
        else
          Thing(null)
      }
    } else {
      Thing(null)
    }
  }
}
```

See it now? As I said, monads are everywhere. Here's the really useful bit though: every time we `bind`, the very first thing we do _inside_ the function is test the value to see if it is `null`, why not move that logic into `bind`? Of course, we can't really do that without changing `Thing` into something different, so we will define a new monad called `Option`:

```scala
sealed trait Option[+A] {
  def bind[B](f: A => Option[B]): Option[B]
}

case class Some[+A](value: A) extends Option[A] {
  def bind[B](f: A => Option[B]) = f(value)
}

case object None extends Option[Nothing] {
  def bind[B](f: Nothing => Option[B]) = None
}
```

If you block out everything except `Some`, this looks a _lot_ like our old friend, `Thing`. The main difference is that `Option` has two different instantiations: `Some`, which contains a value, and `None`, which doesn't contain a value. Think of `None` as being just an easier way of writing `Thing(null)`.

What's interesting is that `Some` and `None` need to have two different definitions of `bind`. The definition of `bind` in `Some` looks a lot like the definition in `Thing`, which makes sense as `Some` and `Thing` are almost identical. However, `None` defines `bind` to always return `None`, ignoring the specified function. How does this help us? Well, let's return to our `fullName` example:

```scala
def firstName(id: Int): Option[String] = ...    // fetch from database
def lastName(id: Int): Option[String] = ...

def fullName(id: Int): Option[String] = {
  firstName(id) bind { fname =>
    lastName(id) bind { lname =>
      Some(fname + " " + lname)
    }
  }
}
```

All of those nasty `if` statements have disappeared. This works because `firstName` and `lastName` now return `None` when they fail to fetch the database record, rather than `Thing(null)`. Of course, if we try to `bind` on `None`, the result will always be `None`. Thus, the `fullName` function returns the combination of `firstName` and `lastName` inside of a `Some` instance only if neither `firstName` nor `lastName` return `None`. If either one returns `None`, then the result of the whole thing will be `None`.

For those keeping score at home, we have "accidentally" stumbled upon Groovy's [safe-dereference operator](<http://groovy.codehaus.org/Null+Object+Pattern>) (`?.`), [Raganwald's `andand`](<https://github.com/raganwald/andand>) for Ruby and many, many more. See? Monads are everywhere.

### IO

Anyone trying to understand monads will inevitably run into Haskell's `IO` monad, and the results are almost always the same: bewilderment, confusion, anger, and ultimately Perl. The fact is that `IO` is a rather odd monad. It is fundamentally in the same category as `Thing` and `Option`, but it solves a very different problem.

Here's the deal: Haskell doesn't allow side-effects. At all. Functions take parameters and return values; you can't just "change" something outside the function. As an example of what this means, let's return to our earlier Ruby code:

```ruby
def foo(bar)
  puts bar
  bar.size
end
```

This function takes a value, calls its `size` method and returns the result. However, it also _changes_ the standard output stream. This is pretty much the same as if we had a global array hanging around which we just mutated in-place:

```ruby
STDOUT = []

def foo(bar)
  STDOUT += [bar]
  bar.size
end
```

Haskell doesn't have variables of any sort (imagine if Scala _didn't_ have `var`, or if every variable in Java were `final`). Since we don't have any variables, we can't change anything in-place. Since we can't change anything in-place, there's no way we can define a `puts` function. At least, not the `puts` we're used to.

Let's switch back to Scala. Our goal here is to define a `println` function which _doesn't_ rely on any sort of mutable state (ignoring for the moment the fact that the standard output stream is always going to be mutable, since there's no way we can "change" the user's screen by cloning the physical display). One thing we could do is wrap up our standard output stream as a `Vector` which we carry along with our functions:

```scala
def foo(bar: String, stdout: Vector[String]) = {
  val stdout2 = println(bar, stdout)
  (bar.length, stdout2)
}

def println(str: String, stdout: Vector[String]) = stdout + str
```

Theoretically, we could write all of our `println`-enabled functions in this way, passing in the current `stdout` and receiving the new state as a result. At the end of the day, the entire program would produce a result in addition to the final state of `stdout`, which could be printed to the screen by the language runtime.

This works (at least for `println`), but it's ridiculously ugly. I would hate it if I had to write code like this, and early Haskell adopters felt exactly the same way. Unfortunately, things only get worse from there. Our trick with `Vector[String]` may work for the standard output stream, but what about standard in? At first blush, it _seems_ as if the `readLine` function wouldn't be as bad as `println`, after all, we aren't changing anything! Unfortunately, something is clearly being changed at some level, otherwise repeated calls to `readLine` would yield the same result (clearly not the case).

Graphics updates, networking, the list goes on. It turns out that any _useful_ program is going to need to have side-effects, otherwise there's no way for all of that usefulness to get _outside_ the program where we can see it. So, Haskell's designers (more specifically, Phillip Wadler) needed to come up with a way to solve this problem not only for standard out, but for _all_ side-effects.

The solution is actually quite simple. To solve the standard out problem with `println`, we just passed a `Vector[String]` around, "modifying" it by returning the new state along with our regular return value. Here's the inspiration: what if we did that for the _entire universe?_ Instead of passing around just a plain `Vector[String]`, we pass around `Universe`:

```scala
def foo(bar: String, everything: Universe) = {
  val everything2 = println(bar, everything)
  (bar.length, everything2)
}

def println(str: String, everything: Universe) = everything.println(str)
```

As long as the language runtime is able to somehow give us an instance of `Universe` which behaves the way we would expect, then this code will work just fine. Obviously, the runtime can't _really_ package up the entire cosmos and allow us to get new versions of it, but it can cheat a bit and _pretend_ to give us the entire universe. The language runtime is allowed to implement the `println` function on the `Universe` object in whatever way it deems best (hopefully by actually appending to standard out). Thus, we let the runtime perform whatever magic is necessary while we remain blissfully ignorant of any and all side-effects.

This solves our problems alright, but it's horrible in almost every other respect. We have this manual threading of the `Universe`, which is both verbose and painful. Even worse, this sort of thing is very error prone (i.e. what happens if we "modify" the universe and then go back and change an older version of it? do we get two, parallel universes?). The heart of our problem now is that we are using the old version of the universe to compute a new version of the universe, and we're doing that manually. We're taking something (in this case, the universe) and using its value to compute a new something (the new version of the universe). Sound familiar?

Phillip Wadler's inspiration was to take advantage of the monad pattern to solve this problem. The result is the `IO` monad:

```scala
def foo(bar: String): IO[Int] = {
  println(bar) bind { _ => IO(bar.length) }
}

def println(str: String): IO[Unit] = {
  // TODO insert magic incantations
}
```

Of course, we can't _actually_ implement `println`, since this fake language isn't allowed to involve itself in side-effects. However, the language runtime could provide a native (read: cheat code) implementation of `println` which performs its side-effects and then conjures a new version of `IO` (i.e. the modified universe).

The only catch with this design is that we can never get anything _out_ of `IO`. Once you start down the dark path, forever will it dominate your destiny. The reason for this is one of language purity. Haskell wants to disallow side-effects, but if it were to allow us to pull values out of `IO`, then we could very easily bypass its safe-guards:

```scala
def readLine(): IO[String] = {
  // TODO insert magic incantations
}

def fakeReadLine(str: String): String = {
  val back: IO[String] = readLine()
  back.get      // whew!  doesn't work
}
```

As you can see, if we could pull values _out_ of `IO`, then the whole exercise would become a waste of time, since it would be trivially easy to hide side-effects within wrapper functions.

Of course, none of this is particularly relevant to Scala or Ruby, much less Java! Scala doesn't restrict side-effects. It allows you to call `println` whenever you feel like it, and it provides a way of declaring mutable variables (`var`). In a sense, Scala is hiding a hypothetical `IO` monad. It's as if _all_ Scala code is implicitly inside of `IO`, and thus implicitly threading the state of the universe from one moment to the next. In light of this fact, why should we care at all about how `IO` works? Mostly because it's a monad which is very different from `Thing` and `Option`. I briefly considered using `State`, but that's too much ancillary complexity when we're trying to focus on the core idea of using the value from one thing to compute another thing.

### Now What?

So, we've identified the monad pattern. We can spot it in code and employ it in an impressive range of situations, but why are we bothering with all the ceremony? If we're already using monads all over the place without realizing it (e.g. semicolon), then why do we have to bother futzing with all of this nomenclature? Simply put: why do we care that `Option` is a monad so long as it just "does the right thing"?

Well, the first answer to that lies in the nature of mathematics, and by extension, computer programming. As I said, before, mathematics is all about identifying patterns (well, you also play with those patterns to generate larger, more intricate patterns, but that's really just a means to an end). Once you identify a pattern, you name it. That's just what mathematicians do. Imagine if Newton hadn't named the "derivative", and he simply insisted on calling it "an expression for the line tangent to a given line relative to a particular value of _x_ , where _x_ is some variable in the expression for the given line." For one thing, calculus textbooks everywhere would be about 50 times longer. For another, we probably never would have been able to see "the derivative" as an abstract entity. Partial derivation would have never been devised. Integrals, differential equations, infinite series, and almost all of physics might never have happened. None of these consequences have anything to do with the _name_ , that's just a label. Rather, it was the fact that Newton was able to see (and represent) the derivative in the abstract, as a mathematical _shape_ to be manipulated and applied in novel ways.

If you understand the `Option` monad, then you can use the `Option` monad. You can see places in your code where it can be applied, and you will reap enormous benefits as a result. However, if you understand monads as an abstraction, then you will not only understand `Option`, but also `IO`, `State`, `Parser`, `STM`, the list goes on. Or at least, you will understand the fundamental properties of these constructs (the rest is details). You will begin to see places where you are doing monadic things even when those things don't fit exactly into the restricted mold of `Option` or `State`. This is where the true utility can be found.

Besides the (vast) improvements in your thought process, there's also a more immediate practical upshot. It's possible to define functions which work on monads in the generic sense, rather than specializing in one or the other. Just as Swing programming would be impossible if you had to rewrite every function for each specific instance of `Component`, there are many things which would be impossible (or at least, very very impractical) if you had to rewrite them for each specific monad. One such function is `sequence`:

```scala
trait Monad[+M[_]] {
  def unit[A](a: A): M[A]
  def bind[A, B](m: M[A])(f: A => M[B]): M[B]
}

implicit object ThingMonad extends Monad[Thing] {
  def unit[A](a: A) = Thing(a)
  def bind[A, B](thing: Thing[A])(f: A => Thing[B]) = thing bind f
}

implicit object OptionMonad extends Monad[Option] {
  def unit[A](a: A) = Some(a)
  def bind[A, B](opt: Option[A])(f: A => Option[B]) = opt bind f
}

def sequence[M[_], A](ms: List[M[A]])(implicit tc: Monad[M]) = {
  ms.foldRight(tc.unit(List[A]())) { (m, acc) =>
    tc.bind(m) { a => tc.bind(acc) { tail => tc.unit(a :: tail) } }
  }
}
```

There are a lot of ways to pretty this up, but I wanted to be as explicit as possible for the sake of illustration. The general function of `sequence` is to take a `List` of monad instances and return a monad instance of a `List` of those elements. For example:

```scala
val nums = List(Some(1), Some(2), Some(3))
val nums2 = sequence(nums)           // Some(List(1, 2, 3))
```

The magic is that this function works on _any_ monad:

```scala
val nums = List(Thing(1), Thing(2), Thing(3))
val nums2 = sequence(nums)           // Thing(List(1, 2, 3))
```

In this case, `Monad` (the trait) is an example of a _typeclass_. Basically, we're saying that there is this general idea of a monad, and any monad will define two functions: `unit` and `bind`. In this way, define functions which operate on monads without knowing which _specific_ monad we're manipulating. Think of it like the adapter pattern on steroids (sprinkled with a goodly portion of Scala's implicit magic).

This is reason number two for understanding the _general_ concept of a monad, rather than just the specific: you suddenly become able to define tons of nifty utility functions. For more examples of this, you need look no further than [Haskell's standard library](<http://members.chello.nl/hjgtuyl/tourdemonad.html>).

### Those Pesky Axioms

Congratulations! You just made it through an entire monad tutorial without ever having to worry about the monadic axioms or what they mean. Now that you've got the concept down (hopefully), you can graduate to the axioms themselves.

As it turns out, the monadic axioms are really quite intuitive. We've actually been assuming them all along without ever really saying so. The axioms define how the `unit` (constructor) and `bind` (composition) functions are supposed to behave under certain situations. Think of them a bit like the laws governing integer addition (commutativity, associativity, etc). They don't tell you _everything_ about monads (actually, they don't tell you much at all from an intuitive standpoint), but they do give you the basic, mathematical underpinnings.

Excited yet? No, I didn't think so. Well, here they are anyway, defined in terms of the `Monad` typeclass we used earlier:

```scala
def axioms[M[_]](implicit tc: Monad[M]) {
  // identity 1
  def identity1[A, B](a: A, f: A => M[B]) {
    val ma: M[A] = tc.unit(a)
    assert(tc.bind(ma)(f) == f(a))
  }

  forAll { (x, f) => identity1(a, f) }        // sort-of ScalaCheck
  
  // identity 2
  def identity2[A](ma: M[A]) {
    assert(tc.bind(ma)(tc.unit) == ma)
  }

  forAll { m => identity2(m) }

  // associativity
  def associativity[A, B, C](m: M[A], f: A => M[B], g: B => M[C]) {
    val mf: M[B] = tc.bind(m)(f)
    val mg: M[C] = tc.bind(mf)(g)

    val mg2: M[C] = tc.bind(m) { a =>
      tc.bind(f(a))(g)
    }

    assert(mg == mg2)
  }

  forAll { (m, f, g) => associativity(m, f, g) }
}
```

The first two axioms (the "identity" ones) are basically saying that the `unit` function is a simple constructor with respect to the `bind` function. Thus, when `bind` "pulls apart" the monad and passes the value to its function parameter, that value will be precisely the value that `unit` puts _into_ the monad. Likewise, if the function parameter given to `bind` simply takes the value and wraps it back up inside the monad, then the final result is exactly the same as if we had left the monad alone.

The third axiom is the most complicated to express, but I think it's actually the most intuitive. Basically, this axiom is saying that if you first `bind` with one function, then you `bind` the result against another function, that's the same as applying the first function to the value _inside_ the monad and then calling `bind` on the result of that application. This isn't exactly associativity in the classical sense, but you can sort of think about it in that way.

One of the useful consequences of the second law comes up (quite frequently) when you have one `bind` nested inside the another. Whenever you find yourself in that situation, you can move the nested `bind` outside of the outer `bind`, flattening your code slightly. Like so:

```scala
val opt: Option[String] = Some("string")

opt bind { str => 
  val innerOpt = Some("Head: " + str)
  innerOpt bind { str => Some(str + " :Tail") }
}

// is the same as...

opt bind { str => Some("Head: " + str) } bind { str => Some(str + " :Tail") }
```

The rewritten code has a much more "sequential" feel (the essence of monads!) and is almost always shorter than the nested form.

As I said before, the axioms are very, very intuitive once you understand the abstract concept of a monad. They may not be intuitive to _express_ , but their consequences are very easy to understand and quite natural in practice. So, don't spend a lot of time trying to memorize the axioms. Your time will be better spent contemplating the way that semicolon works.

### Conclusion

Monads are _not_ scary. They are not complex, academic or esoteric. Monads are an abstract, mathematical label affixed to a pattern found in almost all code. [We all use monads every day](<http://www.youtube.com/watch?v=TaAftRgptkQ>). The hardest part in understanding monads is recognizing that the hardest part isn't so hard after all.

I sincerely hope that this latest, dubious venture down the well-trod path of monad exposition has proven a fruitful one. I can say without hesitation that the understanding and perspective which comes from a solid grasp of monads is invaluable in very practical, down-to-earth coding (even in staid languages like Java!). Monads impart an understanding of the very fabric of sequential computation and composability, and if that isn't sufficient motivation to learn, I don't know what is.