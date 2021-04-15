package forge.screens.home.gauntlet;

import java.awt.Font;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JSlider;
import javax.swing.SwingConstants;

import forge.deck.DeckType;
import forge.deckchooser.FDeckChooser;
import forge.game.GameType;
import forge.gui.framework.DragCell;
import forge.gui.framework.DragTab;
import forge.gui.framework.EDocID;
import forge.model.FModel;
import forge.screens.home.EMenuGroup;
import forge.screens.home.IVSubmenu;
import forge.screens.home.StartButton;
import forge.screens.home.VHomeUI;
import forge.toolbox.FCheckBox;
import forge.toolbox.FLabel;
import forge.toolbox.FPanel;
import forge.toolbox.FSkin;
import forge.toolbox.FSkin.SkinnedSlider;
import forge.util.Localizer;
import net.miginfocom.swing.MigLayout;

/** 
 * Assembles Swing components of "quick gauntlet" submenu singleton.
 *
 * <br><br><i>(V at beginning of class name denotes a view class.)</i>
 *
 */
public enum VSubmenuGauntletQuick implements IVSubmenu<CSubmenuGauntletQuick> {
    /** */
    SINGLETON_INSTANCE;
    final Localizer localizer = Localizer.getInstance();
    // Fields used with interface IVDoc
    private DragCell parentCell;
    private final DragTab tab = new DragTab(localizer.getMessage("lblQuickGauntlets"));

    // Other fields
    private final FPanel pnlOptions = new FPanel(new MigLayout("insets 0, gap 0, wrap"));
    private final FLabel lblTitle = new FLabel.Builder()
        .text(localizer.getMessage("lblQuickGauntletBuilder")).fontAlign(SwingConstants.CENTER)
        .opaque(true).fontSize(16).build();

    private final FLabel lblDecklist = new FLabel.Builder()
        .text(localizer.getMessage("lblDecklist"))
        .fontSize(12).build();

    private final SkinnedSlider sliOpponents = new SkinnedSlider(SwingConstants.HORIZONTAL, 5, 50, 20);
    //private SkinnedSlider sliGamesPerMatch = new SkinnedSlider(JSlider.HORIZONTAL, 1, 7, 3);

    private final JCheckBox boxUserDecks = new FCheckBox(DeckType.CUSTOM_DECK.toString());
    private final JCheckBox boxPreconDecks = new FCheckBox(DeckType.PRECONSTRUCTED_DECK.toString());
    private final JCheckBox boxQuestDecks = new FCheckBox(DeckType.QUEST_OPPONENT_DECK.toString());
    private final JCheckBox boxColorDecks = new FCheckBox(DeckType.COLOR_DECK.toString());
    private final JCheckBox boxStandardColorDecks = new FCheckBox(DeckType.STANDARD_COLOR_DECK.toString());
    private final JCheckBox boxStandardCardgenDecks = new FCheckBox(DeckType.STANDARD_CARDGEN_DECK.toString());
    private final JCheckBox boxPioneerCardgenDecks = new FCheckBox(DeckType.PIONEER_CARDGEN_DECK.toString());
    private final JCheckBox boxHistoricCardgenDecks = new FCheckBox(DeckType.HISTORIC_CARDGEN_DECK.toString());
    private final JCheckBox boxModernCardgenDecks = new FCheckBox(DeckType.MODERN_CARDGEN_DECK.toString());
    private final JCheckBox boxLegacyCardgenDecks = new FCheckBox(DeckType.LEGACY_CARDGEN_DECK.toString());
    private final JCheckBox boxVintageCardgenDecks = new FCheckBox(DeckType.VINTAGE_CARDGEN_DECK.toString());
    private final JCheckBox boxModernColorDecks = new FCheckBox(DeckType.MODERN_COLOR_DECK.toString());
    private final JCheckBox boxThemeDecks = new FCheckBox(DeckType.THEME_DECK.toString());

    private final FDeckChooser lstDecks = new FDeckChooser(null, false, GameType.Constructed, false);

    private final FLabel lblOptions = new FLabel.Builder().fontSize(16)
            .fontStyle(Font.BOLD).text(localizer.getMessage("lblOptions")).fontAlign(SwingConstants.CENTER).build();

    private final FLabel lblDesc1 = new FLabel.Builder()
            .text(localizer.getMessage("lblMatchesperGauntlet")).fontStyle(Font.ITALIC).build();

    private final FLabel lblDesc3 = new FLabel.Builder()
            .text(localizer.getMessage("lblAllowedDeckTypes")).fontStyle(Font.ITALIC).build();

    private final FLabel lblDesc = new FLabel.Builder().text(localizer.getMessage("lblAutosaveInf")).fontSize(12).build();

    private final StartButton btnStart  = new StartButton();

    VSubmenuGauntletQuick() {
        lblTitle.setBackground(FSkin.getColor(FSkin.Colors.CLR_THEME2));

        boxUserDecks.setSelected(true);
        boxPreconDecks.setSelected(true);
        boxQuestDecks.setSelected(true);
        boxThemeDecks.setSelected(true);
        boxColorDecks.setSelected(true);
        boxStandardColorDecks.setSelected(true);
        if(FModel.isdeckGenMatrixLoaded()) {
            boxStandardCardgenDecks.setSelected(true);
            boxPioneerCardgenDecks.setSelected(true);
            boxModernCardgenDecks.setSelected(true);
            boxLegacyCardgenDecks.setSelected(true);
            boxVintageCardgenDecks.setSelected(true);
        }else{
            boxStandardCardgenDecks.setSelected(false);
            boxPioneerCardgenDecks.setSelected(false);
            boxModernCardgenDecks.setSelected(false);
            boxLegacyCardgenDecks.setSelected(false);
            boxVintageCardgenDecks.setSelected(false);
        }
        boxModernColorDecks.setSelected(true);

        sliOpponents.setMajorTickSpacing(5);
        sliOpponents.setMinorTickSpacing(0);
        sliOpponents.setPaintTicks(false);
        sliOpponents.setPaintLabels(true);
        sliOpponents.setSnapToTicks(true);
        sliOpponents.setOpaque(false);
        sliOpponents.setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT));
        sliOpponents.setFont(FSkin.getFont());

        pnlOptions.setBackground(FSkin.getColor(FSkin.Colors.CLR_THEME2));
        pnlOptions.add(lblOptions, "h 30px!, w 96%!, gap 2% 0 0 5px");
        pnlOptions.add(sliOpponents, "h 40px!, w 96%!, gap 2% 0 0 5px, ax center");
        pnlOptions.add(lblDesc1, "w 96%!, gap 2% 0 0 20px");
        pnlOptions.add(lblDesc3, "w 96%!, gap 2% 0 0 0");
        pnlOptions.setCornerDiameter(0);
        pnlOptions.add(boxUserDecks, "w 96%!, h 30px!, gap 2% 0 0 5px");
        pnlOptions.add(boxPreconDecks, "w 96%!, h 30px!, gap 2% 0 0 5px");
        pnlOptions.add(boxQuestDecks, "w 96%!, h 30px!, gap 2% 0 0 5px");
        pnlOptions.add(boxThemeDecks, "w 96%!, h 30px!, gap 2% 0 0 5px");
        pnlOptions.add(boxColorDecks, "w 96%!, h 30px!, gap 2% 0 0 5px");
        if(FModel.isdeckGenMatrixLoaded()) {
            pnlOptions.add(boxStandardCardgenDecks, "w 96%!, h 30px!, gap 2% 0 0 5px");
            pnlOptions.add(boxPioneerCardgenDecks, "w 96%!, h 30px!, gap 2% 0 0 5px");
            pnlOptions.add(boxModernCardgenDecks, "w 96%!, h 30px!, gap 2% 0 0 5px");
            pnlOptions.add(boxLegacyCardgenDecks, "w 96%!, h 30px!, gap 2% 0 0 5px");
            pnlOptions.add(boxVintageCardgenDecks, "w 96%!, h 30px!, gap 2% 0 0 5px");
        }
        pnlOptions.add(boxStandardColorDecks, "w 96%!, h 30px!, gap 2% 0 0 5px");
        pnlOptions.add(boxModernColorDecks, "w 96%!, h 30px!, gap 2% 0 0 0");
    }

    public void updateDeckPanel() {
        lstDecks.restoreSavedState();
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
        return localizer.getMessage("lblQuickGauntlet");
    }

    /* (non-Javadoc)
     * @see forge.gui.home.IVSubmenu#getItemEnum()
     */
    @Override
    public EDocID getItemEnum() {
        return EDocID.HOME_GAUNTLETQUICK;
    }

    /* (non-Javadoc)
     * @see forge.gui.home.IVSubmenu#populate()
     */
    @Override
    public void populate() {
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().removeAll();
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().setLayout(new MigLayout("insets 0, gap 0, wrap 2"));

        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(lblTitle, "w 98%!, h 30px!, gap 1% 0 15px 15px, span 2");
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(lblDesc, "ax center, gap 0 0 0 5px, span 2");
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(lblDecklist, "ax center, gap 0 0 0 15px, span 2");
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(pnlOptions, "w 40%!, gap 1% 1% 0 0, pushy, growy");
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(lstDecks, "w 57%!, pushy, growy");
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(btnStart, "w 98%!, ax center, gap 1% 0 20px 20px, span 2");

        getLstDecks().populate();
        
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().repaintSelf();
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().revalidate();
    }

    /** @return {@link javax.swing.JList} */
    public FDeckChooser getLstDecks() {
        return this.lstDecks;
    }

    /** @return {@link javax.swing.JCheckBox} */
    public JCheckBox getBoxUserDecks() {
        return boxUserDecks;
    }

    /** @return {@link javax.swing.JCheckBox} */
    public JCheckBox getBoxPreconDecks() {
        return boxPreconDecks;
    }

    /** @return {@link javax.swing.JCheckBox} */
    public JCheckBox getBoxQuestDecks() {
        return boxQuestDecks;
    }

    /** @return {@link javax.swing.JCheckBox} */
    public JCheckBox getBoxColorDecks() {
        return boxColorDecks;
    }

    /** @return {@link javax.swing.JCheckBox} */
    public JCheckBox getBoxStandardColorDecks() {
        return boxStandardColorDecks;
    }

    /** @return {@link javax.swing.JCheckBox} */
    public JCheckBox getBoxModernColorDecks() {
        return boxModernColorDecks;
    }

    /** @return {@link javax.swing.JCheckBox} */
    public JCheckBox getBoxStandardGenDecks() {
        return boxStandardCardgenDecks;
    }
    /** @return {@link javax.swing.JCheckBox} */
    public JCheckBox getBoxModernGenDecks() {
        return boxModernCardgenDecks;
    }
    public JCheckBox getBoxPioneerGenDecks() {
        return boxPioneerCardgenDecks;
    }
    public JCheckBox getBoxHistoricGenDecks() {
        return boxHistoricCardgenDecks;
    }
    public JCheckBox getBoxLegacyGenDecks() {
        return boxLegacyCardgenDecks;
    }
    public JCheckBox getBoxVintageGenDecks() {
        return boxVintageCardgenDecks;
    }

    /** @return {@link javax.swing.JCheckBox} */
    public JCheckBox getBoxThemeDecks() {
        return boxThemeDecks;
    }
    /** @return {@link javax.swing.JSlider} */
    public JSlider getSliOpponents() {
        return this.sliOpponents;
    }

    /** @return {@link javax.swing.JButton} */
    public JButton getBtnStart() {
        return this.btnStart;
    }

    //========== Overridden from IVDoc

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getDocumentID()
     */
    @Override
    public EDocID getDocumentID() {
        return EDocID.HOME_GAUNTLETQUICK;
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
    public CSubmenuGauntletQuick getLayoutControl() {
        return CSubmenuGauntletQuick.SINGLETON_INSTANCE;
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
