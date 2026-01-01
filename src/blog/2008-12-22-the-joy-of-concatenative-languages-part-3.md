{%
laika.title = "The Joy of Concatenative Languages Part 3: Kindly Types"
laika.metadata.date = "2008-12-22"
%}


# The Joy of Concatenative Languages Part 3: Kindly Types

In parts [one](<http://www.codecommit.com/blog/cat/the-joy-of-concatenative-languages-part-1>) and [two](<http://www.codecommit.com/blog/cat/the-joy-of-concatenative-languages-part-2>) of this series, we dipped our toes into the fascinating world that is stack-based languages.  By this point, you should be fairly familiar with how to construct simple algorithms using Cat (the language we have been working with) as well as the core terminology of the paradigm.  In fact, with just the information given so far, you could probably go on to be productive with a real-world concatenative language like Factor.  However, the interest does not just stop there...

One of the interesting challenges in programming language design is the construction of a type system.  So as to clear up any possible misconception _before_ it arises, this is how Pierce defines such a thing:

> A type system is a tractable syntactic method for proving the absence of certain program behaviors by classifying phrases according to the kinds of values they compute.

For Java, which has a comparatively weak type system, this usually means preventing you from accidentally using a `String` as if it were an `int`.  In other words, Java's type system generally proves the absence of things like `NoSuchMethodError` and similar.  C#, which has a slightly more-powerful type system, can also prove the absence of most `NullPointerException`(s) when code is written in a correct and idiomatic fashion.  Scala goes even further with pattern matching...need I go on?  The point is that type systems do different things in different languages, so the definition needs to be flexible enough to reflect that.

In this article, we're going to look at how we can define a type system for a functional (meaning that we have quotations) concatenative language.  In [a comment on the first part of this series](<http://www.codecommit.com/blog/cat/the-joy-of-concatenative-languages-part-1#comment-4376>), it was suggested that the task of typing stack-based languages is a fairly trivial one.  This is true, but only to a certain point.  As we will see, there are dragons lurking in the conceptual shadows, waiting for us to disturb their sleep.

### Simple Expressions

Let's start out with typing something simple.  Consider the following program:

```cat

```

For those of you reading the RSS, what you see between the previous paragraph and this is exactly what I intended to write: nothing at all.  In a concatenative language, the empty program is usually considered to be valid.  After all, it takes a stack as input and returns the _exact_ same stack.  We could replicate the semantics of this program by writing "`dup pop`", but why bother?

The empty program has the following type:

```
(->)
```

Or, more properly:

```
('A -> 'A)
```

To the left of the `-<` we have what I like to call the "input constraints": what types must be on the stack coming into the program (or phrase).  To the right of the arrow are the "output constraints": what types will be on the stack when we're done.  For reasons which will become clear later on, `'A` in this case represents the whole input stack (regardless of what it contains).  Since we never change anything on the stack (the program is, after all, empty), the output stack has whatever type the input stack was given.  Another way of writing this type would be as follows:

```
* -> *
```

This literally symbolizes our intuition that the empty program has no input _or_ output constraints.  However, this is somewhat less correct notationally since it implies that the input and output stacks are unrelated.  In fact, I would go so far as to say that this notation is _wrong_.  The only reason it is produced here is to serve as a memory aid.  For the remainder of the article, we will be using Cat's notation for types.

Let's look at something a little less trivial.  Consider the following program one word at a time:

```cat
1 2 +
```

Remember that an integer literal (or any literal for that matter) is just a function which pushes a specific constant onto the stack.  Let's assign types based on what we _expect_ the input/output constraints of these functions to be.  **Note:** I will be using the colon (`:`) notation to denote a type.  This isn't conventional coming from C-land, but it is the gold standard of formal type theory:

```
1 : ('A -> 'A Int)
2 : ('A -> 'A Int)
+ : ('A Int Int -> 'A Int)
```

This is all very intuitive.  Integer literals work on any stack and just produce that stack with a new `Int` pushed onto the top.  Both `1` and `2` have the same type, which is a good sign that we're on the right track.

The `+` word is a little more interesting.  Its runtime semantics are as follows: pop two integers off the stack, add them together and then push the result back on.  This word will not be able to execute without _both_ integer values on the top of the stack.  Thus, it only makes sense that its input constraints be some stack with two values of type `Int` at the top.  Likewise, when we're done, those two integers will be gone and a new `Int` will be pushed onto the remainder of the stack which was given to us.  Remember that `'A` represents _any_ stack, even if it is completely empty.

Coming back to our program, we can see that it is well typed by simply string together the types we have generated.  Starting from the top (using `*` to symbolize the empty stack):

**Word** | **Input Stack** | **Output Stack**  
---|---|---  
`1` | `*` | `* Int`  
`2` | `* Int` | `* Int Int`  
`+` | `* Int Int` | `* Int`  
  
Do you see how the input stack of each word matches the output stack of the previous?  In this case, this sort of one-to-one matching indicates that the program is well-typed, producing a final stack with a single `Int` on it.  If we actually run this program, we would see that the evaluation matches the assigned types.

### First-Order Functions

This is fine for a simple addition program, but what if we throw functions into the mix?  Consider the same program we just analyzed wrapped up within a function:

```cat
define addSome {
  1 2 +
}

addSome
```

Here we define a function which has as a body the program we have already analyzed.  Down at the bottom of our new program, we actually call this function.  Here is the question: what type does the `addSome` word have?

To answer this question, look back at the table above and consider the **Input Stack** for the first word in concert with the **Output Stack** for the last.  Putting these two types together yields the following type for the aggregated whole:

```
1 2 + : ('A -> 'A Int)
```

These words (or "phrase") takes any stack as input, and then through some manipulation produces a single `Int` on top of that stack as a result.  The stack may grow and shrink within the function, but at the end of the day, only the `Int` remains.  As we would expect, this matches the runtime semantics perfectly.

Given the fact that the phrase "`1 2 +`" has the type `('A -> 'A Int)`, it is reasonable to assign that same type to the function which contains it.  Thus, we can type-check the `addSome` program in a simple, one-row table:

**Word** | **Input Stack** | **Output Stack**  
---|---|---  
`addSome` | `*` | `* Int`  
  
At the start of execution, the input stack to any program is `*`, or the empty stack.  However, this is fine with our type checker, since the program has `'A` — or any stack — for its input parameters.

This is all so nice and intuitive, so let's consider the case where we have a function which actually takes some parameters.  Specifically, let's consider the following definition:

```cat
define addTwice {
  + +
}
```

At runtime, this function will take three values off the stack and then add them all together.  It is the Cat equivalent of the following in Scala:

```scala
def addTwice(a: Int, b: Int, c: Int) = a + b + c
```

The question is: how do we assign this (the Cat function) a type?  As we have done before, let's look at the types of the individual words:

```
+ : ('A Int Int -> 'A Int)
+ : ('A Int Int -> 'A Int)
```

Not much help there.  Let's try making a table:

**Word** | **Input Stack** | **Output Stack**  
---|---|---  
`+` | `* Int Int` | `* Int`  
`+` | `* Int Int` | `* Int`  
  
It's tempting to look at this and just assign `addTwice` the type of `('A Int Int -> 'A Int)`.  However, this would be a mistake.  Notice the problem with our table above: the **Input Stack** type of the second word does not match the **Output Stack** of the first.  In other words, this program does not immediately type-check.

The problem is the second word is accessing more of the stack than the first.  We're effectively "deferring" a parameter access until later in the function, rather than grabbing everything right away and threading the processing through from start to finish.  This is a perfectly reasonable pattern, but it plays havoc with our naive type system.

The solution is to merge the input constraints across both words.  The first word (`+`) requires two `Int`(s) to be on the top of the stack.  When it is done, those `Int`(s) are gone and a single `Int` has taken their place.  The second word (again `+`) _also_ requires two `Int`(s) on the stack.  We only have one that we know of (the output `Int` from the first word), so we must unify the constraints and merge things back "up the chain" as it were.  In other words, our first word (`+`) will require not just two `Int`(s) on the stack but _three_ : two for itself and one for the second word (`+`).  Our corrected table will look something like the following:

**Word** | **Input Stack** | **Output Stack**  
---|---|---  
`+` | `* Int Int Int` | `* Int Int`  
`+` | `* Int Int` | `* Int`  
  
With this new table, all of the **Input** and **Output** stacks match, which means that the type is valid and can prove runtime evaluation.  Thus, based on this whole song and dance, we can assign the following type:

```
addTwice : ('A Int Int Int -> 'A Int)
```

As expected, this function takes not two, but _three_ `Int`(s) on the stack and returns the remainder of that stack with a new `Int` on top.

#### Polymorphic Words

One mildly-annoying issue that we have just skated over is the problem of polymorphism.  Consider the following two programs:

```cat
42 pop
```

And this...

```cat
"fourty-two" pop
```

The question is: what type do we assign to `pop`?  We can easily make the following two assertions:

```
42           : ('A -> 'A Int)
"fourty-two" : ('A -> 'A String)
```

If we attempt to use this information to type-check the first program (assuming that it is sound), we will arrive at the following type for `pop`:

```
pop : ('A Int -> 'A)
```

That's intuitive, right?  All that we're doing here is taking the first value off of the stack (an `Int`, in the case of the first program) and throwing it away, returning the remainder of the stack.  However, if we use this type, we will run into some serious troubles type-checking the second program:

**Word** | **Input Stack** | **Output Stack**  
---|---|---  
`"fourty-two"` | `*` | `* String`  
`pop` | `* Int` | `*`  
  
Since `pop` has type `('A Int -> 'A)` (as we asserted above), it is inapplicable to a stack with `String` on top.  Note that we can't just push these constraints "up the chain", since it is a case of direct type mismatch, rather than a stack of insufficient depth.  In short: we're stuck.

The only way to solve this problem is to introduce the concept of parametric types.  Literally, we need to define a type which can be _instantiated_ against a given stack, regardless of what type happens to match the parameters in question.  Java calls this concept "generics".  Rather than giving `pop` the overly-restrictive type of `('A Int -> 'A)`, we will instead allow the value on top of the stack to be of _any_ type (not just `Int`):

```
pop : ('A 'a -> 'A)
```

Note the fact that `'A` and `'a` are very separate type variables in this snippet.  `'A` represents the "rest of the stack", while `'a` represents a specific type which just happens to be on top of the input stack.  Using this new, more flexible type, we can produce tables for both of our earlier programs:

**Word** | **Input Stack** | **Output Stack**  
---|---|---  
`42` | `*` | `* Int`  
`pop` | `* Int` | `*`  
  
 

**Word** | **Input Stack** | **Output Stack**  
---|---|---  
`"fourty-two"` | `*` | `* String`  
`pop` | `* String` | `*`  
  
Everything matches and the world is once again very happy.  Note that we can also apply this parametric type concept to the slightly more interesting example of `dup`:

```
dup : ('A 'a -> 'A 'a 'a)
```

In other words, `dup` says that whatever type is on top of the stack when it starts, that type will be on top of the stack _twice_ when it is finished.  Just like `pop`, this type can be instantiated against any stack with at least one type, regardless of whether that type is `Int`, `String`, or anything else for that matter.

### Higher-Order Functions

We've seen how to type-check simple phrases, as well as first-order functions with deferred stack access and the occasional polymorphic word.  However, there is one particularly troublesome aspect of concatenative type systems which we have completely ignored: functions which take quotations off the stack.  In other words: what type do we assign to `apply`?  Consider the following function:

```cat
define trouble {
  apply
}
```

At runtime, `trouble` will pop a quotation and then evaluate it against the remainder of the stack.  Intuitively, we need to have some way of representing the type of a quotation, but that's not even the most serious problem.  Somehow, we need to constrain the quotation to itself accept _exactly_ the stack which remains after it is popped.  We also need to find some way of capturing its output type in order to compute the final output type of `trouble`.

More concretely, we can make a first attempt at assigning a type for `trouble`.  The underscores (`_`) illustrate an area where our type system is incapable of helping us:

```
trouble : (_ (_ -> _) -> _)
```

It's very tempting to just throw an `'A` in there and be done with it, but the truth is that for this type expression, there is no "unused stack".  We don't really know how much (or how little) of the stack will be used by the quotation; it could pop five elements, twenty or none at all.  It literally needs access to the remainder of the input stack _in its entirety_ , otherwise the expression is useless.  Enter stack polymorphism...

Just as we needed a way to represent any _single_ type in order to type-check `pop` and `dup`, we now need a way to represent any _stack_ type in order to type-check `apply`.  Fortunately, the answer is already nestled within our pre-established notation.  Consider the type of `+`:

```
+ : ('A Int Int -> 'A Int)
```

We have been taking this to mean "any stack with two `Int`(s) on top resulting in that same stack with only one `Int`".  This is true, but we're being a little hand-wavy about the meaning of "any stack" and how it relates to `'A`.  When we really get down to it, what's happening here is `'A` is being _instantiated_ against a particular input stack, whatever that stack happens to be.  When we were type-checking `+ +`, the first word instantiated `'A` not to mean the empty stack (`*`), but rather a stack with at least one `Int` on it.  This was required to successfully type the second `+`.

We can very easily extend this notational convenience to represent generalized stack parameters.  Rather than being instantiated to specific types, stack parameters are instantiated to some stack in its entirety.  Just as with type parameters, wherever we see that instantiated stack parameter within a type expression, it will be replaced with whatever stack type it was assigned.  Thus, we can assign `trouble` the following type:

```
trouble : ('A ('A -> 'B) -> 'B)
```

In other words, `trouble` takes some stack _A_ which has a quotation on top.  This quotation accepts stack _A_ itself and returns some new stack _B_.  Note that we don't really know anything about _B_.  It could be related to _A_ , but it might not be.  The final result of the whole expression is this new stack _B_.

This concept is remarkably powerful.  With it in combination with the other types we have already examined, we can type check the entirety of Cat and be assured of the absence of type-mismatch and stack-underflow errors.  Considering the fact that Cat is almost exactly as powerful as Joy, that's a pretty impressive feat.

From a theoretical standpoint, things get even more interesting when we consider the type of the following function:

```cat
define y {
  [dup papply] swap compose dup apply
}
```

This has the following type:

```
y : ('A ('A ('A -> 'B) -> 'B) -> 'B)
```

As you may have guessed by the name, this is the [Y-combinator](<http://en.wikipedia.org/wiki/Y-combinator>)[1](<#prt3-1>), one of the most well-known mechanisms for producing recursion in a nameless system.  Note that this definition looks a little different from the pure-untyped lambda calculus (call-by-name semantics):

λf . (λx . f (x x)) (λx . f (x x))

What I'm trying to point out here is the fact that Cat is able to leverage its type system to assign a type to the Y-combinator.  This is something which is literally impossible in System F, a typed form of lambda-calculus.  In fact, the only way to type-check this function in a lambda-calculus-derivative system would be to add recursive types.  Cat is able to get by with a very much non-recursive type definition, something which I find fascinating in the extreme.

**Update:** The above paragraph is somewhat misleading.  It turns out that Cat actually _does_ use a recursive type under the surface to derive the non-recursive type for `y`.  Specifically:

```
dup papply : ('A ('B ('B self -> 'C) -> 'C) -> 'A ('B -> 'C))
```

On a further theoretical note, the device in Cat's type system which allows this power is in fact the stack type variable (e.g. `'A`).  These stack types are conceptually quite similar to the type parameters we used in typing `pop` (e.g. `'a`), but still in a very separate domain.  In fact, stack types have a different _kind_ than regular types.  This is not to say that Cat employs higher-kinds such as Scala's (e.g. `* => *`), but it does have two very different type kinds: stacks and values.

And yet, it is not kinds in and of themselves which allows for the typing of the Y-combinator.  Fω is essentially System F with higher-kinds, and yet it is still incapable of handling this tiny little expression.  Most interesting indeed...

### Conclusion

As you can see, type systems and concatenative languages do fit together nicely, but it takes a lot more effort than one would initially expect.  While typing simple expressions is easy enough, the waters are muddied as soon as higher-order functions and even deferred stack access enters the mix.  This is an extremely fertile area for research, where a lot of interesting ideas are being developed.  For example, [John Nowak's 5th](<http://lambda-the-ultimate.org/node/3050#comment-44535>) attempts to apply a type system to the stack-based paradigm, but in a very different way than Cat.

I hope you enjoyed this mini-series of articles on concatenative languages.  While they are a bit of a backwater in the programming language menagerie, I think that studying them can be a very instructive experience.  Furthermore, there remain some problems that are very nicely expressed in languages like Cat while being extremely unwieldy in more conventional languages like Scala.  Despite the obscurity of concatenative languages, it never hurts to have an extra language on hand, ready for those times when it really is the best tool for the job.

1 Technically, this is a little different from the Y-combinator used in conventional lambda-calculus (it executes the quotation rather than returning a fixed-point).  However, conceptually it is the same idea.