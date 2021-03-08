package forge.screens.home.quest;
import forge.gui.framework.DragCell;
import forge.gui.framework.DragTab;
import forge.gui.framework.EDocID;
import forge.localinstance.assets.FSkinProp;
import forge.model.FModel;
import forge.quest.data.QuestPreferences;
import forge.quest.data.QuestPreferences.QPref;
import forge.screens.home.EMenuGroup;
import forge.screens.home.IVSubmenu;
import forge.screens.home.VHomeUI;
import forge.toolbox.FLabel;
import forge.toolbox.FPanel;
import forge.toolbox.FScrollPane;
import forge.toolbox.FSkin;
import forge.toolbox.FSkin.SkinColor;
import forge.toolbox.FSkin.SkinnedTextField;
import forge.util.Localizer;
import net.miginfocom.swing.MigLayout;
import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
/**
 * Assembles Swing components of quest preferences submenu singleton.
 *
 * <br><br><i>(V at beginning of class name denotes a view class.)</i>
 *
 */
public enum VSubmenuQuestPrefs implements IVSubmenu<CSubmenuQuestPrefs> {
    /** */
    SINGLETON_INSTANCE;
    // Fields used with interface IVDoc
    private DragCell parentCell;
    final Localizer localizer = Localizer.getInstance();
    private final DragTab tab = new DragTab(localizer.getMessage("lblQuestPreferences"));
    /** */
    private final FLabel lblTitle = new FLabel.Builder()
            .text(localizer.getMessage("lblQuestPreferences")).fontAlign(SwingConstants.CENTER)
            .opaque(true).fontSize(16).build();
    private final JPanel pnlContent = new JPanel();
    private final FScrollPane scrContent = new FScrollPane(pnlContent, false);
    private final JPanel pnlRewards = new JPanel();
    private final JPanel pnlDifficulty = new JPanel();
    private final JPanel pnlBooster = new JPanel();
    private final JPanel pnlShop = new JPanel();
    private final JPanel pnlDraftTournaments = new JPanel();
    private final FLabel lblErrRewards = new FLabel.Builder().text(localizer.getMessage("lblRewardsError")).fontStyle(Font.BOLD).build();
    private final FLabel lblErrDifficulty = new FLabel.Builder().text(localizer.getMessage("lblDifficultyError")).fontStyle(Font.BOLD).build();
    private final FLabel lblErrBooster = new FLabel.Builder().text(localizer.getMessage("lblBoosterError")).fontStyle(Font.BOLD).build();
    private final FLabel lblErrShop = new FLabel.Builder().text(localizer.getMessage("lblShopError")).fontStyle(Font.BOLD).build();
    private final FLabel lblErrDraftTournaments = new FLabel.Builder().text(localizer.getMessage("lblDraftTournamentsError")).fontStyle(Font.BOLD).build();
    private final QuestPreferences prefs = FModel.getQuestPreferences();
    private PrefInput focusTarget;
    /** */
    public enum QuestPreferencesErrType { /** */
    REWARDS, /** */
    DIFFICULTY, /** */
    BOOSTER, /** */
    SHOP, /***/
    DRAFT_TOURNAMENTS
    }
    /**
     * Constructor.
     */
    VSubmenuQuestPrefs() {
        lblTitle.setBackground(FSkin.getColor(FSkin.Colors.CLR_THEME2));
        pnlContent.setOpaque(false);
        pnlContent.setLayout(new MigLayout("insets 0, gap 0, wrap"));
        lblErrRewards.setForeground(Color.red);
        lblErrDifficulty.setForeground(Color.red);
        lblErrBooster.setForeground(Color.red);
        lblErrShop.setForeground(Color.red);
        lblErrDraftTournaments.setForeground(Color.red);
        // Rewards panel
        final FPanel pnlTitleRewards = new FPanel();
        pnlTitleRewards.setLayout(new MigLayout("insets 0, align center"));
        pnlTitleRewards.setBackground(FSkin.getColor(FSkin.Colors.CLR_THEME2));
        pnlTitleRewards.add(new FLabel.Builder().text(localizer.getMessage("lblRewards"))
                .icon(FSkin.getIcon(FSkinProp.ICO_QUEST_COIN))
                .fontSize(16).build(), "h 95%!, gap 0 0 2.5% 0");
        pnlContent.add(pnlTitleRewards, "w 96%!, h 36px!, gap 2% 0 10px 20px");
        pnlContent.add(pnlRewards, "w 96%!, gap 2% 0 10px 20px");
        populateRewards();
        // Booster panel
        final FPanel pnlTitleBooster = new FPanel();
        pnlTitleBooster.setLayout(new MigLayout("insets 0, align center"));
        pnlTitleBooster.setBackground(FSkin.getColor(FSkin.Colors.CLR_THEME2));
        pnlTitleBooster.add(new FLabel.Builder().text(localizer.getMessage("lblBoosterPackRatios"))
                .icon(FSkin.getIcon(FSkinProp.ICO_QUEST_BOOK))
                .fontSize(16).build(), "h 95%!, gap 0 0 2.5% 0");
        pnlContent.add(pnlTitleBooster, "w 96%!, h 36px!, gap 2% 0 10px 10px");
        pnlContent.add(pnlBooster, "w 96%!, gap 2% 0 10px 20px");
        populateBooster();
        // Difficulty table panel
        final FPanel pnlTitleDifficulty = new FPanel();
        pnlTitleDifficulty.setLayout(new MigLayout("insets 0, align center"));
        pnlTitleDifficulty.setBackground(FSkin.getColor(FSkin.Colors.CLR_THEME2));
        pnlTitleDifficulty.add(new FLabel.Builder().text(localizer.getMessage("lblDifficultyAdjustments"))
                .icon(FSkin.getIcon(FSkinProp.ICO_QUEST_NOTES))
                .fontSize(16).build(), "h 95%!, gap 0 0 2.5% 0");
        pnlContent.add(pnlTitleDifficulty, "w 96%!, h 36px!, gap 2% 0 10px 10px");
        pnlContent.add(pnlDifficulty, "w 96%!, gap 2% 0 10px 20px");
        populateDifficulty();
        // Shop panel
        final FPanel pnlTitleShop = new FPanel();
        pnlTitleShop.setLayout(new MigLayout("insets 0, align center"));
        pnlTitleShop.setBackground(FSkin.getColor(FSkin.Colors.CLR_THEME2));
        pnlTitleShop.add(new FLabel.Builder().text(localizer.getMessage("lblShopPreferences"))
                .icon(FSkin.getIcon(FSkinProp.ICO_QUEST_COIN))
                .fontSize(16).build(), "h 95%!, gap 0 0 2.5% 0");
        pnlContent.add(pnlTitleShop, "w 96%!, h 36px!, gap 2% 0 10px 10px");
        pnlContent.add(pnlShop, "w 96%!, gap 2% 0 10px 20px");
        populateShop();
        // Draft tournaments panel
        final FPanel pnlTitleDraftTournaments = new FPanel();
        pnlTitleDraftTournaments.setLayout(new MigLayout("insets 0, align center"));
        pnlTitleDraftTournaments.setBackground(FSkin.getColor(FSkin.Colors.CLR_THEME2));
        pnlTitleDraftTournaments.add(new FLabel.Builder().text(localizer.getMessage("lblDraftTournaments"))
                .icon(FSkin.getIcon(FSkinProp.ICO_QUEST_COIN))
                .fontSize(16).build(), "h 95%!, gap 0 0 2.5% 0");
        pnlContent.add(pnlTitleDraftTournaments, "w 96%!, h 36px!, gap 2% 0 10px 10px");
        pnlContent.add(pnlDraftTournaments, "w 96%!, gap 2% 0 10px 20px");
        populateDraftTournaments();
    }
    /* (non-Javadoc)
     * @see forge.view.home.IViewSubmenu#populate()
     */
    @Override
    public void populate() {
        CSubmenuQuestPrefs.resetErrors();
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().removeAll();
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().setLayout(new MigLayout("insets 0, gap 0, wrap"));
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(lblTitle, "w 98%!, h 30px!, gap 1% 0 15px 15px");
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(scrContent, "w 98%!, growy, pushy, gap 1% 0 0 20px");
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().repaintSelf();
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().revalidate();
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
        return localizer.getMessage("lblQuestPreferences");
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
    /** @return {@link javax.swing.JLabel} */
    public JLabel getLblErrDraftTournaments() {
        return lblErrDraftTournaments;
    }
    public void focusFirstTextbox() {
        focusTarget.requestFocusInWindow();
    }
    private final static String fieldConstraints = "w 60px!, h 26px!";
    private final static String labelConstraints = "w 200px!, h 26px!, gap 0 10px 0 0";
    private void populateRewards() {
        pnlRewards.setOpaque(false);
        pnlRewards.setLayout(new MigLayout("insets 0px, gap 0, wrap 2, hidemode 3"));
        pnlRewards.removeAll();
        pnlRewards.add(lblErrRewards, "w 100%!, h 30px!, span 2 1");
        pnlRewards.add(new FLabel.Builder().text(localizer.getMessage("lblBaseWinnings")).fontAlign(SwingConstants.RIGHT).build(), labelConstraints);
        focusTarget = new PrefInput(QPref.REWARDS_BASE, QuestPreferencesErrType.REWARDS);
        pnlRewards.add(focusTarget, fieldConstraints);
        pnlRewards.add(new FLabel.Builder().text(localizer.getMessage("lblNoLosses")).fontAlign(SwingConstants.RIGHT).build(), labelConstraints);
        pnlRewards.add(new PrefInput(QPref.REWARDS_UNDEFEATED, QuestPreferencesErrType.REWARDS), fieldConstraints);
        pnlRewards.add(new FLabel.Builder().text(localizer.getMessage("lblPoisonWin")).fontAlign(SwingConstants.RIGHT).build(), labelConstraints);
        pnlRewards.add(new PrefInput(QPref.REWARDS_POISON, QuestPreferencesErrType.REWARDS), fieldConstraints);
        pnlRewards.add(new FLabel.Builder().text(localizer.getMessage("lblMillingWin")).fontAlign(SwingConstants.RIGHT).build(), labelConstraints);
        pnlRewards.add(new PrefInput(QPref.REWARDS_MILLED, QuestPreferencesErrType.REWARDS), fieldConstraints);
        pnlRewards.add(new FLabel.Builder().text(localizer.getMessage("lblMulligan0Win")).fontAlign(SwingConstants.RIGHT).build(), labelConstraints);
        pnlRewards.add(new PrefInput(QPref.REWARDS_MULLIGAN0, QuestPreferencesErrType.REWARDS), fieldConstraints);
        pnlRewards.add(new FLabel.Builder().text(localizer.getMessage("lblAlternativeWin")).fontAlign(SwingConstants.RIGHT).build(), labelConstraints);
        pnlRewards.add(new PrefInput(QPref.REWARDS_ALTERNATIVE, QuestPreferencesErrType.REWARDS), fieldConstraints);
        FLabel winMulti = new FLabel.Builder().text(localizer.getMessage("lblBonusMultiplierperWin")).fontAlign(SwingConstants.RIGHT).build();
        winMulti.setToolTipText(localizer.getMessage("ttBonusMultiplierperWin"));
        pnlRewards.add(winMulti, labelConstraints);
        pnlRewards.add(new PrefInput(QPref.REWARDS_WINS_MULTIPLIER, QuestPreferencesErrType.REWARDS), fieldConstraints);
        FLabel winMultiMax = new FLabel.Builder().text(localizer.getMessage("lblMaxWinsforMultiplier")).fontAlign(SwingConstants.RIGHT).build();
        winMultiMax.setToolTipText(localizer.getMessage("ttMaxWinsforMultiplier"));
        pnlRewards.add(winMultiMax, labelConstraints);
        pnlRewards.add(new PrefInput(QPref.REWARDS_WINS_MULTIPLIER_MAX, QuestPreferencesErrType.REWARDS), fieldConstraints);
        pnlRewards.add(new FLabel.Builder().text(localizer.getMessage("lblWinbyTurn15")).fontAlign(SwingConstants.RIGHT).build(), labelConstraints);
        pnlRewards.add(new PrefInput(QPref.REWARDS_TURN15, QuestPreferencesErrType.REWARDS), fieldConstraints);
        pnlRewards.add(new FLabel.Builder().text(localizer.getMessage("lblWinbyTurn10")).fontAlign(SwingConstants.RIGHT).build(), labelConstraints);
        pnlRewards.add(new PrefInput(QPref.REWARDS_TURN10, QuestPreferencesErrType.REWARDS), fieldConstraints);
        pnlRewards.add(new FLabel.Builder().text(localizer.getMessage("lblWinbyTurn5")).fontAlign(SwingConstants.RIGHT).build(), labelConstraints);
        pnlRewards.add(new PrefInput(QPref.REWARDS_TURN5, QuestPreferencesErrType.REWARDS), fieldConstraints);
        pnlRewards.add(new FLabel.Builder().text(localizer.getMessage("lblFirstTurnWin")).fontAlign(SwingConstants.RIGHT).build(), labelConstraints);
        pnlRewards.add(new PrefInput(QPref.REWARDS_TURN1, QuestPreferencesErrType.REWARDS), fieldConstraints);
        pnlRewards.add(new FLabel.Builder().text(localizer.getMessage("lblMaxLifeDiffBonus")).fontAlign(SwingConstants.RIGHT).build(), labelConstraints);
        pnlRewards.add(new PrefInput(QPref.REWARDS_HEALTH_DIFF_MAX, QuestPreferencesErrType.REWARDS), fieldConstraints);
        pnlRewards.add(new FLabel.Builder().text(localizer.getMessage("lblExcludePromosFromRewardPool")).fontAlign(SwingConstants.RIGHT).build(), labelConstraints);
        pnlRewards.add(new PrefInput(QPref.EXCLUDE_PROMOS_FROM_POOL, QuestPreferencesErrType.REWARDS), fieldConstraints);
    }
    private void populateDifficulty() {
        pnlDifficulty.setOpaque(false);
        pnlDifficulty.setLayout(new MigLayout("insets 0, gap 0, wrap 5, hidemode 3"));
        pnlDifficulty.removeAll();
        pnlDifficulty.add(lblErrDifficulty, "w 100%!, h 30px!, span 5 1");
        pnlDifficulty.add(new FLabel.Builder().text("").build(), labelConstraints);
        pnlDifficulty.add(new FLabel.Builder().text(localizer.getMessage("questDifficultyEasy")).build(), fieldConstraints);
        pnlDifficulty.add(new FLabel.Builder().text(localizer.getMessage("questDifficultyMedium")).build(), fieldConstraints);
        pnlDifficulty.add(new FLabel.Builder().text(localizer.getMessage("questDifficultyHard")).build(), fieldConstraints);
        pnlDifficulty.add(new FLabel.Builder().text(localizer.getMessage("questDifficultyExpert")).build(), fieldConstraints);
        pnlDifficulty.add(new FLabel.Builder().text(localizer.getMessage("lblWinsforBooster")).fontAlign(SwingConstants.RIGHT).build(), labelConstraints);
        pnlDifficulty.add(new PrefInput(QPref.WINS_BOOSTER_EASY, QuestPreferencesErrType.DIFFICULTY), fieldConstraints);
        pnlDifficulty.add(new PrefInput(QPref.WINS_BOOSTER_MEDIUM, QuestPreferencesErrType.DIFFICULTY), fieldConstraints);
        pnlDifficulty.add(new PrefInput(QPref.WINS_BOOSTER_HARD, QuestPreferencesErrType.DIFFICULTY), fieldConstraints);
        pnlDifficulty.add(new PrefInput(QPref.WINS_BOOSTER_EXPERT, QuestPreferencesErrType.DIFFICULTY), fieldConstraints);
        pnlDifficulty.add(new FLabel.Builder().text(localizer.getMessage("lblWinsforRankIncrease")).fontAlign(SwingConstants.RIGHT).build(), labelConstraints);
        pnlDifficulty.add(new PrefInput(QPref.WINS_RANKUP_EASY, QuestPreferencesErrType.DIFFICULTY), fieldConstraints);
        pnlDifficulty.add(new PrefInput(QPref.WINS_RANKUP_MEDIUM, QuestPreferencesErrType.DIFFICULTY), fieldConstraints);
        pnlDifficulty.add(new PrefInput(QPref.WINS_RANKUP_HARD, QuestPreferencesErrType.DIFFICULTY), fieldConstraints);
        pnlDifficulty.add(new PrefInput(QPref.WINS_RANKUP_EXPERT, QuestPreferencesErrType.DIFFICULTY), fieldConstraints);
        pnlDifficulty.add(new FLabel.Builder().text(localizer.getMessage("lblWinsforMediumAI")).fontAlign(SwingConstants.RIGHT).build(), labelConstraints);
        pnlDifficulty.add(new PrefInput(QPref.WINS_MEDIUMAI_EASY, QuestPreferencesErrType.DIFFICULTY), fieldConstraints);
        pnlDifficulty.add(new PrefInput(QPref.WINS_MEDIUMAI_MEDIUM, QuestPreferencesErrType.DIFFICULTY), fieldConstraints);
        pnlDifficulty.add(new PrefInput(QPref.WINS_MEDIUMAI_HARD, QuestPreferencesErrType.DIFFICULTY), fieldConstraints);
        pnlDifficulty.add(new PrefInput(QPref.WINS_MEDIUMAI_EXPERT, QuestPreferencesErrType.DIFFICULTY), fieldConstraints);
        pnlDifficulty.add(new FLabel.Builder().text(localizer.getMessage("lblWinsforHardAI")).fontAlign(SwingConstants.RIGHT).build(), labelConstraints);
        pnlDifficulty.add(new PrefInput(QPref.WINS_HARDAI_EASY, QuestPreferencesErrType.DIFFICULTY), fieldConstraints);
        pnlDifficulty.add(new PrefInput(QPref.WINS_HARDAI_MEDIUM, QuestPreferencesErrType.DIFFICULTY), fieldConstraints);
        pnlDifficulty.add(new PrefInput(QPref.WINS_HARDAI_HARD, QuestPreferencesErrType.DIFFICULTY), fieldConstraints);
        pnlDifficulty.add(new PrefInput(QPref.WINS_HARDAI_EXPERT, QuestPreferencesErrType.DIFFICULTY), fieldConstraints);
        pnlDifficulty.add(new FLabel.Builder().text(localizer.getMessage("lblWinsforExpertAI")).fontAlign(SwingConstants.RIGHT).build(), labelConstraints);
        pnlDifficulty.add(new PrefInput(QPref.WINS_EXPERTAI_EASY, QuestPreferencesErrType.DIFFICULTY), fieldConstraints);
        pnlDifficulty.add(new PrefInput(QPref.WINS_EXPERTAI_MEDIUM, QuestPreferencesErrType.DIFFICULTY), fieldConstraints);
        pnlDifficulty.add(new PrefInput(QPref.WINS_EXPERTAI_HARD, QuestPreferencesErrType.DIFFICULTY), fieldConstraints);
        pnlDifficulty.add(new PrefInput(QPref.WINS_EXPERTAI_EXPERT, QuestPreferencesErrType.DIFFICULTY), fieldConstraints);
        pnlDifficulty.add(new FLabel.Builder().text(localizer.getMessage("lblStartingCommons")).fontAlign(SwingConstants.RIGHT).build(), labelConstraints);
        pnlDifficulty.add(new PrefInput(QPref.STARTING_COMMONS_EASY, QuestPreferencesErrType.DIFFICULTY), fieldConstraints);
        pnlDifficulty.add(new PrefInput(QPref.STARTING_COMMONS_MEDIUM, QuestPreferencesErrType.DIFFICULTY), fieldConstraints);
        pnlDifficulty.add(new PrefInput(QPref.STARTING_COMMONS_HARD, QuestPreferencesErrType.DIFFICULTY), fieldConstraints);
        pnlDifficulty.add(new PrefInput(QPref.STARTING_COMMONS_EXPERT, QuestPreferencesErrType.DIFFICULTY), fieldConstraints);
        pnlDifficulty.add(new FLabel.Builder().text(localizer.getMessage("lblStartingUncommons")).fontAlign(SwingConstants.RIGHT).build(), labelConstraints);
        pnlDifficulty.add(new PrefInput(QPref.STARTING_UNCOMMONS_EASY, QuestPreferencesErrType.DIFFICULTY), fieldConstraints);
        pnlDifficulty.add(new PrefInput(QPref.STARTING_UNCOMMONS_MEDIUM, QuestPreferencesErrType.DIFFICULTY), fieldConstraints);
        pnlDifficulty.add(new PrefInput(QPref.STARTING_UNCOMMONS_HARD, QuestPreferencesErrType.DIFFICULTY), fieldConstraints);
        pnlDifficulty.add(new PrefInput(QPref.STARTING_UNCOMMONS_EXPERT, QuestPreferencesErrType.DIFFICULTY), fieldConstraints);
        pnlDifficulty.add(new FLabel.Builder().text(localizer.getMessage("lblStartingRares")).fontAlign(SwingConstants.RIGHT).build(), labelConstraints);
        pnlDifficulty.add(new PrefInput(QPref.STARTING_RARES_EASY, QuestPreferencesErrType.DIFFICULTY), fieldConstraints);
        pnlDifficulty.add(new PrefInput(QPref.STARTING_RARES_MEDIUM, QuestPreferencesErrType.DIFFICULTY), fieldConstraints);
        pnlDifficulty.add(new PrefInput(QPref.STARTING_RARES_HARD, QuestPreferencesErrType.DIFFICULTY), fieldConstraints);
        pnlDifficulty.add(new PrefInput(QPref.STARTING_RARES_EXPERT, QuestPreferencesErrType.DIFFICULTY), fieldConstraints);
        pnlDifficulty.add(new FLabel.Builder().text(localizer.getMessage("lblStartingCredits")).fontAlign(SwingConstants.RIGHT).build(), labelConstraints);
        pnlDifficulty.add(new PrefInput(QPref.STARTING_CREDITS_EASY, QuestPreferencesErrType.DIFFICULTY), fieldConstraints);
        pnlDifficulty.add(new PrefInput(QPref.STARTING_CREDITS_MEDIUM, QuestPreferencesErrType.DIFFICULTY), fieldConstraints);
        pnlDifficulty.add(new PrefInput(QPref.STARTING_CREDITS_HARD, QuestPreferencesErrType.DIFFICULTY), fieldConstraints);
        pnlDifficulty.add(new PrefInput(QPref.STARTING_CREDITS_EXPERT, QuestPreferencesErrType.DIFFICULTY), fieldConstraints);
        // Basic lands are no longer generated for quest mode (we now use Add Basic Lands)
        //pnlDifficulty.add(new FLabel.Builder().text("Starting Basic Lands").fontAlign(SwingConstants.RIGHT).build(), labelConstraints);
        //pnlDifficulty.add(new PrefInput(QPref.STARTING_BASIC_LANDS, QuestPreferencesErrType.DIFFICULTY), fieldConstraints + ", wrap");
        pnlDifficulty.add(new FLabel.Builder().text(localizer.getMessage("lblWinsforNewChallenge")).fontAlign(SwingConstants.RIGHT).build(), labelConstraints);
        pnlDifficulty.add(new PrefInput(QPref.WINS_NEW_CHALLENGE, QuestPreferencesErrType.DIFFICULTY), fieldConstraints + ", wrap");
        pnlDifficulty.add(new FLabel.Builder().text(localizer.getMessage("lblStartingSnowLands")).fontAlign(SwingConstants.RIGHT).build(), labelConstraints);
        pnlDifficulty.add(new PrefInput(QPref.STARTING_SNOW_LANDS, QuestPreferencesErrType.DIFFICULTY), fieldConstraints + ", wrap");
        FLabel colorBias = new FLabel.Builder().text(localizer.getMessage("lblColorBias")).fontAlign(SwingConstants.RIGHT).build();
        colorBias.setToolTipText(localizer.getMessage("ttColorBias"));
        pnlDifficulty.add(colorBias, labelConstraints);
        pnlDifficulty.add(new PrefInput(QPref.STARTING_POOL_COLOR_BIAS, QuestPreferencesErrType.DIFFICULTY), fieldConstraints + ", wrap");
        pnlDifficulty.add(new FLabel.Builder().text(localizer.getMessage("lblPenaltyforLoss")).fontAlign(SwingConstants.RIGHT).build(), labelConstraints);
        pnlDifficulty.add(new PrefInput(QPref.PENALTY_LOSS, QuestPreferencesErrType.DIFFICULTY), fieldConstraints + ", wrap");
        pnlDifficulty.add(new FLabel.Builder().text(localizer.getMessage("lblMoreDuelChoices")).fontAlign(SwingConstants.RIGHT).build(), labelConstraints);
        pnlDifficulty.add(new PrefInput(QPref.MORE_DUEL_CHOICES, QuestPreferencesErrType.DIFFICULTY), fieldConstraints + ", wrap");
        pnlDifficulty.add(new FLabel.Builder().text(localizer.getMessage("lblWildOpponentMultiplier")).fontAlign(SwingConstants.RIGHT).build(), labelConstraints);
        pnlDifficulty.add(new PrefInput(QPref.WILD_OPPONENTS_MULTIPLIER, QuestPreferencesErrType.DIFFICULTY), fieldConstraints + ", wrap");        
        pnlDifficulty.add(new FLabel.Builder().text(localizer.getMessage("lblWildOpponentNumber")).fontAlign(SwingConstants.RIGHT).build(), labelConstraints);
        pnlDifficulty.add(new PrefInput(QPref.WILD_OPPONENTS_NUMBER, QuestPreferencesErrType.DIFFICULTY), fieldConstraints + ", wrap");        
    }
    private void populateBooster() {
        pnlBooster.setOpaque(false);
        pnlBooster.setLayout(new MigLayout("insets 0, gap 0, wrap 2, hidemode 3"));
        pnlBooster.removeAll();
        pnlBooster.add(lblErrBooster, "w 100%!, h 30px!, span 2 1");
        pnlBooster.add(new FLabel.Builder().text(localizer.getMessage("lblCommon")).fontAlign(SwingConstants.RIGHT).build(), labelConstraints);
        pnlBooster.add(new PrefInput(QPref.BOOSTER_COMMONS, QuestPreferencesErrType.BOOSTER), fieldConstraints);
        pnlBooster.add(new FLabel.Builder().text(localizer.getMessage("lblUncommon")).fontAlign(SwingConstants.RIGHT).build(), labelConstraints);
        pnlBooster.add(new PrefInput(QPref.BOOSTER_UNCOMMONS, QuestPreferencesErrType.BOOSTER), fieldConstraints);
        pnlBooster.add(new FLabel.Builder().text(localizer.getMessage("lblRare")).fontAlign(SwingConstants.RIGHT).build(), labelConstraints);
        pnlBooster.add(new PrefInput(QPref.BOOSTER_RARES, QuestPreferencesErrType.BOOSTER), fieldConstraints);
        FLabel specialBoosters = new FLabel.Builder().text(localizer.getMessage("lblSpecialBoosters")).fontAlign(SwingConstants.RIGHT).build();
        specialBoosters.setToolTipText(localizer.getMessage("ttSpecialBoosters"));
        pnlBooster.add(specialBoosters, labelConstraints);
        pnlBooster.add(new PrefInput(QPref.SPECIAL_BOOSTERS, QuestPreferencesErrType.BOOSTER), fieldConstraints);
    }
    private void populateShop() {
        pnlShop.setOpaque(false);
        pnlShop.setLayout(new MigLayout("insets 0, gap 0, wrap 2, hidemode 3"));
        pnlShop.removeAll();
        pnlShop.add(lblErrShop, "w 100%!, h 30px!, span 2 1");
        pnlShop.add(new FLabel.Builder().text(localizer.getMessage("lblMaximumPacks")).fontAlign(SwingConstants.RIGHT).build(), labelConstraints);
        pnlShop.add(new PrefInput(QPref.SHOP_MAX_PACKS, QuestPreferencesErrType.SHOP), fieldConstraints);
        pnlShop.add(new FLabel.Builder().text(localizer.getMessage("lblMinimumPacks")).fontAlign(SwingConstants.RIGHT).build(), labelConstraints);
        pnlShop.add(new PrefInput(QPref.SHOP_MIN_PACKS, QuestPreferencesErrType.SHOP), fieldConstraints);
        pnlShop.add(new FLabel.Builder().text(localizer.getMessage("lblStartingPacks")).fontAlign(SwingConstants.RIGHT).build(), labelConstraints);
        pnlShop.add(new PrefInput(QPref.SHOP_STARTING_PACKS, QuestPreferencesErrType.SHOP), fieldConstraints);
        pnlShop.add(new FLabel.Builder().text(localizer.getMessage("lblWinsforPack")).fontAlign(SwingConstants.RIGHT).build(), labelConstraints);
        pnlShop.add(new PrefInput(QPref.SHOP_WINS_FOR_ADDITIONAL_PACK, QuestPreferencesErrType.SHOP), fieldConstraints);
        pnlShop.add(new FLabel.Builder().text(localizer.getMessage("lblWinsperSetUnlock")).fontAlign(SwingConstants.RIGHT).build(), labelConstraints);
        pnlShop.add(new PrefInput(QPref.WINS_UNLOCK_SET, QuestPreferencesErrType.SHOP), fieldConstraints);
        pnlShop.add(new FLabel.Builder().text(localizer.getMessage("lblAllowFarUnlocks")).fontAlign(SwingConstants.RIGHT).build(), labelConstraints);
        pnlShop.add(new PrefInput(QPref.UNLIMITED_UNLOCKING, QuestPreferencesErrType.SHOP), fieldConstraints);
        pnlShop.add(new FLabel.Builder().text(localizer.getMessage("lblUnlockDistanceMultiplier")).fontAlign(SwingConstants.RIGHT).build(), labelConstraints);
        pnlShop.add(new PrefInput(QPref.UNLOCK_DISTANCE_MULTIPLIER, QuestPreferencesErrType.SHOP), fieldConstraints);
        pnlShop.add(new FLabel.Builder().text(localizer.getMessage("lblCommonSingles")).fontAlign(SwingConstants.RIGHT).build(), labelConstraints);
        pnlShop.add(new PrefInput(QPref.SHOP_SINGLES_COMMON, QuestPreferencesErrType.SHOP), fieldConstraints);
        pnlShop.add(new FLabel.Builder().text(localizer.getMessage("lblUncommonSingles")).fontAlign(SwingConstants.RIGHT).build(), labelConstraints);
        pnlShop.add(new PrefInput(QPref.SHOP_SINGLES_UNCOMMON, QuestPreferencesErrType.SHOP), fieldConstraints);
        pnlShop.add(new FLabel.Builder().text(localizer.getMessage("lblRareSingles")).fontAlign(SwingConstants.RIGHT).build(), labelConstraints);
        pnlShop.add(new PrefInput(QPref.SHOP_SINGLES_RARE, QuestPreferencesErrType.SHOP), fieldConstraints);
        pnlShop.add(new FLabel.Builder().text(localizer.getMessage("lblCardSalePercentageBase")).fontAlign(SwingConstants.RIGHT).build(), labelConstraints);
        pnlShop.add(new PrefInput(QPref.SHOP_SELLING_PERCENTAGE_BASE, QuestPreferencesErrType.SHOP), fieldConstraints);
        pnlShop.add(new FLabel.Builder().text(localizer.getMessage("lblCardSalePercentageCap")).fontAlign(SwingConstants.RIGHT).build(), labelConstraints);
        pnlShop.add(new PrefInput(QPref.SHOP_SELLING_PERCENTAGE_MAX, QuestPreferencesErrType.SHOP), fieldConstraints);
        pnlShop.add(new FLabel.Builder().text(localizer.getMessage("lblCardSalePriceCap")).fontAlign(SwingConstants.RIGHT).build(), labelConstraints);
        pnlShop.add(new PrefInput(QPref.SHOP_MAX_SELLING_PRICE, QuestPreferencesErrType.SHOP), fieldConstraints);
        pnlShop.add(new FLabel.Builder().text(localizer.getMessage("lblWinstoUncapSalePrice")).fontAlign(SwingConstants.RIGHT).build(), labelConstraints);
        pnlShop.add(new PrefInput(QPref.SHOP_WINS_FOR_NO_SELL_LIMIT, QuestPreferencesErrType.SHOP), fieldConstraints);
        FLabel extraCardsToKeep = new FLabel.Builder().text(localizer.getMessage("lblPlaysetSize")).fontAlign(SwingConstants.RIGHT).build();
        extraCardsToKeep.setToolTipText(localizer.getMessage("ttPlaysetSize"));
        pnlShop.add(extraCardsToKeep, labelConstraints);
        pnlShop.add(new PrefInput(QPref.PLAYSET_SIZE, QuestPreferencesErrType.DIFFICULTY), fieldConstraints + ", wrap");
        FLabel extraLandsToKeep = new FLabel.Builder().text(localizer.getMessage("lblPlaysetSizeBasicLand")).fontAlign(SwingConstants.RIGHT).build();
        extraLandsToKeep.setToolTipText(localizer.getMessage("ttPlaysetSizeBasicLand"));
        pnlShop.add(extraLandsToKeep, labelConstraints);
        pnlShop.add(new PrefInput(QPref.PLAYSET_BASIC_LAND_SIZE, QuestPreferencesErrType.DIFFICULTY), fieldConstraints + ", wrap");
        FLabel infiniteToKeep = new FLabel.Builder().text(localizer.getMessage("lblPlaysetSizeAnyNumber")).fontAlign(SwingConstants.RIGHT).build();
        infiniteToKeep.setToolTipText(localizer.getMessage("ttPlaysetSizeAnyNumber"));
        pnlShop.add(infiniteToKeep, labelConstraints);
        pnlShop.add(new PrefInput(QPref.PLAYSET_ANY_NUMBER_SIZE, QuestPreferencesErrType.DIFFICULTY), fieldConstraints + ", wrap");
        pnlShop.add(new FLabel.Builder().text(localizer.getMessage("lblItemLevelRestriction")).fontAlign(SwingConstants.RIGHT).build(), labelConstraints);
        pnlShop.add(new PrefInput(QPref.ITEM_LEVEL_RESTRICTION, QuestPreferencesErrType.SHOP), fieldConstraints);
        pnlShop.add(new FLabel.Builder().text(localizer.getMessage("lblFoilfilterAlwaysOn")).fontAlign(SwingConstants.RIGHT).build(), labelConstraints);
        pnlShop.add(new PrefInput(QPref.FOIL_FILTER_DEFAULT, QuestPreferencesErrType.SHOP), fieldConstraints);
        pnlShop.add(new FLabel.Builder().text(localizer.getMessage("lblRatingsfilterAlwaysOn")).fontAlign(SwingConstants.RIGHT).build(), labelConstraints);
        pnlShop.add(new PrefInput(QPref.RATING_FILTER_DEFAULT, QuestPreferencesErrType.SHOP), fieldConstraints);
    }
    private void populateDraftTournaments() {
        pnlDraftTournaments.setOpaque(false);
        pnlDraftTournaments.setLayout(new MigLayout("insets 0, gap 0, wrap 2, hidemode 3"));
        pnlDraftTournaments.removeAll();
        pnlDraftTournaments.add(lblErrDraftTournaments, "w 100%!, h 30px!, span 2 1");
        FLabel randomAIMatches = new FLabel.Builder().text(localizer.getMessage("lblSimulateAIvsAIResults")).fontAlign(SwingConstants.RIGHT).build();
        randomAIMatches.setToolTipText(localizer.getMessage("ttSimulateAIvsAIResults"));
        pnlDraftTournaments.add(randomAIMatches, labelConstraints);
        pnlDraftTournaments.add(new PrefInput(QPref.SIMULATE_AI_VS_AI_RESULTS, QuestPreferencesErrType.DRAFT_TOURNAMENTS), fieldConstraints);
        pnlDraftTournaments.add(new FLabel.Builder().text(localizer.getMessage("lblWinsforNewDraft")).fontAlign(SwingConstants.RIGHT).build(), labelConstraints);
        pnlDraftTournaments.add(new PrefInput(QPref.WINS_NEW_DRAFT, QuestPreferencesErrType.DIFFICULTY), fieldConstraints + ", wrap");
        FLabel rotationAmount = new FLabel.Builder().text(localizer.getMessage("lblWinsperDraftRotation")).fontAlign(SwingConstants.RIGHT).build();
        rotationAmount.setToolTipText(localizer.getMessage("ttWinsperDraftRotation"));
        pnlDraftTournaments.add(rotationAmount, labelConstraints);
        pnlDraftTournaments.add(new PrefInput(QPref.WINS_ROTATE_DRAFT, QuestPreferencesErrType.DIFFICULTY), fieldConstraints + ", wrap");
        FLabel rotationType = new FLabel.Builder().text(localizer.getMessage("lblRotationType")).fontAlign(SwingConstants.RIGHT).build();
        rotationType.setToolTipText(localizer.getMessage("ttRotationType"));
        pnlDraftTournaments.add(rotationType, labelConstraints);
        pnlDraftTournaments.add(new PrefInput(QPref.DRAFT_ROTATION, QuestPreferencesErrType.DIFFICULTY), fieldConstraints + ", wrap");
    }
    /** */
    @SuppressWarnings("serial")
    public class PrefInput extends SkinnedTextField {
        private final QPref qpref;
        private final QuestPreferencesErrType err;
        private final SkinColor clrHover, clrActive, clrText;
        private boolean isFocus = false;
        private String previousText = "";
        /**
         * @param qp0 &emsp; {@link forge.quest.data.QuestPreferences.QPref}
         *                  preferences ident enum
         * @param e0 &emsp; {@link forge.screens.home.quest.VSubmenuQuestPrefs.QuestPreferencesErrType}
         *                  where error should display to
         */
        public PrefInput(final QPref qp0, final QuestPreferencesErrType e0) {
            super();
            this.qpref = qp0;
            this.err = e0;
            this.clrHover = FSkin.getColor(FSkin.Colors.CLR_HOVER);
            this.clrActive = FSkin.getColor(FSkin.Colors.CLR_ACTIVE);
            this.clrText = FSkin.getColor(FSkin.Colors.CLR_TEXT);
            this.setOpaque(false);
            this.setBorder((Border)null);
            this.setFont(FSkin.getRelativeFont(13));
            this.setForeground(clrText);
            this.setCaretColor(clrText);
            this.setBackground(clrHover);
            this.setHorizontalAlignment(SwingConstants.CENTER);
            this.setText(prefs.getPref(qpref));
            this.setPreviousText(prefs.getPref(qpref));
            this.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(final MouseEvent e) {
                    if (isFocus) { return; }
                    setOpaque(true);
                    repaint();
                }
                @Override
                public void mouseExited(final MouseEvent e) {
                    if (isFocus) { return; }
                    setOpaque(false);
                    repaint();
                }
            });
            this.addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(final FocusEvent e) {
                    isFocus = true;
                    setOpaque(true);
                    setBackground(clrActive);
                }
                @Override
                public void focusLost(final FocusEvent e) {
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
        /** @return {@link forge.screens.home.quest.VSubmenuQuestPrefs.QuestPreferencesErrType} */
        public QuestPreferencesErrType getErrType() {
            return this.err;
        }
        /** @return {@link java.lang.String} */
        public String getPreviousText() {
            return this.previousText;
        }
        /** @param s0 &emsp; {@link java.lang.String} */
        public void setPreviousText(final String s0) {
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
    public CSubmenuQuestPrefs getLayoutControl() {
        return CSubmenuQuestPrefs.SINGLETON_INSTANCE;
    }
    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#setParentCell(forge.gui.framework.DragCell)
     */
    @Override
    public void setParentCell(final DragCell cell0) {
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

