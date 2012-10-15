package forge.gui.home.quest;

import java.awt.Color;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;

import net.miginfocom.swing.MigLayout;
import forge.gui.framework.DragCell;
import forge.gui.framework.DragTab;
import forge.gui.framework.EDocID;
import forge.gui.framework.ICDoc;
import forge.gui.framework.IVDoc;
import forge.gui.home.EMenuGroup;
import forge.gui.home.IVSubmenu;
import forge.gui.home.StartButton;
import forge.gui.home.VHomeUI;
import forge.gui.toolbox.FCheckBox;
import forge.gui.toolbox.FLabel;
import forge.gui.toolbox.FScrollPane;
import forge.gui.toolbox.FSkin;

/**
 * Assembles Swing components of quest challenges submenu singleton.
 *
 * <br><br><i>(V at beginning of class name denotes a view class.)</i>
 */
public enum VSubmenuChallenges implements IVSubmenu, IStatsAndPet, IVDoc {
    /** */
    SINGLETON_INSTANCE;

    // Fields used with interface IVDoc
    private DragCell parentCell;
    private final DragTab tab = new DragTab("Quest Challenges");

    //========== INSTANTIATION
    private final JPanel pnlChallenges   = new JPanel();
    private final JPanel pnlStats   = new JPanel();
    private final JPanel pnlStart   = new JPanel();

    private final FScrollPane scrChallenges = new FScrollPane(pnlChallenges,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

    private final JButton btnStart  = new StartButton();
    private final JComboBox cbxPet  = new JComboBox();
    private final JCheckBox cbPlant = new FCheckBox("Summon Plant");
    private final JLabel lblZep   = new FLabel.Builder().text("<html>Launch<br>Zeppelin</html>")
            .hoverable(true).icon(FSkin.getIcon(FSkin.QuestIcons.ICO_ZEP))
            .fontSize(16).build();

    private final FLabel lblLife      = new FLabel.Builder()
        .icon(FSkin.getIcon(FSkin.QuestIcons.ICO_LIFE))
        .fontSize(15).build();
    private final FLabel lblCredits   = new FLabel.Builder()
        .icon(FSkin.getIcon(FSkin.QuestIcons.ICO_COINSTACK))
        .fontSize(15).build();
    private final FLabel lblWins      = new FLabel.Builder()
        .icon(FSkin.getIcon(FSkin.QuestIcons.ICO_PLUS))
        .fontSize(15).build();
    private final FLabel lblLosses    = new FLabel.Builder()
        .icon(FSkin.getIcon(FSkin.QuestIcons.ICO_MINUS))
        .fontSize(15).build();
    private final FLabel lblWinStreak = new FLabel.Builder()
        .icon(FSkin.getIcon(FSkin.QuestIcons.ICO_PLUSPLUS))
        .fontSize(15).build();
    private final FLabel lblTitle     = new FLabel.Builder()
        .text("Title Hasn't Been Set Yet").fontAlign(SwingConstants.CENTER)
        .opaque(true).fontSize(16).build();
    private final FLabel lblNextChallengeInWins = new FLabel.Builder()
        .fontSize(15).build();
    private final FLabel btnCurrentDeck = new FLabel.Builder()
        .fontSize(15).opaque(true).hoverable(true).build();
    private final FLabel btnBazaar = new FLabel.Builder()
        .opaque(true).hoverable(true).text("Bazaar")
        .fontSize(14).tooltip("Peruse the Bazaar").build();
    private final FLabel btnSpellShop = new FLabel.Builder()
        .opaque(true).hoverable(true).text("Spell Shop")
        .fontSize(14).tooltip("Travel to the Spell Shop").build();

    /**
     * Constructor.
     */
    private VSubmenuChallenges() {
        lblTitle.setBackground(FSkin.getColor(FSkin.Colors.CLR_THEME2));

        populateStats();
        populateStart();
        btnStart.setEnabled(false);

        scrChallenges.setBorder(null);
        pnlChallenges.setOpaque(false);
        pnlChallenges.setLayout(new MigLayout("insets 0, gap 0, wrap"));
    }

    /* (non-Javadoc)
     * @see forge.view.home.IViewSubmenu#getGroup()
     */
    @Override
    public EMenuGroup getGroupEnum() {
        return EMenuGroup.QUEST;
    }

    /* (non-Javadoc)
     * @see forge.gui.home.IVSubmenu#getMenuTitle()
     */
    @Override
    public String getMenuTitle() {
        return "Challenges";
    }

    /* (non-Javadoc)
     * @see forge.gui.home.IVSubmenu#getMenuName()
     */
    @Override
    public EDocID getItemEnum() {
        return EDocID.HOME_QUESTCHALLENGES;
    }

    /* (non-Javadoc)
     * @see forge.view.home.IViewSubmenu#populate()
     */
    @Override
    public void populate() {
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().removeAll();
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().setLayout(new MigLayout("insets 0, gap 0"));

        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().removeAll();
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().setOpaque(false);
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().setLayout(new MigLayout("insets 0, gap 0, wrap"));
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(lblTitle, "w 98%!, h 30px!, gap 1% 0 15px 15px");
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(pnlStats, "w 98%!, gap 1% 0 0 20px");
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(scrChallenges, "w 98%!, pushy, growy, gap 1% 0 0 0");
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(pnlStart, "w 98%, gap 1% 0 20px 50px");

        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().repaintSelf();
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().revalidate();
    }

    @Override
    public void updateCurrentDeckStatus() {
        final JLabel btnCurrentDeck = VSubmenuChallenges.SINGLETON_INSTANCE.getBtnCurrentDeck();
        if (SSubmenuQuestUtil.getCurrentDeck() == null) {
            btnCurrentDeck.setBackground(Color.red.darker());
            btnCurrentDeck.setText("  Build, then select a deck in the \"Decks\" submenu.  ");
        }
        else {
            btnCurrentDeck.setBackground(FSkin.getColor(FSkin.Colors.CLR_INACTIVE));
            btnCurrentDeck.setText("Current deck: "
                    + SSubmenuQuestUtil.getCurrentDeck().getName());
        }
    }

    /** @return {@link javax.swing.JPanel} */
    public JPanel getPnlChallenges() {
        return pnlChallenges;
    }

    /** @return {@link forge.gui.toolbox.FPanel} */
    public JPanel getPnlStats() {
        return pnlStats;
    }

    /** @return {@link forge.gui.toolbox.FPanel} */
    public JPanel getPnlStart() {
        return pnlStart;
    }

    /** @return {@link javax.swing.JLabel} */
    public JLabel getLblTitle() {
        return lblTitle;
    }

    @Override
    public FLabel getLblLife() {
        return lblLife;
    }

    @Override
    public FLabel getLblCredits() {
        return lblCredits;
    }

    @Override
    public FLabel getLblWins() {
        return lblWins;
    }

    @Override
    public FLabel getLblLosses() {
        return lblLosses;
    }

    @Override
    public FLabel getLblNextChallengeInWins() {
        return lblNextChallengeInWins;
    }

    @Override
    public FLabel getLblWinStreak() {
        return lblWinStreak;
    }

    /** @return {@link forge.gui.toolbox.FLabel} */
    public FLabel getBtnCurrentDeck() {
        return btnCurrentDeck;
    }

    @Override
    public FLabel getBtnBazaar() {
        return btnBazaar;
    }

    @Override
    public FLabel getBtnSpellShop() {
        return btnSpellShop;
    }

    @Override
    public JCheckBox getCbPlant() {
        return cbPlant;
    }

    @Override
    public JLabel getLblZep() {
        return lblZep;
    }

    @Override
    public JComboBox getCbxPet() {
        return cbxPet;
    }

    /** @return {@link javax.swing.JButton} */
    public JButton getBtnStart() {
        return btnStart;
    }

    private void populateStats() {
        final String constraints = "w 23%!, h 35px!, gap 0 0 5px 5px";
        pnlStats.removeAll();
        pnlStats.setOpaque(false);
        pnlStats.setLayout(new MigLayout("insets 0, gap 0, hidemode 0"));
        pnlStats.add(btnSpellShop, constraints);
        pnlStats.add(lblWins, constraints);
        pnlStats.add(lblLosses, constraints);
        pnlStats.add(lblCredits, constraints + ", wrap");

        pnlStats.add(btnBazaar, constraints);
        pnlStats.add(lblWinStreak, "w 48%!, h 35px!, gap 1% 1% 5px 5px, span 2 1");
        pnlStats.add(lblLife, constraints + ", wrap");

        pnlStats.add(lblNextChallengeInWins, "span 4 1, h 20px!, gap 0 0 5px 5px, ax center, wrap");
        pnlStats.add(btnCurrentDeck, "span 4 1, w 350px!, h 30px!, gap 0 0 0 5px, ax center");
    }

    private void populateStart() {
        pnlStart.removeAll();
        pnlStart.setOpaque(false);
        pnlStart.setLayout(new MigLayout("insets 0, gap 0, align center, hidemode 3"));

        pnlStart.add(cbxPet, "h 20px!, ax center, gap 0 10px 10px 0");
        pnlStart.add(btnStart, "ax center, span 1 2");
        pnlStart.add(lblZep, "w 130px!, h 80px!, ax center, span 1 2, gap 10px 0 0 0");
        pnlStart.add(cbPlant, "newline, h 30px!, gap 0 10px 10px 10px");
    }

    //========== Overridden from IVDoc

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getDocumentID()
     */
    @Override
    public EDocID getDocumentID() {
        return EDocID.HOME_QUESTCHALLENGES;
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
    public ICDoc getLayoutControl() {
        return CSubmenuChallenges.SINGLETON_INSTANCE;
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
