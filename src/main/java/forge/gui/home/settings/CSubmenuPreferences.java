package forge.gui.home.settings;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import org.apache.commons.lang3.text.WordUtils;

import forge.Command;
import forge.Constant.Preferences;
import forge.Singletons;
import forge.control.RestartUtil;
import forge.gui.framework.ICDoc;
import forge.gui.toolbox.FSkin;
import forge.properties.ForgePreferences;
import forge.properties.ForgePreferences.FPref;

/** 
 * Controls the preferences submenu in the home UI.
 * 
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 *
 */
public enum CSubmenuPreferences implements ICDoc {
    /** */
    SINGLETON_INSTANCE;

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

        view.getCbAnte().addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(final ItemEvent arg0) {
                final boolean toggle = view.getCbAnte().isSelected();
                prefs.setPref(FPref.UI_ANTE, String.valueOf(toggle));
                prefs.save();
            }
        });

        view.getCbManaBurn().addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(final ItemEvent arg0) {
                final boolean toggle = view.getCbManaBurn().isSelected();
                prefs.setPref(FPref.UI_MANABURN, String.valueOf(toggle));
                prefs.save();
            }
        });

        view.getCbScaleLarger().addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(final ItemEvent arg0) {
                final boolean toggle = view.getCbScaleLarger().isSelected();
                prefs.setPref(FPref.UI_SCALE_LARGER, String.valueOf(toggle));
                prefs.save();
            }
        });

        view.getCbDevMode().addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(final ItemEvent arg0) {
                final boolean toggle = view.getCbDevMode().isSelected();
                prefs.setPref(FPref.DEV_MODE_ENABLED, String.valueOf(toggle));
                Preferences.DEV_MODE = toggle;
                prefs.save();
            }
        });

        view.getCbEnforceDeckLegality().addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(final ItemEvent arg0) {
                final boolean toggle = view.getCbEnforceDeckLegality().isSelected();
                prefs.setPref(FPref.ENFORCE_DECK_LEGALITY, String.valueOf(toggle));
                prefs.save();
            }
        });

        view.getCbRemoveSmall().addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(final ItemEvent arg0) {
                final boolean toggle = view.getCbRemoveSmall().isSelected();
                prefs.setPref(FPref.DECKGEN_NOSMALL, String.valueOf(toggle));
                prefs.save();
            }
        });

        view.getCbRemoveArtifacts().addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(final ItemEvent arg0) {
                final boolean toggle = view.getCbRemoveArtifacts().isSelected();
                prefs.setPref(FPref.DECKGEN_ARTIFACTS, String.valueOf(toggle));
                prefs.save();
            }
        });

        view.getCbSingletons().addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(final ItemEvent arg0) {
                final boolean toggle = view.getCbSingletons().isSelected();
                prefs.setPref(FPref.DECKGEN_SINGLETONS, String.valueOf(toggle));
                prefs.save();
            }
        });

        view.getCbUploadDraft().addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(final ItemEvent arg0) {
                final boolean toggle = view.getCbUploadDraft().isSelected();
                prefs.setPref(FPref.UI_UPLOAD_DRAFT , String.valueOf(toggle));
                Preferences.UPLOAD_DRAFT = toggle;
                prefs.save();
            }
        });

        view.getCbStackLand().addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(final ItemEvent arg0) {
                prefs.setPref(FPref.UI_SMOOTH_LAND, view.getCbRandomFoil().isSelected());
                prefs.save();
            }
        });

        view.getCbRandomFoil().addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(final ItemEvent arg0) {
                prefs.setPref(FPref.UI_RANDOM_FOIL, view.getCbRandomFoil().isSelected());
                prefs.save();
            }
        });

        view.getCbTextMana().addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(final ItemEvent arg0) {
                final boolean toggle = view.getCbTextMana().isSelected();
                prefs.setPref(FPref.UI_CARD_OVERLAY, String.valueOf(toggle));
                prefs.save();
            }
        });

        view.getCbRandomizeArt().addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(final ItemEvent arg0) {
                prefs.setPref(FPref.UI_RANDOM_CARD_ART, view.getCbRandomizeArt().isSelected());
                prefs.save();
            }
        });

        view.getCbEnableSounds().addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(final ItemEvent arg0) {
                prefs.setPref(FPref.UI_ENABLE_SOUNDS, view.getCbEnableSounds().isSelected());
                prefs.save();
            }
        });

        view.getBtnReset().setCommand(new Command() {
            @Override
            public void execute() {
                ForgePreferences prefs = Singletons.getModel().getPreferences();
                prefs.reset();
                prefs.save();
                update();
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

        view.getCbRemoveSmall().setSelected(prefs.getPrefBoolean(FPref.DECKGEN_NOSMALL));
        view.getCbSingletons().setSelected(prefs.getPrefBoolean(FPref.DECKGEN_SINGLETONS));
        view.getCbRemoveArtifacts().setSelected(prefs.getPrefBoolean(FPref.DECKGEN_ARTIFACTS));
        view.getCbAnte().setSelected(prefs.getPrefBoolean(FPref.UI_ANTE));
        view.getCbManaBurn().setSelected(prefs.getPrefBoolean(FPref.UI_MANABURN));
        view.getCbUploadDraft().setSelected(prefs.getPrefBoolean(FPref.UI_UPLOAD_DRAFT));
        view.getCbStackLand().setSelected(prefs.getPrefBoolean(FPref.UI_SMOOTH_LAND));
        view.getCbDevMode().setSelected(prefs.getPrefBoolean(FPref.DEV_MODE_ENABLED));
        view.getCbEnforceDeckLegality().setSelected(prefs.getPrefBoolean(FPref.ENFORCE_DECK_LEGALITY));
        view.getCbRandomFoil().setSelected(prefs.getPrefBoolean(FPref.UI_RANDOM_FOIL));
        view.getCbRandomizeArt().setSelected(prefs.getPrefBoolean(FPref.UI_RANDOM_CARD_ART));
        view.getCbScaleLarger().setSelected(prefs.getPrefBoolean(FPref.UI_SCALE_LARGER));
        view.getCbTextMana().setSelected(prefs.getPrefBoolean(FPref.UI_CARD_OVERLAY));
        view.getCbEnableSounds().setSelected(prefs.getPrefBoolean(FPref.UI_ENABLE_SOUNDS));
        view.reloadShortcuts();
    }

    private void updateSkinNames() {
        final VSubmenuPreferences view = VSubmenuPreferences.SINGLETON_INSTANCE;
        final String[] uglyNames = FSkin.getSkins().toArray(new String[0]);
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
            public void execute() { RestartUtil.restartApplication(null); } });

        prefs.setPref(FPref.UI_SKIN, name);
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
