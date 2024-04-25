package forge.screens.home.settings;

import forge.MulliganDefs;
import forge.Singletons;
import forge.StaticData;
import forge.ai.AiProfileUtil;
import forge.control.FControl.CloseAction;
import forge.download.AutoUpdater;
import forge.game.GameLogEntryType;
import forge.gamemodes.net.server.FServerManager;
import forge.gui.GuiBase;
import forge.gui.UiCommand;
import forge.gui.framework.FScreen;
import forge.gui.framework.ICDoc;
import forge.localinstance.properties.ForgeConstants;
import forge.localinstance.properties.ForgePreferences;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.player.GamePlayerUtil;
import forge.screens.deckeditor.CDeckEditorUI;
import forge.screens.deckeditor.controllers.CEditorTokenViewer;
import forge.sound.MusicPlaylist;
import forge.sound.SoundSystem;
import forge.toolbox.FComboBox;
import forge.toolbox.FComboBoxPanel;
import forge.toolbox.FLabel;
import forge.toolbox.FOptionPane;
import forge.util.Localizer;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Controls the preferences submenu in the home UI.
 *
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 *
 */
public enum CSubmenuPreferences implements ICDoc {
    /** */
    SINGLETON_INSTANCE;
    final Localizer localizer = Localizer.getInstance();


    private VSubmenuPreferences view;
    private ForgePreferences prefs;
    private boolean updating;

    private final List<Pair<JCheckBox, FPref>> lstControls = new ArrayList<>();

    @Override
    public void register() {
    }

    /* (non-Javadoc)
     * @see forge.gui.control.home.IControlSubmenu#update()
     */
    @SuppressWarnings("serial")
    @Override
    public void initialize() {

        this.view = VSubmenuPreferences.SINGLETON_INSTANCE;
        this.prefs = FModel.getPreferences();

        // This updates variable right now and is not standard
        view.getCbDevMode().addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(final ItemEvent arg0) {
                if (updating) { return; }
                // prevent changing DEV_MODE while network game running
                if (FServerManager.getInstance().isMatchActive()) {
                    System.out.println(localizer.getMessage("CantChangeDevModeWhileNetworkMath"));
                    return;
                }

                final boolean toggle = view.getCbDevMode().isSelected();
                prefs.setPref(FPref.DEV_MODE_ENABLED, String.valueOf(toggle));
                ForgePreferences.DEV_MODE = toggle;
                prefs.save();
            }
        });

        // This updates background track immediately and is not standard
        view.getCbEnableMusic().addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(final ItemEvent arg0) {
                if (updating) { return; }

                final boolean toggle = view.getCbEnableMusic().isSelected();
                prefs.setPref(FPref.UI_ENABLE_MUSIC, String.valueOf(toggle));
                prefs.save();
                SoundSystem.instance.changeBackgroundTrack();
            }
        });

        // This updates Experimental Network Option
        view.getCbUseExperimentalNetworkStream().addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(final ItemEvent arg0) {
                if (updating) { return; }

                final boolean toggle = view.getCbUseExperimentalNetworkStream().isSelected();
                GuiBase.enablePropertyConfig(toggle);
                prefs.setPref(FPref.UI_NETPLAY_COMPAT, String.valueOf(toggle));
                prefs.save();
            }
        });

        lstControls.clear(); // just in case
        lstControls.add(Pair.of(view.getCbAnte(), FPref.UI_ANTE));
        lstControls.add(Pair.of(view.getCbAnteMatchRarity(), FPref.UI_ANTE_MATCH_RARITY));
        lstControls.add(Pair.of(view.getCbManaBurn(), FPref.UI_MANABURN));
        lstControls.add(Pair.of(view.getCbScaleLarger(), FPref.UI_SCALE_LARGER));
        lstControls.add(Pair.of(view.getCbRenderBlackCardBorders(), FPref.UI_RENDER_BLACK_BORDERS));
        lstControls.add(Pair.of(view.getCbLargeCardViewers(), FPref.UI_LARGE_CARD_VIEWERS));
        lstControls.add(Pair.of(view.getCbSmallDeckViewer(), FPref.UI_SMALL_DECK_VIEWER));
        lstControls.add(Pair.of(view.getCbRandomArtInPools(), FPref.UI_RANDOM_ART_IN_POOLS));
        lstControls.add(Pair.of(view.getCbEnforceDeckLegality(), FPref.ENFORCE_DECK_LEGALITY));
        lstControls.add(Pair.of(view.getCbPerformanceMode(), FPref.PERFORMANCE_MODE));
        lstControls.add(Pair.of(view.getCbFilteredHands(), FPref.FILTERED_HANDS));
        lstControls.add(Pair.of(view.getCbCloneImgSource(), FPref.UI_CLONE_MODE_SOURCE));
        lstControls.add(Pair.of(view.getCbRemoveSmall(), FPref.DECKGEN_NOSMALL));
        lstControls.add(Pair.of(view.getCbCardBased(), FPref.DECKGEN_CARDBASED));
        lstControls.add(Pair.of(view.getCbRemoveArtifacts(), FPref.DECKGEN_ARTIFACTS));
        lstControls.add(Pair.of(view.getCbSingletons(), FPref.DECKGEN_SINGLETONS));
        lstControls.add(Pair.of(view.getCbEnableAICheats(), FPref.UI_ENABLE_AI_CHEATS));
        lstControls.add(Pair.of(view.getCbEnableUnknownCards(), FPref.UI_LOAD_UNKNOWN_CARDS));
        lstControls.add(Pair.of(view.getCbEnableNonLegalCards(), FPref.UI_LOAD_NONLEGAL_CARDS));
        lstControls.add(Pair.of(view.getCbAllowCustomCardsDeckConformance(), FPref.ALLOW_CUSTOM_CARDS_IN_DECKS_CONFORMANCE));
        lstControls.add(Pair.of(view.getCbUseExperimentalNetworkStream(), FPref.UI_NETPLAY_COMPAT));
        lstControls.add(Pair.of(view.getCbImageFetcher(), FPref.UI_ENABLE_ONLINE_IMAGE_FETCHER));
        lstControls.add(Pair.of(view.getCbDisableCardImages(), FPref.UI_DISABLE_CARD_IMAGES));
        lstControls.add(Pair.of(view.getCbDisplayFoil(), FPref.UI_OVERLAY_FOIL_EFFECT));
        lstControls.add(Pair.of(view.getCbRandomFoil(), FPref.UI_RANDOM_FOIL));
        lstControls.add(Pair.of(view.getCbEnableSounds(), FPref.UI_ENABLE_SOUNDS));
        lstControls.add(Pair.of(view.getCbAltSoundSystem(), FPref.UI_ALT_SOUND_SYSTEM));
        lstControls.add(Pair.of(view.getCbSROptimize(), FPref.UI_SR_OPTIMIZE));
        lstControls.add(Pair.of(view.getCbUiForTouchScreen(), FPref.UI_FOR_TOUCHSCREN));
        lstControls.add(Pair.of(view.getCbTimedTargOverlay(), FPref.UI_TIMED_TARGETING_OVERLAY_UPDATES));
        lstControls.add(Pair.of(view.getCbCompactMainMenu(), FPref.UI_COMPACT_MAIN_MENU));
        lstControls.add(Pair.of(view.getCbUseSentry(), FPref.USE_SENTRY));
        lstControls.add(Pair.of(view.getCbPromptFreeBlocks(), FPref.MATCHPREF_PROMPT_FREE_BLOCKS));
        lstControls.add(Pair.of(view.getCbPauseWhileMinimized(), FPref.UI_PAUSE_WHILE_MINIMIZED));
        lstControls.add(Pair.of(view.getCbWorkshopSyntax(), FPref.DEV_WORKSHOP_SYNTAX));

        lstControls.add(Pair.of(view.getCbCompactPrompt(), FPref.UI_COMPACT_PROMPT));
        lstControls.add(Pair.of(view.getCbHideReminderText(), FPref.UI_HIDE_REMINDER_TEXT));
        lstControls.add(Pair.of(view.getCbCardTextUseSansSerif(), FPref.UI_CARD_IMAGE_RENDER_USE_SANS_SERIF_FONT));
        lstControls.add(Pair.of(view.getCbCardTextHideReminder(), FPref.UI_CARD_IMAGE_RENDER_HIDE_REMINDER_TEXT));
        lstControls.add(Pair.of(view.getCbOpenPacksIndiv(), FPref.UI_OPEN_PACKS_INDIV));
        lstControls.add(Pair.of(view.getCbTokensInSeparateRow(), FPref.UI_TOKENS_IN_SEPARATE_ROW));
        lstControls.add(Pair.of(view.getCbStackCreatures(), FPref.UI_STACK_CREATURES));
        lstControls.add(Pair.of(view.getCbManaLostPrompt(), FPref.UI_MANA_LOST_PROMPT));
        lstControls.add(Pair.of(view.getCbEscapeEndsTurn(), FPref.UI_ALLOW_ESC_TO_END_TURN));
        lstControls.add(Pair.of(view.getCbDetailedPaymentDesc(), FPref.UI_DETAILED_SPELLDESC_IN_PROMPT));
        lstControls.add(Pair.of(view.getCbGrayText(), FPref.UI_GRAY_INACTIVE_TEXT));
        lstControls.add(Pair.of(view.getCbPreselectPrevAbOrder(), FPref.UI_PRESELECT_PREVIOUS_ABILITY_ORDER));
        lstControls.add(Pair.of(view.getCbShowStormCount(), FPref.UI_SHOW_STORM_COUNT_IN_PROMPT));
        lstControls.add(Pair.of(view.getCbRemindOnPriority(), FPref.UI_REMIND_ON_PRIORITY));

        lstControls.add(Pair.of(view.getCbFilterLandsByColorId(), FPref.UI_FILTER_LANDS_BY_COLOR_IDENTITY));
        lstControls.add(Pair.of(view.getCbLoadCardsLazily(), FPref.LOAD_CARD_SCRIPTS_LAZILY));
        lstControls.add(Pair.of(view.getCbLoadArchivedFormats(), FPref.LOAD_ARCHIVED_FORMATS));
        lstControls.add(Pair.of(view.getCbSmartCardArtSelectionOpt(), FPref.UI_SMART_CARD_ART));
        lstControls.add(Pair.of(view.getCbShowDraftRanking(), FPref.UI_OVERLAY_DRAFT_RANKING));


        for(final Pair<JCheckBox, FPref> kv : lstControls) {
          kv.getKey().addItemListener(new ItemListener() {
                @Override
                public void itemStateChanged(final ItemEvent arg0) {
                    if (updating) { return; }

                    prefs.setPref(kv.getValue(), String.valueOf(kv.getKey().isSelected()));
                    prefs.save();
                }
            });
        }

        view.getCbSmartCardArtSelectionOpt().addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(final ItemEvent e) {
                if (updating) { return; }
                boolean isEnabled = e.getStateChange() == ItemEvent.SELECTED;
                FModel.getMagicDb().setEnableSmartCardArtSelection(isEnabled);
            }
        });

        view.getBtnReset().setCommand(new UiCommand() {
            @Override
            public void run() {
                CSubmenuPreferences.this.resetForgeSettingsToDefault();
            }
        });

        view.getBtnDeleteEditorUI().setCommand(new UiCommand() {
            @Override
            public void run() {
                CSubmenuPreferences.this.resetDeckEditorLayout();
            }
        });

        view.getBtnDeleteWorkshopUI().setCommand(new UiCommand() {
            @Override
            public void run() {
                CSubmenuPreferences.this.resetWorkshopLayout();
            }
        });

        view.getBtnDeleteMatchUI().setCommand(new UiCommand() {
            @Override
            public void run() {
                CSubmenuPreferences.this.resetMatchScreenLayout();
            }
        });

        view.getBtnUserProfileUI().setCommand(new UiCommand() {
            @Override
            public void run() {
                CSubmenuPreferences.this.openUserProfileDirectory();
            }
        });

        view.getBtnClearImageCache().setCommand(new UiCommand() {
            @Override
            public void run() {
                CSubmenuPreferences.this.clearImageCache();
            }
        });

        view.getBtnTokenPreviewer().setCommand(new UiCommand() {
            @Override
            public void run() {
                CSubmenuPreferences.this.openTokenPreviewer();
            }
        });

        view.getBtnResetJavaFutureCompatibilityWarnings().setCommand(new UiCommand() {
            @Override
            public void run() {
                prefs.setPref(FPref.DISABLE_DISPLAY_JAVA_8_UPDATE_WARNING, false);
                prefs.save();
                FOptionPane.showMessageDialog(localizer.getMessage("CompatibilityWarningsReEnabled"));
            }
        });

        view.getBtnContentDirectoryUI().setCommand(new UiCommand() {
            @Override
            public void run() {
                CSubmenuPreferences.this.openContentDirectory();
            }
        });

        initializeGameLogVerbosityComboBox();
        initializeCloseActionComboBox();
        initializeDefaultFontSizeComboBox();
        initializeCardArtFormatComboBox();
        initializeCardArtPreference();
        initializeAutoUpdaterComboBox();
        initializeMulliganRuleComboBox();
        initializeAiProfilesComboBox();
        initializeAiSideboardingModeComboBox();
        initializeSoundSetsComboBox();
        initializeMusicSetsComboBox();
        initializeStackAdditionsComboBox();
        initializeLandPlayedComboBox();
        initializeColorIdentityCombobox();
        initializeSwitchStatesCombobox();
        initializeAutoYieldModeComboBox();
        initializeCounterDisplayTypeComboBox();
        initializeCounterDisplayLocationComboBox();
        initializeGraveyardOrderingComboBox();
        initializePlayerNameButton();
        initializeDefaultLanguageComboBox();

        disableLazyLoading();
    }

    /* (non-Javadoc)
     * @see forge.gui.control.home.IControlSubmenu#update()
     */
    @Override
    public void update() {
        updating = true; //prevent itemStateChanged causing prefs to be saved or other logic occurring while updating values

        this.view = VSubmenuPreferences.SINGLETON_INSTANCE;
        this.prefs = FModel.getPreferences();

        setPlayerNameButtonText();
        view.getCbDevMode().setSelected(ForgePreferences.DEV_MODE);
        view.getCbEnableMusic().setSelected(prefs.getPrefBoolean(FPref.UI_ENABLE_MUSIC));
        view.getCbUseExperimentalNetworkStream().setSelected(prefs.getPrefBoolean(FPref.UI_NETPLAY_COMPAT));

        for(final Pair<JCheckBox, FPref> kv: lstControls) {
            kv.getKey().setSelected(prefs.getPrefBoolean(kv.getValue()));
        }
        view.reloadShortcuts();

        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() { view.getCbRemoveSmall().requestFocusInWindow(); }
        });

        updating = false;
    }

    private void resetForgeSettingsToDefault() {
        final String userPrompt =localizer.getMessage("AresetForgeSettingsToDefault");
        if (FOptionPane.showConfirmDialog(userPrompt, localizer.getMessage("TresetForgeSettingsToDefault"))) {
            final ForgePreferences prefs = FModel.getPreferences();
            prefs.reset();
            prefs.save();
            update();
            Singletons.getControl().restartForge();
        }
    }

    private void resetDeckEditorLayout() {
        final String userPrompt =localizer.getMessage("AresetDeckEditorLayout");
        if (FOptionPane.showConfirmDialog(userPrompt, localizer.getMessage("TresetDeckEditorLayout"))) {
            if (FScreen.DECK_EDITOR_CONSTRUCTED.deleteLayoutFile()) {
                FOptionPane.showMessageDialog(localizer.getMessage("OKresetDeckEditorLayout"));
            }
        }
    }

    private void resetWorkshopLayout() {
        final String userPrompt =localizer.getMessage("AresetWorkshopLayout");
        if (FOptionPane.showConfirmDialog(userPrompt, localizer.getMessage("TresetWorkshopLayout"))) {
            if (FScreen.WORKSHOP_SCREEN.deleteLayoutFile()) {
                FOptionPane.showMessageDialog(localizer.getMessage("OKresetWorkshopLayout"));
            }
        }
    }

    private void resetMatchScreenLayout() {
        final String userPrompt =localizer.getMessage("AresetMatchScreenLayout");
        if (FOptionPane.showConfirmDialog(userPrompt, localizer.getMessage("TresetMatchScreenLayout"))) {
            if (FScreen.deleteMatchLayoutFile()) {
                FOptionPane.showMessageDialog(localizer.getMessage("OKresetMatchScreenLayout"));
            }
        }
    }

    private void openUserProfileDirectory() {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(new File(ForgeConstants.USER_DIR));
            }
        } catch(final Exception e) {
            System.out.println("Unable to open Directory: " + e.toString());
        }
    }

    private void clearImageCache() {
        try {
            GuiBase.getInterface().clearImageCache();
        } catch(final Exception e) {
            System.out.println("Unable to clear cache: " + e.toString());
        }
    }

    private void openTokenPreviewer() {
        Singletons.getControl().setCurrentScreen(FScreen.TOKEN_VIEWER);
        CDeckEditorUI.SINGLETON_INSTANCE.setEditorController(
                new CEditorTokenViewer(CDeckEditorUI.SINGLETON_INSTANCE.getCDetailPicture()));
    }

    private void openContentDirectory() {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(new File(ForgeConstants.CACHE_DIR));
            }
        } catch(final Exception e) {
            System.out.println("Unable to open Directory: " + e.toString());
        }
    }

    private void initializeGameLogVerbosityComboBox() {
        final FPref userSetting = FPref.DEV_LOG_ENTRY_TYPE;
        final FComboBoxPanel<GameLogEntryType> panel = this.view.getGameLogVerbosityComboBoxPanel();
        final FComboBox<GameLogEntryType> comboBox = createComboBox(GameLogEntryType.values(), userSetting);
        final GameLogEntryType selectedItem = GameLogEntryType.valueOf(this.prefs.getPref(userSetting));
        panel.setComboBox(comboBox, selectedItem);
    }

    private void initializeCloseActionComboBox() {
        final FComboBoxPanel<CloseAction> panel = this.view.getCloseActionComboBoxPanel();
        final FComboBox<CloseAction> comboBox = new FComboBox<>(CloseAction.values());
        comboBox.addItemListener(new ItemListener() {
            @Override public void itemStateChanged(final ItemEvent e) {
                Singletons.getControl().setCloseAction(comboBox.getSelectedItem());
            }
        });
        panel.setComboBox(comboBox, Singletons.getControl().getCloseAction());
    }

    private void initializeDefaultLanguageComboBox() {
    	final File lang_root = new File(ForgeConstants.LANG_DIR);
    	final File[] files = lang_root.listFiles();
    	final List<String> allLanguages = new ArrayList<>();
    	for ( File file : files ) {
    		if ( !file.isFile() ) {
    			continue;
    		}
    		String languageName = file.getName();
    		if (!languageName.endsWith(".properties")) {
    			continue;
    		}
    		allLanguages.add(languageName.replace(".properties", ""));
    	}
        final String [] choices = new String[ allLanguages.size() ];
        allLanguages.toArray( choices );
        final FPref userSetting = FPref.UI_LANGUAGE;
        final FComboBoxPanel<String> panel = this.view.getCbpDefaultLanguageComboBoxPanel();
        final FComboBox<String> comboBox = createComboBox(choices, userSetting);
        final String selectedItem = this.prefs.getPref(userSetting);
        panel.setComboBox(comboBox, selectedItem);
    }

    private void initializeAutoUpdaterComboBox() {
        // TODO: Ideally we would filter out update paths based on the type of Forge people have
        final String[] updatePaths = AutoUpdater.updateChannels;
        final FPref updatePreference = FPref.AUTO_UPDATE;
        final FComboBoxPanel<String> panel = this.view.getCbpAutoUpdater();
        final FComboBox<String> comboBox = createComboBox(updatePaths, updatePreference);
        final String selectedItem = this.prefs.getPref(updatePreference);
        panel.setComboBox(comboBox, selectedItem);
    }

    private void initializeMulliganRuleComboBox() {
        final String [] choices = MulliganDefs.getMulliganRuleNames();
        final FPref userSetting = FPref.MULLIGAN_RULE;
        final FComboBoxPanel<String> panel = this.view.getCbpMulliganRule();
        final FComboBox<String> comboBox = createComboBox(choices, userSetting);
        final String selectedItem = this.prefs.getPref(userSetting);
        comboBox.addItemListener(new ItemListener() {
            @Override public void itemStateChanged(final ItemEvent e) {
                StaticData.instance().setMulliganRule(MulliganDefs.GetRuleByName(prefs.getPref(FPref.MULLIGAN_RULE)));
            }
        });
        panel.setComboBox(comboBox, selectedItem);
    }

    private void initializeDefaultFontSizeComboBox() {
        final String [] choices = {"10", "11", "12", "13", "14", "15", "16", "17", "18"};
        final FPref userSetting = FPref.UI_DEFAULT_FONT_SIZE;
        final FComboBoxPanel<String> panel = this.view.getCbpDefaultFontSizeComboBoxPanel();
        final FComboBox<String> comboBox = createComboBox(choices, userSetting);
        final String selectedItem = this.prefs.getPref(userSetting);
        panel.setComboBox(comboBox, selectedItem);
    }

    private void initializeCardArtFormatComboBox() {
        final String [] choices = {"Full", "Crop"};
        final FPref userSetting = FPref.UI_CARD_ART_FORMAT;
        final FComboBoxPanel<String> panel = this.view.getCbpCardArtFormatComboBoxPanel();
        final FComboBox<String> comboBox = createComboBox(choices, userSetting);
        final String selectedItem = this.prefs.getPref(userSetting);
        panel.setComboBox(comboBox, selectedItem);
    }

    private void initializeAiProfilesComboBox() {
        final FPref userSetting = FPref.UI_CURRENT_AI_PROFILE;
        final FComboBoxPanel<String> panel = this.view.getAiProfilesComboBoxPanel();
        final FComboBox<String> comboBox = createComboBox(AiProfileUtil.getProfilesArray(), userSetting);
        final String selectedItem = this.prefs.getPref(userSetting);
        panel.setComboBox(comboBox, selectedItem);
    }

    private void initializeAiSideboardingModeComboBox() {
        final FPref userSetting = FPref.MATCH_AI_SIDEBOARDING_MODE;
        final FComboBoxPanel<String> panel = this.view.getAiSideboardingModeComboBoxPanel();
        final FComboBox<String> comboBox = createComboBox(new String[] {"Off", "AI", "Human For AI"}, userSetting);
        final String selectedItem = this.prefs.getPref(userSetting);
        panel.setComboBox(comboBox, selectedItem);
        comboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                AiProfileUtil.setAiSideboardingMode(AiProfileUtil.AISideboardingMode.normalizedValueOf(comboBox.getSelectedItem()));
                System.out.println(AiProfileUtil.getAISideboardingMode());
            }
        });
    }

    private void initializeSoundSetsComboBox() {
        final FPref userSetting = FPref.UI_CURRENT_SOUND_SET;
        final FComboBoxPanel<String> panel = this.view.getSoundSetsComboBoxPanel();
        final FComboBox<String> comboBox = createComboBox(SoundSystem.instance.getAvailableSoundSets(), userSetting);
        final String selectedItem = this.prefs.getPref(userSetting);
        panel.setComboBox(comboBox, selectedItem);
        comboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                SoundSystem.instance.invalidateSoundCache();
            }
        });
    }

    private void initializeMusicSetsComboBox() {
        final FPref userSetting = FPref.UI_CURRENT_MUSIC_SET;
        final FComboBoxPanel<String> panel = this.view.getMusicSetsComboBoxPanel();
        final FComboBox<String> comboBox = createComboBox(SoundSystem.instance.getAvailableMusicSets(), userSetting);
        final String selectedItem = this.prefs.getPref(userSetting);
        panel.setComboBox(comboBox, selectedItem);
        comboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                MusicPlaylist.invalidateMusicPlaylist();
                SoundSystem.instance.changeBackgroundTrack();
            }
        });
    }

    private void initializeCardArtPreference() {
        final String latestOpt = Localizer.getInstance().getMessage("latestArtOpt");
        final String originalOpt = Localizer.getInstance().getMessage("originalArtOpt");
        final String [] choices = {latestOpt, originalOpt};
        final FPref uiPreferredArt = FPref.UI_PREFERRED_ART;

        final FComboBoxPanel<String> panel = this.view.getCbpCardArtPreference();
        final FComboBox<String> comboBox = new FComboBox<>(choices);
        comboBox.addItemListener(new ItemListener() {
            @Override public void itemStateChanged(final ItemEvent e) {
                String artPreference = comboBox.getSelectedItem();
                if (artPreference == null)
                    artPreference = latestOpt;  // default, just in case
                boolean latestArt = artPreference.equalsIgnoreCase(latestOpt);
                boolean coreExpFilter = FModel.getMagicDb().isCoreExpansionOnlyFilterSet();
                FModel.getMagicDb().setCardArtPreference(latestArt, coreExpFilter);
                String preferenceOpt = FModel.getMagicDb().getCardArtPreferenceName();
                CSubmenuPreferences.this.prefs.setPref(uiPreferredArt, preferenceOpt);
                CSubmenuPreferences.this.prefs.save();
            }
        });
        final String selectedItem = FModel.getMagicDb().cardArtPreferenceIsLatest() ? latestOpt : originalOpt;
        panel.setComboBox(comboBox, selectedItem);

        final JCheckBox coreExpFilter = this.view.getCbCardArtCoreExpansionsOnlyOpt();
        boolean selected = FModel.getMagicDb().isCoreExpansionOnlyFilterSet();
        coreExpFilter.setSelected(selected);
        coreExpFilter.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                boolean latestArt = FModel.getMagicDb().cardArtPreferenceIsLatest();
                boolean coreExpFilter = e.getStateChange() == ItemEvent.SELECTED;
                FModel.getMagicDb().setCardArtPreference(latestArt, coreExpFilter);
                String preferenceOpt = FModel.getMagicDb().getCardArtPreferenceName();
                CSubmenuPreferences.this.prefs.setPref(uiPreferredArt, preferenceOpt);
                CSubmenuPreferences.this.prefs.save();
            }
        });
    }

    private void initializeStackAdditionsComboBox() {
        final String[] elems = {ForgeConstants.STACK_EFFECT_NOTIFICATION_NEVER, ForgeConstants.STACK_EFFECT_NOTIFICATION_ALWAYS,
                ForgeConstants.STACK_EFFECT_NOTIFICATION_AI_AND_TRIGGERED};
        final FPref userSetting = FPref.UI_STACK_EFFECT_NOTIFICATION_POLICY;
        final FComboBoxPanel<String> panel = this.view.getCbpStackAdditionsComboBoxPanel();
        final FComboBox<String> comboBox = createComboBox(elems, userSetting);
        final String selectedItem = this.prefs.getPref(userSetting);
        panel.setComboBox(comboBox, selectedItem);
    }

    private void initializeLandPlayedComboBox() {
        final String[] elems = {ForgeConstants.LAND_PLAYED_NOTIFICATION_NEVER, ForgeConstants.LAND_PLAYED_NOTIFICATION_ALWAYS,
                ForgeConstants.LAND_PLAYED_NOTIFICATION_ALWAYS_FOR_NONBASIC_LANDS, ForgeConstants.LAND_PLAYED_NOTIFICATION_AI,
                ForgeConstants.LAND_PLAYED_NOTIFICATION_AI_FOR_NONBASIC_LANDS};
        final FPref userSetting = FPref.UI_LAND_PLAYED_NOTIFICATION_POLICY;
        final FComboBoxPanel<String> panel = this.view.getCbpLandPlayedComboBoxPanel();
        final FComboBox<String> comboBox = createComboBox(elems, userSetting);
        final String selectedItem = this.prefs.getPref(userSetting);
        panel.setComboBox(comboBox, selectedItem);
    }

    private void initializeColorIdentityCombobox() {
        final String[] elems = {ForgeConstants.DISP_CURRENT_COLORS_NEVER, ForgeConstants.DISP_CURRENT_COLORS_CHANGED,
            ForgeConstants.DISP_CURRENT_COLORS_MULTICOLOR, ForgeConstants.DISP_CURRENT_COLORS_MULTI_OR_CHANGED,
            ForgeConstants.DISP_CURRENT_COLORS_ALWAYS};
        final FPref userSetting = FPref.UI_DISPLAY_CURRENT_COLORS;
        final FComboBoxPanel<String> panel = this.view.getDisplayColorIdentity();
        final FComboBox<String> comboBox = createComboBox(elems, userSetting);
        final String selectedItem = this.prefs.getPref(userSetting);
        panel.setComboBox(comboBox, selectedItem);
    }

    private void initializeSwitchStatesCombobox() {
        final String[] elems = {ForgeConstants.SWITCH_CARDSTATES_DECK_NEVER, ForgeConstants.SWITCH_CARDSTATES_DECK_HOVER, ForgeConstants.SWITCH_CARDSTATES_DECK_ALWAYS};
        final FPref userSetting = FPref.UI_SWITCH_STATES_DECKVIEW;
        final FComboBoxPanel<String> panel = this.view.getSwitchStates();
        final FComboBox<String> comboBox = createComboBox(elems, userSetting);
        final String selectedItem = this.prefs.getPref(userSetting);
        panel.setComboBox(comboBox, selectedItem);
    }

    private void initializeAutoYieldModeComboBox() {
        final String[] elems = {ForgeConstants.AUTO_YIELD_PER_ABILITY, ForgeConstants.AUTO_YIELD_PER_CARD};
        final FPref userSetting = FPref.UI_AUTO_YIELD_MODE;
        final FComboBoxPanel<String> panel = this.view.getAutoYieldModeComboBoxPanel();
        final FComboBox<String> comboBox = createComboBox(elems, userSetting);
        final String selectedItem = this.prefs.getPref(userSetting);
        panel.setComboBox(comboBox, selectedItem);
    }

    private void initializeGraveyardOrderingComboBox() {
        final String[] elems = {ForgeConstants.GRAVEYARD_ORDERING_NEVER, ForgeConstants.GRAVEYARD_ORDERING_OWN_CARDS,
                ForgeConstants.GRAVEYARD_ORDERING_ALWAYS};
        final FPref userSetting = FPref.UI_ALLOW_ORDER_GRAVEYARD_WHEN_NEEDED;
        final FComboBoxPanel<String> panel = this.view.getCbpGraveyardOrdering();
        final FComboBox<String> comboBox = createComboBox(elems, userSetting);
        final String selectedItem = this.prefs.getPref(userSetting);
        panel.setComboBox(comboBox, selectedItem);
    }

    private void initializeCounterDisplayTypeComboBox() {

        final String[] elements = new String[ForgeConstants.CounterDisplayType.values().length];

        ForgeConstants.CounterDisplayType[] values = ForgeConstants.CounterDisplayType.values();
        for (int i = 0; i < values.length; i++) {
            elements[i] = values[i].getName();
        }

        final FPref userSetting = FPref.UI_CARD_COUNTER_DISPLAY_TYPE;
        final FComboBoxPanel<String> panel = this.view.getCounterDisplayTypeComboBoxPanel();

        final FComboBox<String> comboBox = createComboBox(elements, userSetting);
        final String selectedItem = this.prefs.getPref(userSetting);

        panel.setComboBox(comboBox, selectedItem);

    }

    private void initializeCounterDisplayLocationComboBox() {

        final String[] elements = new String[ForgeConstants.CounterDisplayLocation.values().length];

        ForgeConstants.CounterDisplayLocation[] values = ForgeConstants.CounterDisplayLocation.values();
        for (int i = 0; i < values.length; i++) {
            elements[i] = values[i].getName();
        }

        final FPref userSetting = FPref.UI_CARD_COUNTER_DISPLAY_LOCATION;
        final FComboBoxPanel<String> panel = this.view.getCounterDisplayLocationComboBoxPanel();

        final FComboBox<String> comboBox = createComboBox(elements, userSetting);
        final String selectedItem = this.prefs.getPref(userSetting);

        panel.setComboBox(comboBox, selectedItem);

    }

    private <E> FComboBox<E> createComboBox(final E[] items, final ForgePreferences.FPref setting) {
        final FComboBox<E> comboBox = new FComboBox<>(items);
        addComboBoxListener(comboBox, setting);
        return comboBox;
    }

    private <E> void addComboBoxListener(final FComboBox<E> comboBox, final ForgePreferences.FPref setting) {
        comboBox.addItemListener(new ItemListener() {
            @Override public void itemStateChanged(final ItemEvent e) {
                final E selectedType = comboBox.getSelectedItem();
                CSubmenuPreferences.this.prefs.setPref(setting, selectedType.toString());
                CSubmenuPreferences.this.prefs.save();
            }
        });
    }

    private void initializePlayerNameButton() {
        final FLabel btn = view.getBtnPlayerName();
        setPlayerNameButtonText();
        btn.setCommand(getPlayerNameButtonCommand());
    }

    private void disableLazyLoading() {
        view.getCbLoadCardsLazily().setSelected(false);
        view.getCbLoadCardsLazily().setEnabled(false);
        prefs.save();
    }

    private void setPlayerNameButtonText() {
        final FLabel btn = view.getBtnPlayerName();
        final String name = prefs.getPref(FPref.PLAYER_NAME);
        btn.setText(StringUtils.isBlank(name) ? localizer.getMessage("lblHuman") : name);
    }

    @SuppressWarnings("serial")
    private UiCommand getPlayerNameButtonCommand() {
        return new UiCommand() {
            @Override public void run() {
                GamePlayerUtil.setPlayerName();
                setPlayerNameButtonText();
            }
        };
    }
}
