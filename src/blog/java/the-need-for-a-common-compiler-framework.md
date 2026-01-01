{%
laika.title = "The Need for a Common Compiler Framework"
laika.metadata.date = "2008-06-23"
%}


# The Need for a Common Compiler Framework

In recent years, we have seen a dramatic rise in the number of languages used in mainstream projects.  In particular, languages which run on the JVM or CLR have become quite popular (probably because sane people hate dealing with x86 assembly).  Naturally, such languages prefer to interoperate with other languages built on these core platforms, particularly Java and C# (respectively).  Collectively, years of effort have been put into devising and implementing better ways of working with libraries written in these "parent languages".  The problem is that such efforts are crippled by one fundamental limitation: circular dependencies.

Let's take Scala as an example.  Of all of the JVM languages, this one probably has the potential for the tightest integration with Java.  Even Groovy, which is renowned for its integration, still falls short in many key areas.  (generics, anyone?)  With Scala, every class is a Java class, every method is a Java method, and there is no API which cannot be accessed from Java as natively as any other.  For example, I can write a simple linked list implementation in Scala and then use it in Java without any fuss whatsoever ( **warning:** untested sample):

```scala
class LinkedList[T] {
  private var root: Node = _
  
  def add(data: T) = {
    val insert = Node(data, null)
    
    if (root == null) {
      root = insert
    } else {
      root.next = insert
    }
    
    this
  }
  
  def get(index: Int) = {
    def walk(node: Node, current: Int): T = {
      if (node == null) {
        throw new IndexOutOfBoundsException(index.toString)
      }
      
      if (current < index) {
        walk(node.next, current + 1)
      } else {
        node.data
      }
    }
    
    if (index < 0) {
      throw new IndexOutOfBoundsException(index.toString)
    }
    
    walk(root, 0)
  }
  
  def size = {
    def walk(node: Node): Int = if (node == null) 0 else 1 + walk(node.next)
    
    walk(root)
  }
  
  private case class Node(data: T, var next: Node)
}
```

Once this class is compiled, we can use it in our Java code just as if it were written within the language itself:

```java
public class Driver {
    public static void main(String[] args) {
        LinkedList<String> list = new LinkedList<String>();
        
        for (String arg : args) {
            list.add(arg);
        }
        
        System.out.println("List has size: " + list.size());
        
        for (int i = 0; i < list.size(); i++) {
            System.out.println(list.get(i).trim());
        }
    }
}
```

Impressively seamless interoperability!  We actually could have gotten really fancy and thrown in some operator overloading.  Obviously, Java wouldn't have been able to use the operators themselves, but it still would have been able to call them just like normal Java instance methods.  Using Scala in this way, we can get all the advantages of its concise syntax and slick design without really abandoning our Java code base.

The problem comes in when we try to satisfy more complex cases.  Groovy proponents often trot out the example of a Java class inherited by a Groovy class which is in turn inherited by another Java class.  In Scala, that would be doing something like this:

```java
public class Shape {
    public abstract void draw(Canvas c);
}
```

```scala
class Rectangle(val width: Int, val height: Int) extends Shape {
  override def draw(c: Canvas) {
    // ...
  }
}
```

```java
public class Square extends Rectangle {
    public Square(int size) {
        super(size, size);
    }
}
```

Unfortunately, this isn't exactly possible in Scala.  Well, I take that back.  We can cheat a bit and first compile `Shape` using javac, then compile `Rectangle` using scalac and finally `Square` using javac, but that would be quite nasty indeed.  What's worse is such a technique would completely fall over if the `Canvas` class were to have a dependency on `Rectangle`, something which isn't too hard to imagine.  In short, Scala is bound by the limitations of a separate compiler, as are most languages on the JVM.

Groovy solves this problem by building their own Java compiler into groovyc, thus allowing the compilation of both Java and Groovy sources within the same process.  This solves the problem of circular references because neither set of sources is completely compiled before the other.  It's a nice solution, and one which Scala will be adopting in an upcoming release of its compiler.  However, it doesn't really solve everything.

Consider a more complex scenario.  Imagine we have Java class `Shape`, which is extended by Scala class `Rectangle` _and_ Groovy class `Circle`.  Imagine also that class `Canvas` has a dependency on both `Rectangle` and `Circle`, perhaps for some special graphics optimizations.  Suddenly we have a three-way circular dependency and no way of resolving it without a compiler which can handle _all three_ languages: Java, Groovy and Scala.  This is starting to become a bit more interesting.

Of course, we can solve this problem in the same way we solved the Groovy-Java dependence problem: just add support to the compiler!  Unfortunately, it may have been trivial to implement a Java compiler as part of groovyc, but Scala is a much more difficult language from a compiler's point of view.  But even supposing that we do create an integrated Scala compiler, we still haven't solved the problem.  It's not difficult to imagine throwing another language into the mix; Clojure, for example.  Do we keep going, tacking languages onto our once-Groovy compiler until we support everything usable on the JVM?  It should be obvious why this is a bad plan.

A more viable solution would be to create a common compiler framework, one which would be used as the basis for all JVM languages.  This framework would have common abstractions for things like name resolution and type checking.  Instead of creating an entire compiler from scratch, every language would simply extend this core framework and implement their own language as some sort of module.  In this way, it would be easy to build up a custom set of modules which solve the needs of your project.  Since the compilers are modular and based on the same core framework, they would be able to handle simultaneous compilation of all JVM languages involved, effectively solving the circular dependency problem in a generalized fashion.

The framework could even make things easier on would-be compiler implementors by handling common operations like bytecode emission.  Fundamentally, all of these tightly-integrated languages are just different front-ends to a common backend: the JVM.  I haven't looked at the sources, but I would imagine that there is a _lot_ of work which had to be done in each compiler to solve problems which were already handled in another.

Of course, all this is purely speculative.  Everyone builds their compiler in a slightly different way (slightly => radically in the case of languages like Scala) and I wouldn't imagine that it would be easy to build this sort of common compiler backend.  However, the technology is in place.  We already have nice module systems like OSGi, and we're certainly no strangers to the work involved in building up a proper CLASSPATH for a given project.  Why should this be any different?

It's not without precedent either.  GCC defines a common backend for a number of compilers, such as G++, GCJ and even an Objective-C compiler.  Granted, it's neither as high-level nor as modular as we would need to solve circular dependencies, but it's something to go on.

It will be interesting to see where the JVM language sphere is headed next.  The rapid emergence of so many new languages is leading to problems which will have to be addressed before the polyglot methodology will be truly accepted by the industry.  Some of the smartest people in the development community are working toward solutions; and whether they take my idea of a modular framework or not, somewhere along the line the problem of simultaneous compilation must be solved.