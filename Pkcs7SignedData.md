### PKCS7 signed data ###

The [PKCS7 signed data](http://www.alvestrand.no/objectid/1.2.840.113549.1.7.2.html) standard allows for sending data with a digital signature that verifies that the data was not tampered-with during transmission between parties.

OpenSSL describes the structure as such:

```
    0:d=0  hl=2 l=inf  cons: SEQUENCE 
    2:d=1  hl=2 l=   9 prim:  OBJECT            :pkcs7-signedData 
   13:d=1  hl=2 l=inf  cons:  cont [ 0 ] 
   15:d=2  hl=2 l=inf  cons:   SEQUENCE 
   17:d=3  hl=2 l=   1 prim:    INTEGER           :01 
   20:d=3  hl=2 l=  11 cons:    SET 
   22:d=4  hl=2 l=   9 cons:     SEQUENCE 
   24:d=5  hl=2 l=   5 prim:      OBJECT            :sha1 
   31:d=5  hl=2 l=   0 prim:      NULL 
   33:d=3  hl=2 l=inf  cons:    SEQUENCE 
   35:d=4  hl=2 l=   9 prim:     OBJECT            :pkcs7-data 
   46:d=4  hl=2 l=inf  cons:     cont [ 0 ] 
   48:d=5  hl=2 l=inf  cons:      OCTET STRING 
   50:d=6  hl=3 l= 197 prim:       OCTET STRING      :Content-Type: text/x-aolrtf; charset=us-ascii 
Content-Language: en 
Content-Transfer-Encoding: binary 
  
<HTML><BODY BGCOLOR="#ffffff"><FONT FACE="Verdana" LANG="0" SIZE=2>hey</FONT></BODY></HTML> 
  250:d=6  hl=2 l=   0 prim:       EOC 
  252:d=5  hl=2 l=   0 prim:      EOC 
  254:d=4  hl=2 l=   0 prim:     EOC 
  256:d=3  hl=4 l= 408 cons:    SET 
  260:d=4  hl=4 l= 404 cons:     SEQUENCE 
  264:d=5  hl=2 l=   1 prim:      INTEGER           :01 
  267:d=5  hl=3 l= 146 cons:      SEQUENCE 
  270:d=6  hl=3 l= 140 cons:       SEQUENCE 
  273:d=7  hl=2 l=  11 cons:        SET 
  275:d=8  hl=2 l=   9 cons:         SEQUENCE 
  277:d=9  hl=2 l=   3 prim:          OBJECT            :countryName 
  282:d=9  hl=2 l=   2 prim:          PRINTABLESTRING   :AB 
  286:d=7  hl=2 l=  18 cons:        SET 
  288:d=8  hl=2 l=  16 cons:         SEQUENCE 
  290:d=9  hl=2 l=   3 prim:          OBJECT            :stateOrProvinceName 
  295:d=9  hl=2 l=   9 prim:          PRINTABLESTRING   :Statename 
  306:d=7  hl=2 l=  17 cons:        SET 
  308:d=8  hl=2 l=  15 cons:         SEQUENCE 
  310:d=9  hl=2 l=   3 prim:          OBJECT            :localityName 
  315:d=9  hl=2 l=   8 prim:          PRINTABLESTRING   :Locality 
  325:d=7  hl=2 l=  21 cons:        SET 
  327:d=8  hl=2 l=  19 cons:         SEQUENCE 
  329:d=9  hl=2 l=   3 prim:          OBJECT            :organizationName 
  334:d=9  hl=2 l=  12 prim:          PRINTABLESTRING   :Organization 
  348:d=7  hl=2 l=  16 cons:        SET 
  350:d=8  hl=2 l=  14 cons:         SEQUENCE 
  352:d=9  hl=2 l=   3 prim:          OBJECT            :organizationalUnitName 
  357:d=9  hl=2 l=   7 prim:          PRINTABLESTRING   :OrgUnit 
  366:d=7  hl=2 l=  19 cons:        SET 
  368:d=8  hl=2 l=  17 cons:         SEQUENCE 
  370:d=9  hl=2 l=   3 prim:          OBJECT            :commonName 
  375:d=9  hl=2 l=  10 prim:          PRINTABLESTRING   :CommonName 
  387:d=7  hl=2 l=  24 cons:        SET 
  389:d=8  hl=2 l=  22 cons:         SEQUENCE 
  391:d=9  hl=2 l=   9 prim:          OBJECT            :emailAddress 
  402:d=9  hl=2 l=   9 prim:          IA5STRING         :yo@yo.com 
  413:d=6  hl=2 l=   1 prim:       INTEGER           :01 
  416:d=5  hl=2 l=   9 cons:      SEQUENCE 
  418:d=6  hl=2 l=   5 prim:       OBJECT            :sha1 
  425:d=6  hl=2 l=   0 prim:       NULL 
  427:d=5  hl=2 l=  93 cons:      cont [ 0 ] 
  429:d=6  hl=2 l=  24 cons:       SEQUENCE 
  431:d=7  hl=2 l=   9 prim:        OBJECT            :contentType 
  442:d=7  hl=2 l=  11 cons:        SET 
  444:d=8  hl=2 l=   9 prim:         OBJECT            :pkcs7-data 
  455:d=6  hl=2 l=  28 cons:       SEQUENCE 
  457:d=7  hl=2 l=   9 prim:        OBJECT            :signingTime 
  468:d=7  hl=2 l=  15 cons:        SET 
  470:d=8  hl=2 l=  13 prim:         UTCTIME           :030916055808Z 
  485:d=6  hl=2 l=  35 cons:       SEQUENCE 
  487:d=7  hl=2 l=   9 prim:        OBJECT            :messageDigest 
  498:d=7  hl=2 l=  22 cons:        SET 
  500:d=8  hl=2 l=  20 prim:         OCTET STRING 
  522:d=5  hl=2 l=  13 cons:      SEQUENCE 
  524:d=6  hl=2 l=   9 prim:       OBJECT            :rsaEncryption 
  535:d=6  hl=2 l=   0 prim:       NULL 
  537:d=5  hl=3 l= 128 prim:      OCTET STRING 
  668:d=3  hl=2 l=   0 prim:    EOC 
  670:d=2  hl=2 l=   0 prim:   EOC 
  672:d=1  hl=2 l=   0 prim:  EOC 
```

#### Extracting PKCS7 signed data ####

Extracting the signed data from a PKCS7 signed data block is easy in Java:

```
// we assume these are imported
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSProcessableByteArray;

// we assume these are defined
byte[] pkcs7SignedData; // a PKCS7 signed data block

CMSSignedData csd = new CMSSignedData(pkcs7SignedData);
CMSProcessableByteArray cpb = (CMSProcessableByteArray) csd.getSignedContent();
byte[] signedContent = (byte[]) cpb.getContent();
```

At the end of that code block, `signedContent` will contain the data that was signed. To verify that the data was signed by the sender (and not tampered with in transit), you could use code like the following:

```
// we assume this is imported
import org.bouncycastle.cms.CMSSignedData;

// we assume these are defined
CMSSignedData csd;
X509Certificate senderCert; // the sender's certificate

Collection signers = csd.getSignerInfos().getSigners();
for (Iterator sit = signers.iterator(); sit.hasNext();) {
    SignerInformation si = (SignerInformation) sit.next();
    boolean verified = si.verify(senderCert, "BC");
    System.out.println("verified: " + verified);
}
```

#### Creating PKCS7 signed data blocks ####

Signing data and placing it in a PKCS7 signed data block in Java is easy as well:

```
// we assume these are imported
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;

// we assume these are defined
RSAPrivateKey privateKey; // your private key
X509Certificate myPubCert; // your public certificate
byte[] dataToSign; // the data to be signed

CMSSignedDataGenerator sgen = new CMSSignedDataGenerator();
sgen.addSigner(privateKey, myPubCert,
        CMSSignedDataGenerator.DIGEST_MD5);
CMSSignedData csd = sgen.generate(
        new CMSProcessableByteArray(dataToSign),
        true, "BC");
byte[] signedData = csd.getEncoded();
```

At the end of that code block, `signedData` contains the PKCS7 signed data block.