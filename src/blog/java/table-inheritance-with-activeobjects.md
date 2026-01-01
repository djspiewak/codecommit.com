{%
laika.title = "Table Inheritance with ActiveObjects"
laika.metadata.date = "2007-11-19"
%}


# Table Inheritance with ActiveObjects

The main obstacle in building an ORM is deciding how to map between a class hierarchy and a database table set.  While this may seem fairly clear cut in many situations, it's unfortunately not so easy once you get into more complex issues like inheritance.

At a basic level, tables are classes.  It makes sense, right?  A class defines a structure which is instantiated and populated with data.  A table defines a structure into which data is inserted, one set per row.  Superficially, these two concepts sound more-or-less identical.  The differences creep in when you look at things like a class hierarchy.  Classes have the ability to inherit attributes from a superclass.  Thus, if class A inherits from class B, class A has everything that class B has (simplistically put). 

Unfortunately, database tables have no such capability.  You can't define a table which has everything a "supertable" has.  Nor can you select data out of a "subtable" as if it were a supertable (polymorphism).  This lack of capability creates a disconnect between database structure and object-oriented class structure.

As with all of these "little differences" (between languages and databases), many solutions have been tried, none of them very successfully.  One of the most popular ways to solve the problem is to use separate tables for the supertable and subtable.  Then, whenever one SELECTs from the subtable, a JOIN is used to include the inherited row from the supertable in the result set.  In principle, it sounds right.  After all, this is how C++ handles inheritance.  However, in practice it becomes rather unwieldy.

To start with, JOINing _every_ time you perform a simple SELECT is both inefficient and annoying.  Maintaining two separate rows is also a difficult problem to keep up with.  Inevitably, the data gets a bit out of sync and the whole thing falls apart.  Granted a good ORM will prevent this from happening in the first place, but ORMs are not the exclusive means for accessing database schemata.  Often times your schema will be in use not just by your application, but also by a web site, a demo utility and random DBAs who refuse to enter data in any way other than hand-written SQL.  In short, table inheritance using JOINs is clunky, unmaintainable and really messes you up down the line.

Another, more conservative way to map inheritance is inclining, where every subtable contains all of the fields which _would_ be in a supertable.  Thus, if table A and B inherit from table C, table A and B are created with all of the fields that would have been in table C, and table C isn't created at all.  Technically speaking, it's not inheritance, just a slightly more centralized way to specify fields.  However, this technique is much more in line with how a database schema would normally work if designed by hand, and that's what ORMs are supposed to simplify, right?

### The ActiveObjects Approach

As you have have guessed from my comments, I really lean toward the inline strategy.  I think this works best in a practical environment and is far more maintainable down the line.  Thankfully, this strategy is considerably easier to implement than JOINing.  Syntax-wise, inheritance is used like this:

```java
public interface Person extends Entity {
    public String getFirstName();
    public void setFirstName(String firstName);

    public String getLastName();
    public void setLastName(String lastName);
}

public interface Employee extends Person {
    public String getTitle();
    public void setTitle(String title);

    public short getHourlyRate();
    public void setHourlyRate(short rate);

    public Manager getManager();
    public void setManager(Manager manager);
}

public interface Manager extends Person {
    @OneToMany
    public Employee[] getPeons();

    public long getSalary();
    public void setSalary(long salary);
}
```

A fairly standard, object-oriented interface hierarchy, right?  Logically it makes sense.  This is how ActiveObjects would represent such a hierarchy in the database:

**employees**  
---  
id  
firstName  
lastName  
title  
hourlyRate  
managerID  
  **managers**  
---  
id  
firstName  
lastName  
title  
salary  
  
So basically inheritance in ActiveObjects is just a mechanism which allows the definition of common fields in a single place.  There's no real polymorphism (that is to say, you can't INSERT an employee where a person is required, since there is no "people" table).  The idea is: keep it simple stupid.  In fact, ActiveObjects would happily generate a table to correspond to Person if we asked it to, since as far as it's concerned it's just an entity like any other.

Of course, there are quite a few serious disadvantages to this approach.  For one thing, without polymorphism or actually centralizing the fields in a common table, inheritance is just an illusion.  From a database standpoint, the tables aren't really related in any interesting way.  You're still duplicating fields in the database (though not duplicating data), though since the duplication is all handled by the ORM, it's not too much of an issue.

For me though, it all comes back to the decision I made to not make the schema over-complicated.  To ensure that no matter what the object hierarchy, it could be represented in the database (assuming it's valid) without undue weirdness.  Keep the schema simple, make it easy to do stuff outside of the ORM.  The ORM should be a liberating too, not a constraining one.

I realize not everyone agrees with this particular style of table inheritance, so maybe at some point in the future ActiveObjects will allow configuration in this area.  But for the moment, the uncomplicated schema is the way to go.  :-)  Enjoy!