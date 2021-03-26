package forge.gamemodes.net;

import java.io.ObjectInputStream;
import java.io.StreamCorruptedException;

import forge.gui.GuiBase;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.serialization.ClassResolver;
import net.jpountz.lz4.LZ4BlockInputStream;

public class CompatibleObjectDecoder extends LengthFieldBasedFrameDecoder {
    private final ClassResolver classResolver;

    public CompatibleObjectDecoder(ClassResolver classResolver) {
        this(1048576, classResolver);
    }

    public CompatibleObjectDecoder(int maxObjectSize, ClassResolver classResolver) {
        super(maxObjectSize, 0, 4, 0, 4);
        this.classResolver = classResolver;
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        ByteBuf frame = (ByteBuf)super.decode(ctx, in);
        if (frame == null) {
            return null;
        }
        ObjectInputStream ois = GuiBase.hasPropertyConfig() ?
                new ObjectInputStream(new LZ4BlockInputStream(new ByteBufInputStream(frame, true))):
                    new CObjectInputStream(new LZ4BlockInputStream(new ByteBufInputStream(frame, true)),this.classResolver);

        Object var5 = null;
        try {
            var5 = ois.readObject();
        } catch (StreamCorruptedException e) {
            System.err.println(String.format("Version Mismatch: %s", e.getMessage()));
        } finally {
            ois.close();
        }

        return var5;
    }
}
