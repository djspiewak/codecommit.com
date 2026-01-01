{%
laika.title = "Techniques of Java UI Development"
laika.metadata.date = "2008-02-18"
%}


# Techniques of Java UI Development

Too often these days I see Java developers new to the psuedo-science of UI development finding themselves completely lost before they even get started.  There are a lot of misconceptions about the "best" and "easiest" way to create a professional UI in Java, and precious few resources which attempt to clear up the confusion.

In this article, I'm going to make use of the Swing framework simply because it's more widely known.  All of the same techniques and processes apply to SWT with equal validity.  I'll walk through the steps required to create a simple form UI in the easiest and most maintainable fashion.  I'm well aware that there are _faster_ ways of doing this, but in my experience, this process will lead to more maintainable and less rigid UIs.

**Note:** These steps will obviously be different if you're working in a team or with a designer.  The intention of this article is not to present the "be all, end all" of UI design practices, but rather to be a set of guidelines for the common-case developer.

### Design

It may surprise you, but the very first step in creating a UI is design.  You have to know fairly precisely how you want the UI to look and (more importantly) behave.  This step is more than just drawing a few mockups in Illustrator.  This is sitting down and hashing out what you want the controls to do when the window is resized, if the state of any controls should be tied to that of others, whether your labels LEFT aligned or RIGHT, etc.  Often I find the easiest way to start is with a diagram:

 ![Diagram](/assets/images/blog/wp-content/uploads/2008/02/diagram.jpg)

This is what my diagram would look like, but it's important to note that this isn't a "one size fits all" style.  The point of the diagram is to give _you_ a medium to lay out the semantics of a panel and to help you understand just what it is you have to do.  This step becomes invaluable later on as you start actually writing code.

The important thing about this diagram is that it is annotated.  Anyone can just draw a picture of what they want the UI to look like, what components to use, how they should be spaced.  The real meat of the question is not what does it _look_ like, but how does it _behave_.  One of the biggest questions Java UI developer have to answer (as opposed to, say .NET developers) is how do the controls behave when the form is resized.  This is a much more complicated question than "where should the component be placed" or "how should it look".  In essence, the question is asking "what sort of layout manager configuration do you need?"

There are two directions which need to be considered: vertical and horizontal resize.  This may not be news, but it's something that a lot of developers (myself included) seem to forget.  The way the form scales horizontally is just as important as how it scales vertically, so it is critical that these considerations are dealt with in the design phase.  You'll notice on my diagram that I have annotated almost every component with scaling constraints, dictating visually how the component will behave as the form changes size.  For example, all but one of the text fields scale out horizontally, but remain with a fixed height.  The one text field which does not scale horizontally is annotated so that it remains with completely fixed dimensions, both vertical and horizontal.  It's also important to note that the **City** text field scales horizontally, "pushing" the **State** combo box as it does so.  Thus the state selection is always right aligned, the city label is left aligned, and the intervening space is filled with the city text field.  This sort of "resize mapping" (to coin a phrase) _must_ be considered before any code is written.

You'll notice that there is just a single annotation indicating behavior when the form resizes vertically.  This is because, like most form-based UIs, there isn't really a good way to deal with this situation.  In this case, I just drew a line through the form where I thought I saw the most logical split and indicated annotatively that this "non-component" should be what scales vertically.  Thus the buttons remain fixed to the bottom, while the remainder of the components build down from above.  Some forms have text areas (multi-line text fields) which may scale vertically, filling this space.  Our form however has no components which logically scale in the vertical direction, thus we invent a non-component which fills the gap in our imagination.  This non-component will only be reflected in the layout constraints, we won't have to add anything extra to the panel itself.

When drawing this diagram, note that I wasn't afraid to make mistakes and then annotatively clarify them.  For example, I made the form to narrow to clearly express the intent of the layout.  The **City** and **State** fields take up more room than I expected.  Rather than starting from scratch, I just draw the fields as it seems most intuitive and then indicate with arrows that they should be aligned with the other text fields.  Likewise, the **Line 1** and **Line 2** fields should actually stretch all the way to the edge of the section.  This wasn't clear in the diagram, so I added the **\--|** annotation to remind myself of this fact.  Always remember that we're not trying to make an unambiguous specification, just a reminder to ourselves.  It's a design aid, nothing more.

The final bit of annotative goodness which needs to be observed is the "disables" marker attached to the **Address** checkbox and the section below it.  This marker indicates some basic UI state behavior: when the checkbox is selected, the section will be enabled; otherwise the section below (and all its components) will be disabled and no data may be entered.  Note that the annotation doesn't say "clears" or "resets", since we just want the section to be disabled and ignored.  Someone may disable the address section only to realize later (prior to saving) that they actually _do_ need to enter an address.  They would likely want all of their previous changes to still be there, rather than completely lost.

Notice what we _didn't_ annotate here.  We didn't clutter our design with intuitive concepts like "Save button saves the contents and closes the window".  Rather, we left that up to our imagination and (likely) prodigious memory.  Remember that this diagram is for _our_ use, not our manager or the other developers in our team.  If we were building a diagram for external consumption, we would have had to make an effort to be more clear.  As it is, we can trust ourselves to build a UI that makes sense according to convention.

### First Steps

So we have our design diagram in hand, it's finally time to consider some code.  It's usually around this time that developers start thinking about what tools to use and how they can be of help.  Let me start out by saying flat-out that I am _not_ in favor of _any_ graphical UI design tool.  Don't get me wrong, I think Matisse is a brilliant bit of engineering and a credit to the entire NetBeans team, but it's not a silver bullet.  In fact, I don't even think it's a bullet worth firing.  I could create the code for the diagramed UI in a few minutes using Matisse, but what would the code look like?  Would it be code worth coding?

![image](/assets/images/blog/wp-content/uploads/2008/02/image.png)

```java
javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
jPanel1.setLayout(jPanel1Layout);
jPanel1Layout.setHorizontalGroup(
  jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
  .addGroup(jPanel1Layout.createSequentialGroup()
    .addContainerGap()
    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(jPanel1Layout.createSequentialGroup()
        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
          .addComponent(jLabel3)
          .addComponent(jLabel4)
          .addComponent(jLabel5))
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
          .addComponent(jTextField4, javax.swing.GroupLayout.DEFAULT_SIZE, 376, Short.MAX_VALUE)
          .addComponent(jTextField3, javax.swing.GroupLayout.DEFAULT_SIZE, 376, Short.MAX_VALUE)
          .addGroup(jPanel1Layout.createSequentialGroup()
            .addComponent(jTextField5, javax.swing.GroupLayout.DEFAULT_SIZE, 282, Short.MAX_VALUE)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(jLabel6)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, 
                 javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
      .addGroup(jPanel1Layout.createSequentialGroup()
        .addComponent(jLabel7)
        .addGap(18, 18, 18)
        .addComponent(jTextField6, javax.swing.GroupLayout.PREFERRED_SIZE, 93, 
            javax.swing.GroupLayout.PREFERRED_SIZE)))
    .addContainerGap())
);
```

Oh yeah, I'm looking forward to working with that code.  What's actually worse is you _can't_ work with that code.  NetBeans prevents you from editing the auto-generated sections of the code.  Meaning if Matisse got it wrong (and it did a couple times during the construction of the screenshot), there's no way for you to fix it by hand.  And even assuming you drop back to your favorite external editor to make your fix, the next time you open the file in NetBeans, your changes will be overwritten.  Matisse is a totally dominating, uncompromising beast that takes over your UI and doesn't let go.

Honestly, this isn't a stab at NetBeans in any way.  Matisse does a fine job auto-generating its code and it's amazingly good at inferring layout constraints based on convention and control placement.  It's no substitute for fine-grained manual control however.  People may argue that "UI design is a visual task", but that's what the design diagram is for.  I've worked with some fairly complex Swing UIs in my time, and let me tell you that if you don't know how to write the code by hand, you'll never be able to maintain it.  And if you can't maintain your UI code, that's going to cause tons of problems down the road.  So no quick and easy tools for our UI-making.

If we're going to write the code by hand (and we are), then we're going to need to decide which layout manager(s) to make use of.  This is where the diagram can be extremely helpful.  Just looking at our design suggests a grid based approach, a subpanel (for the address) and a footer panel for the buttons (probably using something like _FlowLayout_ ).  Working from the high-level down to the fine-grained details:

  1. From top down: we have a body, some empty space which stretches on resize and a footer section.  This suggests the use of _BorderLayout_ with the "body" as the _CENTER_ constraint and our button panel as _SOUTH_
  2. Button panel appears to keep the components centered, so _FlowLayout_ would be the most appropriate 
  3. Body panel seems to be a grid of two columns, one for the labels and one for the text fields.  The **Address** component spans both grids with a left offset of 15-20 pixels.  Below this is the address panel which spans both columns and stretches to fill.  The address panel is _not_ set to stretch vertically, but is rather attached to the top of its cell, allowing empty space to stretch below it 
  4. Address panel appears to be a grid divided into _four_ columns.  The first two rows have text fields which span three columns.  The bottom row contains a text field which also spans three columns, but does not stretch to fill

And no, I'm not concerned with resolution independence here.  After all, if Apple doesn't care, why should I?  :-)

This process mainly depends upon familiarity with layout managers and some experience with form layout.  I happen to know that some of the most powerful layout managers are grid based, so I try to fit most UI designs into a grid.  I also know that Swing comes with a very nice (if limited) default layout manager called _BorderLayout_.  Many high-level layout constructions fall into a category of problems which are trivial to manage with _BorderLayout_ (like our button footer).  There really are a number of patterns which can be kept in mind during this layout process, but in the end it all comes down to experience.  Once you've built a form-based UI, the steps involved in creating another become somewhat instinctive.

We've already specified two of the three layout managers we will need for this UI.  All that remains is to select a grid-style manager which can accommodate both fixed and fluid constraints.  We could use _GridBagLayout_ , but for reasons which should be obvious, I like to avoid that one.  Unfortunately, Swing doesn't really have another grid layout which is powerful enough to accomplish our goals.  So we look outside of the core Swing API.

There are a number of third-party layout managers available, but the one I really favor is a layout manager I [ported from SWT](<http://www.jroller.com/djspiewak/entry/swt_gridlayout_in_swing>) a number of years ago.  SWT's _GridLayout_ is basically most of the power of _GridBagLayout_ , wrapped in a clean and intuitive API.  It's more than sufficient for our purposes, and it won't needlessly gum up our code with dozens of verbose constraint definitions.

### Fleshing it Out

With all three layout managers chosen and most of the constraints solidified, it's time to get our hands dirty and actually write some code.  For simplicity of example, we're just going to put everything into a single class with a main method.  This main method will simply set the look and feel (avoiding metal) and launch the frame:

```java
public static void main(String... args) {
    EventQueue.invokeLater(new Runnable() {
        public void run() {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (ClassNotFoundException e) {
            } catch (InstantiationException e) {
            } catch (IllegalAccessException e) {
            } catch (UnsupportedLookAndFeelException e) {
            }
            
            new ContactDetails().setVisible(true);
        }
    });
}
```

As per convention, all of our components will be created and initialized in the class constructor.  Some people are proponents of the _init()_ or _initComponents()_ convention, but I personally don't see that it serves any purpose.  If you're just calling the method from the constructor anyway, why bother?

You'll notice that we've wrapped the entire launch sequence in an _invokeLater()_.  **This is extremely important.**   Swing (or rather, AWT) manages the entire event dispatch queue for you in separate threads.  It's not like SWT where the event dispatch is manually, er...dispatched.  This means that the main thread is not in fact the EDT (event dispatch thread).  If we just made a call to our constructor within the main thread itself, we would be creating components and interacting with Swing in a thread other than the EDT.  Swing is pretty good at making sure that the event dispatch still _works_ , but odd bugs can creep in around the edges.  Ever wonder why the _setVisible(true)_ invocation sometimes blocks and doesn't at other times?  This is usually due to the differences between an in-EDT invocation and a main thread call.  Romain Guy turned me on to this misunderstood bit of Swing usage, and I've been using it ever since.  Believe me when I say that you'll save yourself a world of hurt by making sure you're square with the EDT.

Now that we're sure our invocations are properly wrapped, we can get started writing the UI layout code, adding the components to the frame.  Honestly, this code is more than slightly verbose and repetitious, so I'm not going to reproduce all of it.  This is just the interesting stuff:

```java
setDefaultCloseOperation(EXIT_ON_CLOSE);
setSize(400, 265);

JPanel body = new JPanel(new SWTGridLayout(2, false));
getContentPane().add(body);

body.add(new JLabel("First Name:"));

JTextField firstName = new JTextField();

SWTGridData data = new SWTGridData();
data.grabExcessHorizontalSpace = true;
data.horizontalAlignment = SWTGridData.FILL;
body.add(firstName, data);

body.add(new JLabel("Last Name:"));

JTextField lastName = new JTextField();

data = new SWTGridData();
data.grabExcessHorizontalSpace = true;
data.horizontalAlignment = SWTGridData.FILL;
body.add(lastName, data);

JCheckBox address = new JCheckBox("Address");

data = new SWTGridData();
data.horizontalSpan = 2;
data.horizontalIndent = 15;
body.add(address, data);

JPanel addressPanel = new JPanel(new SWTGridLayout(4, false));
addressPanel.setBorder(BorderFactory.createEtchedBorder());

data = new SWTGridData();
data.grabExcessVerticalSpace = true;
data.horizontalAlignment = SWTGridData.FILL;
data.verticalAlignment = SWTGridData.BEGINNING;
data.horizontalSpan = 2;
body.add(addressPanel, data);
```

 ![image](/assets/images/blog/wp-content/uploads/2008/02/image1.png)

Well, this isn't _quite_ like our diagram, but it's close enough.  Remember that the diagram is only a guide.  It's purpose is to focus your thought and allow you to create the layout constraints.  It's never intended to be a hard-and-fast spec.

You'll notice how much more readable the manually written code is than its Matisse-generated counterpart.  It may be repetitive and annoying, but it's very readable, very maintainable.  We could actually add a little bit of abstraction here (e.g. _createGridData()_ ) to make things even _more_ readable, but that's overkill for this example.  Actually, not only is this code readable and maintainable by myself, but I can also check it into a shared repository and other people (potentially using different editors) can make changes to the layout and UI logic and expect them to actually work.  I've had nothing but bad experiences when using UI builders in a team setting, especially builders like Matisse which rely on external meta files.

One thing that is worth calling attention to is that magic number pair in the _setSize()_ invocation.  Of course, I could have used _pack()_ , but that rarely sizes a frame as something which looks pleasing to the eye.  I've found that the easiest way to get a frame which is sized nicely for most platforms is to put in some easy default width and height (say, 400x400), then once the layout is fixed, size the form by hand.  Literally run the form and resize the window until everything looks good.  Once that's done, screenshot it and measure the width and height in Paint.  :-)  It may sound crude, but it works surprisingly well.  Of course, the size will have to change a bit, depending on which platform the app is running on, but this is easy enough to handle.  Usually I repeat the process on each platform, taking the largest width-height pair as making that the fixed standard for all platforms.

### Conclusion

And there you have it, a simple Java form UI created with minimal fuss and maximum compatibility.  More importantly, the techniques presented in this article can be applied to more UIs in future, and not just forms.  All of us at some point have to build a basic Swing UI, and it's best if we're comfortable doing so and able to create something that's worth keeping around.  Hopefully the concepts I have presented will be useful to you the next time you find yourself faced with a similar situation.

  * [Download ContactDetails as Eclipse Project](<http://www.codecommit.com/blog/misc/ContactDetails.zip>)