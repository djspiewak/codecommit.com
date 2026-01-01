{%
laika.title = "Scala as a Scripting Language?"
laika.metadata.date = "2008-11-03"
%}


# Scala as a Scripting Language?

I know, the title seems a bit...bizarre.  I don't know about you, but when I think of Scala, I think of many of the same uses to which I apply Java.   Scala is firmly entrenched in my mind as a static, [mid-level language](<http://www.codecommit.com/blog/java/defining-high-mid-and-low-level-languages>) highly applicable to things like large-scale applications and non-trivial architectures, but less so for tasks like file processing and system maintenance.   However, as I have been discovering, Scala is also _extremely_ well suited to basic scripting tasks, things that I would normally solve in a dynamic language like Ruby.

One particular task which I came across quite recently was the parsing of language files into [bloom filters](<http://www.codecommit.com/blog/scala/bloom-filters-in-scala>), which were then stored on disk.  To me, this sounds like a perfect application for a scripting language.  It's fairly simple, self-contained, involves a moderate degree of file processing, and should be designed, coded and then discarded as quickly as possible.   Dynamic languages have a tendency to produce working designs much faster than static ones, and given the fact that the use-case required access to a library written in Scala, JRuby seemed like the obvious choice (Groovy would have been a fine choice as well, but I'm more familiar with Ruby).  The result looked something like this:

```ruby
require 'scala'

import com.codecommit.collection.BloomSet

import java.io.BufferedOutputStream
import java.io.FileOutputStream

WIDTH = 2000000

def compute_k(lines, width)
  # ...
end

def compute_m(lines)
  #...
end

Dir.foreach 'wordlists' do |fname|
  unless File.directory? fname
    count = 0
    File.new "wordlists/#{fname}" do |file|
      file.each { |line| count += 1 }
    end
    
    optimal_m = compute_m(count)
    optimal_k = compute_k(count, WIDTH)
    
    set = BloomSet.new(optimal_m, optimal_k)
    
    File.new "wordlists/#{fname}" do |fname|
      file.each do |line|
        set += line.strip
      end
    end
    
    os = BufferedOutputStream.new FileOutputStream.new("gen/#{fname}")
    set.store os
    os.close
  end
end
```

As far as scripts go, this one isn't too bad.  I've written some real whoppers for things like video encoding and incremental backups.  The main trick here is the fact that we need to make two separate passes over the same file in order to get the number of lines before constructing the set.  We _could_ load the file into an array buffer in a single pass, count its length and then iterate over the array, placing each element in the bloom filter.   However, this really wouldn't be too much faster than just hitting the file twice (we still need two separate passes) and it has the additional drawback of requiring a fair amount of memory.

All in all, this script is a fairly natural representation of my requirements.   I needed to loop over a number of word lists, push the results into separate bloom filters and then freeze-dry the state.  However, look at what we've actually done here.  Remember earlier where we were considering which language to use?  We wanted a language which could concisely and quickly express our intent.  For that decision making process, we just _assumed_ that a dynamic language would suffice better than one hampered by a static type system.  However, at no point in the above script do we actually do anything _truely_ dynamic.  By that I mean: open classes, unfixed parameter types, `method_missing`, that sort of thing.   In fact, we haven't really done anything that we couldn't do in Scala:

```scala
import com.codecommit.collection.BloomSet
import java.io.{BufferedOutputStream, File, FileOutputStream}
import scala.io.Source

val WIDTH = 2000000

def computeK(lines: Int, width: Int) = // ...

def computeM(lines: Double) = // ...

for (file <- new File("wordlists").listFiles) {
  if (!file.isDirectory) {
    val src = Source.fromFile(file)
    val count = src.getLines.foldLeft(0) { (i, line) => i + 1 }
    
    val optimalM = computeM(count)
    val optimalK = computeK(count, optimalM)
    
    val init = new BloomSet[String](optimalM, optimalK)
    
    val set = src.reset.getLines.foldLeft(init) { _ + _.trim }
    
    val os = new BufferedOutputStream(new FileOutputStream("gen/" + file.getName))
    set.store(os)
    os.close()
  }
}
```

This is actually runnable Scala.  I'm not omitting boiler-plate or cheating in any similar respect.  If you copy this code into a `.scala` file and make sure that `BloomSet` is on your CLASSPATH (which you would have needed anyway for JRuby), you would be able to run the script _uncompiled_ using the `scala` command.  Unlike Java, Scala actually includes an "interpreter" which can parse raw Scala sources and execute the representative program just as if it had been pre-compiled using `scalac`.   One of the perquisites of this approach is the ability to simply omit any `main` method or `Application` class.  In nearly every sense of the word, Scala is a scripting language...as well as an enterprise-ready Java-killer (let the flames begin).

Now that we're fairly convinced that the above _is_ valid Scala, let's compare it with the original version of the script written using JRuby.  If we just go off LoC (Lines of Code), Scala actually wins here.  This was a more-than-slightly surprising discovery for me, given how often dynamic languages (and Ruby in particular) are touted as being more concise and expressive than static languages.  But of course, sheer LoC-brevity isn't everything: we also should consider things like readability.  A few characters of [Befunge](<http://esolangs.org/wiki/Befunge>) can accomplish more than I can do in several lines of Scala, but that doesn't mean I'll be able to figure out what it means tomorrow morning.

On the readability score, I think Scala wins here too.  The file processing and set creation is all done in a highly functional style (using `foldLeft`).   At least to my eyes, this is a lot easier to follow than the imperative form in Ruby.  More importantly, I think it's a bit harder to make silly mistakes.  When I wrote the Ruby version of the script, it took several tries before I solidly pinned down the exact incantation I was seeking.  The Scala version literally required only one revision after the initial prototype.   Granted, I had the Ruby version to go off of, but I think we would all agree that the scripts use some fairly different libraries and methodologies for accomplishing identical tasks.

So what is it that makes Scala so surprisingly well suited to the task of quick-and-dirty file processing and scripting?  After all, isn't is just a fancy syntax wrapping around the plain-old-Java standard library?  While it is true that Scala has first-class access to Java libraries (as demonstrated in the script), that isn't all that it offers.  I believe that Scala has two important features which make it so suitable for these tasks:

  * Type inference
  * Powerful core libraries

The first feature is of course evident wherever you look in the script.  With the exception of the two methods and the `BloomSet` constructor, we never actually declare a type anywhere in the script.  This gives the whole thing a very "dynamic feel" without actually sacrificing static type safety.   The first time you try this sort of language feature it is an almost euphoric experience (especially coming from highly-verbose languages like Java).

The second feature is a bit harder to see.  It is most evident in the way in which we handle file IO.  The directory listing is of course yet another application of the venerable `java.io.File` class, but the process of opening and reading the file line-by-line seems to be a lot easier than anything Java can muster.  This is made possible by Scala's `Source` API.   Rather than fiddling with `BufferedReader` and the whole menagerie that goes along with it, we just get a new `Source` from a `File` instance and then use conventional Scala methods to _iterate_ over its contents.  In fact, we're actually applying a functional idiom (fold) rather than a standard imperative iteration.  Finally, when we're done with our first pass, we don't need to re-open the file from scratch (inviting initialization mistakes in our coding), we just `reset` the `Source` and start from the beginning once more.

Using Scala as a scripting language comes with some pretty hefty benefits.   For one thing, you get immediate and idiomatic access to the mighty wealth of libraries which exist in Java.  Even for scripting, this sort of interoperability is invaluable.  JRuby does provide some excellent Java interop, but it simply can't compare to what you get with Scala.  Further, Scala has a static type system to check you (at runtime with a script) to ensure that you haven't done anything obviously bone-headed.  This too is nothing to sniff at.

Given the fact that Scala's "scripting syntax" is just as concise as Ruby's (sometimes more), it's hard to see a reason _not_ to employ it for around-the-server tasks.  Amusingly, the most compelling reason not to use Scala for scripting just might be its comment syntax.  Not having direct support for the magic "hash bang" (`#!`) incantation to define a file interpreter just means that Scala scripts have to go through some extra steps to be directly executable.  However, if immediately-executable scripts aren't an issue, you may want to consider Scala as your scripting language of choice for your next non-trivial outing.  You may reap the rewards in ways you weren't even expecting.