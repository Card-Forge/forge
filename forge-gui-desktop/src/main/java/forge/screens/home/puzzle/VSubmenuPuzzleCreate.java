package forge.screens.home.puzzle;

import forge.gui.framework.DragCell;
import forge.gui.framework.DragTab;
import forge.gui.framework.EDocID;
import forge.interfaces.IPlayerChangeListener;
import forge.match.GameLobby;
import forge.match.LocalLobby;
import forge.net.event.UpdateLobbyPlayerEvent;
import forge.screens.home.EMenuGroup;
import forge.screens.home.IVSubmenu;
import forge.screens.home.VHomeUI;
import forge.screens.home.VLobby;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;

public enum VSubmenuPuzzleCreate implements IVSubmenu<CSubmenuPuzzleCreate> {
    SINGLETON_INSTANCE;

    private DragCell parentCell;
    private final DragTab tab = new DragTab("Puzzle Mode: Create");

    private final GameLobby lobby = new LocalLobby();
    private final VLobby vLobby = new VLobby(lobby);

    VSubmenuPuzzleCreate() {
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
        return "Create";
    }

    @Override
    public EDocID getItemEnum() {
        return EDocID.HOME_PUZZLE_CREATE;
    }

    @Override
    public EDocID getDocumentID() {
        return EDocID.HOME_PUZZLE_CREATE;
    }

    @Override
    public DragTab getTabLabel() {
        return tab;
    }

    @Override
    public CSubmenuPuzzleCreate getLayoutControl() {
        return CSubmenuPuzzleCreate.SINGLETON_INSTANCE;
    }

    @Override
    public void setParentCell(DragCell cell0) {
        this.parentCell = cell0;
    }

    @Override
    public DragCell getParentCell() {
        return this.parentCell;
    }

    @Override
    public void populate() {
        final JPanel container = VHomeUI.SINGLETON_INSTANCE.getPnlDisplay();

        container.removeAll();
        container.setLayout(new MigLayout("insets 0, gap 0, wrap 1, ax right"));
        vLobby.getLblTitle().setText("Puzzle Mode: Create");
        container.add(vLobby.getLblTitle(), "w 80%, h 40px!, gap 0 0 15px 15px, span 2, al right, pushx");


        if (container.isShowing()) {
            container.validate();
            container.repaint();
        }
    }
}
