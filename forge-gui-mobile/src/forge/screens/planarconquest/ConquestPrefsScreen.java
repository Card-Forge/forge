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
        AETHER,
        BOOSTER,
        PLANESWALK,
        CHAOS
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

        scroller.add(new PrefsHeader("Aether Shards", FSkinImage.AETHER_SHARD, PrefsGroup.BOOSTER));
        scroller.add(new PrefsOption("Base Duplicate Value", CQPref.AETHER_BASE_DUPLICATE_VALUE, PrefsGroup.AETHER));
        scroller.add(new PrefsOption("Base Exile Value", CQPref.AETHER_BASE_EXILE_VALUE, PrefsGroup.AETHER));
        scroller.add(new PrefsOption("Base Retrieve Cost", CQPref.AETHER_BASE_RETRIEVE_COST, PrefsGroup.AETHER));
        scroller.add(new PrefsOption("Base Pull Cost", CQPref.AETHER_BASE_PULL_COST, PrefsGroup.AETHER));
        scroller.add(new PrefsOption("Uncommon Multiplier", CQPref.AETHER_UNCOMMON_MULTIPLIER, PrefsGroup.AETHER));
        scroller.add(new PrefsOption("Rare Multiplier", CQPref.AETHER_RARE_MULTIPLIER, PrefsGroup.AETHER));
        scroller.add(new PrefsOption("Mythic Multiplier", CQPref.AETHER_MYTHIC_MULTIPLIER, PrefsGroup.AETHER));
        scroller.add(new PrefsOption("Starting Shards", CQPref.AETHER_START_SHARDS, PrefsGroup.AETHER));
        scroller.add(new PrefsOption("Chaos Wheel Shard Value", CQPref.AETHER_WHEEL_SHARDS, PrefsGroup.AETHER));

        scroller.add(new PrefsHeader("Booster Packs", FSkinImage.PACK, PrefsGroup.BOOSTER));
        scroller.add(new PrefsOption("Commons", CQPref.BOOSTER_COMMONS, PrefsGroup.BOOSTER));
        scroller.add(new PrefsOption("Uncommons", CQPref.BOOSTER_UNCOMMONS, PrefsGroup.BOOSTER));
        scroller.add(new PrefsOption("Rares", CQPref.BOOSTER_RARES, PrefsGroup.BOOSTER));
        scroller.add(new PrefsOption("Boosters per Mythic", CQPref.BOOSTERS_PER_MYTHIC, PrefsGroup.BOOSTER));

        scroller.add(new PrefsHeader("Planeswalk Emblems", FSkinImage.PW_BADGE_COMMON, PrefsGroup.PLANESWALK));
        scroller.add(new PrefsOption("Base Conquer Reward", CQPref.PLANESWALK_CONQUER_EMBLEMS, PrefsGroup.PLANESWALK));
        scroller.add(new PrefsOption("Chaos Wheel Bonus", CQPref.PLANESWALK_WHEEL_EMBLEMS, PrefsGroup.PLANESWALK));
        scroller.add(new PrefsOption("First Plane Unlock Cost", CQPref.PLANESWALK_FIRST_UNLOCK, PrefsGroup.PLANESWALK));
        scroller.add(new PrefsOption("Cost Increase Per Unlock", CQPref.PLANESWALK_UNLOCK_INCREASE, PrefsGroup.PLANESWALK));

        scroller.add(new PrefsHeader("Chaos Battles", FSkinImage.CHAOS, PrefsGroup.CHAOS));
        scroller.add(new PrefsOption("Wins For Medium AI", CQPref.CHAOS_BATTLE_WINS_MEDIUMAI, PrefsGroup.CHAOS));
        scroller.add(new PrefsOption("Wins For Hard AI", CQPref.CHAOS_BATTLE_WINS_HARDAI, PrefsGroup.CHAOS));
        scroller.add(new PrefsOption("Wins For Expert AI", CQPref.CHAOS_BATTLE_WINS_EXPERTAI, PrefsGroup.CHAOS));
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
                        case BOOSTER:
                            prefType = "Booster Packs";
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
