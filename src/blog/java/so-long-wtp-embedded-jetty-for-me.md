{%
laika.title = "So Long WTP, Embedded Jetty for Me"
laika.metadata.date = "2007-11-05"
%}


# So Long WTP, Embedded Jetty for Me

As some of you may know, I've been doing some work for Teachscape (think [Eelco Hillenius](<http://chillenious.wordpress.com/>)) on their Wicket-based application.  It's been a learning experience for me as much as a working position in that I've got to experiment with a lot of technologies and techniques I haven't tried before.  For example, Teachscape is based on Hibernate and Spring, so I've been getting some really in-depth exposure to both technologies (I still dislike them both).  More interesting to me though is the way the application is run in a developer environment. 

It uses Jetty, not Tomcat or Glassfish.  There's a main class within the application which sets up the Jetty instance, configures it to use the Wicket servlet and starts the server.  This means that running the web application is as simple as "Run As..." -> "Java Application".  It starts fast, is more responsive than a straight-up Tomcat instance, and best of all, hot code replace works properly in debug mode.  Now having used this approach at Teachscape for some time, I'm starting to really miss it when I jump back to my own, WTP-based Wicket projects.

So after much wrestling with pros and cons, I decided to switch one of my more substantial projects over to using embedded Jetty.  I did this partially for the experience, partially because Jetty makes development so much easier, and partially because it lets me do cool things like package the app up in a single JAR and easily run it from any Java-equipped computer.

### Step One: Dependencies

Dependencies are always an issue.  Modern web applications have in excess of 20-30 JAR files which are either packaged into the WAR, or thrown into the app server's lib/ directory.  Some applications (like the one I was working with) have many more.  As you can imagine, this poses a more-than-minor inconvenience.

WTP makes dependency management (comparatively) easy, since everything is just thrown indiscriminately into the WebContent/WEB-INF/lib/ directory.  Since the JARs just sit there in a centralized spot, deploying is easy, the Eclipse classpath configuration is trivial and I don't need to work all that hard to upgrade a certain library (such as wicket-1.3-beta3 to beta4). 

Of course, I can keep this approach when I switch to Jetty, but it's not the cleanest thing to do.  What's best is to configure the Eclipse classpath to reference the JAR files in their unzipped directories in my system somewhere (preferably through the use of classpath variables).  After all, that's what I would do for a standard Java project, and with Jetty, a web application is no different from any other Java app.  With this rosy-eyed decision having been made, I deleted the lib/ directory and began reconfiguring the classpath.

Eclipse wasn't too bad, since I already had half the libraries set as variables to begin with, but the Ant build was another story.  Two hours of typing into a build.properties file and several cups of strong coffee later, I was just about at the end of my rope.  The dependencies were all set, and the project was building, but it was a nightmare.  I can only imagine what's going to happen if I have a significant influx of libraries and dependencies...

Lesson learned: use Maven.  Actually, don't use Maven, since it's just more headache than it's worth.  What I should have done (had I really thought things through), is take advantage of the now-incubating Apache project, [Buildr](<http://buildr.rubyforge.org/>).  It's basically a Maven-like build system with scripts based on Ruby and Rake.  Instead of writing an epic-length POM file, you write a 10-20 line _buildfile_ (which is actually a Ruby script with some DSL syntax) and Buildr figures out the rest.  Unfortunately, by the time I bethought myself of this option, I was almost finished with the Ant configuration, and I'm too stubborn to give up half-way through something.

### Step Two: The Launcher

The next step in my evil plan was to create a main class somewhere in the project which could configure Jetty and start the server.  Unfortunately, this is a bit harder than it sounds on paper.

The problem is not that Jetty has a tricky API, or any sort of gotchas in its configuration.  The problem is there's very little useful documentation which shows how to turn the trick.  So, for the record, here's the source for the main class I wrote:

```java
public class StartApplication {
    public static void main(String[] args) throws Exception {
        Server server = new Server(8080);
        Context context = new Context(server, "/", Context.SESSIONS);

        ServletHolder servletHolder = new ServletHolder(new WicketServlet());
        servletHolder.setInitParameter("applicationClassName",
                "com.myapp.wicket.Application");
        servletHolder.setInitOrder(1);
        context.addServlet(servletHolder, "/*");

        server.start();
        server.join();
    }
}
```

Not really too much there once you see it all laid out for you, but it took me _way_ longer than it should have to figure out that it needed to be done that way.  Executing this class starts a new Jetty instance serving my application at <http://localhost:8080>.  I can start the app in debug mode, getting proper hot code replace (something WTP _never_ got right) and every debug feature Eclipse offers for Java apps.  To stop the server, all I need to do is hit that cryptically labeled button in the Console view which kill the running application.  Oh, and there's no need to maintain a _web.xml_ file, Jetty doesn't need it.

Surprisingly, that's all that's different between WTP and embedded Jetty from a code standpoint.  I had expected to have to make significant changes elsewhere in my code base, but that was the extent of my fiddling.  Pretty slick!

### Evaluation

Once the Jetty environment was up and running, it was time to decide whether or not it was worth it.  After all, I could always do an _svn revert_ if I really didn't like the results.

**Feature** | **WTP/Tomcat** | **Jetty**  
---|---|---  
Rough startup time | 10 sec | 2 sec  
Page responsiveness | Decent (a bit sluggish) | Excellent (pages generate insanely fast)  
Server errors | Cryptic and well formatted | Cryptic and ugly  
Debugging | Passable, no hot code replace | Perfect, just as good as Java apps  
Ease of setup | Very good | Harder than it should be  
Setup time | 10 minutes | 3 hours  
  
Overall, I'll take Jetty any day, but I definitely need to use something like Buildr the next time I do this.  That would have pretty much dropped the setup time from 3 hours down to maybe 30 min (if that).  Oh well, live and learn.

So the moral of the story is that I'm done messing around with Eclipse WTP, trying to beat it into submission and putting up with all its oddities and extreme slowness.  I'm keeping it installed, if only for the HTML editor, but no more will I attempt to use it as a runtime controller.