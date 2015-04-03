"Unix date" (also known as "unixtime") is a means of storing a date and time as a single number. The number is the number of seconds since the "Unix epoch," January 1, 1970, 00:00:00 UTC.

Unix dates are stored in the OSCAR protocol as UnsignedInt``s.

To convert a Unix date to Java's `Date`, you could use code like the following:

{{{#!code java
// we assume this is defined
long unixtime;

Date date = new Date(unixtime 