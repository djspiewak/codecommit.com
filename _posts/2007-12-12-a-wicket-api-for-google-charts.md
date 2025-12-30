---
categories:
- Java
date: '2007-12-12 01:00:26 '
layout: post
title: A Wicket API for Google Charts
wordpress_id: 167
wordpress_path: /java/a-wicket-api-for-google-charts
---

Google made the headlines last week with their announcement of a [brand new API](<http://code.google.com/apis/chart/>) for graphical display of data, supporting several different display types (line, bar, pie, etc).  The API (such as it is) works by taking different parameters as part of a URL and then generating a PNG chart based on the data.  For example:

``` http://chart.apis.google.com/chart?cht=p3&chd;=s:hW&chs;=250x100&chl;=Hello|World ``` ![](http://chart.apis.google.com/chart?cht=p3&chd=s:hW&chs=250x100&chl=Hello|World)

Great, so now you all know that I can read publicly available websites.  What's cool here is I can tweak the graph however I please, just by messing with the URL:

``` http://chart.apis.google.com/chart?cht=p3&chd;=s:hWj&chs;=250x100&chl;=Hello|Chicago|London ``` ![](http://chart.apis.google.com/chart?cht=p3&chd=s:hWj&chs=250x100&chl=Hello|Chicago|London)

Pretty magical!  But aside from playing with different ways to represent useless information in 3D, it's hard to imagine actually _using_ it in one of your own web applications.  Oh sure, maybe you'll employ it to generate the odd plot for your blog now and again, but for your really solid business needs, you're going to stick with something which has a slightly more developer-friendly API.

I was pondering different excuses I could use to convince myself to try this new tool somewhere when it struck me: what about [Wicket](<http://wicket.apache.org>)?  Wicket's entire structure is based around extreme component extensibility.  The framework is designed to allow developers to create their own third-party components and use them as easily as those in the core Wicket API.  In fact, to even use Wicket at all requires the creation of custom components, implicitly or otherwise.

So the idea is to create a nice, object-oriented _Chart_ component backed by Google Charts and usable in any Wicket project.  The API should follow the conventions laid down by the Wicket API itself, and be relatively easy to understand as well.  :-)  Obviously the Google Charts URL "API" follows a very strict set of constraints, thus it should be possible to render some sort of object-oriented MVC representation of a chart down into the required URL.  Thanks to Wicket componentization, it should even be possible to seamlessly utilize the component within a standard page.

I took some time today, and managed to prototype a fully-functional version of just such an API.  Cleverly enough, I'm calling it "wicket-googlecharts".  Thanks to the inherent complexities of charting and the difficulties of intuitively mapping the concepts into an object-oriented hierarchy, the API is still a bit odd.  However, I'm fairly confident that it's easier than doing the URLs by hand:

```java IChartData data = new AbstractChartData() { public double[][] getData() { return new double[][] {{34, 22}}; } }; ChartProvider provider = new ChartProvider(new Dimension(250, 100), ChartType.PIE_3D, data); provider.setPieLabels(new String[] {"Hello", "World"}); add(new Chart("helloWorld", provider)); data = new AbstractChartData() { public double[][] getData() { return new double[][] {{34, 30, 38, 38, 41, 22, 41, 44, 38, 29}}; } }; provider = new ChartProvider(new Dimension(200, 125), ChartType.LINE, data); ChartAxis axis = new ChartAxis(ChartAxisType.BOTTOM); axis.setLabels(new String[] {"Mar", "Apr", "May", "June", "July"}); provider.addAxis(axis); axis = new ChartAxis(ChartAxisType.LEFT); axis.setLabels(new String[] {null, "50 Kb"}); provider.addAxis(axis); add(new Chart("lineHelloWorld", provider)); ``` 

And the HTML:

```html4strict 

## Hello World

## Line Hello World

``` 

The resulting charts look like this:

![](http://chart.apis.google.com/chart?chs=250x100&chd=s:hW&cht=p3&chl=Hello|World)    ![](http://chart.apis.google.com/chart?chs=200x125&chd=s:helloWorld&cht=lc&chxt=x,y&chxl=0:|Mar|Apr|May|June|July|1:||50+Kb)

I know, pretty much the same charts that we have above. Not terribly impressive.  What should be impressive is that all of this was configured using Java.  I didn't have to understand the Google Charts URL structure.  I didn't have to even think about what goes on behind the scenes.  In fact, as far as the API is concerned, there could be an entirely different chart generating engine working under the surface, the developer doesn't care.

### 

### Architecture

The real component of interest here is the _Chart_ component that we add to the enclosing container at two different points (one instance for each chart).  This is where the actual work goes on, transforming the object hierarchy into a properly formatted URL. 

The class itself is surprisingly simple.  Basically it's just a bunch of logic that runs through the hierarchy represented in _IChartProvider_ (and yes, I am following the Wicket convention and using interfaces everywhere).  _Chart_ contains almost all of the semantics of the Google Charts API itself, providing a nice separation of concerns from a maintainability standpoint.  This way, if Google changes the API down the road (and you know they will), it's a simple matter of tweaking the _Chart_ class, allowing the rest of the hierarchy to remain untouched.

As far as Wicket is concerned, here's the only interesting bit in the entire API:

```java @Override protected void onComponentTag(ComponentTag tag) { checkComponentTag(tag, "img"); super.onComponentTag(tag); tag.put("src", constructURL()); } ``` 

Devilishly clever, I know.  See how incredibly easy it is to write your own Wicket component?  Practically speaking, there's almost nothing going on here.  All we do is listen for the _onComponentTag_ event, verify that we are indeed working with an <img/> tag, modify the _src_ attribute to some generated URL and we're home free!  I really hate to go on and on about this, but the Wicket devs really deserve their props for making a very slick and extensible API.  Anyway, back to the charts mechanism...

That's really about the only earth-shattering bit of code in the entire chart API.  _constructURL_ just has a set of all of the attributes in which Google Charts is interested.  It blindly runs through these attributes, checking corresponding values in the _IChartProvider_ instance.  If something is non-null, it's run through a basic rendering algorithm (dependant on what attribute it is) and a properly formatted character sequence is spit out the other end.  Baby stuff.

### More API

One thing that's worth drawing attention to is that I tried to stick with Java-conventional values within the chart model rather than the more normal HTML values.  For example, setting the first two graph colors might look something like this:

```java provider.setColors(new Color[] {Color.RED, new Color(255, 0, 255, 127}); ``` 

Notice the extra integer value in the second color?  Because the API is using proper _java.awt.Color_ instances, we can specify colors not just in RGB, but in full ARGB (the 'A' stands for "alpha"), allowing transparency.  Alex Blewitt has [an example of dubious usefulness](<http://alblue.blogspot.com/2007/12/use-transparent-backgrounds-in-google.html>) which shows how this can be applied.

Of course, the full API is far too extensive to cover in a non-trivial blog post (much less a single night's hard coding).  However, I did whip up a demo page that I've been using to test the framework.  It's not hosted live right now, but I did upload [the fully-rendered HTML](<http://www.codecommit.com/blog/misc/wicket-googlecharts/rendering.html>).  Sources for this particular page are also available ([java](<http://www.codecommit.com/blog/misc/wicket-googlecharts/Home.java>), [html](<http://www.codecommit.com/blog/misc/wicket-googlecharts/Home.html>)).

More importantly, I took the time to pilfer a build script from [another project](<https://activeobjects.dev.java.net>) of mine and actually build a proper distributable for the library.  Both tarball and zip archive are available.  Go nuts!

Oh, one more thing...  I assume I don't have to define the words "untested", "prototype" and "danger-will-robinson".  Just remember I warned you if your server inexplicably turns into a piece of fairy cake.

  * [wicket-googlecharts-0.1.tar.gz](<http://www.codecommit.com/blog/misc/wicket-googlecharts/wicket-googlecharts-0.1.tar.gz>)
  * [wicket-googlecharts-0.1.zip](<http://www.codecommit.com/blog/misc/wicket-googlecharts/wicket-googlecharts-0.1.zip>)
  * ([the full Eclipse project](<http://www.codecommit.com/blog/misc/wicket-googlecharts/eclipse-project.zip>))

**Update:** The project has been released as a wicket-stuff subproject. You can get the source via SVN at this URL: `https://wicket-stuff.svn.sourceforge.net/svnroot/wicket-stuff/trunk/wicket-googlecharts`