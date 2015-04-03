The user information block is used to describe properties of a buddy. It is used in many places, including the BuddyList. The format of this data structure is as follows:

[[Include(/Format)]]

`screenname` is the screenname of the user being described by this structure.

`warningLevel` is the user's warning level, multiplied by 10. For example, if the user's warning level is 25%, then `warningLevel` will be 250.

`count` is the number of TLV's in the TlvChain.

The individual TLV's in the chain are described below.

### User Flags ###

The type 0x0001 TLV contains a bit mask describing some properties of the user. The format of the 0x0001 TLV data is as follows:

  * ClassMask: `classMask`

The format and contents of `classMask` is described in detail in ClassMask.

### Acct. Creation Date & Member Since Date ###

The type 0x0002 TLV contains the date at which the user's AIM account was created. As of this writing, in Jan. 2004, this TLV is never present in any user info block. The format of the TLV data is:

  * UnixDate: `creationDate`

The type 0x0005 TLV appears to contain the same data with the same meaning as the 0x0002 TLV, but it appears to be present more often. Its format is also:

  * UnixDate: `memberSince`

Normally, neither of these TLV's is present.

### On Since Date & Session Length (AIM & AOL) ###

The type 0x0003 TLV contains the date at which the user signed on to his or her current AIM session. This TLV is normally always present. The format of the TLV data is as follows:

  * UnixDate: `onlineSince`

`onlineSince` is the date at which the user signed on.

The type 0x000f TLV contains the number of seconds that have passed since the user signed on to his or her current AIM session. This TLV is normally always present if the user is an AIM user, not an AOL user. The format of the TLV data is as follows:

  * UnsignedInt: `aimSessionLength`

The type 0x0010 TLV contains the same data as the type 0x000f TLV, but is only present for actual AOL users. The format is the same:

  * UnsignedInt: `aolSessionLength`

`aimSessionLength` and `aolSessionLength` are both the number of seconds since the user signed on to his or her current session.

Only one of these two TLV's will be present in a user's user info block.

### Idle Time ###

The type 0x0004 TLV contains the number of minutes for which the user is idle. This TLV will not be present unless the user is idle. The format of the TLV data is as follows:

  * UnsignedShort: `idleMins`

`idleMins` is the number of minutes for which the user has been idle.

The server will not send a new user info block for each minute that the user is idle; the client is expected to keep track of idle time.

If this TLV is not present in a user's user info block, he is not idle. This means that if previously this TLV was present, the user has come back from being idle.

### ICQ Status ###

For ICQ buddies, the type 0x0006 TLV will be present and will contain a set of flags indicating his ICQ availability status. The format of the TLV data is as follows:

  * UnsignedInt: `status`

`status` will be a set of bit flags (see DataRepresentation for details on bit flags) indicating the user's ICQ status. The possible bit flags are as follows:

| Bit Flag | Meaning |
|:---------|:--------|
| 0x0001 | User is away |
| 0x0002 | User should not be disturbed ("Do Not Disturb") |
| 0x0004 | User is "not available" |
| 0x0010 | User is "occupied" |
| 0x0020 | User is "free for chat" |
| 0x0100 | User is marked as invisible |

This TLV will not be present for buddies who are using AIM or AOL; it will only be present in the user info blocks of buddies who are using ICQ.

### Capability Blocks (Short & Long) ###

The 0x000d TLV contains a list of the user's CapabilityBlock``s in their long form. This TLV may not contain all of the user's capability blocks, if your client has short capability blocks enabled. See below for details on short capability blocks. The format of the 0x000d TLV data is as follows:

  * LongCapabilityBlock: `cap1`
  * LongCapabilityBlock: `cap2`
  * LongCapabilityBlock: `cap3`...

The `capN` blocks are the user's capability blocks. If your client has short capability blocks enabled, this TLV will only contain the user's capability blocks which cannot be represented as short capability blocks. For details, see the CapabilityBlock documentation.

If you have the "short capability block" CapabilityBlock set, an 0x0019 TLV will be present and will contain a list of the user's capability blocks that can be represented as short capability blocks. The format of the TLV data is as follows:

  * ShortCapabilityBlock: `shortCap1`
  * ShortCapabilityBlock: `shortCap2`
  * ShortCapabilityBlock: `shortCap3`...

The `shortCapN` blocks are the user's short capability blocks. For details on short capability blocks, see the CapabilityBlock documentation.

### Security Certificate Info ###

For buddies who have AimSecureIm certificates set, an 0x001b TLV will be present and will contain information about his or her security certificates. The format of the packet is as follows:

  * ByteBlock size 16: `certHash`

`certHash` is an Md5Hash of the binary data representing the buddy's CertificateInfoBlock.

### Extra Information Blocks ###

Some users will have a type 0x001d TLV present, which contains the user's public ExtraInfoBlock``s which were set by the buddy using SetExtraInfoSnac. The format of the TLV data is as follows:

  * ExtraInfoBlock: `block1`
  * ExtraInfoBlock: `block2`
  * ExtraInfoBlock: `block3`...

The `blockN` extra info blocks are the user's public ExtraInfoBlock``s. For details, see the ExtraInfoBlock documentation.