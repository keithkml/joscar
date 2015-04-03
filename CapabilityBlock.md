# Capability Blocks #

OSCAR uses "capability blocks" to indicate what particular capabilities a client has (such as file transfer, direct IM, and so on). Capability blocks will show up in users of AOL's AIM client in a buddy's tooltip. It is important for a client developer to properly set capabilities other clients can see what features your client does and does not support.

## Capability Block Format ##

Capability blocks are used in several places in the OSCAR protocol. There are two ways in which they are represented in the protocol: a long format and a short format. Every capability block can be represented in the long format, but only some can be represented in the short format.

### Long Format ###

The "long capability block" format is 16 bytes. An example of a long capability block is the capability block for file transfer:

| 094613434c7f11d18222444553540000 |
|:---------------------------------|

In this documentation, capability block values are shown in hexadecimal format. For file transfer, the first byte of the capability block is 0x09, the second byte is 0x46, and so on.

### Short Format ###

In the middle of 2003, AOL introduced the short capability block format, which is enabled by sending the "Short capability block" capability shown in the table below. This format saves space by only storing two of the bytes of the capability block. This is useful because most of the official AOL capability blocks, like file transfer, direct IM, and voice chat, for example, contain 14 of the same bytes in the same positions.

The long capability blocks for file transfer, direct IM, and voice chat are:

| Capability name | Long Capability Block value |
|:----------------|:----------------------------|
| File transfer | 0946'''1343'''4c7f11d18222444553540000 |
| Direct IM | 0946'''1345'''4c7f11d18222444553540000 |
| Voice chat | 0946'''1341'''4c7f11d18222444553540000 |

The bold indicates the parts where the capability block values differ. These two bytes, the third and fourth bytes, are the bytes stored in the short capability block format. The short capability block format for file transfer, direct IM, and voice chat are:

| Capability name | Short Capability Block value |
|:----------------|:-----------------------------|
| File transfer | 1343 |
| Direct IM | 1345 |
| Voice chat | 1341 |

The leading 0946 and trailing 4c7f11d18222444553540000 are implied. This means that only capability blocks whose long format matches 0946'''xxxx'''4c7f11d18222444553540000 can be stored in the short format.

As of this writing (Dec 2003), short capability blocks are only used in UserInfoBlock``s. A user's user info block may contain both long and short capability blocks.

Normally, the only way a client needs to accommodate for short capability blocks is by decoding them in UserInfoBlock``s and recognizing them as capability blocks. Short capability blocks are only sent by the OSCAR server, never by the client.

## Known Capability Block Values ##

This is a list of the known capability blocks that various AIM clients send.

Some of these can be found in the GAIM source file [protocols/oscar/locate.c](http://cvs.sourceforge.net/viewcvs.py/gaim/gaim/src/protocols/oscar/locate.c?view=markup), others in the joscar source file [net/kano/joscar/snaccmd/CapabilityBlock.java](http://cvs.sourceforge.net/viewcvs.py/joustim/joscar/src/net/kano/joscar/snaccmd/CapabilityBlock.java?view=markup).

| Capability name | Capability Block value | Notes |
|:----------------|:-----------------------|:------|
| '''The official AIM clients''' | |  |
| Add-ins | Short: 1347 |  |
| AIM direct IM | Short: 1345 |  |
| AIM secure IM | Short: 0001 |  |
| Buddy list transfer | Short: 134b |  |
| Chat rooms | 748f2420628711d18222444553540000 |  |
| File transfer | Short: 1343 |  |
| Games (proper) | Short: 134a |  |
| Games (old) | 0946134a4c7f11d12282444553540000 | There are two Games capability blocks. The only difference between them is the position of two adjacent bytes (8222 vs. 2282). This is probably due to a bug in some version of the official AIM client that mixed up endianness of the 0x8222 value. |
| Get File | Short: 1348 |  |
| Old buddy icon transfer | Short: 1346 |  |
| ''Save stocks'' | Short: 1347 |  |
| Short capability blocks | Short: 0000 |  |
| Voice chat | Short: 1341 |  |
| '''iChat''' | |  |
| ''Video chat'' | Short: 0100 |  |
| '''Trillian''' | |  |
| Trillian SecureIM | f2e7c7f4fead4dfbb23536798bdf0000 |  |
| '''ICQ''' | |  |
| ''SMS functions'' | Short: 01ff |  |
| ''ICQ direct connect'' | Short: 1344 |  |
| ''ICQ server relay'' | Short: 1349 |  |
| Interoperate AIM/ICQ | Short: 134d | Setting this lets AIM users receive messages from ICQ users, and ICQ users receive messages from AIM users. |
| ''UTF8 messages'' | Short: 134e |  |
| ''Old UTF8'' | 2e7a6475fadf4dc8886fea3595fdb6df |  |
| ''ICQ RTF'' | 97b12751243c4334ad22d6abf73f1492 |  |

All capability block values described above are either 16-bit byte blocks (shown in hexadecimal) or short capability blocks (described above).

Capabilities in ''italics'' have not been personally tested by the author (KeithLea).