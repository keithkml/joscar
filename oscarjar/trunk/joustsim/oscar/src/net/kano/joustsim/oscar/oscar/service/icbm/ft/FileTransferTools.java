package net.kano.joustsim.oscar.oscar.service.icbm.ft;

import net.kano.joscar.rvproto.ft.FileTransferChecksum;

import java.nio.channels.FileChannel;
import java.nio.ByteBuffer;
import java.io.IOException;
import java.util.Random;
import java.util.Timer;

public class FileTransferTools {

    public static long getChecksum(FileChannel fileChannel, long offset,
            long length)
            throws IOException {
        long oldPos = fileChannel.position();
        try {
            return getChecksumRuinPosition(fileChannel, offset, length);

        } finally {
            fileChannel.position(oldPos);
        }
    }

    static long getChecksumRuinPosition(FileChannel fileChannel,
            long offset, long length) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        fileChannel.position(offset);
        FileTransferChecksum summer = new FileTransferChecksum();
        long remaining = length;
        while (remaining > 0) {
            buffer.rewind();
            buffer.limit((int) Math.min(remaining, buffer.capacity()));
            int count = fileChannel.read(buffer);
            if (count == -1) break;
            buffer.flip();
            remaining -= buffer.limit();
            summer.update(buffer.array(), buffer.arrayOffset(), buffer.limit());
        }
        if (remaining > 0) {
            throw new IOException("could not get checksum for entire file; "
                    + remaining + " failed of " + length);
        }

        return summer.getValue();
    }

    public static long getRandomIcbmId(Random random) {
        return random.nextLong();
    }

    public static Timer getTimer(FileTransfer transfer) {
        if (transfer instanceof CachedTimerHolder) {
            CachedTimerHolder timerHolder = (CachedTimerHolder) transfer;
            return timerHolder.getTimer();
        } else {
            return new Timer("File transfer scheduler", true);
        }
    }
}
