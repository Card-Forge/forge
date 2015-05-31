package forge.screens.home.online;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JMenu;
import javax.swing.SwingUtilities;

import org.apache.commons.lang3.StringUtils;

import forge.FThreads;
import forge.GuiBase;
import forge.assets.FSkinProp;
import forge.gui.FNetOverlay;
import forge.gui.SOverlayUtils;
import forge.gui.framework.ICDoc;
import forge.interfaces.IGuiGame;
import forge.interfaces.ILobbyListener;
import forge.interfaces.IPlayerChangeListener;
import forge.interfaces.IUpdateable;
import forge.match.GameLobby.GameLobbyData;
import forge.menus.IMenuProvider;
import forge.menus.MenuUtil;
import forge.model.FModel;
import forge.net.IRemote;
import forge.net.client.ClientGameLobby;
import forge.net.client.FGameClient;
import forge.net.event.IdentifiableNetEvent;
import forge.net.event.MessageEvent;
import forge.net.event.NetEvent;
import forge.net.event.UpdateLobbyPlayerEvent;
import forge.net.server.FServerManager;
import forge.net.server.ServerGameLobby;
import forge.player.GamePlayerUtil;
import forge.properties.ForgePreferences.FPref;
import forge.properties.ForgeProfileProperties;
import forge.screens.home.CLobby;
import forge.screens.home.VLobby;
import forge.screens.home.sanctioned.ConstructedGameMenu;
import forge.util.gui.SOptionPane;

public enum CSubmenuOnlineLobby implements ICDoc, IMenuProvider {
    SINGLETON_INSTANCE;

    private CLobby lobby;

    void setLobby(final VLobby lobbyView) {
        lobby = new CLobby(lobbyView);
        initialize();
    }

    void connectToServer() {
        final String url = SOptionPane.showInputDialog("Enter URL of server to join. Leave blank to host your own server.", "Connect to Server");
        if (url == null) { return; }

        //prompt user for player one name if needed
        if (StringUtils.isBlank(FModel.getPreferences().getPref(FPref.PLAYER_NAME))) {
            GamePlayerUtil.setPlayerName();
        }

        FThreads.invokeInBackgroundThread(new Runnable() {
            @Override
            public void run() {
                if (url.length() > 0) {
                    join(url);
                }
                else {
                    host();
                }
            }
        });
    }

    private void host() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                SOverlayUtils.startGameOverlay("Starting server...");
                SOverlayUtils.showOverlay();
            }
        });

        final int port = ForgeProfileProperties.getServerPort();
        final FServerManager server = FServerManager.getInstance();
        final ServerGameLobby lobby = new ServerGameLobby();
        final VLobby view = VSubmenuOnlineLobby.SINGLETON_INSTANCE.setLobby(lobby);

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
                FNetOverlay.SINGLETON_INSTANCE.addMessage(source, message);
            }
            @Override
            public final void close() {
                // NO-OP, server can't receive close message
            }
        });
        FNetOverlay.SINGLETON_INSTANCE.setGameClient(new IRemote() {
            @Override
            public final void send(final NetEvent event) {
                if (event instanceof MessageEvent) {
                    final MessageEvent message = (MessageEvent) event;
                    FNetOverlay.SINGLETON_INSTANCE.addMessage(message.getSource(), message.getMessage());
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

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                SOverlayUtils.hideOverlay();
                FNetOverlay.SINGLETON_INSTANCE.showUp(String.format("Hosting on port %d", port));
                VSubmenuOnlineLobby.SINGLETON_INSTANCE.populate();
            }
        });
    }

    private void join(final String url) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                SOverlayUtils.startGameOverlay("Connecting to server...");
                SOverlayUtils.showOverlay();
            }
        });

        final IGuiGame gui = GuiBase.getInterface().getNewGuiGame();
        final FGameClient client = new FGameClient(FModel.getPreferences().getPref(FPref.PLAYER_NAME), "0", gui);
        VSubmenuOnlineLobby.SINGLETON_INSTANCE.setClient(client);
        FNetOverlay.SINGLETON_INSTANCE.setGameClient(client);
        final ClientGameLobby lobby = new ClientGameLobby();
        final VLobby view =  VSubmenuOnlineLobby.SINGLETON_INSTANCE.setLobby(lobby);
        lobby.setListener(view);
        client.addLobbyListener(new ILobbyListener() {
            @Override
            public final void message(final String source, final String message) {
                FNetOverlay.SINGLETON_INSTANCE.addMessage(source, message);
            }
            @Override
            public final void update(final GameLobbyData state, final int slot) {
                lobby.setLocalPlayer(slot);
                lobby.setData(state);
            }
            @Override
            public final void close() {
                SOptionPane.showMessageDialog("Connection to the host was interrupted.", "Error", FSkinProp.ICO_WARNING);
                VSubmenuOnlineLobby.SINGLETON_INSTANCE.setClient(null);
            }
        });
        view.setPlayerChangeListener(new IPlayerChangeListener() {
            @Override
            public final void update(final int index, final UpdateLobbyPlayerEvent event) {
                client.send(event);
            }
        });

        final String hostname;
        int port0 = ForgeProfileProperties.getServerPort();

        //see if port specified in URL
        int index = url.indexOf(':');
        if (index >= 0) {
            hostname = url.substring(0, index);
            String portStr = url.substring(index + 1);
            try {
                port0 = Integer.parseInt(portStr);
            }
            catch (Exception ex) {}
        }
        else {
            hostname = url;
        }
        final int port = port0;

        client.connect(hostname, port);

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                SOverlayUtils.hideOverlay();
                FNetOverlay.SINGLETON_INSTANCE.showUp(String.format("Connected to %s:%d", hostname, port));
                VSubmenuOnlineLobby.SINGLETON_INSTANCE.populate();
            }
        });
    }

    @Override
    public void register() {
    }

    /* (non-Javadoc)
     * @see forge.gui.home.ICSubmenu#update()
     */
    @Override
    public void update() {
        MenuUtil.setMenuProvider(this);
        if (lobby != null) {
            lobby.update();
        }
    }

    /* (non-Javadoc)
     * @see forge.gui.home.ICSubmenu#initialize()
     */
    @Override
    public void initialize() {
        if (lobby != null) {
            lobby.initialize();
        }
    }

    /* (non-Javadoc)
     * @see forge.gui.menubar.IMenuProvider#getMenus()
     */
    @Override
    public List<JMenu> getMenus() {
        final List<JMenu> menus = new ArrayList<JMenu>();
        menus.add(ConstructedGameMenu.getMenu());
        return menus;
    }
}
