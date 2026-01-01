{%
laika.title = "Rapid Prototyping with JRuby"
laika.metadata.date = "2007-02-16"
%}


# Rapid Prototyping with JRuby

For this entry, let's consider a situation which arises fairly frequently in real world development: the need to quickly prototype something. Now, when I say prototype, I mean something even more atomic than building a demo of an application or feature. What I'm talking about is trying out a certain aspect of an API to make sure it works as expected; or possibly something even more trivial like quickly retrieving a system property. Now, Java as a language just doesn't lend itself very well to this kind of development. I've heard it said that "in a world of lightweight tools, Java is a sledge-hammer." While this analogy is somewhat derogatory, it's true to a certain extent. The way the Java language is structured syntactically, coupled with the sheer overly-designed bulkiness of most Java APIs, makes Java utterly unsuitable for quickly trying something with a minimum of fuss. For example: 

```java
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.JComponent;
import javax.swing.JFrame;

/*
 * Created: Feb 16, 2007
 */

/**
 * TODO	add description
 *
 * @author Daniel Spiewak
 */
public class TestClass {
    public static void main(String... args) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                final GradientPaint paint = new GradientPaint(0, 0, Color.BLACK, 0, 200, Color.WHITE);

                JFrame frame = new JFrame("Test Frame");
                frame.setSize(200, 200);

                JComponent component = new JComponent() {
                    @Override
                    protected void paintComponent(Graphics g) {
                        Graphics2D g2 = (Graphics2D) g;

                        g2.setPaint(paint);
                        g2.fillRect(0, 0, getWidth(), getHeight());
                    }
                };
                frame.getContentPane().add(component);

                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setVisible(true);
            }
        });
    }
}
```

This example could be code produced by someone trying to test the Swing gradient functionality. Here, however, is the corresponding JRuby code: 

```java
require 'java'

JFrame = javax.swing.JFrame
JComponent = javax.swing.JComponent
GradientPaint = java.awt.GradientPaint
Color = java.awt.Color

class CustomComponent < JComponent
    @paint = GradientPaint.new(0, 0, Color::BLACK, 0, 200, Color::WHITE)

    def initialize
    end

    def paintComponent(g)
        g.paint = @paint
        g.fillRect(0, 0, getWidth, getHeight)
    end
end

frame = JFrame.new 'Test Frame'
frame.setSize(200, 200)

frame.add CustomComponent.new

frame.defaultCloseOperation = JFrame::EXIT_ON_CLOSE
frame.visible = true
```

![](http://blogs.dzone.com/daniel/files/2007/02/021607_2341_RapidProtot1.png)There, isn't that ever so much cleaner? :-) (actually, readers who haven't fallen asleep yet will notice I cheated in the JRuby version by not wrapping everything into the EDT, but that's another post…) Either way, the result is the same (pictured right), a simple JFrame showing a full gradient from black to white. Not a terribly impressive example, but one which does demonstrate the atomic nature of a lot of modern prototyping needs. But let's look at something smaller for a moment… Let's say that you're building some code which needs to (among other things) detect the _exact_ operating system upon which it is running and perform different operations based on those facts. This is a fairly common scenario among desktop applications. In fact, I came across it myself just a few days ago. Now, Java provides a system property which returns a string representation of the operating system name. (it's the os.name property) While this is a good starting place, the property itself doesn't follow a standardized format. And since the only way to really determine the OS name is to perform string manipulation on the value - which is in a non-standardized format - we're a bit up the creek. The only way to be sure we're running our tests properly is to actually examine the value of the os.name property. This would be ever-so-easy if Java provided a nice mechanism to peer into the standardized system properties, but alas, no such mechanism exists. The only way to get a property value, in fact, is to actually run some code which retrieves the value and prints it out. Unfortunately, due to Java's rather verbose syntax, such a program would be ridiculously long (given the problem). A better way is to use the JIRB utility, an interactive JRuby console: 

```
Microsoft Windows [Version 6.0.6000]
Copyright (c) 2006 Microsoft Corporation.  All rights reserved.

C:\\Users\\Daniel Spiewak>jirb
irb(main):001:0> require 'java'
=> true
irb(main):002:0> System = java.lang.System
=> System
irb(main):003:0> System.getProperty 'os.name'
=> "Windows Vista"
irb(main):004:0> exit

C:\\Users\\Daniel Spiewak>
```

And it's just that simple. In fact, I have JRuby installed on all of my test systems for precisely this reason. It's so easy to do really quick, highly atomic prototyping in JRuby. Things (such as the OS name example) which would ordinarily take five to ten minutes of typing (in Java) require mere seconds in JIRB.