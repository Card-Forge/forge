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
import javax.swing.SwingConstants;
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
        lobby = lobbyView.getController();
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
            showServerAddressesDialog();
        });
    }

    static void showServerAddressesDialog() {
        final ForgeNetPreferences netPrefs = FModel.getNetPreferences();
        final int port = netPrefs.getPrefInt(ForgeNetPreferences.FNetPref.NET_PORT);
        final LinkedHashMap<String, String> addresses = FServerManager.getAllLocalAddresses();
        final String externalAddress = FServerManager.getExternalAddress();
        final Localizer localizer = Localizer.getInstance();

        // Collect rows in display order; we auto-copy and star whichever row matches
        // the last-copied URL, falling back to the first row.
        final List<String> orderedLabels = new ArrayList<>();
        final List<String> orderedUrls = new ArrayList<>();
        if (externalAddress != null) {
            orderedLabels.add("External (WAN)");
            orderedUrls.add(externalAddress + ":" + port);
        }
        for (final Map.Entry<String, String> entry : addresses.entrySet()) {
            orderedLabels.add(entry.getKey());
            orderedUrls.add(entry.getValue() + ":" + port);
        }

        // If the remembered URL is present in the current list, auto-copy and star it.
        // Otherwise fall back to the first entry (external if present, else first local
        // interface) — matches the old copyHostedServerUrl default. Do NOT overwrite
        // the remembered value on fallback, so a later reconnect to the original
        // network restores the preference.
        final String rememberedUrl = netPrefs.getPref(ForgeNetPreferences.FNetPref.NET_LAST_COPIED_URL);
        int starIndex = orderedUrls.indexOf(rememberedUrl);
        if (starIndex < 0) {
            starIndex = orderedUrls.isEmpty() ? -1 : 0;
        }
        if (starIndex >= 0) {
            copyToClipboard(orderedUrls.get(starIndex));
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
        for (int i = 0; i < orderedUrls.size(); i++) {
            final String url = orderedUrls.get(i);
            final String label = (i == starIndex) ? orderedLabels.get(i) + " \u2605" : orderedLabels.get(i);
            panel.add(new FLabel.Builder().text(label).fontSize(12).fontAlign(SwingConstants.LEFT).build(), "growx");
            panel.add(new FLabel.Builder().text(url).fontSize(12).fontAlign(SwingConstants.LEFT).build(), "growx");
            final FButton btnCopy = new FButton(localizer.getMessage("lblCopy"));
            btnCopy.setFont(FSkin.getFont(11));
            btnCopy.addActionListener(e -> {
                copyToClipboard(url);
                netPrefs.setPref(ForgeNetPreferences.FNetPref.NET_LAST_COPIED_URL, url);
                netPrefs.save();
                holder[0].setVisible(false);
            });
            panel.add(btnCopy, "w 70!, h 24!");
        }

        if (starIndex >= 0) {
            panel.add(new FLabel.Builder()
                    .text(localizer.getMessage("lblServerUrlCopiedToClipboard", orderedUrls.get(starIndex)))
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
}
