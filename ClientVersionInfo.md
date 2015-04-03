A client version information block is sent in the InitialAuthentication authentication request. It has no true structure; it is only a set of TLV's present in the auth request TLV chain.

The TLV's are as follows:

[[Include(/Format)]]

The meaning of these values is not very important; what is important for most developers is using the same values as one of the official clients. The values used by the "official" OSCAR clients are as follows:

| `` | Windows AIM 5.9 | iChat 1.0 | ICQ 2003a |
|:---|:----------------|:----------|:----------|
| Ver. string | "AOL Instant Messenger, version 5.9.3702/WIN32" | "Apple iChat" | "ICQ Inc. - Product of ICQ (TM).2003a.5.45.1.3777.85" |
| Client ID | 0x0109 | 0x311a | 0x010a |
| Major ver. | 5 | 1 | 5 |
| Minor ver. | 9 | 0 | 45 |
| Point ver. | 0 | 0 | 1 |
| Build no. | 3702 | 60 | 3777 |
| Distro. code | 272 | 198 | 85 |