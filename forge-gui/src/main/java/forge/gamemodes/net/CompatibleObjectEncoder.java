package forge.gamemodes.net;

import forge.gamemodes.net.event.GuiGameEvent;
import forge.trackable.Tracker;
import forge.util.IHasForgeLog;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import net.jpountz.lz4.LZ4BlockOutputStream;

import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * Netty outbound handler that frames and serializes one network message per
 * call. The wire format for each message is a 4-byte big-endian length followed
 * by an LZ4-compressed Java serialization stream produced by
 * {@link CObjectOutputStream}. Tracker-aware reference compression (CardView/
 * PlayerView → {@link TrackableSerializer.IdRef}) is delegated to that stream
 * via {@code replaceObject}, gated per-message by {@link #shouldReplaceTrackables}.
 */
public class CompatibleObjectEncoder extends MessageToByteEncoder<Serializable> implements IHasForgeLog {

    static final int LARGE_MESSAGE_LOG_THRESHOLD_BYTES = 20_000;

    private static final byte[] LENGTH_PLACEHOLDER = new byte[4];
    private final NetworkByteTracker byteTracker;
    private volatile Tracker tracker;
    /**
     * Per-client {@code DeltaSyncManager.consumerId} for IdRef gating on the
     * server side. {@code -1} = use tracker presence instead (client-side
     * encoders never set this; server-side wires it via {@link #setTracker}).
     */
    private volatile int consumerId = -1;

    public CompatibleObjectEncoder(NetworkByteTracker byteTracker) {
        this.byteTracker = byteTracker;
    }

    public void setTracker(Tracker tracker) {
        this.tracker = tracker;
    }

    public void setConsumerId(int consumerId) {
        this.consumerId = consumerId;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, Serializable msg, ByteBuf out) throws Exception {
        encodeInto(msg, out, this.tracker, this.consumerId, this.byteTracker);
    }

    /** Caller passes the returned buffer to writeAndFlush, which takes ownership. */
    public ByteBuf encodeToBuf(Serializable msg, ByteBufAllocator alloc) throws Exception {
        ByteBuf out = alloc.buffer();
        try {
            encodeInto(msg, out, this.tracker, this.consumerId, this.byteTracker);
        } catch (Exception e) {
            out.release();
            throw e;
        }
        return out;
    }

    private static void encodeInto(Serializable msg, ByteBuf out, Tracker tracker, int consumerId,
                                   NetworkByteTracker byteTracker) throws Exception {
        int startIdx = out.writerIndex();
        ByteBufOutputStream byteOut = new ByteBufOutputStream(out);
        ObjectOutputStream objectOut = null;

        boolean replace = shouldReplaceTrackables(msg);

        try {
            byteOut.write(LENGTH_PLACEHOLDER);
            objectOut = new CObjectOutputStream(new LZ4BlockOutputStream(byteOut), replace, tracker, consumerId);
            objectOut.writeObject(msg);
            objectOut.flush();
        } finally {
            if (objectOut != null) {
                objectOut.close();
            } else {
                byteOut.close();
            }
        }

        int endIdx = out.writerIndex();
        int msgSize = endIdx - startIdx - 4;
        out.setInt(startIdx, msgSize);

        int bytesSent = endIdx - startIdx;
        if (byteTracker != null) {
            String messageType = msg.getClass().getSimpleName();
            byteTracker.recordBytesSent(bytesSent, messageType);
        }
        if (msgSize > LARGE_MESSAGE_LOG_THRESHOLD_BYTES) {
            netLog.info("Encoded {} bytes (compressed) for {}", msgSize, msg.getClass().getSimpleName());
        }
    }

    /**
     * Determines whether TrackableObject references should be replaced with
     * IdRef markers for this message. Excludes only:
     * - setGameView/openView: carry full state to populate the client's Tracker
     *
     * applyDelta is NOT excluded: its property maps already use Integer IDs
     * (via DeltaSyncManager.toNetworkValue), and bundled events are wrapped
     * by TrackableSerializer.wrapEvents before entering the packet, so no
     * raw TrackableObjects remain in the serialization graph.
     */
    private static boolean shouldReplaceTrackables(Serializable msg) {
        if (msg instanceof GuiGameEvent event) {
            ProtocolMethod method = event.getMethod();
            return method != ProtocolMethod.setGameView
                    && method != ProtocolMethod.openView;
        }
        return true;
    }

}
