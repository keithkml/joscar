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
 *  File created by keith @ Jan 14, 2004
 *
 */

package net.kano.aimcrypto;

import net.kano.aimcrypto.forms.SignonWindow;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.plaf.metal.MetalLookAndFeel;
import java.io.File;
import java.io.StringWriter;
import java.io.PrintWriter;
import java.util.logging.ConsoleHandler;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;
import java.security.Security;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import com.jgoodies.plaf.plastic.Plastic3DLookAndFeel;
import com.jgoodies.plaf.plastic.PlasticLookAndFeel;
import com.jgoodies.plaf.plastic.theme.DesertBlue;
import com.jgoodies.plaf.plastic.theme.SkyBluer;
import com.jgoodies.clearlook.ClearLookManager;

public final class AimCrypto {
    private AimCrypto() { }

    public static void main(String[] args) {
        System.out.println("starting");
        Logger logger = Logger.getLogger("net.kano.aimcrypto");
        logger.setLevel(Level.FINE);
        ConsoleHandler handler = new ConsoleHandler();
        handler.setFormatter(new Formatter() {
            public String format(LogRecord record) {
                String clname = record.getSourceClassName();
                String shname = clname.substring(clname.lastIndexOf('.') + 1);
                Throwable thrown = record.getThrown();
                StringWriter sw = null;
                if (thrown != null) {
                    sw = new StringWriter();
                    thrown.printStackTrace(new PrintWriter(sw));
                }
                return "[" + record.getLevel() + "] "
                        + shname + ": "
                        + record.getMessage() + (sw == null ? ""
                        : sw.getBuffer().toString()) + "\n";
            }
        });
        handler.setLevel(Level.ALL);
        logger.addHandler(handler);

        Security.addProvider(new BouncyCastleProvider());

        try {
            String laf = UIManager.getSystemLookAndFeelClassName();

            if (laf.endsWith(".GTKLookAndFeel")
                    || laf.endsWith(".MetalLookAndFeel")) {
                Plastic3DLookAndFeel plastic = new Plastic3DLookAndFeel();
                Plastic3DLookAndFeel.setMyCurrentTheme(new SkyBluer());
                ClearLookManager.installDefaultMode();
                UIManager.setLookAndFeel(plastic);
            } else {
                UIManager.setLookAndFeel(laf);
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }
        System.out.println("set look and feel");

        String home = System.getProperty("user.home", "~");
        AppSession sess = new AppSession(new File(home, ".joustsim"));
        sess.setSavePrefsOnExit(true);
        System.out.println("initialized app session");

        GuiSession guiSession = new GuiSession(sess);
        guiSession.addListener(new GuiSessionListener() {
            public void opened(GuiSession session) {
            }

            public void closed(GuiSession session) {
                System.exit(0);
            }
        });
        guiSession.open();
        System.out.println("initialized gui session");
    }
}
