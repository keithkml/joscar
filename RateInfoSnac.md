The rate limiting information SNAC is sent by the OSCAR server as a SnacResponse to a RateInfoRequestSnac while LoggingIn. The client normally responds to this packet with a RateAckSnac. The format of this packet is:

[[Include(/Format)]]

The SNAC commands in the rate class `rateClassInfoN` are listed in `rateClassCmdsN`.

For details on rate limiting, see RateLimiting.