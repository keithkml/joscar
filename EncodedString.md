An encoded string block is used to send a textual string of characters encoded to binary data with a certain Unicode charset.

The format of this data structure is as follows:

[[Include(/Format)]]

`charset` is a numeric code indicating the charset that was used to encode the `data`. Valid codes and the Unicode charsets they map to are as follows:

| Code | Unicode Charset |
|:-----|:----------------|
| 0 | US-ASCII |
| 2 | UTF-16BE (UCS-2BE) |
| 3 | ISO-8859-1 |

`charsubset` is a numeric code that appears to always be 0 when sent by the official AIM clients. As of this writing, its significance is not known for sure. Rumors exist that this contains the suffix for ISO-8859-X, if `charset` is 3. It may also be a language code. Either way, it seems to always be 0, and can probably be safely ignored.

`data` is the string, encoded to binary data using the algorithm specified by the charset parameters.