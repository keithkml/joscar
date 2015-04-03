The authorization request SNAC is sent by the OSCAR client during the InitialAuthentication process. The server normally responds to this packet with an AuthResponseSnac. The format of this packet is:

[[Include(/Format)]]

`screenname` is the user's screenname. `country` is a country code, like "us". `lang` is a language code, like "en". `encryptedPass` is the encrypted password data.