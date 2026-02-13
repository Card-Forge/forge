package forge.screens.home.online;

import java.net.BindException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.swing.JMenu;
import javax.swing.SwingUtilities;

import forge.gamemodes.net.ChatMessage;
import forge.gamemodes.net.NetConnectUtil;
import forge.gui.FNetOverlay;
import forge.gui.FThreads;
import forge.gui.SOverlayUtils;
import forge.gui.error.BugReporter;
import forge.gui.framework.EDocID;
import forge.gui.framework.ICDoc;
import forge.gui.util.SOptionPane;
import forge.localinstance.properties.ForgeConstants;
import forge.menus.IMenuProvider;
import forge.menus.MenuUtil;
import forge.screens.home.CHomeUI;
import forge.screens.home.CLobby;
import forge.screens.home.VLobby;
import forge.screens.home.sanctioned.ConstructedGameMenu;
import forge.toolbox.FOptionPane;
import forge.util.Localizer;

public enum CSubmenuOnlineLobby implements ICDoc, IMenuProvider {
    SINGLETON_INSTANCE;

    private CLobby lobby;

    void setLobby(final VLobby lobbyView) {
        lobby = new CLobby(lobbyView);
        initialize();
    }

    void connectToServer() {
        final String url = NetConnectUtil.getServerUrl();
        if (url == null) { return; }

        FThreads.invokeInBackgroundThread(() -> {
            if (!url.isEmpty()) {
                join(url);
            }
            else {
                try {
                    host();
                } catch (Exception ex) {
                    // IntelliJ swears that BindException isn't thrown in this try block, but it is!
                    if (ex.getClass() == BindException.class) {
                        SOptionPane.showErrorDialog(Localizer.getInstance().getMessage("lblUnableStartServerPortAlreadyUse"));
                        SOverlayUtils.hideOverlay();
                    } else {
                        BugReporter.reportException(ex);
                    }
                }
            }
        });
    }

    private void host() {
        SwingUtilities.invokeLater(() -> {
            SOverlayUtils.startGameOverlay(Localizer.getInstance().getMessage("lblStartingServer"));
            SOverlayUtils.showOverlay();
        });

        final ChatMessage result = NetConnectUtil.host(VSubmenuOnlineLobby.SINGLETON_INSTANCE, FNetOverlay.SINGLETON_INSTANCE);

        SwingUtilities.invokeLater(() -> {
            SOverlayUtils.hideOverlay();
            FNetOverlay.SINGLETON_INSTANCE.show(result);
            if (CHomeUI.SINGLETON_INSTANCE.getCurrentDocID() == EDocID.HOME_NETWORK) {
                VSubmenuOnlineLobby.SINGLETON_INSTANCE.populate();
            }
            NetConnectUtil.copyHostedServerUrl();
        });
    }

    private void join(final String url) {
        SwingUtilities.invokeLater(() -> {
            SOverlayUtils.startGameOverlay(Localizer.getInstance().getMessage("lblConnectingToServer"));
            SOverlayUtils.showOverlay();
        });

        final ChatMessage result = NetConnectUtil.join(url, VSubmenuOnlineLobby.SINGLETON_INSTANCE, FNetOverlay.SINGLETON_INSTANCE);
        String message = result.getMessage();
        if(Objects.equals(message, ForgeConstants.CLOSE_CONN_COMMAND)) {
            FOptionPane.showErrorDialog(Localizer.getInstance().getMessage("UnableConnectToServer", url));
            SOverlayUtils.hideOverlay();
        } else if (message != null && message.startsWith(ForgeConstants.CONN_ERROR_PREFIX)) {
            // Show detailed connection error
            String errorDetail = message.substring(ForgeConstants.CONN_ERROR_PREFIX.length());
            FOptionPane.showErrorDialog(errorDetail, Localizer.getInstance().getMessage("lblConnectionError"));
            SOverlayUtils.hideOverlay();
        } else if (Objects.equals(message, ForgeConstants.INVALID_HOST_COMMAND)) {
            FOptionPane.showErrorDialog(Localizer.getInstance().getMessage("lblDetectedInvalidHostAddress", url));
            SOverlayUtils.hideOverlay();
        } else {
            SwingUtilities.invokeLater(() -> {
                SOverlayUtils.hideOverlay();
                if (result instanceof ChatMessage) {
                    FNetOverlay.SINGLETON_INSTANCE.show(result);
                    if (CHomeUI.SINGLETON_INSTANCE.getCurrentDocID() == EDocID.HOME_NETWORK) {
                        VSubmenuOnlineLobby.SINGLETON_INSTANCE.populate();
                    }
                }
            });
        }
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
        final List<JMenu> menus = new ArrayList<>();
        menus.add(ConstructedGameMenu.getMenu());
        return menus;
    }
}
