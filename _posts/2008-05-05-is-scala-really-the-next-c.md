---
categories:
- Scala
date: '2008-05-05 00:00:16 '
layout: post
title: Is Scala Really the Next C++?
wordpress_id: 226
wordpress_path: /scala/is-scala-really-the-next-c
---

I've been turning that question over in my head for a few months now.  It's really a worthy thought.  At face value, it's incredibly derogatory and implicative of an over-bulked, poorly designed language.  While I'm sure this is not how the concept was [originally intended](<http://fishbowl.pastiche.org/2008/03/03/scala_is_to_java_as>), it certainly comes across that way.

But the more I think about it, the more I realize that the parallels are uncanny.  Consider the situation back in 1983, when C++ first got started.  C had become the dominant language for serious developments.  It was fast, commonly known and perhaps more importantly, structured.  C had successfully applied the concepts shown in Pascal to systems programming, revolutionizing the industry and becoming the lingua franca of developers everywhere.

When C++ arrived, one of its main selling points was as a "better C".  It was possible to interoperate seamlessly between C++ and C, even to the point of compiling most C programs unmodified in C++.  But despite its roots, it still managed to introduce a number of features drawn from Smalltalk et al. (such as classes and virtual member functions).  It represented a paradigm shift in the way developers represented concepts.  In fact, I think it's safe to say that the popular object-oriented design principles that we all take for granted would never have evolved to this level without the introduction of C++.  (yes, I'm aware of Objective-C and other such efforts, but C++ was the one which caught on)

So we've got a few catch-phrases here: "better C", "seamless interop", "backwards compatibility", "paradigm shift", etc.  Sound familiar?  (actually, it sounds a lot like [Groovy](<http://groovy.codehaus.org/Differences+from+Java>))  The truth is that Scala seems to occupy a very similar place in history (if six months ago can be considered "history").  Scala is almost an extension to Java.  It brings to the language things like higher-order functions, type inference and a type system of frightening power.  Scala represents a fundamental shift in the concepts and designs we use to model problems.  I truly believe that whatever language we're using in a decade's time, it will borrow heavily from the concepts introduced in Scala (in the same way that Java borrowed from C++).

But if Scala and C++ are so similar in historical inception, shouldn't we view the language with a certain amount of distrust?  We all know what a mess C++ turned out to be, why should Scala be any different?  I believe the answer has to do with Scala's fundamental design principles.  Specifically, Scala is _not_ trying to be source-compatible with Java.  You can't just take Java sources and compile them with Scala.

This clean break with the progenitor language has a number of ramifications.  Most importantly, Scala is able to smooth many of the rough edges in Java without breaking existing libraries.  For example, Scala's generics are far more consistent than Java's, despite still being implemented using erasure.  This snippet, for example, fails to compile:

```scala
def doSomething(ls:List) = {
  ...
}
```

All we have done is omit the generic type parameter.  In Java, the equivalent would lead to a compiler warning _at worst_ , because Java has to remain backwards compatible with code written before the introduction of generics.  This "error vs warning" distinction seems a bit trivial at first, but the distinction has massive implications throughout the rest of the type system.  Anyone who has ever tried to write a "generified" library in Java will know what I mean.

Scala represents a clean break from Java.  This is in sharp contrast to C++, which was trying to remain fully backward compatible with C sources.  This meant inheriting all of C's weird wrinkles (pass-by-value, no forward referencing, etc).  If C++ had just abandoned it's C legacy, it would have been a much nicer language.  Arguably, a language more like Java.  :-)

Perhaps the most important distinction between Scala and C++ is that Scala is being designed from the ground up with _consistency_ in mind.  All of the major problems in C++ can be traced back to inconsistencies in syntax, semantics or both.  That's not to say that the designers of C++ didn't put a good deal of effort into keeping the language homogenous, but the truth is that they ultimately failed.  Now we could argue until the cows come home about _why_ they failed, but whatever the reasons, it's done and it has given C++ a very bad reputation.  Scala on the other hand is being built by a close-knit team of academics who have spent a lifetime thinking about how to properly design a language.  I tend to think that they have a better chance of succeeding than the C++ folks did.

So the moral of this long and rambling post is that you shouldn't be wary of the Scala language.  It's not going to become the next evil emperor of the language world.  Far from it, Scala may just represent the next step forward into true programmatic enlightenment.