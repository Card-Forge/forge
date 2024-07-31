package forge.screens.home.puzzle;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;

import forge.gamemodes.match.GameLobby;
import forge.gamemodes.match.LocalLobby;
import forge.gui.framework.DragCell;
import forge.gui.framework.DragTab;
import forge.gui.framework.EDocID;
import forge.screens.home.EMenuGroup;
import forge.screens.home.IVSubmenu;
import forge.screens.home.StartButton;
import forge.screens.home.VHomeUI;
import forge.screens.home.VLobby;
import forge.toolbox.FList;
import forge.toolbox.FScrollPane;
import forge.util.Localizer;
import net.miginfocom.swing.MigLayout;

public enum VSubmenuTutorial implements IVSubmenu<CSubmenuTutorial> {
    SINGLETON_INSTANCE;

    private final FList tutorialList;
    private final FScrollPane tutorialListPane;

    final DefaultListModel model = new DefaultListModel();

    private final StartButton btnStart  = new StartButton();

    private DragCell parentCell;
    final Localizer localizer = Localizer.getInstance();
    private final DragTab tab = new DragTab(localizer.getMessage("lblTutorialMode"));

    private final GameLobby lobby = new LocalLobby();
    private final VLobby vLobby = new VLobby(lobby);

    VSubmenuTutorial() {
        tutorialList = new FList<>();
        tutorialListPane = new FScrollPane(this.tutorialList, true);

        lobby.setListener(vLobby);

        vLobby.setPlayerChangeListener(lobby::applyToSlot);

        vLobby.update(false);
    }

    @Override
    public EMenuGroup getGroupEnum() {
        return EMenuGroup.PUZZLE;
    }

    @Override
    public String getMenuTitle() {
        final Localizer localizer = Localizer.getInstance();
        return localizer.getMessage("lblTutorial");
    }

    @Override
    public EDocID getItemEnum() {
        return EDocID.HOME_TUTORIAL;
    }

    @Override
    public EDocID getDocumentID() {
        return EDocID.HOME_TUTORIAL;
    }

    @Override
    public DragTab getTabLabel() {
        return tab;
    }

    @Override
    public CSubmenuTutorial getLayoutControl() {
        return CSubmenuTutorial.SINGLETON_INSTANCE;
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
        return tutorialList;
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
        final Localizer localizer = Localizer.getInstance();
        vLobby.getLblTitle().setText(localizer.getMessage("lblTutorialMode"));
        container.add(vLobby.getLblTitle(), "w 80%, h 40px!, gap 0 0 15px 15px, span 2, al right, pushx");
        tutorialList.setModel(model);
        container.add(tutorialListPane, "w 80%, h 80%, gap 0 0 0px 0px, span 2, al center");
        container.add(btnStart, "w 98%!, ax center, gap 1% 0 20px 20px, span 2");


        if (container.isShowing()) {
            container.validate();
            container.repaint();
        }
    }
}
