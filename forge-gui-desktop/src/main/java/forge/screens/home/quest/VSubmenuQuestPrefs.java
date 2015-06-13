package forge.screens.home.quest;

import forge.assets.FSkinProp;
import forge.gui.framework.DragCell;
import forge.gui.framework.DragTab;
import forge.gui.framework.EDocID;
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
    private final DragTab tab = new DragTab("Quest Preferences");

    /** */
    private final FLabel lblTitle = new FLabel.Builder()
    .text("Quest Preferences").fontAlign(SwingConstants.CENTER)
    .opaque(true).fontSize(16).build();

    private final JPanel pnlContent = new JPanel();
    private final FScrollPane scrContent = new FScrollPane(pnlContent, false);

    private final JPanel pnlRewards = new JPanel();
    private final JPanel pnlDifficulty = new JPanel();
    private final JPanel pnlBooster = new JPanel();
    private final JPanel pnlShop = new JPanel();

    private final FLabel lblErrRewards = new FLabel.Builder().text("Rewards Error").fontStyle(Font.BOLD).build();
    private final FLabel lblErrDifficulty = new FLabel.Builder().text("Difficulty Error").fontStyle(Font.BOLD).build();
    private final FLabel lblErrBooster = new FLabel.Builder().text("Booster Error").fontStyle(Font.BOLD).build();
    private final FLabel lblErrShop = new FLabel.Builder().text("Shop Error").fontStyle(Font.BOLD).build();

    private final QuestPreferences prefs = FModel.getQuestPreferences();
    private PrefInput focusTarget;

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
    VSubmenuQuestPrefs() {
        lblTitle.setBackground(FSkin.getColor(FSkin.Colors.CLR_THEME2));

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
                .icon(FSkin.getIcon(FSkinProp.ICO_QUEST_COIN))
                .fontSize(16).build(), "h 95%!, gap 0 0 2.5% 0");

        pnlContent.add(pnlTitleRewards, "w 96%!, h 36px!, gap 2% 0 10px 20px");
        pnlContent.add(pnlRewards, "w 96%!, gap 2% 0 10px 20px");
        populateRewards();

        // Booster panel
        final FPanel pnlTitleBooster = new FPanel();
        pnlTitleBooster.setLayout(new MigLayout("insets 0, align center"));
        pnlTitleBooster.setBackground(FSkin.getColor(FSkin.Colors.CLR_THEME2));
        pnlTitleBooster.add(new FLabel.Builder().text("Booster Pack Ratios")
                .icon(FSkin.getIcon(FSkinProp.ICO_QUEST_BOOK))
                .fontSize(16).build(), "h 95%!, gap 0 0 2.5% 0");
        pnlContent.add(pnlTitleBooster, "w 96%!, h 36px!, gap 2% 0 10px 10px");
        pnlContent.add(pnlBooster, "w 96%!, gap 2% 0 10px 20px");
        populateBooster();

        // Difficulty table panel
        final FPanel pnlTitleDifficulty = new FPanel();
        pnlTitleDifficulty.setLayout(new MigLayout("insets 0, align center"));
        pnlTitleDifficulty.setBackground(FSkin.getColor(FSkin.Colors.CLR_THEME2));
        pnlTitleDifficulty.add(new FLabel.Builder().text("Difficulty Adjustments")
                .icon(FSkin.getIcon(FSkinProp.ICO_QUEST_NOTES))
                .fontSize(16).build(), "h 95%!, gap 0 0 2.5% 0");
        pnlContent.add(pnlTitleDifficulty, "w 96%!, h 36px!, gap 2% 0 10px 10px");
        pnlContent.add(pnlDifficulty, "w 96%!, gap 2% 0 10px 20px");
        populateDifficulty();

        // Shop panel
        final FPanel pnlTitleShop = new FPanel();
        pnlTitleShop.setLayout(new MigLayout("insets 0, align center"));
        pnlTitleShop.setBackground(FSkin.getColor(FSkin.Colors.CLR_THEME2));
        pnlTitleShop.add(new FLabel.Builder().text("Shop Preferences")
                .icon(FSkin.getIcon(FSkinProp.ICO_QUEST_COIN))
                .fontSize(16).build(), "h 95%!, gap 0 0 2.5% 0");
        pnlContent.add(pnlTitleShop, "w 96%!, h 36px!, gap 2% 0 10px 10px");
        pnlContent.add(pnlShop, "w 96%!, gap 2% 0 10px 20px");
        populateShop();
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

        pnlRewards.add(new FLabel.Builder().text("Base Winnings").fontAlign(SwingConstants.RIGHT).build(), labelConstraints);
        focusTarget = new PrefInput(QPref.REWARDS_BASE, QuestPreferencesErrType.REWARDS);
        pnlRewards.add(focusTarget, fieldConstraints);

        pnlRewards.add(new FLabel.Builder().text("No Losses").fontAlign(SwingConstants.RIGHT).build(), labelConstraints);
        pnlRewards.add(new PrefInput(QPref.REWARDS_UNDEFEATED, QuestPreferencesErrType.REWARDS), fieldConstraints);

        pnlRewards.add(new FLabel.Builder().text("Poison Win").fontAlign(SwingConstants.RIGHT).build(), labelConstraints);
        pnlRewards.add(new PrefInput(QPref.REWARDS_POISON, QuestPreferencesErrType.REWARDS), fieldConstraints);

        pnlRewards.add(new FLabel.Builder().text("Milling Win").fontAlign(SwingConstants.RIGHT).build(), labelConstraints);
        pnlRewards.add(new PrefInput(QPref.REWARDS_MILLED, QuestPreferencesErrType.REWARDS), fieldConstraints);

        pnlRewards.add(new FLabel.Builder().text("Mulligan 0 Win").fontAlign(SwingConstants.RIGHT).build(), labelConstraints);
        pnlRewards.add(new PrefInput(QPref.REWARDS_MULLIGAN0, QuestPreferencesErrType.REWARDS), fieldConstraints);

        pnlRewards.add(new FLabel.Builder().text("Alternative Win").fontAlign(SwingConstants.RIGHT).build(), labelConstraints);
        pnlRewards.add(new PrefInput(QPref.REWARDS_ALTERNATIVE, QuestPreferencesErrType.REWARDS), fieldConstraints);

        pnlRewards.add(new FLabel.Builder().text("Win by Turn 15").fontAlign(SwingConstants.RIGHT).build(), labelConstraints);
        pnlRewards.add(new PrefInput(QPref.REWARDS_TURN15, QuestPreferencesErrType.REWARDS), fieldConstraints);

        pnlRewards.add(new FLabel.Builder().text("Win by Turn 10").fontAlign(SwingConstants.RIGHT).build(), labelConstraints);
        pnlRewards.add(new PrefInput(QPref.REWARDS_TURN10, QuestPreferencesErrType.REWARDS), fieldConstraints);

        pnlRewards.add(new FLabel.Builder().text("Win by Turn 5").fontAlign(SwingConstants.RIGHT).build(), labelConstraints);
        pnlRewards.add(new PrefInput(QPref.REWARDS_TURN5, QuestPreferencesErrType.REWARDS), fieldConstraints);

        pnlRewards.add(new FLabel.Builder().text("First Turn Win").fontAlign(SwingConstants.RIGHT).build(), labelConstraints);
        pnlRewards.add(new PrefInput(QPref.REWARDS_TURN1, QuestPreferencesErrType.REWARDS), fieldConstraints);
    }

    private void populateDifficulty() {

        pnlDifficulty.setOpaque(false);
        pnlDifficulty.setLayout(new MigLayout("insets 0, gap 0, wrap 5, hidemode 3"));
        pnlDifficulty.removeAll();
        pnlDifficulty.add(lblErrDifficulty, "w 100%!, h 30px!, span 5 1");

        pnlDifficulty.add(new FLabel.Builder().text("").build(), labelConstraints);
        pnlDifficulty.add(new FLabel.Builder().text("Easy").build(), fieldConstraints);
        pnlDifficulty.add(new FLabel.Builder().text("Medium").build(), fieldConstraints);
        pnlDifficulty.add(new FLabel.Builder().text("Hard").build(), fieldConstraints);
        pnlDifficulty.add(new FLabel.Builder().text("Expert").build(), fieldConstraints);

        pnlDifficulty.add(new FLabel.Builder().text("Wins for Booster").fontAlign(SwingConstants.RIGHT).build(), labelConstraints);
        pnlDifficulty.add(new PrefInput(QPref.WINS_BOOSTER_EASY, QuestPreferencesErrType.DIFFICULTY), fieldConstraints);
        pnlDifficulty.add(new PrefInput(QPref.WINS_BOOSTER_MEDIUM, QuestPreferencesErrType.DIFFICULTY), fieldConstraints);
        pnlDifficulty.add(new PrefInput(QPref.WINS_BOOSTER_HARD, QuestPreferencesErrType.DIFFICULTY), fieldConstraints);
        pnlDifficulty.add(new PrefInput(QPref.WINS_BOOSTER_EXPERT, QuestPreferencesErrType.DIFFICULTY), fieldConstraints);

        pnlDifficulty.add(new FLabel.Builder().text("Wins for Rank Increase").fontAlign(SwingConstants.RIGHT).build(), labelConstraints);
        pnlDifficulty.add(new PrefInput(QPref.WINS_RANKUP_EASY, QuestPreferencesErrType.DIFFICULTY), fieldConstraints);
        pnlDifficulty.add(new PrefInput(QPref.WINS_RANKUP_MEDIUM, QuestPreferencesErrType.DIFFICULTY), fieldConstraints);
        pnlDifficulty.add(new PrefInput(QPref.WINS_RANKUP_HARD, QuestPreferencesErrType.DIFFICULTY), fieldConstraints);
        pnlDifficulty.add(new PrefInput(QPref.WINS_RANKUP_EXPERT, QuestPreferencesErrType.DIFFICULTY), fieldConstraints);

        pnlDifficulty.add(new FLabel.Builder().text("Wins for Medium AI").fontAlign(SwingConstants.RIGHT).build(), labelConstraints);
        pnlDifficulty.add(new PrefInput(QPref.WINS_MEDIUMAI_EASY, QuestPreferencesErrType.DIFFICULTY), fieldConstraints);
        pnlDifficulty.add(new PrefInput(QPref.WINS_MEDIUMAI_MEDIUM, QuestPreferencesErrType.DIFFICULTY), fieldConstraints);
        pnlDifficulty.add(new PrefInput(QPref.WINS_MEDIUMAI_HARD, QuestPreferencesErrType.DIFFICULTY), fieldConstraints);
        pnlDifficulty.add(new PrefInput(QPref.WINS_MEDIUMAI_EXPERT, QuestPreferencesErrType.DIFFICULTY), fieldConstraints);

        pnlDifficulty.add(new FLabel.Builder().text("Wins for Hard AI").fontAlign(SwingConstants.RIGHT).build(), labelConstraints);
        pnlDifficulty.add(new PrefInput(QPref.WINS_HARDAI_EASY, QuestPreferencesErrType.DIFFICULTY), fieldConstraints);
        pnlDifficulty.add(new PrefInput(QPref.WINS_HARDAI_MEDIUM, QuestPreferencesErrType.DIFFICULTY), fieldConstraints);
        pnlDifficulty.add(new PrefInput(QPref.WINS_HARDAI_HARD, QuestPreferencesErrType.DIFFICULTY), fieldConstraints);
        pnlDifficulty.add(new PrefInput(QPref.WINS_HARDAI_EXPERT, QuestPreferencesErrType.DIFFICULTY), fieldConstraints);

        pnlDifficulty.add(new FLabel.Builder().text("Wins for Expert AI").fontAlign(SwingConstants.RIGHT).build(), labelConstraints);
        pnlDifficulty.add(new PrefInput(QPref.WINS_EXPERTAI_EASY, QuestPreferencesErrType.DIFFICULTY), fieldConstraints);
        pnlDifficulty.add(new PrefInput(QPref.WINS_EXPERTAI_MEDIUM, QuestPreferencesErrType.DIFFICULTY), fieldConstraints);
        pnlDifficulty.add(new PrefInput(QPref.WINS_EXPERTAI_HARD, QuestPreferencesErrType.DIFFICULTY), fieldConstraints);
        pnlDifficulty.add(new PrefInput(QPref.WINS_EXPERTAI_EXPERT, QuestPreferencesErrType.DIFFICULTY), fieldConstraints);

        pnlDifficulty.add(new FLabel.Builder().text("Starting Commons").fontAlign(SwingConstants.RIGHT).build(), labelConstraints);
        pnlDifficulty.add(new PrefInput(QPref.STARTING_COMMONS_EASY, QuestPreferencesErrType.DIFFICULTY), fieldConstraints);
        pnlDifficulty.add(new PrefInput(QPref.STARTING_COMMONS_MEDIUM, QuestPreferencesErrType.DIFFICULTY), fieldConstraints);
        pnlDifficulty.add(new PrefInput(QPref.STARTING_COMMONS_HARD, QuestPreferencesErrType.DIFFICULTY), fieldConstraints);
        pnlDifficulty.add(new PrefInput(QPref.STARTING_COMMONS_EXPERT, QuestPreferencesErrType.DIFFICULTY), fieldConstraints);

        pnlDifficulty.add(new FLabel.Builder().text("Starting Uncommons").fontAlign(SwingConstants.RIGHT).build(), labelConstraints);
        pnlDifficulty.add(new PrefInput(QPref.STARTING_UNCOMMONS_EASY, QuestPreferencesErrType.DIFFICULTY), fieldConstraints);
        pnlDifficulty.add(new PrefInput(QPref.STARTING_UNCOMMONS_MEDIUM, QuestPreferencesErrType.DIFFICULTY), fieldConstraints);
        pnlDifficulty.add(new PrefInput(QPref.STARTING_UNCOMMONS_HARD, QuestPreferencesErrType.DIFFICULTY), fieldConstraints);
        pnlDifficulty.add(new PrefInput(QPref.STARTING_UNCOMMONS_EXPERT, QuestPreferencesErrType.DIFFICULTY), fieldConstraints);

        pnlDifficulty.add(new FLabel.Builder().text("Starting Rares").fontAlign(SwingConstants.RIGHT).build(), labelConstraints);
        pnlDifficulty.add(new PrefInput(QPref.STARTING_RARES_EASY, QuestPreferencesErrType.DIFFICULTY), fieldConstraints);
        pnlDifficulty.add(new PrefInput(QPref.STARTING_RARES_MEDIUM, QuestPreferencesErrType.DIFFICULTY), fieldConstraints);
        pnlDifficulty.add(new PrefInput(QPref.STARTING_RARES_HARD, QuestPreferencesErrType.DIFFICULTY), fieldConstraints);
        pnlDifficulty.add(new PrefInput(QPref.STARTING_RARES_EXPERT, QuestPreferencesErrType.DIFFICULTY), fieldConstraints);

        pnlDifficulty.add(new FLabel.Builder().text("Starting Credits").fontAlign(SwingConstants.RIGHT).build(), labelConstraints);
        pnlDifficulty.add(new PrefInput(QPref.STARTING_CREDITS_EASY, QuestPreferencesErrType.DIFFICULTY), fieldConstraints);
        pnlDifficulty.add(new PrefInput(QPref.STARTING_CREDITS_MEDIUM, QuestPreferencesErrType.DIFFICULTY), fieldConstraints);
        pnlDifficulty.add(new PrefInput(QPref.STARTING_CREDITS_HARD, QuestPreferencesErrType.DIFFICULTY), fieldConstraints);
        pnlDifficulty.add(new PrefInput(QPref.STARTING_CREDITS_EXPERT, QuestPreferencesErrType.DIFFICULTY), fieldConstraints);

        pnlDifficulty.add(new FLabel.Builder().text("Starting Basic Lands").fontAlign(SwingConstants.RIGHT).build(), labelConstraints);
        pnlDifficulty.add(new PrefInput(QPref.STARTING_BASIC_LANDS, QuestPreferencesErrType.DIFFICULTY), fieldConstraints + ", wrap");

        pnlDifficulty.add(new FLabel.Builder().text("Wins for New Draft").fontAlign(SwingConstants.RIGHT).build(), labelConstraints);
        pnlDifficulty.add(new PrefInput(QPref.WINS_NEW_DRAFT, QuestPreferencesErrType.DIFFICULTY), fieldConstraints + ", wrap");

        pnlDifficulty.add(new FLabel.Builder().text("Wins per Draft Rotation").fontAlign(SwingConstants.RIGHT).build(), labelConstraints);
        pnlDifficulty.add(new PrefInput(QPref.WINS_ROTATE_DRAFT, QuestPreferencesErrType.DIFFICULTY), fieldConstraints + ", wrap");

        pnlDifficulty.add(new FLabel.Builder().text("Starting Snow Lands").fontAlign(SwingConstants.RIGHT).build(), labelConstraints);
        pnlDifficulty.add(new PrefInput(QPref.STARTING_SNOW_LANDS, QuestPreferencesErrType.DIFFICULTY), fieldConstraints + ", wrap");

        FLabel colorBias = new FLabel.Builder().text("Color Bias (1-100%)").fontAlign(SwingConstants.RIGHT).build();
        colorBias.setToolTipText("The percentage of cards in your starting pool that will be the colors you select.");
        pnlDifficulty.add(colorBias, labelConstraints);
        pnlDifficulty.add(new PrefInput(QPref.STARTING_POOL_COLOR_BIAS, QuestPreferencesErrType.DIFFICULTY), fieldConstraints + ", wrap");

        pnlDifficulty.add(new FLabel.Builder().text("Penalty for Loss").fontAlign(SwingConstants.RIGHT).build(), labelConstraints);
        pnlDifficulty.add(new PrefInput(QPref.PENALTY_LOSS, QuestPreferencesErrType.DIFFICULTY), fieldConstraints + ", wrap");

    }

    private void populateBooster() {

        pnlBooster.setOpaque(false);
        pnlBooster.setLayout(new MigLayout("insets 0, gap 0, wrap 2, hidemode 3"));
        pnlBooster.removeAll();
        pnlBooster.add(lblErrBooster, "w 100%!, h 30px!, span 2 1");

        pnlBooster.add(new FLabel.Builder().text("Common").fontAlign(SwingConstants.RIGHT).build(), labelConstraints);
        pnlBooster.add(new PrefInput(QPref.BOOSTER_COMMONS, QuestPreferencesErrType.BOOSTER), fieldConstraints);

        pnlBooster.add(new FLabel.Builder().text("Uncommon").fontAlign(SwingConstants.RIGHT).build(), labelConstraints);
        pnlBooster.add(new PrefInput(QPref.BOOSTER_UNCOMMONS, QuestPreferencesErrType.BOOSTER), fieldConstraints);

        pnlBooster.add(new FLabel.Builder().text("Rare").fontAlign(SwingConstants.RIGHT).build(), labelConstraints);
        pnlBooster.add(new PrefInput(QPref.BOOSTER_RARES, QuestPreferencesErrType.BOOSTER), fieldConstraints);

        FLabel specialBoosters = new FLabel.Builder().text("Special Boosters").fontAlign(SwingConstants.RIGHT).build();
        specialBoosters.setToolTipText("Allows special, color-specific boosters to appear in the shop and as match rewards.");
        pnlBooster.add(specialBoosters, labelConstraints);
        pnlBooster.add(new PrefInput(QPref.SPECIAL_BOOSTERS, QuestPreferencesErrType.BOOSTER), fieldConstraints);

    }

    private void populateShop() {

        pnlShop.setOpaque(false);
        pnlShop.setLayout(new MigLayout("insets 0, gap 0, wrap 2, hidemode 3"));
        pnlShop.removeAll();
        pnlShop.add(lblErrShop, "w 100%!, h 30px!, span 2 1");

        pnlShop.add(new FLabel.Builder().text("Maximum Packs").fontAlign(SwingConstants.RIGHT).build(), labelConstraints);
        pnlShop.add(new PrefInput(QPref.SHOP_MAX_PACKS, QuestPreferencesErrType.SHOP), fieldConstraints);

        pnlShop.add(new FLabel.Builder().text("Minimum Packs").fontAlign(SwingConstants.RIGHT).build(), labelConstraints);
        pnlShop.add(new PrefInput(QPref.SHOP_MIN_PACKS, QuestPreferencesErrType.SHOP), fieldConstraints);

        pnlShop.add(new FLabel.Builder().text("Starting Packs").fontAlign(SwingConstants.RIGHT).build(), labelConstraints);
        pnlShop.add(new PrefInput(QPref.SHOP_STARTING_PACKS, QuestPreferencesErrType.SHOP), fieldConstraints);

        pnlShop.add(new FLabel.Builder().text("Wins for Pack").fontAlign(SwingConstants.RIGHT).build(), labelConstraints);
        pnlShop.add(new PrefInput(QPref.SHOP_WINS_FOR_ADDITIONAL_PACK, QuestPreferencesErrType.SHOP), fieldConstraints);

        pnlShop.add(new FLabel.Builder().text("Wins per Set Unlock").fontAlign(SwingConstants.RIGHT).build(), labelConstraints);
        pnlShop.add(new PrefInput(QPref.WINS_UNLOCK_SET, QuestPreferencesErrType.SHOP), fieldConstraints);

        pnlShop.add(new FLabel.Builder().text("Common Singles").fontAlign(SwingConstants.RIGHT).build(), labelConstraints);
        pnlShop.add(new PrefInput(QPref.SHOP_SINGLES_COMMON, QuestPreferencesErrType.SHOP), fieldConstraints);

        pnlShop.add(new FLabel.Builder().text("Uncommon Singles").fontAlign(SwingConstants.RIGHT).build(), labelConstraints);
        pnlShop.add(new PrefInput(QPref.SHOP_SINGLES_UNCOMMON, QuestPreferencesErrType.SHOP), fieldConstraints);

        pnlShop.add(new FLabel.Builder().text("Rare Singles").fontAlign(SwingConstants.RIGHT).build(), labelConstraints);
        pnlShop.add(new PrefInput(QPref.SHOP_SINGLES_RARE, QuestPreferencesErrType.SHOP), fieldConstraints);

        pnlShop.add(new FLabel.Builder().text("Card Sale Price Cap").fontAlign(SwingConstants.RIGHT).build(), labelConstraints);
        pnlShop.add(new PrefInput(QPref.SHOP_MAX_SELLING_PRICE, QuestPreferencesErrType.SHOP), fieldConstraints);

        pnlShop.add(new FLabel.Builder().text("Wins to Uncap Sale Price").fontAlign(SwingConstants.RIGHT).build(), labelConstraints);
        pnlShop.add(new PrefInput(QPref.SHOP_WINS_FOR_NO_SELL_LIMIT, QuestPreferencesErrType.SHOP), fieldConstraints);

        FLabel extraCardsToKeep = new FLabel.Builder().text("Playset Size").fontAlign(SwingConstants.RIGHT).build();
        extraCardsToKeep.setToolTipText("The number of copies of cards to keep before selling extras.");
        pnlShop.add(extraCardsToKeep, labelConstraints);
        pnlShop.add(new PrefInput(QPref.PLAYSET_SIZE, QuestPreferencesErrType.DIFFICULTY), fieldConstraints + ", wrap");

        FLabel extraLandsToKeep = new FLabel.Builder().text("Playset Size: Basic Land").fontAlign(SwingConstants.RIGHT).build();
        extraLandsToKeep.setToolTipText("The number of copies of basic lands to keep before selling extras.");
        pnlShop.add(extraLandsToKeep, labelConstraints);
        pnlShop.add(new PrefInput(QPref.PLAYSET_BASIC_LAND_SIZE, QuestPreferencesErrType.DIFFICULTY), fieldConstraints + ", wrap");

        FLabel infiniteToKeep = new FLabel.Builder().text("Playset Size: Any Number").fontAlign(SwingConstants.RIGHT).build();
        infiniteToKeep.setToolTipText("The number of copies of Relentless Rats or Shadowborn Apostles to keep before selling extras.");
        pnlShop.add(infiniteToKeep, labelConstraints);
        pnlShop.add(new PrefInput(QPref.PLAYSET_ANY_NUMBER_SIZE, QuestPreferencesErrType.DIFFICULTY), fieldConstraints + ", wrap");

        pnlShop.add(new FLabel.Builder().text("Item Level Restriction").fontAlign(SwingConstants.RIGHT).build(), labelConstraints);
        pnlShop.add(new PrefInput(QPref.ITEM_LEVEL_RESTRICTION, QuestPreferencesErrType.SHOP), fieldConstraints);

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
            this.setFont(FSkin.getFont(13));
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
