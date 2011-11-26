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

import org.apache.commons.lang3.StringUtils;

import net.miginfocom.swing.MigLayout;
import forge.AllZone;
import forge.Singletons;
import forge.control.match.ControlDock;
import forge.gui.skin.FButton;
import forge.gui.skin.FPanel;
import forge.gui.skin.FRoundedPanel;
import forge.gui.skin.FSkin;
import forge.properties.ForgePreferences;
import forge.view.toolbox.FOverlay;

/** 
 * Swing component for button dock.
 *
 */
@SuppressWarnings("serial")
public class ViewDock extends FRoundedPanel {
    private FSkin skin;
    private ControlDock control;
    private Map<String, KeyboardShortcutField> keyboardShortcutFields;
    private Action actClose, actSaveKeyboardShortcuts;

    /** 
     * Swing component for button dock.
     *
     */
    public ViewDock() {
        super();
        setCorners(new boolean[] {false, false, false, false});
        setBorders(new boolean[] {true, true, false, true});
        setToolTipText("Shortcut Button Dock");
        setBackground(AllZone.getSkin().getClrTheme());
        setLayout(new MigLayout("insets 0, gap 0, ay center, ax center"));
        String constraints = "w 30px!, h 30px!, gap 0 10px";
        skin = AllZone.getSkin();
        keyboardShortcutFields = new HashMap<String, KeyboardShortcutField>();

        actClose = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                AllZone.getOverlay().hideOverlay();
            }
        };

        actSaveKeyboardShortcuts = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                control.saveKeyboardShortcuts();
            }
        };

        JLabel btnConcede = new DockButton(skin.getIconConcede(), "Concede Game");
        btnConcede.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                control.concede();
            }
        });

        JLabel btnShortcuts = new DockButton(skin.getIconShortcuts(), "Keyboard Shortcuts");
        btnShortcuts.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                overlayKeyboard();
            }
        });

        JLabel btnSettings = new DockButton(skin.getIconSettings(), "Game Settings");
        btnSettings.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                overlaySettings();
            }
        });

        JLabel btnEndTurn = new DockButton(skin.getIconEndTurn(), "Game Settings");
        btnEndTurn.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                control.endTurn();
            }
        });

        add(btnConcede, constraints);
        add(btnShortcuts, constraints);
        add(btnSettings, constraints);
        add(btnEndTurn, constraints);

        // After all components are in place, instantiate controller.
        control = new ControlDock(this);
    }

    /** @return ControlDock */
    public ControlDock getController() {
        return control;
    }

    /**
     * Buttons in Dock.  JLabels are used to allow hover effects.
     */
    public class DockButton extends JLabel {
        private Image img;
        private Color hoverBG = skin.getClrHover();
        private Color defaultBG = new Color(0, 0, 0, 0);
        private Color clrBorders = new Color(0, 0, 0, 0);
        private int w, h;

        /**
         * Buttons in Dock.  JLabels are used to allow hover effects.
         * 
         * @param i0 &emsp; ImageIcon to show in button
         * @param s0 &emsp; Tooltip string
         */
        public DockButton(ImageIcon i0, String s0) {
            super();
            setToolTipText(s0);
            setOpaque(false);
            setBackground(defaultBG);
            img = i0.getImage();

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    clrBorders = skin.getClrBorders();
                    setBackground(hoverBG);
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    clrBorders = new Color(0, 0, 0, 0);
                    setBackground(defaultBG);
                }
            });
        }

        @Override
        public void paintComponent(Graphics g) {
            w = getWidth();
            h = getHeight();
            g.setColor(getBackground());
            g.fillRect(0, 0, w, h);
            g.setColor(clrBorders);
            g.drawRect(0, 0, w - 1, h - 1);
            g.drawImage(img, 0, 0, w, h, null);
            super.paintComponent(g);
        }
    }

    /** */
    private void overlayKeyboard() {
        FOverlay overlay = AllZone.getOverlay();
        overlay.removeAll();
        overlay.setLayout(new MigLayout("insets 0"));
        overlay.showOverlay();

        FPanel parent = new FPanel();
        parent.setBGImg(skin.getTexture1());
        parent.setBorder(new LineBorder(skin.getClrBorders(), 1));
        parent.setLayout(new MigLayout("insets 0, wrap 2, ax center, ay center"));
        overlay.add(parent, "w 80%!, h 80%!, gaptop 10%, gapleft 10%, span 2 1");

        FButton btnOK = new FButton();
        FButton btnCancel = new FButton();

        overlay.add(btnOK, "width 30%, newline, gapright 10%, gapleft 15%, gaptop 10px");
        overlay.add(btnCancel, "width 30%!");

        btnOK.setAction(actSaveKeyboardShortcuts);
        btnOK.setText("Save and Exit");

        btnCancel.setAction(actClose);
        btnCancel.setText("Exit Without Save");

        KeyboardShortcutLabel lblBlurb = new KeyboardShortcutLabel();
        lblBlurb.setText("<html><center>Focus in a box and press a key.<br>"
                + "Backspace will remove the last keypress.<br>"
                + "Restart Forge to map any new changes.</center></html>");
        parent.add(lblBlurb, "span 2 1, gapbottom 30px");

        ForgePreferences fp = Singletons.getModel().getPreferences();

        // Keyboard shortcuts are a bit tricky to make, since they involve three
        // different parts of the codebase: Preferences, main match control, and
        // the customize button in the dock. To make a keyboard shortcut:
        //
        // 1. Go to ForgePreferences and set a variable, default value, setter, and getter
        // 2. Go to ControlMatchUI and map an action using mapKeyboardShortcuts
        // 3. (Optional) Come back to this file and add a new KeyboardShortcutField so
        //      the user can customize this shortcut.  Update ControlDock to ensure any
        //      changes will be saved.

        // Keyboard shortcuts must be created, then added to the HashMap to help saving.
        // Their actions must also be declared, using mapKeyboardShortcuts in ControlMatchUI.
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
        FOverlay overlay = AllZone.getOverlay();
        overlay.setLayout(new MigLayout("insets 0"));
        overlay.showOverlay();

        JPanel parent = new JPanel();
        parent.setBackground(Color.red.darker());
        overlay.add(parent, "w 80%!, h 80%!, gaptop 10%, gapleft 10%, span 2 1");

        FButton btnOK = new FButton("Save and Exit");
        FButton btnCancel = new FButton("Exit Without Save");

        overlay.add(btnOK, "width 30%, newline, gapright 10%, gapleft 15%, gaptop 10px");
        overlay.add(btnCancel, "width 30%!");

        btnOK.setAction(actClose);
        btnOK.setText("Save and Exit");

        btnCancel.setAction(actClose);
        btnCancel.setText("Exit Without Save");

        JLabel test = new JLabel();
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

        public KeyboardShortcutLabel(String s0) {
            super(s0);

            this.setForeground(skin.getClrText());
            this.setFont(skin.getFont1().deriveFont(Font.PLAIN, 16));
        }
    }

    /** A JTextField plus a "codeString" property, that stores keycodes for the shortcut.
     * Also, an action listener that handles translation of keycodes into characters and
     * (dis)assembly of keycode stack.
     */
    public class KeyboardShortcutField extends JTextField {
        private String codeString;

        /** A JTextField plus a "codeString" property, that stores keycodes for the shortcut.
         * Also, an action listener that handles translation of keycodes into characters and
         * (dis)assembly of keycode stack.
         * 
         * This constructor sets the keycode string for this shortcut.
         * Important: this parameter is keyCODEs not keyCHARs.
         * 
         * @param s0 &emsp; The string of keycodes for this shortcut
         */
        public KeyboardShortcutField(String s0) {
            this();
            this.setCodeString(s0);
        }

        /** A JTextField plus a "codeString" property, that stores keycodes for the shortcut.
         * Also, an action listener that handles translation of keycodes into characters and
         * (dis)assembly of keycode stack.
         */
        public KeyboardShortcutField() {
            super();
            this.setEditable(false);
            this.setFont(skin.getFont1().deriveFont(Font.PLAIN, 14));

            this.addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    control.addKeyCode(e);
                }
            });

            this.addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    setBackground(skin.getClrActive());
                }
                @Override
                public void focusLost(FocusEvent e) {
                    setBackground(Color.white);
                }
            });
        }

        /** @return String */
        public String getCodeString() {
            return codeString;
        }

        /** @param s0 &emsp; The new code string (space delimited) */
        public void setCodeString(String s0) {
            if (s0.equals("null")) {
                return;
            }

            codeString = s0.trim();

            List<String> codes = new ArrayList<String>(Arrays.asList(codeString.split(" ")));
            List<String> displayText = new ArrayList<String>();

            for (String s : codes) {
                if (!s.isEmpty()) {
                    displayText.add(KeyEvent.getKeyText(Integer.valueOf(s)));
                }
            }

            this.setText(StringUtils.join(displayText, ' '));
        }
    }

    /** @return Map<String, JTextField> */
    public Map<String, KeyboardShortcutField> getKeyboardShortcutFields() {
        return keyboardShortcutFields;
    }
}
