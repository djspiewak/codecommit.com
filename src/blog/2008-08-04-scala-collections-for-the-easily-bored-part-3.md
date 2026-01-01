{%
laika.title = "Scala Collections for the Easily Bored Part 3: All at Once"
laika.metadata.date = "2008-08-04"
%}


# Scala Collections for the Easily Bored Part 3: All at Once

In the [previous installment](<http://www.codecommit.com/blog/scala/scala-collections-for-the-easily-bored-part-2>) of this series, we looked at how Scala's collections provide common mechanisms for iteration, as well as many higher-order operations in the same conceptual vein. In this, the third and final part of the series, we will examine some mechanisms for conceptually operating on the collection as a whole. Thus, rather than transforming individual collection elements, we will be looking at ways to inspect and modify the data structure itself.

### Filtration

Stepping out of Scala for a moment, let's consider the common design paradigm of the relational database. Imagine that we have defined a table, `people`, which contains hundreds of thousands of records, assembled by some despotic government. Now perhaps the secret service in this government wants to dispatch legal enforcers to the residences of all adults with the "wrong" political leaning. The most natural way to accomplish this task would be to perform an SQL `SELECT`, filtering by age and politics:

```sql
SELECT * FROM people WHERE age > 18 AND affiliation = 'Green Party'
```

This query would of course return a data set which could be iterated over, performing the necessary operations (in this case, incarceration) for each record. As it turns out, this sort of use-case is not confined solely to databases. It is often necessary to restrict or _filter_ a data structure based on certain criteria. As a trivial example, imagine that we have been passed a `List` of `Int`(s) and we want to remove all odd numbers. We could accomplish this by using the `filter` method and passing a function value to describe the criterion:

```scala
def onlyEven(nums: List[Int]) = {
  nums.filter((n) => n % 2 == 0)
}
```

If we call the `onlyEven` method, passing `List(1, 2, 3, 4, 5)`, the result will be `List(2, 4)`, since `2` and `4` are the only numbers into which 2 divides evenly. As with many of the other common collection operations, Scala includes a syntax sugar for `filter`. We can rewrite the previous example using `for`-comprehensions in the following way:

```scala
def onlyEven(nums: List[Int]) = {
  for {
    n <- nums
    if n % 2 == 0
  } yield n
}
```

This is a little different from the `for`-comprehensions we have seen already in that we have placed the yield statement _outside_ the braces with the comprehension definition within. Believe it or not, this syntax is perfectly valid, coming from Haskell's `do`-notation. This construct will be translated at compile time into a corresponding invocation of `filter` (and `map`, but that isn't terribly relevant here) prior to type checking. This sort of notation can be very convenient for many tasks, similar to LINQ in the .NET languages.

### Partition

In the previous example, we looked at how to selectively remove elements from a collection based on a single criterion. However, sometimes the requirement is not to _remove_ elements, but rather to _separate_ them into different collections. For example, we might not want to simply throw away all odd elements in a list, it might be useful to keep both even and odd lists on hand for further operation. This can be accomplished using the `partition` method:

```scala
def separateEven(nums: List[Int]) = {
  nums.partition((n) => n % 2 == 0)
}
```

You will notice that the signature for `partition` looks remarkably similar to `filter`. This uniformity in the API is by design, making usage both easier to remember and reducing the number of changes necessary to switch between similar operations. However, despite the similarity in dispatch, `partition` returns a very different result:

```scala
val numbers = List(1, 2, 3, 4, 5, 6, 7)
val sep = separateEven(numbers)

sep == (List(2, 4, 6), List(1, 3, 5, 7))    // true
```

Literally, the `partition` method splits elements into two different lists, based on the `boolean` value of the given predicate (in this case, even or odd). These lists are returned as a tuple, Scala's idiom for returning multiple values from a single method.

### Searching

Having the capability to filter and split an entire list is all well and good, but it is perhaps even more common to need to find a specific element within a collection. This is most often seen when dealing with sets or maps, but it can also be useful in the context of linear structures such as list and array. A simple example might be a trivial caching mechanism for database lookups:

```scala
val cache = new HashMap[Int, String]

def readData(id: Int) = {
  if (cache.contains(id)) {    // gonna find her
    cache(id)
  } else {
    val conn = createConnection()

    val back = try {
      val stmt = conn.prepareStatement("SELECT value FROM dataTab WHERE id = ?")
      stmt.setInt(1, id)

      val res = stmt.executeQuery()
      if (res.next()) {
        res.getString(1)
      } else {
        null
      }
    } finally {
      conn.close()
    }

    cache += (id -> back)
    back
  }
}
```

Unlike Java, Scala's collections API is extremely consistent in what methods correspond to what functionality. The `contains` method on a `Map` does in fact search based on _key_ , not value. However, sometimes the situation calls for a solution which is not so specific. Looking for a particular element is great (and very efficient on maps and sets), but it isn't the most general implementation. A more flexible form of searching would be to match based on a higher-order function (just like filter), rather than an explicit value. This not only allows searching for a specific element, but it also provides the ability to look for a range. More generally, the exists method makes it possible to check to see if an element of a given collection satisfies a certain property.

```scala
val nums = List(1, 2, 3, 4, 5, 6)
nums.exists((n) => (3 to 5).contains(n))
```

In this example, we are literally checking the list, `nums`, for any values which are in the mathematical range `[3, 5]`. The `exists` method calls the predicate (our function parameter - or "lambda" if you prefer) for each element in the list until it returns true, at which point the iteration is short circuited. The predicate itself creates a new `Range` and checks to see if the specified value is within that range. As it turns out, `Range` itself is a collection just like any other, defining the same methods that we've come to know at love.

There is one final variation on the "search" theme that is worth examining: `find`. While it's great to know that _some_ element within a collection satisfies a certain property, sometimes it is even more useful to be able to ascertain _what_ element that was. Thus, rather than returning a `Boolean`, the `find` method returns an instance of the `Option` monad containing the first satisfactory element find, or `None` if the property remains unsatisfied. Adapting our example from above yields the following code and its associated result:

```scala
val nums = List(1, 2, 3, 4, 5, 6)
val elem = nums.find((n) => (3 to 5).contains(n))

elem == Some(3)   // true
```

### Conclusion

Well, it's been a rather short series, but hopefully still worth reading. In truth, I skipped a great deal of detail and idiomatic beauty that can be found within the Scala collections API. While the type definitions could certainly stand improvement in some areas, it is already without a doubt the best-designed collections framework I have ever used (in any language). Literally, once you figure out how to best employ one collection type, you will have learned them all. For further reading on the topic, you can always peruse [the scaladoc](<http://www.scala-lang.org/docu/files/api/index.html>), or alternatively just play around in the Scala interpreter. Have fun!