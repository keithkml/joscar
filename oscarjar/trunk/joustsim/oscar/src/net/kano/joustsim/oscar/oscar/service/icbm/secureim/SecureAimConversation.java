/*
 *  Copyright (c) 2004, The Joust Project
 *  All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without 
 *  modification, are permitted provided that the following conditions 
 *  are met:
 *
 *  - Redistributions of source code must retain the above copyright 
 *    notice, this list of conditions and the following disclaimer. 
 *  - Redistributions in binary form must reproduce the above copyright 
 *    notice, this list of conditions and the following disclaimer in 
 *    the documentation and/or other materials provided with the 
 *    distribution. 
 *  - Neither the name of the Joust Project nor the names of its 
 *    contributors may be used to endorse or promote products derived 
 *    from this software without specific prior written permission. 
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS 
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT 
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS 
 *  FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE 
 *  COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, 
 *  INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, 
 *  BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; 
 *  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER 
 *  CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT 
 *  LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN 
 *  ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
 *  POSSIBILITY OF SUCH DAMAGE.
 *
 *  File created by keith @ Jan 25, 2004
 *
 */

package net.kano.joustsim.oscar.oscar.service.icbm.secureim;

import net.kano.joscar.ByteBlock;
import net.kano.joscar.DefensiveTools;
import net.kano.joscar.snaccmd.icbm.InstantMessage;
import net.kano.joustsim.Screenname;
import net.kano.joustsim.oscar.AimConnection;
import net.kano.joustsim.oscar.BuddyInfo;
import net.kano.joustsim.oscar.BuddyInfoManager;
import net.kano.joustsim.oscar.BuddyInfoTracker;
import net.kano.joustsim.oscar.BuddyInfoTrackerListener;
import net.kano.joustsim.oscar.GlobalBuddyInfoAdapter;
import net.kano.joustsim.oscar.GlobalBuddyInfoListener;
import net.kano.joustsim.oscar.oscar.service.icbm.Conversation;
import net.kano.joustsim.oscar.oscar.service.icbm.ConversationEventInfo;
import net.kano.joustsim.oscar.oscar.service.icbm.ConversationException;
import net.kano.joustsim.oscar.oscar.service.icbm.IcbmService;
import net.kano.joustsim.oscar.oscar.service.icbm.InternalIcbmTools;
import net.kano.joustsim.oscar.oscar.service.icbm.Message;
import net.kano.joustsim.oscar.oscar.NoBuddyKeysException;
import net.kano.joustsim.trust.BuddyCertificateInfo;
import net.kano.joustsim.trust.PrivateKeys;
import net.kano.joustsim.trust.PrivateKeysPreferences;
import net.kano.joustsim.trust.TrustPreferences;
import org.bouncycastle.cms.CMSException;

import java.beans.PropertyChangeEvent;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class SecureAimConversation extends Conversation {
  private static final Logger LOGGER = Logger
      .getLogger(SecureAimConversation.class.getName());

  private final AimConnection conn;
  private final BuddyInfoManager buddyInfoMgr;
  private final BuddyInfoTracker buddyInfoTracker;

  private SecureAimEncoder encoder = new SecureAimEncoder();
  private SecureAimDecoder decoder = new SecureAimDecoder();

  private PrivateKeys privates = null;
  private BuddyCertificateInfo buddyCertInfo = null;

  private Map<ByteBlock, LinkedList<QueuedDecryptableMsg>> decryptables
      = new HashMap<ByteBlock, LinkedList<QueuedDecryptableMsg>>();

  private boolean canSend = false;

  private final BuddyInfoTrackerListener trackerListener
      = new BuddyInfoTrackerListener() {
    // this does nothing??
  };
  private final GlobalBuddyInfoListener buddyInfoListener
      = new GlobalBuddyInfoAdapter() {
    public void buddyInfoChanged(BuddyInfoManager manager, Screenname buddy,
        BuddyInfo info, PropertyChangeEvent event) {
      if (!buddy.equals(getBuddy())) return;

      if (event.getPropertyName().equals(BuddyInfo.PROP_CERTIFICATE_INFO)) {
        BuddyCertificateInfo certInfo
            = (BuddyCertificateInfo) event.getNewValue();
        setBuddyCerts(certInfo);
        tryQueuedMessages(certInfo);
      }
    }
  };

  protected SecureAimConversation(AimConnection conn, Screenname buddy) {
    super(buddy);

    DefensiveTools.checkNull(conn, "conn");

    this.conn = conn;
    buddyInfoMgr = conn.getBuddyInfoManager();
    buddyInfoTracker = conn.getBuddyInfoTracker();
  }

  protected void initialize() {
    TrustPreferences prefs = conn.getAimSession().getTrustPreferences();
    PrivateKeysPreferences keysMgr = prefs.getPrivateKeysPreferences();
    PrivateKeys pkinfo = keysMgr.getKeysInfo();
    Boolean canSendUpdated;
    synchronized (this) {
      privates = pkinfo;
      canSendUpdated = updateCanSend();
    }
    fireCanSendChanged(canSendUpdated);
  }

  private void tryQueuedMessages(BuddyCertificateInfo certInfo) {
    if (certInfo == null || !certInfo.isUpToDate()) return;

    List<? extends QueuedDecryptableMsg> decrypted = tryDecrypting(certInfo);

    for (Object aDecrypted : decrypted) {
      QueuedDecryptableMsg msg = (QueuedDecryptableMsg) aDecrypted;

      String decryptedMsg = msg.getDecryptedForm().getMessage();
      handleDecrypted(msg.getMessageInfo(), decryptedMsg);
    }
  }

  private synchronized List<QueuedDecryptableMsg> tryDecrypting(
      BuddyCertificateInfo certInfo) {
    ByteBlock hash = certInfo.getCertificateInfoHash();
    if (hash == null) return DefensiveTools.emptyList();

    List<QueuedDecryptableMsg> forHash = tryDecryptingWithHash(certInfo, hash);
    List<QueuedDecryptableMsg> hashless = tryDecryptingWithHash(certInfo, null);
    // if one of the lists is empty, we don't need to combine them
    if (forHash.isEmpty()) return hashless;
    if (hashless.isEmpty()) return forHash;

    List<QueuedDecryptableMsg> combined
        = new ArrayList<QueuedDecryptableMsg>(forHash.size()
        + hashless.size());
    combined.addAll(forHash);
    combined.addAll(hashless);

    return combined;
  }

  private synchronized List<QueuedDecryptableMsg> tryDecryptingWithHash(
      BuddyCertificateInfo certInfo, ByteBlock hash) {
    List<QueuedDecryptableMsg> decHash = decryptables.get(hash);
    if (decHash == null) return DefensiveTools.emptyList();

    decoder.setBuddyCerts(certInfo);
    List<QueuedDecryptableMsg> success = null;
    for (Iterator<QueuedDecryptableMsg> it = decHash.iterator(); it.hasNext();)
    {
      QueuedDecryptableMsg msg = it.next();

      ByteBlock encrypted = msg.getMessage().getEncryptedForm();
      try {
        decoder.setLocalKeys(msg.getPrivates());
        DecryptedMessageInfo decrypted
            = decoder.decryptMessage(encrypted);
        it.remove();
        msg.setDecryptedForm(decrypted);
        if (success == null) success = new ArrayList<QueuedDecryptableMsg>();
        success.add(msg);

      } catch (Exception expected) {
        continue;
      }
    }
    if (decHash.isEmpty()) decryptables.remove(hash);

    if (success == null) {
      return DefensiveTools.emptyList();
    } else {
      return success;
    }
  }

  private void setBuddyCerts(BuddyCertificateInfo certInfo) {
    Boolean updated;
    synchronized (this) {
      buddyCertInfo = certInfo;
      updated = updateCanSend();
    }
    fireCanSendChanged(updated);
  }

  private void fireCanSendChanged(Boolean updated) {
    if (updated != null) fireCanSendChangedEvent(updated);
  }

  private synchronized Boolean updateCanSend() {
    BuddyCertificateInfo buddyCertInfo = this.buddyCertInfo;
    boolean can = privates != null && buddyCertInfo != null
        && buddyCertInfo.hasBothCertificates();

    if (can != canSend) {
      canSend = can;
      return can;
    } else {
      // the can-send status did not change
      return null;
    }
  }

  protected synchronized void opened() {
    LOGGER.fine("secure conversation opened");

    Screenname buddy = getBuddy();
    buddyInfoMgr.addGlobalBuddyInfoListener(buddyInfoListener);
    buddyInfoTracker.addTracker(buddy, trackerListener);
    BuddyInfo buddyInfo = buddyInfoMgr.getBuddyInfo(buddy);
    setBuddyCerts(buddyInfo.getCertificateInfo());
  }

  protected void closed() {
    buddyInfoMgr.removeGlobalBuddyInfoListener(buddyInfoListener);
    buddyInfoTracker.removeTracker(getBuddy(), trackerListener);
  }

  public synchronized boolean canSendMessage() {
    return canSend;
  }

  public void sendMessage(Message msg)
      throws ConversationException {
    IcbmService icbmService = conn.getIcbmService();
    if (icbmService == null) {
      throw new ConversationException("no ICBM service present to "
          + "send through", this);
    }

    byte[] encrypted;
    PrivateKeys privates;
    BuddyCertificateInfo buddyCertInfo;
    try {
      synchronized (this) {
        privates = this.privates;
        buddyCertInfo = this.buddyCertInfo;

        encoder.setLocalKeys(privates);
        encoder.setBuddyCerts(buddyCertInfo);
        encrypted = encoder.encryptMsg(msg.getMessageBody());
      }

    } catch (NoBuddyKeysException e) {
      throw new NotTrustedException(e, this);

    } catch (Exception e) {
      throw new EncryptionFailedException(e, this);
    }

    InstantMessage im = new InstantMessage(ByteBlock.wrap(encrypted));
    InternalIcbmTools.sendIM(icbmService, getBuddy(), im, msg.isAutoResponse());
    OutgoingSecureAimMessageInfo msginfo = OutgoingSecureAimMessageInfo
        .getInstance(conn.getScreenname(), getBuddy(), msg, new Date(),
            privates, buddyCertInfo, im);
    fireOutgoingEvent(msginfo);
  }

  protected void handleIncomingEvent(ConversationEventInfo minfo) {
    open();

    if (!(minfo instanceof EncryptedAimMessageInfo)) {
      throw new IllegalArgumentException("SecureAimConversation can't "
          + "handle incoming message objects of type "
          + minfo.getClass().getName() + " (" + minfo + ")");
    }

    EncryptedAimMessageInfo info = (EncryptedAimMessageInfo) minfo;
    EncryptedAimMessage msg = (EncryptedAimMessage) info.getMessage();

    BuddyCertificateInfo certInfo = info.getMessageCertificates();
    String decryptedMsg = null;
    Exception exception = null;
    synchronized (this) {
      try {
        decoder.setLocalKeys(privates);
        decoder.setBuddyCerts(info.getMessageCertificates());
        ByteBlock enc = msg.getEncryptedForm();
        DecryptedMessageInfo decrypted = decoder.decryptMessage(enc);

        if (decrypted != null) decryptedMsg = decrypted.getMessage();
      } catch (Exception e) {
        exception = e;
      }

      if (exception != null
          && (exception instanceof NoLocalKeysException
          || exception instanceof NoBuddyKeysException)) {
        queueDecryptable(info);
      }
    }

    if (exception != null) {
      if (exception instanceof NoSuchProviderException
          || exception instanceof NoSuchAlgorithmException
          || exception instanceof CMSException) {
        handleUndecryptable(info, certInfo,
            UndecryptableAimMessageInfo.Reason.DECRYPT_ERROR,
            exception);

      } else if (exception instanceof InvalidSignatureException) {
        handleUndecryptable(info,
            certInfo,
            UndecryptableAimMessageInfo.Reason.BAD_SIGNATURE,
            exception);

      } else if (exception instanceof NoLocalKeysException
          || exception instanceof NoBuddyKeysException) {
        handleDecryptable(info, certInfo);

      } else {
        handleUndecryptable(info, certInfo,
            UndecryptableAimMessageInfo.Reason.UNKNOWN, exception);
      }
      return;
    }

    if (decryptedMsg == null) {
      handleUndecryptable(info, certInfo,
          UndecryptableAimMessageInfo.Reason.UNKNOWN, null);
      return;
    }

    handleDecrypted(info, decryptedMsg);
  }

  private void handleDecrypted(EncryptedAimMessageInfo info,
      String decryptedMsg) {
    BuddyCertificateInfo certInfo = info.getMessageCertificates();
    DecryptedAimMessageInfo dinfo = DecryptedAimMessageInfo.getInstance(
        info, decryptedMsg, certInfo);

    fireIncomingEvent(dinfo);
  }

  private void handleUndecryptable(EncryptedAimMessageInfo info,
      BuddyCertificateInfo certInfo,
      UndecryptableAimMessageInfo.Reason reason, Exception exception) {

    UndecryptableAimMessageInfo minfo
        = UndecryptableAimMessageInfo.getInstance(info, certInfo,
        reason, exception);
    fireIncomingEvent(minfo);
  }

  private synchronized void queueDecryptable(EncryptedAimMessageInfo info) {
    QueuedDecryptableMsg queued = new QueuedDecryptableMsg(privates, info);

    ByteBlock hash = queued.getCertificateHash();

    LinkedList<QueuedDecryptableMsg> queue = decryptables.get(hash);
    if (queue == null) {
      queue = new LinkedList<QueuedDecryptableMsg>();
      decryptables.put(hash, queue);
    }
    queue.addLast(queued);
  }

  private void handleDecryptable(EncryptedAimMessageInfo info,
      BuddyCertificateInfo certInfo) {
    fireIncomingEvent(DecryptableAimMessageInfo.getInstance(info,
        certInfo));
  }

  private static class QueuedDecryptableMsg {
    private final PrivateKeys privates;
    private final BuddyCertificateInfo buddyCerts;
    private final EncryptedAimMessageInfo msgInfo;
    private final EncryptedAimMessage emsg;
    private final ByteBlock certHash;
    private DecryptedMessageInfo decryptedForm = null;

    public QueuedDecryptableMsg(PrivateKeys privates,
        EncryptedAimMessageInfo msgInfo) {
      DefensiveTools.checkNull(privates, "privates");
      DefensiveTools.checkNull(msgInfo, "msg");

      EncryptedAimMessage emsg = (EncryptedAimMessage) msgInfo.getMessage();
      if (emsg == null) throw new IllegalArgumentException("empty message");

      this.privates = privates;
      this.msgInfo = msgInfo;
      this.emsg = emsg;
      BuddyCertificateInfo certs = msgInfo.getMessageCertificates();
      this.buddyCerts = certs;
      if (certs == null) {
        this.certHash = null;
      } else {
        this.certHash = certs.getCertificateInfoHash();
      }
    }

    public PrivateKeys getPrivates() {
      return privates;
    }

    public BuddyCertificateInfo getBuddyCerts() {
      return buddyCerts;
    }

    public EncryptedAimMessageInfo getMessageInfo() {
      return msgInfo;
    }

    public EncryptedAimMessage getMessage() {
      return emsg;
    }

    public ByteBlock getCertificateHash() {
      return certHash;
    }

    public synchronized void setDecryptedForm(
        DecryptedMessageInfo decryptedForm) {
      this.decryptedForm = decryptedForm;
    }

    public synchronized DecryptedMessageInfo getDecryptedForm() {
      return decryptedForm;
    }
  }
}
