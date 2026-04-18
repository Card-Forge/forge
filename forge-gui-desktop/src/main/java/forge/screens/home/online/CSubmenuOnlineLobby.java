package forge.screens.home.online;

import java.awt.Font;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.net.BindException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.google.common.collect.ImmutableList;
import net.miginfocom.swing.MigLayout;

import forge.gamemodes.net.ChatMessage;
import forge.gamemodes.net.NetConnectUtil;
import forge.gamemodes.net.server.FServerManager;
import forge.gui.FNetOverlay;
import forge.gui.FThreads;
import forge.gui.SOverlayUtils;
import forge.gui.error.BugReporter;
import forge.gui.framework.EDocID;
import forge.gui.framework.ICDoc;
import forge.gui.util.SOptionPane;
import forge.localinstance.properties.ForgeConstants;
import forge.localinstance.properties.ForgeNetPreferences;
import forge.menus.IMenuProvider;
import forge.menus.MenuUtil;
import forge.model.FModel;
import forge.screens.home.CHomeUI;
import forge.screens.home.CLobby;
import forge.screens.home.VLobby;
import forge.screens.home.sanctioned.ConstructedGameMenu;
import forge.toolbox.FButton;
import forge.toolbox.FLabel;
import forge.toolbox.FOptionPane;
import forge.toolbox.FSkin;
import forge.util.Localizer;

public enum CSubmenuOnlineLobby implements ICDoc, IMenuProvider {
    SINGLETON_INSTANCE;

    private CLobby lobby;

    void setLobby(final VLobby lobbyView) {
        lobby = new CLobby(lobbyView);
        initialize();
    }

    void hostGame() {
        NetConnectUtil.ensurePlayerName();
        FThreads.invokeInBackgroundThread(() -> {
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
        });
    }

    void joinGame() {
        final String url = NetConnectUtil.getJoinServerUrl();
        if (url == null) { return; }

        FThreads.invokeInBackgroundThread(() -> join(url));
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
            showServerAddressesDialog();
        });
    }

    private void showServerAddressesDialog() {
        final int port = FModel.getNetPreferences().getPrefInt(ForgeNetPreferences.FNetPref.NET_PORT);
        final LinkedHashMap<String, String> addresses = FServerManager.getAllLocalAddresses();
        final String externalAddress = FServerManager.getExternalAddress();
        final Localizer localizer = Localizer.getInstance();

        final JPanel panel = new JPanel(new MigLayout("insets 0, gap 4 6, wrap 3", "[grow][grow][pref]"));
        panel.setOpaque(false);

        panel.add(new FLabel.Builder().text(localizer.getMessage("lblInterface")).fontStyle(Font.BOLD).fontSize(12).build(), "growx");
        panel.add(new FLabel.Builder().text(localizer.getMessage("lblAddress")).fontStyle(Font.BOLD).fontSize(12).build(), "growx");
        panel.add(new FLabel.Builder().text("").build());

        if (externalAddress != null) {
            final String externalUrl = externalAddress + ":" + port;
            panel.add(new FLabel.Builder().text("External (WAN)").fontSize(12).build(), "growx");
            panel.add(new FLabel.Builder().text(externalUrl).fontSize(12).build(), "growx");
            final FButton btnCopy = new FButton(localizer.getMessage("lblCopy"));
            btnCopy.setFont(FSkin.getFont(11));
            btnCopy.addActionListener(e -> copyToClipboard(externalUrl));
            panel.add(btnCopy, "w 70!, h 24!");
        }

        boolean first = true;
        for (final Map.Entry<String, String> entry : addresses.entrySet()) {
            final String url = entry.getValue() + ":" + port;
            final String label = first ? entry.getKey() + " \u2605" : entry.getKey();
            first = false;

            panel.add(new FLabel.Builder().text(label).fontSize(12).build(), "growx");
            panel.add(new FLabel.Builder().text(url).fontSize(12).build(), "growx");
            final FButton btnCopy = new FButton(localizer.getMessage("lblCopy"));
            btnCopy.setFont(FSkin.getFont(11));
            btnCopy.addActionListener(e -> copyToClipboard(url));
            panel.add(btnCopy, "w 70!, h 24!");
        }

        FOptionPane.showOptionDialog(
                localizer.getMessage("lblChooseAddressToCopy"),
                localizer.getMessage("lblServerURL"),
                FOptionPane.INFORMATION_ICON,
                panel,
                ImmutableList.of(localizer.getMessage("lblOK")),
                0);
    }

    private static void copyToClipboard(final String text) {
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
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
