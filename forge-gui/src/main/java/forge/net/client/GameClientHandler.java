package forge.net.client;

import forge.FThreads;
import forge.game.player.PlayerView;
import forge.interfaces.IGuiGame;
import forge.match.MatchButtonType;
import forge.model.FModel;
import forge.net.GameProtocol;
import forge.net.GameProtocol.ProtocolMethod;
import forge.net.event.GuiGameEvent;
import forge.net.event.LoginEvent;
import forge.net.event.ReplyEvent;
import forge.properties.ForgePreferences.FPref;
import forge.trackable.TrackableCollection;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

class GameClientHandler extends ChannelInboundHandlerAdapter {
    private final FGameClient client;
    private final IGuiGame gui;

    /**
     * Creates a client-side game handler.
     */
    public GameClientHandler(final FGameClient client) {
        this.client = client;
        this.gui = client.getGui();
    }

    @Override
    public void channelActive(final ChannelHandlerContext ctx) {
        // Don't use send() here, as this.channel is not yet set!
        ctx.channel().writeAndFlush(new LoginEvent(FModel.getPreferences().getPref(FPref.PLAYER_NAME), Integer.parseInt(FModel.getPreferences().getPref(FPref.UI_AVATARS).split(",")[0])));
    }

    @SuppressWarnings("unchecked")
    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) {
        System.out.println("Client received: " + msg);
        if (msg instanceof ReplyEvent) {
            final ReplyEvent event = (ReplyEvent) msg;
            client.getReplyPool().complete(event.getIndex(), event.getReply());
        } else if (msg instanceof GuiGameEvent) {
            final GuiGameEvent event = (GuiGameEvent) msg;
            final String methodName = event.getMethod();

            final Object[] originalArgs = event.getObjects();
            final ProtocolMethod protocolMethod = GameProtocol.getProtocolMethod(methodName);
            if (protocolMethod == null) {
                throw new IllegalStateException(String.format("Protocol method %s unknown", methodName));
            }
            final Method method = protocolMethod.getMethod();
            if (method == null) {
                throw new IllegalStateException(String.format("Method %s not found", protocolMethod.name()));
            }

            final Object toInvoke;
            final Object[] args;
            if (protocolMethod.invokeOnButton()) {
                toInvoke = originalArgs[1] == MatchButtonType.OK ? gui.getBtnOK((PlayerView) originalArgs[0]) : gui.getBtnCancel((PlayerView) originalArgs[0]);
                // Remove the player and button type from the args passed to the method
                args = Arrays.copyOfRange(originalArgs, 2, originalArgs.length);
            } else {
                toInvoke = gui;
                args = originalArgs;
            }

            // Pre-call actions
            switch (protocolMethod) {
            case openView:
                final TrackableCollection<PlayerView> myPlayers = (TrackableCollection<PlayerView>) args[0];
                client.setGameControllers(myPlayers);
                break;
            default:
                break;
            }

            final Class<?> returnType = method.getReturnType();
            if (returnType.equals(Void.TYPE)) {
                FThreads.invokeInEdtNowOrLater(new Runnable() {
                    @Override public final void run() {
                        try {
                            method.invoke(toInvoke, args);
                        } catch (final IllegalAccessException | IllegalArgumentException e) {
                            System.err.println(String.format("Unknown protocol method %s with %d args", methodName, args == null ? 0 : args.length));
                        } catch (final InvocationTargetException e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
            } else {
                Serializable reply = null;
                try {
                    final Object theReply = method.invoke(toInvoke, args);
                    if (theReply instanceof Serializable) {
                        reply = (Serializable) method.invoke(toInvoke, args);
                    } else {
                        System.err.println(String.format("Non-serializable return type %s for method %s", returnType.getName(), methodName));
                    }
                } catch (final IllegalAccessException | IllegalArgumentException e) {
                    System.err.println(String.format("Unknown protocol method %s with %d args, replying with null", methodName, args == null ? 0 : args.length));
                } catch (final InvocationTargetException e) {
                    throw new RuntimeException(e);
                }
                client.send(new ReplyEvent(event.getId(), reply));
            }
        }
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}