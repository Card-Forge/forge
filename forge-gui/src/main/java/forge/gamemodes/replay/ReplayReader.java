package forge.gamemodes.replay;

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Reads a replay file one frame at a time, so a game of any length costs only the size of its biggest frame in memory.
 * A file cut short ends cleanly at its last complete frame.
 */
public final class ReplayReader implements Closeable {
    private final DataInputStream in;
    private final String title;
    private int timeMs;
    private byte[] payload;

    private ReplayReader(final DataInputStream in0, final String title0) {
        in = in0;
        title = title0;
    }

    public static ReplayReader open(final File file) throws IOException {
        final DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(file), 32 * 1024));
        try {
            if (in.readInt() != ReplayStore.MAGIC) {
                throw new IOException("Not a Forge replay");
            }
            if (in.readInt() != ReplayStore.FORMAT) {
                throw new IOException("This replay was written by a different version of Forge");
            }
            in.readUTF(); // Forge version
            in.readLong(); // start time
            return new ReplayReader(in, in.readUTF());
        } catch (final IOException | RuntimeException e) {
            in.close();
            throw e;
        }
    }

    public String getTitle() {
        return title;
    }

    /** Moves to the next frame; false at the end of the recording. */
    public boolean next() {
        try {
            timeMs = in.readInt();
            final int len = in.readInt();
            if (len < 0 || len > 64 * 1024 * 1024) {
                return false;
            }
            payload = new byte[len];
            in.readFully(payload);
            return true;
        } catch (final EOFException e) {
            return false;
        } catch (final IOException e) {
            return false;
        }
    }

    /** Milliseconds from the start of the game to the current frame. */
    public int getTimeMs() {
        return timeMs;
    }

    /** The current frame as the netplay codec wrote it, without its length prefix. */
    public byte[] getPayload() {
        return payload;
    }

    @Override
    public void close() throws IOException {
        in.close();
    }
}
