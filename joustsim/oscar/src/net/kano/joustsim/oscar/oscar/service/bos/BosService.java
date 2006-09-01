package net.kano.joustsim.oscar.oscar.service.bos;

import net.kano.joustsim.oscar.oscar.service.Service;
import net.kano.joscar.ratelim.RateMonitor;

public interface BosService extends Service {
  RateMonitor getRateMonitor();
}
