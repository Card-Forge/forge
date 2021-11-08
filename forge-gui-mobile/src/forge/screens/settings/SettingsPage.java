package forge.screens.settings;

import com.badlogic.gdx.utils.Align;
import forge.Forge;
import forge.Graphics;
import forge.MulliganDefs;
import forge.StaticData;
import forge.ai.AiProfileUtil;
import forge.assets.*;
import forge.game.GameLogEntryType;
import forge.gui.GuiBase;
import forge.localinstance.properties.ForgeConstants;
import forge.localinstance.properties.ForgePreferences;
import forge.localinstance.properties.ForgePreferences.FPref;
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
import forge.util.Callback;
import forge.util.Localizer;
import forge.util.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SettingsPage extends TabPage<SettingsScreen> {
    private final FGroupList<Setting> lstSettings = add(new FGroupList<>());
    private final CustomSelectSetting settingSkins;
    private final CustomSelectSetting settingCJKFonts;

    public SettingsPage() {
        super(Localizer.getInstance().getMessage("lblSettings"), Forge.hdbuttons ? FSkinImage.HDPREFERENCE : FSkinImage.SETTINGS);

        final Localizer localizer = Localizer.getInstance();

        lstSettings.setListItemRenderer(new SettingRenderer());

        lstSettings.addGroup(localizer.getMessage("lblGeneralSettings"));
        lstSettings.addGroup(localizer.getMessage("lblGameplayOptions"));
        lstSettings.addGroup(localizer.getMessage("RandomDeckGeneration"));
        lstSettings.addGroup(localizer.getMessage("AdvancedSettings"));
        lstSettings.addGroup(localizer.getMessage("GraphicOptions"));
        lstSettings.addGroup(localizer.getMessage("lblCardOverlays"));
        lstSettings.addGroup(localizer.getMessage("lblVibrationOptions"));
        lstSettings.addGroup(localizer.getMessage("SoundOptions"));

        //General Settings
        lstSettings.addItem(new CustomSelectSetting(FPref.UI_LANGUAGE, localizer.getMessage("cbpSelectLanguage"),
                localizer.getMessage("nlSelectLanguage"),
                FLanguage.getAllLanguages()) {
            @Override
            public void valueChanged(String newValue) {
                // if the new locale needs to use CJK font, disallow change if UI_CJK_FONT is not set yet
                ForgePreferences prefs = FModel.getPreferences();
                if (prefs.getPref(FPref.UI_CJK_FONT).equals("") &&
                        (newValue.equals("zh-CN") || newValue.equals("ja-JP"))) {
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

                FOptionPane.showConfirmDialog(localizer.getMessage("lblRestartForgeDescription"), localizer.getMessage("lblRestartForge"), localizer.getMessage("lblRestart"), localizer.getMessage("lblLater"), new Callback<Boolean>() {
                    @Override
                    public void run(Boolean result) {
                        if (result) {
                            Forge.restart(true);
                        }
                    }
                });
            }
        }, 0);
        settingSkins = new CustomSelectSetting(FPref.UI_SKIN, localizer.getMessage("lblTheme"),
                localizer.getMessage("nlTheme"),
                FSkin.getAllSkins()) {
            @Override
            public void valueChanged(String newValue) {
                FSkin.changeSkin(newValue);
            }
        };
        lstSettings.addItem(settingSkins, 0);
        settingCJKFonts = new CustomSelectSetting(FPref.UI_CJK_FONT, localizer.getMessage("lblCJKFont"),
                localizer.getMessage("nlCJKFont"),
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
        lstSettings.addItem(new BooleanSetting(FPref.UI_LANDSCAPE_MODE,
                localizer.getMessage("lblLandscapeMode"),
                localizer.getMessage("nlLandscapeMode")) {
                    @Override
                    public void select() {
                        super.select();
                        boolean landscapeMode = FModel.getPreferences().getPrefBoolean(FPref.UI_LANDSCAPE_MODE);
                        Forge.getDeviceAdapter().setLandscapeMode(landscapeMode); //ensure device able to save off ini file so landscape change takes effect
                        if (Forge.isLandscapeMode() != landscapeMode) {
                            FOptionPane.showConfirmDialog(localizer.getMessage("lblRestartForgeDescription"), localizer.getMessage("lblRestartForge"), localizer.getMessage("lblRestart"), localizer.getMessage("lblLater"), new Callback<Boolean>() {
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
        lstSettings.addItem(new BooleanSetting(FPref.UI_ANDROID_MINIMIZE_ON_SCRLOCK,
                localizer.getMessage("lblMinimizeScreenLock"),
                localizer.getMessage("nlMinimizeScreenLock")),
                0);
        lstSettings.addItem(new BooleanSetting(FPref.USE_SENTRY,
                localizer.getMessage("lblAutomaticBugReports"),
                localizer.getMessage("nlAutomaticBugReports")),
                0);

        //Gameplay Options
        lstSettings.addItem(new CustomSelectSetting(FPref.MULLIGAN_RULE, localizer.getMessage("cbpMulliganRule"),
                 localizer.getMessage("nlpMulliganRule"),
                 MulliganDefs.getMulliganRuleNames()) {
                    @Override
                    public void valueChanged(String newValue) {
                        super.valueChanged(newValue);
                        StaticData.instance().setMulliganRule(MulliganDefs.GetRuleByName(FModel.getPreferences().getPref(FPref.MULLIGAN_RULE)));
                    }
        }, 1);
        lstSettings.addItem(new CustomSelectSetting(FPref.UI_CURRENT_AI_PROFILE,
                localizer.getMessage("cbpAiProfiles"),
                localizer.getMessage("nlpAiProfiles"),
                AiProfileUtil.getProfilesArray()),
                1);
        lstSettings.addItem(new BooleanSetting(FPref.UI_ANTE,
                localizer.getMessage("cbAnte"),
                localizer.getMessage("nlAnte")),
                1);
        lstSettings.addItem(new BooleanSetting(FPref.UI_ANTE_MATCH_RARITY,
                localizer.getMessage("cbAnteMatchRarity"),
                localizer.getMessage("nlAnteMatchRarity")),
                1);
        lstSettings.addItem(new BooleanSetting(FPref.MATCH_HOT_SEAT_MODE,
                localizer.getMessage("lblHotSeatMode"),
                localizer.getMessage("nlHotSeatMode")),
                1);
        lstSettings.addItem(new BooleanSetting(FPref.UI_ENABLE_AI_CHEATS,
                localizer.getMessage("cbEnableAICheats"),
                localizer.getMessage("nlEnableAICheats")),
                1);
        lstSettings.addItem(new BooleanSetting(FPref.UI_MANABURN,
                localizer.getMessage("cbManaBurn"),
                localizer.getMessage("nlManaBurn")),
                1);
        lstSettings.addItem(new BooleanSetting(FPref.UI_MANA_LOST_PROMPT,
                localizer.getMessage("cbManaLostPrompt"),
                localizer.getMessage("nlManaLostPrompt")),
                1);
        lstSettings.addItem(new BooleanSetting(FPref.ENFORCE_DECK_LEGALITY,
                localizer.getMessage("cbEnforceDeckLegality"),
                localizer.getMessage("nlEnforceDeckLegality")),
                1);
        lstSettings.addItem(new BooleanSetting(FPref.PERFORMANCE_MODE,
                localizer.getMessage("cbPerformanceMode"),
                localizer.getMessage("nlPerformanceMode")),
                1);
        lstSettings.addItem(new BooleanSetting(FPref.MATCH_SIDEBOARD_FOR_AI,
                localizer.getMessage("cbSideboardForAI"),
                localizer.getMessage("nlSideboardForAI")),
                1);
        lstSettings.addItem(new BooleanSetting(FPref.FILTERED_HANDS,
                localizer.getMessage("cbFilteredHands"),
                localizer.getMessage("nlFilteredHands")),
                1);
        lstSettings.addItem(new BooleanSetting(FPref.UI_CLONE_MODE_SOURCE,
                localizer.getMessage("cbCloneImgSource"),
                localizer.getMessage("nlCloneImgSource")),
                1);
        lstSettings.addItem(new BooleanSetting(FPref.MATCHPREF_PROMPT_FREE_BLOCKS,
                localizer.getMessage("cbPromptFreeBlocks"),
                localizer.getMessage("nlPromptFreeBlocks")),
                1);
        lstSettings.addItem(new BooleanSetting(FPref.UI_DETAILED_SPELLDESC_IN_PROMPT,
                localizer.getMessage("cbDetailedPaymentDesc"),
                localizer.getMessage("nlDetailedPaymentDesc")),
                1);
        lstSettings.addItem(new BooleanSetting(FPref.UI_GRAY_INACTIVE_TEXT,
                localizer.getMessage("cbGrayText"),
                localizer.getMessage("nlGrayText")),
                1);
        lstSettings.addItem(new BooleanSetting(FPref.UI_SHOW_STORM_COUNT_IN_PROMPT,
                localizer.getMessage("cbShowStormCount"),
                localizer.getMessage("nlShowStormCount")),
                1);
        lstSettings.addItem(new BooleanSetting(FPref.UI_PRESELECT_PREVIOUS_ABILITY_ORDER,
                localizer.getMessage("cbPreselectPrevAbOrder"),
                localizer.getMessage("nlPreselectPrevAbOrder")),
                1);
        lstSettings.addItem(new CustomSelectSetting(FPref.UI_ALLOW_ORDER_GRAVEYARD_WHEN_NEEDED,
                localizer.getMessage("lblOrderGraveyard"),
                localizer.getMessage("nlOrderGraveyard"),
                        new String[]{
                                ForgeConstants.GRAVEYARD_ORDERING_NEVER, ForgeConstants.GRAVEYARD_ORDERING_OWN_CARDS,
                                ForgeConstants.GRAVEYARD_ORDERING_ALWAYS}),
                1);
        lstSettings.addItem(new CustomSelectSetting(FPref.UI_AUTO_YIELD_MODE,
                localizer.getMessage("lblAutoYields"),
                localizer.getMessage("nlpAutoYieldMode"),
                new String[]{ForgeConstants.AUTO_YIELD_PER_ABILITY, ForgeConstants.AUTO_YIELD_PER_CARD}),
                1);
        lstSettings.addItem(new BooleanSetting(FPref.UI_ALLOW_ESC_TO_END_TURN,
                localizer.getMessage("cbEscapeEndsTurn"),
                localizer.getMessage("nlEscapeEndsTurn")),
                1);
        lstSettings.addItem(new BooleanSetting(FPref.UI_ALT_PLAYERINFOLAYOUT,
                localizer.getMessage("lblAltLifeDisplay"),
                localizer.getMessage("nlAltLifeDisplay")){
                @Override
                public void select() {
                    super.select();
                    //update
                    Forge.altPlayerLayout = FModel.getPreferences().getPrefBoolean(FPref.UI_ALT_PLAYERINFOLAYOUT);
                    if (MatchController.instance != null)
                        MatchController.instance.resetPlayerPanels();
                }
            },1);
        lstSettings.addItem(new BooleanSetting(FPref.UI_ALT_PLAYERZONETABS,
                localizer.getMessage("lblAltZoneTabs"),
                localizer.getMessage("nlAltZoneTabs")){
            @Override
            public void select() {
                super.select();
                //update
                Forge.altZoneTabs = FModel.getPreferences().getPrefBoolean(FPref.UI_ALT_PLAYERZONETABS);
                if (MatchController.instance != null)
                    MatchController.instance.resetPlayerPanels();
            }
        },1);
        lstSettings.addItem(new BooleanSetting(FPref.UI_ANIMATED_CARD_TAPUNTAP,
                localizer.getMessage("lblAnimatedCardTapUntap"),
                localizer.getMessage("nlAnimatedCardTapUntap")){
            @Override
            public void select() {
                super.select();
                //update
                Forge.animatedCardTapUntap = FModel.getPreferences().getPrefBoolean(FPref.UI_ANIMATED_CARD_TAPUNTAP);
            }
        },1);

        //Random Deck Generation
        lstSettings.addItem(new BooleanSetting(FPref.DECKGEN_NOSMALL,
                localizer.getMessage("cbRemoveSmall"),
                localizer.getMessage("nlRemoveSmall")),
                2);
        lstSettings.addItem(new BooleanSetting(FPref.DECKGEN_CARDBASED,
                localizer.getMessage("cbCardBased"),
                localizer.getMessage("nlCardBased")),
                2);
        lstSettings.addItem(new BooleanSetting(FPref.DECKGEN_SINGLETONS,
                localizer.getMessage("cbSingletons"),
                localizer.getMessage("nlSingletons")),
                2);
        lstSettings.addItem(new BooleanSetting(FPref.DECKGEN_ARTIFACTS,
                localizer.getMessage("cbRemoveArtifacts"),
                localizer.getMessage("nlRemoveArtifacts")),
                2);

        //Advanced Settings
        lstSettings.addItem(new BooleanSetting(FPref.DEV_MODE_ENABLED,
                localizer.getMessage("cbDevMode"),
                localizer.getMessage("nlDevMode")) {
                    @Override
                    public void select() {
                        super.select();
                        //update DEV_MODE flag when preference changes
                        ForgePreferences.DEV_MODE = FModel.getPreferences().getPrefBoolean(FPref.DEV_MODE_ENABLED);
                    }
                }, 3);
        lstSettings.addItem(new CustomSelectSetting(FPref.DEV_LOG_ENTRY_TYPE,
                localizer.getMessage("cbpGameLogEntryType"),
                localizer.getMessage("nlGameLogEntryType"),
                GameLogEntryType.class),
                3);
        lstSettings.addItem(new BooleanSetting(FPref.LOAD_CARD_SCRIPTS_LAZILY,
                localizer.getMessage("cbLoadCardsLazily"),
                localizer.getMessage("nlLoadCardsLazily")),
                3);
        lstSettings.addItem(new BooleanSetting(FPref.LOAD_HISTORIC_FORMATS,
                localizer.getMessage("cbLoadHistoricFormats"),
                localizer.getMessage("nlLoadHistoricFormats")),
                3);
        lstSettings.addItem(new BooleanSetting(FPref.UI_LOAD_UNKNOWN_CARDS,
                localizer.getMessage("lblEnableUnknownCards"),
                localizer.getMessage("nlEnableUnknownCards")) {
                    @Override
                    public void select() {
                        super.select();
                        FOptionPane.showConfirmDialog(
                            localizer.getMessage("lblRestartForgeDescription"),
                            localizer.getMessage("lblRestartForge"),
                            localizer.getMessage("lblRestart"),
                            localizer.getMessage("lblLater"), new Callback<Boolean>() {
                                @Override
                                public void run(Boolean result) {
                                if (result) {
                                    Forge.restart(true);
                                    }
                                }
                            }
                        );
                    }
                },
               3);
        lstSettings.addItem(new BooleanSetting(FPref.UI_LOAD_NONLEGAL_CARDS,
                localizer.getMessage("lblEnableNonLegalCards"),
                localizer.getMessage("nlEnableNonLegalCards")) {
                    @Override
                    public void select() {
                        super.select();
                        FOptionPane.showConfirmDialog(
                            localizer.getMessage("lblRestartForgeDescription"),
                            localizer.getMessage("lblRestartForge"),
                            localizer.getMessage("lblRestart"),
                            localizer.getMessage("lblLater"), new Callback<Boolean>() {
                                @Override
                                public void run(Boolean result) {
                                    if (result) {
                                        Forge.restart(true);
                                        }
                                    }
                                }
                            );
                        }
                    },
                3);
        lstSettings.addItem(new BooleanSetting(FPref.ALLOW_CUSTOM_CARDS_IN_DECKS_CONFORMANCE,
                                    localizer.getMessage("lblAllowCustomCardsInDecks"),
                                    localizer.getMessage("nlAllowCustomCardsInDecks")) {
                                @Override
                                public void select() {
                                    super.select();
                                    FOptionPane.showConfirmDialog(
                                            localizer.getMessage("lblRestartForgeDescription"),
                                            localizer.getMessage("lblRestartForge"),
                                            localizer.getMessage("lblRestart"),
                                            localizer.getMessage("lblLater"), new Callback<Boolean>() {
                                                @Override
                                                public void run(Boolean result) {
                                                    if (result) {
                                                        Forge.restart(true);
                                                    }
                                                }
                                            }
                                    );
                                }
                            },
                3);
        lstSettings.addItem(new BooleanSetting(FPref.UI_NETPLAY_COMPAT,
                localizer.getMessage("lblExperimentalNetworkCompatibility"),
                localizer.getMessage("nlExperimentalNetworkCompatibility")) {
                    @Override
                    public void select() {
                        super.select();
                            GuiBase.enablePropertyConfig(FModel.getPreferences().getPrefBoolean(FPref.UI_NETPLAY_COMPAT));
                    }
                },
               3);
        lstSettings.addItem(new BooleanSetting(FPref.UI_ENABLE_DISPOSE_TEXTURES,
                 localizer.getMessage("lblDisposeTextures"),
                 localizer.getMessage("nlDisposeTextures")) {
                     @Override
                     public void select() {
                         super.select();
                             Forge.disposeTextures = FModel.getPreferences().getPrefBoolean(FPref.UI_ENABLE_DISPOSE_TEXTURES);
                     }
                 },
                3);
        if (GuiBase.isAndroid()) { //this option does nothing except on Android
            lstSettings.addItem(new BooleanSetting(FPref.UI_AUTO_CACHE_SIZE,
                  localizer.getMessage("lblAutoCacheSize"),
                  localizer.getMessage("nlAutoCacheSize")) {
                      @Override
                      public void select() {
                          super.select();
                          FOptionPane.showConfirmDialog (
                              localizer.getMessage("lblRestartForgeDescription"),
                              localizer.getMessage("lblRestartForge"),
                              localizer.getMessage("lblRestart"),
                              localizer.getMessage("lblLater"), new Callback<Boolean>() {
                                  @Override
                                  public void run(Boolean result) {
                                      if (result) {
                                          Forge.restart(true);
                                      }
                                  }
                              }
                          );
                      }
                  },
                 3);
        }
        //Graphic Options
        lstSettings.addItem(new BooleanSetting(FPref.UI_DISABLE_CARD_IMAGES,
                localizer.getMessage("lblDisableCardImages"),
                localizer.getMessage("nlDisableCardImages")) {
                    @Override
                    public void select() {
                        super.select();
                        ImageCache.disposeTexture();
                    }
                },
                4);
        lstSettings.addItem(new BooleanSetting(FPref.UI_ENABLE_ONLINE_IMAGE_FETCHER,
                localizer.getMessage("cbImageFetcher"),
                localizer.getMessage("nlImageFetcher")),
                4);
        lstSettings.addItem(new CustomSelectSetting(FPref.UI_PREFERRED_ART,
                localizer.getMessage("lblPreferredArt"),
                localizer.getMessage("nlPreferredArt"),
                FModel.getMagicDb().getCardArtAvailablePreferences()) {
                    @Override
                    public void valueChanged(String newValue) {
                        super.valueChanged(newValue);
                        FOptionPane.showConfirmDialog (
                                localizer.getMessage("lblRestartForgeDescription"),
                                localizer.getMessage("lblRestartForge"),
                                localizer.getMessage("lblRestart"),
                                localizer.getMessage("lblLater"), new Callback<Boolean>() {
                                    @Override
                                    public void run(Boolean result) {
                                        if (result) {
                                            Forge.restart(true);
                                        }
                                    }
                                }
                        );
                    }
                },
                4);
        lstSettings.addItem(new BooleanSetting(FPref.UI_SMART_CARD_ART,
                        localizer.getMessage("lblSmartCardArtOpt"),
                        localizer.getMessage("nlSmartCardArtOpt") + "\n"
                                + localizer.getMessage("nlSmartCardArtOptNote")),
                4);
        lstSettings.addItem(new BooleanSetting(FPref.UI_OVERLAY_FOIL_EFFECT,
                localizer.getMessage("cbDisplayFoil"),
                localizer.getMessage("nlDisplayFoil")),
                4);
        lstSettings.addItem(new BooleanSetting(FPref.UI_RANDOM_FOIL,
                localizer.getMessage("cbRandomFoil"),
                localizer.getMessage("nlRandomFoil")),
                4);
        lstSettings.addItem(new BooleanSetting(FPref.UI_RANDOM_ART_IN_POOLS,
                localizer.getMessage("cbRandomArtInPools"),
                localizer.getMessage("nlRandomArtInPools")),
                4);
        lstSettings.addItem(new BooleanSetting(FPref.UI_COMPACT_TABS,
                localizer.getMessage("lblCompactTabs"),
                localizer.getMessage("nlCompactTabs")) {
                    @Override
                    public void select() {
                        super.select();
                        //update layout of screen when this setting changes
                        TabPageScreen.COMPACT_TABS = FModel.getPreferences().getPrefBoolean(FPref.UI_COMPACT_TABS);
                        parentScreen.revalidate();
                    }
                },4);
        lstSettings.addItem(new BooleanSetting(FPref.UI_COMPACT_LIST_ITEMS,
                localizer.getMessage("lblCompactListItems"),
                localizer.getMessage("nlCompactListItems")),
                4);
        lstSettings.addItem(new BooleanSetting(FPref.UI_HIDE_REMINDER_TEXT,
                localizer.getMessage("cbHideReminderText"),
                localizer.getMessage("nlHideReminderText")),
                4);
        lstSettings.addItem(new BooleanSetting(FPref.UI_MATCH_IMAGE_VISIBLE,
                localizer.getMessage("lblShowMatchBackground"),
                localizer.getMessage("nlShowMatchBackground")),
                4);
        lstSettings.addItem(new BooleanSetting(FPref.UI_LIBGDX_TEXTURE_FILTERING,
                localizer.getMessage("lblBattlefieldTextureFiltering"),
                localizer.getMessage("nlBattlefieldTextureFiltering")),
                4);
        lstSettings.addItem(new CustomSelectSetting(FPref.UI_DISPLAY_CURRENT_COLORS,
                localizer.getMessage("cbpDisplayCurrentCardColors"),
                localizer.getMessage("nlDisplayCurrentCardColors"),
                new String[]{
                    ForgeConstants.DISP_CURRENT_COLORS_NEVER, ForgeConstants.DISP_CURRENT_COLORS_MULTICOLOR,
                    ForgeConstants.DISP_CURRENT_COLORS_CHANGED, ForgeConstants.DISP_CURRENT_COLORS_MULTI_OR_CHANGED,
                    ForgeConstants.DISP_CURRENT_COLORS_ALWAYS}),
                4);
        lstSettings.addItem(new BooleanSetting(FPref.UI_ROTATE_SPLIT_CARDS,
                localizer.getMessage("lblRotateZoomSplit"),
                localizer.getMessage("nlRotateZoomSplit")),
                4);
        lstSettings.addItem(new BooleanSetting(FPref.UI_ROTATE_PLANE_OR_PHENOMENON,
                localizer.getMessage("lblRotateZoomPlanesPhenomena"),
                localizer.getMessage("nlRotateZoomPlanesPhenomena")),
                4);
        lstSettings.addItem(new BooleanSetting(FPref.UI_DYNAMIC_PLANECHASE_BG,
                localizer.getMessage("lblDynamicBackgroundPlanechase"),
                localizer.getMessage("nlDynamicBackgroundPlanechase")),
                4);
        lstSettings.addItem(new BooleanSetting(FPref.UI_DISABLE_IMAGES_EFFECT_CARDS,
                localizer.getMessage("lblDisableCardEffect"),
                localizer.getMessage("nlDisableCardEffect")),
                4);
        lstSettings.addItem(new CustomSelectSetting(FPref.UI_ENABLE_BORDER_MASKING,
                localizer.getMessage("lblBorderMaskOption"),
                localizer.getMessage("nlBorderMaskOption"),
                new String[]{"Off", "Crop", "Full", "Art"}) {
            @Override
            public void valueChanged(String newValue) {
                super.valueChanged(newValue);
                Forge.enableUIMask = FModel.getPreferences().getPref(FPref.UI_ENABLE_BORDER_MASKING);
            }
        }, 4);
        lstSettings.addItem(new BooleanSetting(FPref.UI_ENABLE_PRELOAD_EXTENDED_ART,
                localizer.getMessage("lblPreloadExtendedArtCards"),
                localizer.getMessage("nlPreloadExtendedArtCards")){
                @Override
                    public void select() {
                        super.select();
                        //update
                        Forge.enablePreloadExtendedArt = FModel.getPreferences().getPrefBoolean(FPref.UI_ENABLE_PRELOAD_EXTENDED_ART);
                    }
                },4);
        lstSettings.addItem(new BooleanSetting(FPref.UI_ENABLE_MATCH_SCROLL_INDICATOR,
                localizer.getMessage("lblMatchScrollIndicator"),
                localizer.getMessage("nlMatchScrollIndicator")),
                4);
        lstSettings.addItem(new BooleanSetting(FPref.UI_SHOW_FPS,
                localizer.getMessage("lblShowFPSDisplay"),
                localizer.getMessage("nlShowFPSDisplay")){
                @Override
                    public void select() {
                        super.select();
                        //update
                        Forge.showFPS = FModel.getPreferences().getPrefBoolean(FPref.UI_SHOW_FPS);
                    }
                },4);
        lstSettings.addItem(new CustomSelectSetting(FPref.UI_CARD_COUNTER_DISPLAY_TYPE,
                localizer.getMessage("cbpCounterDisplayType"),
                localizer.getMessage("nlCounterDisplayType"),
                new String[]{
                    ForgeConstants.CounterDisplayType.TEXT.getName(), ForgeConstants.CounterDisplayType.IMAGE.getName(),
                    ForgeConstants.CounterDisplayType.HYBRID.getName(), ForgeConstants.CounterDisplayType.OLD_WHEN_SMALL.getName()}),
                4);
        //Card Overlays
        lstSettings.addItem(new BooleanSetting(FPref.UI_SHOW_CARD_OVERLAYS,
                localizer.getMessage("lblShowCardOverlays"),
                localizer.getMessage("nlShowCardOverlays")),
                5);
        lstSettings.addItem(new BooleanSetting(FPref.UI_OVERLAY_CARD_NAME,
                localizer.getMessage("lblShowCardNameOverlays"),
                localizer.getMessage("nlShowCardNameOverlays")),
                5);
        lstSettings.addItem(new BooleanSetting(FPref.UI_OVERLAY_CARD_MANA_COST,
                localizer.getMessage("lblShowCardManaCostOverlays"),
                localizer.getMessage("nlShowCardManaCostOverlays")),
                5);
        lstSettings.addItem(new BooleanSetting(FPref.UI_OVERLAY_CARD_POWER,
                localizer.getMessage("lblShowCardPTOverlays"),
                localizer.getMessage("nlShowCardPTOverlays")),
                5);
        lstSettings.addItem(new BooleanSetting(FPref.UI_OVERLAY_CARD_ID,
                localizer.getMessage("lblShowCardIDOverlays"),
                localizer.getMessage("nlShowCardIDOverlays")),
                5);
        lstSettings.addItem(new BooleanSetting(FPref.UI_OVERLAY_ABILITY_ICONS,
                localizer.getMessage("lblShowAbilityIconsOverlays"),
                localizer.getMessage("nlShowAbilityIconsOverlays")),
                5);
        lstSettings.addItem(new BooleanSetting(FPref.UI_USE_LASER_ARROWS,
                        localizer.getMessage("lblUseLaserArrows"),
                        localizer.getMessage("nlUseLaserArrows")),
                5);
        //Vibration Options
        lstSettings.addItem(new BooleanSetting(FPref.UI_VIBRATE_ON_LIFE_LOSS,
                localizer.getMessage("lblVibrateWhenLosingLife"),
                localizer.getMessage("nlVibrateWhenLosingLife")),
                6);
        lstSettings.addItem(new BooleanSetting(FPref.UI_VIBRATE_ON_LONG_PRESS,
                localizer.getMessage("lblVibrateAfterLongPress"),
                localizer.getMessage("nlVibrateAfterLongPress")),
                6);
        //Sound Options
        lstSettings.addItem(new CustomSelectSetting(FPref.UI_CURRENT_SOUND_SET,
                        localizer.getMessage("cbpSoundSets"),
                        localizer.getMessage("nlpSoundSets"),
                        SoundSystem.instance.getAvailableSoundSets()) {
                            @Override
                            public void valueChanged(String newValue) {
                                super.valueChanged(newValue);
                                SoundSystem.instance.invalidateSoundCache();
                            }
                        },
                7);
        lstSettings.addItem(new CustomSelectSetting(FPref.UI_CURRENT_MUSIC_SET,
                                    localizer.getMessage("cbpMusicSets"),
                                    localizer.getMessage("nlpMusicSets"),
                                    SoundSystem.getAvailableMusicSets()) {
                                @Override
                                public void valueChanged(String newValue) {
                                    super.valueChanged(newValue);
                                    MusicPlaylist.invalidateMusicPlaylist();
                                    SoundSystem.instance.changeBackgroundTrack();
                                }
                            },
                7);
        lstSettings.addItem(new CustomSelectSetting(FPref.UI_VOL_SOUNDS,
                localizer.getMessage("cbAdjustSoundsVolume"),
                localizer.getMessage("nlAdjustSoundsVolume"),
                new String[]{"0", "10", "20", "30", "40", "50", "60", "70", "80", "90", "100"}),
                7);
        lstSettings.addItem(new CustomSelectSetting(FPref.UI_VOL_MUSIC,
                localizer.getMessage("cbAdjustMusicVolume"),
                localizer.getMessage("nlAdjustMusicVolume"),
                new String[]{"0", "10", "20", "30", "40", "50", "60", "70", "80", "90", "100"}) {
                    @Override
                        public void valueChanged(String newValue) {
                            super.valueChanged(newValue);
                            //update background music when this setting changes
                            SoundSystem.instance.changeBackgroundTrack();
                        }
                }, 7);
        /*lstSettings.addItem(new BooleanSetting(FPref.UI_ALT_SOUND_SYSTEM,
                "Use Alternate Sound System",
                "Use the alternate sound system (only use if you have issues with sound not playing or disappearing)."),
                7);*/
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
        private final List<String> options = new ArrayList<>();

        public CustomSelectSetting(FPref pref0, String label0, String description0, String[] options0) {
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
            FModel.getPreferences().setPref(pref, newValue);
            FModel.getPreferences().save();
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
            private final String currentValue = FModel.getPreferences().getPref(pref);

            private CustomSelectScreen() {
                super("Select " + label.substring(0, label.length() - 1));
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
            g.drawText(FModel.getPreferences().getPref(pref), font, color, x, y, w, h, false, Align.right, false);
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
