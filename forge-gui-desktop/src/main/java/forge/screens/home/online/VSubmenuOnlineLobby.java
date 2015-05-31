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
import forge.interfaces.ILobbyView;
import forge.match.GameLobby;
import forge.net.IOnlineLobby;
import forge.net.client.FGameClient;
import forge.net.server.FServerManager;
import forge.screens.home.EMenuGroup;
import forge.screens.home.IVSubmenu;
import forge.screens.home.VHomeUI;
import forge.screens.home.VLobby;
import forge.toolbox.FButton;
import forge.toolbox.FSkin;
import forge.util.gui.SOptionPane;

public enum VSubmenuOnlineLobby implements IVSubmenu<CSubmenuOnlineLobby>, IOnlineLobby, IVTopLevelUI {
    SINGLETON_INSTANCE;

    private DragCell parentCell;
    private final DragTab tab = new DragTab("Lobby");
    private VLobby lobby;
    private FGameClient client;

    private VSubmenuOnlineLobby() {
    }

    public ILobbyView setLobby(final GameLobby lobby) {
        this.lobby = new VLobby(lobby);
        getLayoutControl().setLobby(this.lobby);
        return this.lobby;
    }

    public void setClient(final FGameClient client) {
        this.client = client;
    }

    @Override
    public void populate() {
        final JPanel container = VHomeUI.SINGLETON_INSTANCE.getPnlDisplay();

        container.removeAll();

        if (lobby == null) {
            final FButton btnConnect = new FButton("Connect to Server");
            btnConnect.setFont(FSkin.getFont(20));
            btnConnect.addActionListener(new ActionListener() {
                @Override
                public final void actionPerformed(final ActionEvent e) {
                    getLayoutControl().connectToServer();
                }
            });
            container.setLayout(new MigLayout("insets 0, gap 0, ax center, ay center"));
            container.add(btnConnect, "w 300!, h 75!");

            if (container.isShowing()) {
                container.validate();
                container.repaint();
            }
            return;
        }

        container.setLayout(new MigLayout("insets 0, gap 0, wrap 1, ax right"));

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
