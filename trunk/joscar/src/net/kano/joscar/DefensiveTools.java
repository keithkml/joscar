/*
 *  Copyright (c) 2002, The Joust Project
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
 *  File created by keith @ Apr 23, 2003
 *
 */

package net.kano.joscar;



/**
 * A set of utilities for ensuring the validity (and non-<code>null</code>ness)
 * of arguments passed into methods or constructors.
 */
public final class DefensiveTools {
    /**
     * Never let anyone instantiate us.
     */
    private DefensiveTools() { }

    /**
     * Ensures that the given value is greater than or equal to the given
     * minimum value.
     *
     * @param val the value to check
     * @param name the name of this variable, for debugging purposes
     * @param min the minimum value of the given value
     *
     * @throws IllegalArgumentException if the given value is less than the
     *         given minimum
     */
    public static final void checkRange(int val, String name, int min)
            throws IllegalArgumentException {
        if (val < min) {
            throw new IllegalArgumentException(name + " (" + val + ") must " +
                    "be >= " + min);
        }
    }

    /**
     * Ensures that the given value is inclusively between the given minumum
     * and maximum values.
     *
     * @param val the value to check
     * @param name the name of this variable, for debugging purposes
     * @param min the minimum value of the given value
     * @param max the maximum value of the given value
     *
     * @throws IllegalArgumentException if the given value is less than the
     *         given minimum
     */
    public static final void checkRange(int val, String name,
            int min, int max) throws IllegalArgumentException {
        if (val < min || val > max) {
            throw new IllegalArgumentException(name + " (" + val + ") must " +
                    "be >=" + min + " and <=" + max);
        }
    }

    /**
     * Ensures that the given value is not <code>null</code>.
     *
     * @param val the value to check
     * @param name the name of this variable, for debugging purposes
     *
     * @throws NullPointerException if the given value is <code>null</code>
     */
    public static final void checkNull(Object val, String name)
            throws NullPointerException {
        if (val == null) {
            throw new NullPointerException("value of " + name
                    + " cannot be null");
        }
    }

    /**
     * Ensures that each element of the given array is not <code>null</code>.
     *
     * @param array an array of objects
     * @param arrayName the name of this array, for debugging purposes
     *
     * @throws NullPointerException if an element of the given array is
     *         <code>null</code>
     */
    public static void checkNullElements(Object[] array, String arrayName)
            throws NullPointerException {
        checkNull(array, arrayName);

        for (int i = 0; i < array.length; i++) {
            if (array[i] == null) {
                throw new IllegalArgumentException(arrayName + " value must " +
                        "not contain any null elements (" + arrayName + "[" + i
                        + "] == null)");
            }
        }
    }

    /**
     * Ensures that the given value is greater than or equal to the given
     * minimum value.
     *
     * @param val the value to check
     * @param name the name of this variable, for debugging purposes
     * @param min the minimum value of the given value
     *
     * @throws IllegalArgumentException if the given value is less than the
     *         given minimum
     */
    public static void checkRange(long val, String name, int min)
            throws IllegalArgumentException {
        if (val < min) {
            throw new IllegalArgumentException(name + " (" + val + ") must " +
                    "be >= " + min);
        }
    }
}
