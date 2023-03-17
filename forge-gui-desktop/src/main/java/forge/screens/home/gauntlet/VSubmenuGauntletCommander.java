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
 * Assembles Swing components of "commander gauntlet" submenu singleton.
 *
 * <br><br><i>(V at beginning of class name denotes a view class.)</i>
 *
 */
public enum VSubmenuGauntletCommander implements IVSubmenu<CSubmenuGauntletCommander> {
    /** */
    SINGLETON_INSTANCE;
    final Localizer localizer = Localizer.getInstance();
    // Fields used with interface IVDoc
    private DragCell parentCell;
    private final DragTab tab = new DragTab(localizer.getMessage("lblCommanderGauntlet"));

    // Other fields
    private final FPanel pnlOptions = new FPanel(new MigLayout("insets 0, gap 0, wrap"));
    private final FLabel lblTitle = new FLabel.Builder()
            .text(localizer.getMessage("lblCommanderGauntletBuilder")).fontAlign(SwingConstants.CENTER)
            .opaque(true).fontSize(16).build();

    private final FLabel lblDecklist = new FLabel.Builder()
            .text(localizer.getMessage("lblDecklist"))
            .fontSize(12).build();

    private final SkinnedSlider sliOpponents = new SkinnedSlider(SwingConstants.HORIZONTAL, 5, 50, 20);
    //private SkinnedSlider sliGamesPerMatch = new SkinnedSlider(JSlider.HORIZONTAL, 1, 7, 3);

    private final JCheckBox boxRandomCommanderDecks = new FCheckBox(DeckType.RANDOM_COMMANDER_DECK.toString());
    private final JCheckBox boxPreconCommanderDecks = new FCheckBox(DeckType.PRECON_COMMANDER_DECK.toString());
    private final JCheckBox boxCommanderDecks = new FCheckBox(DeckType.COMMANDER_DECK.toString());

    private final FDeckChooser lstDecks = new FDeckChooser(null, false, GameType.CommanderGauntlet, true);

    private final FLabel lblOptions = new FLabel.Builder().fontSize(16)
            .fontStyle(Font.BOLD).text(localizer.getMessage("lblOptions")).fontAlign(SwingConstants.CENTER).build();

    private final FLabel lblDesc1 = new FLabel.Builder()
            .text(localizer.getMessage("lblMatchesperGauntlet")).fontStyle(Font.ITALIC).build();

    private final FLabel lblDesc3 = new FLabel.Builder()
            .text(localizer.getMessage("lblAllowedDeckTypes")).fontStyle(Font.ITALIC).build();

    private final FLabel lblDesc = new FLabel.Builder().text(localizer.getMessage("lblAutosaveInf")).fontSize(12).build();

    private final StartButton btnStart  = new StartButton();

    VSubmenuGauntletCommander() {
        lblTitle.setBackground(FSkin.getColor(FSkin.Colors.CLR_THEME2));

        boxRandomCommanderDecks.setSelected(true);
        boxPreconCommanderDecks.setSelected(true);
        boxCommanderDecks.setSelected(true);

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
        pnlOptions.add(boxRandomCommanderDecks, "w 96%!, h 30px!, gap 2% 0 0 5px");
        pnlOptions.add(boxPreconCommanderDecks, "w 96%!, h 30px!, gap 2% 0 0 5px");
        pnlOptions.add(boxCommanderDecks, "w 96%!, h 30px!, gap 2% 0 0 5px");
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
        return localizer.getMessage("lblCommanderGauntlet");
    }

    /* (non-Javadoc)
     * @see forge.gui.home.IVSubmenu#getItemEnum()
     */
    @Override
    public EDocID getItemEnum() {
        return EDocID.HOME_GAUNTLETCOMMANDER;
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
    public JCheckBox getBoxRandomCommanderDecks() {
        return boxRandomCommanderDecks;
    }

    /** @return {@link javax.swing.JCheckBox} */
    public JCheckBox getBoxPreconCommanderDecks() {
        return boxPreconCommanderDecks;
    }

    /** @return {@link javax.swing.JCheckBox} */
    public JCheckBox getBoxCommanderDecks() {
        return boxCommanderDecks;
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
        return EDocID.HOME_GAUNTLETCOMMANDER;
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
    public CSubmenuGauntletCommander getLayoutControl() {
        return CSubmenuGauntletCommander.SINGLETON_INSTANCE;
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
