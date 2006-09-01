package net.kano.joustsim.oscar.oscar.service.info;

import net.kano.joustsim.oscar.oscar.service.Service;
import net.kano.joustsim.Screenname;
import net.kano.joscar.snaccmd.CertificateInfo;

public interface InfoService extends Service {
  void addInfoListener(InfoServiceListener l);

  void removeInfoListener(InfoServiceListener l);

  String getLastSetAwayMessage();

  //TODO(klea): find max info length, throw exception if too long
//            or return the truncated version or something
  void setAwayMessage(String awayMessage);

  String getLastSetUserProfile();

  void setUserProfile(String userProfile);

  CertificateInfo getCurrentCertificateInfo();

  void setCertificateInfo(CertificateInfo certificateInfo);

  void requestUserProfile(Screenname buddy);

  void requestUserProfile(Screenname buddy,
      InfoResponseListener listener);

  void requestAwayMessage(Screenname buddy);

  void requestAwayMessage(Screenname buddy,
      InfoResponseListener listener);

  void requestCertificateInfo(Screenname buddy);

  void requestCertificateInfo(Screenname buddy,
      InfoResponseListener listener);

  void requestDirectoryInfo(Screenname buddy);

  void requestDirectoryInfo(Screenname buddy,
      InfoResponseListener listener);
}
