---
categories:
- Java
date: '2007-12-18 01:00:55 '
layout: post
title: Java Needs Map<?, ?> Syntax Sugar
wordpress_id: 168
wordpress_path: /java/java-needs-map-syntax-sugar
---

I've never been a big fan of useless syntax sugar.  I've always thought that the simpler a language is syntactically, the better.  If you think about it, there's a lot of merit to this sort of viewpoint.  I mean, which of these is easier to read?

```java public class Test { public void put(Object key, Object value) { // ... } public Object get(Object key) { // ... } } // ... Test test = new Test(); test.put(String.class, "blah"); test.put(StringBuilder.class, "blahblah"); String value = (String) test.get(StringBuilder.class); ``` 

Or this?

```java public class Test { public void put(K key, V value) { // ... } public V get(K key) { // ... } } // ... Test, String> test = new Test, String>(); test.put(String.class, "blah"); test.put(StringBuilder.class, "blahblah"); String value = test.get(StringBuilder.class); ``` 

The only thing that has different between the examples is one makes use of generics, the other doesn't.  The top example makes use of a cast, the bottom avoids this.  The lower example also has some trivial compile-time checking, but that's about it. 

So what have we done here?  We complicated a trivial example with 50-100 extra, _cryptic_ characters and managed to avoid a single, 9-character cast.  Not really an improvement.  I'll grant you that generics are usually very beneficial, and I won't deny that I like them and make use of them myself, but the point of the example is unchanged: extra "beneficial" syntax constructs are not always a good thing.

With all this said, I still have to admit that I'm (grudgingly) in favor of certain syntax additions.  Closures for example are probably the absolute best proposal for Java 7 I've heard yet.  Obviously we can quibble over the syntax, but I think the construct itself is a very valuable one.  I'm also (again grudgingly) in favor of another syntax sugar having to do with method parameters.

Many methods these days take a _Map_ as a parameter.  It's a fairly common thing to pass values that way.  JDBC is a good example (well, actually it's a _Properties_ instance).  [ActiveObjects](<https://activeobjects.dev.java.net>) has a method to create an entity, passing a map of field => value pairs to be set on the INSERT.  Often times, such methods are an enormous pain to call.  Example:

```java Map params = new HashMap(); params.put("firstName", "Daniel"); params.put("lastName", "Spiewak"); Person person = em.create(Person.class, params); ``` 

Alright, not horrible. But imagine you had several dozen values to pass. It's not that uncommon for such a situation to arise, and right now it always leads to vast quantities of syntax cruft.

Of course, there is a shortcut notation, but it's almost as verbose and virtually unknown (which makes it a bad choice due to readability concerns):

```Java Person person = em.create(Person.class, new HashMap() {{ put("firstName", "Daniel"); put("lastName", "Daniel"); }}); ``` 

What this is actually doing is declaring a constructor for an anonymous inner class extending _HashMap._   It's not _too_ bad in terms of syntax, but very few Java developers are aware of this construct, thus it should probably be avoided.

Now if we consider the corresponding syntax in a language like Ruby, things fall out beautifully (here using symbols instead of Strings for the keys):

```ruby person = em.create(Person, :firstName => "Daniel", :lastName => "Spiewak") ``` 

Obviously I'm taking a bit of artistic license with the API, but you get my drift.  This code is so much cleaner because Ruby has a syntax construct which allows the implicit creation of _Hash_ objects (the Ruby equivalent of _Map_ ) as required within the method invocation.  The above syntax is equivalent to the following:

```ruby params = Hash.new params[:firstName] = "Daniel" params[:lastName] = "Spiewak" person = em.create(Person, params) ``` 

So nothing too complex, Ruby just makes it cleaner.  Most scripting languages actually have a similar construct, for example Groovy allows for the _key:value_ syntax in method parameters, dynamically creating a Java _Map_ as necessary.

I think Java should add a similar syntax.  There are enough cases where such a construct would greatly simplify the code in question.  We wouldn't even need to add symbol literals to the language, we could just use standard Java types.  Perhaps something like the following is in order:

```java Person person = em.create(Person.class, "firstName":"daniel", "lastName":"Spiewak"); ``` 

Completely unobtrusive, easy to read, and even _more_ compact than the Ruby version.  The syntax is familiar to anyone using Groovy (more and more these days) and it doesn't require the introduction of a new keyword or clumsy over-riding construct.  It's type-checked, so compile-time safe, and totally backwards compatible (you could use this syntax on legacy APIs without modification).

So the question: is it worth it?  Since Java was open sourced, we've been seeing more and more efforts like [KSL](<https://ksl.dev.java.net/>) (the Kitchen Sink Language).  People have been hearing so many new language proposals that there's beginning to be an anti-change backlash.  Many of the developers I respect are more and more of the opinion that Java should remain the way it is.  Those who want all the fancy language additions should use [Scala](<http://www.scala-lang.org>).

In a lot of ways, I agree with them.  In my opinion, Java's too bulky and inconsistent in its syntax already, but I think this might just be one area where an exception could be made.  Perhaps this construct is simple enough, unobtrusive enough and ubiquitously useful enough to be worth the effort of putting it into the language.