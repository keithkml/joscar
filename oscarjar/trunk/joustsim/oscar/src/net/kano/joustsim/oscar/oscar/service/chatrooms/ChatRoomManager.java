/*
 *  Copyright (c) 2005, The Joust Project
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
 */

package net.kano.joustsim.oscar.oscar.service.chatrooms;

import net.kano.joscar.BinaryTools;
import net.kano.joscar.ByteBlock;
import net.kano.joscar.CopyOnWriteArrayList;
import net.kano.joscar.rv.RecvRvEvent;
import net.kano.joscar.rv.RvSession;
import net.kano.joscar.rv.RvSnacResponseEvent;
import net.kano.joscar.rvcmd.chatinvite.ChatInvitationRvCmd;
import net.kano.joscar.snaccmd.CapabilityBlock;
import net.kano.joscar.snaccmd.MiniRoomInfo;
import net.kano.joscar.snaccmd.icbm.RvCommand;
import net.kano.joustsim.Screenname;
import net.kano.joustsim.oscar.AbstractCapabilityHandler;
import net.kano.joustsim.oscar.AimConnection;
import net.kano.joustsim.oscar.BuddyInfoManager;
import net.kano.joustsim.oscar.CapabilityManager;
import net.kano.joustsim.oscar.oscar.BasicConnection;
import net.kano.joustsim.oscar.oscar.NoBuddyKeysException;
import net.kano.joustsim.oscar.oscar.OscarConnection;
import net.kano.joustsim.oscar.oscar.service.Service;
import net.kano.joustsim.oscar.oscar.service.ServiceListener;
import net.kano.joustsim.oscar.oscar.service.icbm.IcbmService;
import net.kano.joustsim.oscar.oscar.service.icbm.RendezvousCapabilityHandler;
import net.kano.joustsim.oscar.oscar.service.icbm.RendezvousSessionHandler;
import net.kano.joustsim.trust.BuddyCertificateInfo;
import net.kano.joustsim.trust.KeyPair;
import net.kano.joustsim.trust.PrivateKeys;
import net.kano.joustsim.trust.PrivateKeysPreferences;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.cms.KeyTransRecipientInfo;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.SignerInformation;
import org.jetbrains.annotations.NotNull;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

public class ChatRoomManager {
    private final AimConnection aimConnection;

    private CopyOnWriteArrayList<ChatRoomManagerListener> listeners
            = new CopyOnWriteArrayList<ChatRoomManagerListener>();

    private WeakHashMap<ChatInvitationImpl, Object> acceptedInvitations
            = new WeakHashMap<ChatInvitationImpl, Object>();

    private Map<OscarConnection,ChatRoomSession> sessions
            = new HashMap<OscarConnection, ChatRoomSession>();

    public ChatRoomManager(AimConnection conn) {
        this.aimConnection = conn;
        conn.getCapabilityManager().setCapabilityHandler(
                CapabilityBlock.BLOCK_CHAT,
                new ChatInvitationCapabilityHandler());
        RoomFinderServiceArbiter arbiter= conn.getExternalServiceManager()
                .getChatRoomFinderServiceArbiter();
        arbiter.addRoomManagerServiceListener(new MyRoomManagerServiceListener());
    }

    public AimConnection getAimConnection() {
        return aimConnection;
    }

    public void addListener(ChatRoomManagerListener listener) {
        listeners.addIfAbsent(listener);
    }

    public void removeListener(ChatRoomManagerListener listener) {
        listeners.remove(listener);
    }

    public ChatRoomSession acceptInvitation(ChatInvitation inv)
            throws IllegalArgumentException {
        if (!(inv instanceof ChatInvitationImpl)) {
            throw new IllegalArgumentException("Invitation was "
                    + "not received by this chat manager: " + inv);
        }
        ChatInvitationImpl chatInvitation = (ChatInvitationImpl) inv;
        if (chatInvitation.getChatRoomManager() != this) {
            throw new IllegalArgumentException("Invitation was "
                    + "not received by this chat manager: " + inv);
        }

        if (!chatInvitation.isValid()) {
            throw new IllegalArgumentException("Chat invitation is not valid: "
                    + inv);
        }

        synchronized (this) {
            if (acceptedInvitations.containsKey(chatInvitation)) {
                //TODO: return the corresponding session
                throw new InternalError();
            }
            acceptedInvitations.put(chatInvitation, null);
        }

        getChatArbiter().joinChatRoom(chatInvitation.getRoomInfo());
        //TODO: return a session
        throw new InternalError();
    }

    public ChatRoomSession joinRoom(String name) {
        return joinRoom(4, name);
    }

    public ChatRoomSession joinRoom(int exchange, String name) {
        RoomFinderServiceArbiter arbiter = getChatArbiter();
        arbiter.joinChatRoom(exchange, name);

        //TODO: return a session
        throw new InternalError();
    }

    private RoomFinderServiceArbiter getChatArbiter() {
        return aimConnection.getExternalServiceManager()
                .getChatRoomFinderServiceArbiter();
    }

    private class ChatInvitationCapabilityHandler
            extends AbstractCapabilityHandler
            implements RendezvousCapabilityHandler {
        public RendezvousSessionHandler handleSession(IcbmService service,
                RvSession session) {
            return new MyRendezvousSessionHandler();
        }

        public void handleAdded(CapabilityManager manager) {
        }

        public void handleRemoved(CapabilityManager manager) {
        }
    }

    private class MyRendezvousSessionHandler
            implements RendezvousSessionHandler {
        public void handleRv(RecvRvEvent event) {
            RvCommand cmd = event.getRvCommand();
            if (cmd instanceof ChatInvitationRvCmd) {
                ChatInvitationRvCmd invitation = (ChatInvitationRvCmd) cmd;
                Screenname sn = new Screenname(
                        event.getRvSession().getScreenname());
                SecretKey roomKey = null;
                InvalidInvitationReason reason = null;
                X509Certificate buddyCert = null;
                ByteBlock securityInfo = invitation.getSecurityInfo();
                if (securityInfo != null) {
                    try {
                        buddyCert = getBuddySigningCert(sn);
                        //noinspection ConstantConditions
                        assert buddyCert != null;
                    } catch (NoBuddyKeysException e) {
                        reason = InvalidInvitationReason.NO_BUDDY_KEYS;
                    }
                    try {
                        roomKey = extractChatKey(securityInfo, buddyCert);
                        //noinspection ConstantConditions
                        assert roomKey != null;
                    } catch (NoPrivateKeyException e) {
                        reason = InvalidInvitationReason.NO_LOCAL_KEYS;
                    } catch (CertificateNotYetValidException e) {
                        reason = InvalidInvitationReason.CERT_NOT_YET_VALID;
                    } catch (CertificateExpiredException e) {
                        reason = InvalidInvitationReason.CERT_EXPIRED;
                    } catch (BadKeyException e) {
                        reason = InvalidInvitationReason.INVALID_SIGNATURE;
                    }
                }
                ChatInvitation ourInvitation;
                MiniRoomInfo roomInfo = invitation.getRoomInfo();
                String msgString = invitation.getInvMessage().getMessage();
                if (securityInfo == null) {
                    ourInvitation = new ChatInvitationImpl(ChatRoomManager.this,
                            sn, roomInfo,
                            msgString);
                } else if (roomKey == null) {
                    ourInvitation = new ChatInvitationImpl(ChatRoomManager.this,
                            sn, roomInfo, reason,
                            msgString);

                } else {
                    assert buddyCert != null;
                    ourInvitation = new ChatInvitationImpl(ChatRoomManager.this,
                            sn, roomInfo,
                            buddyCert, roomKey, msgString);
                }

                for (ChatRoomManagerListener listener : listeners) {
                    listener.handleInvitation(ChatRoomManager.this,
                            ourInvitation);
                }
            }
        }

        public void handleSnacResponse(RvSnacResponseEvent event) {
        }


        private @NotNull SecretKey extractChatKey(ByteBlock data,
                X509Certificate buddySigningCert)
                throws NoPrivateKeyException, CertificateNotYetValidException,
                CertificateExpiredException, BadKeyException {

            KeyPair myEncryptingKeys = getMyEncryptingKeys();

            try {
                CMSSignedData csd
                        = new CMSSignedData(ByteBlock.createInputStream(data));
                Collection<SignerInformation> signers = csd.getSignerInfos()
                        .getSigners();
                for (SignerInformation signer : signers) {
                    boolean verified;
                    try {
                        verified = signer.verify(buddySigningCert, "BC");
                    } catch (CertificateExpiredException e) {
                        throw e;
                    } catch (CertificateNotYetValidException e) {
                        throw e;
                    }
                    if (!verified) throw new BadKeyException();
                }
                CMSProcessableByteArray cpb
                        = (CMSProcessableByteArray) csd.getSignedContent();
                ByteBlock signedContent = ByteBlock
                        .wrap((byte[]) cpb.getContent());
                MiniRoomInfo mri = MiniRoomInfo.readMiniRoomInfo(signedContent);

                ByteBlock rest = signedContent.subBlock(mri.getTotalSize());
                int kdlen = BinaryTools.getUShort(rest, 0);
                ByteBlock keyData = rest.subBlock(2, kdlen);

                InputStream kdin = ByteBlock.createInputStream(keyData);
                ASN1InputStream ain = new ASN1InputStream(kdin);
                ASN1Sequence root = (ASN1Sequence) ain.readObject();
                ASN1Sequence seq = (ASN1Sequence) root.getObjectAt(0);
                KeyTransRecipientInfo ktr = KeyTransRecipientInfo
                        .getInstance(seq);
                DERObjectIdentifier keyoid
                        = (DERObjectIdentifier) root.getObjectAt(1);

                String encoid = ktr.getKeyEncryptionAlgorithm().getObjectId()
                        .getId();
                Cipher cipher = Cipher.getInstance(encoid, "BC");
                cipher.init(Cipher.DECRYPT_MODE,
                        myEncryptingKeys.getPrivateKey());

                byte[] result = cipher
                        .doFinal(ktr.getEncryptedKey().getOctets());
                return new SecretKeySpec(result, keyoid.getId());

            } catch (NoSuchProviderException e) {
            } catch (BadPaddingException e) {
            } catch (NoSuchAlgorithmException e) {
            } catch (IOException e) {
            } catch (IllegalBlockSizeException e) {
            } catch (InvalidKeyException e) {
            } catch (NoSuchPaddingException e) {
            } catch (CMSException e) {
            }
            throw new BadKeyException();
        }

        private @NotNull KeyPair getMyEncryptingKeys()
                throws NoPrivateKeyException {
            PrivateKeysPreferences pkPrefs = aimConnection.getLocalPrefs()
                    .getPrivateKeysPreferences();
            PrivateKeys keysInfo = pkPrefs.getKeysInfo();
            if (keysInfo == null) throw new NoPrivateKeyException();
            return keysInfo.getEncryptingKeys();
        }

        private @NotNull X509Certificate getBuddySigningCert(Screenname sn)
                throws
                NoBuddyKeysException {
            BuddyInfoManager infoMgr = aimConnection.getBuddyInfoManager();
            BuddyCertificateInfo certInfo = infoMgr.getBuddyInfo(sn)
                    .getCertificateInfo();
            if (certInfo == null) throw new NoBuddyKeysException();
            X509Certificate buddySigningCert = certInfo.getSigningCertificate();
            if (buddySigningCert == null) throw new NoBuddyKeysException();
            return buddySigningCert;
        }
    }

    private class MyRoomManagerServiceListener implements
            RoomManagerServiceListener {
        public void handleNewChatRoom(RoomManagerService service,
                MiniRoomInfo roomInfo, BasicConnection connection) {
            ChatRoomSession session = new ChatRoomSession(aimConnection,
                    connection, roomInfo);
            sessions.put(connection, session);
            connection.addGlobalServiceListener(new MyServiceListener(session));
        }
    }

    private class MyServiceListener implements ServiceListener {
        private ChatRoomSession session;

        public MyServiceListener(ChatRoomSession session) {
            this.session = session;
        }

        public void handleServiceReady(Service service) {
            if (!(service instanceof ChatRoomService)) return;

            ChatRoomService roomService = (ChatRoomService) service;
            for (ChatInvitationImpl invitation : acceptedInvitations.keySet()) {
                MiniRoomInfo invInfo = invitation.getRoomInfo();
                if (invInfo.getExchange() == session.getRoomInfo().getExchange()
                        && roomService.getRoomName().equals(invitation.getRoomName())) {
                    session.setInvitation(invitation);
                    //TODO: find fullroominfo before joining room
//                    String contentType = roomInfo.getContentType();
//                    if (contentType.equals(ChatMsg.CONTENTTYPE_SECURE)) {
//                        roomService.setMessageFactory(
//                                new EncryptedChatRoomMessageFactory(
//                                        aimConnection, roomService,
//                                        invitation.getRoomKey()));
//                    } else {
//                        roomService.setMessageFactory(
//                                new PlainChatRoomMessageFactory());
//                    }
                    break;
                }
            }
        }

        public void handleServiceFinished(Service service) {
        }
    }
}
