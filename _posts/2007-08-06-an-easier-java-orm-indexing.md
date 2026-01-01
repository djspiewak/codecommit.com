---
categories:
- Java
date: '2007-08-06 14:58:39 '
layout: post
title: 'An Easier Java ORM: Indexing'
wordpress_id: 119
wordpress_path: /java/an-easier-java-orm-indexing
---

In continuing with my series on [ActiveObjects](<https://activeobjects.dev.java.net>), this post delves into the eternal mysteries of search indexing and [Lucene](<http://lucene.apache.org/java/docs>) integration. Most modern web applications not only store data in a database, but also in an index of some kind to allow fast and efficient searching. Java's Lucene framework provides an excellent mechanism for this functionality, however it can be somewhat cryptic and hard to use. To ease this pain, ActiveObjects provides auto-magical Lucene integration for specified fields, making it trivial to index and search for entities. Unless there is great public outcry, I intend this to be the last of my "Easier Java ORM" series (with the exception a roundup post for linking purposes). As fun as it is being self-promoting and pushing my favorite open source project, I feel a slight twinge of guilt every time I flood your feed agregator with _more_ information on a library in which you may or may not have interest. I'll probably still post about ActiveObjects from time to time, but only on occasions when there is something of special note. 

### Indexing

Of course, we can't even begin to talk about searching for entities unless there is some data from the entity added to the index. The actual creation and maintenance of the index is usually considered the hardest part of working with Lucene. In ActiveObjects, it requires two separate steps. Firstly, you must decide which fields and which entities you wish to index. Let's say that we have a simple blog schema as follows: 

```java
public interface UserModifiedEntity extends SaveableEntity {
    @Default("CURRENT_TIMESTAMP")
    public Calendar getDate();
    @Default("CURRENT_TIMESTAMP")
    public void setDate(Calendar calendar);

    @Default("false")
    public boolean isDeleted();
    @Default("false")
    public void setDeleted(boolean deleted);
}

public interface Post extends UserModifiedEntity {
    public String getTitle();
    public void setTitle(String title);

    @SQLType(Types.CLOB)
    public String getText();
    @SQLType(Types.CLOB)
    public void setText(String text);

    @OneToMany
    public Comment[] getComments();
}

public interface Comment extends UserModifiedEntity {
    public Post getPost();
    public void setPost(Post post);

    public String getCommenter();
    public void setCommenter(String name);

    @SQLType(Types.CLOB)
    public String getText();
    @SQLType(Types.CLOB)
    public void setText(String text);
}
```

In this schema, we have both Post and Comment entities. Both entity types extend UserModifiedEntity, which contains some fields which will be common to both resulting tables. Both Comment and Post also have "text" fields, containing the actual meat of each entity's value. Now, for our blog's search engine, we're going to want to do something a bit more precise than search for _all_ values contained in any entities. Actually, at this point, ActiveObjects wouldn't index any values whatsoever. We need to tag the fields we want to add to the index with the @Indexed annotation. Let's assume that we don't need to search on comments at all, just posts. The modified Post entity might look something like this: 

```java
public interface Post extends UserModifiedEntity {
    @Index
    public String getTitle();
    @Index
    public void setTitle(String title);

    @Index
    @SQLType(Types.CLOB)
    public String getText();

    @Index
    @SQLType(Types.CLOB)
    public void setText(String text);

    @OneToMany
    public Comment[] getComments();
}
```

That takes care of step one in the indexing procedure. ActiveObjects now has everything it needs to know relating to _what_ it should index. Now we need to inform it to actually perform the indexing, and _where_ to store the result. This is all handled using a special _EntityManager_ subclass: _IndexingEntityManager_. 

```java
// ...
IndexingEntityManager manager = new IndexingEntityManager(jdbcURI, username, password, 
        FSDirectory.getDirectory("~/lucene_index"));

Post post = manager.create(Post.class);
post.setTitle("My Cool Post");
post.setText("Here's some test text that I'll use to test the search indexing.  "
        + "It's really amazing what you can do with so little code...");
post.save();
```

As you can see, we're using an instance of IndexingEntityManager to access and create all of our entity instances (all one of them). This is all that is necessary to cause ActiveObjects to handle the indexing for these entities. Oh, _FSDirectory_ is actually a Lucene class (sub-classing Directory) which is used to tell the Lucene backend where to store the index. Since we're actually using the Lucene Directory abstraction classes, the index could just as easily be stored in memory, or even in another database. 

### Searching

Obviously, an index isn't all that useful if you can't do anything with it. Since our goal from the start was to provide search capabilities to our rather limited blog, we need to have a way of accessing the Lucene indexing and performing a search. Again, ActiveObjects makes this incredibly easy: 

```java
// ...code from above
Post[] results = manager.search(Post.class, "test search terms");

System.out.println("Search results:");
for (Post post : results) {
    System.out.println("   " + post.getTitle());
}
```

The search method delegates its call down to the Lucene engine, which parses the search terms and runs through the index searching for any key-value sets (or Document(s), as Lucene refers to them) which match in the "title" or "text" fields. By default, ActiveObjects runs the search against all index fields in the specified entity type. Since this is usually the behavior people want when using Lucene, it is a sane default. If the mindless defaults aren't good enough for your application, you are quite free to use the Lucene index directly. IndexingEntityManager provides accessors for the Directory containing the index, as well as the Analyzer in use. (getIndexDir() and getAnalyzer()) Of course, you can also extend IndexingEntityManager and provide your own search() implementation. 

### Removing from the Index

Almost as important as adding entities to an index is removing them. We don't want our searches to pull back deleted posts. IndexingEntityManager can handle this task for us automatically, to a point. The problem is that in our case, we're not actually _deleting_ the posts as such. We're simply setting a flag in the row which indicates the post is deleted. We're supplying all of the logic (theoretically) to ignore deleted posts and comments. If we were using the EntityManager#delete(Entity...) method, we would be DELETEing the rows properly and then IndexingEntityManager could automatically remove the relevant Document(s) from the index. However, since we're not doing this, we need a bit more logic. For simplicity's sake, we're going to put this logic into a defined implementation for the UserEditableEntity interface: 

```java
@Implementation(UserEditableEntityImpl.class)
public interface UserEditableEntity extends SaveableEntity {
    // ...
}

public class UserEditableEntityImpl {
    private UserEditableEntity entity;

    public UserEditableEntityImpl(UserEditableEntity entity) {
        this.entity = entity;
    }

    public void setDeleted(boolean deleted) {
        if (deleted && !entity.isDeleted()) {
            // deleting the entity, remove it from index
            ((IndexingEntityManager) entity.getEntityManager()).removeFromIndex(entity);
        } else if (!deleted && entity.isDeleted()) {
            // we're un-deleting the entity here
            ((IndexingEntityManager) entity.getEntityManager()).addToIndex(entity);
        }

         entity.setDeleted(deleted);
    }
}
```

Now, whenever we call _setDeleted(boolean)_ on a Post or Comment instance, it will be removed from the index (if we're deleting the entity), or re-added to the index (if we're un-deleting it). In the case of Comment, it has no @Indexed methods, so IndexingEntityManager will more or less ignore the call to addToIndex(Entity) (it actually will iterate through all of the methods to find any @Indexed). 

### Related Content

Many sites have need of a "related content" algorithm. This is most often seen in blogs which show a list of "related posts". Since ActiveObjects auto-magically handles indexing and searching, it only makes sense that it provide some mechanism for accessing related entities based on their indexed values. This is handled using the _RelatedEntity_ super-interface. Let's assume that we want to be able to find related posts to a given Post instance. The only thing we need to do is make sure that the Post interface also extends RelatedEntity: 

```java
public interface Post extends UserEditableEntity, RelatedEntity<Post> {
    // ...
}
```

Now we can call: 

```java
Post post = // ...
Post[] related = post.getRelated();

System.out.println("Posts related to " + post.getTitle() + ":");
for (Post relate : related) {
    System.out.println("   " + relate.getTitle());
}
```

Alright, caveat time... First off, this does depend on the Lucene Queries contrib library, specifically the _MoreLikeThis_ class. Secondly, I'm not entirely sure that this is working right. :-) I've yet to actually get it to return any related values whatsoever in my test bed. This could be due to the way I'm indexing, or possibly the way I'm using MoreLikeThis; I'm not sure. If it works for you, let me know! Also, if you have any experience with the MoreLikeThis functionality, I'd appreciate any pointers you may have. Well, that about sums it up for indexing in ActiveObjects. Hopefully, this simplifies your data backend code still some more and eases your pain in dealing with Lucene.