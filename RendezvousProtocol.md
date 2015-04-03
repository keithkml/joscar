Description about 'RendezvousProtocol'

Table of contents:
[[TableOfContents](TableOfContents.md)]

## RendezvousProtocol ##

Basic instant messages on the OSCAR protocol are sent via InterClientBasicMsg (ICBM) structures on ICBM channel 2.
ICBM channel 2 is used for 'File Transfer' and 'Complex Messages (rtf, utf8)'.
cf. http://iserverd1.khstu.ru/oscar/snac_04_06.html

Before to introduce each ICBM format for 'file transfer', we need to know entire file transfer work-flows.

> i. Sender sends an ICBM rendezvous request with the sender�s IP and info.
> i. Receiver connects to the sender�s IP based on info in the request.
> i. If there was a successful connection, negotiation proceeds to step 8.
> i. If connection fails (due to firewall, LAN, or router), the receiver sends an ICBM telling the sender to connect to the receiver instead, with the receiver�s IP & port.
> i. If there was a successful connection, negotiation proceeds to step 8.
> i. If this connection fails, the sender establishes a connection with an AOL proxy server, and sends a rendezvous file transfer ICBM with the IP and port of the proxy server to the receiver.
> i. The receiver parses the ICBM and connects to the proxy server.
> > In the case of an AOL proxy server, some extra data is exchanged over the connection:
      * The receiver must send an initial proxy negotiation packet that is used to inform the proxy server of the transfer details.
      * The proxy server responds with a 12 byte packet acknowledging the connection. Then, normal FT (Step 8) continues.

> i. If the connection succeeds, the client (whoever connected to the other person�s IP, receiver in the case of a proxy server) sends an ICBM acknowledgment packet that indicates connection was successful.
> i. Sender sends a 256-byte negotiation packet over the FT connection.
> i. Receiver responds with the same packet, but the 7th and 8th bytes changed to 0x0202 and the next 8 bytes (9-16) changed to match the ICBM cookie used during negotiation.
> i. Sender begins to push the file through the FT connection as raw data, no encapsulation.
> i. When the file completes, receiver should know and send the negotiation packet again (with ICBM cookie) but instead of 0x0202 as the 7th and 8th bytes, the packet should have 0x0204.
> i. Connection is dropped on both sides.

( Notice. I borrow above contents from 'TerraIM\_1.0\_source/src/oscar/TerraIM OSCAR Implementation.html')

### Outgoing Rendezvous Format ###

[[Include(OutgoingICBMSnac)]]

[[Include(RendezvousBlock)]]