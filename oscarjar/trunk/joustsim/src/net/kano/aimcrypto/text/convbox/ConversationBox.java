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

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.AbstractAction;
import javax.swing.ToolTipManager;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.HyperlinkEvent;
import javax.swing.text.html.StyleSheet;
import javax.swing.text.Keymap;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.io.IOException;
import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.net.URL;

public class ConversationBox extends JPanel {
    private final JScrollPane scrollPane = new JScrollPane(
            JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    private final JTextPane textpane = new JTextPane();

    private final ConversationEditorKit editorKit = new ConversationEditorKit();
    private final ConversationDocument document;
    private final StyleSheet stylesheet;

    {
        textpane.setEditorKit(editorKit);
        document = (ConversationDocument) textpane.getDocument();
        document.addConversationDocumentListener(
                new BottomScroller(scrollPane, textpane, document));
        textpane.setNavigationFilter(new EndAvoider(document));
        textpane.setEditable(false);
        textpane.setDragEnabled(true);
        Keymap keymap = textpane.getKeymap();
        Keymap parent = keymap.getResolveParent();

        System.out.println("keymap: " + keymap);
        System.out.println(Arrays.asList(keymap.getBoundKeyStrokes()));
        System.out.println("parent: " + parent);
        System.out.println(Arrays.asList(parent.getBoundKeyStrokes()));
        System.out.println("up: " + keymap.getAction(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0)));

        stylesheet = document.getStyleSheet();

        textpane.addHyperlinkListener(new HyperlinkListener() {
            public void hyperlinkUpdate(HyperlinkEvent e) {
                HyperlinkEvent.EventType eventType = e.getEventType();
                if (eventType == HyperlinkEvent.EventType.ACTIVATED) {
                    URL url = e.getURL();
                    System.out.println("url: " + url);
                }
            }
        });

        loadRulesResource("css/imbox.css");

        scrollPane.setViewport(new BottomScrollingViewport());
        scrollPane.setViewportView(textpane);

        setLayout(new BorderLayout());
        add(scrollPane);
    }

    public JScrollPane getScrollPane() {
        return scrollPane;
    }

    public JTextPane getTextpane() {
        return textpane;
    }

    public ConversationEditorKit getEditorKit() {
        return editorKit;
    }

    public ConversationDocument getDocument() {
        return document;
    }

    public StyleSheet getStylesheet() {
        return stylesheet;
    }

    private boolean loadRulesResource(String resource) {
        ClassLoader cl = getClass().getClassLoader();
        InputStream stream = cl.getResourceAsStream(resource);
        if (stream == null) return false;

        InputStreamReader isr;
        try {
            isr = new InputStreamReader(stream, "US-ASCII");
        } catch (UnsupportedEncodingException impossible) {
            return false;
        }

        String rules = readIntoString(isr);
        if (rules == null) return false;
        stylesheet.addRule(rules);

        return true;
    }

    private String readIntoString(InputStreamReader isr) {
        StringBuffer sb = new StringBuffer(500);
        try {
            char[] chars = new char[500];
            while (true) {
                int c = isr.read(chars);
                if (c == -1) break;

                sb.append(chars, 0, c);
            }

        } catch (IOException e) {
            return null;
        }
        return sb.toString();
    }

    private class UpAction extends AbstractAction {
        public UpAction() {
            super("Scroll up");
        }

        public void actionPerformed(ActionEvent e) {
//            textpane.getScrollableUnitIncrement()
        }
    }
}
