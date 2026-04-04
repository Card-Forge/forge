package forge.gamemodes.net;

import forge.gamemodes.net.event.GuiGameEvent;
import forge.gui.GuiBase;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import net.jpountz.lz4.LZ4BlockOutputStream;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;

public class CompatibleObjectEncoder extends MessageToByteEncoder<Serializable> implements IHasNetLog {

    private static final byte[] LENGTH_PLACEHOLDER = new byte[4];
    private final NetworkByteTracker byteTracker;

    public CompatibleObjectEncoder(NetworkByteTracker byteTracker) {
        this.byteTracker = byteTracker;
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
                        ? new ReplacingObjectOutputStream(new LZ4BlockOutputStream(bout))
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

    private static boolean shouldReplaceTrackables(Serializable msg) {
        if (msg instanceof GuiGameEvent event) {
            ProtocolMethod method = event.getMethod();
            return method != ProtocolMethod.setGameView && method != ProtocolMethod.openView;
        }
        return false;
    }

    private static class ReplacingObjectOutputStream extends ObjectOutputStream {
        ReplacingObjectOutputStream(OutputStream out) throws IOException {
            super(out);
            enableReplaceObject(true);
        }

        @Override
        protected Object replaceObject(Object obj) {
            return TrackableRef.replace(obj);
        }
    }
}
