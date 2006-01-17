package net.kano.joscar.rvcmd;

public interface RejectRvCmd {
  /**
   * A rejection code indicating that the user cancelled or denied a
   * rendezvous request.
   */
  int REJECTCODE_CANCELLED = 0x0001;

  int getRejectCode();
}
