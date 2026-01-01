{%
laika.title = "The Plague of Polyglotism"
laika.metadata.date = "2008-04-28"
%}


# The Plague of Polyglotism

For those of you who don't know, polyglotism is not some weird religion but actually a growing trend in the programming industry.  In essence, it is the concept that one should not be confined to a single language for a given system or even a specific application.  With polyglot programming, a single project could use dozens of different languages, each for a different task to which they are uniquely well-suited.

As a basic example, we could write a Wicket component which makes use of Ruby's RedCloth library for working with Textile.  Because of Scala's flexible syntax, we can use it to perform the interop between Wicket and Ruby using an [internal DSL](<http://www.codecommit.com/blog/ruby/jruby-interop-dsl-in-scala>):

```scala
class TextileLabel(id:String, model:IModel) extends WebComponent(id, model) with JRuby {
  require("textile_utils")
  
  override def onComponentTagBody(stream:MarkupStream, openTag:ComponentTag) {
    replaceComponentTagBody(markupStream, openTag, 
        'textilize(model.getObject().toString()))
  }
}
```

```ruby
# textile_utils.rb
require 'redcloth'

def textilize(text)
  doc = RedCloth.new text
  doc.to_html
end
```

**Warning:** Untested code

We're actually using three languages here, even though we only have source for two of them.  The Wicket library itself is written in Java, our component is written in Scala and we work with the RedCloth library in Ruby.    This is hardly the best example of polyglotism, but it suffices for a simple illustration.  The general idea is that you would apply this concept to a more serious project and perform more significant tasks in each of the various languages.

### The Bad News

This is all well and good, but there's a glaring problem with this philosophy of design: not everyone knows _every_ language.  You may be a language aficionado, picking up everything from Scheme to Objective-C, but it's only a very small percentage of developers who share that passion.  Many projects are composed of developers without extensive knowledge of diverse languages.  In fact, even with a _really_ good sampling of talent, it's doubtful you'll have more than one or two people fluent in more than two languages.  And unfortunately, there's this pesky concern we all love called "maintainability".

Let's pretend that Slava Pestov comes into your project as a consultant and decides that he's going to apply the polyglot programming philosophy.  He writes a good portion of your application in Java, Lisp and some language called [Factor](<http://factorcode.org/>), pockets his consultant's fee and then moves on.  Now the code he wrote may have been phenomenally well-designed and really perfect for the task, but you're going to have a very hard time finding a developer who can maintain it.  Let's say that six months down the road, you decide that your widget really needs a red push button, rather than a green radio selector.  Either you need a developer who knows Factor (hint: there aren't very many), or you need a developer who's willing to learn it.  The thing is that most developers with the knowledge and motivation to learn a language have either already done so, or are familiar enough with the base concepts as to be capable of jumping right in.  These developers fall into that limited group of people fluent in many different languages, and as such are a rare find.

Now I'm not picking on Factor in any way, it's a very interesting language, but it still isn't very widespread in terms of developer expertise.  That's really what this all comes down to: developer expertise.  Every time you make a language choice, you limit the pool of developers who are even capable of groking your code.  If I decide to build an application in Java, even assuming that's the _only_ language I use, I have still eliminated maybe 20% of all developers from ever touching the project.  If I make the decision to use Ruby for some parts of the application, while still using Java for the others, I've now factored that 80% down to maybe 35% (developers who know Java _and_ Ruby).  Once I throw in Scala, that cuts it down still further (maybe at 15% now).  If I add a _fourth_ language - for example, Haskell - I've now narrowed the field so far, that it's doubtful I'll find anyone capable of handling all aspects within a reasonable price range.  It's the same problem as with framework choice, except that frameworks are much easier to learn than languages.

The polyglot ideal was really devised by a bunch of nerdy folks like me.  I _love_ languages and would like nothing better than to get paid to learn half a dozen new ones (assuming I'm coming into a project with a strange combination I haven't seen before).  However, as I understand the industry, that's not a common sentiment.  So a very loud minority of developers (`/me waves`) has managed to forge a very hot methodology, one which excludes almost all of the hard-working developer community.  If I didn't know better, I would be tempted to say that it was a self-serving industry ploy to foster exclusivity in the job market.

I want to work on multi-language projects as much as anyone, but I really don't think it's the best thing right now.  I'm working on a project now which has an aspect for which Scala would be absolutely perfect, but since I'm the only developer on hand who is remotely familiar with the language, I'm probably going to end up recommending _against_ its adoption.  Consider carefully the ramifications of trying new languages on your own projects, you may not be doing future developers any favors by going down that path.