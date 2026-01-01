---
categories:
- Java
date: '2007-07-30 13:38:57 '
layout: post
title: An Easier Java ORM Part 4
wordpress_id: 117
wordpress_path: /java/an-easier-java-orm-part-4
---

In keeping with my [ActiveObjects ](<https://activeobjects.dev.java.net>)series, here's part 4. In this part, we'll look at schema generation/migration and the ever-interesting topic of pluggable name converters and English pluralization. One of ActiveObjects's main concepts is that you (the developer) should never have to worry about the semantics of database design. You should simply be given the tools to design your models in a natural and object-oriented way, and the database just sort-of takes care of itself. To allow this sort of simplicity, the database schema has to be automatically generated, leaving nothing to the developer in this area. Fortunately, ActiveObjects does poses this capacity. 

### Schema Generation

In a nutshell, schema generation in ActiveObjects works by parsing the specified entity interfaces. First, a dependency tree is built, ensuring that the schema generation occurs in the proper order satisfying all dependent tables. ActiveObjects does generate all foreign keys for you, ensuring data integrity and maximum performance. This of course has the unfortunate side effect that everything must be inserted in the proper order; hence the tree. Next, the dependency tree is passed through a loop which iterates through it and invokes _DatabaseProvider#render(DDLTable)_ , which generates the database-specific DDL statements necessary to create the entity-corresponding schema. This all sounds fine-and-dandy on paper (or in this case, screen), but when you actually try to implement it in a real-world senario it gets a bit sticky. For one thing, Java's types are nowhere near as robust as the ANSI SQL types. Additionally, almost every DDL allows developers to put certain restrictions on fields such as default values, auto incrementing or even forcing table-unique values. The solution, avoiding XML and other non-Java meta-programming, is to use annotations: 

```java
public interface Person extends SaveableEntity {
    public String getFirstName();
    public void setFirstName(String firstName);

    @Unique
    @SQLType(precision=128)
    public String getLastName();

    @Unique
    @SQLType(precision=128)
    public void setLastName(String lastName);

    @SQLType(Types.DATE)
    public Calendar getBirthday();
    @SQLType(Types.DATE)
    public void setBirthday(Calendar birthday);

    @Accessor("url")
    public URL getURL();
    @Mutator("url")
    public void setURL(URL url);
}

// ...
manager.migrate(Person.class);    // generates and executes the appropriate DDL
```

It might seem a bit ugly and annotation-ridden, but it does the job well and in pure-Java. There are more annotations which could be used (@OnUpdate, @Default, etc...), but these suffice to give a basic example. You'll notice right off the bat that each annotation has to be applied to both the accessors and the mutators. This is for two reasons. First, Java reflection doesn't guarantee any particular ordering in which it will return the methods for a _Class <?>. _Thus, either the accessor or the mutator could be reached first and used to generate that field's DDL, meaning the metadata needs to be applied to both or it could possibly be ignored by the schema generator. Second, ActiveObjects allows for read-only or write-only fields, meaning you don't need the full accessor/mutator pair to access the database, one will suffice. This could be useful for application static data or for fields you just-plain don't want the application to be able to mutate directly. Because ActiveObjects doesn't assume an accessor/mutator pair, it can't just wait for both the accessor and the mutator to be parsed. Thus, meta is required on both. The other point of interest in this example is the use of the @Accessor and @Mutator annotations. From very early on, ActiveObjects has allowed developers to specify non-conventional method-names as database fields. In this case, ActiveObjects would normally assume the getURL method corresponded to to the "uRL" field (case intentional). This is because AO will assume the default Java get/set/is convention and recase the method accordingly. Since we obviously don't want that for the "url" field, we use the @Accessor and @Mutator annotations to override ActiveObjects's field name parser. 

### Pluggable Name Converters

By default, ActiveObjects uses a very simple set of heuristics to determine the table name from the entity class name. Essentially, this heuristics boil down to a simple camelCase implementation. For example, the "Person" entity would correspond to the "person" table. A "BillingAddress" entity would be mapped to "billingAddress" and so on. This is nicely conventional, but certainly not everyone would agree with this style of table naming. This is why ActiveObjects provides a mechanism to override the table name conversion and specify a custom algorithm. The name converter interface is pretty simple. It has a single method ( _getName(Class <? extends Entity>):String_) which is where the actual "meat" of the conversion happens. It also defines four algorithmically optional methods, which are intended to be used by developers as a quick-and-easy way to specify explicit mappings (for example, sometimes you want to override the algorithm for a specific class or name pattern without adding the @Table annotation to the entity). These methods are important, but could be left as stubs for a custom name-converter used "in house". To make it easier to write custom name converters, an abstract superclass has been created which handles most of the "boiler plate" functionality. I would recommend that this superclass ( _AbstractNameConverter_ ) be used in lieu of a direct implementation of _PluggableNameConverter._ The only difference is that instead of overriding the _getName(Class <? extends Entity>):String_ method from the superinterface, the method to override is _getNameImpl(Class <? extends Entity>):String_. This is the method called by _AbstractNameConverter_ if the entity class in question fails to match an existing mapping. Specifying the name converter to use is as simple as a single method call to _EntityManager_ : 

```java
manager.setNameConverter(new MyNameConverter());
```

From that moment onward, all operations handled by that EntityManager instance (including schema generation) will use the specified pluggable name converter. This allows us to do some interesting things which would be otherwise out of reach... 

#### English Pluralization

Table name mappings like {Person => person} and {BusinessAddress => businessAddress} may be conventional and nicely deterministic, but most people like database designs which reflect the underlying data a bit better. Since tables by definition contain multiple rows, it only makes sense that their names should be pluralized. Since we now have a mechanism to override the default name converter, we should be able to create an implementation which handles this pluralization for us for an arbitrary English word. To accomplish this, ActiveObjects defines a class _PluralizedNameConverter_ in the "net.java.ao.schema" package which extends the default _CamelCaseNameConverter_. The actual implementation within the name converter is fairly simple. All the algorithm needs to do is load an ordered .properties file (included with ActiveObjects) which contains all of the pluralization mappings as regular expressions. (e.g. "(.+)={1}s") These mappings are then added to the name mappings implemented in _AbstractNameConverter_ , which handles the semantic details of mapping the generated (in _CamelCaseNameConverter_ ) singular CamelCase names to their pluralized equivalents. Thus, _PluralizedNameConverter_ doesn't really do much work at all, the real action is in the abstract superclass as it handles the actual logic defined in _englishPluralRules.properties._ This is where the real interest lies. However, detailing all of the ins and outs of English pluralization rules would extend this article even farther beyond the breaking point; so I will have to revisit the topic in a future post. To use the English pluralization for your own projects, simply initialize your _EntityManager_ instance in the following way: 

```java
EntityManager em = new EntityManager(jdbcURI, username, password);
em.setNameConverter(new PluralizedNameConverter());
// ...
```

Now, the _Person_ class will map to the "people" table. Likewise, the _BusinessAddress_ entity will correspond to the "businessAddresses" table. Just like magic! :-) It's not a silver bullet, and I'm sure you'll come across situations where the pluralization will be invalid. This is one of the reasons why manually adding mappings to name converters is possible. My only request is that you send your mappings my way so that I can include them in the properties file, allowing others to benefit from your wisdom. However, despite all the rules (both manual and default) which can be specified, automatic English pluralization is still a very inaccurate science. Thus, the decision was made _not_ to make pluralization the default name conversion method for ActiveObjects, in contrast to the same decision with ActiveRecord, which _is_ pluralized by default. Hopefully, by not forcing pluralization upon you, I've made database design with ActiveObjects a little less stressful than it otherwise could have been. :-) 

### Conclusion

In conclusion: I write blog posts that are way too long. I hope this taste of automated schema generation and name conversion algorithms was helpful and useful to you in your quest for that ever-elusive, intuitive ORM. 

#### (P.S.)

_One of the things I'm currently working on for the upcoming ActiveObjects 0.4 release is the concept of schema migrations. Migrations are a little different than a straight schema generation as they don't eliminate pre-existing data, but merely convert the existing tables to match the current schema definition. This is a mind-bogglingly useful feature in database refactoring, a common activity early in the application design process. Unfortunately, it's also rather hard to do correctly. If you'd like to check up on my progress, criticize my coding style, or just play with the latest-and-greatest version, feel free to checkout ActiveObjects from the SVN:_ svn co https://activeobjects.dev.java.net/svn/activeobjects/trunk/ActiveObjects