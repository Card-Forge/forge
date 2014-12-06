package forge.screens.planarconquest;

import forge.assets.FImage;
import forge.assets.FSkinFont;
import forge.assets.FSkinImage;
import forge.model.FModel;
import forge.planarconquest.ConquestPreferences;
import forge.planarconquest.ConquestPreferences.CQPref;
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

public class ConquestPrefsScreen extends FScreen {
    private static final float PADDING = Utils.scale(5);

    private enum PrefsGroup {
        WINS_TO_UNLOCK,
        VARIANT_FREQUENCY,
        BOOSTER,
        REWARDS
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

    public ConquestPrefsScreen() {
        super("Conquest Preferences", ConquestMenu.getMenu());

        scroller.add(new PrefsHeader("Wins to Unlock", FSkinImage.LOCK, PrefsGroup.WINS_TO_UNLOCK));
        scroller.add(new PrefsOption("Wins to unlock commander 2", CQPref.WINS_TO_UNLOCK_COMMANDER_2, PrefsGroup.WINS_TO_UNLOCK));
        scroller.add(new PrefsOption("Wins to unlock commander 3", CQPref.WINS_TO_UNLOCK_COMMANDER_3, PrefsGroup.WINS_TO_UNLOCK));
        scroller.add(new PrefsOption("Wins to unlock commander 4", CQPref.WINS_TO_UNLOCK_COMMANDER_4, PrefsGroup.WINS_TO_UNLOCK));
        scroller.add(new PrefsOption("Wins to unlock planar portal", CQPref.WINS_TO_UNLOCK_PORTAL, PrefsGroup.WINS_TO_UNLOCK));

        scroller.add(new PrefsHeader("Variant Frequency", FSkinImage.PLANESWALKER, PrefsGroup.VARIANT_FREQUENCY));
        scroller.add(new PrefsOption("Commander Game (%)", CQPref.PERCENT_COMMANDER, PrefsGroup.VARIANT_FREQUENCY));
        scroller.add(new PrefsOption("Planechase Game (%)", CQPref.PERCENT_PLANECHASE, PrefsGroup.VARIANT_FREQUENCY));
        scroller.add(new PrefsOption("Commander & Planechase Game (%)", CQPref.PERCENT_DOUBLE_VARIANT, PrefsGroup.VARIANT_FREQUENCY));

        scroller.add(new PrefsHeader("Booster Pack Ratios", FSkinImage.PACK, PrefsGroup.BOOSTER));
        scroller.add(new PrefsOption("Common", CQPref.BOOSTER_COMMONS, PrefsGroup.BOOSTER));
        scroller.add(new PrefsOption("Uncommon", CQPref.BOOSTER_UNCOMMONS, PrefsGroup.BOOSTER));
        scroller.add(new PrefsOption("Rare", CQPref.BOOSTER_RARES, PrefsGroup.BOOSTER));

        scroller.add(new PrefsHeader("Rewards", FSkinImage.QUEST_COIN, PrefsGroup.REWARDS));
        scroller.add(new PrefsOption("Recruit Bonus Card Odds (%)", CQPref.RECRUIT_BONUS_CARD_ODDS, PrefsGroup.REWARDS));
        scroller.add(new PrefsOption("Study Bonus Card Odds (%)", CQPref.STUDY_BONUS_CARD_ODDS, PrefsGroup.REWARDS));
    }

    @Override
    protected void doLayout(float startY, float width, float height) {
        scroller.setBounds(0, startY, width, height - startY);
    }

    private class PrefsHeader extends FLabel {
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

    private static class PrefsOption extends FContainer {
        private static final float FIELD_WIDTH = new FTextField("99999").getAutoSizeWidth(); //base width on 5 digit number

        private final FLabel label = add(new FLabel.Builder().build());
        private final OptionField field = add(new OptionField());
        private final CQPref pref;
        private final PrefsGroup group;

        private PrefsOption(String label0, CQPref pref0, PrefsGroup group0) {
            label.setText(label0);
            pref = pref0;
            group = group0;
            field.setText(FModel.getConquestPreferences().getPref(pref0));
        }

        @Override
        protected void doLayout(float width, float height) {
            label.setBounds(0, 0, width - FIELD_WIDTH - PADDING, height);
            field.setBounds(width - FIELD_WIDTH, 0, FIELD_WIDTH, height);
        }

        private class OptionField extends FNumericTextField {
            private OptionField() {
            }

            @Override
            protected boolean validate() {
                if (super.validate()) {
                    final ConquestPreferences prefs = FModel.getConquestPreferences();

                    int val = Integer.parseInt(getText());

                    String validationError = prefs.validatePreference(pref, val);
                    if (validationError != null) {
                        String prefType;
                        switch (group) {
                        case WINS_TO_UNLOCK:
                            prefType = "Wins to Unlock";
                            break;
                        case VARIANT_FREQUENCY:
                            prefType = "Variant Frequency";
                            break;
                        default:
                            prefType = "";
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
