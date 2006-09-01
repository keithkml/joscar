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

package net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers;

import net.kano.joustsim.oscar.oscar.service.icbm.ft.ChecksummerImpl;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.OutgoingFileTransfer;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.RvConnection;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.Checksummer;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.ChecksummingEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.state.ComputedChecksumsInfo;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChecksumController extends AbstractStateController {
  public void start(RvConnection transfer, StateController last) {
    try {
      Map<TransferredFile, Long> checksums = new HashMap<TransferredFile, Long>();
      if (transfer instanceof OutgoingFileTransfer) {
        OutgoingFileTransfer otransfer = (OutgoingFileTransfer) transfer;
        List<TransferredFile> files = otransfer.getFiles();
        for (TransferredFile tfile : files) {
          File file = tfile.getRealFile();
          RandomAccessFile raf = new RandomAccessFile(file, "r");

          Checksummer summer = new ChecksummerImpl(raf.getChannel(), raf.length());
          otransfer.getEventPost().fireEvent(new ChecksummingEvent(tfile, summer));
          checksums.put(tfile, summer.compute());
        }
      }
      fireSucceeded(new ComputedChecksumsInfo(checksums));
    } catch (IOException e) {
      fireFailed(e);
    }
  }

  public void stop() {
  }

}
