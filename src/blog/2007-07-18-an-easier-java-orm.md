{%
laika.title = "An Easier Java ORM"
laika.metadata.date = "2007-07-18"
%}


# An Easier Java ORM

There is no doubt that Hibernate is the dominant ORM for Java applications. There are Hibernate conferences and Hibernate user groups. It even has its own shelf at Barnes and Nobel. However, I have always found it suffering from a fatal flaw: it's incredibly complex. Even accomplishing the simplest of functionality requires hundreds of lines of code, reams of boiler-plate POJO definitions, and a fair share of XML. Granted, it's probably simpler and more scalable than using JDBC in its raw form, but I still think it makes things harder than they have to be. This has been a common theme throughout most ORMs in any language. Even various Python ORMs, touted for simplicity and power, still remained burdened with either cryptic syntaxes, or mountains of boiler-plate. This general failing is probably the single most important factor which has contributed to the success of Ruby on Rails. Rails is nothing without its flagship component, ActiveRecord. AR provides an incredibly simple and powerful database wrapper layer which makes working with almost any schema simple and intuitive. Granted, migrations are a bit weird and it's performance is far from satisfactory, but the fact remains that it's much easier, quicker (and thus cheaper, at least in the short term) to develop a database-reliant application using ActiveRecord than it is to build the same application on top of Hibernate or iBatis. This has been carried to such an extreme that developers have taken to using ActiveRecord on top of JDBC through JRuby just to handle the database layer of their Java application. Clearly a better solution is required. Much of ActiveRecord's power is drawn from the fact that very little code is required to interact with the database. For example, extracting data from a hypothetical _people_ table in a database is as simple as: 

```ruby
class Person < ActiveRecord::Base
end

puts Person.find(1).first_name   # roughly == SELECT first_name FROM people WHERE id = 1
```

Obviously, the first_name method doesn't exist in the Person class. This code is using a feature of Ruby which allows for methods to be defined at runtime (or rather, not defined and just handled). It should be apparent how much this could simplify database access, and how unfortunately inapplicable it is to Java. Fortunately, the principles of ActiveRecord are not _entirely_ irrelevant. The Java reflection API contains a little-known feature called interface proxies. It allows code to define a dynamic implementation of a given interface which handles method invocations in a single, dynamic location. It's not quite dynamic method definitions, but it is dynamic method handling and it eliminates a lot of boiler-plate. So what good does this do us? Enter [ActiveObjects](<https://activeobjects.dev.java.net>)! ActiveObjects is a Java ORM based on the concept of interface proxies. The whole idea behind it is that you shouldn't have to write any more code than absolutely necessary. In fact, ActiveObjects was designed from the ground up to be dead-easy to use. Here's a rough approximation of the previous ActiveRecord example, translated into Java using the ActiveObjects ORM: 

```java
public interface Person extends Entity {
    public String getFirstName();
    public void setFirstName(String firstName);
}

EntityManager em = new EntityManager(jdbcURI, username, password);

Person p = em.get(1);
System.out.println(p.getFirstName());    // executes "SELECT firstName FROM person WHERE id = 1"
```

Isn't that refreshingly simple? With the exception of placing the appropriate JDBC driver in the classpath, that's all there is to using ActiveObjects. AO automatically detects the database type from the JDBC URI and loads the appropriate driver. If you have placed a pooling library on the classpath, it will be used to pool the database connections. You'll notice the Person implementation is created by the EntityManager instance corresponding to the (presumably existing) database row, and the SQL is automatically generated and executed, typing the result and returning it from the method. AO even uses PreparedStatement(s) for maximum performance (currently unsupported by ActiveRecord). And if we were to call the _setFirstName(String)_ method, an SQL UPDATE statement would be automatically executed based on the data specified. Of course, simple CRUD operations do not an ORM make. For one thing, it'd be really nice to be able to automatically generate database-specific DDL statements, ala Hibernate and ActiveRecord. Coolly enough, ActiveObjects allows for this too: 

```java
System.out.println(Generator.generate("../classes", "jdbc:mysql://localhost/db", "test.package.Person"));
```

This would output: 

```sql
CREATE TABLE person (
    id INTEGER AUTO_INCREMENT,
    firstName VARCHAR(45),
    PRIMARY KEY(id)
);
```

With a different JDBC URI, the DDL would be different. ActiveObjects completely insulates the developer from any database inconsistencies. In fact, the only steps needed to switch to a different database are to place the appropriate driver on the classpath, and to change the URI. To round up our little tour of ActiveObjects, it's worth looking at relations a bit. In ActiveRecord, relations are specified using the _has_many_ and _belongs_to_ methods. Hibernate uses annotations/custom javadoc syntax to accomplish roughly the same thing. In ActiveObjects, things are nice and clean: 

```java
public interface Person extends Entity {
    public String getFirstName();
    public void setFirstName(String firstName);

    public House getHouse();
    public void setHouse(House house);
}

public interface House extends Entity {
    public String getAddress();
    public void setAddress(String address);

    @OneToMany
    public Person[] getPeople();
}
```

With just the above entity definitions, calls can be made to instances of Person, passing the House instance to store. Likewise, House#getPeople() can be called, which will return an array of all Person instances associated with that House. Obviously, the Java instance itself isn't being stored in the database, but rather the houseID INTEGER value is persisted, through the restriction of the appropriate foreign key. It almost seems magical when you first start using it. :-) I've written two test applications (not downloadable, but included within the [ActiveObjects SVN](<https://activeobjects.dev.java.net/source/browse/activeobjects/>)) which illustrate the framework's potential more fully. Also, I've been using ActiveObjects as the basis for a reasonably complex Wicket application deployed in the "real world", and I must say it's performed beautifully. Now here comes a moment of bad news: it's not fully documented yet. Since it's only on its 0.3 release, I've been more interested in squashing bugs and adding features than writing javadoc, though I assure you it is on my agenda. There are a few brief (and somewhat dated) examples on the website, as well as partial javadoc for the [EntityManager](<https://activeobjects.dev.java.net/api/net/java/ao/EntityManager.html>) class. Thankfully, the framework itself is remarkably intuitive and very easy to pick up. There're so many more cool features I would love to have shown here, but unfortunately all bloggers have sworn in blood that they will not extend their posts beyond 1500 words. If you are interested, I'll write a follow-up post going into further detail about the framework (including the more performant _SaveableEntity_ super-interface, which does not execute an UPDATE on every setter call). Do yourself a favor and take a look at the API and examples. Once you get used to the simplicity of Entity interfaces and non-configuration of database connections, you'll never look back. <https://activeobjects.dev.java.net>