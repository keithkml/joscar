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

import java.util.logging.Level;

/**
 * A logger implementation that uses Java 1.4's own
 * <code>java.util.logging</code> logging facilities.
 */
public final class JavaLogger implements Logger {
    /** The underlying Java logging framework logger. */
    private final java.util.logging.Logger logger;

    /**
     * Creates a new Java logger instance representing the given Java logging
     * framework logger.
     *
     * @param logger the logger to use
     */
    JavaLogger(java.util.logging.Logger logger) {
        DefensiveTools.checkNull(logger, "logger");

        this.logger = logger;
    }

    public void logException(String s, Throwable t) {
        logger.log(Level.WARNING, s, t);
    }

    public void logWarning(String s) { logger.warning(s); }

    public void logFine(String s) { logger.fine(s); }

    public void logFiner(String s) { logger.finer(s); }

    public boolean logWarningEnabled() {
        return logger.isLoggable(Level.WARNING);
    }

    public boolean logFineEnabled() {
        return logger.isLoggable(Level.FINE);
    }

    public boolean logFinerEnabled() {
        return logger.isLoggable(Level.FINER);
    }
}
