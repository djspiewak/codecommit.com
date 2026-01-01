{%
laika.title = "Defining Scala Design Idioms"
laika.metadata.date = "2008-04-14"
%}


# Defining Scala Design Idioms

With any new language comes a certain amount of uncertainty as to what is "the right way" to do things.  Now I'm not just talking about recursion vs iteration or object-oriented vs straight procedural.  What I'm referring to is the design idioms which govern everything from naming conventions to array deconstruction.

This idioms are highly language specific and can even differ between languages with otherwise similar lexical elements.  Consider the following C++, Java, Ruby and Scala examples:

```cpp
vector<string> first_names;
first_names.push_back("Daniel");
first_names.push_back("Chris");
first_names.push_back("Joseph");

for (vector<string>::iterator i = first_names.begin(); i != first_names.end(); ++i) {
    cout << *i << endl;
}
```

Java:

```java
String[] firstNames = {"Daniel", "Chris", "Joseph"};

for (String name : firstNames) {
    System.out.println(name);
}
```

Ruby:

```ruby
first_names = ['Daniel', 'Chris', 'Joseph']

first_names.each do |name|
  puts name
end
```

Scala:

```scala
val firstNames = Array("Daniel", "Chris", "Joseph")

for (name <- firstNames) {
  println(name)
}
```

As a matter of interest, the Scala example could also be shortened to the following:

```scala
val firstNames = Array("Daniel", "Chris", "Joseph")
firstNames.foreach(println)
```

All of these samples perform essentially the same task: traverse an array of strings and print each value to stdout.  Of course, the C++ example is actually using a `vector` rather than an array due to the evil nature of C/C++ arrays, but it comes to the same thing.  Passing over the differences in syntax between these four languages, what really stands out are the different _ways_ in which the task is performed.  C++ and Java are both using iterators, while Ruby and Scala are making use of higher order functions.  Ruby and C++ both use lowercase variables separated by underscores, while Java and Scala share the camelCase convention.

This is a bit of a trivial example, but it does open the door to a much more interesting discussion: what are these idioms in Scala's case?  Scala is a very new language which has yet to see truly wide-spread adoption.  More than that, Scala is fundamentally different from what has come before.  Certainly it draws inspiration from many languages - most notably Java, C# and Haskell - but even languages which are direct descendants can impose entirely different idioms.  Just look at the differences between the Java and the C++ examples above.  The practical, day-to-day implications of this become even more apparent when you consider object-oriented constructs:

```cpp
// Book.h
class Book {
    std::string title;
    Author *author;

public:
    Book(string);

    std::string get_title();
    Author* get_author();
    void set_author(Author*);
};

// Book.cpp
Book::Book(string t) : title(t), author(0) {}

string Book::get_title() {
    return title;
}

Author* Book::get_author() {
    return author;
}

void Book::set_author(Author *a) {
    author = a;
}
```

The equivalent in Java:

```java
public class Book {
    private String title;
    private Author author;

    public Book(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public Author getAuthor() {
        return author;
    }

    public void setAuthor(Author author) {
        this.author = author;
    }
}
```

And the (much shorter) Ruby code:

```ruby
class Book
  attr_reader :title, :author
  attr_writer :author

  def initialize(title)
    @title = title
  end
end
```

This code uses standard, accepted idioms for class design in each of the three languages.  Notice how in C++ we avoid name-clashes between method formals and data members?  This is because the compiler tends to get a little confused if we try to combine name shadowing and the initialization syntax (the bit that follows the `:` in the constructor).  This contrasts strongly with Java which by convention _encourages_ shadowing of fields by method formals.  It's a very strong convention to do what I've done here, using `this.` _fieldName_ to disambiguate between formals and fields.  Ruby stands apart from both of these languages by _enforcing_ the use of prefix symbols on variable names to define the container.  There really can't be any shadowing of instance variables by formals since all instance variables must be prefixed with the `@` symbol.

The question here is: what conventions are applicable in Scala?  At first blush, it's tempting to write a class which looks like this:

```scala
class Book(val title:String) {
  var author:Author = null
}
```

Because every variable/value in Scala is actually a method, the accessor/mutator pairs are already generated for us (similar to how it is in Ruby with attributes).  The problem here is of course redefining an accessor or mutator.  For example, we may want to perform a check in the `author` mutator to ensure that the new value is not `null`.  In C++ and Java, we would just add an `if` statement to the correct method and leave it at that.  Ruby is more interesting because of the auto-generated methods, but it's still fairly easy to do:

```ruby
class Book
  def author=(author)
    @author = author unless author.nil?
  end
end
```

Scala poses a different problem.  In our example above, we've essentially created a class with two public fields, something which is sternly frowned upon in most object-oriented languages.  If we had done this in Java, it would be impossible to implement the null check without changing the public interface of the class.  Fortunately Scala is more flexible.  Here's a naive implementation of `Book` in Scala which performs the appropriate check:

```scala
class Book(val title:String) {
  private var author:Author = null

  def author = author

  def author_=(author:Author) {
    if (author != null) {
      this.author = author
    }
  }
}
```

Unfortunately, there are some fairly significant issues with the above code.  First off, it won't compile.  Scala lacks separate namespaces for fields and methods, so you can't just give a method the same name as a field and expect it to work.  This merging of namespaces actually allows a lot of interesting design and is on the whole a good thing, but it puts a serious crimp in our design.  The second problem with this example is closely related to the first.  Assuming the code did compile, at runtime execution would enter the `author_=` method, presumably pass the conditional and then execute the `this.author = author` statement.  However, the Scala compiler will interpret this line in the following way:

```scala
this.author_=(author)
```

That's right, infinite recursion.  Usually this issue isn't apparent because the compiler error will take precedence over the runtime design flaw, but it's still something which merits notice.  Obviously, using the same names for variables as we do for methods and their formals just isn't going to work here.  It may be the convention in Java, but we'll have to devise something new for Scala.

Over the last few months, I've read a lot of Scala written by other people.  Design strategies to solve problems like these range all over the map, from totally separate names to prepending or appending characters, etc.  The community just doesn't seem to be able to standardize on any one "right way".  Personally, I favor the prepended underscore solution and would really like to see it become the convention, but that's just me:

```scala
class Book(val title:String) {
  private var _author:Author = null   // notice the leading underscore

  def author = _author

  def author_=(author:Author) {
    if (author != null) {
      _author = author
    }
  }
}
```

The Scala community really needs to get together on this and other issues related to conventional design idioms.  I see a lot of code that's written in Java, but with C or C++ idioms.  This sort of thing is rare nowadays, but was quite common in the early days of the language.  People weren't sure what worked best in Java, so they tried to apply old techniques to the new syntax, often with disastrously unreadable results.  As Scala moves into the mainstream, we _have to_ come to some sort of consensus as to what our code should look like and what conventions apply.  If we don't, then the language will be forced to struggle through many of the same problems which plagued Java only a decade ago: new developers trying to get a handle on this foreign language, misapplying familiar constructs along the way.