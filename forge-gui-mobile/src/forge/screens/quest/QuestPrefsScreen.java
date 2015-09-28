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

public class QuestPrefsScreen extends FScreen {
    private static final float PADDING = Utils.scale(5);

    private enum PrefsGroup {
        REWARDS,
        BOOSTER,
        SHOP,
        DIFFICULTY_ALL,
        DIFFICULTY_EASY,
        DIFFICULTY_MEDIUM,
        DIFFICULTY_HARD,
        DIFFICULTY_EXPERT
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
        super("Quest Preferences", QuestMenu.getMenu());

        //Rewards
        scroller.add(new PrefsHeader("Rewards", FSkinImage.QUEST_COIN, PrefsGroup.REWARDS));
        scroller.add(new PrefsOption("Base winnings", QPref.REWARDS_BASE, PrefsGroup.REWARDS));
        scroller.add(new PrefsOption("No losses", QPref.REWARDS_UNDEFEATED, PrefsGroup.REWARDS));
        scroller.add(new PrefsOption("Poison win", QPref.REWARDS_POISON, PrefsGroup.REWARDS));
        scroller.add(new PrefsOption("Milling win", QPref.REWARDS_MILLED, PrefsGroup.REWARDS));
        scroller.add(new PrefsOption("Mulligan 0 win", QPref.REWARDS_MULLIGAN0, PrefsGroup.REWARDS));
        scroller.add(new PrefsOption("Alternative win", QPref.REWARDS_ALTERNATIVE, PrefsGroup.REWARDS));
        scroller.add(new PrefsOption("Win by turn 15", QPref.REWARDS_TURN15, PrefsGroup.REWARDS));
        scroller.add(new PrefsOption("Win by turn 10", QPref.REWARDS_TURN10, PrefsGroup.REWARDS));
        scroller.add(new PrefsOption("Win by turn 5", QPref.REWARDS_TURN5, PrefsGroup.REWARDS));
        scroller.add(new PrefsOption("First turn win", QPref.REWARDS_TURN1, PrefsGroup.REWARDS));

        //Booster Pack Ratios
        scroller.add(new PrefsHeader("Booster Pack Ratios", FSkinImage.QUEST_BOOK, PrefsGroup.BOOSTER));
        scroller.add(new PrefsOption("Common", QPref.BOOSTER_COMMONS, PrefsGroup.BOOSTER));
        scroller.add(new PrefsOption("Uncommon", QPref.BOOSTER_UNCOMMONS, PrefsGroup.BOOSTER));
        scroller.add(new PrefsOption("Rare", QPref.BOOSTER_RARES, PrefsGroup.BOOSTER));
        scroller.add(new PrefsOption("Special Boosters", QPref.SPECIAL_BOOSTERS, PrefsGroup.BOOSTER));

        //Shop Preferences
        scroller.add(new PrefsHeader("Shop Preferences", FSkinImage.QUEST_COIN, PrefsGroup.SHOP));
        scroller.add(new PrefsOption("Maximum Packs", QPref.SHOP_MAX_PACKS, PrefsGroup.SHOP));
        scroller.add(new PrefsOption("Starting Packs", QPref.SHOP_STARTING_PACKS, PrefsGroup.SHOP));
        scroller.add(new PrefsOption("Wins for Pack", QPref.SHOP_WINS_FOR_ADDITIONAL_PACK, PrefsGroup.SHOP));
        scroller.add(new PrefsOption("Wins per Set Unlock", QPref.WINS_UNLOCK_SET, PrefsGroup.SHOP));
        scroller.add(new PrefsOption("Common Singles", QPref.SHOP_SINGLES_COMMON, PrefsGroup.SHOP));
        scroller.add(new PrefsOption("Uncommon Singles", QPref.SHOP_SINGLES_UNCOMMON, PrefsGroup.SHOP));
        scroller.add(new PrefsOption("Rare Singles", QPref.SHOP_SINGLES_RARE, PrefsGroup.SHOP));
        scroller.add(new PrefsOption("Playset Size", QPref.PLAYSET_SIZE, PrefsGroup.SHOP));
        scroller.add(new PrefsOption("Playset Size: Basic Lands", QPref.PLAYSET_BASIC_LAND_SIZE, PrefsGroup.SHOP));
        scroller.add(new PrefsOption("Playset Size: Any Number", QPref.PLAYSET_ANY_NUMBER_SIZE, PrefsGroup.SHOP));

        //Difficulty Adjustments (All)
        scroller.add(new PrefsHeader("Difficulty Adjustments (All)", FSkinImage.QUEST_NOTES, PrefsGroup.DIFFICULTY_ALL));
        //scroller.add(new PrefsOption("Starting basic lands", QPref.STARTING_BASIC_LANDS, PrefsGroup.DIFFICULTY_ALL)); // Add Basic Lands is used instead
        scroller.add(new PrefsOption("Starting snow lands", QPref.STARTING_SNOW_LANDS, PrefsGroup.DIFFICULTY_ALL));
        scroller.add(new PrefsOption("Color bias (1-100%)", QPref.STARTING_POOL_COLOR_BIAS, PrefsGroup.DIFFICULTY_ALL));
        scroller.add(new PrefsOption("Penalty for loss", QPref.PENALTY_LOSS, PrefsGroup.DIFFICULTY_ALL));

        //Difficult Adjustments (Easy)
        scroller.add(new PrefsHeader("Difficulty Adjustments (Easy)", FSkinImage.QUEST_NOTES, PrefsGroup.DIFFICULTY_EASY));
        scroller.add(new PrefsOption("Wins For Booster", QPref.WINS_BOOSTER_EASY, PrefsGroup.DIFFICULTY_EASY));
        scroller.add(new PrefsOption("Wins For Rank Increase", QPref.WINS_RANKUP_EASY, PrefsGroup.DIFFICULTY_EASY));
        scroller.add(new PrefsOption("Wins For Medium AI", QPref.WINS_MEDIUMAI_EASY, PrefsGroup.DIFFICULTY_EASY));
        scroller.add(new PrefsOption("Wins For Hard AI", QPref.WINS_HARDAI_EASY, PrefsGroup.DIFFICULTY_EASY));
        scroller.add(new PrefsOption("Wins For Expert AI", QPref.WINS_EXPERTAI_EASY, PrefsGroup.DIFFICULTY_EASY));
        scroller.add(new PrefsOption("Starting commons", QPref.STARTING_COMMONS_EASY, PrefsGroup.DIFFICULTY_EASY));
        scroller.add(new PrefsOption("Starting uncommons", QPref.STARTING_UNCOMMONS_EASY, PrefsGroup.DIFFICULTY_EASY));
        scroller.add(new PrefsOption("Starting rares", QPref.STARTING_RARES_EASY, PrefsGroup.DIFFICULTY_EASY));
        scroller.add(new PrefsOption("Starting credits", QPref.STARTING_CREDITS_EASY, PrefsGroup.DIFFICULTY_EASY));

        //Difficult Adjustments (Medium)
        scroller.add(new PrefsHeader("Difficulty Adjustments (Medium)", FSkinImage.QUEST_NOTES, PrefsGroup.DIFFICULTY_MEDIUM));
        scroller.add(new PrefsOption("Wins For Booster", QPref.WINS_BOOSTER_MEDIUM, PrefsGroup.DIFFICULTY_MEDIUM));
        scroller.add(new PrefsOption("Wins For Rank Increase", QPref.WINS_RANKUP_MEDIUM, PrefsGroup.DIFFICULTY_MEDIUM));
        scroller.add(new PrefsOption("Wins For Medium AI", QPref.WINS_MEDIUMAI_MEDIUM, PrefsGroup.DIFFICULTY_MEDIUM));
        scroller.add(new PrefsOption("Wins For Hard AI", QPref.WINS_HARDAI_MEDIUM, PrefsGroup.DIFFICULTY_MEDIUM));
        scroller.add(new PrefsOption("Wins For Expert AI", QPref.WINS_EXPERTAI_MEDIUM, PrefsGroup.DIFFICULTY_MEDIUM));
        scroller.add(new PrefsOption("Starting commons", QPref.STARTING_COMMONS_MEDIUM, PrefsGroup.DIFFICULTY_MEDIUM));
        scroller.add(new PrefsOption("Starting uncommons", QPref.STARTING_UNCOMMONS_MEDIUM, PrefsGroup.DIFFICULTY_MEDIUM));
        scroller.add(new PrefsOption("Starting rares", QPref.STARTING_RARES_MEDIUM, PrefsGroup.DIFFICULTY_MEDIUM));
        scroller.add(new PrefsOption("Starting credits", QPref.STARTING_CREDITS_MEDIUM, PrefsGroup.DIFFICULTY_MEDIUM));

        //Difficult Adjustments (Hard)
        scroller.add(new PrefsHeader("Difficulty Adjustments (Hard)", FSkinImage.QUEST_NOTES, PrefsGroup.DIFFICULTY_HARD));
        scroller.add(new PrefsOption("Wins For Booster", QPref.WINS_BOOSTER_HARD, PrefsGroup.DIFFICULTY_HARD));
        scroller.add(new PrefsOption("Wins For Rank Increase", QPref.WINS_RANKUP_HARD, PrefsGroup.DIFFICULTY_HARD));
        scroller.add(new PrefsOption("Wins For Medium AI", QPref.WINS_MEDIUMAI_HARD, PrefsGroup.DIFFICULTY_HARD));
        scroller.add(new PrefsOption("Wins For Hard AI", QPref.WINS_HARDAI_HARD, PrefsGroup.DIFFICULTY_HARD));
        scroller.add(new PrefsOption("Wins For Expert AI", QPref.WINS_EXPERTAI_HARD, PrefsGroup.DIFFICULTY_HARD));
        scroller.add(new PrefsOption("Starting commons", QPref.STARTING_COMMONS_HARD, PrefsGroup.DIFFICULTY_HARD));
        scroller.add(new PrefsOption("Starting uncommons", QPref.STARTING_UNCOMMONS_HARD, PrefsGroup.DIFFICULTY_HARD));
        scroller.add(new PrefsOption("Starting rares", QPref.STARTING_RARES_HARD, PrefsGroup.DIFFICULTY_HARD));
        scroller.add(new PrefsOption("Starting credits", QPref.STARTING_CREDITS_HARD, PrefsGroup.DIFFICULTY_HARD));

        //Difficult Adjustments (Expert)
        scroller.add(new PrefsHeader("Difficulty Adjustments (Expert)", FSkinImage.QUEST_NOTES, PrefsGroup.DIFFICULTY_EXPERT));
        scroller.add(new PrefsOption("Wins For Booster", QPref.WINS_BOOSTER_EXPERT, PrefsGroup.DIFFICULTY_EXPERT));
        scroller.add(new PrefsOption("Wins For Rank Increase", QPref.WINS_RANKUP_EXPERT, PrefsGroup.DIFFICULTY_EXPERT));
        scroller.add(new PrefsOption("Wins For Medium AI", QPref.WINS_MEDIUMAI_EXPERT, PrefsGroup.DIFFICULTY_EXPERT));
        scroller.add(new PrefsOption("Wins For Hard AI", QPref.WINS_HARDAI_EXPERT, PrefsGroup.DIFFICULTY_EXPERT));
        scroller.add(new PrefsOption("Wins For Expert AI", QPref.WINS_EXPERTAI_EXPERT, PrefsGroup.DIFFICULTY_EXPERT));
        scroller.add(new PrefsOption("Starting commons", QPref.STARTING_COMMONS_EXPERT, PrefsGroup.DIFFICULTY_EXPERT));
        scroller.add(new PrefsOption("Starting uncommons", QPref.STARTING_UNCOMMONS_EXPERT, PrefsGroup.DIFFICULTY_EXPERT));
        scroller.add(new PrefsOption("Starting rares", QPref.STARTING_RARES_EXPERT, PrefsGroup.DIFFICULTY_EXPERT));
        scroller.add(new PrefsOption("Starting credits", QPref.STARTING_CREDITS_EXPERT, PrefsGroup.DIFFICULTY_EXPERT));
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
                            prefType = "Rewards";
                            break;
                        case BOOSTER:
                            prefType = "Booster Pack Ratios";
                            break;
                        case SHOP:
                            prefType = "Shop Preferences";
                            break;
                        default:
                            prefType = "Difficulty Adjustments";
                            break;
                        }
                        FOptionPane.showErrorDialog(validationError, "Save Failed - " + prefType);
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
