{%
laika.title = "Working with Scala's XML Support"
laika.metadata.date = "2010-05-24"
%}


# Working with Scala's XML Support

XML is probably one of Scala's most controversial language features (right behind unrestricted operator overloading). On the one hand, it's very nice to be able to simply embed XML fragments and XPath-like expressions within your Scala source code. At least, it's certainly a lot nicer than the string-literal approach that is required in many other languages. However, XML literals also complicate the syntax tremendously and pose endless difficulties for incremental syntax-aware editors such as IDEs.

Irrespective of the controversy though, XML literals are part of the language and they are here to stay. Martin Odersky has mentioned on multiple occasions that he half-regrets the inclusion of XML literal support, but he can't really do anything about it now that the language has taken hold and the syntax has solidified. So, we may as well make the best of it...

Unfortunately, Scala's XML library is very...weird. Especially in Scala 2.7. The class hierarchy is unintuitive, and there are odd pitfalls and correctness dangers just waiting to entrap the unwary. That fact, coupled with the lack of appropriate documentation in the language specification, leads to a very steep learning curve for new users. This is quite unfortunate, because a solid understanding of Scala's XML support is vital to many applications of the language, most notably the Lift web framework.

I can't personally do anything about the strangeness in the XML library. Like the literal syntax itself, it's too late to make many fundamental changes to the way XML works in Scala. However, I _can_ try to make it easier for beginners to get up and running with Scala's XML support.

### The Hierarchy

Before we get to literals and queries, it's important to have some idea of the shape of Scala's XML library and how its class hierarchy works. I found (and find) this to be the most unintuitive part of the entire ordeal.

![s6EW-5XuGuUAjHDi-zmvofQ.png](/assets/images/blog/wp-content/uploads/2010/05/s6EW-5XuGuUAjHDi-zmvofQ.png)

There are actually more classes than just this (such as `Document`, which extends `NodeSeq`, and `Unparsed`, which extends `Atom`), but you get the general idea. The ones I have shown are the classes which you are most likely to use on a regular basis.

Starting from the top, [`NodeSeq`](<http://www.scala-lang.org/docu/files/api/scala/xml/NodeSeq.html>) is probably the most significant class in the entire API. The most commonly used methods in the library are defined in the `NodeSeq` class, and most third-party methods which work with XML usually work at the level of `NodeSeq`. More specifically, `NodeSeq` defines the `\\` and `\` methods, which are used for XPath selection, as well as the `text` method, which is used to recursively extract all text within a particular set of nodes. If you're familiar with libraries like [Nokogiri](<http://nokogiri.org/>), you should be right at home with the functionality of these methods.

One particularly useful aspect of Scala's XML library is the fact that `NodeSeq` extends `Seq[Node]`. This means that you can use standard Scala collections operations to fiddle with XML (`map`, `flatMap`, etc). Unfortunately, more often than not, these methods will return something of type `Seq[_]`, rather than choosing the more specific `NodeSeq` when possible. This is something which _could_ have been solved in Scala 2.8, but has not been as of the latest nightly. Until this design flaw is rectified, the only recourse is to use the `NodeSeq.fromSeq` utility method to explicitly convert anything of type `Seq[Node]` back into the more specific `NodeSeq` as necessary:

```scala
val nodes: Seq[Node] = ...
val ns: NodeSeq = NodeSeq fromSeq nodes
```

Immediately deriving from `NodeSeq` is another landmark class in the Scala API, `Node`. At first glance, this may seem just a bit weird. After all, `Node` inherits from `NodeSeq` which in turn inherits from `Seq[Node]`. Thus, a single `Node` can also be viewed as a `NodeSeq` of length one, containing exactly itself. Yeah, that one took me a while...

Everything in the Scala XML library is a `NodeSeq`, and _almost_ everything is a `Node`. If you remember this fact, then you understand the entire API. The `Elem` class represents a single XML element with associated attributes and a `child` `NodeSeq` (which may of course be empty). The `Group` class is a bit of a hack and should never be used directly (use `NodeSeq.fromSeq` instead).

Of the `SpecialNode` hierarchy, only `Atom` deserves any special attention, and of its children, `Text` is really the most significant. `Text` is simply the way in which the Scala XML library represents text fragments within XML. Clearly, XML elements can have textual content, but since the `child`(ren) of an `Elem` have to be `Node`(s), we need some way of wrapping up a text `String` as a `Node`. This is where `Text` comes in.

It is worth noting that the `Atom` class actually takes a single type parameter. `Text` inherits from `Atom[String]`. I find this aspect of the API just a bit odd, since there aren't _any_ subclasses of `Atom` which inherit from anything other than `Atom[String]`, but that's just the way it is.

### Literals

Now that we've got the fundamental class hierarchy out of the way, it's time to look at the most visible aspect of Scala's XML support: XML literals. Most Scala web frameworks tend to make heavy use of XML literals, which can be a bit annoying due to the difficulties they cause most editors (I'm still trying to get the [jEdit support](<http://github.com/djspiewak/jedit-modes/blob/master/scala.xml>) nailed down). Even still, XML literals are a very useful part of the language and almost essential if you're going to be working with XML content.

Fortunately, Scala's XML syntax is as intuitive to write as it is difficult to parse:

```scala
val ns = <span id="foo"><strong>Hello,</strong> World!</span>
println(ns.toString)      // prints the raw XML
```

The thing to remember is that any time text appears after the `<` operator without any whitespace, Scala's parser will jump into "XML mode". Thus, the following code is _invalid_ , even though it seems like it should work:

```scala
val foo = new {
  def <(a: Any) = this
}

foo <foo          // error!
```

**< rant>**This is yet another example of Scala's compiler behaving in strange and unintuitive ways due to arbitrary resolution of ambiguity in the parser. The correct way to handle this would be for the parser to accept the local ambiguity (XML literal vs operator and value reference) and defer the resolution until a later point. In this case, the final parse tree would be unambiguous (there is no way this could correctly parse as an XML fragment), so there's no danger of complicating later phases like the type checker. Unfortunately, Scala's parser (as it stands) is not powerful enough to handle this sort of functionality. [*sigh*](<http://citeseer.ist.psu.edu/visser97scannerles.html>) **< /rant>**

Scala's XML literal syntax is actually sugar for a series of `Elem` and `Text` instantiations. Specifically, Scala will parse our earlier example as the following:

```scala
val ns = Elem(null, "span", new UnprefixedAttribute("id", Text("foo"), Null), TopScope, 
  Elem(null, "strong", Null, TopScope, Text("Hello,")), Text(" World!"))

println(ns.toString)
```

You will notice that the attribute value is actually wrapped in a `Text` node. This is necessary because attributes can be returned from XPath selectors, which always return values of type `NodeSeq`. Thus, the content of an attribute must be of type `Node`. Unfortunately, this opens up a rather obvious hole in the type safety of the API: the compiler will allow you to store _any_ `Node` within an attribute, including something of type `Elem`. In fact, you won't even get an exception at runtime! The following code compiles and runs just fine:

```scala
new UnprefixedAttribute("id", <foo/>, Null)
```

The good news is that you will almost never use `UnprefixedAttribute` directly, mostly because the API is so clumsy. Most of the time, you will spend your time either consuming pre-baked XML coming in from some external source, or synthesizing it yourself using literals.

Of course, not all XML is fully-known at compile time. In fact, most often XML is just a structured wrapper around some data which is produced dynamically. To that end, Scala provides a convenient syntax for XML interpolation. This makes it possible to construct XML dynamically based on variables and expressions. For example, we might want to make the `id` attribute of the `foo` element dynamic based on some method parameter:

```scala
def makeXML(id: String) = <span id={ id }><strong>Hello,</strong> World!</span>

makeXML("foo")        // => <span id="foo">...</span>
```

The interpolation syntax is actually fairly generous about what you are allowed to embed. By default, any values within the `{ ... }` markers will first be converted to a `String` (using its `toString` method) and then wrapped in a `Text` before embedding in the XML. However, if the expression within the braces is _already_ of type `NodeSeq`, the interpolation will simply embed that value without any conversion. For example:

```scala
val ns1 = <foo/>
val ns2 = <bar>{ ns1 }</bar>       // => <bar><foo/></bar>
```

You can even embed something of type `Seq[Node]` and the interpolation will "do the right thing", flattening the sequence into an XML fragment which takes the place of the interpolated segment:

```scala
val xs = List(<foo/>, <bar/>)
val ns = <baz>{ xs }</baz>          // => <baz><foo/><bar/></baz>
```

These auto-magical interpolation features are incredibly useful when assembling XML from multiple sources. Their only downside is the fact that the Eclipse IDE v2.7 _really_ struggles with XML literals and interpolated expressions in particular. My recommendation: if you need to work with XML literals, either avoid Eclipse entirely or be careful to wrap _all_ XML literals in parentheses (like this: `(<foo><bar/></foo>)`). Note that the 2.8 version of the Scala IDE for Eclipse doesn't impose this requirement.

### XPath

Of course, creating XML is really only half the story. In fact, it's actually much less than that. In practice, most XML-aware applications spend the majority of their time processing XML, not synthesizing it. Fortunately, the Scala XML API provides some very nice functionality in this department.

For starters, it is possible to perform XPath-like queries. I say "XPath-like" because it's really not quite as nice as XPath, nor as full-featured. Sometimes it takes several chained queries to perform the same action as a single, compound XPath query. However, despite its shortcomings, Scala's XPath support is still dramatically superior to manual DOM walking or SAX handling.

The most fundamental XML query operator is `\` (bear in mind that all XML operators are defined on `NodeSeq`). This operator applies a given `String` pattern to the _direct_ descendants of the target `NodeSeq`. For example:

```scala
val ns = <foo><bar><baz/>Text</bar><bin/></foo>

ns \ "foo"              // => <foo><bar>...</bar><bin/></foo>
ns \ "foo" \ "bar"      // => <bar><baz/>Text</bar>
```

As you can see, the most generic pattern which can be fed into the `\` operator is simply the name of the element. All XML operators return `NodeSeq`, and so it's very easy and natural to chain multiple operators together to perform chained queries.

However, we don't always want to chain scores of `\` operators together to get at a single deeply-nested element. In this case, we might be better served by the `\\` operator:

```scala
val ns = <foo><bar><baz/>Text</bar><bin/></foo>

ns \\ "bar"          // => <bar><baz/>Test</bar>
ns \\ "baz"          // => <baz/>
```

Essentially, `\\` behaves exactly the same as `\` except that it recurses into the node structure. It will return _all_ possible matches to a particular pattern within a given `NodeSeq`. Thus, if a pattern matches a containing element as well as one of its children, _both_ will be returned:

```scala
val ns = <foo><foo/><foo>

ns \\ "foo"          // => <foo><foo/></foo><foo/>
```

The `NodeSeq` returned from the `ns \\ "foo"` query above actually has two elements in it: `<foo><foo/></foo>` as well as `<foo/>`. This sort of recursive searching is very useful for drilling down into deeply nested structures, but its unconstrained nature makes it somewhat dangerous if you aren't _absolutely_ sure of the depth of your tree. Just as a tip, I generally confine myself to `\` unless I know that the node name in question is truly unique across the entire tree.

In addition to simply selecting elements, Scala also makes it possible to fetch attribute names using its XML selectors. This is done by prefixing the name of the attribute with '`@`' in the selector pattern:

```scala
val ns = <foo id="bar"/>

ns \ "@id"        // => Text(bar)
```

One minor gotcha in this department: the `\` _always_ returns something of type `NodeSeq`. Thus, the results of querying an attribute value are actually of type `Text`. If you want to get a `String` out of an attribute (and most of us do), you will need to use the `text` method:

```scala
(ns \ "@id").text         // => "bar"
```

Take care though that your selector is only returning a _single_ `Text` node, otherwise invoking the `text` method will concatenate the results together. For example:

```scala
val ns = <foo><bar id="1"/><bar id="2"/></foo>

(ns \\ "@id").text          // => "12"
```

Unlike XPath, Scala does _not_ allow you to query for specific attribute values (e.g. `"@id=1"` or similar). In order to achieve this functionality, you would need to first query for all `id` values and then `find` the one you want:

```scala
ns \\ "@id" find { _.text == "1" }        // => Some("1")
```

Also unlike XPath, Scala does not allow you to query for attributes associated with a particular element name in a single pattern. Thus, if you want to find only the `id` attributes from `bar` elements, you will need to perform two chained selections:

```scala
ns \\ "bar" \ "@id"
```

Oh, and one fun added tidbit, Scala's XML selectors also define a wildcard character, underscore (`_`) of course, which can be used to substitute for any _element_ name. However, this wildcard cannot be used in attribute patterns, nor can it be mixed into a partial name pattern (e.g. `ns \ "b_"` will not work). Really, the wildcard is useful in conjunction with a purely-`\` pattern when attempting to "skip" a level in the tree without filtering for a particular element name.

Despite all of these shortcomings, Scala's almost-XPath selectors are still very useful. With a little bit of practice, they can be an extremely effective way of getting at XML data at arbitrary tree depths.

### Pattern Matching

What nifty Scala feature would be complete without some form of pattern matching? We can `match` on `String` literals, `Int` literals, and `List` literals; why not XML?

```scala
<foo/> match {           // prints "foo"
  case <foo/> => println("foo")
  case <bar/> => println("bar")
}
```

As we would expect, this code evaluates and prints `foo` to standard out. Unfortunately, things are not all sunshine and roses. In fact, pattern matching is where Scala's XML support gets decidedly weird. Consider:

```scala
<foo>bar</foo> match {   // throws a MatchError!
  case <foo/> => println("foo")
  case <bar/> => println("bar")
}
```

The problem is that when we define the pattern, `<foo/>`, we're actually telling the pattern matcher to match on _exactly_ an empty `Elem` with label, `foo`. Of course, we can fix this by adding the appropriate to our pattern:

```scala
<foo>bar</foo> match {   // prints "foo"
  case <foo>bar</foo> => println("foo")
  case <bar>bar</bar> => println("bar")
}
```

Ok, that's a little better, but we rarely know exactly what the contents of a particular node is going to be. In fact, the whole reason we're pattern matching on this stuff is to extract data we don't already have, so maybe a more useful case would be matching on the `foo` element and printing out its contents:

```scala
<foo>mystery</foo> match {   // prints "foo: mystery"
  case <foo>{ txt }</foo> => println("foo: " + txt)
  case <bar>{ txt }</bar> => println("bar: " + txt)
}
```

Ok, that worked, and it used our familiar interpolation syntax. Let's try something fancier. What if we have text _and_ an element inside our `Elem`?

```scala
<foo>mystery<bar/></foo> match {   // throws a MatchError!
  case <foo>{ txt }</foo> => println("foo: " + txt)
  case <bar>{ txt }</bar> => println("bar: " + txt)
}
```

Like I said, decidedly weird. The problem is that the `txt` pattern is looking for one `Node` and one `Node` only. The `Elem` we're feeding into this pattern has two child `Node`(s) (a `Text` and an `Elem`), so it doesn't match any of the patterns and throws an error.

The solution is to remember the magic of Scala's `@` symbol within patterns:

```scala
<foo>mystery<bar/></foo> match {   // prints "foo: ArrayBuffer(mystery,<bar></bar>)"
  case <foo>{ ns @ _* }</foo> => println("foo: " + ns)
  case <bar>{ ns @ _* }</bar> => println("bar: " + ns)
}
```

Closer, but still not right. If we were to examine the types here, we would see that `ns` is actually not a `NodeSeq`, but a `Seq[Node]`. This means that even if we weren't naïvely printing out our match results, we would still have problems attempting to use XML selectors or other `NodeSeq`-like operations on `ns`.

To get around this problem, we have to explicitly wrap our results in a `NodeSeq` using the utility method mentioned earlier:

```scala
<foo>mystery<bar/></foo> match {   // prints "foo: mystery<bar></bar>"
  case <foo>{ ns @ _* }</foo> => println("foo: " + NodeSeq.fromSeq(ns))
  case <bar>{ ns @ _* }</bar> => println("bar: " + NodeSeq.fromSeq(ns))
}
```

Success at last! Now let's try some attributes. To make things easier, we'll pattern match on static values rather than trying to actually extract data:

```scala
<foo id="bar"/> match {
  case <foo id="bar"/> => println("bar")      // does not compile!
  case <foo id="baz"/> => println("baz")
}
```

As the comment says, this snippet doesn't compile. Why? Because Scala doesn't support XML patterns with attributes. This is a _horrible_ restriction and one that I run up against almost daily. Even from a strictly philosophical sense, pattern matching should be symmetric with the literal syntax (just like `List` and the `::` operator). We've already seen one instance of asymmetry in XML pattern matching (child extraction), but this one is far worse.

The only way to pattern match in an attribute-aware sense is to use pattern guards to explicitly query for the attribute in question. This leads to vastly more obfuscated patterns like the one shown below:

```scala
<foo id="bar"/> match {       // prints "bar"
  case n @ <foo/> if (n \ "@id" text) == "bar" => println("bar")
  case n @ <foo/> if (n \ "@id" text) == "baz" => println("baz")
}
```

This situation is also somewhat confusing when attempting to read code which uses pattern matching and branches on attributes. I'm constantly tripping over this when I look back at even my own code, mostly because it looks for all the world like we're matching on a `foo` element with no attributes! Very frustrating.

Oh, and one final added goodie: namespaces. Pattern matching on an unqualified element (e.g. `<foo/>`) will match not only exactly that element name, but also any namespaced permutations thereof:

```scala
<w:gadget/> match {       // prints "gadget"
  case <gadget/> => println("gadget")
}
```

If you want to match a specific namespace, you need to include it in the pattern:

```scala
<w:gadget/> match {       // prints "w:gadget"
  case <m:gadget/> => println("m:gadget")
  case <w:gadget/> => println("w:gadget")
}
```

In practice, this is actually fairly useful, but it's still another head-scratcher in the Scala XML design. I know I struggled with this as a beginner, and I can't imagine it's that much easier for anyone else.

### Concurrency Pitfalls

One thing we (at Novell) learned the hard way is that Scala's XML library is _not_ thread-safe. Yes, XML literals are immutable, but this alone is not sufficient. Even though the API is immutable (doesn't provide a way to change an XML literal in-place), the underlying data structures are not. Observant readers will have caught this fact from our pattern matching example earlier, when we mistakenly printed "`ArrayBuffer(mystery,<bar></bar>)`".

`ArrayBuffer` is a little like Scala's answer to Java's `ArrayList`. It's pretty much the defacto mutable `Seq` implementation. Under the surface, it's using an asymptotically-growing dynamic array to store its data, providing constant-time read and append. Unfortunately, like all array-based data structures, `ArrayBuffer` suffers from volatility issues. Unsynchronized use across multiple threads involving mutation (even copy mutation like the `++` method) can result in undefined behavior.

The good news is that this problem is fixed in Scala 2.8. The bad news is that a lot of people are still stuck on 2.7. For now, the only solution is to ensure that you never access a single XML value concurrently. This either requires locking or extra data copying to ensure that no two threads have the same copy of a particular `NodeSeq`. Needless to say, neither solution is ideal.

### Conclusion

Scala's XML support is flaky, inconsistent and arguably a bad idea in the first place. However, the fact that it's already part of the language means that it's a little late to bring up inherent design flaws. Instead, we should focus on all that's good about the library, like the convenience of a very straightforward literal syntax and the declarative nature of almost-XPath selectors. I may not like everything about Scala's XML support — for that matter, I may not like _most_ of Scala's XML support — but I can appreciate the benefits to XML-driven applications and libraries such as Lift. Hopefully, this brief guide will help you avoid some of the pitfalls and reap the rewards of XML in Scala with a minimum of casualties.