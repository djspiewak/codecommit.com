{%
laika.title = "The Joy of Concatenative Languages Part 2: Innately Functional"
laika.metadata.date = "2008-12-15"
%}


# The Joy of Concatenative Languages Part 2: Innately Functional

In [part one of this series](<http://www.codecommit.com/blog/cat/the-joy-of-concatenative-languages-part-1>), I introduced the concept of a stack-based language and in particular the syntax and rough ideas behind [Cat](<http://www.cat-language.com>).  However, to anyone coming into concatenative land for the first time, my examples likely seemed both odd and unconvincing.  After all, why would you ever use point-free programming when everyone else seems to be sold on the idea of name binding?  More importantly, where do these languages fit in with our established menagerie of language paradigms?

The answer to the first question really depends on the situation.  I personally think that the best motivation for concatenative languages is their syntax.  If you want to create an internal DSL, there will be no language better suited to it than one which is concatenative, Cat, Factor or otherwise.  This is because stack-oriented languages can get away with almost no syntax whatsoever.  They say that Lisp is a syntax-free language, but this holds even more strongly for languages like Cat.  Well, that and you don't have to deal with all the parentheses...

The second question is (I think) the more interesting one: how do we classify these languages and what sort of methodologies should we apply?  At first glance, Cat (and other languages like it) seem to be quite imperative in nature.  After all, you have a single mutable stack that any function can modify.  However, if you turn your head sideways and blink twice, you begin to realize that concatenative languages are really much closer to the functional side of the oyster.

Consider the following Cat program:

```cat
define plus { + }
define minus { - }

7 2 3
plus minus
```

Trivial, but to the point.  This program first adds the numbers `2` and `3`, then subtracts the result from `7`.  Thus, the final result is a value of `2` on the stack.  The only twist is that we have defined functions `plus` and `minus` to do the dirty work for us.  This wasn't strictly necessary, but I wanted to emphasize that `+` and `-` really are functions.  We could express the exact same program in Scala:

```scala
def plus(a: Int, b: Int) = a + b
def minus(a: Int, b: Int) = a - b

minus(7, plus(2, 3))
```

Do you see how the consecutive invocations of `plus` and `minus` in Cat became composed invocations in Scala?  This is where the term "concatenative language" derives from: the whole program is just a series of function compositions.  [Wikipedia's article on Cat](<http://en.wikipedia.org/wiki/Cat_\(programming_language\)#Semantics>) has a very nice, mathematical description:

> Two adjacent terms in Cat imply the composition of functions that generate stacks, so the Cat program `f g` is equivalent to the mathematical expressions ![](http://upload.wikimedia.org/math/a/5/a/a5a0406f40aa56d878c3c55c0f019d58.png) and ![](http://upload.wikimedia.org/math/0/2/7/02795b8bf2da65c7ce93f65cdd14fe6d.png), where _x_ is the stack input to the expression.

Strictly speaking, a concatenative language could be implemented without a stack, but such an implementation would likely be a bit harder to use than the average stack-based language.

Coming back to my original premise: concatenative languages are functional in nature.  Absolutely _everything_ in Cat is a function.  Operators, words, even numeric literals like "`3`" are actually functions at the conceptual level.  Additionally, Cat, Joy and Factor all offer a mechanism for treating functions as first-class values:

```cat
2 3
[ + ]
apply
```

The square-bracket (`[]`) syntax is representative of a quotation.  Literally this mean "create a function of the enclosed words and place it as a value on the stack".  We can pop this function off the stack and invoke it by using the `apply` word.  Incidentally, you may have noticed that this syntax is remarkably close to that which is used in `if` conditionals:

```cat
5 0 <
[ "strange math" ]
[ "all is well" ]
if
```

This syntax works because `if` isn't _conceptually_ a language primitive: it's just another function which happens to take a boolean and two quotations off the stack.  For the sake of efficiency, Cat does indeed implement `if` as a primitive, but this was a deliberate optimization rather than an implementation forced by the language design.  Untyped Cat (see Part 3) is equivalent in power to the [pure-untyped lambda calculus](<http://en.wikipedia.org/wiki/Lambda_calculus>), and as our friend Alonzo Church showed us, `if`-style conditionals are easily accomplished:

TRUE = λa . λb . a FALSE = λa . λb . b  
IF = λp . λt . λe . p t e

Yeah, maybe we're drifting a bit off-point here...

### Higher-Order Programming

So if Cat is just another functional programming language, then we should be able to implement all of those higher-order design patterns that we've come to know and love in languages like Scala and ML.  To see how, let's look at implementing some simple list manipulation functions in Cat.  The easiest would be to start with `append`, which pops two lists off of the stack and pushes a new list which is the end-to-end concatenation of the two originals:

```cat
define append {
  empty
  [ pop ]
  [
    uncons
    [append] dip
    cons
  ]
  if
}
```

This function first starts by checking to see if the top list is empty.  If so, then just pop it off the stack and leave the other right where it is.  Appending an empty list should always yield the original list.  However, if the head list is _not_ empty, then we need to work a bit.  First, we decompose it into its tail and head, which are pushed onto the stack in order by the `uncons` function.  Next, we need to recursively append the tail with our second list on the stack.  However, the head of the list from `uncons` is in the way on top of the stack.  We could use stack manipulation to move things around and get our lists up to the head of the stack, but `dip` provides us with a handy, higher-order shortcut.  We temporarily remove the top of the stack, invoke the quotation "`[append]`" against the remainder and then push the old top back on top of the result.

The `dip` operation is surprisingly powerful, making it possible to completely live without either variables or multiple stacks.  Any non-trivial Cat program will need to make use of this handy function at some level.

Once we have the old head and the new appended-list on the stack, all we need to do is put them back together using `cons`.  This function leaves a new list on the stack in place of the old list and head element.  This Cat program is almost precisely analogous to the following ML:

```ml
fun append ls nil = ls
  | append ls (hd :: tail) = hd :: (append ls tail)
```

Personally, I find the ML a lot easier to read, but that's just me.  Obviously it's a lot shorter, but as it turns out, our Cat implementation, while intuitive, was sub-optimal.  Cat already implements `append` in the guise of the `cat` function, and it is far more concise than what I showed:

```cat
define cat {
  swap [cons] rfold
}
```

It's almost frightening how short this is: only three words.  It's not as if `rfold` is doing anything mysterious either; it's just a simple right-fold function that takes a list, an initial value and a quotation, producing a result by traversing the entire list.  We can use something similar back in ML-land, achieving an implementation which is arguably equivalent in subjective elegance:

```ml
val append = foldr (op::)
```

Moving on, we can also implement a `length` function in Cat, this time using `fold` to tighten things up:

```cat
define length {
  0 [ pop 1 + ] fold
}
```

You'll notice that we have to mess around a bit in the quotation in order to avoid the first "parameter", the current element of the list (which we do not need).  Expressing this in ML yields a very similar degree of cruft:

```ml
val length = foldl (fn (n, _) => n + 1) 0
```

### Conclusion

The important take-away from this tangled morass of an article is the fact that Cat is a highly functional language, capable of easily keeping up with some of the stalwart champions of the paradigm.  More significantly, this is a trait which is shared by _all_ concatenative languages.  Rather than throwing away all of the old wisdom learned in language design, stack-based languages build on it by providing an alternative view into the world of functions.

In the [next (and final) article of the series](<http://www.codecommit.com/blog/cat/the-joy-of-concatenative-languages-part-3>), we will take a brief look at the challenges of applying a type system to a concatenative language and the fascinating techniques used by Cat to achieve just that.