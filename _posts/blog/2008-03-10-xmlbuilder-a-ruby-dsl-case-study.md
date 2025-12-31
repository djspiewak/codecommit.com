---
categories:
- Ruby
date: '2008-03-10 01:00:07 '
layout: post
title: 'XMLBuilder: A Ruby DSL Case Study'
wordpress_id: 200
wordpress_path: /ruby/xmlbuilder-a-ruby-dsl-case-study
---

XML is probably the most ubiquitous and most recognizable format in the modern development landscape.  It's simple power in representing hierarchically structured data has made it the standard for representing everything from Word documents to databases.  It's also one of the most verbose and meta-rich syntaxes known to man.

So in that sense, XML is a mixed blessing.  Its flexibility and intuitive nature allows developers to store just about any data in a human readable, easy-to-debug manner.  Unfortunately, its verboseness often makes _generating_ the actual XML a very frustrating and boring foray into the land of boiler-plate.  Various techniques have been developed over the years to smooth this process (e.g. manipulating a DOM tree or reflectively marshalling objects directly to XML), but on the whole, generating XML in code is just as annoying as it has always been.  We've all written code like this in the past:

```java public String toXML() { final String INDENT = " "; StringBuilder back = new StringBuilder( "\n\n"); back.append("\n"); for (Book book : getBooks()) { back.append(INDENT).append("").append(book.getTitle()).append("\n"); } back.append(""); return back.toString(); } ``` 

Not the most pleasant of algorithms.  Oh there's nothing complex or challenging about the code, it's just annoying (as String manipulation often is).  Things would get a little more interesting if we actually had some sort of recursive hierarchy to traverse, but even then it would still be pretty straight-forward.  XML generation is tedious grudge-work to which we all must submit from time to time.

### Domain Specific Languages

There's a new wave in programming (especially in the communities surrounding dynamic languages like Ruby and Groovy) called "Domain Specific Language", or DSL for short.  The DSL technique has really been around for a long time, but it's just now finding its way to the mainstream, "Citizen Joe" developers.  This is because for decades, domain specific languages have been separate languages unto themselves, with their own syntax, quirks, and utter lack of tool support.

For example, if a company wanted to specify their business logic in a more readable format using a DSL, the would have to spend months, sometimes years of effort to build an entirely new language, just for the one application.  While there was no need to generate a truly flexible and extensible syntax (or even one which was Turing-complete), such efforts would still require an enormous amount of work to be put into trivialities like parsers, AST walkers and core libraries.  Obviously this meant that DSLs were extremely uncommon.  There are very few use-cases for something which requires so much and gains you so little.

This variety of domain specific language is called an "External DSL".  This name stems from the fact that the language is completely independent and _external_ to other languages.  The innovation which made the DSL attainable for the common-man is that of the "Internal DSL".

People often struggle to precisely define internal DSLs.  In the broadest sense, an internal DSL is a language API carefully structured to satisfy a particular syntax when used in code.  There is no syntax parsing involved in implementing an internal DSL.  Rather, effort is focused on the API itself.  The more flexible the syntax of the underlying language, the more powerful and potentially intuitive the syntax of the DSL can be.  It is for this reason that languages such as Ruby, Python, Groovy and company are often used to implement internal DSLs.  These languages are defined by extremely flexible and dynamic syntax, lending themselves perfectly to such efforts.

With this in mind, it should theoretically be possible to design an "API" for XML generation that's simple and intuitive.  The DSL could be implemented using Ruby, though actually sufficiently dynamic language could do.  Implementing an internal DSL is a notoriously difficult task, so perhaps a step-by-step walk through is in order.

### Getting Started

The very first step in creating an internal DSL is to design the syntax.  Similar to how test-driven development starts with a set of unit tests and builds functionality which satisfies the tests, DSL development starts with a few code samples and builds an API to satisfy the syntax.  This step will guide all of the code we write as we implement the DSL.

One of the primary goals for our DSL syntax should be to reflect (as much as possible) the structure and essence of the XML output in the generating code.  One of the major shortcomings of the ad hoc "string concat" XML generation technique is an utter lack of logical structure in the code.  Sure your _algorithm_ may be nicely organized and formatted, but does it really reflect the actual structure of the XML it's generating?  Probably not.  Another major goal should be brevity.  XML generating code is extremely verbose, and the whole idea behind writing a DSL for XML generation is to elevate some of this hassle.

So, we've got brevity and logical structure.  As minor goals, we also may want to spend some effort making the syntax versatile.  We don't want the algorithm to perform incorrectly if we try to generate a document with the < character in some field.  With that said however, we don't need to be _overly_ concerned with flexibility.  Yes, our DSL should be as capable as possible, but not to the detriment of brevity and clarity.  These concerns are paramount, functionality can take a back seat.

With these goals in mind, let's try writing a static syntax for our Person/Book example above. Bear in mind that every construct used in the syntax must be valid Ruby which the interpreter will swallow.  _How_ it will swallow is unimportant right now.  For this step, it's helpful to remember that the _ruby -c_ command will perform a syntax check on a source file without actually attempting to run any sort of interpretation.

```ruby xml.person :name => 'Daniel "Leroy" Spiewak' do book do 'Godel, Escher, Bach' end book do 'The Elegant Universe' end book do 'Fabric of the Cosmos' end book do "The Hitchhiker's Guide to the Galaxy" end book # book with no title end puts xml # prints the generated XML ``` 

Note that this is all static, there's no dynamically generated _data_ to muddle the picture.  We can worry about that later.

This code sample gives us a rough idea of what the syntax should look like.  According to _ruby -c_ , it's valid syntax, so we're ready to proceed.  Everything looks fairly unencumbered by meta-syntax, and the logical structure is certainly represented.  Now we do a quick mental pass through the syntax to ensure that all of our _functional_ bases are covered.  In the example we've used attributes, nested elements and text data.  One of our attributes is making use of illegal XML characters (double quotes).  We haven't tried mixing elements and text yet, but it's fairly obvious how this could be done.

Strictly speaking, we haven't rigorously _defined_ anything.  What we have done is given ourselves a framework to build off of.  This sample will serve as an invaluable template to guide our coding.

### Parsing the Syntax

Remember I said that internal DSLs do not involve any syntax parsing?  Well, in a way I was wrong.  The next thing we need to do is walk through the syntax ourselves and understand how Ruby will understand it.  This step requires some fairly in-depth knowledge of Ruby's (or whatever language you're using) syntax.  Hopefully the following diagram will help to clarify the process.

![image1](/assets/images/blog/wp-content/uploads/2008/03/image1.png)

I've annotated the areas of interest in cute, candy colors to try and illustrate roughly what the Ruby syntax parser will see when it looks at this code.  It's important to understand this information since it is the key to translating our contrived syntax into a rigorous API.

For the sake of this discussion, I'm going to assume you have a working knowledge of Ruby and have already recognized some of the more basic syntax elements within the sample.  For example, the _do/end_ block is just that, a _Block_ object (roughly the Ruby equivalent of a closure).  The annotated String literal is an implicit return value for the inner block (the last statement of a block is implicitly the return value).  This syntax employs a Ruby "trick" which allows us to drop the _return_ statement (an important trick, since you can't return from a block anyway).

Anyone familiar with Ruby on Rails should be aware of Ruby's [convenient Hash literal syntax](<http://www.codecommit.com/blog/java/java-needs-map-syntax-sugar>) employed for the element attributes.  As annotated, this literal is being passed as a method parameter (Ruby allows us to drop the parentheses).  Here again we're using little oddities in the Ruby syntax to clean up our DSL and reduce unnecessary cruft.

You should also be familiar with the blurred line between variable and method so common in Ruby.  As annotated, the _xml_ syntax element could be either a local field, or a method that we're invoking without the parentheses.  To clear up this point, we need to refer to the full sample above.  In no place do we declare, nor do we provide for the declaration of a local variable _xml._   Thus, _xml()_ will be a method within our API available in the global scope.

Now we get to the really interesting stuff: those mysterious undefined methods.  As with other dynamic languages, Ruby allows method invocations upon non-existent methods.  To someone from a static language background, this seems rather peculiar, but it's perfectly legal I assure you.  In a sense, it's much like the "methods as messages" metaphor employed by Objective-C and Smalltalk.  When you call the method, you're just broadcasting a message to the object, hoping someone answers the call.

In Ruby, when a method is called which has no corresponding handler, a special method called _method_missing_ is invoked.  This method is what allows us to handle messages even without a pre-defined method body.  ActiveRecord makes extensive use of this feature.  Every time you access a database field, you're calling a non-existent method and thus, _method_missing._

So based on the first non-existent method we're calling ( _person(...)_ ), we can already say something about our API.  Somewhere in the global scope, we're going to need to have a method called _xml()_.  This method will return an instance of a class which defines _method_missing_ and so can handle our _person(...)_ invocation.  It seems this implementation of _method_missing_ will also need to optionally take a _Block_ as the final parameter, allowing it to pass execution on to its logical children.  When referring back to our original sample, the final line seems to indicate that the instance must implement the _to_s()_ method, allowing _puts()_ to implicitly convert it to a String.

### Implementation

All that work and we still haven't even written a solid line of code.  What we have done though is given ourselves a clear idea of where we need to go, and a rough outline for how it can be done.  We've laid the foundation for the implementation by giving ourselves two critical starting points: _xml()_ and that mysterious outer-scoped _method_missing._   We may not know _how_ we're going to implement all this yet, but we at least have an idea on where to begin.

Starting with the easy one, let's implement a basic framework around _xml()_.

```ruby require 'singleton' module XML class XMLBuilder attr_reader :last private def initialize end def method_missing(sym, *args, █) # ... end def to_s @last end def self.instance @@instance ||= new end end end def xml XML::XMLBuilder.instance end ``` 

Notice how we're using a singleton instance of _XMLBuilder_?  That's because there's no need for us to have more than one instance exposed to the DSL users.  _XMLBuilder_ is just a placeholder class that dispatches the first level of commands for the DSL and will assemble the result for us as it executes.  Thus, _XMLBuilder_ can be considered the root of our DSL, the corner-stone upon which the entire API is bootstrapped.  We do however need to allow for other, private instances as we'll see later on.

Another item worthy of note in this snippet is the non-standard _method_missing_ signature.  This is because we will actually need the block as a proper _Proc_ object down the line.  A block parameter (prefixed with &) is the only parameter which can follow a varargs parameter (prefixed with *) and there can only be one of them.

We can now try a first-pass implementation of _method_missing_.  This implementation is really just a sample with a very significant shortcoming.  The actual implementation is quite a bit more complex.

```ruby def method_missing(sym, *args, █) @last = "<#{sym.to_s}" if args.size > 0 and args[0].is_a? Hash # never hurts to be sure args[0].each do |key, value| @last += " #{key.to_s}=\"#{value.to_s}\"" end end if block.nil? @last += "/>" # if there's no children, just close the tag else @last += ">" builder = XMLBuilder.new builder.instance_eval block @last += builder.last @last += "" end end ``` 

Again, this is just the rough idea.  In our actual implementation we need to be concerned about things like valid attribute values (as demonstrated in our sample), proper element names, etc.

The key to the whole algorithm is the _instance_eval_ invocation.  This single statement passes control to the next block down and starts the process all over again.  The important thing about this is evaluating the block within the context of the an _XMLBuilder_ instance, rather than _just_ its enclosing context.  This allows the nested block to take advantage of the same _method_missing_ implementaiton, hence implicitly recursing further into the XML tree.  This technique is extremely powerful and absolutely critical to a lot of DSL implementations.

You'll also notice that this is breaking the singleton pattern we established in our class design.  This is because a separate instance of _XMLBuilder_ is required to handle each nested block within the DSL tree.  It's very important to remember that we're not actually exposing this instance in the public API, it's just a tool we use within the implementation.  The API users will still only see the singleton _XMLBuilder_ instance.

So a bit more semantically, what we're doing is the following:

  1. Handle the root non-existant method invocation 
  2. Deal with any attributes in the single _Hash_ parameter (if exists) 
  3. Check for nested block. If found, create a _new_ builder instance and use its context to evaluate the child block 
  4. Within child block evaluation, recurse to step 1 for each method invocation 
  5. Accumulate result from child block evaluation and return 

Of course, this doesn't deal with nested text content within elements.  However, the principles of the implementation are fairly clear and the rest is just code.

As with all internal DSLs, the user-friendly DSL syntax is supported by an API that's ugly, hacky and heavily dependant on language quirks (such as the super-critical _instance_eval_ ).  In fact, it has been said that internal DSLs _encourage_ the use of language quirks if it simplifies the API from the end-developer standpoint.  Of course this makes the end-developer code very clean and easy to maintain at the cost of the DSL developer's code, which is nightmarish and horrible to work with.  It's a tradeoff that must be considered when the decision is made to go with a DSL-style API.

### Conclusion

Hopefully this was a worthwhile trek into the gory innards of implementing an internal DSL.  I leave you with one final code sample to whet your appetite for the fully-implemented API.  This is an extract from the [ActiveObjects](<https://activeobjects.dev.java.net>) Ant build file converted to use our new DSL.  It's interesting that this converted version is significantly cleaner than the original, XML form.

```ruby require 'xmlbuilder' xml.project :name => 'ActiveObjects', :default => :build do dirname :property => 'activeobjects.dir', :file => '${ant.file.ActiveObjects}' property :file => '${activeobjects.dir}/build.properties' target :name => :init do mkdir :dir => '${activeobjects.dir}/bin' end target :name => :build, :depends => :init do javac :srcdir => '${activeobjects.dir}/src', :source => 1.5, :debug => true end target :name => :build_test, :depends => [:init, :build] do property :name => 'javadoc.intern.path', :value => '${activeobjects.dir}/${javadoc.path}' end end puts xml ``` 

This will print the following XML:

```xml  ``` 

The fully implemented DSL is available in a single Ruby file. Also linked are some examples to provide a more balanced view of the capabilities.

  * [xmlbuilder.rb](<http://www.codecommit.com/blog/misc/xmlbuilder/xmlbuilder.rb>) \- **The actual DSL implementation**
  * [antfile.rb](<http://www.codecommit.com/blog/misc/xmlbuilder/antfile.rb>)
  * [htmlexample.rb](<http://www.codecommit.com/blog/misc/xmlbuilder/htmlexample.rb>)
  * [testfile.rb](<http://www.codecommit.com/blog/misc/xmlbuilder/testfile.rb>)