{%
laika.title = "Unveiling the Mysteries of GLL Part 1: Welcome to the Field"
laika.metadata.date = "2010-05-31"
%}


# Unveiling the Mysteries of GLL Part 1: Welcome to the Field

Generalized parsing is probably the most misunderstood topic in the entire field of automated language processing. There is a persistent perception that generalized parsing is slow and impractical. Even worse, most people seem to believe that generalized parsing is complicated and unpredictable (a perception deriving from the extremely obfuscated nature of most generalized parsing algorithms). This is all very unfortunate, because none of it is true anymore.

Now, before I move forward and justify that rather bold statement, I should probably define what I mean when I say "generalized parsing". Parsing algorithms generally fall into one of any number of categories. The most common ones are:

  * LL(k) (e.g. ANTLR, JavaCC)
  * Non-Commutative LL(*) (e.g. most hand-written parsers, parser combinators)
  * Memoized Non-Commutative LL(*) (e.g. Packrat parsers, Scala 2.8 parser combinators)
  * LR(k) (e.g. YACC, Bison)
  * Generalized

These are arranged roughly in order from least to most powerful. This means that the supported expressivity of grammars increases as you go down the list. Techniques like LL don't support left-recursion or ambiguity; LR supports left-recursion, but not ambiguity; and generalized supports anything that's [context-free](<http://en.wikipedia.org/wiki/Context-free_language>). Note that this is a very rough arrangement. It's difficult to formally analyze the non-commutative LL(*) techniques, and so theorists tend to be a little unclear as to exactly how powerful the techniques are with respect to more well defined classes like LL and LR. However, it is generally assumed that non-commutative LL(*) is strictly more powerful than LL(k) but likely less powerful than LR(k) (since left-recursion can be handled with memoization, but some LR local ambiguities do not always resolve correctly).

As intuition would suggest, algorithms are generally more complex (both in terms of comprehension and asymptotic performance) the more powerful you get. LL(k) algorithms, both the table-driven and the directly-encoded, are usually quite easy to understand. Parser states correspond directly to grammatical rules, and so it's usually pretty easy to tease out the structure of the parser. By contrast, LR(k) algorithms (most commonly, [tabular LALR](<http://en.wikipedia.org/wiki/LALR>) and [recursive-ascent](<http://en.wikipedia.org/wiki/Recursive_ascent_parser>)) are usually very difficult to conceptualize and next to impossible to read when encoded in a programming language. One look at the [recursive-ascent example](<http://en.wikipedia.org/wiki/Recursive_ascent_parser#Example>) on Wikipedia is sufficient to confirm this property.

Most listed non-generalized parsing techniques are _O(n)_ in the length of the input. The one exception here is non-commutative LL(*), which is _O(k^n)_ in the case where the grammar is recursively ambiguous and the input is invalid. Generalized parsing on the other hand has an absolute lower-bound of _o(n^2)_ (a property which falls out of the equivalence with matrix multiplication), though no one has ever found an algorithm which can do better than _O(n^3)_. Clearly, generalized parsing does impose a performance penalty beyond more "conventional" techniques.

For these reasons, generalized parsing is usually confined to applications which actually need to be able to handle the full set of context-free grammars — most notably, genome analysis and natural language processing. Even worse, the reticence surrounding generalized parsing has led to its avoidance in several less esoteric situations which would benefit greatly from its power — most notably, the Scala, Haskell and C/C++ compilers _should_ use generalized parsing, but don't.

This is really a shame, because generalized parsing benefits from two major areas of advancement in the past several decades: CPU clock speed and algorithmic improvements. The bias against generalized parsing dates back to a time when processors were slow enough that the difference between _O(n)_ and _O(n^3)_ was fairly significant, even on shorter input strings. It also predates several newer algorithms which encode the full power of generalized parsing in clean, elegant and understandable ways.

It's a prejudice, plain and simple, and I plan to do something about it. In this series of articles, I will explain some of the fundamental techniques used to make generalized parsing fast and efficient, particularly as they relate to a newer algorithm known as "generalized LL" (GLL). I'll give a brief outline of how this algorithm is implemented and how it can be easily used in Scala via the [gll-combinators](<http://github.com/djspiewak/gll-combinators>) framework. Additionally, I will provide some motivation for why generalized parsing is so important, particularly in light of the modern trend in language design toward more and more powerful syntax. Even if you don't agree with my conclusions, I hope you will come away from this series with a more complete understanding of the state of generalized parsing and how it can be effectively applied.