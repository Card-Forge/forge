package forge.screens.home.gauntlet;

import forge.deckchooser.FDeckChooser;
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
import forge.toolbox.FSkin.SkinnedPanel;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;

import java.awt.*;

/** 
 * Assembles Swing components of "build gauntlet" submenu singleton.
 *
 * <br><br><i>(V at beginning of class name denotes a view class.)</i>
 *
 */
public enum VSubmenuGauntletContests implements IVSubmenu<CSubmenuGauntletContests> {
    /** */
    SINGLETON_INSTANCE;

    // Fields used with interface IVDoc
    private DragCell parentCell;
    private final DragTab tab = new DragTab("Gauntlet Contests");

    // Other fields
    private final FLabel lblTitle = new FLabel.Builder()
        .text("Gauntlet Contests").fontAlign(SwingConstants.CENTER)
        .opaque(true).fontSize(16).build();

    private final StartButton btnStart  = new StartButton();

    private final SkinnedPanel pnlLoad = new SkinnedPanel(new MigLayout("insets 0, gap 0, wrap"));

    private final ContestGauntletLister gauntletList = new ContestGauntletLister();
    private final FDeckChooser lstDecks = new FDeckChooser(null, false);

    private final FScrollPane scrLeft  = new FScrollPane(gauntletList, false,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

    private final FLabel lblLoad = new FLabel.Builder().fontSize(16)
            .fontStyle(Font.BOLD).text("PICK A CONTEST").fontAlign(SwingConstants.CENTER).build();

    private final FLabel lblDesc1 = new FLabel.Builder()
        .text("A gauntlet that has been started will keep the same deck until it is finished.").build();

    private VSubmenuGauntletContests() {
        lblTitle.setBackground(FSkin.getColor(FSkin.Colors.CLR_THEME2));

        pnlLoad.setLayout(new MigLayout("insets 0, gap 0, wrap"));
        pnlLoad.setBackground(FSkin.getColor(FSkin.Colors.CLR_THEME2));
        pnlLoad.add(lblLoad, "h 30px!, w 94%!, gap 1% 0 0 5px, ax center");
       // pnlLoad.add(new FLabel.Builder().text("If a gauntlet has been started, its deck is frozen.").build(),
         //       "gap 0 0 0 5px, ax center");
        pnlLoad.add(scrLeft, "w 94%!, pushy, growy, gap 3% 0 0 10px");
    }

    /* (non-Javadoc)
     * @see forge.gui.home.IVSubmenu#getGroupEnum()
     */
    @Override
    public EMenuGroup getGroupEnum() {
        return EMenuGroup.GAUNTLET;
    }

    /* (non-Javadoc)
     * @see forge.gui.home.IVSubmenu#getMenuTitle()
     */
    @Override
    public String getMenuTitle() {
        return "Gauntlet Contests";
    }

    /* (non-Javadoc)
     * @see forge.gui.home.IVSubmenu#getItemEnum()
     */
    @Override
    public EDocID getItemEnum() {
        return EDocID.HOME_GAUNTLETCONTESTS;
    }

    /* (non-Javadoc)
     * @see forge.gui.home.IVSubmenu#populate()
     */
    @Override
    public void populate() {
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().removeAll();
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().setLayout(new MigLayout("insets 0, gap 0, wrap 2"));

        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(lblTitle, "w 98%!, h 30px!, gap 1% 0 15px 15px, span 2");
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(lblDesc1, "gap 0 0 0 15px, ax center, span 2");
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(pnlLoad, "w 56%!, gap 1% 2% 0 15px, pushy, growy");
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(lstDecks, "w 40%!, gap 0 0 0 15px, pushy, growy");
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(btnStart, "w 98%!, ax center, gap 1% 0 20px 20px, span 2");
        
        lstDecks.populate();

        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().repaintSelf();
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().revalidate();
    }


    /** @return {@link javax.swing.JButton} */
    public JButton getBtnStart() {
        return this.btnStart;
    }

    /** @return {@link forge.screens.home.gauntlet.ContestGauntletLister} */
    public ContestGauntletLister getGauntletLister() {
        return this.gauntletList;
    }

    /** @return {@link javax.swing.JList} */
    public FDeckChooser getLstDecks() {
        return this.lstDecks;
    }

    //========== Overridden from IVDoc

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getDocumentID()
     */
    @Override
    public EDocID getDocumentID() {
        return EDocID.HOME_GAUNTLETCONTESTS;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getTabLabel()
     */
    @Override
    public DragTab getTabLabel() {
        return tab;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getLayoutControl()
     */
    @Override
    public CSubmenuGauntletContests getLayoutControl() {
        return CSubmenuGauntletContests.SINGLETON_INSTANCE;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#setParentCell(forge.gui.framework.DragCell)
     */
    @Override
    public void setParentCell(DragCell cell0) {
        this.parentCell = cell0;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getParentCell()
     */
    @Override
    public DragCell getParentCell() {
        return parentCell;
    }
}
