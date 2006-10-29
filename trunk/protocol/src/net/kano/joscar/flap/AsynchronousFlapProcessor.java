package net.kano.joscar.flap;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.Socket;
import java.util.LinkedList;
import net.kano.joscar.logging.Logger;
import net.kano.joscar.logging.LoggingSystem;

/**
 * Created by IntelliJ IDEA. User: klea Date: Aug 8, 2006 Time: 6:38:12 PM To
 * change this template use File | Settings | File Templates.
 */
public class AsynchronousFlapProcessor extends AbstractFlapProcessor {
    private final Object lock = new Object();
    private final LinkedList<FlapPacket> list = new LinkedList<FlapPacket>();

    private final Thread thread;
    private static final FlapPacket[] EMPTY_FLAP_ARRAY = new FlapPacket[0];
	
	private final WeakReference<AsynchronousFlapProcessor> processorReference = new WeakReference<AsynchronousFlapProcessor>(this);

	private static final Logger LOGGER
		= LoggingSystem.getLogger(AsynchronousFlapProcessor.class.getName());

    {
        thread = new Thread(new BackgroundPacketProcessor(processorReference),
                "FLAP processor");
        thread.start();
    }

    public AsynchronousFlapProcessor() {
    }

    public AsynchronousFlapProcessor(Socket socket) throws IOException {
        super(socket);		
    }
	
	protected void finalize() throws Throwable {
		try {
			LOGGER.logFine(this + ": ** finalize() **");
		} finally {
			super.finalize();
		}
	}

    protected void handlePacket(FlapPacket packet) {
        synchronized(lock) {
            list.add(packet);
            lock.notifyAll();
        }
    }

    private static class BackgroundPacketProcessor implements Runnable {
		private final WeakReference<AsynchronousFlapProcessor> backgroundProcessorReference;
		
        public BackgroundPacketProcessor(WeakReference<AsynchronousFlapProcessor> inProcessorReference) {
			backgroundProcessorReference = inProcessorReference;
        }

        public void run() {
            while (true) {
                if (!tryProcessingPackets()) 
					break;
            }
        }

        private boolean tryProcessingPackets() {
            AsynchronousFlapProcessor processor = backgroundProcessorReference.get();
            if (processor == null) {
                return false;
            }
            try {
                for (FlapPacket packet : processor.waitForPackets(100)) {
                    processor.processPacketSynchronously(packet);
                }
            } catch (InterruptedException e) {
                // if we're interrupted we should die
                return false;
            }
            return true;
        }
    }

    private FlapPacket[] waitForPackets(long wait)
            throws InterruptedException {
        synchronized(lock) {
            if (!list.isEmpty()) {
                return clearPackets();
            }
            lock.wait(wait);
            if (!list.isEmpty()) {
                return clearPackets();
            }
        }
        return EMPTY_FLAP_ARRAY;
    }

    private FlapPacket[] clearPackets() {
        assert Thread.holdsLock(lock);
        FlapPacket[] packets = list.toArray(EMPTY_FLAP_ARRAY);
        list.clear();
        return packets;
    }
	
	public void breakdown() {
		synchronized(lock) {
			processorReference.clear();

			super.breakdown();
		}
	}
}
