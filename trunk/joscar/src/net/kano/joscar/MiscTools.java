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
 *  File created by keith @ Mar 28, 2003
 *
 */

package net.kano.joscar;

import java.util.regex.Pattern;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * Provides a set of miscellaneous tools used throughout joscar.
 */
public final class MiscTools {
    /**
     * This private constructor is never called, ensuring that an instance of
     * <code>MiscTools</code> will not be created.
     */
    private MiscTools() { }

    /**
     * Returns the class name of the given object, with the package name
     * stripped off. There must be a better way to do this.
     *
     * @param obj the object whose class name will be returned
     * @return the class name of the given object, without package name
     */
    public static String getClassName(Object obj) {
        return getClassName(obj.getClass());
    }

    /**
     * Returns the name of the given class, with the package name stripped off.
     *
     * @param cl the class whose name will be returned
     * @return the name of the given class, without package name
     */
    public static String getClassName(Class cl) {
        return getClassName(cl.getName());
    }

    /**
     * Returns the class name of the given fully qualified class name. For
     * example, <code>getClassName("net.kano.joscar.MiscTools")</code> would
     * produce the string <code>"MiscTools"</code>.
     *
     * @param fullName the fully qualified class name
     * @return the given fully qualified class name, without the package name
     */
    public static String getClassName(String fullName) {
        return fullName.substring(fullName.lastIndexOf('.') + 1);
    }

    /**
     * Finds a <code>static final</code> field of the given class whose name
     * matches the given pattern and whose value is equal to the given
     * <code>value</code>. The field's name is returned, or <code>null</code> if
     * no field is found.
     *
     * @param cl a class
     * @param value the value to search for
     * @param p a pattern that matches the fields to be searched, or
     *        <code>null</code> to match all fields of the class
     * @return the name of a field matching the given constraints
     */
    public static String findIntField(Class cl, long value, Pattern p) {
        Field[] fields = cl.getFields();

        for (int i = 0; i < fields.length; i++) {
            Field field = fields[i];

            int modifiers = field.getModifiers();

            // only accept static final fields
            if (!Modifier.isStatic(modifiers) || !Modifier.isFinal(modifiers)) {
                continue;
            }

            String fieldName = field.getName();
            if (p != null && !p.matcher(fieldName).matches()) continue;

            try {
                if (field.getLong(null) == value) return fieldName;
            } catch (IllegalAccessException e) {
                continue;
            }
        }

        // we couldn't find anything
        return null;
    }

    /**
     * Finds a <code>static final</code> field of the given class whose name
     * matches the given pattern and whose value is equal to the given
     * <code>value</code>. The field's name is returned, or <code>null</code> if
     * no field is found.
     *
     * @param cl a class
     * @param value the value to search for
     * @param p a pattern that matches the fields to be searched, or
     *        <code>null</code> to match all fields of the class
     * @return the name of a field matching the given constraints
     */
    public static String findEqualField(Class cl, Object value, Pattern p) {
        Field[] fields = cl.getFields();
        for (int i = 0; i < fields.length; i++) {
            Field field = fields[i];

            int modifiers = field.getModifiers();
            if (!Modifier.isStatic(modifiers) || !Modifier.isFinal(modifiers)) {
                continue;
            }

            String fieldName = field.getName();
            if (p != null && !p.matcher(fieldName).matches()) continue;

            Object fieldValue;
            try {
                fieldValue = field.get(null);
            } catch (IllegalAccessException e) {
                continue;
            }
            if (fieldValue == null) {
                if (value == null) return fieldName;
            } else {
                // fieldValue is not null
                if (fieldValue.equals(value)) return fieldName;
            }
        }

        // we didn't find anything
        return null;
    }
}
