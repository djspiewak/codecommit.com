---
categories:
- Java
date: '2008-08-18 00:00:00 '
layout: post
title: How Do You Apply Polyglotism?
wordpress_id: 254
wordpress_path: /java/how-do-you-apply-polyglotism
---

For the past two years or so, there has been an increasing meme across the developer blogosphere encouraging the application of the polyglot methodology. For those of you who have been living under a rock, the idea behind polyglot programming is that each section of a given project should use whatever language happens to be most applicable to the problem in question. This makes for a great topic for arm-chair bloggers, leading to endless pontification and flame-wars on forum after forum, but it seems to be a bit more difficult to apply in the real world.

The fact is that very few companies are open to the idea of diversity in language selection. Just look at Google, one of the most open-minded and developer-friendly companies around. They employ some of the smartest people I know, programmers who have actually _invented_ languages with wide-scale adoption. However, this same company mandates the use of a very small set of languages including Python, Java, C++ and JavaScript. If a company like Google can't even bring itself to dabble in language diversity, what hope do we have for the Apples of the world?

A few months ago, I received an internal email from the startup company where I work. This email was putting forth a new policy which would restrict all future developments to one of two languages: PHP or Java. In fact, this policy went on to push for the eventual rewrite of all legacy projects which had been written in other languages including Objective-C, Ruby, Python and a fair number of shell scripts. I was utterly flabbergasted (to say the least). A few swift emails later, we were able to come to a more moderate position, but the prevailing attitude remains extremely focused on minimizing the choice of languages.

To my knowledge, this sort of policy is fairly common in the industry. Companies (particularly those employing consultants) seem to prefer to keep the technologies employed to a minimum, focusing on the least-common denominator so as to reduce the requirements for incoming developer skill sets. This is rather distressing to me, because I get a great deal of pleasure out of solving problems differently using alternative languages. For example, I would have loved to build the clustering system at my company using the highly-scalable actor model with Scala, but the idea was shot down right out of the gate because it involved a non-mainstream language. To be fair to my colleagues, the overall design involved was given more serious consideration, but it was always within the confines of Java, rather than the original actor-driven concept.

There is actually another aspect to this question: assuming you are _allowed_ to use a variety of languages to "get the job done", how do you apply them? Ola Bini has talked about the various layers of a system, but this is harder to see in practice than it would seem. How do you define where to "draw the line" between using Java and Scala, or even the more dramatic differences between Java and JRuby or Groovy? Of course, we can base our decision strictly on lines of code, but in that case, Scala would trump Java every time. For that matter, Ruby would probably beat out the two of them, and I'm certainly not writing my next large-scale enterprise app exclusively in a dynamic language.

I realize this is somewhat of a cop-out post, just asking a question and never arriving at a satisfactory conclusion, but I would really like to know how other developers approach this issue. What criteria do you weigh in making the decision to go with a particular language? What sorts of languages work well for which tasks? And above all, how do you convince your boss that this is the right way to go? The floor is open, please enlighten me! :-)