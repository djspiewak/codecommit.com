{%
laika.title = "The Benefits of Type Inference"
laika.metadata.date = "2008-01-16"
%}


# The Benefits of Type Inference

There's been some [complaining](<http://creativekarma.com/ee.php/weblog/comments/my_verdict_on_the_scala_language/>) around the blogosphere about the use of Scala's type inference mechanism, how it makes it difficult to know what type you're dealing with since the only explicit declaration of type could be several steps up the call stack.  This is true.  What is also true (and really more important) is that Scala almost completely eliminates the _need_ to worry about what type you're dealing with; and it does this without sacrificing the all-important compile-time type checking.

Developers with experience in dynamic languages such as Ruby or Python will know what I mean when I say that there comes a time when you stop worrying about _what_ type a value is and concern yourself more with _how_ you can use it.  For example, when I code in Ruby, I often write code like this:

```ruby
def operate(v)
  if v < "daniel"
    v += "spiewak"
    return value[0..(v.size - 7)]
  end

  v
end
```

When I wrote this method, I wasn't necessarily thinking about _v_ as a _String_ , but rather as some value which defined the less-than operator against a _String_ , the + operator again taking a _String_ and the square brackets operator taking a _Range.   _I also never documented anywhere that this method expects a _String_.  I probably could document it that way, and many people would do so.  But really, all that needs to be documented is that the value passed to the method defines those three functions.

Scala enables the same sort of "duck typing" but adds static checking.  It has constructs which enable values to be typed based on defined functions and such, but I'm not referring to these.  The feature of Scala which _really_ enables one to almost forget about type is the static type inference.  Without it, not only would the language be far more verbose, but also every algorithm, every function would have to pedantically spell out precisely the type expected in every situation.  This sort of restriction makes algorithms far more rigid and far more difficult to manage in the long run.  This is the reason interfaces are used for everything in Java, to satisfy the compiler just enough to allow an arbitrary type to be passed under the surface.

Think about it.  How many times have you written code to use the _java.util.Map_ interface rather than _HashMap_ directly to avoid strongly coupling yourself to one _Map_ implementation?  In Java, one usually writes code like this:

```java
import java.util.Map;
import java.util.HashMap;

public class MapTest {
    private Map<Integer, String> myMap;    // using the interface type

    public MapTest() {
        myMap = createMap();
    }

    public Map<Integer, String> createMap() {
        return new HashMap<Integer, String>();
    }
}
```

Granted, needlessly verbose, but not too far off the mark when it comes to Java design patterns.  This code makes it fairly trivial to change the underlying map implementation used throughout the class.  The implementation can even be changed by a subclass if it overrides the _createMap()_ method.

In this code, we're specifying semantically what sort of operations we need.  We're being forced to tell the compiler that we want a map, but we're not particular about what sort of map we get.  In short, we're typing _myMap_ less strongly than if we used the _HashMap_ type directly.  This sort of "loose typing" is good, because we don't want to rely on a particular implementation.  Unfortunately, the Java way of implementing this typing has given rise to the ubiquitous use of what I call "the I pattern".  Just look at the Eclipse RCP hierarchy or virtually any of the classes in the Wicket API.  Almost all of them are typed against their almost-pointless interface definition; [IModel](<http://people.apache.org/~tobrien/wicket/apidocs/org/apache/wicket/model/IModel.html>) for example.

Scala avoids this problem with type inference.  Type inference is inherently "loose typing" because it's constraining the type based on the value which defines it.  Translating the Java sample into Scala yields the following:

```scala
import scala.collection.mutable.HashMap

class MapTest {
  var myMap = createMap()

  def createMap() = new HashMap[Int, String]
}
```

The _myMap_ variable is actually of type _HashMap,_ and the compiler knows and enforces this.  However, we can change the return value from _createMap()_ and it will be immediately reflected in the type of _myMap_.  Thus there is no need to type the variable against its superinterface, since the flexibility we want is already available through type inference.

The only thing we do need to worry about here is not using any methods which are specific to _HashMap._   Once we do that, we'll have to actually change our code in more than one place if we want to change the map implementation.  However, when's the last time you used any function that was specific to a collection implementation?

The point of the original article is actually that type inference makes it harder to see what type your variable (or even your method) is going to be, since the only explicit declaration of type is tucked away out of sight.  This is true.  In the above example, to know what type _myMap_ is, we need to remember that _createMap()_ actually returns a _HashMap[Int, String]_ value.  The point I'm trying to make though is that worrying about precisely what type each variable may be is the wrong approach.  All you need to know is _what_ they can do, and this is something which is easy enough to handle.

So does type inference make you do more work than necessary: yes, but only if you're so rigidly dogmatic in your mind-set that you can't accept the fact that it doesn't matter.  To those of you who have an aversion to this sort of pattern: deal with it (you'll thank me later).