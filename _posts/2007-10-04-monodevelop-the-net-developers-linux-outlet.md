---
categories:
- .NET
date: '2007-10-04 10:09:23 '
layout: post
title: 'MonoDevelop: The .NET Developer''s Linux Outlet'
wordpress_id: 133
wordpress_path: /net/monodevelop-the-net-developers-linux-outlet
---

I've done my fair share of .NET development.  I've never actually enjoyed it, nor would I want to make a living out of it, but I have done some.  Every time, I've been _forced_ to work on Windows to do any serious project.  Granted, jEdit can get you awfully far in terms of source editing, but unfortunately (?) it's no IDE.  Really, the only way to do serious .NET development is to use VisualStudio.

Now, for a number of reasons (none of which are important right now), I'm already using Windows as my primary OS.  However, I don't like being boxed into one OS or another.  I try to keep my options open.  If I ever could cut those final ties to Windows, I'd love to switch to Linux or Mac.  Also, I just don't like feeling forced to do something in a certain way.  With Java, I can write the code in Eclipse, NetBeans, jEdit or Notepad for all my employer cares, just as long as it gets done.  With .NET, I really don't have any choice but to use Windows.

Well, until nowish.  MonoDevelop recently announced the release of [1.0 beta 1](<http://www.monodevelop.com/Release_notes_for_MonoDevelop_1.0_Beta_1>).  From what I've read, things are still comparatively unstable, but the features are all there and bug fixing is proceeding apace.  Also, for the first time it seems that they're offering some binary packages, allowing users to install easily rather than wrestling with the sources for hours and hours (which is what happened to me last time I tried MonoDevelop).

![monodevelop](/assets/images/blog/wp-content/uploads/2007/10/monodevelop.png) Actually, the bigger news for me is the addition of all of the "serious coding" features.  Things like content assist, searching, error-underlining, etc.  These are _huge_ when working on a non-trivial project.  In fact, these are precisely the reason I tied myself to Windows and VisualStudio for .NET development rather than just using jEdit or VIM on Linux.  Last time I tried MonoDevelop (back in like, 0.2), it really wasn't more than a glorified text editor with syntax highlighting.  Now, it's a full-fledged IDE.

As far as I'm concerned, MonoDevelop has reached the point where it can be considered as a serious VisualStudio on Linux.  In fact, from what I've seen it's at a level where .NET developers need no longer consider themselves tied to Windows just for the tools.

Of course, the big problem is MonoDevelop is a tool to write code that runs in _Mono_ (hence the name), not really .NET.  Technically, the two platforms are very very close, but .NET has some libraries and provides certain functionality that Mono just doesn't emulate yet (things like the win32 API).  Also, Mono _is_ a black-box port, so there are bound to be some inconsistencies in behavior here and there.  As a result, you can probably write your .NET application on Linux using Mono, but you had better test it running on Windows and the CLR.  Otherwise you can never really be sure that your app is doing what you want it to on Windows.

But on the whole, I think this is great news!  MonoDevelop gives .NET developers a nice (and free) alternative to VisualStudio, not to mention the benefit of unfettering these developers from the Windows platform.  Just one more way to thumb your nose at the boys in Redmond and support FOSS.

**Update:** The How-To Geek has some [excellent instructions](<http://www.howtogeek.com/howto/linux/installing-monodevelop-from-source-on-ubuntu/>) on how to install the latest version of MonoDevelop on Ubuntu.