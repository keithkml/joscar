An ICBM ID is an 8-byte value representing a unique identifier for a single InterClientBasicMsg.

These can be represented as 64-bit `long` types in Java, but AOL's official AIM clients often send a seven-byte ASCII string followed by a null (0) byte as the ICBM ID.

To read an ICBM ID block into a value of Java's `long` type, you could use code like the following:

{{{#!code java
// we assume this is defined
byte[.md](.md) data; // the 8-byte ICBM message ID block

long num = 0;

for (int i = 0; i < 8; i++) {
> final int offset = (8 - i - 1) 