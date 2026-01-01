---
categories:
- Ruby
date: '2007-02-18 18:51:26 '
layout: post
title: JRuby Event Proxy
wordpress_id: 87
wordpress_path: /ruby/jruby-event-proxy
---

One of the things which really which annoys me about JRuby is the lack of a clean syntax corresponding to Java's anonymous inner classes. Now, Ruby has a neat little syntax trick called blocks. It really would be the ultimate solution if you could use Ruby blocks in things like Java event listeners instead of subclasses. Here's a quick example: 

### test3.rb

```ruby
require 'java'

JFrame = javax.swing.JFrame
JButton = javax.swing.JButton
FlowLayout = java.awt.FlowLayout
ActionListener = java.awt.event.ActionListener

class CustomActionListener < ActionListener
    def actionPerformed(e)
        puts 'Button Clicked'
    end
end

frame = JFrame.new 'Test Frame'
frame.layout = FlowLayout.new

button = JButton.new 'Click Me'
button.addActionListener CustomActionListener.new
frame.content_pane.add button

frame.setSize(200, 75)
frame.default_close_operation = JFrame::EXIT_ON_CLOSE
frame.visible = true
```

![](http://blogs.dzone.com/daniel/files/2007/02/021807_2355_JRubyEventP17.png) Notice anything annoying about the above code? We're using this really nice, clean Ruby syntax with this incredibly ugly listener syntax (the custom class). Now, Ruby does have a slightly cleaner recourse called anonymous classes which is a little nicer, but still fairly ugly: 

### test2.rb

```ruby
require 'java'

JFrame = javax.swing.JFrame
JButton = javax.swing.JButton
FlowLayout = java.awt.FlowLayout
ActionListener = java.awt.event.ActionListener

frame = JFrame.new 'Test Frame'
frame.layout = FlowLayout.new

button = JButton.new 'Click Me'
frame.content_pane.add button

listener = Class.new(ActionListener).new
def listener.actionPerformed(e)
    puts 'Button Clicked'
end
button.addActionListener listener

frame.setSize(200, 75)
frame.default_close_operation = JFrame::EXIT_ON_CLOSE
frame.visible = true
```

As you see, this is a little nicer primarily because there's no extraneous named-class to be defined. However, it still lacks a truly clean syntax. What would be really nice is if we could write code like the following: 

### test.rb

```ruby
require 'java'
require 'jruby_utils'

JFrame = javax.swing.JFrame
JButton = javax.swing.JButton
FlowLayout = java.awt.FlowLayout
ActionListener = java.awt.event.ActionListener

frame = JFrame.new 'Test Frame'
frame.layout = FlowLayout.new

button = JButton.new 'Click Me'
button.addActionListener proxy_method(ActionListener, 'actionPerformed') { |e|
    puts 'Button Clicked'
}
frame.content_pane.add button

frame.setSize(200, 75)
frame.default_close_operation = JFrame::EXIT_ON_CLOSE
frame.visible = true
```

I think that this is pretty clean. There's no class definition anywhere. In fact, it's all done in a block with the _ActionEvent_ instance passed as a parameter. This is cute and all, but it doesn't seem to be very useful. To make this work, we're utilizing a fictional method: "proxy_method". Intuitively, this method should take a _Class_ and a _String_ defining a method name to override as parameters (along with a block), and then reflectively wrap the block into an overriding method within a subclass of the specified _Class._ In Java, this would be absolutely impossible - not to mention unnecessary and somewhat bizarre since Java a) defines no such "block" syntax, and b) Java already has anonymous inner classes, so there really isn't a problem to solve. However, Ruby has a far more dynamic nature and it is actually possible for us to write _proxy_method_. The implementation looks something like this: 

### jruby_utils.rb

```ruby
require 'java'

def proxy_method(superclass, name, &block)
    clazz = Class.new(superclass)
    instance = clazz.new
    instance.instance_eval do
        @name = name
        @block = block

        def method_missing(missing, *args)
            if missing.id2name == @name
                @block.call args
            else
                super
            end
        end
    end
    instance
end
```

As you can see, this is pretty much doing reflectively what we worked out mentally above. _proxy_method_ accepts two parameters, a _Class_ and a _String._ The third parameter is a special "block parameter" which is assigned by Ruby to the specified block wrapped into a _Proc_ instance. (the specified block being the block used in conjunction with the _proxy_method_ invocation in the example above) Then, the method dynamically creates an anonymous class with the specified superclass and assigns it to the _clazz_ variable. _clazz_ (which really represents a bona fide type) is instantiated and a dynamic code block is invoked. (a dynamic instance block is effectively a reflective body to a class) The _name_ and _block_ parameters are assigned to instance fields within the dynamic block and a new method is defined (or rather overridden). Now, in Ruby, the _method_missing_ method is a special method which is invoked by Ruby in the case of a method invocation which cannot be resolved against the specified instance. The default implementation (in _Object_ ) raises an exception, but by overriding this method, developers can easily add arbitrary methods to instances dynamically. In this case, we check to see if the missing method's name matches the method name we were passed as a parameter of _proxy_method._ If the names do indeed match, then we invoke the block we were passed in the first place, passing all of the parameters which were intended for the missing method. Then, the value returned from the block is returned from _method_missing_. (though, in the case of an event listener this really isn't necessary) The a vitally important part of our code is the else statement within _method_missing._ This call passes the method missing invocation up the hierarchy if it doesn't match the method name with which we are concerned. This is important because this allows either the runtime to throw an exception (in case of a truly missing method), or another _method_missing_ implementation in a super-class to also check for method to reflectively invoke. In our case, the _method_missing_ call will get sent on to the _JavaObject_ class (defined by JRuby) which will check for any corresponding methods in the associated Java class. If none are found, it too will send the _method_missing_ invocation up the hierarchy where it will eventually reach _Object,_ which will raise an exception. Just as an aside, _proxy_method_ is also suitable for usage in other situations such as creating Java threads directly in JRuby. Consider the following example: 

### test4.rb

```ruby
require 'java'
require 'jruby_utils'

JThread = java.lang.Thread
thread = proxy_method(JThread, 'run') { puts '...in another thread' }
thread.start
```

This code will print "…in another thread" once to stdout. But the cool bit is this is actually equivalent to the following code: 

### test4.rb

```ruby
require 'java'

JThread = java.lang.Thread
class ThreadImpl < JThread
    def run
        puts '...in another thread'
    end
end

thread = ThreadImpl.new
thread.start
```

Notice how much cleaner it is with _proxy_method_? :-) Hopefully, you find this to be a useful overview of Ruby method reflection in general, and more specifically, a useful method which can greatly simplify any JRuby code using Java APIs designed for use with anonymous inner classes.