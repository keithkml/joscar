An outgoing IM InterClientBasicMsg has the following format:

**SnacCommand Family 0x0004 Subtype 0x0006
  * IcbmId: `cookie`
  * UnsignedShort: `channel`
  * AsciiStringBlock: `screenname`
  * TlvChain: `tlvs`
    * MessageBlock (0x0002 TLV): `message` or RendezvousBlock (0x0005 TLV): `rendezvous`**


`id` is an 8-byte value identifying the ICBM.

`channel` is the channel to send the message on.

`screenname` is the screenname of the user to whom the IM is being sent.

`tlvs` is a set of TLV's described directly below and in the Common TLV's section further below.

The TLV's which must only be present in outgoing ICBM's are as follows:

  * TlvBlock type 0x0003 ''(empty)'' ''(optional)'': `acknowledge`

If the type 0x0003 TLV (`acknowledge`) is present, an ImAckSnac will be sent as a SnacResponse to this IM as soon as it is received by the server. The acknowledgment packet is sent as soon as the server receives it, and does not indicate that the user to whom the message was sent, actually received the message.
