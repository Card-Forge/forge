package forge.net;

import org.apache.commons.lang3.StringUtils;

import forge.GuiBase;
import forge.assets.FSkinProp;
import forge.interfaces.IGuiGame;
import forge.interfaces.ILobbyListener;
import forge.interfaces.ILobbyView;
import forge.interfaces.IPlayerChangeListener;
import forge.interfaces.IUpdateable;
import forge.match.GameLobby.GameLobbyData;
import forge.model.FModel;
import forge.net.client.ClientGameLobby;
import forge.net.client.FGameClient;
import forge.net.event.IdentifiableNetEvent;
import forge.net.event.MessageEvent;
import forge.net.event.NetEvent;
import forge.net.event.UpdateLobbyPlayerEvent;
import forge.net.server.FServerManager;
import forge.net.server.ServerGameLobby;
import forge.player.GamePlayerUtil;
import forge.properties.ForgeProfileProperties;
import forge.properties.ForgePreferences.FPref;
import forge.util.gui.SOptionPane;

public class NetConnectUtil {
    private NetConnectUtil() { }

    public static String getServerUrl() {
        final String url = SOptionPane.showInputDialog("Enter URL of server to join. Leave blank to host your own server.", "Connect to Server");
        if (url == null) { return null; }

        //prompt user for player one name if needed
        if (StringUtils.isBlank(FModel.getPreferences().getPref(FPref.PLAYER_NAME))) {
            GamePlayerUtil.setPlayerName();
        }
        return url;
    }

    public static ChatMessage host(final IOnlineLobby onlineLobby, final IOnlineChatInterface chatInterface) {
        final int port = ForgeProfileProperties.getServerPort();
        final FServerManager server = FServerManager.getInstance();
        final ServerGameLobby lobby = new ServerGameLobby();
        final ILobbyView view = onlineLobby.setLobby(lobby);

        server.startServer(port);
        server.setLobby(lobby);

        lobby.setListener(new IUpdateable() {
            @Override
            public final void update(final boolean fullUpdate) {
                view.update(fullUpdate);
                server.updateLobbyState();
            }
        });
        view.setPlayerChangeListener(new IPlayerChangeListener() {
            @Override
            public final void update(final int index, final UpdateLobbyPlayerEvent event) {
                server.updateSlot(index, event);
                server.updateLobbyState();
            }
        });

        server.setLobbyListener(new ILobbyListener() {
            @Override
            public final void update(final GameLobbyData state, final int slot) {
                // NO-OP, lobby connected directly
            }
            @Override
            public final void message(final String source, final String message) {
                chatInterface.addMessage(new ChatMessage(source, message));
            }
            @Override
            public final void close() {
                // NO-OP, server can't receive close message
            }
        });
        chatInterface.setGameClient(new IRemote() {
            @Override
            public final void send(final NetEvent event) {
                if (event instanceof MessageEvent) {
                    final MessageEvent message = (MessageEvent) event;
                    chatInterface.addMessage(new ChatMessage(message.getSource(), message.getMessage()));
                    server.broadcast(event);
                }
            }
            @Override
            public final Object sendAndWait(final IdentifiableNetEvent event) {
                send(event);
                return null;
            }
        });

        view.update(true);

        return new ChatMessage(null, String.format("Hosting on port %d.", port));
    }

    public static void copyHostedServerUrl() {
        String hostname = FServerManager.getInstance().getLocalAddress();
        String url = hostname + ":" + ForgeProfileProperties.getServerPort();
        GuiBase.getInterface().copyToClipboard(url);
        SOptionPane.showMessageDialog("Share the following URL with anyone who wishes to join your server. It has been copied to your clipboard for convenience.\n\n" + url, "Server URL", SOptionPane.INFORMATION_ICON);
    }

    public static ChatMessage join(final String url, final IOnlineLobby onlineLobby, final IOnlineChatInterface chatInterface) {
        final IGuiGame gui = GuiBase.getInterface().getNewGuiGame();
        final FGameClient client = new FGameClient(FModel.getPreferences().getPref(FPref.PLAYER_NAME), "0", gui);
        onlineLobby.setClient(client);
        chatInterface.setGameClient(client);
        final ClientGameLobby lobby = new ClientGameLobby();
        final ILobbyView view =  onlineLobby.setLobby(lobby);
        lobby.setListener(view);
        client.addLobbyListener(new ILobbyListener() {
            @Override
            public final void message(final String source, final String message) {
                chatInterface.addMessage(new ChatMessage(source, message));
            }
            @Override
            public final void update(final GameLobbyData state, final int slot) {
                lobby.setLocalPlayer(slot);
                lobby.setData(state);
            }
            @Override
            public final void close() {
                SOptionPane.showMessageDialog("Connection to the host was interrupted.", "Error", FSkinProp.ICO_WARNING);
                onlineLobby.setClient(null);
            }
        });
        view.setPlayerChangeListener(new IPlayerChangeListener() {
            @Override
            public final void update(final int index, final UpdateLobbyPlayerEvent event) {
                client.send(event);
            }
        });

        String hostname = url;
        int port = ForgeProfileProperties.getServerPort();

        //see if port specified in URL
        int index = url.indexOf(':');
        if (index >= 0) {
            hostname = url.substring(0, index);
            String portStr = url.substring(index + 1);
            try {
                port = Integer.parseInt(portStr);
            }
            catch (Exception ex) {}
        }

        client.connect(hostname, port);

        return new ChatMessage(null, String.format("Connected to %s:%d", hostname, port));
    }
}
