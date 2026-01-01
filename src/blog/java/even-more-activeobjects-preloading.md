{%
laika.title = "Even More ActiveObjects: Preloading"
laika.metadata.date = "2007-08-13"
%}


# Even More ActiveObjects: Preloading

There has been some talk recently regarding the ActiveObjects lazy-loading mechanism.  It's starting to seem that what I thought was a great idea and terribly innovative when I designed the framework might not have been such a great idea after all.  :-)  That's a good thing though, finding my mistakes that is, it just forces me to think a little harder about how to solve the problem.

One of the guiding ideas behind ActiveObjects is that nothing should be loaded until it's needed.  Once it's loaded, it should be cached and then up-chucked on command, obviating the need for multiple loads.  This technique, commonly known as "lazy-loading", works really well if you're in a memory-crunch situation.  This is because even for tables with extremely large numbers of columns (think 50-100), none of the data in a row is loaded if you don't need it.  Thus, you could work with a database-peered object without having to load the entire row into memory, a potentially long and expensive operation.

The problem with this is it tends to create large numbers of queries.  Also, it can be very inefficient for certain types of operations.  For example:

```java
for (Person p : manager.find(Person.class)) {
    System.out.println(p.getName());
}
```

This will generate the following SQL (assuming 6 rows in the people table):

```sql
SELECT id FROM people
SELECT name FROM people WHERE id = ?
SELECT name FROM people WHERE id = ?
SELECT name FROM people WHERE id = ?
SELECT name FROM people WHERE id = ?
SELECT name FROM people WHERE id = ?
SELECT name FROM people WHERE id = ?
```

Granted, it's a prepared statement, so it will be compiled and run very quickly 5 out of 6 times.  However, this is still pretty inefficient.  Imagine if there were 100,000 people in the database, instead of 6 (not an unreasonable assumption).  This code could take hours to run.

Now, if you were writing the JDBC code by hand, you'd probably do something like this (exception handling omitted):

```java
Connection conn = getConnection();
PreparedStatement ps = conn.prepareStatement("SELECT name FROM people");
ResultSet res = ps.executeQuery();
while (res.next()) {
    System.out.println(res.getString("name"));
}
res.close();
ps.close();
conn.close();
```

One statement, that's all that's really required.  Paging through a result set is a pretty quick operation, so even with 100,000 rows this shouldn't be an insanely slow piece of code.  In fact, the slow-down here is probably how fast the console can print the text in question (not very fast actually).

So, obviously we have very disparate performance between JDBC by hand and using ActiveObjects, and we really can't have that.  The solution is to force ActiveObjects to somehow load all of the names for the people in the first query, like we did when we ran the SQL by hand.  For a while now, ActiveObjects has had this capability:

```java
for (Person p : manager.find(Person.class, Query.select("id,name"))) {
    System.out.println(p.getName());
}
```

Now we just execute a single line of SQL:

```sql
SELECT id,name FROM people
```

Much more efficient.  However, the code is now much uglier and a little unintuitive. (I mean, who's going to think of Query.select("...") when looking to override lazy-loading?)  Also, we would have to use this cryptic syntax in every single query in which we want to override the lazy-loading.  This could be a bit of a pain, especially if you know at design time that every time you get a Person, you'll probably need a "name" shortly thereafter.  So, for situations just like this one, I've now added the @Preload annotation (not in the 0.4 release, available in trunk/)

```java
@Preload("name")
public interface Person extends Entity {
    public String getName();
    public void setName(String name);

    public int getAge();
    public void setAge(int age);
}

// ...
for (Person p : manager.find(Person.class)) {
    System.out.println(p.getName());
}
```

Just as we would expect, this now runs the following single-query SQL statement:

```sql
SELECT name,id FROM people
```

If we were to add a call to p.getAge(), it would of course lazy-load that value, leading to another SQL statement.  However, we can just as easily add it to the @Preload clause like this:

```java
@Preload({"name", "age"})
public interface Person extends Entity {
    // ...
}
```

Or, since this is really all of the properties in Person, we can use the following, shorter syntax:

```java
@Preload
public interface Person extends Entity {
    // ...
}
```

So effectively, you can disable lazy-loading in ActiveObjects by adding the @Preload annotation without any parameters to every entity you use.  However, this is a little inefficient since it will pretty much turn any non-joining SELECT statement into a SELECT *.  For this reason, I suggest you only use @Preload for situations like our name-printing loop.  In other words: only for values you know will be queried every time you grab a bunch of entities of a given type.

One more thing worthy of note: this is a _hint_ only.  It doesn't mean that every Person instance will have a preloaded name value.  Any Query(s) with JOIN clauses will ignore the @Preload annotation to avoid accidentally running JOINs with SELECT *.  Also, quite a few Person instances won't have any values at all by default.  For example, if you use _EntityManager#create(),_ a new row will be INSERTed into the people table, but the resulting Person instance won't have any value cached for name.  Likewise, if you make a simple call to _EntityManager#get(Class <? extends Entity>, int)_, this will return the Entity instance which corresponds to that _id_ value, but it may or may not have a cached _name.   _Thus, the _get()_ method still does not run any queries, it merely creates the object peers.