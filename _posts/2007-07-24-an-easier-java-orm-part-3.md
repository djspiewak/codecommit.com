---
categories:
- Java
date: '2007-07-24 12:44:56 '
layout: post
title: An Easier Java ORM Part 3
wordpress_id: 116
wordpress_path: /java/an-easier-java-orm-part-3
---

As a continuation to [part 1](<http://blogs.dzone.com/daniel/2007/07/18/an-easier-java-orm/>) and [part 2](<http://blogs.dzone.com/daniel/2007/07/19/an-easier-java-orm-part-2/>), this article examines a slightly less interesting (though none-the-less important) feature, transactions in [ActiveObjects](<https://activeobjects.dev.java.net>). I also figured I should give a minor update on the development status. [Eelco Hillenius](<http://chillenious.wordpress.com/>) (of the [Wicket](<http://wicket.apache.org>) project) has been contributing some code, fixing up areas where I messed up and cleaning up one of the sample projects (the DogfoodBlog sample, based on Wicket). We've also been looking into areas where Wicket and ActiveObjects could be more easily integrated (such as entity-specific models and lists). I've been keeping busy trying to stabilize some of other database providers. We now officially support (in the trunk/) the following databases: 

  * Derby (standalone and embedded)
  * MySQL
  * PostgreSQL
  * Oracle (with the limitation that @AutoIncrement is only supported on primary keys)

We also have the following (almost untested) providers: 
  * Microsoft SQL Server (Microsoft and JTDS providers)
  * HSQLDB (standalone and embedded)

I'm hardly an expert in any of the above databases, so I welcome any and all input on bugs/errors/etc... 

### Transactions

While I might qualify transactions as comparatively unimportant, anyone who's written a multi-user web application knows how significant they can be in maintaining data integrity. An ORM which didn't support transactions would be sorely crippled indeed. Hibernate solves the problem of transactions by forcing every database action to be wrapped in a transaction and committed at the close. This works well, but the significant downside is that it increases code complexity and forces the developer to remember to explicitly close _every transaction._ As before, I've tried to take some inspiration from ActiveRecord's implementation of concepts: ```ruby p = Person.find(1) lc = LibraryCard.find(321) transaction do p.first_name = 'Daniel' p.last_name = 'Spiewak' p.save lc.person_name = 'Daniel Spiewak' lc.save end ``` As is typical of ActiveRecord, this is an amazingly intuitive syntax. Unfortunately, again in accordance with the ActiveRecord MO, it depends upon a very DSL-like syntax. We might not be able to match the syntax exactly in Java, but we can fairly close: ```java new Transaction(manager) { public void run() { Account david = getEntityManager().get(Account.class, 1); david.setValue(david.getValue() - 1000); david.save(); Account mary = getEntityManager().get(Account.class, 2); mary.setValue(mary.getValue() + 1000); mary.save(); } }.execute(); ``` Obviously, we could potentially have gone with the less verbose: ```java Transaction t = new Transaction(entityManager); t.begin(); // blah t.commit(); ``` However, this syntax doesn't have the major advantage of the ActiveRecord DSL which is ensuring the transaction is closed. Also, with our anonymous inner-class syntax, we can do something like this: ```java new Transaction(manager) { public void run() { // yadda yadda yadda } }.executeConcurrently(); ``` This will execute the transaction in a separate thread, rather than blocking the current one. I haven't actually seen a proper use-case for this so far, but it's bound to have some application. :-) 

#### Limitations

The trick with transactions in JDBC is all queries must be run against the same Connection instance. To accommodate this, an internal abstraction class is used by the ActiveObjects interface proxy to ensure that if a transaction is in progress, a single Connection is used, rather than opening a new one from the database provider. This works quite well, but it has its limitations. Specifically, any custom code executed within the transaction which uses a Connection instance directly will run its SQL statements outside of the transaction boundaries. Any EntityManager method (get, find, findWithSQL, count, etc...) will work within a transaction, but any direct use of a Connection instance will likely cause unexpected behavior. I have a plan to fix this problem, but I haven't gotten around to implementing it yet. Hopefully in a shortly upcoming release...