package forge.screens.settings;

import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;

import forge.Forge;
import forge.Graphics;
import forge.ai.AiProfileUtil;
import forge.assets.FSkin;
import forge.assets.FSkinColor;
import forge.assets.FSkinFont;
import forge.assets.FSkinImage;
import forge.game.GameLogEntryType;
import forge.model.FModel;
import forge.properties.ForgeConstants;
import forge.properties.ForgePreferences;
import forge.properties.ForgePreferences.FPref;
import forge.screens.FScreen;
import forge.screens.TabPageScreen;
import forge.screens.TabPageScreen.TabPage;
import forge.screens.home.HomeScreen;
import forge.sound.SoundSystem;
import forge.toolbox.FCheckBox;
import forge.toolbox.FGroupList;
import forge.toolbox.FList;
import forge.toolbox.FOptionPane;
import forge.util.Callback;
import forge.util.Utils;

import java.util.ArrayList;
import java.util.List;

public class SettingsPage extends TabPage<SettingsScreen> {
    private final FGroupList<Setting> lstSettings = add(new FGroupList<Setting>());

    public SettingsPage() {
        super("Settings", FSkinImage.SETTINGS);

        lstSettings.setListItemRenderer(new SettingRenderer());

        lstSettings.addGroup("General Settings");
        lstSettings.addGroup("Gameplay Options");
        lstSettings.addGroup("Random Deck Generation");
        lstSettings.addGroup("Advanced Settings");
        lstSettings.addGroup("Graphic Options");
        lstSettings.addGroup("Card Overlays");
        lstSettings.addGroup("Vibration Options");
        lstSettings.addGroup("Sound Options");

        //General Settings
        lstSettings.addItem(new CustomSelectSetting(FPref.UI_SKIN, "Theme",
                "Sets the theme that determines how display components are skinned.",
                FSkin.getAllSkins()) {
            @Override
            public void valueChanged(String newValue) {
                FSkin.changeSkin(newValue);
            }
        }, 0);
        if (Forge.getDeviceAdapter().isTablet()) { //only offer landscape option on tablets
            lstSettings.addItem(new BooleanSetting(FPref.UI_LANDSCAPE_MODE,
                    "Landscape Mode",
                    "Use landscape (horizontal) orientation for app instead of portrait (vertical).") {
                        @Override
                        public void select() {
                            super.select();
                            boolean landscapeMode = FModel.getPreferences().getPrefBoolean(FPref.UI_LANDSCAPE_MODE);
                            Forge.getDeviceAdapter().setLandscapeMode(landscapeMode); //ensure device able to save off ini file so landscape change takes effect
                            if (Forge.isLandscapeMode() != landscapeMode) {
                                FOptionPane.showConfirmDialog("You must restart Forge for this change to take effect.", "Restart Forge", "Restart", "Later", new Callback<Boolean>() {
                                    @Override
                                    public void run(Boolean result) {
                                        if (result) {
                                            Forge.restart(true);
                                        }
                                    }
                                });
                            }
                        }
                    }, 0);
        }

        //Gameplay Options
        lstSettings.addItem(new CustomSelectSetting(FPref.UI_CURRENT_AI_PROFILE,
                "AI Personality",
                "Choose your AI opponent.",
                AiProfileUtil.getProfilesArray()),
                1);
        lstSettings.addItem(new BooleanSetting(FPref.UI_ANTE,
                "Play for Ante",
                "Determines whether or not the game is played for ante."),
                1);
        lstSettings.addItem(new BooleanSetting(FPref.UI_ANTE_MATCH_RARITY,
                "Match Ante Rarity",
                "Attempts to make antes the same rarity for all players."),
                1);
        lstSettings.addItem(new BooleanSetting(FPref.MATCH_HOT_SEAT_MODE,
                "Hot Seat Mode",
                "When starting a game with 2 human players, use single prompt to control both players."),
                1);
        lstSettings.addItem(new BooleanSetting(FPref.UI_ENABLE_AI_CHEATS,
                "Allow AI Cheating",
                "Allow the AI to cheat to gain advantage (for personalities that have cheat shuffling options set)."),
                1);
        lstSettings.addItem(new BooleanSetting(FPref.UI_MANABURN,
                "Mana Burn",
                "Play with mana burn (from pre-Magic 2010 rules)."),
                1);
        lstSettings.addItem(new BooleanSetting(FPref.UI_MANA_LOST_PROMPT,
                "Prompt Mana Pool Emptying",
                "When enabled, you get a warning if passing priority would cause you to lose mana in your mana pool."),
                1);
        lstSettings.addItem(new BooleanSetting(FPref.ENFORCE_DECK_LEGALITY,
                "Deck Conformance",
                "Enforces deck legality relevant to each environment (minimum deck sizes, max card count etc)."),
                1);
        lstSettings.addItem(new BooleanSetting(FPref.UI_CLONE_MODE_SOURCE,
                "Clones Use Original Card Art",
                "When enabled clones will use their original art instead of the cloned card's art."),
                1);
        lstSettings.addItem(new BooleanSetting(FPref.MATCHPREF_PROMPT_FREE_BLOCKS,
                "Free Block Handling",
                "When enabled, if you would have to pay 0 to block, pay automatically without prompt."),
                1);

        //Random Deck Generation
        lstSettings.addItem(new BooleanSetting(FPref.DECKGEN_NOSMALL,
                "Remove Small Creatures",
                "Disables 1/1 and 0/X creatures in generated decks."),
                2);
        lstSettings.addItem(new BooleanSetting(FPref.DECKGEN_SINGLETONS,
                "Singleton Mode",
                "Disables non-land duplicates in generated decks."),
                2);
        lstSettings.addItem(new BooleanSetting(FPref.DECKGEN_ARTIFACTS,
                "Remove Artifacts",
                "Disables artifact cards in generated decks."),
                2);

        //Advanced Settings
        lstSettings.addItem(new BooleanSetting(FPref.DEV_MODE_ENABLED,
                "Developer Mode",
                "Enables menu with functions for testing during development.") {
                    @Override
                    public void select() {
                        super.select();
                        //update DEV_MODE flag when preference changes
                        ForgePreferences.DEV_MODE = FModel.getPreferences().getPrefBoolean(FPref.DEV_MODE_ENABLED);
                    }
                }, 3);
        lstSettings.addItem(new CustomSelectSetting(FPref.DEV_LOG_ENTRY_TYPE,
                "Game Log Verbosity",
                "Changes how much information is displayed in the game log. Sorted by least to most verbose.",
                GameLogEntryType.class),
                3);

        //Graphic Options
        lstSettings.addItem(new BooleanSetting(FPref.UI_OVERLAY_FOIL_EFFECT,
                "Display Foil Overlay",
                "Displays foil cards with the visual foil overlay effect."),
                4);
        lstSettings.addItem(new BooleanSetting(FPref.UI_RANDOM_FOIL,
                "Random Foil",
                "Adds foil effect to random cards."),
                4);
        lstSettings.addItem(new BooleanSetting(FPref.UI_RANDOM_ART_IN_POOLS,
                "Randomize Card Art",
                "Generates cards with random art in generated limited mode card pools."),
                4);
        lstSettings.addItem(new BooleanSetting(FPref.UI_COMPACT_TABS,
                "Compact Tabs",
                "Show smaller tabs on the top of tab page screens (such as this screen).") {
                    @Override
                    public void select() {
                        super.select();
                        //update layout of screen when this setting changes
                        TabPageScreen.COMPACT_TABS = FModel.getPreferences().getPrefBoolean(FPref.UI_COMPACT_TABS);
                        parentScreen.revalidate();
                    }
                }, 4);
        lstSettings.addItem(new BooleanSetting(FPref.UI_COMPACT_LIST_ITEMS,
                "Compact List Items",
                "Show only a single line of text for cards and decks on all list views by default."),
                4);
        lstSettings.addItem(new BooleanSetting(FPref.UI_HIDE_REMINDER_TEXT,
                "Hide Reminder Text",
                "Hide reminder text in Card Detail pane."),
                4);
        lstSettings.addItem(new BooleanSetting(FPref.UI_MATCH_IMAGE_VISIBLE,
                "Show Match Background",
                "Show match background image on battlefield, otherwise background texture shown instead."),
                4);
        lstSettings.addItem(new CustomSelectSetting(FPref.UI_DISPLAY_CURRENT_COLORS,
                "Detailed Card Color",
                "Displays the breakdown of the current color of cards in the card detail information panel.",
                new String[]{
                    ForgeConstants.DISP_CURRENT_COLORS_NEVER, ForgeConstants.DISP_CURRENT_COLORS_MULTICOLOR, 
                    ForgeConstants.DISP_CURRENT_COLORS_CHANGED, ForgeConstants.DISP_CURRENT_COLORS_MULTI_OR_CHANGED,
                    ForgeConstants.DISP_CURRENT_COLORS_ALWAYS}),
                4);

        //Card Overlays
        lstSettings.addItem(new BooleanSetting(FPref.UI_SHOW_CARD_OVERLAYS,
                "Show Card Overlays",
                "Show name, mana cost, p/t, and id overlays for cards, otherwise they're hidden."),
                5);
        lstSettings.addItem(new BooleanSetting(FPref.UI_OVERLAY_CARD_NAME,
                "Show Card Name Overlays",
                "Show name overlays for cards, otherwise they're hidden."),
                5);
        lstSettings.addItem(new BooleanSetting(FPref.UI_OVERLAY_CARD_MANA_COST,
                "Show Card Mana Cost Overlays",
                "Show mana cost overlays for cards, otherwise they're hidden."),
                5);
        lstSettings.addItem(new BooleanSetting(FPref.UI_OVERLAY_CARD_POWER,
                "Show Card P/T Overlays",
                "Show power/toughness/loyalty overlays for cards, otherwise they're hidden."),
                5);
        lstSettings.addItem(new BooleanSetting(FPref.UI_OVERLAY_CARD_ID,
                "Show Card ID Overlays",
                "Show id overlays for cards, otherwise they're hidden."),
                5);

        //Vibration Options
        lstSettings.addItem(new BooleanSetting(FPref.UI_VIBRATE_ON_LIFE_LOSS,
                "Vibrate When Losing Life",
                "Enable vibration when your player loses life or takes damage during a game."),
                6);
        lstSettings.addItem(new BooleanSetting(FPref.UI_VIBRATE_ON_LONG_PRESS,
                "Vibrate After Long Press",
                "Enable quick vibration to signify a long press, such as for card zooming."),
                6);

        //Sound Options
        lstSettings.addItem(new BooleanSetting(FPref.UI_ENABLE_SOUNDS,
                "Enable Sounds",
                "Enable sound effects during the game."),
                7);
        lstSettings.addItem(new BooleanSetting(FPref.UI_ENABLE_MUSIC,
                "Enable Music",
                "Enable background music during the game.") {
                    @Override
                    public void select() {
                        super.select();
                        //update background music when this setting changes
                        SoundSystem.instance.changeBackgroundTrack();
                    }
                }, 7);
        /*lstSettings.addItem(new BooleanSetting(FPref.UI_ALT_SOUND_SYSTEM,
                "Use Alternate Sound System",
                "Use the alternate sound system (only use if you have issues with sound not playing or disappearing)."),
                7);*/
    }

    @Override
    protected void doLayout(float width, float height) {
        lstSettings.setBounds(0, 0, width, height);
    }

    private abstract class Setting {
        protected String label;
        protected String description;
        protected FPref pref;

        public Setting(FPref pref0, String label0, String description0) {
            label = label0;
            description = description0;
            pref = pref0;
        }

        public abstract void select();
        public abstract void drawPrefValue(Graphics g, FSkinFont font, FSkinColor color, float x, float y, float width, float height);
    }

    private class BooleanSetting extends Setting {
        public BooleanSetting(FPref pref0, String label0, String description0) {
            super(pref0, label0, description0);
        }

        @Override
        public void select() {
            FModel.getPreferences().setPref(pref, !FModel.getPreferences().getPrefBoolean(pref));
            FModel.getPreferences().save();
        }

        @Override
        public void drawPrefValue(Graphics g, FSkinFont font, FSkinColor color, float x, float y, float w, float h) {
            x += w - h;
            w = h;
            FCheckBox.drawCheckBox(g, SettingsScreen.DESC_COLOR, color, FModel.getPreferences().getPrefBoolean(pref), x, y, w, h);
        }
    }

    private class CustomSelectSetting extends Setting {
        private final List<String> options = new ArrayList<String>();

        public CustomSelectSetting(FPref pref0, String label0, String description0, String[] options0) {
            super(pref0, label0 + ":", description0);

            for (String option : options0) {
                options.add(option);
            }
        }
        public CustomSelectSetting(FPref pref0, String label0, String description0, Iterable<String> options0) {
            super(pref0, label0 + ":", description0);

            for (String option : options0) {
                options.add(option);
            }
        }
        public <E extends Enum<E>> CustomSelectSetting(FPref pref0, String label0, String description0, Class<E> enumData) {
            super(pref0, label0 + ":", description0);

            for (E option : enumData.getEnumConstants()) {
                options.add(option.toString());
            }
        }

        public void valueChanged(String newValue) {
            FModel.getPreferences().setPref(pref, newValue);
            FModel.getPreferences().save();
        }

        @Override
        public void select() {
            Forge.openScreen(new CustomSelectScreen());
        }

        private class CustomSelectScreen extends FScreen {
            private final FList<String> lstOptions;
            private final String currentValue = FModel.getPreferences().getPref(pref);

            private CustomSelectScreen() {
                super("Select " + label.substring(0, label.length() - 1));
                lstOptions = add(new FList<String>(options));
                lstOptions.setListItemRenderer(new FList.DefaultListItemRenderer<String>() {
                    @Override
                    public boolean tap(Integer index, String value, float x, float y, int count) {
                        if (!value.equals(currentValue)) {
                            valueChanged(value);
                        }
                        Forge.back();
                        return true;
                    }

                    @Override
                    public void drawValue(Graphics g, Integer index, String value, FSkinFont font, FSkinColor foreColor, FSkinColor backColor, boolean pressed, float x, float y, float w, float h) {
                        float offset = SettingsScreen.getInsets(w) - FList.PADDING; //increase padding for settings items
                        x += offset;
                        y += offset;
                        w -= 2 * offset;
                        h -= 2 * offset;

                        g.drawText(value, font, foreColor, x, y, w, h, false, HAlignment.LEFT, true);

                        float radius = h / 3;
                        x += w - radius;
                        y += h / 2;
                        g.drawCircle(Utils.scale(1), SettingsScreen.DESC_COLOR, x, y, radius);
                        if (value.equals(currentValue)) {
                            g.fillCircle(foreColor, x, y, radius / 2);
                        }
                    }
                });
            }

            @Override
            protected void doLayout(float startY, float width, float height) {
                lstOptions.setBounds(0, startY, width, height - startY);
            }

            @Override
            public FScreen getLandscapeBackdropScreen() {
                if (SettingsScreen.launchedFromHomeScreen()) {
                    return HomeScreen.instance;
                }
                return null;
            }
        }

        @Override
        public void drawPrefValue(Graphics g, FSkinFont font, FSkinColor color, float x, float y, float w, float h) {
            g.drawText(FModel.getPreferences().getPref(pref), font, color, x, y, w, h, false, HAlignment.RIGHT, false);
        }
    }

    private class SettingRenderer extends FList.ListItemRenderer<Setting> {
        @Override
        public float getItemHeight() {
            return SettingsScreen.SETTING_HEIGHT;
        }

        @Override
        public boolean tap(Integer index, Setting value, float x, float y, int count) {
            value.select();
            return true;
        }

        @Override
        public void drawValue(Graphics g, Integer index, Setting value, FSkinFont font, FSkinColor foreColor, FSkinColor backColor, boolean pressed, float x, float y, float w, float h) {
            float offset = SettingsScreen.getInsets(w) - FList.PADDING; //increase padding for settings items
            x += offset;
            y += offset;
            w -= 2 * offset;
            h -= 2 * offset;

            float totalHeight = h;
            h = font.getMultiLineBounds(value.label).height + SettingsScreen.SETTING_PADDING;

            g.drawText(value.label, font, foreColor, x, y, w, h, false, HAlignment.LEFT, false);
            value.drawPrefValue(g, font, foreColor, x, y, w, h);
            h += SettingsScreen.SETTING_PADDING;
            g.drawText(value.description, SettingsScreen.DESC_FONT, SettingsScreen.DESC_COLOR, x, y + h, w, totalHeight - h + SettingsScreen.getInsets(w), true, HAlignment.LEFT, false);
        }
    }
}
