# Server-Stored Information (SSI) #

"Server-stored information" is a means of storing information on the AIM server for a particular screenname. This information persists even when the user signs off. Information that can be stored on the server includes buddy list information, allow/block lists, presence information, buddy icon information, and linked screen name information.

SSI makes several old SNAC commands and families practically obsolete, including adding/removing items to the buddy list in family 0x03, adding/removing allowed and blocked users in family 0x09, and advertising your buddy icon inside InstantMessages.

Information is stored on the server in the form of "SSI items." These are thoroughly documented at SsiItems.

## SSI-Dependent OSCAR Features ##

The following features either partially or completely depend on server-stored information:

  * The BuddyList
  * The BuddyIconServer and the new buddy icon system
  * Your Block and Allow lists, along with privacy settings
  * LinkedScreenNames

## Retrieving Server-Stored Information ##

To retrieve your server-stored information, you can use a SsiDataRequestSnac. The command looks like the following:

[[Include(SsiDataRequestSnac/Format)]]

In response, the server will send one ''or more'' SsiDataSnac```s, as SnacResponse```s. These packets will look like this:

[[Include(SsiDataSnac/Format)]]

As of this writing, `ssiVersion` is always 0.

`itemCount` is the number of SsiItemBlock```s sent in this packet. Normally, this number will not be greater than 110. If your SSI contains more than 110 items, they will be spread over several SsiDataSnac```s.

`lastMod` is the date at which the data were last modified. If this value is 0 and `itemCount` is not 0, more SsiDataSnac``s will follow this packet. This is because all SSI items may not fit in a single packet.

Downloading your entire SSI each time you log in would waste bandwidth for users who mainly use one client to connect to AIM - there's no reason to keep downloading the SSI when it's not changing, or when your client is the one changing it. For this reason, the SsiDataCheckSnac was created. With this command, your client can request that the SSI only be downloaded if it has changed since a given date. This packet looks like this:

[[Include(SsiDataCheckSnac/Format)]]

`lastModified` should be the date that your client last saw the buddy list modified. This value might be from the last SsiDataSnac received, or it might be the date when your client last modified the SSI.

`numItems` should be the number of SSI items present in the client's latest copy of the SSI.

If the value of `numItems` matches and the server's SSI last modified time is earlier or equal to `lastModified`, the server will send an SsiUnchangedSnac as a SnacResponse. Otherwise, the server will send one or more SsiDataSnac``s, as described above.

## Activating Server-Stored Information ##

When you log in, your SSI is only a block of data stored on the server. Your buddy list is empty, as are your block list, your buddy icon, and many other aspects of the protocol that depend on SSI.

To make the buddy list stored in your SSI your buddy list, to enable your buddy icon, and so on, you must "activate" your SSI. This is done with an ActivateSsiSnac:

[[Include(ActivateSsiSnac/Format)]]

When this command is sent, all SSI-dependent aspects of the protocol are enabled, and all future changes to your SSI (during the current AIM connection) take effect immediately.

## Modifying Server-Stored Information ##

Each SSI item has a type code. Buddy items are type 0, privacy items are type 5, and so on. For details, see SsiItems. Before modifying your SSI, you should make sure you know the maximum numbers of each item type that the server allows.

After activating your SSI, described in the previous section, all changes you make to your server-stored information will take effect immediately. For example, if you add a Block item, that user will be immediately blocked, assuming the add command succeeds.

I recommend that you read or at least skim the SsiItems documentation before reading any further, for the purpose of understanding the SSI item storage format.

### SSI Item Limits ###

To request this information, you send an SsiRightsRequestSnac. This packet looks like this:

[[Include(SsiRightsRequestSnac/Format)]]

The server will respond with an SsiRightsSnac (as a SnacResponse). This packet looks like this:

[[Include(SsiRightsSnac/Format)]]

`maxN` is the maximum number of SSI items of type `N` that can be stored. For details, see the SsiRightsSnac documentation and the SsiItems documentation.

### Creating New SSI Items ###

To add one or more new items to your SSI, you should send a CreateSsiItemsSnac. This packet looks like this:

[[Include(CreateSsiItemsSnac/Format)]]

The `itemN` items are the items to be added to your list. The combination of group ID and buddy ID of each item in this list must be different from every item currently in your SSI.

The server will respond to this command with an SsiAckSnac (as a SnacResponse). For details, see the SsiAckSnac documentation.

### Modifying Existing SSI Items ###

To change an item that's in your SSI, you must send a copy of the new item contents in a UpdateSsiItemsSnac. This packet looks just like the create items SNAC:

[[Include(UpdateSsiItemsSnac/Format)]]

The `itemN` items are the items to be modified. An item of the same type with the same combination of group ID and buddy ID must already exist in your SSI for each item in this list.

The server will respond to this command with an SsiAckSnac (as a SnacResponse). For details, see the SsiAckSnac documentation.

### Deleting Existing SSI Items ###

To remove an item from your SSI, you must send a copy of the item in a DeleteSsiItemsSnac. This packet looks like the other two SSI modification SNAC's:

[[Include(DeleteSsiItemsSnac/Format)]]

The `itemN` items are the items to be deleted. The items should have the same name, type, parent ID, and group ID as the items you want to delete, but the item data may be empty, to save bandwidth.

If you send an item in a delete items SNAC with the same type, parent ID, and group ID, but with a different name, for example, strange things may happen. For example, if you send a Block item in a delete items SNAC with a different name, the item will be deleted, but the user will not be unblocked.

The server will respond to this command with an SsiAckSnac (as a SnacResponse). For details, see the SsiAckSnac documentation.