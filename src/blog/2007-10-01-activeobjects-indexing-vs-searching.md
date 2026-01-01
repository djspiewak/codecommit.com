{%
laika.title = "ActiveObjects: Indexing vs Searching"
laika.metadata.date = "2007-10-01"
%}


# ActiveObjects: Indexing vs Searching

So in the intervening time since I last updated you on [ActiveObjects](<https://activeobjects.dev.java.net/>), I've been busy refactoring and repurposing some of the core.  I've added a full JUnit test suite, which definitely helps my own confidence about the stability of the source.  Also, there's a whole bunch of new features that have come down the pipe which hopefully I'll get to address in the next few posts.  So, without further ado...

The change which is probably going to cause you the most grief is the switch from _@Index_ to _@Searchable_ , and the addition of the _@Indexed_ annotation.  Yes, you really did read that correctly; and no _@Indexed_ isn't even related to the functionality provided by the old _@Index_ annotation.

The old _@Index_ annotation used to handle tagging of entity methods to mark them as to be added to the Lucene full-text search index (see [this post](</java/an-easier-java-orm-indexing>) for more details).  This was a little confusing for a number of reasons, not the least of which my failure to remember the tense of the annotation name (see the comments on the indexing post).  By convention, most Java annotations are declarative in name.  Thus, the name should not be the present tense "index" but the past tense "indexed".  So my first thought was to just refactor the annotation, but then I came into a slightly hairier name-clash.

### Database Field Indexing

One of the most common techniques for optimizing your database's read (SELECT) performance is to create indexes on certain fields.  When a field is indexed, the database will maintain some separate hash tables to enable very fast selection of rows based on the field in question.  This is a really good thing for almost all foreign keys, for example:

```sql
SELECT id FROM people WHERE companyID = ?
```

Here we're SELECTing the _id_ field from the _people_ table where _companyID_ matches a certain value.  The database can execute this query fairly quickly.  In fact, the only bottle-neck is finding all of the rows which match the specified _companyID_ value.  In a table containing hundreds of thousands of rows, one can see how this could be a problem.

The problem goes away (sort of) with the use of field indexing.  Instead of having to linearly search through the table for rows matching the _companyID,_ the database can perform a quick hash lookup in an index and get a set of rowid(s) based on the _companyID._   Simple, efficient, and incredibly scalable.  Practically, the DBMS wouldn't take any longer to execute such a query against a table of 100,000,000 rows than it does to execute the same query against a table of 100 rows.  So, this is a _really_ good thing right?

Well, indexes have their drawbacks.  I won't go into all of the reasons not to use indexes, the following two points will likely suffice:

  * Indexing slows down UPDATEs and INSERTs 
  * Indexing adds to your table's storage requirements (all those hashes have to be put somewhere)

So perfect database performance isn't attainable just by indexing every field, one has to be quite judicious about it.  In fact, choosing the fields to be indexed is really as much of an art as it is a science.

This long and tangled introduction actually did have a point...  ActiveObjects didn't have any support for field indexing.  I hadn't really considered the possibility, so I didn't factor it into my design.  In retrospect, this was probably a bad idea.  So making up for lost time, I've now introduced field indexing into the library!

```java
public interface Person extends Entity {

    @Indexed
    public String getName();
    @Indexed
    public void setName(String name);

    public int getAge();
    public void setAge(int age);

    public Company getCompany();
    public void setCompany(Company company);
}
```

When ActiveObjects generates the migration DDL for this table (running against MySQL), it will look something like this:

```sql
CREATE TABLE people (
    id INTEGER NOT NULL AUTO_INCREMENT,
    name VARCHAR(255),
    age INTEGER,
    companyID INTEGER,
    CONSTRAINT fk_people_companyID FOREIGN KEY (companyID) REFERENCES companies(id),
    PRIMARY KEY(id)
);
CREATE INDEX name ON people(name);
CREATE INDEX companyID ON people(companyID);
```

This is where the aforementioned _@Indexed_ annotation comes in.  As you can see from the resultant DDL, adding a field index is as simple as tagging the corresponding method.  Also, foreign keys are automatically indexed, to ensure maximum performance in SELECTion of relations.

So that's the good news, the bad news is that index creation doesn't play too nicely with migrations.  Everything works of course, and if you're actually adding a table or field, the corresponding index(es) will also be created.  Likewise if you drop a table or field, the corresponding index(es) will also be dropped.  However, that's about the limit of the migrations support for indexes at the moment.  JDBC has an unfortunately limitation which prevents developers from getting a list of indexes within a database.  Since I have no way (within JDBC) of finding pre-existing indexes, they must be excluded from the schema diff and thus are CREATEd or DROPped irregardless of the existing schema.  I do have a plan to fix this (along with migrations on Oracle), but I seriously doubt that it'll be included in the 1.0 release, owing to the changes required.

### Refactoring End Result

The final result of the refactoring and the adding of field indexing is that the annotation formerly named _@Index_ is now the _@Searchable_ annotation.  Likewise (and keeping with convention), the formerly _IndexingEntityManager_ is now named _SearchableEntityManager_.  Both of these types remain in the _net.java.ao_ package.  To allow for field indexing, the _@Indexed_ annotation was added to the _net.java.ao.schema_ package, owing to the fact that it only effects schema generation and doesn't change runtime behavior in the framework at all.

Hopefully these changes won't be too confusing (now that you're aware of them) and will be a welcome addition to the ActiveObjects functionality.  As always, I welcome comments, suggestions and criticisms!