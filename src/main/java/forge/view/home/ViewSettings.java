package forge.view.home;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
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

import forge.PlayerType;
import forge.Singletons;
import forge.control.KeyboardShortcuts;
import forge.control.KeyboardShortcuts.Shortcut;
import forge.control.home.ControlSettings;
import forge.properties.ForgePreferences.FPref;
import forge.properties.ForgeProps;
import forge.properties.NewConstants.Lang.OldGuiNewGame.NewGameText;
import forge.view.ViewHomeUI;
import forge.view.toolbox.FLabel;
import forge.view.toolbox.FList;
import forge.view.toolbox.FScrollPane;
import forge.view.toolbox.FSkin;
import forge.view.toolbox.SubTab;

/** 
 * Assembles swing components for "Settings" mode menu.
 *
 */
@SuppressWarnings("serial")
public class ViewSettings extends JPanel {
    private final ControlSettings control;
    private final FLabel btnReset, lblTitleSkin;
    private final ViewHomeUI parentView;

    private final JList lstChooseSkin;

    private final JCheckBox cbAnte, cbScaleLarger, cbDevMode, cbRemoveSmall, cbRemoveArtifacts,
        cbUploadDraft, cbStackLand, cbRandomFoil, cbTextMana, cbSingletons;

    private final JPanel pnlTabber, pnlPrefs, pnlAvatars, tabPrefs, tabAvatars;
    private final JScrollPane scrContent;

    private String sectionConstraints, regularConstraints, tabberConstraints;

    private AvatarLabel avatarHuman, avatarAI;
    private List<AvatarLabel> lstAvatars;
    /**
     * 
     * Assembles swing components for "Settings" mode menu.
     * @param view0 &emsp; HomeTopLevel
     */
    public ViewSettings(final ViewHomeUI view0) {
        // Display
        super();
        this.setOpaque(false);
        this.parentView = view0;

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
        pnlPrefs.add(new FScrollPane(lstChooseSkin), "h 60px!, w 150px!, gap 10% 0 0 2%, wrap");
        lstChooseSkin.setListData(FSkin.getSkins().toArray(new String[0]));
        lstChooseSkin.setSelectedValue(Singletons.getModel().getPreferences().getPref(FPref.UI_SKIN), true);
        lstChooseSkin.ensureIndexIsVisible(lstChooseSkin.getSelectedIndex());

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
        final JLabel lblTitle = new SectionLabel("Avatar Selection");
        final JLabel lblNote1 = new NoteLabel("Click on an image to set that avatar for a player or AI.");
        final JLabel lblNote2 = new NoteLabel("Click multiple times to cycle.");

        lblTitle.setHorizontalAlignment(SwingConstants.CENTER);
        lblNote1.setHorizontalAlignment(SwingConstants.CENTER);
        lblNote2.setHorizontalAlignment(SwingConstants.CENTER);

        pnlAvatars.setLayout(new MigLayout("insets 0, gap 0, align center, wrap 3"));
        pnlAvatars.add(lblTitle, "w 330px!, span 3 1");
        pnlAvatars.add(lblNote1, "w 330px!, span 3 1");
        pnlAvatars.add(lblNote2, "w 330px!, span 3 1");

        lstAvatars = new ArrayList<AvatarLabel>();
        int counter = 0;
        for (final Image i : FSkin.getAvatars().values()) {
            lstAvatars.add(new AvatarLabel(i, counter++));
            pnlAvatars.add(lstAvatars.get(lstAvatars.size() - 1), "gap 5px 5px 5px 5px");
        }

        final String[] indexes = Singletons.getModel().getPreferences().getPref(FPref.UI_AVATARS).split(",");
        int humanIndex = Integer.parseInt(indexes[0]);
        int aiIndex = Integer.parseInt(indexes[1]);

        // Set human avatar from preferences
        if (humanIndex >= lstAvatars.size()) {
            humanIndex = (int) (Math.random() * (lstAvatars.size() - 1));
        }

        avatarHuman = lstAvatars.get(humanIndex);
        avatarHuman.setOwner(PlayerType.HUMAN);
        avatarHuman.repaint();

        if (humanIndex == aiIndex || aiIndex >= lstAvatars.size()) {
            aiIndex = humanIndex;
            while (aiIndex == humanIndex) {
                aiIndex = (int) (Math.random() * (lstAvatars.size() - 1));
            }
        }

        avatarAI = lstAvatars.get(aiIndex);
        avatarAI.setOwner(PlayerType.COMPUTER);
        avatarAI.repaint();
    }

    /** Surprisingly complicated - be careful when modifying! */
    private void cycleOwner(final AvatarLabel lbl0) {
        if (lbl0.getOwner() == null) {
            lbl0.setOwner(PlayerType.HUMAN);
            lbl0.repaint();

            if (avatarHuman != null) {
                avatarHuman.setOwner(null);
                avatarHuman.repaint();
            }

            avatarHuman = lbl0;
        }
        else if (lbl0.getOwner() == PlayerType.HUMAN) {
            // Re-assign avatar to human
            avatarHuman.setOwner(null);
            avatarHuman.repaint();

            for (int i = 0; i < lstAvatars.size(); i++) {
                if (lstAvatars.get(i) != lbl0) {
                    avatarHuman = lstAvatars.get(i);
                    avatarHuman.setOwner(PlayerType.HUMAN);
                    avatarHuman.repaint();
                    break;
                }
            }

            // Assign computer
            lbl0.setOwner(PlayerType.COMPUTER);
            lbl0.repaint();

            if (avatarAI != null) {
                avatarAI.setOwner(null);
                avatarAI.repaint();
            }

            avatarAI = lbl0;
        }
        else {
            lbl0.setOwner(null);
            lbl0.repaint();

            // Re-assign avatar to computer
            avatarAI.setOwner(null);
            avatarAI.repaint();

            for (int i = 0; i < lstAvatars.size(); i++) {
                if (lstAvatars.get(i) != avatarHuman) {
                    avatarAI = lstAvatars.get(i);
                    avatarAI.setOwner(PlayerType.COMPUTER);
                    avatarAI.repaint();
                    break;
                }
            }
        }

        Singletons.getModel().getPreferences().setPref(
                FPref.UI_AVATARS, avatarHuman.getIndex() + "," + avatarAI.getIndex());
        Singletons.getModel().getPreferences().save();
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

    private class AvatarLabel extends JLabel {
        private final Image img;
        private final int index;
        private PlayerType owner;
        private boolean hovered = false;

        public AvatarLabel(final Image img0, final int index0) {
            super();
            img = img0;
            index = index0;
            setMaximumSize(new Dimension(100, 120));
            setMinimumSize(new Dimension(100, 120));
            setPreferredSize(new Dimension(100, 120));

            this.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(final MouseEvent evt) { hovered = true; repaint(); }

                @Override
                public void mouseExited(final MouseEvent evt) { hovered = false; repaint(); }

                @Override
                public void mouseClicked(final MouseEvent evt) { cycleOwner(AvatarLabel.this); repaint(); }
            });
        }

        public void setOwner(final PlayerType player0) {
            this.owner = player0;
        }

        public PlayerType getOwner() {
            return this.owner;
        }

        public int getIndex() {
            return this.index;
        }

        protected void paintComponent(final Graphics graphics0) {
            if (hovered) {
                graphics0.setColor(FSkin.getColor(FSkin.Colors.CLR_HOVER));
                graphics0.fillRect(0, 0, 100, 120);
            }

            graphics0.drawImage(img, 0, 20, null);
            if (owner == null) { return; }

            graphics0.setColor(FSkin.getColor(FSkin.Colors.CLR_TEXT));
            graphics0.drawRect(0, 0, 99, 119);
            graphics0.setFont(FSkin.getBoldFont(14));
            graphics0.drawString(owner.toString(), 5, 15);
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
    public ControlSettings getController() {
        return ViewSettings.this.control;
    }

    /** @return {@link forge.view.home.HomeTopLevel} */
    public ViewHomeUI getParentView() {
        return parentView;
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
