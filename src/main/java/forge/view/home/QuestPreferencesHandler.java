package forge.view.home;

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
import forge.quest.data.QuestPreferences;
import forge.quest.data.QuestPreferences.QPref;
import forge.view.toolbox.FLabel;
import forge.view.toolbox.FSkin;

/** 
 * TODO: Write javadoc for this type.
 *
 */
@SuppressWarnings("serial")
public class QuestPreferencesHandler extends JPanel {
    private final FSkin skin;
    private final QuestPreferences prefs;
    private final JPanel pnlDifficulty, pnlBooster, pnlRewards;
    private final FLabel lblErrRewards, lblErrBooster, lblErrDifficulty;
    private String constraints1, constraints2;

    private enum ErrType {
        REWARDS,
        DIFFICULTY,
        BOOSTER
    }

    /** */
    public QuestPreferencesHandler() {
        this.setOpaque(false);
        this.setLayout(new MigLayout("insets 0, gap 0, wrap"));
        this.skin = Singletons.getView().getSkin();
        this.prefs = Singletons.getModel().getQuestPreferences();

        pnlRewards = new JPanel();
        pnlDifficulty = new JPanel();
        pnlBooster = new JPanel();

        lblErrRewards = new FLabel("Rewards Error");
        lblErrDifficulty = new FLabel("Difficulty Error");
        lblErrBooster = new FLabel("Booster Error");

        lblErrRewards.setForeground(Color.red);
        lblErrRewards.setFontStyle(Font.BOLD);
        lblErrDifficulty.setForeground(Color.red);
        lblErrDifficulty.setFontStyle(Font.BOLD);
        lblErrBooster.setForeground(Color.red);
        lblErrBooster.setFontStyle(Font.BOLD);

        // Rewards panel
        pnlRewards.setOpaque(false);
        pnlRewards.setLayout(new MigLayout("insets 0, gap 0, wrap 2"));

        pnlRewards.add(new FLabel("Rewards", new ImageIcon("res/images/icons/CoinIcon.png")), "w 100%!, h 30px!, span 2 1");
        pnlRewards.add(lblErrRewards, "w 100%!, h 30px!, span 2 1");

        constraints1 = "w 60px, h 26px!";
        constraints2 = "w 150px!, h 26px!";

        pnlRewards.add(new FLabel("Base winnings"), constraints2);
        pnlRewards.add(new PrefInput(QPref.REWARDS_BASE, ErrType.REWARDS), constraints1);

        pnlRewards.add(new FLabel("No losses"), constraints2);
        pnlRewards.add(new PrefInput(QPref.REWARDS_UNDEFEATED, ErrType.REWARDS), constraints1);

        pnlRewards.add(new FLabel("Poison win"), constraints2);
        pnlRewards.add(new PrefInput(QPref.REWARDS_POISON, ErrType.REWARDS), constraints1);

        pnlRewards.add(new FLabel("Milling win"), constraints2);
        pnlRewards.add(new PrefInput(QPref.REWARDS_MILLED, ErrType.REWARDS), constraints1);

        pnlRewards.add(new FLabel("Mulligan 0 win"), constraints2);
        pnlRewards.add(new PrefInput(QPref.REWARDS_MULLIGAN0, ErrType.REWARDS), constraints1);

        pnlRewards.add(new FLabel("Alternative win"), constraints2);
        pnlRewards.add(new PrefInput(QPref.REWARDS_ALTERNATIVE, ErrType.REWARDS), constraints1);

        pnlRewards.add(new FLabel("Win by turn 15"), constraints2);
        pnlRewards.add(new PrefInput(QPref.REWARDS_TURN15, ErrType.REWARDS), constraints1);

        pnlRewards.add(new FLabel("Win by turn 10"), constraints2);
        pnlRewards.add(new PrefInput(QPref.REWARDS_TURN10, ErrType.REWARDS), constraints1);

        pnlRewards.add(new FLabel("Win by turn 5"), constraints2);
        pnlRewards.add(new PrefInput(QPref.REWARDS_TURN5, ErrType.REWARDS), constraints1);

        pnlRewards.add(new FLabel("First turn win"), constraints2);
        pnlRewards.add(new PrefInput(QPref.REWARDS_TURN1, ErrType.REWARDS), constraints1);

        // Difficulty table panel
        pnlDifficulty.setOpaque(false);
        pnlDifficulty.setLayout(new MigLayout("insets 0, gap 0, wrap 5"));

        pnlDifficulty.add(new FLabel("Difficulty Adjustments", new ImageIcon("res/images/icons/NotesIcon.png")), "w 100%!, h 30px!, span 5 1");
        pnlDifficulty.add(lblErrDifficulty, "w 100%!, h 30px!, span 5 1");

        constraints1 = "w 60px!, h 26px!";
        constraints2 = "w 150px!, h 26px!";

        pnlDifficulty.add(new FLabel(""), constraints2);
        pnlDifficulty.add(new FLabel("Easy"), constraints1);
        pnlDifficulty.add(new FLabel("Medium"), constraints1);
        pnlDifficulty.add(new FLabel("Hard"), constraints1);
        pnlDifficulty.add(new FLabel("Expert"), constraints1);

        pnlDifficulty.add(new FLabel("Wins For Booster"), constraints2);
        pnlDifficulty.add(new PrefInput(QPref.WINS_BOOSTER_EASY, ErrType.DIFFICULTY), constraints1);
        pnlDifficulty.add(new PrefInput(QPref.WINS_BOOSTER_MEDIUM, ErrType.DIFFICULTY), constraints1);
        pnlDifficulty.add(new PrefInput(QPref.WINS_BOOSTER_HARD, ErrType.DIFFICULTY), constraints1);
        pnlDifficulty.add(new PrefInput(QPref.WINS_BOOSTER_EXPERT, ErrType.DIFFICULTY), constraints1);

        pnlDifficulty.add(new FLabel("Wins For Rank Increase"), constraints2);
        pnlDifficulty.add(new PrefInput(QPref.WINS_RANKUP_EASY, ErrType.DIFFICULTY), constraints1);
        pnlDifficulty.add(new PrefInput(QPref.WINS_RANKUP_MEDIUM, ErrType.DIFFICULTY), constraints1);
        pnlDifficulty.add(new PrefInput(QPref.WINS_RANKUP_HARD, ErrType.DIFFICULTY), constraints1);
        pnlDifficulty.add(new PrefInput(QPref.WINS_RANKUP_EXPERT, ErrType.DIFFICULTY), constraints1);

        pnlDifficulty.add(new FLabel("Wins For Medium AI"), constraints2);
        pnlDifficulty.add(new PrefInput(QPref.WINS_MEDIUMAI_EASY, ErrType.DIFFICULTY), constraints1);
        pnlDifficulty.add(new PrefInput(QPref.WINS_MEDIUMAI_MEDIUM, ErrType.DIFFICULTY), constraints1);
        pnlDifficulty.add(new PrefInput(QPref.WINS_MEDIUMAI_HARD, ErrType.DIFFICULTY), constraints1);
        pnlDifficulty.add(new PrefInput(QPref.WINS_MEDIUMAI_EXPERT, ErrType.DIFFICULTY), constraints1);

        pnlDifficulty.add(new FLabel("Wins For Hard AI"), constraints2);
        pnlDifficulty.add(new PrefInput(QPref.WINS_HARDAI_EASY, ErrType.DIFFICULTY), constraints1);
        pnlDifficulty.add(new PrefInput(QPref.WINS_HARDAI_MEDIUM, ErrType.DIFFICULTY), constraints1);
        pnlDifficulty.add(new PrefInput(QPref.WINS_HARDAI_HARD, ErrType.DIFFICULTY), constraints1);
        pnlDifficulty.add(new PrefInput(QPref.WINS_HARDAI_EXPERT, ErrType.DIFFICULTY), constraints1);

        pnlDifficulty.add(new FLabel("Wins For Expert AI"), constraints2);
        pnlDifficulty.add(new PrefInput(QPref.WINS_EXPERTAI_EASY, ErrType.DIFFICULTY), constraints1);
        pnlDifficulty.add(new PrefInput(QPref.WINS_EXPERTAI_MEDIUM, ErrType.DIFFICULTY), constraints1);
        pnlDifficulty.add(new PrefInput(QPref.WINS_EXPERTAI_HARD, ErrType.DIFFICULTY), constraints1);
        pnlDifficulty.add(new PrefInput(QPref.WINS_EXPERTAI_EXPERT, ErrType.DIFFICULTY), constraints1);

        pnlDifficulty.add(new FLabel("Starting commons"), constraints2);
        pnlDifficulty.add(new PrefInput(QPref.STARTING_COMMONS_EASY, ErrType.DIFFICULTY), constraints1);
        pnlDifficulty.add(new PrefInput(QPref.STARTING_COMMONS_MEDIUM, ErrType.DIFFICULTY), constraints1);
        pnlDifficulty.add(new PrefInput(QPref.STARTING_COMMONS_HARD, ErrType.DIFFICULTY), constraints1);
        pnlDifficulty.add(new PrefInput(QPref.STARTING_COMMONS_EXPERT, ErrType.DIFFICULTY), constraints1);

        pnlDifficulty.add(new FLabel("Starting uncommons"), constraints2);
        pnlDifficulty.add(new PrefInput(QPref.STARTING_UNCOMMONS_EASY, ErrType.DIFFICULTY), constraints1);
        pnlDifficulty.add(new PrefInput(QPref.STARTING_UNCOMMONS_MEDIUM, ErrType.DIFFICULTY), constraints1);
        pnlDifficulty.add(new PrefInput(QPref.STARTING_UNCOMMONS_HARD, ErrType.DIFFICULTY), constraints1);
        pnlDifficulty.add(new PrefInput(QPref.STARTING_UNCOMMONS_EXPERT, ErrType.DIFFICULTY), constraints1);

        pnlDifficulty.add(new FLabel("Starting rares"), constraints2);
        pnlDifficulty.add(new PrefInput(QPref.STARTING_RARES_EASY, ErrType.DIFFICULTY), constraints1);
        pnlDifficulty.add(new PrefInput(QPref.STARTING_RARES_MEDIUM, ErrType.DIFFICULTY), constraints1);
        pnlDifficulty.add(new PrefInput(QPref.STARTING_RARES_HARD, ErrType.DIFFICULTY), constraints1);
        pnlDifficulty.add(new PrefInput(QPref.STARTING_RARES_EXPERT, ErrType.DIFFICULTY), constraints1);

        pnlDifficulty.add(new FLabel("Starting credits"), constraints2);
        pnlDifficulty.add(new PrefInput(QPref.STARTING_CREDITS_EASY, ErrType.DIFFICULTY), constraints1);
        pnlDifficulty.add(new PrefInput(QPref.STARTING_CREDITS_MEDIUM, ErrType.DIFFICULTY), constraints1);
        pnlDifficulty.add(new PrefInput(QPref.STARTING_CREDITS_HARD, ErrType.DIFFICULTY), constraints1);
        pnlDifficulty.add(new PrefInput(QPref.STARTING_CREDITS_EXPERT, ErrType.DIFFICULTY), constraints1);

        pnlDifficulty.add(new FLabel("Starting basic lands"), constraints2);
        pnlDifficulty.add(new PrefInput(QPref.STARTING_BASIC_LANDS, ErrType.DIFFICULTY), constraints1 + ", wrap");

        pnlDifficulty.add(new FLabel("Starting snow lands"), constraints2);
        pnlDifficulty.add(new PrefInput(QPref.STARTING_SNOW_LANDS, ErrType.DIFFICULTY), constraints1 + ", wrap");

        pnlDifficulty.add(new FLabel("Penalty for loss"), constraints2);
        pnlDifficulty.add(new PrefInput(QPref.PENALTY_LOSS, ErrType.DIFFICULTY), constraints1 + ", wrap");

        // Booster breakdown panel
        pnlBooster.setOpaque(false);
        pnlBooster.setLayout(new MigLayout("insets 0, gap 0, wrap 2"));

        pnlBooster.add(new FLabel("Booster Pack Ratios", new ImageIcon("res/images/icons/BookIcon.png")), "w 100%!, h 30px!, span 2 1");
        pnlBooster.add(lblErrBooster, "w 100%!, h 30px!, span 2 1");

        constraints1 = "w 60px!, h 26px!";
        constraints2 = "w 150px!, h 26px!";
        pnlBooster.add(new FLabel("Common"), constraints2);
        pnlBooster.add(new PrefInput(QPref.BOOSTER_COMMONS, ErrType.BOOSTER), constraints1);

        pnlBooster.add(new FLabel("Uncommon"), constraints2);
        pnlBooster.add(new PrefInput(QPref.BOOSTER_UNCOMMONS, ErrType.BOOSTER), constraints1);

        pnlBooster.add(new FLabel("Rare"), constraints2);
        pnlBooster.add(new PrefInput(QPref.BOOSTER_RARES, ErrType.BOOSTER), constraints1);

        constraints1 = "w 100%!, gap 0 0 20px 0";
        this.add(pnlRewards, constraints1);
        this.add(pnlDifficulty, constraints1);
        this.add(pnlBooster, constraints1);

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
            this.clrHover = skin.getColor(FSkin.Colors.CLR_HOVER);
            this.clrActive = skin.getColor(FSkin.Colors.CLR_ACTIVE);
            this.clrText = skin.getColor(FSkin.Colors.CLR_TEXT);

            this.setOpaque(false);
            this.setBorder(null);
            this.setFont(skin.getFont(13));
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

    private int temp1, temp2;
    /**
     * Checks validity of values entered into prefInputs.
     * @param i0 &emsp; a PrefInput object
     */
    private void validateAndSave(PrefInput i0) {
        if (i0.getText().equals(i0.getPreviousText())) { return; }

        int val = Integer.parseInt(i0.getText());
        resetErrors();

        switch (i0.getQPref()) {
            case STARTING_CREDITS_EASY: case STARTING_CREDITS_MEDIUM:
            case STARTING_CREDITS_HARD: case STARTING_CREDITS_EXPERT:
            case REWARDS_MILLED: case REWARDS_MULLIGAN0:
            case REWARDS_ALTERNATIVE: case REWARDS_TURN5:
                if (val > 500) {
                    showError(i0, "Value too large (maximum 500).");
                    return;
                }
                break;
            case BOOSTER_COMMONS:
                temp1 = prefs.getPreferenceInt(QPref.BOOSTER_UNCOMMONS);
                temp2 = prefs.getPreferenceInt(QPref.BOOSTER_RARES);

                if (temp1 + temp2 + val > 15) {
                    showError(i0, "Booster packs must have maximum 15 cards.");
                    return;
                }
                break;
            case BOOSTER_UNCOMMONS:
                temp1 = prefs.getPreferenceInt(QPref.BOOSTER_COMMONS);
                temp2 = prefs.getPreferenceInt(QPref.BOOSTER_RARES);

                if (temp1 + temp2 + val > 15) {
                    showError(i0, "Booster packs must have maximum 15 cards.");
                    return;
                }
                break;
            case BOOSTER_RARES:
                temp1 = prefs.getPreferenceInt(QPref.BOOSTER_COMMONS);
                temp2 = prefs.getPreferenceInt(QPref.BOOSTER_UNCOMMONS);

                if (temp1 + temp2 + val > 15) {
                    showError(i0, "Booster packs must have maximum 15 cards.");
                    return;
                }
                break;
            case REWARDS_TURN1:
                if (val > 2000) {
                    showError(i0, "Value too large (maximum 2000).");
                    return;
                }
                break;
            default:
                if (val > 100) {
                    showError(i0, "Value too large (maximum 100).");
                    return;
                }
                break;
        }

        prefs.setPreference(i0.getQPref(), i0.getText());
        prefs.save();
        i0.setPreviousText(i0.getText());
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
            default:
        }

        i0.setText(i0.getPreviousText());
    }

    private void resetErrors() {
        lblErrBooster.setVisible(false);
        lblErrDifficulty.setVisible(false);
        lblErrRewards.setVisible(false);
    }
}
