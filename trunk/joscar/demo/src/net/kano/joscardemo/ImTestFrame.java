/*
 *  Copyright (c) 2002-2003, The Joust Project
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
 *  File created by keith @ Mar 6, 2003
 *
 */
package net.kano.joscardemo;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

public class ImTestFrame extends JFrame {

    protected JButton connectButton;
    protected JTextArea output;
    protected JTextField input;

    protected List lines = new ArrayList();
    protected int lineIndex;

    protected JScrollPane outputScrollPane;

    protected JoscarTester tester;
    protected String nick = null;

    public ImTestFrame(JoscarTester tester) {
        lines.add("");
        lineIndex = 1;
        this.tester = tester;
        initializeComponents();
    }

    protected void initializeComponents() {
        final Container p = getContentPane();
        GridBagLayout g = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        p.setLayout(g);

        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.NORTHWEST;

        c.weightx = 1;
        c.weighty = 1;

        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.NORTH;
        output = new JTextArea();
        outputScrollPane = new JScrollPane(output);
        outputScrollPane.setVerticalScrollBarPolicy(
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        outputScrollPane.setHorizontalScrollBarPolicy(
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        output.setEditable(false);
        output.setLineWrap(true);
        output.setWrapStyleWord(true);
        g.setConstraints(outputScrollPane, c);


        c.weighty = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.SOUTH;
        input = new JTextField();
        input.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String text = input.getText();
                lines.add(text);
                input.setText("");

                if (text.startsWith("/set")) {
                    nick = text.substring(5);
                    echo("\nnow sending to " + nick);
                } else if (nick == null) {
                    echo("\nnick is null; set with /set <nick>");
                } else {
                    lineIndex = lines.size();
                    echo("\nsending to " + nick + ": " + text);
                    tester.sendIM(nick, text);
                }
            }
        });
        input.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                int key = e.getKeyCode();
                if (key == KeyEvent.VK_UP) {
                    lineIndex--;
                    if (lineIndex < 0) lineIndex = lines.size() - 1;
                    input.setText((String) lines.get(lineIndex));
                } else if (key == KeyEvent.VK_DOWN) {
                    lineIndex++;
                    if (lineIndex >= lines.size()) lineIndex = 0;
                    input.setText((String) lines.get(lineIndex));
                }
            }
        });
        g.setConstraints(input, c);

        p.add(outputScrollPane);
        p.add(input);

        setTitle("IM tester");
        pack();
        setVisible(true);
    }

    protected synchronized void echo(final String line) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                output.append("\n");
                output.append(line);
            }
        });
    }

}