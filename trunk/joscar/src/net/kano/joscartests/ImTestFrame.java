/*
 * Created by IntelliJ IDEA.
 * User: Sam
 * Date: Apr 7, 2003
 * Time: 1:19:14 PM
 */
package net.kano.joscartests;

import javax.swing.*;
import java.awt.*;
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