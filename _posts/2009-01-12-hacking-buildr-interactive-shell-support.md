---
categories:
- Java
- Ruby
- Scala
date: '2009-01-12 00:52:51 '
layout: post
title: 'Hacking Buildr: Interactive Shell Support'
wordpress_id: 283
wordpress_path: /java/hacking-buildr-interactive-shell-support
---

Last week, we looked at the unfortunately-unexplored topic of [Scala/Java joint compilation](<http://www.codecommit.com/blog/scala/joint-compilation-of-scala-and-java-sources>).  Specifically, we saw several different ways in which this functionality may be invoked covering a number of different tools.  Among these tools was Buildr, a fast Ruby-based drop-in replacement for Maven with a penchant for simple configuration.  In the article I mentioned that Buildr doesn't actually have support for the Scala joint compiler out of the box.  In fact, this feature actually requires the use of a Buildr fork I've been using to experiment with different extensions.  Among these extensions is a feature I've been wanting from Buildr for a long time: the ability to launch a pre-configured interactive shell.

For those coming from a primarily-Java background, the concept of an interactive shell may seem a bit foreign.  Basically, an interactive shell — or [_REPL_](<http://en.wikipedia.org/wiki/REPL>), as it is often called — is a line-by-line language interpreter which allows you to execute snippets of code with immediate result.  This has been a common tool in the hands of dynamic language enthusiasts since the days of LISP, but has only recently found its way into the world of mainstream static languages such as Scala.

![interactive-shells.png](/assets/images/blog/wp-content/uploads/2009/01/interactive-shells.png)

One of the most useful applications of these tools is the testing of code (particularly frameworks) before the implementations are fully completed.  For example, when working on [my port of Clojure's `PersistentVector`](<http://github.com/djspiewak/scala-collections/tree/master/src/main/scala/com/codecommit/collection/Vector.scala>), I would often spin up a Scala shell to quickly test one aspect or another of the class.  As a minor productivity plug, [JavaRebel](<http://www.zeroturnaround.com/javarebel/>) is a truly _invaluable_ tool for development of this variety.

The problem with this pattern of work is it requires the interactive shell in question to be pre-configured to include the project's output directory on the CLASSPATH.  While this isn't usually so bad, things can get very sticky when you're working with a project which includes a large number of dependencies.  It isn't too unreasonable to imagine shell invocations stretching into the tens of lines, just to spin up a "quick and dirty" test tool.

Further complicating affairs is the fact that many projects do away with the notion of fixed dependency paths and simply allow tools like Maven or Buildr to manage the CLASSPATH entirely out of sight.  In order to fire up a Scala shell for a project with any external dependencies, I must first manually read my `buildfile`, parsing out all of the artifacts in use.  Then I have to grope about in my `~/.m2/repository` directory until I find the JARs in question.  Needless to say, the productivity benefits of this technique become extremely suspect after the first or second expedition.

For this reason, I strongly believe that the launch of an interactive shell should be the responsibility of the tool managing the dependencies, rather than that of the developer.  Note that Maven already has some support for shells in conjunction with certain languages (Scala among them), but it is as crude and verbose as the tool itself.  What I really want is to be able to invoke the following command and have the appropriate shell launched with a pre-configured CLASSPATH.  I shouldn't have to worry about the language of my project, the location of my repository or even if the shell requires extra configuration on my platform.  The idea is that everything should all work auto-magically:

``` $ buildr shell ``` 

This is exactly what the [`interactive-shell` branch of my Buildr fork](<http://github.com/djspiewak/buildr/tree/interactive-shell>) is designed to accomplish.  Whenever the `shell` task is invoked, Buildr looked through the current project and attempts to guess the language involved.  This guesswork is required for a number of other features, so Buildr is actually pretty accurate in this area.  If the language in question is Groovy or Scala, then the desired shell is obvious.  Java does not have an integrated shell, which means that the default behavior on a Java project would be to raise an error.

However, the benefits of interactive shells are not limited to just the latest-and-greatest languages.  I often use a Scala shell with Java projects, and for certain things a JRuby shell as well (`jirb`).  Thus, my interactive shell extension also provides a mechanism to allow users to override the default shell on a per-project basis:

```ruby define 'my-project' do shell.using :clj end ``` 

With this configuration, regardless of the language used by the compiler for "my-project", Buildr will launch the Clojure REPL whenever the `shell` task is invoked.  The currently supported shells and their corresponding Buildr identifiers:

  * Clojure's REPL — `:clj`
  * Groovy's Shell — `:groovysh`
  * JRuby's IRB — `:jirb`
  * Scala's Shell — `:scala`

It is also possible to explicitly launch a specific shell.  This is useful for situations where you might want to use the Scala shell for testing some things and the JRuby IRB for quickly prototyping other ideas (I do this a lot).  The command to launch the JIRB shell in the context of `my-project` would be as follows:

``` $ buildr my-project:shell:jirb ``` 

As a special value-added feature, all of these shells (except for Groovy's, which is weird) will be automatically configured to use JavaRebel for the project compilation target classes if it can be automatically detected.  This detection is performed by examining `REBEL_HOME`, `JAVA_REBEL`, `JAVAREBEL` and `JAVAREBEL_HOME` environment variables in order.  If any one of these points to a directory which contains `javarebel.jar` or points directly to `javarebel.jar` itself, the configuration is assumed and the respective shell invocation is appropriately modified.

![javarebel-integration.png](/assets/images/blog/wp-content/uploads/2009/01/javarebel-integration.png)

Best of all, this support is implemented using a highly-extensible framework similar to Buildr's own `Compiler` API.  It's very easy for plugin implementors or even average developers to simply drop-in a new shell provider, perhaps for an internal language or even some unexpected application.  The core functionality of shell detection is integrated into Buildr itself, but this in no way hampers extensibility.  For example, I could easily create a third-party `.rake` plugin for Buildr which added support for a whole new language (e.g. Haskell).  In this plugin, I could also define a new shell provider which would be the default for projects using that language (e.g. GHCi).

### Open Question

The good news is that this feature [has been discussed extensively](<http://www.nabble.com/Interactive-Shell-Support-td21273331.html>) on the `buildr-user` mailing-list and the prevailing opinion seems to be that it should be folded into the main Buildr distribution.  Exactly what form this will take has yet to be decided.  The bad news is that there is still some dispute about a fundamental aspect of this feature's operation.

The question revolves around what the exact behavior should be when the `shell` task is invoked.  Should Buildr detect the project (or sub-project) you are in and automatically configure the shell's CLASSPATH accordingly?  This would give the interactive shell access to different classes depending on the current working directory.  Alternatively, should there be one all-powerful shell per-`buildfile` configured at the root level?  This would allow your shell to remain consistent throughout the project, regardless of your current directory.  However, it would also mean that some configuration would be required in order to enable the functionality.  (more details of this debate can be found [on the mailing-list](<http://www.nabble.com/Interactive-Shell-Support-td21273331.html>)).

Additionally, what should the exact syntax be for invoking a specific shell?  Rake 0.8 allows tasks to take parameters enclosed within square brackets.  Thus, the syntax would be something more like the following:

``` $ buildr collection:shell[jirb] ``` 

In some sense, this is more logical since it reflects the fact that a single task, `shell`, is taking care of the work of invoking stuff.  On the other hand, it's a little less consistent with the rest of Buildr's tasks, particularly things like "`test:TestClass`" and so on.  This too is a matter which has yet to be settled.

All in all, this is a pretty experimental branch which is very open (and desirous) of outside input.  How would you use a feature like this?  Is there anything missing from what I have presented?  What design path should be we take with regards to project-local vs global shell configurations?

If you feel like adding your voice to the chorus, feel free to leave a comment or (better yet) post a reply on the mailing-list thread.  You're also perfectly free to fork my remote branch at GitHub to better experiment with things yourself.  The root of the whole plate of spaghetti is the `lib/buildr/shell.rb` file.  _Bon appetit!_