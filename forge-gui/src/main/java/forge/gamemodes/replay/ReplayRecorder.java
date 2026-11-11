package forge.gamemodes.replay;

import forge.gamemodes.net.CompatibleObjectEncoder;
import forge.gamemodes.net.IRemote;
import forge.gamemodes.net.event.IdentifiableNetEvent;
import forge.gamemodes.net.event.NetEvent;
import forge.trackable.Tracker;
import forge.util.BuildInfo;
import forge.util.IHasForgeLog;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.UnpooledByteBufAllocator;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;

/**
 * The "remote" end of a {@link RecordingGuiGame}: instead of a network channel it appends each message, encoded
 * exactly as the netplay encoder would, to a replay file (see {@link ReplayStore} for the layout).
 *
 * <p>Built for phones: heap buffers only (no direct-memory pool), a small LZ4 block per message, one buffered stream
 * that is flushed every few seconds rather than per message, and no threads. Messages are encoded on the calling
 * (game) thread, which the trackable graph requires anyway. Any I/O problem ends the recording quietly and never
 * reaches the game.
 */
final class ReplayRecorder implements IRemote, IHasForgeLog {
    /** Most messages are a few hundred bytes; the library default of 64 KiB per block would be wasted on them. */
    private static final int LZ4_BLOCK = 16 * 1024;
    private static final int FILE_BUFFER = 32 * 1024;
    private static final long FLUSH_EVERY_MS = 5000;

    private final File file;
    private final DataOutputStream out;
    private final CompatibleObjectEncoder encoder = new CompatibleObjectEncoder(null);
    private final ByteBufAllocator alloc = new UnpooledByteBufAllocator(false);
    private final long startNanos = System.nanoTime();
    private long lastFlushMs;
    private boolean failed;

    ReplayRecorder(final File file0, final String title) throws IOException {
        file = file0;
        out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file0), FILE_BUFFER));
        try {
            out.writeInt(ReplayStore.MAGIC);
            out.writeInt(ReplayStore.FORMAT);
            out.writeUTF(BuildInfo.getVersionString());
            out.writeLong(System.currentTimeMillis());
            out.writeUTF(title);
        } catch (final IOException e) {
            out.close();
            file0.delete();
            throw e;
        }
    }

    /** The codec follows the current game's tracker, as on a remote client's channel. */
    void setCodecTracker(final Tracker tracker, final int consumerId) {
        encoder.setTracker(tracker);
        encoder.setConsumerId(consumerId);
    }

    @Override
    public void send(final NetEvent event) {
        if (failed) {
            return;
        }
        ByteBuf buf = null;
        try {
            buf = encoder.encodeToBuf((Serializable) event, alloc, LZ4_BLOCK);
            buf.skipBytes(4); // the length prefix; the frame header carries its own
            final int len = buf.readableBytes();
            final long nowMs = (System.nanoTime() - startNanos) / 1_000_000L;
            out.writeInt((int) Math.min(nowMs, Integer.MAX_VALUE));
            out.writeInt(len);
            buf.readBytes(out, len);
            if (nowMs - lastFlushMs > FLUSH_EVERY_MS) {
                out.flush();
                lastFlushMs = nowMs;
            }
        } catch (final Exception e) {
            netLog.error(e, "Replay recording stopped");
            failed = true;
            closeQuietly();
        } finally {
            if (buf != null) {
                buf.release();
            }
        }
    }

    /** Nobody answers on a recording, so there is nothing to wait for. */
    @Override
    public Object sendAndWait(final IdentifiableNetEvent event) {
        return null;
    }

    boolean hasFailed() {
        return failed;
    }

    File getFile() {
        return file;
    }

    void close() {
        if (!failed) {
            closeQuietly();
        }
    }

    private void closeQuietly() {
        try {
            out.close();
        } catch (final IOException ignored) {
            // nothing useful left to do
        }
    }
}
