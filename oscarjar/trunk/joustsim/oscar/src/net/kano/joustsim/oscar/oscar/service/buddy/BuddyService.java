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

package net.kano.joustsim.oscar.oscar.service.buddy;

import net.kano.joscar.CopyOnWriteArrayList;
import net.kano.joscar.flapcmd.SnacCommand;
import net.kano.joscar.snac.SnacPacketEvent;
import net.kano.joscar.snaccmd.FullUserInfo;
import net.kano.joscar.snaccmd.buddy.BuddyCommand;
import net.kano.joscar.snaccmd.buddy.BuddyOfflineCmd;
import net.kano.joscar.snaccmd.buddy.BuddyStatusCmd;
import net.kano.joscar.snaccmd.conn.SnacFamilyInfo;
import net.kano.joustsim.Screenname;
import net.kano.joustsim.oscar.AimConnection;
import net.kano.joustsim.oscar.oscar.OscarConnection;
import net.kano.joustsim.oscar.oscar.service.AbstractService;

public class BuddyService extends AbstractService {
  private CopyOnWriteArrayList<BuddyServiceListener> listeners
      = new CopyOnWriteArrayList<BuddyServiceListener>();

  public BuddyService(AimConnection aimConnection,
      OscarConnection oscarConnection) {
    super(aimConnection, oscarConnection, BuddyCommand.FAMILY_BUDDY);
  }

  public void addBuddyListener(BuddyServiceListener l) {
    listeners.addIfAbsent(l);
  }

  public void removeBuddyListener(BuddyServiceListener l) {
    listeners.remove(l);
  }

  public SnacFamilyInfo getSnacFamilyInfo() { return BuddyCommand.FAMILY_INFO; }

  public void connected() {
    setReady();
  }

  public void handleSnacPacket(SnacPacketEvent snacPacketEvent) {
    SnacCommand snac = snacPacketEvent.getSnacCommand();

    if (snac instanceof BuddyStatusCmd) {
      BuddyStatusCmd bsc = (BuddyStatusCmd) snac;
      FullUserInfo userInfo = bsc.getUserInfo();
      String snText = userInfo == null ? null : userInfo.getScreenname();
      if (userInfo != null && snText != null) {
        Screenname sn = new Screenname(snText);
        for (BuddyServiceListener listener : listeners) {
          listener.gotBuddyStatus(this, sn, userInfo);
        }
      }

    } else if (snac instanceof BuddyOfflineCmd) {
      BuddyOfflineCmd boc = (BuddyOfflineCmd) snac;
      String snText = boc.getScreenname();
      if (snText != null) {
        Screenname sn = new Screenname(snText);
        for (BuddyServiceListener listener : listeners) {
          listener.buddyOffline(this, sn);
        }
      }
    }
  }
}
