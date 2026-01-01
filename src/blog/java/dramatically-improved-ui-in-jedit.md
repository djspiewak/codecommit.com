{%
laika.title = "Dramatically Improved UI in jEdit"
laika.metadata.date = "2008-05-22"
%}


# Dramatically Improved UI in jEdit

This is definitely old news by now (in fact, almost a month old), but I'm just now discovering it myself so I decided to share.  The jEdit project is renowned for two things:  
  

  * Marvelous support for every language under the sun
  * Eye-bleedingly bad UI design

It's always been possible to hack yourself an improved version without too much trouble; but by default, jEdit has always looked terrible.  This one factor, more than anything else, has contributed to jEdit's reputation as the supercharged editor which everyone refuses to try.  Fortunately, this influence has been seriously reduced in the [4.3pre14](<https://sourceforge.net/project/showfiles.php?group_id=588&package_id=3753&release_id=595374>) release:

![image](/assets/images/blog/wp-content/uploads/2008/05/image2.png)

Compare that to [the old look](<http://jedit.org/index.php?page=screenshot&image=34>).  Even with Java 6 subpixel rendering, the interface remained a mess.  What's more, many of the interface elements were custom renderings, preventing the platform-native LAF from appropriately styling them (the toolbar controls are a prime example).  All of this is fixed in 4.3pre14.

jEdit is rapidly approaching "usable editor" status out of the box, something that even the mighty TextMate hasn't quite achieved.  Granted, it's still Swing-based, which means the fonts render horribly on Vista without Java 6uN, but it's a step in the right direction.  Now, if only they would do something about [their website](<http://www.jedit.org>)...