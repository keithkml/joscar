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
 *  File created by keith @ Apr 1, 2003
 *
 */

package net.kano.joscartools.taglets;

import com.sun.javadoc.Tag;
import com.sun.tools.doclets.Taglet;

import java.util.Map;

/**
 * A javadoc taglet that provides a <code>@rvproxy.src</code> tag to indicate
 * the source of an RV Proxy command (client or server).
 */
public class RvProxySourceTaglet implements Taglet {
    /**
     * Registers this taglet with the given taglet map.
     *
     * @param tagletMap a taglet map with which to register this taglet
     */
    public static void register(Map<String,RvProxySourceTaglet> tagletMap) {
        RvProxySourceTaglet instance = new RvProxySourceTaglet();
        tagletMap.put(instance.getName(), instance);
    }

    public boolean inField() { return false; }

    public boolean inConstructor() { return false; }

    public boolean inMethod() { return false; }

    public boolean inOverview() { return false; }

    public boolean inPackage() { return false; }

    public boolean inType() { return true; }

    public boolean isInlineTag() { return false; }

    public String getName() { return "rvproxy.src"; }

    public String toString(Tag tag) {
        StringBuffer buffer = new StringBuffer();

        buffer.append("<dt title=\"Where this RV Proxy command comes from on " +
                "a normal AOL Proxy Server connection\"><b>Source of this " +
                "RV Proxy command:</b></dt><dd> ");

        String source = tag.text();
        if (source.equals("client")) {
            buffer.append("AIM client");
        } else if (source.equals("server")) {
            buffer.append("AOL Proxy Server");
        } else if (source.equals("both")) {
            buffer.append("both AIM client and AOL Proxy Server");
        } else {
            buffer.append(source);
        }
        buffer.append("</dd>");

        return buffer.toString();
    }

    public String toString(Tag[] tags) {
        if (tags.length == 0) return null;

        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < tags.length; i++) {
            if (i != 0) buffer.append(System.getProperty("line.separator"));

            buffer.append(toString(tags[i]));
        }

        return buffer.toString();
    }
}
