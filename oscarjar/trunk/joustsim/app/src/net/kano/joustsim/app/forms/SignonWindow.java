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

package net.kano.joustsim.app.forms;

import net.kano.joustsim.app.GuiResources;
import net.kano.joustsim.app.GuiSession;
import net.kano.joscar.DefensiveTools;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class SignonWindow extends JFrame {
    private static final Object STATE_SIGNON_BOX = "SIGNON_BOX";
    private static final Object STATE_SIGNON_PROGRESS_BOX = "SIGNON_PROGRESS_BOX";

    private final GuiSession guiSession;

    private final ImageIcon programIcon = GuiResources.getTinyProgramIcon();
    private SignonWindowBox current = null;
    private Object currentState = null;

    {
        setResizable(false);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                if (currentState == STATE_SIGNON_BOX) {
                    //TODO: only disconnect if the user closes while signing on
                    guiSession.close();
                } else if (currentState == STATE_SIGNON_PROGRESS_BOX) {
                    guiSession.getAimSession().closeConnection();
                }
            }
        });
        if (programIcon != null) {
            setIconImage(programIcon.getImage());
        }
    }

    public SignonWindow(GuiSession session) {
        DefensiveTools.checkNull(session, "session");

        this.guiSession = session;
    }

    public void setToSignonBox() {
        setTitle("Sign On");
        currentState = STATE_SIGNON_BOX;
        setTo(guiSession.getSignonBox());
    }

    public void setToSignonProgressBox() {
        setTitle("Signing On...");
        currentState = STATE_SIGNON_PROGRESS_BOX;
        setTo(guiSession.getSignonProgressBox());
    }

    private void setTo(SignonWindowBox component) {
        getContentPane().removeAll();
        this.current = null;
        if (component != null) {
            getContentPane().add(component.getSignonWindowBoxComponent());
            component.signonWindowBoxShown(this);
            pack();
        }
        this.current = component;
    }

    public void updateSize(SignonWindowBox box) {
        if (box == current) pack();
    }
}
