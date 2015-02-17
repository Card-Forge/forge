package forge.screens.home.settings;

import forge.UiCommand;
import forge.Singletons;
import forge.ai.AiProfileUtil;
import forge.control.FControl.CloseAction;
import forge.control.RestartUtil;
import forge.game.GameLogEntryType;
import forge.gui.framework.FScreen;
import forge.gui.framework.ICDoc;
import forge.model.FModel;
import forge.player.GamePlayerUtil;
import forge.properties.ForgePreferences;
import forge.properties.ForgePreferences.FPref;
import forge.properties.ForgeConstants;
import forge.sound.SoundSystem;
import forge.toolbox.FComboBox;
import forge.toolbox.FComboBoxPanel;
import forge.toolbox.FLabel;
import forge.toolbox.FOptionPane;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import javax.swing.*;

import java.awt.Desktop;
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

    private VSubmenuPreferences view;
    private ForgePreferences prefs;
    private boolean updating;

    private final List<Pair<JCheckBox, FPref>> lstControls = new ArrayList<Pair<JCheckBox,FPref>>();

    @Override
    public void register() {
    }

    /* (non-Javadoc)
     * @see forge.control.home.IControlSubmenu#update()
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

        lstControls.clear(); // just in case
        lstControls.add(Pair.of(view.getCbAnte(), FPref.UI_ANTE));
        lstControls.add(Pair.of(view.getCbAnteMatchRarity(), FPref.UI_ANTE_MATCH_RARITY));
        lstControls.add(Pair.of(view.getCbManaBurn(), FPref.UI_MANABURN));
        lstControls.add(Pair.of(view.getCbScaleLarger(), FPref.UI_SCALE_LARGER));
        lstControls.add(Pair.of(view.getCbLargeCardViewers(), FPref.UI_LARGE_CARD_VIEWERS));
        lstControls.add(Pair.of(view.getCbRandomArtInPools(), FPref.UI_RANDOM_ART_IN_POOLS));
        lstControls.add(Pair.of(view.getCbEnforceDeckLegality(), FPref.ENFORCE_DECK_LEGALITY));
        lstControls.add(Pair.of(view.getCbCloneImgSource(), FPref.UI_CLONE_MODE_SOURCE));
        lstControls.add(Pair.of(view.getCbRemoveSmall(), FPref.DECKGEN_NOSMALL));
        lstControls.add(Pair.of(view.getCbRemoveArtifacts(), FPref.DECKGEN_ARTIFACTS));
        lstControls.add(Pair.of(view.getCbSingletons(), FPref.DECKGEN_SINGLETONS));
        lstControls.add(Pair.of(view.getCbUploadDraft(), FPref.UI_UPLOAD_DRAFT));
        lstControls.add(Pair.of(view.getCbEnableAICheats(), FPref.UI_ENABLE_AI_CHEATS));
        lstControls.add(Pair.of(view.getCbDisplayFoil(), FPref.UI_OVERLAY_FOIL_EFFECT));
        lstControls.add(Pair.of(view.getCbRandomFoil(), FPref.UI_RANDOM_FOIL));
        lstControls.add(Pair.of(view.getCbEnableSounds(), FPref.UI_ENABLE_SOUNDS));
        lstControls.add(Pair.of(view.getCbAltSoundSystem(), FPref.UI_ALT_SOUND_SYSTEM));
        lstControls.add(Pair.of(view.getCbUiForTouchScreen(), FPref.UI_FOR_TOUCHSCREN));
        lstControls.add(Pair.of(view.getCbCompactMainMenu(), FPref.UI_COMPACT_MAIN_MENU));
        lstControls.add(Pair.of(view.getCbPromptFreeBlocks(), FPref.MATCHPREF_PROMPT_FREE_BLOCKS));
        lstControls.add(Pair.of(view.getCbPauseWhileMinimized(), FPref.UI_PAUSE_WHILE_MINIMIZED));
        lstControls.add(Pair.of(view.getCbWorkshopSyntax(), FPref.DEV_WORKSHOP_SYNTAX));

        lstControls.add(Pair.of(view.getCbCompactPrompt(), FPref.UI_COMPACT_PROMPT));
        lstControls.add(Pair.of(view.getCbHideReminderText(), FPref.UI_HIDE_REMINDER_TEXT));
        lstControls.add(Pair.of(view.getCbOpenPacksIndiv(), FPref.UI_OPEN_PACKS_INDIV));
        lstControls.add(Pair.of(view.getCbStackCreatures(), FPref.UI_STACK_CREATURES));
        lstControls.add(Pair.of(view.getCbManaLostPrompt(), FPref.UI_MANA_LOST_PROMPT));

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

        view.getBtnContentDirectoryUI().setCommand(new UiCommand() {
            @Override
            public void run() {
                CSubmenuPreferences.this.openContentDirectory();
            }
        });

        initializeGameLogVerbosityComboBox();
        initializeCloseActionComboBox();
        initializeAiProfilesComboBox();
        initializePlayerNameButton();
    }

    /* (non-Javadoc)
     * @see forge.control.home.IControlSubmenu#update()
     */
    @Override
    public void update() {
        updating = true; //prevent itemStateChanged causing prefs to be saved or other logic occurring while updating values

        this.view = VSubmenuPreferences.SINGLETON_INSTANCE;
        this.prefs = FModel.getPreferences();

        setPlayerNameButtonText();
        view.getCbDevMode().setSelected(ForgePreferences.DEV_MODE);
        view.getCbEnableMusic().setSelected(prefs.getPrefBoolean(FPref.UI_ENABLE_MUSIC));

        for(Pair<JCheckBox, FPref> kv: lstControls) {
            kv.getKey().setSelected(prefs.getPrefBoolean(kv.getValue()));
        }
        view.reloadShortcuts();

        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() { view.getCbRemoveSmall().requestFocusInWindow(); }
        });

        updating = false;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#getCommandOnSelect()
     */
    @Override
    public UiCommand getCommandOnSelect() {
        return null;
    }

    private void resetForgeSettingsToDefault() {
        String userPrompt =
                "This will reset all preferences to their defaults and restart Forge.\n\n" +
                        "Reset and restart Forge?";
        if (FOptionPane.showConfirmDialog(userPrompt, "Reset Settings")) {
            ForgePreferences prefs = FModel.getPreferences();
            prefs.reset();
            prefs.save();
            update();
            RestartUtil.restartApplication(null);
        }
    }

    private void resetDeckEditorLayout() {
        String userPrompt =
                "This will reset the Deck Editor screen layout.\n" +
                        "All tabbed views will be restored to their default positions.\n\n" +
                        "Reset layout?";
        if (FOptionPane.showConfirmDialog(userPrompt, "Reset Deck Editor Layout")) {
            if (FScreen.DECK_EDITOR_CONSTRUCTED.deleteLayoutFile()) {
                FOptionPane.showMessageDialog("Deck Editor layout has been reset.");
            }
        }
    }

    private void resetWorkshopLayout() {
        String userPrompt =
                "This will reset the Workshop screen layout.\n" +
                        "All tabbed views will be restored to their default positions.\n\n" +
                        "Reset layout?";
        if (FOptionPane.showConfirmDialog(userPrompt, "Reset Workshop Layout")) {
            if (FScreen.WORKSHOP_SCREEN.deleteLayoutFile()) {
                FOptionPane.showMessageDialog("Workshop layout has been reset.");
            }
        }
    }

    private void resetMatchScreenLayout() {
        String userPrompt =
                "This will reset the layout of the Match screen.\n" +
                        "If you want to save the current layout first, please use " +
                        "the Dock tab -> Save Layout option in the Match screen.\n\n" +
                        "Reset layout?";
        if (FOptionPane.showConfirmDialog(userPrompt, "Reset Match Screen Layout")) {
            if (FScreen.deleteMatchLayoutFile()) {
                FOptionPane.showMessageDialog("Match Screen layout has been reset.");
            }
        }
    }

    private void openUserProfileDirectory() {
        try{
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(new File(ForgeConstants.USER_DIR));
            }
        }
        catch(Exception e) {
            System.out.println("Unable to open Directory: " + e.toString());
        }
    }

    private void openContentDirectory() {
        try{
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(new File(ForgeConstants.CACHE_DIR));
            }
        }
        catch(Exception e) {
            System.out.println("Unable to open Directory: " + e.toString());
        }
    }

    private void initializeGameLogVerbosityComboBox() {
        FPref userSetting = FPref.DEV_LOG_ENTRY_TYPE;
        FComboBoxPanel<GameLogEntryType> panel = this.view.getGameLogVerbosityComboBoxPanel();
        FComboBox<GameLogEntryType> comboBox = createComboBox(GameLogEntryType.values(), userSetting);
        GameLogEntryType selectedItem = GameLogEntryType.valueOf(this.prefs.getPref(userSetting));
        panel.setComboBox(comboBox, selectedItem);
    }
    
    private void initializeCloseActionComboBox() {
        final FComboBoxPanel<CloseAction> panel = this.view.getCloseActionComboBoxPanel();
        final FComboBox<CloseAction> comboBox = new FComboBox<CloseAction>(CloseAction.values());
        comboBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(final ItemEvent e) {
                Singletons.getControl().setCloseAction((CloseAction) comboBox.getSelectedItem());
            }
        });
        panel.setComboBox(comboBox, Singletons.getControl().getCloseAction());
    }

    private void initializeAiProfilesComboBox() {
        FPref userSetting = FPref.UI_CURRENT_AI_PROFILE;
        FComboBoxPanel<String> panel = this.view.getAiProfilesComboBoxPanel();
        FComboBox<String> comboBox = createComboBox(AiProfileUtil.getProfilesArray(), userSetting);
        String selectedItem = this.prefs.getPref(userSetting);
        panel.setComboBox(comboBox, selectedItem);
    }

    private <E> FComboBox<E> createComboBox(E[] items, final ForgePreferences.FPref setting) {
        final FComboBox<E> comboBox = new FComboBox<E>(items);
        addComboBoxListener(comboBox, setting);
        return comboBox;
    }

    private <E> void addComboBoxListener(final FComboBox<E> comboBox, final ForgePreferences.FPref setting) {
        comboBox.addItemListener(new ItemListener() {
            @SuppressWarnings("unchecked")
            @Override
            public void itemStateChanged(final ItemEvent e) {
                E selectedType = (E) comboBox.getSelectedItem();
                CSubmenuPreferences.this.prefs.setPref(setting, selectedType.toString());
                CSubmenuPreferences.this.prefs.save();
            }
        });
    }

    private void initializePlayerNameButton() {
        FLabel btn = view.getBtnPlayerName();
        setPlayerNameButtonText();
        btn.setCommand(getPlayerNameButtonCommand());
    }

    private void setPlayerNameButtonText() {
        FLabel btn = view.getBtnPlayerName();
        String name = prefs.getPref(FPref.PLAYER_NAME);
        btn.setText(StringUtils.isBlank(name) ? "Human" : name);
    }

    @SuppressWarnings("serial")
    private UiCommand getPlayerNameButtonCommand() {
        return new UiCommand() {
            @Override
            public void run() {
                GamePlayerUtil.setPlayerName();
                setPlayerNameButtonText();
            }
        };
    }
}
