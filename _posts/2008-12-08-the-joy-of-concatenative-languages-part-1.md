---
categories:
- Cat
date: '2008-12-08 01:00:00 '
layout: post
title: The Joy of Concatenative Languages Part 1
wordpress_id: 275
wordpress_path: /cat/the-joy-of-concatenative-languages-part-1
---

Concatenative languages like Forth have been around for a long time.  Hewlett-Packard famously employed a stack-based language called "RPL" on their HP-28 and HP-48 calculators, bringing the concept of Reverse Polish Notation to the mainstream...or as close to the mainstream as a really geeky toy can get.  Surprisingly though, these languages have not seen serious adoption beyond the experimental and embedded device realms.  And by "adoption", I mean real programmers writing real code, not this whole [interpreted bytecode nonsense](<http://en.wikipedia.org/wiki/JVM>).

This is a shame, because stack-based languages have a remarkable number of things to teach us.  Their superficial distinction from conventional programming languages very quickly gives way to a deep connection, particularly with functional languages.  However, if we dig even deeper, we find that this similarity has its limits.  There are some truly profound nuggets of truth waiting to be uncovered within these murky depths.  Shall we?

_**Trivial aside:** I'm going to use the terms "concatenative" and "stack-based" interchangeably through the article.  While these are most definitely related concepts, they are not exactly synonyms.  Bear that in mind if you read anything more in-depth on the subject._

### The Basics

Before we look at some of those "deeper truths of which I speak, it might be helpful to at least understand the fundamentals of stack-based programming.  From Wikipedia:

> The concatenative or stack-based programming languages are ones in which the concatenation of two pieces of code expresses the composition of the functions they express. These languages use a stack to store the arguments and return values of operations.

Er, right.  I didn't find that very helpful either.  Let's try again...

Stack-based programming languages all share a common element: an operand stack.  Consider the following program:

```cat 2 ``` 

Yes, this is a real program.  You can copy this code and run/compile it unmodified using most stack-based languages.  However, for reasons which will become clear later in this series, I will be using [Cat](<http://www.cat-language.com>) for most of my examples.  Joy and Factor would both work well for the first two parts, but for part three we're going to need some rather unique features.

Returning to our example: all this will do is take the numeric value of `2` and push it onto the operand stack.  Since there are no further _words_ , the program will exit.  If we want, we can try something a little more interesting:

```cat 2 3 + ``` 

This program first pushes 2 onto the stack, then 3, and finally it pops the top two values off of the stack, adds them together and pushes the result.  Thus, when this program exits, the stack will only contain `5`.

We can mix and match these operations until we're blue in the face, but it's still not a terribly interesting language.  What we really need is some sort of flow control.  To do that, we need to understand quotations.  Consider the following Scala program:

```scala val plus = { (x: Int, y: Int) => x + y } plus(2, 3) ``` 

Notice how rather than directly adding `2` and `3`, we first create a closure/lambda which encapsulates the operation.  We then invoke this closure, passing `2` and `3` as arguments.  We can emulate these exact semantics in Cat:

```cat 2 3 [ + ] apply ``` 

The first line pushes `2` and `3` onto the stack.  The second line uses square brackets to define a quotation, which is Cat's version of a lambda.  Note that it isn't really a closure since there are no variables to en _close_.  Joy and Factor also share this construct.  Within the quotation we have a single word: `+`.  The important thing is the quotation itself is what is put on the stack; the `+` word is not immediately executed.  This is exactly how we declared `plus` in Scala.

The final line invokes the `apply` word.  When this executes, it pops one value off the stack (which must be a quotation).  It then executes this quotation, giving it access to the current stack.  Since the quotation on the head of the stack consists of a single word, `+`, executing it will result in the next two elements being popped off (`2` and `3`) and the result (`5`) being pushed on.  Exactly the same result as the earlier example and the exact same semantics as the Scala example, but a lot more concise.

Cat also provides a number of primitive operations which perform their dirty work directly on the stack.  These operations are what make it possible to reasonably perform tasks without variables.  The most important operations are as follows:

  * `swap` — exchanges the top two elements on the stack.  Thus, `2 3 swap` results in a stack of "`3 2`" in that order.
  * `pop` — drops the first element of the stack.
  * `dup` — duplicates the first element and pushes the result onto the stack.  Thus, `2 dup` results in a stack of "`2 2`".
  * `dip` — pops a quotation off the stack, temporarily removes the next item, executes the quotation against the remaining stack and then pushes the old head back on.  Thus, `2 3 1 [ + ] dip` results in a stack of "`5 1`".

There are other primitives, but these are the big four.  It is possible to emulate any control structure (such as `if`/`then`) just using the language shown so far.  However, to do so would be pretty ugly and not very useful.  Cat does provide some other operations to make life a little more interesting.  Most significantly: functions and conditionals.  A function is defined in the following way:

```cat define plus { + } ``` 

Those coming from a programming background involving variables (that would be just about all of us) would probably look at this function and feel as if something is missing.  The odd part of this is there is no need to declare parameters, all operands are on the stack anyway, so there's no need to pass anything around explicitly.  This is part of why concatenative languages are so extraordinarily concise.

Conditionals also look quite weird at first glance, but under the surface they are profoundly elegant:

```cat 2 3 plus // invoke the `plus` function 10 < [ 0 ] [ 42 ] if ``` 

Naturally enough, this code pushes `0` onto the stack.  The conditional for an `if` is just a boolean value pushed onto the stack.  On top of that value, `if` will expect to find two quotations, one for the "then" branch and the other for the "else" branch.  Since `5` is less than `10`, the boolean value will be `True`.  The `if` function (and it could just as easily be a function) pops the quotations off of the stack as well as the boolean.  Since the value is `True`, it discards the second quotation and executes the first, producing `0` on the stack.

I'll leave you with the more complicated example of the factorial function:

```cat define fac { dup 0 eq [ pop 1 ] [ dup 1 - fac * ] if } ``` 

Note that this isn't even the most concise way of writing this, but it does the job.  To see how, let's look at how this will execute word-by-word (assuming an input of `4`):

**Stack** | **Word**  
---|---  
`4` | `dup`  
`4 4` | `0`  
`4 4 0` | `eq`  
`4 False` | `[ pop 1 ]`  
`4 False [pop 1]` | `[ dup 1 - fac * ]`  
`4 False [pop 1] [dup 1 - fac *]` | `if`  
`4` | `dup`  
`4 4` | `1`  
`4 4 1` | `-`  
`4 3` | `fac` _(assume magic recursion)_  
`4 6` | `*`  
  
The final result is `24`, a value which is left on the stack.  Pretty nifty, eh?

### Conclusion

You'll notice this is a shorter post than I usually spew forth (no pun intended...this time).  The reason being that I want this to be fairly easy to digest.  Concatenative languages (and Cat in particular) are not all that difficult to digest.  They are a slightly different way of thinking about programming, but as [we will see in the next part](<http://www.codecommit.com/blog/cat/the-joy-of-concatenative-languages-part-2>), not so different as it would seem.

**Note:** Cat is written in C# and is available under the MIT License.  Don't fear the CLR though, Cat runs just fine under Mono.  If you really want to experiment with no risk to yourself, a Javascript interpreter is available.

  * [Cat Language Website](<http://www.cat-language.com>)
  * [Interactive Javascript Interpreter for Cat](<http://www.cat-language.com/interpreter.html>)