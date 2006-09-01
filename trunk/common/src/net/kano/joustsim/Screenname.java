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

package net.kano.joustsim;

import net.kano.joscar.DefensiveTools;

public final class Screenname implements Comparable {
    private final String format;
    private final String normal;
    private final int hashCode;

    public Screenname(String format) {
        DefensiveTools.checkNull(format, "format");

        this.format = format;
        this.normal = normalize(format);
        this.hashCode = normal.hashCode();
    }

    public String getFormatted() { return format; }

    public String getNormal() { return normal; }

    public boolean matches(String screenname) {
        DefensiveTools.checkNull(screenname, "screenname");

        return normalize(screenname).equals(normal);
    }

    public int compareTo(Object o) {
        return format.compareTo(format);
    }

    public int hashCode() {
        return hashCode;
    }

    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Screenname)) return false;

        Screenname sn = (Screenname) obj;

        return hashCode() == sn.hashCode()
                && getNormal().equals(sn.getNormal());
    }

    public String toString() {
        return format;
    }

  /**
     * Returns a "normalized" version of the given string by removing all spaces
   * and converting to lowercase. Several aspects of the AIM protocol are
   * "normalized": an IM to "joustacular" is the same as an IM to "Joust
   * Acular". Similarly, joining the chat room "JoUsTeRrIfIc" is equivalent
   * to joining "Jousterrific".
   * <br>
   * <br>
   * This method will return the original string <code>str</code> if the
   * string is already in normal form (that is, if no modifications are
   * needed to normalize it).
   *
   * @param str the string to normalize
   * @return a normalized version of the given string, or the given string if
   *         it is already in normal form
   */
  public static String normalize(String str) {
      DefensiveTools.checkNull(str, "str");

      // see if it's already normalized. this doesn't hurt performance on my
      // machine.
      boolean normalized = true;
      int len = str.length();
      for (int i = 0; i < len; i++) {
          char c = str.charAt(i);
          if ((c < '0' || c > '9') && (c < 'a' || c > 'z')) {
              normalized = false;
              break;
          }
      }
      if (normalized) return str;

      StringBuffer buffer = new StringBuffer(len);

      for (int i = 0; i < len; i++) {
          char c = str.charAt(i);

          if (c != ' ') buffer.append(Character.toLowerCase(c));
      }

      return buffer.toString();
  }
}
