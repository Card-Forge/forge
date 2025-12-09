package forge.screens.home.rogue;

import forge.gui.framework.DragCell;
import forge.gui.framework.DragTab;
import forge.gui.framework.EDocID;
import forge.screens.home.EMenuGroup;
import forge.screens.home.IVSubmenu;
import forge.screens.home.StartButton;
import forge.screens.home.VHomeUI;
import forge.toolbox.FLabel;
import forge.toolbox.FPanel;
import forge.toolbox.FSkin;
import forge.util.Localizer;
import net.miginfocom.swing.MigLayout;

import javax.swing.JButton;
import javax.swing.SwingConstants;
import java.awt.Font;

/**
 * Assembles Swing components of "rogue map" submenu singleton.
 *
 * <br><br><i>(V at beginning of class name denotes a view class.)</i>
 */
public enum VSubmenuRogueMap implements IVSubmenu<CSubmenuRogueMap> {
    SINGLETON_INSTANCE;

    final Localizer localizer = Localizer.getInstance();
    private DragCell parentCell;
    private final DragTab tab = new DragTab("Rogue Commander");

    private final FLabel lblTitle = new FLabel.Builder()
        .text("Rogue Commander - Path")
        .fontAlign(SwingConstants.CENTER)
        .opaque(true)
        .fontSize(16)
        .build();

    private final FLabel lblLife = new FLabel.Builder()
        .text("Life: 20")
        .fontSize(14)
        .fontStyle(Font.BOLD)
        .build();

    private final FLabel lblNodeInfo = new FLabel.Builder()
        .text("Current Node: None")
        .fontSize(12)
        .build();

    private final FPanel pnlPathDisplay = new FPanel(new MigLayout("insets 10, gap 10, wrap"));

    private final StartButton btnEnterNode = new StartButton();

    VSubmenuRogueMap() {
        lblTitle.setBackground(FSkin.getColor(FSkin.Colors.CLR_THEME2));
        pnlPathDisplay.setBackground(FSkin.getColor(FSkin.Colors.CLR_THEME2));
        pnlPathDisplay.setCornerDiameter(10);

        btnEnterNode.setText("Enter Node");
    }

    public void updateDisplay(String lifeText, String nodeText) {
        lblLife.setText(lifeText);
        lblNodeInfo.setText(nodeText);
    }

    @Override
    public EMenuGroup getGroupEnum() {
        return EMenuGroup.GAUNTLET; // Using GAUNTLET group for now
    }

    @Override
    public String getMenuTitle() {
        return "Rogue Commander";
    }

    @Override
    public EDocID getItemEnum() {
        return EDocID.HOME_ROGUEMAP;
    }

    @Override
    public void populate() {
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().removeAll();
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().setLayout(new MigLayout("insets 0, gap 0, wrap"));

        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(lblTitle, "w 98%!, h 30px!, gap 1% 0 15px 15px");
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(lblLife, "ax center, gap 0 0 10px 10px");
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(pnlPathDisplay, "w 96%!, gap 2% 2% 0 0, pushy, growy");
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(btnEnterNode, "w 96%!, h 40px!, ax center, gap 2% 2% 20px 20px");

        // Add node info to path display
        pnlPathDisplay.removeAll();
        pnlPathDisplay.add(lblNodeInfo, "w 100%!");

        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().repaintSelf();
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().revalidate();
    }

    public JButton getBtnEnterNode() {
        return btnEnterNode;
    }

    public FPanel getPnlPathDisplay() {
        return pnlPathDisplay;
    }

    @Override
    public EDocID getDocumentID() {
        return EDocID.HOME_ROGUEMAP;
    }

    @Override
    public DragTab getTabLabel() {
        return tab;
    }

    @Override
    public CSubmenuRogueMap getLayoutControl() {
        return CSubmenuRogueMap.SINGLETON_INSTANCE;
    }

    @Override
    public void setParentCell(DragCell cell0) {
        this.parentCell = cell0;
    }

    @Override
    public DragCell getParentCell() {
        return parentCell;
    }
}
