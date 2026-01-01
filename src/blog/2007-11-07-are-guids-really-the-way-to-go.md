{%
laika.title = "Are GUIDs Really the Way to Go?"
laika.metadata.date = "2007-11-07"
%}


# Are GUIDs Really the Way to Go?

I recently read a (slightly inflammatory) posting entitled ["The Gospel of the GUID"](<http://weblogs.asp.net/wwright/archive/2007/11/04/the-gospel-of-the-guid-and-why-it-matters.aspx>).  In it, the author attempts to put forward several arguments in favor of using [GUIDs](<http://en.wikipedia.org/wiki/GUID>) for all database primary keys (as opposed to the more pedestrian sequence-generated INTEGER).  I've heard similar arguments in the past, and I keep coming back to the same conclusion: it's all bunk.

To get right to the heart of my opinion, I really don't see a compelling reason to use GUIDs for 90% of database use-cases.  Consider for a moment some of the _really_ large databases in this world (Wikipedia, Slashdot's comments table, Digg, etc).  I can't think of a single web application in that league which uses GUIDs.  If you don't believe me, look at the code for MediaWiki, the URL structure for Digg and the idiocy involving INTEGER vs BIGINT on Slashdot.  While _de facto_ practice may not dictate optimal design, it does point to a trend that's worthy of notice.  More importantly, it proves that INTEGER (or rather, BIGINT) primary key fields are practicable under real-world stress.  But what about the theoretical use-case?

Well to be honest, the theoretical use-case doesn't interest me all that much.  I mean, if you tell me that your schema can support theoretically 29 trillion rows in its users table, I'll geek out right along with you.  But if you try to feed that to a client, you'll get either blank stares, or the equally common: "but there are only 5 billion people to chose from."  (unless you're talking to someone in upper management)  For an even better party, try telling your client that you can merge their data with one of their competitor's in 45 minutes flat.  Somehow I doubt that argument will fly.

Now let's look at the cons involved.  GUIDs are really, _really_ hard to remember and type by hand.  When you're in a hurry, pulling a little data-mine-while-you-wait for your boss, you're not going to want to dig around and find that string you copy/pasted into Notepad.  Oh, and heaven forbid you actually type the GUID wrong!  Finding a single-bit deviation in a 64-bit alpha-numeric is not a trivial task let me tell you.  It's at about this point that you start to think that maybe INTEGER keys would have been a better way to go.  At least then you can throw the userID into good ol' short-term memory and bang it out ten seconds later in your SQL query window.  Oh, in case you've never seen a GUID before, here's a couple quick examples:

  * **{3F2504E0-4F89-11D3-9A0C-0305E82C3301}**
  * **{52335ABA-2D8B-4892-A8B7-86B817AAC607}**
  * **{79E9F560-FD70-4807-BEED-50A87AA911B1}**

I'm not even sure how to _read_ that, much less remember it.

Another thing worth considering is that GUIDs are VARCHARs within the database.  This means that not all ORMs will handle them appropriately.  Hibernate will, as will iBatis and [ActiveObjects](<https://activeobjects.dev.java.net>).  But if you're using something like Django or ActiveRecord, you're out of luck (disclaimer: Django might actually support VARCHAR primary keys, I'm not sure).  _On top of that_ , some database clustering techniques don't seem to work nicely when applied to GUID-based key schemes.  I haven't run into this myself, but my good friend [Lowell Heddings](<http://www.lowellheddings.com>) has told me tales of strange mystics and evil tides when playing with distributed indexes and GUIDs in MS SQL Server.  Technically speaking, while any sane database may _work_ with a VARCHAR for the table's primary key, the architects really had INTEGERs in mind when they designed the algorithms.  This means that by using GUIDs, you're flirting with a less-well-tested use-case.  It's certainly not unsupported, but you should consider yourself in the minority of users if you go that route.

Oh, and one little myth I'd like to debug: there really isn't that much more overhead in grabbing the generated value of an INTEGER primary key than in sending an in-code generated GUID value.  Granted, some databases make this harder than others, but that's their fault isn't it?  As long as you're using a well designed ORM, you really won't see much of a performance increase from removing that miniscule callback.  In fact, depending on how your GUID algorithm works, you may see a significant _degradation_ in overall performance due to extra overhead in your application.

### Conclusion

So, the obligatory thirty second overview for our attention-deprived era:

**Benefits of GUIDs** | perfectly unique across all your data and tables; way, way more head-room in terms of values  
---|---  
**Problems with GUIDs** | really, really un-developer friendly; weird side-effects in databases and ORMs; unconventional approach to a "solved" problem; easy way to upset your DBA  
  
So it really seems the only upshot for GUIDs is their massive range.  In trade-off, you lose usability from a raw SQL standpoint, your result sets are that much more cluttered and you risk odd bugs in your persistence backend.  Hmm, I wonder which one I'm going to use in my next app?

Before you make the choice, ask yourself: "how much uniqueness do I really require?"  Over-designing a solution is just as much a problem as under-designing one.  Try to find the mid-point that works best for your use-case.