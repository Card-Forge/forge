package forge.net.client;

import io.netty.channel.ChannelHandlerContext;
import forge.game.player.PlayerView;
import forge.interfaces.IGuiGame;
import forge.model.FModel;
import forge.net.GameProtocolHandler;
import forge.net.IRemote;
import forge.net.ProtocolMethod;
import forge.net.ReplyPool;
import forge.net.event.LoginEvent;
import forge.properties.ForgePreferences.FPref;
import forge.trackable.TrackableCollection;

final class GameClientHandler extends GameProtocolHandler<IGuiGame> {
    private final FGameClient client;
    private final IGuiGame gui;

    /**
     * Creates a client-side game handler.
     */
    public GameClientHandler(final FGameClient client) {
        super(true);
        this.client = client;
        this.gui = client.getGui();
    }

    @Override
    protected ReplyPool getReplyPool(final ChannelHandlerContext ctx) {
        return client.getReplyPool();
    }

    @Override
    protected IRemote getRemote(final ChannelHandlerContext ctx) {
        return client;
    }

    @Override
    protected IGuiGame getToInvoke(final ChannelHandlerContext ctx) {
        return gui;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void beforeCall(final ProtocolMethod protocolMethod, final Object[] args) {
        switch (protocolMethod) {
        case openView:
            final TrackableCollection<PlayerView> myPlayers = (TrackableCollection<PlayerView>) args[0];
            client.setGameControllers(myPlayers);
            break;
        default:
            break;
        }
    }

    @Override
    public void channelActive(final ChannelHandlerContext ctx) {
        // Don't use send() here, as this.channel is not yet set!
        ctx.channel().writeAndFlush(new LoginEvent(FModel.getPreferences().getPref(FPref.PLAYER_NAME), Integer.parseInt(FModel.getPreferences().getPref(FPref.UI_AVATARS).split(",")[0])));
    }

}