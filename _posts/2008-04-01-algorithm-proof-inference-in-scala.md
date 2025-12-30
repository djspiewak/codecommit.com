---
categories:
- Scala
date: '2008-04-01 01:00:33 '
layout: post
title: Algorithm Proof Inference in Scala
wordpress_id: 207
wordpress_path: /scala/algorithm-proof-inference-in-scala
---

Anyone who's written any sort of program or framework knows first-hand the traumas of testing.  The story always goes something like this.  First, you spend six months writing two hundred thousand lines of code which elegantly expresses your intent.  Next, you spend six _years_ writing two hundred _million_ lines of code which tests that your program actually does what you think it does.  Of course, even then you're not entirely sure you've caught everything, so you throw in a few more years writing code which tests your tests.  Needless to say, this is a vicious cycle which usually ends badly for all involved (especially the folks in marketing who promised the client that you would have the app done in six hours).  The solution to this "test overload cycle" is remarkably simple and well-known, but certain problems have constrained its penetration into the enterprise world (that is, until now).  The solution is program proving.

### A More Civilized Age

Program proofs are basically a (usually large) set of mathematical expressions which rigorously prove that all accepted program outcomes are correct.  A program prover takes these expressions and performs the appropriate static analysis on your code, spitting out either a "yes" or a "no".  It's far more effective than boring old unit testing since you can be absolutely certain that all the bugs have been caught.  More than that, it doesn't require you to write reams of test code.  All that is required is the series of expressions defining program intent:

\Gamma_m \to \\{\Gamma_0 = \textit{input} | m \sub \Gamma_0\\} \cup \Sigma^\star

\epsilon = e^{\epsilon \pm \delta}

f \colon \\{ x \in [\epsilon, \infty) | x \to \Gamma_x \\}

\rho_\delta = f(\Gamma_\delta \bullet \Gamma_\alpha) \Rightarrow \alpha \in \Sigma^\star

\mbox{prove}(\Omega) = \Omega \in \Gamma_\Beta \Rightarrow (\Omega \times \epsilon) \vee \rho_\omega

There, isn't that elegant?  Much better than some nasty JUnit test suite.  In a happier world, all tests would be replaced by this simple, easy-to-read representation of intent.  Unfortunately, program provers have yet to overcome the one significant stumbling block that has barred them from general adoption: the limitations of the ASCII character set.

Sadly, the designers of ASCII never anticipated the widespread need to express a Greek delta, or to properly render an implication.  Of course, we could always fall back on Unicode, but support for that character set is somewhat lacking, even in modern programming languages.  And so program provers languish in the outer darkness, unable to see the wide-scale adoption they so richly deserve.

### An Elegant Weapon

Fortunately, Scala can be the salvation for the program prover.  It's hybrid functional / object-oriented nature lends itself beautifully to the expression of highly mathematical concepts of intense precision.  Theoreticians have long suspected this, but the research simply has not been there to back it up.  After all, most PhDs write all their proofs on a blackboard, making use of a character set extension to ASCII.  Fortunately for the world, that is no longer an issue.

The answer is to to turn the problem on its ear (as it were) and eschew mathematical expressions altogether.  Instead of the developer expressing intent in terms of epsilon, delta and gamma-transitions, a simple framework in Scala will _infer_ intent just based on the input code.  All of the rules will be built dynamically using an internal DSL, without any need to mess about in low-level character encodings.  Scala is the perfect language for this effort.  Not only does its flexible syntax allow for powerful expressiveness, but it even supports UTF-8 string literals!  (allowing us to fall back on plan B when necessary)

Note that while Ruby is used in the following sample, the proof inference is actually language agnostic.  This is because the parsed ASTs for any language are virtually identical (which is what allows so many languages to run on the JVM).

```ruby class MyTestClass def multiply(a, b) a * b end end obj = MyTestClass.new puts obj.multiply(5, 6) ``` 

Such a simple example, but so many possible bugs.  For example, we could easily misspell the `multiply` method name, leading to a runtime error.  Also, we could add instead of multiply in the method definition.  There are truly hundreds of ways this can go wrong.  That's where the program prover steps in.

We define a simple Scala driver which reads the input from stdin and drives the proof inference framework.  The framework then returns output which we print to stdout.

```scala object Driver extends Application { val ast = Parser.parseAST(System.in) val infer = new ProofInference(InferenceStyle.AGRESSIVE) val prover = new ProgramProver(infer.createInference(ast)) val output = prover.prove(ast) println(output) } ``` 

It's as simple as that!  When we run this driver against our sample application, we get the following result:

![image](/assets/images/blog/wp-content/uploads/2008/03/image5.png)

Notice how the output is automatically formatted for easy reading?  This feature _dramatically_ improves developer productivity by reducing the time devoted to understanding proof output.  One of the many criticisms leveled against program provers is that their output is too hard to read.  Not anymore!

Of course, anyone can write a program which outputs fancy ASCII art, the real trick is making the output actually mean something.  If there's a bug in our program, we want the prover to find it and notify us.  To see how this works, let's write a new Ruby sample with a minor bug:

```ruby class Person < AR::Base end me = Person.find(1) me.ssn = '123-45-6789' me.save ``` 

It's an extremely subtle flaw.  The problem here is that the `ssn` field does not exist in the database.  This Ruby snippet will parse correctly and the Ruby interpreter will blithely execute it until the critical point in code, when the entire runtime will crash.  This is exactly the sort of bug that Ruby on Rails adopters have had to deal with constantly.

No IDE in the world will be able to check this code for you, but fortunately our prover can.  We feed the test program into stdin and watch the prover do its thing:

![image](/assets/images/blog/wp-content/uploads/2008/03/image6.png)

Once again, clear and to the point.  Notice how the output is entirely uncluttered by useless debug traces or line numbers.  The only thing we need to know is that something is wrong, and the prover can tell us that.

### Conclusion

I can speak from experience when I say that this simple tool can work wonders on any project.  Catching bugs early in the development cycle is the Holy Grail of software engineering.  By learning there's a problem early on, effort can be devoted to finding the bug and correcting it.  I strongly recommend that you take the time to check out this valuable aid.  By integrating this framework into your development process, you may save thousands of hours in QA and testing.

  * Download [proof-inference.zip](<http://www.codecommit.com/blog/misc/proof-inference.zip>)