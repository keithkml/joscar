package net.kano.joustsim.oscar.oscar.service.icon;

import net.kano.joustsim.oscar.oscar.service.Service;
import net.kano.joscar.Writable;

public interface IconService extends Service, IconRequestHandler {
  void uploadIcon(Writable data, IconSetListener listener);
}
