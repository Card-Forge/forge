package forge.screens.home.online;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;
import forge.deckchooser.DecksComboBoxEvent;
import forge.deckchooser.FDeckChooser;
import forge.deckchooser.IDecksComboBoxListener;
import forge.gui.FNetOverlay;
import forge.gui.framework.DragCell;
import forge.gui.framework.DragTab;
import forge.gui.framework.EDocID;
import forge.gui.framework.FScreen;
import forge.gui.framework.IVTopLevelUI;
import forge.match.GameLobby;
import forge.net.client.FGameClient;
import forge.net.server.FServerManager;
import forge.properties.ForgeConstants;
import forge.screens.home.EMenuGroup;
import forge.screens.home.IVSubmenu;
import forge.screens.home.VHomeUI;
import forge.screens.home.VLobby;
import forge.toolbox.FButton;
import forge.toolbox.FLabel;
import forge.toolbox.FPanel;
import forge.toolbox.FTextField;
import forge.util.gui.SOptionPane;

public enum VSubmenuOnlineLobby implements IVSubmenu<CSubmenuOnlineLobby>, IVTopLevelUI {
    SINGLETON_INSTANCE;

    private DragCell parentCell;
    private final DragTab tab = new DragTab("Lobby");
    private VLobby lobby;
    private FGameClient client;

    private VSubmenuOnlineLobby() {
    }

    VLobby setLobby(final GameLobby lobby) {
        this.lobby = new VLobby(lobby);
        getLayoutControl().setLobby(this.lobby);
        return this.lobby;
    }

    void setClient(final FGameClient client) {
        this.client = client;
    }

    @Override
    public void populate() {
        final JPanel container = VHomeUI.SINGLETON_INSTANCE.getPnlDisplay();

        container.removeAll();
        container.setLayout(new MigLayout("insets 0, gap 0, wrap 1, ax right"));

        if (lobby == null) {
            final FPanel pnlHost = new FPanel(new MigLayout("insets 5px 10% 5px 10%, wrap 2", "[grow,l]10[grow,r]", "[grow,c][grow,c]"));
            container.add(pnlHost, "west, w 50%!, h 100%!");

            final FLabel lblServerPort = new FLabel.Builder().text("Server port").build();
            pnlHost.add(lblServerPort, "w 100!, h 50!");

            final FTextField txtServerPort = new FTextField.Builder().text(String.valueOf(ForgeConstants.SERVER_PORT_NUMBER)).build();
            txtServerPort.setEditable(false);
            pnlHost.add(txtServerPort, "wrap");

            final FButton btnHost = new FButton("Host");
            btnHost.addActionListener(new ActionListener() {
                @Override
                public final void actionPerformed(final ActionEvent e) {
                    getLayoutControl().host(Integer.parseInt(txtServerPort.getText()));
                    populate();
                }
            });
            pnlHost.add(btnHost, "span 2, wrap, w 200!, h 50!");

            final FPanel pnlJoin = new FPanel(new MigLayout("insets 5px 10% 5px 10%, wrap 2", "[grow,l]10[grow,r]", "[grow,c][grow,c][grow,c]"));
            container.add(pnlJoin, "east, w 50%!, h 100%!");

            final FLabel lblJoinHost = new FLabel.Builder().text("Hostname").build();
            pnlJoin.add(lblJoinHost, "w 100!, h 50!");

            final FTextField txtJoinHost = new FTextField.Builder().text("localhost").build();
            pnlJoin.add(txtJoinHost, "wrap, w 250!");

            final FLabel lblJoinPort = new FLabel.Builder().text("Host port").build();
            pnlJoin.add(lblJoinPort, "w 100!, h 50!");

            final FTextField txtJoinPort = new FTextField.Builder().text(String.valueOf(ForgeConstants.SERVER_PORT_NUMBER)).build();
            txtJoinPort.setEditable(false);
            pnlJoin.add(txtJoinPort, "wrap");

            final FButton btnJoin = new FButton("Join");
            btnJoin.addActionListener(new ActionListener() {
                @Override
                public final void actionPerformed(final ActionEvent e) {
                    getLayoutControl().join(txtJoinHost.getText(), Integer.parseInt(txtJoinPort.getText()));
                    populate();
                }
            });
            pnlJoin.add(btnJoin, "span 2, w 200!, h 50!");

            if (container.isShowing()) {
                container.validate();
                container.repaint();
            }
        }
        else {
            lobby.getLblTitle().setText("Online Multiplayer: Lobby");
            container.add(lobby.getLblTitle(), "w 80%, h 40px!, gap 0 0 15px 15px, span 2, al right, pushx");
    
            for (final FDeckChooser fdc : lobby.getDeckChoosers()) {
                fdc.populate();
                fdc.getDecksComboBox().addListener(new IDecksComboBoxListener() {
                    @Override
                    public final void deckTypeSelected(final DecksComboBoxEvent ev) {
                        lobby.focusOnAvatar();
                    }
                });
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
    }

    @Override
    public EMenuGroup getGroupEnum() {
        return EMenuGroup.ONLINE;
    }

    @Override
    public String getMenuTitle() {
        return "Lobby";
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
            if (SOptionPane.showConfirmDialog("Leave lobby? Doing so will shut down all connections and stop hosting.", "Leave")) {
                FServerManager.getInstance().stopServer();
                return true;
            }
        } else {
            if (client == null || SOptionPane.showConfirmDialog("Leave lobby?", "Leave")) {
                if (client != null) {
                    client.close();
                    client = null;
                }
                FNetOverlay.SINGLETON_INSTANCE.setGameClient(null);
                return true;
            }
        }
        return false;
    }
}
