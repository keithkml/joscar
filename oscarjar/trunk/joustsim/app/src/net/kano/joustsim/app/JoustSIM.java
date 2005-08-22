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

package net.kano.joustsim.app;

import com.jgoodies.plaf.plastic.Plastic3DLookAndFeel;
import com.jgoodies.plaf.plastic.theme.SkyBluer;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import java.io.File;
import java.security.Security;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class JoustSIM {
    private static final Logger logger
            = Logger.getLogger(JoustSIM.class.getName());

    private JoustSIM() { }

    public static void main(String[] args) {
        System.out.println("starting");
        Logger logger = Logger.getLogger("net.kano");
        logger.setLevel(Level.FINE);
        ConsoleHandler handler = new ConsoleHandler();
        handler.setFormatter(new CoolFormatter());
        handler.setLevel(Level.ALL);
        logger.addHandler(handler);

        Security.addProvider(new BouncyCastleProvider());

        try {
            String laf = UIManager.getSystemLookAndFeelClassName();

            if (laf.endsWith(".GTKLookAndFeel")
                    || laf.endsWith(".MetalLookAndFeel")) {
                Plastic3DLookAndFeel plastic = new Plastic3DLookAndFeel();
                Plastic3DLookAndFeel.setMyCurrentTheme(new SkyBluer());
//                ClearLookManager.installDefaultMode();
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
        if (System.getProperty("os.name").toLowerCase().indexOf("windows") != -1) {
            try {
                net.java.plaf.LookAndFeelPatchManager.initialize();
            } catch (Throwable t) {
                logger.log(Level.WARNING, "Couldn't load WinLAF patches", t);
            }
        }
        System.out.println("set look and feel");

        String home = System.getProperty("user.home", "~");
        JoustsimSession sess = new JoustsimSession(new File(home, ".joustsim"));
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
