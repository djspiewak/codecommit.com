{%
laika.title = "Joint Compilation of Scala and Java Sources"
laika.metadata.date = "2009-01-05"
%}


# Joint Compilation of Scala and Java Sources

One of the features that the Groovy people like to flaunt is the joint compilation of `.groovy` and `.java` files.  This is a fantastically powerful concept which (among other things) allows for circular dependencies between Java, Groovy and back again.  Thus, you can have a Groovy class which extends a Java class which in turn extends another Groovy class.

All this is old news, but what you may not know is the fact that Scala is capable of the same thing.  The Scala/Java joint compilation mode is new in Scala 2.7.2, but despite the fact that this release has been out for more than two months, there is still a remarkable lack of tutorials and documentation regarding its usage.  Hence, this post...

### Concepts

For starters, you need to know a little bit about how joint compilation works, both in Groovy and in Scala.  Our motivating example will be the following stimulating snippet:

```scala
// foo.scala
class Foo

class Baz extends Bar
```

...and the Java class:

```java
// Bar.java
public class Bar extends Foo {}
```

If we try to compile `foo.scala` before `Bar.java`, the Scala compiler will issue a type error complaining that class `Bar` does not exist.  Similarly, if we attempt the to compile `Bar.java` first, the Java compiler will whine about the lack of a `Foo` class.  Now, there is actually a way to resolve this _particular_ case (by splitting `foo.scala` into two separate files), but it's easy to imagine other examples where the circular dependency is impossible to linearize.  For the sake of example, let's just assume that this circular dependency is a problem and cannot be handled piece-meal.

In order for this to work, either the Scala compiler will need to know about class `Bar` before its compilation, or vice versa.  This implies that one of the compilers will need to be able to analyze sources which target the other.  Since Scala is the language in question, it only makes sense that it be the accommodating one (rather than `javac`).

What `scalac` has to do is literally parse and analyze all of the Scala sources it is given in addition to any Java sources which may also be supplied.  It doesn't need to be a full fledged Java compiler, but it does have to know enough about the Java language to be able to produce an annotated structural AST for any Java source file.  Once this AST is available, circular dependencies may be handled in exactly the same way as circular dependencies internal to Scala sources (because all Scala _and_ all Java classes are available simultaneously to the compiler).

Once the analysis phase of `scalac` has blessed the Scala AST, all of the Java nodes may be discarded.  At this point, circular dependencies have been resolved and all type errors have been handled.  Thus, there is no need to carry around useless class information.  Once `scalac` is done, both the `Foo` and the `Baz` classes will have produced resultant `Foo.class` and `Baz.class` output files.

However, we're still not quite done yet.  Compilation has successfully completed, but if we try to run the application, we will receive a `NoClassDefFoundError` due to the fact that the `Bar` class has not actually been compiled.  Remember, `scalac` only _analyzed_ it for the sake of the type checker, no actual bytecode was produced.  `Bar` may even suffer from a compile error of some sort, as long as this error is within the method definitions, `scalac` isn't going to catch it.

The final step is to invoke `javac` against the `.java` source files (the same ones we passed to `scalac`) adding `scalac`'s output directory to `javac`'s classpath.  Thus, `javac` will be able to find the `Foo` class that we just compiled so as to successfully (hopefully) compile the `Bar` class.  If all goes well, the final result will be three separate files: `Foo.class`, `Bar.class` and `Baz.class`.

### Usage

Although the concepts are identical, Scala's joint compilation works slightly differently from Groovy's from a usage standpoint.  More specifically: `scalac` does _not_ automatically invoke `javac` on the specified `.java` sources.  This means that you can perform "joint compilation" using `scalac`, but without invoking `javac` you will only receive the compiled Scala classes, the Java classes will be ignored (except by the type checker).  This design has some nice benefits, but it does mean that we usually need at least one extra command in our compilation process.

All of the following usage examples assume that you have defined the earlier example in the following hierarchy:

  * src
    * main
      * java
        * Bar.java
      * scala
        * foo.scala
  * target
    * classes

### Command Line

```
# include both .scala AND .java files
scalac -d target/classes src/main/scala/*.scala src/main/java/*.java

javac -d target/classes \
      -classpath $SCALA_HOME/lib/scala-library.jar:target/classes \
       src/main/java/*.java
```

### Ant

```xml
<target name="build">
    <scalac srcdir="src/main" destdir="target/classes">
        <include name="scala/**/*.scala"/>
        <include name="scala/**/*.java"/>
    </scalac>

    <javac srcdir="src/main/java" destdir="${scala.library}:target/classes" 
           classpath="target/classes"/>
</target>
```

### Maven

One thing you gotta love about Maven: it's fairly low on configuration for certain common tasks.  Given the above directory structure and the most recent version of the `maven-scala-plugin`, the following command should be sufficient for joint compilation:

```
mvn compile
```

Unfortunately, there [have been some problems](<http://www.nabble.com/forum/ViewPost.jtp?post=20845683&framed=y>) reported with the default configuration and complex inter-dependencies between Scala and Java (and back again).  I'm not a Maven...maven, so I can't help too much, but as I understand things, this POM fragment seems to work well:

```xml
<plugin>
    <groupId>org.scala-tools</groupId>
    <artifactId>maven-scala-plugin</artifactId>
    
    <executions>
        <execution>
            <id>compile</id>
            <goals>
            <goal>compile</goal>
            </goals>
            <phase>compile</phase>
        </execution>
        
        <execution>
            <id>test-compile</id>
            <goals>
            <goal>testCompile</goal>
            </goals>
            <phase>test-compile</phase>
        </execution>
        
        <execution>
            <phase>process-resources</phase>
            <goals>
            <goal>compile</goal>
            </goals>
        </execution>
    </executions>
</plugin>

<plugin>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <source>1.5</source>
        <target>1.5</target>
    </configuration>
</plugin>
```

You can find more information on [the mailing-list thread](<http://www.nabble.com/forum/ViewPost.jtp?post=20806619&framed=y>).

### Buildr

Joint compilation for mixed Scala / Java projects has been [a long-standing request of mine](<http://issues.apache.org/jira/browse/BUILDR-136>) in Buildr's JIRA.  However, because it's not a high priority issue, the developers were never able to address it themselves.  Of course, that doesn't stop the rest of us from pitching in!

I had a little free time yesterday afternoon, so I decided to blow it by hacking out a quick implementation of joint Scala compilation in Buildr, based on its pre-existing support for joint compilation in Groovy projects.  All of my work is available in [my Buildr fork on GitHub](<http://github.com/djspiewak/buildr>).  This also includes some other unfinished goodies, so if you want only the joint compilation, clone just the `scala-joint-compilation` branch.

Once you have Buildr's full sources, `cd` into the directory and enter the following command:

```
rake setup install
```

You may need to `gem install` a few packages.  Further, the exact steps required may be slightly different on different platforms.  You can find more details [on Buildr's project page](<http://incubator.apache.org/buildr/contributing.html#working_with_source_code>).

With this highly-unstable version of Buildr installed on your unsuspecting system, you should now be able to make the following addition to your `buildfile` (assuming the directory structure given earlier):

```ruby
require 'buildr/scala'

# rest of the file...
```

Just like Buildr's joint compilation for Groovy, you must explicitly `require` the language, otherwise important things will break.  With this slight modification, you should be able to build your project as per normal:

```
buildr
```

This support is so bleeding-edge, I don't even think that it's safe to call it "pre-alpha".  If you run into any problems, feel free to [shoot me an email](<mailto:djspiewak@gmail.com>) or [comment on the issue](<http://issues.apache.org/jira/browse/BUILDR-136>).

### Conclusion

Joint compilation of Java and Scala sources is a profound addition to the Scala feature list, making it significantly easier to use Scala alongside Java in pre-existing or future projects.  With this support, it is finally possible to use Scala as a truly drop-in replacement for Java without modifying the existing infrastructure beyond the CLASSPATH.  Hopefully this article has served to bring slightly more exposure to this feature, as well as provide some much-needed documentation on its use.