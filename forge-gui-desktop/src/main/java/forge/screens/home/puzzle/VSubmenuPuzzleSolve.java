package forge.screens.home.puzzle;

import forge.gui.framework.DragCell;
import forge.gui.framework.DragTab;
import forge.gui.framework.EDocID;
import forge.interfaces.IPlayerChangeListener;
import forge.match.GameLobby;
import forge.match.LocalLobby;
import forge.net.event.UpdateLobbyPlayerEvent;
import forge.screens.home.*;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;

public enum VSubmenuPuzzleSolve implements IVSubmenu<CSubmenuPuzzleSolve> {
    SINGLETON_INSTANCE;

    private final JList puzzleList;
    final DefaultListModel model = new DefaultListModel();

    private final StartButton btnStart  = new StartButton();

    private DragCell parentCell;
    private final DragTab tab = new DragTab("Puzzle Mode: Solve");

    private final GameLobby lobby = new LocalLobby();
    private final VLobby vLobby = new VLobby(lobby);

    VSubmenuPuzzleSolve() {
        puzzleList = new JList<>();
        lobby.setListener(vLobby);

        vLobby.setPlayerChangeListener(new IPlayerChangeListener() {
            @Override public final void update(final int index, final UpdateLobbyPlayerEvent event) {
                lobby.applyToSlot(index, event);
            }
        });

        vLobby.update(false);
    }

    @Override
    public EMenuGroup getGroupEnum() {
        return EMenuGroup.PUZZLE;
    }

    @Override
    public String getMenuTitle() {
        return "Solve";
    }

    @Override
    public EDocID getItemEnum() {
        return EDocID.HOME_PUZZLE_SOLVE;
    }

    @Override
    public EDocID getDocumentID() {
        return EDocID.HOME_PUZZLE_SOLVE;
    }

    @Override
    public DragTab getTabLabel() {
        return tab;
    }

    @Override
    public CSubmenuPuzzleSolve getLayoutControl() {
        return CSubmenuPuzzleSolve.SINGLETON_INSTANCE;
    }

    @Override
    public void setParentCell(DragCell cell0) {
        this.parentCell = cell0;
    }

    @Override
    public DragCell getParentCell() {
        return this.parentCell;
    }

    public JList getList() {
        return puzzleList;
    }

    public DefaultListModel getModel() {
        return model;
    }

    public StartButton getBtnStart() {
        return btnStart;
    }

    @Override
    public void populate() {
        final JPanel container = VHomeUI.SINGLETON_INSTANCE.getPnlDisplay();

        container.removeAll();
        container.setLayout(new MigLayout("insets 0, gap 0, wrap 1, ax right"));
        vLobby.getLblTitle().setText("Puzzle Mode: Solve");
        container.add(vLobby.getLblTitle(), "w 80%, h 40px!, gap 0 0 15px 15px, span 2, al right, pushx");
        puzzleList.setModel(model);
        container.add(puzzleList, "w 80%, h 200px!, gap 0 0 0px 0px, span 2, al center");
        container.add(btnStart, "w 98%!, ax center, gap 1% 0 20px 20px, span 2");


        if (container.isShowing()) {
            container.validate();
            container.repaint();
        }
    }
}
