## A brief introduction ##

A FLAP connection is a connection over which only FLAP packets are sent. It has a concept of "channels" of data and keeps a running "sequence number" for ensuring that packets are sent and received in order. Sequence numbering is also a part of TCP itself and is thus an unnecessary part of the FLAP protocol; it must, however, be followed. (Some speculate that sequence numbers are remnants of old AOL technology which did not use TCP.)

I don't know what FLAP stands for.

## FLAP packets ##

All data sent over a FLAP connection is in the form of a FLAP packet. A FLAP packet has the following form:

  * UnsignedByte: always 0x2a
  * UnsignedByte: `channel`
  * UnsignedShort: `seqnum`
  * UnsignedShort: `length`
  * Data: `data`

The first byte of each FLAP packet is always 0x2a.

`length` is the length of `data`, in bytes. Providing the length of the data before the data itself allows a sequence of packets to be read from an incoming stream by telling the parser how much to read before the next packet starts.

A FLAP packet may or may not be sent in a single TCP packet. Multiple FLAP packets may be sent in one TCP packet and, conversely, large FLAP packets may be spread over several TCP packets.

When sending a FLAP packet, its `seqnum` must be the value of the `seqnum` of the previously sent packet, plus one. (The first packet sent on a FLAP connection can have any `seqnum`.) When the `seqnum` of the previously sent packet was 65535 (the highest value able to be represented as an UnsignedShort), the next `seqnum` should be 0.

The `seqnum` of your outgoing FLAP packets has no relation to the `seqnum` of the other end's packets which you are receiving. That is, sequence numbers do not need to be synchronized between incoming and outgoing packets; rather, it is your job only to keep track of your own last sent sequence number.

## FLAP channels ##

While the protocol allows for up to 256 different channels, only the first five are used.

### Channel 1 ###

Channel 1 is called the "login channel," as it is only used when initiating a FLAP connection. The format of the data inside a channel-1 FLAP packet is as follows:

  * UnsignedInt: `flapVersion`
  * TlvChain
    * TlvBlock type 0x0006 ''(optional)''
      * Data: `loginCookie`

The only value for `flapVersion` ever used is 1. If you receive a packet whose `flapVersion` is not 1, you may wish to disconnect.

When a FLAP connection is made, the server sends a channel-1 FLAP packet containing only the FLAP version (with no login cookie). If the connection is to the InitialAuthentication server, the client sends back another channel-1 FLAP packet also containing only the FLAP version. If the connection is to any other OSCAR server, the client sends a channel-1 FLAP packet containing both the FLAP version and a login cookie. (When redirecting you to another OSCAR connection, the server doing the redirecting will provide a login cookie for use here.)

AOL's servers normally allow the client to send his channel-1 FLAP packet (and any other packets afterwards) before receiving the server's. Doing this may reduce the amount of time it takes to log into AIM, as no time will be wasted waiting for the server's initial packet.

After that initialization is complete, FLAP commands on channels 2-4 may be sent over the connection. See InitialAuthentication and LoggingIn for details on what normally happens next.

### Channel 2 ###

Packets sent over channel 2 are the most common packets sent over the OSCAR protocol. A channel-2 FLAP packet contains a SnacCommand object in its data field. Practically all of AIM's functionality is in the form of SNAC commands. See SnacCommand for details.

### Channel 3 ###

Packets sent over channel 3 are used to indicate errors at the FLAP level of processing. A channel-3 FLAP packet looks like the following:

  * UnsignedShort: `errorCode`

As of this writing I am not sure which error codes mean which things. In my experience, FLAP errors are very rare, and I can't remember if I've ever seen one.

### Channel 4 ###

Packets sent over channel 4 are used to indicate that the connection will be closed. A channel-4 FLAP packet looks like the following:

  * TlvChain
    * TlvBlock type 0x0009 ''(optional)''
      * UnsignedShort: `errorCode`
    * TlvBlock type 0x000b ''(optional)''
      * AsciiString: `errorUrl`

If present, `errorCode` and `errorUrl` describe the reason the connection is being closed. The only known error code is 0x0001, which means that someone was logging into your screen from elsewhere, causing your session to be closed. In this case, the error URL is normally http://www.aol.com.

### Channel 5 ###

Channel-5 FLAP packets appear to be a rather new and somewhat mysterious element of the protocol, and appear to serve only as "keepalive" or "ping" packets. They never contain any data. A client can send these packets as much or as little as it wants.