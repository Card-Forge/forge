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
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;

import net.miginfocom.swing.MigLayout;
import arcane.ui.PlayArea;
import forge.AllZone;
import forge.Constant.Zone;
import forge.Player;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.mana.ManaPool;
import forge.control.match.ControlField;
import forge.view.toolbox.FRoundedPanel;
import forge.view.toolbox.FSkin;

/**
 * Assembles Swing components of player field instance.
 * 
 */
@SuppressWarnings("serial")
public class ViewField extends FRoundedPanel {
    private final FSkin skin;
    private int counter;

    private final ControlField control;
    private final PlayArea tabletop;

    private final Border hoverBorder, inactiveBorder;

    private DetailLabel lblHand, lblGraveyard, lblLibrary, lblExile,
        lblFlashback, lblPoison, lblBlack, lblBlue,
        lblGreen, lblRed, lblWhite, lblColorless;

    private PhaseLabel lblUpkeep, lblDraw, lblMain1, lblBeginCombat,
        lblDeclareAttackers, lblDeclareBlockers, lblFirstStrike, lblCombatDamage,
        lblEndCombat, lblMain2, lblEndTurn, lblCleanup;

    private JPanel avatarArea, phaseArea, poolArea;
    private JLabel lblAvatar, lblLife;
    private Image img;
    private final Color transparent = new Color(0, 0, 0, 0);

    /**
     * Assembles Swing components of player field instance.
     * 
     * @param player
     *            &emsp; a Player object.
     */
    public ViewField(final Player player) {
        super();
        this.setOpaque(false);
        this.setLayout(new MigLayout("insets 0, gap 0"));
        this.setCornerRadius(5);
        this.setToolTipText(player.getName() + " Gameboard");
        this.setBackground(AllZone.getSkin().getColor("theme"));

        this.skin = AllZone.getSkin();
        this.inactiveBorder = new LineBorder(new Color(0, 0, 0, 0), 1);
        this.hoverBorder = new LineBorder(this.skin.getColor("borders"), 1);
        this.counter = -1;

        String filename = "res/pics/icons/" + (player.isHuman() ? "Mage01.jpg" : "Mage02.jpg");
        img = new ImageIcon(filename).getImage();

        // Avatar and life
        avatarArea = new JPanel();
        avatarArea.setOpaque(false);
        avatarArea.setBackground(skin.getColor("hover"));
        avatarArea.setLayout(new MigLayout("insets 0, gap 0"));

        lblAvatar = new JLabel();
        lblAvatar.setHorizontalAlignment(SwingConstants.CENTER);
        avatarArea.add(lblAvatar, "w 100%!, wrap, gaptop 2px");

        lblLife = new JLabel();
        lblLife.setHorizontalAlignment(SwingConstants.CENTER);
        lblLife.setForeground(skin.getColor("text"));
        avatarArea.add(lblLife, "w 100%!, h 30%!");

        this.add(avatarArea, "w 10%!, h 30%!");

        // Phases
        phaseArea = new JPanel();
        phaseArea.setOpaque(false);
        phaseArea.setLayout(new MigLayout("insets 0 0 1% 0, gap 0, wrap"));
        addPhaseIndicators();
        this.add(phaseArea, "w 5%!, h 100%!, span 1 2");

        // Play area
        final JScrollPane scroller = new JScrollPane();

        this.tabletop = new PlayArea(scroller, player.equals(AllZone.getComputerPlayer()) ? true : false);
        this.tabletop.setBorder(new MatteBorder(0, 1, 0, 0, this.skin.getColor("borders")));
        this.tabletop.setOpaque(false);

        scroller.setViewportView(this.tabletop);
        scroller.setOpaque(false);
        scroller.getViewport().setOpaque(false);
        scroller.setBorder(null);

        this.add(scroller, "w 85%!, h 100%!, span 1 2, wrap");

        // Pool info
        poolArea = new JPanel();
        poolArea.setOpaque(false);
        poolArea.setLayout(new MigLayout("insets 0, gap 0, wrap 2, filly"));
        addPoolLabels();
        this.add(poolArea, "w 10%!, h 70%!");

        // Resize adapter
        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                int side = (int) (avatarArea.getHeight() * 0.7);
                int size = (int) (avatarArea.getHeight() * 0.24);
                lblLife.setFont(ViewField.this.skin.getFont1().deriveFont(Font.BOLD, (size)));
                lblAvatar.setIcon(new ImageIcon(img.getScaledInstance(side, side, java.awt.Image.SCALE_SMOOTH)));
            }
        });

        // Player hover effect
        avatarArea.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(final MouseEvent e) {
                ViewField.this.avatarArea.setOpaque(true);
                ViewField.this.avatarArea.setBorder(ViewField.this.hoverBorder);
            }

            @Override
            public void mouseExited(final MouseEvent e) {
                ViewField.this.avatarArea.setOpaque(false);
                ViewField.this.avatarArea.setBorder(ViewField.this.inactiveBorder);
            }
        });

        //============

        // After all components are in place, instantiate controller.
        this.control = new ControlField(player, this);
    }

    /**
     * Gets the controller.
     * 
     * @return ControlField
     */
    public ControlField getController() {
        return this.control;
    }

    /** Adds phase indicator labels to phase area JPanel container. */
    public void addPhaseIndicators() {
        // Constraints string, set once
        final String constraints = "w 94%!, h 7.2%, gaptop 1%, gapleft 3%";

        ViewField.this.lblUpkeep = new PhaseLabel("UP");
        ViewField.this.lblUpkeep.setToolTipText("<html>Phase: Upkeep<br>Click to toggle.</html>");
        phaseArea.add(ViewField.this.lblUpkeep, constraints);

        ViewField.this.lblDraw = new PhaseLabel("DR");
        ViewField.this.lblDraw.setToolTipText("<html>Phase: Draw<br>Click to toggle.</html>");
        phaseArea.add(ViewField.this.lblDraw, constraints);

        ViewField.this.lblMain1 = new PhaseLabel("M1");
        ViewField.this.lblMain1.setToolTipText("<html>Phase: Main 1<br>Click to toggle.</html>");
        phaseArea.add(ViewField.this.lblMain1, constraints);

        ViewField.this.lblBeginCombat = new PhaseLabel("BC");
        ViewField.this.lblBeginCombat.setToolTipText("<html>Phase: Begin Combat<br>Click to toggle.</html>");
        phaseArea.add(ViewField.this.lblBeginCombat, constraints);

        ViewField.this.lblDeclareAttackers = new PhaseLabel("DA");
        ViewField.this.lblDeclareAttackers.setToolTipText("<html>Phase: Declare Attackers<br>Click to toggle.</html>");
        phaseArea.add(ViewField.this.lblDeclareAttackers, constraints);

        ViewField.this.lblDeclareBlockers = new PhaseLabel("DB");
        ViewField.this.lblDeclareBlockers.setToolTipText("<html>Phase: Declare Blockers<br>Click to toggle.</html>");
        phaseArea.add(ViewField.this.lblDeclareBlockers, constraints);

        ViewField.this.lblFirstStrike = new PhaseLabel("FS");
        ViewField.this.lblFirstStrike.setToolTipText("<html>Phase: First Strike Damage<br>Click to toggle.</html>");
        phaseArea.add(ViewField.this.lblFirstStrike, constraints);

        ViewField.this.lblCombatDamage = new PhaseLabel("CD");
        ViewField.this.lblCombatDamage.setToolTipText("<html>Phase: Combat Damage<br>Click to toggle.</html>");
        phaseArea.add(ViewField.this.lblCombatDamage, constraints);

        ViewField.this.lblEndCombat = new PhaseLabel("EC");
        ViewField.this.lblEndCombat.setToolTipText("<html>Phase: End Combat<br>Click to toggle.</html>");
        phaseArea.add(ViewField.this.lblEndCombat, constraints);

        ViewField.this.lblMain2 = new PhaseLabel("M2");
        ViewField.this.lblMain2.setToolTipText("<html>Phase: Main 2<br>Click to toggle.</html>");
        phaseArea.add(ViewField.this.lblMain2, constraints);

        ViewField.this.lblEndTurn = new PhaseLabel("ET");
        ViewField.this.lblEndTurn.setToolTipText("<html>Phase: End Turn<br>Click to toggle.</html>");
        phaseArea.add(ViewField.this.lblEndTurn, constraints);

        ViewField.this.lblCleanup = new PhaseLabel("CL");
        ViewField.this.lblCleanup.setToolTipText("<html>Phase: Cleanup<br>Click to toggle.</html>");
        phaseArea.add(ViewField.this.lblCleanup, constraints);
    }

    /** Adds various labels to pool area JPanel container. */
    public void addPoolLabels() {
        final String constraintsL = "w 47%!, h 12.5%!, growy, gapleft 3%";
        final String constraintsR = "w 47%!, h 12.5%!, growy, gapright 3%";

        // Hand, library, graveyard, exile, flashback, poison labels
        ViewField.this.lblHand = new DetailLabel("zone.hand", "99");
        ViewField.this.lblHand.setToolTipText("Cards in hand");
        poolArea.add(ViewField.this.lblHand, constraintsL);

        ViewField.this.lblLibrary = new DetailLabel("zone.library", "99");
        ViewField.this.lblLibrary.setToolTipText("Cards in library");
        poolArea.add(ViewField.this.lblLibrary, constraintsR);

        ViewField.this.lblGraveyard = new DetailLabel("zone.graveyard", "99");
        ViewField.this.lblGraveyard.setToolTipText("Cards in graveyard");
        poolArea.add(ViewField.this.lblGraveyard, constraintsL);

        ViewField.this.lblExile = new DetailLabel("zone.exile", "99");
        ViewField.this.lblExile.setToolTipText("Exiled cards");
        poolArea.add(ViewField.this.lblExile, constraintsR);

        ViewField.this.lblFlashback = new DetailLabel("zone.flashback", "99");
        ViewField.this.lblFlashback.setToolTipText("Flashback cards");
        poolArea.add(ViewField.this.lblFlashback, constraintsL);

        ViewField.this.lblPoison = new DetailLabel("zone.poison", "99");
        ViewField.this.lblPoison.setToolTipText("Poison counters");
        poolArea.add(ViewField.this.lblPoison, constraintsR);

        // Black, Blue, Colorless, Green, Red, White mana labels
        ViewField.this.lblBlack = new DetailLabel("mana.black", "99");
        ViewField.this.lblBlack.setToolTipText("Black mana");
        poolArea.add(ViewField.this.lblBlack, constraintsL);

        ViewField.this.lblBlue = new DetailLabel("mana.blue", "99");
        ViewField.this.lblBlue.setToolTipText("Blue mana");
        poolArea.add(ViewField.this.lblBlue, constraintsR);

        ViewField.this.lblGreen = new DetailLabel("mana.green", "99");
        ViewField.this.lblGreen.setToolTipText("Green mana");
        poolArea.add(ViewField.this.lblGreen, constraintsL);

        ViewField.this.lblRed = new DetailLabel("mana.red", "99");
        ViewField.this.lblRed.setToolTipText("Red mana");
        poolArea.add(ViewField.this.lblRed, constraintsR);

        ViewField.this.lblWhite = new DetailLabel("mana.white", "99");
        ViewField.this.lblWhite.setToolTipText("White mana");
        poolArea.add(ViewField.this.lblWhite, constraintsL);

        ViewField.this.lblColorless = new DetailLabel("mana.colorless", "99");
        ViewField.this.lblColorless.setToolTipText("Colorless mana");
        poolArea.add(ViewField.this.lblColorless, constraintsR);
    }

    // ========== Observer update methods
    /**
     * Handles observer update of player Zones - hand, graveyard, etc.
     * 
     * @param p0
     *            &emsp; Player obj
     */
    public void updateZones(final Player p0) {
        this.getLblHand().setText("" + p0.getZone(Zone.Hand).size());
        this.getLblGraveyard().setText("" + p0.getZone(Zone.Graveyard).size());
        this.getLblLibrary().setText("" + p0.getZone(Zone.Library).size());
        this.getLblFlashback().setText("" + CardFactoryUtil.getExternalZoneActivationCards(p0).size());
        this.getLblExile().setText("" + p0.getZone(Zone.Exile).size());
    }

    /**
     * Handles observer update of non-Zone details - life, poison, etc. Also
     * updates "players" panel in tabber for this player.
     * 
     * @param p0
     *            &emsp; Player obj
     */
    public void updateDetails(final Player p0) {
        // "Players" panel update
        final ViewTopLevel t = (ViewTopLevel) AllZone.getDisplay();
        t.getTabberController().getView().updatePlayerLabels(p0);

        // Poison/life
        this.getLblLife().setText("" + p0.getLife());
        this.getLblPoison().setText("" + p0.getPoisonCounters());

        if (p0.getLife() <= 5) {
            this.getLblLife().setForeground(Color.red);
        }
        else {
            this.getLblLife().setForeground(skin.getColor("text"));
        }

        if (p0.getPoisonCounters() >= 8) {
            this.getLblPoison().setForeground(Color.red);
        }
        else {
            this.getLblPoison().setForeground(skin.getColor("text"));
        }

        //mana pool
        updateManaPool(p0);
    }

    /**
     * Handles observer update of the mana pool.
     * 
     * @param p0
     *            &emsp; Player obj
     */
    public void updateManaPool(final Player p0) {
        ManaPool m = p0.getManaPool();
        getLblBlack().setText("" + m.getAmountOfColor(forge.Constant.Color.BLACK));
        getLblBlue().setText("" + m.getAmountOfColor(forge.Constant.Color.BLUE));
        getLblGreen().setText("" + m.getAmountOfColor(forge.Constant.Color.GREEN));
        getLblRed().setText("" + m.getAmountOfColor(forge.Constant.Color.RED));
        getLblWhite().setText("" + m.getAmountOfColor(forge.Constant.Color.WHITE));
        getLblColorless().setText("" + m.getAmountOfColor(forge.Constant.Color.COLORLESS));
    }

    // ========= Retrieval methods
    /**
     * Gets the tabletop.
     *
     * @return PlayArea where cards for this field are in play
     */
    public PlayArea getTabletop() {
        return this.tabletop;
    }

    /**
     * Gets the avatar area.
     *
     * @return JPanel containing avatar pic and life label
     */
    public JPanel getAvatarArea() {
        return this.avatarArea;
    }

    /**
     * Gets the lbl life.
     *
     * @return DetailLabel
     */
    public JLabel getLblLife() {
        return this.lblLife;
    }

    /**
     * Gets the lbl hand.
     *
     * @return DetailLabel for hand cards
     */
    public DetailLabel getLblHand() {
        return this.lblHand;
    }

    /**
     * Gets the lbl library.
     *
     * @return DetailLabel for library cards
     */
    public DetailLabel getLblLibrary() {
        return this.lblLibrary;
    }

    /**
     * Gets the lbl graveyard.
     *
     * @return DetailLabel for graveyard cards
     */
    public DetailLabel getLblGraveyard() {
        return this.lblGraveyard;
    }

    /**
     * Gets the lbl exile.
     *
     * @return DetailLabel for exiled cards
     */
    public DetailLabel getLblExile() {
        return this.lblExile;
    }

    /**
     * Gets the lbl flashback.
     *
     * @return DetailLabel for flashback cards
     */
    public DetailLabel getLblFlashback() {
        return this.lblFlashback;
    }

    /**
     * Gets the lbl poison.
     *
     * @return DetailLabel for poison counters
     */
    public DetailLabel getLblPoison() {
        return this.lblPoison;
    }

    /**
     * Gets the lbl colorless.
     *
     * @return DetailLabel for colorless mana count
     */
    public DetailLabel getLblColorless() {
        return this.lblColorless;
    }

    /**
     * Gets the lbl black.
     *
     * @return DetailLabel for black mana count
     */
    public DetailLabel getLblBlack() {
        return this.lblBlack;
    }

    /**
     * Gets the lbl blue.
     *
     * @return DetailLabel for blue mana count
     */
    public DetailLabel getLblBlue() {
        return this.lblBlue;
    }

    /**
     * Gets the lbl green.
     *
     * @return DetailLabel for green mana count
     */
    public DetailLabel getLblGreen() {
        return this.lblGreen;
    }

    /**
     * Gets the lbl red.
     *
     * @return DetailLabel for red mana count
     */
    public DetailLabel getLblRed() {
        return this.lblRed;
    }

    /**
     * Gets the lbl white.
     *
     * @return DetailLabel for white mana count
     */
    public DetailLabel getLblWhite() {
        return this.lblWhite;
    }

    // Phases
    /**
     * Gets the lbl upkeep.
     *
     * @return PhaseLabel
     */
    public PhaseLabel getLblUpkeep() {
        return this.lblUpkeep;
    }

    /**
     * Gets the lbl draw.
     *
     * @return PhaseLabel
     */
    public PhaseLabel getLblDraw() {
        return this.lblDraw;
    }

    /**
     * Gets the lbl main1.
     *
     * @return PhaseLabel
     */
    public PhaseLabel getLblMain1() {
        return this.lblMain1;
    }

    /**
     * Gets the lbl begin combat.
     *
     * @return PhaseLabel
     */
    public PhaseLabel getLblBeginCombat() {
        return this.lblBeginCombat;
    }

    /**
     * Gets the lbl declare attackers.
     *
     * @return PhaseLabel
     */
    public PhaseLabel getLblDeclareAttackers() {
        return this.lblDeclareAttackers;
    }

    /**
     * Gets the lbl declare blockers.
     *
     * @return PhaseLabel
     */
    public PhaseLabel getLblDeclareBlockers() {
        return this.lblDeclareBlockers;
    }

    /**
     * Gets the lbl combat damage.
     *
     * @return PhaseLabel
     */
    public PhaseLabel getLblCombatDamage() {
        return this.lblCombatDamage;
    }

    /**
     * Gets the lbl first strike.
     *
     * @return PhaseLabel
     */
    public PhaseLabel getLblFirstStrike() {
        return this.lblFirstStrike;
    }

    /**
     * Gets the lbl end combat.
     *
     * @return PhaseLabel
     */
    public PhaseLabel getLblEndCombat() {
        return this.lblEndCombat;
    }

    /**
     * Gets the lbl main2.
     *
     * @return PhaseLabel
     */
    public PhaseLabel getLblMain2() {
        return this.lblMain2;
    }

    /**
     * Gets the lbl end turn.
     *
     * @return PhaseLabel
     */
    public PhaseLabel getLblEndTurn() {
        return this.lblEndTurn;
    }

    /**
     * Gets the lbl cleanup.
     *
     * @return PhaseLabel
     */
    public PhaseLabel getLblCleanup() {
        return this.lblCleanup;
    }

    // ========= Setter methods
    /**
     * Sets the image.
     *
     * @param i an Image
     */
    public void setImage(final Image i) {
        img = i;
    }

    // ========== Custom classes

    /**
     * Used to show various values in "details" panel. Also increments grid bag
     * constraints object as it goes, and zebra-stripes the labels.
     */
    // Design note: Labels are used here since buttons have various
    // difficulties in displaying the desired "flat" background and
    // also strange icon/action behavior.
    public class DetailLabel extends JLabel {
        private final Dimension labelSize = new Dimension(40, 25);
        private Color defaultBG;
        private final Color hoverBG;
        private Color clrBorders;
        private final MouseAdapter madHover;
        private int w, h, padding;
        private String iconAddress;

        /**
         * Instance of JLabel detailing info about field: has icon and optional
         * hover effect.
         * 
         * @param s0
         *            &emsp; Label's icon address
         * @param txt0
         *            &emsp; Label's text
         */
        public DetailLabel(final String s0, final String txt0) {
            super();
            padding = 6;
            iconAddress = s0;

            this.setIcon(skin.getIcon(iconAddress, h - 2 * padding, h - 2 * padding));
            this.setText(txt0);
            this.setOpaque(false);
            this.setForeground(ViewField.this.skin.getColor("text"));
            this.setPreferredSize(this.labelSize);
            this.setMaximumSize(this.labelSize);
            this.setMinimumSize(this.labelSize);
            this.setBorder(new LineBorder(new Color(0, 0, 0, 0), 1));
            this.setHorizontalAlignment(SwingConstants.CENTER);
            this.setVerticalAlignment(SwingConstants.BOTTOM);

            // Increment counter and check for zebra. Set default BG
            // so hover effects return to the same color.
            ViewField.this.counter++;

            if (((ViewField.this.counter % 4) == 2) || ((ViewField.this.counter % 4) == 3)) {
                this.defaultBG = ViewField.this.skin.getColor("zebra");
            } else {
                this.defaultBG = ViewField.this.skin.getColor("theme");
            }
            this.setBackground(this.defaultBG);

            // Resize adapter
            this.addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    setFont(ViewField.this.skin.getFont1().deriveFont(Font.PLAIN, (getHeight() / 2)));
                    setIcon(skin.getIcon(iconAddress, h - 2 * padding, h - 2 * padding));
                }
            });

            // Hover effect
            this.madHover = new MouseAdapter() {
                @Override
                public void mouseEntered(final MouseEvent e) {
                    DetailLabel.this.setBackground(DetailLabel.this.hoverBG);
                    DetailLabel.this.clrBorders = ViewField.this.skin.getColor("borders");
                }

                @Override
                public void mouseExited(final MouseEvent e) {
                    DetailLabel.this.setBackground(DetailLabel.this.defaultBG);
                    DetailLabel.this.clrBorders = ViewField.this.transparent;
                }
            };

            this.hoverBG = ViewField.this.skin.getColor("hover");
            this.clrBorders = ViewField.this.transparent;
        }

        /** Enable hover effects for this label. */
        public void enableHover() {
            this.addMouseListener(this.madHover);
        }

        /** Disable hover effects for this label. */
        public void disableHover() {
            this.removeMouseListener(this.madHover);
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
            g.fillRect(0, 0, this.w, this.h);
            g.setColor(this.clrBorders);
            g.drawRect(0, 0, this.w - 1, this.h - 1);
            super.paintComponent(g);
        }
    }

    /**
     * Shows phase labels, handles repainting and on/off states. A PhaseLabel
     * has "skip" and "active" states, meaning "this phase is (not) skipped" and
     * "this is the current phase".
     */
    public class PhaseLabel extends JLabel {
        private boolean enabled = true;
        private boolean active = false;
        private boolean hover = false;
        private final Color hoverBG = AllZone.getSkin().getColor("hover");

        /**
         * Shows phase labels, handles repainting and on/off states. A
         * PhaseLabel has "skip" and "active" states, meaning
         * "this phase is (not) skipped" and "this is the current phase".
         * 
         * @param txt
         *            &emsp; Label text
         */
        public PhaseLabel(final String txt) {
            super(txt);
            this.setHorizontalTextPosition(SwingConstants.CENTER);
            this.setHorizontalAlignment(SwingConstants.CENTER);

            this.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(final MouseEvent e) {
                    if (PhaseLabel.this.enabled) {
                        PhaseLabel.this.enabled = false;
                    } else {
                        PhaseLabel.this.enabled = true;
                    }
                }

                @Override
                public void mouseEntered(final MouseEvent e) {
                    PhaseLabel.this.hover = true;
                    PhaseLabel.this.repaint();
                }

                @Override
                public void mouseExited(final MouseEvent e) {
                    PhaseLabel.this.hover = false;
                    PhaseLabel.this.repaint();
                }
            });
        }

        /**
         * Determines whether play pauses at this phase or not.
         * 
         * @param b
         *            &emsp; boolean, true if play pauses
         */
        @Override
        public void setEnabled(final boolean b) {
            this.enabled = b;
        }

        /**
         * Determines whether play pauses at this phase or not.
         * 
         * @return boolean
         */
        public boolean getEnabled() {
            return this.enabled;
        }

        /**
         * Determines if this phase is the current phase (or not).
         * 
         * @param b
         *            &emsp; boolean, true if phase is current
         */
        public void setActive(final boolean b) {
            this.active = b;
        }

        /**
         * Determines if this phase is the current phase (or not).
         * 
         * @return boolean
         */
        public boolean getActive() {
            return this.active;
        }

        /*
         * (non-Javadoc)
         * 
         * @see javax.swing.JComponent#paintComponent(java.awt.Graphics)
         */
        @Override
        public void paintComponent(final Graphics g) {
            final int w = this.getWidth();
            final int h = this.getHeight();
            Color c;

            // Set color according to skip or active or hover state of label
            if (this.hover) {
                c = this.hoverBG;
            } else if (this.enabled) {
                c = Color.green;
            } else {
                c = Color.red;
            }

            if (!this.active && !this.hover) {
                c = c.darker().darker();
            }

            // Center vertically and horizontally. Show border if active.
            g.setColor(c);
            g.fillRoundRect(1, 1, w - 2, h - 2, 5, 5);
            g.setColor(Color.black);
            super.paintComponent(g);
        }
    }
}
