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
 *  File created by keith @ Feb 22, 2004
 *
 */

package net.kano.joustsim.text.convbox;

import net.kano.joustsim.Screenname;
import net.kano.joustsim.text.AolRtfString;
import net.kano.joscar.DefensiveTools;

import javax.swing.Icon;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.StyleSheet;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConversationDocument extends HTMLDocument {
    public static final String ATTR_SPLIT_WORDS = "SPLIT_WORDS";

    private static final String ATTR_ICON_IDS = "ICON_IDS";
    private static final char[] SPACE_CHARS = new char[] { ' ' };

    private HTMLDocument.BlockElement rootTable;
    private Map<IconID,Icon[]> iconMap = new HashMap<IconID, Icon[]>();

    public ConversationDocument() {
        setupStyles();
    }

    public ConversationDocument(StyleSheet styles) {
        super(styles);
        setupStyles();
    }

    public ConversationDocument(Content c, StyleSheet styles) {
        super(c, styles);
        setupStyles();
    }

    private void setupStyles() {
        getStyleSheet().addRule(".screenname { }");
        registerInOutStyle("screenname-type-incoming");
        registerInOutStyle("screenname-type-outgoing");
    }

    private void registerInOutStyle(String classname) {
        Style styleifexists = getStyleForClassIfExists(classname);
        if (styleifexists == null || styleifexists.getResolveParent() == null) {
            Style style = getStyleForClass(classname);
            Style snclass = getStyleForClass("screenname");
            style.setResolveParent(snclass);
        }
    }

    public void addConversationDocumentListener(ConversationDocumentListener l) {
        listenerList.add(ConversationDocumentListener.class, l);
    }

    public void removeConversationDocumentListener(ConversationDocumentListener l) {
        listenerList.remove(ConversationDocumentListener.class, l);
    }

    protected AbstractElement createDefaultRoot() {
        writeLock();

        AbstractElement root = super.createDefaultRoot();
        BranchElement body = (BranchElement) root.getElement(0);
        assert body.getName().equalsIgnoreCase("body");

        MutableAttributeSet attrs = new SimpleAttributeSet();
        attrs.addAttribute(StyleConstants.NameAttribute, HTML.Tag.TABLE);
        attrs.addAttribute(HTML.Attribute.VALIGN, "top");
        attrs.addAttribute(HTML.Attribute.CELLSPACING, "0");
        attrs.addAttribute(HTML.Attribute.CELLPADDING, "3");
//        attrs.addAttribute(HTML.Attribute.WIDTH, "100%");

        Element[] els = new Element[1];
        BlockElement rootTable = new BlockElement(body, attrs.copyAttributes());

        attrs.removeAttributes(attrs);
        attrs.addAttribute(StyleConstants.NameAttribute, HTML.Tag.TR);
        BlockElement tr = new BlockElement(rootTable, attrs.copyAttributes());

        attrs.removeAttributes(attrs);
        attrs.addAttribute(StyleConstants.NameAttribute, HTML.Tag.TD);
        BlockElement td = new BlockElement(rootTable, attrs.copyAttributes());

        attrs.removeAttributes(attrs);
        attrs.addAttribute(StyleConstants.NameAttribute, HTML.Tag.IMPLIED);
        BlockElement p = new BlockElement(rootTable, attrs.copyAttributes());

        attrs = new SimpleAttributeSet();
        attrs.addAttribute(StyleConstants.NameAttribute, HTML.Tag.CONTENT);
        RunElement c = new RunElement(rootTable, attrs.copyAttributes(), 0, 1);

        els[0] = c;
        p.replace(0, 0, els);
        els[0] = p;
        td.replace(0, 0, els);
        els[0] = td;
        tr.replace(0, 0, els);
        els[0] = tr;
        rootTable.replace(0, 0, els);
        els[0] = rootTable;
        body.replace(0, body.getElementCount(), els);

        this.rootTable = rootTable;

        writeUnlock();

        return root;
    }

    public void addConversationLine(ConversationLine line) {
        StyleSheet sheet = getStyleSheet();

        Screenname sn = line.getSender();
        AolRtfString str = line.getMessage();

        String snclass = getClassForScreenname(sn);
        AttributeSet snpattr = addCssClass(sheet.getEmptySet(), snclass);
        AttributeSet pattr = addCssClass(sheet.getEmptySet(), "msgbody");
        AttributeSet tdattrs = addCssClass(sheet.getEmptySet(), "msgbodybox");

        Color bgcolor = str.getBackgroundColor();
        if (bgcolor != null) {
            tdattrs = sheet.addAttribute(tdattrs,
                    StyleConstants.Background, bgcolor);
        }

        AttributeSet nowraps = sheet.addAttribute(sheet.getEmptySet(),
                HTML.Attribute.NOWRAP, "nowrap");
        nowraps = sheet.addAttribute(nowraps, HTML.Attribute.ALIGN, "right");
        AttributeSet icontd = nowraps;
        IconID[] iconids = line.getIconIDs();
        if (iconids != null) {
            icontd = sheet.addAttribute(icontd, ATTR_ICON_IDS, iconids);
        }

        ElementSpec[] specs = new ElementSpec[] {
            getTagSpec(HTML.Tag.IMPLIED, false),
            getTagSpec(HTML.Tag.TD, false),
            getTagSpec(HTML.Tag.TR, false),
            getTagSpec(HTML.Tag.TR, true),
            getTagSpec(HTML.Tag.TD, true, icontd),
        };
        ElementSpec[] iconSpecs = getIconSpecs(getIcons(iconids));
        ElementSpec[] specs2 = new ElementSpec[] {
            getTagSpec(HTML.Tag.TD, false),
            getTagSpec(HTML.Tag.TD, true, nowraps),
            getTagSpec(HTML.Tag.IMPLIED, true, snpattr),
            getStringSpec(sn.getFormatted() + ": "),
            getTagSpec(HTML.Tag.IMPLIED, false),
            getTagSpec(HTML.Tag.TD, false),
            getTagSpec(HTML.Tag.TD, true, tdattrs),
            getWordSplittingParagraphElement(pattr),
        };

        ElementSpec[] textSpecs = str.generateDocumentElements();
        ElementSpec[] newlinespecs = new ElementSpec[] {
            getStringSpec(" "),
        };
        ElementSpec[] finspecs = concatenateSpecs(new ElementSpec[][] {
            specs, iconSpecs, specs2, textSpecs, newlinespecs
        });
        insertAtEnd(finspecs);
    }

    private Map<Screenname,Integer> snids = new HashMap<Screenname, Integer>();
    private int nextsnid = 1;

    private synchronized int getScreennameID(Screenname sn) {
        Integer val = snids.get(sn);
        if (val == null) {
            int id = nextsnid;
            snids.put(sn, new Integer(id));
            nextsnid = id + 1;
            return id;
        }
        return val.intValue();
    }

    private String getClassForScreenname(Screenname sn) {
        return "screenname-for-" + sn.getNormal();
    }

    private String getClassForID(int id) {
        return "screenname-num-" + id;
    }

    private String getClassForInOut(boolean outgoing) {
        String classname;
        if (outgoing) classname = "screenname-type-outgoing";
        else classname = "screenname-type-incoming";
        return classname;
    }

    public void registerScreenname(Screenname sn, boolean outgoing) {
        String snclass = getClassForScreenname(sn);
        Style style = getStyleForClass(snclass);
        int id = getScreennameID(sn);
        registerSnidStyle(id, outgoing);
        String snidClass = getClassForID(id);
        style.setResolveParent(getStyleForClass(snidClass));
    }

    private void registerSnidStyle(int id, boolean outgoing) {
        String snidclass = getClassForID(id);
        if (getStyleForClassIfExists(snidclass) == null) {
            Style style = getStyleForClass(snidclass);

            String classname = getClassForInOut(outgoing);

            style.setResolveParent(getStyleForClass(classname));
        }
    }

    private Style getStyleForClassIfExists(String classname) {
        StyleSheet sheet = getStyleSheet();
        String cssclassname = "." + classname;
        return sheet.getStyle(cssclassname);
    }

    private Style getStyleForClass(String classname) {
        StyleSheet sheet = getStyleSheet();
        String cssclassname = "." + classname;
        Style style = sheet.getStyle(cssclassname);
        if (style == null) {
            String rule = cssclassname + " { }";
            System.out.println("adding rule:" + rule);
            sheet.addRule(rule);
            style = sheet.getStyle(cssclassname);
        }
        return style;
    }

    private ElementSpec[] concatenateSpecs(ElementSpec[][] specss) {
        int total = 0;
        for (int i = 0; i < specss.length; i++) {
            total += specss[i].length;
        }
        List<ElementSpec> list = new ArrayList<ElementSpec>(total);
        for (int i = 0; i < specss.length; i++) {
            list.addAll(Arrays.asList(specss[i]));
        }
        return (ElementSpec[]) list.toArray(new ElementSpec[list.size()]);
    }

    private synchronized Icon[] getIcons(IconID[] iconids) {
        List<Icon> list = new ArrayList<Icon>();
        for (int i = 0; i < iconids.length; i++) {
            IconID id = iconids[i];
            Icon[] icons = iconMap.get(id);
            if (icons != null) list.addAll(Arrays.asList(icons));
        }
        return (Icon[]) list.toArray(new Icon[list.size()]);
    }

    public void addLightMessage(AolRtfString str) {
        addMessage(str, "lightmsgbox", "lightmsg");
    }

    public void addErrorMessage(AolRtfString str) {
        addMessage(str, "errormsgbox", "errormsg");
    }

    public void addInfoMessage(AolRtfString str) {
        addMessage(str, "infomsgbox", "infomsg");
    }

    public void addStatusMessage(AolRtfString str) {
        addMessage(str, "statusmsgbox", "statusmsg");
    }

    public void addCustomMessage(AolRtfString str, String boxstyle,
            String textstyle) {
        addMessage(str, boxstyle, textstyle);
    }

    private void addMessage(AolRtfString str, String boxstyle,
            String textstyle) {
        DefensiveTools.checkNull(str, "str");

        StyleSheet sheet = getStyleSheet();

        AttributeSet colcol = sheet.getEmptySet();
        if (boxstyle != null) colcol = addCssClass(colcol, boxstyle);
        colcol = sheet.addAttribute(colcol, HTML.Attribute.COLSPAN, "4");
        colcol = sheet.addAttribute(colcol, HTML.Attribute.ALIGN, "center");

        AttributeSet pattr = sheet.getEmptySet();
        if (textstyle != null) pattr = addCssClass(pattr, textstyle);

        ElementSpec[] specs = new ElementSpec[] {
            getTagSpec(HTML.Tag.IMPLIED, false),
            getTagSpec(HTML.Tag.TD, false),
            getTagSpec(HTML.Tag.TR, false),
            getTagSpec(HTML.Tag.TR, true),
            getTagSpec(HTML.Tag.TD, true, colcol),
            getWordSplittingParagraphElement(pattr),
        };
        ElementSpec[] newlinespecs = new ElementSpec[] {
            getStringSpec(" "),
        };
        ElementSpec[] bigspecs = concatenateSpecs(new ElementSpec[][] {
            specs, str.generateDocumentElements(), newlinespecs,
        });
        insertAtEnd(bigspecs);
    }

    private void insertAtEnd(ElementSpec[] bigspecs) {
        int length = getLength();
        Element currentp = getParagraphElement(length);

        int offset;
        if (currentp == null) offset = length;
        else offset = currentp.getEndOffset()-1;

        try {
            insert(offset, bigspecs);
        } catch (BadLocationException ignored) { }

        ConversationDocumentListener[] listeners
                = (ConversationDocumentListener[]) getListeners(
                        ConversationDocumentListener.class);
        ConversationDocumentEvent event = new ConversationDocumentEvent(this,
                length, getLength());
        for (int i = 0; i < listeners.length; i++) {
            ConversationDocumentListener listener = listeners[i];
            listener.handleEvent(event);
        }
    }

    public void setIconsForID(IconID id, Icon[] icons) {
        writeLock();
        Icon[] sicons = (Icon[]) DefensiveTools.getSafeNonnullArrayCopy(icons, "icons");
        try {
            doSetIconsForID(id, sicons);
        } finally {
            writeUnlock();
        }
    }

    private void doSetIconsForID(IconID id, Icon[] icons) {
        synchronized(this) {
            Icon[] oldIcons = iconMap.put(id, icons);
            if (oldIcons == icons || (oldIcons != null
                    && Arrays.equals(oldIcons, icons))) {
                // there was no change
                return;
            }
        }
        Enumeration trs = rootTable.children();
        while (trs.hasMoreElements()) {
            Element tr = (Element) trs.nextElement();

            if (!(tr instanceof BlockElement)) continue;
            BlockElement trblock = (BlockElement) tr;

            Object name = trblock.getAttribute(StyleConstants.NameAttribute);
            if (name != HTML.Tag.TR) continue;

            Enumeration tds = trblock.children();
            TdSearch: while (tds.hasMoreElements()) {
                Element td = (Element) tds.nextElement();

                if (!(td instanceof BlockElement)) continue;
                BlockElement tdblock = (BlockElement) td;

                IconID[] iconids = (IconID[]) tdblock.getAttribute(ATTR_ICON_IDS);
                if (iconids == null) continue;

                for (int i = 0; i < iconids.length; i++) {
                    IconID iconid = iconids[i];
                    if (iconid != null && iconid.equals(id)) {
                        setIcons(tdblock, getIcons(iconids));
                        break TdSearch;
                    }
                }
            }
        }
    }

    private void setIcons(BlockElement tdblock, Icon[] icons) {
        Icon[] sicons = (Icon[]) DefensiveTools.getSafeNonnullArrayCopy(icons, "icons");

        ElementSpec[] els = getIconSpecs(sicons);

        try {
            int start = tdblock.getStartOffset();
            int end = tdblock.getEndOffset();
            insert(end, els);
            remove(start, end-start);
        } catch (BadLocationException ok) { }
    }

    private ElementSpec[] getIconSpecs(Icon[] icons) {
        ElementSpec[] els;
        if (icons.length == 0) {
            els = new ElementSpec[] { getStringSpec(" ") };
        } else {
            els = new ElementSpec[icons.length];
            for (int i = 0; i < icons.length; i++) {
                Icon icon = icons[i];

                els[i] = getIconSpec(icon, " ");
            }
        }
        return els;
    }

    private ElementSpec getIconSpec(Icon icon, String alt) {
        MutableAttributeSet attrs = new SimpleAttributeSet();
        attrs.addAttribute(StyleConstants.NameAttribute,
                StyleConstants.IconElementName);
        StyleConstants.setIcon(attrs, icon);

        char[] chars;
        if (alt == null || alt.length() == 0) chars = SPACE_CHARS;
        else chars = alt.toCharArray();

        return new ElementSpec(attrs, ElementSpec.ContentType,
                chars, 0, chars.length);
    }

    private AttributeSet addCssClass(AttributeSet attrs, String stylename) {
        return getStyleSheet().addAttribute(attrs,
                HTML.Attribute.CLASS, stylename);
    }

    private ElementSpec getWordSplittingParagraphElement(AttributeSet extras) {
        StyleSheet sheet = getStyleSheet();

        AttributeSet attrs;
        if (extras == null) attrs = sheet.getEmptySet();
        else attrs = extras;

        attrs = sheet.addAttribute(attrs,
                StyleConstants.NameAttribute, HTML.Tag.IMPLIED);
        attrs = sheet.addAttribute(attrs, ATTR_SPLIT_WORDS, Boolean.TRUE);

        return new ElementSpec(attrs, ElementSpec.StartTagType);
    }

    private ElementSpec getStringSpec(String s) {
        return getStringSpec(s, null);
    }

    private ElementSpec getStringSpec(String s, AttributeSet extras) {
        SimpleAttributeSet attrs;
        if (extras == null) attrs = new SimpleAttributeSet();
        else attrs = new SimpleAttributeSet(extras);

        attrs.addAttribute(StyleConstants.NameAttribute, HTML.Tag.CONTENT);

        char[] chars = s.toCharArray();
        return new ElementSpec(attrs, ElementSpec.ContentType,
                chars, 0, chars.length);
    }

    private ElementSpec getTagSpec(HTML.Tag tag, boolean on) {
        return getTagSpec(tag, on, null);
    }

    private ElementSpec getTagSpec(HTML.Tag tag, boolean on,
            AttributeSet extras) {
        AttributeSet tattrs = getAttrs(tag);
        if (extras != null) {
            tattrs = getStyleSheet().addAttributes(tattrs, extras);
        }
        return new ElementSpec(tattrs,
                on ? ElementSpec.StartTagType : ElementSpec.EndTagType);
    }

    private AttributeSet getAttrs(HTML.Tag tag) {
        StyleSheet sheet = getStyleSheet();
        AttributeSet attrs = sheet.getEmptySet();
        attrs = sheet.addAttribute(attrs, StyleConstants.NameAttribute, tag);
        return attrs;
    }
}
