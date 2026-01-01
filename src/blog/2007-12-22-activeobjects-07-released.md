{%
laika.title = "ActiveObjects 0.7 Released"
laika.metadata.date = "2007-12-22"
%}


# ActiveObjects 0.7 Released

Just in time for the Christmas holidays, I give you [ActiveObjects](<https://activeobjects.dev.java.net>) version 0.7!  This release sports a whole slew of minor performance tweaks and bugfixes, providing better reliability.  Also, we've greatly increased the number and scope of the unit tests running against the core.  This gives us more confidence that the code is indeed as stable as we hope.

Actually, the big ticket item for 0.7 is the introduction of [polymorphic relations](<http://www.codecommit.com/blog/java/polymorphic-relational-types-in-activeobjects>).  This feature is fully tested and stable, so I don't anticipate it introducing any issues in the coming days as we narrow our focus to 1.0.  However, no feature is so well tested as to be certainly bug free under all circumstances.  I look forward to fixing any problems you may encounter with the library!

A few minor feature additions:

  * Support for persisting enum values
  * One-to-one relations
  * _EntityManager#flush(...)_ now also flushes the relations cache

Looking toward the road ahead, things are really starting to settle down a bit.  The main feature I'm looking to implement for 0.8 is extensible value caching, which should allow you to determine how and where the cached field values are stored.  The main use-case behind this is support for memcached, which has been requested a few times and has actually been on the agenda since 0.3.  With memcached driving the value cache, ActiveObjects should be almost perfectly scalable.

To that end, it's also worth mentioning that I did some profiling (using the NetBeans Profiler) using a [Wicket](<http://wicket.apache.org>)-based web application I'm developing on the side.  I'm happy to report that the overhead imposed by ActiveObjects is extremely minimal.  In each test, the bottleneck was in the _PreparedStatement#execute_ method (in JDBC).  Since ActiveObjects generates very streamlined and natural queries, it seems that this is really the minimal execution time possible for such database access.  Extrapolating further from this data, and given that the JDBC exec time was avg 40ms, while the execution time in the ActiveObjects API was on the order of 1ms-5ms, we can safely say that ActiveObjects is less than 10% slower than using JDBC directly (compare that to the [50% overhead claimed by ActiveRecord](<http://wiki.rubyonrails.org/rails/pages/ActiveRecord>)).  This "estimate" doesn't even take into account the many performance _advantages_ offered by ORMs over hand-coded database access (such as relations caching, optimal query construction, etc).

Anyway, enough shameless boasting.  As Hibernate has often pointed out, ORM benchmarks are usually worse than useless.  The only benchmark which really matters is how well the ORM performs in your application.  Try it out, see what you think.

  * [activeobjects-0.7.tar.gz](<https://activeobjects.dev.java.net/files/documents/6960/80096/activeobjects-0.7.tar.gz>)
  * [activeobjects-0.7.zip](<https://activeobjects.dev.java.net/files/documents/6960/80097/activeobjects-0.7.zip>)