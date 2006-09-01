package net.kano.joustsim.oscar;

import net.kano.joustsim.oscar.oscar.BasicConnection;
import net.kano.joustsim.oscar.oscar.service.MutableService;
import net.kano.joustsim.oscar.oscar.service.ServiceArbiter;

class ServiceRequestInfo<S extends MutableService> {
  public final long startTime = System.currentTimeMillis();
  public final int family;
  public final ServiceArbiter<S> arbiter;
  private BasicConnection connection = null;
  private boolean canceled = false;

  public ServiceRequestInfo(int family, ServiceArbiter<S> arbiter) {
    this.family = family;
    this.arbiter = arbiter;
  }

  public synchronized BasicConnection getConnection() {
    return connection;
  }

  public synchronized void setConnection(BasicConnection connection) {
    this.connection = connection;
  }

  public void cancel() {
    canceled = true;
    BasicConnection connection = getConnection();
    if (connection != null) connection.disconnect();
  }

  public boolean isCanceled() {
    return canceled;
  }

  public String toString() {
    return "<" + family + "> " + arbiter;
  }
//
//        public boolean isChatRequest() {
//            return chatRequest;
//        }
//
//        public @Nullable ChatInfo getChatInfo() {
//            return chatInfo;
//        }


  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ServiceRequestInfo that = (ServiceRequestInfo) o;

    if (family != that.family) return false;
    return !(arbiter != null ? !arbiter.equals(that.arbiter)
        : that.arbiter != null);

  }

  public int hashCode() {
    int result = family;
    result = 31 * result + (arbiter != null ? arbiter.hashCode() : 0);
    return result;
  }
}
