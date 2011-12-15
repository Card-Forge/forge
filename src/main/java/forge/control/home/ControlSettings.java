package forge.control.home;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JRadioButton;

import forge.AllZone;
import forge.Singletons;
import forge.error.ErrorViewer;
import forge.gui.ListChooser;
import forge.properties.ForgePreferences;
import forge.properties.ForgePreferences.CardSizeType;
import forge.properties.ForgePreferences.StackOffsetType;
import forge.view.home.ViewSettings;
import forge.view.toolbox.FSkin;

/** 
 * Controls logic and listeners for Settings panel of the home screen.
 *
 */
public class ControlSettings {
    private ViewSettings view;

    /**
     * 
     * Controls logic and listeners for Settings panel of the home screen.
     * 
     * @param v0 &emsp; ViewSettings
     */
    public ControlSettings(ViewSettings v0) {
        this.view = v0;
        addListeners();
    }

    /** @return ViewSettings */
    public ViewSettings getView() {
        return view;
    }

    /** */
    public void addListeners() {
        /*this.view.getLstChooseSkin().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
                doChooseSkin();
            }
        });*/

        //slapshot5 - work in progress, but I need to check this file in for other changes.
        /*
        this.view.getCbAnte().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
                Singletons.getModel().getPreferences()
                        .setPlayForAnte(ControlSettings.this.view.getCbAnte().isSelected());
            }
        });
        */
        this.view.getCbScaleLarger().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
                Singletons.getModel().getPreferences()
                        .setScaleLargerThanOriginal(ControlSettings.this.view.getCbScaleLarger().isSelected());
            }
        });

        this.view.getCbDevMode().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
                Singletons.getModel().getPreferences()
                        .setDeveloperMode(ControlSettings.this.view.getCbDevMode().isSelected());
            }
        });

        this.view.getCbRemoveSmall().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
                Singletons.getModel().getPreferences()
                    .setDeckGenRmvSmall(ControlSettings.this.view.getCbRemoveSmall().isSelected());
            }
        });

        this.view.getCbRemoveArtifacts().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
                Singletons.getModel().getPreferences()
                    .setDeckGenRmvArtifacts(ControlSettings.this.view.getCbRemoveArtifacts().isSelected());
            }
        });

        this.view.getCbSingletons().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
                Singletons.getModel().getPreferences()
                    .setDeckGenSingletons(ControlSettings.this.view.getCbSingletons().isSelected());
            }
        });

        this.view.getCbOldUI().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
                Singletons.getModel().getPreferences()
                    .setOldGui(ControlSettings.this.view.getCbOldUI().isSelected());
            }
        });

        this.view.getCbUploadDraft().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
                Singletons.getModel().getPreferences()
                    .setUploadDraftAI(ControlSettings.this.view.getCbUploadDraft().isSelected());
            }
        });

        this.view.getCbStackLand().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
                Singletons.getModel().getPreferences()
                    .setStackAiLand(ControlSettings.this.view.getCbStackLand().isSelected());
            }
        });

        this.view.getCbRandomFoil().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
                Singletons.getModel().getPreferences()
                    .setRandCFoil(ControlSettings.this.view.getCbRandomFoil().isSelected());
            }
        });

        this.view.getCbTextMana().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
                Singletons.getModel().getPreferences()
                    .setCardOverlay(ControlSettings.this.view.getCbTextMana().isSelected());
            }
        });
    }

    private void doChooseSkin() {
        final ListChooser<String> ch = new ListChooser<String>("Choose a skin", 0, 1, FSkin.getSkins());
        if (ch.show()) {
            try {
                final String name = ch.getSelectedValue();
                final int index = ch.getSelectedIndex();
                if (index == -1) {
                    return;
                }
                ForgePreferences preferences = Singletons.getModel().getPreferences();
                preferences.setSkin(name);
                final FSkin skin = new FSkin(name);
                AllZone.setSkin(skin);
                preferences.save();

            } catch (final Exception ex) {
                ErrorViewer.showError(ex);
            }
        }
    }
    
    private void updateSkinList() {
        
    }

    /** @param rad0 &emsp; JRadioButton */
    public void updateStackOffset(JRadioButton rad0) {
        StackOffsetType sot = StackOffsetType.valueOf(rad0.getText());
        Singletons.getModel().getPreferences().setStackOffset(sot);
    }

    /** @param rad0 &emsp; JRadioButton */
    public void updateCardSize(JRadioButton rad0) {
        CardSizeType cst = CardSizeType.valueOf(rad0.getText());
        Singletons.getModel().getPreferences().setCardSize(cst);
    }

    /** @param rad0 &emsp; JRadioButton */
    public void updateStackSize(JRadioButton rad0) {
        Singletons.getModel().getPreferences().setMaxStackSize(Integer.parseInt(rad0.getText()));
    }
}
