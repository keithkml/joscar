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
 *  File created by keith @ Jun 4, 2003
 *
 */

package net.kano.joscar;

import java.util.Collection;
import java.util.AbstractCollection;
import java.util.Iterator;

public class MultiCollection extends AbstractCollection {
    private final Collection[] parts;
    private final int size;

    public MultiCollection(Collection[] parts) {
        DefensiveTools.checkNull(parts, "parts");

        parts = (Collection[]) parts.clone();

        DefensiveTools.checkNullElements(parts, "parts");

        this.parts = parts;

        int size = 0;
        for (int i = 0; i < parts.length; i++) {
            size += parts[i].size();
        }
        this.size = size;
    }

    public int size() { return size; }

    public Iterator iterator() {
        return new Iterator() {
            private int index = 0;
            private Iterator it = parts[index].iterator();

            public void remove() {
                throw new UnsupportedOperationException();
            }

            public boolean hasNext() {
                advance();

                return it.hasNext();
            }

            public Object next() {
                advance();

                return it.next();
            }

            private void advance() {
                while (!it.hasNext() && index < parts.length) {
                    index++;
                    it = parts[index].iterator();
                }
            }
        };
    }

}
