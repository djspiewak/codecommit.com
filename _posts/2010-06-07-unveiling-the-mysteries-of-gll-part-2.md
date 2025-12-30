---
categories:
- Scala
date: '2010-06-07 00:00:00 '
layout: post
title: 'Unveiling the Mysteries of GLL Part 2: The Problem Space'
wordpress_id: 326
wordpress_path: /scala/unveiling-the-mysteries-of-gll-part-2
---

[In the previous article](<http://www.codecommit.com/blog/scala/unveiling-the-mysteries-of-gll-part-1>), we skimmed the surface of automated text parsing and set the stage for our impending exploration of the GLL algorithm itself. However, before we can move ahead and do just that, we should first build up some idea of what the requirements are for truly generalized parsing and what sort of problems we are likely to encounter.

I'm going to assume you already have a working understanding of context-free grammars and how to read them. If you don't, then I recommend you to the [Wikipedia page on CFGs](<http://en.wikipedia.org/wiki/Context-free_grammar>). Specifically, the examples are quite instructive.

### Recursion

``` S ::= '(' S ')' | '(' ')' ``` 

In this grammar, the `S` non-terminal is recursive because one of its productions refers back to itself. Specifically, the first rule corresponding to the `S` non-terminal is of the form _α`S` β_, where _α_ and _β_ stand for some arbitrary rule fragments (in this case, `'('` and `')'`, respectively).

When a non-terminal maps to a production which is recursive in its _first_ token, we say that rule is _left-recursive_. For example:

``` E ::= E '+' N | E '-' N | N N ::= '1' | '2' | '3' | ... ``` 

In this grammar, the `E` non-terminal is left-recursive in two of its three productions. Left-recursion is a particularly significant property of a grammar because it means that any left-to-right parse process would need to parse `E` by _first_ parsing `E` itself, and then parsing `'+'` and finally `N` (assuming that the parser is using the first production). As you can imagine, it would be very easy for a naïve parsing algorithm to get into an infinite loop, trying to parse `E` by first parsing `E`, which requires parsing `E`, which requires parsing `E`, etc.

Mathematically, left-recursive productions are always of the form _α`E` β_ where _α —> ε_. In plain-English, this means that a production is left recursive if the part of the production preceding the recursive token represents the empty string ( _ε_ ). This is a very nice way of defining left-recursion, because it allows for a specific type of left-recursion known as _hidden_ left-recursion. For example:

``` A ::= B A '.' | '.' B ::= ',' | ``` 

Notice how the second production for `B` is empty? This means that `B` _can_ map to _ε_ , and thus `A` exhibits hidden left-recursion. The difference between hidden and direct left-recursion is that hidden left-recursion is obscured by other rules in the grammar. If we didn't know that `B` had the potential to produce the empty string, then we would never have realized that `A` is left-recursive.

LR parsing algorithms (such as tabular LALR or recursive-ascent) can handle direct left-recursion without a problem. However, not even Tomita's GLR can handle hidden left-recursion (which technically means that the GLR algorithm isn't fully general). Hidden left-recursion is a perfectly valid property for a context-free grammar to exhibit, and so in order to be fully general, a parsing algorithm must be able to handle it. As it turns out, this is just a little bit troublesome, and many papers on parsing algorithms spend a large majority of their time trying to explain how they handle hidden left-recursion.

It's worth noting that left-recursion cannot be handled by top-down algorithms (such as tabular LL(k) or recursive-descent) without fairly significant contortions. However, such algorithms have no trouble at all with other forms of recursion (such as our original recursive example with `S`). Left-recursion arises very naturally in many grammars (particularly involving binary forms such as object-oriented method dispatch or mathematical operators) and is one of the primary reasons why many people prefer algorithms in the LR family over LL algorithms.

### Ambiguity

It is perhaps surprising that context-free grammars are not required to be unambiguous. This means that a grammar is allowed to accept a particular input by using more than one possible sequence of rules. The classic example of this is arithmetic associativity:

``` E ::= E '+' E | E '-' E | '1' | '2' | '3' | '4' | ... ``` 

This is an extremely natural way to encode the grammar for mathematical plus and minus. After all, when we mentally think about the `+` operator, we imagine the structure as two expressions separated by `+`, where an expression may be a primitive number, or a complex expression like another addition or a subtraction operation. Unfortunately, this particular encoding has a rather problematic ambiguity. Consider the following expression:

``` 4 + 5 + 2 ``` 

Clearly this is a valid expression, and a parser for the example grammar will certainly accept it as input. However, if we try to generate a parse tree for this expression, we're going to run into two possible outcomes:

![sX1lVBmkjLnLTaII_1Ugqtg.png](/assets/images/blog/wp-content/uploads/2010/06/sX1lVBmkjLnLTaII_1Ugqtg.png)| ![slSIFdPcKoBgFuSZqB8FXJw.png](/assets/images/blog/wp-content/uploads/2010/06/slSIFdPcKoBgFuSZqB8FXJw.png)  
---|---  
  
Literally, the question is whether or not we first expand the left or the right `E` in the top-level `+` expression. Expanding the left `E` will give us the tree to the left, where the first two operands (`4` and `5`) are added together with the final result being added to `2`. Expanding the right `E` gives us the tree to the right, where we add `5` and `2` together, adding _that_ to `4`.

Of course, in the case of addition, associativity doesn't matter too much, we get the same answer either way. However, if this were division, then associativity could make all the difference in the world. _(4 / 5) / 2 = 0.4_ , but _4 / (5 / 2) = 1.6_. The point is that we can follow all of the rules set forth by the grammar and arrive at two very different answers. This is the essence of ambiguity, and it poses endless problems for most parsing algorithms.

If you think about it, in order to correctly handle ambiguity, a parser would need to return not just one parse tree for a particular input, but _all possible_ parse trees. The parser's execution would have to keep track of all of these possibilities at the same time, somehow following each one to its conclusion maintaining its state to the very end. This is not an easy problem, particularly in the face of grammars like the following:

``` S ::= S S S | S S | 'a' ``` 

Clearly, this is a contrived grammar. However, it's still a valid CFG that a generalized parsing algorithm would need to be able to handle. The problem is that there are an _exponential_ number of possible parse trees for any given (valid) input. If the parser were to naïvely follow each and every one of these possibilities one at a time, even on a short string, the parse process would take more time than is left in the age of the universe as we know it. Obviously, that's not an option.

### Conclusion

As you can see, generalized parsing has some very thorny problems to solve. It's really no wonder that the algorithms tend to be cryptic and difficult to understand. However, this is not to say that the problems are insurmountable. There are some very elegant and easy-to-understand algorithms for solving these problems, and GLL is one of them.

In the next article, we will start looking at the GLL algorithm itself, along with that chronically under-documented data structure at its core, the graph-structured stack (GSS).