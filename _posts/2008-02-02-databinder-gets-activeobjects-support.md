---
categories:
- Java
date: '2008-02-02 10:27:06 '
layout: post
title: Databinder Gets ActiveObjects Support
wordpress_id: 185
wordpress_path: /java/databinder-gets-activeobjects-support
---

[Nathan Hamblen](<http://technically.us/code>) (otherwise known as **n8han** from Coderspiel) has been putting a lot of work recently into his persistence interop framework, [Databinder](<http://databinder.net>).  Interestingly enough, some of this work has involved ActiveObjects.  Nathan has taken some of the code I did for the wicket-activeobjects module, adapted it to Databinder and enhanced it 10 fold.  As of right now in the Databinder SVN, it is possible to achieve complicated interaction between your Wicket views and the ActiveObjects database model without any undo hassle. ([announcement](<http://databinder.net/forum/viewtopic.php?f=1&t=259>))

For those of you who don't know, Databinder can be thought of as a compatibility layer between Wicket and Hibernate.  The idea being to smooth some of the rough spots that can lead to extensive boiler plate when using Hibernate and Wicket together for an application.  Yes, I know there's a wicket-extras project which has the same goal, but in my opinion, Databinder does it better.  Databinder acts as a completely natural layer between Wicket and Hibernate, _not_ between you and either of the two frameworks.  This means that you can write code naturally which uses Wicket and write vanilla-standard Hibernate code without any framework weirdness introduced by Databinder.  In short, the framework does precisely what it needs to when it needs to and stays out of the way for the rest of the time.

Anyway, there's not much more I can say about the framework, it really [speaks for itself](<http://databinder.net/site/show/about-examples>).  I'll I can tell you is that if you're using Wicket and ActiveObjects together _without_ Databinder, you're really missing out.  ActiveObjects integration is included in the unreleased version 1.2, currently only obtainable directly from the SVN.  So fire up your SVN client ( _svn://databinder.net/databinder/trunk_ ), run off a Maven build and get hacking!