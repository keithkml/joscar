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
 *  File created by keith @ Jan 24, 2004
 *
 */

package net.kano.aimcrypto.conv;

import java.util.Random;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Iterator;

public class ListTester {
    private final Object obj = new Object();
    private Random random = new Random();

    public void start() {
        testRandoms(5);
        testRandoms(10);
        testRandoms(20);
    }

    private void testRandoms(int lsize) {
        testRandom(lsize, 3);
        testRandom(lsize, 5);
        testRandom(lsize, 11);
        testRandom(lsize, 20);
        testRandom(lsize, 50);
        testRandom(lsize, 100);
    }

    private void testRandom(int lsize, int num) {
        System.gc();
        System.out.println("Adding " + num + " to 10,000 ArrayLists of length " + lsize);
        List[] lists = new List[10000];
        long start = System.currentTimeMillis();
        for (int i = 0; i < lists.length; i++) {
            lists[i] = new ArrayList(lsize);
        }
        for (int i = 0; i < lists.length; i++) {
            for (int j = 0; j < num; j++) {
                lists[i].add(obj);
            }
        }
        long took = System.currentTimeMillis() - start;
        System.out.println("Took " + took + "ms (" + ((float) took)/num + " per element)");
        System.out.println("Iterating...");
        start = System.currentTimeMillis();
        Object temp = null;
        for (int i = 0; i < lists.length; i++) {
            for (Iterator it = lists[i].iterator(); it.hasNext();) {
                temp = it.next();
                if (temp == lists) break;
            }
        }
        took = System.currentTimeMillis() - start;
        System.out.println("Took " + took + "ms (" + ((float) took)/num + " per element)");
        System.out.println("");
        System.out.println("Adding " + num + " to 10,000 LinkedLists");
        for (int i = 0; i < lists.length; i++) {
            lists[i] = null;
        }
        System.gc();
        start = System.currentTimeMillis();
        for (int i = 0; i < lists.length; i++) {
            lists[i] = new LinkedList();
        }
        for (int i = 0; i < lists.length; i++) {
            for (int j = 0; j < num; j++) {
                lists[i].add(obj);
            }
        }
        took = System.currentTimeMillis() - start;
        System.out.println("Took " + took + "ms (" + ((float) took)/num + " per element)");
        System.out.println("Iterating...");
        start = System.currentTimeMillis();
        for (int i = 0; i < lists.length; i++) {
            for (Iterator it = lists[i].iterator(); it.hasNext();) {
                temp = it.next();
                if (temp == lists) break;
            }
        }
        took = System.currentTimeMillis() - start;
        System.out.println("Took " + took + "ms (" + ((float) took)/num + " per element)");
        System.out.println("");
        System.out.println("***");
    }

    public static void main(String[] args) {
        new ListTester().start();
    }
}
