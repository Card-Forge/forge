package forge.screens.quest;

import forge.assets.FImage;
import forge.assets.FSkinFont;
import forge.assets.FSkinImage;
import forge.model.FModel;
import forge.quest.data.QuestPreferences;
import forge.quest.data.QuestPreferences.QPref;
import forge.screens.FScreen;
import forge.toolbox.FContainer;
import forge.toolbox.FDisplayObject;
import forge.toolbox.FEvent;
import forge.toolbox.FEvent.FEventHandler;
import forge.toolbox.FLabel;
import forge.toolbox.FNumericTextField;
import forge.toolbox.FOptionPane;
import forge.toolbox.FScrollPane;
import forge.toolbox.FTextField;
import forge.util.Utils;
import forge.util.Localizer;

public class QuestPrefsScreen extends FScreen {
    private static final float PADDING = Utils.scale(5);
    private static final Localizer localizer = Localizer.getInstance();

    private enum PrefsGroup {
        REWARDS,
        BOOSTER,
        SHOP,
        DIFFICULTY_ALL,
        DIFFICULTY_EASY,
        DIFFICULTY_MEDIUM,
        DIFFICULTY_HARD,
        DIFFICULTY_EXPERT,
        DRAFT_TOURNAMENTS
    }

    private FScrollPane scroller = add(new FScrollPane() {
        @Override
        protected ScrollBounds layoutAndGetScrollBounds(float visibleWidth, float visibleHeight) {
            float x = PADDING;
            float y = PADDING;
            float w = visibleWidth - 2 * PADDING;
            float fieldHeight = FTextField.getDefaultHeight();
            float dy = fieldHeight + PADDING;

            for (FDisplayObject child : getChildren()) {
                if (child.isVisible()) {
                    child.setBounds(x, y, w, fieldHeight);
                    y += dy;
                }
            }

            return new ScrollBounds(visibleWidth, y);
        }
    });

    public QuestPrefsScreen() {
        super(localizer.getMessage("lblQuestPreferences"), QuestMenu.getMenu());

        //Rewards
        scroller.add(new PrefsHeader(localizer.getMessage("lblRewards"), FSkinImage.QUEST_COIN, PrefsGroup.REWARDS));
        scroller.add(new PrefsOption(localizer.getMessage("lblBaseWinnings"), QPref.REWARDS_BASE, PrefsGroup.REWARDS));
        scroller.add(new PrefsOption(localizer.getMessage("lblNoLosses"), QPref.REWARDS_UNDEFEATED, PrefsGroup.REWARDS));
        scroller.add(new PrefsOption(localizer.getMessage("lblPoisonWin"), QPref.REWARDS_POISON, PrefsGroup.REWARDS));
        scroller.add(new PrefsOption(localizer.getMessage("lblMillingWin"), QPref.REWARDS_MILLED, PrefsGroup.REWARDS));
        scroller.add(new PrefsOption(localizer.getMessage("lblMulligan0Win"), QPref.REWARDS_MULLIGAN0, PrefsGroup.REWARDS));
        scroller.add(new PrefsOption(localizer.getMessage("lblAlternativeWin"), QPref.REWARDS_ALTERNATIVE, PrefsGroup.REWARDS));
        scroller.add(new PrefsOption(localizer.getMessage("lblWinbyTurn15"), QPref.REWARDS_TURN15, PrefsGroup.REWARDS));
        scroller.add(new PrefsOption(localizer.getMessage("lblWinbyTurn10"), QPref.REWARDS_TURN10, PrefsGroup.REWARDS));
        scroller.add(new PrefsOption(localizer.getMessage("lblWinbyTurn5"), QPref.REWARDS_TURN5, PrefsGroup.REWARDS));
        scroller.add(new PrefsOption(localizer.getMessage("lblFirstTurnWin"), QPref.REWARDS_TURN1, PrefsGroup.REWARDS));
        scroller.add(new PrefsOption(localizer.getMessage("lblExcludePromosFromRewardPool"), QPref.EXCLUDE_PROMOS_FROM_POOL, PrefsGroup.REWARDS));

        //Booster Pack Ratios
        scroller.add(new PrefsHeader(localizer.getMessage("lblBoosterPackRatios"), FSkinImage.QUEST_BOOK, PrefsGroup.BOOSTER));
        scroller.add(new PrefsOption(localizer.getMessage("lblCommon"), QPref.BOOSTER_COMMONS, PrefsGroup.BOOSTER));
        scroller.add(new PrefsOption(localizer.getMessage("lblUncommon"), QPref.BOOSTER_UNCOMMONS, PrefsGroup.BOOSTER));
        scroller.add(new PrefsOption(localizer.getMessage("lblRare"), QPref.BOOSTER_RARES, PrefsGroup.BOOSTER));
        scroller.add(new PrefsOption(localizer.getMessage("lblSpecialBoosters"), QPref.SPECIAL_BOOSTERS, PrefsGroup.BOOSTER));

        //Shop Preferences
        scroller.add(new PrefsHeader(localizer.getMessage("lblShopPreferences"), FSkinImage.QUEST_COIN, PrefsGroup.SHOP));
        scroller.add(new PrefsOption(localizer.getMessage("lblMaximumPacks"), QPref.SHOP_MAX_PACKS, PrefsGroup.SHOP));
        scroller.add(new PrefsOption(localizer.getMessage("lblStartingPacks"), QPref.SHOP_STARTING_PACKS, PrefsGroup.SHOP));
        scroller.add(new PrefsOption(localizer.getMessage("lblWinsforPack"), QPref.SHOP_WINS_FOR_ADDITIONAL_PACK, PrefsGroup.SHOP));
        scroller.add(new PrefsOption(localizer.getMessage("lblWinsperSetUnlock"), QPref.WINS_UNLOCK_SET, PrefsGroup.SHOP));
        scroller.add(new PrefsOption(localizer.getMessage("lblCommonSingles"), QPref.SHOP_SINGLES_COMMON, PrefsGroup.SHOP));
        scroller.add(new PrefsOption(localizer.getMessage("lblUncommonSingles"), QPref.SHOP_SINGLES_UNCOMMON, PrefsGroup.SHOP));
        scroller.add(new PrefsOption(localizer.getMessage("lblRareSingles"), QPref.SHOP_SINGLES_RARE, PrefsGroup.SHOP));
        scroller.add(new PrefsOption(localizer.getMessage("lblSellingPercentageBase"), QPref.SHOP_SELLING_PERCENTAGE_BASE, PrefsGroup.SHOP));
        scroller.add(new PrefsOption(localizer.getMessage("lblSellingPercentageMax"), QPref.SHOP_SELLING_PERCENTAGE_MAX, PrefsGroup.SHOP));
        scroller.add(new PrefsOption(localizer.getMessage("lblSellingPriceMax"), QPref.SHOP_MAX_SELLING_PRICE, PrefsGroup.SHOP));
        scroller.add(new PrefsOption(localizer.getMessage("lblPlaysetSize"), QPref.PLAYSET_SIZE, PrefsGroup.SHOP));
        scroller.add(new PrefsOption(localizer.getMessage("lblPlaysetSizeBasicLand"), QPref.PLAYSET_BASIC_LAND_SIZE, PrefsGroup.SHOP));
        scroller.add(new PrefsOption(localizer.getMessage("lblPlaysetSizeAnyNumber"), QPref.PLAYSET_ANY_NUMBER_SIZE, PrefsGroup.SHOP));

        //Quest Draft Tournament Preferences
        //NOTE: -- currently this setting is ignored since only the AI vs. AI Simulation mode can be used on mobile without hanging the game.
        //scroller.add(new PrefsHeader("Quest Draft Tournaments", FSkinImage.QUEST_NOTES, PrefsGroup.DIFFICULTY_ALL));
        //scroller.add(new PrefsOption("Simulate AI vs. AI Results", QPref.SIMULATE_AI_VS_AI_RESULTS, PrefsGroup.DRAFT_TOURNAMENTS));

        //Difficulty Adjustments (All)
        scroller.add(new PrefsHeader(localizer.getMessage("lblDifficultyAdjustmentsAll"), FSkinImage.QUEST_NOTES, PrefsGroup.DIFFICULTY_ALL));
        //scroller.add(new PrefsOption("Starting basic lands", QPref.STARTING_BASIC_LANDS, PrefsGroup.DIFFICULTY_ALL)); // Add Basic Lands is used instead
        scroller.add(new PrefsOption(localizer.getMessage("lblStartingSnowLands"), QPref.STARTING_SNOW_LANDS, PrefsGroup.DIFFICULTY_ALL));
        scroller.add(new PrefsOption(localizer.getMessage("lblColorBias"), QPref.STARTING_POOL_COLOR_BIAS, PrefsGroup.DIFFICULTY_ALL));
        scroller.add(new PrefsOption(localizer.getMessage("lblPenaltyforLoss"), QPref.PENALTY_LOSS, PrefsGroup.DIFFICULTY_ALL));

        //wild opponents addon
        scroller.add(new PrefsOption(localizer.getMessage("lblWildOpponentMultiplier"), QPref.WILD_OPPONENTS_MULTIPLIER, PrefsGroup.DIFFICULTY_ALL));
        scroller.add(new PrefsOption(localizer.getMessage("lblWildOpponentNumber"), QPref.WILD_OPPONENTS_NUMBER, PrefsGroup.DIFFICULTY_ALL));

        //Difficulty Adjustments (Easy)
        scroller.add(new PrefsHeader(localizer.getMessage("lblDifficultyAdjustmentsEasy"), FSkinImage.QUEST_NOTES, PrefsGroup.DIFFICULTY_EASY));
        scroller.add(new PrefsOption(localizer.getMessage("lblWinsForBooster"), QPref.WINS_BOOSTER_EASY, PrefsGroup.DIFFICULTY_EASY));
        scroller.add(new PrefsOption(localizer.getMessage("lblWinsForRankIncrease"), QPref.WINS_RANKUP_EASY, PrefsGroup.DIFFICULTY_EASY));
        scroller.add(new PrefsOption(localizer.getMessage("lblWinsForMediumAI"), QPref.WINS_MEDIUMAI_EASY, PrefsGroup.DIFFICULTY_EASY));
        scroller.add(new PrefsOption(localizer.getMessage("lblWinsForHardAI"), QPref.WINS_HARDAI_EASY, PrefsGroup.DIFFICULTY_EASY));
        scroller.add(new PrefsOption(localizer.getMessage("lblWinsForExpertAI"), QPref.WINS_EXPERTAI_EASY, PrefsGroup.DIFFICULTY_EASY));
        scroller.add(new PrefsOption(localizer.getMessage("lblStartingCommons"), QPref.STARTING_COMMONS_EASY, PrefsGroup.DIFFICULTY_EASY));
        scroller.add(new PrefsOption(localizer.getMessage("lblStartingUncommons"), QPref.STARTING_UNCOMMONS_EASY, PrefsGroup.DIFFICULTY_EASY));
        scroller.add(new PrefsOption(localizer.getMessage("lblStartingRares"), QPref.STARTING_RARES_EASY, PrefsGroup.DIFFICULTY_EASY));
        scroller.add(new PrefsOption(localizer.getMessage("lblStartingCredits"), QPref.STARTING_CREDITS_EASY, PrefsGroup.DIFFICULTY_EASY));

        //Difficulty Adjustments (Medium)
        scroller.add(new PrefsHeader(localizer.getMessage("lblDifficultyAdjustmentsMedium"), FSkinImage.QUEST_NOTES, PrefsGroup.DIFFICULTY_MEDIUM));
        scroller.add(new PrefsOption(localizer.getMessage("lblWinsForBooster"), QPref.WINS_BOOSTER_MEDIUM, PrefsGroup.DIFFICULTY_MEDIUM));
        scroller.add(new PrefsOption(localizer.getMessage("lblWinsForRankIncrease"), QPref.WINS_RANKUP_MEDIUM, PrefsGroup.DIFFICULTY_MEDIUM));
        scroller.add(new PrefsOption(localizer.getMessage("lblWinsForMediumAI"), QPref.WINS_MEDIUMAI_MEDIUM, PrefsGroup.DIFFICULTY_MEDIUM));
        scroller.add(new PrefsOption(localizer.getMessage("lblWinsForHardAI"), QPref.WINS_HARDAI_MEDIUM, PrefsGroup.DIFFICULTY_MEDIUM));
        scroller.add(new PrefsOption(localizer.getMessage("lblWinsForExpertAI"), QPref.WINS_EXPERTAI_MEDIUM, PrefsGroup.DIFFICULTY_MEDIUM));
        scroller.add(new PrefsOption(localizer.getMessage("lblStartingCommons"), QPref.STARTING_COMMONS_MEDIUM, PrefsGroup.DIFFICULTY_MEDIUM));
        scroller.add(new PrefsOption(localizer.getMessage("lblStartingUncommons"), QPref.STARTING_UNCOMMONS_MEDIUM, PrefsGroup.DIFFICULTY_MEDIUM));
        scroller.add(new PrefsOption(localizer.getMessage("lblStartingRares"), QPref.STARTING_RARES_MEDIUM, PrefsGroup.DIFFICULTY_MEDIUM));
        scroller.add(new PrefsOption(localizer.getMessage("lblStartingCredits"), QPref.STARTING_CREDITS_MEDIUM, PrefsGroup.DIFFICULTY_MEDIUM));

        //Difficulty Adjustments (Hard)
        scroller.add(new PrefsHeader(localizer.getMessage("lblDifficultyAdjustmentsHard"), FSkinImage.QUEST_NOTES, PrefsGroup.DIFFICULTY_HARD));
        scroller.add(new PrefsOption(localizer.getMessage("lblWinsForBooster"), QPref.WINS_BOOSTER_HARD, PrefsGroup.DIFFICULTY_HARD));
        scroller.add(new PrefsOption(localizer.getMessage("lblWinsForRankIncrease"), QPref.WINS_RANKUP_HARD, PrefsGroup.DIFFICULTY_HARD));
        scroller.add(new PrefsOption(localizer.getMessage("lblWinsForMediumAI"), QPref.WINS_MEDIUMAI_HARD, PrefsGroup.DIFFICULTY_HARD));
        scroller.add(new PrefsOption(localizer.getMessage("lblWinsForHardAI"), QPref.WINS_HARDAI_HARD, PrefsGroup.DIFFICULTY_HARD));
        scroller.add(new PrefsOption(localizer.getMessage("lblWinsForExpertAI"), QPref.WINS_EXPERTAI_HARD, PrefsGroup.DIFFICULTY_HARD));
        scroller.add(new PrefsOption(localizer.getMessage("lblStartingCommons"), QPref.STARTING_COMMONS_HARD, PrefsGroup.DIFFICULTY_HARD));
        scroller.add(new PrefsOption(localizer.getMessage("lblStartingUncommons"), QPref.STARTING_UNCOMMONS_HARD, PrefsGroup.DIFFICULTY_HARD));
        scroller.add(new PrefsOption(localizer.getMessage("lblStartingRares"), QPref.STARTING_RARES_HARD, PrefsGroup.DIFFICULTY_HARD));
        scroller.add(new PrefsOption(localizer.getMessage("lblStartingCredits"), QPref.STARTING_CREDITS_HARD, PrefsGroup.DIFFICULTY_HARD));

        //Difficulty Adjustments (Expert)
        scroller.add(new PrefsHeader(localizer.getMessage("lblDifficultyAdjustmentsExpert"), FSkinImage.QUEST_NOTES, PrefsGroup.DIFFICULTY_EXPERT));
        scroller.add(new PrefsOption(localizer.getMessage("lblWinsForBooster"), QPref.WINS_BOOSTER_EXPERT, PrefsGroup.DIFFICULTY_EXPERT));
        scroller.add(new PrefsOption(localizer.getMessage("lblWinsForRankIncrease"), QPref.WINS_RANKUP_EXPERT, PrefsGroup.DIFFICULTY_EXPERT));
        scroller.add(new PrefsOption(localizer.getMessage("lblWinsForMediumAI"), QPref.WINS_MEDIUMAI_EXPERT, PrefsGroup.DIFFICULTY_EXPERT));
        scroller.add(new PrefsOption(localizer.getMessage("lblWinsForHardAI"), QPref.WINS_HARDAI_EXPERT, PrefsGroup.DIFFICULTY_EXPERT));
        scroller.add(new PrefsOption(localizer.getMessage("lblWinsForExpertAI"), QPref.WINS_EXPERTAI_EXPERT, PrefsGroup.DIFFICULTY_EXPERT));
        scroller.add(new PrefsOption(localizer.getMessage("lblStartingCommons"), QPref.STARTING_COMMONS_EXPERT, PrefsGroup.DIFFICULTY_EXPERT));
        scroller.add(new PrefsOption(localizer.getMessage("lblStartingUncommons"), QPref.STARTING_UNCOMMONS_EXPERT, PrefsGroup.DIFFICULTY_EXPERT));
        scroller.add(new PrefsOption(localizer.getMessage("lblStartingRares"), QPref.STARTING_RARES_EXPERT, PrefsGroup.DIFFICULTY_EXPERT));
        scroller.add(new PrefsOption(localizer.getMessage("lblStartingCredits"), QPref.STARTING_CREDITS_EXPERT, PrefsGroup.DIFFICULTY_EXPERT));
    }

    @Override
    protected void doLayout(float startY, float width, float height) {
        scroller.setBounds(0, startY, width, height - startY);
    }

    private final class PrefsHeader extends FLabel {
        private PrefsHeader(String title, FImage icon, final PrefsGroup group) {
            super(new ButtonBuilder().text(title).font(FSkinFont.get(16)).icon(icon).iconScaleFactor(1f)
                    .command(new FEventHandler() {
                private boolean showOptions = true;

                @Override
                public void handleEvent(FEvent e) {
                    showOptions = !showOptions;
                    for (FDisplayObject child : scroller.getChildren()) {
                        if (child instanceof PrefsOption && ((PrefsOption)child).group == group) {
                            child.setVisible(showOptions);
                        }
                    }
                    scroller.revalidate();
                }
            }));
        }
    }

    private static final class PrefsOption extends FContainer {
        private static final float FIELD_WIDTH = new FTextField("99999").getAutoSizeWidth(); //base width on 5 digit number

        private final FLabel label = add(new FLabel.Builder().build());
        private final OptionField field = add(new OptionField());
        private final QPref pref;
        private final PrefsGroup group;

        private PrefsOption(String label0, QPref pref0, PrefsGroup group0) {
            label.setText(label0);
            pref = pref0;
            group = group0;
            field.setText(FModel.getQuestPreferences().getPref(pref0));
        }

        @Override
        protected void doLayout(float width, float height) {
            label.setBounds(0, 0, width - FIELD_WIDTH - PADDING, height);
            field.setBounds(width - FIELD_WIDTH, 0, FIELD_WIDTH, height);
        }

        private final class OptionField extends FNumericTextField {
            private OptionField() {
            }

            @Override
            protected boolean validate() {
                if (super.validate()) {
                    final QuestPreferences prefs = FModel.getQuestPreferences();

                    int val = Integer.parseInt(getText());

                    String validationError = prefs.validatePreference(pref, val);
                    if (validationError != null) {
                        String prefType;
                        switch (group) {
                        case REWARDS:
                            prefType = localizer.getMessage("lblRewards");
                            break;
                        case BOOSTER:
                            prefType = localizer.getMessage("lblBoosterPackRatios");
                            break;
                        case SHOP:
                            prefType = localizer.getMessage("lblShopPreferences");
                            break;
                        case DRAFT_TOURNAMENTS:
                            prefType = localizer.getMessage("lblDraftTournaments");
                            break;
                        default:
                            prefType = localizer.getMessage("lblDifficultyAdjustments");
                            break;
                        }
                        FOptionPane.showErrorDialog(validationError, localizer.getMessage("lblSaveFailed") + prefType);
                        return false;
                    }

                    prefs.setPref(pref, getText());
                    prefs.save();
                    return true;
                }
                return false;
            }
        }
    }
}
