package forge.gamemodes.net;

import forge.gamemodes.match.GameLobby.GameLobbyData;
import forge.gamemodes.match.LobbySlotType;
import forge.gamemodes.net.client.ClientGameLobby;
import forge.gamemodes.net.client.FGameClient;
import forge.gamemodes.net.event.IdentifiableNetEvent;
import forge.gamemodes.net.event.MessageEvent;
import forge.gamemodes.net.event.NetEvent;
import forge.gamemodes.net.server.FServerManager;
import forge.gamemodes.net.server.ServerGameLobby;
import forge.localinstance.properties.ForgeNetPreferences;
import forge.gui.GuiBase;
import forge.gui.interfaces.IGuiGame;
import forge.gui.interfaces.ILobbyView;
import forge.gui.util.SOptionPane;
import forge.interfaces.ILobbyListener;
import forge.interfaces.IUpdateable;
import forge.localinstance.properties.ForgeConstants;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.player.GamePlayerUtil;
import forge.util.Localizer;
import forge.util.URLValidator;
import org.apache.commons.lang3.StringUtils;

import static forge.util.URLValidator.parseURL;

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
        final int port = FModel.getNetPreferences().getPrefInt(ForgeNetPreferences.FNetPref.NET_PORT);
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
                    if (server.handleCommand(message.getMessage())) {
                        return;
                    }
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
        String internalAddress = FServerManager.getLocalAddress();
        String externalAddress = FServerManager.getExternalAddress();
        String internalUrl = internalAddress + ":" + FModel.getNetPreferences().getPrefInt(ForgeNetPreferences.FNetPref.NET_PORT);
        String externalUrl = null;
        if (externalAddress != null) {
            externalUrl = externalAddress + ":" + FModel.getNetPreferences().getPrefInt(ForgeNetPreferences.FNetPref.NET_PORT);
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
        String hostname;
        int port;

        URLValidator.HostPort hostPort = parseURL(url);
        if(hostPort == null) {
            return new ChatMessage(null, ForgeConstants.INVALID_HOST_COMMAND);
        }

        hostname = hostPort.host();
        port = hostPort.port();
        if(port == -1) port = ForgeConstants.DEFAULT_SERVER_CONNECTION_PORT;


        final FGameClient client = new FGameClient(FModel.getPreferences().getPref(FPref.PLAYER_NAME), "0", gui, hostname, port);
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



        try {
            client.connect();
        }
        catch (Exception ex) {
            // Return error with details for GUI display
            String errorDetail = getConnectionErrorMessage(ex, hostname, port);
            return new ChatMessage(null, ForgeConstants.CONN_ERROR_PREFIX + errorDetail);
        }

        return new ChatMessage(null, Localizer.getInstance().getMessage("lblConnectedIPPort", hostname, String.valueOf(port)));
    }

    /**
     * Generate a user-friendly error message for connection failures.
     */
    private static String getConnectionErrorMessage(Exception ex, String hostname, int port) {
        Localizer localizer = Localizer.getInstance();
        StringBuilder sb = new StringBuilder();

        // Get the root cause for better error messages
        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
        String causeName = cause.getClass().getSimpleName();

        sb.append(localizer.getMessage("lblConnectionFailedTo", hostname, String.valueOf(port)));
        sb.append("\n\n");

        // Provide specific messages for common error types
        if (causeName.contains("ConnectException") || causeName.contains("ConnectionRefused")) {
            sb.append(localizer.getMessage("lblConnectionRefused"));
        } else if (causeName.contains("UnknownHost")) {
            sb.append(localizer.getMessage("lblUnknownHost"));
        } else if (causeName.contains("Timeout") || causeName.contains("TimedOut")) {
            sb.append(localizer.getMessage("lblConnectionTimeout"));
        } else if (causeName.contains("NoRouteToHost")) {
            sb.append(localizer.getMessage("lblNoRouteToHost"));
        } else {
            // Generic error with the exception message
            String msg = cause.getMessage();
            if (msg != null && !msg.isEmpty()) {
                sb.append(msg);
            } else {
                sb.append(causeName);
            }
        }

        return sb.toString();
    }
}
