package forge.view.match;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;

import arcane.ui.PlayArea;

import net.miginfocom.swing.MigLayout;
import forge.AllZone;
import forge.Player;
import forge.Constant.Zone;
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
    private FSkin skin;
    private int counter;

    private ControlField control;
    private PlayArea tabletop;

    private Border hoverBorder, inactiveBorder;

    private DetailLabel
        lblHand, lblGraveyard, lblLibrary,
        lblExile, lblFlashback, lblPoison,
        lblBlack, lblBlue, lblGreen,
        lblRed, lblWhite, lblColorless;

    private PhaseLabel
        lblUpkeep, lblDraw, lblBeginCombat, lblEndCombat, lblEndTurn;

    private JLabel lblLife;
    private Map<String, JLabel> keywordLabels;
    private Color transparent = new Color(0, 0, 0, 0);

    /**
     * Assembles Swing components of player field instance.
     * 
     * @param player &emsp; a Player object.
     */
    public ViewField(Player player) {
        super();
        setOpaque(false);
        setLayout(new MigLayout("insets 1% 0.5%, gap 0.5%"));
        setCornerRadius(5);
        setToolTipText(player.getName() + " Gameboard");
        setBackground(AllZone.getSkin().getClrTheme());

        skin = AllZone.getSkin();
        inactiveBorder = new LineBorder(new Color(0, 0, 0, 0), 1);
        hoverBorder = new LineBorder(skin.getClrBorders(), 1);
        counter = -1;

        JScrollPane scroller = new JScrollPane();
        tabletop = new PlayArea(scroller, true);

        scroller.setViewportView(tabletop);
        scroller.setOpaque(false);
        scroller.getViewport().setOpaque(false);
        scroller.setBorder(null);
        tabletop.setBorder(new MatteBorder(0, 1, 0, 0, skin.getClrBorders()));
        tabletop.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;

        //
        Avatar pic = new Avatar("res/pics/icons/unknown.jpg");
        Details pool = new Details();

        add(pic, "h 98%!, w 10%!");
        add(pool, "h 98%!, w 11%!");
        add(scroller, "h 98%!, w 77%!");

        // After all components are in place, instantiate controller.
        control = new ControlField(player, this);
    }

    /** @return ControlField */
    public ControlField getController() {
        return control;
    }

    /** Handles observer update of player Zones - hand, graveyard, etc.
     * 
     * @param p0 &emsp; Player obj
     */
    public void updateZones(Player p0) {
        getLblHand().setText(""
                + p0.getZone(Zone.Hand).size());
        getLblGraveyard().setText(""
                + p0.getZone(Zone.Graveyard).size());
        getLblLibrary().setText(""
                + p0.getZone(Zone.Library).size());
        getLblFlashback().setText(""
                + CardFactoryUtil.getExternalZoneActivationCards(p0).size());
        getLblExile().setText(""
                + p0.getZone(Zone.Exile).size());
    }

    /** Handles observer update of non-Zone details - life, poison, etc.
     * Also updates "players" panel in tabber for this player.
     * 
     * @param p0 &emsp; Player obj
     */
    public void updateDetails(Player p0) {
        // "Players" panel update
        ViewTopLevel t = (ViewTopLevel) AllZone.getDisplay();
        t.getTabberController().getView().updatePlayerLabels(p0);

        // Poison/life
        getLblLife().setText("" + p0.getLife());
        getLblPoison().setText("" + p0.getPoisonCounters());

        // Hide all keyword labels, then show the appropriate ones.
        for (JLabel lbl : keywordLabels.values()) {
            lbl.setVisible(false);
        }

        for (String s : p0.getKeywords()) {
            keywordLabels.get(s).setVisible(true);
        }
    }

    //========= Retrieval methods
    /** @return PlayArea where cards for this field are in play */
    public PlayArea getTabletop() {
        return tabletop;
    }

    /** @return DetailLabel */
    public JLabel getLblLife() {
        return lblLife;
    }

    /** @return DetailLabel for hand cards */
    public DetailLabel getLblHand() {
        return lblHand;
    }

    /** @return DetailLabel for library cards */
    public DetailLabel getLblLibrary() {
        return lblLibrary;
    }

    /** @return DetailLabel for graveyard cards */
    public DetailLabel getLblGraveyard() {
        return lblGraveyard;
    }

    /** @return DetailLabel for exiled cards */
    public DetailLabel getLblExile() {
        return lblExile;
    }

    /** @return DetailLabel for flashback cards */
    public DetailLabel getLblFlashback() {
        return lblFlashback;
    }

    /** @return DetailLabel for poison counters */
    public DetailLabel getLblPoison() {
        return lblPoison;
    }

    /** @return DetailLabel for colorless mana count */
    public DetailLabel getLblColorless() {
        return lblColorless;
    }

    /** @return DetailLabel for black mana count */
    public DetailLabel getLblBlack() {
        return lblBlack;
    }

    /** @return DetailLabel for blue mana count */
    public DetailLabel getLblBlue() {
        return lblBlue;
    }

    /** @return DetailLabel for green mana count */
    public DetailLabel getLblGreen() {
        return lblGreen;
    }

    /** @return DetailLabel for red mana count */
    public DetailLabel getLblRed() {
        return lblRed;
    }

    /** @return DetailLabel for white mana count */
    public DetailLabel getLblWhite() {
        return lblWhite;
    }

    // Phases
    /** @return PhaseLabel for upkeep */
    public PhaseLabel getLblUpkeep() {
        return lblUpkeep;
    }

    /** @return PhaseLabel for draw */
    public PhaseLabel getLblDraw() {
        return lblDraw;
    }

    /** @return PhaseLabel for beginning of combat*/
    public PhaseLabel getLblBeginCombat() {
        return lblBeginCombat;
    }

    /** @return PhaseLabel for end of combat */
    public PhaseLabel getLblEndCombat() {
        return lblEndCombat;
    }

    /** @return PhaseLabel for end of turn */
    public PhaseLabel getLblEndTurn() {
        return lblEndTurn;
    }

    /** @return Map<String,JLabel> */
    public Map<String, JLabel> getKeywordLabels() {
        return keywordLabels;
    }

    //========== Custom classes

    /**
     * Shows user icon, keywords, and phase for this field.
     */
    private class Avatar extends FPanel {
        private ImageIcon icon;
        private Color transparent = new Color(0, 0, 0, 0);

        public Avatar(String filename) {
            // Panel and background image icon init
            super();
            setOpaque(false);
            setLayout(new MigLayout("fill, wrap, insets 0, gap 0"));
            icon = new ImageIcon(filename);

            // Life label
            lblLife = new JLabel("--");
            lblLife.setBorder(inactiveBorder);
            lblLife.setToolTipText("<html>Player life.<br>Click to select player.</html>");
            lblLife.setFont(skin.getFont1().deriveFont(Font.BOLD, 18));
            lblLife.setForeground(skin.getClrText());
            lblLife.setBackground(skin.getClrTheme().darker());
            lblLife.setOpaque(true);
            lblLife.setHorizontalAlignment(JLabel.CENTER);
            lblLife.setAlignmentX(Component.CENTER_ALIGNMENT);
            lblLife.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    lblLife.setBackground(AllZone.getSkin().getClrHover());
                    lblLife.setBorder(hoverBorder);
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    lblLife.setBackground(skin.getClrTheme());
                    lblLife.setBorder(inactiveBorder);
                }
            });
            add(lblLife, "w 100%!, dock north");

            keywordLabels = new HashMap<String, JLabel>();
            // TODO link these map keys to correct keyword constant
            keywordLabels.put("shroud", new KeywordLabel("Shroud"));
            keywordLabels.put("extraturn", new KeywordLabel("+1 turn"));
            keywordLabels.put("skipturn", new KeywordLabel("Skip turn"));
            keywordLabels.put("problack", new KeywordLabel("Pro: Black"));
            keywordLabels.put("problue", new KeywordLabel("Pro: Blue"));
            keywordLabels.put("progreen", new KeywordLabel("Pro: Green"));
            keywordLabels.put("prored", new KeywordLabel("Pro: Red"));
            keywordLabels.put("prowhite", new KeywordLabel("Pro: White"));

            JPanel pnlKeywords = new JPanel(new MigLayout("insets 0, wrap, hidemode 2"));
            pnlKeywords.setOpaque(false);
            for (JLabel lbl : keywordLabels.values()) {
                pnlKeywords.add(lbl);
            }

            JScrollPane scrKeywords = new JScrollPane(pnlKeywords,
                    ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                    ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

            scrKeywords.setBorder(new EmptyBorder(0, 0, 0, 0));
            scrKeywords.setOpaque(false);
            scrKeywords.getViewport().setOpaque(false);
            scrKeywords.setViewportView(pnlKeywords);
            add(scrKeywords, "w 100%!, growy");

            JPanel phase = new JPanel();
            phase.setOpaque(false);
            phase.setLayout(new MigLayout("fillx, insets 0, gap 0"));
            add(phase, "w 100%!, h 20px!");

            // Constraints string must be set once, for ease and also
            // since dynamic sizing is buggy.
            String constraints = "w 20%!, h 100%!";

            lblUpkeep = new PhaseLabel("UP");
            lblUpkeep.setToolTipText("<html>Phase: Upkeep<br>Click to toggle.</html>");
            phase.add(lblUpkeep, constraints);

            lblDraw = new PhaseLabel("DR");
            lblDraw.setToolTipText("<html>Phase: Draw<br>Click to toggle.</html>");
            phase.add(lblDraw, constraints);

            lblBeginCombat = new PhaseLabel("BC");
            lblBeginCombat.setToolTipText("<html>Phase: Begin Combat<br>Click to toggle.</html>");
            phase.add(lblBeginCombat, constraints);

            lblEndCombat = new PhaseLabel("EC");
            lblEndCombat.setToolTipText("<html>Phase: End Combat<br>Click to toggle.</html>");
            phase.add(lblEndCombat, constraints);

            lblEndTurn = new PhaseLabel("ET");
            lblEndTurn.setToolTipText("<html>Phase: End Turn<br>Click to toggle.</html>");
            phase.add(lblEndTurn, constraints);
        }

        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            setBorder(new MatteBorder(getWidth(), 0, 0, 0, transparent));
            g.drawImage(icon.getImage(), 0, 0, getWidth(), getWidth(),
                    0, 0, icon.getIconWidth(), icon.getIconHeight(), null);
            lblLife.setFont(skin.getFont1().deriveFont(Font.PLAIN, (int) (getWidth() / 4)));
        }
    }

    /**
     * The "details" section of player info:
     * Hand, library, graveyard, exiled, flashback, poison,
     * and mana pool (BBGRW and colorless).
     *
     */
    // Design note: Labels are used here since buttons have various
    // difficulties in displaying the desired "flat" background and
    // also strange icon/action behavior.
    private class Details extends JPanel {
        public Details() {
            super();
            setLayout(new MigLayout("insets 0, gap 0, wrap 2, filly"));
            setOpaque(false);
            final String constraints = "w 50%!, h 12.5%!, growy";

            // Hand, library, graveyard, exile, flashback, poison labels
            lblGraveyard = new DetailLabel(new ImageIcon("res/images/symbols-13/detail_grave.png"), "99");
            lblGraveyard.setToolTipText("Cards in graveyard");
            add(lblGraveyard, constraints);

            lblLibrary = new DetailLabel(new ImageIcon("res/images/symbols-13/detail_library.png"), "99");
            lblLibrary.setToolTipText("Cards in library");
            add(lblLibrary, constraints);

            lblExile = new DetailLabel(new ImageIcon("res/images/symbols-13/detail_exile.png"), "99");
            lblExile.setToolTipText("Exiled cards");
            add(lblExile, constraints);

            lblFlashback = new DetailLabel(new ImageIcon("res/images/symbols-13/detail_flashback.png"), "99");
            lblFlashback.setToolTipText("Flashback cards");
            add(lblFlashback, constraints);

            lblHand = new DetailLabel(new ImageIcon("res/images/symbols-13/detail_hand.png"), "99");
            lblHand.setToolTipText("Cards in hand");
            add(lblHand, constraints);

            lblPoison = new DetailLabel(new ImageIcon("res/images/symbols-13/detail_poison.png"), "99");
            lblPoison.setToolTipText("Poison counters");
            add(lblPoison, constraints);

            // Black, Blue, Colorless, Green, Red, White mana labels
            lblBlack = new DetailLabel(new ImageIcon("res/images/symbols-13/B.png"), "99");
            lblBlack.setToolTipText("Black mana");
            add(lblBlack, constraints);

            lblBlue = new DetailLabel(new ImageIcon("res/images/symbols-13/U.png"), "99");
            lblBlue.setToolTipText("Blue mana");
            add(lblBlue, constraints);

            lblGreen = new DetailLabel(new ImageIcon("res/images/symbols-13/G.png"), "99");
            lblGreen.setToolTipText("Green mana");
            add(lblGreen, constraints);

            lblRed = new DetailLabel(new ImageIcon("res/images/symbols-13/R.png"), "99");
            lblRed.setToolTipText("Red mana");
            add(lblRed, constraints);

            lblWhite = new DetailLabel(new ImageIcon("res/images/symbols-13/W.png"), "99");
            lblWhite.setToolTipText("White mana");
            add(lblWhite, constraints);

            lblColorless = new DetailLabel(new ImageIcon("res/images/symbols-13/X.png"), "99");
            lblColorless.setToolTipText("Colorless mana");
            add(lblColorless, constraints);
        }
    }

    /**
     * Used to show various values in "details" panel.  Also increments
     * grid bag constraints object as it goes, and zebra-stripes the labels.
     */
    public class DetailLabel extends JLabel {
        private final Dimension labelSize = new Dimension(40, 25);
        private Color defaultBG;
        private Color hoverBG;
        private Color clrBorders;
        private MouseAdapter madHover;
        private int w, h;

        /**
         * Instance of JLabel detailing info about field:
         * has icon and optional hover effect.
         * 
         * @param icon &emsp; Label's icon
         * @param txt &emsp; Label's text
         */
        public DetailLabel(ImageIcon icon, String txt) {
            super();
            setIcon(icon);
            setText(txt);
            setOpaque(false);
            setForeground(skin.getClrText());
            setPreferredSize(labelSize);
            setMaximumSize(labelSize);
            setMinimumSize(labelSize);
            setBorder(new LineBorder(new Color(0, 0, 0, 0), 1));
            setHorizontalAlignment(CENTER);

            // Increment counter and check for zebra. Set default BG
            // so hover effects return to the same color.
            counter++;

            if (counter % 4 == 2 || counter % 4 == 3) {
                defaultBG = skin.getClrZebra();
            }
            else {
                defaultBG = skin.getClrTheme();
            }
            setBackground(defaultBG);

            madHover = new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    setBackground(hoverBG);
                    clrBorders = skin.getClrBorders();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    setBackground(defaultBG);
                    clrBorders = transparent;
                }
            };

            hoverBG = skin.getClrHover();
            clrBorders = transparent;
        }

        /** Enable hover effects for this label. */
        public void enableHover() {
            addMouseListener(madHover);
        }

        /** Disable hover effects for this label. */
        public void disableHover() {
            removeMouseListener(madHover);
        }

        @Override
        protected void paintComponent(Graphics g) {
            w = getWidth();
            h = getHeight();
            g.setColor(getBackground());
            g.fillRect(0, 0, w, h);
            g.setColor(clrBorders);
            g.drawRect(0, 0, w - 1, h - 1);
            this.setFont(skin.getFont1().deriveFont(Font.PLAIN, (int) (h / 2)));
            super.paintComponent(g);
        }
    }

    private class KeywordLabel extends JLabel {
        public KeywordLabel(String s) {
            super(s);
            setToolTipText(s);
        }
    }

    /**
     * Shows phase labels, handles repainting and on/off states.  A PhaseLabel
     * has "skip" and "active" states, meaning "this phase is (not) skipped"
     * and "this is the current phase".
     */
    public class PhaseLabel extends JLabel {
        private boolean enabled = true;
        private boolean active = false;
        private boolean hover = false;
        private Color hoverBG = AllZone.getSkin().getClrHover();

        /**
         * Shows phase labels, handles repainting and on/off states.  A PhaseLabel
         * has "skip" and "active" states, meaning "this phase is (not) skipped"
         * and "this is the current phase".
         * 
         * @param txt &emsp; Label text
         */
        public PhaseLabel(String txt) {
            super(txt);
            this.setHorizontalTextPosition(CENTER);
            this.setHorizontalAlignment(CENTER);

            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (enabled) { enabled = false; }
                    else { enabled = true; }
                }
                @Override
                public void mouseEntered(MouseEvent e) {
                    hover = true;
                    repaint();
                }
                @Override
                public void mouseExited(MouseEvent e) {
                    hover = false;
                    repaint();
                }
            });
        }

        /**
         * Determines whether play pauses at this phase or not.
         *
         * @param b &emsp; boolean, true if play pauses
         */
        public void setEnabled(boolean b) {
            enabled = b;
        }

        /**
         * Determines whether play pauses at this phase or not.
         *
         * @return boolean
         */
        public boolean getEnabled() {
            return enabled;
        }

        /**
         * Determines if this phase is the current phase (or not).
         *
         * @param b &emsp; boolean, true if phase is current
         */
        public void setActive(boolean b) {
            active = b;
        }

        /**
         * Determines if this phase is the current phase (or not).
         * 
         * @return boolean
         */
        public boolean getActive() {
            return active;
        }

        @Override
        public void paintComponent(Graphics g) {
            int w = getWidth();
            int h = getHeight();
            Color c;

            // Set color according to skip or active or hover state of label
            if (hover) {
                c = hoverBG;
            }
            else if (enabled) {
                c = Color.green;
            }
            else {
                c = Color.red;
            }

            if (!active && !hover) {
                c = c.darker().darker();
            }

            // Center vertically and horizontally. Show border if active.
            g.setColor(c);
            g.fillRoundRect(1, 1, w - 2, h - 2, 5, 5);

            setFont(new Font("TAHOMA", Font.PLAIN, (int) (w / 2)));
            g.setColor(Color.black);
            super.paintComponent(g);
        }
    }
}
