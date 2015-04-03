# AIM Secure IM (AIM Personal Certificates) #

In AIM version 5.2, AOL introduced a feature called "Encrypted Instant Messaging," "Personal Certificates," and a few other names. AOL released some [information](http://enterprise.netscape.com/products/aim/personalcerts/) about the protocol, but it took quite a bit of hacking to figure it out even with that information.

''NOTE:'' Java code samples in this document assume the presence of the wonderful [Bouncy Castle Crypto Libraries](http://www.bouncycastle.org) as well as the [JCE unlimited strength jurisdiction policy files](http://java.sun.com/products/jce/index-14.html#UnlimitedDownload).

''NOTE:'' As of this writing, I am not sure in which situations the signing certificate or the encryption certificate should be used. I only tested when both signing and encryption certificates were the same. This should be somewhat easy to figure out; I plan to investigate further later.

## A brief overview ##

To use secure IM/chat/filetransfer/directIM/getfile, a client first has to send his certificate(s) to the server and add a certain CapabilityBlock to his list of capability blocks. That capability block is then shown to your buddies along with an MD5 hash of your certificate information block (these are sent in the UserInfoBlock of a buddy status update).

Before using any form of encrypted communication (file transfer, IM, chat rooms, etc.), both users involved must have each other's certificate. This is not a requirement of the protocol, but a requirement of the PKI encryption method used.

Secure IM's are simply encrypted and signed in a CMS Signed Data block which is wrapped in a CMS Enveloped Data block.

Secure file transfers, Direct IM connections, and Get File connections are encrypted using SSL without any other modification to the protocol. A special "flag" TLV in the request rendezvous ICBM (0x11) indicates that the connection will be made over SSL. Secure connections over the AolProxyServer are not over SSL themselves; once the proxy initiation is finished, the connection should be "converted" to an SSL connection.

Secure chat rooms use a different method of encryption. When a user creates a secure chat room, he generates an AES key (a symmetric, or "secret" key). When he invites a buddy to the chat room, he sends that key, RSA-encrypted with that buddy's public key. (This way, only he can see the chat room key, since only he has his private key to decode the chat room key.) From then on, anyone who wants to join the chat room must be invited by someone who knows the key. (Anyone could join the chat room, but he wouldn't be able to see what other users were saying, or say anything to other users, as he would not know the AES key.)

## Uploading certificates ##

Uploading your certificate(s) is just as simple as setting your away message or user profile. Certificates are sent inside an 0x0006 TLV in the set-user-info SNAC. That TLV's data has the following structure:

  * TlvBlock type 0x0004
    * UnsignedShort: `numcerts`
  * TlvBlock type 0x0001
    * Data: `certA`
  * TlvBlock type 0x0002 ''(optional)''
    * Data: `certB`
  * TlvBlock type 0x0005
    * ExtraInfoBlock type 0x0402, flags 0x01
      * Data: `hashA`
  * TlvBlock type 0x0006
    * ExtraInfoBlock type 0x0403, flags 0x01
      * Data: `hashB`

If `numcerts` is 2, then `certA` is the user's encryption certificate and `certB` is the user's signing certificate. As of this writing, I do not know in which cases the signing certificate is used and in which the encryption certificate is used. Simple testing should reveal this, however.

If `numcerts` is 1, then `certB` is not present, and `certA` is both the user's signing certificate and his encrypting certificate.

The values of the data in `hashA` and `hashB` extra info blocks is unknown. As of this writing, the value always sent in each of them is the MD5 sum of an empty data set (`d41d8cd98f00b204e9800998ecf8427e`).

## Encrypted IM ##

See SecureInstantMessages.

## Encrypted direct connection, file transfer, Get File ##

See SecureDirectConnections.

## Secure Chat Rooms ##

See SecureChatRooms.