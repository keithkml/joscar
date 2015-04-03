The rate class member command list block is used to describe which SNAC commands are part of a [wiki:RateClassInfoBlock rate class]. This is used in RateInfoSnac``s. The format of this data structure is as follows:

[[Include(/Format)]]

`rateClass` is the rate class whose member SNAC commands are listed in this block. `numCommands` is the number of family-type pairs contained in the block. Each `familyN` and `typeN` pair describes a SNAC command type that is a member of the given rate class.