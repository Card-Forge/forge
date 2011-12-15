package forge.view.home;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.AbstractAction;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
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

    private JList lstChooseSkin;
    private String spacer;

    private JCheckBox cbAnte, cbScaleLarger, cbDevMode, cbRemoveSmall, cbRemoveArtifacts,
        cbOldUI, cbUploadDraft, cbStackLand, cbRandomFoil, cbTextMana,
        cbSingletons;

    private JRadioButton radStack3, radStack4, radStack5, radStack6, radStack7,
        radStack8, radStack9, radStack10, radStack11, radStack12;

    private JRadioButton radOffsetTiny, radOffsetSmall, radOffsetMedium, radOffsetLarge;

    private JRadioButton radCardTiny, radCardSmaller, radCardSmall,
        radCardMedium, radCardLarge, radCardHuge;
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
        this.getVerticalScrollBar().setUnitIncrement(16);
        viewport.setLayout(new MigLayout("insets 0, gap 0, wrap"));

        // Spacing between components is defined here.
        spacer = ", gapbottom 1%";
        String constraints = "w 80%!, gapleft 10%";
        String constraints2 = constraints + spacer;

        // Deck Building Options
        JLabel lblTitleDecks = new JLabel("Deck Building Options");
        lblTitleDecks.setFont(skin.getFont1().deriveFont(Font.PLAIN, 16));
        lblTitleDecks.setBorder(new MatteBorder(0, 0, 1, 0, skin.getColor("borders")));
        lblTitleDecks.setForeground(skin.getColor("text"));
        viewport.add(lblTitleDecks, constraints2 + ", gaptop 2%");

        cbRemoveSmall = new OptionsCheckBox("Remove Small Creatures");
        cbRemoveSmall.setSelected(Singletons.getModel().getPreferences().isDeckGenRmvSmall());
        JLabel lblRemoveSmall = new NoteLabel("Disables 1/1 and 0/X creatures in generated decks.");
        viewport.add(cbRemoveSmall, constraints);
        viewport.add(lblRemoveSmall, constraints2);

        cbSingletons = new OptionsCheckBox("Singleton Mode");
        cbSingletons.setSelected(Singletons.getModel().getPreferences().isDeckGenSingletons());
        JLabel lblSingletons = new NoteLabel("Disables non-land duplicates in generated decks.");
        viewport.add(cbSingletons, constraints);
        viewport.add(lblSingletons, constraints2);

        cbRemoveArtifacts = new OptionsCheckBox("Remove Artifacts");
        cbRemoveArtifacts.setSelected(Singletons.getModel().getPreferences().isDeckGenRmvArtifacts());
        JLabel lblRemoveArtifacts = new NoteLabel("Disables artifact cards in generated decks.");
        viewport.add(cbRemoveArtifacts, constraints);
        viewport.add(lblRemoveArtifacts, constraints2);

        // Gameplay Options
        JLabel lblTitleUI = new JLabel("Gameplay Options");
        lblTitleUI.setFont(skin.getFont1().deriveFont(Font.PLAIN, 16));
        lblTitleUI.setBorder(new MatteBorder(1, 0, 1, 0, skin.getColor("borders")));
        lblTitleUI.setForeground(skin.getColor("text"));
        viewport.add(lblTitleUI, constraints2);

        cbOldUI = new OptionsCheckBox("Old UI");
        cbOldUI.setSelected(Singletons.getModel().getPreferences().isOldGui());
        JLabel lblOldUI = new NoteLabel("Use the user interface from Beta 1.1.9.");
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

        cbUploadDraft = new OptionsCheckBox("Upload Draft Pics");
        cbUploadDraft.setSelected(Singletons.getModel().getPreferences().isUploadDraftAI());
        JLabel lblUploadDraft = new NoteLabel("Sends draft picks to Forge servers for analysis, to improve draft AI.");
        viewport.add(cbUploadDraft, constraints);
        viewport.add(lblUploadDraft, constraints2);

        cbStackLand = new OptionsCheckBox("Stack AI Land");
        cbStackLand.setSelected(Singletons.getModel().getPreferences().isStackAiLand());
        JLabel lblStackLand = new NoteLabel("Minimizes mana lock in AI hands, giving a slight advantage to computer.");
        viewport.add(cbStackLand, constraints);
        viewport.add(lblStackLand, constraints2);

        cbDevMode = new OptionsCheckBox("Developer Mode");
        cbDevMode.setSelected(Singletons.getModel().getPreferences().isDeveloperMode());
        this.cbDevMode.setSelected(Singletons.getModel().getPreferences().isDeveloperMode());
        JLabel lblDevMode = new NoteLabel("Enables menu with functions for testing during development.");
        viewport.add(cbDevMode, constraints);
        viewport.add(lblDevMode, constraints2);

        JLabel lblTitleStack = new TitleLabel("Stack Size");
        JLabel lblStackSize = new NoteLabel("Specify maximum number of spells allowed on stack.");
        viewport.add(lblTitleStack, "gapleft 10%, h 25px!, w 150px!, gapbottom 2px");
        viewport.add(lblStackSize, constraints);

        populateStackRadios();

        JLabel lblTitleOffset = new TitleLabel("Stack Offset");
        JLabel lblStackOffset = new NoteLabel("Not sure what this does...can someone who does update this text");
        viewport.add(lblTitleOffset, "gapleft 10%, h 25px!, w 150px!, gapbottom 2px");
        viewport.add(lblStackOffset, constraints);

        populateOffsetRadios();

        // Graphic Options
        JLabel lblTitleGraphics = new JLabel("Graphic Options");
        lblTitleGraphics.setFont(skin.getFont1().deriveFont(Font.PLAIN, 16));
        lblTitleGraphics.setBorder(new MatteBorder(1, 0, 1, 0, skin.getColor("borders")));
        lblTitleGraphics.setForeground(skin.getColor("text"));
        viewport.add(lblTitleGraphics, constraints2);

        JLabel lblTitleSkin = new TitleLabel("Choose Skin");
        JLabel lblNoteSkin = new NoteLabel("Various user-created themes for Forge backgrounds, fonts, and colors.");
        viewport.add(lblTitleSkin, "gapleft 10%, h 25px!, w 150px!, gapbottom 2px");
        viewport.add(lblNoteSkin, constraints + ", gapbottom 2px");

        lstChooseSkin = new JList();
        viewport.add(new JScrollPane(lstChooseSkin), "gapleft 10%, h 60px!, w 150px!" + spacer);

        JLabel lblTitleCardSize = new TitleLabel("Card Size");
        JLabel lblCardSize = new NoteLabel("Size of cards in hand and playing field, when possible");
        viewport.add(lblTitleCardSize, "gapleft 10%, h 25px!, w 150px!, gapbottom 2px");
        viewport.add(lblCardSize, constraints);

        populateCardSizeRadios();

        cbRandomFoil = new OptionsCheckBox("Random Foil");
        cbRandomFoil.setSelected(Singletons.getModel().getPreferences().isRandCFoil());
        JLabel lblRandomFoil = new NoteLabel("Adds foiled effects to random cards.");
        viewport.add(cbRandomFoil, constraints);
        viewport.add(lblRandomFoil, constraints2);

        cbScaleLarger = new OptionsCheckBox("Scale image larger");
        this.cbScaleLarger.setSelected(Singletons.getModel().getPreferences().isScaleLargerThanOriginal());
        JLabel lblScaleLarger = new NoteLabel("Not sure what this does...can someone who does update this text");
        viewport.add(cbScaleLarger, constraints);
        viewport.add(lblScaleLarger, constraints2);

        cbTextMana = new OptionsCheckBox("Text / Mana Overlay");
        cbTextMana.setSelected(Singletons.getModel().getPreferences().isCardOverlay());
        JLabel lblTextMana = new NoteLabel("Overlays each card with basic card-specific information.");
        viewport.add(cbTextMana, constraints);
        viewport.add(lblTextMana, constraints2);

        ViewSettings.this.control = new ControlSettings(this);
    }

    private void populateStackRadios() {
        JPanel radioContainer = new JPanel();
        radioContainer.setOpaque(false);
        radioContainer.setLayout(new MigLayout("insets 0, gap 0, wrap 2"));

        radStack3 = new StackSizeRadio("3");
        radStack4 = new StackSizeRadio("4");
        radStack5 = new StackSizeRadio("5");
        radStack6 = new StackSizeRadio("6");
        radStack7 = new StackSizeRadio("7");
        radStack8 = new StackSizeRadio("8");
        radStack9 = new StackSizeRadio("9");
        radStack10 = new StackSizeRadio("10");
        radStack11 = new StackSizeRadio("11");
        radStack12 = new StackSizeRadio("12");

        String constraints = "w 50px!, h 25px!";
        radioContainer.add(radStack3, constraints);
        radioContainer.add(radStack4, constraints);
        radioContainer.add(radStack5, constraints);
        radioContainer.add(radStack6, constraints);
        radioContainer.add(radStack7, constraints);
        radioContainer.add(radStack8, constraints);
        radioContainer.add(radStack9, constraints);
        radioContainer.add(radStack10, constraints);
        radioContainer.add(radStack11, constraints);
        radioContainer.add(radStack12, constraints);

        viewport.add(radioContainer, "gapleft 10%" + spacer);
    }

    private void populateOffsetRadios() {
        radOffsetTiny = new StackOffsetRadio("Tiny");
        radOffsetSmall = new StackOffsetRadio("Small");
        radOffsetMedium = new StackOffsetRadio("Medium");
        radOffsetLarge = new StackOffsetRadio("Large");

        String constraints = "gapleft 10%, wrap";
        viewport.add(radOffsetTiny, constraints);
        viewport.add(radOffsetSmall, constraints);
        viewport.add(radOffsetMedium, constraints);
        viewport.add(radOffsetLarge, constraints + spacer);
    }

    private void populateCardSizeRadios() {
        radCardTiny = new CardSizeRadio("Tiny");
        radCardSmaller = new CardSizeRadio("Smaller");
        radCardSmall = new CardSizeRadio("Small");
        radCardMedium = new CardSizeRadio("Medium");
        radCardLarge = new CardSizeRadio("Large");
        radCardHuge = new CardSizeRadio("Huge");

        String constraints = "gapleft 10%, wrap";
        viewport.add(radCardTiny, constraints);
        viewport.add(radCardSmaller, constraints);
        viewport.add(radCardSmall, constraints);
        viewport.add(radCardMedium, constraints);
        viewport.add(radCardLarge, constraints);
        viewport.add(radCardHuge, constraints + spacer);
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

    /** Consolidates checkbox styling in one place. */
    private class OptionsRadio extends JRadioButton {
        public OptionsRadio(String txt0) {
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

    private class StackSizeRadio extends OptionsRadio {
        public StackSizeRadio(String txt0) {
            super(txt0);

            if(Singletons.getModel().getPreferences().getMaxStackSize() == Integer.parseInt(txt0)) {
                setSelected(true);
            }

            this.addActionListener(new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent arg0) {
                    control.updateStackSize(StackSizeRadio.this);
                }
            });
        }
    }

    private class StackOffsetRadio extends OptionsRadio {
        public StackOffsetRadio(String txt0) {
            super(txt0);
            if(Singletons.getModel().getPreferences().getStackOffset().toString() == txt0) {
                setSelected(true);
            }

            this.addActionListener(new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent arg0) {
                    control.updateStackOffset(StackOffsetRadio.this);
                }
            });
        }
    }

    private class CardSizeRadio extends OptionsRadio {
        public CardSizeRadio(String txt0) {
            super(txt0);
            if(Singletons.getModel().getPreferences().getCardSize().toString() == txt0) {
                setSelected(true);
            }

            this.addActionListener(new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent arg0) {
                    control.updateCardSize(CardSizeRadio.this);
                }
            });
        }
    }

    /** Consolidates notation label styling in one place. */
    private class NoteLabel extends JLabel {
        public NoteLabel(String txt0) {
            super(txt0);
            setFont(skin.getFont1().deriveFont(Font.ITALIC, 12));
            setForeground(skin.getColor("text"));
        }
    }

    /** Consolidates title label styling in one place. */
    private class TitleLabel extends JLabel {
        public TitleLabel(String txt0) {
            super(txt0);
            setFont(skin.getFont1().deriveFont(Font.BOLD, 12));
            setForeground(skin.getColor("text"));
        }
    }

    /** @return JList */
    public JList getLstChooseSkin() {
        return lstChooseSkin;
    }

    /** @return JCheckBox */
    public JCheckBox getCbRemoveSmall() {
        return cbRemoveSmall;
    }

    /** @return JCheckBox */
    public JCheckBox getCbSingletons() {
        return cbSingletons;
    }

    /** @return JCheckBox */
    public JCheckBox getCbRemoveArtifacts() {
        return cbRemoveArtifacts;
    }

    /** @return JCheckBox */
    public JCheckBox getCbOldUI() {
        return cbOldUI;
    }

    /** @return JCheckBox */
    public JCheckBox getCbUploadDraft() {
        return cbUploadDraft;
    }

    /** @return JCheckBox */
    public JCheckBox getCbStackLand() {
        return cbStackLand;
    }

    /** @return JCheckBox */
    public JCheckBox getCbTextMana() {
        return cbTextMana;
    }

    /** @return JCheckBox */
    public JCheckBox getCbRandomFoil() {
        return cbRandomFoil;
    }

    /** @return JCheckBox */
    public JCheckBox getCbAnte() {
        return cbAnte;
    }

    /** @return JCheckBox */
    public JCheckBox getCbScaleLarger() {
        return cbScaleLarger;
    }

    /** @return JCheckBox */
    public JCheckBox getCbDevMode() {
        return cbDevMode;
    }

    /** @return ControlSettings */
    public ControlSettings getController() {
        return ViewSettings.this.control;
    }
}
