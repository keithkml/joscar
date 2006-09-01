package net.kano.joustsim.oscar.oscar.service.bos;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Date;

import net.kano.joscar.snaccmd.FullRoomInfo;

public interface MainBosService extends BosService {
  void setIdleSince(@NotNull Date at) throws IllegalArgumentException;

  void setUnidle();

  Date getIdleSince();

  void setVisibleStatus(boolean visible);

  void setStatusMessage(@Nullable String msg);

  void setStatusMessageSong(@Nullable String msg,
      @Nullable String band, @Nullable String album, @Nullable String song);

  void setStatusMessageSong(@Nullable String msg,
      @Nullable String url);

  void addMainBosServiceListener(MainBosServiceListener listener);

  void removeMainBosServiceListener(MainBosServiceListener listener);

  void requestService(int service,
      OpenedExternalServiceListener listener);

  void requestChatService(FullRoomInfo info,
      OpenedChatRoomServiceListener listener);
}
