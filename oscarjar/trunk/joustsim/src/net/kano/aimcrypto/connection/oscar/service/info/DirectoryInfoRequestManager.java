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
 *  File created by keith @ Feb 7, 2004
 *
 */

package net.kano.aimcrypto.connection.oscar.service.info;

import net.kano.aimcrypto.Screenname;
import net.kano.joscar.flapcmd.SnacCommand;
import net.kano.joscar.snac.SnacRequestAdapter;
import net.kano.joscar.snac.SnacRequestTimeoutEvent;
import net.kano.joscar.snac.SnacResponseEvent;
import net.kano.joscar.snaccmd.DirInfo;
import net.kano.joscar.snaccmd.loc.DirInfoCmd;
import net.kano.joscar.snaccmd.loc.GetDirInfoCmd;

import java.util.Iterator;
import java.util.Set;

public class DirectoryInfoRequestManager extends InfoRequestManager {
    public DirectoryInfoRequestManager(InfoService service) {
        super(service);
    }

    protected void sendRequest(final Screenname sn) {
        GetDirInfoCmd cmd = new GetDirInfoCmd(sn.getFormatted());
        getService().sendSnacRequest(cmd, new SnacRequestAdapter() {
            private boolean ran = false;

            public void handleResponse(SnacResponseEvent e) {
                SnacCommand snac = e.getSnacCommand();

                synchronized(this) {
                    if (ran) return;
                    ran = true;
                }

                DirInfo dirInfo = null;
                if (snac instanceof DirInfoCmd) {
                    DirInfoCmd dic = (DirInfoCmd) snac;
                    dirInfo = dic.getDirInfo();
                }
                fireListeners(dirInfo);
            }

            public void handleTimeout(SnacRequestTimeoutEvent event) {
                synchronized(this) {
                    if (ran) return;
                    ran = true;
                }
                fireListeners(null);
            }

            private void fireListeners(DirInfo info) {
                Set listeners = clearListeners(sn);
                for (Iterator it = listeners.iterator(); it.hasNext();) {
                    InfoResponseListener listener = (InfoResponseListener) it.next();
                    listener.handleDirectoryInfo(getService(), sn, info);
                }
            }
        });
    }
}
