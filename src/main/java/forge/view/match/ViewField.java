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
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;

import net.miginfocom.swing.MigLayout;
import arcane.ui.PlayArea;
import forge.AllZone;
import forge.Constant.Zone;
import forge.card.mana.ManaPool;
import forge.Player;
import forge.card.cardfactory.CardFactoryUtil;
import forge.control.match.ControlField;
import forge.view.toolbox.FPanel;
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

    private DetailLabel lblHand, lblGraveyard, lblLibrary, lblExile, lblFlashback, lblPoison, lblBlack, lblBlue,
            lblGreen, lblRed, lblWhite, lblColorless;

    private PhaseLabel lblUpkeep, lblDraw, lblBeginCombat, lblEndCombat, lblEndTurn;

    private JLabel lblAvatar, lblLife;
    private Map<String, JLabel> keywordLabels;
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
        this.setLayout(new MigLayout("insets 1% 0.5%, gap 0.5%"));
        this.setCornerRadius(5);
        this.setToolTipText(player.getName() + " Gameboard");
        this.setBackground(AllZone.getSkin().getClrTheme());

        this.skin = AllZone.getSkin();
        this.inactiveBorder = new LineBorder(new Color(0, 0, 0, 0), 1);
        this.hoverBorder = new LineBorder(this.skin.getClrBorders(), 1);
        this.counter = -1;

        final JScrollPane scroller = new JScrollPane();
        this.tabletop = new PlayArea(scroller, player.equals(AllZone.getComputerPlayer()) ? true : false);

        scroller.setViewportView(this.tabletop);
        scroller.setOpaque(false);
        scroller.getViewport().setOpaque(false);
        scroller.setBorder(null);
        this.tabletop.setBorder(new MatteBorder(0, 1, 0, 0, this.skin.getClrBorders()));
        this.tabletop.setOpaque(false);
        
        final String fileString = "res/pics/icons/" + (player.isHuman() ? "Mage01.jpg" : "Mage02.jpg");

        final Avatar pic = new Avatar(fileString);
        final Details pool = new Details();

        this.add(pic, "h 98%!, w 10%!");
        this.add(pool, "h 98%!, w 11%!");
        this.add(scroller, "h 98%!, w 77%!");

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
        
        //mana pool
        updateManaPool(p0);

        // Hide all keyword labels, then show the appropriate ones.
        for (final JLabel lbl : this.keywordLabels.values()) {
            lbl.setVisible(false);
        }

        /*for (final String s : p0.getKeywords()) {
            this.keywordLabels.get(s).setVisible(true);
        }*/
    }
    
    /**
     * Handles observer update of the mana pool.
     * 
     * @param p0
     *            &emsp; Player obj
     */
    public void updateManaPool(final Player p0) {
        ManaPool m = p0.getManaPool();
        getLblBlack().setText("" +m.getAmountOfColor(forge.Constant.Color.BLACK));
        getLblBlue().setText("" +m.getAmountOfColor(forge.Constant.Color.BLUE));
        getLblGreen().setText("" +m.getAmountOfColor(forge.Constant.Color.GREEN));
        getLblRed().setText("" +m.getAmountOfColor(forge.Constant.Color.RED));
        getLblWhite().setText("" +m.getAmountOfColor(forge.Constant.Color.WHITE));
        getLblColorless().setText("" +m.getAmountOfColor(forge.Constant.Color.COLORLESS));
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
     * @return PhaseLabel for upkeep
     */
    public PhaseLabel getLblUpkeep() {
        return this.lblUpkeep;
    }

    /**
     * Gets the lbl draw.
     * 
     * @return PhaseLabel for draw
     */
    public PhaseLabel getLblDraw() {
        return this.lblDraw;
    }

    /**
     * Gets the lbl begin combat.
     * 
     * @return PhaseLabel for beginning of combat
     */
    public PhaseLabel getLblBeginCombat() {
        return this.lblBeginCombat;
    }

    /**
     * Gets the lbl end combat.
     * 
     * @return PhaseLabel for end of combat
     */
    public PhaseLabel getLblEndCombat() {
        return this.lblEndCombat;
    }

    /**
     * Gets the lbl end turn.
     * 
     * @return PhaseLabel for end of turn
     */
    public PhaseLabel getLblEndTurn() {
        return this.lblEndTurn;
    }

    /**
     * Gets the keyword labels.
     * 
     * @return Map<String,JLabel>
     */
    public Map<String, JLabel> getKeywordLabels() {
        return this.keywordLabels;
    }

    // ========== Custom classes

    /**
     * Shows user icon, keywords, and phase for this field.
     */
    private class Avatar extends FPanel {
        private final Image img;

        public Avatar(final String filename) {
            // Panel and background image icon init
            super();
            this.setOpaque(false);
            this.setLayout(new MigLayout("fill, wrap, insets 0, gap 0"));
            this.img = new ImageIcon(filename).getImage();

            // Resize adapter
            this.addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    lblLife.setFont(ViewField.this.skin.getFont1().deriveFont(Font.PLAIN, (getWidth() / 4)));
                    lblAvatar.setIcon(new ImageIcon(img.getScaledInstance(getWidth(), getWidth(), java.awt.Image.SCALE_SMOOTH)));
                }
            });

            lblAvatar = new JLabel();
            this.add(lblAvatar, "w 100%!, wrap");

            // Life label
            ViewField.this.lblLife = new JLabel("--");
            ViewField.this.lblLife.setBorder(ViewField.this.inactiveBorder);
            ViewField.this.lblLife.setToolTipText("<html>Player life.<br>Click to select player.</html>");
            ViewField.this.lblLife.setFont(ViewField.this.skin.getFont1().deriveFont(Font.BOLD, 18));
            ViewField.this.lblLife.setForeground(ViewField.this.skin.getClrText());
            ViewField.this.lblLife.setBackground(ViewField.this.skin.getClrTheme().darker());
            ViewField.this.lblLife.setOpaque(true);
            ViewField.this.lblLife.setHorizontalAlignment(SwingConstants.CENTER);
            ViewField.this.lblLife.setAlignmentX(Component.CENTER_ALIGNMENT);
            ViewField.this.lblLife.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(final MouseEvent e) {
                    ViewField.this.lblLife.setBackground(AllZone.getSkin().getClrHover());
                    ViewField.this.lblLife.setBorder(ViewField.this.hoverBorder);
                }

                @Override
                public void mouseExited(final MouseEvent e) {
                    ViewField.this.lblLife.setBackground(ViewField.this.skin.getClrTheme());
                    ViewField.this.lblLife.setBorder(ViewField.this.inactiveBorder);
                }
            });
            this.add(ViewField.this.lblLife, "w 100%!, wrap");

            ViewField.this.keywordLabels = new HashMap<String, JLabel>();
            // TODO link these map keys to correct keyword constant
            ViewField.this.keywordLabels.put("shroud", new KeywordLabel("Shroud"));
            ViewField.this.keywordLabels.put("extraturn", new KeywordLabel("+1 turn"));
            ViewField.this.keywordLabels.put("skipturn", new KeywordLabel("Skip turn"));
            ViewField.this.keywordLabels.put("problack", new KeywordLabel("Pro: Black"));
            ViewField.this.keywordLabels.put("problue", new KeywordLabel("Pro: Blue"));
            ViewField.this.keywordLabels.put("progreen", new KeywordLabel("Pro: Green"));
            ViewField.this.keywordLabels.put("prored", new KeywordLabel("Pro: Red"));
            ViewField.this.keywordLabels.put("prowhite", new KeywordLabel("Pro: White"));

            final JPanel pnlKeywords = new JPanel(new MigLayout("insets 0, wrap, hidemode 2"));
            pnlKeywords.setOpaque(false);
            for (final JLabel lbl : ViewField.this.keywordLabels.values()) {
                pnlKeywords.add(lbl);
            }

            final JScrollPane scrKeywords = new JScrollPane(pnlKeywords,
                    ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

            scrKeywords.setBorder(new EmptyBorder(0, 0, 0, 0));
            scrKeywords.setOpaque(false);
            scrKeywords.getViewport().setOpaque(false);
            scrKeywords.setViewportView(pnlKeywords);
            this.add(scrKeywords, "w 100%!, growy");

            final JPanel phase = new JPanel();
            phase.setOpaque(false);
            phase.setLayout(new MigLayout("fillx, insets 0, gap 0"));
            this.add(phase, "w 100%!, h 20px!");

            // Constraints string, set once
            final String constraints = "w 20%!, h 100%!";

            ViewField.this.lblUpkeep = new PhaseLabel("UP");
            ViewField.this.lblUpkeep.setToolTipText("<html>Phase: Upkeep<br>Click to toggle.</html>");
            phase.add(ViewField.this.lblUpkeep, constraints);

            ViewField.this.lblDraw = new PhaseLabel("DR");
            ViewField.this.lblDraw.setToolTipText("<html>Phase: Draw<br>Click to toggle.</html>");
            phase.add(ViewField.this.lblDraw, constraints);

            ViewField.this.lblBeginCombat = new PhaseLabel("BC");
            ViewField.this.lblBeginCombat.setToolTipText("<html>Phase: Begin Combat<br>Click to toggle.</html>");
            phase.add(ViewField.this.lblBeginCombat, constraints);

            ViewField.this.lblEndCombat = new PhaseLabel("EC");
            ViewField.this.lblEndCombat.setToolTipText("<html>Phase: End Combat<br>Click to toggle.</html>");
            phase.add(ViewField.this.lblEndCombat, constraints);

            ViewField.this.lblEndTurn = new PhaseLabel("ET");
            ViewField.this.lblEndTurn.setToolTipText("<html>Phase: End Turn<br>Click to toggle.</html>");
            phase.add(ViewField.this.lblEndTurn, constraints);
        }
    }

    /**
     * The "details" section of player info: Hand, library, graveyard, exiled,
     * flashback, poison, and mana pool (BBGRW and colorless).
     * 
     */
    // Design note: Labels are used here since buttons have various
    // difficulties in displaying the desired "flat" background and
    // also strange icon/action behavior.
    private class Details extends JPanel {
        public Details() {
            super();
            this.setLayout(new MigLayout("insets 0, gap 0, wrap 2, filly"));
            this.setOpaque(false);
            final String constraints = "w 50%!, h 12.5%!, growy";

            // Hand, library, graveyard, exile, flashback, poison labels
            ViewField.this.lblHand = new DetailLabel(new ImageIcon("res/images/symbols-13/detail_hand.png"), "99");
            ViewField.this.lblHand.setToolTipText("Cards in hand");
            this.add(ViewField.this.lblHand, constraints);

            ViewField.this.lblLibrary = new DetailLabel(new ImageIcon("res/images/symbols-13/detail_library.png"), "99");
            ViewField.this.lblLibrary.setToolTipText("Cards in library");
            this.add(ViewField.this.lblLibrary, constraints);

            ViewField.this.lblGraveyard = new DetailLabel(new ImageIcon("res/images/symbols-13/detail_grave.png"), "99");
            ViewField.this.lblGraveyard.setToolTipText("Cards in graveyard");
            this.add(ViewField.this.lblGraveyard, constraints);

            ViewField.this.lblExile = new DetailLabel(new ImageIcon("res/images/symbols-13/detail_exile.png"), "99");
            ViewField.this.lblExile.setToolTipText("Exiled cards");
            this.add(ViewField.this.lblExile, constraints);

            ViewField.this.lblFlashback = new DetailLabel(new ImageIcon("res/images/symbols-13/detail_flashback.png"),
                    "99");
            ViewField.this.lblFlashback.setToolTipText("Flashback cards");
            this.add(ViewField.this.lblFlashback, constraints);

            ViewField.this.lblPoison = new DetailLabel(new ImageIcon("res/images/symbols-13/detail_poison.png"), "99");
            ViewField.this.lblPoison.setToolTipText("Poison counters");
            this.add(ViewField.this.lblPoison, constraints);

            // Black, Blue, Colorless, Green, Red, White mana labels
            ViewField.this.lblBlack = new DetailLabel(new ImageIcon("res/images/symbols-13/B.png"), "99");
            ViewField.this.lblBlack.setToolTipText("Black mana");
            this.add(ViewField.this.lblBlack, constraints);

            ViewField.this.lblBlue = new DetailLabel(new ImageIcon("res/images/symbols-13/U.png"), "99");
            ViewField.this.lblBlue.setToolTipText("Blue mana");
            this.add(ViewField.this.lblBlue, constraints);

            ViewField.this.lblGreen = new DetailLabel(new ImageIcon("res/images/symbols-13/G.png"), "99");
            ViewField.this.lblGreen.setToolTipText("Green mana");
            this.add(ViewField.this.lblGreen, constraints);

            ViewField.this.lblRed = new DetailLabel(new ImageIcon("res/images/symbols-13/R.png"), "99");
            ViewField.this.lblRed.setToolTipText("Red mana");
            this.add(ViewField.this.lblRed, constraints);

            ViewField.this.lblWhite = new DetailLabel(new ImageIcon("res/images/symbols-13/W.png"), "99");
            ViewField.this.lblWhite.setToolTipText("White mana");
            this.add(ViewField.this.lblWhite, constraints);

            ViewField.this.lblColorless = new DetailLabel(new ImageIcon("res/images/symbols-13/X.png"), "99");
            ViewField.this.lblColorless.setToolTipText("Colorless mana");
            this.add(ViewField.this.lblColorless, constraints);
        }
    }

    /**
     * Used to show various values in "details" panel. Also increments grid bag
     * constraints object as it goes, and zebra-stripes the labels.
     */
    public class DetailLabel extends JLabel {
        private final Dimension labelSize = new Dimension(40, 25);
        private Color defaultBG;
        private final Color hoverBG;
        private Color clrBorders;
        private final MouseAdapter madHover;
        private int w, h;

        /**
         * Instance of JLabel detailing info about field: has icon and optional
         * hover effect.
         * 
         * @param icon
         *            &emsp; Label's icon
         * @param txt
         *            &emsp; Label's text
         */
        public DetailLabel(final ImageIcon icon, final String txt) {
            super();
            this.setIcon(icon);
            this.setText(txt);
            this.setOpaque(false);
            this.setForeground(ViewField.this.skin.getClrText());
            this.setPreferredSize(this.labelSize);
            this.setMaximumSize(this.labelSize);
            this.setMinimumSize(this.labelSize);
            this.setBorder(new LineBorder(new Color(0, 0, 0, 0), 1));
            this.setHorizontalAlignment(SwingConstants.CENTER);
            this.setVerticalAlignment(SwingConstants.CENTER);

            // Increment counter and check for zebra. Set default BG
            // so hover effects return to the same color.
            ViewField.this.counter++;

            if (((ViewField.this.counter % 4) == 2) || ((ViewField.this.counter % 4) == 3)) {
                this.defaultBG = ViewField.this.skin.getClrZebra();
            } else {
                this.defaultBG = ViewField.this.skin.getClrTheme();
            }
            this.setBackground(this.defaultBG);

            // Resize adapter
            this.addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    setFont(ViewField.this.skin.getFont1().deriveFont(Font.PLAIN, (getHeight() / 2)));
                }
            });

            // Hover effect
            this.madHover = new MouseAdapter() {
                @Override
                public void mouseEntered(final MouseEvent e) {
                    DetailLabel.this.setBackground(DetailLabel.this.hoverBG);
                    DetailLabel.this.clrBorders = ViewField.this.skin.getClrBorders();
                }

                @Override
                public void mouseExited(final MouseEvent e) {
                    DetailLabel.this.setBackground(DetailLabel.this.defaultBG);
                    DetailLabel.this.clrBorders = ViewField.this.transparent;
                }
            };

            this.hoverBG = ViewField.this.skin.getClrHover();
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

    private class KeywordLabel extends JLabel {
        public KeywordLabel(final String s) {
            super(s);
            this.setToolTipText(s);
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
        private final Color hoverBG = AllZone.getSkin().getClrHover();

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

            // Resize adapter
            this.addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    setFont(new Font("TAHOMA", Font.PLAIN, (getWidth() / 2)));
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
