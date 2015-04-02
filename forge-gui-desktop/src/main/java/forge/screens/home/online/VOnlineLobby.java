package forge.screens.home.online;

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
import forge.gui.framework.IVDoc;
import forge.gui.framework.IVTopLevelUI;
import forge.match.GameLobby;
import forge.net.FGameClient;
import forge.net.FServerManager;
import forge.screens.home.VLobby;
import forge.toolbox.FPanel;
import forge.util.gui.SOptionPane;
import forge.view.FView;

public enum VOnlineLobby implements IVDoc<COnlineLobby>, IVTopLevelUI {
    SINGLETON_INSTANCE;

    private DragCell parentCell;
    private final DragTab tab = new DragTab("Lobby");

    // General variables
    private VLobby lobby;
    private FGameClient client;

    private VOnlineLobby() {
    }

    VLobby getLobby() {
        return lobby;
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
        final JPanel outerContainer = FView.SINGLETON_INSTANCE.getPnlInsets();
        outerContainer.removeAll();

        final FPanel container = new FPanel(new MigLayout("insets 0, gap 0, wrap 1, ax right"));
        outerContainer.add(container);
        lobby.getLblTitle().setText("Online Multiplayer: Lobby");
        container.add(lobby.getLblTitle(), "w 80%, h 40px!, gap 0 0 15px 15px, span 2, al right, pushx");

        for (final FDeckChooser fdc : lobby.getDeckChoosers()) {
            fdc.populate();
            fdc.getDecksComboBox().addListener(new IDecksComboBoxListener() {
                @Override public final void deckTypeSelected(final DecksComboBoxEvent ev) {
                    lobby.getPlayerPanelWithFocus().focusOnAvatar();
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
    public EDocID getDocumentID() {
        return EDocID.ONLINE_LOBBY;
    }

    @Override
    public DragTab getTabLabel() {
        return tab;
    }

    @Override
    public COnlineLobby getLayoutControl() {
        return COnlineLobby.SINGLETON_INSTANCE;
    }

    @Override
    public void setParentCell(final DragCell cell0) {
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
