package forge.screens.home.online;

import java.awt.Font;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.net.BindException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import com.google.common.collect.ImmutableList;
import net.miginfocom.swing.MigLayout;

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
        lobby = lobbyView.getController();
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

    /**
     * Join a game at the given URL without prompting for input.
     * Used by the lobby browser to join with a pre-resolved host:port.
     */
    void joinGameWithUrl(final String url) {
        if (url == null || url.isEmpty()) { return; }
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
            final boolean lobbyEnabled = FModel.getNetPreferences().getPrefBoolean(ForgeNetPreferences.FNetPref.LOBBY_ENABLED);
            if (lobbyEnabled) {
                // Show chat overlay without port message; skip UPnP and server addresses dialog
                FNetOverlay.SINGLETON_INSTANCE.show();
            } else {
                FNetOverlay.SINGLETON_INSTANCE.show(result);
                showServerAddressesDialog();
            }
            if (CHomeUI.SINGLETON_INSTANCE.getCurrentDocID() == EDocID.HOME_NETWORK) {
                VSubmenuOnlineLobby.SINGLETON_INSTANCE.populate();
            }
        });
    }

    static void showServerAddressesDialog() {
        if (FThreads.isGuiThread()) {
            // Collecting the addresses queries the external IP over the network, so it can't run on the EDT.
            FThreads.invokeInBackgroundThread(CSubmenuOnlineLobby::showServerAddressesDialog);
            return;
        }
        final NetConnectUtil.ServerAddressList addresses = NetConnectUtil.collectHostedServerAddresses();
        SwingUtilities.invokeLater(() -> showServerAddressesDialog(addresses));
    }

    private static void showServerAddressesDialog(final NetConnectUtil.ServerAddressList addresses) {
        final Localizer localizer = Localizer.getInstance();

        if (addresses.starIndex >= 0) {
            copyToClipboard(addresses.urls.get(addresses.starIndex));
        }

        final JPanel panel = new JPanel(new MigLayout("insets 0, gap 4 6, wrap 3", "[pref]30[pref]30[pref]"));
        panel.setOpaque(false);

        panel.add(new FLabel.Builder()
                .text(localizer.getMessage("lblChooseAddressToCopy"))
                .fontSize(12).fontAlign(SwingConstants.LEFT).build(),
                "span 3, growx, gapbottom 10");

        panel.add(new FLabel.Builder().text(localizer.getMessage("lblInterface")).fontStyle(Font.BOLD).fontSize(12).fontAlign(SwingConstants.LEFT).build(), "growx");
        panel.add(new FLabel.Builder().text(localizer.getMessage("lblAddress")).fontStyle(Font.BOLD).fontSize(12).fontAlign(SwingConstants.LEFT).build(), "growx");
        panel.add(new FLabel.Builder().text("").build());

        final FOptionPane[] holder = new FOptionPane[1];
        for (int i = 0; i < addresses.urls.size(); i++) {
            final String url = addresses.urls.get(i);
            final String label = (i == addresses.starIndex) ? addresses.labels.get(i) + " \u2605" : addresses.labels.get(i);
            panel.add(new FLabel.Builder().text(label).fontSize(12).fontAlign(SwingConstants.LEFT).build(), "growx");
            panel.add(new FLabel.Builder().text(url).fontSize(12).fontAlign(SwingConstants.LEFT).build(), "growx");
            final FButton btnCopy = new FButton(localizer.getMessage("lblCopy"));
            btnCopy.setFont(FSkin.getFont(11));
            btnCopy.addActionListener(e -> {
                copyToClipboard(url);
                NetConnectUtil.rememberCopiedServerUrl(url);
                holder[0].setVisible(false);
            });
            panel.add(btnCopy, "w 70!, h 24!");
        }

        if (addresses.starIndex >= 0) {
            panel.add(new FLabel.Builder()
                    .text(localizer.getMessage("lblServerUrlCopiedToClipboard", addresses.urls.get(addresses.starIndex)))
                    .fontSize(11).fontStyle(Font.ITALIC).fontAlign(SwingConstants.LEFT).build(),
                    "span 3, growx, gaptop 10");
        }

        // Pass null as the prompt message so dialog width is driven by the panel's
        // actual content width rather than the much-wider localised instruction line.
        holder[0] = new FOptionPane(
                null,
                localizer.getMessage("lblServerURL"),
                FOptionPane.INFORMATION_ICON,
                panel,
                ImmutableList.of(localizer.getMessage("lblOK")),
                0);
        holder[0].setVisible(true);
        holder[0].dispose();
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

    void showLobbySettingsDialog() {
        final ForgeNetPreferences netPrefs = FModel.getNetPreferences();
        final Localizer localizer = Localizer.getInstance();

        final String currentUrl = netPrefs.getPref(ForgeNetPreferences.FNetPref.LOBBY_SERVER_URL);
        final boolean currentlyEnabled = netPrefs.getPrefBoolean(ForgeNetPreferences.FNetPref.LOBBY_ENABLED);

        final JPanel panel = new JPanel(new MigLayout("insets 20, gap 10, wrap 2", "[]10[grow,fill,400!]"));
        panel.setOpaque(false);

        final FLabel lblUrl = new FLabel.Builder()
                .text(localizer.getMessageorUseDefault("lblLobbyServerURL", "Lobby Server URL:"))
                .fontSize(14).build();
        final javax.swing.JTextField txtUrl = new javax.swing.JTextField(currentUrl);
        txtUrl.setFont(FSkin.getRelativeFixedFont(14));
        txtUrl.setPreferredSize(new java.awt.Dimension(400, 30));

        final FLabel lblEnabled = new FLabel.Builder()
                .text(localizer.getMessageorUseDefault("lblLobbyEnableBrowser", "Enable Lobby Browser:"))
                .fontSize(14).build();
        final javax.swing.JCheckBox chkEnabled = new javax.swing.JCheckBox();
        chkEnabled.setSelected(currentlyEnabled);
        chkEnabled.setOpaque(false);
        chkEnabled.setFont(FSkin.getRelativeFixedFont(14));

        panel.add(lblUrl, "");
        panel.add(txtUrl, "growx, wrap");
        panel.add(lblEnabled, "");
        panel.add(chkEnabled, "wrap");

        final List<String> options = new ArrayList<>();
        options.add(localizer.getMessage("lblOK"));
        options.add(localizer.getMessage("lblCancel"));

        final int result = FOptionPane.showOptionDialog(
                localizer.getMessageorUseDefault("lblLobbySettings", "Lobby Settings"),
                localizer.getMessageorUseDefault("lblLobbySettings", "Lobby Settings"),
                null, panel, options, 0);

        if (result == 0) {
            final String newUrl = txtUrl.getText().trim();
            final boolean newEnabled = chkEnabled.isSelected();
            netPrefs.setPref(ForgeNetPreferences.FNetPref.LOBBY_SERVER_URL, newUrl);
            netPrefs.setPref(ForgeNetPreferences.FNetPref.LOBBY_ENABLED, String.valueOf(newEnabled));
            netPrefs.save();
            // Refresh the view to show/hide lobby browser
            if (CHomeUI.SINGLETON_INSTANCE.getCurrentDocID() == EDocID.HOME_NETWORK) {
                VSubmenuOnlineLobby.SINGLETON_INSTANCE.populate();
            }
        }
    }
}
