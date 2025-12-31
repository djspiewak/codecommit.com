---
categories:
- Database
date: '2008-02-25 01:00:03 '
layout: post
title: Should ORMs Insulate Developers from SQL?
wordpress_id: 194
wordpress_path: /database/should-orms-insulate-developers-from-sql
---

This is a question which is fundamental to any ORM design.  And really from a philosophical standpoint, how should ORMs deal with SQL?  Isn't the whole point of the ORM to sit between the developer and the database as an all-encompassing, object oriented layer?

A long time ago in an office far, far away, a very smart cookie named Gavin King got to work on what would become the seminal reference implementation for object relational mapping frameworks the world over (or so Java developers would like to think).  This project was to be bundled with JBoss, possibly the most popular enterprise application server, and would support dozens of databases out of the box.  It was to offer heady benefits such as totally object-oriented database access, transparent multi-tier caching and a flexible transaction model.  At its core though, Hibernate was design to resolve a single problem: application developers hate SQL.

No really, it's true!  Bread-and-butter application developers really dislike accessing data with SQL.  This has led to endless conflict (and bad jokes) between application developers and database administrators.  Often times the developer team would write a set of boilerplate lines in Java and then copy/paste these arbitrarily throughout their code, swapping in the relevant query as supplied by the DBA.  For obvious reasons, this would become very hard to maintain and just intensified the bad blood between developer and database.

If you think about it though, it's a bit odd that this intense dislike would mutate from just hating the insanity of JDBC to hating JDBC, SQL and RDBMS in general.  SQL is a very nice, almost mathematical language which allows phenomenally powerful queries to be expressed simply and elegantly.  It abstracts developers from the headache of database-specific hashing APIs and algorithms which are almost filesystems in complexity.  The language was designed to make it as easy as possible to get data out of a relational database.  The fact that this effort backfired so utterly is a source of endless confusion to me.

But irregardless, we were talking about ORMs.  When it was first introduced, Hibernate held out the promise that developers would never again have to wade knee deep through a sea of half-set SQL.  Instead, developers would pass around POJOs (Plain Old Java Object(s)), modifying their values like any other Java bean and then handing these objects off to the data mapper, which would handle the details of persistence.  Furthermore, Hibernate promised that developers would never again have to worry about which databases support which non-standard SQL extensions.  Since developers would never have to work with SQL, anything database-specific could be handled within the persistence manager deep in the bowels of Hibernate itself.

This all seems lovely and wonderful, but there's a catch: it doesn't work so well in practice.  Now before you stone me, I'm not talking about Hibernate specifically now, but ORMs in general.  It turns out to be completely impossible to interact with a _relational_ database solely through an object-oriented filter.  This is easily seen with a simple example:

```sql SELECT * FROM people WHERE age > 21 GROUP BY lastName ``` 

How in the world are you going to represent that in an object model?  Sure, maybe you can provide a little abstraction for the query details, but it starts to get complex if you try to handle things like grouping non-declaratively.  The developers working on Hibernate quickly realized this problem and came up with an innovative solution: write their own query language!  After all, SQL is too confusing, so why not invent an entirely new query language with the "feel" of SQL (to keep the DBAs happy) but without all of the database-specific wrinkles?

This query language is now called "HQL", and as the name implies, it's really SQL, but not quite.  Here's how the aforementioned example would look in HQL ( **disclaimer:** I'm not a Hibernate expert, so I may have gotten the syntax wrong):

``` FROM Person WHERE :age > 21 GROUP BY :lastName ``` 

Remarkably similar, that.  Executing this query in a Hibernate persistence manager yields an ordered list of _Person_ entities pre-populated with data from the query.  It seems to make a lot of sense, but there are a number of problems with this approach.  First, it requires Hibernate to literally have its own compiler to translate HQL queries into database-specific SQL.  Second, it hasn't really solved the core problem that many developers have with SQL: it's a declarative query language.  As you can see, HQL is really just SQL in disguise, so it really doesn't eliminate SQL from your database access, just dresses it in a funny hat.

Other ORMs have appeared over the years, taking alternative approaches to the problem of object-relational mapping, but none of them quite eliminating the query language.  Even DSL-based ORMs like ActiveRecord fail to remove SQL entirely:

```ruby class Person < AR::Base; end Person.find(:all, :conditions => 'age > 21', :group => 'lastName') ``` 

It's _sort of_ SQL-free, but you can still see bits and pieces of a query language around the edges.  In fact, what ActiveRecord is actually doing here is building a proper SQL query around the SQL fragments which are passed as parameters.  It's a system which is ripe for SQL injection, but surprisingly leads to very few problems in real-world applications.  This is the approach which is also taken by [ActiveObjects](<https://activeobjects.dev.java.net>) for its database query API.

So ORMs in and of themselves seem to have failed to entirely eliminate SQL from the picture, but what about other frameworks?  There are a few quite recent efforts which seem to have nearly succeeded in eliminating the direct use of SQL completely from application code.  [Ambition](<http://ambition.rubyforge.org/>) is perhaps the best (and most clever) example of this, though others like [scala-rel](<http://code.google.com/p/scala-rel/>) are catching up fast.  Ambition is designed from the ground up to interact naturally with ActiveRecord, so the two combined perhaps represent the first "true" ORM: one which does not require the developer at _any_ point to deal with any SQL whatsoever.

But was it really worthwhile?  As clever as things like Ambition are, is it really that much easier than just writing queries in SQL?  As Nathan Hamblen so [eloquently said](<http://technically.us/code/archive/2007/11/#item-4710>) (when referring to a totally different topic): 

> ...is the end of the ORM rainbow.  You get there, throw yourself a party and realize that important things are broken.

A quote taken out of context perhaps, but I think it applies to the "cult of SQL genocide" with as much validity.  In the end, by denying yourself access to the powerful and well-understood mechanism that is SQL, you're just crippling your own application and forcing yourself to write _more_ code instead of less.

So what's the "right" approach?  Is there a happy medium between ActiveRecord+Ambition and full-blown [SQL on Rails](<http://www2.sqlonrails.org/>)?  I think so, and that is the approach I have been _trying_ to implement with ActiveObjects.  As I'm sure you know, ActiveObjects takes a lot of its inspiration from ActiveRecord, so the syntax for querying the database is very similar:

```java EntityManager em = ... em.find(Person.class, Query.select().where("age > 21").group("lastName")); // ...or em.find(Person.class, "name > 21"); // no grouping ``` 

You still have the full power of SQL available to you.  You can still write complex, nested boolean conditionals and funky subqueries, but there's no longer any need to be burdened with the _whole_ of SQL's verbosity.  As with vanilla ActiveRecord, this code intends to be a bit of a hand-holder, shielding innocent application developers from the fierce world of RDBMS.

Is this the right way to go?  I'm honestly not sure.  I've met a lot of developers that would give their left eye to never have to look at another SQL statement again (for developers already missing a right eye, this isn't much of a stretch).  On the other hand, there are purists like myself who revel in the freedom afforded by a powerful, declarative language.  It's hard to say which path is better, but at the end of the day, it's really the question itself that matters.  Giving application developers the _choice_ to select whichever approach they feel is most appropriate, _that_ is the solution.