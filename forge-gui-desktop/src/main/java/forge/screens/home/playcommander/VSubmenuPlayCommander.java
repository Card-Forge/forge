package forge.screens.home.playcommander;

import javax.swing.JPanel;

import forge.deckchooser.FDeckChooser;
import forge.game.GameType;
import forge.gamemodes.match.GameLobby;
import forge.gamemodes.match.LocalLobby;
import forge.gui.framework.DragCell;
import forge.gui.framework.DragTab;
import forge.gui.framework.EDocID;
import forge.screens.home.EMenuGroup;
import forge.screens.home.IVSubmenu;
import forge.screens.home.VHomeUI;
import forge.screens.home.VLobby;
import forge.util.Localizer;
import net.miginfocom.swing.MigLayout;

public enum VSubmenuPlayCommander implements IVSubmenu<CSubmenuPlayCommander> {

    SINGLETON_INSTANCE;
    final Localizer localizer = Localizer.getInstance();
    private DragCell parentCell;
    private final DragTab tab = new DragTab(localizer.getMessage("lblCommander"));
    private final GameLobby lobby;
    private final VLobby vLobby;

    VSubmenuPlayCommander() {
        lobby = new LocalLobby();
        lobby.applyVariant(GameType.Commander);
        vLobby = new VLobby(lobby);
        lobby.setListener(vLobby);
        vLobby.setPlayerChangeListener(lobby::applyToSlot);
        vLobby.update(false);
    }

    public VLobby getLobby() {
        return vLobby;
    }

    @Override
    public EDocID getDocumentID() {
        return EDocID.HOME_PLAY_COMMANDER;
    }

    @Override
    public DragTab getTabLabel() {
        return tab;
    }

    @Override
    public CSubmenuPlayCommander getLayoutControl() {
        return CSubmenuPlayCommander.SINGLETON_INSTANCE;
    }

    @Override
    public void setParentCell(DragCell cell0) {
        this.parentCell = cell0;
    }

    @Override
    public DragCell getParentCell() {
        return parentCell;
    }

    @Override
    public EMenuGroup getGroupEnum() {
        return EMenuGroup.PLAY;
    }

    @Override
    public String getMenuTitle() {
        return localizer.getMessage("lblCommander");
    }

    @Override
    public EDocID getItemEnum() {
        return EDocID.HOME_PLAY_COMMANDER;
    }

    @Override
    public void populate() {
        final JPanel container = VHomeUI.SINGLETON_INSTANCE.getPnlDisplay();
        container.removeAll();
        container.setLayout(new MigLayout("insets 0, gap 0, wrap 1, ax right"));
        container.add(vLobby.getLblTitle(), "w 80%, h 40px!, gap 0 0 15px 15px, span 2, al right, pushx");

        for (final FDeckChooser fdc : vLobby.getDeckChoosers()) {
            fdc.populate();
        }

        container.add(vLobby.getConstructedFrame(), "gap 20px 20px 20px 0px, push, grow");
        container.add(vLobby.getPanelStart(), "gap 0 0 3.5%! 3.5%!, ax center");

        if (container.isShowing()) {
            container.validate();
            container.repaint();
        }

        if (!vLobby.getPlayerPanels().isEmpty()) {
            vLobby.changePlayerFocus(0);
        }
    }
}
