{%
laika.title = "An Easier Java ORM: Relations"
laika.metadata.date = "2007-07-31"
%}


# An Easier Java ORM: Relations

Per request in a comment on the [previous article](<http://blogs.dzone.com/daniel/2007/07/30/an-easier-java-orm-part-4/>), I've decided to devote this post to the subject of relations and how they work in [ActiveObjects](<https://activeobjects.dev.java.net>). There are two types of relations in database design: one-to-many and many-to-many. In a one-to-many relation, multiple rows in one table usually have a reference to a single row in another table (or quite possibly the same table, depending on the design). As a simple example, assume with me that all tapeworms reproduce solely asexually (which isn't exactly true, but that's beside the point). Thus, a single tapeworm would have dozens of offspring. The proud parent tapeworm would have a one-to-many relation to its children (if you could refer to them as such). Another, less parasitic example would be people living in a city. The city would have a one-to-many relation to the thousands of people living within its borders. One-to-many relations are expressed in ActiveObjects about as simply as the concept can be expressed in words: 

```java
public interface City extends Entity {
    public String getName();
    public void setName(String name);

    @OneToMany
    public Person[] getOccupants();
}

public interface Person extends Entity {
    public String getFirstName();
    public void setFirstName(String firstName);

    public String getLastName();
    public void setLastName(String lastName);

    public City getCity();
    public void setCity(City city);
}
```

The "people" table (assuming we're using the pluralizing name converter) would contain fields "id", "firstName", "lastName", and "cityID", with a foreign key constraint on cityID ensuring it matches an existing "cities.id" value. The "cities" table would only contain "id" and "name" fields. The real magic occurs when the dynamic proxy handler attempts to field a call to the getOccupants() method. One of the first thing the method does (after checking for a defined implementation) is look for a relation tagging annotation. In this case, it finds @OneToMany. The handler then looks at the return type for the method, extracts the table name for the Person entity class, and constructs a query to return any people which have a cityID equaling the appropriate id. If there were more than one field in Person which returned a City, each field would be checked for the relation. Thus, minimally the SQL executed would look like this: 

```sql
SELECT DISTINCT outer.id FROM (SELECT id FROM people WHERE cityID = ?) outer
```

The outer/inner query construct is used to allow the UNIONing of multiple SELECT results corresponding with different fields in the people table. With a trivial sub-query like the one given, most database engines will optimize the SQL down to a single, highly performant query. 

### Many-To-Many

Many-to-many relations are even cooler than one-to-many(s). This is where you begin to see complex mappings such as the (usually standard) two-parent to many children relations in families. Another example might be user permissions. There are several user permission types, each of which could map to an arbitrary number of users. A straight one-to-many mapping doesn't work here, since neither "end" of the relation refers to a single entity. Thus, we have a many-to-many relation. In database design, many-to-many relations are almost always expressed using an intermediary table. Thus, if we were to map authors to books (allowing for co-authorships), we would have to create a design with three tables: authors, books and authorships, which solely handles the mapping between author and book. Here's how this would look in ActiveObjects: 

```java
public interface Author extends Entity {
    public String getName();
    public void setName(String name);

    @ManyToMany(Authorship.class)
    public Book[] getBooks();
}

public interface Authorship extends Entity {
    public Author getAuthor();
    public void setAuthor(Author author);

    public Book getBook();
    public void setBook(Book book);
}

public interface Book extends Entity {
    public String getName();
    public void setName(String name);

    @ManyToMany(Authorship.class)
    public Author[] getAuthors();
}
```

The code is very similar to the @OneToMany example, with the exception that here we have to provide the class for the intermediary "mapping table". Once again, the dynamic proxy will find the @ManyToMany annotation, and using the table name for the intermediary table, derive a query relating books to authors based on _any_ relevant mapping fields within Authorship. **Note:** while the example given does use the @ManyToMany annotation on both "ends" of the relation, this isn't required. The annotation merely informs the proxy handler on how to deal with the method call (as well as ensures the schema generator ignores the method signature).