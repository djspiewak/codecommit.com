{%
laika.title = "Universal Type Inference is a Bad Thing"
laika.metadata.date = "2008-04-09"
%}


# Universal Type Inference is a Bad Thing

Recently, Scala has been wowing developers with its concise syntax and powerful capabilities, but perhaps its most impressive feature is local type inference.  When the intended type for an element is obvious, the Scala compiler is able to make the inference and there is no need for any additional type annotations. 

While this is extremely useful, it falls quite a bit short of the type inference mechanisms available in other languages such as Haskell and ML.  Many people have pointed out that Scala doesn't go as far as it could in its inference, forcing the use of type annotations when the type could easily be inferred by the compiler.  To understand this claim, it's necessary to consider an example from a language which does have such universal type inference.  Consider the following function:

```ml
fun sum nil init = init 
 | sum (hd::tail) init = hd + (sum tail init)
```

For those of you not familiar with ML or its derivatives, this is a simple curried function which traverses a given list, adding the values together with an initial value given by `init`.  Obviously, it's a contrived example since we could easily accomplish the above using a fold, but bear with me. 

The function has the following type signature: `int list -> int -> int`.  That is to say, the ML compiler is able to infer the _only possible_ type which satisfies this function.  At no point do we actually annotate any specific nominal type.  Clarity aside, this seems like a pretty nifty language feature.  The ML compiler actually considers the whole function when inferring type, so its inferences can be that much more comprehensive.  Scala of course has type inference, but far less universal.  Consider the equivalent function to the above:

```scala
def sum(list:List[Int])(init:Int):Int = list match {
    case hd::tail => hd + sum(tail)(init)
    case Nil => init
  }
```

Obviously, this is a lot more verbose than the ML example.  Scala's type inference mechanism is local only, which means that it considers the inference on an expression-by-expression basis.  Thus, the compiler has no way of inferring the type of the `list` parameter since it doesn't consider the full context of the method.  Note that the compiler really couldn't say much about this method even if it did have universal type inference because (unlike ML) Scala allows overloaded methods and operators.

Because of the relative verbosity of the Scala example when compared to ML, it's tempting to claim that ML's type inference is superior.  But while ML's type inference may be more _powerful_ than Scala's, I think it is simultaneously more dangerous and less useful.  Let's assume that I was trying to write the `sum` function from above, but I accidentally swapped the `+` operator for a `::` on the second line:

```ml
fun sum nil init = init 
 | sum (hd::tail) init = hd :: (sum tail init)
```

The ML compiler is perfectly happy with this function.  Such a small distinction between the two, but in the case of the second (incorrect) function, the type signature will be inferred as the following: `'a list -> 'a list -> 'a list` (the `'a` notation is equivalent to the mathematical ![](http://www.codecommit.com/blog/wp-content/cache/tex_9efc98bc797ce52f05c52da0d94b08e9.png)).  We've gone from a curried function taking an `int list` and an `int` returning an `int` value to a function taking two `list` values with arbitrary element types and returning a new list with the same type; and all we did was change two characters.

By contrast, the same mistake in Scala will lead to a compiler error:

```scala
def sum(list:List[Int])(init:Int):Int = list match {
    case hd::tail => hd :: sum(tail)(init)    // error
    case Nil => init
  }
```

The error given will be due to the fact that the right-associative cons operator (`::`) is not defined for type `Int`.  In short, the compiler has figured out that we screwed up somewhere and throws an error.  This is very important, especially for more complicated functions.  I can't tell you how many times I've sat and stared at a relatively short function in ML for literally hours before figuring out that the problem was in a simple typo, confusing the type inference.  Of course, ML does _allow_ type annotations just like Scala, but it's considered to be better practice to just allow the compiler to infer things for itself.

ML's type inference ensures _consistency_ , while Scala's type inference ensures _correctness._   Obviously, "correctness" is a word which gets bandied about with horrific flippancy, but I think in this case it's merited.  The only thing that the ML type inference will guarantee is that all of your types match.  It will look through your function and ensure that everything is internally consistent.  Since both the correct and incorrect versions of our `sum` function are consistent, ML is fine with the result.  Scala on the other hand is more restrictive, which leads to better assurance at compile-time that what you just did was the "right thing".  I would argue that it's part of the responsibility of the type checker to catch typos such as the one in the example, but languages with universal type inference just can't do this.

Type inference can be an incredibly nice feature, and it is very tempting to just assume that "more is better" and jump whole-heartedly into languages like ML.  The problem is languages which have such universal type inference don't provide the same safety that languages with local or no type inference can provide.  It's just too easy to make a mistake and then not realize it until the compiler throws some (apparently) unrelated error in a completely different section of the code.  In some sense, type inference weakens the type system by no longer providing the same assurance about a block of code.  It's important to realize where to draw the line as a language designer; to realize how far is too far, and when to step back.