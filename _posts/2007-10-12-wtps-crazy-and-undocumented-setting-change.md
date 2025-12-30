---
categories:
- Eclipse
date: '2007-10-12 00:00:20 '
layout: post
title: WTP's Crazy (and undocumented) Setting Change
wordpress_id: 139
wordpress_path: /eclipse/wtps-crazy-and-undocumented-setting-change
---

I've been working recently on a Wicket-based project for a company called [Teachscape](<http://www.teachscape.com/html/ts/public/html/index.htm>).  Not only is it based on Wicket, but the project is also designed to be run within Jetty, as opposed to the traditional Tomcat or Glassfish deployment.  This makes local development a lot easier to handle, since you just fire a main-class (which instantiates and starts Jetty) and away you go.  Well, theoretically anyway...

Since it's a Wicket-based project, all of the HTML files are thrown in with the Java sources and must be on the classpath along with the compiled classes.  The problem is, lately my copy of Eclipse wasn't doing that properly.  Other Wicket projects on my system (using WTP) still seemed to work fine, but the Jetty-based project just didn't want to run.  It kept complaining about not being able to find the markup for a specific page.  This is Wicket's way of telling the developer that they probably forgot a HTML file somewhere.

Now I was able to manually verify that the file did exist in the appropriate directory, named correctly.  It was in an Eclipse source dir which had no exclusion or inclusion rule which would exclude it (either explicitly or implicitly).  In short, it should be on the classpath.  Being savvy to Eclipse's occasional classpath oddities, I fired a quick clean of the project and tried again.  No luck.  My next step was to do a clean checkout, re-gen the project meta files using Maven, and finally build and run.  Once again, no dice.  This was when I decided to spelunke a bit into the actual build output directory for the project.  When in doubt, check it out by hand, right?

It turns out that _none_ of the HTML files for _any_ of the pages were being copied to the target directory.  I found this more than a little odd, since I knew it was working before and (as I said) none of the source filters should have excluded anything.  So I went in and changed the filters so that _only_ **/*.html files should be included in the build.  The result?  An empty target directory.  Not encouraging.

I tried copying over configurations, editing the _.classpath_ file directly, even looking at the help documentation for JDT, still no luck.  It was the first problem I've had with Eclipse in years which I haven't been able to solve reasonably quickly or work-around.  In short, I was stuck.

Out of sheer desperation, I started browsing through some of the Eclipse JDT and WTP preferences.  I figured that WTP had to have _something_ to do with all of this, since the builder was obviously treating HTML files in a special way and WTP is the only plugin I have installed which might do that.  I came up empty on the WTP front, but when looking through the JDT builder prefs, I found this nugget:

![wtp-screwup](/assets/images/blog/wp-content/uploads/2007/10/wtp-screwup.png)

What?!  I honestly cannot think of _any_ valid reason why I would want those resources excluded.  WTP doesn't require it.  After removing these two lines (SVG files probably should be included in the build too), all of my WTP projects still run fine.  In fact, since this preference was obviously added by WTP, it got me thinking: all of this worked fine not two days ago, what happened?

The answer is: I updated WTP.  It seems the latest update of Eclipse WTP adds this preference into your _JDT_ settings whether you want it or not.  Not only that, but there seems to be no warnings, no documentation of any kind which would indicate its purpose or that it even made the change!  I ask you: _why_ was this necessary?

I lost literally hours of time trying to track this down.  Granted, if I was more familiar with the "Output folder" preferences in the JDT compiler settings, maybe I would have figured it out sooner.  But the point is not my less-than-perfect familiarity, the point is that this change seems to require the developer to have advanced knowledge of the Eclipse preference system, just to track down an apparent bug in their own project config.  Bad move, WTP, very bad.

...now that the flames have died down: what is the purpose behind the exclusion of these resources anyway?  If they don't hurt anything in a WTP project, and they certainly wouldn't mess with anything in a Java project, why exclude them?  Also, in all fairness I really don't know for _certain_ that it was WTP that made the change.  I updated the entire Europa train at the same time.  Theoretically, any one of the projects could have made the undesirable modification.  WTP just seemed the most likely since one of the exclusions was *.html, though PDT would be an equally valid guess.