The SSI modification response SNAC is sent by the OSCAR server as a SnacResponse to the SSI modification commands CreateSsiItemsSnac, UpdateSsiItemsSnac, DeleteSsiItemsSnac. The format of this packet is:

[[Include(/Format)]]

The `resultN` codes are a list of result codes corresponding to each `itemN` in the command to which this is a SnacResponse. Possible codes are as follows:

| | | Possible as a response to... | | |
|:|:|:-----------------------------|:|:|
| Result | Meaning | Create | Update | Delete |
| 0x00 | Change was successful | (./) | (./) | (./) |
| 0x02 | No such item |  | (./) | (./) |
| 0x03 | Item already exists| (./) |  |  |
| 0x0a | Item already exists | (./) |  |  |
| 0x0c | Cannot add any more items of type | (./) |  |  |
| 0x0d | ICQ user cannot be added to AIM buddy list | (./) |  |  |
| 0x0e | (ICQ only) authorization required to add buddy | (./) |  |  |

