---
categories:
- Java
date: '2008-03-31 01:00:18 '
layout: post
title: Groovy's Performance is Not Subjective
wordpress_id: 217
wordpress_path: /java/groovys-performance-is-not-subjective
---

Ah, the saga of misinterpreted micro-benchmarks!  Developers advocating one technology over another have long used micro-benchmarks and trivial examples to illustrate their point.  This is most often seen in the area of language implementation, where performance is a critical (and often emotional) consideration.  [The Computer Language Benchmarks Game](<http://shootout.alioth.debian.org/>) is a good example of this.

With the rise of the JVM as a platform for language development, we've been seeing a lot of benchmarks directly comparing some of these languages with each other and with Java itself.  Actually, it seems that the popular trio (as far as benchmarking goes) are Scala, JRuby and Groovy.  It's been a bit of a favorite past-time of the blogosphere to create benchmarks for these languages and then spin up controversial posts about the results.  Back in September, Darek Young [wrote a post](<http://dmy999.com/article/26/scala-vs-groovy-static-typing-is-key-to-performance>) illustrating a ray tracer benchmark which really caught my eye:

> **Results**
> 
> [ray.java](<http://www.ffconsultancy.com/languages/ray_tracer/code/1/ray.java>)  
> 12.89s 
> 
> [ray.scala](<http://dmy999.com/src/ray.scala>)  
> 11.224s 
> 
> [ray.groovy](<http://dmy999.com/src/ray.groovy>)  
> 2h 31m 42s 
> 
> I was expecting the Groovy code to run longer than the Scala code but was shocked at the actual difference. All three versions of the code produce identical images: ([fullsize](<http://dmy999.com/images/3.jpg>) here)

Wow!  I've seen these results dozens of times (looking back at the post), but they never cease to startle me.  How could Groovy be that much slower than everything else?  Granted it is very much a dynamic language, compared to Java and Scala which are peers in static-land.  But still, this is a ray tracer we're talking about!  There's no meta-programming involved to muddle the scene, so a halfway-decent optimizer should be able to at least squeeze that gradient down to maybe 5x as slow, rather than a factor of **830**.

If this were an isolated incident, I would probably just blow it off as bad benchmarking, or perhaps an odd corner case that trips badness in the Groovy runtime.  Then a week later, I read [this post](<http://www.jroller.com/rants/entry/why_is_groovy_so_slow>) by Pete Knego:

> **Test** | **Groovy** | **Java** | **Java vs Groovy (times faster)**  
> ---|---|---|---  
> Simple counter |  8.450 |  150 |  56x  
> Binary tree building  | 19.500 |  2.580 | 7.6x  
> Binary tree traversing | 2.530 | 76 | 33x  
> Prime numbers |  43.270 |  1.170 | 37x  
>   
> All [non-decimal] times are in milliseconds. 
> 
> Well, this is really disappointing. I expected Groovy to be slower but not by that much. In order to understand where does such a performance hit come from we have to peek under the hood. The culprit of all this is of course Groovy's dynamic nature, implemented as [MOP](<http://en.wikipedia.org/wiki/Metaobject_protocol>). MOP is a way for Groovy to know which class elements (fields/properties, methods, interfaces, superclasses, etc..) are defined on an object and to have a way to alter that data or invoke it. The core of MOP are two methods defined on GroovyObject: get/setProperty() and invokeMethod(). This methods are called every time you access a field or call a method on a Groovy object and quite a lot of work is done behind the scenes. The internals of the MOP are listed in MetaClass interface and implemented in six different classes.

All of this is old news, so the question is: Why am I bringing this up now?  Well, I recently saw [a post](<http://groovy.dzone.com/news/groovy-vs-java-performance-jav>) on Groovy Zone by none-other-than Rick Ross, talking about this very subject.  Rick's post was in response to two posts ([here](<http://tiago.org/cc/2008/03/21/groovy-performance-speed/>) and [here](<http://tiago.org/cc/2008/03/23/revisiting-groovy-performance-issues/>)), discussing ways to improve Groovy code performance by obfuscating code.  Final result? 

> This text is being written as I was changing and trying things, I gained 20s from  
> minor changes of which I lost track. :-) I am currently at 1m30s (down from the  
> original 4m and comparing with Java’s 4s).

I'm sorry, this is acceptable performance?  This is someone who's spent time trying to optimize Groovy, and _by his own admission,_ Groovy is **23x** slower than the equivalent Java code.  Certainly this is a far cry from the 830x slower in the ray tracer benchmark, but in this case it's simple string manipulation, rather than a mathematically intensive test. 

Coming back to Rick's entry, he looks at the conclusion and has this to say about it: 

> **Language performance is highly overrated**
> 
> Much is often made of the theoretical "performance" of a language based on benchmarks and arcane tests. There have even been cases where vendors have built cheats into their products specifically so they would score well on benchmarks. In the end, runtime execution speed is not as important a factor as a lot of people would think it is if they only read about performance comparisons. Other factors such as maintainability, interoperability, developer productivity and tool and library support are all very significant, too.

Wait a minute, that sounds a lot like something else I've read recently!  Maybe something like [this](<http://www.nabble.com/Re%3A-Is-Groovy-the-slowest-dynamic-language--p16143470.html>): 

> Is picking out the few performance weaknesses the right way to judge the  
> overall speed of Groovy? 
> 
> To me the Groovy performance is absolutely sufficient because of the  
> easy integration with Java. If something's too slow, I do it in Java.   
> And Java compared to Python is in most cases much faster. 
> 
> I appreciate the efforts of the Groovy team to improve the performance,   
> but if they wouldn't, this would be no real problem to me. Groovy is the  
> grooviest language with a development team always having the simplicity   
> and elegance of the language usage in mind - and that counts to me.  :-) 

This is almost a mantra for the Groovy proponents: performance is irrelevant.  What's worse, is that the few times where they've been pinned down on a particular performance issue that's _obviously_ a problem, the response seems to be along the lines of: this test doesn't really show anything, since micro-benchmarks are useless. 

I'm sorry, but that's a cop-out.  Face up to it, Groovy's performance is _terrible._   Anyone who claims otherwise is simply not looking at the evidence.  Oh, and if you're going to claim that this is just a function of shoe-horning a dynamic language onto the JVM, check out a [direct comparison](<http://shootout.alioth.debian.org/gp4sandbox/benchmark.php?test=all&lang=jruby&lang2=groovy>) between JRuby and and Groovy.  Groovy comes out ahead in only four of the tests. 

What really bothers me about the Groovy performance debates is that most "Groovyists" seem to believe that performance is in the eye of the beholder.  The thought is that it's all just a subjective issue and so should be discounted almost completely from the language selection process.  People who say this have obviously forgotten what it means to try to write a scalable non-trivial application which performs decently under load.  When you start getting hundreds of thousands of hits an hour, you'll be willing to sell your soul for every last millisecond. 

The fact is that this is simply not the case.  Performance _is_ important and it _must_ be considered when choosing a language for your next project.  Now I'm not implying that it should be the sole consideration, after all, we don't use Assembly much anymore.  But we can't just discard performance altogether as a means of evaluation.  Developer productivity is important, but it means nothing if the end result doesn't meet basic responsiveness standards. 

It's like the first time I tried [Mingle](<http://studios.thoughtworks.com/mingle-project-intelligence>) (which is written using JRuby BTW) back when it was still in pre-beta.  The application was amazing, but it took literally minutes to render the most basic of pages.  The problem was closely related to the state of the JRuby interpreter at the time and its poor memory management with respect to Rails.  In the end, the application was completely unusable.  ThoughtWorks had produced an amazing piece of software in a remarkably short time span, but the cost was the use of a language and framework which (at the time) led to unredeemable performance.  They spared the developers at the expense of the end-users. 

Both Mingle and JRuby have come a long way since those first tests.  Charles and the gang have put an intense amount of effort into optimizing the JRuby runtime and JIT compiler.  They've gone from an implementation which was 5-10x slower than MRI 1.8.6 to a fully compatible implementation that is usually _faster_ than the original.  Obviously performance is achievable in a dynamic language on the JVM, so why is Groovy still so horrible? 

The only answer I can think of is that the Groovy core team just **doesn't value performance.**   Why else would they consistently bury their heads in the sand, ignoring the issues even when the evidence is right in front of them?  It's as if they have repeated their own "performance is irrelevant" mantra so many times that they are actually starting to believe it.  It's unfortunate, because Groovy really is an interesting effort.  I may not see any value for my needs, but I can understand how a lot of people would.  It fills a nice syntactic niche that other languages (such as Ruby) just miss.  But all of its benefits are for naught if it can't deliver when it counts.