package forge.control.home;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JRadioButton;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import forge.AllZone;
import forge.Constant;
import forge.ImageCache;
import forge.Singletons;
import forge.properties.ForgePreferences;
import forge.properties.ForgePreferences.CardSizeType;
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
                prefs.setPlayForAnte(toggle);
                try { prefs.save(); } catch (Exception e) { e.printStackTrace(); }
            }
        });

        this.view.getCbScaleLarger().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
                final boolean toggle = ControlSettings.this.view.getCbScaleLarger().isSelected();
                prefs.setScaleLargerThanOriginal(toggle);
                ImageCache.setScaleLargerThanOriginal(toggle);
                try { prefs.save(); } catch (Exception e) { e.printStackTrace(); }
            }
        });

        this.view.getCbDevMode().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
                final boolean toggle = ControlSettings.this.view.getCbDevMode().isSelected();
                prefs.setDeveloperMode(toggle);
                Constant.Runtime.DEV_MODE[0] = toggle;
                try { prefs.save(); } catch (Exception e) { e.printStackTrace(); }
            }
        });

        this.view.getCbRemoveSmall().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
                final boolean toggle = ControlSettings.this.view.getCbRemoveSmall().isSelected();
                prefs.setDeckGenRmvSmall(toggle);
                try { prefs.save(); } catch (Exception e) { e.printStackTrace(); }
            }
        });

        this.view.getCbRemoveArtifacts().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
                final boolean toggle = ControlSettings.this.view.getCbRemoveArtifacts().isSelected();
                prefs.setDeckGenRmvArtifacts(toggle);
                try { prefs.save(); } catch (Exception e) { e.printStackTrace(); }
            }
        });

        this.view.getCbSingletons().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
                final boolean toggle = ControlSettings.this.view.getCbSingletons().isSelected();
                prefs.setDeckGenSingletons(toggle);
                try { prefs.save(); } catch (Exception e) { e.printStackTrace(); }
            }
        });

        this.view.getCbUploadDraft().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
                final boolean toggle = ControlSettings.this.view.getCbUploadDraft().isSelected();
                prefs.setUploadDraftAI(toggle);
                Constant.Runtime.UPLOAD_DRAFT[0] = toggle;
                try { prefs.save(); } catch (Exception e) { e.printStackTrace(); }
            }
        });

        this.view.getCbStackLand().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
                final boolean toggle = ControlSettings.this.view.getCbStackLand().isSelected();
                prefs.setStackAiLand(toggle);
                Constant.Runtime.SMOOTH[0] = toggle;
                try { prefs.save(); } catch (Exception e) { e.printStackTrace(); }
            }
        });

        this.view.getCbRandomFoil().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
                final boolean toggle = ControlSettings.this.view.getCbRandomFoil().isSelected();
                prefs.setRandCFoil(toggle);
                Constant.Runtime.RANDOM_FOIL[0] = toggle;
                try { prefs.save(); } catch (Exception e) { e.printStackTrace(); }
            }
        });

        this.view.getCbTextMana().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
                final boolean toggle = ControlSettings.this.view.getCbTextMana().isSelected();
                prefs.setCardOverlay(toggle);
                try { prefs.save(); } catch (Exception e) { e.printStackTrace(); }
            }
        });
    }

    private void updateSkin() throws Exception {
        String name = view.getLstChooseSkin().getSelectedValue().toString();
        FSkin skin = new FSkin(name);
        skin.loadFontAndImages();

        prefs.setSkin(name);
        Singletons.getView().setSkin(skin);
        ((GuiTopLevel) AllZone.getDisplay()).getController().changeState(0);
        // TODO This should work, but it doesn't. :|  Doublestrike 15-12-11
        view.getParentView().showSettingsMenu();

        prefs.save();
    }

    /** @param rad0 &emsp; JRadioButton
     * @throws Exception */
    public void updateCardSize(JRadioButton rad0) throws Exception {
        CardSizeType cst = CardSizeType.valueOf(rad0.getText().toLowerCase());
        Singletons.getModel().getPreferences().setCardSize(cst);
        prefs.save();
    }
}
