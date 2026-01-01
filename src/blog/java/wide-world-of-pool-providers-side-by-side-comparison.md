{%
laika.title = "Wide World of Pool Providers: Side-by-Side Comparison"
laika.metadata.date = "2007-11-13"
%}


# Wide World of Pool Providers: Side-by-Side Comparison

It seems that for any conceivable functionality in Java, there exist a myriad of frameworks which accomplish the task in more-or-less the same way.  ORMs for example; I can count five different Java ORMs without even trying, and I'm sure that number would expand exponentially if I actually sat down and used Google to get a more precise estimate. 

Just like any other function, there seems to be a glut of frameworks which provide JDBC connection pooling.  Choosing between these frameworks can sometimes be a daunting task.  After all, what qualifications do you look at?  Performance?  Licensing?  Documentation?  Connection pooling is such an (apparently) small part of an application's infrastructure that many development teams devote very little time to this vital selection process.

Due to this management shortsightedness, many projects will simply use whatever pooling library is default for the ORM their using, or (more likely) the first Google hit when searching for "java connection pool".  Of course, this will get you something which is _usable,_ but rarely will you arrive at a pool provider which is _optimal_.

To help you to make a more informed decision in this vital aspect of project design, I hereby present some of the lessons I have learned on the subject while working on [ActiveObjects](<https://activeobjects.dev.java.net>).  All benchmarks were run against MySQL 5.0 running on Windows Vista Premium, 2 Ghz Intel Core2Duo, 2 GB DDR2, 7200 RPM SATA drive.  Each test was run five times (executing the DDL each time) with the average runtime taken as the result.  Any obviously poor results (several seconds above the mean) were dropped and re-tested.  All tests were run using the Eclipse JUnit 4 test runner.

### commons-dbcp

This is quite possibly the most commonly used pool provider (by default backed by [commons-pool](<http://commons.apache.org/pool>)), mainly because it used to come up first on Google.  Though, perhaps more important is the fact that commons-dbcp is an Apache sub-project.  This gives it both a less-restrictive license than some other projects, as well as a credibility that comes with being hosted at Apache.  Honestly, if I see a project's URL contains "apache.org", I immediately give it the benefit of the doubt, assuming that it will be of reasonable to high quality.  There's certainly something to be said for reputation...

[commons-dbcp](<http://commons.apache.org/dbcp/>) is probably the easiest pool provider I've seen in terms of API and "just work"-ness.  It has two mechanisms for setting up connections: alternative JDBC URI and a JNDI _DataSource_ implementation.  It's interesting to note here that the JDBC javadocs state that _DataSource_ is the preferred way to retrieve connections, while the commons-pool javadoc asserts that the alternative URI method passed directly to _DriverManager_ is preferable.  In practice, I find myself using the _DataSource_ method for connection pools, mainly because it reminds me that I'm not dealing with a normal connection creation, but something which is potentially pooled:

```java
BasicDataSource ds = new BasicDataSource();
ds.setDriverClassName(jdbcDriver);

ds.setUsername(getUsername());
ds.setPassword(getPassword());
ds.setUrl(getURI());

ds.setMaxActive(20);

// get connection here
Connection conn = ds.getConnection();

// dispose of pool
ds.close();
```

It's important to note here that the pool is explicitly disposed. This is _always_ a good idea, even for pools which don't state the requirement in their documentation.  An undisposed pool can hold database connection open, tying up resources and dragging your database performance through the dirt.  Always, always, always dispose of your connection pools when you're done with them.

So the API seems pretty intuitive here.  All of the methods do exactly what one would expect.  What's more, the entire library is extremely well documented.  There's quite a bit of material on the commons-pool project page discussing how to get started, what best practices to follow, etc.  The public API is javadoc'd, and there are a number of examples available.  I was up-and-running with the framework a few short minutes after I punched in the URL to my address bar.

One thing I haven't addressed yet is performance.  It's vitally important that the connection pool chosen run as efficiently as possible.  After all, its whole purpose is to optimize access and reduce the strain on the database in the form of connection create as well as statement compilation.  Obviously all of the really interesting stuff is in this segment of the library.  If this code performs poorly, it would be a very bad idea to try and use the framework for any sort of serious project.

I just happen to have a reasonably comprehensive database benchmark handy in the form of the ActiveObjects JUnit test suite.  ActiveObjects uses a reasonable number of JDBC features (it doesn't use many conventional Statement(s) or stored procedures).  Since neither the suite nor the library itself changes between benchmarks, we can test arbitrary connection pools easily and receive reasonably accurate results.

Continuing my recent obsession with HTML tables and their use in product reviews, here is the obligatory "five second rundown":

**Documentation** | Excellent  
---|---  
**API** | Easy and intuitive  
**License** | Apache License 2.0  
**AO Test Suite Run Time** | 20.6302 seconds  
  
### C3P0

[C3P0](<http://sourceforge.net/projects/c3p0>) is another very common connection pool framework, partially because it's the default pool used with the ever-popular Hibernate ORM.  Unlike commons-pool, C3P0 is actually hosted at SourceForge, that ever popular source of dead open-source projects and over-ambitious specs.  With that said, C3P0 is actually quite respectable as a framework and seems to have avoided the premature fate which befalls most open-source frameworks: developer boredom.

Unfortunately, like so many projects on SourceForge, C3P0 does not have a separate website.  The maintainers opted to stick with the SourceForge project interface as the sole source of "official" information.  Add to that the fact that they decided _not_ to add anything to the "Documentation" section of the page and you arrive at one very frustrating first impression for a new user.  Fortunately there's a lot of material on using C3P0 (both with and without Hibernate) available around the internet.  Always remember: Google is your friend.

```java
ComboPooledDataSource cpds = new ComboPooledDataSource();
cpds.setDriverClass(jdbcDriver);

cpds.setJdbcUrl(getURI());
cpds.setUser(getUsername());
cpds.setPassword(getPassword());

cpds.setMaxPoolSize(20);
cpds.setMaxStatements(180);

// get connection here
Connection conn = cpds.getConnection();

// dispose of pool
try {
    DataSources.destroy(cpds);
} catch (SQLException e) {
}
```

The API is somewhat similar to that of commons-dbcp.  Both use the _DataSource_ API as a foundation (which is the "right" approach according to the JDBC docs), and both allow roughly the same configuration options on a pool.  At face value, the APIs seem similar to the point that comparison between the two on such a level would be pointless.

One very important "feature" of the C3P0 library is its license: LGPL.  For those of you who don't know, LGPL is basically identical to the famed GPL 2.0 without the so-called "viral clause".  GPL pre-3.0 has some legal ambiguity relating to "derivative works" and what qualifies as such.  For this reason, many projects (especially commercial applications written using object-oriented languages) tend to shy away from libraries licensed as such.  LGPL of course doesn't have this problem, so it has seen moderately better acceptance from the corporate gods.  Unfortunately, it is still a fairly restrictive license relating to other matters such as redistribution.  It is fairly common practice to perform what can be described as "static linking" of JAR files for an application (un-JARring dependencies and then re-JARring them into the main application JAR).  This is something which is prohibited under LGPL, thus restricting deployment options somewhat.  Also, if I remember correctly any non-LGPL application or framework using a LGPL licensed dependency must include a copy of LGPL somewhere in the application (the About or Help section springs to mind).  It was due to this licensing that a company I recently worked for decided against using C3P0 for its application.  Of course, everyone's requirements are different, but you should still be aware of the possible consequences of using a restrictively licensed framework.

**Documentation** | Lousy  
---|---  
**API** | Easy and intuitive  
**License** | LGPL  
**AO Test Suite Run Time** | 17.277 seconds  
  
### Proxool

Like C3P0, Proxool is an open-source pooling library hosted on SourceForge.  Thankfully, _unlike_ C3P0, Proxool's maintainers actually took the time to build a full site for the project, containing documentation and examples.  Unfortunately for us, the examples don't do much good.

Proxool's documentation is obfuscated and hidden away, making it somewhat difficult to get started with the framework.  Unintuitively enough, the "Quick start" section is of very little help when trying to actually use the library.  Oh it does contain samples, but in my tests I couldn't get the samples to run successfully.  Add to this the fact that Proxool is a less well-known framework and you lead to some very frustrating experiences trying to get up and running.

To their credit, the Proxool maintainers have written quite a bit of documentation which covers a great deal of the framework functionality.  Organizing a project page intuitively is very hard, it's just a shame that would-be adopters of the framework have to pay the penalty. 

So to make things easier for others like myself looking to try the framework, here's the basic setup code for a Proxool pool:

```java
Class.forName(jdbcDriver);

Properties props = new Properties();
props.setProperty("proxool.maximum-connection-count", "20");
props.setProperty("user", getUsername());
props.setProperty("password", getPassword());

String driverUrl = getURI();
String url = "proxool.mypool:" + jdbcDriver + ":" + getURI();

ProxoolFacade.registerConnectionPool(url, props);

// get connection here
Connection conn = DriverManager.getConnection("proxool.mypool");

// dispose of pool
try {
    ProxoolFacade.removeConnectionPool("mypool");
} catch (ProxoolException e) {
}
```

Hardly intuitive I'd say.  Nevertheless, the above code seems to get the job done.

One of the debatable advantages to the Proxool library is that it allows developers to take advantage of connection pooling simply by using a special JDBC URI prefix (commons-dbcp allows this too).  I'm not using that syntax in the above example mainly because I think that developers are better served remembering when they are or are not using a pool.  Also, I never could quite get the syntax working (again, poorly structured documentation).

One interesting feature of Proxool that's worth mentioning is that it allows developers access to things like pool stats, event listeners and so on.  I believe these features are completely unique to Proxool, and while they're not very interesting in a small test application, imagine the power which can be unleashed in a real-world application.  Exposing this information through something like JMX could make tracing and debugging of database bottlenecks on a production server significantly easier.

**Documentation** | Frustrating  
---|---  
**API** | Poor  
**License** | Apache License  
**AO Test Suite Run Time** | 18.6406 seconds  
  
### Benchmark Comparison

In terms of raw performance, C3P0 comes out ahead by almost a second and a half.  For a short-running test suite like that of ActiveObjects, that's a fairly impressive difference.  That translates into hours of clock time saved on a database-intensive application of the course of a few weeks.  In my book, that's something seriously worth considering.

Proxool came in a solid second place, at eighteen and a half seconds.  It's definitely slower than C3P0, but it's a full two seconds faster than commons-dbcp.  Considering that Proxool is licensed under the far less restrictive Apache License, it may be worth sacrificing the odd millisecond per query, depending on the opinion of your legal department.

commons-dbcp was the slowest of the three benchmarked at a disappointing twenty and one half seconds.  I'm not entirely sure why DBCP is so much slower in its default, commons-pool backed implementation.  However, the fact remains that performance-wise, it isn't even worth comparing with C3P0.  Seems I need to make some changes in the classpath of some of my projects...

Throughout the whole benchmarking process, I was constantly reminding why Vista is so notoriously difficult as a host OS for application benchmarks.  The results were constantly fluctuating dramatically up and down, based on how much Vista had superloaded, indexing state, open apps, etc.  In short, Vista was so frustratingly difficult to deal with in the testing process that the test results should be treated with some skepticism.  After all, it's hard to say that this is empirical, hard evidence when I'm throwing away three quarters of the test results due to vast deviation from the mean.

### Conclusion

I must (grudgingly) admit that C3P0 is probably the best choice for most projects.  I say grudgingly because the extreme lack of documentation really bothers me.  Granted, Proxool, the next closest in performance, only has an advantage in licensing; its documentation is no better than C3P0's.  Proxool of course has the added disadvantage of having a difficult API, as well as less popularity, therefore fewer articles and samples available around the web.

So if you're a license purist, and you want an intuitive API at the expense of performance, commons-dbcp is the way to go.  However, if you're willing to work within the restrictions of the LGPL license and you know how to use Google effectively, C3P0 would be the preferred choice, given its higher performance and excellent configurablility.

**Update:** I didn't have time to run the benchmarks in any sort of rigorous way (see aforementioned whining about Vista's benchmark flakiness), but preliminary runtimes indicate that [DBPool](<http://homepages.nildram.co.uk/~slink/java/DBPool/>) is an even better framework, performance-wise.  It has a less restrictive license than C3P0, and seems to have a second to a second and a half edge in runtime. Again, these are just quick numbers I grabbed as I was adding support for the provider to ActiveObjects, but I thought it was worth mentioning.