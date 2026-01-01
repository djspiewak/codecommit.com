{%
laika.title = "OpenID Prefiller Greasemonkey Script"
laika.metadata.date = "2007-08-17"
%}


# OpenID Prefiller Greasemonkey Script

[OpenID](<http://www.openid.net>) is really taking off.  We're seeing more and more sites which offer OpenID login in addition to the standard create/sign-in login system.  Still more sites are offering to be an OpenID provider (such as AOL and LiveJournal).  Distributed single sign-on for the web is really a compelling concept, and I can see why it's becoming so popular.  However, OpenID does have it's issues.

Obviously, there are questions about login security, since OpenID just authenticates you, it doesn't ensure you aren't a spam-bot or similar.  But the big issue for me is (ironically) how much more typing it costs.  Think about which is easier: to type a username and a password (a pair which you probably type several times a day), or a long, unwieldy URL?  Personally, I find it takes much longer to type the URL than the username/password, and this of course cuts into my workflow and interrupts my train of thought.

The ultimate solution would be if my browser could prefill my OpenID into any OpenID login form elements on the page.  This doesn't have the issues that the Password Manager in Firefox has, because I don't really care if everyone knows what my OpenID is; they can't use it anyway.  (one of the many wonderful things about OpenID)  Now as I understand it, this feature is coming in Firefox 3.0, but I want something that will solve my problems _now._

Enter Greasemonkey.  If you haven't already installed this extension, you really should do so.  You can do _tons_ of stuff with the right script, like moving or removing elements around the page, or adding functionality to Gmail, or even reverting recent interface changes made at [DZone](<http://www.dzone.com>).  This extension really is a must-have for any web power user.  In fact, this extension is exactly what we need to solve our little OpenID prefilling problem.

Since a Greasemonkey script by definition allows us to locally modify elements of pages as they load, we can create a script which will look for OpenID text fields in the loading page, and if found, pre-populate them with a given value (in this case, our OpenID).  This of course depends on all OpenID text fields having certain attributes in common, but thankfully this is the case.  It just so happens that every OpenID text field I've ever found has the word "openid" somewhere in either the element _id,_ or somewhere in the _name.   _Using this bit of information, we can construct a script to search for form elements matching these criterion:

```javascript
// ==UserScript==
// @name           OpenID Prefiller
// @namespace      codecommit.com
// @description    Prefills my openid into any openid text field on the page
// @include        *
// ==/UserScript==

var OPENID = 'openid.danielspiewak.org';    // change this to set your openid

var all = document.getElementsByTagName('input');

for (var i = 0; i < all.length; i++) {
    var current = all[i];
    
    if (current.type == 'text'
        && (current.id.indexOf('openid') >= 0
            || current.name.indexOf('openid') >= 0)
            || current.id.indexOf('openId') >= 0
            || current.name.indexOf('openId') >= 0) {
        current.value = OPENID;
    }
}
```

As you can see, all this does is search for _input type="text"_ elements with "openid" or "openId" in the _id_ or the _name_ attributes.  If it finds such a field, it sets its value to the OpenID we've hard-coded into the script and moves on.  Simple, yet effective.

To use this script yourself, simply install it into Greasemonkey and set it to run on all sites.  Open up the script, and change the _OPENID_ String value to your own OpenID.  Save the script, and you're on your way!

![image](/assets/images/blog/wp-content/uploads/2007/08/image.png)

So far, I've tried this script on several dozen sites and it's worked perfectly so far.  If you like, there's an (incomplete) [OpenID site directory](<https://www.myopenid.com/directory>) available, listing sites upon which you can try this.  Actually, the only site I've found with which this script _doesn't_ work is [DZone](<http://www.dzone.com>).  This is because DZone does some weird, lightbox pre-population of the login div, and thus isn't modifiable by the Greasemonkey script.  (interestingly enough, other sites which do use lightbox logins _do_ work with this script.  DZone is the only one which doesn't)