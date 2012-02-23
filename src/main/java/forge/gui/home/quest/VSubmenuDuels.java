package forge.gui.home.quest;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;

import net.miginfocom.swing.MigLayout;
import forge.gui.home.EMenuGroup;
import forge.gui.home.IVSubmenu;
import forge.view.home.StartButton;
import forge.view.toolbox.FCheckBox;
import forge.view.toolbox.FLabel;
import forge.view.toolbox.FPanel;
import forge.view.toolbox.FScrollPane;
import forge.view.toolbox.FSkin;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public enum VSubmenuDuels implements IVSubmenu {
    /** */
    SINGLETON_INSTANCE;

    //========== INSTANTIATION
    private final JPanel pnl        = new JPanel();
    private final JPanel pnlDuels   = new JPanel();
    private final JButton btnStart  = new StartButton();
    private final JComboBox cbxPet  = new JComboBox();
    private final JCheckBox cbPlant = new FCheckBox("Summon Plant");
    private final JCheckBox cbZep   = new FCheckBox("Launch Zeppelin");

    private final FLabel lblLife      = new FLabel.Builder()
        .icon(FSkin.getIcon(FSkin.QuestIcons.ICO_LIFE)).build();
    private final FLabel lblCredits   = new FLabel.Builder()
        .icon(FSkin.getIcon(FSkin.QuestIcons.ICO_COINSTACK)).build();
    private final FLabel lblWins      = new FLabel.Builder()
        .icon(FSkin.getIcon(FSkin.QuestIcons.ICO_PLUS)).build();
    private final FLabel lblLosses    = new FLabel.Builder()
        .icon(FSkin.getIcon(FSkin.QuestIcons.ICO_MINUS)).build();
    private final FLabel lblNextChallengeInWins = new FLabel.Builder()
        .text("No challenges available.").build();
    private final FLabel lblWinStreak = new FLabel.Builder()
        .build();
    private final FLabel lblTitle     = new FLabel.Builder()
        .text("Title Hasn't Been Set Yet").fontAlign(SwingConstants.CENTER)
        .fontScaleAuto(false).fontSize(15).build();
    private final FLabel btnCurrentDeck = new FLabel.Builder()
        .opaque(true).hoverable(true).build();
    private final FLabel btnBazaar = new FLabel.Builder()
        .selectable(true).opaque(true).hoverable(true).text("Bazaar")
        .fontScaleAuto(false).fontSize(14).tooltip("Peruse the Bazaar").build();
    private final FLabel btnSpellShop = new FLabel.Builder()
        .opaque(true).hoverable(true).text("Spell Shop")
        .fontScaleAuto(false).fontSize(14).tooltip("Travel to the Spell Shop").build();

    /* (non-Javadoc)
     * @see forge.view.home.IViewSubmenu#getPanel()
     */
    @Override
    public JPanel getPanel() {
        return pnl;
    }

    /* (non-Javadoc)
     * @see forge.view.home.IViewSubmenu#getGroup()
     */
    @Override
    public EMenuGroup getGroup() {
        return null;
    }

    /* (non-Javadoc)
     * @see forge.view.home.IViewSubmenu#populate()
     */
    @Override
    public void populate() {
        final FPanel pnlTitle = new FPanel();
        pnlTitle.setLayout(new MigLayout("insets 0, gap 0"));
        pnlTitle.setBackground(FSkin.getColor(FSkin.Colors.CLR_THEME2));
        pnlTitle.add(lblTitle, "w 100%, h 100%, gap 0 0 0 0");

        final JPanel pnlStats = new JPanel();
        pnlStats.setOpaque(false);
        populateStats(pnlStats);

        final FScrollPane scrDuels = new FScrollPane(pnlDuels,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        pnlDuels.setOpaque(false);
        pnlDuels.setLayout(new MigLayout("insets 0, gap 0, wrap"));

        pnl.setOpaque(false);
        pnl.setLayout(new MigLayout("insets 0, gap 0, wrap"));
        pnl.add(pnlTitle, "w 94%!, h 30px!, gap 3% 0 20px 20px");
        pnl.add(pnlStats, "w 94%!, gap 3% 0 0 20px");
        pnl.add(scrDuels, "w 94%!, h 50%!, gap 3% 0 0 10px");
        pnl.add(cbxPet, "ax center, gap 3% 0 5px 0");
        pnl.add(cbPlant, "ax center, gap 3% 0 5px 0");
        pnl.add(cbZep, "ax center, gap 3% 0 5px 0");
        pnl.add(btnStart, "ax center, gap 3% 0 5px 10px");
    }

    /** @return {@link javax.swing.JLabel} */
    public JLabel getLblTitle() {
        return lblTitle;
    }
    /** @return {@link javax.swing.FLabel} */
    public FLabel getLblLife() {
        return lblLife;
    }

    /** @return {@link javax.swing.FLabel} */
    public FLabel getLblCredits() {
        return lblCredits;
    }

    /** @return {@link javax.swing.FLabel} */
    public FLabel getLblWins() {
        return lblWins;
    }

    /** @return {@link javax.swing.FLabel} */
    public FLabel getLblLosses() {
        return lblLosses;
    }

    /** @return {@link javax.swing.FLabel} */
    public FLabel getLblNextChallengeInWins() {
        return lblNextChallengeInWins;
    }

    /** @return {@link javax.swing.FLabel} */
    public FLabel getLblWinStreak() {
        return lblWinStreak;
    }

    /** @return {@link forge.view.toolbox.FLabel} */
    public FLabel getBtnCurrentDeck() {
        return btnCurrentDeck;
    }

    /** @return {@link forge.view.toolbox.FLabel} */
    public FLabel getBtnBazaar() {
        return btnBazaar;
    }

    /** @return {@link forge.view.toolbox.FLabel} */
    public FLabel getBtnSpellShop() {
        return btnSpellShop;
    }

    /** @return {@link javax.swing.JCheckBox} */
    public JCheckBox getCbPlant() {
        return cbPlant;
    }

    /** @return {@link javax.swing.JCheckBox} */
    public JCheckBox getCbZep() {
        return cbZep;
    }

    /** @return {@link javax.swing.JComboBox} */
    public JComboBox getCbxPet() {
        return cbxPet;
    }

    /** @return {@link javax.swing.JButton} */
    public JButton getBtnStart() {
        return btnStart;
    }

    /** Stats panel has different layout depending on classic/fantasy quest. */
    private void populateStats(final JPanel pnl0) {
        pnl0.removeAll();

        if (true) { //(AllZone.getQuestData().isFantasy()) {
            pnl0.setLayout(new MigLayout("insets 0, gap 0"));
            pnl0.add(btnBazaar,     "w 15%!, h 70px!, gap 0 4% 10px 10px, span 1 2");
            pnl0.add(lblWins,       "w 30%!, h 25px!, gap 0 2% 12px 0");
            pnl0.add(lblLosses,     "w 30%!, h 25px!, gap 0 4% 12px 0");
            pnl0.add(btnSpellShop,   "w 14.5%!, h 70px!, gap 0 0 10px 10px, span 1 2, wrap");
            pnl0.add(lblCredits,    "w 30%!, h 25px!, gap 0 2% 0 0");
            pnl0.add(lblLife,       "w 30%!, h 25px!, gap 0 4% 0 0 0, wrap");
            pnl0.add(lblWinStreak, "h 20px!, align center, span 4 1, wrap");
            pnl0.add(lblNextChallengeInWins, "h 20px!, align center, span 4 1, wrap");
            pnl0.add(btnCurrentDeck, "w 40%!, h 26px!, align center, span 4 1, gap 0 0 0 5px");
        }
        else {
            pnl0.setLayout(new MigLayout("insets 0, gap 0, align center"));
            pnl0.add(lblWins,       "w 150px!, h 25px!, gap 0 50px 5px 5px, align center");
            pnl0.add(lblCredits,    "w 150px!, h 25px!, gap 0 0 5px 5px, align center, wrap");
            pnl0.add(lblLosses,     "w 150px!, h 25px!, gap 0 50px 0 5px, align center");
            pnl0.add(btnSpellShop,  "w 150px!, h 25px!, gap 0 0 0 5px, align center, wrap");
            pnl0.add(lblWinStreak, "h 20px!, align center, span 4 1, wrap");
            pnl0.add(lblNextChallengeInWins, "h 20px!, align center, span 4 1, gap 0 0 10px 5px, wrap");
            pnl0.add(btnCurrentDeck, "w 40%!, h 26px!, align center, span 4 1, gap 0 0 0 5px");
        }
    }
}
