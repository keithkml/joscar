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
 *  File created by keith @ Jan 31, 2004
 *
 */

package net.kano.aimcrypto;

import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.x509.X509Name;

import java.security.Principal;
import java.security.cert.X509Certificate;
import java.util.Vector;

public class DistinguishedName {
    public static DistinguishedName getIssuerInstance(X509Certificate cert) {
        return getInstance(cert.getIssuerDN());
    }
    public static DistinguishedName getSubjectInstance(X509Certificate cert) {
        return getInstance(cert.getSubjectDN());
    }

    public static DistinguishedName getInstance(Principal principal) {
        X509Name x509Name = (X509Name) principal;

        Vector oids = x509Name.getOIDs();
        Vector values = x509Name.getValues();

        String name = null;
        String email = null;
        String orgUnit = null;
        String organization = null;
        String city = null;
        String state = null;
        String country = null;

        int size = Math.min(oids.size(), values.size());

        for (int i = 0; i < size; i++) {
            DERObjectIdentifier oid = (DERObjectIdentifier) oids.get(i);
            String value = (String) values.get(i);

            if (oid == null) continue;

            if (oid.equals(X509Name.CN)) name = value;
            else if (oid.equals(X509Name.E)) email = value;
            else if (oid.equals(X509Name.OU)) orgUnit = value;
            else if (oid.equals(X509Name.O)) organization = value;
            else if (oid.equals(X509Name.L)) city = value;
            else if (oid.equals(X509Name.ST)) state = value;
            else if (oid.equals(X509Name.C)) country = value;
        }

        return new DistinguishedName(name, email, orgUnit, organization, city,
                state, country);
    }

    private final String name;
    private final String email;
    private final String orgUnit;
    private final String organization;
    private final String city;
    private final String state;
    private final String country;

    private DistinguishedName(String name, String email, String orgUnit,
            String organization, String city, String state, String country) {
        this.name = name;
        this.email = email;
        this.orgUnit = orgUnit;
        this.organization = organization;
        this.city = city;
        this.state = state;
        this.country = country;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getOrgUnit() {
        return orgUnit;
    }

    public String getOrganization() {
        return organization;
    }

    public String getCity() {
        return city;
    }

    public String getState() {
        return state;
    }

    public String getCountry() {
        return country;
    }
}
