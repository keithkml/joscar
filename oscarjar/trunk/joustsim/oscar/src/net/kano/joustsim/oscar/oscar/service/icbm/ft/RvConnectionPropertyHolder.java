package net.kano.joustsim.oscar.oscar.service.icbm.ft;

public interface RvConnectionPropertyHolder extends RvConnection {
  Key<Boolean> KEY_REDIRECTED = new Key<Boolean>("REDIRECTED");

  <V> void putTransferProperty(Key<V> key, V value);

  <V> V getTransferProperty(Key<V> key);

  class Key<V> {
    private final String name;

    public Key(String name) {
      this.name = name;
    }

    public String getName() {
      return name;
    }

    public String toString() {
      return getName();
    }
  }
}
