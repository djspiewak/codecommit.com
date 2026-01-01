---
categories:
- Java
date: '2007-02-21 19:53:29 '
layout: post
title: Does Swing Need Saving?
wordpress_id: 90
wordpress_path: /java/does-swing-need-saving
---

There's been some discussion lately regarding various scripting languages and if they are (or aren't) the salvation for the "dying" Swing API ([here](<http://rawblock.blogspot.com/2007/02/jruby-can-save-swing.html>), [here](<http://pinderkent.blogsavy.com/archives/53>) and [here](<http://shemnon.com/speling/2007/02/groovy-can-save-swing.html>)). However, all of these blog entries assume one critical fact: Swing is dead or at least dying. I call that assumption into question. Actually, I was kind of surprised that none of the Sun fan-boys beat me to the punch. Over the years, I've shot my mouth of quite a bit _against_ Swing. So it surprises me greatly that I'd be the first to step forward and try to defend Swing. In fact, if you had talked to me a year ago, likely I would have been leading the charge to bury Swing and move on. Apparently, some things have changed… The first post to hit the blogosphere, [JRuby can save Swing](<http://rawblock.blogspot.com/2007/02/jruby-can-save-swing.html>) summed up the core assumptions of all three pretty quickly: 

  * Swing apps are slow to build
  * Swing layout managers suck
  * Swing apps are hard to maintain
  * Swing is too powerful
  * No native features
  * Swing apps have a bad history

To start with, assuming that using Swing from within a scripting language solves _any_ of these problems smacks of dynamically-typed delirium. When I first read the [JRuby cannot save Swing](<http://pinderkent.blogsavy.com/archives/53>) post, I assumed that it would mention this minor fact, but instead it simply berated Swing further and proposed that it is "beyond saving". Somehow, this seems a little bit of an extreme sentiment towards one of Java's most established frameworks. Let's look at item number one… 

## Swing apps are slow to build

That depends greatly on a number of factors, not the least of which is the capabilities of the developer in question. I can build a non-trivial Swing UI in a couple hours. I'm sure [Romain Guy](<http://www.curious-creature.org>) could do it in half that. Compare that to some developers who would take hours to build a simple, three element form. It's all relative. Some of the blog posts propose that using a dynamic language would help with this problem. Let's take a look: 

```java
public class TestClass {
    public static void main(String... args) {
        JFrame frame = new JFrame("Test Frame");

        JLabel label = new JLabel("This is the header");
        label.setFont(label.getFont().deriveFont(label.getFont().getSize2D() + 10));
        frame.add(label, BorderLayout.NORTH);

        JPanel buttonPanel = new JPanel();
        frame.add(buttonPanel);

        buttonPanel.add(new JButton("Button 1"));
        buttonPanel.add(new JButton("Button 2"));
        buttonPanel.add(new JButton("Button 3"));

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 200);
        frame.setVisible(true);
    }
}
```

![](http://blogs.dzone.com/daniel/files/2007/02/022207_0143_DoesSwingNe12.png) This took me about 3 minutes to whip together. Obviously this is a toy example, but it does illustrate the point that Swing isn't _that_ cryptic or bulky. Now, let's take a look at the corresponding JRuby code: 

```ruby
require 'java'

JFrame = javax.swing.JFrame
JLabel = javax.swing.JLabel
JPanel = javax.swing.JPanel
JButton = javax.swing.JButton

BorderLayout = java.awt.BorderLayout

frame = JFrame.new 'Test Frame'

label = JLabel.new 'This is the header'
label.font = label.font.deriveFont(label.font.size_2D + 10)
frame.add(label, BorderLayout::NORTH)

button_panel = JPanel.new
frame.add button_panel

button_panel.add JButton.new 'Button 1'
button_panel.add JButton.new 'Button 2'
button_panel.add JButton.new 'Button 3'

frame.default_close_operation = JFrame::EXIT_ON_CLOSE
frame.setSize(400, 200)
frame.visible = true
```

Now, this took me just as long to put together as the example in Java. And I had already built the UI! Ruby is 1) a nice dynamic language, and 2) I had already done the work. Shouldn't this have gone much faster? Also, this doesn't seem too much clearer to me than the Java example. I mean, it's all Swing. One of the posts proposed that a generic (possibly XML based) overlay be created on top of Swing which would simplify development by only using Swing indirectly. Well, people have already done this (it's called XUL), and it isn't very popular outside of Mozilla.org But let's move on… 

## Swing layout managers suck

Well maybe… It greatly depends on the type of UI you're trying to create. If you're trying to create a form-based UI with lots of elements then I will concede the point. The reason being that most forms are laid out in a grid-like fashion. For example, something like this: ![](http://www.writely.com/File.aspx?id=ajcfvtnsx8qc_3gjvwzk) It's very hard to do something like this in Swing because the only two grid-based layout managers are GridLayout and GridBagLayout. GridLayout is extremely limiting, requiring all cells to be exactly the same dimensions, and GridBagLayout is powerful to a fault (as well as having some glaring weaknesses, like no absolute cell sizes). However, this single shortcoming in the Swing layout manager menagerie (lack of a good form layout) has been rectified in Java 6. More specifically, the problem was fixed with the introduction of GroupLayout, which is the layout manager used by the NetBeans GUI builder (formerly Matisse). Although GroupLayout's API is somewhat cryptic (not to mention Java 6 only), it's still a very powerful layout manager, even more than GridBagLayout. There are also a whole host of third-party layout managers available for Swing which excel at form based layouts. JGoodies's FormLayout is a popular one. I myself have even gone so far as to port a layout manager from SWT, GridLayout (which resembles GridBagLayout in power without the complexity). This brings me to my next point… Even if there were no solid layout managers available for Swing, it's still very easy to create your own. In fact, this process is so simple, and so revealing about the way Swing layout managers work, that I would recommend _anyone_ who's serious about developing with Swing to pursue custom layout managers. I've written several layout managers over the years. While I only use one of them on a regular basis, I greatly value the lessons learned from each and every one of them. And honestly, Swing makes the task of layout manager creation significantly easier than many other GUI toolkits (such as SWT). 

## Swing apps are hard to maintain

Not true. Badly designed code is hard to maintain. Badly designed code is written by bad programmers. Bad programmers are incapable of using Swing properly (and even if they did, the rest of their code would be atrocious). In fact, Swing encourages best-practice coding in a lot of ways (a highly MVC based architecture for one). If anything, Swing-based apps are _easier_ to maintain than those created using other APIs. 

## Swing is too powerful

This is a problem? 

## No native features

Well, it depends on what you mean when you say "no native features". Does every element in a Swing UI correspond with a native peer? No. Are Swing elements indistinguishable from their native counterparts? Eh, depends on the platform and how good your eyes are. On Windows XP, the answer is often yes. On Vista, less often. On Mac the differences are acceptable, but on Linux the GTKLookAndFeel is terrible. Don't get me wrong, it's better than it was, but it still needs a lot of work before it's truly production quality. The "native features" case in which Swing falls drastically short is in integration with the native platform's desktop features. For example: 

  * Determining and firing native apps for a particular file association
  * Retrieving corresponding icons for a certain file association
  * Determining file associations in the first place
  * Access to any sort of platform-specific feature
  * …and so on

I won't deny that SWT is miles ahead of Swing in this regard. However, there is some dissention over a) are these really the responsibility of the GUI toolkit? and b) what's wrong with separate libraries like JDIC? Oh well, we'll still chalk this up as a point for "the other side" (whoever that may be). 

## Swing apps have a bad history

This is flat out undeniable. Swing apps have a _terrible_ reputation. I still walk into random IT departments, mention I'm a Java developer and get lectures about how slow, ugly and unreliable Java is; when in fact, these comments are really directed at legacy Swing applications. Swing is a hard API to use properly. This is true mainly because it is an API worth using. This difficulty that programmers have with Swing is amplified by the fact that back in the day, many GUIs were designed visually with the aid of tools like Delphi or Visual Basic's form editor. Obviously, these tools make it a lot easier to get a "cheap and dirty" UI out the door quickly, but the result really isn't a GUI worth maintaining. Also, it led to a lot of bad programmers thinking that they were good programmers because they were able to create mediocre prototypes. Refer to "Swing apps are hard to maintain" for more on this point. Also, until recently, the look and feels available for use with Swing were either highly esoteric and non-native or flat out ugly (or both). Without a solid look and feel to render the components well, Swing looks terrible. However, as of Java 5 Swing got a vastly improved Luna (Windows XP) look and feel. Java 6 introduced a half-way-there GTKLookAndFeel on linux, as well as sub-pixel rendering. Swing fonts have been the focal point of a lot of criticisms primarily because they (until recently) lacked sub-pixel rendering. (sub-pixel rendering allows fonts to look "smoother" on most screens) I think that you could make an argument that Swing on Windows XP looks just as good as any native application. 

# To Conclude…

Swing is in no need of a rescue from more "hip" languages like Ruby or Groovy. Nor is Swing languishing in the pit of unattractiveness. Rather, Swing is alive and well, and getting stronger by the day. Swing doesn't need saving, what Swing needs is to be given another chance by the world.