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

package net.kano.aimcrypto.connection.oscar.service.bos;

import net.kano.aimcrypto.connection.AimConnection;
import net.kano.aimcrypto.connection.oscar.OscarConnListener;
import net.kano.aimcrypto.connection.oscar.OscarConnection;
import net.kano.aimcrypto.connection.oscar.service.Service;
import net.kano.joscar.flapcmd.SnacCommand;
import net.kano.joscar.net.ClientConnEvent;
import net.kano.joscar.ratelim.RateMonitor;
import net.kano.joscar.snac.SnacPacketEvent;
import net.kano.joscar.snaccmd.SnacFamilyInfoFactory;
import net.kano.joscar.snaccmd.conn.ClientReadyCmd;
import net.kano.joscar.snaccmd.conn.ClientVersionsCmd;
import net.kano.joscar.snaccmd.conn.ConnCommand;
import net.kano.joscar.snaccmd.conn.RateAck;
import net.kano.joscar.snaccmd.conn.RateClassInfo;
import net.kano.joscar.snaccmd.conn.RateInfoCmd;
import net.kano.joscar.snaccmd.conn.RateInfoRequest;
import net.kano.joscar.snaccmd.conn.ServerReadyCmd;
import net.kano.joscar.snaccmd.conn.SnacFamilyInfo;

import java.util.logging.Logger;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.List;

public class BosService extends Service {
    private static final Logger logger = Logger.getLogger(BosService.class.getName());

    private SnacFamilyInfo[] snacFamilyInfos = null;

    private RateMonitor rateMonitor;

    public BosService(AimConnection aimConnection,
            OscarConnection oscarConnection) {
        super(aimConnection, oscarConnection, ConnCommand.FAMILY_CONN);

        OscarConnection oc = getOscarConnection();
        rateMonitor = new RateMonitor(oc.getSnacProcessor());
        oc.addOscarListener(new OscarConnListener() {
            public void registeredSnacFamilies(OscarConnection conn) {
            }

            public void connStateChanged(OscarConnection conn, ClientConnEvent event) {
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

            int[] families = src.getSnacFamilies();

            Service[] services = getOscarConnection().getServices();
            SnacFamilyInfo[] familyInfos = new SnacFamilyInfo[services.length];
            for (int i = 0; i < services.length; i++) {
                familyInfos[i] = services[i].getSnacFamilyInfo();
            }

            setSnacFamilyInfos(familyInfos);

            sendSnac(new ClientVersionsCmd(familyInfos));
            sendSnac(new RateInfoRequest());

            serverReady();

        } else if (snac instanceof RateInfoCmd) {
            RateInfoCmd ric = (RateInfoCmd) snac;

            RateClassInfo[] rateClasses = ric.getRateClassInfos();

            int[] classes = new int[rateClasses.length];
            for (int i = 0; i < rateClasses.length; i++) {
                classes[i] = rateClasses[i].getRateClass();
            }

            sendSnac(new RateAck(classes));

            beforeClientReady();

            setReady();
        }
    }

    protected void beforeClientReady() {

    }

    protected void serverReady() {

    }

    private synchronized void setSnacFamilyInfos(
            SnacFamilyInfo[] snacFamilyInfos) {
        this.snacFamilyInfos = snacFamilyInfos;
    }

    private synchronized SnacFamilyInfo[] getSnacFamilyInfos() {
        return snacFamilyInfos;
    }
}
