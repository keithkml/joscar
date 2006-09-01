package net.kano.joustsim.oscar.oscar.service.chatrooms;

import net.kano.joustsim.Screenname;

import java.security.cert.X509Certificate;

public interface ChatInvitation {
  Screenname getScreenname();
  int getRoomExchange();
  String getRoomName();
  String getMessage();
  InvalidInvitationReason getInvalidReason();
  X509Certificate getBuddySignature();
  boolean isValid();
  boolean isForSecureChatRoom();
  ChatRoomSession accept();
  void reject();
}
