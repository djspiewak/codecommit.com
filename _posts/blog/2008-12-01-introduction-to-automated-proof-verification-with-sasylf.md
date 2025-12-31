---
categories:
- Scala
date: '2008-12-01 01:00:00 '
layout: post
title: Introduction to Automated Proof Verification with SASyLF
wordpress_id: 274
wordpress_path: /scala/introduction-to-automated-proof-verification-with-sasylf
---

Doesn't that title just get the blood pumping?  Proof verification has a reputation for being an inordinately academic subject.  In fact, even within scholarly (otherwise known as " _unrealistically intelligent_ ") circles, the automated verification of proofs is known mainly as a complex, ugly and difficult task often not worth the effort.  This is a shame really, because rigorous proofs are at the very core of both mathematics and computer science.  We are nothing without logic (paraphrased contrapositive from Descartes).  Believe it or not, understanding basic proof techniques will be of tremendous aid to your cognitive process, even when working on slightly less ethereal problems such as how to get the freakin' login page to work properly.

Well, if you made it all the way to the second paragraph, then you either believe me when I say that this is legitimately useful (and cool!) stuff, or you're just plain bored.  Either way, read on as we commence our exciting journey into the land of rigorous proofs!

### SASyLF Crash Course

If you're at all familiar with the somewhat-specialized field of proof verification, you probably know that [SASyLF](<http://www.sasylf.org>) (pronounced "sassy elf") is _not_ the most widely used tool for the job.  In fact, it may very well be the least well-known.  More commonly, proofs that require automatic verification are written in [Twelf](<http://twelf.plparty.org/wiki/Main_Page>) or [Coq](<http://coq.inria.fr/>).  Both of these are fine tools and capable of a lot more than SASyLF, but they can also be extremely difficult to use.  One of the primary motivations behind SASyLF was to produce a tool which was easier to learn, had a higher level syntax (easier to read) and which gave more helpful error messages than Twelf.  The main idea behind these convolutions was to produce a tool which was more suitable for use in the classroom.

The main design decision which sets SASyLF apart from Twelf is the way in which proofs are expressed.  As I understand it, Twelf exploits [Curry-Howard correspondence](<http://en.wikipedia.org/wiki/Curry-Howard_correspondence>) to represent proofs implicitly in the types of a functional program ( **update:** this is incorrect; [see below](<http://www.codecommit.com/blog/scala/introduction-to-automated-proof-verification-with-sasylf#comment-4373>)).  While this can be very powerful, it's not the most intuitive way to think about a proof.  Eschewing this approach, SASyLF expresses proofs using _unification_ (very similar to Prolog) and defines inference rules explicitly in a natural-language style.

There are three main components to a SASyLF proof:

  * Syntax
  * Judgments
  * Theorems/Lemmas

Intuitively enough, the syntax section is where we express the grammar for the language used throughout our proof.  This grammar is expressed very naturally using BNF, just as if we were defining the language mathematically for a hand-written proof.  Left-recursion is allowed, as is right-recursion, arbitrary symbols, ambiguity and so on.  SASyLF's parser is mind bogglingly powerful, capable of chewing threw just about any syntax you throw at it.  The main restriction is that you cannot use parentheses, square brackets (`[]`), pipes (`|`) or periods (`.`) in your syntax.  The pure-untyped lambda calculus defined in SASyLF would look something like this:

|  ``` t ::= fn x => t[x] | t t | x ```   
---|---  
  
I said we couldn't use brackets, but that's only because SASyLF assigns some special magic to these operators.  In a nutshell, they allow the above definition of lambda calculus to ignore all of the issues associated with variable name freshness and context.  For simplicity's sake, that's about as far as I'm going to go into these mysterious little thingies.

The judgments section is where we define our inference rules.  Just as if we were defining these rules by hand, the syntax has the conditionals above a line of hyphens with the conclusion below.  The label for the rule goes to the right of the "line".  What could be more natural?

|  ``` judgment eval: t -> t t1 -> t1' --------------- E-Beta1 t1 t2 -> t1' t2  ```   
---|---  
  
The `judgment` syntax is what defines the syntax for the `->` "operator".  Once SASyLF sees this, it knows that we may define rules of the form `t -> t`, where `t` is defined by the syntax section.  Further on down, SASyLF sees our `E-Beta1` rule.  Each of the tokens within this rule (aside from `->`) begins with "`t`".  From this, SASyLF is able to infer that we mean "a term as defined previously".  Thus, this rule is syntactically valid according to our evaluation judgment and the syntax given above.

Of course, theorems are where you will find the real meat of any proof (I'm using the word "proof" very loosely to mean the collection of proven theorems and lemmas which indicates some fact(s) about a language).  SASyLF wouldn't be a very complete proof verification system without support for some form of proving.  Once again, the syntax is extremely natural language, almost to the point of being overly-verbose.  A simple theorem given the rules above plus a little would be to show that values cannot evaluate:

|  ``` theorem eval-value-implies-contradiction: forall e: t -> t' forall v: t value exists contradiction . _: contradiction by unproved end theorem ```   
---|---  
  
Note that `contradiction` is not more SASyLF magic.  We can actually define what it means to have a contradiction by adding the following lines to our judgment section:

|  ``` judgment absurd: contradiction  ```   
---|---  
  
In other words, we can have a contradiction, but there are no rules which allow us to get it.  In fact, the only way to have a contradiction is to somehow get SASyLF to the point where it sees that there are no cases which satisfy some set of proven facts (given the `forall` assumptions).  If SASyLF cannot find any cases to satisfy some rules, it allows us to derive anything at all, including judgments which have no corresponding rules.

Readers who have yet to fall asleep will notice that I cleverly elided a portion of the "`theorem`" code snippet.  That's because there wasn't really a way to prove that contradiction given the drastically abbreviated rules given in earlier samples.  Instead of proving anything, I used a special SASyLF justification, `unproved`, which allows the derivation of any fact given no input (very useful for testing incomplete proofs).  Lambda calculus isn't much more complicated than what I showed, but it does require more than just an application context rule in its evaluation semantics.  In order to get a taste for SASyLF's proof syntax, we're going to need to look at a much simpler language.

### Case Study: Integer Comparison

For this case study, we're going to be working with simple counting numbers which start with `0` and then proceed upwards, each value expressed as the successor of its previous value.  Thus, the logical number 3 would be `s s s 0`.  Not a very useful language in the real world, but much easier to deal with in the field of proof verification.  The syntax for our natural numbers looks like this:

|  ``` n ::= 0 | s n ```   
---|---  
  
With this humble definition for `n`, we can go on to define the mathematical greater-than comparison using two rules under a single judgment:

|  ``` judgment gt: n > n ------- gt-one s n > n n1 > n2 --------- gt-more s n1 > n2  ```   
---|---  
  
Believe it or not, this is all we need to do in terms of definition.  The first rule says that the successor of any number is greater than that same number (3 > 2).  The second rule states that if we already have two numbers, one greater than the other (12 > 4), then the successor of the greater number will still be greater than the lesser (13 > 4).  All very intuitive, but the real question is whether or not we can prove anything with these definitions.

#### An Easy Lemma

For openers, we can try something reasonably simple: prove that all non-zero numbers are greater than zero.  This is such a simple proof that we won't even bother calling it a theorem, we will give it the lesser rank of "lemma":

|  ``` lemma all-gt-zero: forall n exists s n > 0 . _: s n > 0 by induction on n: case 0 is _: s 0 > 0 by rule gt-one end case case s n1 is g: s n1 > 0 by induction hypothesis on n1 _: s s n1 > 0 by rule gt-more on g end case end induction end lemma ```   
---|---  
  
In order to prove anything about `n`, we first need to "pull it apart" and find out what it's made of.  To do that, we're going to use `induction`.  We could also use `case analysis`, but that would only work if our proof didn't require "recursion" (we'll get to this in a minute).  There are two cases as given by the syntax for `n`: when `n` is "`0`", and when `n` is "`s n1`", where `n1` is some other number.  We must prove that `s n > 0` for both of these cases individually, otherwise our proof is not valid.

The first case is easy.  When `n` is 0, the proof is trivial using the rule `gt-one`.  Notice that within this case we are no longer proving `s n > 0`, but rather `s 0 > 0`.  This is the huge win brought by SASyLF's unification: `n` _is_ "`0`" within this case.  Anything we already know about `n`, we also know about `0`.  When we apply the rule `gt-one`, SASyLF sees that we are attempting to prove `s n > n` where `n` is "`0`".  This is valid by the rule, so the verification passes.

The second case is where things get interesting.  We have that `n` is actually `s n1`, but that doesn't really get us too much closer to proving `s s n1 > 0` (remember, unification).  Fortunately, we _can_ prove that `s n1 > 0` because we're writing a lemma at this very moment which prove that.  This is like writing a function to sum all the values in a list: when the list is empty, the result is trivial; but when the list has contents, we must take the head and then add it to the sum of the tail as computed by...ourself.  Induction is literally just recursion in logic.  Interestingly enough, SASyLF is smart enough to look at all of the inductive cases in your proof and verify that they are valid.  This is sort-of the equivalent of a compiler looking at your code and telling you whether or not it will lead to an infinite loop.

To get that `s n1 > 0`, we use the induction hypothesis, passing `n1` as the "parameter".  However, we're not quite done yet.  We need to prove that `s s n1 > 0` in order to unify with our original target (`s n > 0`).  Fortunately, we already have a rule that allows us to prove the successor of a number retains its greater-than status: `gt-more`.

However, `gt-more` has a condition in our definition.  It requires that we already have some fact `n1 > n2` in order to obtain `s n1 > n2`.  In our case, we already have this fact (`s n1 > 0`), but we need to "pass" it to the rule.  SASyLF allows us to do this by giving our facts labels.  In this case, we have labeled the `s n1 > 0` fact as "`g`".  We take this fact, pack it up and send it to `gt-more` and it gives us back our final goal.

#### A Slightly Harder Theorem

A slightly more difficult task would be to prove that the successors of two numbers preserves their greater-than relationship.  Thus, if we know that 4 > 3, we can prove that 5 > 4.  More formally:

|  ``` theorem gt-implies-gt-succ: forall g: n1 > n2 exists s n1 > s n2 . _: s n1 > s n2 by unproved end theorem ```   
---|---  
  
At first glance, this looks impossible since we don't really have a rule dealing with `s n` on the right-hand side of the `>`-sign.  We can try to prove this one step at a time to see whether or not this intuition is correct.

Almost any lemma of interest is going to require induction, so immediately we jump to inducting on the only fact we have available: `g`.  Note that this is different from what we had in the earlier example.  Instead of getting the different syntactic cases, we're looking at the the rules which would have allowed the input to be constructed.  After all, whoever "called" our theorem will have needed to somehow prove that `n1 > n2`, it would be helpful to know what facts they used to do that.  SASyLF allows this using the `case rule` syntax.  We start with the easy base case:

|  ``` _: s n1 > s n2 by induction on g: case rule ------------ gt-one _: s n2 > n2 is _: s s n2 > s n2 by rule gt-one end case end induction ```   
---|---  
  
In this case, the term `_: s n2 > n2` is unified with `n1 > n2`.  Thus, `n1` is actually "`s n2`".  This means that by unification, we are actually trying to prove `s s n2 > s n2`.  Fortunately, we have a rule for that.  If we let "`n`" be "`s n2`", we can easily apply the rule `gt-one` to produce the desired result.

The second case is a bit trickier.  We start out by defining the case rule according to the inference rules given in the judgment section.  The only case left is `gt-more`, so we mindlessly copy/paste and correct the variables to suit our needs:

|  ``` case rule g1: n11 > n2  ------------- gt-more _: s n11 > n2 is _: s s n11 > s n2 by unproved end case ```   
---|---  
  
In this case, `n1` actually unifies with "`s n11`".  This is probably the most annoying aspect of SASyLF: all of the syntax is determined by token prefix, so _every_ number has to start with `n`, occasionally making proofs a little difficult to follow.

At this point, we need to derive `s s n11 > s n2`.  Since the left and right side of the `>` "operator" do not share a common sub-term, the only rule which could possibly help us is `gt-more`.  In order to apply this rule, we will somehow need to derive `s n11 > s n2` (remember, `gt-more` takes a known greater-than relationship and then tells us something about how the left-successor relates to the right).  We can reflect this "bottom-up" step towards a proof in the following way:

|  ``` case rule g1: n11 > n2  ------------- gt-more _: s n11 > n2 is g: s n11 > s n2 by unproved _: s s n11 > s n2 by rule gt-more on g end case ```   
---|---  
  
At this point, SASyLF will warn us about the `unproved`, but it will happily pass the rest of our theorem.  This technique for proof development is extremely handy in more complicated theorems.  The ability to find out whether or not your logic is sound even before it is complete can be very reassuring (in this way you can avoid chasing entirely down the wrong logical path).

In order to make this whole thing work, we need to somehow prove `s n11 > s n2`.  Fortunately, we just so happen to be working on a theorem which could prove this if we could supply `n11 > n2`.  This fact is conveniently available with the label of "`g1`".  We feed this into the `induction hypothesis` to achieve our goal.  The final theorem looks like this:

|  ``` theorem gt-implies-gt-succ: forall g: n1 > n2 exists s n1 > s n2 . _: s n1 > s n2 by induction on g: case rule ------------ gt-one _: s n2 > n2 is _: s s n2 > s n2 by rule gt-one end case case rule g1: n11 > n2  ------------- gt-more _: s n11 > n2 is g2: s n11 > s n2 by induction hypothesis on g1 _: s s n11 > s n2 by rule gt-more on g2 end case end induction end theorem ```   
---|---  
  
### Conclusion

I realize this was a bit of a deviation from my normal semi-practical posts, but I think it was still a journey well worth taking.  If you're working as a serious developer in this industry, I strongly suggest that you find yourself a good formal language and/or type theory textbook ([might I recommend?](<http://www.amazon.com/Types-Programming-Languages-Benjamin-Pierce/dp/0262162091/ref=pd_bbs_sr_1?ie=UTF8&s=books&qid=1228110398&sr=8-1>)) and follow it through the best that you can.  The understanding of how languages are formally constructed and the mental circuits to create those proofs yourself will have a surprisingly powerful impact on your day-to-day programming.  Knowing how the properties of a language are proven provides tremendous illumination into why that language is the way it is and somtimes how it can be made better.

**Credit:** Examples in this post drawn rather unimaginatively from [Dr. John Boyland's](<http://www.cs.uwm.edu/~boyland/>) excellent course in type theory.

  * [SASyLF Main Page](<http://www.cs.cmu.edu/~aldrich/SASyLF/>) at Carnegie Mellon University
  * [Download math.slf](<http://www.codecommit.com/blog/misc/math.slf>) (the full greater-than example)
  * [Further reading...](<http://www.cs.cmu.edu/~aldrich/SASyLF/fdpe08.pdf>)