package forge.screens.home.puzzle;

import forge.gamemodes.match.GameLobby;
import forge.gamemodes.match.LocalLobby;
import forge.gamemodes.net.event.UpdateLobbyPlayerEvent;
import forge.gui.framework.DragCell;
import forge.gui.framework.DragTab;
import forge.gui.framework.EDocID;
import forge.interfaces.IPlayerChangeListener;
import forge.screens.home.*;
import forge.toolbox.FLabel;
import forge.util.Localizer;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;

public enum VSubmenuPuzzleCreate implements IVSubmenu<CSubmenuPuzzleCreate> {
    SINGLETON_INSTANCE;
    final Localizer localizer = Localizer.getInstance();
    private DragCell parentCell;
    private final DragTab tab = new DragTab(localizer.getMessage("lblPuzzleModeCreate"));

    private final GameLobby lobby = new LocalLobby();
    private final VLobby vLobby = new VLobby(lobby);

    private final FLabel lblInfo = new FLabel.Builder()
            .fontAlign(SwingConstants.LEFT).fontSize(16).fontStyle(Font.BOLD)
            .text(localizer.getMessage("lblCreateNewPuzzle")).build();

    private final FLabel lblDir1 = new FLabel.Builder()
            .text(localizer.getMessage("lblCreatePuzzleDest1"))
            .fontSize(12).build();

    private final FLabel lblDir2 = new FLabel.Builder()
            .text(localizer.getMessage("lblCreatePuzzleDest2"))
            .fontSize(12).build();

    private final FLabel lblDir3 = new FLabel.Builder()
            .text(localizer.getMessage("lblCreatePuzzleDest3"))
            .fontSize(12).build();

    private final FLabel lblDir4 = new FLabel.Builder()
            .text(localizer.getMessage("lblCreatePuzzleDest4"))
            .fontSize(12).build();

    private final FLabel lblDir5 = new FLabel.Builder()
            .text(localizer.getMessage("lblCreatePuzzleDest5"))
            .fontSize(12).build();

    private final StartButton btnStart  = new StartButton();

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
        return localizer.getMessage(("lblCreate"));
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

    public StartButton getBtnStart() {
        return btnStart;
    }

    @Override
    public void populate() {
        final JPanel container = VHomeUI.SINGLETON_INSTANCE.getPnlDisplay();

        container.removeAll();
        container.setLayout(new MigLayout("insets 0, gap 0, wrap 1, ax right"));
        vLobby.getLblTitle().setText(localizer.getMessage("lblPuzzleModeCreate"));
        container.add(vLobby.getLblTitle(), "w 80%, h 40px!, gap 0 0 15px 15px, span 2, al right, pushx");

        container.add(lblInfo, "h 30px!, gap 0 0 0 5px, al center");
        container.add(lblDir1, "gap 0 0 0 5px, al center");
        container.add(lblDir2, "gap 0 0 0 5px, al center");
        container.add(lblDir3, "gap 0 0 0 5px, al center");
        container.add(lblDir4, "gap 0 0 0 5px, al center");
        container.add(lblDir5, "gap 0 0 0 5px, al center");

        container.add(btnStart, "w 98%!, ax center, gap 1% 0 20px 20px, span 2");


        if (container.isShowing()) {
            container.validate();
            container.repaint();
        }
    }
}
