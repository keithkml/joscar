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
 *  File created by keith @ Apr 12, 2003
 *
 */

package net.kano.joscartests;

import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.tree.TreeCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.util.Date;

public class OnlineBuddyCellRenderer implements TreeCellRenderer {
    private JLabel groupLabel = new JLabel("Buddies");
    private JLabel buddyLabel = new JLabel();

    {
        Font font = groupLabel.getFont();
        groupLabel.setFont(new Font(font.getName(), Font.BOLD, font.getSize()));

        buddyLabel.setVerticalAlignment(JLabel.TOP);
        buddyLabel.setVerticalTextPosition(JLabel.TOP);
    }

    private static String getIdleString(Date date) {
        int mins = (int) ((System.currentTimeMillis() - date.getTime())
                / (60*1000));

        if (mins < 1) return "less than 1min";

        StringBuffer buffer = new StringBuffer();
        if (mins >= 60*24) {
            buffer.append(mins/(60*24));
            buffer.append("d");
            mins /= 24;
        }
        if (mins >= 60) {
            buffer.append(mins/(60));
            buffer.append("h");
            mins /= 60;
        }
        if (mins >= 1) {
            buffer.append(mins);
            buffer.append("m");
        }

        return buffer.toString();
    }

    public Component getTreeCellRendererComponent(JTree tree, Object value,
            boolean selected, boolean expanded,
            boolean leaf, int row, boolean hasFocus) {
        if (!leaf) {
            return groupLabel;
        } else {
            OnlineUserInfo info = (OnlineUserInfo) value;
            StringBuffer text = new StringBuffer();
            text.append("<HTML>");
            text.append(info.getScreenname());
//            buddyLabel.setIcon(info.getImageIcon());
            if (info.isAway() && info.getIdleSince() != null) {
                text.append("<BR>Away, Idle "
                        + getIdleString(info.getIdleSince()));
            } else if (info.getIdleSince() != null) {
                text.append("<BR>Idle " + getIdleString(info.getIdleSince()));
            } else if (info.isAway()) {
                text.append("<BR>Away");
            }

            buddyLabel.setText(text.toString());
            buddyLabel.setForeground(info.getIdleSince() != null ? Color.GRAY
                    : UIManager.getColor("Tree.textForeground"));
            Color selectedbg = UIManager.getColor("Tree.selectionBackground");

            buddyLabel.setBackground(selected
                    ? selectedbg
                    : UIManager.getColor("Tree.textBackground"));
            buddyLabel.setOpaque(selected);

            return buddyLabel;
        }
    }
}
