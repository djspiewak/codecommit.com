---
categories:
- Scala
date: '2008-02-04 01:00:16 '
layout: post
title: 'Scala for Java Refugees Part 5: Traits and Types'
wordpress_id: 184
wordpress_path: /scala/scala-for-java-refugees-part-5
---

One of the mantras repeated throughout this series has been that Scala is Java, just better.  Thus it stands to reason that Scala would at a minimum support all of the features and power that Java offers.  So far you've seen things like [object-oriented constructs](<http://www.codecommit.com/blog/scala/scala-for-java-refugees-part-2>), the [power of methods](<http://www.codecommit.com/blog/scala/scala-for-java-refugees-part-3-methods-and-statics>) and even dabbled a bit with [pattern matching](<http://www.codecommit.com/blog/scala/scala-for-java-refugees-part-4>).  Through all this, I've deliberately avoided a particularly obvious Java construct: interfaces.

### Interfaces (or are they?)

Scala doesn't have any direct analogue to Java's interfaces.  This answer may seem a bit surprising, given what I've been saying about similarity to Java.  The fact is that Java's interfaces are really a very weak mechanism and a paltry imitation of their forebear: multiple inheritance.

Back in the days of C++, when the power of the dark side waxed full, object oriented languages commonly had support for inheriting from more than one superclass.  In C++, you could see this in examples like the following (shamelessly stolen from [Wikipedia](<http://en.wikipedia.org/wiki/Multiple_inheritance>)):

```cpp class Person { public: virtual Schedule* get_schedule() const = 0; }; class Student : public Person { public: Student(School*); Schedule* get_schedule() const { return class_schedule; } void learn(); private: Schedule *class_schedule; }; class Worker : public Person { public: Worker(Company*); Schedule* get_schedule() const { return work_schedule; } void work(); private: Schedule *work_schedule; }; class CollegeStudent : public Student, public Worker { public: CollegeStudent(School *s, Company *c) : Student(s), Worker(c) {} }; ``` 

Looks fairly standard right?  The question comes when you call the _get_schedule()_ function on an instance of _CollegeStudent_?  Think about it, what **should** happen?  Should you get a class schedule or a work schedule?

The answer is that nobody knows.  This is called the [diamond problem](<http://en.wikipedia.org/wiki/Diamond_problem>) and it's something which annoyed computer scientists to no end in the early days of OOP.  Of course one solution to the problem is to just never use multiple inheritance.  Unfortunately, this becomes extremely restricting in certain situations which really call for the feature.

Java's designers recognized the need for multiple typing (e.g. _CollegeStudent_ is both a _Student_ and a _Worker_ ), but they wanted to avoid the issues associated with inheriting conflicting method definitions along multiple paths.  Their solution was to design the interface mechanism, a feature which allows multiple typing without the complications of multiple inheritance.  So in Java you would represent the above sample like this:

```java public abstract class Person { public abstract Schedule getSchedule(); } public interface IStudent { public void learn(); } public interface IWorker { public void work(); } public class Student extends Person implements IStudent { private Schedule classSchedule; public Student(School school) {...} public Schedule getSchedule() { return classSchedule; } public void learn() {...} } public class Worker extends Person implements IWorker { private Schedule workSchedule; public Worker(Company company) {...} public Schedule getSchedule() { return workSchedule; } public void work() {...} } public class CollegeStudent extends Person implements IStudent, IWorker { public CollegeStudent(School school, Company company) {...} public Schedule getSchedule() {...} public void learn() {...} public void work() {...} } ``` 

Holy long examples, Batman!

If the sheer verbosity of the example wasn't enough to convince you of its flaws, direct your attention to the _CollegeStudent_ class.  We've achieved our primary goal here of creating a class which is both a _Student_ and a _Worker._   Unfortunately, we had to implement the _learn()_ and _work()_ methods multiple times.  Also, we complicated our hierarchy by a factor of two and introduced the much-despised _IRobot_ convention.  Finally, our hierarchy is less constrained than the C++ version in that there's nothing to prevent us from creating workers which are not people.  Logically this makes sense, but our specification says that all students and workers should be people, not insects or computers.  So we've lost flexibility, been shouldered with verbosity and introduced redundancy, all in the attempt to avoid a trivial logical disjunction.

### Traits

Scala recognizes that interfaces have their issues.  So rather than blinding creating a reimplementation of the same problems found in either Java or C++, Scala takes a new approach.  Inspired by a combination of Java's interfaces and Ruby's mixins, the designers of Scala have created the _trait_ construct.

```scala trait Book { def title:String def title_=(n:String):Unit def computePrice = title.length * 10 } ``` 

Scala's traits are quite nice in that they can not only define abstract members, but also full method definitions.  At the same time, they allow inheriting classes to inherit from more than one trait.  They pass on their type information and implementations to their children, as well as enforcing the abstract members.  At first glance, this seems like it would be just as bad as straight-up multiple inheritance, but it turns out the problems have been mitigated in some very clever ways.

To start with, there's that ever-annoying _override_ keyword.  I mentioned back in the article on [basic OOP](<http://www.codecommit.com/blog/scala/scala-for-java-refugees-part-2>) that any method which overrides a method in a superclass must be declared with the _override_ modifier.  At the time, I likened it to the language mandating the use of the _@Override_ annotation, with its primary purpose being to enforce the good practice.  The keyword however serves a far more important purpose, as we'll see in a second.

The real key to the power of traits is the way in which the compiler treats them in an inheriting class.  Traits are actually mixins, not true parent classes.  Any non-abstract trait members are actually included in the inheriting class, as in physically part of the class.  Well, not physically, but you get the picture.  It's as if the compiler performs a cut-and-paste with the non-abstract members and inserts them into the inheriting class.  This means that there's no ambiguity in the inheritance path, meaning no diamond problem.  We can rewrite our _CollegeStudent_ example in Scala without redundancy or fear of paradox:

```scala abstract class Person { def schedule:Schedule } trait Student extends Person { private var classSchedule:Schedule = ... override def schedule = classSchedule def learn() = {...} } trait Worker extends Person { private var workSchedule:Schedule = ... override def schedule = workSchedule def work() = {...} } class CollegeStudent(school:School, company:Company) extends Student with Worker { // ... } ``` 

Now if we make a call to the _schedule_ method on an instance of _CollegeStudent_ , the compiler knows that we're referring to the implementation of _schedule_ in the _Worker_ trait.  This is because _Worker_ was mixed in **after** the _Student_ trait.  We could just as easily reverse the order and make the _Student_ implementation the dominant one:

```scala class CollegeStudent(school:School, company:Company) extends Worker with Student { // ... } ``` 

Now _Student#schedule_ is the dominant implementation while the implementation in _Worker_ is overridden.  This is where the _override_ keyword becomes extremely useful.  It's sort-of the last stop fallback to ensure that no ambiguities sneak into the hierarchy without glaring compiler errors.

The absolutely most important thing to notice in the example, aside from the multiple-inheritance, is that _CollegeStudent_ is actually a proper sub-type of _Person, Student_ and _Worker._   Thus you can operate on an instance of _CollegeStudent_ polymorphically as an instance of one of these super-traits. In fact, other than the whole mixin thing, Scala traits are pretty much just like abstract classes.  They can declare abstract members, non-abstract members, variables, etc.  They can even extend other classes or traits!  The one catch is that Scala traits cannot accept parameters in their constructor.

This means that in one sense, the C++ version of our _CollegeStudent_ snippet is more powerful than the Scala translation.  In Scala, the _Student_ and _Worker_ traits cannot have constructor parameters, so there's no way to specify a _School_ or _Company_ as part of the inheritance.  If you ask me, it's a small price to pay for all of this power.  And no matter which way you slice it, traits are still far more flexible than Java interfaces.

### Type Parameters

You may not have realized this, but Scala is a statically typed, object-oriented language.  There are many such languages (C++, Java, etc), but they all have a common problem: accessing instances of a specific type from within a container written against a supertype.  An example serves better here than an explanation:

```java ArrayList list = new ArrayList(); list.add("Daniel"); list.add("Chris"); list.add("Joseph"); String str = (String) list.get(0); ``` 

The last line requires the cast because although we **know** that _list_ only contains _String_ (s), the compiler doesn't know it.  As far as the compiler is concerned, it's just a list of _Object_ (s) _.   _From the beginning though, this has been a problem for Java.  It has lead to mountains of ugly code and nasty casts to try and trick some type-safety into generic contains.  Enter type parameters.

Java 5 saw the introduction of a feature called "generics".  Just about every other language calls them "type parameters", so I tend to use the terms interchangeably.  Java's generics allow developers to specify that an instance of a generic type is specific not to the supertype but to a specific sub-type.  For example, the above example rewritten using generics:

```java ArrayList list = new ArrayList(); list.add("Daniel"); list.add("Chris"); list.add("Joseph"); String str = list.get(0); ``` 

Not only have we avoided the cast on the final line, but we have also added type-checking to all of the _add()_ invocations.  This means that the compiler will only allow us to add _String_ instances to the _list._   This is quite nice of course, but it has its limitations.

Because Java 5 was striving to maintain backward compatibility with old code-bases, the generics implementation isn't as flexible as it could be.  Most of this inflexibility stems from the decision to implement generics using [type erasure](<http://en.wikipedia.org/wiki/Generics_in_Java#Type_erasure>), rather than [reified types](<http://gafter.blogspot.com/2006/11/reified-generics-for-java.html>).  However, there are other issues which make working with Java generics weird, such as the incredibly cryptic and inconsistent syntax.  In short, Java generics are a language afterthought, whereas Scala type parameters were designed into the language from day one.

Scala type parameters have a lot of nice things going for them.  Perhaps number one on the list is a more consistent syntax.  Rather than a generics-specific wildcard character ( _?_ ) and an overloading of the _extends_ keyword, Scala type constraints utilize operators and meta-characters which are consistent with the rest of the language.  But I'm getting ahead of myself...

This is how the _list_ example might look translated into Scala:

```scala val list = new ListBuffer[String] list += "Daniel" list += "Chris" list += "Joseph" val str = list(0) ``` 

Passing over the operator overloading ( += ), the important thing to notice is the syntax of the _ListBuffer_ type parameter.  You'll notice that Scala uses square brackets rather than greater-than/less-than symbols to denote type parameters.  This is a bit counter to the traditional syntax (handed down from C++), but it turns out to be quite clean and powerful.  Here's a simple, parameterized class:

```scala class MyContainer[T] { private var obj:T = null def value = obj def value_=(v:T) = obj = v } val cont = new MyContainer[String] cont.value = "Daniel" println(cont.value) ``` 

With the exception of the square brackets, the syntax is remarkably similar to Java's.  Unlike C++, there's no weird template typing; no mind-numbingly verbose method prefixing.  Just solid, clean code.

Now Java allows generics to implement type constraining, which forces the type parameter to satisfy certain conditions.  (extending a certain supertype is a common example)  This can be accomplished easily through Scala, and as I said before, without overloading a keyword:

```scala import scala.collection.mutable.HashMap import java.awt.Component import javax.swing.JLabel class MyMap[V<:Component] extends HashMap[String,V] val map = new MyMap[JLabel] map += "label1" -> new JLabel() map += "label2" -> new JLabel() ``` 

In the class _MyMap_ , the single type parameter must be a subtype of _Component_ , otherwise the compiler will throw an error.  This of course is specified using the <: operator, rather than the _extends_ keyword as it would be in Java.  This has two advantages.  1) it avoids overloading the definition of a basic keyword, and 2) it allows for lower type bounding without unintuitive constructs.

Lower type bounding is when the type parameter is constrained such that a certain type must inherit _from_ the type parameter.  This is accomplishable in Java, but only by doing some extremely weird rearranging of the generic definition.  In Scala, it's as simple as switching the direction of the type constraint operator:

```scala class MyMap[V:>CollegeStudent] extends HashMap[String,V] ``` 

In this declaration, type parameter _V_ can be any type which is extended by _CollegeStudent_ (from our earlier example).  Thus the parameter could be _Student, Worker_ or even _Person_.  Obviously, this is a lot less useful than specifying an upper type bound, but it still has applications, especially in some of the core Scala classes.

Just like Java, Scala allows type parameters on methods as well as classes:

```scala def checkString[A](value:A):A = { value match { case x:String => println("Value is a String") case _ => println("Value is not a String") } value } val test1:String = checkString("Test Value") val test2:Int = checkString(123) ``` 

As you can see, Scala extends its type inference mechanism to type parameters similar to how Java does.  I could just as easily have left the explicit type declarations off of the two values and Scala would have properly inferred the types from the return types of the corresponding _checkString()_ invocations.  This is of course very similar to how Java allows method type parameter inference (see the _[Arrays#asList(T...)](<http://java.sun.com/javase/6/docs/api/java/util/Arrays.html#asList\(T...\)>)_ API).

Again much like Java, Scala allows explicit specification of method type parameters.  For example, this is the unsafe idiom to cast objects in Scala (the "sanctioned" syntax is to make use of pattern matching):

```scala val obj:Object = "Test String" val str:String = obj.asInstanceOf[String] ``` 

Here we are explicitly specifying _String_ as a type parameter to the _asInstanceOf_ method.  This method returns an instance of class _String_ generated from the instance of class _Object_ in the _obj_ value.

In truth, we've barely scratched the surface of the capabilities of Scala's type parameter mechanism.  If you're looking for some [brainf*ck](<http://en.wikipedia.org/wiki/Brainf*ck>) of an evening, I suggest you read up on Scala [type variances](<http://www.scala-lang.org/intro/variances.html>).  If there was any doubt that Scala is a more powerful language than Java, it will be quickly dispelled.  :-)

Oh, as a side note, Scala type parameters are currently (as of version 2.6.1) not quite compatible with Java generics.  While you can **use** parameterized Java classes, specifying the type parameters using the Scala syntax (e.g. _new ArrayList[String]_ ), you cannot actually declare parameterized Scala classes to be usable from within Java.  According to the mailing-lists however, the next release of Scala will indeed have full cross-compatibility between Scala and Java in the area of type parameterization.

### Conclusion

The deeper we go in the Scala language, the more we realize the truly awesome power of its syntax.  What's really amazing though, is that the designers were able to create a language with such flexible constructs without sacrificing the underlying clarity and consistency of the syntax.  At first blush, Scala continues to look a lot like Java and remains extremely readable to the average developer.  Somehow this clarity is retained without coming at the expense of capability.  Traits and Scala's type parameters are just one more example of this fact.

Coming to you in the sixth and final installment: a few steps into the wonderful world of [Scala-specific syntax](<http://www.codecommit.com/blog/scala/scala-for-java-refugees-part-6>).