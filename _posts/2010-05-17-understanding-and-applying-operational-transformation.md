---
categories:
- Java
date: '2010-05-17 00:00:00 '
layout: post
title: Understanding and Applying Operational Transformation
wordpress_id: 302
wordpress_path: /java/understanding-and-applying-operational-transformation
---

Almost exactly a year ago, Google made one of the most remarkable press releases in the Web 2.0 era. Of course, by "press release", I actually mean keynote at their own conference, and by "remarkable" I mean potentially-transformative and groundbreaking. I am referring of course to the announcement of [Google Wave](<http://wave.google.com>), a real-time collaboration tool which has been in open beta for the last several months.

For those of you who don't know, Google Wave is a collaboration tool based on real-time, simultaneous editing of documents via a mechanism known as "operational transformation". Entities which appear as messages in the Wave client are actually "waves". Within each "wave" is a set of "wavelets", each of which contains a set of documents. Individual documents can represent things like messages, conversation structure (which reply goes where, etc), spell check metadata and so on. Documents are composed of well-formed XML with an implicit root node. Additionally, they carry special metadata known as "annotations" which are (potentially-overlapping) key/value ranges which span across specific regions of the document. In the Wave message schema, annotations are used to represent things like bold/italic/underline/strikethrough formatting, links, caret position, the conversation title and a host of other things. An example document following the Wave message schema might look something like this:

```  Test message Lorem ipsum dolor sit amet.  ``` 

(assuming the following annotations):

  * style/font-weight -> bold
  * style/font-style -> italic
  * link/manual -> http://www.google.com

You will notice that the annotations for `style/font-style` and `link/manual` actually overlap. This is perfectly acceptable in Wave's document schema. The resulting rendering would be something like this:

**Test message**

_Lorem_[ _ipsum_ dolor](<http://www.google.com>) sit amet.

The point of all this explaining is to give you at least a passing familiarity with the Wave document schema so that I can safely use its terminology in the article to come. See, Wave itself is not nearly so interesting as the idea upon which it is based. As mentioned, every document in Wave is actually just raw XML with some ancillary annotations. As far as the Wave server is concerned, you can stuff whatever data you want in there, just so long as it's well-formed. It just so happens that Google chose to implement a communications tool on top of this data backend, but they could have just as easily implemented something more esoteric, like a database or a windowing manager.

The key to Wave is the mechanism by which we interact with these documents: [operational transformation](<http://en.wikipedia.org/wiki/Operational_transformation>). Wave actually doesn't allow you to get access to a document as raw XML or anything even approaching it. Instead, it demands that all of your access to the document be performed in terms of operations. This has two consequences: first, it allows for some really incredible collaborative tools like the Wave client; second, it makes it _really_ tricky to implement any sort of Wave-compatible service. Given the fact that I've been working on Novell Pulse (which is exactly this sort of service), and in light of the fact that Google's documentation on the subject is sparing at best, I thought I would take some time to clarify this critical piece of the puzzle. Hopefully, the information I'm about to present will make it easier for others attempting to interoperate with Wave, Pulse and the (hopefully) many OT-based systems yet to come.

### Operations

Intuitively enough, the fundamental building block of operational transforms are operations themselves. An operation is exactly what it sounds like: an action which is to be performed on a document. This action could be inserting or deleting characters, opening (and closing!) an XML element, fiddling with annotations, etc. A single operation may actually perform many of these actions. Thus, an operation is actually made up of a sequence of operation _components_ , each of which performs a particular action with respect to the _cursor_ (not to be confused with the _caret_ , which is specific to the client editor and not at all interesting at the level of OT).

There are a number of possible component types. For example:

  * insertCharacters — Inserts the specified string at the current index
  * deleteCharacters — Deletes the specified string from the current index
  * openElement — Creates a new XML open-tag at the current index
  * deleteOpenElement — Deletes the specified XML open-tag from the current index
  * closeElement — Closes the first currently-open tag at the current index
  * deleteCloseElement — Deletes the XML close-tag at the current index
  * annotationBoundary — Defines the _changes_ to any annotations (starting or ending) at the current index
  * retain — Advances the index a specified number of items

Wave's OT implementation actually has even more component types, but these are the important ones. You'll notice that every component has something to do with the cursor index. This concept is central to Wave's OT implementation. Operations are effectively a stream of components, each of which defines an action to be performed which effects the content, the cursor or both. For example, we can encode the example document from earlier as follows:

  1. openElement(`'body'`)
  2. openElement(`'line'`)
  3. closeElement()
  4. annotationBoundary(`startKeys: ['style/font-weight']`, `startValues: ['bold']`)
  5. insertCharacters(`'Test message'`)
  6. annotationBoundary(`endKeys: ['style/font-weight']`)
  7. openElement(`'line'`)
  8. closeElement()
  9. annotationBoundary(`startKeys: ['style/font-style']`, `startValues: ['italic']`)
  10. openElement(`'line'`)
  11. closeElement()
  12. insertCharacters(`'Lorem '`)
  13. annotationBoundary(`startKeys: ['link/manual']`, `startValues: ['http://www.google.com']`)
  14. insertCharacters(`'ipsum'`)
  15. annotationBoundary(`endKeys: ['style/font-style']`)
  16. insertCharacters(`' dolor'`)
  17. annotationBoundary(`endKeys: ['link/manual']`)
  18. insertCharacters(`' sit amet.'`)
  19. closeElement()

Obviously, this isn't the most streamlined way of referring to a document's content for a human, but a stream of discrete components like this is _perfect_ for automated processing. The real utility of this encoding though doesn't become apparent until we look at operations which only encode a partial document; effectively performing a particular mutation. For example, let's follow the advice of _Strunk and White_ and capitalize the letter 'm' in our title of 'Test message'. What we want to do (precisely-speaking) is delete the 'm' and insert the string 'M' at its previous location. We can do that with the following operation:

  1. retain(`8`)
  2. deleteCharacters(`'m'`)
  3. insertCharacters(`'M'`)
  4. retain(`38`)

Instead of adding content to the document at ever step, most of this operation actually leaves the underlying document untouched. In practice, retain() tends to be the most commonly used component by a wide margin. The trick is that every operation _must_ span the full width of the document. When evaluating this operation, the cursor will start at index 0 and walk forward through the existing document and the incoming operation one item at a time. Each XML tag (open or close) counts as a single item. Characters are also single items. Thus, the entire document contains 47 items.

Our operation above cursors harmlessly over the first eight items (the `<body>` tag, the `<line/>` tag and the string `'Test '`). Once it reaches the `'m'` in `'message'`, we stop the cursor and perform a mutation. Specifically, we're using the deleteCharacters() component to remove the `'m'`. This component doesn't move the cursor, so we're still sitting at index `8`. We then use the insertCharacters() component to add the character `'M'` at precisely our currently location. This time, some new characters have been inserted, so the cursor advances to the end of the newly-inserted string (meaning that we are now at index `9`). This is intuitive because we don't want to have to retain() over the text we just inserted. We do however want to retain() over the remainder of the document, seeing as we don't need to do anything else. The final rendered document looks like the following:

**Test Message**

_Lorem_[ _ipsum_ dolor](<http://www.google.com>) sit amet.

### Composition

One of Google's contributions to the (very old) theory behind operational transformation is the idea of operation composition. Because Wave operations are these nice, full-span sequences of discrete components, it's fairly easy to take two operations which span the same length and merge them together into a single operation. The results of this action are really quite intuitive. For example, if we were to compose our document operation (the first example above) with our `'m'`-changing operation (the second example), the resulting operation would be basically the same as the original document operation, except that instead of inserting the text `'Test message'`, we would insert `'Test Message'`. In composing the two operations together, all of the retains have disappeared and any contradicting components (e.g. a delete and an insert) have been directly merged.

Composition is extremely important to Wave's OT as we will see once we start looking at client/server asymmetry. The important thing to notice now is the fact that composed operations _must_ be fundamentally compatible. Primarily, this means that the two operations must span the same number of indexes. It also means that we cannot compose an operation which consists of only a text insert with an operation which attempts to delete an XML element. Obviously, that's not going to work. Wave's `Composer` utility takes care of validating both the left and the right operation to ensure that they are compatible as part of the composition process.

Please also note that composition is _not_ commutative; ordering is significant. This is also quite intuitive. If you type the character `a` and _then_ type the character `b`, the result is quite different than if you type the character `b` and _then_ type the character `a`.

### Transformation

Here's where we get to some of the really interesting stuff and the motivation behind all of this convoluted representational baggage. Operational Transformation, at its core, is an _optimistic_ concurrency control mechanism. It allows two editors to modify the same section of a document at the same time without conflict. Or rather, it provides a mechanism for sanely resolving those conflicts so that neither user intervention nor locking become necessary.

This is actually a harder problem than it sounds. Imagine that we have the following document (represented as an operation):

  1. insertCharacters(`'go'`) 

Now imagine that we have two editors with their cursors positioned at the end of the document. They _simultaneously_ insert a `t` and `a` character (respectively). Thus, we will have two operations sent to the server. The first will retain 2 items and insert a `t`, the second will retain 2 items and insert `a`. Naturally, the server needs to enforce atomicity of edits at some point (to avoid race conditions during I/O), so one of these operations will be applied first. However, as soon as either one of these operations is applied, the retain for the other will become invalid. Depending on the ordering, the text of the resulting document will either be `'goat'` or `'gota'`.

In and of itself, this isn't really a problem. After all, any asynchronous server needs to make decisions about ordering at some point. However, issues start to crop up as soon as we consider relaying operations from one client to the other. Client A has already applied its operation, so its document text will be `'got'`. Meanwhile, client B has already applied _its_ operation, and so its document text is `'goa'`. Each client needs the operation from the other in order to have any chance of converging to the same document state.

Unfortunately, if we naïvely send A's operation to B and B's operation to A, the results will _not_ converge:

  * `'got'` \+ (retain(`2`); insertCharacters(`'a'`) = `'goat'`
  * `'goa'` \+ (retain(`2`); insertCharacters(`'t'`) = `'gota'`

Even discounting the fact that we have a document size mismatch (our operations each span 2 indexes, while their target documents have width 3), this is obviously not the desired behavior. Even though our server may have a sane concept of consistent ordering, our clients obviously need some extra hand-holding. Enter OT.

What we have here is a simple one-step diamond problem. In the theoretical study of OT, we generally visualize this situation using diagrams like the following:

[![smiz45tGNzAOXq-9cNpzjiw.png](/assets/images/blog/wp-content/uploads/2010/05/smiz45tgnzaoxq-9cnpzjiw.png)](</blog/misc/understanding-and-applying-operational-transformation/One-StepOTDiamondunresolved.svg>)

The way you should read diagrams like this is as a graphical representation of operation application on two documents at the same time. Client operations move the document to the left. Server operations move the document to the right. Both client and server operations move the document downward. Thus, diagrams like these let us visualize the application of operations in a literal "state space". The dark blue line shows the client's path through state space, while the gray line shows the server's. The vertices of these paths (not explicitly rendered) are points in state space, representing a particular state of the document. When both the client and the server line pass through the same point, it means that the content of their respective documents were in sync, at least at that particular point in time.

So, in the diagram above, operation _a_ could be client A's operation (retain(`2`); insertCharacters(`'t'`)) and operation _b_ could be client B's operation. This is of course assuming that the server chose B's operation as the "winner" of the race condition. As we showed earlier, we cannot simply naïvely apply operation _a_ on the server and _b_ on the client, otherwise we could derive differing document states (`'goat'` vs `'gota'`). What we need to do is automatically adjust operation _a_ with respect to _b_ and operation _b_ with respect to _a_.

We can do this using an operational transform. Google's OT is based on the following mathematical identity:

![xform\(a, b\) = \(a', b'\),\\mbox{ where }b' \\circ a \\equiv a' \\circ b](/assets/images/blog/wp-content/uploads/2010/05/ot-identity1.png)

In plain English, this means that the `transform` function takes two operations, one server and one client, and produces a pair of operations. These operations can be applied to their counterpart's end state to produce exactly the same state when complete. Graphically, we can represent this by the following:

[![sldAW1ZXskOrPHbVnvwh8lA.png](/assets/images/blog/wp-content/uploads/2010/05/sldaw1zxskorphbvnvwh8la.png)](</blog/misc/understanding-and-applying-operational-transformation/One-StepOTDiamond\(2\).svg>)

Thus, on the client-side, we receive operation _b_ from the server, pair it with _a_ to produce _(a', b')_ , and then compose _b'_ with _a_ to produce our final document state. We perform an analogous process on the server-side. The mathematical definition of the `transform` function guarantees that this process will produce the _exact_ same document state on both server and client.

Coming back to our concrete example, we can finally solve the problem of `'goat'` vs `'gota'`. We start out with the situation where client A has applied operation _a_ , arriving at a document text of `'got'`. It now receives operation _b_ from the server, instructing it to retain over 2 items and insert character `'a'`. However, before it applies this operation (which would obviously result in the wrong document state), it uses operational transformation to derive operation _b'_. Google's OT implementation will resolve the conflict between `'t'` and `'a'` in favor of the server. Thus, `b'` will consist of the following components:

  1. retain(`2`)
  2. insertCharacters(`'a'`)
  3. retain(`1`)

You will notice that we no longer have a document size mismatch, since that last retain() ensures that the cursor reaches the end of our length-3 document state (`'got'`). 

Meanwhile, the server has received our operation _a_ and it performs an analogous series of steps to derive operation _a'_. Once again, Google's OT must resolve the conflict between `'t'` and `'a'` in the _same_ way as it resolved the conflict for client A. We're trying to apply operation _a_ (which inserts the `'t'` character at position 2) to the server document state, which is currently `'goa'`. When we're done, we must have the exact same document content as client A following the application of _b'_. Specifically, the server document state must be `'goat'`. Thus, the OT process will produce the operation _a'_ consisting of the following components:

  1. retain(`3`)
  2. insertCharacters(`'t'`)

Client A applies operation _b'_ to its document state, the server applies operation _a'_ to its document state, and they _both_ arrive at a document consisting of the text `'goat'`. Magic!

It is very important that you really understand this process. OT is all about the `transform` function and how it behaves in this exact situation. As it turns out, this is _all_ that OT does for us in and of itself. Operational transformation is really just a concurrency primitive. It doesn't solve every problem with collaborative editing of a shared document (as we will see in a moment), but it does solve this problem very well.

One way to think of this is to keep in mind the "diamond" shape shown in the above diagram. OT solves a very simple problem: given the top two sides of the diamond, it can derive the bottom two sides. In practice, often times we only want one side of the box (e.g. client A only needs operation _b'_ , it doesn't need _a'_ ). However, OT _always_ gives us both pieces of the puzzle. It "completes" the diamond, so to speak.

### Compound OT

So far, everything I have presented has come pretty directly from the whitepapers on [waveprotocol.org](<http://www.waveprotocol.org>). However, contrary to popular belief, this is _not_ enough information to actually go out and implement your own collaborative editor or Wave-compatible service.

The problem is that OT doesn't really do all that much in and of itself. As mentioned above, OT solves for two sides of the diamond in state space. It _only_ solves for two sides of a simple, one-step diamond like the one shown above. Let me say it a third time: the case shown above is the _only_ case which OT handles. As it turns out, there are other cases which arise in a client/server collaborative editor like Google Wave or Novell Pulse. In fact, _most_ cases in practice are much more complex than the one-step diamond.

For example, consider the situation where the client performs _two_ operations (say, by typing two characters, one after the other) while at the same time the server performs one operation (originating from another client). We can diagram this situation in the following way:

[![sVkNXT1Hbu9jmjrwnGCCqXA.png](/assets/images/blog/wp-content/uploads/2010/05/svknxt1hbu9jmjrwngccqxa.png)](</blog/misc/understanding-and-applying-operational-transformation/TwoClientOneServerUnresolved.svg>)

So we have two operations in the client history, _a_ and _b_ , and only one operation in the server history, _c_. The client is going to send operations _a_ and _b_ to the server, presumably one after the other. The first operation ( _a_ ) is no problem at all. Here we have the simple one-step diamond problem from above, and as well know, OT has no trouble at all in resolving this issue. The server transforms _a_ and _c_ to derive operation _a'_ , which it applies to its current state. The resulting situation looks like the following:

[![sJkWQr0hTeGxwPpNZgRERLw.png](/assets/images/blog/wp-content/uploads/2010/05/sjkwqr0htegxwppnzgrerlw.png)](</blog/misc/understanding-and-applying-operational-transformation/TwoClientOneServerHalf-Resolved.svg>)

Ok, so far so good. The server has successfully transformed operation _a_ against _c_ and applied the resulting _a'_ to its local state. However, the moment we move on to operation _b_ , disaster strikes. The problem is that the server receives operation _b_ , but it has nothing against which to transform it!

Remember, OT _only_ solves for the bottom two sides of the diamond given the top two sides. In the case of the first operation ( _a_ ), the server had both top sides ( _a_ and _c_ ) and thus OT was able to derive the all-important _a'_. However, in this case, we only have one of the sides of the diamond ( _b_ ); we don't have the server's half of the equation because the server never performed such an operation!

In general, the problem we have here is caused by the client and server diverging by more than one step. Whenever we get into this state, the OT becomes more complicated because we effectively need to transform incoming operations (e.g. _b_ ) against operations which _never happened!_ In this case, the phantom operation that we need for the purposes of OT would take us from the tail end of _a_ to the tail end of _a'_. Think of it like a "bridge" between client state space and server state space. We need this bridge, this second half of the diamond, if we are to apply OT to solve the problem of transforming _b_ into server state space.

#### Operation Parentage

In order to do this, we need to add some metadata to our operations. Not only do our operations need to contain their components (retain, etc), they also must maintain some notion of parentage. We need to be able to determine exactly what state an operation requires for successful application. We will then use this information to detect the case where an incoming operation is parented on a state which is not in our history (e.g. _b_ on receipt by the server).

For the record, Google Wave uses a monotonically-increasing scalar version number to label document states and thus, operation parents. Novell Pulse does the exact same thing for compatibility reasons, and I recommend that anyone attempting to build a Wave-compatible service follow the same model. However, I personally think that compound OT is a lot easier to understand if document states are labeled by a hash of their contents.

This scheme has some very nice advantages. Given an operation (and its associated parent hash), we can determine instantly whether or not we have the appropriate document state to apply said operation. Hashes also have the very convenient property of converging exactly when the document states converge. Thus, in our one-step diamond case from earlier, operations _a_ and _b_ would be parented off of the same hash. Operation _b'_ would be parented off of the hash of the document resulting from applying _a_ to the initial document state (and similarly for _a'_ ). Finally, the point in state space where the client and server converge once again (after applying their respective operations) will have a single hash, as the document states will be synchronized. Thus, any further operations applied on either side will be parented off of a correctly-shared hash.

Just a quick terminology note: when I say "parent hash", I'm referring to the hash of the document state _prior_ to applying a particular operation. When I say "parent operation" (which I probably will from time to time), I'm referring to the hash of the document state which results from applying the "parent operation" to its parent document state. Thus, operation _b_ in the diagram above is parented off of operation _a_ which is parented off of the same hash as operation _c_.

#### Compound OT

Now that our operations have parent information, our server is capable of detecting that operation _b_ is not parented off of any state in its history. What we need to do is derive an operation which will take us from the parent of _b_ to some point in server state-space. Graphically, this operation would look something like the following (rendered in dark green):

[![sr3ykMn1qJTwYjnSRdu_QOg.png](/assets/images/blog/wp-content/uploads/2010/05/sr3ykmn1qjtwyjnsrdu-qog.png)](</blog/misc/understanding-and-applying-operational-transformation/TwoClientOneServerInferredResolution.svg>)

Fortunately for us, this operation is fairly easy to derive. In fact, we already derived and subsequently threw it away! Remember, OT solves for _two_ sides of the diamond. Thus, when we transformed _a_ against _c_ , the resulting operation pair consisted of _a'_ (which we applied to our local state) and another operation which we discarded. That operation is precisely the operation shown in green above. Thus, all we have to do is re-derive this operation and use it as the second top side of the one-step diamond. At this point, we have all of the information we need to apply OT and derive _b'_ , which we can apply to our local state:

[![syGoinEP_Oz2132nS0Tldqg.png](/assets/images/blog/wp-content/uploads/2010/05/sygoinep-oz2132ns0tldqg.png)](</blog/misc/understanding-and-applying-operational-transformation/TwoClientOneServerAlmostResolved.svg>)

At this point, we're _almost_ done. The only problem we have left to resolve is the application of operation _c_ on the client. Fortunately, this is a fairly easy thing to do; after all, _c_ is parented off of a state which the client has in its history, so it should be able to directly apply OT.

The one tricky point here is the fact that the client must transform _c_ against not one but _two_ operations ( _a_ and _b_ ). Fortunately, this is fairly easy to do. We could apply OT twice, deriving an intermediary operation in the first step (which happens to be exactly equivalent to the green intermediary operation we derived on the server) and then transforming that operation against _b_. However, this is fairly inefficient. OT is fast, but it's still _O(n log n)_. The better approach is to first compose _a_ with _b_ and then transform _c_ against the composition of the two operations. Thanks to Google's careful definition of operation composition, this is guaranteed to produce the same operation as we would have received had we applied OT in two separate steps.

The final state diagram looks like the following:

[![sRZHxo2A6by5-umoTOUe5oQ.png](/assets/images/blog/wp-content/uploads/2010/05/srzhxo2a6by5-umotoue5oq.png)](</blog/misc/understanding-and-applying-operational-transformation/TwoClientOneServer.svg>)

#### Client/Server Asymmetry

Technically, what we have here is enough to implement a fully-functional client/server collaborative editing system. In fact, this is very close to what was presented in the 1995 paper on [the Jupiter collaboration system](<http://doi.acm.org/10.1145/215585.215706>). However, while this approach is quite functional, it isn't going to work in practice.

The reason for this is in that confusing middle part where the server had to derive an intermediary operation (the green one) in order to handle operation _b_ from the client. In order to do this, the server needed to hold on to operation _a_ in order to use it a second time in deriving the intermediary operation. Either that, or the server would have needed to speculatively retain the intermediary operation when it was derived for the first time during the transformation of _a_ to _a'_. Now, this may sound like a trivial point, but consider that the server must maintain this sort of information essentially indefinitely for _every_ client which it handles. You begin to see how this could become a serious scalability problem!

In order to solve this problem, Wave (and Pulse) imposes a very important constraint on the operations incoming to the server: any operation received by the server _must_ be parented on some point in the server's history. Thus, the server would have rejected operation _b_ in our example above since it did not branch from any point in server state space. The parent of _b_ was _a_ , but the server didn't have _a_ , it only had _a'_ (which is clearly a different point in state space).

Of course, simply rejecting any divergence which doesn't fit into the narrow, one-step diamond pattern is a bit harsh. Remember that practically, _almost all_ situations arising in collaborative editing will be multi-step divergences like our above example. Thus, if we naïvely rejected anything which didn't fit into the one-step mold, we would render our collaborative editor all-but useless.

The solution is to move all of the heavy lifting onto the client. We don't want the server to have to track every single client as it moves through state space since there could be thousands (or even millions) of clients. But if you think about it, there's really no problem with the client tracking the _server_ as it moves through state space, since there's never going to be any more than one (logical) server. Thus, we can offload most of the compound OT work onto the client side.

Before it sends any operations to the server, the client will be responsible for ensuring those operations are parented off of some point in the server's history. Obviously, the server may have applied some operations that the client doesn't know about yet, but that's ok. As long as any operations sent by the client are parented off of _some_ point in the server's history, the server will be able to transform that incoming operation against the composition of anything which has happened since that point without tracking any history other than its own. Thus, the server never does anything more complicated than the simple one-step diamond divergence (modulo some operation composition). In other words, the server can always directly apply OT to incoming operations, deriving the requisite operation extremely efficiently.

Unfortunately, not all is sunshine and roses. Under this new regime, the client needs to work twice as hard, translating its operations into server state space and (correspondingly) server operations back into its state space. We haven't seen an example of this "reverse" translation (server to client) yet, but we will in a moment.

In order to maintain this guarantee that the client will never send an operation to the server which is not parented on a version in server state space, we need to impose a restriction on the client: we can never send more than one operation at a time to the server. This means that as soon as the client sends an operation (e.g. _a_ in the example above), it must wait on sending _b_ until the server acknowledges _a_. This is necessary because the client needs to somehow translate _b_ into server state space, but it can't just "undo" the fact that _b_ is parented on _a_. Thus, wherever _b_ eventually ends up in server state space, it has to be a descendant of _a'_ , which is the server-transformed version of _a_. Literally, we don't know _where_ to translate _b_ into until we know _exactly_ where _a_ fits in the server's history.

To help shed some light into this rather confusing scheme, let's look at an example:

[![s4UWF_7Hjj47GVmZZ9LzE7Q.png](/assets/images/blog/wp-content/uploads/2010/05/s4uwf-7hjj47gvmzz9lze7q.png)](</blog/misc/understanding-and-applying-operational-transformation/InFlightExamplePre.svg>)

In this situation, the client has performed two operations, _a_ and _b_. The client immediately sends operation _a_ to the server and buffers operation _b_ for later transmission (the lighter blue line indicates the buffer boundary). Note that this buffering in no way hinders the _application_ of local operations. When the user presses a key, we want the editor to reflect that change _immediately_ , regardless of the buffer state. Meanwhile, the server has applied two other operations, _c_ and _d_ , which presumably come from other clients. The server still hasn't received our operation _a_.

Note that we were able to send _a_ immediately because we are preserving every bit of data the server sends us. We still don't know about _c_ and _d_ , but we do know that the last time we heard from the server, it was at the same point in state space as we were (the parent of _a_ and _c_ ). Thus, since _a_ is already parented on a point in server state space, we can just send it off.

Now let's fast-forward just a little bit. The server receives operation _a_. It looks into its history and retrieves whatever operations have been applied since the parent of _a_. In this case, those operations are _c_ and _d_. The server then composes _c_ and _d_ together and transforms _a_ against the result, producing _a'_.

[![sF4htlJRMvStlGdlM42IYGA.png](/assets/images/blog/wp-content/uploads/2010/05/sf4htljrmvstlgdlm42iyga.png)](</blog/misc/understanding-and-applying-operational-transformation/InFlightExampleAppliedClient.svg>)

After applying _a'_ , the server broadcasts the operation to all clients, including the one which originated the operation. This is a very important design feature: whenever the server applies a transformed operation, it sends that operation off to all of its clients without delay. As long as we can guarantee strong ordering in the communication channels between the client and the server (and often we can), the clients will be able to count on the fact that they will receive operations from the server in _exactly_ the order in which the server applied them. Thus, they will be able to maintain a locally-inferred copy of the server's history.

This also means that our client is going to receive _a'_ from the server just like any other operation. In order to avoid treating our own transformed operations as if they were new server operations, we need some way of identifying our own operations and treating them specially. To do this, we add another bit of metadata to the operation: a locally-synthesized unique ID. This unique ID will be attached to the operation when we send it to the server and _preserved_ by the server through the application of OT. Thus, operation _a'_ will have the same ID as operation _a_ , but a very different ID from operations _c_ and _d_.

With this extra bit of metadata in place, clients are able to distinguish their own operations from others sent by the server. Non-self-initiated operations (like _c_ and _d_ ) must be translated into client state space and applied to the local document. Self-initiated operations (like _a'_ ) are actually server acknowledgements of our currently-pending operation. Once we receive this acknowledgement, we can flush the client buffer and send the pending operations up to the server.

Moving forward with our example, let's say that the client receives operation _c_ from the server. Since _c_ is already parented on a version in our local history, we can apply simple OT to transform it against the composition of _a_ and _b_ and apply the resulting operation to our local document:

[![sjLUgN387JTmEuLO58TktCQ.png](/assets/images/blog/wp-content/uploads/2010/05/sjlugn387jtmeulo58tktcq.png)](</blog/misc/understanding-and-applying-operational-transformation/InFlightExampleAppliedClientAppliedServer1.svg>)

Of course, as we always need to keep in mind, the client is a live editor which presumably has a real person typing madly away, changing the document state. There's nothing to prevent the client from creating _another_ operation, parented off of _c'_ which pushes it even further out of sync with the server:

[![s7TvO-Jtrw9RYxEEmjpKBIA.png](/assets/images/blog/wp-content/uploads/2010/05/s7tvo-jtrw9ryxeemjpkbia.png)](</blog/misc/understanding-and-applying-operational-transformation/InFlightExampleAppliedClientAppliedServer1WayOut.svg>)

This is really getting to be a bit of a mess! We've only sent one of our operations to the server, we're trying to buffer the rest, but the server is trickling in more operations to confuse things and we still haven't received the acknowledgement for our very first operation! As it turns out, this is the most complicated case which can ever arise in a Wave-style collaborative editor. If we can nail this one, we're good to go.

The first thing we need to do is figure out what to do with _d_. We're going to receive that operation before we receive _a'_ , and so we really need to figure out how to apply it to our local document. Once again, the problem is that the incoming operation ( _d_ ) is not parented off of any point in our state space, so OT can't help us directly. Just as with _b_ in our fundamental compound OT example from earlier, we need to infer a "bridge" between server state space and client state space. We can then use this bridge to transform _d_ and slide it all the way down into position at the end of our history.

To do this, we need to identify conceptually what operation(s) would take us from the parent of _d_ to the the most recent point in our history (after applying _e_ ). Specifically, we need to infer the green dashed line in the diagram below. Once we have this operation (whatever it is), we can compose it with _e_ and get a single operation against which we can transform _d_.

[![sSTrn9pivyEMy1aq9UeQszg.png](/assets/images/blog/wp-content/uploads/2010/05/sstrn9pivyemy1aq9ueqszg.png)](</blog/misc/understanding-and-applying-operational-transformation/InFlightExampleServerTranslation.svg>)

The first thing to recognize is that the inferred bridge (the green dashed line) is going to be composed exclusively of client operations. This is logical as we are attempting to translate a server operation, so there's no need to transform it against something which the server already has. The second thing to realize is that this bridge is traversing a line parallel to the composition of _a_ and _b_ , just "shifted down" exactly one step. To be precise, the bridge is what we would get if we composed _a_ and _b_ and then transformed the result against _c_.

Now, we could try to detect this case specifically and write some code which would fish out _a_ and _b_ , compose them together, transform the result against _c_ , compose the result of _that_ with _e_ and finally transform _d_ against the final product, but as you can imagine, it would be a mess. More than that, it would be dreadfully inefficient. No, what we want to do is proactively maintain a bridge which will always take us from the absolute latest point in server state space (that we know of) to the absolute latest point in client state space. Thus, whenever we receive a new operation from the server, we can directly transform it against this bridge without any extra effort.

#### Building the Bridge

We can maintain this bridge by composing together all operations which have been synthesized locally since the point where we diverged from the server. Thus, at first, the bridge consists only of _a_. Soon afterward, the client applies its next operation, _b_ , which we compose into the bridge. Of course, we inevitably receive an operation from the server, in this case, _c_. At this point, we use our bridge to transform _c_ immediately to the correct point in client state space, resulting in _c'_. Remember that OT derives _both_ bottom sides of the diamond. Thus, we not only receive _c'_ , but we also receive a new bridge which has been transformed against _c_. This new bridge is precisely the green dashed line in our diagram above.

Meanwhile, the client has performed another operation, _e_. Just as before, we immediately compose this operation onto the bridge. Thanks to our bit of trickery when transforming _c_ into _c'_ , we can rest assured that this composition will be successful. In other words, we know that the result of applying the bridge to the document resulting from _c_ will be precisely the document state before applying _e_ , thus we can cleanly compose _e_ with the bridge.

Finally, we receive _d_ from the server. Just as with _c_ , we can immediately transform _d_ against the bridge, deriving both _d'_ (which we apply to our local document) as well as the new bridge, which we hold onto for future server translations.

[![shAY5YVvXThuNzmGtZcKQiA.png](/assets/images/blog/wp-content/uploads/2010/05/shay5yvvxthunzmgtzckqia.png)](</blog/misc/understanding-and-applying-operational-transformation/InFlightExampleAppliedServer.svg>)

With _d'_ now in hand, the next operation we will receive from the server will be _a'_ , the transformed version of our _a_ operation from earlier. As soon as we receive this operation, we need to compose together any operations which have been held in the buffer and send them off to the server. However, before we send this buffer, we need to make sure that it is parented off of some point in server state space. And as you can see by the diagram above, we're going to have troubles both in composing _b_ and _e_ (since _e_ does not descend directly from _b_ ) and in guaranteeing server parentage (since _b_ is parented off of a point in client state space not shared with the server).

To solve this problem, we need to play the same trick with our buffer as we previously played with the translation bridge: any time the client or the server does anything, we adjust the buffer accordingly. With the bridge, our invariant was that the bridge would always be parented off of a point in server state space and would be the one operation needed to transform incoming server operations. With the buffer, the invariant must be that the buffer is always parented off of a point in server state space and will be the one operation required to bring the server into perfect sync with the client (given the operations we have received from the server thus far).

The one wrinkle in this plan is the fact that the buffer _cannot_ contain the operation which we have already sent to the server (in this case, _a_ ). Thus, the buffer isn't really going to be parented off of server state space until we receive _a'_ , at which point we should have adjusted the buffer so that it is parented precisely on _a'_ , which we now know to be in server state space.

Building the buffer is a fairly straightforward matter. Once the client sends _a_ to the server, it goes into a state where any further local operations will be composed into the buffer (which is initially empty). After _a_ , the next client operation which is performed is _b_ , which becomes the first operation composed into the buffer. The next operation is _c_ , which comes from the server. At this point, we must somehow transform the buffer with respect to the incoming server operation. However, obviously the server operation ( _c_ ) is not parented off of the same point as our buffer (currently _b_ ). Thus, we must _first_ transform _c_ against _a_ to derive an intermediary operation, _c''_ , which is parented off of the parent of the buffer ( _b_ ):

[![sF1HdlPLYD4J0v7i2BStT1w\(2\).png](/assets/images/blog/wp-content/uploads/2010/05/sf1hdlplyd4j0v7i2bstt1w2.png)](</blog/misc/understanding-and-applying-operational-transformation/InFlightExampleAppliedServerInferredPartialBuffer.svg>)

Once we have this inferred operation, _c''_ , we can use it to transform the buffer ( _b_ ) "down" one step. When we derive _c''_ , we also derive a transformed version of _a_ , which is _a''_. In essence, we are anticipating the operation which the server will derive when it transforms _a_ against its local history. The idea is that when we finally do receive the real _a'_ , it should be exactly equivalent to our inferred _a''_.

At this point, the client performs another operation, _e_ , which we immediately compose into the buffer (remember, we also composed it into the bridge, so we've got several things going on here). This composition works because we already transformed the buffer ( _b_ ) against the intervening server operation ( _c_ ). So _e_ is parented off of _c'_ , which is the same state as we get were we to apply _a''_ and then the buffer to the server state resulting from _c_. This should sound familiar. By a strange coincidence, _a''_ composed with the buffer is precisely equivalent to the bridge. In practice, we use this fact to only maintain one set of data, but the process is a little easier to explain when we keep them separate.

Checkpoint time! The client has performed operation _a_ , which it sent to the server. It then performed operation _b_ , received operation _c_ and finally performed operation _e_. We have an operation, _a''_ which will be equivalent to _a'_ if the server has no other intervening operations. We also have a buffer which is the composition of a transformed _b_ and _e_. This buffer, composed with _a''_ , serves as a bridge from the very latest point in server state space (that we know of) to the very latest point in client state space.

Now is when we receive the next operation from the server, _d_. Just as when we received _c_ , we start by transforming it against _a''_ (our "in flight" operation). The resulting transformation of _a''_ becomes our new in flight operation, while the resulting transformation of _d_ is in turn used to transform our buffer down another step. At this point, we have a new _a''_ which is parented off of _d_ and a newly-transformed buffer which is parented off of _a''_.

_Finally,_ we receive _a'_ from the server. We could do a bit of verification now to ensure that _a''_ really is equivalent to _a'_ , but it's not necessary. What we do need to do is take our buffer and send it up to the server. Remember, the buffer is parented off of _a''_ , which happens to be equivalent to _a'_. Thus, when we send the buffer, we know that it is parented off of a point in server state space. The server will eventually acknowledge the receipt of our buffer operation, and we will (finally) converge to a shared document state:

[![sgGTv_bxol7LtNWAnFsCCXg\(2\).png](/assets/images/blog/wp-content/uploads/2010/05/sgGTv_bxol7LtNWAnFsCCXg2.png)](</blog/misc/understanding-and-applying-operational-transformation/InFlightExample.svg>)

The good news is that, as I mentioned before, this was the most complicated case that a collaborative editor client ever needs to handle. It should be clear that no matter how many additional server operations we receive, or how many more client operations are performed, we can simply handle them within this general framework of buffering and bridging. And, as when we sent the _a_ operation, sending the buffer puts the client back into buffer mode with any new client operations being composed into this buffer. In practice, an actively-editing client will spend most of its time in this state: very much out of sync with the server, but maintaining the inferred operations required to get things back together again.

### Conclusion

The OT scheme presented in this article is precisely what we use on Novell Pulse. And while I've never seen Wave's client code, numerous little hints in the waveprotocol.org whitepapers as well as discussions with the Wave API team cause me to strongly suspect that this is how Google does it as well. What's more, Google Docs recently revamped their word processing application with a new editor based on operational transformation. While there hasn't been any word from Google on how exactly they handle "compound OT" cases within Docs, it looks like they followed the same route as Wave and Pulse (the tell-tale sign is a perceptible "chunking" of incoming remote operations during connection lag).

None of the information presented in this article on "compound OT" is available within Google's documentation on waveprotocol.org (unfortunately). Anyone attempting to implement a collaborative editor based on Wave's OT would have to rediscover all of these steps on their own. My hope is that this article rectifies that situation. To the best of my knowledge, the information presented here should be everything you need to build your own client/server collaborative editor based on operational transformation. So, no more excuses for second-rate collaboration!

### Resources

  * To obtain Google's OT library, you must take a Mercurial clone of the [wave-protocol](<http://code.google.com/p/wave-protocol/>) repository:

``` $ hg clone https://wave-protocol.googlecode.com/hg/ wave-protocol ``` 

Once you have the source, you should be able to build everything you need by simply running the Ant build script. The main OT classes are `org.waveprotocol.wave.model.document.operation.algorithm.Composer` and `org.waveprotocol.wave.model.document.operation.algorithm.Transformer`. Their use is exactly as described in this article. Please note that `Transformer` does not handle compound OT, you will have to implement that yourself by using `Composer` and `Transformer`. Operations are represented by the `org.waveprotocol.wave.model.document.operation.DocOp` interface, and can be converted into the more useful `org.waveprotocol.wave.model.document.operation.BufferedDocOp` implementation by using the `org.waveprotocol.wave.model.document.operation.impl.DocOpUtil.buffer` method.

All of these classes can be found in the **fedone-api-0.2.jar** file.

  * [Google's Own Whitepaper on OT](<http://www.waveprotocol.org/whitepapers/operational-transform>)
  * [The original paper on the Jupiter system](<http://doi.acm.org/10.1145/215585.215706>) (the primary theoretical basis for Google's OT)
  * [Wikipedia's article on operational transformation](<http://en.wikipedia.org/wiki/Operational_transformation>) (surprisingly informative)