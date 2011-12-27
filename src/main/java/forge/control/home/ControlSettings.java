package forge.control.home;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JRadioButton;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import forge.AllZone;
import forge.Singletons;
import forge.properties.ForgePreferences;
import forge.properties.ForgePreferences.CardSizeType;
import forge.view.GuiTopLevel;
import forge.view.home.ViewSettings;
import forge.view.toolbox.FSkin;

/** 
 * Controls logic and listeners for Settings panel of the home screen.
 *
 * Saving of preferences happens at every state change in ControlAllUI.
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
                Singletons.getModel().getPreferences()
                        .setPlayForAnte(ControlSettings.this.view.getCbAnte().isSelected());
                try { prefs.save(); } catch (Exception e) { e.printStackTrace(); }
            }
        });

        this.view.getCbScaleLarger().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
                prefs.setScaleLargerThanOriginal(ControlSettings.this.view.getCbScaleLarger().isSelected());
                try { prefs.save(); } catch (Exception e) { e.printStackTrace(); }
            }
        });

        this.view.getCbDevMode().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
                prefs.setDeveloperMode(ControlSettings.this.view.getCbDevMode().isSelected());
                try { prefs.save(); } catch (Exception e) { e.printStackTrace(); }
            }
        });

        this.view.getCbRemoveSmall().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
                prefs.setDeckGenRmvSmall(ControlSettings.this.view.getCbRemoveSmall().isSelected());
                try { prefs.save(); } catch (Exception e) { e.printStackTrace(); }
            }
        });

        this.view.getCbRemoveArtifacts().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
                prefs.setDeckGenRmvArtifacts(ControlSettings.this.view.getCbRemoveArtifacts().isSelected());
                try { prefs.save(); } catch (Exception e) { e.printStackTrace(); }
            }
        });

        this.view.getCbSingletons().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
                prefs.setDeckGenSingletons(ControlSettings.this.view.getCbSingletons().isSelected());
                try { prefs.save(); } catch (Exception e) { e.printStackTrace(); }
            }
        });

        this.view.getCbOldUI().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
                prefs.setOldGui(ControlSettings.this.view.getCbOldUI().isSelected());
                try { prefs.save(); } catch (Exception e) { e.printStackTrace(); }
            }
        });

        this.view.getCbUploadDraft().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
                prefs.setUploadDraftAI(ControlSettings.this.view.getCbUploadDraft().isSelected());
                try { prefs.save(); } catch (Exception e) { e.printStackTrace(); }
            }
        });

        this.view.getCbStackLand().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
                prefs.setStackAiLand(ControlSettings.this.view.getCbStackLand().isSelected());
                try { prefs.save(); } catch (Exception e) { e.printStackTrace(); }
            }
        });

        this.view.getCbRandomFoil().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
                prefs.setRandCFoil(ControlSettings.this.view.getCbRandomFoil().isSelected());
                try { prefs.save(); } catch (Exception e) { e.printStackTrace(); }
            }
        });

        this.view.getCbTextMana().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
                prefs.setCardOverlay(ControlSettings.this.view.getCbTextMana().isSelected());
                try { prefs.save(); } catch (Exception e) { e.printStackTrace(); }
            }
        });
    }

    private void updateSkin() throws Exception {
        String name = view.getLstChooseSkin().getSelectedValue().toString();
        FSkin skin = new FSkin(name);

        prefs.setSkin(name);
        AllZone.setSkin(skin);
        ((GuiTopLevel) AllZone.getDisplay()).getController().changeState(0);
        // This should work, but it doesn't. :|  Doublestrike 15-12-11
        view.getParentView().showSettingsMenu();

        prefs.save();
    }

    /** @param rad0 &emsp; JRadioButton
     * @throws Exception */
    /*
    public void updateStackOffset(JRadioButton rad0) throws Exception {
        StackOffsetType sot = StackOffsetType.valueOf(rad0.getText().toLowerCase());
        Singletons.getModel().getPreferences().setStackOffset(sot);
        prefs.save();
    }
    */

    /** @param rad0 &emsp; JRadioButton
     * @throws Exception */
    public void updateCardSize(JRadioButton rad0) throws Exception {
        CardSizeType cst = CardSizeType.valueOf(rad0.getText());
        Singletons.getModel().getPreferences().setCardSize(cst);
        prefs.save();
    }

    /** @param rad0 &emsp; JRadioButton
     * @throws Exception */
    /*
    public void updateStackSize(JRadioButton rad0) throws Exception {
        Singletons.getModel().getPreferences().setMaxStackSize(Integer.parseInt(rad0.getText()));
        prefs.save();
    }
    */
}
