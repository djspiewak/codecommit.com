{%
laika.title = "Scala for Java Refugees Part 2: Basic OOP"
laika.metadata.date = "2008-01-14"
%}


# Scala for Java Refugees Part 2: Basic OOP

In the [previous installment](</blog/scala/scala-for-java-refugees-part-1.md>), we looked at the basics of Scala syntax and provided some simple conceptual explanations.  Obviously there's a lot more to this language than what I was able to present in a single (albeit very long) article.  In this post we're going to examine Scala's object oriented constructs (classes, objects, methods, etc) and how they compare to Java.

### A More Complex Example

```scala
package com.codecommit.examples

import java.awt.{Color, Graphics}

abstract class Shape {
  var fillColor:Color = null

  def draw(g:Graphics):Unit
  def area:Double
}

class Circle(var radius:Int) extends Shape {
  def draw(g:Graphics):Unit = {
    if (fillColor == null) {
      g.drawOval(0, 0, radius / 2, radius / 2)
    } else {
      g.setColor(fillColor);
      g.fillOval(0, 0, radius / 2, radius / 2)
    }
  }

  def area:Double = {
    var back = Math.Pi * radius 
    back * radius
  }
}

class Square(var width:Int) extends Shape {
  def draw(g:Graphics):Unit = {
    if (fillColor == null) {
      g.drawRect(0, 0, width, width)
    } else {
      g.setColor(fillColor)
      g.fillRect(0, 0, width, width)
    }
  }

  def area:Double = width * width
}
```

Remember that Scala does not require every public class to be declared in a file of the same name.  In fact, it doesn't even require every class to be declared in a separate file.  Organizationally, it makes sense for all of these trivial classes to be contained within the same file.  With that in mind, we can copy/paste the entire code snippet above into a new file called "shapes.scala".

The first thing you should notice about this snippet is the package declaration.  All of these classes are declared to be within the "com.codecommit.examples" package.  If this were Java, we'd have to create a new directory hierarchy to match (com/codecommit/examples/).  Fortunately, Scala saves us work here as well.  We can just store this file right in the root of our project and compile it in place, no hassle necessary.

With that said, it's still best-practice to separate your packages off into their own directories.  This organization makes things easier to find and eases the burden on you (the developer) in the long run.  And after all, isn't that what we're trying to do by looking at Scala in the first place?

To compile this class, we're going to use the _fsc_ command (short for Fast Scala Compiler).  FSC is one of those brilliant and obvious Scala innovations which allows repeated compilation of Scala files with almost no latency.  FSC almost completely eliminates the compiler startup time incurred by the fact that the Scala compiler runs on the JVM.  It does this by "warm starting" the compiler each time, keeping the process persistent behind the scenes.  Effectively, it's a compiler daemon, sitting in the background and using almost no resources until called upon.  The command syntax is identical to the _scalac_ command:

```bash
fsc -d bin src/com/codecommit/examples/*.scala
```

This command will compile all of the _.scala_ files within the src/com/codecommit/examples/ directory and place the resultant _.class_ files into bin/.  Experienced Java developers will know the value of this convention, especially on a larger project.  Once again, Scala doesn't intend to upset all best-practices and conventions established over decades.  Rather, its purpose is to make it easier to do your job by staying out of the way.

### First Impressions

Of course, compiling an example doesn't do us much good if we don't understand what it means.  Starting from the top, we declare the package for all of the classes within the same file.  Immediately following is a single statement which imports the _java.awt.Color_ and _java.awt.Graphics_ classes.  Notice Scala's powerful import syntax which allows for greater control over individual imports.  If we wanted to import the entire _java.awt_ package, the statement would look like this:

```scala
import java.awt._
```

In Scala, the _ character is a wildcard.  In the case of an import it means precisely the same thing as the * character in Java:

```java
import java.awt.*;
```

Scala is more consistent than Java however in that the _ character also serves as a wildcard in other areas, such as type parameters and pattern matching.  But I digress...

Looking further into the code sample, the first declaration is an abstract class, _Shape._   It's worth noting here that the order of declarations in a file does not hold any significance.  Thus, _Shape_ could just as easily have been declared below _Circle_ and _Rectangle_ without changing the meaning of the code.  This stands in sharp contrast to Ruby's syntax, which can lead to odd scenarios such as errors due to classes referencing other classes which haven't been declared yet.  Order is also insignificant for method declarations.  As in Java, a method can call to another method even if it is declared above the delegate.

```scala
class Person {
  def name() = firstName() + ' ' + lastName()

  def firstName() = "Daniel"
  def lastName() = "Spiewak"
}
```

### Properties

Returning to our primary example, the first thing we see when we look at class _Shape_ is the _color_ variable.  This is a public variable (remember, Scala elements are public by default) of type _Color_ with a default value of _null_.  Now if you're a Java developer with any experience at all, warning sirens are probably clanging like mad at the sight of a public variable.  In Java (as in other object-oriented languages), best practice says to make all fields private and provide accessors and mutators.  This is to promote encapsulation, a concept critical to object oriented design.

Scala supports such encapsulation as well, but its syntax is considerably less verbose than Java's.  Effectively, all public variables become instance properties.  You can imagine it as if there were mutator and accessor methods being auto-generated for the variable.  It's _as if_ these two Java snippets were equivalent (the analog is not precise, it just gives the rough idea):

```java
public class Person {
    public String name;
}
```

```java
public class Person {
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
```

It's not _quite_ like that, but you get the point of the example.  What's really happening is we're taking advantage of the fact that in Scala, variables are actually functions.  It's a bit of an odd concept to wrap your head around coming from an imperative background, so it's probably easier just to keep thinking of variables as variables.

Let's imagine that we've got our _Shape_ class and its public variable _fillColor_.  Down the road we decide that we need to add a check to the mutator to ensure that the color is never red (why, I'm really not sure, but we do).  In Java, this would be impossible because the variable is public and forcing the use of a mutator would change the public class signature.  Thankfully, Scala allows us to easily rewrite that portion of the class without affecting the class signature or any code which actually uses the class:

```scala
abstract class Shape {
  private var theFillColor:Color = null

  def fillColor = theFillColor
  
  def fillColor_=(fillColor:Color) = {
    if (!fillColor.equals(Color.RED)) {
      theFillColor = fillColor
    } else {
      throw new IllegalArgumentException("Color cannot be red")
    }
  }
}

var shape = ...
shape.fillColor = Color.BLUE
var color = shape.fillColor

shape.fillColor = Color.RED  // throws IllegalArgumentException
```

As you can see, the _= method suffix is a bit of magic syntax which allows us to redefine the assignment operator (effectively) for a variable.  Notice from the perspective of the code using _Shape_ , it still looks like a public field.  The code snippet using _Shape_ will work with both the previous and the modified versions of the class.  Think of it, no more _get*_ and _set*_...  Now you can use public fields with impunity and without fear of design reprisal down the road.

Just to ensure this point is really clear, here's the analog of the syntax in Ruby:

```ruby
class Shape
  def initialize
    @fill_color = nil
  end

  def fill_color
    @fill_color
  end

  def fill_color=(color)
    raise "Color cannot be red" if color == Color::RED

    @fill_color = color
  end
end
```

This obvious difference being that the Scala example will be type-checked by the compiler, while the Ruby code will not.  This is one of the many areas where Scala demonstrates all the flexibility of Ruby's syntax coupled with the power and security of Java.

### Abstract Methods

One of the important features of the _Shape_ class in the example is that it's declared to be abstract.  Abstract classes are very important in object oriented design, and Scala wouldn't be much of an object oriented language if it didn't support them.  However, looking through the definition of the class, there do not _seem_ to be any abstract methods declared.  Of course, as in Java this is perfectly legal (declaring an abstract class without abstract members), it just doesn't seem all that useful.

Actually, there are two abstract methods in the _Shape_ class.  You'll notice that neither the _draw_ nor the _area_ methods are actually defined (there is no method body).  Scala detects this and implicitly makes the method abstract.  This is very similar to the C++ syntax for declaring abstract methods:

```cpp
class Shape {
public:
    virtual void draw(Graphics *g2) = 0;
    virtual double area() const = 0;

    Color *fillColor;
};
```

Thankfully, this is the end of Scala's similarity to C++ in the area of abstract types.  In C++, if a class inherits from an abstract base class and fails to implement all of the inherited abstract methods, the derived class implicitly becomes abstract.  If such a situation occurs in Scala, a compiler error will be raised informing the developer (as in Java).  The only exception to this rule are case classes, which are weird enough to begin with and will require further explanation down the road.

### Constructors

Despite the best efforts of Josh Bloch, the constructor lives on in the modern object-oriented architecture.  I personally can't imagine writing a non-trivial class which lacked this virtually essential element.  Now, it may not have been immediately obvious, but all of the Scala classes shown in this post have a constructor.  In fact, _every_ class (and object) has at least one constructor.  Not just a compiler-generated default mind you, but an actually declared in-your-code constructor.  Scala constructors are a bit different than those in Java, which are really just special methods named after the enclosing class:

```java
public class Person {
    public Person(int age) {
        if (age > 21) {
            System.out.println("Over drinking age in the US");
        } 
    }

    public void dance() { ... }
}

Person p = new Person(26);   // prints notice that person is overage
```

In Scala, things are done a bit differently.  The constructor isn't a special method syntactically, _it is the body_ of the class itself.  The above sample is equivalent to the following bit of Scala code:

```scala
class Person(age:Int) {
  if (age > 21) {
    println("Over drinking age in the US")
  }

  def dance() = { ... }
}

var p = new Person(26)   // prints notice that person is over-age
```

Remember the first Scala example I listed in this series?  (the three line HelloWorld)  In this code, the body of the "main method" is implemented as a constructor in the _HelloWorld_ object.  Thusly:

```scala
object HelloWorld extends Application {
  println("Hello, World!")
}
```

The _println_ statement isn't contained within a main method of any sort, it's actually part of the constructor for the _HelloWorld_ object (more on objects in a later post).  Any statement declared at the class level is considered to be part of the default constructor.  The exceptions to this are method declarations, which are part of the class itself.  This is to allow forward-referencing method calls:

```scala
class ChiefCallingMethod {
  private var i = 1

  if (i < 5) {
    lessFive(i)
  }

  def lessFive(value:Int) = println(value + " is less than 5")
}

var chief = new ChiefCallingMethod()
```

This code will run and successfully determine that 1 is indeed less than 5, once again proving the trustworthiness of mathematics.

This constructor syntax seems very strange to those of us accustomed to Java.  If you're like me, the first thought which comes into your head is: how do I overload constructors?  Well, Scala does allow this, with a few caveats:

```scala
class Person(age:Int) {
  if (age > 21) {
    println("Over drinking age in the US")
  }

  def this() = {
    this(18)
    println("Created an 18 year old by default")
  }

  def dance() = { ... }
}
```

This revised _Person_ now has two constructors, one which takes an _Int_ and one which takes no parameters at all.  The major caveat here is that all overloaded constructors must delegate to the default constructor (the one declared as part of the class name).  Thus it is not possible to overload a constructor to perform entirely different operations than the default constructor.  This makes sense from a design standpoint (when was the last time you declared such constructors anyway?) but it's still a little constraining.

### Constructor Properties

So you understand Scala constructors and you're getting the feel for public instance properties, now it's time to merge the two concepts in constructor properties.  What are constructor properties?  The answer has to do with a (seemingly) odd bit of Scala syntax trivia, which says that all constructor parameters become private instance vals (constants).  Thus you can access constructor parameter values from within methods:

```scala
class Person(age:Int) {
  def isOverAge = age > 21
}
```

This code compiles and runs exactly as expected.  It's important to remember that _age_ isn't really a variable (it's a value) and so it cannot be reassigned anywhere in the class.  However, it might make sense to change a person's age.  After all, people do (unfortunately) grow older.  Supporting this is surprisingly easy:

```scala
class Person(var age:Int) {     // notice the "var" modifier
  def isOverAge = age > 21

  def grow() = {
    age += 1
  }
}
```

In this code, _age_ is no longer just a private value, it is now a public variable (just like _fillColor_ in _Shape_ ).  You can see how this can be an extremely powerful bit of syntax when dealing with simple bean classes:

```scala
class Complex(var a:Int, var b:Int)

// ...
val number = new Complex(1, 0)
number.b = 12
```

Omission of the curly braces is valid syntax in Scala for classes which do not require a body.  This is similar to how Java allows the omission of braces for single line _if_ statements and so on.  The roughly equivalent Java code would be as follows:

```java
public class Complex {
    private int a, b;

    public Complex(int a, int b) {
        this.a = a;
        this.b = b;
    }

    public int getA() {
        return a;
    }

    public void setA(int a) {
        this.a = a;
    }

    public int getB() {
        return b;
    }

    public void setB(int b) {
        this.b = b;
    }
}

// ...
final Complex number = new Complex(1, 0);
number.setB(12);
```

Much more verbose, much less intuitive, and far less maintainable.  With the Scala example, adding another property is as simple as adding a parameter to the constructor.  In Java, we have to add a private field, add the getter, the setter and then _finally_ add the extra parameter to the constructor with the corresponding code to intialize the field value.  Starting to see how much code this could save you?

In our code sample way up at the top of the page, _Circle_ declares a property, _radius_ (which is a variable, and so subject to change by code which uses the class).  Likewise _Square_ has a property, _width._   These are constructor properties, and they're about the most cruft-saving device in the entire Scala language.  It's really amazing just how useful these things are.

### Conclusion

We've really just scratched the surface of all that Scala is able to accomplish in the object-oriented arena.  The power of its constructs and the elegance of its terse syntax allows for far greater productivity, especially when dealing with a larger project.  Moreover, Scala's object oriented capabilities prove that it's not just an interesting functional language for pasty academics but a powerful, expressive and _practical_ language well suited to almost any real-world application.

Coming up: [more on methods](<http://www.codecommit.com/blog/scala/scala-for-java-refugees-part-3-methods-and-statics>) (including overriding) and a rundown on static members.