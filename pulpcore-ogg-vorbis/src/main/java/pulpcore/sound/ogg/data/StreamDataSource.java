package pulpcore.sound.ogg.data;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicInteger;

public class StreamDataSource extends ByteArrayDataSource {

    private InputStream stream;
    private final AtomicInteger limit = new AtomicInteger(0);
    private Thread streamingThread;

    public StreamDataSource(InputStream stream, int size) {
        super(new byte[size]);
        this.stream = stream;
    }

    @Override
    public DataSource newView() {
        return new ByteArrayDataSource(data) {
            @Override
            public int limit() {
                return limit.get();
            }
        };
    }

    @Override
    public int limit() {
        return limit.get();
    }

    /**
     Starts streaming from the InputStream in a separate thread.
     Does nothing if already streaming, or if the streaming was previously completed.
     */
    public synchronized void startStreaming() {
        if (stream != null && streamingThread == null) {
            Thread t = new Thread("StreamDataSource") {
                @Override
                public void run() {
                    while (this == streamingThread && stream != null) {
                        downloadChunk();
                    }
                }
            };
            t.start();
            streamingThread = t;
        }
    }

    /**
     Pauses streaming. Streaming can be continued by calling startStreaming().
     */
    public synchronized void pauseStreaming() {
        Thread t = streamingThread;
        if (t != null) {
            streamingThread = null;
            if (Thread.currentThread() != t) {
                try {
                    t.join();
                }
                catch (InterruptedException ex) { }
            }
        }
    }

    /**
     Stops streaming. Once stopped, the streaming cannot be continued.
     */
    public synchronized void stopStreaming() {
        pauseStreaming();
        InputStream s = stream;
        stream = null;
        try {
            s.close();
        }
        catch (IOException ex) { }
    }

    public boolean isStreaming() {
        return (streamingThread != null);
    }

    private void downloadChunk() {
        int maxToWrite = size() - limit();

        if (maxToWrite <= 0) {
            stopStreaming();
        }
        else {
            try {
                int available = Math.min(stream.available(), maxToWrite);
                if (available == 0) {
                    try {
                        Thread.sleep(10);
                    }
                    catch (InterruptedException ex) { }
                }
                else {
                    // Limit size just in case available() is lying.
                    available = Math.min(4096, available);
                    int bytesRead = stream.read(data, limit(), available);
                    limit.addAndGet(bytesRead);
                }
            }
            catch (IOException ex) {
                stopStreaming();
            }
        }
    }

}
