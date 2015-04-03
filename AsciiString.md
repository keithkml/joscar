An ASCII string is a block of text encoded as US-ASCII.

To convert from Java's `String` to a block of binary data encoded as US-ASCII, you can use code like the following:

{{{#!code java
// we assume this is defined
String string;

byte[.md](.md) encoded = null;
try {
> encoded = string.getBytes("US-ASCII");
} catch (UnsupportedEncodingException impossible) { }}}}

Note that the unsupported encoding exception can be ignored, as Java specifies that all JVM's must support the US-ASCII charset.

To convert from a block of binary data to Java's {{{String}}}, you can use code like the following:

{{{#!code java
// we assume this is defined
byte[] encoded;

String string = null;
try {
    string = new String(encoded, "US-ASCII");
} catch (UnsupportedEncodingException impossible) { }
}}}

Once again, the unsupported encoding exception can be ignored, as Java specifies that all JVM's must support the US-ASCII charset.
```