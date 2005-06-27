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
 *  File created by keith @ Feb 21, 2004
 *
 */

package net.kano.joustsim.app;

import net.kano.joustsim.Screenname;
import net.kano.joustsim.text.AolRtfString;
import net.kano.joustsim.text.convbox.IconID;
import net.kano.joustsim.text.convbox.ConversationEditorKit;
import net.kano.joustsim.text.convbox.ConversationDocument;
import net.kano.joustsim.text.convbox.BottomScroller;
import net.kano.joustsim.text.convbox.EndAvoider;
import net.kano.joustsim.text.convbox.ConversationLine;

import javax.swing.Icon;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.text.Style;
import javax.swing.text.html.StyleSheet;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Date;
import java.util.Enumeration;
import java.util.Random;

public class ConversationTester {

    private IconID id1 = new IconID();
    private IconID id2 = new IconID();
    private final Random random = new Random();

    public static void main(String[] args) {
        new ConversationTester().go();
    }

    private void go() {
        ConversationEditorKit ek = new ConversationEditorKit();
        final JTextPane textpane = new JTextPane();
        textpane.setEditorKit(ek);
        final ConversationDocument doc = (ConversationDocument) textpane.getDocument();
        final Screenname out = new Screenname("Out Go Ing");
        final Screenname in = new Screenname("In Com Ing");
        final StyleSheet sheet = doc.getStyleSheet();

        sheet.addRule(".lightmsg { color: gray }");
        sheet.addRule(".statusmsg { font-weight: bold }");
        sheet.addRule(".infomsg {  }");
        sheet.addRule(".errormsg { font-weight: bold; background: red }");

        sheet.addRule(".screenname { font-weight: bold }");
        sheet.addRule(".screenname-type-outgoing { color: red }");
        sheet.addRule(".screenname-type-incoming { color: blue }");
        sheet.addRule(".screenname-for-incoming { text-decoration: underline }");

        doc.registerScreenname(new Screenname("incoming"), false);
        doc.registerScreenname(new Screenname("outgoing"), true);
//        printStyleTree(doc, "screenname-for-incoming");

        final JFrame frame = new JFrame();
        final JScrollPane scrolly = new JScrollPane(
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrolly.setViewport(new JViewport() {
            private int distFromBottom = 0;
            private boolean ignore = false;

            public void doLayout() {
                ignore = true;
                super.doLayout();
                ignore = false;
            }

            public void reshape(int x, int y, int w, int h) {
                updateViewPosition(h);
                ignore = true;
                super.reshape(x, y, w, h);
                ignore = false;
            }

            public void setViewPosition(Point p) {
                ignore = true;
                super.setViewPosition(p);
                ignore = false;
                updateDistFromBottom();
            }

            public void setViewSize(Dimension newSize) {
                updateViewPosition(getHeight());
                ignore = true;
                super.setViewSize(newSize);
                ignore = false;
            }

            private void updateViewPosition(int h) {
                System.out.println("reshaping to " + h + "..");
                Point vp = getViewPosition();
                int viewHeight = getView().getHeight();
                vp.y = Math.max(0, viewHeight - h - distFromBottom);
                System.out.println("- new y position: " + vp.y);

                setViewPosition(vp);
            }

            private void updateDistFromBottom() {
                if (ignore) return;
                Component view = getView();
                Point viewPosition = getViewPosition();
                if (view == null || viewPosition == null) return;
                distFromBottom = view.getHeight()
                        - (viewPosition.y + getHeight());

//                System.out.println("updated distance to " + distFromBottom);
//                new Throwable().printStackTrace(System.out);
            }
        });
        scrolly.setViewportView(textpane);
        final JViewport viewport = scrolly.getViewport();
//        MyComponentAdapter thing = new MyComponentAdapter(viewport);
//        viewport.addComponentListener(thing);
//        viewport.addChangeListener(thing);
        frame.getContentPane().add(scrolly);
        doc.addConversationDocumentListener(new BottomScroller(scrolly, textpane, doc));

        textpane.setNavigationFilter(new EndAvoider(doc));
        textpane.setEditable(false);
        textpane.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    doc.dump(System.out);
                    Enumeration styleNames = sheet.getStyleNames();
                    while (styleNames.hasMoreElements()) {
                        String o = (String) styleNames.nextElement();
                        System.out.println("- " + o + " - " + sheet.getStyle(o));
                    }
                }
            }
        });
//        textpane.setText("<p>test</p> <p class=screenname-for-incoming>test</p> <p>test</p>");

        frame.setSize(500, 300);

        frame.setVisible(true);
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                for (int i = 0; i < 50; i++) {
                    insertRandom(doc, out, in);
                }

                frame.setVisible(true);
            }
        });

//        Timer timer = new Timer(1000, new ActionListener() {
//            public void actionPerformed(ActionEvent e) {
//                insertRandom(doc, out, in);
//            }
//        });
//        timer.start();
    }

    private void printStyleTree(ConversationDocument doc, String sn) {
        Style style = doc.getStyle("." + sn);
        printStyleTree(style, 0);
    }

    private void printStyleTree(Style style, int depth) {
        for (int i = 0; i < depth; i++) System.out.print(' ');
        if (depth != 0) System.out.print("based on ");
        if (style == null) {
            System.out.println(style);
        } else {
            System.out.println("style: " + style.getName());
            printStyleTree((Style) style.getResolveParent(), depth+1);
        }
    }

    private void printThings(ConversationDocument doc, Screenname out,
            Screenname in, int n) {
        long start = System.currentTimeMillis();
        printFreeMemory();
        for (int i = 0; i < n; i++) {
            insertRandom(doc, out, in);
        }
        System.out.println("Took " + (System.currentTimeMillis()
                - start)/1000. + "sec");
        printFreeMemory();
    }

    private void printFreeMemory() {
        Runtime runtime = Runtime.getRuntime();
        long total = runtime.totalMemory();
        long used = total-runtime.freeMemory();
        System.out.println("Using " + (used/1048576.) + "MB of "
                + (total/1048576.) + "MB");
    }

    private void insertRandom(ConversationDocument doc, Screenname out,
            Screenname in) {
        StyleSheet sheet = doc.getStyleSheet();
        int rand = random.nextInt(7)+1;
        System.out.println("inserting random: " + rand);
        if (rand == 1) {
            doc.addConversationLine(new ConversationLine(out,
                    AolRtfString.readLine(sheet, "This is a <b>test"),
                    new Date(), id1));
        } else if (rand == 2) {
            doc.addConversationLine(new ConversationLine(in,
                    AolRtfString.readLine(sheet, "<body bgcolor=#aaaaff>"
                    + "bgcolor is lightblueeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee"
                    + "This is <font back=yellow>YELLOW TEXT BG test2<br>newline"
                    + "<hr>horrule<hr>newline<br><br>newlines"),
                    new Date(System.currentTimeMillis() + 21340), id2));
        } else if (rand == 3) {
            doc.addLightMessage(AolRtfString.readLine("This is a light message."));
        } else if (rand == 4) {
            doc.addStatusMessage(AolRtfString.readLine("This is a status message."));
        } else if (rand == 5) {
            doc.addInfoMessage(AolRtfString.readLine("This is an info message."));
        } else if (rand == 6) {
            doc.addErrorMessage(AolRtfString.readLine("This is an error message."));
        } else if (rand == 7) {
            Icon[] icons = new Icon[] {
                GuiResources.getTinyCertificateIcon(),
                GuiResources.getTinyProgramIcon(),
                GuiResources.getTinySignerIcon(),
            };
            IconID id = random.nextBoolean() ? id1 : id2;
            Icon icon1 = icons[random.nextInt(icons.length)];
            Icon icon2 = icons[random.nextInt(icons.length)];
            doc.setIconsForID(id, new Icon[] { icon1, icon2 });
        }
    }

}
