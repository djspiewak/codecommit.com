{%
laika.title = "Wicket TextileLabel"
laika.metadata.date = "2007-07-08"
%}


# Wicket TextileLabel

Well, first I must apologize for not updating my blog in some time. Loads of interesting (and time consuming) things have been happening recently, specifically related to my employment as well as a rather jam-packed holiday week in the US. On a slightly different (but related) note, I resigned from my full time job and am once again "programmer for hire" (if you're interested, you can drop me an email: djspiewak \[AT\] gmail). Thankfully, all the closing details seem to be in order, so I'll finally have a little more time to devote to this blog as well as to writings on EclipseZone. In that vein I've spent a bit more time with Wicket (now Apache Wicket) recently, and I have to confess even more impressed with it than I was a year ago when I first looked at it. The separation of markup and code is a powerful concept that no other framework seems to have achieved to the same level without massive libraries of custom tags. For those of you who aren't already familiar with Wicket, it's an Open Source, component-based web framework. You create pages in Wicket by writing a standard HTML file, adding a **wicket:id="yadayada"** attribute to the dynamic elements, then you add the corresponding component instances to the **Page** instance in code. No Java in your HTML, no HTML in your Java. One of the things I stumbled upon in my latest run at the Wicket is the limitations of its **MultiLineLabel** component. MultiLineLabel lets you display large blocks of text with the characters appropriately escaped and all line breaks converted to proper **< p></p>** blocks. It's not a complicated component, and this is glaringly obvious if you actually need something a bit more substantial. The site I'm using to experiment has a need for large blocks of (preferably) formatted text. At first, I figured I'd just throw a **MultiLineLabel** up and call it done. However, the need for formatting seemed a bit more pressing, so I began to look at alternatives. And it occurred to me that perhaps the simplest way to enter formatted text is to use [Textile](<http://en.wikipedia.org/wiki/Textile>). Unfortunately, this means I need some way to render Textile into HTML within Wicket. After some Googling, I was able to positively ascertain that there are no Wicket components which provide Textile rendering. Not being one to give up there, I decided to roll my own. After all, one of Wicket's major selling points is that it makes custom components dead easy, right? Well, seems the hype is justified in this area too. Although, I must admit the documentation is sorely lacking in this area. I ended up cracking open the source for **MultiLineLabel** (which was surprisingly readable) and discovering that the key is to override the **onComponentTagBody** method. With a little more Googling, I found [PLextile](<http://www.plink-search.com/blog.html?fetch=developer&get=PLextile>), which is the most complete Java Textile rendering library. A few minutes of quick hacking later, and I came up with this: 

```java
protected void onComponentTagBody(MarkupStream markupStream, ComponentTag openTag) {
    String text = (String) getModel().getObject();
    if (text == null) {
        text = "";
    }

    replaceComponentTagBody(markupStream, openTag, postTextilize(new TextParser().parseTextile(text, true)));
}
```

PLextile handles all the heavy lifting here, and parses out just about everything just fine. It's main stumbling point is how it handles line breaks. According to Why's [comprehensive Textile reference](<http://hobix.com/textile>), line breaks should be handled by wrapping the sections into **< p></p>** blocks, whereas PLextile was inserting **< br/>** tags. Quite frustrating, let me tell you. I considered using RedCloth (Why-the-Lucky-Stiff's Ruby Textile renderer, wrapped by Rails for the famous **textilize** method) through JRuby and Java 6 embedded scripting, but it seemed awfully heavy to fire up an entire JRuby interpreter instance just to parse some text, so the decision was made to steer away from that. Instead, I wrote a post-processor for PLextile (hence the _postTextilize_ method in the example above). This method is actually where most of the code is for the component: 

```java
private String postTextilize(String textile) {
    textile = "<p>" + textile + "</p>";
    textile = textile.replace("<br />\\r<br />", "</p><p>");
    textile = textile.replace("<br />", "");

    return textile;
}
```

Anyway, wrap it all up into a **WebComponent** subclass and it's ready to use in a page. Swap **TextileLabel** for your former **MultiLineLabel** usages, and you're ready to go! You can download the finished component [here](<http://blogs.dzone.com/daniel/files/2007/07/textilelabel.zip>).