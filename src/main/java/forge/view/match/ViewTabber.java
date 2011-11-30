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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;

import net.miginfocom.swing.MigLayout;
import forge.AllZone;
import forge.Constant;
import forge.MagicStack;
import forge.Player;
import forge.card.spellability.SpellAbilityStackInstance;
import forge.control.match.ControlTabber;
import forge.gui.MultiLineLabelUI;
import forge.view.toolbox.FPanel;
import forge.view.toolbox.FSkin;
import forge.view.toolbox.FVerticalTabPanel;

/**
 * Vertical tab panel for viewing stack, combat, etc. Unfortunately, cannot
 * extend a Swing component, since vertical tabs are generated dynamically in
 * the constructor.
 * 
 */
public class ViewTabber {
    private final List<JPanel> panelList;
    private HashMap<Player, JLabel[]> detailLabels;

    private final ControlTabber control;
    private final FSkin skin;

    private final FPanel pnlStack, pnlCombat, pnlConsole, pnlPlayers, pnlDev;

    private DevLabel lblMilling, lblHandView, lblLibraryView, lblGenerateMana, lblSetupGame, lblTutor,
            lblCounterPermanent, lblTapPermanent, lblUntapPermanent, lblUnlimitedLands, lblHumanLife;

    private final FVerticalTabPanel vtpTabber;

    /**
     * Assembles Swing components for tabber area in sidebar.
     */
    public ViewTabber() {
        this.skin = AllZone.getSkin();

        // Assemble card pic viewer
        this.panelList = new ArrayList<JPanel>();
        final String constraints = "wrap, insets 0, gap 0";

        this.pnlStack = new FPanel();
        this.pnlStack.setName("Stack");
        this.pnlStack.setOpaque(false);
        this.pnlStack.setLayout(new MigLayout(constraints));
        this.pnlStack.setToolTipText("View Stack");
        this.panelList.add(this.pnlStack);

        this.pnlCombat = new FPanel();
        this.pnlCombat.setName("Combat");
        this.pnlCombat.setOpaque(false);
        this.pnlCombat.setLayout(new MigLayout(constraints));
        this.pnlCombat.setToolTipText("View Combat");
        this.panelList.add(this.pnlCombat);

        this.pnlConsole = new FPanel();
        this.pnlConsole.setName("Log");
        this.pnlConsole.setOpaque(false);
        this.pnlConsole.setLayout(new MigLayout(constraints));
        this.pnlConsole.setToolTipText("View Console");
        this.panelList.add(this.pnlConsole);

        this.pnlPlayers = new FPanel();
        this.pnlPlayers.setName("Players");
        this.pnlPlayers.setOpaque(false);
        this.pnlPlayers.setLayout(new MigLayout(constraints));
        this.pnlPlayers.setToolTipText("Player List");
        this.panelList.add(this.pnlPlayers);

        this.pnlDev = new FPanel();
        this.pnlDev.setName("Dev");
        this.pnlDev.setOpaque(false);
        this.pnlDev.setLayout(new MigLayout(constraints));
        this.pnlDev.setToolTipText("Developer Mode");

        if (Constant.Runtime.DEV_MODE[0]) {
            this.panelList.add(this.pnlDev);
        }

        // Populate the various panels in the tabber.
        this.populatePnlDev();
        this.populatePnlPlayers();
        this.populatePnlConsole();

        this.vtpTabber = new FVerticalTabPanel(this.panelList);
        this.vtpTabber.getContentPanel().setBorder(new MatteBorder(1, 0, 0, 1, this.skin.getClrBorders()));

        // After all components are in place, instantiate controller.
        this.control = new ControlTabber(this);
    }

    /**
     * Gets the controller.
     * 
     * @return ControlTabber
     */
    public ControlTabber getController() {
        return this.control;
    }

    /**
     * Removes and adds JTextAreas to stack panel, which briefly summarize the
     * spell and allow mouseover.
     * 
     */
    public void updateStack() {
        final MagicStack stack = AllZone.getStack();
        final ViewTopLevel t = (ViewTopLevel) AllZone.getDisplay();

        int count = 1;
        JTextArea tar;
        String txt, isOptional;

        this.pnlStack.removeAll();
        this.vtpTabber.showTab(0);
        
        this.vtpTabber.getAllVTabs().get(0).setText("Stack : " + stack.size());
        
        final Font font = this.skin.getFont1().deriveFont(Font.PLAIN, 14);
        final Border border = new MatteBorder(0, 0, 1, 0, this.skin.getClrBorders());

        for (int i = stack.size() - 1; 0 <= i; i--) {
            isOptional = stack.peekAbility(i).isOptionalTrigger()
                    && stack.peekAbility(i).getSourceCard().getController().isHuman() ? "(OPTIONAL) " : "";
            txt = (count++) + ". " + isOptional + stack.peekInstance(i).getStackDescription();
            tar = new JTextArea(txt);
            tar.setToolTipText(txt);
            tar.setOpaque(false);
            tar.setBorder(border);
            tar.setFont(font);
            tar.setForeground(this.skin.getClrText());
            tar.setFocusable(false);
            tar.setEditable(false);
            tar.setLineWrap(true);
            tar.setWrapStyleWord(true);

            final SpellAbilityStackInstance spell = stack.peekInstance(i);

            tar.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(final MouseEvent e) {
                    t.getCardviewerController().showCard(spell.getSpellAbility().getSourceCard());
                    System.out.println();
                }
            });

            this.pnlStack.add(tar, "w 95%!, gapleft 3%, gaptop 1%");
        }
    }

    /**
     * Removes and adds JTextAreas to combat panel, which briefly summarize the
     * current combat situation.
     * 
     * @param s
     *            &emsp; String message
     */

    // Note: Can (should?) be easily retrofitted to fit stack-style reporting:
    // multiple text areas, with mouseovers highlighting combat cards.
    // Doublestrike 06-11-11
    public void updateCombat(final String s) {
        this.pnlCombat.removeAll();
        this.vtpTabber.showTab(1);

        final Font font = this.skin.getFont1().deriveFont(Font.PLAIN, 14);
        final Border border = new MatteBorder(0, 0, 0, 0, skin.getClrBorders());

        final JTextArea tar = new JTextArea(s);
        tar.setOpaque(false);
        tar.setBorder(border);
        tar.setFont(font);
        tar.setForeground(skin.getClrText());
        tar.setFocusable(false);
        tar.setLineWrap(true);
        this.pnlCombat.add(tar, "w 95%!, gapleft 3%, gaptop 1%, h 95%");
    }

    /**
     * Updates labels in the "player" panel, which display non-critical details
     * about each player in the game.
     * 
     * @param p0
     *            &emsp; Player obj
     */
    public void updatePlayerLabels(final Player p0) {
        final JLabel[] temp = this.detailLabels.get(p0);
        temp[0].setText("Life: " + String.valueOf(p0.getLife()) + "  |  Poison counters: " + String.valueOf(p0.getPoisonCounters()));
        temp[1].setText("Maximum hand size: " + String.valueOf(p0.getMaxHandSize()));
        temp[2].setText("Cards drawn this turn: " + String.valueOf(p0.getNumDrawnThisTurn()));
        temp[3].setText("Damage Prevention: " + String.valueOf(p0.getPreventNextDamage()));
    }

    /**
     * Gets the vtp tabber.
     * 
     * @return FVerticalTabPanel
     */
    public FVerticalTabPanel getVtpTabber() {
        return this.vtpTabber;
    }

    /**
     * Gets the pnl stack.
     * 
     * @return FPanel
     */
    public FPanel getPnlStack() {
        return this.pnlStack;
    }

    /**
     * Gets the pnl combat.
     * 
     * @return FPanel
     */
    public FPanel getPnlCombat() {
        return this.pnlCombat;
    }

    /**
     * Gets the pnl players.
     * 
     * @return FPanel
     */
    public FPanel getPnlPlayers() {
        return this.pnlPlayers;
    }

    /**
     * Gets the pnl dev.
     * 
     * @return FPanel
     */
    public FPanel getPnlDev() {
        return this.pnlDev;
    }

    /**
     * Gets the lbl milling.
     * 
     * @return DevLabel
     */
    public DevLabel getLblMilling() {
        return this.lblMilling;
    }

    /**
     * Gets the lbl hand view.
     * 
     * @return DevLabel
     */
    public DevLabel getLblHandView() {
        return this.lblHandView;
    }

    /**
     * Gets the lbl library view.
     * 
     * @return DevLabel
     */
    public DevLabel getLblLibraryView() {
        return this.lblLibraryView;
    }

    /**
     * Gets the lbl generate mana.
     * 
     * @return DevLabel
     */
    public DevLabel getLblGenerateMana() {
        return this.lblGenerateMana;
    }

    /**
     * Gets the lbl setup game.
     * 
     * @return DevLabel
     */
    public DevLabel getLblSetupGame() {
        return this.lblSetupGame;
    }

    /**
     * Gets the lbl tutor.
     * 
     * @return DevLabel
     */
    public DevLabel getLblTutor() {
        return this.lblTutor;
    }

    /**
     * Gets the lbl counter permanent.
     * 
     * @return DevLabel
     */
    public DevLabel getLblCounterPermanent() {
        return this.lblCounterPermanent;
    }

    /**
     * Gets the lbl tap permanent.
     * 
     * @return DevLabel
     */
    public DevLabel getLblTapPermanent() {
        return this.lblTapPermanent;
    }

    /**
     * Gets the lbl untap permanent.
     * 
     * @return DevLabel
     */
    public DevLabel getLblUntapPermanent() {
        return this.lblUntapPermanent;
    }

    /**
     * Gets the lbl unlimited lands.
     * 
     * @return DevLabel
     */
    public DevLabel getLblUnlimitedLands() {
        return this.lblUnlimitedLands;
    }

    /**
     * Gets the lbl human life.
     * 
     * @return DevLabel
     */
    public DevLabel getLblHumanLife() {
        return this.lblHumanLife;
    }

    /**
     * Gets the detail labels.
     * 
     * @return HashMap<Player, JLabel[]>
     */
    public HashMap<Player, JLabel[]> getDetailLabels() {
        return this.detailLabels;
    }

    /** Assembles Swing components for "players" panel. */
    private void populatePnlPlayers() {
        final List<Player> players = AllZone.getPlayersInGame();
        this.detailLabels = new HashMap<Player, JLabel[]>();

        for (final Player p : players) {
            // Create and store labels detailing various non-critical player
            // info.
            final InfoLabel name = new InfoLabel();
            final InfoLabel life = new InfoLabel();
            final InfoLabel hand = new InfoLabel();
            final InfoLabel draw = new InfoLabel();
            final InfoLabel prevention = new InfoLabel();
            this.detailLabels.put(p, new JLabel[] { life, hand, draw, prevention });

            // Set border on bottom label, and larger font on player name
            prevention.setBorder(new MatteBorder(0, 0, 1, 0, this.skin.getClrBorders()));
            name.setText(p.getName());
            name.setFont(this.skin.getFont1().deriveFont(Font.PLAIN, 14));

            // Add to "players" tab panel
            final String constraints = "w 97%!, gapleft 2%, gapbottom 1%";
            this.pnlPlayers.add(name, constraints);
            this.pnlPlayers.add(life, constraints);
            this.pnlPlayers.add(hand, constraints);
            this.pnlPlayers.add(draw, constraints);
            this.pnlPlayers.add(prevention, constraints);
        }
    }

    /** Assembles Swing components for "dev mode" panel. */
    private void populatePnlDev() {
        final JPanel viewport = new JPanel();
        viewport.setLayout(new MigLayout("wrap, insets 0"));
        viewport.setOpaque(false);

        final JScrollPane jsp = new JScrollPane(viewport, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        jsp.setOpaque(false);
        jsp.getViewport().setOpaque(false);

        this.pnlDev.add(jsp, "w 100%!, h 100%!");

        this.lblMilling = new DevLabel("Loss by Milling: Enabled", "Loss by Milling: Disabled");
        this.lblHandView = new DevLabel("View Any Hand: Enabled", "View Any Hand: Disabled");
        this.lblLibraryView = new DevLabel("View Any Library: Enabled", "View Any Library: Disabled");
        this.lblGenerateMana = new DevLabel("Generate Mana");
        this.lblSetupGame = new DevLabel("Setup Game State");
        this.lblTutor = new DevLabel("Tutor for card");
        this.lblCounterPermanent = new DevLabel("Add Counter to Permanent");
        this.lblTapPermanent = new DevLabel("Tap Permanent");
        this.lblUntapPermanent = new DevLabel("Untap Permanent");
        this.lblUnlimitedLands = new DevLabel("Play Unlimited Lands This Turn");
        this.lblHumanLife = new DevLabel("Set Player Life");

        final String constraints = "w 100%!, gap 0 0 5px 0";
        viewport.add(this.lblMilling, constraints);
        viewport.add(this.lblHandView, constraints);
        viewport.add(this.lblLibraryView, constraints);
        viewport.add(this.lblGenerateMana, constraints);
        viewport.add(this.lblSetupGame, constraints);
        viewport.add(this.lblTutor, constraints);
        viewport.add(this.lblCounterPermanent, constraints);
        viewport.add(this.lblTapPermanent, constraints);
        viewport.add(this.lblUntapPermanent, constraints);
        viewport.add(this.lblUnlimitedLands, constraints);
        viewport.add(this.lblHumanLife, constraints);
    }

    /** Assembles swing components for "console" panel. */
    private void populatePnlConsole() {
        final JLabel prompt = new JLabel("IN > ");
        final JTextField input = new JTextField();

        final JTextArea log = new JTextArea();
        log.setBackground(new Color(0, 0, 0, 20));
        log.setWrapStyleWord(true);
        log.setLineWrap(true);
        log.setWrapStyleWord(true);
        log.setEditable(false);
        log.setFocusable(false);
        log.setForeground(this.skin.getClrText());
        log.setFont(this.skin.getFont1().deriveFont(Font.PLAIN, 12));
        log.setBorder(new MatteBorder(1, 0, 0, 0, this.skin.getClrBorders()));

        log.setText("Not implemented yet. Input codes entered above. " + "Output data recorded below.");

        this.pnlConsole.setLayout(new MigLayout("insets 0, gap 0, wrap 2"));

        this.pnlConsole.add(prompt, "w 28%!, h 10%!, gapleft 2%, gaptop 2%, gapbottom 2%");
        this.pnlConsole.add(input, "w 68%!, gapright 2%, gaptop 2%, gapbottom 2%");
        this.pnlConsole.add(log, "w 94%!, h 80%!, gapleft 4%, span 2 1");
    }

    /**
     * Labels that act as buttons which control dev mode functions. Labels are
     * used to support multiline text.
     * 
     */
    public class DevLabel extends JLabel {
        private static final long serialVersionUID = 7917311680519060700L;

        private Color defaultBG = Color.green;
        private final Color hoverBG = ViewTabber.this.skin.getClrHover();
        private boolean enabled;
        private final String enabledText, disabledText;
        private int w, h; // Width, height, radius, insets (for paintComponent)

        private final int r;

        private final int i;

        /**
         * Labels that act as buttons which control dev mode functions. Labels
         * are used (instead of buttons) to support multiline text.
         * 
         * Constructor for DevLabel which doesn't use enabled/disabled states;
         * only single text string required.
         * 
         * @param s0
         *            &emsp; String text/tooltip of label
         */
        public DevLabel(final String s0) {
            this(s0, s0);
        }

        /**
         * Labels that act as buttons which control dev mode functions. Labels
         * are used (instead of buttons) to support multiline text.
         * 
         * This constructor for DevLabels empowers an "enable" state that
         * displays them as green (enabled) or red (disabled).
         * 
         * @param en0
         *            &emsp; String text/tooltip of label, in "enabled" state
         * @param dis0
         *            &emsp; String text/tooltip of label, in "disabled" state
         */
        public DevLabel(final String en0, final String dis0) {
            super();
            this.setUI(MultiLineLabelUI.getLabelUI());
            this.setFont(ViewTabber.this.skin.getFont1().deriveFont(Font.PLAIN, 11));
            this.setBorder(new EmptyBorder(5, 5, 5, 5));
            this.enabledText = en0;
            this.disabledText = dis0;
            this.r = 6; // Radius (for paintComponent)
            this.i = 2; // Insets (for paintComponent)
            this.setEnabled(true);

            this.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(final MouseEvent e) {
                    DevLabel.this.setBackground(DevLabel.this.hoverBG);
                }

                @Override
                public void mouseExited(final MouseEvent e) {
                    DevLabel.this.setBackground(DevLabel.this.defaultBG);
                }
            });
        }

        /**
         * Changes enabled state per boolean parameter, automatically updating
         * text string and background color.
         * 
         * @param b
         *            &emsp; boolean
         */
        @Override
        public void setEnabled(final boolean b) {
            String s;
            if (b) {
                this.defaultBG = Color.green;
                s = this.enabledText;
            } else {
                this.defaultBG = Color.red;
                s = this.disabledText;
            }
            this.enabled = b;
            this.setText(s);
            this.setToolTipText(s);
            this.setBackground(this.defaultBG);
        }

        /**
         * Gets the enabled.
         * 
         * @return boolean
         */
        public boolean getEnabled() {
            return this.enabled;
        }

        /**
         * In many cases, a DevLabel state will just be toggling a boolean. This
         * method sets up and evaluates the condition and toggles as
         * appropriate.
         * 
         */
        public void toggleEnabled() {
            if (this.enabled) {
                this.setEnabled(false);
            } else {
                this.setEnabled(true);
            }
        }

        /*
         * (non-Javadoc)
         * 
         * @see javax.swing.JComponent#paintComponent(java.awt.Graphics)
         */
        @Override
        protected void paintComponent(final Graphics g) {
            this.w = this.getWidth();
            this.h = this.getHeight();
            g.setColor(this.getBackground());
            g.fillRoundRect(this.i, this.i, this.w - (2 * this.i), this.h - this.i, this.r, this.r);
            super.paintComponent(g);
        }
    }

    /** A quick JLabel for info in "players" panel, to consolidate styling. */
    @SuppressWarnings("serial")
    private class InfoLabel extends JLabel {
        public InfoLabel() {
            super();
            this.setFont(ViewTabber.this.skin.getFont1().deriveFont(Font.PLAIN, 11));
            this.setForeground(ViewTabber.this.skin.getClrText());
        }
    }
}
