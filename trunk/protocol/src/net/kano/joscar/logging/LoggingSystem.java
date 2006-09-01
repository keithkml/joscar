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
 *  File created by keith @ Jan 1, 2004
 *
 */

package net.kano.joscar.logging;

import net.kano.joscar.DefensiveTools;

/**
 * A static utility class for the joscar logging system. The default log manager
 * is a {@link JavaLogManager}.
 */
public final class LoggingSystem {
    /**
     * A private constructor that is never called ensures that this class is
     * never instantiated.
     */
    private LoggingSystem() { }

    /** The current log manager. */
    private static LogManager logManager = new JavaLogManager();

    /**
     * Returns a <code>Logger</code> instance for the given namespace using the
     * {@linkplain #getLogManager() current joscar log manager}.
     *
     * @param namespace the namespace whose logger should be returned
     * @return a logger for the given namespace
     *
     * @throws IllegalStateException if the logger returned by the log manager
     *         for the given namespace is <code>null</code>
     */
    public synchronized static Logger getLogger(String namespace)
            throws IllegalStateException {
        DefensiveTools.checkNull(namespace, "namespace");

        Logger logger = logManager.getLogger(namespace);
        if (logger == null) {
            throw new IllegalStateException("logger for " + namespace
                    + " is null, logManager is " + logManager);
        }
        return logger;
    }

    /**
     * Returns the current joscar logging manager.
     *
     * @return the current logging manager
     */
    public synchronized static LogManager getLogManager() {
        return logManager;
    }

    /**
     * Sets the current joscar logging manager to the given logging manager.
     *
     * @param manager a new logging manager
     */
    public synchronized static void setLogManager(LogManager manager) {
        DefensiveTools.checkNull(manager, "manager");

        logManager = manager;
    }
}
