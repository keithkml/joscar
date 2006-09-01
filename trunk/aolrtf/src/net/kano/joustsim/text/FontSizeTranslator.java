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
 *  File created by keith @ Feb 27, 2004
 *
 */

package net.kano.joustsim.text;

import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.CSS;
import javax.swing.text.html.HTML;
import javax.swing.text.html.StyleSheet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class FontSizeTranslator {
    private static final Pattern PATTERN_PTPX = Pattern.compile("(\\d+)(?:pt|px)?");

    public boolean convertHtmlFontSizeToCSS(StyleSheet sheet,
            MutableAttributeSet html,
            MutableAttributeSet css) {
        Object val = html.getAttribute(HTML.Attribute.SIZE);
        if (val == null) return false;

        String sizestr = val.toString().trim();
        Integer realPointSize = getRealFromHtmlSize(sizestr);
        if (realPointSize == null) return false;

        String cssSize = realPointSize.intValue() + "pt";
        sheet.addCSSAttribute(css, CSS.Attribute.FONT_SIZE, cssSize);
        html.removeAttribute(HTML.Attribute.SIZE);
        return true;
    }

    public String getHtmlFontSizeFromCss(String s) {
        String normal = s.toLowerCase().trim();
        Matcher m = PATTERN_PTPX.matcher(normal);
        if (m.matches()) {
            int real = Integer.parseInt(m.group(1));
            int absolute = getAbsoluteFromReal(real);
            return String.valueOf(absolute);
        } else {
            return null;
        }
    }

    public Integer getRealFromHtmlSize(String htmlSizeString) {
        int absHtmlSize;
        if (htmlSizeString.startsWith("+") || htmlSizeString.startsWith("-")) {
            try {
                absHtmlSize = getAbsoluteFromRelativeHtmlSize(htmlSizeString);
            } catch (NumberFormatException e) {
                return null;
            }

        } else {
            try {
                absHtmlSize = getAbsoluteFromAbsoluteHtmlSize(htmlSizeString);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return new Integer(getRealFromAbsolute(absHtmlSize));
    }

    public int getRealFromAbsolute(int absHtmlSize) {
        int[] realPointSizes = getRealPointSizes();
        int numRealSizes = realPointSizes.length;

        int index;
        if (absHtmlSize < 0) index = 0;
        else if (absHtmlSize > numRealSizes-1) index = numRealSizes-1;
        else index = absHtmlSize;

        return realPointSizes[index];
    }

    public int getAbsoluteFromReal(int realPointSize) {
        int[] realPointSizes = getRealPointSizes();
        int lastIndex = realPointSizes.length-1;
        for (int i = 0; i < lastIndex; i++) {
            int bottom = realPointSizes[i];
            int top = realPointSizes[i+1];

            // if the two sizes are the same, there's nothing to do
            if (bottom >= top) continue;

            if (realPointSize > top) continue;

            int middle = ((top-bottom)/2) + bottom;
            if (realPointSize <= middle) return i;
            else return i+1;
        }
        return lastIndex;
    }

    protected int getAbsoluteFromAbsoluteHtmlSize(String sizestr)
            throws NumberFormatException {
        return Integer.parseInt(sizestr);
    }

    protected int getAbsoluteFromRelativeHtmlSize(String sizestr)
            throws NumberFormatException {
        int relativeSize = getRelativeFromRelativeHtmlSize(sizestr);

        return getAbsoluteFromRelative(relativeSize);
    }

    protected int getRelativeFromRelativeHtmlSize(String sizestr)
            throws NumberFormatException {
        int multiplier = (sizestr.charAt(0) == '-') ? -1 : 1;

        return multiplier * Integer.parseInt(sizestr.substring(1));
    }

    protected abstract int getAbsoluteFromRelative(int relativeSize);

    protected abstract int[] getRealPointSizes();
}
