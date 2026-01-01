---
categories:
- Java
date: '2007-08-14 12:22:46 '
layout: post
title: 'Performance is Good: ActiveObjects vs ActiveRecord'
wordpress_id: 121
wordpress_path: /java/performance-is-good-activeobjects-vs-activerecord
---

So [ActiveObjects](<https://activeobjects.dev.java.net>) is a fairly cool ORM.  However, coolness alone does not an enterprise ORM make.  In fact, the real qualifications for an enterprise-ready framework are as follows:

  * Stability 
  * Performance

I'm sure there are other questions which factor into design decisions on whether or not to use a library, but those are the two which I look at most closely.  Stability is usually a hard metric to find, since it usually depends on a lot of adopters hammering the library until it breaks, is fixed and then hammered again.  However, performance numbers are almost always easy to come by, since all that is required are a few simple benchmark tests to just get a ballpark-number.

Since benchmarks are so fun, I've decided to do a few for ActiveObjects.  Or rather, I've decided to run a simple (read, _very_ simple) benchmark test with ActiveObjects as well as a number of other ORMs.  At the moment, I've only been able to run the test with ActiveRecord (sorry guys, Hibernate's a really complex framework), but I think the numbers are still worth looking at.

ActiveRecord [claims](<http://wiki.rubyonrails.org/rails/pages/ActiveRecord>) _only_ a 50% overhead compared to manual database access (that number is actually listed as a feature).  There has been some dispute over whether the test used to obtain that particular figure was valid or not, but that's besides the point.  ActiveObjects should be able to do at least that well, right?

Well, as it turns out, it can.  Here are the numbers from my reasonably simple benchmark:

```
ActiveObjects
==============
Queries test: 55 ms
Retrieval test: 68 ms
Persistence test: 55 ms
Relations test: 154 ms

ActiveRecord
=============
Queries test: 154 ms
Retrieval test: 6 ms
Persistence test: 76 ms
Relations test: 75 ms
```

Surprisingly close numbers actually.  I had assumed that there would be some significant disparity, one way or another.  However, as you can see ActiveObjects is fairly comparable to ActiveRecord on a set of extremely trivial tests.  There are some jumps and obvious areas of strength/weakness in both frameworks, but on average they're pretty similar in performance.

As my friend [Lowell Heddings](<http://www.lowellheddings.com>) pointed out, ORM benchmarks are far more useful if you actually examine the SQL generated to see how efficient it really is from a theoretical standpoint.  So, to make things easier I sed/grepped the logs and arrived at the following SQL outputs for each respective ORM.

  * [ao_log.txt](<http://www.codecommit.com/blog/wp-content/uploads/2007/08/ao_log.txt>) \- ActiveObjects 
  * [ar_log.txt](<http://www.codecommit.com/blog/wp-content/uploads/2007/08/ar_log.txt>) \- ActiveRecord

### Details

Now, I will be the first to admit that this is hardly at even test to begin with.  Obviously there are different strengths and weaknesses in every library, and though I tried to be impartial in the designing of the benchmarks, I probably accidentally favored one ORM over the other.  Also, there are inherent performance advantages to Java over Ruby, especially in the area of database access.  In short, ActiveObjects probably had a sizeable advantage coming right out of the gate, so take my numbers with a grain of salt.

The test itself consisted of four phases, each involving three entities: Person, Profession and Workplace.  Person has a many-to-many relation with Profession through a fourth entity, Professional.  Workplace has a one-to-many relation with Person.  These relations were exploited directly in the relations benchmark (e.g. _Person#getProfessions(), Workplace#getPeople(),_ etc).  Each entity had a number of fields, including one CLOB (or TEXT, as MySQL refers to them) in the Person entity.  The tables for each respective schema were pre-populated with the same data, which involved several rows with different values (except for the CLOB, which was a roughly 4000 character paragraph and the same for every row).  In the ActiveObjects Person entity, I used the @Preload annotation to eagerly load _firstName_ and _lastName._

For the retrieval test, the benchmark iterates through every Person row and grabs _firstName, lastName, age, alive,_ and _bio._   Since ActiveObjects only preloaded _firstName_ and _lastName_ , it suffered a bit here. 

The persistence test iterates through every person row and changes the first and last name to one selected from a pool of names I populated with random names which came to mind.  It then goes through the same iteration again and sets the _age_ , _alive_ flag and the _bio_  to our 4000 word Pulitzer-winning essay.  Each row is saved through each iteration, thus each row is saved exactly twice throughout the test.  ActiveObjects came out ahead here probably because of its use of PreparedStatements, as well as the more efficient UPDATE statement generation.

The relations test involved first finding all of the Professions associated with each individual Person and retrieving the Profession _name_.  Next, the Workplace for the Person is retrieved, then all of the Person(s) associated with that Workplace and their _firstName_ and _lastName_ values accessed.

The queries test was little more than getting all of the Person(s), all of the Workplace(s), all of the Professional mappings, along with all of the Profession(s).  ActiveObjects far outperformed ActiveRecord in this area since ActiveRecord uses SELECT * for everything and eagerly loads the row values.  This means (especially with a CLOB thrown into the mix) that ActiveRecord's initial query time will be very long, while it's field access time will be very quick.  Most ORMs function in this way, and it can be a very good thing at times (our benchmark is one of those times).

### Lessons Learned

  * Eager loading can be a good thing 
  * ActiveObjects generates some _weird_ SQL for relations access

Obviously I can only do so much about the eager loading issue.  I believe pretty strongly that ActiveObject's approach (in lazy loading most things) is the right one for most use-cases.  However, the second lesson to be learned here is one which I think I need to take a bit more to heart: keep it simple SQL.

Normally, ActiveObjects will generate a query something like the following for accessing a one-to-many relation:

```sql
SELECT DISTINCT a.outMap AS outMap FROM (
    SELECT id AS outMap,workplaceID AS inMap FROM people 
       WHERE workplaceID = ?) a
```

Yuck!  For obvious reasons, this is an incredibly inefficient bit of querying.  Actually, not only is it inefficient, but needlessly so.  You and I of course know that we could replace the above query with the much simpler:

```sql
SELECT id FROM people WHERE workplaceID = ?
```

So why doesn't ActiveObjects do that?  Frankly, I was lazy in my coding of the _EntityProxy#retrieveRelations_ method, so a lot of ugly SQL slipped through the cracks in cases where it really wasn't necessary.  I've spent a bit of time on this, and I think I've got the issue resolved.  The problem is that ActiveObjects was assuming that any relation (one-to-many or many-to-many) can have multiple mapping fields, thus requiring a wrapping DISTINCT outer query around a subquery SELECT which is UNIONed with an arbitrary number of other SELECTs, corresponding to the other mapping fields.  Obviously, it is almost never the case that we have to deal with multiple mapping paths, so I added a short-circuit to the logic which creates far simpler queries if at all possible.  As a result, the benchmark numbers for the relations test in ActiveObjects are between 80 and 100 ms.  Still slower than ActiveRecord, but much improved.

It's worth noting that if we ran each benchmark twice, we would see a marked improvement in the ActiveObjects performance the second time through.  Not just because a lot of the values would be cached, but also because the prepared statements in question would have been compiled and stored.  This is a fairly major area in which ActiveRecord falls short since it doesn't utilize prepared statements, thus having a constant runtime for its queries and remaining unable to take advantage of cached, compiled queries.

So in short, ActiveObjects may be really neat, but it's performance numbers don't seem all that superior to those of ActiveRecord, a Ruby ORM with numerous known shortcomings in this area.  I guess I need to work on things a bit more.  :-)  Next up, either manual JDBC code or Hibernate running the same benchmark, depending on how soon I'm able to figure out Hibernate's crazy XML mapping schema.

**Note:** I forgot to mention this... You can get the source for my benchmark from the ActiveObjects SVN repository: `svn co https://activeobjects.dev.java.net/svn/activeobjects/trunk/Benchmarks`