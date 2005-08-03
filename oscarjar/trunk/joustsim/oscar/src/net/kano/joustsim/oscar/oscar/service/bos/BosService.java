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
 *  File created by keith @ Jan 17, 2004
 *
 */

package net.kano.joustsim.oscar.oscar.service.bos;

import net.kano.joscar.flapcmd.SnacCommand;
import net.kano.joscar.ratelim.RateMonitor;
import net.kano.joscar.snac.SnacPacketEvent;
import net.kano.joscar.snaccmd.conn.ClientReadyCmd;
import net.kano.joscar.snaccmd.conn.ClientVersionsCmd;
import net.kano.joscar.snaccmd.conn.ConnCommand;
import net.kano.joscar.snaccmd.conn.RateAck;
import net.kano.joscar.snaccmd.conn.RateClassInfo;
import net.kano.joscar.snaccmd.conn.RateInfoCmd;
import net.kano.joscar.snaccmd.conn.RateInfoRequest;
import net.kano.joscar.snaccmd.conn.ServerReadyCmd;
import net.kano.joscar.snaccmd.conn.SnacFamilyInfo;
import net.kano.joustsim.oscar.AimConnection;
import net.kano.joustsim.oscar.oscar.OscarConnListener;
import net.kano.joustsim.oscar.oscar.OscarConnStateEvent;
import net.kano.joustsim.oscar.oscar.OscarConnection;
import net.kano.joustsim.oscar.oscar.service.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public abstract class BosService extends Service {
    private static final Logger logger = Logger.getLogger(BosService.class.getName());

    private List<SnacFamilyInfo> snacFamilyInfos = null;

    private RateMonitor rateMonitor;

    protected BosService(AimConnection aimConnection,
            OscarConnection oscarConnection) {
        super(aimConnection, oscarConnection, ConnCommand.FAMILY_CONN);

        OscarConnection oc = getOscarConnection();
        rateMonitor = new RateMonitor(oc.getSnacProcessor());
        oc.addOscarListener(new OscarConnListener() {
            public void registeredSnacFamilies(OscarConnection conn) {
            }

            public void connStateChanged(OscarConnection conn, OscarConnStateEvent event) {
            }

            public void allFamiliesReady(OscarConnection conn) {
                allReady();
            }
        });
    }

    public SnacFamilyInfo getSnacFamilyInfo() {
        return ConnCommand.FAMILY_INFO;
    }

    private void allReady() {
        logger.fine("All families are ready, sending client ready");
        sendSnac(new ClientReadyCmd(getSnacFamilyInfos()));
    }

    public void handleSnacPacket(SnacPacketEvent snacPacketEvent) {
        SnacCommand snac = snacPacketEvent.getSnacCommand();

        if (snac instanceof ServerReadyCmd) {
            logger.fine("Server is ready");

            ServerReadyCmd src = (ServerReadyCmd) snac;

            List<Service> services = getOscarConnection().getServices();
            List<SnacFamilyInfo> familyInfos = new ArrayList<SnacFamilyInfo>(services.size());
            for (Service service : services) {
                familyInfos.add(service.getSnacFamilyInfo());
            }

            setSnacFamilyInfos(familyInfos);

            sendSnac(new ClientVersionsCmd(familyInfos));
            sendSnac(new RateInfoRequest());

            getOscarConnection().postServiceEvent(new ServerReadyEvent(this));
            serverReady();

        } else if (snac instanceof RateInfoCmd) {
            RateInfoCmd ric = (RateInfoCmd) snac;

            List<RateClassInfo> rateClasses = ric.getRateClassInfos();

            int[] classes = new int[rateClasses.size()];
            for (int i = 0; i < rateClasses.size(); i++) {
                classes[i] = rateClasses.get(i).getRateClass();
            }

            sendSnac(new RateAck(classes));

            trySetReady();
        }
    }

    protected void trySetReady() {
        reallySetReady();
    }

    protected void reallySetReady() {
        beforeClientReady();

        setReady();
    }

    protected void beforeClientReady() {

    }

    protected void serverReady() {

    }

    private synchronized void setSnacFamilyInfos(
            List<SnacFamilyInfo> snacFamilyInfos) {
        this.snacFamilyInfos = snacFamilyInfos;
    }

    private synchronized List<SnacFamilyInfo> getSnacFamilyInfos() {
        return snacFamilyInfos;
    }

}
