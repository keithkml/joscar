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
 *  File created by keith @ Feb 25, 2004
 *
 */

package net.kano.aimcrypto.text.convbox;

import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.JViewport;
import javax.swing.text.BadLocationException;
import java.awt.Rectangle;

public class BottomScroller implements ConversationDocumentListener {
    private final JScrollPane scrollpane;
    private final JTextPane textpane;
    private final ConversationDocument doc;

    public BottomScroller(JScrollPane scrollpane, JTextPane textpane,
            ConversationDocument doc) {
        this.scrollpane = scrollpane;
        this.textpane = textpane;
        this.doc = doc;
    }

    public void handleEvent(ConversationDocumentEvent event) {
        assert SwingUtilities.isEventDispatchThread();

        JViewport viewport = scrollpane.getViewport();
        if (viewport == null) return;
        Rectangle visible = viewport.getViewRect();
        if (visible == null) return;
        try {
            Rectangle oldEndRect = textpane.modelToView(event.getOldLength()-1);
            Rectangle newEndRect = textpane.modelToView(event.getNewLength());
            if (oldEndRect == null || newEndRect == null) return;
            if (SwingUtilities.isRectangleContainingRectangle(
                    visible, oldEndRect)) {
                newEndRect.width = 0;
                newEndRect.height = 0;
                viewport.scrollRectToVisible(newEndRect);
            }
        } catch (BadLocationException ignored) { return; }
    }
}
