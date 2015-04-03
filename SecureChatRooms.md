This page is part of the AimSecureIm documentation.

## Secure Chat Rooms ##

Secure chat rooms are implemented using the [AES symmetric encryption algorithm](http://www.alvestrand.no/objectid/2.16.840.1.101.3.4.1.42.html). When you send a message in a Secure Chat Room, it is signed and encrypted with AES, then stored in a custom ASN.1 structure containing a [PKCS7 encrypted data block](http://www.alvestrand.no/objectid/1.2.840.113549.1.7.6.html).

Symmetric encryption algorithms like AES use a single key to encrypt and decrypt; no public certificates or private keys are used. Anyone who has the key can participate in the chat room. To invite a user to a Secure Chat Room, you give him the key.

### Creating a chat room ###

To create a secure chat room, you must generate an AES key. To do this in Java, you can use code like the following:

```
// we assume these are defined
SecureRandom srandom; // this could be a new SecureRandom()

// 2.16.840.1.101.3.4.1.42 is AES256-CBC
KeyGenerator kg = KeyGenerator.getInstance("2.16.840.1.101.3.4.1.42");
kg.init(srandom);
SecretKey key = kg.generateKey();
```

You can then join the chat room normally, but with a content type of "application/pkcs7-mime" instead of the usual "text/x-aolrtf". (The key is never sent to the server; it was only generated for use later.)

### Inviting a buddy ###

To invite a buddy to a chat room, you send a normal buddy invitation rendezvous ICBM but with two extra TLV's:

  * TlvBlock type 0x0011, no data
  * TlvBlock type 0x2713
    * Data: `securityBlock`

#### Reading the security block of an incoming invitation ####

Stored in `securityBlock` is a PKCS7 signed data block whose data can be extracted and verified as described in Pkcs7SignedData.

After extracting the signed content, it should look like the following:

  * MiniChatRoomInfo: `chatInfo`
  * UnsignedShort: `len`
  * Data: `data`

`chatInfo` is normally the same miniature chat room information block sent in the 0x2711 service data TLV of the chat invitation structure. This block should probably be used instead of the 0x2711 block, however, in the case that they are different.

`len` contains the length of the `data` block.

`data` contains what appears to be a custom ASN.1 structure which contains a PKCS7 encrypted data structure. OpenSSL's ASN.1 parser describes it as follows:

```
    0:d=0  hl=4 l= 313 cons: SEQUENCE
    4:d=1  hl=4 l= 298 cons:  SEQUENCE
    8:d=2  hl=2 l=   1 prim:   INTEGER           :00
   11:d=2  hl=3 l= 146 cons:   SEQUENCE
   14:d=3  hl=3 l= 140 cons:    SEQUENCE
   17:d=4  hl=2 l=  11 cons:     SET
   19:d=5  hl=2 l=   9 cons:      SEQUENCE
   21:d=6  hl=2 l=   3 prim:       OBJECT            :countryName
   26:d=6  hl=2 l=   2 prim:       PRINTABLESTRING   :AB
   30:d=4  hl=2 l=  18 cons:     SET
   32:d=5  hl=2 l=  16 cons:      SEQUENCE
   34:d=6  hl=2 l=   3 prim:       OBJECT            :stateOrProvinceName
   39:d=6  hl=2 l=   9 prim:       PRINTABLESTRING   :Statename
   50:d=4  hl=2 l=  17 cons:     SET
   52:d=5  hl=2 l=  15 cons:      SEQUENCE
   54:d=6  hl=2 l=   3 prim:       OBJECT            :localityName
   59:d=6  hl=2 l=   8 prim:       PRINTABLESTRING   :Locality
   69:d=4  hl=2 l=  21 cons:     SET
   71:d=5  hl=2 l=  19 cons:      SEQUENCE
   73:d=6  hl=2 l=   3 prim:       OBJECT            :organizationName
   78:d=6  hl=2 l=  12 prim:       PRINTABLESTRING   :Organization
   92:d=4  hl=2 l=  16 cons:     SET
   94:d=5  hl=2 l=  14 cons:      SEQUENCE
   96:d=6  hl=2 l=   3 prim:       OBJECT            :organizationalUnitName
  101:d=6  hl=2 l=   7 prim:       PRINTABLESTRING   :OrgUnit
  110:d=4  hl=2 l=  19 cons:     SET
  112:d=5  hl=2 l=  17 cons:      SEQUENCE
  114:d=6  hl=2 l=   3 prim:       OBJECT            :commonName
  119:d=6  hl=2 l=  10 prim:       PRINTABLESTRING   :CommonName
  131:d=4  hl=2 l=  24 cons:     SET
  133:d=5  hl=2 l=  22 cons:      SEQUENCE
  135:d=6  hl=2 l=   9 prim:       OBJECT            :emailAddress
  146:d=6  hl=2 l=   9 prim:       IA5STRING         :yo@yo.com
  157:d=3  hl=2 l=   1 prim:    INTEGER           :01
  160:d=2  hl=2 l=  13 cons:   SEQUENCE
  162:d=3  hl=2 l=   9 prim:    OBJECT            :rsaEncryption
  173:d=3  hl=2 l=   0 prim:    NULL
  175:d=2  hl=3 l= 128 prim:   OCTET STRING
  306:d=1  hl=2 l=   9 prim:  OBJECT            :aes-256-cbc
```

The structure contains the distinguished name (DN) of the receiver as well as the chat room's AES key, encrypted using RSA.

The root sequence of the ASN.1 object contains another sequence (at index 0). That inner sequence is a [KeyTransRecipientInfo object](http://www.faqs.org/rfcs/rfc2630.html). To extract the key transport recipient information object from the entire ASN.1 block in Java, you could use code like the following:

```
// we assume these are imported
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.cms.KeyTransRecipientInfo;

// we assume these are defined
byte[] data; // the data block containing the ASN.1 structure

ByteArrayInputStream bin = new ByteArrayInputStream(data);
ASN1InputStream ain = new ASN1InputStream(bin);
ASN1Sequence root = (ASN1Sequence) ain.readObject();
ASN1Sequence seq = (ASN1Sequence) root.getObjectAt(0);
KeyTransRecipientInfo ktr = KeyTransRecipientInfo.getInstance(seq);
```

We now need to extract and decrypt the chat room key data, which is stored inside `ktr`. The chat room key was encrypted using our public certificate, so we need to use our private key to decrypt it.

```
// we assume these are imported
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.cms.KeyTransRecipientInfo;

// we assume these are defined
ASN1Sequence root; // from above code sample
KeyTransRecipientInfo ktr; // from above code sample
RSAPrivateKey privateKey; // your private key

// extract the object identifier describing what encryption this key is for
// (this should be 2.16.840.1.101.3.4.1.42)
String keyoid = ((DERObjectIdentifier) root.getObjectAt(1)).getId();

// extract the object identifier describing what encryption method the key
// data is encrypted with (this should be 1.2.840.113549.1.7.1)
String encoid = ktr.getKeyEncryptionAlgorithm().getObjectId().getId();

// initialize the decryption cipher
Cipher cipher = Cipher.getInstance(encoid, "BC");
cipher.init(Cipher.DECRYPT_MODE, privateKey);

// decrypt the chat room key data with our private key
byte[] result = cipher.doFinal(ktr.getEncryptedKey().getOctets());
SecretKey roomKey = new SecretKeySpec(result, keyoid);
```

We now know that `roomKey` is the key with which messages sent in the chat room are encrypted, and we know that `keyoid` is the algorithm used. (The algorithm will normally be [2.16.840.1.101.3.4.1.42](http://www.alvestrand.no/objectid/2.16.840.1.101.3.4.1.42.html), which is AES256-CBC.)

#### Sending a secure chat room invitation ####

Building the `securityBlock` is a somewhat tedious process.

First, you need to encrypt the chat room key with the receiver's public X.509 certificate.

```
// we assume these are defined
SecretKey key; // the chat room key
X509Certificate receiverCert; // the receiver's public certificate

byte[] keyData = key.getEncoded();

Cipher c = Cipher.getInstance("1.2.840.113549.1.1.1", "BC");
c.init(Cipher.ENCRYPT_MODE, cert);

byte[] encryptedKey = c.doFinal(keyData);
```

`encryptedKey` now contains the encrypted chat room key. Next we need to place it in a key transport recipient information object:

```
// we assume these are imported
import org.bouncycastle.asn1.cms.IssuerAndSerialNumber;
import org.bouncycastle.asn1.cms.KeyTransRecipientInfo;
import org.bouncycastle.asn1.cms.RecipientIdentifier;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.X509Name;
import org.bouncycastle.asn1.DEROctetString;

// we assume these are defined
X509Certificate receiverCert; // the receiver's certificate
byte[] encryptedKey; // the encrypted key data generated above

X509Name xname = new X509Name(receiverCert.getSubjectDN().getName());
IssuerAndSerialNumber ias = new IssuerAndSerialNumber(xname,
        receiverCert.getSerialNumber());
KeyTransRecipientInfo ktr = new KeyTransRecipientInfo(
        new RecipientIdentifier(ias),
        new AlgorithmIdentifier("1.2.840.113549.1.1.1"),
        new DEROctetString(encryptedKey));
```

Next, we have to create the custom ASN.1 structure. It contains a sequence which contains another sequence which contains the key transport recipient information object and the key cipher object ID.

```
// we assume these are imported
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1OutputStream;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.DERSequence;

// we assume this is defined
KeyTransRecipientInfo ktr; // the key transport recipient information object, created above

ASN1EncodableVector vec = new ASN1EncodableVector();
vec.add(ktr);
vec.add(new DERObjectIdentifier("2.16.840.1.101.3.4.1.42"));

ByteArrayOutputStream bout = new ByteArrayOutputStream();
ASN1OutputStream aout = new ASN1OutputStream(bout);
aout.writeObject(new DERSequence(vec));
aout.close();

byte[] asn1data = bout.toByteArray();
```

Now `asn1data` contains the entire custom ASN.1 structure (DER-encoded).

You should already have a MiniChatRoomInfo object for the chat room to which you're inviting your buddy. Either it was sent to you in your invitation message or you received it from the server when creating the room.

After concatenating the MiniChatRoomInfo, the length of `asn1data`, and then `asn1data` itself (see structure description above for details) into a single byte array, you need to sign the data and place it in a PKCS7 signed data block (as described in Pkcs7SignedData).

The PKCS7 signed data block you generate can then be sent in the chat invitation rendezvous ICBM in the 0x2711 TLV. It should be about 800 bytes long.

### Encrypted chat messages ###

Secure Chat messages are encrypted with AES, wrapped in what appears to be custom ASN.1 format, and then signed in a PKCS7 signed data block.

An encrypted chat message is sent in the same way as a normal chat message, except that its content type (TLV 0x0004) is "application/pkcs7-mime" instead of "text/x-aolrtf".

#### Decoding an encrypted chat message ####

The structure contains a BER sequence which contains a the OID of the [PKCS7 encrypted data structure](http://www.alvestrand.no/objectid/1.2.840.113549.1.7.6.html) followed by a BER tagged object. The BER tagged object contains a BER sequence which is an encrypted data block.

To decode the encrypted chat message in Java, you could use code like the following:

```
// we assume these are imported
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.BERTaggedObject;
import org.bouncycastle.asn1.cms.EncryptedContentInfo;
import org.bouncycastle.asn1.cms.EncryptedData;

// we assume these are defined
byte[] encryptedMsgData; // the encrypted message block

ByteArrayInputStream bin = new ByteArrayInputStream(encryptedMsgData);
ASN1InputStream ain = new ASN1InputStream(bin);

ASN1Sequence seq = (ASN1Sequence) ain.readObject();
BERTaggedObject bert = (BERTaggedObject) seq.getObjectAt(1);
ASN1Sequence seq2 = (ASN1Sequence) bert.getObject();
EncryptedData encd = new EncryptedData(seq2);
EncryptedContentInfo enci = encd.getEncryptedContentInfo();

AlgorithmIdentifier alg = enci.getContentEncryptionAlgorithm();
byte[] encryptedData = enci.getEncryptedContent().getOctets();
```

Now `alg` is an algorithm identifier that describes the encryption algorithm (normally 2.16.840.1.101.3.4.1.42, which is AES256-CBC), and `encryptedData` is the encrypted data. Next we need to decrypt the data:

```
// extract the initialization vector from the algorithm
// identifier object
byte[] iv = ((ASN1OctetString) alg.getParameters()).getOctets();

Cipher c = Cipher.getInstance(alg.getObjectId().getId(), "BC");
c.init(Cipher.DECRYPT_MODE, getChatKey(chat),
        new IvParameterSpec(iv));

bye[] decrypted = c.doFinal(encryptedData);
```

Now `decrypted` contains the decrypted data. It should look like this:

```
Content-Type: application/pkcs7-mime; charset=us-ascii
Content-Language: en
Content-Transfer-Encoding: binary

[binary garbage]
```

That binary garbage is a PKCS7 signed data structure, whose data can be extracted and verified as described at Pkcs7SignedData. The signed data is in the same format as that of the signed data sent in SecureInstantMessages:

```
Content-Type: text/x-aolrtf; charset=us-ascii
Content-Language: en
Content-Transfer-Encoding: binary

&amp;lt;HTML&amp;gt;&amp;lt;BODY BGCOLOR="#ffffff"&amp;gt;&amp;lt;FONT FACE="Verdana" LANG="0" SIZE=2&amp;gt;hey&amp;lt;/FONT&amp;gt;&amp;lt;/BODY&amp;gt;&amp;lt;/HTML&amp;gt;
```

Simple string parsing can then extract the message from the HTTP-style stream using the charset specified (it won't always be "us-ascii").

#### Encoding an encrypted chat message ####

To encode a message for a Secure Chat Room, it must first be wrapped in a PKCS7 signed data object with the same HTTP-style headers used in SecureInstantMessages.

Code like the following will create the data that needs to be signed:

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

''NOTE:'' US-ASCII is used in these examples to simplify the example code. Your code should accommodate for different charsets if the given text cannot be encoded in US-ASCII. Information about charsets may be placed here later.

The data then needs to be signed as described in Pkcs7SignedData. That signed data block then needs to be encrypted with AES.

```
// we assume these are defined
Random random;
SecretKey chatkey; // the chat room's key
byte[] dataToEncrypt; // the PKCS7 signed data object data
                      // (that is, in DER-encoded form)

byte[] iv = new byte[16];
random.nextBytes(iv);

// 2.16.840.1.101.3.4.1.42 is AES256-CBC
Cipher c = Cipher.getInstance("2.16.840.1.101.3.4.1.42", "BC");
c.init(Cipher.ENCRYPT_MODE, chatKey, new IvParameterSpec(iv));

byte[] encryptedData = c.doFinal(dataToEncrypt);
```

Next, you need to create a PKCS7 encrypted data object containing the encrypted data (as a BER octet string) as well as information about the encryption method:

```
// we assume these are imported
import org.bouncycastle.asn1.cms.EncryptedContentInfo;
import org.bouncycastle.asn1.cms.EncryptedData;

// we assume these are defined
byte[] iv; // the initialization vector generated above
byte[] encryptedData; // the encrypted data made above

EncryptedContentInfo eci = new EncryptedContentInfo(
        new DERObjectIdentifier("1.2.840.113549.1.7.1"),
        new AlgorithmIdentifier(
                new DERObjectIdentifier("2.16.840.1.101.3.4.1.42"),
                new DEROctetString(iv)),
        new BERConstructedOctetString(encryptedData));
EncryptedData ed = new EncryptedData(eci, null);
```

Next you need to build the custom ASN.1 structure. It consists of a BER sequence which contains two elements: the OID of the [PKCS7 encrypted data structure](http://www.alvestrand.no/objectid/1.2.840.113549.1.7.6.html) and a BER tagged object containing the encrypted content object.

```
// we assume these are imported
import org.bouncycastle.asn1.BERSequence;
import org.bouncycastle.asn1.BERTaggedObject;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.ASN1OutputStream;
import org.bouncycastle.asn1.ASN1EncodableVector;

// we assume these are defined
EncryptedData ed; // the encrypted data object created above

BERTaggedObject bert = new BERTaggedObject(0, ed.getDERObject());
DERObjectIdentifier rootid
        = new DERObjectIdentifier("1.2.840.113549.1.7.6");
ASN1EncodableVector vec = new ASN1EncodableVector();
vec.add(rootid);
vec.add(bert);

ByteArrayOutputStream fout = new ByteArrayOutputStream();
ASN1OutputStream out = new ASN1OutputStream(fout);
out.writeObject(new BERSequence(vec));
out.close();

byte[] asn1data = fout.toByteArray();
```

Finally, `asn1data` contains the data you can send in the chat message TLV. It should be around 850 bytes, or more depending on the length of the message. The content type of the chat message should be "application/pkcs7-mime".