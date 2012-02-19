package forge.control.home;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import forge.Command;
import forge.Constant;
import forge.Singletons;
import forge.control.FControl;
import forge.properties.ForgePreferences;
import forge.properties.ForgePreferences.FPref;
import forge.view.home.ViewSettings;
import forge.view.toolbox.FSkin;

/** 
 * Controls logic and listeners for Settings panel of the home screen.
 *
 * Saving of preferences happens at every state change in FControl.
 */
public class ControlSettings {
    private ViewSettings view;
    private ForgePreferences prefs;
    private JPanel selectedTab;
    private final MouseListener madPreferences, madAvatars;

    /**
     * 
     * Controls logic and listeners for Settings panel of the home screen.
     * 
     * @param v0 &emsp; ViewSettings
     */
    public ControlSettings(ViewSettings v0) {
        this.view = v0;
        view.updateSkinNames();

        addListeners();
        prefs = Singletons.getModel().getPreferences();

        view.getCbRemoveSmall().setSelected(prefs.getPrefBoolean(FPref.DECKGEN_NOSMALL));
        view.getCbSingletons().setSelected(prefs.getPrefBoolean(FPref.DECKGEN_SINGLETONS));
        view.getCbRemoveArtifacts().setSelected(prefs.getPrefBoolean(FPref.DECKGEN_ARTIFACTS));
        view.getCbAnte().setSelected(prefs.getPrefBoolean(FPref.UI_ANTE));
        view.getCbUploadDraft().setSelected(prefs.getPrefBoolean(FPref.UI_UPLOAD_DRAFT));
        view.getCbStackLand().setSelected(prefs.getPrefBoolean(FPref.UI_SMOOTH_LAND));
        view.getCbDevMode().setSelected(prefs.getPrefBoolean(FPref.DEV_MODE_ENABLED));
        view.getCbRandomFoil().setSelected(prefs.getPrefBoolean(FPref.UI_RANDOM_FOIL));
        view.getCbScaleLarger().setSelected(prefs.getPrefBoolean(FPref.UI_SCALE_LARGER));
        view.getCbTextMana().setSelected(prefs.getPrefBoolean(FPref.UI_CARD_OVERLAY));

        madPreferences = new MouseAdapter() { @Override
            public void mouseClicked(MouseEvent e) { view.showPrefsTab(); } };

        madAvatars = new MouseAdapter() { @Override
            public void mouseClicked(MouseEvent e) { view.showAvatarsTab(); } };

        view.getTabPrefs().removeMouseListener(madPreferences);
        view.getTabPrefs().addMouseListener(madPreferences);

        view.getTabAvatars().removeMouseListener(madAvatars);
        view.getTabAvatars().addMouseListener(madAvatars);
    }

    /** @return ViewSettings */
    public ViewSettings getView() {
        return view;
    }

    /** */
    @SuppressWarnings("serial")
    public void addListeners() {
        this.view.getLstChooseSkin().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting()) { return; }
                updateSkin();
            }
        });

        this.view.getCbAnte().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
                final boolean toggle = ControlSettings.this.view.getCbAnte().isSelected();
                prefs.setPref(FPref.UI_ANTE, String.valueOf(toggle));
                prefs.save();
            }
        });

        this.view.getCbScaleLarger().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
                final boolean toggle = ControlSettings.this.view.getCbScaleLarger().isSelected();
                prefs.setPref(FPref.UI_SCALE_LARGER, String.valueOf(toggle));
                prefs.save();
            }
        });

        this.view.getCbDevMode().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
                final boolean toggle = ControlSettings.this.view.getCbDevMode().isSelected();
                prefs.setPref(FPref.DEV_MODE_ENABLED, String.valueOf(toggle));
                Constant.Runtime.DEV_MODE[0] = toggle;
                prefs.save();
            }
        });

        this.view.getCbRemoveSmall().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
                final boolean toggle = ControlSettings.this.view.getCbRemoveSmall().isSelected();
                prefs.setPref(FPref.DECKGEN_NOSMALL, String.valueOf(toggle));
                prefs.save();
            }
        });

        this.view.getCbRemoveArtifacts().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
                final boolean toggle = ControlSettings.this.view.getCbRemoveArtifacts().isSelected();
                prefs.setPref(FPref.DECKGEN_ARTIFACTS, String.valueOf(toggle));
                prefs.save();
            }
        });

        this.view.getCbSingletons().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
                final boolean toggle = ControlSettings.this.view.getCbSingletons().isSelected();
                prefs.setPref(FPref.DECKGEN_SINGLETONS, String.valueOf(toggle));
                prefs.save();
            }
        });

        this.view.getCbUploadDraft().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
                final boolean toggle = ControlSettings.this.view.getCbUploadDraft().isSelected();
                prefs.setPref(FPref.UI_UPLOAD_DRAFT , String.valueOf(toggle));
                Constant.Runtime.UPLOAD_DRAFT[0] = toggle;
                prefs.save();
            }
        });

        this.view.getCbStackLand().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
                final boolean toggle = ControlSettings.this.view.getCbStackLand().isSelected();
                prefs.setPref(FPref.UI_SMOOTH_LAND, String.valueOf(toggle));
                Constant.Runtime.SMOOTH[0] = toggle;
                prefs.save();
            }
        });

        this.view.getCbRandomFoil().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
                final boolean toggle = ControlSettings.this.view.getCbRandomFoil().isSelected();
                prefs.setPref(FPref.UI_RANDOM_FOIL, String.valueOf(toggle));
                Constant.Runtime.RANDOM_FOIL[0] = toggle;
                prefs.save();
            }
        });

        this.view.getCbTextMana().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
                final boolean toggle = ControlSettings.this.view.getCbTextMana().isSelected();
                prefs.setPref(FPref.UI_CARD_OVERLAY, String.valueOf(toggle));
                prefs.save();
            }
        });

        this.view.getBtnReset().setCommand(new Command() {
            @Override
            public void execute() {
                Singletons.getModel().getPreferences().reset();
                Singletons.getView().getViewHome().resetSettings();
            }
        });
    }

    /**
     * Updates visual state of tabber.
     * @param tab0 &emsp; JPanel tab object (can pass SubTab too).
     */
    public void updateTabber(JPanel tab0) {
        if (selectedTab != null) {
            selectedTab.setEnabled(false);
        }

        tab0.setEnabled(true);
        selectedTab = tab0;
    }

    private void updateSkin() {
        view.getLblTitleSkin().setText(" Loading...");
        view.getLblTitleSkin().setIcon(new ImageIcon("res/images/skins/default/loader.gif"));

        final String name = view.getLstChooseSkin().getSelectedValue().toString();

        final SwingWorker<Object, Object> w = new SwingWorker<Object, Object>() {
            @Override
            public String doInBackground() {
                FSkin.loadLight(name);
                FSkin.loadFull();

                prefs.setPref(FPref.UI_SKIN, name);
                prefs.save();
                return null;
            }

            @Override
            protected void done() {
                Singletons.getView().instantiateCachedUIStates();
                Singletons.getControl().changeState(FControl.HOME_SCREEN);
                Singletons.getView().getViewHome().showSettingsMenu();
            }
        };
        w.execute();
    }

    /** @param rad0 &emsp; JRadioButton
     * @throws Exception */
    /*public void updateCardSize(JRadioButton rad0) throws Exception {
        CardSizeType cst = CardSizeType.valueOf(rad0.getText().toLowerCase());
        Singletons.getModel().getPreferences().setPref(FPref.UI_CARD_SIZE, cst.toString());
        prefs.save();
    }*/
}
