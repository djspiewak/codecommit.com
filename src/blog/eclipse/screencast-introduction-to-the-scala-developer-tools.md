{%
laika.title = "Screencast: Introduction to the Scala Developer Tools"
laika.metadata.date = "2008-04-21"
%}


# Screencast: Introduction to the Scala Developer Tools

Virtually everyone who has visited the Scala project page has seen [the info page](<http://www.scala-lang.org/tools/eclipse/index.html>) for the Scala plugin for Eclipse.  There are a few screenshots, an update site and very little instruction on how to proceed from there.  Those of you who have actually installed this plugin can vouch for how terribly it works as well as the remarkable lack of usefulness in its functionality.  It's basically a very crude syntax highlighting editor for Scala embedded into Eclipse.  It has the ability to run programs and compile them within the IDE, but that's about all.  Worse than that, it seems to make everything else about Eclipse less stable; somehow crashing random, unrelated plugins (such as DLTK).  Needless to say, it's often a race to see how fast we can _remove_ the Scala Eclipse plugin from our systems.

What is far less widely known is that there is a second Eclipse plugin which offers support for Scala development.  Basically, the guys at LAMP decided that it wasn't worth trying to build out the original plugin any further.  Instead, they started from scratch and created a [whole new implementation](<http://scala.sygneca.com/tools/eclipse>).  The result is entitled the "Scala Developer Tools" (or SDT, if you're into short and phonetically confusing acronyms).  Basically, this plugin is a very unstable, very experimental attempt to build a first-class IDE for Scala on top of Eclipse.  Obviously, they still have a ways to go:

![image](/assets/images/blog/wp-content/uploads/2008/04/image.png)

In case you were wondering, no that isn't my default editor font.  To say the least, the plugin suffers from an annoying plethora of UI-related bugs.  Behavior is inconsistent, and often times changing a value doesn't seem to be permanent (it took me several tries to get the syntax highlighting to stop shifting before my very eyes).  To make matters worse, it seems that installing the plugin in the first place is a bit like playing a game of hopscotch using un-anchored floats in the middle of a pool.  The [update site](<http://www.scala-lang.org/downloads/distrib/files/nightly/scala.update/>) has a nasty habit of throwing a 404 about 50% of the time.  You know what they say: if at first you don't succeed...

The good news is that once you get the plugin installed, the preferences beaten into submission, and the UI bugs safely ignored, things become quite nice indeed.  The new editor is _vastly_ improved over the old one, and it's easy to see tremendous potential in the project.  Things are actually getting to a point where I would consider using the plugin rather than my current jEdit setup.

Of course, it's hard to get a good idea of how a tool works until you see it in action.  That's why I took the time to put together a small screencast which illustrates some of the highlights of the new editor.  I made no attempt to hide the bugs which cropped up during my testing, so this should give you a fair approximation of the current state of the plugin and whether it's worth trying for your own projects.  The screencast has been produced at a reasonably high resolution (1024x732) in both Flash and downloadable AVI format.  Enjoy!

[![screencast-front](/assets/images/blog/wp-content/uploads/2008/04/screencast-front.jpg)](<http://www.codecommit.com/blog/misc/introduction-to-sdt>)

 

  * [Download "Introduction to SDT" as AVI](<http://www.codecommit.com/blog/misc/introduction-to-sdt.avi>)
  * [Download Eclipse Project](<http://www.codecommit.com/blog/misc/introduction-to-sdt/SwingTest.zip>)