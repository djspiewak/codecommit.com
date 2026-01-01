{%
laika.title = "Better Utilizing JRuby in Project Scripting"
laika.metadata.date = "2007-08-21"
%}


# Better Utilizing JRuby in Project Scripting

Often times in a large-scale Java project, I find myself in need of performing small, discrete tasks with the existing project infrastructure.   The standard way of doing this has always been to add a main(String[]) method to a class or to add a separate driver class to perform the one operation.  This is a bit messy though, since it requires polluting your nice clean project with random utility classes.  A better way to do this would be if we could have a set of separate scripts which just perform the actions we need.  After all, that's what _scripting_ languages are best at, right?  Enter [JRuby](<http://www.jruby.org>).

JRuby is really designed for this sort of thing.  Interoperating with Java is second nature to it, and easily performing small, discrete tasks is a perfect use for Ruby's compact and intuitive syntax.  The one problem is JRuby makes it pretty hard to get at a project's class library.

For example, I have a medium-sized web application project based on [Wicket](<http://wicket.apache.org>) and [ActiveObjects](<https://activeobjects.dev.java.net>).  This project has not only its own sources (and hence compiled binaries) as part of its classpath, but it also relies heavily upon almost two dozen other JARs (contained within the WebContent/WEB-INF/lib directory).  Any sort of utility scripting would have to somehow gain access to all of these classes, otherwise its capabilities would be extremely limited.

Now the task I need to perform with this scripting (at least, the task which precipitated this effort) is to optimize the Lucene search index.  This index is managed through ActiveObject's automagical indexing of entity values.  The problem is that the index will grow rather organically, eventually becoming extremely slow.  I can't just cause the application to stop at some arbitrary point and optimize the index since that would mean blocking a page load somewhere along the line.  As such, this is a perfect candidate for an external utility script.

### Examining the Problem

So it really would be nice if we could just fire a JRuby script within the project root and expect it to be able to grab all of the project's required classes.  However, there's no way JRuby can do this for us.  What we need to do is actually manage a classloader of our own which searches through three separate locations relevant to the project:

  * Project compiled binaries (build/classes/) 
  * ActiveObjects project compiled binaries (../ActiveObjects/bin/) 
  * Project JAR dependencies (WebContent/WEB-INF/lib/*.jar)

While JRuby doesn't provide any handy mechanism to do this (nor should they), Java does.  We can use a URLClassLoader instance from within JRuby and associate it with all of the given paths.  Using this URLClassLoader, we can load whatever class we need by String name and then - through the Java reflection API - access whatever methods/constructors are required.

If this sounds complicated, then you're probably paying attention.  Classloaders and Java reflection both have notoriously verbose and complicated APIs.  In short, this isn't something with which we want to pollute every single utility script we may need to write.  We need to develop some sort of Ruby API to wrap around all of this complexity.  Hopefully one which will enable us to access project classes in an intuitive (and memorable) manner.

### Designing the Interface

Ruby lends itself extremely well to the development of so-called DSLs (Domain Specific Languages).  While we don't need a full-blown DSL here, it would be really nice if we could have an intuitive syntax which could handle all of the complexity for us.  With that in mind, let's imagine exactly what syntax we really want for our wrapper API:

```ruby
#!/usr/bin/env jruby

require 'java'
require 'java_classes'

Utilities = get_class 'com.myproject.app.Utilities'
Utilities.createManager.optimize
```

In my project, the _com.myproject.app.Utilties_ class contains a static method _createManager()_ which returns an instance of _IndexingEntityManager._   This ActiveObjects class in turn contains an _optimize()_ instance method which calls the appropriate Lucene functions to optimize the index.  As you can see, our goal is to use the Utilities class within our script exactly as if it were a standard Ruby class (with the exception of the "dot" syntax as opposed to the double semi-colon for the class method).  Thankfully, this is an achievable goal.

If we write the _get_class_ method to return a Ruby wrapper class corresponding to our custom-loaded project class, we could conceivably result in a syntax like the above.  Obviously, in the above example we're assigning our wrapper class to the "Utilities" variable, and then treating that variable as if it were a proper class symbol (actually, as far as Ruby's concerned, it may as well be).  So conceptually, that part of the problem is taken care of.

The second aspect to the syntax is actually treating project class static methods (and constructors, since we would potentially want to call "Utilities.new" and get a Utilities instance) as member methods of our wrapper class.  This is actually where much of the complicated will have to go.  Ruby does provide a mechanism for accomplishing this called "method_missing" (this is how ActiveRecord works).  However, we still need to write the logic needed to actually convert parameters, reflectively invoke methods, etc...

### Implementation

So of course the first thing we need to take care of is the initialization of the classloader.  This is just hard work with the URLClassLoader API, so were going to assume we took care of it already.  :-)  For the sake of brevity, we're going to pretend that the CLASSLOADER variable in the _java_classes.rb_ file is an instance of URLClassLoader, properly initialized to hit the project classpath. 

The real interest of this mini API is in the _JClassWrapper_ implementation.  It will be an instance of this Ruby class which is returned from the _get_class_ method.  For the sake of simplicity, we'll make the _method_missing_ method within JClassWrapper to all the work.  Thus, the only things for which the _get_class_ method is responsible are loading the class in question via CLASSLOADER and creating an instance of JClassWrapper to return.  The implementation is shown below:

```ruby
def get_class(name)
    JClassWrapper.new java.lang.Class.forName(name, true, CLASSLOADER)
end
```

The really interesting code is in _method_missing.   _This is where we will handle both static methods and the special _new_ method, which will be passed on to the wrapped Class's constructor.  This is also where we need to worry about auto-converting the method parameters into values which will make sense to the wrapped Java method.  For the sake of simplicity, we're not going to worry about wrapping anything like complex classes.  Instead, we'll just assume that the parameters passed will either be already Java objects, or simple primitives like _String_ or _Fixnum_.

The auto-conversion logic should look something like this (we can add to it as necessary):

```ruby
def method_missing(sym, *args)
    jarg_types = java.lang.Class[args.size].new
    jargs = java.lang.Object[args.size].new
    
    for i in 0..(args.length - 1)
        if args[i].kind_of? String
            args[i] = java.lang.String.new args[i]
        elsif args[i].kind_of? Fixnum
            args[i] = java.lang.Integer.new args[i]
        elsif args[i].kind_of? JClassWrapper
            args[i] = args[i].java_class
        end
    
        jarg_types[i] = args[i].java_class
        jargs[i] = args[i]
    end
    # ...
end
```

As you can see, all this does is create and populate a types and a values array.  There are some basic, hard-coded conversions, and that's about it.  This gives us all we need to pass values to the reflectively discovered static methods or constructor.  In fact, the only really interesting logic left to us is the actual reflective invocations:

```ruby
def method_missing(sym, *args)
    # ...
    if sym == :new
        begin
            constructor = @clazz.getConstructor jarg_types
        rescue
            return super
        end
        
        return constructor.newInstance(jargs)
    elsif sym == :java_class
        return @clazz
    end

    begin
        method = @clazz.getMethod(sym.to_s, jarg_types)
    rescue
        return super
    end

    return method.invoke(nil, jargs) if defined? method
		
    super
end
```

Here we have some special logic for the _new_ and _java_class_ methods, since we don't want to pass these directly to the wrapped class, but the rest of the logic is surprisingly simple.  Really all we need to do is find the corresponding static method by name and using the types array we populated earlier.  Then, using the _java.lang.reflect.Method_ instance, we invoke the method passing the values array and _nil_ for the instance (since it's a static method).

One of the nice things about this is any Java values returned from these methods will be automatically handled and wrapped by JRuby.  Thus, we can do something like this if we really want to:

```ruby
Person = get_class 'com.myproject.db.Person'
EntityManager = get_class 'net.java.ao.EntityManager'

manager = EntityManager.new(config[:uri], config[:user], config[:pass])
people = manager.find(Person.java_class)

people.each do |p|
    puts "Person: #{p.first_name} #{p.last_name} is #{p.age} years old"
end
```

You'll notice we don't have any special logic at work dealing with the _EntityManager_ instance or the _Person_ array and instances returned from the _find_ method.  Regardless of our fancy ClassLoader tricks and class wrapping, we can still rely upon JRuby's built-in Java integration facilities to take care of most of the heavy lifting.  The full source for the "java_classes.rb" file is available [here](<http://www.codecommit.com/blog/wp-content/uploads/2007/08/java_classes.rb>).  Note, you will have to customize the values a bit depending on your project's classpath.  Enjoy!