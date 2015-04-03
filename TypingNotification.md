## Typing Notification ##
Typing notification is a simple protocol used to indicate to a buddy which of three possi
ble "typing states" the user is currently in.

### Setting up Typing Notification ###

To enable typing notification, your client must set the appropriate flag in its IcbmParam
eterInfo when logging in. This flag tells the server to automatically tell buddies that y
ou support receiving typing notifications. (This is done via a TLV flag the server insert
s into the InstantMessages you send.)

### Sending Typing Notifications ###

The SendTypingStateSnac is used to send a buddy your typing state. The SNAC packet has th
e following format:

[[Include(SendTypingStateSnac/Format)]]

For details, see the SendTypingStateSnac documentation.

### Receiving Typing Notifications ###

The ReceiveTypingStateSnac is used to tell you of a buddy's typing state. The SNAC packet
> has the following format:

[[Include(ReceiveTypingStateSnac/Format)]]

For details, see the ReceiveTypingStateSnac documentation.