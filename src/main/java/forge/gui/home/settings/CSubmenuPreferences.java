package forge.gui.home.settings;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;

import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.apache.commons.lang3.tuple.Pair;

import forge.Command;
import forge.Constant.Preferences;
import forge.GameLogEntryType;
import forge.Singletons;
import forge.control.FControl.Screens;
import forge.control.RestartUtil;
import forge.game.ai.AiProfileUtil;
import forge.gui.framework.ICDoc;
import forge.gui.framework.SLayoutIO;
import forge.gui.toolbox.FSkin;
import forge.properties.ForgePreferences;
import forge.properties.ForgePreferences.FPref;
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
    

    private final List<Pair<JCheckBox, FPref>> lstControls = new ArrayList<Pair<JCheckBox,FPref>>();

    /* (non-Javadoc)
     * @see forge.control.home.IControlSubmenu#update()
     */
    @SuppressWarnings("serial")
    @Override
    public void initialize() {
        final VSubmenuPreferences view = VSubmenuPreferences.SINGLETON_INSTANCE;
        final ForgePreferences prefs = Singletons.getModel().getPreferences();

        view.getLstChooseSkin().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(final MouseEvent e) {
                updateSkin();
            }
        });

        view.getLstChooseAIProfile().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(final MouseEvent e) {
                updateAIProfile();
            }
        });

        // This updates variable right now and is not standard 
        view.getCbDevMode().addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(final ItemEvent arg0) {
                final boolean toggle = view.getCbDevMode().isSelected();
                prefs.setPref(FPref.DEV_MODE_ENABLED, String.valueOf(toggle));
                Preferences.DEV_MODE = toggle;
                prefs.save();
            }
        });
        
        view.getCboLogEntryType().addItemListener(new ItemListener() {            
            @Override
            public void itemStateChanged(final ItemEvent e) {
                GameLogEntryType selectedType = (GameLogEntryType) view.getCboLogEntryType().getSelectedItem();
                prefs.setPref(FPref.DEV_LOG_ENTRY_TYPE, selectedType.toString());
                prefs.save();
            }
        });

        lstControls.clear(); // just in case
        lstControls.add(Pair.of(view.getCbCompactMainMenu(), FPref.UI_COMPACT_MAIN_MENU));        
        lstControls.add(Pair.of(view.getCbAnte(), FPref.UI_ANTE));
        lstControls.add(Pair.of(view.getCbManaBurn(), FPref.UI_MANABURN));
        lstControls.add(Pair.of(view.getCbScaleLarger(), FPref.UI_SCALE_LARGER));
        lstControls.add(Pair.of(view.getCbEnforceDeckLegality(), FPref.ENFORCE_DECK_LEGALITY));
        lstControls.add(Pair.of(view.getCbCloneImgSource(), FPref.UI_CLONE_MODE_SOURCE));
        lstControls.add(Pair.of(view.getCbRemoveSmall(), FPref.DECKGEN_NOSMALL));
        lstControls.add(Pair.of(view.getCbRemoveArtifacts(), FPref.DECKGEN_ARTIFACTS));
        lstControls.add(Pair.of(view.getCbSingletons(), FPref.DECKGEN_SINGLETONS));
        lstControls.add(Pair.of(view.getCbUploadDraft(), FPref.UI_UPLOAD_DRAFT));
        lstControls.add(Pair.of(view.getCbStackLand(), FPref.UI_SMOOTH_LAND));
        lstControls.add(Pair.of(view.getCbRandomFoil(), FPref.UI_RANDOM_FOIL));        
        lstControls.add(Pair.of(view.getCbOverlayCardName(), FPref.UI_OVERLAY_CARD_NAME));
        lstControls.add(Pair.of(view.getCbOverlayCardPower(), FPref.UI_OVERLAY_CARD_POWER));
        lstControls.add(Pair.of(view.getCbOverlayCardManaCost(), FPref.UI_OVERLAY_CARD_MANA_COST));
        lstControls.add(Pair.of(view.getCbRandomizeArt(), FPref.UI_RANDOM_CARD_ART));
        lstControls.add(Pair.of(view.getCbEnableSounds(), FPref.UI_ENABLE_SOUNDS));
        lstControls.add(Pair.of(view.getCbAltSoundSystem(), FPref.UI_ALT_SOUND_SYSTEM));
        lstControls.add(Pair.of(view.getCbUiForTouchScreen(), FPref.UI_FOR_TOUCHSCREN));

        for(final Pair<JCheckBox, FPref> kv : lstControls) {
            kv.getKey().addItemListener(new ItemListener() {
                @Override
                public void itemStateChanged(final ItemEvent arg0) {
                    prefs.setPref(kv.getValue(), String.valueOf(kv.getKey().isSelected()));
                    prefs.save();
                }
            });
        }

        view.getBtnReset().setCommand(new Command() {
            @Override
            public void run() {
                CSubmenuPreferences.this.resetForgeSettingsToDefault();
            }
        });
        
        view.getBtnDeleteEditorUI().setCommand(new Command() {
            @Override
            public void run() {
                CSubmenuPreferences.this.resetDeckEditorLayout();
            }
        });
        
        view.getBtnDeleteMatchUI().setCommand(new Command() {
            @Override
            public void run() {
                CSubmenuPreferences.this.resetMatchScreenLayout();
            }
        });        
    }

    /* (non-Javadoc)
     * @see forge.control.home.IControlSubmenu#update()
     */
    @Override
    public void update() {
        final VSubmenuPreferences view = VSubmenuPreferences.SINGLETON_INSTANCE;
        final ForgePreferences prefs = Singletons.getModel().getPreferences();
        updateSkinNames();
        updateAIProfiles();

        view.getCbDevMode().setSelected(prefs.getPrefBoolean(FPref.DEV_MODE_ENABLED));
        view.getCboLogEntryType().setSelectedItem(GameLogEntryType.valueOf(prefs.getPref(FPref.DEV_LOG_ENTRY_TYPE)));
        
        for(Pair<JCheckBox, FPref> kv: lstControls) {
            kv.getKey().setSelected(prefs.getPrefBoolean(kv.getValue()));
        }
        view.reloadShortcuts();

        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() { view.getCbRemoveSmall().requestFocusInWindow(); }
        });
    }

    private void resetForgeSettingsToDefault() {
        String userPrompt = 
                "This will reset all preferences to their defaults and restart Forge.\n\n" +
                "Reset and restart Forge?";               
        int reply = JOptionPane.showConfirmDialog(null, userPrompt, "Reset Settings", JOptionPane.YES_NO_OPTION);
        if (reply == JOptionPane.YES_OPTION) {
            ForgePreferences prefs = Singletons.getModel().getPreferences();
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
        int reply = JOptionPane.showConfirmDialog(null, userPrompt, "Reset Deck Editor Layout", JOptionPane.YES_NO_OPTION);
        if (reply == JOptionPane.YES_OPTION) {
            deleteScreenLayoutFile(Screens.DECK_EDITOR_CONSTRUCTED);
            JOptionPane.showMessageDialog(null, "Deck Editor layout has been reset.");
        }        
    }
    
    private void resetMatchScreenLayout() {
        String userPrompt = 
                "This will reset the layout of the Match screen.\n" +
                "If you want to save the current layout first, please use " +
                "the Dock tab -> Save Layout option in the Match screen.\n\n" +
                "Reset layout?";
        int reply = JOptionPane.showConfirmDialog(null, userPrompt, "Reset Match Screen Layout", JOptionPane.YES_NO_OPTION);
        if (reply == JOptionPane.YES_OPTION) {
            deleteScreenLayoutFile(Screens.MATCH_SCREEN);
            JOptionPane.showMessageDialog(null, "Match Screen layout has been reset.");            
        }        
    }
        
    private void deleteScreenLayoutFile(Screens screen) {
        String fd = SLayoutIO.getFilePreferred(screen);
        File f = new File(fd);
        f.delete();      
    }
    	
    private void updateSkinNames() {
        final VSubmenuPreferences view = VSubmenuPreferences.SINGLETON_INSTANCE;
        final String[] uglyNames = FSkin.getSkins().toArray(ArrayUtils.EMPTY_STRING_ARRAY);
        final String[] prettyNames = new String[uglyNames.length];
        final String currentName = Singletons.getModel().getPreferences().getPref(FPref.UI_SKIN);
        int currentIndex = 0;

        for (int i = 0; i < uglyNames.length; i++) {
            prettyNames[i] = WordUtils.capitalize(uglyNames[i].replace('_', ' '));
            if (currentName.equalsIgnoreCase(prettyNames[i])) { currentIndex = i; }
        }

        view.getLstChooseSkin().setListData(prettyNames);
        view.getLstChooseSkin().setSelectedIndex(currentIndex);
        view.getLstChooseSkin().ensureIndexIsVisible(view.getLstChooseSkin().getSelectedIndex());
    }
	
    private void updateAIProfiles() {
        final VSubmenuPreferences view = VSubmenuPreferences.SINGLETON_INSTANCE;
        final List<String> profileNames = AiProfileUtil.getProfilesDisplayList();
        final String currentName = Singletons.getModel().getPreferences().getPref(FPref.UI_CURRENT_AI_PROFILE);
        int currentIndex = 0;

        for (int i = 0; i < profileNames.size(); i++) {
            if (currentName.equalsIgnoreCase(profileNames.get(i))) { currentIndex = i; }
        }

        view.getLstChooseAIProfile().setListData(profileNames.toArray(ArrayUtils.EMPTY_STRING_ARRAY));
        view.getLstChooseAIProfile().setSelectedIndex(currentIndex);
        view.getLstChooseAIProfile().ensureIndexIsVisible(view.getLstChooseAIProfile().getSelectedIndex());
    }

    @SuppressWarnings("serial")
    private void updateSkin() {
        final VSubmenuPreferences view = VSubmenuPreferences.SINGLETON_INSTANCE;
        final String name = view.getLstChooseSkin().getSelectedValue().toString();
        final ForgePreferences prefs = Singletons.getModel().getPreferences();
        if (name.equals(prefs.getPref(FPref.UI_SKIN))) { return; }

        view.getScrChooseSkin().setVisible(false);
        view.getLblChooseSkin().setText("Please restart Forge (click here to close).");
        view.getLblChooseSkin().setHoverable(true);
        view.getLblChooseSkin().setCommand(new Command() { @Override
            public void run() { RestartUtil.restartApplication(null); } });

        prefs.setPref(FPref.UI_SKIN, name);
        prefs.save();
    }

    private void updateAIProfile() {
        final VSubmenuPreferences view = VSubmenuPreferences.SINGLETON_INSTANCE;
        final String name = view.getLstChooseAIProfile().getSelectedValue().toString();
        final ForgePreferences prefs = Singletons.getModel().getPreferences();
        if (name.equals(prefs.getPref(FPref.UI_CURRENT_AI_PROFILE))) { return; }

        prefs.setPref(FPref.UI_CURRENT_AI_PROFILE, name);
        prefs.save();
    }
    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#getCommandOnSelect()
     */
    @Override
    public Command getCommandOnSelect() {
        return null;
    }
}
