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
 *  File created by keith @ Jan 22, 2004
 *
 */

package net.kano.aimcrypto.conv;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.Document;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.HTML;
import javax.swing.text.html.StyleSheet;
import javax.swing.JTextPane;
import javax.swing.JFrame;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Enumeration;

public class ConversationDocumentTester extends HTMLDocument {
    public static void main(String[] args) {
//        String html = "<html><body bgcolor=\"green\"><b>a    b     c<i><a href=URL>b</a>"
//                + "<br><strike><u>b</u></strike><font "
//                + "bAcK=green color=\"red\" size=\"2\">yo</font></i>c</b> x = y"
//                + "</body></html>";

        HTMLEditorKit ek = new HTMLEditorKit() {
            public Document createDefaultDocument() {
                ConversationDocumentTester doc = new ConversationDocumentTester();
                doc.setParser(getParser());
                doc.setAsynchronousLoadPriority(4);
                doc.setTokenThreshold(100);
                return doc;
            }
        };

        JTextPane pane = new JTextPane();
        pane.setEditable(false);
        pane.setEditorKit(ek);
        JFrame frame = new JFrame("frame");
        frame.getContentPane().add(pane);
        frame.setSize(100, 100);
        frame.setVisible(true);

        String html = "snargles!<a href=\"test\">snar</a>";
        ConversationDocumentTester cd = (ConversationDocumentTester) pane.getDocument();
        Element root = cd.getDefaultRootElement();
        try {
            if (false) cd.insertAfterStart(root, html);
        } catch (BadLocationException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        printElTree(root);

        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            StyleSheet ss = new StyleSheet();
            for (;;) {
                String line = br.readLine();
                if (line == null) break;
                AolRtfText convline = AolRtfText.readLine(ss, line);
                ElementSpec[] els = convline.generateDocumentElements();
                cd.insert(root.getEndOffset()-1, els);
                printElTree(root);
                pane.repaint();
            }
        } catch (BadLocationException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void printElTree(Element root) {
        try {
            printChildren(root, 0);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    public static void printChildren(Element el, int level)
            throws BadLocationException {
        printSpaces(level);

        System.out.println();
        System.out.print(el.getClass().getName() + ": " + el.toString().trim()
                + " = ");
        System.out.println(el.getDocument().getText(el.getStartOffset(),
                el.getEndOffset()-el.getStartOffset()));
        AttributeSet attr = el.getAttributes();
        Enumeration names = attr.getAttributeNames();
        while (names.hasMoreElements()) {
            printSpaces(level+3);
            Object o = names.nextElement();
            Object val = attr.getAttribute(o);
            printAttribute(o, val);
            if (val instanceof AttributeSet) {
                AttributeSet valset = (AttributeSet) val;
                Enumeration valsetnames = valset.getAttributeNames();
                while (valsetnames.hasMoreElements()) {
                    Object o1 = valsetnames.nextElement();
                    printSpaces(level+4);
                    System.out.print("*-");
                    printAttribute(o1, valset.getAttribute(o1));
                }
            }
        }
        for (int i = 0; i < el.getElementCount(); i++) {
            printChildren(el.getElement(i), level+1);
        }
    }

    private static void printAttribute(Object o, Object val) {
        System.out.println("* " + o.getClass().getName() + ": " + o + " = "
                + val.getClass().getName() + " (" + val + ")");
    }

    private static void printSpaces(int level) {
        for (int i = 0; i < level; i++) System.out.print(' ');
    }
}
