{%
laika.title = "ActiveObjects 0.6 Released"
laika.metadata.date = "2007-10-30"
%}


# ActiveObjects 0.6 Released

As a minor side-bar in this (hopefully) noise-less blog, I'd like to announce the release of ActiveObjects 0.6.  If you could care less about ActiveObjects and/or random announcements about it, please feel free to completely ignore this post.

ActiveObjects 0.6 is the most stable release yet (hopefully).  With this release, we see the rise of _RawEntity_ , a superinterface to _Entity_ which allows for greater customization, particularly in the area of primary keys.  Most developers will never need to even be aware of this interface, but for those that have such requirements, it should be very helpful.  Likewise, this release also allows for arbitrary types to be persisted into the database, through the use of custom classes which manage the mapping between Java type and database type.  ( **hint** : this even allows for database-specific types such as PostgreSQL's MATRIX if you really want them)

Most importantly, 0.6 is the release where I actually buckled down and started writing some documentation.  What's available on the project page right now is still a little sparse, but rest assured this will be rectified soon (not sure when, but soon).  The main focus for the moment has been javadocing the public API.  This is far from complete at the moment, but all the important (and lengthy) classes are done (specifically, everything in the _net.java.ao_ package).  With this documentation, it should hopefully be somewhat easier to use ActiveObjects in a project without resorting to desperate Google searches at the wee hours of the morning.

Most of the interesting stuff in this release I've already covered in [other posts](</blog/tag/activeobjects>) on this blog, so I won't bore you by repeating all of it.  Suffice it to say, if you've been waiting for a more stable release to start playing with ActiveObjects, this is it.  I won't guarantee that the API won't change at all leading up to version 1.0, but I can say that most of the earth-shattering stuff is behind us.  Documentation is in place, and we've got a large (and growing) number of tests which are run to ensure quality and stability in the core functionality.  Download it, try it out, break it, file bugs, you know the drill.  I welcome all suggestions, comments, questions and pro-Hibernate rants.

[Download activeobjects-0.6 from java.net](<https://activeobjects.dev.java.net>)