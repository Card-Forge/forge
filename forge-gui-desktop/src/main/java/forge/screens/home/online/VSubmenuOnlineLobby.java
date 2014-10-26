package forge.screens.home.online;

import javax.swing.JButton;
import javax.swing.JPanel;
import net.miginfocom.swing.MigLayout;

import forge.gui.framework.DragCell;
import forge.gui.framework.DragTab;
import forge.gui.framework.EDocID;
import forge.screens.home.EMenuGroup;
import forge.screens.home.IVSubmenu;
import forge.screens.home.LblHeader;
import forge.screens.home.StartButton;
import forge.screens.home.VHomeUI;
import forge.toolbox.FSkin;


public enum VSubmenuOnlineLobby implements IVSubmenu<CSubmenuOnlineLobby> {
    SINGLETON_INSTANCE;

    private DragCell parentCell;
    private final DragTab tab = new DragTab("Lobby");

    // General variables
    private final LblHeader lblTitle = new LblHeader("Online Multiplayer: Lobby");

    private final StartButton btnStart  = new StartButton();
    private final JPanel pnlStart = new JPanel(new MigLayout("insets 0, gap 0, wrap 2"));
    private final JPanel frame = new JPanel(new MigLayout("insets 0, gap 0, wrap 2")); // Main content frame

    private VSubmenuOnlineLobby() {
        lblTitle.setBackground(FSkin.getColor(FSkin.Colors.CLR_THEME2));

        frame.setOpaque(false);
        pnlStart.setOpaque(false);
        pnlStart.add(btnStart, "align center");
    }

    @Override
    public void populate() {
        JPanel container = VHomeUI.SINGLETON_INSTANCE.getPnlDisplay();

        container.removeAll();
        container.setLayout(new MigLayout("insets 0, gap 0, wrap 1, ax right"));
        container.add(lblTitle, "w 80%, h 40px!, gap 0 0 15px 15px, span 2, al right, pushx");

        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(frame, "gap 20px 20px 20px 0px, push, grow");
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(pnlStart, "gap 0 0 3.5%! 3.5%!, ax center");

        if (container.isShowing()) {
            container.validate();
            container.repaint();
        }
    }

    public JButton getBtnStart() {
        return btnStart;
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
        return EDocID.HOME_LOBBY;
    }

    @Override
    public EDocID getDocumentID() {
        return EDocID.HOME_LOBBY;
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
}
