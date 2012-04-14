package forge.gui.home.settings;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.MatteBorder;

import net.miginfocom.swing.MigLayout;

import org.apache.commons.lang3.StringUtils;

import forge.Singletons;
import forge.control.KeyboardShortcuts;
import forge.control.KeyboardShortcuts.Shortcut;
import forge.gui.home.EMenuGroup;
import forge.gui.home.EMenuItem;
import forge.gui.home.ICSubmenu;
import forge.gui.home.IVSubmenu;
import forge.gui.toolbox.FLabel;
import forge.gui.toolbox.FList;
import forge.gui.toolbox.FScrollPane;
import forge.gui.toolbox.FSkin;
import forge.properties.ForgeProps;
import forge.properties.NewConstants.Lang.OldGuiNewGame.NewGameText;

/** 
 * Assembles Swing components of preferences submenu singleton.
 *
 * <br><br><i>(V at beginning of class name denotes a view class.)</i>
 */
public enum VSubmenuPreferences implements IVSubmenu {
    /** */
    SINGLETON_INSTANCE;

    /** */
    private final JPanel pnl = new JPanel();

    private final FLabel btnReset = new FLabel.Builder().opaque(true)
            .hoverable(true).text("Reset to defaults").build();

    private final FLabel lblTitleSkin = new FLabel.Builder()
        .text("Choose Skin").fontScaleAuto(false).fontStyle(Font.BOLD).fontSize(14).build();

    private final JList lstChooseSkin = new FList();
    private final FLabel lblChooseSkin = new FLabel.Builder().fontSize(12).fontStyle(Font.ITALIC)
            .text("Various user-created themes for Forge backgrounds, fonts, and colors.")
            .fontAlign(SwingConstants.LEFT).build();
    private final JScrollPane scrChooseSkin = new FScrollPane(lstChooseSkin);

    private final JCheckBox cbRemoveSmall = new OptionsCheckBox("Remove Small Creatures");
    private final JCheckBox cbSingletons = new OptionsCheckBox("Singleton Mode");
    private final JCheckBox cbRemoveArtifacts = new OptionsCheckBox("Remove Artifacts");
    private final JCheckBox cbAnte = new OptionsCheckBox("Play for Ante");
    private final JCheckBox cbUploadDraft = new OptionsCheckBox("Upload Draft Pics");
    private final JCheckBox cbStackLand = new OptionsCheckBox("Stack AI Land");
    private final JCheckBox cbManaBurn = new OptionsCheckBox("Mana Burn");
    private final JCheckBox cbDevMode = new OptionsCheckBox(ForgeProps.getLocalized(NewGameText.DEV_MODE));
    private final JCheckBox cbTextMana = new OptionsCheckBox("Text / Mana Overlay");
    private final JCheckBox cbScaleLarger = new OptionsCheckBox("Scale Image Larger");
    private final JCheckBox cbRandomFoil = new OptionsCheckBox("Random Foil");

    /* (non-Javadoc)
     * @see forge.view.home.IViewSubmenu#populate()
     */
    @Override
    public void populate() {
        pnl.removeAll();
        pnl.setOpaque(false);
        pnl.setLayout(new MigLayout("insets 0, gap 0"));

        final JPanel pnlPrefs = new JPanel();
        pnlPrefs.setOpaque(false);
        pnlPrefs.setLayout(new MigLayout("insets 0, gap 0, wrap 2"));

        // Spacing between components is defined here.
        final String sectionConstraints = "w 80%!, h 42px!, gap 10% 0 10px 10px, span 2 1";
        final String regularConstraints = "w 80%!, h 22px!, gap 10% 0 0 10px, span 2 1";

        // Deck building options
        pnlPrefs.add(new SectionLabel("Deck Building Options"), sectionConstraints + ", gaptop 2%");

        pnlPrefs.add(cbRemoveSmall, regularConstraints);
        pnlPrefs.add(new NoteLabel("Disables 1/1 and 0/X creatures in generated decks."), regularConstraints);

        pnlPrefs.add(cbSingletons, regularConstraints);
        pnlPrefs.add(new NoteLabel("Disables non-land duplicates in generated decks."), regularConstraints);

        pnlPrefs.add(cbRemoveArtifacts, regularConstraints);
        pnlPrefs.add(new NoteLabel("Disables artifact cards in generated decks."), regularConstraints);

        // Gameplay Options
        pnlPrefs.add(new SectionLabel("Gameplay Options"), sectionConstraints);

        pnlPrefs.add(cbAnte, regularConstraints);
        pnlPrefs.add(new NoteLabel("Determines whether or not the game is played for ante."), regularConstraints);

        pnlPrefs.add(cbUploadDraft, regularConstraints);
        pnlPrefs.add(new NoteLabel("Sends draft picks to Forge servers for analysis, to improve draft AI."), regularConstraints);

        pnlPrefs.add(cbStackLand, regularConstraints);
        pnlPrefs.add(new NoteLabel("Minimizes mana lock in AI hands, giving a slight advantage to computer."), regularConstraints);

        pnlPrefs.add(cbManaBurn, regularConstraints);
        pnlPrefs.add(new NoteLabel("Play with mana burn (from pre-Magic 2010 rules)."), regularConstraints);

        pnlPrefs.add(cbDevMode, regularConstraints);
        pnlPrefs.add(new NoteLabel("Enables menu with functions for testing during development."), regularConstraints);

        // Graphic Options
        pnlPrefs.add(new SectionLabel("Graphic Options"), sectionConstraints);

        pnlPrefs.add(lblTitleSkin, regularConstraints);
        pnlPrefs.add(lblChooseSkin, regularConstraints);
        pnlPrefs.add(scrChooseSkin, "h 200px!, w 200px!, gap 10% 0 0 2%, wrap");

        pnlPrefs.add(cbRandomFoil, regularConstraints);
        pnlPrefs.add(new NoteLabel("Adds foiled effects to random cards."), regularConstraints);

        pnlPrefs.add(cbScaleLarger, regularConstraints);
        pnlPrefs.add(new NoteLabel("Allows card pictures to be expanded larger than their original size."), regularConstraints);

        pnlPrefs.add(cbTextMana, regularConstraints);
        pnlPrefs.add(new NoteLabel("Overlays each card with basic card-specific information."), regularConstraints);

        // Keyboard shortcuts
        final JLabel lblShortcuts = new SectionLabel("Keyboard Shortcuts");
        pnlPrefs.add(lblShortcuts, sectionConstraints);

        final List<Shortcut> shortcuts = Singletons.getControl().getShortcuts();

        for (final Shortcut s : shortcuts) {
            pnlPrefs.add(new FLabel.Builder().text(s.getDescription())
                    .fontAlign(SwingConstants.RIGHT).build(), "w 50%!, h 22px!, gap 0 2% 0 1%");
            pnlPrefs.add(new KeyboardShortcutField(s), "w 25%!");
        }

        // Reset button
        pnlPrefs.add(new SectionLabel(" "), sectionConstraints);
        pnlPrefs.add(btnReset, sectionConstraints);

        pnl.add(new FScrollPane(pnlPrefs,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED), "w 100%!, h 100%!");
    }

    /* (non-Javadoc)
     * @see forge.view.home.IViewSubmenu#getGroup()
     */
    @Override
    public EMenuGroup getGroupEnum() {
        return EMenuGroup.SETTINGS;
    }

    /* (non-Javadoc)
     * @see forge.view.home.IViewSubmenu#getPanel()
     */
    @Override
    public JPanel getPanel() {
        return pnl;
    }

    /* (non-Javadoc)
     * @see forge.gui.home.IVSubmenu#getMenuTitle()
     */
    @Override
    public String getMenuTitle() {
        return "Preferences";
    }

    /* (non-Javadoc)
     * @see forge.gui.home.IVSubmenu#getMenuName()
     */
    @Override
    public String getItemEnum() {
        return EMenuItem.SETTINGS_PREFS.toString();
    }

    /* (non-Javadoc)
     * @see forge.gui.home.IVSubmenu#getControl()
     */
    @Override
    public ICSubmenu getControl() {
        return CSubmenuPreferences.SINGLETON_INSTANCE;
    }

    /** Consolidates checkbox styling in one place. */
    @SuppressWarnings("serial")
    private class OptionsCheckBox extends JCheckBox {
        public OptionsCheckBox(final String txt0) {
            super();
            setText(txt0);
            setFont(FSkin.getBoldFont(12));
            setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT));
            setBackground(FSkin.getColor(FSkin.Colors.CLR_HOVER));
            setOpaque(false);

            this.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(final MouseEvent evt) {
                    setOpaque(true);
                }

                @Override
                public void mouseExited(final MouseEvent evt) {
                    setOpaque(false);
                }
            });
        }
    }

    /** Consolidates section title label styling in one place. */
    @SuppressWarnings("serial")
    private class SectionLabel extends JLabel {
        public SectionLabel(final String txt0) {
            super(txt0);
            setBorder(new MatteBorder(0, 0, 1, 0, FSkin.getColor(FSkin.Colors.CLR_BORDERS)));
            setHorizontalAlignment(SwingConstants.CENTER);
            setFont(FSkin.getBoldFont(16));
            setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT));
        }
    }

    /** Consolidates notation label styling in one place. */
    @SuppressWarnings("serial")
    private class NoteLabel extends JLabel {
        public NoteLabel(final String txt0) {
            super(txt0);
            setFont(FSkin.getItalicFont(12));
            setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT));
        }
    }

    /**
     * A JTextField plus a "codeString" property, that stores keycodes for the
     * shortcut. Also, an action listener that handles translation of keycodes
     * into characters and (dis)assembly of keycode stack.
     */
    @SuppressWarnings("serial")
    public class KeyboardShortcutField extends JTextField {
        private String codeString;

        /**
         * A JTextField plus a "codeString" property, that stores keycodes for
         * the shortcut. Also, an action listener that handles translation of
         * keycodes into characters and (dis)assembly of keycode stack.
         * 
         * @param shortcut0 &emsp; Shortcut object
         */
        public KeyboardShortcutField(final Shortcut shortcut0) {
            super();
            this.setEditable(false);
            this.setFont(FSkin.getFont(14));
            this.setCodeString(Singletons.getModel().getPreferences().getPref(shortcut0.getPrefKey()));

            this.addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(final KeyEvent evt) {
                    KeyboardShortcuts.addKeyCode(evt);
                }
            });

            this.addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(final FocusEvent evt) {
                    KeyboardShortcutField.this.setBackground(FSkin.getColor(FSkin.Colors.CLR_ACTIVE));
                }

                @Override
                public void focusLost(final FocusEvent evt) {
                    Singletons.getModel().getPreferences().setPref(
                            shortcut0.getPrefKey(), getCodeString());
                    Singletons.getModel().getPreferences().save();
                    shortcut0.attach();
                    KeyboardShortcutField.this.setBackground(Color.white);
                }
            });
        }

        /**
         * Gets the code string.
         * 
         * @return String
         */
        public final String getCodeString() {
            return this.codeString;
        }

        /**
         * Sets the code string.
         * 
         * @param str0
         *            &emsp; The new code string (space delimited)
         */
        public final void setCodeString(final String str0) {
            if ("null".equals(str0)) {
                return;
            }

            this.codeString = str0.trim();

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
    public final JList getLstChooseSkin() {
        return lstChooseSkin;
    }

    /** @return {@link forge.gui.toolbox.FLabel} */
    public final FLabel getLblChooseSkin() {
        return lblChooseSkin;
    }

    /** @return {@link javax.swing.JScrollPane} */
    public final JScrollPane getScrChooseSkin() {
        return scrChooseSkin;
    }

    /** @return {@link javax.swing.JCheckBox} */
    public final JCheckBox getCbRemoveSmall() {
        return cbRemoveSmall;
    }

    /** @return {@link javax.swing.JCheckBox} */
    public final JCheckBox getCbSingletons() {
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
    public JCheckBox getCbManaBurn() {
        return cbManaBurn;
    }

    /** @return {@link javax.swing.JCheckBox} */
    public JCheckBox getCbScaleLarger() {
        return cbScaleLarger;
    }

    /** @return {@link javax.swing.JCheckBox} */
    public JCheckBox getCbDevMode() {
        return cbDevMode;
    }

    /** @return {@link forge.gui.toolbox.FLabel} */
    public FLabel getBtnReset() {
        return btnReset;
    }
}
