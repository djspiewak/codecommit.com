---
categories:
- Java
- Ruby
date: '2008-05-12 00:00:49 '
layout: post
title: Buildr Still Not Ready for Prime Time
wordpress_id: 228
wordpress_path: /java/buildr-still-not-ready-for-prime-time
---

As some of you may know, I've been following the [Buildr project](<http://incubator.apache.org/buildr/>) with a fair degree of interest of late.  Just last week, [Assaf announced](<http://www.nabble.com/Buildr-1.3-is-out!-tt17011441.html>) their first release as an Apache project: Buildr 1.3.0.  Being of the inquisitive bend, I decided that this would be an excellent time to reevaluate its suitability for use in my projects, both commercial and personal.

A while back, I [evaluated Buildr](<http://www.codecommit.com/blog/java/in-search-of-a-better-build-system>) as a potential replacement for Ant and Maven2 and eventually came to the unfortunate conclusion that it just wasn't ready.  At the time, Buildr was still hampered by a number of annoying limitations (such as being unable to reconfigure the default directory structure).  I'm happy to say that many of these problems have been fixed in the 1.3 release, as well as a large number of feature additions which I hadn't thought to request.  Despite all that, I'm still forced to conclude that Buildr simply isn't (yet) suitable as my build tool of choice.

Now before you stone me for utter heresy, try to hear me out.  I'm not dissing Buildr in any way, I really _want_ to use it, but I just can't justify moving over to it in the face of all of its current issues.  What's really unfortunate is that all of these issues can be summed up with a single word: **transitivity**.

One of Maven's killer features is the ability to resolve the _entire_ transitive dependency graph.  Now I'll grant that it does this with varying degrees of success, but for the most part it's pretty smart about how it fixes up your CLASSPATH.  As of 1.3, Buildr claims to have experimental support for transitive dependency resolution, but judging from the problems I encountered in my experimentation, it's barely even deserving of mention as an experiment, much less a full-fledged feature.

To understand why transitive dependencies are such a problem, it is of course necessary to first understand the definition of such.  In a word (well, several anyway), a transitive dependency is an _inherited_ dependency, an artifact which is not depended upon directly by the project, but rather by one of its dependencies.  This definitions carries recursively to dependent parents, grand-parents and so on, thus defining a transitive dependency graph.  Consider it this way:

![image](/assets/images/blog/wp-content/uploads/2008/05/image.png)

In the diagram, **MyCoolProject** is the artifact we are trying to compile.  The only dependencies we have actually specified for this artifact are the `DBPool` and `Databinder` artifacts.  However, the `Databinder` artifact has declared that it depends upon both the `Wicket` and the `ActiveObjects` artifacts.  ActiveObjects doesn't depend upon anything, but `Wicket` has dependencies `SLF4J` and on the `Servlets` API.  Thus, our original goal, **MyCoolProject** , has _transitive_ dependencies `Wicket`, `SLF4J`, `Servlets` and `ActiveObjects`.  Quite a bit more than we thought we were asking for when we declared our dependency on `Databinder`.

In general, this sort of transitive resolution is a good thing.  It means that instead of specifying six dependencies, we only had to specify two.  Furthermore, we observed the DRY principle by not re-specifying dependency information already contained within the various packages.  As with any "good thing", there's definitely a valid argument regarding its down sides, but on the whole I'm quite fond of this feature.

In Maven, this sort of thing happens by default, which can lead to some very confusing and subtle CLASSPATH problems (conflicting packages, etc).  Buildr takes a slightly different approach in 1.3 by forcing the use of the `transitive` method:

```ruby
repositories.remote << 'http://www.ibiblio.org/maven2'

define 'MyCoolProject' do
  project.version = '1.0'

  compile.with transitive('xmlrpc:xmlrpc-server:jar:3.0')

  package :jar
end
```

It's easy to see why Buildr is raising such a ruckus in the build genre.  It's syntax is elegance itself, and since it's actually a proper scripting language (Ruby), there's really nothing you can't do with it.  Having to remember to specify the default repository is a bit of a pain, but it's certainly something I can live with.  The real problem with this example is a little less subtle: It doesn't work.

If you create a `buildfile` with the above contents and then run the `buildr` command, the results will be something like the following:

```
Downloading org.apache.xmlrpc:xmlrpc:jar:3.0
rake aborted!
Failed to download org.apache.xmlrpc:xmlrpc:jar:3.0, tried the following repositories:
http://www.ibiblio.org/maven2/

(See full trace by running task with --trace)
```

After a fairly significant amount of digging, I managed to discover that this problem is caused by the fact that Buildr attempts to download a corresponding JAR file for _every_ POM it resolves.  This seems logical until you consider that many large projects (including Databinder, Wicket, and Hibernate) define POM-only projects which exist for the sole purpose of creating transitive dependencies.  They're organizational units, designed to allow projects to depend upon one super-artifact, rather than a dozen sub-projects.  It's a very common practice, and one which Buildr completely fails to handle appropriately.

After some prodding on the Buildr dev mailing-list, Assaf admitted that this is an issue worth looking at and provided a temporary workaround (pending a rework of the functionality in 1.4):

```ruby
def pom_only(a)
  artifact a do |task|
    mkpath File.dirname(task.to_s)
    Zip::ZipOutputStream.open task.to_s
  end
end
```

This was about the point that I started wondering if maybe Ant/Ivy would be a better bet, at least for the time being.  To use this workaround, you must call the `pom_only` method once for _every_ POM-only project in the dependency graph.  Usually, this means you must invoke Buildr repeatedly and find the troublesome artifacts by trial and error.  Not exactly a "just works" solution.

Pressing forward however, I unearthed a deeper, even more insidious issue: an intermittent failure to generate Eclipse project meta.  I'm not sure if this is due to the POM-only dependencies or just bad juju, but whatever the reason, it's annoying.  I've raised the issue on the Buildr mailing lists, but so far no response.  Basically, what happens is something like this:

```
C:\\Users\\Daniel Spiewak\\Desktop\\MyCoolProject> buildr eclipse
(in C:/Users/Daniel Spiewak/Desktop/MyCoolProject, development)
Completed in 0.499s
C:\\Users\\Daniel Spiewak\\Desktop\\MyCoolProject>
```

Not exactly the most helpful output.  In case you were wondering, this process did not create the Eclipse metadata.  It's interesting to note that calling `buildr idea` (to create project metadata for IntelliJ) seems to work just fine.  Whatever causes the bug, it seems to be specific to just the Eclipse project generator.

Buildr is a remarkable project.  It shows potential to someday become the de-facto build system, possibly even unseating Ant.  Unfortunately, that day is not today.  There are too many odd wrinkles and unpredictable errors to really call it a "finished product".  Hopefully, the Buildr dev team will continue their excellent work, eventually producing a tool worthy of serious consideration.  Until then, I guess that I'm (still) stuck with Ant.

**Update:** It seems that the issues with transitive dependencies [have been resolved](<https://issues.apache.org/jira/browse/BUILDR-63>) in the latest Buildr versions in their SVN.  I'm looking forward to when this all becomes stable and publicly consumable!

  * Download sample [buildfile](<http://www.codecommit.com/blog/misc/buildr-still-not-ready-for-prime-time/buildfile>)