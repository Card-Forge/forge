package forge.gamemodes.net;

import forge.gamemodes.net.event.GuiGameEvent;
import forge.gamemodes.net.event.ReplyEvent;
import forge.gui.FThreads;
import forge.gui.util.SOptionPane;
import forge.localinstance.skin.FSkinProp;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.tinylog.Logger;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public abstract class GameProtocolHandler<T> extends ChannelInboundHandlerAdapter {

    private final boolean runInEdt;
    protected GameProtocolHandler(final boolean runInEdt) {
        this.runInEdt = runInEdt;
    }

    protected abstract ReplyPool getReplyPool(ChannelHandlerContext ctx);
    protected abstract IRemote getRemote(ChannelHandlerContext ctx);

    protected abstract T getToInvoke(ChannelHandlerContext ctx);
    protected abstract void beforeCall(ProtocolMethod protocolMethod, Object[] args);

    @Override
    public final void channelRead(final ChannelHandlerContext ctx, final Object msg) {
        final String[] catchedError = {""};
        Logger.info("Received: {}", msg);
        if (msg instanceof ReplyEvent) {
            final ReplyEvent event = (ReplyEvent) msg;
            getReplyPool(ctx).complete(event.getIndex(), event.getReply());
        } else if (msg instanceof GuiGameEvent) {
            final GuiGameEvent event = (GuiGameEvent) msg;
            final ProtocolMethod protocolMethod = event.getMethod();
            final String methodName = protocolMethod.name();

            final Method method = protocolMethod.getMethod();
            if (method == null) {
                //throw new IllegalStateException(String.format("Method %s not found", protocolMethod.name()));
                catchedError[0] += String.format("IllegalStateException: Method %s not found (GameProtocolHandler.java Line 43)\n", protocolMethod.name());
                Logger.error("Method {} not found", protocolMethod.name());
            }

            final Object[] args = event.getObjects();
            protocolMethod.checkArgs(args);

            final Object toInvoke = getToInvoke(ctx);

            // Pre-call actions (runs on IO thread — blocks all subsequent messages)
            final long beforeCallStart = System.currentTimeMillis();
            beforeCall(protocolMethod, args);
            final long beforeCallMs = System.currentTimeMillis() - beforeCallStart;
            if (beforeCallMs > 50) {
                Logger.info("beforeCall({}) took {} ms on IO thread", methodName, beforeCallMs);
            }

            final Class<?> returnType = protocolMethod.getReturnType();
            final long receiveTimeMs = System.currentTimeMillis();
            final Runnable toRun = () -> {
                final long startMs = System.currentTimeMillis();
                final long queueDelayMs = startMs - receiveTimeMs;
                if (returnType.equals(Void.TYPE)) {
                    try {
                        method.invoke(toInvoke, args);
                    } catch (final IllegalAccessException | IllegalArgumentException e) {
                        Logger.error("Unknown protocol method {} with {} args", methodName, args == null ? 0 : args.length);
                    } catch (final InvocationTargetException e) {
                        //throw new RuntimeException(e.getTargetException());
                        catchedError[0] += (String.format("RuntimeException: %s (GameProtocolHandler.java Line 65)\n", e.getTargetException().toString()));
                        Logger.error("InvocationTargetException: {}", e.getTargetException().toString());
                    }
                } else {
                    Serializable reply = null;
                    try {
                        final Object theReply = method.invoke(toInvoke, args);
                        if (theReply instanceof Serializable) {
                            protocolMethod.checkReturnValue(theReply);
                            reply = (Serializable) theReply;
                        } else if (theReply != null) {
                            Logger.error("Non-serializable return type {} for method {}, returning null", returnType.getName(), methodName);
                        }
                    } catch (final IllegalAccessException | IllegalArgumentException e) {
                        Logger.error("Unknown protocol method {} with {} args, replying with null", methodName, args == null ? 0 : args.length);
                    } catch (final NullPointerException | InvocationTargetException e) {
                        //throw new RuntimeException(e.getTargetException());
                        catchedError[0] += e.toString();
                        SOptionPane.showMessageDialog(catchedError[0], "Error", FSkinProp.ICO_WARNING);
                        Logger.error("Exception in protocol method {}: {}", methodName, e.toString());
                    }
                    getRemote(ctx).send(new ReplyEvent(event.getId(), reply));
                }
                final long elapsed = System.currentTimeMillis() - startMs;
                if (queueDelayMs > 50 || elapsed > 50) {
                    Logger.info("Protocol {} processed in {} ms (queued {} ms)", methodName, elapsed, queueDelayMs);
                }
            };

            if (runInEdt) {
                FThreads.invokeInEdtNowOrLater(toRun);
            } else {
                FThreads.invokeInBackgroundThread(toRun);
            }
        }
    }

    @Override
    public final void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

}
