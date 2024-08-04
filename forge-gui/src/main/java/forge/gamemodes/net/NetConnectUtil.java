package forge.gamemodes.net;

import org.apache.commons.lang3.StringUtils;

import forge.gamemodes.match.GameLobby.GameLobbyData;
import forge.gamemodes.match.LobbySlotType;
import forge.gamemodes.net.client.ClientGameLobby;
import forge.gamemodes.net.client.FGameClient;
import forge.gamemodes.net.event.IdentifiableNetEvent;
import forge.gamemodes.net.event.MessageEvent;
import forge.gamemodes.net.event.NetEvent;
import forge.gamemodes.net.server.FServerManager;
import forge.gamemodes.net.server.ServerGameLobby;
import forge.gui.GuiBase;
import forge.gui.interfaces.IGuiGame;
import forge.gui.interfaces.ILobbyView;
import forge.gui.util.SOptionPane;
import forge.interfaces.ILobbyListener;
import forge.interfaces.IUpdateable;
import forge.localinstance.properties.ForgeConstants;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.localinstance.properties.ForgeProfileProperties;
import forge.model.FModel;
import forge.player.GamePlayerUtil;
import forge.util.Localizer;

public class NetConnectUtil {
    private NetConnectUtil() { }

    public static String getServerUrl() {
        final String url = SOptionPane.showInputDialog(Localizer.getInstance().getMessage("lblOnlineMultiplayerDest"), Localizer.getInstance().getMessage("lblConnectToServer"));
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
            public void update(final boolean fullUpdate) {
                view.update(fullUpdate);
                server.updateLobbyState();
            }
            @Override
            public void update(final int slot, final LobbySlotType type) {return;}
        });
        view.setPlayerChangeListener((index, event) -> {
            server.updateSlot(index, event);
            server.updateLobbyState();
        });

        server.setLobbyListener(new ILobbyListener() {
            @Override
            public void update(final GameLobbyData state, final int slot) {
                // NO-OP, lobby connected directly
            }
            @Override
            public void message(final String source, final String message) {
                chatInterface.addMessage(new ChatMessage(source, message));
            }
            @Override
            public void close() {
                // NO-OP, server can't receive close message
            }
            @Override
            public ClientGameLobby getLobby() {
                return null;
            }
        });
        chatInterface.setGameClient(new IRemote() {
            @Override
            public void send(final NetEvent event) {
                if (event instanceof MessageEvent) {
                    final MessageEvent message = (MessageEvent) event;
                    chatInterface.addMessage(new ChatMessage(message.getSource(), message.getMessage()));
                    server.broadcast(event);
                }
            }
            @Override
            public Object sendAndWait(final IdentifiableNetEvent event) {
                send(event);
                return null;
            }
        });

        view.update(true);

        return new ChatMessage(null, Localizer.getInstance().getMessage("lblHostingPortOnN", String.valueOf(port)));
    }

    public static void copyHostedServerUrl() {
        String internalAddress = FServerManager.getInstance().getLocalAddress();
        String externalAddress = FServerManager.getInstance().getExternalAddress();
        String internalUrl = internalAddress + ":" + ForgeProfileProperties.getServerPort();
        String externalUrl = null;
        if (externalAddress != null) {
            externalUrl = externalAddress + ":" + ForgeProfileProperties.getServerPort();
            GuiBase.getInterface().copyToClipboard(externalUrl);
        } else {
            GuiBase.getInterface().copyToClipboard(internalAddress);
        }

        String message = "";
        if (externalUrl != null) {
            message = Localizer.getInstance().getMessage("lblShareURLToMakePlayerJoinServer", externalUrl, internalUrl);
        } else {
            message = Localizer.getInstance().getMessage("lblForgeUnableDetermineYourExternalIP", message + internalUrl);
        }
        SOptionPane.showMessageDialog(message, Localizer.getInstance().getMessage("lblServerURL"), SOptionPane.INFORMATION_ICON);
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
            public void message(final String source, final String message) {
                chatInterface.addMessage(new ChatMessage(source, message));
            }
            @Override
            public void update(final GameLobbyData state, final int slot) {
                lobby.setLocalPlayer(slot);
                lobby.setData(state);
            }
            @Override
            public void close() {
                GuiBase.setInterrupted(true);
                onlineLobby.closeConn(Localizer.getInstance().getMessage("lblYourConnectionToHostWasInterrupted", url));
            }
            @Override
            public ClientGameLobby getLobby() {
                return lobby;
            }
        });
        view.setPlayerChangeListener((index, event) -> client.send(event));

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

        try {
            client.connect(hostname, port);
        }
        catch (Exception ex) {
            //return a message to close the connection so we will not crash...
            return new ChatMessage(null, ForgeConstants.CLOSE_CONN_COMMAND);
        }

        return new ChatMessage(null, Localizer.getInstance().getMessage("lblConnectedIPPort", hostname, String.valueOf(port)));
    }
}
