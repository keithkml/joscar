package net.kano.joustsim.oscar.oscar.service.icbm.ft.state;

import net.kano.joustsim.oscar.oscar.service.icbm.dim.SelectorInputStream;
import net.kano.joustsim.oscar.oscar.service.icbm.dim.SelectorOutputStream;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;

public abstract class AbstractStreamInfo
    extends SuccessfulStateInfo implements StreamInfo {
  public InputStream getInputStream() {
    try {
      return new SelectorInputStream(getReadableChannel(),
          getSelectableChannel());
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  public OutputStream getOutputStream() {
    try {
      return new SelectorOutputStream(getWritableChannel(),
          getSelectableChannel());
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }
}
