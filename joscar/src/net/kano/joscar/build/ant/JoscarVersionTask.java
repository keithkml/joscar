/*
 *  Copyright (c) 2003, The Joust Project
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
 *  File created by keith @ Jun 25, 2003
 *
 */

package net.kano.joscar.build.ant;

import org.apache.tools.ant.Task;
import org.apache.tools.ant.BuildException;

import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

public class JoscarVersionTask extends Task {
    private String property;

    public void setProperty(String property) { this.property = property; }

    public void execute() throws BuildException {
        Class joscarToolsClass;
        try {
            joscarToolsClass = Class.forName("net.kano.joscar.JoscarTools");
        } catch (ClassNotFoundException e) {
            throw new BuildException(e);
        }
        Method versionStringMethod;
        try {
            versionStringMethod = joscarToolsClass.getMethod("getVersionString",
                    new Class[0]);
        } catch (NoSuchMethodException e) {
            throw new BuildException(e);
        } catch (SecurityException e) {
            throw new BuildException(e);
        }

        String version;
        try {
            version = (String) versionStringMethod.invoke(null, new Object[0]);
        } catch (IllegalAccessException e) {
            throw new BuildException(e);
        } catch (IllegalArgumentException e) {
            throw new BuildException(e);
        } catch (InvocationTargetException e) {
            throw new BuildException(e);
        }

        getProject().setProperty(property, version);
    }
}
