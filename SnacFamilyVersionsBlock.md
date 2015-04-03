The SNAC family versions block is used to store version information for a SNAC family. This structure is used solely in the ClientReadySnac. The format of this data structure is as follows:

[[Include(/Format)]]

`family` is the SNAC family ID. `version` is the version of that family supported. `toolID` describes which AIM module is being used to communicate with the family; `toolVersion` is the version of that module.

Since your client does not use the AIM modules, it should probably emulate the values sent by the official AIM clients. See ClientReadySnac for details.