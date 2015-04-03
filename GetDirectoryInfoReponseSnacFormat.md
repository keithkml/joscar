All the the fields are optional, and not all may be included in the packet.

  * SnacCommand family 0x0002, 0x000C
    * UnsignedShort: `directoryInfoType`
    * UnsignedShort: `numberOfFields`
    * TlvChain
      * TlvBlock: type 0x0001
        * AsciiString: `firstName`
      * TlvBlock: type 0x0002
        * AsciiString: `lastName`
      * TlvBlock: type 0x0003
        * AsciiString: `middleName`
      * TlvBlock: type 0x0004
        * AsciiString: `maidenName`
      * TlvBlock: type 0x0005
        * AsciiString: `emailAddress`
      * TlvBlock: type 0x0006
        * AsciiString: `country`
      * TlvBlock: type 0x0007
        * AsciiString: `state`
      * TlvBlock: type 0x0008
        * AsciiString: `city`
      * TlvBlock: type 0x0009
        * AsciiString: `screenName`
      * TlvBlock: type 0x000C
        * AsciiString: `nickName`
      * TlvBlock: type 0x000D
        * AsciiString: `zipCode`
      * TlvBlock: type 0x0021
        * AsciiString: `streetAddress`