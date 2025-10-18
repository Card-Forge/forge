package forge.screens.settings;

import com.badlogic.gdx.utils.Align;
import com.google.common.collect.Lists;
import forge.Forge;
import forge.Graphics;
import forge.MulliganDefs;
import forge.StaticData;
import forge.adventure.util.Config;
import forge.ai.AiProfileUtil;
import forge.assets.*;
import forge.game.GameLogEntryType;
import forge.gui.GuiBase;
import forge.localinstance.properties.ForgeConstants;
import forge.localinstance.properties.ForgeNetPreferences;
import forge.localinstance.properties.ForgePreferences;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.localinstance.properties.PreferencesStore;
import forge.model.FModel;
import forge.screens.FScreen;
import forge.screens.TabPageScreen;
import forge.screens.TabPageScreen.TabPage;
import forge.screens.home.HomeScreen;
import forge.screens.match.MatchController;
import forge.sound.MusicPlaylist;
import forge.sound.SoundSystem;
import forge.toolbox.FCheckBox;
import forge.toolbox.FGroupList;
import forge.toolbox.FList;
import forge.toolbox.FOptionPane;
import forge.util.Utils;

import java.util.*;

public class SettingsPage extends TabPage<SettingsScreen> {
    private final FGroupList<Setting> lstSettings = add(new FGroupList<>());
    private final CustomSelectSetting settingSkins;
    private final CustomSelectSetting settingCJKFonts;

    public SettingsPage() {
        super(Forge.getLocalizer().getMessage("lblSettings"), Forge.hdbuttons ? FSkinImage.HDPREFERENCE : FSkinImage.SETTINGS);

        lstSettings.setListItemRenderer(new SettingRenderer());

        lstSettings.addGroup(Forge.getLocalizer().getMessage("lblGeneralSettings"));
        lstSettings.addGroup(Forge.getLocalizer().getMessage("lblGameplayOptions"));
        lstSettings.addGroup(Forge.getLocalizer().getMessage("RandomDeckGeneration"));
        lstSettings.addGroup(Forge.getLocalizer().getMessage("AdvancedSettings"));
        lstSettings.addGroup(Forge.getLocalizer().getMessage("GraphicOptions"));
        lstSettings.addGroup(Forge.getLocalizer().getMessage("lblCardOverlays"));
        lstSettings.addGroup(Forge.getLocalizer().getMessage("lblVibrationOptions"));
        lstSettings.addGroup(Forge.getLocalizer().getMessage("SoundOptions"));
        lstSettings.addGroup(Forge.getLocalizer().getMessage("ServerPreferences"));

        // GENERAL SETTINGS TAB
        lstSettings.addItem(new CustomSelectSetting(FPref.UI_LANGUAGE, Forge.getLocalizer().getMessage("cbpSelectLanguage"),
                Forge.getLocalizer().getMessage("nlSelectLanguage"),
                FLanguage.getAllLanguages()) {
            @Override
            public void valueChanged(String newValue) {
                // if the new locale needs to use CJK font, disallow change if UI_CJK_FONT is not set yet
                ForgePreferences prefs = FModel.getPreferences();
                if (prefs.getPref(FPref.UI_CJK_FONT).isEmpty() && (newValue.equals("zh-CN") || newValue.equals("ja-JP"))) {
                    String message = "Please download CJK font (from \"Files\"), and set it before change language.";
                    if (newValue.equals("zh-CN")) {
                        message += "\nChinese please use \"SourceHanSansCN\".";
                    }
                    if (newValue.equals("ja-JP")) {
                        message += "\nJapanese please use \"SourceHanSansJP\".";
                    }
                    FOptionPane.showMessageDialog(message, "Please set CJK Font");
                    return;
                }

                FLanguage.changeLanguage(newValue);

                FOptionPane.showConfirmDialog(Forge.getLocalizer().getMessage("lblRestartForgeDescription"), Forge.getLocalizer().getMessage("lblRestartForge"), Forge.getLocalizer().getMessage("lblRestart"), Forge.getLocalizer().getMessage("lblLater"), result -> {
                    if (result) {
                        Forge.restart(true);
                    }
                });
            }
        }, 0);
        settingSkins = new CustomSelectSetting(FPref.UI_SKIN, Forge.getLocalizer().getMessage("lblTheme"),
            Forge.getLocalizer().getMessage("nlTheme"),
            FSkin.getAllSkins()) {
                @Override
                public void valueChanged(String newValue) {
                    FSkin.changeSkin(newValue);
                }
            };
        lstSettings.addItem(settingSkins, 0);
        settingCJKFonts = new CustomSelectSetting(FPref.UI_CJK_FONT, Forge.getLocalizer().getMessage("lblCJKFont"),
            Forge.getLocalizer().getMessage("nlCJKFont"),
            FSkinFont.getAllCJKFonts()) {
                @Override
                public void valueChanged(String newValue) {
                    ForgePreferences prefs = FModel.getPreferences();
                    if (newValue.equals("None")) {
                        // If locale needs to use CJK fonts, disallow change to None
                        String locale = prefs.getPref(FPref.UI_LANGUAGE);
                        if (locale.equals("zh-CN") || locale.equals("ja-JP")) {
                            return;
                        }
                        newValue = "";
                    }
                    if (newValue.equals(prefs.getPref(FPref.UI_CJK_FONT))) {
                        return;
                    }
                    super.valueChanged(newValue);
                }
            };
        lstSettings.addItem(settingCJKFonts, 0);
        if (GuiBase.isAndroid()) {
            lstSettings.addItem(new BooleanSetting(FPref.UI_LANDSCAPE_MODE,
                Forge.getLocalizer().getMessage("lblLandscapeMode"),
                Forge.getLocalizer().getMessage("nlLandscapeMode")) {
                    @Override
                    public void select() {
                        super.select();
                        boolean landscapeMode = FModel.getPreferences().getPrefBoolean(FPref.UI_LANDSCAPE_MODE);
                        Forge.getDeviceAdapter().setLandscapeMode(landscapeMode); //ensure device able to save off ini file so landscape change takes effect
                        if (Forge.isLandscapeMode() != landscapeMode) {
                            FOptionPane.showConfirmDialog(Forge.getLocalizer().getMessage("lblRestartForgeDescription"), Forge.getLocalizer().getMessage("lblRestartForge"), Forge.getLocalizer().getMessage("lblRestart"), Forge.getLocalizer().getMessage("lblLater"), result -> {
                                if (result) {
                                    Forge.restart(true);
                                }
                            });
                        }
                    }
                }, 0);
            /*lstSettings.addItem(new BooleanSetting(FPref.UI_ANDROID_MINIMIZE_ON_SCRLOCK,
                Forge.getLocalizer().getMessage("lblMinimizeScreenLock"),
                Forge.getLocalizer().getMessage("nlMinimizeScreenLock")), 0);*/
        } else {
            //fullscreen
            lstSettings.addItem(new BooleanSetting(FPref.UI_FULLSCREEN_MODE,
                Forge.getLocalizer().getMessage("lblFullScreenMode"),
                Forge.getLocalizer().getMessage("nlFullScreenMode")) {
                    @Override
                    public void select() {
                        super.select();
                        Config.instance().getSettingData().fullScreen = FModel.getPreferences().getPrefBoolean(FPref.UI_FULLSCREEN_MODE);
                        Config.instance().saveSettings();
                    }
                }, 0);
            lstSettings.addItem(new CustomSelectSetting(FPref.UI_VIDEO_MODE,
                Forge.getLocalizer().getMessage("lblVideoMode"),
                Forge.getLocalizer().getMessage("nlVideoMode"),
                ForgeConstants.VIDEO_MODES) {
                    @Override
                    public void valueChanged(String newValue) {
                        super.valueChanged(newValue);
                        Graphics.setVideoMode(newValue);
                    }
                }, 0);
        }
        lstSettings.addItem(new CustomSelectSetting(FPref.UI_SELECTOR_MODE,
            Forge.getLocalizer().getMessage("lblSelectorMode"),
            Forge.getLocalizer().getMessage("nlSelectorMode"),
            new String[] { "Default", "Classic", "Adventure" }) {
                @Override
                public void valueChanged(String newValue) {
                    super.valueChanged(newValue);
                    Forge.selector = FModel.getPreferences().getPref(FPref.UI_SELECTOR_MODE);
                }
            }, 0);
        lstSettings.addItem(new BooleanSetting(FPref.USE_SENTRY,
            Forge.getLocalizer().getMessage("lblAutomaticBugReports"),
            Forge.getLocalizer().getMessage("nlAutomaticBugReports")), 0);

        // GAMEPLAY OPTIONS TAB
        lstSettings.addItem(new CustomSelectSetting(FPref.MULLIGAN_RULE,
            Forge.getLocalizer().getMessage("cbpMulliganRule"),
            Forge.getLocalizer().getMessage("nlpMulliganRule"),
            MulliganDefs.getMulliganRuleNames()) {
                @Override
                public void valueChanged(String newValue) {
                    super.valueChanged(newValue);
                    StaticData.instance().setMulliganRule(MulliganDefs.GetRuleByName(FModel.getPreferences().getPref(FPref.MULLIGAN_RULE)));
                }
            }, 1);
        lstSettings.addItem(new CustomSelectSetting(FPref.UI_CURRENT_AI_PROFILE,
            Forge.getLocalizer().getMessage("cbpAiProfiles"),
            Forge.getLocalizer().getMessage("nlpAiProfiles"),
            AiProfileUtil.getProfilesArray()), 1);
        lstSettings.addItem(new BooleanSetting(FPref.UI_ANTE,
            Forge.getLocalizer().getMessage("cbAnte"),
            Forge.getLocalizer().getMessage("nlAnte")), 1);
        lstSettings.addItem(new BooleanSetting(FPref.UI_ANTE_MATCH_RARITY,
            Forge.getLocalizer().getMessage("cbAnteMatchRarity"),
            Forge.getLocalizer().getMessage("nlAnteMatchRarity")), 1);
        lstSettings.addItem(new BooleanSetting(FPref.MATCH_HOT_SEAT_MODE,
            Forge.getLocalizer().getMessage("lblHotSeatMode"),
            Forge.getLocalizer().getMessage("nlHotSeatMode")), 1);
        lstSettings.addItem(new BooleanSetting(FPref.UI_ENABLE_AI_CHEATS,
            Forge.getLocalizer().getMessage("cbEnableAICheats"),
            Forge.getLocalizer().getMessage("nlEnableAICheats")), 1);
        lstSettings.addItem(new BooleanSetting(FPref.UI_MANABURN,
            Forge.getLocalizer().getMessage("cbManaBurn"),
            Forge.getLocalizer().getMessage("nlManaBurn")), 1);
        lstSettings.addItem(new BooleanSetting(FPref.LEGACY_ORDER_COMBATANTS,
            Forge.getLocalizer().getMessage("cbOrderCombatants"),
            Forge.getLocalizer().getMessage("nlOrderCombatants")), 1);
        lstSettings.addItem(new BooleanSetting(FPref.UI_MANA_LOST_PROMPT,
            Forge.getLocalizer().getMessage("cbManaLostPrompt"),
            Forge.getLocalizer().getMessage("nlManaLostPrompt")), 1);
        lstSettings.addItem(new BooleanSetting(FPref.ENFORCE_DECK_LEGALITY,
            Forge.getLocalizer().getMessage("cbEnforceDeckLegality"),
            Forge.getLocalizer().getMessage("nlEnforceDeckLegality")), 1);
        lstSettings.addItem(new BooleanSetting(FPref.PERFORMANCE_MODE,
            Forge.getLocalizer().getMessage("cbPerformanceMode"),
            Forge.getLocalizer().getMessage("nlPerformanceMode")), 1);
        lstSettings.addItem(new CustomSelectSetting(FPref.MATCH_AI_SIDEBOARDING_MODE,
            Forge.getLocalizer().getMessage("cbpAiSideboardingMode"),
            Forge.getLocalizer().getMessage("nlpAiSideboardingMode"),
            Lists.newArrayList("Off", "AI", "Human For AI")) {
                @Override
                public void valueChanged(String newValue) {
                    super.valueChanged(newValue);
                    AiProfileUtil.setAiSideboardingMode(AiProfileUtil.AISideboardingMode.normalizedValueOf(newValue));
                }
            }, 1);
        lstSettings.addItem(new BooleanSetting(FPref.MATCH_EXPERIMENTAL_RESTORE,
            Forge.getLocalizer().getMessage("cbExperimentalRestore"),
            Forge.getLocalizer().getMessage("nlExperimentalRestore")), 1);
        lstSettings.addItem(new CustomSelectSetting(FPref.MATCH_AI_TIMEOUT, Forge.getLocalizer().getMessage("cbAITimeout"),
            Forge.getLocalizer().getMessage("nlAITimeout"),
            Lists.newArrayList("5", "10", "60", "120", "240", "300", "600")), 1);
        lstSettings.addItem(new BooleanSetting(FPref.UI_ORDER_HAND,
            Forge.getLocalizer().getMessage("cbOrderHand"),
            Forge.getLocalizer().getMessage("nlOrderHand")), 1);
        lstSettings.addItem(new BooleanSetting(FPref.FILTERED_HANDS,
            Forge.getLocalizer().getMessage("cbFilteredHands"),
            Forge.getLocalizer().getMessage("nlFilteredHands")), 1);
        lstSettings.addItem(new BooleanSetting(FPref.UI_CLONE_MODE_SOURCE,
            Forge.getLocalizer().getMessage("cbCloneImgSource"),
            Forge.getLocalizer().getMessage("nlCloneImgSource")), 1);
        lstSettings.addItem(new BooleanSetting(FPref.UI_DETAILED_SPELLDESC_IN_PROMPT,
            Forge.getLocalizer().getMessage("cbDetailedPaymentDesc"),
            Forge.getLocalizer().getMessage("nlDetailedPaymentDesc")), 1);
        lstSettings.addItem(new BooleanSetting(FPref.UI_GRAY_INACTIVE_TEXT,
            Forge.getLocalizer().getMessage("cbGrayText"),
            Forge.getLocalizer().getMessage("nlGrayText")), 1);
        lstSettings.addItem(new BooleanSetting(FPref.UI_SHOW_STORM_COUNT_IN_PROMPT,
            Forge.getLocalizer().getMessage("cbShowStormCount"),
            Forge.getLocalizer().getMessage("nlShowStormCount")), 1);
        lstSettings.addItem(new BooleanSetting(FPref.UI_PRESELECT_PREVIOUS_ABILITY_ORDER,
            Forge.getLocalizer().getMessage("cbPreselectPrevAbOrder"),
            Forge.getLocalizer().getMessage("nlPreselectPrevAbOrder")), 1);
        lstSettings.addItem(new CustomSelectSetting(FPref.UI_ALLOW_ORDER_GRAVEYARD_WHEN_NEEDED,
            Forge.getLocalizer().getMessage("lblOrderGraveyard"),
            Forge.getLocalizer().getMessage("nlOrderGraveyard"),
            new String[] {
                ForgeConstants.GRAVEYARD_ORDERING_NEVER, ForgeConstants.GRAVEYARD_ORDERING_OWN_CARDS,
                ForgeConstants.GRAVEYARD_ORDERING_ALWAYS
            }), 1);
        lstSettings.addItem(new CustomSelectSetting(FPref.UI_AUTO_YIELD_MODE,
            Forge.getLocalizer().getMessage("lblAutoYields"),
            Forge.getLocalizer().getMessage("nlpAutoYieldMode"),
            new String[] { ForgeConstants.AUTO_YIELD_PER_ABILITY, ForgeConstants.AUTO_YIELD_PER_CARD }), 1);
        lstSettings.addItem(new BooleanSetting(FPref.UI_ALLOW_ESC_TO_END_TURN,
            Forge.getLocalizer().getMessage("cbEscapeEndsTurn"),
            Forge.getLocalizer().getMessage("nlEscapeEndsTurn")), 1);
        lstSettings.addItem(new BooleanSetting(FPref.UI_ALT_PLAYERINFOLAYOUT,
            Forge.getLocalizer().getMessage("lblAltLifeDisplay"),
            Forge.getLocalizer().getMessage("nlAltLifeDisplay")) {
                @Override
                public void select() {
                    super.select();
                    //update
                    Forge.altPlayerLayout = FModel.getPreferences().getPrefBoolean(FPref.UI_ALT_PLAYERINFOLAYOUT);
                    if (MatchController.instance != null)
                        MatchController.instance.resetPlayerPanels();
                }
            }, 1);
        lstSettings.addItem(new CustomSelectSetting(FPref.UI_ALT_PLAYERZONETABS,
            Forge.getLocalizer().getMessage("lblAltZoneTabs"),
            Forge.getLocalizer().getMessage("nlAltZoneTabs"),
            Lists.newArrayList("Off", "Vertical", "Horizontal")) {
                @Override
                public void valueChanged(String newValue) {
                    super.valueChanged(newValue);
                    //update
                    Forge.setAltZoneTabMode(FModel.getPreferences().getPref(FPref.UI_ALT_PLAYERZONETABS));
                    if (MatchController.instance != null)
                        MatchController.instance.resetPlayerPanels();
                }
            }, 1);
        lstSettings.addItem(new BooleanSetting(FPref.UI_ANIMATED_CARD_TAPUNTAP,
            Forge.getLocalizer().getMessage("lblAnimatedCardTapUntap"),
            Forge.getLocalizer().getMessage("nlAnimatedCardTapUntap")) {
                @Override
                public void select() {
                    super.select();
                    //update
                    Forge.animatedCardTapUntap = FModel.getPreferences().getPrefBoolean(FPref.UI_ANIMATED_CARD_TAPUNTAP);
                }
            }, 1);
        lstSettings.addItem(new BooleanSetting(FPref.UI_STACK_CREATURES,
            Forge.getLocalizer().getMessage("cbStackCreatures"),
            Forge.getLocalizer().getMessage("nlStackCreatures")), 1);
        lstSettings.addItem(new BooleanSetting(FPref.UI_AUTO_AIDECK_SELECTION,
            Forge.getLocalizer().getMessage("lblAutoAIDeck"),
            Forge.getLocalizer().getMessage("nlAutoAIDeck")) {
                @Override
                public void select() {
                    super.select();
                    //update
                    Forge.autoAIDeckSelection = FModel.getPreferences().getPrefBoolean(FPref.UI_AUTO_AIDECK_SELECTION);
                }
            }, 1);
        // Preffered Prompt button
        lstSettings.addItem(new BooleanSetting(FPref.UI_REVERSE_PROMPT_BUTTON,
            Forge.getLocalizer().getMessage("lblReversePromptButton"),
            Forge.getLocalizer().getMessage("nlReversePromptButton")) {
                @Override
                public void select() {
                    super.select();
                    //update
                    Forge.reversedPrompt = FModel.getPreferences().getPrefBoolean(FPref.UI_REVERSE_PROMPT_BUTTON);
                }
            }, 1);

        // RANDOM DECK GENERATION TAB
        lstSettings.addItem(new BooleanSetting(FPref.DECKGEN_NOSMALL,
            Forge.getLocalizer().getMessage("cbRemoveSmall"),
            Forge.getLocalizer().getMessage("nlRemoveSmall")), 2);
        lstSettings.addItem(new BooleanSetting(FPref.DECKGEN_CARDBASED,
            Forge.getLocalizer().getMessage("cbCardBased"),
            Forge.getLocalizer().getMessage("nlCardBased")), 2);
        lstSettings.addItem(new BooleanSetting(FPref.DECKGEN_SINGLETONS,
            Forge.getLocalizer().getMessage("cbSingletons"),
            Forge.getLocalizer().getMessage("nlSingletons")), 2);
        lstSettings.addItem(new BooleanSetting(FPref.DECKGEN_ARTIFACTS,
            Forge.getLocalizer().getMessage("cbRemoveArtifacts"),
            Forge.getLocalizer().getMessage("nlRemoveArtifacts")), 2);

        // ADVANCED SETTINGS TAB
        lstSettings.addItem(new BooleanSetting(FPref.DEV_MODE_ENABLED,
            Forge.getLocalizer().getMessage("cbDevMode"),
            Forge.getLocalizer().getMessage("nlDevMode")) {
                @Override
                public void select() {
                    super.select();
                    //update DEV_MODE flag when preference changes
                    ForgePreferences.DEV_MODE = FModel.getPreferences().getPrefBoolean(FPref.DEV_MODE_ENABLED);
                }
            }, 3);
        lstSettings.addItem(new CustomSelectSetting(FPref.DEV_LOG_ENTRY_TYPE,
            Forge.getLocalizer().getMessage("cbpGameLogEntryType"),
            Forge.getLocalizer().getMessage("nlGameLogEntryType"),
            GameLogEntryType.class), 3);
        lstSettings.addItem(new BooleanSetting(FPref.LOAD_CARD_SCRIPTS_LAZILY,
            Forge.getLocalizer().getMessage("cbLoadCardsLazily"),
            Forge.getLocalizer().getMessage("nlLoadCardsLazily")), 3);
        lstSettings.addItem(new BooleanSetting(FPref.LOAD_ARCHIVED_FORMATS,
            Forge.getLocalizer().getMessage("cbLoadArchivedFormats"),
            Forge.getLocalizer().getMessage("nlLoadArchivedFormats")), 3);
        lstSettings.addItem(new BooleanSetting(FPref.PRELOAD_CUSTOM_DRAFTS,
            Forge.getLocalizer().getMessage("cbPreloadCustomDrafts"),
            Forge.getLocalizer().getMessage("nlPreloadCustomDrafts")), 3);
        lstSettings.addItem(new BooleanSetting(FPref.UI_LOAD_UNKNOWN_CARDS,
            Forge.getLocalizer().getMessage("lblEnableUnknownCards"),
            Forge.getLocalizer().getMessage("nlEnableUnknownCards")) {
                @Override
                public void select() {
                    super.select();
                        FOptionPane.showConfirmDialog(
                            Forge.getLocalizer().getMessage("lblRestartForgeDescription"),
                            Forge.getLocalizer().getMessage("lblRestartForge"),
                            Forge.getLocalizer().getMessage("lblRestart"),
                            Forge.getLocalizer().getMessage("lblLater"), result -> {
                                if (result) {
                                    Forge.restart(true);
                                }
                            }
                        );
                }
            }, 3);
        lstSettings.addItem(new BooleanSetting(FPref.UI_LOAD_NONLEGAL_CARDS,
            Forge.getLocalizer().getMessage("lblEnableNonLegalCards"),
            Forge.getLocalizer().getMessage("nlEnableNonLegalCards")) {
                @Override
                public void select() {
                    super.select();
                    FOptionPane.showConfirmDialog(
                        Forge.getLocalizer().getMessage("lblRestartForgeDescription"),
                        Forge.getLocalizer().getMessage("lblRestartForge"),
                        Forge.getLocalizer().getMessage("lblRestart"),
                        Forge.getLocalizer().getMessage("lblLater"), result -> {
                            if (result) {
                                Forge.restart(true);
                            }
                        }
                    );
                }
            }, 3);
        lstSettings.addItem(new BooleanSetting(FPref.ALLOW_CUSTOM_CARDS_IN_DECKS_CONFORMANCE,
            Forge.getLocalizer().getMessage("lblAllowCustomCardsInDecks"),
            Forge.getLocalizer().getMessage("nlAllowCustomCardsInDecks")) {
                @Override
                public void select() {
                    super.select();
                    FOptionPane.showConfirmDialog(
                        Forge.getLocalizer().getMessage("lblRestartForgeDescription"),
                        Forge.getLocalizer().getMessage("lblRestartForge"),
                        Forge.getLocalizer().getMessage("lblRestart"),
                        Forge.getLocalizer().getMessage("lblLater"), result -> {
                            if (result) {
                                Forge.restart(true);
                            }
                        }
                    );
               }
            }, 3);
        lstSettings.addItem(new BooleanSetting(FPref.UI_NETPLAY_COMPAT,
            Forge.getLocalizer().getMessage("lblExperimentalNetworkCompatibility"),
            Forge.getLocalizer().getMessage("nlExperimentalNetworkCompatibility")) {
                @Override
                public void select() {
                    super.select();
                    GuiBase.enablePropertyConfig(FModel.getPreferences().getPrefBoolean(FPref.UI_NETPLAY_COMPAT));
                }
            }, 3);
        lstSettings.addItem(new BooleanSetting(FPref.UI_ENABLE_DISPOSE_TEXTURES,
            Forge.getLocalizer().getMessage("lblDisposeTextures"),
            Forge.getLocalizer().getMessage("nlDisposeTextures")) {
                @Override
                    public void select() {
                        super.select();
                        Forge.disposeTextures = FModel.getPreferences().getPrefBoolean(FPref.UI_ENABLE_DISPOSE_TEXTURES);
                    }
                }, 3);
        if (GuiBase.isAndroid()) { //this option does nothing except on Android
            lstSettings.addItem(new BooleanSetting(FPref.UI_AUTO_CACHE_SIZE,
                Forge.getLocalizer().getMessage("lblAutoCacheSize"),
                Forge.getLocalizer().getMessage("nlAutoCacheSize")) {
                    @Override
                    public void select() {
                        super.select();
                        FOptionPane.showConfirmDialog (
                            Forge.getLocalizer().getMessage("lblRestartForgeDescription"),
                            Forge.getLocalizer().getMessage("lblRestartForge"),
                            Forge.getLocalizer().getMessage("lblRestart"),
                            Forge.getLocalizer().getMessage("lblLater"), result -> {
                                if (result) {
                                    Forge.restart(true);
                                }
                            }
                        );
                    }
                }, 3);
        }

        // GRAPHICS OPTIONS TAB
        lstSettings.addItem(new BooleanSetting(FPref.UI_DISABLE_CARD_IMAGES,
            Forge.getLocalizer().getMessage("lblDisableCardImages"),
            Forge.getLocalizer().getMessage("nlDisableCardImages")) {
                @Override
                public void select() {
                    super.select();
                    ImageCache.getInstance().disposeTextures();
                }
            }, 4);
        lstSettings.addItem(new BooleanSetting(FPref.UI_ENABLE_ONLINE_IMAGE_FETCHER,
            Forge.getLocalizer().getMessage("cbImageFetcher"),
            Forge.getLocalizer().getMessage("nlImageFetcher")), 4);
        lstSettings.addItem(new CustomSelectSetting(FPref.UI_PREFERRED_ART,
            Forge.getLocalizer().getMessage("lblPreferredArt"),
            Forge.getLocalizer().getMessage("nlPreferredArt"),
            FModel.getMagicDb().getCardArtAvailablePreferences()) {
                @Override
                public void valueChanged(String newValue) {
                    super.valueChanged(newValue);
                    FOptionPane.showConfirmDialog (
                        Forge.getLocalizer().getMessage("lblRestartForgeDescription"),
                        Forge.getLocalizer().getMessage("lblRestartForge"),
                        Forge.getLocalizer().getMessage("lblRestart"),
                        Forge.getLocalizer().getMessage("lblLater"), result -> {
                            if (result) {
                                Forge.restart(true);
                            }
                        }
                    );
                }
            }, 4);
        lstSettings.addItem(new BooleanSetting(FPref.UI_SMART_CARD_ART,
            Forge.getLocalizer().getMessage("lblSmartCardArtOpt"),
            Forge.getLocalizer().getMessage("nlSmartCardArtOpt") + "\n" +
            Forge.getLocalizer().getMessage("nlSmartCardArtOptNote")), 4);
        lstSettings.addItem(new BooleanSetting(FPref.UI_OVERLAY_FOIL_EFFECT,
            Forge.getLocalizer().getMessage("cbDisplayFoil"),
            Forge.getLocalizer().getMessage("nlDisplayFoil")), 4);
        lstSettings.addItem(new BooleanSetting(FPref.UI_RANDOM_FOIL,
            Forge.getLocalizer().getMessage("cbRandomFoil"),
            Forge.getLocalizer().getMessage("nlRandomFoil")), 4);
        lstSettings.addItem(new BooleanSetting(FPref.UI_RANDOM_ART_IN_POOLS,
            Forge.getLocalizer().getMessage("cbRandomArtInPools"),
            Forge.getLocalizer().getMessage("nlRandomArtInPools")), 4);
        lstSettings.addItem(new BooleanSetting(FPref.UI_COMPACT_TABS,
            Forge.getLocalizer().getMessage("lblCompactTabs"),
            Forge.getLocalizer().getMessage("nlCompactTabs")) {
                @Override
                public void select() {
                    super.select();
                    //update layout of screen when this setting changes
                    TabPageScreen.COMPACT_TABS = FModel.getPreferences().getPrefBoolean(FPref.UI_COMPACT_TABS);
                    parentScreen.revalidate();
                }
            }, 4);
        lstSettings.addItem(new BooleanSetting(FPref.UI_COMPACT_LIST_ITEMS,
            Forge.getLocalizer().getMessage("lblCompactListItems"),
            Forge.getLocalizer().getMessage("nlCompactListItems")), 4);
        lstSettings.addItem(new BooleanSetting(FPref.UI_HIDE_REMINDER_TEXT,
            Forge.getLocalizer().getMessage("cbHideReminderText"),
            Forge.getLocalizer().getMessage("nlHideReminderText")), 4);
        lstSettings.addItem(new BooleanSetting(FPref.UI_MATCH_IMAGE_VISIBLE,
            Forge.getLocalizer().getMessage("lblShowMatchBackground"),
            Forge.getLocalizer().getMessage("nlShowMatchBackground")), 4);
        lstSettings.addItem(new BooleanSetting(FPref.UI_LIBGDX_TEXTURE_FILTERING,
            Forge.getLocalizer().getMessage("lblBattlefieldTextureFiltering"),
            Forge.getLocalizer().getMessage("nlBattlefieldTextureFiltering")), 4);
        lstSettings.addItem(new CustomSelectSetting(FPref.UI_DISPLAY_CURRENT_COLORS,
            Forge.getLocalizer().getMessage("cbpDisplayCurrentCardColors"),
            Forge.getLocalizer().getMessage("nlDisplayCurrentCardColors"),
            new String[] {
                ForgeConstants.DISP_CURRENT_COLORS_NEVER, ForgeConstants.DISP_CURRENT_COLORS_MULTICOLOR,
                ForgeConstants.DISP_CURRENT_COLORS_CHANGED, ForgeConstants.DISP_CURRENT_COLORS_MULTI_OR_CHANGED,
                ForgeConstants.DISP_CURRENT_COLORS_ALWAYS}), 4);
        lstSettings.addItem(new BooleanSetting(FPref.UI_ROTATE_SPLIT_CARDS,
            Forge.getLocalizer().getMessage("lblRotateZoomSplit"),
            Forge.getLocalizer().getMessage("nlRotateZoomSplit")), 4);
        lstSettings.addItem(new BooleanSetting(FPref.UI_ROTATE_PLANE_OR_PHENOMENON,
            Forge.getLocalizer().getMessage("lblRotateZoomPlanesPhenomena"),
            Forge.getLocalizer().getMessage("nlRotateZoomPlanesPhenomena")), 4);
        lstSettings.addItem(new BooleanSetting(FPref.UI_DISABLE_IMAGES_EFFECT_CARDS,
            Forge.getLocalizer().getMessage("lblDisableCardEffect"),
            Forge.getLocalizer().getMessage("nlDisableCardEffect")), 4);
        lstSettings.addItem(new CustomSelectSetting(FPref.UI_ENABLE_BORDER_MASKING,
            Forge.getLocalizer().getMessage("lblBorderMaskOption"),
            Forge.getLocalizer().getMessage("nlBorderMaskOption"),
            new String[] { "Off", "Crop", "Full", "Art" }) {
                @Override
                public void valueChanged(String newValue) {
                    super.valueChanged(newValue);
                    Forge.enableUIMask = FModel.getPreferences().getPref(FPref.UI_ENABLE_BORDER_MASKING);
                    ImageCache.getInstance().disposeTextures();
                }
            }, 4);
        lstSettings.addItem(new BooleanSetting(FPref.UI_ENABLE_MATCH_SCROLL_INDICATOR,
            Forge.getLocalizer().getMessage("lblMatchScrollIndicator"),
            Forge.getLocalizer().getMessage("nlMatchScrollIndicator")), 4);
        if (!GuiBase.isAndroid()) {
            lstSettings.addItem(new BooleanSetting(FPref.UI_ENABLE_MAGNIFIER,
                Forge.getLocalizer().getMessage("lblEnableMagnifier"),
                Forge.getLocalizer().getMessage("nlEnableMagnifier")) {
                    @Override
                    public void select() {
                        super.select();
                        //set default
                        if (!FModel.getPreferences().getPrefBoolean(FPref.UI_ENABLE_MAGNIFIER)) {
                            Forge.setCursor(FSkin.getCursor().get(0), "0");
                        }
                    }
                },4);
        }
        lstSettings.addItem(new BooleanSetting(FPref.UI_SHOW_FPS,
            Forge.getLocalizer().getMessage("lblShowFPSDisplay"),
            Forge.getLocalizer().getMessage("nlShowFPSDisplay")) {
            @Override
            public void select() {
                super.select();
                //update
                Forge.showFPS = FModel.getPreferences().getPrefBoolean(FPref.UI_SHOW_FPS);
            }
        },4);
        lstSettings.addItem(new CustomSelectSetting(FPref.UI_CARD_COUNTER_DISPLAY_TYPE,
            Forge.getLocalizer().getMessage("cbpCounterDisplayType"),
            Forge.getLocalizer().getMessage("nlCounterDisplayType"),
            new String[] {
                ForgeConstants.CounterDisplayType.TEXT.getName(), ForgeConstants.CounterDisplayType.IMAGE.getName(),
                ForgeConstants.CounterDisplayType.HYBRID.getName(), ForgeConstants.CounterDisplayType.OLD_WHEN_SMALL.getName()
            }), 4);

        // CARD OVERLAYS TAB
        lstSettings.addItem(new BooleanSetting(FPref.UI_SHOW_CARD_OVERLAYS,
            Forge.getLocalizer().getMessage("lblShowCardOverlays"),
            Forge.getLocalizer().getMessage("nlShowCardOverlays")), 5);
        lstSettings.addItem(new BooleanSetting(FPref.UI_OVERLAY_CARD_NAME,
            Forge.getLocalizer().getMessage("lblShowCardNameOverlays"),
            Forge.getLocalizer().getMessage("nlShowCardNameOverlays")), 5);
        lstSettings.addItem(new BooleanSetting(FPref.UI_OVERLAY_CARD_MANA_COST,
            Forge.getLocalizer().getMessage("lblShowCardManaCostOverlays"),
            Forge.getLocalizer().getMessage("nlShowCardManaCostOverlays")), 5);
        lstSettings.addItem(new BooleanSetting(FPref.UI_OVERLAY_CARD_POWER,
            Forge.getLocalizer().getMessage("lblShowCardPTOverlays"),
            Forge.getLocalizer().getMessage("nlShowCardPTOverlays")), 5);
        lstSettings.addItem(new BooleanSetting(FPref.UI_OVERLAY_CARD_ID,
            Forge.getLocalizer().getMessage("lblShowCardIDOverlays"),
            Forge.getLocalizer().getMessage("nlShowCardIDOverlays")), 5);
        lstSettings.addItem(new BooleanSetting(FPref.UI_OVERLAY_DRAFT_RANKING,
            Forge.getLocalizer().getMessage("lblShowDraftRankingOverlay"),
            Forge.getLocalizer().getMessage("nlShowDraftRankingOverlay")), 5);
        lstSettings.addItem(new BooleanSetting(FPref.UI_OVERLAY_ABILITY_ICONS,
            Forge.getLocalizer().getMessage("lblShowAbilityIconsOverlays"),
            Forge.getLocalizer().getMessage("nlShowAbilityIconsOverlays")), 5);
        lstSettings.addItem(new BooleanSetting(FPref.UI_USE_LASER_ARROWS,
            Forge.getLocalizer().getMessage("lblUseLaserArrows"),
            Forge.getLocalizer().getMessage("nlUseLaserArrows")), 5);

        // VIBRATION OPTIONS TAB
        lstSettings.addItem(new BooleanSetting(FPref.UI_VIBRATE_ON_LIFE_LOSS,
            Forge.getLocalizer().getMessage("lblVibrateWhenLosingLife"),
            Forge.getLocalizer().getMessage("nlVibrateWhenLosingLife")), 6);
        lstSettings.addItem(new BooleanSetting(FPref.UI_VIBRATE_ON_LONG_PRESS,
            Forge.getLocalizer().getMessage("lblVibrateAfterLongPress"),
            Forge.getLocalizer().getMessage("nlVibrateAfterLongPress")), 6);

        // SOUND OPTIONS TAB
        lstSettings.addItem(new CustomSelectSetting(FPref.UI_CURRENT_SOUND_SET,
            Forge.getLocalizer().getMessage("cbpSoundSets"),
            Forge.getLocalizer().getMessage("nlpSoundSets"),
            SoundSystem.instance.getAvailableSoundSets()) {
                @Override
                public void valueChanged(String newValue) {
                    super.valueChanged(newValue);
                    SoundSystem.instance.invalidateSoundCache();
                }
            }, 7);
        lstSettings.addItem(new CustomSelectSetting(FPref.UI_CURRENT_MUSIC_SET,
            Forge.getLocalizer().getMessage("cbpMusicSets"),
            Forge.getLocalizer().getMessage("nlpMusicSets"),
            SoundSystem.getAvailableMusicSets()) {
                @Override
                    public void valueChanged(String newValue) {
                        super.valueChanged(newValue);
                            MusicPlaylist.invalidateMusicPlaylist();
                            SoundSystem.instance.changeBackgroundTrack();
                        }
                    }, 7);
        lstSettings.addItem(new CustomSelectSetting(FPref.UI_VOL_SOUNDS,
            Forge.getLocalizer().getMessage("cbAdjustSoundsVolume"),
            Forge.getLocalizer().getMessage("nlAdjustSoundsVolume"),
            new String[] { "0", "10", "20", "30", "40", "50", "60", "70", "80", "90", "100" }), 7);
        lstSettings.addItem(new CustomSelectSetting(FPref.UI_VOL_MUSIC,
            Forge.getLocalizer().getMessage("cbAdjustMusicVolume"),
            Forge.getLocalizer().getMessage("nlAdjustMusicVolume"),
            new String[] { "0", "10", "20", "30", "40", "50", "60", "70", "80", "90", "100" }) {
                @Override
                public void valueChanged(String newValue) {
                    super.valueChanged(newValue);
                    //update background music when this setting changes
                    SoundSystem.instance.changeBackgroundTrack();
                }
            }, 7);
        /*lstSettings.addItem(new BooleanSetting(FPref.UI_ALT_SOUND_SYSTEM,
            "Use Alternate Sound System",
            "Use the alternate sound system (only use if you have issues with sound not playing or disappearing)."), 7);*/
        lstSettings.addItem(new IntegerSelectSetting(
            ForgeNetPreferences.FNetPref.NET_PORT,
            Forge.getLocalizer().getMessage("lblServerPort"),
            Forge.getLocalizer().getMessage("nlServerPort"), 1024, 65535), 8);
        lstSettings.addItem(new LocalizedSelectSetting(
            ForgeNetPreferences.FNetPref.UPnP,
            Forge.getLocalizer().getMessage("lblUPnPTitle"),
            Forge.getLocalizer().getMessage("nlServerUPnPOptions"),
            ForgeConstants.getUPnPPreferenceMapping()), 8);
    }

    public void refreshSkinsList() {
        settingSkins.updateOptions(FSkin.getAllSkins());
    }

    public void refreshCJKFontsList() {
        settingCJKFonts.updateOptions(FSkinFont.getAllCJKFonts());
    }

    @Override
    protected void doLayout(float width, float height) {
        lstSettings.setBounds(0, 0, width, height);
    }

    private abstract class Setting {
        protected String label;
        protected String description;
        protected PreferencesStore.IPref pref;

        public Setting(PreferencesStore.IPref pref0, String label0, String description0) {
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
            if(pref instanceof FPref) {
                FModel.getPreferences().setPref((FPref)pref, !FModel.getPreferences().getPrefBoolean((FPref)pref));
                FModel.getPreferences().save();
            } else if(pref instanceof ForgeNetPreferences.FNetPref) {
                FModel.getNetPreferences().setPref((ForgeNetPreferences.FNetPref)pref, !FModel.getNetPreferences().getPrefBoolean((ForgeNetPreferences.FNetPref)pref));
                FModel.getNetPreferences().save();
            }
        }

        @Override
        public void drawPrefValue(Graphics g, FSkinFont font, FSkinColor color, float x, float y, float w, float h) {
            x += w - h;
            w = h;
            boolean checked = false;
            if(pref instanceof FPref) {
                checked = FModel.getPreferences().getPrefBoolean((FPref) pref);
            } else if(pref instanceof ForgeNetPreferences.FNetPref) {
                checked = FModel.getNetPreferences().getPrefBoolean((ForgeNetPreferences.FNetPref) pref);
            }
            FCheckBox.drawCheckBox(g, SettingsScreen.DESC_COLOR, color, checked, x, y, w, h);
        }
    }

    private class CustomSelectSetting extends Setting {
        private final List<String> options = new ArrayList<>();

        public CustomSelectSetting(PreferencesStore.IPref pref0, String label0, String description0, String[] options0) {
            super(pref0, label0 + ":", description0);

            options.addAll(Arrays.asList(options0));
        }
        public CustomSelectSetting(FPref pref0, String label0, String description0, Iterable<String> options0) {
            super(pref0, label0 + ":", description0);

            if (options0 != null) {
                for (String option : options0) {
                    options.add(option);
                }
            }
        }
        public <E extends Enum<E>> CustomSelectSetting(FPref pref0, String label0, String description0, Class<E> enumData) {
            super(pref0, label0 + ":", description0);

            for (E option : enumData.getEnumConstants()) {
                options.add(option.toString());
            }
        }

        public void valueChanged(String newValue) {
            if(pref instanceof FPref) {
                FModel.getPreferences().setPref((FPref)pref, newValue);
                FModel.getPreferences().save();
            } else if(pref instanceof ForgeNetPreferences.FNetPref) {
                FModel.getNetPreferences().setPref((ForgeNetPreferences.FNetPref)pref, newValue);
                FModel.getNetPreferences().save();
            }

        }

        public void updateOptions(Iterable<String> options0) {
            options.clear();
            if (options0 != null) {
                for (String option : options0) {
                    options.add(option);
                }
            }
        }

        @Override
        public void select() {
            Forge.openScreen(new CustomSelectScreen());
        }

        private class CustomSelectScreen extends FScreen {
            private final FList<String> lstOptions;
            private final String currentValue;

            private CustomSelectScreen() {
                super("Select " + label.substring(0, label.length() - 1));
                if(pref instanceof FPref) {
                    currentValue = FModel.getPreferences().getPref((FPref) pref);
                } else if(pref instanceof ForgeNetPreferences.FNetPref) {
                    currentValue = FModel.getNetPreferences().getPref((ForgeNetPreferences.FNetPref) pref);
                } else {
                    currentValue = null;
                }
                lstOptions = add(new FList<>(options));
                lstOptions.setListItemRenderer(new FList.DefaultListItemRenderer<String>() {
                    @Override
                    public boolean tap(Integer index, String value, float x, float y, int count) {
                        Forge.back();
                        if (!value.equals(currentValue)) {
                            valueChanged(value);
                        }
                        return true;
                    }

                    @Override
                    public void drawValue(Graphics g, Integer index, String value, FSkinFont font, FSkinColor foreColor, FSkinColor backColor, boolean pressed, float x, float y, float w, float h) {
                        float offset = SettingsScreen.getInsets(w) - FList.PADDING; //increase padding for settings items
                        x += offset;
                        y += offset;
                        w -= 2 * offset;
                        h -= 2 * offset;

                        g.drawText(value, font, foreColor, x, y, w, h, false, Align.left, true);

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
            String value;
            if(pref instanceof FPref) {
                value = FModel.getPreferences().getPref((FPref)pref);
            } else if(pref instanceof ForgeNetPreferences.FNetPref) {
                value = FModel.getNetPreferences().getPref((ForgeNetPreferences.FNetPref)pref);
            } else {
                value = "";
            }
            g.drawText(value, font, color, x, y, w, h, false, Align.right, false);
        }
    }

    private class LocalizedSelectSetting extends CustomSelectSetting {
        private final Map<String, String> localizedToBackingMap;
        private final Map<String, String> backingToLocalizedMap = new HashMap<>();

        public LocalizedSelectSetting(PreferencesStore.IPref pref0,
                                      String label0,
                                      String description0,
                                      Map<String, String> localizationMap) {
            super(pref0, label0, description0, localizationMap.keySet().toArray(new String[0]));
            this.localizedToBackingMap = localizationMap;

            // Create reverse lookup map
            for (Map.Entry<String, String> entry : localizationMap.entrySet()) {
                backingToLocalizedMap.put(entry.getValue(), entry.getKey());
            }
        }

        @Override
        public void valueChanged(String newLocalizedValue) {
            String backingValue = localizedToBackingMap.get(newLocalizedValue);
            if (backingValue == null) return;

            if (pref instanceof FPref) {
                FModel.getPreferences().setPref((FPref) pref, backingValue);
                FModel.getPreferences().save();
            } else if (pref instanceof ForgeNetPreferences.FNetPref) {
                FModel.getNetPreferences().setPref((ForgeNetPreferences.FNetPref) pref, backingValue);
                FModel.getNetPreferences().save();
            }
        }


        @Override
        public void drawPrefValue(Graphics g, FSkinFont font, FSkinColor color,
                                  float x, float y, float w, float h) {
            String currentBackingValue;
            if (pref instanceof FPref) {
                currentBackingValue = FModel.getPreferences().getPref((FPref) pref);
            } else if (pref instanceof ForgeNetPreferences.FNetPref) {
                currentBackingValue = FModel.getNetPreferences().getPref((ForgeNetPreferences.FNetPref) pref);
            } else {
                currentBackingValue = "";
            }

            String displayValue = backingToLocalizedMap.get(currentBackingValue);
            g.drawText(displayValue, font, color, x, y, w, h, false, Align.right, false);
        }
    }


    private class IntegerSelectSetting extends Setting {
        private final int minValue;
        private final int maxValue;

        public IntegerSelectSetting(PreferencesStore.IPref pref0, String label0, String description0, int minValue, int maxValue) {
            super(pref0, label0 + ":", description0);
            this.minValue = minValue;
            this.maxValue = maxValue;
        }

        public void valueChanged(String newValue) {
            if (pref instanceof FPref) {
                FModel.getPreferences().setPref((FPref) pref, newValue);
                FModel.getPreferences().save();
            } else if (pref instanceof ForgeNetPreferences.FNetPref) {
                FModel.getNetPreferences().setPref((ForgeNetPreferences.FNetPref) pref, newValue);
                FModel.getNetPreferences().save();
            }
        }

        @Override
        public void select() {
            String currentValue;
            if (pref instanceof FPref) {
                currentValue = FModel.getPreferences().getPref((FPref) pref);
            } else if (pref instanceof ForgeNetPreferences.FNetPref) {
                currentValue = FModel.getNetPreferences().getPref((ForgeNetPreferences.FNetPref) pref);
            } else {
                currentValue = "";
            }
            if (currentValue == null || currentValue.isEmpty()) {
                currentValue = String.valueOf(minValue);
            }
            FOptionPane.showInputDialog(
                    label,
                    description,
                    currentValue,
                    null,
                    input -> {
                        if (input == null) return; // cancelled
                        if (!input.matches("\\d+")) {
                            FOptionPane.showMessageDialog("Please enter a valid number.", "Invalid Input");
                            return;
                        }
                        int value = Integer.parseInt(input);
                        if (value < minValue || value > maxValue) {
                            FOptionPane.showMessageDialog("Value must be between " + minValue + " and " + maxValue + ".", "Invalid Input");
                            return;
                        }
                        valueChanged(input);
                    },
                    true // isNumeric
            );
        }

        @Override
        public void drawPrefValue(Graphics g, FSkinFont font, FSkinColor color, float x, float y, float w, float h) {
            String value;
            if (pref instanceof FPref) {
                value = FModel.getPreferences().getPref((FPref) pref);
            } else if (pref instanceof ForgeNetPreferences.FNetPref) {
                value = FModel.getNetPreferences().getPref((ForgeNetPreferences.FNetPref) pref);
            } else {
                value = "";
            }
            g.drawText(value, font, color, x, y, w, h, false, Align.right, false);
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

            g.drawText(value.label, font, foreColor, x, y, w, h, false, Align.left, false);
            value.drawPrefValue(g, font, foreColor, x, y, w, h);
            h += SettingsScreen.SETTING_PADDING;
            g.drawText(value.description, SettingsScreen.DESC_FONT, SettingsScreen.DESC_COLOR, x, y + h, w, totalHeight - h + SettingsScreen.getInsets(w), true, Align.left, false);
        }
    }
}
