---
categories:
- Ruby
date: '2008-02-06 01:00:58 '
layout: post
title: Adding Type Checking to Ruby
wordpress_id: 186
wordpress_path: /ruby/adding-type-checking-to-ruby
---

What's the first thing you think of when you consider the Ruby Language?  Dynamic types, right?  Ruby is famous (infamous?) for its extremely flexible type system, and as a so-called "scripting language", the core of this mechanism is a lack of type checking.  This feature allows for some very concise expressions and a great deal of flexibility, but sometimes makes your code quite a bit harder to understand.  More importantly, it weakens the assurances that a certain method will actually _work_ when passed a given value.

Several different solutions have been proposed to workaround this limitation.  The canonical technique involves intensifying tests and increasing test coverage.  Ruby has some excellent unit test frameworks (such as RSpec) which serve to ease the pain associated with this approach, but no matter how you slice it, tests are a pain.  Having to rely on tests to take the place of type checking in the code assurance process can be extremely frustrating.

Another, less common technique is to simply perform dynamic type checks within the method itself.  Like so:

```ruby def create_name(fname, lname) raise "fname must be a String" unless fname.kind_of? String raise "lname must be a String" unless lname.kind_of? String fname + " " + lname end ``` 

This code explicitly checking the dynamic _kind_ of the parameter values to ensure that they are of type or subtype of _String_.  The issues with this sample should be relatively obvious.

Primarily, it's ugly!  This sort of repetitious, boiler-plate conditional checking is exactly the sort of thing Ruby tries to avoid.  What's more is the added bulk of all of these repetitive checks (assuming you perform one check per-parameter per-method) because far more unwieldy than just improving the rspec test coverage.

While manually type checking may be a bad solution syntactically, it's on the right track conceptually.  What we really want is some sort of assertion that the parameters are of a certain type, but that won't overly bloat our existing code.  We need some sort of framework that will "weave in" (think [AOP](<http://en.wikipedia.org/wiki/Aspect_oriented_programming>)) its type assertions without getting in the way our our algorithms.

Well it turns out that someone's [already done this](<http://people.freebsd.org/~eivind/ruby/types/>).  [Eivind Eklund](<http://people.freebsd.org/~eivind/>) kindly pointed me to his type checking framework in a comment on a previous post.  The basic idea is to perform the type checking assertions, but to factor the work out into an API encapsulated by an intuitive DSL.  So rather than performing all those nasty _unless_ statements as above, we could simply do something like this:

```ruby typesig String, String def create_name(fname, lname) fname + " " + lname end ``` 

It's really as simple as that.  Passing the type values to the _typesig_ method just prior to a method declaration give the cue to the Types framework to perform some extra work on each call that method.  Now we have the runtime assurances that the following code will not work (with a very intuitive error message):

```ruby create_name("Daniel", 123) ``` 

Will produce the folling output:

``` ArgumentError: Arg 1 is of invalid type (expected String, got Fixnum) ``` 

But the fun doesn't stop there.  Ruby encourages the "[duck typing](<http://en.wikipedia.org/wiki/Duck_typing>)" pattern, where algorithm developers concern themselves not with what the value **is** but rather what it **does**.  This means that the type checking really should be done based on what methods are available, not just the raw type.  It turns out that the Types framework supports this as well:

```ruby class Company def name "Blue Danube" end end class Person def name "Daniel Spiewak" end end typesig String, Type::Respond(:name) def output(msg, value) puts msg + " " + value.name end c = Company.new p = Person.new output("The company name is: ", c) output("The person is: ", p) output("The programmer is: ", "a genius") # error ``` 

Types can check not only the kind of the object but also to what methods it responds.  This is crucial to enabling its adoption into modern Ruby code bases, many of which rely heavily on this "duck typing" technique.

You can think of the Types framework just like another layer in your testing architecture.  Obviously it's not performing any sort of static type checking (since Ruby has no compile phase).  All it's doing is providing that extra certainty that you're never passing something weird from somewhere in your code, something that would break your algorithm.

So what's the catch?  Well, obviously you need to have the Types framework installed.  It's not as easy as just typing _gem install types_ either, since the framework actually predates Ruby Gems.  You'll have to [download the framework](<http://people.freebsd.org/~eivind/ruby/types/>) and then copy around the _types.rb_ file yourself.  But this is just deployment semantics.  The more interesting issue are the limitations of the code itself.

As far as I can tell, the only restriction on the framework is that it must be used within a proper class, not in the root scope.  This means that all of my examples above would have to be enclosed in a class, rather than just copy-pasted into a .rb file and run in place.  But other than this one limitation, the framework is incredibly flexible.  I really haven't shown you the seriously interesting stuff in terms of the API (there are more examples at the top of the _types.rb_ file).  In many ways, Types is actually more powerful than any static type checking mechanism could be (yes, I'm even including Scala in that evaluation).

I haven't had a chance to use Types on any serious project myself, but I can see tremendous potential, particularly for companies with large-scale Ruby/Rails deployments or even smaller projects looking for just a bit tighter code assurance.  As far as I'm concerned, there shouldn't be a non-trivial Ruby project attempted without this lovely library, Rails or no Rails.