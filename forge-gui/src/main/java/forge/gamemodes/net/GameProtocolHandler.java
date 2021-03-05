package forge.gamemodes.net;

import forge.gamemodes.net.event.GuiGameEvent;
import forge.gamemodes.net.event.ReplyEvent;
import forge.gui.FThreads;
import forge.gui.util.SOptionPane;
import forge.localinstance.assets.FSkinProp;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

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
        System.out.println("Received: " + msg);
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
                System.err.println(String.format("Method %s not found", protocolMethod.name()));
            }

            final Object[] args = event.getObjects();
            protocolMethod.checkArgs(args);

            final Object toInvoke = getToInvoke(ctx);

            // Pre-call actions
            beforeCall(protocolMethod, args);

            final Class<?> returnType = protocolMethod.getReturnType();
            final Runnable toRun = new Runnable() {
                @Override public final void run() {
                    if (returnType.equals(Void.TYPE)) {
                        try {
                            method.invoke(toInvoke, args);
                        } catch (final IllegalAccessException | IllegalArgumentException e) {
                            System.err.println(String.format("Unknown protocol method %s with %d args", methodName, args == null ? 0 : args.length));
                        } catch (final InvocationTargetException e) {
                            //throw new RuntimeException(e.getTargetException());
                            catchedError[0] += (String.format("RuntimeException: %s (GameProtocolHandler.java Line 65)\n", e.getTargetException().toString()));
                            System.err.println(e.getTargetException().toString());
                        }
                    } else {
                        Serializable reply = null;
                        try {
                            final Object theReply = method.invoke(toInvoke, args);
                            if (theReply instanceof Serializable) {
                                protocolMethod.checkReturnValue(theReply);
                                reply = (Serializable) theReply;
                            } else if (theReply != null) {
                                System.err.println(String.format("Non-serializable return type %s for method %s, returning null", returnType.getName(), methodName));
                            }
                        } catch (final IllegalAccessException | IllegalArgumentException e) {
                            System.err.println(String.format("Unknown protocol method %s with %d args, replying with null", methodName, args == null ? 0 : args.length));
                        } catch (final NullPointerException | InvocationTargetException e) {
                            //throw new RuntimeException(e.getTargetException());
                            catchedError[0] += e.toString();
                            SOptionPane.showMessageDialog(catchedError[0], "Error", FSkinProp.ICO_WARNING);
                            System.err.println(e.toString());
                        }
                        getRemote(ctx).send(new ReplyEvent(event.getId(), reply));
                    }
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
