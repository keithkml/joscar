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

/**
 * An abstract logging interface.
 */
public interface Logger {
    /**
     * Logs the given message at a "warning" level. This level is for the most
     * important messages.
     *
     * @param s the message to log
     *
     * @see java.util.logging.Level#WARNING
     */
    void logWarning(String s);
    /**
     * Logs the given message at a "fine" level. This level is for fine-grained
     * messages that may only be useful while debugging.
     *
     * @param s the message to log

     * @see java.util.logging.Level#FINE
     */
    void logFine(String s);
    /**
     * Logs the given message at a "finer" level than {@linkplain
     * #logFine(String) "fine"}. This level is for very fine-grained
     * messages that are probably only useful while debugging.
     *
     * @param s the message to log

     * @see java.util.logging.Level#FINER
     */
    void logFiner(String s);
    /**
     * Logs the given message and exception at the same level as
     * {@link #logWarning(String) logWarning} would.
     *
     * @param s the message to log
     * @param t the exception to log
     */
    void logException(String s, Throwable t);

    /**
     * Returns whether logging at the {@linkplain #logWarning(String) "warning"}
     * level is currently enabled.
     *
     * @return whether logging at the "warning" level is currently enabled
     */
    boolean logWarningEnabled();
    /**
     * Returns whether logging at the {@linkplain #logFine(String) "fine"} level
     * is currently enabled.
     *
     * @return whether logging at the "fine" level is currently enabled
     */
    boolean logFineEnabled();
    /**
     * Returns whether logging at the {@linkplain #logFiner(String) "finer"}
     * level is currently enabled.
     *
     * @return whether logging at the "finer" level is currently enabled
     */
    boolean logFinerEnabled();
}
