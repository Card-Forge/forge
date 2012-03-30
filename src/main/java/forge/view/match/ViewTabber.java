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
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.BorderFactory;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;

import net.miginfocom.swing.MigLayout;
import forge.AllZone;
import forge.CardList;
import forge.CardUtil;
import forge.Constant.Zone;
import forge.GameLog;
import forge.MagicStack;
import forge.Player;
import forge.Singletons;
import forge.card.spellability.SpellAbilityStackInstance;
import forge.control.match.ControlTabber;
import forge.gui.ForgeAction;
import forge.gui.MultiLineLabelUI;
import forge.gui.toolbox.FSkin;
import forge.gui.toolbox.FVerticalTabPanel;
import forge.properties.ForgePreferences.FPref;
import forge.properties.NewConstants;

/**
 * Vertical tab panel for viewing stack, combat, etc. Unfortunately, cannot
 * extend a Swing component, since vertical tabs are generated dynamically in
 * the constructor.
 * 
 */
@SuppressWarnings("serial")
public class ViewTabber extends JPanel {
    private final List<JPanel> panelList;
    private Map<Player, JLabel[]> infoLBLs;
    private JLabel stormLabel;
    private List<JTextArea> stackTARs;
    private List<JTextArea> combatTARs;
    private List<JTextArea> consoleTARs;
    private List<JLabel> devLBLs;

    private final ControlTabber control;
    private TriggerReactionMenu triggerMenu;
    private final JPanel pnlStack, pnlCombat, pnlConsole, pnlPlayers, pnlDev;

    private DevLabel lblMilling, lblGenerateMana, lblSetupGame, lblTutor, lblAddCard,
            lblCounterPermanent, lblTapPermanent, lblUntapPermanent, lblUnlimitedLands, lblSetLife;

    private final FVerticalTabPanel vtpTabber;

    private final Color activeColor, inactiveColor, hoverColor;

    /**
     * Assembles Swing components for tabber area in sidebar.
     */
    public ViewTabber() {
        this.hoverColor = FSkin.getColor(FSkin.Colors.CLR_HOVER);
        this.activeColor = FSkin.getColor(FSkin.Colors.CLR_ACTIVE);
        this.inactiveColor = FSkin.getColor(FSkin.Colors.CLR_INACTIVE);
        this.setOpaque(false);

        // Assemble card pic viewer
        this.panelList = new ArrayList<JPanel>();
        final String constraints = "wrap, insets 0 3% 0 0, gap 0";
        stackTARs = new ArrayList<JTextArea>();
        combatTARs = new ArrayList<JTextArea>();
        consoleTARs = new ArrayList<JTextArea>();
        devLBLs = new ArrayList<JLabel>();

        // Trigger Context Menu creation
        this.triggerMenu = new TriggerReactionMenu();

        this.pnlStack = new JPanel();
        this.pnlStack.setName("Stack");
        this.pnlStack.setOpaque(false);
        this.pnlStack.setLayout(new MigLayout(constraints));
        this.pnlStack.setToolTipText("View Stack");
        this.panelList.add(this.pnlStack);

        this.pnlCombat = new JPanel();
        this.pnlCombat.setName("Combat");
        this.pnlCombat.setOpaque(false);
        this.pnlCombat.setLayout(new MigLayout(constraints));
        this.pnlCombat.setToolTipText("View Combat");
        this.panelList.add(this.pnlCombat);

        this.pnlConsole = new JPanel();
        this.pnlConsole.setName("Log");
        this.pnlConsole.setOpaque(false);
        this.pnlConsole.setLayout(new MigLayout(constraints));
        this.pnlConsole.setToolTipText("View Console");
        this.panelList.add(this.pnlConsole);

        this.pnlPlayers = new JPanel();
        this.pnlPlayers.setName("Players");
        this.pnlPlayers.setOpaque(false);
        this.pnlPlayers.setLayout(new MigLayout(constraints));
        this.pnlPlayers.setToolTipText("Player List");
        this.panelList.add(this.pnlPlayers);

        this.pnlDev = new JPanel();
        this.pnlDev.setName("Dev");
        this.pnlDev.setOpaque(false);
        this.pnlDev.setLayout(new MigLayout(constraints));
        this.pnlDev.setToolTipText("Developer Mode");
        this.panelList.add(this.pnlDev);

        // Populate the various panels in the tabber.
        this.populatePnlDev();
        this.populatePnlPlayers();
        this.populatePnlConsole();

        this.vtpTabber = new FVerticalTabPanel(this.panelList);
        this.setBackground(FSkin.getColor(FSkin.Colors.CLR_THEME));
        this.setLayout(new MigLayout("insets 0, gap 0"));

        this.add(vtpTabber, "w 97%!, h 100%!, gapleft 2%");

        // Resize adapter
        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                int big = getWidth() / 15, regular, x;
                big = (big < 13 ? 13 : big);
                regular = big - 2;

                // Player panel info
                Iterator<Entry<Player, JLabel[]>> it = infoLBLs.entrySet().iterator();
                while (it.hasNext()) {
                    JLabel[] labels = (JLabel[]) it.next().getValue();
                    for (x = 0; x < labels.length; x++) {
                        if (x > 0) {
                            labels[x].setFont(FSkin.getFont(regular));
                        } else {
                            labels[x].setFont(FSkin.getFont(big));
                        }
                    }
                }

                // Storm label
                stormLabel.setFont(FSkin.getFont(big));

                // Stack text areas
                for (JTextArea tar : stackTARs) {
                    tar.setFont(FSkin.getFont(big));
                }

                // Combat text areas
                for (JTextArea tar : combatTARs) {
                    tar.setFont(FSkin.getFont(big));
                }

                // Console text areas
                for (JTextArea tar : consoleTARs) {
                    tar.setFont(FSkin.getFont(big));
                }

                // Devmode Labels
                for (JLabel lbl : devLBLs) {
                    lbl.setFont(FSkin.getFont(regular));
                }
            }
        });

        // After all components are in place, instantiate controller.
        this.control = new ControlTabber(this);
    }

    /**
     * Gets the controller.
     * 
     * @return ControlTabber
     */
    public ControlTabber getControl() {
        return this.control;
    }

    /**
     * Removes and adds JTextAreas to stack panel, which briefly summarize the
     * spell and allow mouseover.
     * 
     */
    public void updateStack() {
        final MagicStack stack = AllZone.getStack();

        int count = 1;
        JTextArea tar;
        String txt, isOptional;

        this.pnlStack.removeAll();
        this.control.showPnlStack();

        this.vtpTabber.getAllVTabs().get(ControlTabber.STACK_PANEL).setText("Stack : " + stack.size());

        // final Border border = new LineBorder(FSkin.getClrBorders(), 1);
        final Border border = new EmptyBorder(5, 5, 5, 5);
        Color[] scheme;

        stackTARs.clear();
        for (int i = stack.size() - 1; 0 <= i; i--) {
            final SpellAbilityStackInstance spell = stack.peekInstance(i);
            final int index = i;

            scheme = getSpellColor(spell);

            isOptional = stack.peekAbility(i).isOptionalTrigger()
                    && stack.peekAbility(i).getSourceCard().getController().isHuman() ? "(OPTIONAL) " : "";
            txt = (count++) + ". " + isOptional + spell.getStackDescription();
            tar = new JTextArea(txt);
            tar.setToolTipText(txt);
            tar.setOpaque(true);
            tar.setBorder(border);
            tar.setForeground(scheme[1]);
            tar.setBackground(scheme[0]);

            tar.setFocusable(false);
            tar.setEditable(false);
            tar.setLineWrap(true);
            tar.setWrapStyleWord(true);

            /*
             * TODO - we should figure out how to display cards on the stack in
             * the Picture/Detail panel The problem not is that when a computer
             * casts a Morph, the real card shows because Picture/Detail checks
             * isFaceDown() which will be false on for spell.getSourceCard() on
             * the stack.
             */

            // this functionality was present in v 1.1.8
            tar.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(final MouseEvent e) {
                    Singletons.getControl().getControlMatch().setCard(spell.getSpellAbility().getSourceCard());
                }
            });

            /*
             * This updates the Card Picture/Detail when the spell is added to
             * the stack. This funcaitonality was not present in v 1.1.8.
             * 
             * Problem is described in TODO right above this.
             */
            /*
             * if (i == 0) {
             * AllZone.getDisplay().setCard(spell.getSourceCard()); }
             */

            this.pnlStack.add(tar, "w 98%!, gapright 1%, gaptop 1%");
            stackTARs.add(tar);

            if (stack.peekInstance(i).isOptionalTrigger()) {
                tar.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mousePressed(final MouseEvent e) {

                        if (e.getButton() != MouseEvent.BUTTON3) {
                            return;
                        }

                        ViewTabber.this.triggerMenu.setTrigger(stack.peekAbility(index).getSourceTrigger());
                        ViewTabber.this.triggerMenu.show(e.getComponent(), e.getX(), e.getY());
                    }
                });
            }
        }

        Singletons.getView().getViewMatch().getBtnOK().requestFocusInWindow();
    }

    /** Returns array with [background, foreground] colors. */
    private Color[] getSpellColor(SpellAbilityStackInstance s0) {
        if (CardUtil.getColors(s0.getSourceCard()).size() > 1) {
            return new Color[] { new Color(253, 175, 63), Color.black };
        } else if (s0.getSourceCard().isBlack()) {
            return new Color[] { Color.black, Color.white };
        } else if (s0.getSourceCard().isBlue()) {
            return new Color[] { new Color(71, 108, 191), Color.white };
        } else if (s0.getSourceCard().isGreen()) {
            return new Color[] { new Color(23, 95, 30), Color.white };
        } else if (s0.getSourceCard().isRed()) {
            return new Color[] { new Color(214, 8, 8), Color.white };
        } else if (s0.getSourceCard().isWhite()) {
            return new Color[] { Color.white, Color.black };
        } else if (s0.getSourceCard().isArtifact() || s0.getSourceCard().isLand()) {
            return new Color[] { new Color(111, 75, 43), Color.white };
        }

        return new Color[] { new Color(0, 0, 0, 0), FSkin.getColor(FSkin.Colors.CLR_TEXT) };
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

        // if this is not cleared every time, we keep a history of combat
        // strings. Not very useful.
        // probably will never be useful, even if we have multiple text areas to
        // display combat...
        this.combatTARs.clear();
        this.control.showPnlCombat();

        final Border border = new MatteBorder(0, 0, 0, 0, FSkin.getColor(FSkin.Colors.CLR_BORDERS));

        this.vtpTabber.getAllVTabs().get(ControlTabber.COMBAT_PANEL)
                .setText("Combat : " + AllZone.getCombat().getAttackers().size());

        final JTextArea tar = new JTextArea(s);
        tar.setOpaque(false);
        tar.setBorder(border);
        tar.setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT));
        tar.setFocusable(false);
        tar.setLineWrap(true);
        this.pnlCombat.add(tar, "w 95%!, gapleft 3%, gaptop 1%, h 95%");
        combatTARs.add(tar);
    }

    /**
     * Sets the text for the GameLog.
     * 
     */
    public void updateConsole() {
        final GameLog gl = AllZone.getGameLog();

        this.pnlConsole.removeAll();
        // final Border border = new MatteBorder(0, 0, 0, 0,
        // FSkin.getClrBorders());

        // by default, grab everything logging level 3 or less
        // TODO - some option to make this configurable is probably desirable
        // TODO - add these components to resize adapter in constructor
        JTextArea tar = new JTextArea(gl.getLogText(3));
        tar.setOpaque(false);
        // tar.setBorder(border);
        tar.setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT));

        tar.setFocusable(false);
        tar.setEditable(false);
        tar.setLineWrap(true);
        tar.setWrapStyleWord(true);

        JScrollPane jsp = new JScrollPane(tar);
        jsp.setOpaque(false);
        jsp.getViewport().setOpaque(false);

        this.pnlConsole.add(jsp, "w 95%!, gapleft 3%, gaptop 1%");
    }

    /**
     * Updates labels in the "player" panel, which display non-critical details
     * about each player in the game.
     * 
     * @param p0
     *            &emsp; Player obj
     */
    public void updatePlayerLabels(final Player p0) {
        final JLabel[] temp = this.infoLBLs.get(p0);
        temp[1].setText("Life: " + String.valueOf(p0.getLife()) + "  |  Poison counters: "
                + String.valueOf(p0.getPoisonCounters()));
        temp[2].setText("Maximum hand size: " + String.valueOf(p0.getMaxHandSize()));
        temp[3].setText("Cards drawn this turn: " + String.valueOf(p0.getNumDrawnThisTurn()));
        temp[4].setText("Damage Prevention: " + String.valueOf(p0.getPreventNextDamage()));
        if (!p0.getKeywords().isEmpty()) {
            temp[5].setText(p0.getKeywords().toString());
        } else {
            temp[5].setText("");
        }
        if (Singletons.getModel().getPreferences().getPrefBoolean(FPref.UI_ANTE)) {
            CardList list = p0.getCardsIn(Zone.Ante);
            StringBuilder sb = new StringBuilder();
            sb.append("Ante'd: ");
            for (int i = 0; i < list.size(); i++) {
                sb.append(list.get(i));
                if (i < (list.size() - 1)) {
                    sb.append(", ");
                }
            }
            temp[6].setText(sb.toString());
        }

        stormLabel.setText("Storm count: " + AllZone.getStack().getCardsCastThisTurn().size());
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
     * @return {@link javax.swing.JPanel}
     */
    public JPanel getPnlStack() {
        return this.pnlStack;
    }

    /**
     * Gets the pnl combat.
     * 
     * @return {@link javax.swing.JPanel}
     */
    public JPanel getPnlCombat() {
        return this.pnlCombat;
    }

    /**
     * Gets the pnl console.
     * 
     * @return {@link javax.swing.JPanel}
     */
    public JPanel getPnlConsole() {
        return this.pnlConsole;
    }

    /**
     * Gets the pnl players.
     * 
     * @return {@link javax.swing.JPanel}
     */
    public JPanel getPnlPlayers() {
        return this.pnlPlayers;
    }

    /**
     * Gets the pnl dev.
     * 
     * @return {@link javax.swing.JPanel}
     */
    public JPanel getPnlDev() {
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

    public DevLabel getAnyCard() {
        return this.lblAddCard;
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
    public DevLabel getLblSetLife() {
        return this.lblSetLife;
    }

    /** Assembles Swing components for "players" panel. */
    private void populatePnlPlayers() {
        final List<Player> players = AllZone.getPlayersInGame();
        this.infoLBLs = new HashMap<Player, JLabel[]>();

        final String constraints = "w 97%!, gapleft 2%, gapbottom 1%";

        for (final Player p : players) {
            // Create and store labels detailing various non-critical player
            // info.
            final InfoLabel name = new InfoLabel();
            final InfoLabel life = new InfoLabel();
            final InfoLabel hand = new InfoLabel();
            final InfoLabel draw = new InfoLabel();
            final InfoLabel prevention = new InfoLabel();
            final InfoLabel keywords = new InfoLabel();
            final InfoLabel antes = new InfoLabel();
            this.infoLBLs.put(p, new JLabel[] { name, life, hand, draw, prevention, keywords, antes });

            // Set border on bottom label, and larger font on player name
            antes.setBorder(new MatteBorder(0, 0, 1, 0, FSkin.getColor(FSkin.Colors.CLR_BORDERS)));
            name.setText(p.getName());

            // Add to "players" tab panel
            this.pnlPlayers.add(name, constraints);
            this.pnlPlayers.add(life, constraints);
            this.pnlPlayers.add(hand, constraints);
            this.pnlPlayers.add(draw, constraints);
            this.pnlPlayers.add(prevention, constraints);
            this.pnlPlayers.add(keywords, constraints);
            this.pnlPlayers.add(antes, constraints);
        }

        stormLabel = new InfoLabel();
        this.pnlPlayers.add(stormLabel, constraints);
    }

    /** Assembles Swing components for "dev mode" panel. */
    private void populatePnlDev() {
        final JPanel viewport = new JPanel();
        viewport.setLayout(new MigLayout("wrap, insets 0"));
        viewport.setOpaque(false);

        final JScrollPane jsp = new JScrollPane(viewport, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        jsp.setBorder(BorderFactory.createEmptyBorder());
        jsp.setOpaque(false);
        jsp.getViewport().setOpaque(false);

        this.pnlDev.add(jsp, "w 98%!, h 98%!");

        lblMilling = new DevLabel("Loss by Milling: Enabled", "Loss by Milling: Disabled");
        lblUnlimitedLands = new DevLabel("Play Unlimited Lands This Turn: Enabled",
                "Play Unlimited Lands This Turn: Disabled");
        lblGenerateMana = new DevLabel("Generate Mana");
        lblSetupGame = new DevLabel("Setup Game State");
        lblTutor = new DevLabel("Tutor for Card");
        lblCounterPermanent = new DevLabel("Add Counter to Permanent");
        lblTapPermanent = new DevLabel("Tap Permanent");
        lblUntapPermanent = new DevLabel("Untap Permanent");
        lblSetLife = new DevLabel("Set Player Life");
        lblAddCard = new DevLabel("Add any card");

        devLBLs.add(lblMilling);
        // devLBLs.add(lblHandView);
        // devLBLs.add(lblLibraryView);
        devLBLs.add(lblUnlimitedLands);
        devLBLs.add(lblGenerateMana);
        devLBLs.add(lblSetupGame);
        devLBLs.add(lblTutor);
        devLBLs.add(lblAddCard);
        devLBLs.add(lblCounterPermanent);
        devLBLs.add(lblTapPermanent);
        devLBLs.add(lblUntapPermanent);
        devLBLs.add(lblSetLife);

        final String constraints = "w 95%!, gap 0 0 5px 0";
        viewport.add(this.lblMilling, constraints);
        // viewport.add(this.lblHandView, constraints);
        // viewport.add(this.lblLibraryView, constraints);
        viewport.add(this.lblUnlimitedLands, constraints);
        viewport.add(this.lblGenerateMana, constraints);
        viewport.add(this.lblSetupGame, constraints);
        viewport.add(this.lblTutor, constraints);
        viewport.add(this.lblAddCard, constraints);
        viewport.add(this.lblCounterPermanent, constraints);
        viewport.add(this.lblTapPermanent, constraints);
        viewport.add(this.lblUntapPermanent, constraints);
        viewport.add(this.lblSetLife, constraints);
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
        log.setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT));
        log.setBorder(new MatteBorder(1, 0, 0, 0, FSkin.getColor(FSkin.Colors.CLR_BORDERS)));

        log.setText("No log information yet. Input codes entered above. " + "Output data recorded below.");

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

        private Color defaultBG = ViewTabber.this.activeColor;
        private final Color hoverBG = ViewTabber.this.hoverColor;
        private final Color pressedBG = ViewTabber.this.inactiveColor;
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
            this.setBorder(new EmptyBorder(5, 5, 5, 5));
            this.enabledText = en0;
            this.disabledText = dis0;
            this.r = 6; // Radius (for paintComponent)
            this.i = 2; // Insets (for paintComponent)
            this.setEnabled(true);
            this.setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT));

            this.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(final MouseEvent e) {
                    DevLabel.this.setBackground(DevLabel.this.pressedBG);
                }

                @Override
                public void mouseReleased(final MouseEvent e) {
                    DevLabel.this.setBackground(DevLabel.this.defaultBG);
                }

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
                this.defaultBG = ViewTabber.this.activeColor;
                s = this.enabledText;
            } else {
                this.defaultBG = ViewTabber.this.inactiveColor;
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
    private class InfoLabel extends JLabel {
        public InfoLabel() {
            super();
            this.setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT));
        }
    }

    private class TriggerReactionMenu extends JPopupMenu {
        private static final long serialVersionUID = 6665085414634139984L;
        private int workTrigID;

        public TriggerReactionMenu() {
            super();

            final ForgeAction actAccept = new ForgeAction(NewConstants.Lang.GuiDisplay.Trigger.ALWAYSACCEPT) {
                private static final long serialVersionUID = -3734674058185367612L;

                @Override
                public final void actionPerformed(final ActionEvent e) {
                    AllZone.getTriggerHandler().setAlwaysAcceptTrigger(TriggerReactionMenu.this.workTrigID);
                }
            };

            final ForgeAction actDecline = new ForgeAction(NewConstants.Lang.GuiDisplay.Trigger.ALWAYSDECLINE) {
                private static final long serialVersionUID = -1983295769159971502L;

                @Override
                public final void actionPerformed(final ActionEvent e) {
                    AllZone.getTriggerHandler().setAlwaysDeclineTrigger(TriggerReactionMenu.this.workTrigID);
                }
            };

            final ForgeAction actAsk = new ForgeAction(NewConstants.Lang.GuiDisplay.Trigger.ALWAYSASK) {
                private static final long serialVersionUID = 5045255351332940821L;

                @Override
                public final void actionPerformed(final ActionEvent e) {
                    AllZone.getTriggerHandler().setAlwaysAskTrigger(TriggerReactionMenu.this.workTrigID);
                }
            };

            final JCheckBoxMenuItem jcbmiAccept = new JCheckBoxMenuItem(actAccept);
            final JCheckBoxMenuItem jcbmiDecline = new JCheckBoxMenuItem(actDecline);
            final JCheckBoxMenuItem jcbmiAsk = new JCheckBoxMenuItem(actAsk);

            this.add(jcbmiAccept);
            this.add(jcbmiDecline);
            this.add(jcbmiAsk);
        }

        public void setTrigger(final int trigID) {
            this.workTrigID = trigID;

            if (AllZone.getTriggerHandler().isAlwaysAccepted(trigID)) {
                ((JCheckBoxMenuItem) this.getComponent(0)).setState(true);
                ((JCheckBoxMenuItem) this.getComponent(1)).setState(false);
                ((JCheckBoxMenuItem) this.getComponent(2)).setState(false);
            } else if (AllZone.getTriggerHandler().isAlwaysDeclined(trigID)) {
                ((JCheckBoxMenuItem) this.getComponent(0)).setState(false);
                ((JCheckBoxMenuItem) this.getComponent(1)).setState(true);
                ((JCheckBoxMenuItem) this.getComponent(2)).setState(false);
            } else {
                ((JCheckBoxMenuItem) this.getComponent(0)).setState(false);
                ((JCheckBoxMenuItem) this.getComponent(1)).setState(false);
                ((JCheckBoxMenuItem) this.getComponent(2)).setState(true);
            }
        }
    }
}
