{%
laika.title = "Defining High, Mid and Low-Level Languages"
laika.metadata.date = "2008-02-27"
%}


# Defining High, Mid and Low-Level Languages

I've been writing quite a bit recently about the differences between languages.  Mostly I've just been whining about how annoying it is that everyone keeps searching for the "one language to rule them all", the Aryan Language if you will.  Over the course of some of these articles, I've made some rather loosely defined references to terms like "general purpose" and "mid-level" when trying to describe these languages. 

Several people have (rightly) called me out on these terms, arguing that I haven't really defined what they mean, so I shouldn't be using them to try to argue a certain point.  In the case of "general purpose language", I have to admit that I tend to horribly misuse the term and any instances within my writing should be discarded without thought.  However, I think with a little bit of reflection, we can come to some reasonable definitions for high-, mid- and low-level languages.  To that end, I present the "Language Spectrum of Science!" ([cue reverb](<http://en.wikipedia.org/wiki/Bill_Nye_the_Science_Guy>))

![Language Spectrum of Science](/assets/images/blog/wp-content/uploads/2008/02/image2.png) 

This scale is admittedly arbitrary and rather loosely defined in and of itself, but I think it should be a sufficient visual aid in conveying my point.  In case you hadn't guessed, red languages are low-level, green languages are high-level and that narrow strip of yellow represents the mid-level languages.  Obviously I'm leaving out a large number of languages which could be represented with equal validity, but I only have a finite number of pixels in page-width.

The scale is also somewhat myopic.  It defines Ruby as the highest of the high-level languages.  Very few could argue the other side of the scale since there's not really anything lower than the hardware, but claiming that Ruby is the most high-level language in history seems somewhat odd.  In truth, I picked Ruby as the super high-level language mainly because it's a) more dynamic than both JavaScript and Perl, b) more prone to RAD frameworks like Rails and c) it's the most significant high-level language which I'm _really_ familiar with.

It's also important to note that languages aren't really points on the spectrum, but rather they span ranges which are more or less wide, depending on the capabilities.  These ranges may overlap considerably (as in the case of Java and Scala) or may be entirely disjoint (Assembly and Ruby).  In short, the scale is somewhat blurry and shouldn't be taken as a canonical reference.

### Low-Level

Of all of the categories, it's probably easiest to define what it means to be a low-level language.  Machine code is low level because it runs directly on the processor.  Low-level languages are appropriate for writing operating systems or firmware for micro-controllers.  They can do just about _anything_ with a little bit of work, but obviously you wouldn't want to write the next major web framework in one of them (I can see it now, "Assembly on Rails").

#### Characteristics

  * Direct memory management
  * Little-to-no abstraction from the hardware
  * Register access
  * Statements usually have an obvious correspondence with clock cycles
  * Superb performance

C is actually a very interesting language in this category (more so C++) because of how broad its range happens to be.  C allows you direct access to registers and memory locations, but it also has a number of constructs which allow significant abstraction from the hardware itself.  Really, C and C++ probably represent the most broad spectrum languages in existence, which makes them quite interesting from a theoretical standpoint.  In practice, both C and C++ are too low-level to do anything "enterprisy".

### Mid-Level

This is where things start getting vague.  Most high-level languages are well defined, as are low-level languages, but mid-level languages tend to be a bit difficult to box.  I really define the category by the size of application I would be willing to write using a given language.  I would have no problem writing and maintaining a large desktop application in a mid-level language (such as Java), whereas to do so in a low-level language (like Assembly) would lead to unending pain.

This is really the level at which virtual machines start to become common-place.  Java, Scala, C# etc all use a virtual machine to provide an execution environment.  Thus, many mid-level languages don't compile directly down to the metal (at least, not right away) but represent a blurring between interpreted and compiled languages.  Mid-level languages are almost always defined in terms of low-level languages (e.g. the Java compiler is bootstrapped from C).

#### Characteristics

  * High level abstractions such as objects (or functionals)
  * Static typing
  * Extremely commonplace (mid-level languages are by far the most widely used)
  * Virtual machines
  * Garbage collection
  * Easy to reason about program flow

### High-Level

High-level languages are really interesting if you think about it.  They are essentially mid-level languages which just take the concepts of abstraction and high-level constructs to the extreme.  For example, Java is mostly object-oriented, but it still relies on primitives which are represented directly in memory.  Ruby on the other hand is _completely_ object-oriented.  It has no primitives (outside of the runtime implementation) and everything can be treated as an object.

In short, high-level languages are the logical semantic evolution of mid-level languages.  It makes a lot of sense when you consider the philosophy of simplification and increase of abstraction.  After all, people were _n_ times more productive switching from C to Java with all of its abstractions.  If that really was the case, then can't we just add more and more layers of abstraction to increase productivity exponentially?

High-level languages tend to be extremely dynamic.  Runtime flow is changed on the fly through the use of things like dynamic typing, open classes, etc.  This sort of technique provides a tremendous amount of flexibility in algorithm design.  However, this sort of mucking about with execution also tends to make the programs harder to reason about.  It can be _very_ difficult to follow the flow of an algorithm written in Ruby.  This "obfuscation of flow" is precisely why I don't think high-level languages like Ruby are suitable for large applications.  That's just my opinion though.  :-)

#### Characteristics

  * Interpreted
  * Dynamic constructs (open classes, message-style methods, etc)
  * Poor performance
  * Concise code
  * Flexible syntax (good for internal DSLs)
  * Hybrid paradigm (object-oriented _and_ functional)
  * Fanatic community

Oddly enough, high-level language developers seem to be much more passionate about their favorite language than low- or mid-level developers.  I'm not entirely sure _why_ it has to be this way, but the trend has been far too universal to ignore (Python, Perl, Ruby, etc).  Ruby is of course the canonical example of this primarily because of the sky-rocket popularity of Rails, but any high-level language has its fanatic evangelists.

What's really interesting about many high-level languages is the tendency to fall into a hybrid paradigm category.  Python for example is extremely object-oriented, but also allows things like closures and first-class functions.  It's not as powerful in this respect as a language like Scala (which allows methods within methods within methods), but nevertheless it is capable of representing most elements of a pure-functional language.

As an aside, high-level languages usually perform poorly compared with low- or even mid-level languages.  This is merely a function of the many layers of abstraction between the code and the machine itself.  One instruction in Ruby may translate into literally thousands of machine words.  Of course, high-level languages are almost exclusively used in situations where such "raw-metal" performance is unnecessary, but it's still a language trait worth remembering.

### Conclusion

It's important to remember that I'm absolutely _not_ recommending one language or "level" over another for the general case.  The very reason we have such a gradient variety of language designs is that there is a need for all of them at some point.  The Linux kernel could never be written in Ruby, and I would never want to write an incremental backup system in Assembly.  All of these languages have their uses, it's just a matter of identifying which language matches your current problem most closely.