package forge.screens.home.online;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JPanel;

import forge.screens.home.*;
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
import forge.toolbox.FButton;
import forge.toolbox.FSkin;
import forge.util.gui.SOptionPane;
import forge.util.Localizer;

public enum VSubmenuOnlineLobby implements IVSubmenu<CSubmenuOnlineLobby>, IOnlineLobby, IVTopLevelUI {
    SINGLETON_INSTANCE;

    private DragCell parentCell;
    private final DragTab tab = new DragTab(Localizer.getInstance().getMessage("lblLobby"));
    private VLobby lobby;
    private FGameClient client;

    private final JPanel pnlTitle = new JPanel(new MigLayout());
    private final StopButton btnStop  = new StopButton();

    VSubmenuOnlineLobby() {
    }

    public ILobbyView setLobby(final GameLobby lobby) {
        this.lobby = new VLobby(lobby);
        getLayoutControl().setLobby(this.lobby);
        return this.lobby;
    }

    public void reset() {
        onClosing(null);
        this.client = null;
        this.lobby = null;
        populate();
    }

    public void setClient(final FGameClient client) {
        this.client = client;
    }

    @Override
    public void populate() {
        final JPanel container = VHomeUI.SINGLETON_INSTANCE.getPnlDisplay();

        container.removeAll();

        if (lobby == null) {
            final FButton btnConnect = new FButton(Localizer.getInstance().getMessage("lblConnectToServer"));
            btnConnect.setFont(FSkin.getRelativeFont(20));
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

        lobby.getLblTitle().setText(Localizer.getInstance().getMessage("lblOnlineLobbyTitle"));
        pnlTitle.removeAll();
        pnlTitle.setOpaque(false);
        pnlTitle.add(lobby.getLblTitle(), "w 95%, h 40px!, gap 0 0 15px 15px, span 2");
        pnlTitle.add(btnStop, "gap 10 10 0 0, align right");
        container.add(pnlTitle,"w 80%, gap 0 0 0 0, al right, pushx");

        // Stop button event handling
        btnStop.addActionListener(new ActionListener() {
            @Override
            public final void actionPerformed(final ActionEvent arg0) {
                Runnable stopGame = new Runnable() {
                    @Override
                    public void run() {
                        // do the STOP needful here
                        reset();
                    }
                };
                if (stopGame != null) {
                    stopGame.run();
                }
            }
        });

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
        } else {
            if (client == null || SOptionPane.showConfirmDialog(Localizer.getInstance().getMessage("lblLeaveLobbyConfirm"), Localizer.getInstance().getMessage("lblLeave"))) {
                if (client != null) {
                    client.close();
                    client = null;
                }
                FNetOverlay.SINGLETON_INSTANCE.reset();
                return true;
            }
        }
        return false;
    }

    @Override
    public void closeConn(String msg) {
    }
}
