/*
 *  Copyright (c) 2005, The Joust Project
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
 */

package net.kano.joustsim.oscar.oscar.service.ssi;

import net.kano.joustsim.Screenname;

import java.util.Set;

public interface PermissionList {
    PrivacyMode getPrivacyMode();

    Set<Screenname> getBlockedBuddies();

    Set<Screenname> getAllowedBuddies();

    void addPermissionListListener(PermissionListListener listener);

    void removePermissionListListener(PermissionListListener listener);

    /**
     * Returns the buddies which are actually currently blocked. Buddies may be
     * on the {@linkplain #getBlockedBuddies() block list}, but not actually
     * blocked because the {@linkplain #getPrivacyMode() privacy mode} is
     * not {@link PrivacyMode#BLOCK_BLOCKED}.
     * <br /><br />
     *
     * @return the list of blocked users if the privacy mode is
     *         {@code BLOCK_BLOCKED}, and an empty set otherwise
     */
    Set<Screenname> getEffectiveBlockedBuddies();

    /**
     * Returns the buddies which are actually currently allowed. Buddies may be
     * on the {@linkplain #getAllowedBuddies() allowed list}, but not actually
     * allowed because the {@linkplain #getPrivacyMode() privacy mode} is
     * not {@link PrivacyMode#ALLOW_ALLOWED}.
     * <br /><br />
     *
     * @return the list of allowed users if the privacy mode is
     *         {@code ALLOW_ALLOWED}, and an empty set otherwise
     */
    Set<Screenname> getEffectiveAllowedBuddies();

    void addToBlockList(Screenname sn);
    void addToAllowedList(Screenname sn);

    void removeFromBlockList(Screenname sn);
    void removeFromAllowedList(Screenname sn);

    void setPrivacyMode(PrivacyMode mode);
}
