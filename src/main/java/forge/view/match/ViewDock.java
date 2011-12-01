/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge.view.match;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.LineBorder;

import net.miginfocom.swing.MigLayout;

import org.apache.commons.lang3.StringUtils;

import forge.AllZone;
import forge.Singletons;
import forge.control.match.ControlDock;
import forge.properties.ForgePreferences;
import forge.view.toolbox.FButton;
import forge.view.toolbox.FOverlay;
import forge.view.toolbox.FPanel;
import forge.view.toolbox.FRoundedPanel;
import forge.view.toolbox.FSkin;

/**
 * Swing component for button dock.
 * 
 */
@SuppressWarnings("serial")
public class ViewDock extends FRoundedPanel {
    private final FSkin skin;
    private final ControlDock control;
    private final Map<String, KeyboardShortcutField> keyboardShortcutFields;
    private final Action actClose, actSaveKeyboardShortcuts;

    /**
     * Swing component for button dock.
     * 
     */
    public ViewDock() {
        super();
        this.setToolTipText("Shortcut Button Dock");
        this.setBackground(AllZone.getSkin().getClrTheme());
        this.setLayout(new MigLayout("insets 0, gap 0, ay center, ax center"));
        final String constraints = "w 30px!, h 30px!, gap 0 10px";
        this.skin = AllZone.getSkin();
        this.keyboardShortcutFields = new HashMap<String, KeyboardShortcutField>();

        this.actClose = new AbstractAction() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                AllZone.getOverlay().hideOverlay();
            }
        };

        this.actSaveKeyboardShortcuts = new AbstractAction() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                ViewDock.this.control.saveKeyboardShortcuts();
            }
        };

        final JLabel btnConcede = new DockButton(this.skin.getIconConcede(), "Concede Game");
        btnConcede.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(final MouseEvent e) {
                ViewDock.this.control.concede();
            }
        });

        final JLabel btnShortcuts = new DockButton(this.skin.getIconShortcuts(), "Keyboard Shortcuts");
        btnShortcuts.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(final MouseEvent e) {
                ViewDock.this.overlayKeyboard();
            }
        });

        final JLabel btnSettings = new DockButton(this.skin.getIconSettings(), "Game Settings");
        btnSettings.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(final MouseEvent e) {
                ViewDock.this.overlaySettings();
            }
        });

        final JLabel btnEndTurn = new DockButton(this.skin.getIconEndTurn(), "End Turn");
        btnEndTurn.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(final MouseEvent e) {
                ViewDock.this.control.endTurn();
            }
        });

        this.add(btnConcede, constraints);
        this.add(btnShortcuts, constraints);
        this.add(btnSettings, constraints);
        this.add(btnEndTurn, constraints);

        // After all components are in place, instantiate controller.
        this.control = new ControlDock(this);
    }

    /**
     * Gets the controller.
     * 
     * @return ControlDock
     */
    public ControlDock getController() {
        return this.control;
    }

    /**
     * Buttons in Dock. JLabels are used to allow hover effects.
     */
    public class DockButton extends JLabel {
        private final Image img;
        private final Color hoverBG = ViewDock.this.skin.getClrHover();
        private final Color defaultBG = new Color(0, 0, 0, 0);
        private Color clrBorders = new Color(0, 0, 0, 0);
        private int w, h;

        /**
         * Buttons in Dock. JLabels are used to allow hover effects.
         * 
         * @param i0
         *            &emsp; ImageIcon to show in button
         * @param s0
         *            &emsp; Tooltip string
         */
        public DockButton(final ImageIcon i0, final String s0) {
            super();
            this.setToolTipText(s0);
            this.setOpaque(false);
            this.setBackground(this.defaultBG);
            this.img = i0.getImage();

            this.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(final MouseEvent e) {
                    DockButton.this.clrBorders = ViewDock.this.skin.getClrBorders();
                    DockButton.this.setBackground(DockButton.this.hoverBG);
                }

                @Override
                public void mouseExited(final MouseEvent e) {
                    DockButton.this.clrBorders = new Color(0, 0, 0, 0);
                    DockButton.this.setBackground(DockButton.this.defaultBG);
                }
            });
        }

        /*
         * (non-Javadoc)
         * 
         * @see javax.swing.JComponent#paintComponent(java.awt.Graphics)
         */
        @Override
        public void paintComponent(final Graphics g) {
            this.w = this.getWidth();
            this.h = this.getHeight();
            g.setColor(this.getBackground());
            g.fillRect(0, 0, this.w, this.h);
            g.setColor(this.clrBorders);
            g.drawRect(0, 0, this.w - 1, this.h - 1);
            g.drawImage(this.img, 0, 0, this.w, this.h, null);
            super.paintComponent(g);
        }
    }

    /** */
    private void overlayKeyboard() {
        final FOverlay overlay = AllZone.getOverlay();
        overlay.removeAll();
        overlay.setLayout(new MigLayout("insets 0"));
        overlay.showOverlay();

        final FPanel parent = new FPanel();
        parent.setBGImg(this.skin.getTexture1());
        parent.setBorder(new LineBorder(this.skin.getClrBorders(), 1));
        parent.setLayout(new MigLayout("insets 0, wrap 2, ax center, ay center"));
        overlay.add(parent, "w 80%!, h 80%!, gaptop 10%, gapleft 10%, span 2 1");

        final FButton btnOK = new FButton();
        final FButton btnCancel = new FButton();

        overlay.add(btnOK, "width 30%, newline, gapright 10%, gapleft 15%, gaptop 10px");
        overlay.add(btnCancel, "width 30%!");

        btnOK.setAction(this.actSaveKeyboardShortcuts);
        btnOK.setText("Save and Exit");

        btnCancel.setAction(this.actClose);
        btnCancel.setText("Exit Without Save");

        final KeyboardShortcutLabel lblBlurb = new KeyboardShortcutLabel();
        lblBlurb.setText("<html><center>Focus in a box and press a key.<br>"
                + "Backspace will remove the last keypress.<br>"
                + "Restart Forge to map any new changes.</center></html>");
        parent.add(lblBlurb, "span 2 1, gapbottom 30px");

        final ForgePreferences fp = Singletons.getModel().getPreferences();

        // Keyboard shortcuts are a bit tricky to make, since they involve three
        // different parts of the codebase: Preferences, main match control, and
        // the customize button in the dock. To make a keyboard shortcut:
        //
        // 1. Go to ForgePreferences and set a variable, default value, setter,
        // and getter
        // 2. Go to ControlMatchUI and map an action using mapKeyboardShortcuts
        // 3. (Optional) Come back to this file and add a new
        // KeyboardShortcutField so
        // the user can customize this shortcut. Update ControlDock to ensure
        // any
        // changes will be saved.

        // Keyboard shortcuts must be created, then added to the HashMap to help
        // saving.
        // Their actions must also be declared, using mapKeyboardShortcuts in
        // ControlMatchUI.
        final KeyboardShortcutField showStack = new KeyboardShortcutField(fp.getShowStackShortcut());
        this.keyboardShortcutFields.put("showstack", showStack);

        final KeyboardShortcutField showCombat = new KeyboardShortcutField(fp.getShowCombatShortcut());
        this.keyboardShortcutFields.put("showcombat", showCombat);

        final KeyboardShortcutField showConsole = new KeyboardShortcutField(fp.getShowConsoleShortcut());
        this.keyboardShortcutFields.put("showconsole", showConsole);

        final KeyboardShortcutField showPlayers = new KeyboardShortcutField(fp.getShowPlayersShortcut());
        this.keyboardShortcutFields.put("showplayers", showPlayers);

        final KeyboardShortcutField showDev = new KeyboardShortcutField(fp.getShowDevShortcut());
        this.keyboardShortcutFields.put("showdev", showDev);

        final KeyboardShortcutField concedeGame = new KeyboardShortcutField(fp.getConcedeShortcut());
        this.keyboardShortcutFields.put("concede", concedeGame);

        final KeyboardShortcutField showPicture = new KeyboardShortcutField(fp.getShowPictureShortcut());
        this.keyboardShortcutFields.put("showpicture", showPicture);

        final KeyboardShortcutField showDetail = new KeyboardShortcutField(fp.getShowDetailShortcut());
        this.keyboardShortcutFields.put("showdetail", showDetail);

        //
        parent.add(new KeyboardShortcutLabel("Show stack tab: "), "w 200px!");
        parent.add(showStack, "w 100px!");
        parent.add(new KeyboardShortcutLabel("Show combat tab: "), "w 200px!");
        parent.add(showCombat, "w 100px!");
        parent.add(new KeyboardShortcutLabel("Show console tab: "), "w 200px!");
        parent.add(showConsole, "w 100px!");
        parent.add(new KeyboardShortcutLabel("Show players tab: "), "w 200px!");
        parent.add(showPlayers, "w 100px!");
        parent.add(new KeyboardShortcutLabel("Show devmode tab: "), "w 200px!");
        parent.add(showDev, "w 100px!");
        parent.add(new KeyboardShortcutLabel("Concede game: "), "w 200px!");
        parent.add(concedeGame, "w 100px!");
        parent.add(new KeyboardShortcutLabel("Show card picture: "), "w 200px!");
        parent.add(showPicture, "w 100px!");
        parent.add(new KeyboardShortcutLabel("Show card detail: "), "w 200px!");
        parent.add(showDetail, "w 100px!");
    }

    /** */
    private void overlaySettings() {
        final FOverlay overlay = AllZone.getOverlay();
        overlay.setLayout(new MigLayout("insets 0"));
        overlay.showOverlay();

        final JPanel parent = new JPanel();
        parent.setBackground(Color.red.darker());
        overlay.add(parent, "w 80%!, h 80%!, gaptop 10%, gapleft 10%, span 2 1");

        final FButton btnOK = new FButton("Save and Exit");
        final FButton btnCancel = new FButton("Exit Without Save");

        overlay.add(btnOK, "width 30%, newline, gapright 10%, gapleft 15%, gaptop 10px");
        overlay.add(btnCancel, "width 30%!");

        btnOK.setAction(this.actClose);
        btnOK.setText("Save and Exit");

        btnCancel.setAction(this.actClose);
        btnCancel.setText("Exit Without Save");

        final JLabel test = new JLabel();
        test.setForeground(Color.white);
        test.setText("<html><center>'Settings' does not do anything yet.<br>"
                + "This button is just here to demonstrate the dock feature.<br>"
                + "'Settings' can be removed or developed further.</center></html>");

        parent.add(test);
    }

    /** Small private class to centralize label styling. */
    private class KeyboardShortcutLabel extends JLabel {
        public KeyboardShortcutLabel() {
            this("");
        }

        public KeyboardShortcutLabel(final String s0) {
            super(s0);

            this.setForeground(ViewDock.this.skin.getClrText());
            this.setFont(ViewDock.this.skin.getFont1().deriveFont(Font.PLAIN, 16));
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
         * This constructor sets the keycode string for this shortcut.
         * Important: this parameter is keyCODEs not keyCHARs.
         * 
         * @param s0
         *            &emsp; The string of keycodes for this shortcut
         */
        public KeyboardShortcutField(final String s0) {
            this();
            this.setCodeString(s0);
        }

        /**
         * A JTextField plus a "codeString" property, that stores keycodes for
         * the shortcut. Also, an action listener that handles translation of
         * keycodes into characters and (dis)assembly of keycode stack.
         */
        public KeyboardShortcutField() {
            super();
            this.setEditable(false);
            this.setFont(ViewDock.this.skin.getFont1().deriveFont(Font.PLAIN, 14));

            this.addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(final KeyEvent e) {
                    ViewDock.this.control.addKeyCode(e);
                }
            });

            this.addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(final FocusEvent e) {
                    KeyboardShortcutField.this.setBackground(ViewDock.this.skin.getClrActive());
                }

                @Override
                public void focusLost(final FocusEvent e) {
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

    /**
     * Gets the keyboard shortcut fields.
     * 
     * @return Map<String, JTextField>
     */
    public Map<String, KeyboardShortcutField> getKeyboardShortcutFields() {
        return this.keyboardShortcutFields;
    }
}
