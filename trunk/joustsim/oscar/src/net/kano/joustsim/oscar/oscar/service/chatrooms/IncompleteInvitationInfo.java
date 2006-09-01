/*
 * Copyright (c) 2005, Your Corporation. All Rights Reserved.
 */

/*
 * Created by IntelliJ IDEA.
 * User: keithkml
 * Date: Dec 5, 2005
 * Time: 10:17:50 AM
 */

package net.kano.joustsim.oscar.oscar.service.chatrooms;

import net.kano.joustsim.Screenname;
import net.kano.joscar.snaccmd.MiniRoomInfo;
import net.kano.joscar.ByteBlock;
import net.kano.joscar.rv.RvSession;

import javax.crypto.SecretKey;
import java.security.cert.X509Certificate;

public class IncompleteInvitationInfo {
  private final Screenname sn;
  private final MiniRoomInfo roomInfo;
  private final String msgString;
  private final SecretKey roomKey;
  private final ByteBlock securityInfo;
  private final InvalidInvitationReason reason;
  private final X509Certificate buddyCert;
  private final RvSession session;

  public IncompleteInvitationInfo(RvSession session, Screenname sn,
      MiniRoomInfo roomInfo, String msgString, SecretKey roomKey,
      ByteBlock securityInfo, InvalidInvitationReason reason,
      X509Certificate buddyCert) {
    this.session = session;
    this.sn = sn;
    this.roomInfo = roomInfo;
    this.msgString = msgString;
    this.roomKey = roomKey;
    this.securityInfo = securityInfo;
    this.reason = reason;
    this.buddyCert = buddyCert;
  }

  public RvSession getSession() {
    return session;
  }

  public Screenname getSn() {
    return sn;
  }

  public MiniRoomInfo getRoomInfo() {
    return roomInfo;
  }

  public String getMsgString() {
    return msgString;
  }

  public SecretKey getRoomKey() {
    return roomKey;
  }

  public ByteBlock getSecurityInfo() {
    return securityInfo;
  }

  public InvalidInvitationReason getReason() {
    return reason;
  }

  public X509Certificate getBuddyCert() {
    return buddyCert;
  }

  
}
