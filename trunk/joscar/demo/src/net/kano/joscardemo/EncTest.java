
package net.kano.joscardemo;

import net.kano.joscar.ByteBlock;
import net.kano.joscar.OscarTools;
import org.bouncycastle.cms.CMSEnvelopedData;
import org.bouncycastle.cms.CMSProcessable;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.KeyTransRecipientInformation;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.SignerInformationStore;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Iterator;

public class EncTest {
    private static X509Certificate pubCert;
    private static PrivateKey privateKey;

    private static void readKeys() throws Exception {
        KeyStore ks = KeyStore.getInstance("PKCS12", "BC");
        ks.load(new FileInputStream("certificate-info.p12"),
                "pass".toCharArray());
        String alias = (String) ks.aliases().nextElement();
        pubCert = (X509Certificate) ks.getCertificate(alias);
        privateKey = (PrivateKey) ks.getKey(alias,
                "pass".toCharArray());
    }

    public static void readEncryptedData() throws Exception {
        InputStream encDataIn = new FileInputStream("encrypted-message-data");
        CMSEnvelopedData ced = new CMSEnvelopedData(encDataIn);
        Collection recip = ced.getRecipientInfos().getRecipients();

        for (Iterator it = recip.iterator(); it.hasNext();) {
            KeyTransRecipientInformation rinfo
                    = (KeyTransRecipientInformation) it.next();

            byte[] content;
            try {
                content = rinfo.getContent(privateKey, "BC");
            } catch (Exception e) {
                e.printStackTrace();
                continue;
            }

            OscarTools.HttpHeaderInfo hdrInfo
                    = OscarTools.parseHttpHeader(ByteBlock.wrap(content));

            for (Iterator hit = hdrInfo.getHeaders().entrySet().iterator();
                 hit.hasNext();) {
                System.out.println("ENTRY: " + hit.next());
            }

            FileOutputStream fout = new FileOutputStream("pkcsinternalthing");
            hdrInfo.getData().write(fout);
            fout.close();

            InputStream in = ByteBlock.createInputStream(hdrInfo.getData());
            CMSSignedData csd = new CMSSignedData(in);
            SignerInformationStore signerInfos = csd.getSignerInfos();
            Collection signers = signerInfos.getSigners();
            for (Iterator sit = signers.iterator(); sit.hasNext();) {
                SignerInformation si = (SignerInformation) sit.next();
                boolean verified = si.verify(pubCert, "BC");
                System.out.println("verified: " + verified);
            }
            CMSProcessable signedContent = csd.getSignedContent();
            ByteBlock data = ByteBlock.wrap((byte[]) signedContent.getContent());

            OscarTools.HttpHeaderInfo bodyInfo
                    = OscarTools.parseHttpHeader(data);

            String msg = OscarTools.getInfoString(bodyInfo.getData(),
                            (String) bodyInfo.getHeaders().get("content-type"));

            System.out.println(OscarTools.stripHtml(msg));
        }
    }

    public static void main(String[] args) {
        try {
            Class bcp = Class.forName(
                    "org.bouncycastle.jce.provider.BouncyCastleProvider");
            Security.addProvider((Provider) bcp.newInstance());
        } catch (Throwable e) {
            e.printStackTrace();
        }

        try {
            readKeys();
            readEncryptedData();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
