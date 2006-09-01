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
 *  File created by keith @ Jan 22, 2004
 *
 */

package net.kano.joustsim.text;

import net.kano.joscar.DefensiveTools;

import javax.swing.text.DefaultStyledDocument.ElementSpec;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTML.Tag;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;
import javax.swing.text.html.parser.ParserDelegator;
import java.awt.Color;
import java.io.IOException;
import java.io.StringReader;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AolRtfString {
  private static final Logger logger
      = Logger.getLogger(AolRtfString.class.getName());

  private static final char[] CHARS_HR = " \n-----\n".toCharArray();
  private static final char[] CHARS_NEWLINE = new char[]{'\n'};

  public static AolRtfString readLine(String text) {
    return readLine(new StyleSheet(), text);
  }

  public static AolRtfString readLine(StyleSheet context, String text) {
    DefensiveTools.checkNull(context, "context");
    DefensiveTools.checkNull(text, "text");

    LineReader reader = new LineReader(context);
    ParserDelegator realParser = new ParserDelegator();
    HTMLEditorKit.Parser parser = new AolRtfFilterParser(realParser);
    try {
      parser.parse(new StringReader(text), reader, false);
    } catch (IOException e) {
      logger.log(Level.WARNING, "Couldn't decode AOLRTF string: " + text, e);
      return null;
    }
    return new AolRtfString(text, reader.getElements(),
        reader.getBackgroundColor());
  }

  private final Color backgroundColor;
  private final String originalText;
  private final LineElement[] elements;

  public AolRtfString(String originalText, LineElement[] elements,
      Color bgColor) {
    DefensiveTools.checkNull(originalText, "originalText");
    DefensiveTools.checkNull(elements, "elements");

    this.originalText = originalText;
    this.elements = elements;
    this.backgroundColor = bgColor;
  }

  public String getOriginalText() {
    return originalText;
  }

  public Color getBackgroundColor() {
    return backgroundColor;
  }

  public LineElement[] getElements() {
    return elements.clone();
  }

  public ElementSpec[] generateDocumentElements() {
    ElementSpec[] specs = new ElementSpec[elements.length];
    for (int i = 0; i < elements.length; i++) {
      specs[i] = getSpec(elements[i]);
    }
    return specs;
  }

  private static ElementSpec getSpec(LineElement element) {
    if (element instanceof TextElement) {
      TextElement te = (TextElement) element;
      char[] chars = te.getString().toCharArray();
      MutableAttributeSet attrs = new SimpleAttributeSet(te.getAttrs());
      if (!attrs.isDefined(StyleConstants.NameAttribute)) {
        attrs.addAttribute(StyleConstants.NameAttribute, HTML.Tag.CONTENT);
      }
      return new ElementSpec(attrs, ElementSpec.ContentType,
          chars, 0, chars.length);

    } else if (element instanceof BreakElement) {
      return getTagSpec(Tag.BR, CHARS_NEWLINE);

    } else if (element instanceof RuleElement) {
      return getTagSpec(Tag.HR, CHARS_HR);

    } else {
      return null;
    }
  }

  private static ElementSpec getTagSpec(Tag tag, char[] alt) {
    SimpleAttributeSet attr = new SimpleAttributeSet();
    if (!attr.isDefined(StyleConstants.NameAttribute)) {
      attr.addAttribute(StyleConstants.NameAttribute, tag);
    }
    return new ElementSpec(attr, ElementSpec.ContentType, alt, 0, alt.length);
  }
}