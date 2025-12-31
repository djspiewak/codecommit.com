---
categories:
- Scala
date: '2008-04-30 00:00:44 '
layout: post
title: 'Useless Hackery: A Scala Quine'
wordpress_id: 225
wordpress_path: /scala/useless-hackery-a-scala-quine
---

**Warning** : This post has little-to-no practical value.  Waste time at your own risk...

While double-checking the terms for [my previous post](<http://www.codecommit.com/blog/java/the-plague-of-polyglotism>), I came across the Wikipedia definition of a [polyglot program](<http://en.wikipedia.org/wiki/Polyglot_\(computing\)>):

> In the context of computing, a **polyglot** is a computer program or script written in a valid form of multiple programming languages, which performs the same operations or output independently of the programming language used to compile or interpret it.

Not precisely the same as the definition which has now come into common use (referring to the _use_ of multiple languages in a single application).  The article goes on to give two examples of a polyglot, one in PHP/C/Bash and one in Haskell/OCaml/Scheme (I don't count the Perl/DOS example since it doesn't perform the same function in both languages).  These examples are quite interesting, but what really caught my eye were the additional properties of the second example: not only is it a polyglot, but it is also a [quine](<http://en.wikipedia.org/wiki/Quine_\(computing\)>):

> In computing, a **quine** is a program, a form of metaprogram, that produces its complete source code as its only output.

Think about that for just a second: A program which produces itself as its only output.  I think that's probably the most profound brain-teaser that I've run across in months.  Consider for a moment just _how_ one would accomplish this.  For example, we could try a naive implementation in Ruby:

```ruby puts "puts \"puts \\\\\"..." ``` 

You'll notice that we have run into a bit of a problem.  In fact, the infinitely recursive nature of the definition is precisely what makes quines so interesting.  Of course, I'm aware that there are already a number of [very clever Ruby quines](<http://wiki.rubygarden.org/Ruby/page/show/RubyQuines>), but that's not the point.  After all, what good is a puzzle if someone else gives you the solution?

By putting a little thought into this, we can devise a slightly more advanced attempt which brings us a bit closer to quine-ness:

```ruby s = "s = \"#{s}\"; puts s" puts s ``` 

We're getting closer, anyway.  We still have a serious problem in that string declaration (hint: it has something to do with the whole recursiveness thing).  We have to somehow include the string within itself once explicitly, but on the inner recursion only include a textual _reference_ to itself.  This is by no means trivial to accomplish.

One technique we can employ is string formatting.  Old C salts will certainly be familiar with the `printf` function.  There's a clever little trick we can employ which allows us to format a string using _itself_ as the format string.  This is one way to provide single-level recursion in the string resolution:

```c char *s = "char *s = \"%s\"; printf(s, s);"; printf(s, s); ``` 

Note that I'm cheating a bit on the formatting to make things more readable.  There's really nothing preventing this sample from formatting a bit more correctly (newline, etc).

We're almost there now.  Our only remaining problem is the fact that the second recursion of the string will have improperly quoted double-quotes.  [Gary Thompson shows](<http://www.nyx.net/~gthompso/quine.htm>) a fully fleshed-out C quine which gets around this problem by exploiting the `int`/`char` duality in the language.  However, this little trick isn't precisely available in languages like Scala.  Well, it is, but there are problems with the `printf` formatting which obviate the possibility.  Specifically, Scala's `printf` method does not allow for the standard `%s`-style formatting (even though the [scaladoc](<http://www.scala-lang.org/docu/files/api/scala/Console$object.html#printf%28String%2CAny*%29>) claims that it does).  All that this function allows us are simple substitutions, but it turns out that this is enough to (finally) complete our quine in Scala (formatted for easy reading):

```scala object Q extends Application { val s = "object Q extends Application'{'val s={0}{2}{0};printf(s,{0}{1}{0}{0},{0}{1}{1}{0},s)}" printf(s, "\"", "\\\", s) } ``` 

Even with the reformatting, the second line still overflows the formatting on most browsers (sorry about that).  I've uploaded the unformatted, "true quine" [here](<http://www.codecommit.com/blog/misc/quine.scala>).

It's somewhat interesting that Scala's syntax is concise enough (especially with type inference) that this sort of thing is possible in only **149** characters.  If you look around for Java quines (there are a few of them), you'll see that most of them take a similar approach, but they usually have trouble with the encumbrances of Java's highly-verbose syntax.  It's sort-of depressing to condense your code when you have to type "`public static void main`" regardless.

Anyway, I'm certainly not an expert on the ins-and-outs of the Scala library.  Suggestions welcome on how to golf this down a bit.   Or better yet, an extremely clever solution which eluded me entirely.