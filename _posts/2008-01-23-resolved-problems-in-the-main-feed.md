---
categories:
- Scripts
date: '2008-01-23 19:55:29 '
layout: post
title: '[RESOLVED] Problems in the Main Feed'
wordpress_id: 182
wordpress_path: /scripts/resolved-problems-in-the-main-feed
---

To those who were affected by the recent problems in the RSS, you have my sincere apologies.  It seems that the WP-SuperCache plugin doesn't work nicely with PHP in safe mode (thanks to [the How-To Geek](<http://www.howtogeek.com>) for turning me on to the solution).  Since MediaTemple switched it's GS service over to PHP in safe mode recently, there wasn't much of a precedent to go off of when debugging.

Anyway, the supercache has been disabled (just relying on wp-cache now) and the feed seems to have been stable for a while now.  Once again, I apologize for force-feeding you invalid XML!