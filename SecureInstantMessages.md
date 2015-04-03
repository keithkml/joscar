This page is part of the AimSecureIm documentation.

## Encrypted IM ##

An encrypted IM is essentially a [PKCS7 enveloped data](http://www.alvestrand.no/objectid/1.2.840.113549.1.7.3.html) structure which contains an [RSA-encrypted](http://www.alvestrand.no/objectid/1.2.840.113549.1.1.1.html) [PKCS7 signed data](http://www.alvestrand.no/objectid/1.2.840.113549.1.7.2.html) structure.

This structure is stored after the four charset bytes in the 0x0101 TLV inside the 0x0002 TLV in an instant message ICBM. This structure is described in the InstantMessages documentation.

OpenSSL's ASN1 parser describes an encrypted IM structure as follows:
```
    0:d=0  hl=2 l=inf  cons: SEQUENCE
    2:d=1  hl=2 l=   9 prim:  OBJECT            :pkcs7-envelopedData
   13:d=1  hl=2 l=inf  cons:  cont [ 0 ]
   15:d=2  hl=2 l=inf  cons:   SEQUENCE
   17:d=3  hl=2 l=   1 prim:    INTEGER           :02
   20:d=3  hl=3 l= 176 cons:    SET
   23:d=4  hl=3 l= 173 cons:     SEQUENCE
   26:d=5  hl=2 l=   1 prim:      INTEGER           :02
   29:d=5  hl=2 l=  22 cons:      cont [ 0 ]
   31:d=6  hl=2 l=  20 prim:       OCTET STRING
   53:d=5  hl=2 l=  13 cons:      SEQUENCE
   55:d=6  hl=2 l=   9 prim:       OBJECT            :rsaEncryption
   66:d=6  hl=2 l=   0 prim:       NULL
   68:d=5  hl=3 l= 128 prim:      OCTET STRING
  199:d=3  hl=2 l=inf  cons:    SEQUENCE
  201:d=4  hl=2 l=   9 prim:     OBJECT            :pkcs7-data
  212:d=4  hl=2 l=  29 cons:     SEQUENCE
  214:d=5  hl=2 l=   9 prim:      OBJECT            :aes-128-cbc
  225:d=5  hl=2 l=  16 prim:      OCTET STRING
  243:d=4  hl=2 l=inf  cons:     cont [ 0 ]
  245:d=5  hl=4 l= 784 prim:      OCTET STRING
 1033:d=5  hl=2 l=  16 prim:      OCTET STRING
 1051:d=5  hl=2 l=   0 prim:      EOC
 1053:d=4  hl=2 l=   0 prim:     EOC
 1055:d=3  hl=2 l=   0 prim:    EOC
 1057:d=2  hl=2 l=   0 prim:   EOC
 1059:d=1  hl=2 l=   0 prim:  EOC
```

### Decrypting ###

Java developers (and probably users of other libraries and languages) don't need to parse the ASN.1 structure by hand. To extract the enveloped data (which contains the PKCS7 signed data), code like the following will work:

```
// we assume these are imported, along with some other obvious imports
import org.bouncycastle.cms.CMSEnvelopedData;
import org.bouncycastle.cms.KeyTransRecipientInformation;

// we assume these were defined
byte[] encryptedData; // the raw data sent in the IM SNAC
RSAPrivateKey privateKey; // your private RSA key

CMSEnvelopedData ced = new CMSEnvelopedData(encryptedData);
Collection recip = ced.getRecipientInfos().getRecipients();

if (recip.isEmpty()) {
    // empty message?
    return;
}

KeyTransRecipientInformation rinfo = (KeyTransRecipientInformation) recip.iterator().next();

byte[] content = rinfo.getContent(privateKey, "BC");
```

At the end of that code block, the `content` array contains the content that was enveloped. If you look at this content, you will see that it looks something like:

```
Content-Type: application/pkcs7-mime; charset=us-ascii
Content-Language: en
Content-Transfer-Encoding: binary

[binary garbage]
```

As you can see, it resembles an HTTP header followed by a stream of content. The newlines sent by AIM 5.2 are `\r\n`, or a carriage return followed by a linefeed. This is the Windows newline system.

That binary garbage is a PKCS7 signed data structure, which, after extracted from the `content` HTTP structure, can be parsed as described in Pkcs7SignedData.

After extracting and verifying the data in the PKCS7 signed data structure, the data should look something like this:

```
Content-Type: text/x-aolrtf; charset=us-ascii 
Content-Language: en 
Content-Transfer-Encoding: binary 
  
<HTML><BODY BGCOLOR="#ffffff"><FONT FACE="Verdana" LANG="0" SIZE=2>hey</FONT></BODY></HTML> 
```

Simple string parsing can then extract the message from the HTTP-style stream using the charset specified (it won't always be "us-ascii").

### Encrypting ###

Code like the following will create the data to be signed:

```
// we assume these are defined
String msg; // the message to be sent

ByteArrayOutputStream bout = new ByteArrayOutputStream();
OutputStreamWriter osw = new OutputStreamWriter(bout, "US-ASCII");

osw.write("Content-Transfer-Encoding: binary\r\n"
        + "Content-Type: text/x-aolrtf; charset=us-ascii\r\n"
        + "Content-Language: en\r\n"
        + "\r\n");
osw.flush();
bout.write(msg.getBytes("US-ASCII"));
byte[] dataToSign = bout.toByteArray();
```

> /!\ ''NOTE:'' US-ASCII is used in these examples to simplify the example code. Your code should accommodate for different charsets if the given text cannot be encoded in US-ASCII. Information about charsets may be placed here later.

`dataToSign` then needs to be placed in a PKCS7 signed data block (as specified in Pkcs7SignedData). This needs to be wrapped in another HTTP-style structure and then inside a PKCS7 enveloped data structure:

```
// we assume these are imported
import org.bouncycastle.cms.CMSEnvelopedData;
import org.bouncycastle.cms.CMSEnvelopedDataGenerator;
import org.bouncycastle.cms.CMSProcessableByteArray;

// we assume these are defined
byte[] pkcs7signedData; // the PKCS7 signed data block
X509Certificate buddyCert; // the recipient buddy's certificate

ByteArrayOutputStream bout = new ByteArrayOutputStream();
OutputStreamWriter osw = new OutputStreamWriter(bout, "US-ASCII");

osw.write("Content-Transfer-Encoding: binary\r\n"
        + "Content-Type: application/pkcs7-mime; charset=us-ascii\r\n"
        + "Content-Language: en\r\n"
        + "\r\n");
osw.flush();
bout.write(signedData);

CMSEnvelopedDataGenerator gen = new CMSEnvelopedDataGenerator();
gen.addKeyTransRecipient(buddyCert);
CMSEnvelopedData envData = gen.generate(
        new CMSProcessableByteArray(bout.toByteArray()),
        "2.16.840.1.101.3.4.1.2", "BC");

byte[] pkcs7envelopedData = envData.getEncoded();
```

The `pkcs7envelopedData` array contains the full encrypted message block. It should be around 1 kilobyte of data (or longer depending on the length of the message).