package forge.screens.home.online;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JMenu;

import forge.GuiBase;
import forge.Singletons;
import forge.UiCommand;
import forge.gui.FNetOverlay;
import forge.gui.framework.FScreen;
import forge.gui.framework.ICDoc;
import forge.interfaces.IGuiGame;
import forge.interfaces.ILobbyListener;
import forge.interfaces.IPlayerChangeListener;
import forge.interfaces.IUpdateable;
import forge.match.GameLobby.GameLobbyData;
import forge.menus.IMenuProvider;
import forge.menus.MenuUtil;
import forge.model.FModel;
import forge.net.ClientGameLobby;
import forge.net.FGameClient;
import forge.net.FServerManager;
import forge.net.ServerGameLobby;
import forge.net.game.IRemote;
import forge.net.game.IdentifiableNetEvent;
import forge.net.game.MessageEvent;
import forge.net.game.NetEvent;
import forge.net.game.UpdateLobbyPlayerEvent;
import forge.properties.ForgePreferences.FPref;
import forge.screens.home.VLobby;
import forge.screens.home.sanctioned.ConstructedGameMenu;

public enum CSubmenuOnlineLobby implements ICDoc, IMenuProvider {
    SINGLETON_INSTANCE;

    final void host(final int portNumber) {
        final FServerManager server = FServerManager.getInstance();
        final ServerGameLobby lobby = new ServerGameLobby();
        final VLobby view = VOnlineLobby.SINGLETON_INSTANCE.setLobby(lobby);

        server.startServer(portNumber);
        server.setLobby(lobby);

        FNetOverlay.SINGLETON_INSTANCE.showUp("Hosting game");
        lobby.setListener(new IUpdateable() {
            @Override public final void update() {
                view.update();
                server.updateLobbyState();
            }
        });
        view.setPlayerChangeListener(new IPlayerChangeListener() {
            @Override public final void update(final int index, final UpdateLobbyPlayerEvent event) {
                server.updateSlot(index, event);
                server.updateLobbyState();
            }
        });

        server.setLobbyListener(new ILobbyListener() {
            @Override public final void update(final GameLobbyData state, final int slot) {
                // NO-OP, lobby connected directly
            }
            @Override public final void message(final String source, final String message) {
                FNetOverlay.SINGLETON_INSTANCE.addMessage(source, message);
            }
        });
        FNetOverlay.SINGLETON_INSTANCE.setGameClient(new IRemote() {
            @Override public final void send(final NetEvent event) {
                if (event instanceof MessageEvent) {
                    final MessageEvent message = (MessageEvent) event;
                    FNetOverlay.SINGLETON_INSTANCE.addMessage(message.getSource(), message.getMessage());
                    server.broadcast(event);
                }
            }
            @Override public final Object sendAndWait(final IdentifiableNetEvent event) {
                send(event);
                return null;
            }
        });

        view.update();

        Singletons.getControl().setCurrentScreen(FScreen.ONLINE_LOBBY);
        FNetOverlay.SINGLETON_INSTANCE.showUp(String.format("Hosting on port %d", portNumber));
    }

    final void join(final String hostname, final int port) {
        final IGuiGame gui = GuiBase.getInterface().getNewGuiGame();
        final FGameClient client = new FGameClient(FModel.getPreferences().getPref(FPref.PLAYER_NAME), "0", gui);
        FNetOverlay.SINGLETON_INSTANCE.setGameClient(client);
        final ClientGameLobby lobby = new ClientGameLobby(); 
        final VLobby view =  VOnlineLobby.SINGLETON_INSTANCE.setLobby(lobby);
        lobby.setListener(view);
        client.addLobbyListener(new ILobbyListener() {
            @Override public final void message(final String source, final String message) {
                FNetOverlay.SINGLETON_INSTANCE.addMessage(source, message);
            }
            @Override public final void update(final GameLobbyData state, final int slot) {
                lobby.setLocalPlayer(slot);
                lobby.setData(state);
            }
        });
        view.setPlayerChangeListener(new IPlayerChangeListener() {
            @Override public final void update(final int index, final UpdateLobbyPlayerEvent event) {
                client.send(event);
            }
        });
        client.connect(hostname, port);

        Singletons.getControl().setCurrentScreen(FScreen.ONLINE_LOBBY);
        FNetOverlay.SINGLETON_INSTANCE.showUp(String.format("Connected to %s:%s", hostname, port));
    }

    @Override
    public void register() {
    }

    /* (non-Javadoc)
     * @see forge.gui.home.ICSubmenu#initialize()
     */
    @Override
    public void update() {
        MenuUtil.setMenuProvider(this);
    }

    /* (non-Javadoc)
     * @see forge.gui.home.ICSubmenu#initialize()
     */
    @Override
    public void initialize() {
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#getCommandOnSelect()
     */
    @Override
    public UiCommand getCommandOnSelect() {
        return null;
    }

    /* (non-Javadoc)
     * @see forge.gui.menubar.IMenuProvider#getMenus()
     */
    @Override
    public List<JMenu> getMenus() {
        List<JMenu> menus = new ArrayList<JMenu>();
        menus.add(ConstructedGameMenu.getMenu());
        return menus;
    }

}
