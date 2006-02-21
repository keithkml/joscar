/*
 * Copyright (c) 2006, The Joust Project
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * - Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in
 *   the documentation and/or other materials provided with the
 *   distribution.
 * - Neither the name of the Joust Project nor the names of its
 *   contributors may be used to endorse or promote products derived
 *   from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * File created by keithkml
 */

package net.kano.joustsim.oscar.oscar.service.icbm;

import net.kano.joscar.DefensiveTools;
import net.kano.joustsim.oscar.oscar.service.icbm.dim.Attachment;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A message with attachments. Each attachment must have an ID which is unique
 * within this message. Normally, each attachment corresponds to an {@code IMG}
 * tag in the message body, like
 * {@code <IMG SRC="65.gif" ID="1" WIDTH="30" HEIGHT="31" DATASIZE="1502">}.
 */
public class DirectMessage implements Message {
  private final String message;
  private final boolean autoResponse;
  private final Set<Attachment> attachments;

  @SuppressWarnings({"unchecked"})
  public DirectMessage(String message, boolean autoResponse) {
    this(message, autoResponse, Collections.EMPTY_SET);
  }

  /**
   * Creates a new direct message with attachments. Each attachment have an ID
   * which is unique among the other attachments in this message.
   *
   * @param attachments a list of attachments
   */
  public DirectMessage(String message, boolean autoResponse,
      Attachment... attachments) {
    this(message, autoResponse, new LinkedHashSet<Attachment>(
        Arrays.asList(attachments)));
  }

  /**
   * Creates a new direct message with attachments. Each attachment have an ID
   * which is unique among the other attachments in this message.
   *
   * @param attachments a list of attachments
   */
  public DirectMessage(String message, boolean autoResponse,
      Set<Attachment> attachments) {
    this.message = message;
    this.autoResponse = autoResponse;
    this.attachments = DefensiveTools.getUnmodifiableSetCopy(attachments);
  }

  public String getMessageBody() { return message; }

  public boolean isAutoResponse() { return autoResponse; }

  @SuppressWarnings({"ReturnOfCollectionOrArrayField"})
  public Set<Attachment> getAttachments() { return attachments; }
}
