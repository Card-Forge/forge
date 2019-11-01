package forge.net;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.serialization.ClassResolver;
import org.mapdb.elsa.ElsaObjectInputStream;

public class CustomObjectDecoder extends LengthFieldBasedFrameDecoder {
    private final ClassResolver classResolver;

    public CustomObjectDecoder(ClassResolver classResolver) {
        this(1048576, classResolver);
    }

    public CustomObjectDecoder(int maxObjectSize, ClassResolver classResolver) {
        super(maxObjectSize, 0, 4, 0, 4);
        this.classResolver = classResolver;
    }

    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        ByteBuf frame = (ByteBuf) super.decode(ctx, in);
        if (frame == null) {
            return null;
        } else {
            ElsaObjectInputStream ois = new ElsaObjectInputStream(new ByteBufInputStream(frame, true));

            Object var5;
            try {
                var5 = ois.readObject();
            } finally {
                ois.close();
            }

            return var5;
        }
    }

    public static int maxObjectsize = 10000000; //10megabyte???
}
