package forge.view.home;

import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.MatteBorder;

import net.miginfocom.swing.MigLayout;

import forge.AllZone;
import forge.Singletons;
import forge.control.home.ControlSettings;
import forge.view.toolbox.FSkin;

/** 
 * TODO: Write javadoc for this type.
 *
 */
@SuppressWarnings("serial")
public class ViewSettings extends JScrollPane {
    private ControlSettings control;
    private FSkin skin;
    private JPanel viewport;
    
    SubButton btnChooseSkin;
    OptionsCheckBox cbAnte, cbScaleLarger, cbDevMode;
    /**
     * 
     * TODO: Write javadoc for Constructor.
     * @param v0 &emsp; HomeTopLevel
     */
    public ViewSettings(HomeTopLevel v0) {
        super(VERTICAL_SCROLLBAR_ALWAYS, HORIZONTAL_SCROLLBAR_AS_NEEDED);

        skin = AllZone.getSkin();
        viewport = new JPanel();
        viewport.setOpaque(false);

        this.setOpaque(false);
        this.getViewport().setOpaque(false);
        this.setBorder(null);
        this.setViewportView(viewport);
        viewport.setLayout(new MigLayout("insets 0, gap 0, wrap"));

        String constraints = "w 80%!, gapleft 10%";
        String constraints2 = constraints + ", gapbottom 2%";

        // Deck Building Options
        JLabel lblTitleDecks = new JLabel("Deck Building Options");
        lblTitleDecks.setFont(skin.getFont1().deriveFont(Font.PLAIN, 16));
        lblTitleDecks.setBorder(new MatteBorder(0, 0, 1, 0, skin.getColor("borders")));
        lblTitleDecks.setForeground(skin.getColor("text"));
        viewport.add(lblTitleDecks, constraints2 + ", gaptop 2%");

        OptionsCheckBox cbRemoveSmall = new OptionsCheckBox("Remove Small Creatures");
        NoteLabel lblRemoveSmall = new NoteLabel("Disables 1/1 and 0/X creatures in generated decks.");
        viewport.add(cbRemoveSmall, constraints);
        viewport.add(lblRemoveSmall, constraints2);

        OptionsCheckBox cbSingletons = new OptionsCheckBox("Singleton Mode");
        NoteLabel lblSingletons = new NoteLabel("Disables non-land duplicates in generated decks.");
        viewport.add(cbSingletons, constraints);
        viewport.add(lblSingletons, constraints2);

        OptionsCheckBox cbRemoveArtifacts = new OptionsCheckBox("Remove Artifacts");
        NoteLabel lblRemoveArtifacts = new NoteLabel("Disables artifact cards in generated decks.");
        viewport.add(cbRemoveArtifacts, constraints);
        viewport.add(lblRemoveArtifacts, constraints2);

        // Gameplay Options
        JLabel lblTitleUI = new JLabel("Gameplay Options");
        lblTitleUI.setFont(skin.getFont1().deriveFont(Font.PLAIN, 16));
        lblTitleUI.setBorder(new MatteBorder(1, 0, 1, 0, skin.getColor("borders")));
        lblTitleUI.setForeground(skin.getColor("text"));
        viewport.add(lblTitleUI, constraints2);

        OptionsCheckBox cbOldUI = new OptionsCheckBox("Old UI");
        NoteLabel lblOldUI = new NoteLabel("Use the user interface from Beta 1.1.9.");
        viewport.add(cbOldUI, constraints);
        viewport.add(lblOldUI, constraints2);

        //slapshot5 - this is in progress, but I need to check this in for some other changes.
        this.cbAnte = new OptionsCheckBox("Play for Ante");
        //this.cbAnte.setSelected(Singletons.getModel().getPreferences().isPlayForAnte());
        //NoteLabel lblAnte = new NoteLabel("Each player antes a card and the game is for keeps.");
        /*
        viewport.add(cbAnte, constraints);
        viewport.add(lblAnte, constraints2);
        */

        OptionsCheckBox cbUploadDraft = new OptionsCheckBox("Upload Draft Pics");
        NoteLabel lblUploadDraft = new NoteLabel("Sends draft picks to Forge servers for analysis, to improve draft AI.");
        viewport.add(cbUploadDraft, constraints);
        viewport.add(lblUploadDraft, constraints2);

        OptionsCheckBox cbStackLand = new OptionsCheckBox("Stack AI Land");
        NoteLabel lblStackLand = new NoteLabel("Minimizes mana lock in AI hands, giving a slight advantage to computer.");
        viewport.add(cbStackLand, constraints);
        viewport.add(lblStackLand, constraints2);

        cbDevMode = new OptionsCheckBox("Developer Mode");
        this.cbDevMode.setSelected(Singletons.getModel().getPreferences().isDeveloperMode());
        NoteLabel lblDevMode = new NoteLabel("Enables menu with functions for testing during development.");
        viewport.add(cbDevMode, constraints);
        viewport.add(lblDevMode, constraints2);

        SubButton btnStackSize = new SubButton("Stack Size: 12");
        NoteLabel lblStackSize = new NoteLabel("Specify maximum number of spells allowed on stack.");
        viewport.add(btnStackSize, "gapleft 10%, h 25px!, w 150px!, gapbottom 2px");
        viewport.add(lblStackSize, constraints2);

        SubButton btnStackOffset = new SubButton("Stack Offset: Large");
        NoteLabel lblStackOffset = new NoteLabel("Not sure what this does...can someone who does update this text");
        viewport.add(btnStackOffset, "gapleft 10%, h 25px!, w 150px!, gapbottom 2px");
        viewport.add(lblStackOffset, constraints2);

        // Graphic Options
        JLabel lblTitleGraphics = new JLabel("Graphic Options");
        lblTitleGraphics.setFont(skin.getFont1().deriveFont(Font.PLAIN, 16));
        lblTitleGraphics.setBorder(new MatteBorder(1, 0, 1, 0, skin.getColor("borders")));
        lblTitleGraphics.setForeground(skin.getColor("text"));
        viewport.add(lblTitleGraphics, constraints2);

        btnChooseSkin = new SubButton("Choose Skin");
        viewport.add(btnChooseSkin, "gapleft 10%, h 25px!, w 150px!, gapbottom 2px");

        OptionsCheckBox cbRandomFoil = new OptionsCheckBox("Random Foil");
        NoteLabel lblRandomFoil = new NoteLabel("Adds foiled effects to random cards.");
        viewport.add(cbRandomFoil, constraints);
        viewport.add(lblRandomFoil, constraints2);

        cbScaleLarger = new OptionsCheckBox("Scale image larger");
        this.cbScaleLarger.setSelected(Singletons.getModel().getPreferences().isPlayForAnte());
        NoteLabel lblScaleLarger = new NoteLabel("Not sure what this does...can someone who does update this text");
        viewport.add(cbScaleLarger, constraints);
        viewport.add(lblScaleLarger, constraints2);

        OptionsCheckBox cbTextMana = new OptionsCheckBox("Text / Mana Overlay");
        NoteLabel lblTextMana = new NoteLabel("Overlays each card with basic card-specific information.");
        viewport.add(cbTextMana, constraints);
        viewport.add(lblTextMana, constraints2);
        
        ViewSettings.this.control = new ControlSettings(this);
    }

    /** Consolidates checkbox styling in one place. */
    private class OptionsCheckBox extends JCheckBox {
        public OptionsCheckBox(String txt0) {
            super();
            setText(txt0);
            setFont(skin.getFont1().deriveFont(Font.BOLD, 12));
            setForeground(skin.getColor("text"));
            setBackground(skin.getColor("hover"));
            setOpaque(false);

            this.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    setOpaque(true);
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    setOpaque(false);
                }
            });
        }
    }

    private class NoteLabel extends JLabel {
        public NoteLabel(String txt0) {
            super(txt0);
            setFont(skin.getFont1().deriveFont(Font.ITALIC, 12));
            setForeground(skin.getColor("text"));
        }
    }
    
    public SubButton getBtnChooseSkin() {
        return btnChooseSkin;
    }
    
    public JCheckBox getCbAnte() {
        return cbAnte;
    }
    
    public JCheckBox getCbScaleLarger() {
        return cbScaleLarger;
    }
    
    public JCheckBox getCbDevMode() {
        return cbDevMode;
    }
    
    public ControlSettings getController() {
        return ViewSettings.this.control;
    }
}
