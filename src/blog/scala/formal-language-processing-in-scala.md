{%
laika.title = "Formal Language Processing in Scala"
laika.metadata.date = "2008-06-16"
%}


# Formal Language Processing in Scala

Quite some time ago, a smart cookie named [Phillip Wadler](<http://en.wikipedia.org/wiki/Philip_Wadler>) authored [a publication](<http://portal.acm.org/citation.cfm?id=5288>) explaining the concept of "parser combinators", a method of representing the well-understood concept of text parsing as the composition of atomic constructs which behaved according to [monadic law](<http://en.wikipedia.org/wiki/Monad_\(functional_programming\)>).  This idea understandably captured the imaginations of a number of leading researchers, eventually developing into the Haskell [parsec library](<http://legacy.cs.uu.nl/daan/parsec.html>).  Being a functional language with academic roots, it is understandable that Scala would have an implementation of the combinator concept included in its standard library.

The inclusion of this framework into Scala's core library has brought advanced text parsing to within the reach of the "common man" perhaps for the first time.  Of course, tools like [Bison](<http://www.gnu.org/software/bison/>), [ANTLR](<http://www.antlr.org/>) and [JavaCC](<https://javacc.dev.java.net/>) have been around for a long time and gained quite a following, but such tools often have a steep learning curve and are quite intimidating for the casual experimenter.  Parser combinators are often much easier to work with and can streamline the transition from "experimenter" to "compiler hacker".

Of course, all of this functional goodness does come at a price: flexibility.  The Scala implementation of parser combinators rules out any left-recursive grammars.  Right-recursiveness is supported (thanks to call-by-name parameters), but any production rule which is left-recursive creates an infinitely recursive call chain and overflows the stack.  It is _possible_ to overcome this limitation with right-associative operators, but even if such an implementation existed in Scala, it wouldn't do any good.  Scala's parser combinators effectively produce an [LL(*) parser](<http://en.wikipedia.org/wiki/LL_parser>) instead of the far more flexible LR or even better, LALR such as would be produced by Bison or [SableCC](<http://sablecc.org/>).  It is unknown to me (and everyone I've asked) whether or not it is even possible to produce an LR parser combinitorially, though the problem of left-recursion in LL(*) has been [studied at length](<http://portal.acm.org/citation.cfm?id=1149982.1149988&coll=GUIDE&dl=GUIDE&CFID=9681311&CFTOKEN=34522455>).

The good news is that you really don't _need_ all of the flexibility of an LR parser for most cases.  (in fact, you don't theoretically _need_ LR at all, it's just easier for a lot of things)  Parser combinators are capable of satisfying many of the common parsing scenarios which face the average developer.  Unless you're planning on building the next Java-killing scripting language, the framework should be just fine.

Of course, any time you talk about language parsing, the topic of language analysis and interpretation is bound to come up.  This article explores the construction of a simple interpreter for a trivial language.  I chose to create an interpreter rather than a compiler mainly for simplicity (I didn't want to deal with bytecode generation in a blog post).  I also steer clear of a lot of the more knotty issues associated with semantic analysis, such as type checking, object-orientation, stack frames and the like.To be honest, I've been looking forward to writing this article ever since I read [Debasish Ghosh's](<http://debasishg.blogspot.com/>) excellent introduction to [external DSL implementation in Scala](<http://debasishg.blogspot.com/2008/04/external-dsls-made-easy-with-scala.html>).  Scala's parser combinators are dreadfully under-documented, especially when you discount purely academic publications.  Hopefully, this article will help to rectify the situation in some small way.

### Simpletalk: Iteration 1

Before we implement the interpreter, it is usually nice to have a language to interpret.  For the purposes of this article, we will be using an extremely contrived language based around the standard output (hence the name: "simple" "talk").  The language isn't [complete](<http://en.wikipedia.org/wiki/Turing_completeness>) (even in the later iterations) and you would be hard-pressed to find any useful application; but, it makes for a convenient running example to follow.

In the first iteration, Simpletalk is based around two commands: `print` and `space`.  Two hard-coded messages are available for output via print: `HELLO` and `GOODBYE`.  We will increase the complexity of the language later, allowing for literals and alternative constructs, but for the moment, this will suffice.  An example Simpletalk program which exercises all language features could be as follows:

```
print HELLO
space
space
print GOODBYE
space
print HELLO
print GOODBYE
```

The output would be the following:

```
Hello, World!

Farewell, sweet petunia!

Hello, World!
Farewell, sweet petunia!
```

As I said, not very useful.

The first thing we need to do is to define a [context-free grammar](<http://en.wikipedia.org/wiki/Context-free_grammar>) using the combinator library included with Scala.  The language itself is simple enough that we _could_ [write the parser by hand](<http://www.codecommit.com/blog/scala/naive-text-parsing-in-scala>), but it is easier to extend a language with a declarative definition.  Besides, I wanted an article on using parser combinators to build an interpreter...

```scala
object Simpletalk extends StandardTokenParsers with Application {
  lexical.reserved += ("print", "space", "HELLO", "GOODBYE")
  
  val input = Source.fromFile("input.talk").getLines.reduceLeft[String](_ + '\n' + _)
  val tokens = new lexical.Scanner(input)
  
  val result = phrase(program)(tokens)
  
  // grammar starts here
  def program = stmt+
  
  def stmt = ( "print" ~ greeting
             | "space" )
  
  def greeting = ( "HELLO"
                 | "GOODBYE" )
}
```

All of this is fairly standard [EBNF notation](<http://en.wikipedia.org/wiki/Extended_Backus%E2%80%93Naur_Form>).  The one critical bit of syntax here is the tilde (`~`) operator, shown separating the `"print"` and `greeting` tokens.  This method is the concatenation operator for the parsers.  Literally it means: first `"print"`, then parse `greeting`, whatever that entails.  What would normally be terminals in a context-free grammar are represented by full Scala methods.  Type inference makes the syntax extremely concise.

It's also worth noticing the use of the unary plus operator (`+`) to specify a repetition with one or more occurrences.  This is standard EBNF and implemented as perfectly valid Scala within the combinator library.  We could have also used the `rep1(...)` method, but I prefer the operator simply because it is more notational.

Looking back up toward the top of the `Simpletalk` singleton, we see the use of `lexical.reserved`.  We must define the keywords used by our language to avoid the parser marking them as identifiers.  To save time and effort, we're going to implement the interpreter by extending the `StandardTokenParsers` class.  This is nice because we get a lot of functionality for free (such as parsing of string literals), but we also have to make sure that our language is relatively Scala-like in its syntax.  In this case, that is not a problem.

Down the line, we initialize the scanner and use it to parse a result from the hard-coded file, `input.talk`.  It would seem that even if we _wanted_ to use Simpletalk for some useful application, we would have to ensure that the entire script was contained in a single, rigidly defined file relative to the interpreter.

#### Defining the AST

Once the grammar has been created, we must create the classes which will define the abstract syntax tree.  Almost any language defines a grammar which can be logically represented as a tree.  This tree structure is desirable as it is far easier to work with than the raw token stream.  The tree nodes may be manipulated at a high-level in the interpreter, allowing us to implement advanced features like name resolution (which we handle in iteration 3).

At the root of our AST is the statement, or rather, a `List` of statements.  Currently, the only statements we need to be concerned with are `print` and `space`, so we will only need to define two classes to represent them in the AST.  These classes will extend the abstract superclass, `Statement`.

The `Space` class will be fairly simple, as the command requires no arguments.  However, `Print` will need to contain some high-level representation of its greeting.  Since there are only two greetings available and as they are hard-coded into the language, we can safely represent them with separate classes extending a common superclass, `Greeting`.  The full hierarchy looks like this:

```scala
sealed abstract class Statement

case class Print(greeting: Greeting) extends Statement
case class Space extends Statement

sealed abstract class Greeting {
  val text: String
}

case class Hello extends Greeting {
  override val text = "Hello, World!"
}

case class Goodbye extends Greeting {
  override val text = "Farewell, sweet petunia!"
}
```

Believe it or not, this is all we need to represent the language as it stands in tree form.  However, the combinator library does not simply look through the classpath and guess which classes might represent AST nodes, we must explicitly tell it how to convert the results of each parse into a node.  This is done using the `^^` and `^^^` methods:

```scala
def program = stmt+

def stmt = ( "print" ~ greeting ^^ { case _ ~ g => Print(g) }
           | "space" ^^^ Space() )

def greeting = ( "HELLO" ^^^ Hello()
               | "GOODBYE" ^^^ Goodbye() )
```

The `^^^` method takes a parameter as a literal value which will be returned if the parse is successful.  Thus, any time the `space` command is parsed, the result will be an instance of the `Space` class, defined here.  This is nicely compact and efficient, but it does not satisfy all cases.  The `print` command, for example, takes a greeting argument.  To allow for this, we use the `^^` method and pass it a Scala [`PartialFunction`](<http://www.scala-lang.org/docu/files/api/scala/PartialFunction.html>).  The partial function defines a pattern which is matched against the parse result.  If it is successful, then the inner expression is resolved (in this case, `Print(g)`) and the result is returned.  Since the `Parser` defined by the `greeting` method is already defined to return an instance of `Greeting`, we can safely pass the result of this parse as a parameter to the `Print` constructor.  Note that we need not define any node initialization for the `program` terminal as the `+` operator is already defined to return a list of whatever type it encapsulates (in this case, `Statement`).

#### The Interpreter

So far, we have been focused exclusively on the front side of the interpreter: input parsing.  Our parser is now capable of consuming and checking the textual statements from `input.talk` and producing a corresponding AST.  We must now write the code which walks the AST and executes each statement in turn.  The result is a fairly straightforward recursive deconstruction of a list, with each node corresponding to an invocation of `println`.

```scala
class Interpreter(tree: List[Statement]) {
  def run() {
    walkTree(tree)
  }
  
  private def walkTree(tree: List[Statement]) {
    tree match {
      case Print(greeting) :: rest => {
        println(greeting.text)
        walkTree(rest)
      }
      
      case Space() :: rest => {
        println()
        walkTree(rest)
      }
      
      case Nil => ()
    }
  }
}
```

This is where all that work we did constructing the AST begins to pay off.  We don't even have to manually resolve the greeting constants, that can be handled polymorphically within the nodes themselves.  Actually, in a real interpreter, it probably would be best to let the statement nodes handle the actual execution logic, thus enabling the interpreter to merely function as a dispatch core, remaining relatively agnostic of language semantics.

The final piece we need to tie this all together is a bit of logic handling the parse result (assuming it is successful) and transferring it to the interpreter.  We can accomplish this using pattern matching in the `Simpletalk` application:

```scala
result match {
  case Success(tree, _) => new Interpreter(tree).run()
  
  case e: NoSuccess => {
    Console.err.println(e)
    exit(100)
  }
}
```

We could get a bit fancier with our error handling, but in this case it is easiest just to print the error and give up.  The error handling we have is sufficient for our experimental needs.  We can test this with the following input:

```
print errorHere
space
```

The result is the following TeX-like trace:

```
[1.7] failure: ``GOODBYE'' expected but identifier errorHere found

print errorHere

      ^
```

One of the advantages of LL parsing is the parser can automatically generate relatively accurate error messages, just by inspecting the grammar and comparing it to the input.  This is much more difficult with an LR parser, which allows the parse process to consume multiple production rules simultaneously.

Pat yourself on the back!  This is all that is required to wire up a very simple language interpreter.  The full source is linked at the [bottom of the article](<#conclusion>).

### Iteration 2

Now that we have a working language implementation, it's time to expand upon it.  After all, we can't leave things working for long, can we?  For iteration 2, we will add the ability to print arbitrary messages using string and numeric literals, as well as a simple loop construct.  This loop will demonstrate some of the real merits of representing the program as a tree rather than a simple token stream.  As for syntax, we will define an example of these features as follows:

```
print HELLO
print 42

space
repeat 10
  print GOODBYE
next

space
print "Adios!"
```

The result should be the following:

```
Hello, World!
42

Farewell, sweet petunia!
Farewell, sweet petunia!
Farewell, sweet petunia!
Farewell, sweet petunia!
Farewell, sweet petunia!
Farewell, sweet petunia!
Farewell, sweet petunia!
Farewell, sweet petunia!
Farewell, sweet petunia!
Farewell, sweet petunia!

Adios!
```

The first thing we will need to do to support these new features is update our grammar.  This is where we start to see the advantages of using a declarative API like Scala's combinators as opposed to creating the parser by hand.  We will need to change `stmt` terminal (to accept the repeat command) as well as the `greeting` terminal (to allow string and numeric literals):

```scala
def stmt: Parser[Statement] = ( "print" ~ greeting ^^ { case _ ~ g => Print(g) }
                              | "space" ^^^ Space()
                              | "repeat" ~ numericLit ~ (stmt+) ~ "next" ^^ {
                                  case _ ~ times ~ stmts ~ _ => Repeat(times.toInt, stmts)
                                } )

def greeting = ( "HELLO" ^^^ Hello()
               | "GOODBYE" ^^^ Goodbye()
               | stringLit ^^ { case s => Literal(s) }
               | numericLit ^^ { case s => Literal(s) } )
```

Notice that we can no longer rely upon type inference in the `stmt` method, as it is now recursive.  This recursion is in the `repeat` rule, which contains a one-or-more repetition of `stmt`.  This is logical since we want `repeat` to contain other statements, including other instances of `repeat`.  The `repeat` rule also makes use of the `numericLit` terminal.  This is a rule which is defined for us as part of the `StandardTokenParsers`.  Technically, it is more accepting than we want since it will also allow decimals.  However, we don't need to worry about such trivialities.  After all, this is just an experiment, right?

The `numericLit` and `stringLit` terminals are used again in two of the productions for `greeting`.  Both of these parsers resolve to instances of `String`, which we pattern match and encapsulate within a new AST node: `Literal`.  It makes sense for `numericLit` to resolve to `String` because Scala has no way of knowing how our specific language will handle numbers.

These are the new AST classes required to satisfy the language changes:

```scala
case class Repeat(times: Int, stmts: List[Statement]) extends Statement

case class Literal(override val text: String) extends Greeting
```

`Literal` merely needs to encapsulate a `String` resolved directly out of the parse, there is no processing required.  `Repeat`, on the other hand, has a bit more interest to it.  This class contains a list of `Statement`(s), as well as an iteration count, which will define the behavior of the `repeat` when executed.  This is our first example of a truly recursive AST structure. `Repeat` is defined as a subclass of `Statement`, and it contains a `List` of such `Statement`(s).  Thus, it is conceivable that a `Repeat` could contain another instance of `Repeat`, nested within its structure.  This is really the true power of the AST: the ability to represent a recursive grammar in a logical, high-level structure.

Of course, `Interpreter` also must be modified to support these new features; but because of our polymorphic `Greeting` design, we only need to worry about `Repeat`.  This node is easily handled by adding another pattern to our recursive `match`:

```scala
case Repeat(times, stmts) :: rest => {
  for (i <- 0 until times) {
    walkTree(stmts)
  }
  
  walkTree(rest)
}
```

Here we see the primary advantage to leaving the execution processing within `Interpreter` rather than polymorphically farming it out to the AST: direct access to the `walkTree` method.  Logically, each `repeat` statement contains a new Simpletalk program within itself.  Since we already have a method defined to interpret such programs, it only makes sense to use it!  The looping itself can be handled by a simple for-comprehension.  Following the loop, we deconstruct the list and move on to the next statement in the enclosing scope (which could be a loop itself).  This design is extremely flexible and capable of handling the fully recursive nature of our new language.

The only other change we need to make is to add our new keywords to the lexer, so that they are not parsed as identifiers:

```scala
lexical.reserved += ("print", "space", "repeat", "next", "HELLO", "GOODBYE")
```

### Iteration 3

It's time to move onto something moderately advanced.  So far, we've stuck to easy modifications like new structures and extra keywords.  A more complicated task would be the addition of variables and scoping.  For example, we might want a syntax something like this:

```
let y = HELLO

space
print y
let x = 42
print x

space
repeat 10
  let y = GOODBYE
  print y
next

space
print y

space
print "Adios!"
```

And the result:

```
Hello, World!
42

Farewell, sweet petunia!
Farewell, sweet petunia!
Farewell, sweet petunia!
Farewell, sweet petunia!
Farewell, sweet petunia!
Farewell, sweet petunia!
Farewell, sweet petunia!
Farewell, sweet petunia!
Farewell, sweet petunia!
Farewell, sweet petunia!

Hello, World!

Adios!
```

This is significantly more complicated than our previous iterations partially because it requires name resolution (and thus, some semantic analysis), but more importantly because it requires a generalization of expressions.  We already have expressions of a sort in the `Greeting` nodes.  These are not really general-purpose expressions as they do not resolve to a value which can be used in multiple contexts.  This was not previously a problem since we only had one context in which they could be resolved (`print`).  But now, we will need to resolve `Greeting`(s) for both `print` and `let` statements.

We will start with the AST this time.  We need to modify our `Greeting` superclass to allow for more complex resolutions than a static text value.  More than that, these resolutions no longer take place in isolation, but within a variable context (referencing environment).  This context will not be required for `Literal`, `Hello` or `Goodbye` expressions, but it will be essential to handle our new AST: `Variable`.

```scala
sealed abstract class Statement

case class Print(expr: Expression) extends Statement
case class Space extends Statement

case class Repeat(times: Int, stmts: List[Statement]) extends Statement
case class Let(val id: String, val expr: Expression) extends Statement

sealed abstract class Expression {
  def value(context: Context): String
}

case class Literal(text: String) extends Expression {
  override def value(context: Context) = text
}

case class Variable(id: String) extends Expression {
  override def value(context: Context) = {
    context.resolve(id) match {
      case Some(binding) => binding.expr.value(context)
      case None => throw new RuntimeException("Unknown identifier: " + id)
    }
  }
}

case class Hello extends Expression {
  override def value(context: Context) = "Hello, World!"
}

case class Goodbye extends Expression {
  override def value(context: Context) = "Farewell, sweet petunia!"
}
```

Notice that the `Print` and `Let` nodes both accept instances of `Expression`, rather than the old `Greeting` node.  This generalization is quite powerful, allowing variables to be assigned the result of other variables, literals or greetings.  Likewise, the `print` statement may also be used with variables, literals or greetings alike.

Actually, the real meat of the implementation is contained within the `Context` class (not shown).  This data structure will manage the gritty details of resolving variable names into let-bindings (which can then be resolved as expressions).  Additionally, `Context` must deal with all of the problems associated with nested scopes and name shadowing (remember that our motivation example shadows the definition of the `y` variable).

For the moment, we need not concern ourselves with reassignment (mutability).  Redefinition will be allowed, but simplicity in both the grammar and the interpreter calls for fully immutable constants, rather than true variables.  Additionally, this restriction allows our interpreter to easily make use of fully immutable data structures in its implementation.  Scala doesn't really impose this as an implementation requirement, but it's neat to be able to do.  Also, the pure-functional nature of the data structures provide greater assurance as to the correctness of the algorithm.

Here is the full implementation of both `Context` and the modified `Interpreter`:

```scala
class Interpreter(tree: List[Statement]) {
  def run() {
    walkTree(tree, EmptyContext)
  }
  
  private def walkTree(tree: List[Statement], context: Context) {
    tree match {
      case Print(expr) :: rest => {
        println(expr.value(context))
        walkTree(rest, context)
      }
      
      case Space() :: rest => {
        println()
        walkTree(rest, context)
      }
      
      case Repeat(times, stmts) :: rest => {
        for (i <- 0 until times) {
          walkTree(stmts, context.child)
        }
        
        walkTree(rest, context)
      }
      
      case (binding: Let) :: rest => walkTree(rest, context + binding)
      
      case Nil => ()
    }
  }
}

class Context(ids: Map[String, Let], parent: Option[Context]) {
  lazy val child = new Context(Map[String, Let](), Some(this))
  
  def +(binding: Let) = {
    val newIDs = ids + (binding.id -> binding)
    new Context(newIDs, parent)
  }
  
  def resolve(id: String): Option[Let] = {
    if (ids contains id) {
      Some(ids(id))
    } else {
      parent match {
        case Some(c) => c resolve id
        case None => None
      }
    }
  }
}

object EmptyContext extends Context(Map[String, Let](), None)
```

`Context` is really little more than a thin wrapper around an immutable `Map` from `String` to `Let`.  The `resolve` method attempts to resolve a given identifier within the local context.  If that fails, it recursively attempts resolution on the parent context, to which it has a reference.  This procedes all the way up to the root context of the program, which has no parent.  If a resolution still fails at this point, `None` is returned and an error is thrown from within `Interpreter`.

The only other method of significance within `Context` is the `+` operator, which is the means by which new bindings are added to the context.  More specifically, a new context is constructed from the old in which the new binding is present (remember, immutable data).  The `child` value represents a new `Context` which is nested within the current (such as within a `repeat` block).  This can be a constant because of the immutability of the structure.  Thus, a single context may contain many nested contexts; but if none of them contain new bindings, then they will all be the exact same instance.

Jumping up to `Interpreter`, we see how `Context` is actually used.  Because our language lacks forward-referencing, we don't have to worry about multiple passes through the AST.  This simplifies things tremendously, enabling us to handle semantic analysis and interpretation in a single effective phase.  As we walk through the AST, we carry the current `Context` along with us.  Whenever we come across a new binding (a `let` statement), we create a new Context with the binding and recursively pass it to the next node interpretation.  Whenever we come to the definition of a new context (in this case, a `repeat` statement), the `child` value of the current context is retrieved and passed to the interpretation of all nested statements.  This `Context` is discarded once we resume interpretation of the current level.

The final implementation step is to handle the grammar for our new language features.  This is easily done by modifying our existing combinitor definitions.  Additionally, we must add a new keyword ("let") to the list of reserved identifiers, as well as the equals sign ("=") to the list of token delimiters.

```scala
lexical.reserved += ("print", "space", "repeat", "next", "let", "HELLO", "GOODBYE")
lexical.delimiters += ("=")

// ...

def program = stmt+

def stmt: Parser[Statement] = ( "print" ~ expr ^^ { case _ ~ e => Print(e) }
                              | "space" ^^^ Space()
                              | "repeat" ~ numericLit ~ (stmt+) ~ "next" ^^ {
                                  case _ ~ times ~ stmts ~ _ => Repeat(times.toInt, stmts)
                                } 
                              | "let" ~ ident ~ "=" ~ expr ^^ { 
                                  case _ ~ id ~ _ ~ e => Let(id, e) 
                                } )

def expr = ( "HELLO" ^^^ Hello()
           | "GOODBYE" ^^^ Goodbye()
           | stringLit ^^ { case s => Literal(s) }
           | numericLit ^^ { case s => Literal(s) }
           | ident ^^ { case id => Variable(id) } )
```

Once again, the `StandardTokenParsers` gives us a great deal of functionality for free.  In this case, we make use of the `ident` terminal, which represents a Scala-style identifier in our language.  This token occurs in two places: the `let` statement and as a raw expression.  Just like `stringLit` and `numericLit`, `ident` is resolved down to a `String` instance, which we store in `Let` and `Variable` nodes for future use.

### Conclusion

This concludes my introduction to the subtle and fascinating world of language processing.  In truth, I'm rather upset that I had to leave so many things out of the article and that a lot of my explanations were rushed at best.  There's just so much information to fit into so little space!  I strongly suggest that you experiment further with the techniques associated with interpreters and (more interestingly) compilers.  These studies have helped me immeasurably as a programmer, imparting a much deeper understanding of what goes on "under the surface" and why things are the way they are in languages.

  * [Download Iteration 1](<http://www.codecommit.com/blog/misc/language-interpret/iteration1.zip>) as an Eclipse Project 
  * [Download Iteration 2](<http://www.codecommit.com/blog/misc/language-interpret/iteration2.zip>) as an Eclipse Project 
  * [Download Iteration 3](<http://www.codecommit.com/blog/misc/language-interpret/iteration3.zip>) as an Eclipse Project