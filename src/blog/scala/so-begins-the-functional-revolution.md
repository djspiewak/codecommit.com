{%
laika.title = "So Begins the Functional Revolution"
laika.metadata.date = "2008-05-19"
%}


# So Begins the Functional Revolution

When I started learning Scala, I was convinced that its designers were positively the worst marketers I had ever seen.  The official project page was (and is) peppered with Scala examples of things like quicksort, factoring, prime number sieves and so on.  All of these examples were written in such a way as to be virtually incomprehensible to the average OOP developer.  In fact, they all had a very simple common denominator which really set me off: they were written with a very functional flair.

Functional programming is an ancient and venerable tradition, dating back all the way to the days of Lisp and APL.  In its purest form ( _ahem:_ Haskell), functional programming is the absence of side-effects.  Everything in the language is some sort of declarative expression, taking some values and returning a result.  This sweeping restricting has some fairly profound consequences.  Consider the following ML function:

```ml
fun search nil _ = false
  | search (hd::tail) x = (hd = x) orelse (search tail x)
```

For most developers sporting an imperative background, this is probably fairly difficult to read.  If you actually boil it down, all it does is walk through a list, returning `true` if it finds a certain element (`x`), otherwise `false`.  This implementation is a far cry from how we would do it in an imperative language:

```java
public <T> boolean search(List<T> list, T element) {
    for (T hd : list) {
        if (element.equals(hd)) {
            return true;
        }
    }

    return false;
}
```

Arguably, this is harder to read, but it _is_ much more familiar to most people.  Both functions do exactly the same thing, but one of them relies upon mutable state and the side effects imposed by iterators, while the other is completely declarative (in the mathematical sense).  The one critical thing to notice is that the Java version is less constrained.  In ML, you can only have one expression per function, and you certainly can't have anything mutable floating around.  By contrast, Java offers a far greater sense of freedom in what you can do.  Want to modify an instance field?  Go right ahead; there's nothing stopping you!  I believe that it is for this reason that imperative languages like C/C++, Java, Ruby and such have really become the dominant force in the industry.

Getting back to my original point though...  I have to admit that I used to be a firm believer in the One True (imperative) way of doing things.  The OOP mothership had landed and I was 100% convinced that it was here to stay.  However, after spending some time getting to know Scala, I'm beginning to sway more to the "academic" way of thinking: functional languages are pretty nice.  Consider the following Scala algorithm which takes a series of integers as its arguments and prints their sum:

```scala
object Main {
  def main(args: Array[String]) {
    println(args.map(_.toInt).foldLeft(0)(_ + _))
  }
}
```

Compare that to the equivalent Java code:

```java
public class Main {
    public static void main(String[] args) {
        int sum = 0;
        for (String arg : args) {
            sum += Integer.parseInt(arg);
        }

        System.out.println(sum);
    }
}
```

Scala's naturally concise syntax aside, the use of functional concepts _definitely_ contributes to a more expressive solution.  Once you understand what the `map` and `foldLeft` methods accomplish, this code becomes startlingly readable.  What's more, because this code is simpler and more expressive, the potential for subtle bugs and maintenance problems because virtually non-existent.  If something goes wrong in our Scala example and somehow the type checker doesn't catch it, the problem will still be fairly easy to fix because of how straightforward and consistent the code is.

Contrast this with the Java solution.  It's easy to imagine subtle errors slipping into the implementation.  Even something as simple as a typo can be difficult to track down.  Consider:

```java
public class Main {
    public static void main(String[] args) {
        int sum = 0;
        for (String arg : args) {
            sum = Integer.parseInt(arg);
        }

        System.out.println(sum);
    }
}
```

Can you spot the error?  I'll give you a hint, given the input "`1 2 3 4 5`", the correct answer is `15`.  Our revised Java solution prints "`5`".

I can't even figure out how to trivially break the Scala solution so that it does something bad.  This sort of stability happens time and time again with functional solutions.  You write the code, it's expressive and elegant, and somehow it manages to do everything you wanted without fuss. 

This isn't even limited to simple examples.  Somehow, this effect holds even in larger, more complicated systems.  I recently built a modest application in Scala using it's trademark hyrbrid-functional style.  It's hard to estimate of course, but judging by my experiences with similar projects in Java, I saved a great deal of time and effort just due to Scala's functional expressiveness.

I doubt that the industry is going to change overnight, but it's hard to deny the benefits of the FP approach.  Functional languages are on the rise; Scala is only the tip of the iceberg.  Languages like Erlang and F# are also gaining popularity as developers begin to recognize how expressive they can be.  It may take some time, but I predict that within a decade or so, the dominant paradigm in the industry will be functional, or more likely some hybrid thereof.  Welcome the revolution!