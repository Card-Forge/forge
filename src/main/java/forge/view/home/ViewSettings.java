package forge.view.home;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.swing.ImageIcon;
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
import org.apache.commons.lang3.text.WordUtils;

import forge.Command;
import forge.Singletons;
import forge.control.KeyboardShortcuts;
import forge.control.KeyboardShortcuts.Shortcut;
import forge.control.home.ControlSettings;
import forge.properties.ForgePreferences.FPref;
import forge.properties.ForgeProps;
import forge.properties.NewConstants.Lang.OldGuiNewGame.NewGameText;
import forge.view.toolbox.FLabel;
import forge.view.toolbox.FList;
import forge.view.toolbox.FScrollPane;
import forge.view.toolbox.FSkin;
import forge.view.toolbox.SubTab;
import forge.view.toolbox.WrapLayout;

/** 
 * Assembles swing components for "Settings" mode menu.
 *
 */
@SuppressWarnings("serial")
public class ViewSettings extends JPanel {
    private final ControlSettings control;
    private final FLabel btnReset, lblTitleSkin;

    private final JList lstChooseSkin;

    private final JCheckBox cbAnte, cbScaleLarger, cbDevMode, cbRemoveSmall, cbRemoveArtifacts,
        cbUploadDraft, cbStackLand, cbRandomFoil, cbTextMana, cbSingletons;

    private final JPanel pnlTabber, pnlPrefs, pnlAvatars, tabPrefs, tabAvatars;
    private final JScrollPane scrContent;

    private String sectionConstraints, regularConstraints, tabberConstraints;

    private final FLabel lblAvatarHuman, lblAvatarAI;

    /** Assembles swing components for "Settings" mode menu. */
    public ViewSettings() {
        // Display
        super();
        this.setOpaque(false);

        // Final component inits: JPanels
        pnlTabber = new JPanel();
        pnlTabber.setOpaque(false);

        pnlPrefs = new JPanel();
        pnlPrefs.setOpaque(false);

        pnlAvatars = new JPanel();
        pnlAvatars.setOpaque(false);

        tabPrefs = new SubTab("Preferences");
        tabAvatars = new SubTab("Avatars");

        cbRemoveSmall = new OptionsCheckBox("Remove Small Creatures");
        cbSingletons = new OptionsCheckBox("Singleton Mode");
        cbRemoveArtifacts = new OptionsCheckBox("Remove Artifacts");
        cbAnte = new OptionsCheckBox("Play for Ante");
        cbUploadDraft = new OptionsCheckBox("Upload Draft Pics");
        cbStackLand = new OptionsCheckBox("Stack AI Land");
        cbDevMode = new OptionsCheckBox(ForgeProps.getLocalized(NewGameText.DEV_MODE));
        cbTextMana = new OptionsCheckBox("Text / Mana Overlay");
        cbScaleLarger = new OptionsCheckBox("Scale Image Larger");
        cbRandomFoil = new OptionsCheckBox("Random Foil");

        // Final component inits: Various
        scrContent = new JScrollPane(
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrContent.getViewport().setOpaque(false);
        scrContent.setBorder(null);
        scrContent.setOpaque(false);
        scrContent.getVerticalScrollBar().setUnitIncrement(16);

        lstChooseSkin = new FList();

        btnReset = new FLabel.Builder().opaque(true)
                .hoverable(true).text("Reset to defaults").build();

        lblTitleSkin = new FLabel.Builder().text("Choose Skin").fontScaleAuto(false).build();
        lblTitleSkin.setFont(FSkin.getBoldFont(14));

        lblAvatarHuman = new FLabel.Builder().hoverable(true).selectable(true)
                .iconScaleFactor(0.99f).iconInBackground(true).build();
        lblAvatarAI = new FLabel.Builder().hoverable(true).selectable(true)
                .iconScaleFactor(0.99f).iconInBackground(true).build();

        populateTabber();
        populatePrefs();
        populateAvatars();

        this.setLayout(new MigLayout("insets 0, gap 0, wrap, hidemode 3"));
        this.add(pnlTabber, "w 95%!, h 20px!, gap 2.5% 0 0 0");
        this.add(scrContent, "w 95%!, h 92%!, gap 2.5% 0 20px 0");

        // After all components are instantiated, fire up control.
        this.control = new ControlSettings(this);
        showPrefsTab();
    }

    /** */
    public void updateSkinNames() {
        final String[] uglyNames = FSkin.getSkins().toArray(new String[0]);
        final String[] prettyNames = new String[uglyNames.length];

        for (int i = 0; i < uglyNames.length; i++) {
            prettyNames[i] = WordUtils.capitalize(uglyNames[i].replace('_', ' '));
        }

        lstChooseSkin.setListData(prettyNames);
        lstChooseSkin.setSelectedValue(Singletons.getModel().getPreferences().getPref(FPref.UI_SKIN), true);
        lstChooseSkin.ensureIndexIsVisible(lstChooseSkin.getSelectedIndex());
    }

    private void populateTabber() {
        tabPrefs.setToolTipText("Global preference options");
        tabAvatars.setToolTipText("Human and AI avatar select");

        tabberConstraints = "w 50%!, h 20px!";
        pnlTabber.setOpaque(false);
        pnlTabber.setLayout(new MigLayout("insets 0, gap 0, align center"));

        pnlTabber.add(tabPrefs, tabberConstraints);
        pnlTabber.add(tabAvatars, tabberConstraints);
    }

    private void populatePrefs() {
        pnlPrefs.setLayout(new MigLayout("insets 0, gap 0, wrap 2"));

        // Spacing between components is defined here.
        sectionConstraints = "w 80%!, h 42px!, gap 10% 0 10px 10px, span 2 1";
        regularConstraints = "w 80%!, h 22px!, gap 10% 0 0 10px, span 2 1";

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

        pnlPrefs.add(cbDevMode, regularConstraints);
        pnlPrefs.add(new NoteLabel("Enables menu with functions for testing during development."), regularConstraints);

        // Graphic Options
        pnlPrefs.add(new SectionLabel("Graphic Options"), sectionConstraints);

        pnlPrefs.add(lblTitleSkin, regularConstraints);
        pnlPrefs.add(new NoteLabel("Various user-created themes for Forge backgrounds, fonts, and colors."), regularConstraints);
        pnlPrefs.add(new FScrollPane(lstChooseSkin), "h 120px!, w 150px!, gap 10% 0 0 2%, wrap");

        pnlPrefs.add(new FLabel.Builder().text("Card Size").fontStyle(Font.BOLD).build(), regularConstraints);
        pnlPrefs.add(new NoteLabel("Size of cards in hand and playing field, when possible"), regularConstraints);

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

        FLabel lblTemp;
        KeyboardShortcutField ksf;
        for (final Shortcut s : shortcuts) {
            lblTemp = new FLabel.Builder().text(s.getDescription()).build();
            ksf  = new KeyboardShortcutField(s);
            pnlPrefs.add(lblTemp, "w 40%!, h 22px!, gap 10%! 0 0 1%");
            pnlPrefs.add(ksf, "w 25%!");
        }

        // Reset button
        pnlPrefs.add(new SectionLabel(" "), sectionConstraints);
        pnlPrefs.add(btnReset, sectionConstraints);
    } // End populatePrefs()

    private void populateAvatars() {
        final Map<Integer, Image> avatarMap = FSkin.getAvatars();
        final JPanel pnlAvatarPics = new JPanel(new WrapLayout());
        final JPanel pnlAvatarUsers = new JPanel(new MigLayout("insets 0, gap 0, align center"));

        pnlAvatarUsers.setOpaque(false);
        pnlAvatarPics.setOpaque(false);

        pnlAvatarUsers.add(new FLabel.Builder().fontSize(12).text("Human").build(),
                "w 100px!, h 20px!, gap 0 20px 0 0");
        pnlAvatarUsers.add(new FLabel.Builder().fontSize(12).text("AI").build(),
                "w 100px!, h 20px!, wrap");

        pnlAvatarUsers.add(lblAvatarHuman, "w 100px!, h 100px!, gap 0 20px 0 0");
        pnlAvatarUsers.add(lblAvatarAI, "w 100px!, h 100px!");

        for (final Integer i : avatarMap.keySet()) {
            pnlAvatarPics.add(makeAvatarLabel(avatarMap.get(i), i));
        }

        pnlAvatars.removeAll();
        pnlAvatars.setLayout(new MigLayout("insets 0, gap 0"));
        pnlAvatars.add(pnlAvatarUsers, "w 90%!, h 150px!, wrap");
        pnlAvatars.add(new FScrollPane(pnlAvatarPics,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER),
                "w 90%!, pushy, growy, gap 5% 0 0 0");

        final Command cmdHuman = new Command() { @Override
            public void execute() { lblAvatarAI.setSelected(false); } };

        final Command cmdAI = new Command() { @Override
            public void execute() { lblAvatarHuman.setSelected(false); } };

        lblAvatarHuman.setCommand(cmdHuman);
        lblAvatarAI.setCommand(cmdAI);

        lblAvatarHuman.setSelected(true);

        final String[] indexes = Singletons.getModel().getPreferences().getPref(FPref.UI_AVATARS).split(",");
        int humanIndex = Integer.parseInt(indexes[0]);
        int aiIndex = Integer.parseInt(indexes[1]);

        if (humanIndex >= FSkin.getAvatars().size()) { humanIndex = 0; }
        if (aiIndex >= FSkin.getAvatars().size()) { aiIndex = 0; }

        lblAvatarHuman.setIcon(new ImageIcon(FSkin.getAvatars().get(humanIndex)));
        lblAvatarAI.setIcon(new ImageIcon(FSkin.getAvatars().get(aiIndex)));

        Singletons.getModel().getPreferences().setPref(FPref.UI_AVATARS, aiIndex + "," + humanIndex);
        Singletons.getModel().getPreferences().save();
    }

    private FLabel makeAvatarLabel(final Image img0, final int index0) {
        final FLabel lbl = new FLabel.Builder().icon(new ImageIcon(img0)).iconScaleFactor(1.0)
                .iconAlpha(0.7f).iconInBackground(true).hoverable(true).build();

        final Dimension size = new Dimension(100, 100);
        lbl.setPreferredSize(size);
        lbl.setMaximumSize(size);
        lbl.setMinimumSize(size);

        final Command cmd = new Command() {
            @Override
            public void execute() {
                String[] indices = Singletons.getModel().getPreferences()
                        .getPref(FPref.UI_AVATARS).split(",");

                if (lblAvatarAI.isSelected()) {
                    lblAvatarAI.setIcon(new ImageIcon(FSkin.getAvatars().get(index0)));
                    lblAvatarAI.repaintOnlyThisLabel();
                    indices[0] = String.valueOf(index0);
                }
                else {
                    lblAvatarHuman.setIcon(new ImageIcon(FSkin.getAvatars().get(index0)));
                    lblAvatarHuman.repaintOnlyThisLabel();
                    indices[1] = String.valueOf(index0);
                }

                Singletons.getModel().getPreferences().setPref(FPref.UI_AVATARS, indices[0] + "," + indices[1]);
                Singletons.getModel().getPreferences().save();
            }
        };

        lbl.setCommand(cmd);
        return lbl;
    }

    /** Consolidates checkbox styling in one place. */
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

    /** @return {@link javax.swing.JPanel} */
    public final JPanel getPnlPrefs() {
        return this.pnlPrefs;
    }

    /** @return {@link javax.swing.JPanel} */
    public final JPanel getPnlAvatars() {
        return this.pnlAvatars;
    }

    /** @return {@link javax.swing.JList} */
    public final JList getLstChooseSkin() {
        return lstChooseSkin;
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
    public JCheckBox getCbScaleLarger() {
        return cbScaleLarger;
    }

    /** @return {@link javax.swing.JCheckBox} */
    public JCheckBox getCbDevMode() {
        return cbDevMode;
    }

    /** @return {@link javax.swing.FLabel} */
    public FLabel getLblTitleSkin() {
        return lblTitleSkin;
    }

    /** @return ControlSettings */
    public ControlSettings getControl() {
        return ViewSettings.this.control;
    }

    /** @return {@link forge.view.toolbox.FLabel} */
    public FLabel getBtnReset() {
        return btnReset;
    }

    /** @return {@link javax.swing.JPanel} */
    public JPanel getTabPrefs() {
        return this.tabPrefs;
    }

    /** @return {@link javax.swing.JPanel} */
    public JPanel getTabAvatars() {
        return this.tabAvatars;
    }

    /** */
    public final void showPrefsTab() {
        this.scrContent.setViewportView(pnlPrefs);
        control.updateTabber(tabPrefs);
    }

    /** */
    public final void showAvatarsTab() {
        this.scrContent.setViewportView(pnlAvatars);
        control.updateTabber(tabAvatars);
    }
}
