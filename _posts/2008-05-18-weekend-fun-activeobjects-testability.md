---
categories:
- Java
date: '2008-05-18 12:43:21 '
layout: post
title: 'Weekend Fun: ActiveObjects Testability'
wordpress_id: 230
wordpress_path: /java/weekend-fun-activeobjects-testability
---

I'm not entirely sure what [these metrics](<http://code.google.com/p/testability-explorer>) mean, but they give me a warm feeling inside.  :-)

```
Analyzed classes:   136
 Excellent classes (.):   121  89.0%
      Good classes (=):     9   6.6%
Needs work classes (@):     6   4.4%
             Breakdown: [.............................................===@@]
       0                                                                    118
    10 |......................................................................:   118
    31 |..                                                                    :     3
    52 |                                                                      :     0
    73 |===                                                                   :     5
    94 |===                                                                   :     4
   115 |                                                                      :     0
   136 |@@                                                                    :     3
   157 |                                                                      :     0
   178 |                                                                      :     0
   199 |                                                                      :     0
   220 |                                                                      :     0
   241 |                                                                      :     0
   262 |                                                                      :     0
   283 |                                                                      :     0
   304 |@                                                                     :     1
   325 |                                                                      :     0
   346 |@                                                                     :     1
   367 |                                                                      :     0
   388 |                                                                      :     0
   409 |                                                                      :     0
   430 |                                                                      :     0
   451 |                                                                      :     0
   472 |                                                                      :     0
   493 |@                                                                     :     1
   514 |                                                                      :     0

Highest Cost
============
net.java.ao.EntityManager 501
net.java.ao.schema.SchemaGenerator 353
net.java.ao.EntityProxy 296
net.java.ao.schema.ddl.SchemaReader 141
net.java.ao.RelatedEntityImpl 127
net.java.ao.SearchableEntityManager 127
net.java.ao.Query 99
net.java.ao.types.EntityType 87
net.java.ao.db.HSQLDatabaseProvider 85
net.java.ao.db.OracleDatabaseProvider 85
net.java.ao.DatabaseProvider 83
net.java.ao.Common 82
net.java.ao.EntityManager$1 82
net.java.ao.db.PostgreSQLDatabaseProvider 82
net.java.ao.types.TypeManager 80
net.java.ao.schema.ddl.DDLAction 30
net.java.ao.SoftHashMap 28
net.java.ao.schema.AbstractFieldNameConverter 28
net.java.ao.SoftHashMap$HashIterator 20
```

Most of the badness seems to stem from `EntityManager`, which makes a lot of sense given the way it is designed.  `EntityProxy` also poses issues, but in practice this isn't a real problem because of how extensive the JUnit tests are for just this class.  Overall, ActiveObjects testability isn't anywhere near to the [Guice score](<http://www.testabilityexplorer.org/report/guice/guice/1.0>), but it's not as horrible as [JRuby](<http://www.testabilityexplorer.org/report/jruby/jruby/0.9.8>).