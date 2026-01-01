{%
laika.title = "The Magic Behind Parser Combinators"
laika.metadata.date = "2009-03-24"
%}


# The Magic Behind Parser Combinators

If you're like me, one of the first things that attracted you to Scala was its parser combinators.  Well, maybe that wasn't the _first_ thing for me, but it was pretty far up there.  Parser combinators make it almost too easy to create a parser for a complex language without ever leaving the comfortable play-pen afforded by Scala.  Incidentally, if you aren't familiar with the fundamentals of text parsing, context-free grammars and/or parser generators, then you might want to [do](<http://en.wikipedia.org/wiki/Context-free_grammar>) [some](<http://en.wikipedia.org/wiki/LL_parsing>) [reading](<http://en.wikipedia.org/wiki/Recursive_descent>) before you continue with this article.

### Intro to Parser Combinators

In most languages, the process of creating a text parser is usually an arduous and clumsy affair involving a parser generator (such as [ANTLR](<http://www.antlr.org/>), [JavaCC](<https://javacc.dev.java.net/>), [Beaver](<http://beaver.sourceforge.net/>) or <shamelessPlug>[ScalaBison](<http://www.cs.uwm.edu/~boyland/scala-bison.html>)</shamelessPlug>) and (usually) a lexer generator such as [JFlex](<http://jflex.de/>).  These tools do a very good job of generating sources for efficient and powerful parsers, but they aren't exactly the easiest tools to use.  They generally have a very steep learning curve, and due to their unique status as [compiler-compilers](<http://en.wikipedia.org/wiki/Compiler-compiler>), an unintuitive architecture.  Additionally, these tools can be somewhat rigid, making it very difficult to implement unique or experimental features.  For this reason alone, many real world compilers and interpreters (such as `javac`, `ruby`, `jruby` and `scalac`) actually use hand-written parsers.  These are usually easier to tweak, but they can be very difficult to develop and test.  Additionally, hand-written parsers have a tendency toward poor performance (think: the Scala compiler).

When creating a compiler in Scala, it is perfectly acceptable to make use of these conventional Java-generating tools like ANTLR or Beaver, but we do have other options.  Parser combinators are a domain-specific language baked into the standard library.  Using this internal DSL, we can create an instance of a parser for a given grammar using Scala methods and fields.  What's more, we can do this in a very declarative fashion.  Thanks to the magic of DSLs, our sources will actually _look_ like a plain-Jane context-free grammar for our language.  This means that we get most of the benefits of a hand-written parser without losing the maintainability afforded by parser generators like bison.  For example, here is a very simple grammar for a simplified Scala-like language, expressed in terms of parser combinators:

```scala
object SimpleScala extends RegexpParsers {
  
  val ID = """[a-zA-Z]([a-zA-Z0-9]|_[a-zA-Z0-9])*"""r
  
  val NUM = """[1-9][0-9]*"""r
  
  def program = clazz*
  
  def classPrefix = "class" ~ ID ~ "(" ~ formals ~ ")"
  
  def classExt = "extends" ~ ID ~ "(" ~ actuals ~ ")"
  
  def clazz = classPrefix ~ opt(classExt) ~ "{" ~ (member*) ~ "}"
  
  def formals = repsep(ID ~ ":" ~ ID, ",")
  
  def actuals = expr*
  
  def member = (
      "val" ~ ID ~ ":" ~ ID ~ "=" ~ expr
    | "var" ~ ID ~ ":" ~ ID ~ "=" ~ expr
    | "def" ~ ID ~ "(" ~ formals ~ ")" ~ ":" ~ ID ~ "=" ~ expr
    | "def" ~ ID ~ ":" ~ ID ~ "=" ~ expr
    | "type" ~ ID ~ "=" ~ ID
  )
  
  def expr: Parser[Expr] = factor ~ (
      "+" ~ factor
    | "-" ~ factor
  )*
  
  def factor = term ~ ("." ~ ID ~ "(" ~ actuals ~ ")")*
  
  def term = (
      "(" ~ expr ~ ")"
    | ID
    | NUM
  )
}
```

This is all valid and correct Scala.  The `program` method returns an instance of `Parser[List[Class_]]`, assuming that `Class_` is the AST class representing a syntactic class in the language (and assuming that we had added all of the boiler-plate necessary AST generation).  This `Parser` instance can then be used to process a `java.io.Reader`, producing some result if the input is valid, otherwise producing an error.

### How the Magic Works

The really significant thing to notice here is that `program` is nothing special; just another method which returns an instance of class `Parser`.  In fact, _all_ of these methods return instances of `Parser`.  Once you realize this, the magic behind all of this becomes quite a bit more obvious.  However, to really figure it all out, we're going to need to take a few steps back.

Conceptually, a `Parser` represents a very simple idea:

> Parsers are invoked upon an input stream.  They will consume a certain number of tokens and then return a result along with the truncated stream.  Alternatively, they will fail, producing an error message.

Every `Parser` instance complies with this description.  To be more concrete, consider the `keyword` parser (what I like to call the " _literal_ " parser) which consumes a single well-defined token.  For example (note that the `keyword` method is implicit in Scala's implementation of parser combinators, which is why it doesn't appear in the long example above):

```scala
def boring = keyword("bore")
```

The `boring` method returns a value of type `Parser[String]`.  That is, a parser which consumes input and somehow produces a `String` as a result (along with the truncated stream).  This parser will either parse the characters b-o-r-e in that order, or it will fail.  If it succeeds, it will return the string `"bore"` as a result along with a stream which is shortened by four characters.  If it fails, it will return an error message along the lines of "`Expected 'bore', got 'Boer'`", or something to that effect.

By itself, such a parser is really not very useful.  After all, it's easy enough to perform a little bit of `String` equality testing when looking for a well-defined token.  The real power of parser combinators is in what happens when you start combining them together (hence the name).  A few literal parsers combined in sequence can give us a phrase in our grammar, and a few of these sequences combined in a disjunction can give us the full power of a non-terminal with multiple productions.  As it turns out, all we need is the literal parser (`keyword`) combined with these two types of combinators to express _any_ LL(*) grammar.

Before we get into the combinators themselves, let's build a framework.  We will define `Parser[A]` as a function from `Stream[Character]` to `Result[A]`, where `Result[A]` has two implementations: `Success[A]` and `Failure`.  The framework looks like the following:

```scala
trait Parser[+A] extends (Stream[Character]=>Result[A])

sealed trait Result[+A]

case class Success[+A](value: A, rem: Stream[Character]) extends Result[A]

case class Failure(msg: String) extends Result[Nothing]
```

Additionally, we must add a concrete parser, `keyword`, to handle our literals.  For the sake of syntactic compatibility with Scala's parser combinators, this parser will be defined within the `RegexpParsers` singleton object (despite the fact that we don't really support regular expressions):

```scala
object RegexpParsers {
  implicit def keyword(str: String) = new Parser[String] {
    def apply(s: Stream[Character]) = {
      val trunc = s take str.length
      lazy val errorMessage = "Expected '%s' got '%s'".format(str, trunc.mkString)

      if (trunc lengthCompare str.length != 0) 
        Failure(errorMessage)
      else {
        val succ = trunc.zipWithIndex forall {
          case (c, i) => c == str(i)
        }

        if (succ) Success(str, s drop str.length)
        else Failure(errorMessage)
      }
    }
  }
}
```

For those of you who are still a little uncomfortable with the more obscure higher-order utility methods in the Scala collections framework: don't worry about it.  While the above may be a bit obfuscated, there isn't really a need to understand what's going on at any sort of precise level.  The important point is that this `Parser` defines an `apply` method which compares `str` to an equally-lengthed prefix from `s`, the input character `Stream`.  At the end of the day, it returns either `Success` or `Failure`.

#### The Sequential Combinator

The first of the two combinators we need to look at is the sequence combinator.  Conceptually, this combinator takes two parsers and produces a new parser which matches the first _and_ the second in order.  If either one of the parsers produces a `Failure`, then the entire sequence will fail.  In terms of classical logic, this parser corresponds to the AND operation.  The code for this parser is almost ridiculously simple:

```scala
class SequenceParser[+A, +B](l: =>Parser[A], 
                             r: =>Parser[B]) extends Parser[(A, B)] {
  lazy val left = l
  lazy val right = r

  def apply(s: Stream[Character]) = left(s) match {
    case Success(a, rem) => right(rem) match {
      case Success(b, rem) => Success((a, b), rem)
      case f: Failure => f
    }

    case f: Failure => f
  }
}
```

This is literally just a parser which applies its `left` operand and then applies its `right` to whatever is left.  As long as _both_ parsers succeed, a composite `Success` will be produced containing a tuple of the left and right parser's results.  Note that Scala's parser combinator framework actually yields an instance of the `~` case class from its sequence combinator.  This is particularly convenient as it allows for a very nice syntax in pattern matching for semantic actions (extracting parse results).  However, since we will not be dealing with the action combinators in this article, it seemed simpler to just use a tuple.

One item worthy of note is the fact that both `left` and `right` are evaluated lazily.  This means that we don't actually evaluate our constructor parameters until the parser is applied to a specific stream.  This is very important as it allows us to define parsers with recursive rules.  Recursion is really what separates context-free grammars from regular expressions.  Without this laziness, any recursive rules would lead to an infinite loop in the parser construction.

Once we have the sequence combinator in hand, we can add a bit of syntax sugar to enable its use.  All instances of `Parser` will define a `~` operator which takes a single operand and produces a `SequenceParser` which handles the receiver and the parameter in order:

```scala
trait Parser[+A] extends (Stream[Character]=>Result[A]) {
  def ~[B](that: =>Parser[B]) = new SequenceParser(this, that)
}
```

With this modification to `Parser`, we can now create parsers which match arbitrary sequences of tokens.  For example, our framework so far is more than sufficient to define the `classPrefix` parser from our earlier snippet (with the exception of the regular expression defined in `ID`, which we currently have no way of handling):

```scala
def classPrefix = "class" ~ ID ~ "(" ~ formals ~ ")"
```

#### The Disjunctive Combinator

This is a very academic name for a very simple concept.  Let's think about the framework so far.  We have both literal parsers and sequential combinations thereof.  Using this framework, we are capable of defining parsers which match arbitrary token strings.  We can even define parsers which match _infinite_ token sequences, simply by involving recursion:

```scala
def fearTheOnes: Parser[Any] = "1" ~ fearTheOnes
```

Of course, this parser is absurd, since it only matches an infinite input consisting of the `'1'` character, but it does serve to illustrate that we have a reasonably powerful framework even in its current form.  This also provides a nice example of how the lazy evaluation of sequence parsers is an absolutely essential feature.  Without it, the `fearTheOnes` method would enter an infinite loop and would never return an instance of `Parser`.

However, for all its glitz, our framework is still somewhat impotent compared to "real" parser generators.  It is almost trivial to derive a grammar which cannot be handled by our parser combinators.  For example:

```
e ::= '1' | '2'
```

This grammar simply says "match either the `'1'` character, or the `'2'` character".  Unfortunately, our framework is incapable of defining a parser according to this rule.  We have no facility for saying "either this or that".  This is where the disjunctive combinator comes into play.

In boolean logic, a disjunction is defined according to the following truth table:

**P** | **Q** | **P V Q**  
---|---|---  
T | T | T  
T | F | T  
F | T | T  
F | F | F  
  
In other words, the disjunction is true if one or both of its component predicates are true.  This is exactly the sort of combinator we need to bring our framework to full LL(*) potential.  We need to define a parser combinator which takes two parsers as parameters, trying each of them in order.  If the first parser succeeds, we yield its value; otherwise, we try the second parser and return its `Result` (whether `Success` or `Failure`).  Thus, our disjunctive combinator should yield a parser which succeeds if and only if one of its component parsers succeeds.  This is very easily accomplished:

```scala
class DisParser[+A](left: Parser[A], right: Parser[A]) extends Parser[A] {
  def apply(s: Stream[Character]) = left(s) match {
    case res: Success => res
    case _: Failure => right(s)
  }
}
```

Once again, we can beautify the syntax a little bit by adding an operator to the `Parser` super-trait:

```scala
trait Parser[+A] extends (Stream[Character]=>Result[A]) {
  def ~[B](that: =>Parser[B]) = new SequenceParser(this, that)

  def |(that: Parser[A]) = new DisParser(this, that)
}
```

### ...is that all?

It's almost as if by magic, but the addition of the disjunctive combinator to the sequential actually turns our framework into something really special, capable of chewing through any LL(*) grammar.  Just in case you don't believe me, consider the grammar for the pure-untyped lambda calculus, expressed using our framework (`alph` definition partially elided for brevity):

```scala
object LambdaCalc extends RegexpParsers {

  def expr: Parser[Any] = term ~ (expr | "")

  def term = (
      "fn" ~ id ~ "=>" ~ expr
    | id
  )

  val alph = "a"|"b"|"c"|...|"X"|"Y"|"Z"
  val num = "0"|"1"|"2"|"3"|"4"|"5"|"6"|"7"|"8"|"9"

  def id = alph ~ idSuffix

  def idSuffix = (
      (alph | num) ~ idSuffix
    | ""
  )
}
```

While this grammar may seem a bit obfuscated, it is only because I had to avoid the use of regular expressions to define the `ID` rule.  Instead, I used a combination of sequential and disjunctive combinators to produce a `Parser` which matches the desired pattern.  Note that the "`...`" is not some special syntax, but rather my laziness and wish to avoid a code snippet 310 characters wide.

We can also use our framework to define some other, useful combinators such as `opt` and `*` (used in the initial example). Specifically:

```scala
trait Parser[+A] extends (Stream[Character]=>Result[A]) {
  ...

  def *: Parser[List[A]] = (
      this ~ * ^^ { case (a, b) => a :: b }
    | "" ^^^ Nil
  )
}

object RegexpParsers {
  ...

  def opt[A](p: Parser[A]) = (
      p ^^ { Some(_) }
    | "" ^^^ None
  )
}
```

Readers who have managed to stay awake to this point may notice that I'm actually cheating a bit in these definitions.  Specifically, I'm using the `^^` and `^^^` combinators.  These are the semantic action combinators which I promised to avoid discussing.  However, for the sake of completeness, I'll include the sources and leave you to figure out the rest:

```scala
trait Parser[+A] extends (Stream[Character]=>Result[A]) { outer =>
  ...
  
  def ^^[B](f: A=>B) = new Parser[B] {
    def apply(s: Stream[Character]) = outer(s) match {
      case Success(v, rem) => Success(f(v), rem)
      case f: Failure => f
    }
  }

  def ^^^[B](v: =>B) = new Parser[B] {
    def apply(s: Stream[Character]) = outer(s) match {
      case Success(_, rem) => Success(v, rem)
      case f: Failure => f
    }
  }
}
```

In short, these combinators are only interesting to people who want their parsers to give them a value upon completion (usually an AST).  In short, just about any useful application of parser combinators will require these combinators, but since we're not planning to use our framework for anything useful, there is really no need.

One really interesting parser from our first example which is worthy of attention is the `member` rule.  If you recall, this was defined as follows:

```scala
def member = (
    "val" ~ ID ~ ":" ~ ID ~ "=" ~ expr
  | "var" ~ ID ~ ":" ~ ID ~ "=" ~ expr
  | "def" ~ ID ~ "(" ~ formals ~ ")" ~ ":" ~ ID ~ "=" ~ expr
  | "def" ~ ID ~ ":" ~ ID ~ "=" ~ expr
  | "type" ~ ID ~ "=" ~ ID
)
```

This is interesting for two reasons.  First: we have multiple disjunctions handled in the same rule, showing that disjunctive parsers chain just as nicely as do sequential.  But more importantly, our chain of disjunctions includes two parsers which have the same prefix (`"def" ~ ID`).  In other words, if we attempt to parse an input of "`def a: B = 42`", one of these deeply nested parsers will erroneously match the input for the first two tokens.

This grammatical feature forces us to implement some sort of backtracking within our parser combinators.  Intuitively, the `"def" ~ ID` parser is going to successfully match "`def a`", but the enclosing sequence (`"def" ~ ID ~ "("`) will fail as soon as the "`:`" token is reached.  At this point, the parser has to take two steps back in the token stream and try again with another parser, in this case, `"def" ~ ID ~ ":" ~ ID ~ "=" ~ expr`.  It is this feature which separates LL(*) parsing from LL(1) and LL(0).

The good news is that we already have this feature almost by accident.  Well, obviously not by accident since I put some careful planning into this article, but at no point so far did we actually _set out_ to implement backtracking, and yet it has somehow dropped into our collective lap.  Consider once more the implementation of the disjunctive parser:

```scala
class DisParser[+A](left: Parser[A], right: Parser[A]) extends Parser[A] {
  def apply(s: Stream[Character]) = left(s) match {
    case res: Success => res
    case _: Failure => right(s)
  }
}
```

Notice what happens if `left` fails: it invokes the `right` parser passing the _same_ `Stream` instance (`s`).  Recall that `Stream` is immutable, meaning that there is nothing `left` can do which could possibly change the value of `s`.  Each parser is merely grabbing characters from the head of the stream and then producing a _new_ `Stream` which represents the remainder.  Parsers farther up the line (like our disjunctive parser) are still holding a reference to the stream _prior_ to these "removals".  This means that we don't need to make any special effort to implement backtracking, it just falls out as a natural consequence of our use of the `Stream` data structure.  Isn't that nifty?

### Conclusion

Parser combinators are an incredibly clever bit of functional programming.  Every time I think about them, I am once again impressed by the ingenuity of their design and the simple elegance of their operation.  The fact that two combinators and a single parser can encode the vast diversity of LL(*) grammars is simply mind-boggling.  Despite their simplicity, parser combinators are capable of some very powerful parsing in a very clean and intuitive fashion.  That to me is magical.