The ICBM (Inter-Client Basic Message) structure is used to hold data sent between two OSCAR clients. An ICBM structure holds a single message. The format of this data structure is as follows:

[[Include(/Format)]]

`id` is the message ID for this message.

`channel` is the channel on which this message is being sent. The channel on which a message is sent determines the format of `data`. The channel values are as follows:

| Channel | Description |
|:--------|:------------|
| 1 | InstantMessages |
| 2 | RendezvousProtocol |
| 3 | Messages to ChatRooms |

For details on the formats of `data`, see InstantMessages, RendezvousProtocol, and ChatRooms.