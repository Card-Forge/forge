package forge.gamemodes.net;

import forge.gamemodes.net.event.GuiGameEvent;
import forge.gui.GuiBase;
import forge.trackable.Tracker;
import forge.util.IHasForgeLog;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import net.jpountz.lz4.LZ4BlockOutputStream;

import java.io.ObjectOutputStream;
import java.io.Serializable;

public class CompatibleObjectEncoder extends MessageToByteEncoder<Serializable> implements IHasForgeLog {

    private static final byte[] LENGTH_PLACEHOLDER = new byte[4];
    private final NetworkByteTracker byteTracker;
    private volatile Tracker tracker;

    public CompatibleObjectEncoder(NetworkByteTracker byteTracker) {
        this.byteTracker = byteTracker;
    }

    public void setTracker(Tracker tracker) {
        this.tracker = tracker;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, Serializable msg, ByteBuf out) throws Exception {
        int startIdx = out.writerIndex();
        ByteBufOutputStream bout = new ByteBufOutputStream(out);
        ObjectOutputStream oout = null;

        boolean replace = shouldReplaceTrackables(msg);

        try {
            bout.write(LENGTH_PLACEHOLDER);
            if (GuiBase.hasPropertyConfig()) {
                oout = replace
                        ? new TrackableSerializer.ReplacingOutputStream(new LZ4BlockOutputStream(bout), tracker)
                        : new ObjectOutputStream(new LZ4BlockOutputStream(bout));
            } else {
                oout = new CObjectOutputStream(new LZ4BlockOutputStream(bout), replace);
            }
            oout.writeObject(msg);
            oout.flush();
        } finally {
            if (oout != null) {
                oout.close();
            } else {
                bout.close();
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
        if (msgSize > 20_000) {
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
