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
 *  File created by keith @ Feb 10, 2004
 *
 */

package net.kano.aimcrypto.forms;

import net.kano.joscar.DefensiveTools;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.LinkedHashSet;
import java.util.Set;

public class MiniDialogStack extends JPanel {
    private Set popping = new LinkedHashSet();

    {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setVisible(false);
    }

    public synchronized boolean popup(MiniDialog dialog) {
        DefensiveTools.checkNull(dialog, "dialog");

//        if (popping.contains(dialog)) return false;

//        popping.add(dialog);
        final Component comp = dialog.getComponent();
        comp.addComponentListener(new ComponentAdapter() {
            public void componentHidden(ComponentEvent e) {
                comp.removeComponentListener(this);
                remove(comp);
                updateVisibility();
            }
        });
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                add(comp);
                updateVisibility();
            }
        });
        return true;
    }

    private void updateVisibility() {
        boolean visible = getComponentCount() > 0;
        System.out.println("setting stack to visible=" + visible);
        setVisible(visible);
    }
}
