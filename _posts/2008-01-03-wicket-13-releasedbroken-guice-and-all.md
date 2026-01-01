---
categories:
- Java
date: '2008-01-03 01:00:33 '
layout: post
title: Wicket 1.3 Released...Broken Guice and All
wordpress_id: 174
wordpress_path: /java/wicket-13-releasedbroken-guice-and-all
---

I usually try to avoid such negative topics, but this time I really couldn't help myself.  Once in a while something in the "current events" section of the blogosphere will bug me enough to merit a slam post.  The "support" for Google Guice in the latest stable release of Wicket is one of those things...

To start with, the good news: Martijn Dashorst [has announced](<http://martijndashorst.com/blog/2008/01/02/apache-wicket-13-released/>) the release of [Apache Wicket 1.3](<http://wicket.apache.org>)!  This really is a great release all-around and the guys in the band deserve a round of applause.  This release fixes the number one "bug" with Wicket: it's rather odd package namespace.  ( _wicket.*_ )  :-)  Welcome to the land of happy packages and tired fingers.

Wicket is really starting (or just proceeding at an accelerated rate) to feel like a rock solid, production-ready framework.  I've used it quite a bit over the last few years, and I'll say flat-out that I don't think any framework matches it for productivity and maintainability (that includes Rails, dynlang notwithstanding).

One of the new features in 1.3 (important enough to merit inclusion in Martijn's 20-odd points) is support for [Google Guice](<http://code.google.com/p/google-guice/>) dependency injection.  This is a huge deal for those of us who have nominated Guice for the "cleverest framework of the decade" award.  Support for Guice in Wicket makes it possible to utilize dependency injection right in your page classes (where it's most needed).  Wicket has had similar support for Spring for a while now, but it was only recently that [Al Maw](<http://herebebeasties.com/>) got the chance to refactor the guts out into the wicket-ioc project and thus enable support for alternative DI frameworks like Guice.

This all seems well and good, but unfortunately Wicket's Guice support is not quite up to par with the rest of the framework.  I tried the support a while back in beta4 and ran headlong into a fairly serious problem.  The following code doesn't work:

```java
public class MyModule extends AbstractModule {
    // ...

    @Override
    public void configure() {
        EntityManager manager = new EntityManager(uri, username, password);
        bind(EntityManager.class).toInstance(manager);
    }
}
```

This is fairly standard Guice configuration code.  All that's happening here is I'm binding all injected fields of type _EntityManager_ to a given instance.  Of course the classic use of DI is to have it instantiate the injected values based on classname (or class literal in Guice's case).  However, this panacea of IOC breaks down when working with classes which lack default constructors (like _EntityManager_ ).  This is why Guice enables developers to bind classes to instances (as I'm doing above).  The problem is this code will crash when executed using wicket-guice.

I opened an issue in the Wicket JIRA back in November when I first identified the bug ([WICKET-1130](<https://issues.apache.org/jira/browse/WICKET-1130>)).  I even included some simple example code I could use to repeat the problem!  Since then the issue has been reassigned and bumped back in fix version twice, all without any word on if the problem is being looked at or how soon I could expect a solution.  Now I know the Wicket devs are busy and all with tons of more sweeping issues and last-minute polish for the 1.3 release, but this is pretty absurd.

Honestly, if this were a trivial edge-case that only effected me and my neighbor's cow, I wouldn't put up much of a fuss.  But the fact is, this is something so broad and repeatable that it will touch just about anyone who seriously uses Guice with Wicket.  Even if there wasn't time to _fix_ the problem before the 1.3 release, I would hope there would be some sort of prominent notice ("KNOWNBUGS" anyone?) included with the distributable.  Unfortunately the only reference to the problem I was able to find on the Wicket site was an [obscure wiki article](<http://cwiki.apache.org/WICKET/guice-integration-pitfall.html>) (well written though) done by [Uwe Schäfer](<http://www.codesmell.org/confluence/display/~uwe/Another+useless+Blog%2C+virtually+noone+is+going+to+read....>).  All this entry serves to do is further aggravate me since it means someone else has run headlong into this problem and been annoyed by it enough to write an article (still without receiving response from the Wicket core devs).

Uwe does propose a workaround (add a protected no-arg constructor to the injected class), but that's impractical for my use case (and if I ran into it, you can bet your boots half a dozen other people did too).  I'm certainly not going to randomly add broken constructors to the [ActiveObjects](<https://activeobjects.dev.java.net>) API, and I wouldn't even have the option to _consider_ doing so except for the fact that I'm the developer on the class I was trying to bind.  If I was trying to bind an instance from a third-party library, I'd be out of luck completely.

I'm very disappointed in the Wicket project for dropping the ball on this issue.  I really have the utmost respect for those guys, which is why it's so surprising to see something like this happen.  As it stands, WICKET-1130 is slated for 1.3.1, but given its track record of reassignment I'm not holding my breath.  Hopefully this posting will serve as a more prominent warning for those considering using Wicket and Guice together in their project.

  * [WICKET-1130 in the Apache JIRA](<https://issues.apache.org/jira/browse/WICKET-1130>)