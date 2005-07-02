package net.kano.joustsim.oscar.oscar.service.icbm.ft;

import net.kano.joscar.rvcmd.RvConnectionInfo;

public interface TransferPropertyHolder extends FileTransfer {
    Key<Boolean> KEY_REDIRECTED = new Key<Boolean>("REDIRECTED");
    Key<RvConnectionInfo> KEY_CONN_INFO = new Key<RvConnectionInfo>("CONN_INFO");

    <V> void putTransferProperty(FileTransferImpl.Key<V> key, V value);

    <V> V getTransferProperty(FileTransferImpl.Key<V> key);

    static class Key<V> {
        private final String name;

        public Key(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }
}
