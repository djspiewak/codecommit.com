{%
laika.title = "ActiveObjects 0.6.1 Bug Fix Release"
laika.metadata.date = "2007-11-07"
%}


# ActiveObjects 0.6.1 Bug Fix Release

Well, I did it again.  I pushed out 0.6 with a critical (and fairly obvious) bug.  Basically, it involved the way I was handling column names and MySQL in result sets.  Thus, 0.6 probably won't work with the 5.1 version of the MySQL JDBC connector.  :-S  My bad.

Anyway I've fixed the bug (thanks to Zach Cox) and included the fix in a minor release on the site.  So if you're interested in trying ActiveObjects, you really should use the (now available) 0.6.1 release rather than 0.6.  Enjoy!