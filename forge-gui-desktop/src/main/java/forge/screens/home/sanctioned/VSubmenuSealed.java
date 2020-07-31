package forge.screens.home.sanctioned;

import java.awt.Font;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import forge.toolbox.*;
import forge.util.Localizer;
import net.miginfocom.swing.MigLayout;
import forge.assets.FSkinProp;
import forge.game.GameType;
import forge.gui.SOverlayUtils;
import forge.gui.framework.DragCell;
import forge.gui.framework.DragTab;
import forge.gui.framework.EDocID;
import forge.itemmanager.DeckManager;
import forge.itemmanager.ItemManagerContainer;
import forge.screens.deckeditor.CDeckEditorUI;
import forge.screens.home.EMenuGroup;
import forge.screens.home.IVSubmenu;
import forge.screens.home.LblHeader;
import forge.screens.home.StartButton;
import forge.screens.home.VHomeUI;
import forge.screens.home.VHomeUI.PnlDisplay;
import forge.toolbox.FSkin.SkinnedTextPane;

/** 
 * Assembles Swing components of sealed submenu singleton.
 *
 * <br><br><i>(V at beginning of class name denotes a view class.)</i>
 */
public enum VSubmenuSealed implements IVSubmenu<CSubmenuSealed> {
    /** */
    SINGLETON_INSTANCE;
    final Localizer localizer = Localizer.getInstance();

    // Fields used with interface IVDoc
    private DragCell parentCell;
    private final DragTab tab = new DragTab(localizer.getMessage("lblSealedDeck"));

    /** */
    private final LblHeader lblTitle = new LblHeader(localizer.getMessage("lblHeaderSealed"));

    private final JPanel pnlStart = new JPanel();
    private final StartButton btnStart = new StartButton();
    private final DeckManager lstDecks = new DeckManager(GameType.Sealed, CDeckEditorUI.SINGLETON_INSTANCE.getCDetailPicture());

    private final JRadioButton radSingle = new FRadioButton(localizer.getMessage("lblPlayAnOpponent"));
    private final JRadioButton radAll = new FRadioButton(localizer.getMessage("lblPlayAll7opponents"));

    private final JComboBox<String> cbOpponent = new JComboBox<>();

    private final FLabel lblInfo = new FLabel.Builder()
        .fontAlign(SwingConstants.LEFT).fontSize(16).fontStyle(Font.BOLD)
        .text(localizer.getMessage("lblSealedText1")).build();

    private final FLabel lblDir1 = new FLabel.Builder()
        .text(localizer.getMessage("lblSealedText2"))
        .fontSize(12).build();

    private final FLabel lblDir2 = new FLabel.Builder()
        .text(localizer.getMessage("lblSealedText3"))
        .fontSize(12).build();

    private final FLabel lblDir3 = new FLabel.Builder()
        .text(localizer.getMessage("lblSealedText4"))
        .fontSize(12).build();

    private final FLabel btnBuildDeck = new FLabel.ButtonBuilder().text(localizer.getMessage("btnBuildNewSealedDeck")).fontSize(16).build();

    private final FLabel btnDirections = new FLabel.Builder()
        .fontSize(16).opaque(true).hoverable(true)
        .text(localizer.getMessage("lblHowtoPlay")).fontAlign(SwingConstants.CENTER).build();

    /**
     * Constructor.
     */
    VSubmenuSealed() {
        btnStart.setEnabled(false);

        lblTitle.setBackground(FSkin.getColor(FSkin.Colors.CLR_THEME2));
        lstDecks.setCaption(localizer.getMessage("lblSealedDecks"));
    }

    /* (non-Javadoc)
     * @see forge.view.home.IViewSubmenu#populate()
     */
    @Override
    public void populate() {
        PnlDisplay pnlDisplay = VHomeUI.SINGLETON_INSTANCE.getPnlDisplay();
        pnlDisplay.removeAll();
        pnlDisplay.setLayout(new MigLayout("insets 0, gap 0, wrap, ax right"));
        pnlDisplay.add(lblTitle, "w 80%!, h 40px!, gap 0 0 15px 15px, ax right");

        pnlDisplay.add(lblInfo, "w 80%!, h 30px!, gap 0 10% 20px 5px");
        pnlDisplay.add(lblDir1, "gap 0 0 0 5px");
        pnlDisplay.add(lblDir2, "gap 0 0 0 5px");
        pnlDisplay.add(lblDir3, "gap 0 0 0 20px");

        pnlDisplay.add(btnBuildDeck, "w 250px!, h 30px!, ax center, gap 0 10% 0 20px");
        pnlDisplay.add(new ItemManagerContainer(lstDecks), "w 80%!, gap 0 10% 0 0, pushy, growy");
        //pnlDisplay.add(btnStart, "gap 0 10% 50px 50px, ax center");

        final JXButtonPanel grpPanel = new JXButtonPanel();
        grpPanel.add(radSingle, "w 200px!, h 30px!");
        grpPanel.add(radAll, "w 200px!, h 30px!");
        radSingle.setSelected(true);
        grpPanel.add(cbOpponent, "w 200px!, h 30px!");
        pnlStart.setLayout(new MigLayout("insets 0, gap 0, wrap 2"));
        pnlStart.setOpaque(false);
        pnlStart.add(grpPanel, "gapright 20");
        pnlStart.add(btnStart);

        pnlDisplay.add(pnlStart, "gap 0 10% 50px 50px, ax center");

        pnlDisplay.repaintSelf();
        pnlDisplay.revalidate();
    }

    /* (non-Javadoc)
     * @see forge.view.home.IViewSubmenu#getGroup()
     */
    @Override
    public EMenuGroup getGroupEnum() {
        return EMenuGroup.SANCTIONED;
    }

    /* (non-Javadoc)
     * @see forge.gui.home.IVSubmenu#getMenuTitle()
     */
    @Override
    public String getMenuTitle() {
        return localizer.getMessage("lblSealedDeck");
    }

    /* (non-Javadoc)
     * @see forge.gui.home.IVSubmenu#getItemEnum()
     */
    @Override
    public EDocID getItemEnum() {
        return EDocID.HOME_SEALED;
    }

    /** @return {@link forge.toolbox.FLabel} */
    public FLabel getBtnDirections() {
        return this.btnDirections;
    }

    /** @return {@link forge.toolbox.FLabel} */
    public FLabel getBtnBuildDeck() {
        return this.btnBuildDeck;
    }

    /** @return {@link javax.swing.JButton} */
    public JButton getBtnStart() {
        return this.btnStart;
    }

    /** @return {@link forge.itemmanager.DeckManager} */
    public DeckManager getLstDecks() {
        return lstDecks;
    }

    public boolean isSingleSelected() {
        return radSingle.isSelected();
    }
    public JComboBox<String> getCbOpponent() { return cbOpponent; }
    public JRadioButton getRadSingle() { return radSingle; }
    public JRadioButton getRadAll() { return radAll; }


    /** */
    public void showDirections() {
        final JPanel overlay = SOverlayUtils.genericOverlay();
        final int w = overlay.getWidth();

        final String instructions = localizer.getMessage("lblSealedModeInstruction");

        // Init directions text pane
        final SkinnedTextPane tpnDirections = new SkinnedTextPane();
        tpnDirections.setOpaque(false);
        tpnDirections.setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT));
        tpnDirections.setFont(FSkin.getRelativeFont(15));
        tpnDirections.setAlignmentX(SwingConstants.CENTER);
        tpnDirections.setFocusable(false);
        tpnDirections.setEditable(false);
        tpnDirections.setBorder((Border)null);
        tpnDirections.setText(instructions);

        final StyledDocument doc = tpnDirections.getStyledDocument();
        final SimpleAttributeSet center = new SimpleAttributeSet();
        StyleConstants.setAlignment(center, StyleConstants.ALIGN_CENTER);
        doc.setParagraphAttributes(0, doc.getLength(), center, false);

        final JButton btnCloseBig = new FButton(localizer.getMessage("lblOK"));
        btnCloseBig.setBounds(new Rectangle((w / 2 - 100), 510, 200, 30));
        btnCloseBig.addActionListener(new ActionListener() { @Override
            public void actionPerformed(final ActionEvent arg0) { SOverlayUtils.hideOverlay(); } });

        final FPanel pnl = new FPanel();
        pnl.setCornerDiameter(0);
        pnl.setBackgroundTexture(FSkin.getIcon(FSkinProp.BG_TEXTURE));
        pnl.setLayout(new MigLayout("insets 0, gap 0"));
        pnl.add(tpnDirections, "w 90%!, h 90%!, gap 5% 0 5% 0");
        pnl.setBounds(new Rectangle((w / 2 - 250), 80, 500, 400));

        overlay.setLayout(null);
        overlay.add(btnCloseBig);
        overlay.add(pnl);
        SOverlayUtils.showOverlay();
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getDocumentID()
     */
    @Override
    public EDocID getDocumentID() {
        return EDocID.HOME_SEALED;
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
    public CSubmenuSealed getLayoutControl() {
        return CSubmenuSealed.SINGLETON_INSTANCE;
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
