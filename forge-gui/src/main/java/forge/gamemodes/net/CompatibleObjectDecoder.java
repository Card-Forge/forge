package forge.gamemodes.net;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.serialization.ClassResolver;
import net.jpountz.lz4.LZ4BlockInputStream;
import org.tinylog.Logger;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.ObjectInputStream;
import java.io.StreamCorruptedException;

/**
 * Decodes LZ4-compressed Java-serialized objects from Netty frames. Auto-detects whether
 * the encoder uses compact ({@link CObjectOutputStream}) or standard serialization on the
 * first message, then caches the result for the connection. This handles mismatched
 * {@code UI_NETPLAY_COMPAT} preferences between server and client.
 */
public class CompatibleObjectDecoder extends LengthFieldBasedFrameDecoder {
    private final ClassResolver classResolver;
    /** Cached format for this connection: null = not yet detected, true = compact, false = standard. */
    private Boolean useCompact;

    public CompatibleObjectDecoder(ClassResolver classResolver) {
        this(1048576, classResolver);
    }

    public CompatibleObjectDecoder(int maxObjectSize, ClassResolver classResolver) {
        super(maxObjectSize, 0, 4, 0, 4);
        this.classResolver = classResolver;
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        ByteBuf frame = (ByteBuf) super.decode(ctx, in);
        if (frame == null) {
            return null;
        }

        try {
            if (useCompact == null) {
                return detectFormat(frame);
            }
            return decodeWith(frame, useCompact);
        } finally {
            if (frame.refCnt() > 0) {
                frame.release();
            }
        }
    }

    private Object detectFormat(ByteBuf frame) {
        byte[] data = new byte[frame.readableBytes()];
        frame.readBytes(data);

        try {
            Object result = readObject(new CObjectInputStream(
                    new LZ4BlockInputStream(new ByteArrayInputStream(data)), classResolver));
            useCompact = true;
            return result;
        } catch (StreamCorruptedException | EOFException e) {
            // Compact format failed — try standard
        } catch (Exception e) {
            Logger.error(e, "Unexpected error during compact format detection");
        }

        try {
            Object result = readObject(new ObjectInputStream(
                    new LZ4BlockInputStream(new ByteArrayInputStream(data))));
            useCompact = false;
            return result;
        } catch (Exception e) {
            Logger.error(e, "Failed to decode with both compact and standard formats");
        }
        return null;
    }

    private Object decodeWith(ByteBuf frame, boolean compact) throws Exception {
        ObjectInputStream ois = compact
                ? new CObjectInputStream(new LZ4BlockInputStream(new ByteBufInputStream(frame, false)), classResolver)
                : new ObjectInputStream(new LZ4BlockInputStream(new ByteBufInputStream(frame, false)));
        return readObject(ois);
    }

    private Object readObject(ObjectInputStream ois) throws Exception {
        try {
            return ois.readObject();
        } finally {
            ois.close();
        }
    }
}
