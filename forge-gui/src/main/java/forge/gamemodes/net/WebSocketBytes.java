package forge.gamemodes.net;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.ContinuationWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientProtocolHandler;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;

import java.nio.channels.ClosedChannelException;

/**
 * Carries the netplay byte stream in binary WebSocket frames, so a netplay connection can run through a web server's port.
 * The handlers after this one see channelActive only when the WebSocket handshake completes, because the netplay
 * handlers register the client and send its login on channelActive.
 */
public final class WebSocketBytes extends ChannelDuplexHandler {
    private ChannelPromise opened;

    /** Completes when the WebSocket handshake does. Fails if the other side refuses it or the connection closes first. */
    public ChannelFuture opened() {
        return opened;
    }

    @Override
    public void handlerAdded(final ChannelHandlerContext ctx) {
        opened = ctx.newPromise();
    }

    @Override
    public void channelActive(final ChannelHandlerContext ctx) {
    }

    @Override
    public void channelInactive(final ChannelHandlerContext ctx) throws Exception {
        opened.tryFailure(new ClosedChannelException());
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) throws Exception {
        opened.tryFailure(cause);
        super.exceptionCaught(ctx, cause);
    }

    @Override
    public void userEventTriggered(final ChannelHandlerContext ctx, final Object evt) throws Exception {
        if (evt instanceof WebSocketServerProtocolHandler.HandshakeComplete
                || evt == WebSocketClientProtocolHandler.ClientHandshakeStateEvent.HANDSHAKE_COMPLETE) {
            opened.trySuccess();
            ctx.fireChannelActive();
        }
        super.userEventTriggered(ctx, evt);
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) {
        if (!(msg instanceof WebSocketFrame frame)) {
            ctx.fireChannelRead(msg);
        } else if (frame instanceof BinaryWebSocketFrame || frame instanceof ContinuationWebSocketFrame) {
            ctx.fireChannelRead(frame.content());
        } else {
            // Netplay sends binary frames only. Any other frame is released here because no later handler would release it
            frame.release();
            ctx.close();
        }
    }

    @Override
    public void write(final ChannelHandlerContext ctx, final Object msg, final ChannelPromise promise) {
        ctx.write(msg instanceof ByteBuf bytes ? new BinaryWebSocketFrame(bytes) : msg, promise);
    }
}
