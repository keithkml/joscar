package net.kano.joustsim.oscar.oscar.service.icbm;

import net.kano.joustsim.oscar.oscar.service.Service;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.RvConnectionManager;
import net.kano.joustsim.oscar.oscar.service.icbm.secureim.SecureAimConversation;
import net.kano.joustsim.Screenname;
import net.kano.joscar.rv.RvProcessor;

import java.util.Set;

public interface IcbmService extends Service {
  RvConnectionManager getRvConnectionManager();

  void addIcbmListener(IcbmListener l);

  void removeIcbmListener(IcbmListener l);

  RvProcessor getRvProcessor();

  SecureAimConversation getSecureAimConversation(Screenname sn);

  Set<DirectimConversation> getDirectimConversations(
      Screenname sn);

  ImConversation getImConversation(Screenname sn);

  /**
   * This method sends to whichever conversation is appropriate.
   * <ol>
   * <li> If one or more direct IM conversations are pending or open, the message
   *    is sent to each of them
   * <li> If no DIM conversations are pending or open, but the message is a
   *    {@code DirectMessage}, a new DIM conversation is opened and the message
   *    is sent there
   * <li> If no DIM conversations are pending or open and the message is not a
   *    {@code DirectMessage}, the message is sent through the user's IM
   *    conversation (and one is created if none is not currently open)
   * </ol>
   */
  void sendAutomatically(Screenname sn, Message message);

  void sendTypingAutomatically(Screenname sn, TypingState state);
}
