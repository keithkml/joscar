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
 *  File created by keith @ Feb 26, 2004
 *
 */

package net.kano.aimcrypto.text.aolrtfbox;

import javax.swing.AbstractAction;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextPane;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.text.StyledEditorKit;
import javax.swing.text.html.HTMLDocument;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

public class AolRtfTester {
    private static final StyledEditorKit.BoldAction boldAction
            = new StyledEditorKit.BoldAction();
    private static final StyledEditorKit.ItalicAction italicAction
            = new StyledEditorKit.ItalicAction();
    private static final StyledEditorKit.UnderlineAction underlineAction
            = new StyledEditorKit.UnderlineAction();
    private static final StyledEditorKit.FontSizeAction fontSizeActionBig
            = new StyledEditorKit.FontSizeAction("size=20", 20);
    private static final StyledEditorKit.FontSizeAction fontSizeActionSmall
            = new StyledEditorKit.FontSizeAction("size=10", 10);
    private static final StyledEditorKit.ForegroundAction foregroundActionRed
            = new StyledEditorKit.ForegroundAction("Red", Color.RED);
    private static final StyledEditorKit.ForegroundAction foregroundActionGreen
            = new StyledEditorKit.ForegroundAction("Green", Color.GREEN);

    public static void main(String[] args) {
        final JFrame frame = new JFrame();
        final JTextPane textpane = new JTextPane();
        textpane.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    ((HTMLDocument) textpane.getDocument()).dump(System.out);
                }
            }
        });
//        textpane.setEditorKit(new HTMLEditorKit());
        textpane.setEditorKit(new AolRtfEditorKit());
        JToolBar tb = new JToolBar();
        final JScrollPane scrolly = new JScrollPane(textpane,
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        JSplitPane pane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true, tb, scrolly);
        frame.getContentPane().add(pane);
        tb.add(boldAction);
        tb.add(italicAction);
        tb.add(underlineAction);
        tb.add(fontSizeActionBig);
        tb.add(fontSizeActionSmall);
        tb.add(foregroundActionRed);
        tb.add(foregroundActionGreen);
        tb.add(new AbstractAction("Write") {
            public void actionPerformed(ActionEvent e) {
                String text = textpane.getText();
                textpane.setText(text);
                System.out.println(text);
            }
        });
//        textpane.setText("<body bgcolor=green><font style=\"background: red\">"
//                + "red</font><hr><hr>test<br><br>"
//                + "<font style=text-decoration:line-through>test</font>test");

        frame.setSize(600, 300);
        frame.setVisible(true);

        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        try {
            for (; ;) {
                String line = br.readLine();
                if (line == null) return;

                textpane.setText(line.replaceAll("\\\\n", "\n"));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
