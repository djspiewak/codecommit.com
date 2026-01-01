---
categories:
- Java
date: '2008-03-22 22:07:47 '
layout: post
title: ActiveObjects 0.8 Released
wordpress_id: 216
wordpress_path: /java/activeobjects-08-released
---

Happy Easter everyone, we've got a new release!  [ActiveObjects](<https://activeobjects.dev.java.net>) 0.8 is simmering at low heat (and footprint) on the servers even as I type.  This is probably the most significant milestone we've released thus far in that the 1.0 stream is now basically feature-complete.  I won't be adding _any_ new features to this release, just polishing and testing the heck out of the old ones.  We've already got a suite of 91 tests and I've got lots of ideas for more.  Expect this framework to get very, _very_ stable in the next few months.

### Features

Despite an annoyingly cramped schedule in the last couple months, I have managed to implement a few long-requested features.  The big ticket item for this release is pluggable cache implementations.  For those of you not "in the know", ActiveObjects has a multi-tiered cache system composed of several layers and delegates.  This isn't entirely unusual for an ORM, which is why I hadn't mentioned it before.  The three basic layers:

  * **Entity Cache** \- A `SoftReference` map from id/type pairs to actual entity values.  This means that if you run a query which returns an entity for `people {id=1}` and then run a second query which returns an entity for the same row, the two instances will be identical.  This is what enables the additional cache layers to function properly (and it saves on memory). 
  * **Value Cache** \- This is where all the field values for a given row are stored.  This will store both field values from the database as well as entity values (corresponding to foreign key fields).  Each entity has a separate instance of this cache.  Most of the memory required by ActiveObjects is taken up here.  With a long-running application, quite a bit of the database can actually get paged into memory.  This is a really good thing, since it drastically reduces round-trips to the database.  Thanks to the `SoftReference`(s) in the entity cache, running out of memory due to stale cache entries isn't a problem. 
  * **Relations Cache** \- This is probably the most complex of all of the cache layers.  This cache literally caches any _one-to-one_ , _one-to-many_ or _many-to-many_ result sets and spits them out on demand.  This reduces round-trips still more to the point where certain applications can be running completely from memory, even querying on complex relations.  This in and of itself isn't that complex; the hard part is knowing when to expire specific cache entries.  This cache layer is essentially three different indexes to the same data, allowing cache expiry for a number of different circumstances.  Naturally, these structures don't represent well as a single map of key / value pairs.  (more on this in a bit)

As of the 0.8 release, the value cache and relations cache layers have been unified under a single controller.  They're still separate caches, but this unification allows for something I've been wanting to implement since 0.2: Memcached support.

High-volume applications often run into the problem of limited memory across the various server nodes.  Solutions like Terracotta can help tremendously, but unfortunately this doesn't solve everything.  One answer is to provide every node in the cluster with a shared, distributed, in-memory map of key / value pairs.  This allows the application to page data into cluster memory and retrieve it extremely quickly.  Memcached is effectively this.  Naturally, it would be very useful if an ORM could automatically make use of just such a distributed memory cache and thus share cached rows between nodes.  Hibernate has had this for a long time, and now ActiveObjects does as well.

By making use of the `MemcachedCache` class in the [activeobjects-memcached](<https://activeobjects.dev.java.net/files/documents/6960/91091/activeobjects-memcached-0.8.tar.gz>) module (separate from the main distribution), it is possible to point ActiveObjects at an existing Memcached cluster and cause it to store the value cache there, rather than in local memory.  Once the relevant call has been made to `EntityManager` (setting up the cache), all cached rows will be stored in Memcached and shared between every instance of ActiveObjects running in your cluster.

#### Caveats

Of course, nothing is perfect.  For the moment, the major issues is that the relations cache doesn't work properly against Memcached.  I had originally planned to just release it running against the local RAM, but this causes issue with proper cache expiry.  As such, the relations cache is disabled when running ActiveObjects against Memcached.  This doesn't really impair functionality other than forcing ActiveObjects to hit the database every time a relationship is queried.  Since this is what most ORMs do anyway, it's not too horrible.

I plan to have this issue resolved in time for the 0.9 release.  The biggest problem is figuring out how to flatten the multi-index structure into a single map.  Once I can do that, rewriting things for Memcached should be a snap.  (btw, if anyone wants to help out with patches in this area, you're more than welcome!)

### Databases Galore

One of the major focuses of this release was testing on different platforms.  This process uncovered an embarrassing number of bugs and glitches in some of the supposedly "supported" databases.  With the exception of Oracle, every bug I've been able to identify has been fixed.  So if you've tried ActiveObjects in the past and run into problems on non-MySQL platforms, now would be the time to give it another shot!

Speaking of Oracle, we've made some tremendous progress toward full support of this ornery database.  To be fair, I'm not the one responsible.  One of our community members has been tirelessly submitting patches and running tests.  We didn't have time to get things fully sorted for this release (so don't try AO 0.8 with Oracle unless you're willing to risk server meltdown), but you can expect dramatic improvements in 0.9.  Once these fixes are in place, we should be able to safely claim that we support most of the database market (in terms of product adoption).

### Final Thoughts

This really is an amazing release, well worth the download.  Most of the core API has remained the same, so things should be basically plug-n-play for existing applications.  The user-facing API is now in freeze and (unless some serious bug arises) will experience no changes between now and 1.0.  If you've been considering trying ActiveObjects for your project, now would be an excellent time.  I can't promise no bugs, but if you point out my idiocy, I'll certainly work to fix it!

Oh, as an aside, ActiveObjects is now in a Maven2 repository.  (thanks to some POM reworking by [Nathan Hamblen](<http://technically.us/code>))  To make use of this somewhat dubious feature, add the [java.net Maven2 repository](<https://maven2-repository.dev.java.net/>) to your POM and then insert the following dependency:

```xml
<dependency>
    <groupId>net.java.dev.activeobjects</groupId>
    <artifactId>activeobjects</artifactId>
    <version>0.8</version>
</dependency>
```

After that, the magic of Maven will take over.  Of course, you'll need to add the dependency for the appropriate database driver and (optionally) connection pool.  Those dependencies are left as an exercise to the reader.  Note, _activeobjects-memcached_ isn't in Maven yet, but it will be for the 0.9 release (either that or integrated into the core, I haven't decided yet).

I certainly hope you enjoy this latest release.  As always, feel free to drop by the [users list](<mailto:users@activeobjects.dev.java.net>) if you have any questions or (more likely) uncover any problems.

  * [activeobjects-0.8.tar.gz](<https://activeobjects.dev.java.net/files/documents/6960/91089/activeobjects-0.8.tar.gz>)
  * [activeobjects-0.8.zip](<https://activeobjects.dev.java.net/files/documents/6960/91090/activeobjects-0.8.zip>)