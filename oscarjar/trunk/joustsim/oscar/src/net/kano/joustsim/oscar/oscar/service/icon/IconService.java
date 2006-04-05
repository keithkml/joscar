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

package net.kano.joustsim.oscar.oscar.service.icon;

import net.kano.joscar.ByteBlock;
import net.kano.joscar.CopyOnWriteArrayList;
import net.kano.joscar.Writable;
import net.kano.joscar.flapcmd.SnacCommand;
import net.kano.joscar.snac.ClientSnacProcessor;
import net.kano.joscar.snac.SnacRequestAdapter;
import net.kano.joscar.snac.SnacResponseEvent;
import net.kano.joscar.snac.SnacResponseListener;
import net.kano.joscar.snaccmd.ExtraInfoBlock;
import net.kano.joscar.snaccmd.ExtraInfoData;
import net.kano.joscar.snaccmd.conn.SnacFamilyInfo;
import net.kano.joscar.snaccmd.error.SnacError;
import net.kano.joscar.snaccmd.icon.IconCommand;
import net.kano.joscar.snaccmd.icon.IconDataCmd;
import net.kano.joscar.snaccmd.icon.IconRequest;
import net.kano.joscar.snaccmd.icon.UploadIconAck;
import net.kano.joscar.snaccmd.icon.UploadIconCmd;
import net.kano.joustsim.Screenname;
import net.kano.joustsim.oscar.AimConnection;
import net.kano.joustsim.oscar.oscar.OscarConnection;
import net.kano.joustsim.oscar.oscar.service.AbstractService;

import java.util.logging.Logger;

public class IconService extends AbstractService implements IconRequestHandler {
  private static final Logger LOGGER = Logger
      .getLogger(IconService.class.getName());

  private CopyOnWriteArrayList<IconRequestListener> listeners
      = new CopyOnWriteArrayList<IconRequestListener>();

  public void addIconRequestListener(IconRequestListener listener) {
    listeners.addIfAbsent(listener);
  }

  public void removeIconRequestListener(IconRequestListener listener) {
    listeners.remove(listener);
  }

  protected IconService(AimConnection aimConnection,
      OscarConnection oscarConnection) {
    super(aimConnection, oscarConnection, IconCommand.FAMILY_ICON);

    ClientSnacProcessor snacProcessor = oscarConnection.getSnacProcessor();
    snacProcessor.addGlobalResponseListener(new SnacResponseListener() {
      public void handleResponse(SnacResponseEvent e) {
        SnacCommand cmd = e.getSnacCommand();
        if (cmd instanceof IconDataCmd) {
          IconDataCmd iconDataCmd = (IconDataCmd) cmd;
          ExtraInfoBlock iconInfo = iconDataCmd.getIconInfo();
          ExtraInfoData data = iconInfo.getExtraData();
          ByteBlock hash = data.getData();
          Screenname sn = new Screenname(iconDataCmd.getScreenname());
          if ((data.getFlags() & ExtraInfoData.FLAG_HASH_PRESENT) == 0
              && hash.equals(ExtraInfoData.HASH_SPECIAL)) {
            for (IconRequestListener listener : listeners) {
              listener.buddyIconCleared(IconService.this, sn, data);
            }
          } else {
            for (IconRequestListener listener : listeners) {
              listener.buddyIconUpdated(IconService.this, sn,
                  data, iconDataCmd.getIconData());
            }
          }
        }
      }
    });
  }

  public SnacFamilyInfo getSnacFamilyInfo() {
    return IconCommand.FAMILY_INFO;
  }

  public void connected() {
    setReady();
  }

//    public void requestIcon(Screenname sn, ByteBlock hash) {
//        ExtraInfoBlock block = new ExtraInfoBlock(TYPE_ICONHASH,
//                new ExtraInfoData(ExtraInfoData.FLAG_HASH_PRESENT,
//                        hash));
//        requestIcon(sn, block);
//    }

  public void requestIcon(Screenname sn, ExtraInfoData block) {
    sendSnac(new IconRequest(sn.getFormatted(), block));
  }

  public void uploadIcon(Writable data) {
    uploadIcon(data, null);
  }

  public void uploadIcon(final Writable data, final IconSetListener listener) {
    LOGGER.fine("Uploading icon " + data);
    sendSnacRequest(new UploadIconCmd(data), new SnacRequestAdapter() {
      public void handleResponse(SnacResponseEvent e) {
        SnacCommand cmd = e.getSnacCommand();
        if (cmd instanceof UploadIconAck) {
          UploadIconAck iconAck = (UploadIconAck) cmd;
          if (iconAck.getCode() != UploadIconAck.CODE_DEFAULT) {
            LOGGER.fine("Got unknown code from UploadIconAck: "
                + iconAck);
          }
          ExtraInfoBlock iconInfo = iconAck.getIconInfo();
          if (iconInfo == null) {
            LOGGER.finer("Got icon ack with no iconInfo: "
                + iconAck);
          }
          if (listener != null) {
            LOGGER.fine("Successfully set icon " + data);
            listener.handleIconSet(IconService.this, data, true);
          }
        } else if (cmd instanceof SnacError) {
          LOGGER.warning("Got SnacError while setting icon: " + cmd);
          listener.handleIconSet(IconService.this, data, false);
        }
      }
    });
  }
}
