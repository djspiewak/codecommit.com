{%
laika.title = "Case Classes Are Cool"
laika.metadata.date = "2008-08-11"
%}


# Case Classes Are Cool

Of all of Scala's many features, this one has probably taken the most flack over the past year or so. Not immutable data structures or even structural types, but rather a minor variation on a standard object-oriented construct. This is more than a little surprising, especially considering how much work they can save when properly employed.

### Quick Primer

Before we get into _why_ they're so nice, we should probably look at _what_ they are and how to use them. Syntactically, case classes are standard classes with a special modifier: `case`. This modifier signals the compiler to assume certain things about the class and to define certain boiler-plate based on those assumptions. Specifically:

  * Constructor parameters become public "fields" (Scala-style, which means that they really just have an associated accessor/mutator method pair) 
  * Methods `toString()`, `equals()` and `hashCode()` are defined based on the constructor fields 
  * A companion object containing: 
    * An `apply()` constructor based on the class constructor 
    * An extractor based on constructor fields 

What this means is that we can write code like the following:

```scala
case class Person(firstName: String, lastName: String)

val me = Person("Daniel", "Spiewak")
val first = me.firstName
val last = me.lastName

if (me == Person(first, last)) {
  println("Found myself!")
  println(me)
}
```

The output of the above is as follows:

```
Found myself!
Person(Daniel,Spiewak)
```

Notice that we're glossing over the issue of pattern matching and extractors for the moment. To the regular-Joe object-oriented developer, the really interesting bits are the `equals()` method and the automatic conversion of the constructor parameters into fields. Considering how many times I have built "Java Bean" classes solely for the purpose of wrapping data up in a nice neat package, it is easy to see where this sort of syntax sugar could be useful.

However, the above does deserve some qualification: the compiler hasn't actually generated both the accessors _and_ the mutators for the constructor fields, only the accessors. This comes back to Scala's convention of "immutability first". As we all know, Scala is more than capable of expressing standard imperative idioms with all of their mutable gore, but it tries to encourage the use of a more functional style. In a sense, case classes are really more of a counterpart to type constructors in languages like ML or Haskell than they are to Java Beans. Nevertheless, it is still possible to make use of the syntax sugar provided by case classes without giving up mutability:

```scala
case class Person(var firstName: String, var lastName: String)

val me = Person("Daniel", "Spiewak")
me.firstName = "Christopher"   // call to a mutator
```

By prefixing each constructor field with the `var` keyword, we are effectively instructing the compiler to generate a mutator as well as an accessor method. It does require a bit more syntactic bulk than the immutable default, but it also provides more flexibility. Note that we may also use this `var`-prefixed parameter syntax on standard classes to define constructor fields, but the compiler will only auto-generate an `equals()` (as well as `hashCode()` and `toString()`) method on a case class.

### Why Are They Useful?

All of this sounds quite nice, so why are case classes so overly-maligned? [Cedric Beust](<http://beust.com/weblog/>), the creator of the TestNG framework, even went so far as to [call case classes](<http://beust.com/weblog/archives/000490.html>) "...a failed experiment".

> From my understanding of Scala's history, case classes were added in an attempt to support [pattern matching](<http://en.wikipedia.org/wiki/Pattern_matching>), but after thinking about the consequences of the points I just gave, it's hard for me to see case classes as anything but a failure. Not only do they fail to capture the powerful pattern matching mechanisms that Prolog and Haskell have made popular, but they are actually a step backward from an OO standpoint, something that I know Martin \[Odersky\] feels very strongly about and that is a full part of Scala's mission statement.

Well, he's right...at least as far as the pattern matching bit is involved. Case classes are almost essential for useful pattern matching. I say "almost" because it is possible to have pattern matching in Scala without ever using a single case class, thanks to the powerful extractors mechanism. Case classes just provide some nice, auto-generated magic to speed things along, as well as allowing the compiler to do a bit more checking than would be otherwise possible.

The point that I think Cedric (and others) have missed entirely is that case classes are far more than just a means to get at pattern matching. Even the most stringent object-oriented developer has to admit that a slick syntax for declaring a data container (like a bean) would be a nice thing to have. What's more, Scala's automatic generation of a companion object for every case class lends itself very nicely to some convenient abstractions. Consider a scenario I ran into a few months back:

```scala
class MainWindow(parent: Shell) extends Composite(parent, SWT.NONE) {
  private lazy val display = parent.getDisplay
  
  private val panels = Map("Foreground" -> ForegroundPanel, 
                           "Background" -> BackgroundPanel, 
                           "Font" -> FontPanel)
  
  setLayout(new FillLayout())
  
  val folder = new TabFolder(this, SWT.BORDER)
  for ((text, make) <- panels) {
    val item = new TabItem(folder, SWT.NONE)
    val panel = make(folder)

    item.setText(text)
    item.setControl(panel)
  }

  def this() = this(new Shell(new Display()))

  def open() {
    parent.open()
    layout()

    while (!parent.isDisposed) {
      if (!display.readAndDispatch()) {
        display.sleep()
      }
    }
  }
}

case class ForegroundPanel(parent: Composite) extends Composite(parent, SWT.NONE) {
  ...
}

case class BackgroundPanel(parent: Composite) extends Composite(parent, SWT.NONE) {
  ...
}

case class FontPanel(parent: Composite) extends Composite(parent, SWT.NONE) {
  ...
}
```

If you ignore the SWT boiler-plate, the really interesting bits here are the `Map` of `panels` and the initialization loop for the `TabItem`(s). In essence, I am making use of a cute little trick with the companion objects of each of the panel case classes. These objects are automatically generated by the compiler extending function type: `(Composite)=>` _ForegroundPanel_ , where _ForegroundPanel_ is replaced by the case class in question. Because each of these classes extends `Composite`, the inferred type of `panels` will be: `Map[String, (Composite)=>Composite]`. _(actually, I'm cheating a bit and not giving the_ precise _inference, only its effective equivalent)_

This definition allows the iteration over the elements of `panels`, generating a new instance by using the value element as a function taking a `Composite` and returning a new `Composite` instance: the desired child panel. It's all statically typed without giving up either the convenience of a natural configuration syntax (in the `panels` declaration) or the familiarity of a class definition for each panel. This sort of thing would certainly be possible without case classes, but more work would be required on my part to properly declare each companion object by hand.

### Conclusion

I think the reason that a lot of staid object-oriented developers tend to frown on case classes is their close connection to pattern matching, a more powerful relative of the much-despised `switch`/`case` mechanism. What these developers fail to realize is that case classes are really much more than that, freeing us from the boiler-plate tyranny of endless getter/setter declarations and the manual labor of proper `equals()` and `toString()` methods. Case classes are the object-oriented developer's best friend, just no one seems to realize it yet.