package net.kano.joustsim.oscar.oscar.service.info;

import net.kano.joustsim.oscar.oscar.service.MutableService;

public interface MutableInfoService extends MutableService, InfoService {
  InfoResponseListener getInfoRequestListener();
}
