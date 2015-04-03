Some features of the official AIM clients (AIM and iChat, mainly) do not use the OSCAR protocol.

### Advertisements ###

AOL's official clients display advertisements at the top of the buddy list window. Originally, these ads were retrieved over the OSCAR protocol using SNAC family 0x05. This family has been removed, however, and modern clients use HTTP to retrieve advertisements.

TODO: find sample ad URL

### Downloading AIM Expressions ###

AIM Expressions are downloaded by the AIM clients over HTTP from http://aimexp.aim.com.

TODO: find sample expression URL

### Buddy Numbers ###

While Buddy Comments are stored on the server, Buddy Numbers (a set of phone numbers associated with a buddy) are not stored anywhere but on your computer.

### Changing Passwords ###

AIM used to allow someone to change his or her password over OSCAR in the AccountAdmin family. This is no longer used; modern AIM clients redirect to the AIM website to change your password over a SSL connection.

It may still be possible to change the user's password over AIM using the old SNAC command, but it has been deprecated for a reason: this method is not secure.

### Managing Linked Screen Names ###

Managing the LinkedScreenNames feature is done via a web browser at the site http://www.aim.com/redirects/inclient/linkingsn.adp. The web site appears to update the user's ServerStoredInfo by itself.