---
categories:
- Scala
date: '2008-05-26 00:00:14 '
layout: post
title: Naïve Text Parsing in Scala
wordpress_id: 232
wordpress_path: /scala/naive-text-parsing-in-scala
---

One of the truly incredible things about Scala is that it really inspires people to consider problems that they never would have attempted before.  Recently, the urge came upon me to try my hand at some more advanced text processing.  Not quite so advanced as a full language, but more complicated than can be easily handled by regular expressions.  As usual, Scala proved more than up to the challenge.

All of us have evolved more-or-less ad hoc methods for handling simple text processing.  In this modern age, it's almost a requirement to be familiar with basic regular expressions, or at the very least `split`, `subString` and `find`.  These techniques tend to work well in small-scale applications, but things become a bit muddled when trying to deal with more complex or conditional representations.  It gets even worse when you must perform some sort of complex resolution on the results of your parsing, requiring you to devise an intermediate form.

One such example of a more complicated text parse would be the C-style `printf` function.  Java has had this functionality since 1.5 in the form of `PrintStream#printf(...)` as well as `String.format(String, Object...)`; but unfortunately, Scala lacks this highly useful method.  Oh, it has a `printf` function, but it [doesn't support C-style syntax](<http://www.nabble.com/Console.printf-is-misleading!-td16359307.html>) for reasons of backwards compatibility.  This caused me no end of grief when I was trying to [construct a quine in Scala](<http://www.codecommit.com/blog/scala/useless-hackery-a-scala-quine>).  Since Scala has no `printf`, I decided to try my hand at implementing one (just for kicks).

### Finite State Machines

As I said, the ad hoc parsing techniques may serve us well when we're just trying to split a full name into a firstname/lastname tuple, but I'm afraid that `printf` requires a more disciplined approach.  Fortunately, there are a number of beautiful formalisms for dealing with text parsing.  Chief among these are deterministic finite state machines.

If you took formal language theory in college, you've probably already worked with DFAs (Deterministic Finite Automata), NFAs (Non-deterministic Finite Automata) and PDAs (Pushdown Automata); but since everyone I know slept through that class, I'll just assume that you did too and go over some of the basics again.  Finite state machines (automata) are at the core of [Turing's](<http://en.wikipedia.org/wiki/Alan_Turing>) seminal thesis on computability.  Actually, the so-called "Turing Machine" is at the core of his work, but DFAs are really just a limited form of this concept.

The ideas behind the acronyms are very simple: a finite state machine is a collection of "states" which have connections to each other which dictate ensuing states or termination of the execution.  The most common representation of a DFA is a directed graph.  The states are represented by the vertices of the graph.  The double-circle indicates an "accepting state".

 ![image](/assets/images/blog/wp-content/uploads/2008/05/image1.png)

This is a simple DFA which has four accepting states ( **4** , **6** , **7** and **8** ).  There is also a loop transition on state **3**.  Each of these states represents a different position (or "state", hence the term) in the parse process.  The idea is that you consume just one character at a time and based on the character value, the automaton "chooses" the correct transition.  It's all very mindless, very sequential (hence the name "automaton").

The only problem here is there may not be a transition for _every_ possible character.  For example, starting from state **1** , we know how to handle characters `a`, `b`, `c`, `d` and `g`, but what happens if we actually get an `s` or a `7`?  By some definitions, this failing would indicate that we have an invalid DFA, something which is obviously bad.  Most representations however allow unsatisfied input and merely have an _implicit_ transition to an accepting error state.  Most common applications make use of this rule (we'll get to that in a minute).

If you execute the given automaton, you will find that it accepts the following inputs:

  1. `ban`
  2. `aa120m`
  3. `da1o`
  4. `gs`
  5. `ga`

...but rejects (or errors) on these alternative sequences:

  1. `aaa7`
  2. `da6mn`
  3. `gq88`

These claims are fairly easy to verify by mentally consuming each character in turn and transitioning to the corresponding state (if any).  Thus, for the first series of inputs, the state sequences will be as follows:

  1. **1** , **2** , **3** , **4**
  2. **1** , **2** , **3** , **3** , **3** , **3** , **4**
  3. **1** , **2** , **3** , **3** , **7**
  4. **1** , **5** , **6**
  5. **1** , **5** , **8**

Notice how every parse starts with the initial state ( **1** ).  This may seem sort of academic (since the parse information is all encoded in the transitions), but it turns out that without this formalism, many common every-day tasks which we take for granted would be impossible.

If you look closely at my example, you'll notice that you can very easily encode the same accept/reject information using a regular expression:

``` [a-d]a[0-9]*([nm]|[a-lo-z])|g([n-z][0-9]?|[abc]) ``` 

Ok, so maybe that's not the easiest connection to make, but I think you get the picture.  As it turns out, regular expressions are a direct textual representation of deterministic finite state automata.  In fact, algorithms for executing regular expressions compile the regular expression into a DFA (using various techniques) and then execute this DFA against the input.  It does require an intervening step to convert from a regular expression to a DFA, but it's not that difficult to do.

### The printf Case Study

Now that I've managed to lull all of you to sleep, it's time to get back to more practical matters.  All of this formal theory actually has some very down-to-earth applications, including the algorithms required to implement `printf`.

C-style `printf` has a fairly flexible syntax which allows not only simple substitutions, but also type-dependent formatting, padding and truncation.  For example, we can do something like this in Java:

```java double pi = 3.14159; System.out.printf("My favorite number: %n%80.2f", pi); ``` 

The result looks like this (including all the space):

``` My favorite number: 3.14 ``` 

There are a number of different conversions available, denoted by the letter trailing the escape - in this case, `n` and `f` respectively.  There are also a large number of flags which can be used, the capability to specify the argument index, etc.  Altogether, the [context-free grammar](<http://en.wikipedia.org/wiki/Context-free_grammar>) for this format looks like this ([source](<http://java.sun.com/javase/6/docs/api/java/util/Formatter.html>)):

``` format ::= '%' index flags width precision conversion index ::= INTEGER '$' | '<' | ε flags ::= flags flag | ε flag ::= '-' | '#' | '+' | ' ' | '0' | ',' | '(' width ::= INTEGER | ε precision ::= '.' INTEGER | ε conversion ::= 'b' | 'B' | 'h' | 'H' | 's' | 'S' | 'c' | 'C' | 'd' | 'o' | 'x' | 'X' | 'E' | 'E' | 'f' | 'g' | 'G' | 'a' | 'A' | ( 't' | 'T' ) date_format | '%' | 'n' date_format ::= ... ``` 

I have omitted the grammar for date formatting just for the sake of simplicity.  The epsilon (ε) symbolizes the empty string (`""`).  In case you found the above confusing, this is a (slightly) more human-readable variant:

``` %[argument_index$][flags][width][.precision]conversion ``` 

Essentially, this boils down to the following: A substitution format is escaped by a percent sign (%) followed by an optional index, flags, width and precision, as well as a mandatory conversion indicator.  The date/time conversion is special and takes a series of formatting parameters immediately trailing the conversion.  For the sake of sanity, our parser implementation will ignore this inconvenient fact.

We could take this CFG (context-free grammar) and feed it into a parser generator (such as the one [built into Scala](<http://debasishg.blogspot.com/2008/04/external-dsls-made-easy-with-scala.html>)) and generate an AST in that way.  However, in this particular instance there is no need.  A cursory glance at the grammar indicates that there is no case wherein the syntax is self-recursive.  A minor exception to this is the `flags` terminal, but this is really just a way of expressing a repetition in [BNF](<http://en.wikipedia.org/wiki/Backus%E2%80%93Naur_form>)-ish style.  A moment's reflection will lead us to the (correct) conclusion that because the grammar is non-recursive, it can also be represented as a regular expression - thus, a DFA.  In fact, you can prove this point, but that bit of academic trivia is unimportant for the moment.

What _is_ important is to realize that as this grammar is expressible in the form of a DFA, we can actually write code which parses it without too much trouble.  Parsers can (and have) been written by hand, but usually when the grammar gets complex, the parser reflects this exponentially.  While it is not difficult to write a simple PDA by hand, doing so would be overkill.  So rather than starting with the BNF grammar and creating a literal representation, we will work off of the one-line informal syntax and produce a stackless automaton (the defining feature of a pushdown automaton is the use of a stack to maintain recursive state).

### Implementation

As it turns out, all of this gobbly-gook expresses very elegantly in functional languages.  In truth, I could have written the parser in ML, but it is much more fun to use Scala.  We start out by considering how we want the intermediate form to be expressed.  Since we're not writing a true compiler, we don't need to worry about serializing this IF into anything persistent; we can rely entirely on memory state.  For a more complicated grammar, we might write classes to represent a tree structure (commonly referred to as an [AST](<http://en.wikipedia.org/wiki/Abstract_syntax_tree>)).  However, because `printf` escapes are so straightforward, we can simply generate a token stream.  We will represent this as `List[Token]` using the following definitions.

```scala sealed abstract class Token case class CharToken(token: Char) extends Token case class FormatToken(index: Index, flags: Set[Flag.Value], width: Option[Int], precision: Option[Int], format: Char) extends Token sealed abstract class Index case class Value(index: Int) extends Index case class Previous extends Index case class Default extends Index object Flag extends Enumeration { val LEFT_JUSTIFIED, ALTERNATE, SIGNED, LEADING_SPACE, ZERO_PADDED, GROUP_SEPARATED, NEGATIVE_PAREN = Value } val flagMap = { import Flag._ Map('-' -> LEFT_JUSTIFIED, '#' -> ALTERNATE, '+' -> SIGNED, ' ' -> LEADING_SPACE, '0' -> ZERO_PADDED, ',' -> GROUP_SEPARATED, '(' -> NEGATIVE_PAREN) } ``` 

Note that `Option` is insufficient to represent `index` because of the `<` escape (use previous index).  Thus, we define a separate series of types with three alternatives: `Value`, `Previous` and `Default`).  This is similar to `Option`, but more specific to our needs.  Finally, the flags are represented as an enumeration.  Scala doesn't have language-level support for enumerations, so the syntax ends up being a fair-bit more verbose than the equivalent Java.  It is for this reason that enumerations aren't used very much in Scala, instead preferring sealed case classes and object modules (to serve as the namespace).

Our parser will have to consume the entire format string, including non-escapes.  The final representation will be an immutable list of `Token`(s), either `CharToken` for a single run-of-the-mill character, or `FormatToken` which will represent the fully-parsed substitution.  Thus, for the `printf` example [given above](<#printf-example>) (my favorite number), the token stream will look something like this:

```scala CharToken('M') :: CharToken('y') :: CharToken(' ') :: /* ... */ :: FormatToken(Default(), Set(), None, None, 'n') :: FormatToken(Default(), Set(), Some(80), Some(2), 'f') :: Nil ``` 

_For those of you unfamiliar with the cons operator (_`::` _), it is just about the most useful functional idiom known to exist, especially in conjunction with pattern matching.   All it does is construct a new linked list with the value on the left as the head and the_ list _to the right as the tail.   _`Nil` _is the empty list and thus commonly serves as the tail of a compound cons expression._

To produce this token stream, we will need to write an automaton which consumes each character in the stream and inspects it to see if it marks the beginning of a substitution.  If not, then a `CharToken` should be generated and put in the list.  However, if the character does mark an escape, then the automaton should transition to a different branch, consuming characters as necessary and walking through the algorithmic representation of our one-line syntax.  It is possible to diagram the necessary automaton, but to do so would be both pointless and unhelpful.  It's probably easier just to dive into the code:

```scala type Input = ()=>Option[Char] def parse(stream: Input): List[Token] = { stream() match { case Some('%') => parseIndex1(stream) :: parse(stream) case Some(x) => CharToken(x) :: parse(stream) case None => Nil } } ``` 

Rather than trying to efficiently walk through a proper `String` instance, it is easier to deal with a single-character stream.  The `Input` type alias defines a function value which will return `Some(x)` for the next character `x` in the string, or `None` if the end of the string has been reached.  It's like a type-safe EOF.  We will call this method in the following way:

```scala var index = -1 val tokens = parse(() => { index = (index + 1) if (index < pattern.length) Some(pattern(index)) else None }) ``` 

Our cute use of mutable state (`index`) makes this code far more concise than it would have been had we attempted to do things functionally.  As it turns out, this is the only place is in our parser where we need to maintain state which is not on the stack.  Because no lookahead is required, we can simply march blindly through the syntax, consuming every character we come across and transitioning to a corresponding state.

The first state of our automaton is represented by the `parse(Input)` method.  It has a transition to a normal character-consuming state, which in turn transitions back to `parse` (`CharToken(x)` cons'd with the recursive evaluation).  Our first state also has a transition to a more complicated state represented by `parseIndex1(Input)`.  This transition takes place whenever we consume a percent (%) character.  What happens next is much easier to explain with code than in words ( **warning:** 90-line snippet):

```scala def parseIndex1(stream: Input) = stream() match { case Some(c) => { if (flagMap contains c) { parseFlags(stream, Default(), Set(flagMap(c))) } else if (c == '<') { parseFlags(stream, Previous(), Set[Flag.Value]()) } else if (c == '.') { parsePrecision(stream, Default(), Set[Flag.Value](), None, 0) } else if (Character.isDigit(c)) { parseIndex2(stream, Character.digit(c, 10)) } else { parseFormat(stream, Default(), Set[Flag.Value](), None, None, c) } } case None => throw new InvalidFormatException("Unexpected end of parse stream") } def parseIndex2(stream: Input, value: Int): FormatToken = stream() match { case Some(c) => { lazy val index = if (value == 0) Default() else Value(value) lazy val width = if (value == 0) None else Some(value) if (c == '.') { parsePrecision(stream, Default(), Set[Flag.Value](), width, 0) } else if (c == '$') { parseFlags(stream, index, Set[Flag.Value]()) } else if (Character.isDigit(c)) { parseIndex2(stream, (value * 10) + Character.digit(c, 10)) } else { parseFormat(stream, Default(), Set[Flag.Value](), width, None, c) } } case None => throw new InvalidFormatException("Unexpected end of parse stream") } def parseFlags(stream: Input, index: Index, flags: Set[Flag.Value]): FormatToken = stream() match { case Some(c) => { if (flagMap contains c) { parseFlags(stream, index, flags + flagMap(c)) } else if (c == '.') { parsePrecision(stream, index, flags, None, 0) } else if (Character.isDigit(c)) { parseWidth(stream, index, flags, Character.digit(c, 10)) } else { parseFormat(stream, index, flags, None, None, c) } } case None => throw new InvalidFormatException("Unexpected end of parse stream") } def parseWidth(stream: Input, index: Index, flags: Set[Flag.Value], value: Int): FormatToken = stream() match { case Some(c) => { lazy val width = if (value == 0) None else Some(value) if (c == '.') { parsePrecision(stream, index, flags, width, 0) } else if (Character.isDigit(c)) { parseWidth(stream, index, flags, (value * 10) + Character.digit(c, 10)) } else { parseFormat(stream, index, flags, width, None, c) } } case None => throw new InvalidFormatException("Unexpected end of parse stream") } def parsePrecision(stream: Input, index: Index, flags: Set[Flag.Value], width: Option[Int], value: Int): FormatToken = stream() match { case Some(c) => { lazy val precision = if (value == 0) None else Some(value) if (Character.isDigit(c)) { parsePrecision(stream, index, flags, width, (value * 10) + Character.digit(c, 10)) } else { parseFormat(stream, index, flags, width, precision, c) } } case None => throw new InvalidFormatException("Unexpected end of parse stream") } def parseFormat(stream: Input, index: Index, flags: Set[Flag.Value], width: Option[Int], precision: Option[Int], c: Char) = { FormatToken(index, flags, width, precision, c) } ``` 

If you can get past the sheer volume of code here, it actually turns out to be pretty simple.  Each method represents a single state.  Some of these states have loop transitions (so as to consume multi-digit precisions, for example), but for the most part, flow travels smoothly from each state to the next.  The transitions are defined by the `if`/`else if`/`else` expressions within each method.  Note that due to the fact that every `if` statement has a corresponding `else`, we are allowed to treat them has expressions with a proper value and thus avoid the use of any explicit `return`s (improving the conciseness of the code).

The final state is represented by the `parseFormat(...)` method.  This method constructs a `FormatToken` based on the accumulated values and then returns, unwinding our long and recursive automaton branch all the way back to the `parse` method, which places our token in the list and moves on.  Simple and to the point.

#### Tail Recursion

As a side-bonus, it is possible to rewrite the `parse` method so that it is tail recursive, allowing the Scala compiler to overwrite each stack frame with its successor.  Some of the substitution state methods are already tail recursive, but these loop far less frequently than `parse`.  In fact, if we don't write a tail recursive parser, we will be unable to handle large strings due to stack overflow.

The tail recursive form of `parse` is nowhere near as elegant, but it gets the job done.  Like most tail recursive methods, it makes use of an accumulator which is passed from each call to the next.  So rather than parsing the tokens recursively and then constructing the list as we pop back up the stack, we construct the list as we go and return the completed value at the end.  The only problem with this is that cons prepends elements to the list.  This means that when we finally return the accumulated list, it will be the exact inverse of what we want.  Thus, we must must explicitly reverse the list at the termination of the character stream.  This actually means that the tail recursive form will require more bytecode instructions than the original, but it will execute more efficiently due to the local elimination of the stack (effectively, scalac will collapse the method into a `while` loop at compile-time).

```scala def parse(stream: Input) = tailParse(stream, Nil) def tailParse(stream: Input, back: List[Token]): List[Token] = { stream() match { case Some('%') => tailParse(stream, parseIndex1(stream) :: back) case Some(x) => tailParse(stream, CharToken(x) :: back) case None => back.reverse } } ``` 

### Conclusion

Hopefully, this has been a thoroughly enjoyable visit to the land of parsing and formal language theory (I had fun anyway).  As usual, Scala proves itself to be an extremely expressive language, capable of representing both the theoretical and the practical with ease.  It almost makes me want to write a more complicated parser by hand, just to see how well Scala handles it.

I'm not entirely sure what I want to do with the result.  As I mentioned, Scala needs an in-language implementation of `printf`, so maybe I'll flesh out the implementation some more and submit a patch.  The unfortunate problem with this is `printf` is more than just a parser.  We can't just take our token stream and pipe it to stdout, hoping for an epsilon transition.  As it turns out, walking this token stream and formatting the substitutions proves to be a very ugly, very tedious task.  I've already implemented most of the core substitution functionality, but a lot of the more complicated stuff remains undone.  If anyone's interested in the full sources + a [BDD specification](<http://code.google.com/p/specs>) for `printf`, just let me know.  :-)

Parsing is a very interesting science with a world of representations and experiences to draw upon.  Even for simple grammars like `printf`, many lessons can be learned about the fundamentals of computing and just what constitutes a language.  And what better language to use in learning these lessons than Scala?

**Update:** Public interest seems to be high enough to merit uploading the full project.  You should be able to download using the link below (project managed with Buildr).

  * [Download scala_printf sources](<http://www.codecommit.com/blog/misc/scala_printf.zip>)