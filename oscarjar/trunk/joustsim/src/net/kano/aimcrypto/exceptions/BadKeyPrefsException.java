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

package net.kano.aimcrypto.exceptions;

public class BadKeyPrefsException extends Exception {
    private final boolean hasSigningAlias;
    private final boolean hasEncryptionAlias;
    private final boolean hasPass;

    public BadKeyPrefsException(boolean hasSigningAlias, boolean hasEncryptionAlias,
            boolean hasPass) {
        this.hasSigningAlias = hasSigningAlias;
        this.hasEncryptionAlias = hasEncryptionAlias;
        this.hasPass = hasPass;
    }

    public BadKeyPrefsException(String message, boolean hasSigningAlias,
            boolean hasEncryptionAlias, boolean hasPass) {
        super(message);
        this.hasSigningAlias = hasSigningAlias;
        this.hasEncryptionAlias = hasEncryptionAlias;
        this.hasPass = hasPass;
    }

    public BadKeyPrefsException(Throwable cause, boolean hasSigningAlias,
            boolean hasEncryptionAlias, boolean hasPass) {
        super(cause);
        this.hasSigningAlias = hasSigningAlias;
        this.hasEncryptionAlias = hasEncryptionAlias;
        this.hasPass = hasPass;
    }

    public BadKeyPrefsException(String message, Throwable cause, boolean hasSigningAlias,
            boolean hasEncryptionAlias, boolean hasPass) {
        super(message, cause);
        this.hasSigningAlias = hasSigningAlias;
        this.hasEncryptionAlias = hasEncryptionAlias;
        this.hasPass = hasPass;
    }

    public boolean hasSigningAlias() { return hasSigningAlias; }

    public boolean hasEncryptionAlias() { return hasEncryptionAlias; }

    public boolean hasPass() { return hasPass; }
}
