Topics covered:
[[TableOfContents](TableOfContents.md)]

### General documentation guidelines ###

  * The protocol should be explained in a logical format instead of in a format based on the semantics of the protocol. For example, the rendezvous method of transferring buddy icons should be explained in the same section (though not necessarily on the same wiki page) as the new buddy icon server method, even though at the protocol level these two methods are very different.
  * Duplicating small amounts of content among pages is better than confusing or distracting series of links between the pages. If you're documenting something and you feel like you've already documented it elsewhere, it's probably still a good idea to document it there (possibly using the other documentation as a reference); a link to the other documentation might be appropriate as well.
  * When writing about the user of the reader's client, he or she should be referred to as "the user." When writing about other users who are on AIM which are interacting with the reader's client, these users should be referred to as "buddies." For example, when the user receives a ReceiveTypingStateSnac, it indicates that a buddy sent the user a typing state packet.

### Formatting guidelines ###

Oscar doc writers should be familiar with the basics of [wiki:HelpOnFormatting wiki formatting syntax].

#### Describing data structures ####

Data structures in the OSCAR protocol (and in general) are hierarchal in nature. Descriptions of binary data structures in this documentation project should use a common format:

  * Outer structure: `outerStructure`
    * Inner structure: `innerStructure1`
    * Inner structure: `innerStructure2`
    * Inner structure ''(empty)''
  * Outer structure ''(optional)'': `outerStructure2`
    * Inner structure: `innerStructure4`
      * Inner inner structure type 2: `innerInnerStructure1`
      * Inner inner structure type 2: `innerInnerStructure2`
      * Inner inner structure type 2: `innerInnerStructure3`...
  * SnacCommand family 0x0002, type 0x0004
    * UnsignedInt: `someInt`
    * Data: `restOfData`

Several idioms are shown in the above example:

  * Each structure or value in a data structure should be given a name, to make referencing that value easier. References to these names in descriptions of the packet (or elsewhere) should be written in `the code font` for clarity. These names should follow the Java coding standard guidelines for local variable and field names:
    * They should start with a lowercase letter
    * They should not contain any non-alphanumeric characters ''(_(underscore) is not alpha-numeric!)''
    * The first letter of subsequent words in a name should be capitalized
  * Structures like SnacCommand that are documented elsewhere may be referenced in an abbreviated fashion (describing the family and type on the same line instead of as nested values, for example).
  * Structures like SnacCommand and the outer structure and inner structure in the above example have a concept of a "data section." Data within such structures should be described using nested tree nodes as shown above.
  * Repeating sequences of data should be represented by listing the data three times and appending "..." to the end of the third repeated line.
  * Optional values and structures should be marked with an italic ''(optional)'' tag as shown above.
  * Empty structures are structures which contain no data whatsoever (not even zeroes or nulls). Such structures should be marked with an italicized ''(empty)'' tag as shown above._

To see how the tree above was created, click Edit``Text at the bottom of this page.

Some other data structure guidelines:
  * Each important and/or repeating data structure should be given its own wiki page. A [wiki:HelpOnEditing/SubPages wiki sub-page] should be created containing ''only'' the data structure format tree described above. See StructureTemplate for a template (this template can be selected when creating a new page).
  * Each SNAC command should be given its own wiki page whose name ends with "Snac". As with data structures, a wiki sub-page should describe the packet's structure. See SnacTemplate for a template (this template can be selected when creating a new page).
  * A SNAC command that the server sends as a SnacResponse to a previous client request should be documented (at the top of the SNAC's wiki page) like so: "The key response SNAC is sent by the OSCAR server as a SnacResponse to a KeyRequestSnac."

#### Text formatting guidelines ####

  * Text markup should be used sparingly; I think it's safe to say that italics and bold should almost never be used.
  * All tables should have headings in the first column and/or the first row indicating what the data in the table are.
  * Important notes should be marked with the /!\ symbol and indented. An example:

```
Error message 0x0018 means that the client has been connecting too quickly.

 /!\ It is important to make a special case for error message 0x0018 when writing AIM
robots that automatically keep themselves connected. Reconnecting too quickly after an
0x0018 message will reset AOL's timer for that screenname, meaning that if you keep reconnecting,
each attempt will fail.
```

The above message shows up as:

Error message 0x0018 means that the client has been connecting too quickly.

> /!\ It is important to make a special case for error message 0x0018 when writing AIM robots that automatically keep themselves connected. Reconnecting too quickly after an 0x0018 message will reset AOL's timer for that screenname, meaning that if you keep reconnecting, each attempt will fail.

#### Formatting Java code ####

Java code written for examples or other purposes should comply with the [Sun Java code conventions](http://java.sun.com/docs/codeconv/). Nothing has to be perfect, but using it as a guideline is recommended.

Blocks of code in this documentation project should follow a certain format:

{{{#!code java
// we assume these are imported
import some.external.pkg.SomeClass;
import some.external.pkg.SomeOtherClass;
import some.external.pkg.SomeOtherOtherClass;

// we assume these are declared
byte[.md](.md) screenname; // the user's screenname, encoded as US-ASCII
int numUsers; // the number of users in the chat room

int someValue = numUsers 