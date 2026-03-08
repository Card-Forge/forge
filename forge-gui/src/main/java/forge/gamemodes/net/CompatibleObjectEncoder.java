package forge.gamemodes.net;

import forge.gui.GuiBase;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import net.jpountz.lz4.LZ4BlockOutputStream;

import java.io.ObjectOutputStream;
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

        try {
            bout.write(LENGTH_PLACEHOLDER);
            oout = GuiBase.hasPropertyConfig() ? new ObjectOutputStream(new LZ4BlockOutputStream(bout)) : new CObjectOutputStream(new LZ4BlockOutputStream(bout));
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

        // Track actual bytes sent (including compression and all overhead)
        int bytesSent = endIdx - startIdx;
        if (byteTracker != null) {
            String messageType = msg.getClass().getSimpleName();
            byteTracker.recordBytesSent(bytesSent, messageType);
        }
        if (msgSize > 20_000) {
            netLog.info("Encoded {} bytes (compressed) for {}", msgSize, msg.getClass().getSimpleName());
        }
    }
}
