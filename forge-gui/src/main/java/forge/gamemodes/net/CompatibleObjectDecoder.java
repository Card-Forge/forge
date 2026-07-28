package forge.gamemodes.net;

import forge.trackable.Tracker;
import forge.util.IHasForgeLog;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.serialization.ClassResolver;
import net.jpountz.lz4.LZ4BlockInputStream;

import java.io.ObjectInputStream;
import java.io.StreamCorruptedException;

/**
 * Netty inbound handler that reads the 4-byte length frame and deserializes
 * one network message per call. The payload is passed to a
 * {@link CObjectInputStream} for LZ4 decompression and Java deserialization;
 * tracker-aware reference resolution ({@link TrackableSerializer.IdRef} →
 * live CardView/PlayerView) happens inside that stream via {@code resolveObject}.
 */
public class CompatibleObjectDecoder extends LengthFieldBasedFrameDecoder implements IHasForgeLog {

    private static final int SLOW_DECODE_LOG_THRESHOLD_MS = 50;

    private final ClassResolver classResolver;
    private volatile Tracker tracker;

    public CompatibleObjectDecoder(int maxObjectSize, ClassResolver classResolver) {
        // LengthFieldBasedFrameDecoder: maxFrameLength, lengthFieldOffset=0,
        // lengthFieldLength=4, lengthAdjustment=0, initialBytesToStrip=4.
        super(maxObjectSize, 0, 4, 0, 4);
        this.classResolver = classResolver;
    }

    public void setTracker(Tracker tracker) {
        this.tracker = tracker;
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        ByteBuf frame = (ByteBuf) super.decode(ctx, in);
        if (frame == null) {
            return null;
        }
        int frameSize = frame.readableBytes();
        long startMs = System.currentTimeMillis();

        ObjectInputStream objectIn = new CObjectInputStream(
                new LZ4BlockInputStream(new ByteBufInputStream(frame, true)),
                this.classResolver, tracker);

        Object result = null;
        try {
            result = objectIn.readObject();
        } catch (StreamCorruptedException e) {
            netLog.error("Version Mismatch: {}", e.getMessage());
        } finally {
            objectIn.close();
        }

        long elapsed = System.currentTimeMillis() - startMs;
        if (elapsed > SLOW_DECODE_LOG_THRESHOLD_MS
                || frameSize > CompatibleObjectEncoder.LARGE_MESSAGE_LOG_THRESHOLD_BYTES) {
            netLog.info("Decoded {} in {} ms ({} bytes compressed)",
                    result != null ? result.getClass().getSimpleName() : "null", elapsed, frameSize);
        }

        return result;
    }

}
