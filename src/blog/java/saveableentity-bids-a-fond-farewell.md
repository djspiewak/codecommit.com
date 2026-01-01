{%
laika.title = "SaveableEntity Bids a Fond Farewell"
laika.metadata.date = "2007-08-15"
%}


# SaveableEntity Bids a Fond Farewell

Well, to make a small, side entry out of something which probably should be in bold print on the [ActiveObjects](<http://activeobjects.dev.java.net>) website...  It's worth announcing that I've merged SaveableEntity into the Entity super-interface.  The only reason to keep these two separate was so that some entities could be configured to receive calls to setters and immediately execute UPDATE statements.  This is a really inefficient way to code your database model and I think the only real use of it was in my sample code.  :-)  Since it really was an API misstep, I've decided to do away with it.  The _save()_ method is now obligatory for any data modification.  Thus, any legacy code you may have which extended Entity may not function in the way you would expect (e.g. the DogfoodBlog example no longer persists data properly).  If you have any code which extended SaveableEntity, just change this to extend the Entity interface and everything should work as before.  Just thought I'd make a general announcement.