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

package net.kano.joustsim.oscar.oscar.service.icbm.ft;

import net.kano.joscar.CopyOnWriteArrayList;
import net.kano.joscar.flapcmd.SnacCommand;
import net.kano.joscar.rv.RecvRvEvent;
import net.kano.joscar.rv.RvSession;
import net.kano.joscar.rv.RvSnacResponseEvent;
import net.kano.joscar.rvcmd.InvitationMessage;
import net.kano.joscar.rvcmd.RvConnectionInfo;
import net.kano.joscar.rvcmd.sendfile.FileSendAcceptRvCmd;
import net.kano.joscar.rvcmd.sendfile.FileSendBlock;
import net.kano.joscar.rvcmd.sendfile.FileSendReqRvCmd;
import net.kano.joscar.snaccmd.CapabilityBlock;
import net.kano.joscar.snaccmd.icbm.RecvRvIcbm;
import net.kano.joscar.snaccmd.icbm.RvCommand;
import net.kano.joustsim.oscar.CapabilityManager;
import net.kano.joustsim.oscar.DefaultEnabledCapabilityHandler;
import net.kano.joustsim.oscar.oscar.service.icbm.IcbmService;
import net.kano.joustsim.oscar.oscar.service.icbm.RendezvousCapabilityHandler;
import net.kano.joustsim.oscar.oscar.service.icbm.RendezvousSessionHandler;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.logging.Logger;

/*
Possible states:
1. Waiting for connection
2. Connecting
3. Waiting for proxy
4. Waiting for user to connect to proxy
*/
public class FileTransferManager {
    private static final Logger LOGGER = Logger.getLogger(FileTransferManager.class.getName());

    private final IcbmService service;
    private CopyOnWriteArrayList<FileTransferManagerListener> listeners
            = new CopyOnWriteArrayList<FileTransferManagerListener>();

    public FileTransferManager(IcbmService service) {
        this.service = service;
        CapabilityManager capMgr = service.getAimConnection().getCapabilityManager();
        capMgr.setCapabilityHandler(CapabilityBlock.BLOCK_FILE_SEND,
                new FileTransferCapabilityHandler());
    }

    public IcbmService getIcbmService() {
        return service;
    }

    private class FileTransferCapabilityHandler
            extends DefaultEnabledCapabilityHandler implements
            RendezvousCapabilityHandler {
        public RendezvousSessionHandler handleSession(IcbmService service,
                RvSession session) {
            FileTransfer transfer = new IncomingFileTransfer(session);
            return transfer.getRvSessionHandler();
        }
    }

    static class Key<V> {
        private final String name;

        public Key(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }
    static final Key<Long> KEY_REQUEST_ID = new Key<Long>("ACCEPT_ID");
    public abstract class FileTransfer {
        private RvConnectionInfo remoteConnectionInfo;
        private FileSendBlock fileInfo;
        private InvitationMessage message;
        private Timer timer = new Timer("File transfer timer");
        private RendezvousSessionHandler rvSessionHandler;
        private RvSession session;
        private StateController controller = null;
        private Map<Key<?>,Object> transferProperties = new HashMap<Key<?>, Object>();

        private FileTransfer(FileSendBlock fileInfo, InvitationMessage message) {
            this.fileInfo = fileInfo;
            this.message = message;
        }

        public FileTransfer(RvSession session) {
            this.session = session;
            rvSessionHandler = createSessionHandler();
        }

        protected abstract FtRvSessionHandler createSessionHandler();

        public void setInvitationMessage(InvitationMessage message) {
            this.message = message;
        }

        public void setFileInfo(FileSendBlock fileInfo) {
            this.fileInfo = fileInfo;
        }

        public Timer getTimer() { return timer; }

        public RvConnectionInfo getRemoteConnectionInfo() {
            return remoteConnectionInfo;
        }

        public FileSendBlock getFileInfo() {
            return fileInfo;
        }

        public InvitationMessage getMessage() {
            return message;
        }

        protected void startStateController(final StateController controller) {
            StateController last = null;
            startStateController(controller, last);
        }

        private void startStateController(final StateController controller,
                StateController last) {
            this.controller = controller;
            controller.addControllerListener(new ControllerListener() {
                public void handleControllerSucceeded(StateController c,
                        StateInfo info) {
                    goNext();
                }

                public void handleControllerFailed(StateController c,
                        StateInfo info) {
                    goNext();
                }

                private void goNext() {
                    controller.removeControllerListener(this);
                    startStateController(getNextStateController(), controller);
                }
            });
            controller.start(this, last);
        }

        protected StateController getStateController() { return controller; }

        abstract StateController getNextStateController();

        public void setRemoteConnectionInfo(RvConnectionInfo remoteConnectionInfo) {
            this.remoteConnectionInfo = remoteConnectionInfo;
        }

        public RendezvousSessionHandler getRvSessionHandler() {
            return rvSessionHandler;
        }

        RvSession getRvSession() {
            return session;
        }

        public synchronized <V> void putTransferProperty(Key<V> key, V value) {
            transferProperties.put(key, value);
        }

        public synchronized <V> V getTransferProperty(Key<V> key) {
            return (V) transferProperties.get(key);
        }

        public FileTransferManager getFileTransferManager() {
            return FileTransferManager.this;
        }

        protected abstract class FtRvSessionHandler implements RendezvousSessionHandler {
            public void handleRv(RecvRvEvent event) {
                RvCommand cmd = event.getRvCommand();
                if (cmd instanceof FileSendReqRvCmd) {
                    FileSendReqRvCmd reqCmd = (FileSendReqRvCmd) cmd;
                    handleIncomingRequest(event, reqCmd);
                }
            }

            protected abstract void handleIncomingRequest(RecvRvEvent event,
                    FileSendReqRvCmd reqCmd);

            public void handleSnacResponse(RvSnacResponseEvent event) {
            }

            protected FileTransfer getFileTransfer() {
                return FileTransfer.this;
            }
        }
    }

    private class IncomingFileTransfer extends FileTransfer {
        private SimpleConnectingController externalController
                = new SimpleConnectingController(OutgoingConnectionType.EXTERNAL);
        private SimpleConnectingController internalController
                = new SimpleConnectingController(OutgoingConnectionType.INTERNAL);
        private ReverseController reverseController = new ReverseController();
        private ConnectToProxyController proxyController
                = new ConnectToProxyController();
        private ReceiveFileController receiveController = new ReceiveFileController();

        private StateController proxyReverseController = new ProxyRedirectController();
        private List<StateController> connControllers = Arrays.asList(
            externalController,
            internalController,
            proxyController,
            reverseController,
            proxyReverseController
        );
        private StateController lastConnController = null;

        public IncomingFileTransfer(RvSession session) {
            super(session);
        }

        protected FileTransfer.FtRvSessionHandler createSessionHandler() {
            return new IncomingFtpRvSessionHandler();
        }

        protected StateController getNextStateController() {
            StateController oldController = getStateController();
            StateInfo oldState = oldController.getEndState();
            if (oldState instanceof Stream) {
                return changeStateToReceiver(oldController);

            } else if (oldState instanceof ExceptionStateInfo) {
                return changeStateFromError(oldController, oldState);

            } else {
                throw new IllegalStateException("unknown state " + oldState);
            }
        }

        private StateController changeStateToReceiver(
                StateController oldController) {
            if (oldController == externalController
                    || oldController == internalController
                    || oldController == reverseController
                    || oldController == proxyController) {
                return new ReceiveFileController();
            } else {
                throw new IllegalStateException("what state? " + oldController);
            }
        }

        private StateController changeStateFromError(StateController oldController,
                StateInfo oldState) {
            ExceptionStateInfo exInfo = (ExceptionStateInfo) oldState;

            int oldIndex = connControllers.indexOf(oldController);
            if (oldIndex == -1) {
                if (oldController == receiveController) {
                    if (lastConnController == null) {
                        throw new IllegalArgumentException("receiver must "
                                + "have been called before connection was "
                                + "attempted");
                    }
                    int oldConnIndex = connControllers.indexOf(lastConnController);
                    if (oldConnIndex == -1) {
                        throw new IllegalStateException("last connection "
                                + "controller is not in connControllers: "
                                + lastConnController);
                    } else {
                        if (oldConnIndex == connControllers.size()-1) {
                            System.out.println("FAILED 2");
                            return null;
                        } else {
                            StateController nextController
                                    = connControllers.get(oldConnIndex + 1);
                            lastConnController = nextController;
                            return nextController;
                        }
                    }
                } else {
                    throw new IllegalStateException("unknown old controller "
                            + oldController);
                }

            } else {
                if (oldIndex == connControllers.size()-1) {
                    // it's the last one
                    System.out.println("FAILED: couldn't connect");
                    return null;
                } else {
                    StateController nextController = connControllers.get(oldIndex + 1);
                    lastConnController = nextController;
                    return nextController;
                }
            }
        }

        private class IncomingFtpRvSessionHandler extends FtRvSessionHandler {
            protected void handleIncomingRequest(RecvRvEvent event,
                    FileSendReqRvCmd reqCmd) {
                int type = reqCmd.getRequestType();
                if (type == FileSendReqRvCmd.REQTYPE_INITIALREQUEST) {
                    setFileInfo(reqCmd.getFileSendBlock());
                    setInvitationMessage(reqCmd.getMessage());
                    setRemoteConnectionInfo(reqCmd.getConnInfo());

                    for (FileTransferManagerListener listener : listeners) {
                        listener.handleNewIncomingFileTransfer(
                                FileTransferManager.this, getFileTransfer());
                    }

                    Random random = new Random();
                    //TODO: find better ICBM ID tracking system
                    long icbmMessageId = random.nextInt(5000);
                    SnacCommand snacCommand = event.getSnacCommand();
                    if (snacCommand instanceof RecvRvIcbm) {
                        RecvRvIcbm recvRvIcbm = (RecvRvIcbm) snacCommand;
                        putTransferProperty(KEY_REQUEST_ID, recvRvIcbm.getIcbmMessageId());
                    }
                    getRvSession().sendRv(new FileSendAcceptRvCmd(),
                            icbmMessageId);
                    startStateController(externalController);

                } else if (type == FileSendReqRvCmd.REQTYPE_REDIRECT) {

                } else {
                    LOGGER.info("Got rendezvous of unknown type " + type);
                }
            }
        }

        private class ReverseController extends StateController {
            public void start(FileTransfer transfer,
                    StateController last) {
                try {
                    final ServerSocket serverSocket = new ServerSocket(0);
                    RvConnectionInfo connInfo = RvConnectionInfo
                            .createForOutgoingRequest(InetAddress.getLocalHost(),
                                    serverSocket.getLocalPort());
                    Thread thread = new Thread(new Runnable() {
                        public void run() {
                            Socket socket;
                            try {
                                socket = serverSocket.accept();
                            } catch (IOException e) {
                                e.printStackTrace();
                                fireFailed(e);
                                return;
                            }
                            try {
                                fireSucceeded(new Stream(socket.getInputStream(),
                                        socket.getOutputStream()));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                    thread.start();
                    transfer.getRvSession().sendRv(new FileSendReqRvCmd(
                            connInfo));
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
