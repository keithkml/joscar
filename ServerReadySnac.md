The server ready SNAC is sent by the OSCAR server after FLAP version information has been exchanged. This command is normally the first SNAC command sent on a non-InitialAuthentication OSCAR connection; it indicates that the client can begin to log in. The format of this packet is:

[[Include(/Format)]]

The SNAC data will contain a list of the SNAC families that the server supports, as a sequence of UnsignedShort``s. For details, see LoggingIn.