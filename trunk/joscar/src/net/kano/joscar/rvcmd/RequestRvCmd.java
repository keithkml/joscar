package net.kano.joscar.rvcmd;

public interface RequestRvCmd {
  int REQINDEX_FIRST = 1;

  boolean isFirstRequest();

  int getRequestIndex();
}
