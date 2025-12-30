---
categories:
- Java
date: '2007-10-15 00:00:48 '
layout: post
title: Custom Primary Keys with ActiveObjects
wordpress_id: 140
wordpress_path: /java/custom-primary-keys-with-activeobjects
---

One of the main complaints I've heard leveled against [ActiveObjects](<https://activeobjects.dev.java.net/>) is that it's just not suitable for mapping to legacy schemas.  More generically, concerns have been mooted that it enforces naming conventions and field conventions which aren't suitable/preferable for some projects.  I suppose at first both of these were true.  After all, ActiveObjects's entire premise was convention over configuration, and this requires some restrictions by default.  However, I don't think it's entirely accurate any longer.

Over the last few months, I've added several features which satisfy three primary goals:

  * Customize the table name convention 
  * Customize the field name convention 
  * Allow for primary key fields (and types) other than `id INTEGER`

The first two goals were easily met through the addition of `TableNameConverter` and `FieldNameConverter`.  These two classes are used by every feature within ActiveObjects, from migrations to simple data access, to determine the database table and field names from the class and method names respectively.  The canonical example of this is table name pluralization, which can be accomplished in the following way:

```java EntityManager manager = new EntityManager( "jdbc:mysql://localhost/test", "username", "secret"); manager.setTableNameConverter(new PluralizedNameConverter()); ``` 

Not too horrible.  The second use-case is assigning a different field name convention than the default camelCase.  For example, some people really like the ActiveRecord (Rails) field naming convention.  (e.g. "first_name" as opposed to "firstName")  This can easily be accomplished by specifying a field name converter:

```java EntityManager manager = new EntityManager( "jdbc:mysql://localhost/test", "username", "secret"); // lower_case convention manager.setFieldNameConverter(new UnderscoreFieldNameConverter(false)); ``` 

Custom table and field name converters are also possible, allowing for a great deal of flexibility in name conventions.  Additionally, it's always possible to specify field and table names directly in the entities, using the `@Accessor`, `@Mutator` and `@Table` annotations respectively.

### Custom Primary Keys

The most challenging goal (from a library standpoint) is to allow for primary key fields other than "id".  This is partially such a challenge because it had been hard coded literally _everywhere_ in ActiveObjects that the "id" field is the field to use in any sort of SELECT, JOIN, INSERT, UPDATE, etc.  In short, changing this required finding all of these instances and converting the code to query a centralized source for the data.  A few days of fiddling with Eclipse's text search accomplished this without inordinate pain, but the hard part was coming.

The question remained: how to specify the primary key within the entity itself?  After all, it's been hard coded and sort of magically "worked" based on the method definition in the `Entity` superinterface.  There had been a syntax to specify a second PRIMARY KEY for the schema migration, but ActiveObjects didn't treat these fields any differently, and this sort of syntax wouldn't really cut it if we were trying to completely override the existing `getID()` method in the superinterface.

The solution is to refactor all of the interesting functionality in `Entity` up into a super-superinterface, `RawEntity`.  Thus the only method defined within `Entity` would be `getID()`, annotated appropriately to be recognized as a PRIMARY KEY field.  This would do away with all the magic tricks under the surface which assumed the existence of the `getID()` method.  ActiveObjects can easily parse the class to find the PRIMARY KEY field amongst the methods, both defined and inherited.  The only compromise which must be made is only one PRIMARY KEY can now be allowed per table.  This isn't such an issue, since 99% of the time, that's all you need anyway.  Usually that remaining 1% can be more properly accomplished using UNIQUE and some sort of auto-generation of values.

Since we've refactored interesting functionality up into `RawEntity` and kept `getID()` within `Entity`, no legacy code needs to be changed.  Any entities previously written against ActiveObjects will run without modification or any behavior changes.  We are merely allowed the flexibility of specifying our own primary keys.  So, without further ado, the obligatory example:

```java public interface Person extends Entity { public String getFirstName(); public void setFirstName(String firstName); public String getLastName(); public void setLastName(String lastName); public Company getCompany(); public void setCompany(Company company); public House getHome(); public void setHome(House home); } public interface Company extends RawEntity { @PrimaryKey @NotNull @Generator(UUIDValueGenerator.class) public String getCompanyKey(); public String getName(); public void setName(String name); @OneToMany public Person[] getEmployees(); } public interface House extends RawEntity { @PrimaryKey @NotNull @AutoIncrement public int getHouseID(); // ... @OneToMany public Person[] getOccupants(); } public class UUIDValueGenerator implements ValueGenerator { public String generateValue(EntityManager em) { // generate uuid return uuid; } } // ... Person p = manager.get(Person.class, 1); Company c = manager.get(Company.class, "abff999dd99ddf0a225f"); ``` 

Maybe a bit longer of an example than you were expecting, but it does cover the material well.  What's happening here is the `Person` entity has a standard, "id" primary key.  This follows the same convention that ActiveObjects has been enforcing since the beginning of time (or at least since I started the project).  `Company` and `House` are the interesting entities here.

`House` defines a `getHouseID()` method of type `int` which is marked as a PRIMARY KEY as well as being auto-incremented by the database (SERIAL on PostgreSQL, AUTO_INCREMENT on MySQL, etc).  This is the same sort of declaration that you would find if you looked in the source for `Entity`.  The difference is that `House` will not contain the "id" field and its PRIMARY KEY will be "houseID".  The _really_ interesting entity here `Company`.

`Company` defines a primary key that is not only a different field, but also an entirely different type.  Also, its value is generated automatically not by the database, but by the application itself.  This is a fairly common use-case in those crazy databases which use UUIDs as primary keys.  Not only does this field define "companyKey" as a different type than INTEGER, but it also ensures that the "companyID" FORIEGN KEY field in the "person" table is also of type VARCHAR.

Another item of note in this example is that the `RawEntity` interface is parameterized.  This is to allow the `get(...)` method in `EntityManager` to stay type-checked, ensuring that the values passed are actually valid primary key values for the entity in question.  Of course, there's nothing that can be done to ensure that the actual method definition of the primary key is of the proper type.  However, at some point the developer must be trusted to make sure their entity model doesn't violate the dictates of logic.

### Conclusion

With this latest addition to the ActiveObjects feature set, it should be possible to use the ORM with any schema whatsoever.  While AO may still be an implementation of the active record pattern, and thus less powerful than solutions such as Hibernate, there should be no problems applying AO to just about any sane use-case.