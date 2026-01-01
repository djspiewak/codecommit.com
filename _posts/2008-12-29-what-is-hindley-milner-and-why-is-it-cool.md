---
categories:
- Scala
date: '2008-12-29 01:00:40 '
layout: post
title: What is Hindley-Milner?  (and why is it cool?)
wordpress_id: 278
wordpress_path: /scala/what-is-hindley-milner-and-why-is-it-cool
---

Anyone who has taken even a cursory glance at the vast menagerie of programming languages should have at least heard the phrase "Hindley-Milner".  F#, one of the most promising languages ever to emerge from the forbidding depths of Microsoft Research, makes use of this mysterious algorithm, as do Haskell, OCaml and ML before it.  There is even some research being undertaken to find a way to apply the power of HM to optimize dynamic languages like Ruby, JavaScript and Clojure.

However, despite widespread application of the idea, I have yet to see a decent layman's-explanation for what the heck everyone is talking about.  How does the magic actually work?  Can you always trust the algorithm to infer the right types?  Further, why is Hindley-Milner is better than (say) Java?  So, while those of you who actually know what HM is are busy recovering from your recent aneurysm, the rest of us are going to try to figure this out.

### Ground Zero

Functionally speaking, Hindley-Milner (or "Damas-Milner") is an algorithm for inferring value types based on use.  It literally formalizes the intuition that a type can be deduced by the functionality it supports.  Consider the following bit of _psuedo_ -Scala (not a flying toy):

```scala
def foo(s: String) = s.length

// note: no explicit types
def bar(x, y) = foo(x) + y
```

Just looking at the definition of `bar`, we can easily see that its type _must_ be `(String, Int)=>Int`.  As humans, this is an easy thing for us to intuit.  We simply look at the body of the function and see the two uses of the `x` and `y` parameters.  `x` is being passed to `foo`, which expects a `String`.  Therefore, `x` must be of type `String` for this code to compile.  Furthermore, `foo` will return a value of type `Int`.  The `+` method on class `Int` expects an `Int` parameter; thus, `y` must be of type `Int`.  Finally, we know that `+` returns a new value of type `Int`, so there we have the return type of `bar`.

This process is almost exactly what Hindley-Milner does: it looks through the body of a function and computes a _constraint set_ based on how each value is used.  This is what we were doing when we observed that `foo` expects a parameter of type `String`.  Once it has the constraint set, the algorithm completes the type reconstruction by unifying the constraints.  If the expression is well-typed, the constraints will yield an unambiguous type at the end of the line.  If the expression is not well-typed, then one (or more) constraints will be contradictory or merely unsatisfiable given the available types.

### Informal Algorithm

The easiest way to see how this process works is to walk it through ourselves.  The first phase is to derive the constraint set.  We start by assigning each value (`x` and `y`) a _fresh_ type (meaning "non-existent").  If we were to annotate `bar` with these type variables, it would look something like this:

```scala
def bar(x: X, y: Y) = foo(x) + y
```

The type names are not significant, the important restriction is that they do not collide with any "real" type.  Their purpose is to allow the algorithm to unambiguously reference the yet-unknown type of each value.  Without this, the constraint set cannot be constructed.

Next, we drill down into the body of the function, looking specifically for operations which impose some sort of type constraint.  This is a depth-first traversal of the AST, which means that we look at operations with higher-precedence first.  Technically, it doesn't matter what order we look; I just find it easier to think about the process in this way.  The first operation we come across is the dispatch to the `foo` method.  We know that `foo` is of type `String=>Int`, and this allows us to add the following constraint to our set:

_X_   ![\\mapsto](http://alt1.artofproblemsolving.com/Forum/latexrender/pictures/2/2/3/22341972d49b9a3a22a4ef9220996604cf49ad12.gif)  `String`

The next operation we see is `+`, involving the `y` value.  Scala treats all operators as method dispatch, so this expression actually means "`foo(x).+(y)`.  We already know that `foo(x)` is an expression of type `Int` (from the type of `foo`), so we know that `+` is defined as a method on class `Int` with type `Int=>Int` (I'm actually being a bit hand-wavy here with regards to what we do and do not know, but that's an unfortunate consequence of Scala's object-oriented nature).  This allows us to add another constraint to our set, resulting in the following:

_X_   ![\\mapsto](http://alt1.artofproblemsolving.com/Forum/latexrender/pictures/2/2/3/22341972d49b9a3a22a4ef9220996604cf49ad12.gif)  `String`  
_Y_   ![\\mapsto](http://alt1.artofproblemsolving.com/Forum/latexrender/pictures/2/2/3/22341972d49b9a3a22a4ef9220996604cf49ad12.gif)  `Int`

The final phase of the type reconstruction is to _unify_ all of these constraints to come up with real types to substitute for the _X_ and _Y_ type variables.  Unification is literally the process of looking at each of the constraints and trying to find a single type which satisfies them all.  Imagine I gave you the following facts:

  * Daniel is tall
  * Chris is tall
  * Daniel is red
  * Chris is blue

Now, consider the following constraints:

_Person1_ is tall  
_Person1_ is red

Hmm, who do you suppose _Person1_ might be?  This process of combining a constraint set with some given facts can be mathematically formalized in the guise of unification.  In the case of type reconstruction, just substitute "types" for "facts" and you're golden.

In our case, the unification of our set of type constraints is fairly trivial.  We have exactly one constraint per value (`x` and `y`), and both of these constraints map to concrete types.  All we have to do is substitute "`String`" for " _X_ " and "`Int`" for " _Y_ " and we're done.

To really see the power of unification, we need to look at a slightly more complex example.  Consider the following function:

```scala
def baz(a, b) = a(b) :: b
```

This snippet defines a function, `baz`, which takes a function and some other parameter, invoking this function passing the second parameter and then "cons-ing" the result onto the second parameter itself.  We can easily derive a constraint set for this function.  As before, we start by coming up with type variables for each value.  Note that in this case, we not only annotate the parameters but also the return type.  I sort of skipped over this part in the earlier example since it only sufficed to make things more verbose.  Technically, this type is always inferred in this way.

```scala
def baz(a: X, b: Y): Z = a(b) :: b
```

The first constraint we should derive is that `a` must be a function which takes a value of type _Y_ and returns some fresh type _Y'_ (pronounced like " _why prime_ ").  Further, we know that `::` is a function on class `List[A]` which takes a new element `A` and produces a new `List[A]`.  Thus, we know that _Y_ and _Z_ must both be `List[Y']`.  Formalized in a constraint set, the result is as follows:

_X_   ![\\mapsto](http://alt1.artofproblemsolving.com/Forum/latexrender/pictures/2/2/3/22341972d49b9a3a22a4ef9220996604cf49ad12.gif)  ( _Y_`=>` _Y'_ )  
_Y_   ![\\mapsto](http://alt1.artofproblemsolving.com/Forum/latexrender/pictures/2/2/3/22341972d49b9a3a22a4ef9220996604cf49ad12.gif)  `List[` _Y'_ `]`  
_Z_   ![\\mapsto](http://alt1.artofproblemsolving.com/Forum/latexrender/pictures/2/2/3/22341972d49b9a3a22a4ef9220996604cf49ad12.gif)  `List[` _Y'_ `]`  

Now the unification is not so trivial.  Critically, the _X_ variable depends upon _Y_ , which means that our unification will require at least one step:

_X_   ![\\mapsto](http://alt1.artofproblemsolving.com/Forum/latexrender/pictures/2/2/3/22341972d49b9a3a22a4ef9220996604cf49ad12.gif)  ( `List[` _Y'_ `]``=>` _Y'_ )  
_Y_   ![\\mapsto](http://alt1.artofproblemsolving.com/Forum/latexrender/pictures/2/2/3/22341972d49b9a3a22a4ef9220996604cf49ad12.gif)  `List[` _Y'_ `]`  
_Z_   ![\\mapsto](http://alt1.artofproblemsolving.com/Forum/latexrender/pictures/2/2/3/22341972d49b9a3a22a4ef9220996604cf49ad12.gif)  `List[` _Y'_ `]`  

This is the same constraint set as before, except that we have substituted the known mapping for _Y_ into the mapping for _X_.  This substitution allows us to eliminate _X_ , _Y_ and _Z_ from our inferred types, resulting in the following typing for the `baz` function:

```scala
def baz(a: List[Y']=>Y', b: List[Y']): List[Y'] = a(b) :: b
```

Of course, this still isn't valid.  Even assuming that `Y'` were valid Scala syntax, the type checker would complain that no such type can be found.  This situation actually arises surprisingly often when working with Hindley-Milner type reconstruction.  Somehow, at the end of all the constraint inference and unification, we have a type variable "left over" for which there are no known constraints.

The solution is to treat this unconstrained variable as a type parameter.  After all, if the parameter has no constraints, then we can just as easily substitute any type, including a generic.  Thus, the final revision of the `baz` function adds an unconstrained type parameter "`A`" and substitutes it for all instances of _Y'_ in the inferred types:

```scala
def baz[A](a: List[A]=>A, b: List[A]): List[A] = a(b) :: b
```

### Conclusion

...and that's all there is to it!  Hindley-Milner is really no more complicated than all of that.  One can easily imagine how such an algorithm could be used to perform far more complicated reconstructions than the trivial examples that we have shown.

Hopefully this article has given you a little more insight into how Hindley-Milner type reconstruction works under the surface.  This variety of type inference can be of immense benefit, reducing the amount of syntax required for type safety down to the barest minimum.  Our "`bar`" example actually started with (coincidentally) Ruby syntax and showed that it still had all the information we needed to verify type-safety.  Just a bit of information you might want to keep around for the next time someone suggests that all statically typed languages are overly-verbose.