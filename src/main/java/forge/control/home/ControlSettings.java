package forge.control.home;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JRadioButton;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import forge.AllZone;
import forge.Constant;
import forge.Singletons;
import forge.properties.ForgePreferences;
import forge.properties.ForgePreferences.CardSizeType;
import forge.properties.ForgePreferences.FPref;
import forge.view.GuiTopLevel;
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

    /**
     * 
     * Controls logic and listeners for Settings panel of the home screen.
     * 
     * @param v0 &emsp; ViewSettings
     */
    public ControlSettings(ViewSettings v0) {
        this.view = v0;
        addListeners();
        prefs = Singletons.getModel().getPreferences();
    }

    /** @return ViewSettings */
    public ViewSettings getView() {
        return view;
    }

    /** */
    public void addListeners() {
        this.view.getLstChooseSkin().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting()) { return; }
                try { updateSkin(); } catch (Exception e1) { e1.printStackTrace(); }
            }
        });

        this.view.getCbAnte().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
                final boolean toggle = ControlSettings.this.view.getCbAnte().isSelected();
                prefs.setPref(FPref.UI_ANTE, String.valueOf(toggle));
                try { prefs.save(); } catch (Exception e) { e.printStackTrace(); }
            }
        });

        this.view.getCbScaleLarger().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
                final boolean toggle = ControlSettings.this.view.getCbScaleLarger().isSelected();
                prefs.setPref(FPref.UI_SCALE_LARGER, String.valueOf(toggle));
                try { prefs.save(); } catch (Exception e) { e.printStackTrace(); }
            }
        });

        this.view.getCbDevMode().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
                final boolean toggle = ControlSettings.this.view.getCbDevMode().isSelected();
                prefs.setPref(FPref.DEV_MODE_ENABLED, String.valueOf(toggle));
                Constant.Runtime.DEV_MODE[0] = toggle;
                try { prefs.save(); } catch (Exception e) { e.printStackTrace(); }
            }
        });

        this.view.getCbRemoveSmall().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
                final boolean toggle = ControlSettings.this.view.getCbRemoveSmall().isSelected();
                prefs.setPref(FPref.DECKGEN_NOSMALL, String.valueOf(toggle));
                try { prefs.save(); } catch (Exception e) { e.printStackTrace(); }
            }
        });

        this.view.getCbRemoveArtifacts().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
                final boolean toggle = ControlSettings.this.view.getCbRemoveArtifacts().isSelected();
                prefs.setPref(FPref.DECKGEN_ARTIFACTS, String.valueOf(toggle));
                try { prefs.save(); } catch (Exception e) { e.printStackTrace(); }
            }
        });

        this.view.getCbSingletons().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
                final boolean toggle = ControlSettings.this.view.getCbSingletons().isSelected();
                prefs.setPref(FPref.DECKGEN_SINGLETONS, String.valueOf(toggle));
                try { prefs.save(); } catch (Exception e) { e.printStackTrace(); }
            }
        });

        this.view.getCbUploadDraft().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
                final boolean toggle = ControlSettings.this.view.getCbUploadDraft().isSelected();
                prefs.setPref(FPref.UI_UPLOAD_DRAFT , String.valueOf(toggle));
                Constant.Runtime.UPLOAD_DRAFT[0] = toggle;
                try { prefs.save(); } catch (Exception e) { e.printStackTrace(); }
            }
        });

        this.view.getCbStackLand().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
                final boolean toggle = ControlSettings.this.view.getCbStackLand().isSelected();
                prefs.setPref(FPref.UI_SMOOTH_LAND, String.valueOf(toggle));
                Constant.Runtime.SMOOTH[0] = toggle;
                try { prefs.save(); } catch (Exception e) { e.printStackTrace(); }
            }
        });

        this.view.getCbRandomFoil().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
                final boolean toggle = ControlSettings.this.view.getCbRandomFoil().isSelected();
                prefs.setPref(FPref.UI_RANDOM_FOIL, String.valueOf(toggle));
                Constant.Runtime.RANDOM_FOIL[0] = toggle;
                try { prefs.save(); } catch (Exception e) { e.printStackTrace(); }
            }
        });

        this.view.getCbTextMana().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
                final boolean toggle = ControlSettings.this.view.getCbTextMana().isSelected();
                prefs.setPref(FPref.UI_CARD_OVERLAY, String.valueOf(toggle));
                try { prefs.save(); } catch (Exception e) { e.printStackTrace(); }
            }
        });
    }

    private void updateSkin() {
        String name = view.getLstChooseSkin().getSelectedValue().toString();
        FSkin skin = new FSkin(name);

        skin.loadFontsAndImages();

        prefs.setPref(FPref.UI_SKIN, name);
        Singletons.getView().setSkin(skin);
        ((GuiTopLevel) AllZone.getDisplay()).getController().changeState(0);

        // changeState creates a new HomeTopLevel, so we can't just use the view object we already have.
        ((GuiTopLevel) AllZone.getDisplay()).getController().getHomeView().showSettingsMenu();

        prefs.save();
    }

    /** @param rad0 &emsp; JRadioButton
     * @throws Exception */
    public void updateCardSize(JRadioButton rad0) throws Exception {
        CardSizeType cst = CardSizeType.valueOf(rad0.getText().toLowerCase());
        Singletons.getModel().getPreferences().setPref(FPref.UI_CARD_SIZE, cst.toString());
        prefs.save();
    }
}
