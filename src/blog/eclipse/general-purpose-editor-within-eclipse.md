{%
laika.title = "General Purpose Editor Within Eclipse"
laika.metadata.date = "2007-10-31"
%}


# General Purpose Editor Within Eclipse

I've blogged before about the difficulties I've had in finding a solid, general-purpose text editor for my system.  I looked into VIM for Windows, E, SciTi and many more before finally settling on jEdit.  It's a really good editor, if a bit rough around the edges.  A lot of people (myself included) would put it on par with TextMate in terms of features, and superior to it in some ways thanks to its cross-platform nature.

As a separate application from my IDE, jEdit performs superbly, but although this solves the problem of editing arbitrary files from file explorer, it still leaves open the problem of editing arbitrary file types within Eclipse.  What I had been doing is using jEdit as an external editor, opening it up any time I needed to open a weird file type within Eclipse.  This works fairly well, but it's heavy on memory, not integrated with tools like Mylyn (as if there were _any_ tools like Mylyn) and it's just annoying, dealing with a separate app like that. 

What I really want is some sort of embedded jEdit editor canvas within a normal Eclipse editor part.  One would think this would be very possible, given SWT_AWT and its capabilities.  In fact, I was just about to crack open the jEdit source to see if I could roll something myself when an eternal axiom sprang to mind: Google is your friend.  Actually in this case, I used [Eclipse-Plugins.info](<http://www.eclipse-plugins.info>) (which IMHO is _still_ the best Eclipse plugins site, despite lacking an active administrator) and did a quick search for any plugins mentioning the word "jedit".  A few minutes later, I was perusing the page for the little-known [Color Editor](<http://www.gstaff.org/colorEditor/>) plugin.

Color Editor is basically a simple Eclipse editor which will open any file for you.  It's not much more sophisticated than the standard Text Editor which is the default for unknown file types.  However, what it *does* do is parse jEdit's _mode.xml_ files, providing semi-advanced syntax highlighting for over 150 file types.  Granted, it doesn't have all of jEdit's nice editing features or plugins like SuperAbrevs, but if I need that I'll open up jEdit itself.  For most of what I do, quick-and-dirty syntax highlighting is all I need.

The problems with this plugin are mainly caused by the fact that it's quite old and hasn't really been updated recently.  It still defaults to the old-style jEdit colors, which are ugly as sin.  Also, it doesn't support as granular syntax highlighting as the current version of jEdit (only 2 comment types, 2 literals, etc).  It doesn't support easy adding of modes (you have to repackage the plugin JAR file), nor does it allow you to simply point it to the same mode _catalog_ used by jEdit itself (which would simplify management of editor modes).  Despite all of that, it's still a really nice idea.

The plugin works not by embedding a jEdit editor canvas using SWT_AWT, but by just using the standard Eclipse syntax highlighting techniques coupled with the jEdit mode files.  The downside to this approach is the need to write a whole bunch of mode parser code which is effectively already done within jEdit.  Also, odd bugs can leak in around the edges, since the editor is effectively reverse-engineering the jEdit editor part.  However, the approach does have a very unexpected (and pleasant) silver lining: fonts look good.

I've had tons of problems with jEdit's font rendering on Windows, mainly due to the fact that Swing's font renderer doesn't seem to be as sophisticated as Vista's (or at least, less capable of dealing with monospaced fonts).  But since Color Editor uses native font rendering, the text looks 100% native:

**jEdit**

#### 

![jEdit Fonts](/assets/images/blog/wp-content/uploads/2007/10/jedit-thumb.png)

**Eclipse Color Editor**

![Color Editor Fonts](/assets/images/blog/wp-content/uploads/2007/10/syntax-coloring-editor-thumb.png)

I assume you can see the difference.  :-)  So, fonts look great, but if you examine the body of the method in the file a bit more, you'll see examples of those odd bugs I was talking about.  For some reason, Color Editor thinks that the _suite_ variable, as well as the _BaseTests, DBTests, SchemaTests_ and _TypeTests_ classes are all methods, rather than local variables and classes.  This is annoying, but it's not the end of the world.  Granted, I haven't been using this tool for all that long, but I'm guessing that instances like this are fairly rare, and not cause for immediate alarm.  You'll also notice some evidence of the lessened flexibility in the syntax highlighting engine (fewer types of comments in this case) in the way the javadoc and single-line comments are colorized differently.

[Download Color Editor Plugin from gstaff.org](<http://www.gstaff.org/colorEditor/cbg.editor_1.2.6.jar>)  (no update site available)

All you have to do is stick the JAR in your eclipse/plugins directory and start Eclipse with the -clean option (usually unnecessary, but just to be safe).  Color Editor will automatically be registered as the default editor for unknown file types.  If you want to change the default colors (as well you should), you can find the preference under Coloring Editor -> Colors (no idea why the conjugation difference between the editor name and the preference pane).  It's a bit clumsy to try and set _all_ of the colors to a predefined theme (I made mine look like the current jEdit defaults), but it's all possible.

Hopefully you'll find this a useful tool in editing those random shell scripts and who-knows-what-else which got included in the project, but for which Eclipse doesn't have a separate plugin.