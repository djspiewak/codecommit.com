{%
laika.title = "Pipe Dream: Static Analysis for Ruby"
laika.metadata.date = "2008-06-30"
%}


# Pipe Dream: Static Analysis for Ruby

Yes, yes I know: Ruby is a dynamic language.  The word "static" is literally opposed to everything the language stands for.  With that said, I think that even Ruby development environments would benefit from some simple static analysis, just enough to catch the really idiotic errors.

Here's the crux of the problem: people don't test well.  Even with nice, [behavior-driven development](<http://www.codecommit.com/blog/java/the-brilliance-of-bdd>) as facilitated by frameworks like RSpec, very few developers sufficiently test their code.  This isn't just a problem with dynamic languages either, no one is safe from the test disability.  In some ways, it's a product of laziness, but I think in most cases, good developers just don't want to work on mundane problems.  It's _boring_ having to write unit test after unit test, checking and re-checking the same snippet of code with different input.

In some sense, it is this problem that compilers and static type systems try to avert, at least partially.  The very purpose of a static type system is to be able to prove certain things about your code simply by analysis.  By enabling the compiler to say things using the type system, the language is providing a safety net which filters out ninety percent of the annoying "no-brainer" mistakes.  A simple example would be invoking a method with the wrong parameters; or worse yet, misspelling the name of the method or type altogether.

The problem is that there are some problems which are more simply expressed in ways which are not provably sound.  In static languages, we get around this by casting, but such techniques are ugly and obviously contrived.  It is this problem which has given rise to the kingdom of dynamic languages; it is for this reason that most scripting languages have dynamic type systems: simple expression of algorithm without worrying about provability.  In fact, there are so many problems which do not fit nicely within most type systems that many developers have chosen to eschew static languages altogether, claiming that static typing just gets in the way.

Unfortunately, by abandoning static types, these languages lose that typo safety net.  It's too easy to make a trivial mistake in a dynamic language, buried somewhere deep in the bowls of your application.  This mistake could easily be averted by a compiler with validating semantic analysis, but in a dynamic language, such a mistake could go unnoticed, conceivably even making it into production.  For this reason, most dynamic language proponents are also strong advocates of solid, comprehensive testing.  They have to be, for without such testing, one should never trust dynamic code in a production system (or any code, for that matter, but especially the unchecked dynamic variety).

Most large, production systems written in languages like Ruby or Groovy have large test suites which sometimes take hours to run.  These suites are extremely fine-grained, optimally checking every line of code with every possible kind of input, so as to be sure that mistakes are caught.  This is where the flexibility of dynamic typing really comes back to haunt you: extra testing is required to ensure that silly mistakes don't slip through.  The irony is that a lot of developers using dynamic languages do so to get away from the "nuisance" of compilation, when all they have done is trade one inconvenience for another (testing).

Given this situation, it's not unreasonable to conclude that what dynamic languages really need is a tool which can look through code and find all of those brain-dead mistakes.  Such a tool could be run along with the normal test suite, finding and reporting errors in much the same way.  It wouldn't really have to be a compiler, so the tool wouldn't slow down the development process, it would just be an effective layer of automated white-box testing.

But how could such a thing be accomplished in a language like Ruby?  After all, it is a truly dynamic language.  Methods don't even exist until runtime, and sometimes only if certain code paths are run.  Types are completely undeclared, and every object can potentially respond to any method.  The answer is to perform extremely permissive inference.

It was actually a recent post by James Ervin on [the nomenclature of type systems](<http://iacobus.blogspot.com/2008/06/dont-call-it-static-or-dynamic-language.html>) which got me thinking along these lines.  It should be possible by static analysis to infer the _structural_ type of any value based on its usage.  Consider:

```ruby
def do_something(obj)
  if obj.to_i == 0
    obj[:test]
  else
    other = obj.find :name => 'Daniel'
    other.to_s
  end
end
```

Just by examining this code, we can say certain things about the types involved.  For instance, we know that `obj` must respond to the following methods:

  * `to_i`
  * `[Symbol]`
  * `find(Hash)`

In turn, we know that the `find(Hash)` method must return a value which defines `to_s`.  Of course, this last bit of information isn't very useful, because every object defines that method, but it's still worth the inference.  The really useful inference which comes out of `to_s` is the knowledge that this method _sometimes_ returns a value of type `String` (making the assumption that `to_s` hasn't been redefined to return a different type, which isn't exactly a safe assumption).  At other times, `do_something` will return whatever value comes from the square bracket operator (`[]`) on `obj`.  This bit of information we must remember in the analysis.  We can't just assume that this method will return a `String` all the time, even if `to_s` does because method return types need not be homogeneous in dynamic languages.

Now, at this point we have effectively built up a structural type which is accepted by `do_something`.  Literally, we have formalized in the analysis what our intuition has already told us about the method.  There are some gaps, but that is to be expected.  The key to this analysis is _not_ attempting to be comprehensive.  Dynamic languages cannot be analyzed as if they were static, one must expect to have certain limitations.  In such situations where the analysis is insufficient, it must assume that the code is valid, otherwise there will be thousands of false positives in the error checking.

So what is it all good for?  Well, imagine that somewhere else in our application, we have the following bit of code:

```ruby
do_something 42
```

This is something we _know_ will fail, because we have a simple value (42) which has a nominal type we can easily infer.  A little bit of checking on this type reveals the fact that it does not define square brackets, nor does it define a `find(Hash)` method.  This finding could be reported as an error by the analysis engine.

Granted, we still have to account for the fact that Ruby has things like `method_missing` and open classes, but all of this can fall into the fuzzy area of the analysis.  In situations where it _might_ be alright to pass an object which does not satisfy a certain aspect of the structural type, the analysis must let it pass without question.

You can imagine how this analysis could traverse the entire source tree, making the strictest inferences it can and allowing for dynamic fuzziness where applicable.  Since the full sources of every Ruby function, class and module are available at runtime, analysis could be performed without any undue concern regarding obfuscation or parsing of binaries.  Conceivably, most trivial errors could be caught without any tests being written, taking some of the burden off of the developer.  There is a slight concern that developers would build up a false sense of security regarding their testing (or lack thereof), but I think we just have to trust that won't happen, or won't last long if it does.

Most advanced Ruby toolsets already have an analysis somewhat similar to the one I outlined.  NetBeans Ruby for example has some fairly advanced nominal type inference to allow things like semantic highlighting and content assist.  But as far as I know, this type inference is only nominal, and fairly local at that.  The structural type inference that I am proposing could conceivably provide far better assurances and capabilities than mere nominal inference, especially if enhanced through successive iteration and a more "global" approach (similar to Hindley/Milner in static languages).

One thing is certain, it isn't working to just rely on developers being conscientious with their testing.  With the rapid rise in production systems running on dynamic languages, it is in all of our best interests to try to find a way to make these systems more stable and reliable.  The best way to do this is to start with code assurance and try to make it a little less painful to catch mistakes _before_ deployment.