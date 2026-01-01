{%
laika.title = "Integrating Scala into JRuby"
laika.metadata.date = "2008-09-29"
%}


# Integrating Scala into JRuby

More and more projects (especially startups) have been choosing to build their software in multiple languages. Rather than using SQL for the database, XML for the RPC and Java for the _everything_ else, companies have learned that sometimes a different language can serve best in a specific area. Ola Bini [provides some guidance](<http://ola-bini.blogspot.com/2008/01/language-explorations.html>) with regards to methodology in this area in the form of what he calls "language layers". He suggests that an application can be divided logically into separate layers, and for each of these layers there exists a class of language - be it dynamic, static, compiled or otherwise - which can best accomplish the task at hand. All of that aside, there is one problem which is absolutely assured when dealing with polyglot programming: integration between the languages.

I'm told that this issue came up at the JVM languages conference this past week. The problem of integrating very separate languages in a natural way is not an easy one, even when all languages in question are on the JVM. So far, the only solution that anyone has been able to come up with is just to use Java as an intermediary. After all, JRuby integrates with Java, as does Clojure, therefore JRuby has at least _some_ path of integration with Clojure and vice versa.

The problem is that such integration is not really idiomatic. As the title of this post implies, we're going to consider the example of Scala and JRuby. I've already written about how to create a Scala DSL for calling JRuby directly, so let's look at the other side of the problem: it's certainly _possible_ to use Scala classes from within JRuby, but it isn't exactly a pleasant proposition. Let's imagine that I want to make use of [my Scala port](<http://www.codecommit.com/blog/misc/implementing-persistent-vectors-in-scala/final/Vector.scala>) of Rich Hickey's excellent [immutable persistent vector](<http://www.codecommit.com/blog/scala/implementing-persistent-vectors-in-scala>) data structure:

```ruby
import com.codecommit.collection.Vector

vec = Vector.new
vec = vec.send('$plus'.to_sym, 1)
vec = vec.update(0, 2)

puts vec.apply 0       # 2
```

Straightforward, but ugly. Let's break this down a little bit... The import and instantiation are both self-explanatory, so we come to the rather cryptic invocation of `send`. In case you don't know, this is a Ruby method which makes it possible to invoke arbitrary methods on a given object, including those with illegal characters. I happen to know that Scala will compile the `+` operator to a method named `$plus` within class `Vector`. JRuby is perfectly happy to handle and forward this call as necessary, despite the fact that you could never actually declare this method in pure Ruby (`$` has special meaning). Thus, this invocation is the same as the following Scala snippet:

```scala
vec = vec + 1
```

In other words, append `1` to the vector and assign the resulting _new_ vector back into `vec`.

Moving on, we come to the invocations of `update` and `apply`. These should be a bit more comprehensible to those familiar with Scala's intricacies. In short, these methods are how you overload the parentheses and parentheses/assignment operators. Thus, our last two lines correspond as follows:

```scala
vec = (vec(0) = 2)
println(vec(0))
```

This was just a trivial example, you can imagine how a more complicated Scala API could be neigh unusable in JRuby. Intuitively though, it shouldn't have to be this way. After all, JRuby supports many of the same syntactic power as Scala: it has some operator overloading, closures and even more complicated features like mixins. With all of this common ground, surely there is a way to make the two coexist more naturally? I mean, optimally we could have something like this:

```ruby
import com.codecommit.collection.Vector

vec = Vector.new
vec = vec + 1
vec = (vec[0] = 2)       # problematic

puts vec[0]              # 2
```

What we have just done is informally define a desired syntax for a domain-specific problem. Does that sound like an internal DSL to anyone else?

Our goal is to create a simple Ruby API that can be `require`d into any JRuby application to improve the integration with Scala. To do this, we will need to find a way to convert Ruby features into their corresponding Scala features by going through Java. Once we find this translation, we can use meta-programming and dangerously-cool Monkey Patching to augment JRuby's existing Java integration. In this way, we don't have to start our integration layer from scratch, we can just "pretty up" JRuby's existing features, taking advantage of the fact that Scala classes _are_ Java classes.

### Step One: Operators

From our example above, converting the invocation of a `$plus` method into a Ruby `+` operator seems to be the easiest challenge to tackle. Ruby does support operator overloading; unfortunately, this support is incredibly limited when compared with Scala's awesome power. For example, in Scala it is possible to invent arbitrary operators, a feature which is heavily used in the Scala standard library. Ruby has no such capability, operators are implemented using conventional techniques and hard-coded rules in the parser.

In order to avoid blowing this experiment completely out of proportion, we're only going to implement translations for the [set of conventional Ruby operators](<http://phrogz.net/ProgrammingRuby/language.html#table_18.4>). It would be _possible_ to also implement Scala operators which are made by combining existing Ruby operators (e.g. the `++` operator) using techniques developed for the [Superators gem](<http://jicksta.com/posts/superators-add-new-operators-to-ruby>), but to do so would be extremely difficult.

We saw in our original example that the Scala + operator is translated into an invocation of the $plus method. It stands to reason that we could make use of this fact in our translation from Ruby to Scala. The trick is finding a comprehensive list of Scala's operators and what methods they compile to. Fortunately, I had already investigated these translations for a different project:

**Scala Operator** | **Compiles To**  
---|---  
`=` | `$eq`  
`>` | `$greater`  
`<` | `$less`  
`+` | `$plus`  
`-` | `$minus`  
`*` | `$times`  
`/` | `div`  
`!` | `$bang`  
`@` | `$at`  
`#` | `$hash`  
`%` | `$percent`  
`^` | `$up`  
`&` | `$amp`  
`~` | `$tilde`  
`?` | `$qmark`  
`|` | `$bar`  
`\` | `$bslash`  
  
Repeated iterations of an operator become corresponding repetitions of the equivalent character sequence. Thus, `++` becomes `$plus$plus`. This suggests a very natural strategy for converting Ruby operator invocations into Scala: string substitution. We can easily Ruby operator calls using method_missing, substitute the appropriate character sequences and then attempt the modified call against the same object. This idea, when translated into Ruby, is almost as simple as that:

```ruby
OPERATORS = {"=" => "$eq", ">" => "$greater", "<" => "$less", \
        "+" => "$plus", "-" => "$minus", "*" => "$times", "/" => "div", \
        "!" => "$bang", "@" => "$at", "#" => "$hash", "%" => "$percent", \
        "^" => "$up", "&" => "$amp", "~" => "$tilde", "?" => "$qmark", \
        "|" => "$bar", "\\" => "$bslash"}

alias_method :__old_method_missing_in_scala_rb__, :method_missing
def method_missing(sym, *args)
  str = sym.to_s
  
  str = $&[1] + '_=' if str =~ /^(.*[^\]=])=$/
  
  OPERATORS.each do |from, to|
    str.gsub!(from, to)
  end
  
  if methods.include? str
    send(str.to_sym, args)
  else
    __old_method_missing_in_scala_rb__(sym, args)
  end
end
```

Right at the end, after we have transformed the method name to convert any Ruby operators into their Scala equivalents, we actually check to see if the method exists instead of blindly sending the invocation. The reason for this is to avoid creating an infinite loop in cases where a method really doesn't exist. We can rely on the presence of bona fide methods due to the way that JRuby proxies Java objects into Scala.

Astute readers will notice the extra bit of regular expression checking right at the head of the method. We didn't cover this in our target example, but this transformation is none-the-less quite important. Both Scala and Ruby provide mechanisms to simulate assignment to class members through method calls. In Ruby, you just suffix the method name with `=`, whereas in Scala the correct suffix is `_=`. This extra transformation looks for those situations and converts to the appropriate result. Thus, Scala `var` fields are now accessible within JRuby using the same syntax as standard `attr_reader`/`attr_writer` methods in Ruby.

Two other operators which might be useful to enable are the `[]` and `[]=` operators, generally used for collections access. Scala doesn't actually support these operators, reserving square brackets for declaring type parameters. However, it _does_ provide a very similar syntax with parentheses. As a refresher, here is how we might use an array within Scala:

```scala
val arr = new Array(5)
arr(0) = 5
arr(1) = 4
arr(2) = 3
arr(3) = 2
arr(4) = 1

println(arr(1))        // 4
```

At compile-time, this translates into corresponding invocations of the `apply` and `update` methods of class `Array`. Specifically, `apply` is used to retrieve data, while `update` is used to assign it. These are the direct corollaries to Ruby's `[]` and `[]=`. It would only be natural to translate invocations of these operators into corresponding calls to apply and update, and we can do this simply by extending our method_missing just a little bit:

```ruby
# ...
  
  gen_with_args = proc do |name, args|
    code = "#{name}("
    
    unless args.empty?
      for i in 0..(args.size - 2)
        code += "args[#{i}], "
      end
      code += "args[#{args.size - 1}]"
    end
    
    code += ')'
  end
  
  if str == '[]'
    eval(gen_with_args.call('apply', args))
  elsif sym.to_s == '[]='
    eval gen_with_args.call('update', args)            # doesn't work right
  elsif methods.include? str
    send(str.to_sym, args)
  else
    __old_method_missing_in_scala_rb__(sym, args)
  end
end
```

The gen_with_args proc is merely a nifty little utility closure used to cut down on redundancy without creating an entire method. It actually generates a String of Ruby code which invokes the specified method with the given arguments. This is required because JRuby's Java integration is only so smart. It will _try_ to properly convert invocations of _n_ -arity Java methods when Ruby arrays are passed as arguments, but it can only do so well. The safest route is to just call the method, passing each element of the array as a successive argument.

All of this looks quite nice, and it seems to satisfy our requirements, but there is just one problem: Ruby doesn't behave itself with respect to the `[]=` operator. Rather than taking the sensible approach and allowing the receiving class to define its return value, it actually _ignores_ whatever value is returned from the `[]=` method and forces it to be the final argument. In other words, the above code will work, but it might not integrate with Scala in quite the way we would expect. Consider our original motivating example:

```ruby
import com.codecommit.collection.Vector

vec = Vector.new
vec = vec + 1
vec = (vec[0] = 2)       # problematic

puts vec[0]              # 2
```

Well, I said that this would be problematic. The issue is the value of the expression `vec[0] = 2` is not a new `Vector` instance as returned by the `Vector#update` method, but actually the `Fixnum` value `2`. Thus, the snippet above can _never_ work. In what is probably the most bone-headed feature of the entire language, Ruby forces every invocation of `[]=` to return the assigned value. This works fine (sort-of) for mutable, side-effecting implementations like `Array` and Scala's `ListBuffer`, but it completely falls apart for immutable, functional collections like `Vector`. So much for a language that "makes me smile"...

The solution of course is to always be aware of these cases where the `update` method does _not_ return `Unit` and just use `update` directly rather than `[]=`. Thus, a working version of the given snippet would be as follows:

```ruby
import com.codecommit.collection.Vector

vec = Vector.new
vec = vec + 1
vec = vec.update(0, 2)

puts vec[0]              # 2
```

By calling `update` directly, we ensure that its return value is captured and assigned back into `vec`. Kind of ugly, but unavoidable.

### Step Two: Mixins

Mixins are probably the most useful and code-saving feature in the entire Scala language. Like interfaces, they provide the flexibility of multiple inheritance, but they also bring with that the DRYness of shared definitions between subtypes. In fact, it may even be safe to say that the overwhelming power of the Scala collections library is owed primarily to traits. After all, without the inherited method definitions from `Iterable`, each collection would have to re-implement `foldLeft`, `map`, `flatMap` and each of the myriad of common methods defined by collections.

It just so happens that Ruby also has mixins of a very similar variety. It seems natural then that we should be able to use Scala mixins within JRuby just as if they were standard Ruby modules. Unfortunately, Java does not have mixins, meaning that unlike operator overloading, there isn't a nice and easy technique we can use to convert between the two worlds. The good news is that JRuby does give us a _bit_ of a head start in making this integration work: it's interface implementation syntax.

Ruby doesn't support interfaces directly, but JRuby allows us to use the standard `include` syntax on a Java interface. Once we have included the interface and implemented its methods, we can pass an instance of our Ruby class into Java methods expecting instances of that interface. JRuby takes care of all of the magic required to proxy the method calls. Syntactically, it looks something like this

```ruby
class DoSomething
  include java.lang.Runnable

  def run
    puts 'Running!'
  end
end
```

This is how we want our mixin integration to work. We _should_ be able to `include` a Scala trait into a Ruby class, implement any abstract methods and then expect everything to work per-normal. In order to accomplish this, we're going to need to override the `include` method to check to see if its target is a Scala trait. If that is the case, then `include` should create proxies for all of the defined methods within the trait. Basically, we have to do the same thing for Scala traits that Ruby does automatically for its modules.

Fortunately, this too is an area where Ruby's notion of "open classes" will come to our rescue. Not only does Ruby allow us to add methods to classes at runtime, it also allows us to _redefine_ core methods within the standard library; in this case, `Module#include`.

```ruby
class Module
  alias_method :__old_include_in_scala_rb__, :include
  def include(*modules)
    modules.each do |m|
      clazz = nil
      begin
        if m.java_class.interface?
          cl = m.java_class.class_loader
          mixin_methods_for_trait(cl, cl.loadClass(m.java_class.to_s))
        end
      rescue
      end

      # ...
    end
    
    modules.each {|m| __old_include_in_scala_rb__ m }
  end
end
```

The most important thing to see here is regardless of whether or not the module in question is a trait, we still forward the inclusion onto the old definition. This is critical, because it means that JRuby is still able to create an interface proxy for that class, allowing us to pass instances of the including class into Scala and have them treated as instances of the trait in question.

The key to our actual inclusion of the defined methods is the way in which Scala compiles traits. Consider the following:

```scala
trait Test {
  def method1(): Unit

  def method2() {
    println("In method2()")
  }
}

class Usage extends Test {    // mixin
  def method1() {
    println("In method1()")
  }
}
```

Scala will compile this into the bytecode equivalent of the following Java code:

```java
public interface Test {
    public void method1();

    public void method2();
}

public class Test$class {     // actually an inner-class of Test
    public static void method2(Test t) {
        System.out.println("In method2()");
    }
}

public class Usage implements Test {
    public void method1() {
        System.out.println("In method1()");
    }

    public void method2() {
        Test$class.method2(this);
    }
}
```

It's very clever if you think about it. Through a bit of compile-time magic, Scala is able to keep the method definitions within traits centralized (rather than inlining everything) as well as maintain full interface-level interop with Java. It is actually possible to define a Java method which accepts as a parameter an instance of a particular trait. Since traits are interfaces, javac knows how to deal with this definition and the JVM has no problems in actually dispatching the call.

We can use this implementation detail to enable our mixin of traits into JRuby classes. We're already testing within `include` to see whether or not the "module" at hand is in fact an interface, it would be a simple matter to also check for the existence of a class of the form "`TraitName$class`". If such a class exists, we could loop through all of its `public` `static` members and create corresponding proxy methods within the including class. All of this is done within the `mixin_methods_for_trait` method:

```ruby
def mixin_methods_for_trait(cl, trait_class, done=Set.new)
  return if done.include? trait_class
  done << trait_class
  
  clazz = cl.loadClass "#{trait_class.name}$class"
  
  trait_class.interfaces.each do |i| 
    begin
      mixin_methods_for_trait(cl, i, done)
    rescue
    end
  end
  
  clazz.declared_methods.each do |meth|
    mod = meth.modifiers
    
    if java.lang.reflect.Modifier.isStatic mod and \
	    java.lang.reflect.Modifier.isPublic mod
      @@trait_methods ||= []
      
      unless meth.name.include? '$'
        module_eval "\
def #{meth.name}(*args, &block)
  args.insert(0, self)
  args << block unless block.nil?
  
  args.map! do |a|
    if defined? a.java_object
      a.java_object
    else
      a
    end
  end
  
  begin
    scala_reflective_trait_methods[#{@@trait_methods.size}].invoke(nil, args.to_java)
  rescue java.lang.reflect.InvocationTargetException => e
    raise e.cause.message.to_s      # TODO  change over for 1.1.4
  end
end
"
        
        @@trait_methods << meth
      else
        define_method meth.name do |*args|    # fallback for methods with special names
          args.insert(0, self)
          
          begin
            meth.invoke(nil, args.to_java)
          rescue java.lang.reflectInvocationTargetException => e
            raise e.cause.message.to_s
          end
        end
      end
    end
  end
end
```

I know, the amount of code here is a little daunting, but it's really not that bad. Essentially, this just implements our intuition about how mixins should work in Ruby. The only thing about the algorithm that we haven't already considered is how to deal with the super-traits of traits. Since the inheriting of method definitions is only done through static proxying, there would be no reason for Scala to compile an inherited relationship between the definition class of a sub-trait and its super-trait. To get around this problem, we explicitly check for super-interfaces and then mixin their methods _first_. This is to allow for methods which are inherited by sub-traits and then overridden.

The final little tidbit here is the way in which the proxy methods are created. Ruby does allow us to add methods to classes at runtime, but it is unfortunately rather restrictive in _how_ those methods can be added. In a nutshell, there are two main techniques: `module_eval` and `define_method`. By using `define_method`, we are able to avoid `String` evaluation and keep our editor's syntax-highlighting happy with our sources. Unfortunately, methods created using `define_method` have a very critical limitation: they cannot accept blocks. Thus, any mixed-in trait methods which took function values as parameters would be unusable from within Ruby.

This inconsistency is fixed in Ruby 1.9, but until then we still need a way of proxying higher-order Scala methods. Thus, we default to using `module_eval`. Unfortunately, this technique also comes with its own set of issues; specifically: illegal characters. As we saw previously, any Scala method with a non-alphanumeric name will have to use the `$` character to denote certain symbols. However, Ruby assigns special significance to symbols like `$` and `@`, making it impossible to use them within method names. The only way around this restriction on method names is to create such special methods using `define_method`, which brings us right back to our first set of problems.

The only solution I could come up with for Ruby 1.8 was to use `module_eval` by default, unless the method name contained a `$` character, in which case `define_method` would be used. With this technique, almost every case is covered. The only remaining issue would be if a method with a non-alphanumeric name took a function as a parameter. In this case, the method would be unusable from Ruby. However, even a cursory glance over the Scala standard library shows that this is an extremely rare case, one which can be safely made "difficult" if it means improving the integration in other areas (trait mixins).

One final unfortunate note on this method: it doesn't work with JRuby 1.1.3 and later (the current release is 1.1.4). As reported by [JRUBY-2999](<http://jira.codehaus.org/browse/JRUBY-2999>), including two separate Java interfaces with conflicting methods will actually cause the interpreter to crash. At the time of the writing, this bug has not been resolved, either in 1.1.4 or in the latest SVN sources. Since traits quite often have methods which overlap with inherited methods, there is really no way to get around this bug. Thus, the library described in this article was developed with JRuby 1.1.2 and will only work with that release or any upcoming releases which fix the regression. Interestingly, this means that the library must deal with a _different_ , less critical JRuby bug which makes it impossible to `raise` Java exceptions. As this exception bug does not entirely prevent the use of Scala mixins, it is preferable to the more serious interpreter-crash regression.

Now that we have mixins at our disposal, it is possible to further improve our Scala integration by implementing a `scala_object` method for `Array` and `Hash`, one which converts these objects into a form which can be passed to Scala methods expecting `Seq` and `Map`, respectively.

```ruby
class BoxedRubyArray
  include Scala::RandomAccessSeq
  
  def initialize(delegate)
    @delegate = delegate
  end
  
  def apply(i)
    @delegate[i]
  end
  
  def length
    @delegate.size
  end
end

class Array
  def scala_object
    BoxedRubyArray.new self
  end
end
```

The `Hash` proxy is analogous, but much more verbose. The key to this entire implementation here is the enabling of Scala mixins within JRuby. All we have to do is mixin the `RandomAccessSeq` trait, implement `apply` and `length`, and suddenly we have a first-class Scala collection backed by a Ruby `Array`. Not only does this allow us to use some of Scala's higher-order magic on Ruby arrays, it also enables previously impossible usages like the following:

```ruby
import com.codecommit.collection.Vector

vec = Vector.new
vec = vec + 1 + 2 + 3 + 4 + 5

arr = [6, 7, 8, 9, 0]
cat = vec.concat arr.scala_object
```

At the end of this snippet, `cat` is a Scala `Iterable` with contents `[1, 2, 3, 4, 5, 6, 7, 8, 9, 0]`. Remember that `concat` is actually a method within `Vector` which itself expects an instance of `Iterable`. Fortunately, we now have a method to convert a Ruby array into just such an `Iterable`, which we then pass to `concat` achieving our final result.

### Step Three: Closures

Both Ruby and Scala have this notion of closures, which are assignable, anonymous functions with access to their enclosing scope. Closures are most often used as a syntactic device for passing and/or storing a bit of code for later invocation. A common example that even Java has to deal with would be event handling, where a specific code block must be executed every time a button is pressed. Scala and Ruby take this concept a little further, providing higher-order functions like `map` and implementing conventional iteration using `foreach` and `each` (respectively).

Optimally, we should be able to call Scala methods passing Ruby blocks where Scala function values are required. As it happens, even without special Scala integration magic, we can already get very close to this:

```ruby
vec = Vector.new
# ...

vec.foreach do |e|
  puts e
end
```

This will print every element in the `vec` instance. This works because JRuby allows blocks to be passed to methods accepting interfaces. Since Scala's function values are in fact subtypes of traits `Function0`, `Function1` and so on (according to arity), JRuby is able to recognize the method signatures appropriately and proxy the values.

Unfortunately, JRuby doesn't innately understand Scala traits, so it doesn't know that the `compose` method should be proxied to an implementation within the trait. I _assume_ that JRuby will proxy every method in the target interface to the block in question (I haven't actually tested that). Assuming this is the case, the moment that `foreach` (or any other method) attempts to invoke `compose` on a proxied JRuby block, the result will be a `ClassCastException`. It just so happens that `foreach` behaves itself and there is nothing to worry about, but we cannot make that guarantee in general.

The solution of course is to do for Ruby's `Proc` what we already did for `Array` and `Hash`: provide a wrapper which uses the `Function` _n_ mixin and delegates to the `Proc` in question. However, unlike `Array`, there is no single "`Function`" mixin that we can use. Instead, we must create a _separate_ wrapper for each function arity supported by Scala...all 22 of them. Fortunately, Ruby's meta-programming capabilities help us out here, allowing us to define classes within a loop and then dynamically select the correct wrapper class name based on the `Proc` [arity](<http://www.ruby-doc.org/core/classes/Proc.html#M001577>):

```ruby
module ScalaProc
  class ScalaFunction
    def initialize(delegate)
      @delegate = delegate
    end
    
    def apply(*args)
      @delegate.call args
    end
  end
  
  for n in 0..22    # sneaky, but much more concise
    eval "\
class Function#{n} < ScalaFunction
  include Scala::Function#{n}
end
"
  end
end

class Proc
  def to_function
    eval "ScalaProc::Function#{arity}.new self"
  end
  
  def java_object
    to_function
  end
  
  def scala_object
    to_function
  end
end
```

The only abstract method within any of the `Function` _n_ traits is apply, taking the same number of arguments as the function arity. It's easy enough to implement this function once within the `ScalaFunction` superclass, which means that all we have to do within each of the arity-specific wrappers is mixin the relevant `Function`.

Now that we have this conversion between Ruby `Proc`(s) and Scala function values, we can make use of it in situations where it becomes necessary. For example, let's say that I have a Scala method like the following:

```scala
def doAndMultiply(by: Int)(f: (Int)=>Int) = {
  f compose { (_:Int) * by }
}
```

Simply put, this method takes two [curried parameters](<http://www.codecommit.com/blog/scala/function-currying-in-scala>), an `Int` along with a function value taking another `Int` and returning an `Int`.  This method then returns a new function which itself takes an `Int`, first multiplies it by the original first `Int` parameter, then applies the given function to the result. Got all that? :-)

We can call this function from Scala in the following way:

```scala
val comp = doAndMultiply(3) { _ + 2 }
comp(5)      // => 17
```

Now that we have our super-fancy `Proc` conversion, we are able to use an almost identical call syntax from within Ruby. Notice how we are able to take advantage of the way in which Scala compiles curried methods to achieve a JRuby syntax which looks almost exactly like block-standard Ruby:

```ruby
add = proc {|x| x + 2 }
comp = doAndMultiply(3, add.scala_object)

comp.call 5        # => 17
```

And if `doAndMultiply` is a method brought in via a mixin, we can do even better:

```ruby
comp = doAndMultiply 3 do |x| 
  x + 2
end

comp.call 5        # => 17
```

This works because of the way in which we coerce parameters within the proxies for the mixed-in methods. Recall from earlier:

```ruby
args.map! do |a|
  if defined? a.java_object
    a.java_object
  else
    a
  end
end
```

The very reason we have to explicitly map over the method arguments is to satisfy this particular case. JRuby's Ruby-to-Java coercion is pretty smart, but it doesn't seem to be up to this particular challenge (believe me, I tried). Thankfully, a little bit of benign check-and-coerce later, our arguments are none the worse for wear and in a form that Scala can chew on.

### Conclusion

As you may have guessed, I have already taken the liberty of implementing the framework described in this article. It even has a few features I didn't discuss, like auto-detecting the default Scala installation and the ability to use Scala function values as if they were Ruby `Proc`(s). All in all, it makes for a very slick way of working with Scala libraries from within JRuby scripts in an intuitive, idiomatic fashion. Hopefully, this should help to encourage the use of these two fascinating technologies together in future projects.

  * Download [scala.rb](<http://www.codecommit.com/blog/misc/scala.rb>) (does _not_ work with JRuby 1.1.3 or 1.1.4)