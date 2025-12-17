package forge.screens.home.rogue;

import forge.gamemodes.rogue.RogueRun;
import forge.gui.framework.DragCell;
import forge.gui.framework.DragTab;
import forge.gui.framework.EDocID;
import forge.screens.home.EMenuGroup;
import forge.screens.home.IVSubmenu;
import forge.screens.home.StartButton;
import forge.screens.home.VHomeUI;
import forge.toolbox.FLabel;
import forge.toolbox.FScrollPane;
import forge.toolbox.FSkin;
import forge.util.Localizer;
import java.awt.Font;
import javax.swing.JButton;
import javax.swing.SwingConstants;
import net.miginfocom.swing.MigLayout;

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

    private final PathVisualizerPanel pathVisualizer = new PathVisualizerPanel();
    private final FScrollPane scrollPathDisplay;

    private final StartButton btnEnterNode = new StartButton();

    VSubmenuRogueMap() {
        lblTitle.setBackground(FSkin.getColor(FSkin.Colors.CLR_THEME2));

        // Setup scroll pane for path visualizer
        scrollPathDisplay = new FScrollPane(pathVisualizer, true);
        scrollPathDisplay.setOpaque(false);

        btnEnterNode.setText("Enter Node");
    }

    /**
     * Update the display with current run data.
     */
    public void updateDisplay(RogueRun run) {
        if (run != null) {
            lblLife.setText("Life: " + run.getCurrentLife());
            pathVisualizer.updatePath(run);
        } else {
            lblLife.setText("Life: 20");
            pathVisualizer.clearPath();
        }
    }

    @Override
    public EMenuGroup getGroupEnum() {
        return EMenuGroup.ROGUE;
    }

    @Override
    public String getMenuTitle() {
        return "Continue Run";
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
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(scrollPathDisplay, "w 96%!, gap 2% 2% 0 0, pushy, growy");
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(btnEnterNode, "w 96%!, h 40px!, ax center, gap 2% 2% 20px 20px");

        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().repaintSelf();
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().revalidate();
    }

    public JButton getBtnEnterNode() {
        return btnEnterNode;
    }

    public PathVisualizerPanel getPathVisualizer() {
        return pathVisualizer;
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
