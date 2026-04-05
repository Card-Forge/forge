package forge.gamemodes.net;

import forge.gui.GuiBase;
import forge.trackable.Tracker;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.serialization.ClassResolver;
import net.jpountz.lz4.LZ4BlockInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.StreamCorruptedException;

public class CompatibleObjectDecoder extends LengthFieldBasedFrameDecoder implements IHasNetLog {

    private final ClassResolver classResolver;
    private volatile Tracker tracker;

    public CompatibleObjectDecoder(ClassResolver classResolver) {
        this(1048576, classResolver);
    }

    public CompatibleObjectDecoder(int maxObjectSize, ClassResolver classResolver) {
        super(maxObjectSize, 0, 4, 0, 4);
        this.classResolver = classResolver;
    }

    public void setTracker(Tracker tracker) {
        this.tracker = tracker;
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        int frameStart = in.readerIndex();
        ByteBuf frame = (ByteBuf)super.decode(ctx, in);
        if (frame == null) {
            return null;
        }
        int frameSize = frame.readableBytes();
        long startMs = System.currentTimeMillis();

        Tracker currentTracker = this.tracker;
        ObjectInputStream ois;
        if (GuiBase.hasPropertyConfig()) {
            ois = currentTracker != null
                    ? new ResolvingObjectInputStream(new LZ4BlockInputStream(new ByteBufInputStream(frame, true)), currentTracker)
                    : new ObjectInputStream(new LZ4BlockInputStream(new ByteBufInputStream(frame, true)));
        } else {
            ois = new CObjectInputStream(new LZ4BlockInputStream(new ByteBufInputStream(frame, true)), this.classResolver, currentTracker);
        }

        Object var5 = null;
        try {
            var5 = ois.readObject();
        } catch (StreamCorruptedException e) {
            netLog.error("Version Mismatch: {}", e.getMessage());
        } finally {
            ois.close();
        }

        long elapsed = System.currentTimeMillis() - startMs;
        if (elapsed > 50 || frameSize > 20_000) {
            netLog.info("Decoded {} in {} ms ({} bytes compressed)",
                    var5 != null ? var5.getClass().getSimpleName() : "null", elapsed, frameSize);
        }

        return var5;
    }

    private static class ResolvingObjectInputStream extends ObjectInputStream {
        private final Tracker tracker;

        ResolvingObjectInputStream(InputStream in, Tracker tracker) throws IOException {
            super(in);
            this.tracker = tracker;
            enableResolveObject(true);
        }

        @Override
        protected Object resolveObject(Object obj) {
            return TrackableSerializer.resolve(obj, tracker);
        }
    }
}
