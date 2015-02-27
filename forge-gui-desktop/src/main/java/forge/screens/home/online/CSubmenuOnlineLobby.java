package forge.screens.home.online;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JMenu;

import forge.GuiBase;
import forge.Singletons;
import forge.UiCommand;
import forge.game.GameRules;
import forge.game.GameType;
import forge.gui.FNetOverlay;
import forge.gui.framework.FScreen;
import forge.gui.framework.ICDoc;
import forge.interfaces.IPlayerChangeListener;
import forge.menus.IMenuProvider;
import forge.menus.MenuUtil;
import forge.model.FModel;
import forge.net.FGameClient;
import forge.net.FServerManager;
import forge.net.game.LobbyState;
import forge.net.game.LobbyState.LobbyPlayerData;
import forge.net.game.LoginEvent;
import forge.net.game.client.ILobbyListener;
import forge.properties.ForgePreferences.FPref;
import forge.screens.home.VLobby;
import forge.screens.home.VLobby.LobbyType;
import forge.screens.home.sanctioned.ConstructedGameMenu;

public enum CSubmenuOnlineLobby implements ICDoc, IMenuProvider {
    SINGLETON_INSTANCE;

    final void host(final int portNumber) {
        final VLobby lobby = VOnlineLobby.SINGLETON_INSTANCE.setLobby(LobbyType.SERVER);

        FServerManager.getInstance().startServer(portNumber);
        FServerManager.getInstance().setLobby(lobby);
        FServerManager.getInstance().hostGame(new GameRules(GameType.Constructed));

        FNetOverlay.SINGLETON_INSTANCE.showUp("Hosting game");
        lobby.setPlayerChangeListener(new IPlayerChangeListener() {
            @Override public final void update(final LobbyPlayerData data) {
                FServerManager.getInstance().updateLobbyState();
            }
        });

        final FGameClient client = new FGameClient(FModel.getPreferences().getPref(FPref.PLAYER_NAME), "0", GuiBase.getInterface().getNewGuiGame());
        FNetOverlay.SINGLETON_INSTANCE.setGameClient(client);
        client.addLobbyListener(new ILobbyListener() {
            @Override public final void update(final LobbyState state) {
                lobby.setState(state);
            }
            @Override public final void message(final String source, final String message) {
                FNetOverlay.SINGLETON_INSTANCE.addMessage(source, message);
            }
        });
        client.connect("localhost", portNumber);

        Singletons.getControl().setCurrentScreen(FScreen.ONLINE_LOBBY);
        FNetOverlay.SINGLETON_INSTANCE.showUp(String.format("Hosting on port %d", portNumber));
    }

    final void join(final String hostname, final int port) {
        final FGameClient client = new FGameClient(FModel.getPreferences().getPref(FPref.PLAYER_NAME), "0", GuiBase.getInterface().getNewGuiGame());
        FNetOverlay.SINGLETON_INSTANCE.setGameClient(client);
        final VLobby lobby =  VOnlineLobby.SINGLETON_INSTANCE.setLobby(LobbyType.CLIENT);
        client.addLobbyListener(new ILobbyListener() {
            @Override public final void update(final LobbyState state) {
                lobby.setState(state);
            }
            @Override public final void message(final String source, final String message) {
                FNetOverlay.SINGLETON_INSTANCE.addMessage(source, message);
            }
        });
        lobby.setPlayerChangeListener(new IPlayerChangeListener() {
            @Override public final void update(final LobbyPlayerData data) {
                client.send(new LoginEvent(data.getName()));
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
