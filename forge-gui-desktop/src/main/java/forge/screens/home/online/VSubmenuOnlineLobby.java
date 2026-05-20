package forge.screens.home.online;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import forge.deckchooser.FDeckChooser;
import forge.gamemodes.match.GameLobby;
import forge.gamemodes.net.IOnlineLobby;
import forge.gamemodes.net.client.FGameClient;
import forge.gamemodes.net.server.FServerManager;
import forge.gui.FNetOverlay;
import forge.gui.framework.DragCell;
import forge.gui.framework.DragTab;
import forge.gui.framework.EDocID;
import forge.gui.framework.FScreen;
import forge.gui.framework.IVTopLevelUI;
import forge.gui.interfaces.ILobbyView;
import forge.gui.util.SOptionPane;
import forge.localinstance.properties.ForgeConstants;
import forge.screens.home.EMenuGroup;
import forge.screens.home.IVSubmenu;
import forge.screens.home.StopButton;
import forge.screens.home.VHomeUI;
import forge.screens.home.VLobby;
import forge.toolbox.FButton;
import forge.toolbox.FLabel;
import forge.toolbox.FSkin;
import forge.util.Localizer;
import net.miginfocom.swing.MigLayout;

public enum VSubmenuOnlineLobby implements IVSubmenu<CSubmenuOnlineLobby>, IOnlineLobby, IVTopLevelUI {
    SINGLETON_INSTANCE;

    private DragCell parentCell;
    private final DragTab tab = new DragTab(Localizer.getInstance().getMessage("lblLobby"));
    private VLobby lobby;
    private FGameClient client;

    private final JPanel pnlTitle = new JPanel(new MigLayout());
    private final StopButton btnStop  = new StopButton();

    VSubmenuOnlineLobby() {
        btnStop.addActionListener(arg0 -> {
            // do the STOP needful here
            Runnable stopGame = VSubmenuOnlineLobby.this::reset;
            if (stopGame != null) {
                stopGame.run();
            }
        });
    }

    public ILobbyView setLobby(final GameLobby lobby) {
        this.lobby = new VLobby(lobby);
        getLayoutControl().setLobby(this.lobby);
        return this.lobby;
    }

    public void reset() {
        if (onClosing(null)) {
            this.client = null;
            this.lobby = null;
            populate();
        }
    }

    public void setClient(final FGameClient client) {
        this.client = client;
    }

    @Override
    public void populate() {
        final JPanel container = VHomeUI.SINGLETON_INSTANCE.getPnlDisplay();

        container.removeAll();

        if (lobby == null) {
            final Localizer localizer = Localizer.getInstance();

            final JPanel infoBox = new JPanel(new MigLayout("insets 30 40 20 40, gap 0, wrap 1, ax center"));
            infoBox.setBackground(new Color(40, 40, 40));
            infoBox.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(100, 100, 100), 1),
                    BorderFactory.createEmptyBorder(10, 10, 10, 10)));

            final FLabel lblTitle = new FLabel.Builder()
                    .text("- = *  H E R E   B E   E L D R A Z I  * = -")
                    .fontSize(22).fontAlign(SwingConstants.CENTER).build();

            final FLabel lblWarning = new FLabel.Builder()
                    .text(localizer.getMessage("lblOnlineWarning"))
                    .fontSize(16).fontAlign(SwingConstants.CENTER).build();

            final FLabel lblGuideText = new FLabel.Builder()
                    .text(localizer.getMessage("lblOnlineGuideText"))
                    .fontSize(16).fontAlign(SwingConstants.CENTER).build();

            final FLabel lblGuideLink = new FLabel.Builder()
                    .text("<html><u>" + localizer.getMessage("lblNetworkPlayGuide") + "</u></html>")
                    .fontSize(16).fontStyle(Font.BOLD).fontAlign(SwingConstants.CENTER)
                    .hoverable().cmdClick(() -> {
                        try {
                            java.awt.Desktop.getDesktop().browse(java.net.URI.create(ForgeConstants.NETWORK_PLAY_WIKI_URL));
                        } catch (final Exception ex) {
                            java.awt.Toolkit.getDefaultToolkit().getSystemClipboard()
                                    .setContents(new java.awt.datatransfer.StringSelection(ForgeConstants.NETWORK_PLAY_WIKI_URL), null);
                        }
                    }).build();
            lblGuideLink.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));

            final FButton btnHost = new FButton(localizer.getMessage("lblHostGame"));
            btnHost.setFont(FSkin.getRelativeFont(18));
            btnHost.addActionListener(e -> getLayoutControl().hostGame());

            final FButton btnJoin = new FButton(localizer.getMessage("lblJoinGame"));
            btnJoin.setFont(FSkin.getRelativeFont(18));
            btnJoin.addActionListener(e -> getLayoutControl().joinGame());

            final JPanel buttonPanel = new JPanel(new MigLayout("insets 0, gap 20, ax center"));
            buttonPanel.setOpaque(false);
            buttonPanel.add(btnHost, "w 200!, h 50!");
            buttonPanel.add(btnJoin, "w 200!, h 50!");

            infoBox.add(lblTitle, "ax center, gap 0 0 0 15");
            infoBox.add(lblWarning, "ax center, gap 0 0 0 15");
            infoBox.add(lblGuideText, "ax center, gap 0 0 0 0");
            infoBox.add(lblGuideLink, "ax center, gap 0 0 0 25");
            infoBox.add(buttonPanel, "ax center");

            container.setLayout(new BorderLayout());
            final JPanel wrapper = new JPanel(new MigLayout("ax center, ay center"));
            wrapper.setOpaque(false);
            wrapper.add(infoBox);
            container.add(wrapper, BorderLayout.CENTER);

            if (container.isShowing()) {
                container.validate();
                container.repaint();
            }
            return;
        }

        container.setLayout(new MigLayout("insets 0, gap 0, wrap 1, ax right"));

        lobby.getLblTitle().setText(Localizer.getInstance().getMessage("lblOnlineLobbyTitle"));
        pnlTitle.removeAll();
        pnlTitle.setOpaque(false);
        final boolean hosting = FServerManager.getInstance().isHosting();
        pnlTitle.add(lobby.getLblTitle(), "w 95%, h 40px!, gap 0 0 15px 15px, span " + (hosting ? "3" : "2"));
        if (hosting) {
            FButton btnServerUrl = new FButton(Localizer.getInstance().getMessage("lblServerURL"));
            btnServerUrl.setFont(FSkin.getRelativeFont(14));
            pnlTitle.add(btnServerUrl, "w 150!, h 35!, gap 10 10 0 0, align right");
            btnServerUrl.addActionListener(e -> CSubmenuOnlineLobby.showServerAddressesDialog());
        }
        pnlTitle.add(btnStop, "gap 10 10 0 0, align right");
        container.add(pnlTitle,"w 80%, gap 0 0 0 0, al right, pushx");

        for (final FDeckChooser fdc : lobby.getDeckChoosers()) {
            fdc.populate();
            fdc.getDecksComboBox().addListener(ev -> lobby.focusOnAvatar());
        }

        container.add(lobby.getConstructedFrame(), "gap 20px 20px 20px 0px, push, grow");
        container.add(lobby.getPanelStart(), "gap 0 0 3.5%! 3.5%!, ax center");

        if (container.isShowing()) {
            container.validate();
            container.repaint();
        }

        if (!lobby.getPlayerPanels().isEmpty()) {
            lobby.changePlayerFocus(0);
        }
    }

    @Override
    public EMenuGroup getGroupEnum() {
        return EMenuGroup.ONLINE;
    }

    @Override
    public String getMenuTitle() {
        return Localizer.getInstance().getMessage("lblLobby");
    }

    @Override
    public EDocID getItemEnum() {
        return getDocumentID();
    }

    @Override
    public EDocID getDocumentID() {
        return EDocID.HOME_NETWORK;
    }

    @Override
    public DragTab getTabLabel() {
        return tab;
    }

    @Override
    public CSubmenuOnlineLobby getLayoutControl() {
        return CSubmenuOnlineLobby.SINGLETON_INSTANCE;
    }

    @Override
    public void setParentCell(DragCell cell0) {
        parentCell = cell0;
    }

    @Override
    public DragCell getParentCell() {
        return parentCell;
    }

    @Override
    public void instantiate() {
    }

    @Override
    public boolean onSwitching(final FScreen fromScreen, final FScreen toScreen) {
        return true;
    }

    @Override
    public boolean onClosing(final FScreen screen) {
        final FServerManager server = FServerManager.getInstance();
        if (server.isHosting()) {
            if (SOptionPane.showConfirmDialog(Localizer.getInstance().getMessage("lblLeaveLobbyDescription"), Localizer.getInstance().getMessage("lblLeave"))) {
                server.stopServer();
                FNetOverlay.SINGLETON_INSTANCE.reset();
                return true;
            }
        } else if (client == null || SOptionPane.showConfirmDialog(Localizer.getInstance().getMessage("lblLeaveLobbyConfirm"), Localizer.getInstance().getMessage("lblLeave"))) {
            if (client != null) {
                client.close();
                client = null;
            }
            FNetOverlay.SINGLETON_INSTANCE.reset();
            return true;
        }
        return false;
    }

    @Override
    public void closeConn(String msg) {
        // Clean up connection state
        if (client != null) {
            client.close();
            client = null;
        }
        FServerManager server = FServerManager.getInstance();
        if (server.isHosting()) {
            server.stopServer();
        }
        FNetOverlay.SINGLETON_INSTANCE.reset();

        // Clear lobby and repopulate
        this.lobby = null;
        populate();

        // Show error message if provided
        if (msg != null && !msg.isEmpty()) {
            SOptionPane.showErrorDialog(msg, Localizer.getInstance().getMessage("lblConnectionError"));
        }
    }
}
