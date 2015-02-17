package forge.screens.home.gauntlet;

import forge.assets.FSkinProp;
import forge.deckchooser.FDeckChooser;
import forge.gauntlet.GauntletIO;
import forge.gui.framework.DragCell;
import forge.gui.framework.DragTab;
import forge.gui.framework.EDocID;
import forge.screens.home.EMenuGroup;
import forge.screens.home.IVSubmenu;
import forge.screens.home.VHomeUI;
import forge.toolbox.*;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;

import java.awt.*;

/** 
 * Assembles Swing components of "build gauntlet" submenu singleton.
 *
 * <br><br><i>(V at beginning of class name denotes a view class.)</i>
 *
 */
public enum VSubmenuGauntletBuild implements IVSubmenu<CSubmenuGauntletBuild> {
    /** */
    SINGLETON_INSTANCE;

    // Fields used with interface IVDoc
    private DragCell parentCell;
    private final DragTab tab = new DragTab("Gauntlet Builder");

    // Other fields
    private final FLabel lblTitle     = new FLabel.Builder()
        .text("Gauntlet Builder").fontAlign(SwingConstants.CENTER)
        .opaque(true).fontSize(16).build();

    private final JPanel pnlFileHandling = new JPanel(new MigLayout("insets 0, gap 0, align center"));
    private final JPanel pnlButtons = new JPanel();
    private final JPanel pnlStrut = new JPanel();
    private final JPanel pnlDirections = new JPanel();

    private final FDeckChooser lstLeft = new FDeckChooser(null, false);
    private final JList<String> lstRight = new FList<String>();

    private final FScrollPane scrRight  = new FScrollPane(lstRight, true,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

    private final FTextField txfFilename = new FTextField.Builder().ghostText(GauntletIO.TXF_PROMPT).showGhostTextWithFocus().build();

    private final FLabel lblDesc1 = new FLabel.Builder().text("Left/right arrows add or remove decks.").fontSize(12).build();

    private final FLabel lblDesc2 = new FLabel.Builder()
        .text("Up/down arrows change opponent order.")
        .fontSize(12).build();

    private final FLabel lblDecklist = new FLabel.Builder()
        .text("Double click a non-random deck for its decklist.")
        .fontSize(12).build();

    private final JLabel lblSave = new FLabel.Builder().text("Changes not yet saved.")
            .build();

    private final FLabel btnUp = new FLabel.Builder()
        .tooltip("Move this deck up in the gauntlet").hoverable(true)
        .iconScaleAuto(true).iconScaleFactor(1.0)
        .icon(FSkin.getImage(FSkinProp.IMG_CUR_T)).build();

    private final FLabel btnDown = new FLabel.Builder()
        .tooltip("Move this deck down in the gauntlet").hoverable(true)
        .iconScaleAuto(true).iconScaleFactor(1.0)
        .icon(FSkin.getImage(FSkinProp.IMG_CUR_B)).build();

    private final FLabel btnRight = new FLabel.Builder()
        .tooltip("Add this deck to the gauntlet").hoverable(true)
        .iconScaleAuto(true).iconScaleFactor(1.0)
        .icon(FSkin.getImage(FSkinProp.IMG_CUR_R)).build();

    private final FLabel btnLeft = new FLabel.Builder()
        .tooltip("Remove this deck from the gauntlet").hoverable(true)
        .iconScaleAuto(true).iconScaleFactor(1.0)
        .icon(FSkin.getImage(FSkinProp.IMG_CUR_L)).build();

    private final FLabel btnSave = new FLabel.Builder()
        .fontSize(14)
        .tooltip("Save this gauntlet")
        .iconInBackground(true)
        .iconAlignX(SwingConstants.CENTER)
        .icon(FSkin.getIcon(FSkinProp.ICO_SAVE))
        .text(" ").hoverable(true).build();

    private final FLabel btnNew = new FLabel.Builder()
        .fontSize(14)
        .tooltip("Build a new gauntlet")
        .iconInBackground(true)
        .iconAlignX(SwingConstants.CENTER)
        .icon(FSkin.getIcon(FSkinProp.ICO_NEW))
        .text(" ").hoverable(true).build();

    private final FLabel btnOpen = new FLabel.Builder()
        .fontSize(14)
        .tooltip("Load a gauntlet")
        .iconInBackground(true)
        .iconAlignX(SwingConstants.CENTER)
        .icon(FSkin.getIcon(FSkinProp.ICO_OPEN))
        .text(" ").hoverable(true).build();

    private VSubmenuGauntletBuild() {
        lblTitle.setBackground(FSkin.getColor(FSkin.Colors.CLR_THEME2));

        // File handling panel
        final FLabel lblFilename = new FLabel.Builder()
            .text("Gauntlet Name:").fontSize(14).build();
        pnlFileHandling.setOpaque(false);
        pnlFileHandling.add(lblFilename, "h 30px!, gap 0 5px 0");
        pnlFileHandling.add(txfFilename, "h 30px!, w 200px!, gap 0 5px 0 0");
        pnlFileHandling.add(btnSave, "h 30px!, w 30px!, gap 0 5px 0 0");
        pnlFileHandling.add(btnNew, "h 30px!, w 30px!, gap 0 5px 0 0");
        pnlFileHandling.add(btnOpen, "h 30px!, w 30px!, gap 0 5px 0 0");

        // Directions panel
        final JPanel pnlSpacer = new JPanel();
        pnlSpacer.setOpaque(false);
        pnlStrut.setOpaque(false);
        lblSave.setForeground(Color.red);
        lblSave.setVisible(false);
        pnlDirections.setOpaque(false);
        pnlDirections.setLayout(new MigLayout("insets 0, gap 0, wrap"));
        pnlDirections.add(pnlSpacer, "w 100%!, pushy, growy");
        pnlDirections.add(lblDesc1, "gap 1% 0 0 10px");
        pnlDirections.add(lblDesc2, "gap 1% 0 0 10px");
        pnlDirections.add(lblDecklist, "gap 1% 0 0 20px");
        pnlDirections.add(lblSave, "ax center, gap 0 0 0 5px");

        // Deck movement panel
        pnlButtons.setLayout(new MigLayout("insets 0, gap 0, wrap, ay center"));
        pnlButtons.setOpaque(false);
        pnlButtons.add(btnRight, "h 40px!, w 100%!");
        pnlButtons.add(btnLeft, "h 40px!, w 100%!");
        pnlButtons.add(btnUp, "h 40px!, w 100%!, gap 0 0 50px 0, ay bottom");
        pnlButtons.add(btnDown, "h 40px!, w 100%!, ay baseline");
    }

    public void focusName() {
        txfFilename.requestFocusInWindow();
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
        return "Build A Gauntlet";
    }

    /* (non-Javadoc)
     * @see forge.gui.home.IVSubmenu#getItemEnum()
     */
    @Override
    public EDocID getItemEnum() {
        return EDocID.HOME_GAUNTLETBUILD;
    }

    /* (non-Javadoc)
     * @see forge.gui.home.IVSubmenu#populate()
     */
    @Override
    public void populate() {
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().removeAll();
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().setLayout(new MigLayout("insets 0, gap 0, wrap 3"));

        lstLeft.populate();

        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(lblTitle, "w 98%!, h 30px!, gap 1% 0 15px 15px, span 3");
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(pnlFileHandling, "w 98%!, gap 1% 0 1% 5px, span 3");
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(lstLeft, "w 48% - 20px!, gap 1% 0 0 25px, spany 2, growy");
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(pnlStrut, "w 40px!, gap 1% 1% 0 15px");
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(pnlDirections, "w 48% - 20px!, gap 0 0 0 15px");
//        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(lstLeft, "w 48% - 20px!, gap 1% 0 0 25px, pushy, growy");
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(pnlButtons, "w 40px!, gap 1% 1% 0 25px, pushy, growy");
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(scrRight, "w 48% - 20px!, gap 0 0 0 25px, pushy, growy");

        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().repaintSelf();
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().revalidate();
    }

    /** @return {@link javax.swing.JList} */
    public FDeckChooser getLstLeft() {
        return this.lstLeft;
    }

    /** @return {@link javax.swing.JList} */
    public JList<String> getLstRight() {
        return this.lstRight;
    }

    /** @return {@link forge.toolbox.FLabel} */
    public FLabel getBtnUp() {
        return btnUp;
    }

    /** @return {@link forge.toolbox.FLabel} */
    public FLabel getBtnDown() {
        return btnDown;
    }

    /** @return {@link forge.toolbox.FLabel} */
    public FLabel getBtnRight() {
        return btnRight;
    }

    /** @return {@link forge.toolbox.FLabel} */
    public FLabel getBtnLeft() {
        return btnLeft;
    }

    /** @return {@link javax.swing.JLabel} */
    public FLabel getBtnSave() {
        return btnSave;
    }

    /** @return {@link javax.swing.JLabel} */
    public FLabel getBtnOpen() {
        return btnOpen;
    }

    /** @return {@link javax.swing.JLabel} */
    public FLabel getBtnNew() {
        return btnNew;
    }

    /** @return {@link javax.swing.JLabel} */
    public JLabel getLblSave() {
        return lblSave;
    }

    /** @return {@link javax.swing.JTextField} */
    public FTextField getTxfFilename() {
        return txfFilename;
    }

    //========== Overridden from IVDoc

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getDocumentID()
     */
    @Override
    public EDocID getDocumentID() {
        return EDocID.HOME_GAUNTLETBUILD;
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
    public CSubmenuGauntletBuild getLayoutControl() {
        return CSubmenuGauntletBuild.SINGLETON_INSTANCE;
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
