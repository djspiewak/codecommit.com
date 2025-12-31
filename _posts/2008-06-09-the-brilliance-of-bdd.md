---
categories:
- Java
- Ruby
- Scala
date: '2008-06-09 00:00:59 '
layout: post
title: The Brilliance of BDD
wordpress_id: 237
wordpress_path: /java/the-brilliance-of-bdd
---

As I have [previously written](<http://www.codecommit.com/blog/scala/naive-text-parsing-in-scala>), I have recently been spending some time experimenting with various aspects of Scala, including some of the frameworks which have become available.  One of the frameworks I have had the privilege of using is the somewhat unassumingly-titled [Specs](<http://code.google.com/p/specs/>), and implementation of the [behavior-driven development](<http://en.wikipedia.org/wiki/Behavior_driven_development>) methodology in Scala.

Specs takes full advantage of Scala's flexible syntax, offering a very natural format for structuring tests.  For example, we could write a simple specification for a hypothetical `add` method in the following way:

```scala object AddSpec extends Specification { "add method" should { "handle simple positives" in { add(1, 2) mustEqual 3 } "handle simple negatives" in { add(-1, -2) mustEqual -3 } "handle mixed signs" in { add(1, -2) mustEqual -1 add(-1, 2) mustEqual 1 } } } ``` 

We could go on, of course, but you get the picture.  This code will lead to the execution of four separate assertions in three tests (to put things into JUnit terminology).  Fundamentally, this isn't too much different than a standard series of unit tests, just with a slightly nicer syntax.

Specs defines a domain-specific language for structuring test assertions in a simple and intuitive way.  However, this is hardly the only framework for BDD.  Perhaps the most well-known such framework is [RSpec](<http://rspec.info/>), which answers a similar use-case in the Ruby programming language.  Our previous specification could be rewritten using RSpec as follows:

```ruby describe AddLib do it 'should handle simple positives' do add(1, 2).should == 3 end it 'should handle simple negatives' do add(-1, -2).should == -3 end it 'should handle mixed signs' do add(1, -2).should == -1 add(-1, 2).should == 1 end end ``` 

The end-result is basically the same: the `add` method will be tested against the given assertions (all four of them) and the results printed in some sort of report form.  In this area, RSpec is significantly more mature than Specs, generating very slick HTML reports and nicely formatted console output.  This isn't really a fundamental weakness of the Specs framework however, just indicative of the fact that RSpec has been around for a _lot_ longer.

These two frameworks are interesting of course, but they are merely implementations of a much larger concept: behavior-driven development.  I've never been much of a fan of unit testing.  It's always seemed to be incredibly dull and a very nearly fruitless waste of effort.  As much as I hate it though, I have to bow to the benefits of a self-contained test suite; and so I press on, cursing JUnit every step of the way.

BDD provides a nice alternative to unit testing.  At its core, it is not much different in that test groupings and primitive assertions are used to check all aspects of a test unit against predefined data.  However, there is something about the "flow" of a behavioral spec that is considerably easier to deal with.  For some reason, it is far less painful to devise a comprehensive test suite using BDD principles than conventional unit testing.  It seems a little far-fetched, but BDD actually makes it easier to write (and more importantly, formulate) exactly the same tests.

It's an odd phenomenon, one which can only be caused by the storyboard flow of the code itself.  It is very natural to think of distinct requirements for a test unit when each of these requirements are being labeled and entered in a logical sequence.  Moreover, the syntax of both Specs and RSpec is such that there is very little boiler-plate required to setup an additional test.  Compare the previous BDD specs with the following JUnit4 example:

```java public class MathTest { @Test public void testSimplePositives() { assertEquals(3, add(1, 2)); } @Test public void testSimpleNegatives() { assertEquals(-3, add(-1, -2); } @Test public void testMixedSigns() { assertEquals(-1, add(1, -2)); assertEquals(1, add(-1, 2)); } } ``` 

JUnit just requires that much more syntax.  It breaks up the logical flow of the tests and (more importantly) the developer train of thought.  What can be worse is this syntax bloat makes it very tempting to just group all of the assertions into a single test - to save typing if nothing else.  This is problematic because one assertion may shadow all the others in the case of a failure, preventing them from ever being executed.  This can make certain problems much more difficult to isolate.

Logical flow is extremely important to test structure.  BDD frameworks provide a very nice syntax for painlessly defining comprehensive test suites.  The really wonderful thing about all of this is that BDD is available on the JVM, right now.  There's nothing stopping you from writing your code in Java as you normally would, then creating your test suite in Scala using Specs rather than JUnit.  Alternatively, you could use RSpec on top of JRuby, or [Gspec](<http://groovy.codehaus.org/Using+GSpec+with+Groovy>) with Groovy.  All of these are seamless replacements for a test framework like JUnit, and requiring of far less syntactic overhead.

The growing move toward polyglot programming encourages the use of a separate language when it is best suited to a particular task.  In this case, several languages are available which offer far more powerful test frameworks than those which can be found in Java.  Why not take advantage of them?