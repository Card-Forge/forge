package forge.view.home;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.MatteBorder;

import org.apache.commons.lang3.StringUtils;

import net.miginfocom.swing.MigLayout;

import forge.AllZone;
import forge.Singletons;
import forge.control.KeyboardShortcuts;
import forge.control.KeyboardShortcuts.Shortcut;
import forge.control.home.ControlSettings;
import forge.properties.ForgePreferences.FPref;
import forge.properties.ForgeProps;
import forge.properties.NewConstants.Lang.OldGuiNewGame.NewGameText;
import forge.view.GuiTopLevel;
import forge.view.toolbox.FLabel;
import forge.view.toolbox.FList;
import forge.view.toolbox.FScrollPane;
import forge.view.toolbox.FSkin;
import forge.view.toolbox.SubButton;

/** 
 * Assembles swing components for "Settings" mode menu.
 *
 */
@SuppressWarnings("serial")
public class ViewSettings extends JScrollPane {
    private final ControlSettings control;
    private final FSkin skin;
    private final JPanel viewport;
    private final JButton btnReset;
    private HomeTopLevel parentView;

    private JList lstChooseSkin;

    private JCheckBox cbAnte, cbScaleLarger, cbDevMode, cbRemoveSmall, cbRemoveArtifacts,
        cbUploadDraft, cbStackLand, cbRandomFoil, cbTextMana, cbSingletons;

    private JRadioButton radCardTiny, radCardSmaller, radCardSmall,
        radCardMedium, radCardLarge, radCardHuge;

    private final JLabel lblTitleSkin;
    /**
     * 
     * Assembles swing components for "Settings" mode menu.
     * @param v0 &emsp; HomeTopLevel
     */
    public ViewSettings(final HomeTopLevel v0) {
        super(VERTICAL_SCROLLBAR_ALWAYS, HORIZONTAL_SCROLLBAR_AS_NEEDED);

        skin = Singletons.getView().getSkin();
        parentView = v0;
        viewport = new JPanel();
        viewport.setOpaque(false);

        this.setOpaque(false);
        this.getViewport().setOpaque(false);
        this.setBorder(null);
        this.setViewportView(viewport);
        this.getVerticalScrollBar().setUnitIncrement(16);
        viewport.setLayout(new MigLayout("insets 0, gap 0, wrap 2"));

        // Spacing between components is defined here.
        String sectionConstraints = "w 80%!, h 42px!, gap 10% 0 10px 10px, span 2 1";
        String regularConstraints = "w 80%!, h 22px!, gap 10% 0 0 10px, span 2 1";

        // Deck Building Options
        final JLabel lblTitleDecks = new SectionLabel("Deck Building Options");
        viewport.add(lblTitleDecks, sectionConstraints + ", gaptop 2%");

        final JLabel lblRemoveSmall = new NoteLabel("Disables 1/1 and 0/X creatures in generated decks.");
        cbRemoveSmall = new OptionsCheckBox("Remove Small Creatures");
        cbRemoveSmall.setSelected(Singletons.getModel().getPreferences().getPrefBoolean(FPref.DECKGEN_NOSMALL));
        viewport.add(cbRemoveSmall, regularConstraints);
        viewport.add(lblRemoveSmall, regularConstraints);

        final JLabel lblSingletons = new NoteLabel("Disables non-land duplicates in generated decks.");
        cbSingletons = new OptionsCheckBox("Singleton Mode");
        cbSingletons.setSelected(Singletons.getModel().getPreferences().getPrefBoolean(FPref.DECKGEN_SINGLETONS));
        viewport.add(cbSingletons, regularConstraints);
        viewport.add(lblSingletons, regularConstraints);

        final JLabel lblRemoveArtifacts = new NoteLabel("Disables artifact cards in generated decks.");
        cbRemoveArtifacts = new OptionsCheckBox("Remove Artifacts");
        cbRemoveArtifacts.setSelected(Singletons.getModel().getPreferences().getPrefBoolean(FPref.DECKGEN_ARTIFACTS));
        viewport.add(cbRemoveArtifacts, regularConstraints);
        viewport.add(lblRemoveArtifacts, regularConstraints);

        // Gameplay Options
        final JLabel lblTitleUI = new SectionLabel("Gameplay Options");
        viewport.add(lblTitleUI, sectionConstraints);

        cbAnte = new OptionsCheckBox("Play for Ante");
        cbAnte.setSelected(Singletons.getModel().getPreferences().getPrefBoolean(FPref.UI_ANTE));
        final JLabel lblAnte = new NoteLabel("Determines whether or not the game is played for ante.");
        viewport.add(cbAnte, regularConstraints);
        viewport.add(lblAnte, regularConstraints);

        cbUploadDraft = new OptionsCheckBox("Upload Draft Pics");
        cbUploadDraft.setSelected(Singletons.getModel().getPreferences().getPrefBoolean(FPref.UI_UPLOAD_DRAFT));
        final JLabel lblUploadDraft = new NoteLabel("Sends draft picks to Forge servers for analysis, to improve draft AI.");
        viewport.add(cbUploadDraft, regularConstraints);
        viewport.add(lblUploadDraft, regularConstraints);

        cbStackLand = new OptionsCheckBox("Stack AI Land");
        cbStackLand.setSelected(Singletons.getModel().getPreferences().getPrefBoolean(FPref.UI_SMOOTH_LAND));
        final JLabel lblStackLand = new NoteLabel("Minimizes mana lock in AI hands, giving a slight advantage to computer.");
        viewport.add(cbStackLand, regularConstraints);
        viewport.add(lblStackLand, regularConstraints);

        cbDevMode = new OptionsCheckBox(ForgeProps.getLocalized(NewGameText.DEV_MODE));
        cbDevMode.setSelected(Singletons.getModel().getPreferences().getPrefBoolean(FPref.DEV_MODE_ENABLED));
        final JLabel lblDevMode = new NoteLabel("Enables menu with functions for testing during development.");
        viewport.add(cbDevMode, regularConstraints);
        viewport.add(lblDevMode, regularConstraints);

        // Graphic Options
        final JLabel lblTitleGraphics = new SectionLabel("Graphic Options");
        viewport.add(lblTitleGraphics, sectionConstraints);

        lblTitleSkin = new JLabel("Choose Skin");
        lblTitleSkin.setFont(skin.getBoldFont(14));
        lblTitleSkin.setForeground(skin.getColor(FSkin.Colors.CLR_TEXT));
        final JLabel lblNoteSkin = new NoteLabel("Various user-created themes for Forge backgrounds, fonts, and colors.");
        viewport.add(lblTitleSkin, regularConstraints);
        viewport.add(lblNoteSkin, regularConstraints);

        lstChooseSkin = new FList();
        lstChooseSkin.setListData(FSkin.getSkins().toArray(new String[0]));
        lstChooseSkin.setSelectedValue(Singletons.getModel().getPreferences().getPref(FPref.UI_SKIN), true);
        lstChooseSkin.ensureIndexIsVisible(lstChooseSkin.getSelectedIndex());
        viewport.add(new FScrollPane(lstChooseSkin), "h 60px!, w 150px!, gap 10% 0 0 2%, wrap");

        final FLabel lblTitleCardSize = new FLabel("Card Size");
        lblTitleCardSize.setFontStyle(Font.BOLD);
        final JLabel lblCardSize = new NoteLabel("Size of cards in hand and playing field, when possible");
        viewport.add(lblTitleCardSize, regularConstraints);
        viewport.add(lblCardSize, regularConstraints);

        populateCardSizeRadios();

        final JLabel lblRandomFoil = new NoteLabel("Adds foiled effects to random cards.");
        cbRandomFoil = new OptionsCheckBox("Random Foil");
        cbRandomFoil.setSelected(Singletons.getModel().getPreferences().getPrefBoolean(FPref.UI_RANDOM_FOIL));
        viewport.add(cbRandomFoil, regularConstraints);
        viewport.add(lblRandomFoil, regularConstraints);

        final JLabel lblScaleLarger = new NoteLabel("Allows card pictures to be expanded larger than their original size.");
        cbScaleLarger = new OptionsCheckBox("Scale Image Larger");
        cbScaleLarger.setSelected(Singletons.getModel().getPreferences().getPrefBoolean(FPref.UI_SCALE_LARGER));
        viewport.add(cbScaleLarger, regularConstraints);
        viewport.add(lblScaleLarger, regularConstraints);

        final JLabel lblTextMana = new NoteLabel("Overlays each card with basic card-specific information.");
        cbTextMana = new OptionsCheckBox("Text / Mana Overlay");
        cbTextMana.setSelected(Singletons.getModel().getPreferences().getPrefBoolean(FPref.UI_CARD_OVERLAY));
        viewport.add(cbTextMana, regularConstraints);
        viewport.add(lblTextMana, regularConstraints);

        // Keyboard shortcuts
        final JLabel lblShortcuts = new SectionLabel("Keyboard Shortcuts");
        viewport.add(lblShortcuts, sectionConstraints);

        List<Shortcut> shortcuts = ((GuiTopLevel) AllZone.getDisplay()).getController().getShortcuts();

        FLabel lblTemp;
        for (Shortcut s : shortcuts) {
            lblTemp = new FLabel(s.getDescription());
            KeyboardShortcutField ksf = new KeyboardShortcutField(s);
            viewport.add(lblTemp, "w 40%!, h 22px!, gap 10%! 0 0 1%");
            viewport.add(ksf, "w 25%!");
        }

        // Reset button
        final JLabel lblReset = new SectionLabel(" ");
        viewport.add(lblReset, sectionConstraints);

        btnReset = new SubButton("Reset to defaults");
        viewport.add(btnReset, sectionConstraints);

        this.control = new ControlSettings(this);
    }

    private void populateCardSizeRadios() {
        radCardTiny = new CardSizeRadio("Tiny");
        radCardSmaller = new CardSizeRadio("Smaller");
        radCardSmall = new CardSizeRadio("Small");
        radCardMedium = new CardSizeRadio("Medium");
        radCardLarge = new CardSizeRadio("Large");
        radCardHuge = new CardSizeRadio("Huge");

        ButtonGroup group = new ButtonGroup();
        group.add(radCardTiny);
        group.add(radCardSmaller);
        group.add(radCardSmall);
        group.add(radCardMedium);
        group.add(radCardLarge);
        group.add(radCardHuge);

        String constraints = "gapleft 10%, wrap";
        viewport.add(radCardTiny, constraints);
        viewport.add(radCardSmaller, constraints);
        viewport.add(radCardSmall, constraints);
        viewport.add(radCardMedium, constraints);
        viewport.add(radCardLarge, constraints);
        viewport.add(radCardHuge, constraints + ", gapbottom 2%");
    }

    /** Consolidates checkbox styling in one place. */
    private class OptionsCheckBox extends JCheckBox {
        public OptionsCheckBox(final String txt0) {
            super();
            setText(txt0);
            setFont(skin.getBoldFont(12));
            setForeground(skin.getColor(FSkin.Colors.CLR_TEXT));
            setBackground(skin.getColor(FSkin.Colors.CLR_HOVER));
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
        public OptionsRadio(final String txt0) {
            super();
            setText(txt0);
            setFont(skin.getBoldFont(12));
            setForeground(skin.getColor(FSkin.Colors.CLR_TEXT));
            setBackground(skin.getColor(FSkin.Colors.CLR_HOVER));
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

    private class CardSizeRadio extends OptionsRadio {
        public CardSizeRadio(String txt0) {
            super(txt0);
            if (Singletons.getModel().getPreferences().getPref(FPref.UI_CARD_SIZE)
                    .equalsIgnoreCase(txt0)) {
                setSelected(true);
            }

            this.addActionListener(new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent arg0) {
                    try { control.updateCardSize(CardSizeRadio.this); }
                    catch (Exception e) { e.printStackTrace(); }
                }
            });
        }
    }

    /** Consolidates section title label styling in one place. */
    private class SectionLabel extends JLabel {
        public SectionLabel(String txt0) {
            super(txt0);
            setBorder(new MatteBorder(0, 0, 1, 0, skin.getColor(FSkin.Colors.CLR_BORDERS)));
            setHorizontalAlignment(SwingConstants.CENTER);
            setFont(skin.getBoldFont(16));
            setForeground(skin.getColor(FSkin.Colors.CLR_TEXT));
        }
    }

    /** Consolidates notation label styling in one place. */
    private class NoteLabel extends JLabel {
        public NoteLabel(String txt0) {
            super(txt0);
            setFont(skin.getItalicFont(12));
            setForeground(skin.getColor(FSkin.Colors.CLR_TEXT));
        }
    }

    /**
     * A JTextField plus a "codeString" property, that stores keycodes for the
     * shortcut. Also, an action listener that handles translation of keycodes
     * into characters and (dis)assembly of keycode stack.
     */
    public class KeyboardShortcutField extends JTextField {
        private String codeString;

        /**
         * A JTextField plus a "codeString" property, that stores keycodes for
         * the shortcut. Also, an action listener that handles translation of
         * keycodes into characters and (dis)assembly of keycode stack.
         * 
         * @param s0 &emsp; Shortcut object
         */
        public KeyboardShortcutField(final Shortcut s0) {
            super();
            this.setEditable(false);
            this.setFont(skin.getFont(14));
            this.setCodeString(Singletons.getModel().getPreferences().getPref(s0.getPrefKey()));

            this.addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(final KeyEvent e) {
                    KeyboardShortcuts.addKeyCode(e);
                }
            });

            this.addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(final FocusEvent e) {
                    KeyboardShortcutField.this.setBackground(skin.getColor(FSkin.Colors.CLR_ACTIVE));
                }

                @Override
                public void focusLost(final FocusEvent e) {
                    Singletons.getModel().getPreferences().setPref(
                            s0.getPrefKey(), getCodeString());
                    Singletons.getModel().getPreferences().save();
                    s0.attach();
                    KeyboardShortcutField.this.setBackground(Color.white);
                }
            });
        }

        /**
         * Gets the code string.
         * 
         * @return String
         */
        public String getCodeString() {
            return this.codeString;
        }

        /**
         * Sets the code string.
         * 
         * @param s0
         *            &emsp; The new code string (space delimited)
         */
        public void setCodeString(final String s0) {
            if (s0.equals("null")) {
                return;
            }

            this.codeString = s0.trim();

            final List<String> codes = new ArrayList<String>(Arrays.asList(this.codeString.split(" ")));
            final List<String> displayText = new ArrayList<String>();

            for (final String s : codes) {
                if (!s.isEmpty()) {
                    displayText.add(KeyEvent.getKeyText(Integer.valueOf(s)));
                }
            }

            this.setText(StringUtils.join(displayText, ' '));
        }
    }

    /** @return {@link javax.swing.JList} */
    public JList getLstChooseSkin() {
        return lstChooseSkin;
    }

    /** @return {@link javax.swing.JCheckBox} */
    public JCheckBox getCbRemoveSmall() {
        return cbRemoveSmall;
    }

    /** @return {@link javax.swing.JCheckBox} */
    public JCheckBox getCbSingletons() {
        return cbSingletons;
    }

    /** @return {@link javax.swing.JCheckBox} */
    public JCheckBox getCbRemoveArtifacts() {
        return cbRemoveArtifacts;
    }

    /** @return {@link javax.swing.JCheckBox} */
    public JCheckBox getCbUploadDraft() {
        return cbUploadDraft;
    }

    /** @return {@link javax.swing.JCheckBox} */
    public JCheckBox getCbStackLand() {
        return cbStackLand;
    }

    /** @return {@link javax.swing.JCheckBox} */
    public JCheckBox getCbTextMana() {
        return cbTextMana;
    }

    /** @return {@link javax.swing.JCheckBox} */
    public JCheckBox getCbRandomFoil() {
        return cbRandomFoil;
    }

    /** @return {@link javax.swing.JCheckBox} */
    public JCheckBox getCbAnte() {
        return cbAnte;
    }

    /** @return {@link javax.swing.JCheckBox} */
    public JCheckBox getCbScaleLarger() {
        return cbScaleLarger;
    }

    /** @return {@link javax.swing.JCheckBox} */
    public JCheckBox getCbDevMode() {
        return cbDevMode;
    }

    /** @return {@link javax.swing.JLabel} */
    public JLabel getLblTitleSkin() {
        return lblTitleSkin;
    }

    /** @return ControlSettings */
    public ControlSettings getController() {
        return ViewSettings.this.control;
    }

    /** @return {@link forge.view.home.HomeTopLevel} */
    public HomeTopLevel getParentView() {
        return parentView;
    }

    /** @return {@link forge.view.home.HomeTopLevel} */
    public JButton getBtnReset() {
        return btnReset;
    }
}
