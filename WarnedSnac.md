The warning notification SNAC is sent by the OSCAR server when a buddy Warns you. The format of this packet is:

[[Include(/Format)]]

`level` is your new warning level, multiplied by 10. For example, if your warning level is now 25%, this value will be 250.

`warner` is a mini user info block for the user who warned you, if the warning was not anonymous. If the warning was anonymous, there will be no data after `level`.