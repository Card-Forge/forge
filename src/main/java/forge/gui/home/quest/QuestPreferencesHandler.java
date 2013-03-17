package forge.gui.home.quest;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import net.miginfocom.swing.MigLayout;
import forge.Singletons;
import forge.gui.toolbox.FLabel;
import forge.gui.toolbox.FSkin;
import forge.quest.data.QuestPreferences;
import forge.quest.data.QuestPreferences.QPref;

/** 
 * TODO: Write javadoc for this type.
 *
 */
@SuppressWarnings("serial")
public class QuestPreferencesHandler extends JPanel {
    private final QuestPreferences prefs;
    private final JPanel pnlDifficulty, pnlBooster, pnlRewards, pnlShop;
    private final FLabel lblErrRewards, lblErrBooster, lblErrDifficulty, lblErrShop;
    private String constraints1, constraints2;

    private enum ErrType {
        REWARDS,
        DIFFICULTY,
        BOOSTER,
        SHOP
    }

    /** */
    public QuestPreferencesHandler() {
        this.setOpaque(false);
        this.setLayout(new MigLayout("insets 0, gap 0, wrap"));
        this.prefs = Singletons.getModel().getQuestPreferences();

        pnlRewards = new JPanel();
        pnlDifficulty = new JPanel();
        pnlBooster = new JPanel();
        pnlShop = new JPanel();

        lblErrRewards = new FLabel.Builder().text("Rewards Error").fontStyle(Font.BOLD).build();
        lblErrDifficulty = new FLabel.Builder().text("Difficulty Error").fontStyle(Font.BOLD).build();
        lblErrBooster = new FLabel.Builder().text("Booster Error").fontStyle(Font.BOLD).build();
        lblErrShop = new FLabel.Builder().text("Shop Error").fontStyle(Font.BOLD).build();

        lblErrRewards.setForeground(Color.red);
        lblErrDifficulty.setForeground(Color.red);
        lblErrBooster.setForeground(Color.red);
        lblErrShop.setForeground(Color.red);

        // Rewards panel
        pnlRewards.setOpaque(false);
        pnlRewards.setLayout(new MigLayout("insets 0, gap 0, wrap 2"));

        pnlRewards.add(new FLabel.Builder().text("Rewards")
                .icon(new ImageIcon("res/images/icons/CoinIcon.png")).build(),
                "w 100%!, h 30px!, span 2 1");
        pnlRewards.add(lblErrRewards, "w 100%!, h 30px!, span 2 1");

        constraints1 = "w 60px, h 26px!";
        constraints2 = "w 150px!, h 26px!";

        pnlRewards.add(new FLabel.Builder().text("Base winnings").build(), constraints2);
        pnlRewards.add(new PrefInput(QPref.REWARDS_BASE, ErrType.REWARDS), constraints1);

        pnlRewards.add(new FLabel.Builder().text("No losses").build(), constraints2);
        pnlRewards.add(new PrefInput(QPref.REWARDS_UNDEFEATED, ErrType.REWARDS), constraints1);

        pnlRewards.add(new FLabel.Builder().text("Poison win").build(), constraints2);
        pnlRewards.add(new PrefInput(QPref.REWARDS_POISON, ErrType.REWARDS), constraints1);

        pnlRewards.add(new FLabel.Builder().text("Milling win").build(), constraints2);
        pnlRewards.add(new PrefInput(QPref.REWARDS_MILLED, ErrType.REWARDS), constraints1);

        pnlRewards.add(new FLabel.Builder().text("Mulligan 0 win").build(), constraints2);
        pnlRewards.add(new PrefInput(QPref.REWARDS_MULLIGAN0, ErrType.REWARDS), constraints1);

        pnlRewards.add(new FLabel.Builder().text("Alternative win").build(), constraints2);
        pnlRewards.add(new PrefInput(QPref.REWARDS_ALTERNATIVE, ErrType.REWARDS), constraints1);

        pnlRewards.add(new FLabel.Builder().text("Win by turn 15").build(), constraints2);
        pnlRewards.add(new PrefInput(QPref.REWARDS_TURN15, ErrType.REWARDS), constraints1);

        pnlRewards.add(new FLabel.Builder().text("Win by turn 10").build(), constraints2);
        pnlRewards.add(new PrefInput(QPref.REWARDS_TURN10, ErrType.REWARDS), constraints1);

        pnlRewards.add(new FLabel.Builder().text("Win by turn 5").build(), constraints2);
        pnlRewards.add(new PrefInput(QPref.REWARDS_TURN5, ErrType.REWARDS), constraints1);

        pnlRewards.add(new FLabel.Builder().text("First turn win").build(), constraints2);
        pnlRewards.add(new PrefInput(QPref.REWARDS_TURN1, ErrType.REWARDS), constraints1);

        // Difficulty table panel
        pnlDifficulty.setOpaque(false);
        pnlDifficulty.setLayout(new MigLayout("insets 0, gap 0, wrap 5"));

        pnlDifficulty.add(new FLabel.Builder().text("Difficulty Adjustments").icon(new ImageIcon("res/images/icons/NotesIcon.png")).build(), "w 100%!, h 30px!, span 5 1");
        pnlDifficulty.add(lblErrDifficulty, "w 100%!, h 30px!, span 5 1");

        constraints1 = "w 60px!, h 26px!";
        constraints2 = "w 150px!, h 26px!";

        pnlDifficulty.add(new FLabel.Builder().text("").build(), constraints2);
        pnlDifficulty.add(new FLabel.Builder().text("Easy").build(), constraints1);
        pnlDifficulty.add(new FLabel.Builder().text("Medium").build(), constraints1);
        pnlDifficulty.add(new FLabel.Builder().text("Hard").build(), constraints1);
        pnlDifficulty.add(new FLabel.Builder().text("Expert").build(), constraints1);

        pnlDifficulty.add(new FLabel.Builder().text("Wins For Booster").build(), constraints2);
        pnlDifficulty.add(new PrefInput(QPref.WINS_BOOSTER_EASY, ErrType.DIFFICULTY), constraints1);
        pnlDifficulty.add(new PrefInput(QPref.WINS_BOOSTER_MEDIUM, ErrType.DIFFICULTY), constraints1);
        pnlDifficulty.add(new PrefInput(QPref.WINS_BOOSTER_HARD, ErrType.DIFFICULTY), constraints1);
        pnlDifficulty.add(new PrefInput(QPref.WINS_BOOSTER_EXPERT, ErrType.DIFFICULTY), constraints1);

        pnlDifficulty.add(new FLabel.Builder().text("Wins For Rank Increase").build(), constraints2);
        pnlDifficulty.add(new PrefInput(QPref.WINS_RANKUP_EASY, ErrType.DIFFICULTY), constraints1);
        pnlDifficulty.add(new PrefInput(QPref.WINS_RANKUP_MEDIUM, ErrType.DIFFICULTY), constraints1);
        pnlDifficulty.add(new PrefInput(QPref.WINS_RANKUP_HARD, ErrType.DIFFICULTY), constraints1);
        pnlDifficulty.add(new PrefInput(QPref.WINS_RANKUP_EXPERT, ErrType.DIFFICULTY), constraints1);

        pnlDifficulty.add(new FLabel.Builder().text("Wins For Medium AI").build(), constraints2);
        pnlDifficulty.add(new PrefInput(QPref.WINS_MEDIUMAI_EASY, ErrType.DIFFICULTY), constraints1);
        pnlDifficulty.add(new PrefInput(QPref.WINS_MEDIUMAI_MEDIUM, ErrType.DIFFICULTY), constraints1);
        pnlDifficulty.add(new PrefInput(QPref.WINS_MEDIUMAI_HARD, ErrType.DIFFICULTY), constraints1);
        pnlDifficulty.add(new PrefInput(QPref.WINS_MEDIUMAI_EXPERT, ErrType.DIFFICULTY), constraints1);

        pnlDifficulty.add(new FLabel.Builder().text("Wins For Hard AI").build(), constraints2);
        pnlDifficulty.add(new PrefInput(QPref.WINS_HARDAI_EASY, ErrType.DIFFICULTY), constraints1);
        pnlDifficulty.add(new PrefInput(QPref.WINS_HARDAI_MEDIUM, ErrType.DIFFICULTY), constraints1);
        pnlDifficulty.add(new PrefInput(QPref.WINS_HARDAI_HARD, ErrType.DIFFICULTY), constraints1);
        pnlDifficulty.add(new PrefInput(QPref.WINS_HARDAI_EXPERT, ErrType.DIFFICULTY), constraints1);

        pnlDifficulty.add(new FLabel.Builder().text("Wins For Expert AI").build(), constraints2);
        pnlDifficulty.add(new PrefInput(QPref.WINS_EXPERTAI_EASY, ErrType.DIFFICULTY), constraints1);
        pnlDifficulty.add(new PrefInput(QPref.WINS_EXPERTAI_MEDIUM, ErrType.DIFFICULTY), constraints1);
        pnlDifficulty.add(new PrefInput(QPref.WINS_EXPERTAI_HARD, ErrType.DIFFICULTY), constraints1);
        pnlDifficulty.add(new PrefInput(QPref.WINS_EXPERTAI_EXPERT, ErrType.DIFFICULTY), constraints1);

        pnlDifficulty.add(new FLabel.Builder().text("Starting commons").build(), constraints2);
        pnlDifficulty.add(new PrefInput(QPref.STARTING_COMMONS_EASY, ErrType.DIFFICULTY), constraints1);
        pnlDifficulty.add(new PrefInput(QPref.STARTING_COMMONS_MEDIUM, ErrType.DIFFICULTY), constraints1);
        pnlDifficulty.add(new PrefInput(QPref.STARTING_COMMONS_HARD, ErrType.DIFFICULTY), constraints1);
        pnlDifficulty.add(new PrefInput(QPref.STARTING_COMMONS_EXPERT, ErrType.DIFFICULTY), constraints1);

        pnlDifficulty.add(new FLabel.Builder().text("Starting uncommons").build(), constraints2);
        pnlDifficulty.add(new PrefInput(QPref.STARTING_UNCOMMONS_EASY, ErrType.DIFFICULTY), constraints1);
        pnlDifficulty.add(new PrefInput(QPref.STARTING_UNCOMMONS_MEDIUM, ErrType.DIFFICULTY), constraints1);
        pnlDifficulty.add(new PrefInput(QPref.STARTING_UNCOMMONS_HARD, ErrType.DIFFICULTY), constraints1);
        pnlDifficulty.add(new PrefInput(QPref.STARTING_UNCOMMONS_EXPERT, ErrType.DIFFICULTY), constraints1);

        pnlDifficulty.add(new FLabel.Builder().text("Starting rares").build(), constraints2);
        pnlDifficulty.add(new PrefInput(QPref.STARTING_RARES_EASY, ErrType.DIFFICULTY), constraints1);
        pnlDifficulty.add(new PrefInput(QPref.STARTING_RARES_MEDIUM, ErrType.DIFFICULTY), constraints1);
        pnlDifficulty.add(new PrefInput(QPref.STARTING_RARES_HARD, ErrType.DIFFICULTY), constraints1);
        pnlDifficulty.add(new PrefInput(QPref.STARTING_RARES_EXPERT, ErrType.DIFFICULTY), constraints1);

        pnlDifficulty.add(new FLabel.Builder().text("Starting credits").build(), constraints2);
        pnlDifficulty.add(new PrefInput(QPref.STARTING_CREDITS_EASY, ErrType.DIFFICULTY), constraints1);
        pnlDifficulty.add(new PrefInput(QPref.STARTING_CREDITS_MEDIUM, ErrType.DIFFICULTY), constraints1);
        pnlDifficulty.add(new PrefInput(QPref.STARTING_CREDITS_HARD, ErrType.DIFFICULTY), constraints1);
        pnlDifficulty.add(new PrefInput(QPref.STARTING_CREDITS_EXPERT, ErrType.DIFFICULTY), constraints1);

        pnlDifficulty.add(new FLabel.Builder().text("Starting basic lands").build(), constraints2);
        pnlDifficulty.add(new PrefInput(QPref.STARTING_BASIC_LANDS, ErrType.DIFFICULTY), constraints1 + ", wrap");

        pnlDifficulty.add(new FLabel.Builder().text("Starting snow lands").build(), constraints2);
        pnlDifficulty.add(new PrefInput(QPref.STARTING_SNOW_LANDS, ErrType.DIFFICULTY), constraints1 + ", wrap");

        pnlDifficulty.add(new FLabel.Builder().text("Penalty for loss").build(), constraints2);
        pnlDifficulty.add(new PrefInput(QPref.PENALTY_LOSS, ErrType.DIFFICULTY), constraints1 + ", wrap");

        // Booster breakdown panel
        pnlBooster.setOpaque(false);
        pnlBooster.setLayout(new MigLayout("insets 0, gap 0, wrap 2"));

        pnlBooster.add(new FLabel.Builder().text("Booster Pack Ratios")
                .icon(new ImageIcon("res/images/icons/BookIcon.png")).build(),
                "w 100%!, h 30px!, span 2 1");
        pnlBooster.add(lblErrBooster, "w 100%!, h 30px!, span 2 1");

        constraints1 = "w 60px!, h 26px!";
        constraints2 = "w 150px!, h 26px!";
        pnlBooster.add(new FLabel.Builder().text("Common").build(), constraints2);
        pnlBooster.add(new PrefInput(QPref.BOOSTER_COMMONS, ErrType.BOOSTER), constraints1);

        pnlBooster.add(new FLabel.Builder().text("Uncommon").build(), constraints2);
        pnlBooster.add(new PrefInput(QPref.BOOSTER_UNCOMMONS, ErrType.BOOSTER), constraints1);

        pnlBooster.add(new FLabel.Builder().text("Rare").build(), constraints2);
        pnlBooster.add(new PrefInput(QPref.BOOSTER_RARES, ErrType.BOOSTER), constraints1);

        // Shop panel
        pnlShop.setOpaque(false);
        pnlShop.setLayout(new MigLayout("insets 0, gap 0, wrap 2"));

        pnlShop.add(new FLabel.Builder().text("Shop Preferences")
                .icon(new ImageIcon("res/images/icons/CoinIcon.png")).build(), "w 100%!, h 30px!, span 2 1");
        pnlShop.add(lblErrShop, "w 100%!, h 30px!, span 2 1");

        constraints1 = "w 60px, h 26px!";
        constraints2 = "w 150px!, h 26px!";

        pnlShop.add(new FLabel.Builder().text("Maximum Packs").build(), constraints2);
        pnlShop.add(new PrefInput(QPref.SHOP_MAX_PACKS, ErrType.SHOP), constraints1);

        pnlShop.add(new FLabel.Builder().text("Starting Packs").build(), constraints2);
        pnlShop.add(new PrefInput(QPref.SHOP_STARTING_PACKS, ErrType.SHOP), constraints1);

        pnlShop.add(new FLabel.Builder().text("Wins for Pack").build(), constraints2);
        pnlShop.add(new PrefInput(QPref.SHOP_WINS_FOR_ADDITIONAL_PACK, ErrType.SHOP), constraints1);

        pnlShop.add(new FLabel.Builder().text("Common Singles").build(), constraints2);
        pnlShop.add(new PrefInput(QPref.SHOP_SINGLES_COMMON, ErrType.SHOP), constraints1);

        pnlShop.add(new FLabel.Builder().text("Uncommon Singles").build(), constraints2);
        pnlShop.add(new PrefInput(QPref.SHOP_SINGLES_UNCOMMON, ErrType.SHOP), constraints1);

        pnlShop.add(new FLabel.Builder().text("Rare Singles").build(), constraints2);
        pnlShop.add(new PrefInput(QPref.SHOP_SINGLES_RARE, ErrType.SHOP), constraints1);

        constraints1 = "w 100%!, gap 0 0 20px 0";
        this.add(pnlRewards, constraints1);
        this.add(pnlDifficulty, constraints1);
        this.add(pnlBooster, constraints1);
        this.add(pnlShop, constraints1);

        resetErrors();
    }

    private class PrefInput extends JTextField {
        private final QPref qpref;
        private final ErrType err;
        private final Color clrHover, clrActive, clrText;
        private boolean isFocus = false;
        private String previousText = "";

        /**
         * @param qp1 &emsp; {@link forge.quest.data.QuestPreferences.QPref}
         *                  preferences ident enum
         * @param e0 &emsp; {@link forge.view.home.ViewQuestPreference.ErrType}
         *                  where error should display to
         */
        public PrefInput(QPref qp0, ErrType e0) {
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
            this.setText(prefs.getPref(qpref));
            this.setPreviousText(prefs.getPref(qpref));

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

                    // TODO for slight performance improvement
                    // check if value has changed before validating
                    validateAndSave(PrefInput.this);
                }
            });
        }

        public QPref getQPref() {
            return this.qpref;
        }

        public ErrType getErrType() {
            return this.err;
        }

        public String getPreviousText() {
            return this.previousText;
        }

        public void setPreviousText(String s0) {
            this.previousText = s0;
        }
    }

    
    /**
     * Checks validity of values entered into prefInputs.
     * @param i0 &emsp; a PrefInput object
     */
    private void validateAndSave(PrefInput i0) {
        if (i0.getText().equals(i0.getPreviousText())) { return; }

        int val = Integer.parseInt(i0.getText());
        resetErrors();

        String validationError = validatePreference(i0.getQPref(), val, prefs);
        if( null != validationError)
        {
            showError(i0, validationError);
            return;
        }

        prefs.setPref(i0.getQPref(), i0.getText());
        prefs.save();
        i0.setPreviousText(i0.getText());
    }

    public static String validatePreference(QPref qpref, int val, QuestPreferences current) {
        int temp1, temp2;
        switch (qpref) {
            case STARTING_CREDITS_EASY: case STARTING_CREDITS_MEDIUM:
            case STARTING_CREDITS_HARD: case STARTING_CREDITS_EXPERT:
            case REWARDS_MILLED: case REWARDS_MULLIGAN0:
            case REWARDS_ALTERNATIVE: case REWARDS_TURN5:
                if (val > 500) {
                    return "Value too large (maximum 500).";
                }
                break;
            case BOOSTER_COMMONS:
                temp1 = current.getPrefInt(QPref.BOOSTER_UNCOMMONS);
                temp2 = current.getPrefInt(QPref.BOOSTER_RARES);

                if (temp1 + temp2 + val > 15) {
                    return "Booster packs must have maximum 15 cards.";
                }
                break;
            case BOOSTER_UNCOMMONS:
                temp1 = current.getPrefInt(QPref.BOOSTER_COMMONS);
                temp2 = current.getPrefInt(QPref.BOOSTER_RARES);

                if (temp1 + temp2 + val > 15) {
                    return "Booster packs must have maximum 15 cards.";
                }
                break;
            case BOOSTER_RARES:
                temp1 = current.getPrefInt(QPref.BOOSTER_COMMONS);
                temp2 = current.getPrefInt(QPref.BOOSTER_UNCOMMONS);

                if (temp1 + temp2 + val > 15) {
                    return "Booster packs must have maximum 15 cards.";
                }
                break;
            case REWARDS_TURN1:
                if (val > 2000) {
                    return "Value too large (maximum 2000).";
                }
                break;
            case SHOP_STARTING_PACKS:
            case SHOP_SINGLES_COMMON: case SHOP_SINGLES_UNCOMMON: case SHOP_SINGLES_RARE:
                if (val < 0) {
                    return "Value too small (minimum 0).";
                } else if (val > 15) {
                    return "Value too large (maximum 15).";
                }
                break;
            case SHOP_WINS_FOR_ADDITIONAL_PACK: case SHOP_MAX_PACKS:
                if (val < 1) {
                    return "Value too small (minimum 1).";
                } else if (val > 25) {
                    return "Value too large (maximum 25).";
                }
                break;
            default:
                if (val > 100) {
                    return "Value too large (maximum 100).";
                }
                break;
        }
        return null;
    }

    private void showError(PrefInput i0, String s0) {
        String s = "Save failed: " + s0;
        switch(i0.getErrType()) {
            case BOOSTER:
                lblErrBooster.setVisible(true);
                lblErrBooster.setText(s);
                break;
            case DIFFICULTY:
                lblErrDifficulty.setVisible(true);
                lblErrDifficulty.setText(s);
                break;
            case REWARDS:
                lblErrRewards.setVisible(true);
                lblErrRewards.setText(s);
                break;
            case SHOP:
                lblErrShop.setVisible(true);
                lblErrShop.setText(s);
                break;
            default:
        }

        i0.setText(i0.getPreviousText());
    }

    private void resetErrors() {
        lblErrBooster.setVisible(false);
        lblErrDifficulty.setVisible(false);
        lblErrRewards.setVisible(false);
        lblErrShop.setVisible(false);
    }
}
