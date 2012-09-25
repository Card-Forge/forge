package forge.gui.home.quest;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import net.miginfocom.swing.MigLayout;
import forge.Singletons;
import forge.gui.framework.DragCell;
import forge.gui.framework.DragTab;
import forge.gui.framework.EDocID;
import forge.gui.framework.ICDoc;
import forge.gui.home.EMenuGroup;
import forge.gui.home.IVSubmenu;
import forge.gui.toolbox.FLabel;
import forge.gui.toolbox.FPanel;
import forge.gui.toolbox.FScrollPane;
import forge.gui.toolbox.FSkin;
import forge.quest.data.QuestPreferences;
import forge.quest.data.QuestPreferences.QPref;

/** 
 * Assembles Swing components of quest preferences submenu singleton.
 *
 * <br><br><i>(V at beginning of class name denotes a view class.)</i>
 *
 */
public enum VSubmenuQuestPrefs implements IVSubmenu {
    /** */
    SINGLETON_INSTANCE;

    // Fields used with interface IVDoc
    private DragCell parentCell;
    private final DragTab tab = new DragTab("Quest Preferences");

    /** */
    private final JPanel pnlContent = new JPanel();
    private final FScrollPane scrContent = new FScrollPane(pnlContent, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

    private final JPanel pnlRewards = new JPanel();
    private final JPanel pnlDifficulty = new JPanel();
    private final JPanel pnlBooster = new JPanel();
    private final JPanel pnlShop = new JPanel();

    private final FLabel lblErrRewards = new FLabel.Builder().text("Rewards Error").fontStyle(Font.BOLD).build();
    private final FLabel lblErrDifficulty = new FLabel.Builder().text("Difficulty Error").fontStyle(Font.BOLD).build();
    private final FLabel lblErrBooster = new FLabel.Builder().text("Booster Error").fontStyle(Font.BOLD).build();
    private final FLabel lblErrShop = new FLabel.Builder().text("Shop Error").fontStyle(Font.BOLD).build();

    private final QuestPreferences prefs = Singletons.getModel().getQuestPreferences();

    /** */
    public enum QuestPreferencesErrType { /** */
        REWARDS, /** */
        DIFFICULTY, /** */
        BOOSTER, /** */
        SHOP
    }

    /**
     * Constructor.
     */
    private VSubmenuQuestPrefs() {
        pnlContent.setOpaque(false);
        pnlContent.setLayout(new MigLayout("insets 0, gap 0, wrap"));

        lblErrRewards.setForeground(Color.red);
        lblErrDifficulty.setForeground(Color.red);
        lblErrBooster.setForeground(Color.red);
        lblErrShop.setForeground(Color.red);

        // Rewards panel
        final FPanel pnlTitleRewards = new FPanel();
        pnlTitleRewards.setLayout(new MigLayout("insets 0, align center"));
        pnlTitleRewards.setBackground(FSkin.getColor(FSkin.Colors.CLR_THEME2));
        pnlTitleRewards.add(new FLabel.Builder().text("Rewards")
                .icon(FSkin.getIcon(FSkin.QuestIcons.ICO_COIN))
                .fontSize(16).build(), "h 95%!, gap 0 0 2.5% 0");

        pnlContent.add(pnlTitleRewards, "w 96%!, h 36px!, gap 2% 0 10px 20px");
        pnlContent.add(pnlRewards, "w 96%!, gap 2% 0 10px 20px");
        populateRewards();

        // Booster panel
        final FPanel pnlTitleBooster = new FPanel();
        pnlTitleBooster.setLayout(new MigLayout("insets 0, align center"));
        pnlTitleBooster.setBackground(FSkin.getColor(FSkin.Colors.CLR_THEME2));
        pnlTitleBooster.add(new FLabel.Builder().text("Booster Pack Ratios")
                .icon(FSkin.getIcon(FSkin.QuestIcons.ICO_BOOK))
                .fontSize(16).build(), "h 95%!, gap 0 0 2.5% 0");
        pnlContent.add(pnlTitleBooster, "w 96%!, h 36px!, gap 2% 0 10px 10px");
        pnlContent.add(pnlBooster, "w 96%!, gap 2% 0 10px 20px");
        populateBooster();

        // Difficulty table panel
        final FPanel pnlTitleDifficulty = new FPanel();
        pnlTitleDifficulty.setLayout(new MigLayout("insets 0, align center"));
        pnlTitleDifficulty.setBackground(FSkin.getColor(FSkin.Colors.CLR_THEME2));
        pnlTitleDifficulty.add(new FLabel.Builder().text("Difficulty Adjustments")
                .icon(FSkin.getIcon(FSkin.QuestIcons.ICO_NOTES))
                .fontSize(16).build(), "h 95%!, gap 0 0 2.5% 0");
        pnlContent.add(pnlTitleDifficulty, "w 96%!, h 36px!, gap 2% 0 10px 10px");
        pnlContent.add(pnlDifficulty, "w 96%!, gap 2% 0 10px 20px");
        populateDifficulty();

        // Shop panel
        final FPanel pnlTitleShop = new FPanel();
        pnlTitleShop.setLayout(new MigLayout("insets 0, align center"));
        pnlTitleShop.setBackground(FSkin.getColor(FSkin.Colors.CLR_THEME2));
        pnlTitleShop.add(new FLabel.Builder().text("Shop Preferences")
                .icon(FSkin.getIcon(FSkin.QuestIcons.ICO_COIN))
                .fontSize(16).build(), "h 95%!, gap 0 0 2.5% 0");
        pnlContent.add(pnlTitleShop, "w 96%!, h 36px!, gap 2% 0 10px 10px");
        pnlContent.add(pnlShop, "w 96%!, gap 2% 0 10px 20px");
        populateShop();

        scrContent.setBorder(null);

    }

    /* (non-Javadoc)
     * @see forge.view.home.IViewSubmenu#populate()
     */
    @Override
    public void populate() {
        CSubmenuQuestPrefs.resetErrors();

        parentCell.getBody().setLayout(new MigLayout("insets 0, gap 0"));
        parentCell.getBody().add(scrContent, "w 100%!, growy, pushy, gap 0 0 10px 10px");
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
        return "Quest Preferences";
    }

    /* (non-Javadoc)
     * @see forge.gui.home.IVSubmenu#getMenuName()
     */
    @Override
    public EDocID getItemEnum() {
        return EDocID.HOME_QUESTPREFS;
    }

    /** @return {@link javax.swing.JLabel} */
    public JLabel getLblErrRewards() {
        return lblErrRewards;
    }

    /** @return {@link javax.swing.JLabel} */
    public JLabel getLblErrShop() {
        return lblErrShop;
    }

    /** @return {@link javax.swing.JLabel} */
    public JLabel getLblErrDifficulty() {
        return lblErrDifficulty;
    }

    /** @return {@link javax.swing.JLabel} */
    public JLabel getLblErrBooster() {
        return lblErrBooster;
    }

    private void populateRewards() {
        pnlRewards.setOpaque(false);
        pnlRewards.setLayout(new MigLayout("insets 0, gap 0, wrap 2, hidemode 3"));
        pnlRewards.removeAll();
        pnlRewards.add(lblErrRewards, "w 100%!, h 30px!, span 2 1");

        final String constraints1 = "w 60px, h 26px!";
        final String constraints2 = "w 150px!, h 26px!";

        pnlRewards.add(new FLabel.Builder().text("Base winnings").build(), constraints2);
        pnlRewards.add(new PrefInput(QPref.REWARDS_BASE, QuestPreferencesErrType.REWARDS), constraints1);

        pnlRewards.add(new FLabel.Builder().text("No losses").build(), constraints2);
        pnlRewards.add(new PrefInput(QPref.REWARDS_UNDEFEATED, QuestPreferencesErrType.REWARDS), constraints1);

        pnlRewards.add(new FLabel.Builder().text("Poison win").build(), constraints2);
        pnlRewards.add(new PrefInput(QPref.REWARDS_POISON, QuestPreferencesErrType.REWARDS), constraints1);

        pnlRewards.add(new FLabel.Builder().text("Milling win").build(), constraints2);
        pnlRewards.add(new PrefInput(QPref.REWARDS_MILLED, QuestPreferencesErrType.REWARDS), constraints1);

        pnlRewards.add(new FLabel.Builder().text("Mulligan 0 win").build(), constraints2);
        pnlRewards.add(new PrefInput(QPref.REWARDS_MULLIGAN0, QuestPreferencesErrType.REWARDS), constraints1);

        pnlRewards.add(new FLabel.Builder().text("Alternative win").build(), constraints2);
        pnlRewards.add(new PrefInput(QPref.REWARDS_ALTERNATIVE, QuestPreferencesErrType.REWARDS), constraints1);

        pnlRewards.add(new FLabel.Builder().text("Win by turn 15").build(), constraints2);
        pnlRewards.add(new PrefInput(QPref.REWARDS_TURN15, QuestPreferencesErrType.REWARDS), constraints1);

        pnlRewards.add(new FLabel.Builder().text("Win by turn 10").build(), constraints2);
        pnlRewards.add(new PrefInput(QPref.REWARDS_TURN10, QuestPreferencesErrType.REWARDS), constraints1);

        pnlRewards.add(new FLabel.Builder().text("Win by turn 5").build(), constraints2);
        pnlRewards.add(new PrefInput(QPref.REWARDS_TURN5, QuestPreferencesErrType.REWARDS), constraints1);

        pnlRewards.add(new FLabel.Builder().text("First turn win").build(), constraints2);
        pnlRewards.add(new PrefInput(QPref.REWARDS_TURN1, QuestPreferencesErrType.REWARDS), constraints1);
    }

    private void populateDifficulty() {
        final String constraints1 = "w 60px!, h 26px!";
        final String constraints2 = "w 150px!, h 26px!";

        pnlDifficulty.setOpaque(false);
        pnlDifficulty.setLayout(new MigLayout("insets 0, gap 0, wrap 5, hidemode 3"));
        pnlDifficulty.removeAll();
        pnlDifficulty.add(lblErrDifficulty, "w 100%!, h 30px!, span 5 1");

        pnlDifficulty.add(new FLabel.Builder().text("").build(), constraints2);
        pnlDifficulty.add(new FLabel.Builder().text("Easy").build(), constraints1);
        pnlDifficulty.add(new FLabel.Builder().text("Medium").build(), constraints1);
        pnlDifficulty.add(new FLabel.Builder().text("Hard").build(), constraints1);
        pnlDifficulty.add(new FLabel.Builder().text("Expert").build(), constraints1);

        pnlDifficulty.add(new FLabel.Builder().text("Wins For Booster").build(), constraints2);
        pnlDifficulty.add(new PrefInput(QPref.WINS_BOOSTER_EASY, QuestPreferencesErrType.DIFFICULTY), constraints1);
        pnlDifficulty.add(new PrefInput(QPref.WINS_BOOSTER_MEDIUM, QuestPreferencesErrType.DIFFICULTY), constraints1);
        pnlDifficulty.add(new PrefInput(QPref.WINS_BOOSTER_HARD, QuestPreferencesErrType.DIFFICULTY), constraints1);
        pnlDifficulty.add(new PrefInput(QPref.WINS_BOOSTER_EXPERT, QuestPreferencesErrType.DIFFICULTY), constraints1);

        pnlDifficulty.add(new FLabel.Builder().text("Wins For Rank Increase").build(), constraints2);
        pnlDifficulty.add(new PrefInput(QPref.WINS_RANKUP_EASY, QuestPreferencesErrType.DIFFICULTY), constraints1);
        pnlDifficulty.add(new PrefInput(QPref.WINS_RANKUP_MEDIUM, QuestPreferencesErrType.DIFFICULTY), constraints1);
        pnlDifficulty.add(new PrefInput(QPref.WINS_RANKUP_HARD, QuestPreferencesErrType.DIFFICULTY), constraints1);
        pnlDifficulty.add(new PrefInput(QPref.WINS_RANKUP_EXPERT, QuestPreferencesErrType.DIFFICULTY), constraints1);

        pnlDifficulty.add(new FLabel.Builder().text("Wins For Medium AI").build(), constraints2);
        pnlDifficulty.add(new PrefInput(QPref.WINS_MEDIUMAI_EASY, QuestPreferencesErrType.DIFFICULTY), constraints1);
        pnlDifficulty.add(new PrefInput(QPref.WINS_MEDIUMAI_MEDIUM, QuestPreferencesErrType.DIFFICULTY), constraints1);
        pnlDifficulty.add(new PrefInput(QPref.WINS_MEDIUMAI_HARD, QuestPreferencesErrType.DIFFICULTY), constraints1);
        pnlDifficulty.add(new PrefInput(QPref.WINS_MEDIUMAI_EXPERT, QuestPreferencesErrType.DIFFICULTY), constraints1);

        pnlDifficulty.add(new FLabel.Builder().text("Wins For Hard AI").build(), constraints2);
        pnlDifficulty.add(new PrefInput(QPref.WINS_HARDAI_EASY, QuestPreferencesErrType.DIFFICULTY), constraints1);
        pnlDifficulty.add(new PrefInput(QPref.WINS_HARDAI_MEDIUM, QuestPreferencesErrType.DIFFICULTY), constraints1);
        pnlDifficulty.add(new PrefInput(QPref.WINS_HARDAI_HARD, QuestPreferencesErrType.DIFFICULTY), constraints1);
        pnlDifficulty.add(new PrefInput(QPref.WINS_HARDAI_EXPERT, QuestPreferencesErrType.DIFFICULTY), constraints1);

        pnlDifficulty.add(new FLabel.Builder().text("Wins For Expert AI").build(), constraints2);
        pnlDifficulty.add(new PrefInput(QPref.WINS_EXPERTAI_EASY, QuestPreferencesErrType.DIFFICULTY), constraints1);
        pnlDifficulty.add(new PrefInput(QPref.WINS_EXPERTAI_MEDIUM, QuestPreferencesErrType.DIFFICULTY), constraints1);
        pnlDifficulty.add(new PrefInput(QPref.WINS_EXPERTAI_HARD, QuestPreferencesErrType.DIFFICULTY), constraints1);
        pnlDifficulty.add(new PrefInput(QPref.WINS_EXPERTAI_EXPERT, QuestPreferencesErrType.DIFFICULTY), constraints1);

        pnlDifficulty.add(new FLabel.Builder().text("Starting commons").build(), constraints2);
        pnlDifficulty.add(new PrefInput(QPref.STARTING_COMMONS_EASY, QuestPreferencesErrType.DIFFICULTY), constraints1);
        pnlDifficulty.add(new PrefInput(QPref.STARTING_COMMONS_MEDIUM, QuestPreferencesErrType.DIFFICULTY), constraints1);
        pnlDifficulty.add(new PrefInput(QPref.STARTING_COMMONS_HARD, QuestPreferencesErrType.DIFFICULTY), constraints1);
        pnlDifficulty.add(new PrefInput(QPref.STARTING_COMMONS_EXPERT, QuestPreferencesErrType.DIFFICULTY), constraints1);

        pnlDifficulty.add(new FLabel.Builder().text("Starting uncommons").build(), constraints2);
        pnlDifficulty.add(new PrefInput(QPref.STARTING_UNCOMMONS_EASY, QuestPreferencesErrType.DIFFICULTY), constraints1);
        pnlDifficulty.add(new PrefInput(QPref.STARTING_UNCOMMONS_MEDIUM, QuestPreferencesErrType.DIFFICULTY), constraints1);
        pnlDifficulty.add(new PrefInput(QPref.STARTING_UNCOMMONS_HARD, QuestPreferencesErrType.DIFFICULTY), constraints1);
        pnlDifficulty.add(new PrefInput(QPref.STARTING_UNCOMMONS_EXPERT, QuestPreferencesErrType.DIFFICULTY), constraints1);

        pnlDifficulty.add(new FLabel.Builder().text("Starting rares").build(), constraints2);
        pnlDifficulty.add(new PrefInput(QPref.STARTING_RARES_EASY, QuestPreferencesErrType.DIFFICULTY), constraints1);
        pnlDifficulty.add(new PrefInput(QPref.STARTING_RARES_MEDIUM, QuestPreferencesErrType.DIFFICULTY), constraints1);
        pnlDifficulty.add(new PrefInput(QPref.STARTING_RARES_HARD, QuestPreferencesErrType.DIFFICULTY), constraints1);
        pnlDifficulty.add(new PrefInput(QPref.STARTING_RARES_EXPERT, QuestPreferencesErrType.DIFFICULTY), constraints1);

        pnlDifficulty.add(new FLabel.Builder().text("Starting credits").build(), constraints2);
        pnlDifficulty.add(new PrefInput(QPref.STARTING_CREDITS_EASY, QuestPreferencesErrType.DIFFICULTY), constraints1);
        pnlDifficulty.add(new PrefInput(QPref.STARTING_CREDITS_MEDIUM, QuestPreferencesErrType.DIFFICULTY), constraints1);
        pnlDifficulty.add(new PrefInput(QPref.STARTING_CREDITS_HARD, QuestPreferencesErrType.DIFFICULTY), constraints1);
        pnlDifficulty.add(new PrefInput(QPref.STARTING_CREDITS_EXPERT, QuestPreferencesErrType.DIFFICULTY), constraints1);

        pnlDifficulty.add(new FLabel.Builder().text("Starting basic lands").build(), constraints2);
        pnlDifficulty.add(new PrefInput(QPref.STARTING_BASIC_LANDS, QuestPreferencesErrType.DIFFICULTY), constraints1 + ", wrap");

        pnlDifficulty.add(new FLabel.Builder().text("Starting snow lands").build(), constraints2);
        pnlDifficulty.add(new PrefInput(QPref.STARTING_SNOW_LANDS, QuestPreferencesErrType.DIFFICULTY), constraints1 + ", wrap");

        pnlDifficulty.add(new FLabel.Builder().text("Penalty for loss").build(), constraints2);
        pnlDifficulty.add(new PrefInput(QPref.PENALTY_LOSS, QuestPreferencesErrType.DIFFICULTY), constraints1 + ", wrap");
    }

    private void populateBooster() {
        final String constraints1 = "w 60px!, h 26px!";
        final String constraints2 = "w 150px!, h 26px!";

        pnlBooster.setOpaque(false);
        pnlBooster.setLayout(new MigLayout("insets 0, gap 0, wrap 2, hidemode 3"));
        pnlBooster.removeAll();
        pnlBooster.add(lblErrBooster, "w 100%!, h 30px!, span 2 1");

        pnlBooster.add(new FLabel.Builder().text("Common").build(), constraints2);
        pnlBooster.add(new PrefInput(QPref.BOOSTER_COMMONS, QuestPreferencesErrType.BOOSTER), constraints1);

        pnlBooster.add(new FLabel.Builder().text("Uncommon").build(), constraints2);
        pnlBooster.add(new PrefInput(QPref.BOOSTER_UNCOMMONS, QuestPreferencesErrType.BOOSTER), constraints1);

        pnlBooster.add(new FLabel.Builder().text("Rare").build(), constraints2);
        pnlBooster.add(new PrefInput(QPref.BOOSTER_RARES, QuestPreferencesErrType.BOOSTER), constraints1);

    }

    private void populateShop() {
        final String constraints1 = "w 60px, h 26px!";
        final String constraints2 = "w 150px!, h 26px!";

        pnlShop.setOpaque(false);
        pnlShop.setLayout(new MigLayout("insets 0, gap 0, wrap 2, hidemode 3"));
        pnlShop.removeAll();
        pnlShop.add(lblErrShop, "w 100%!, h 30px!, span 2 1");

        pnlShop.add(new FLabel.Builder().text("Maximum Packs").build(), constraints2);
        pnlShop.add(new PrefInput(QPref.SHOP_MAX_PACKS, QuestPreferencesErrType.SHOP), constraints1);

        pnlShop.add(new FLabel.Builder().text("Starting Packs").build(), constraints2);
        pnlShop.add(new PrefInput(QPref.SHOP_STARTING_PACKS, QuestPreferencesErrType.SHOP), constraints1);

        pnlShop.add(new FLabel.Builder().text("Wins for Pack").build(), constraints2);
        pnlShop.add(new PrefInput(QPref.SHOP_WINS_FOR_ADDITIONAL_PACK, QuestPreferencesErrType.SHOP), constraints1);

        pnlShop.add(new FLabel.Builder().text("Common Singles").build(), constraints2);
        pnlShop.add(new PrefInput(QPref.SHOP_SINGLES_COMMON, QuestPreferencesErrType.SHOP), constraints1);

        pnlShop.add(new FLabel.Builder().text("Uncommon Singles").build(), constraints2);
        pnlShop.add(new PrefInput(QPref.SHOP_SINGLES_UNCOMMON, QuestPreferencesErrType.SHOP), constraints1);

        pnlShop.add(new FLabel.Builder().text("Rare Singles").build(), constraints2);
        pnlShop.add(new PrefInput(QPref.SHOP_SINGLES_RARE, QuestPreferencesErrType.SHOP), constraints1);
    }

    /** */
    @SuppressWarnings("serial")
    public class PrefInput extends JTextField {
        private final QPref qpref;
        private final QuestPreferencesErrType err;
        private final Color clrHover, clrActive, clrText;
        private boolean isFocus = false;
        private String previousText = "";

        /**
         * @param qp0 &emsp; {@link forge.quest.data.QuestPreferences.QPref}
         *                  preferences ident enum
         * @param e0 &emsp; {@link forge.view.home.ViewQuestPreference.QuestPreferencesErrType}
         *                  where error should display to
         */
        public PrefInput(QPref qp0, QuestPreferencesErrType e0) {
            super();

            this.qpref = qp0;
            this.err = e0;
            this.clrHover = FSkin.getColor(FSkin.Colors.CLR_HOVER);
            this.clrActive = FSkin.getColor(FSkin.Colors.CLR_ACTIVE);
            this.clrText = FSkin.getColor(FSkin.Colors.CLR_TEXT);

            this.setOpaque(false);
            this.setBorder(null);
            this.setFont(FSkin.getFont(13));
            this.setForeground(clrText);
            this.setCaretColor(clrText);
            this.setBackground(clrHover);
            this.setHorizontalAlignment(SwingConstants.CENTER);
            this.setText(prefs.getPreference(qpref));
            this.setPreviousText(prefs.getPreference(qpref));

            this.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    if (isFocus) { return; }
                    setOpaque(true);
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    if (isFocus) { return; }
                    setOpaque(false);
                    repaint();
                }
            });

            this.addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    isFocus = true;
                    setOpaque(true);
                    setBackground(clrActive);
                }

                @Override
                public void focusLost(FocusEvent e) {
                    isFocus = false;
                    setOpaque(false);
                    setBackground(clrHover);

                    CSubmenuQuestPrefs.validateAndSave(PrefInput.this);
                }
            });
        }

        /** @return {@link forge.quest.data.QuestPreferences.QPref} */
        public QPref getQPref() {
            return this.qpref;
        }

        /** @return {@link forge.gui.home.quest.VSubmenuQuestPrefs.QuestPreferencesErrType} */
        public QuestPreferencesErrType getErrType() {
            return this.err;
        }

        /** @return {@link java.lang.String} */
        public String getPreviousText() {
            return this.previousText;
        }

        /** @param s0 &emsp; {@link java.lang.String} */
        public void setPreviousText(String s0) {
            this.previousText = s0;
        }
    }

    //========== Overridden from IVDoc

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getDocumentID()
     */
    @Override
    public EDocID getDocumentID() {
        return EDocID.HOME_QUESTPREFS;
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
        return CSubmenuQuestPrefs.SINGLETON_INSTANCE;
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
