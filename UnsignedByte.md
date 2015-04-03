An unsigned byte is a single-byte (8-bit) unsigned data type that contains an integer from 0 to 255 (inclusive).

An unsigned byte cannot be stored in Java's `byte` type because `byte` is signed.

To read an unsigned byte from a byte array into Java's `short` data type, you can use code like the following:

{{{#!code java
// we assume this is defined
byte[.md](.md) data;

short ubyteValue = (short) (data[0](0.md) & 0xff);}}}

To write an unsigned byte from Java's {{{short}}} data type to an output stream, you can use code like the following:

{{{#!code java
// we assume these are defined
OuptutStream out;
short number;

out.write((byte) (number & 0xff));
}}}
```