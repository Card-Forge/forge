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
package forge.gui.match.nonsingleton;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;

import net.miginfocom.swing.MigLayout;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.mana.ManaPool;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.gui.framework.DragCell;
import forge.gui.framework.DragTab;
import forge.gui.framework.EDocID;
import forge.gui.framework.IVDoc;
import forge.gui.match.controllers.CPlayers;
import forge.gui.toolbox.FLabel;
import forge.gui.toolbox.FSkin;
import forge.gui.toolbox.FSkin.SkinProp;
import forge.view.arcane.PlayArea;

/** 
 * Assembles Swing components of a player field instance.
 * 
 * <br><br><i>(V at beginning of class name denotes a view class.)</i>
 */
public class VField implements IVDoc<CField> {
    // Fields used with interface IVDoc
    private final CField control;
    private DragCell parentCell;
    private final EDocID docID;
    private final DragTab tab = new DragTab("Field");

    // Other fields
    private Player player = null;

    // Top-level containers
    private final JScrollPane scroller = new JScrollPane();
    private final PlayArea tabletop;
    private final JPanel avatarArea = new JPanel();
    private final JPanel phaseArea  = new JPanel();
    private final JPanel pnlDetails = new JPanel();

    // Avatar area
    private final FLabel lblAvatar = new FLabel.Builder().fontAlign(SwingConstants.CENTER)
            .iconScaleFactor(1.0f).build();
    private final FLabel lblLife = new FLabel.Builder().fontAlign(SwingConstants.CENTER)
            .fontStyle(Font.BOLD).build();

    // Info labels
    private FLabel lblHand = getBuiltFLabel(FSkin.ZoneImages.ICO_HAND, "99", "Cards in hand");
    private FLabel lblGraveyard = getBuiltFLabel(FSkin.ZoneImages.ICO_GRAVEYARD, "99", "Cards in graveyard");
    private FLabel lblLibrary = getBuiltFLabel(FSkin.ZoneImages.ICO_LIBRARY, "99", "Cards in library");
    private FLabel lblExile = getBuiltFLabel(FSkin.ZoneImages.ICO_EXILE, "99", "Exiled cards");
    private FLabel lblFlashback = getBuiltFLabel(FSkin.ZoneImages.ICO_FLASHBACK, "99", "Flashback cards");
    private FLabel lblPoison = getBuiltFLabel(FSkin.ZoneImages.ICO_POISON, "99", "Poison counters");
    private FLabel lblBlack = getBuiltFLabel(FSkin.ManaImages.IMG_BLACK, "99", "Black mana");
    private FLabel lblBlue = getBuiltFLabel(FSkin.ManaImages.IMG_BLUE, "99", "Blue mana");
    private FLabel lblGreen = getBuiltFLabel(FSkin.ManaImages.IMG_GREEN, "99", "Green mana");
    private FLabel lblRed = getBuiltFLabel(FSkin.ManaImages.IMG_RED, "99", "Red mana");
    private FLabel lblWhite = getBuiltFLabel(FSkin.ManaImages.IMG_WHITE, "99", "White mana");
    private FLabel lblColorless = getBuiltFLabel(FSkin.ManaImages.IMG_COLORLESS, "99", "Colorless mana");

    // Phase labels
    private PhaseLabel lblUpkeep = new PhaseLabel("UP");
    private PhaseLabel lblDraw = new PhaseLabel("DR");
    private PhaseLabel lblMain1 = new PhaseLabel("M1");
    private PhaseLabel lblBeginCombat = new PhaseLabel("BC");
    private PhaseLabel lblDeclareAttackers = new PhaseLabel("DA");
    private PhaseLabel lblDeclareBlockers = new PhaseLabel("DB");
    private PhaseLabel lblFirstStrike = new PhaseLabel("FS");
    private PhaseLabel lblCombatDamage = new PhaseLabel("CD");
    private PhaseLabel lblEndCombat = new PhaseLabel("EC");
    private PhaseLabel lblMain2 = new PhaseLabel("M2");
    private PhaseLabel lblEndTurn = new PhaseLabel("ET");
    private PhaseLabel lblCleanup = new PhaseLabel("CL");

    //========= Constructor
    /**
     * Assembles Swing components of a player field instance.
     * 
     * @param player0 &emsp; {@link forge.game.player.Player}
     * @param id0 &emsp; {@link forge.gui.framework.EDocID}
     */
    public VField(final EDocID id0, final Player player0) {
        this.docID = id0;
        id0.setDoc(this);

        this.player = player0;
        if (player0 != null) { tab.setText(player0.getName() + " Field"); }
        else { tab.setText("NO PLAYER FOR " + docID.toString()); }

        // TODO player is hard-coded into tabletop...should be dynamic
        // (haven't looked into it too deeply). Doublestrike 12-04-12
        tabletop = new PlayArea(scroller, id0 == EDocID.FIELD_1 );

        control = new CField(player, this);

        avatarArea.setOpaque(false);
        avatarArea.setBackground(FSkin.getColor(FSkin.Colors.CLR_HOVER));
        avatarArea.setLayout(new MigLayout("insets 0, gap 0"));
        avatarArea.add(lblAvatar, "w 100%!, h 70%!, wrap, gaptop 4%");
        avatarArea.add(lblLife, "w 100%!, h 30%!, gaptop 4%");

        // Player area hover effect
        avatarArea.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(final MouseEvent e) {
                avatarArea.setOpaque(true);
                avatarArea.setBorder(new LineBorder(FSkin.getColor(FSkin.Colors.CLR_BORDERS), 1));
            }

            @Override
            public void mouseExited(final MouseEvent e) {
                avatarArea.setOpaque(false);
                avatarArea.setBorder(new LineBorder(new Color(0, 0, 0, 0), 1));
            }
        });

        phaseArea.setOpaque(false);
        phaseArea.setLayout(new MigLayout("insets 0 0 1% 0, gap 0, wrap"));
        populatePhase();

        tabletop.setBorder(new MatteBorder(0, 1, 0, 0,
                FSkin.getColor(FSkin.Colors.CLR_BORDERS)));
        tabletop.setOpaque(false);

        scroller.setViewportView(this.tabletop);
        scroller.setOpaque(false);
        scroller.getViewport().setOpaque(false);
        scroller.setBorder(null);

        pnlDetails.setOpaque(false);
        pnlDetails.setLayout(new MigLayout("insets 0, gap 0, wrap"));
        populateDetails();
    }

    //========= Overridden methods

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#populate()
     */
    @Override
    public void populate() {
        final JPanel pnl = parentCell.getBody();
        pnl.setLayout(new MigLayout("insets 0, gap 0"));

        pnl.add(avatarArea, "w 10%!, h 30%!");
        pnl.add(phaseArea, "w 5%!, h 100%!, span 1 2");
        pnl.add(scroller, "w 85%!, h 100%!, span 1 2, wrap");
        pnl.add(pnlDetails, "w 10%!, h 69%!, gapleft 1px");
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getDocumentID()
     */
    @Override
    public EDocID getDocumentID() {
        return docID;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getTabLabel()
     */
    @Override
    public DragTab getTabLabel() {
        return tab;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getLayoutControl()
     */
    @Override
    public CField getLayoutControl() {
        return control;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#setParentCell()
     */
    @Override
    public void setParentCell(final DragCell cell0) {
        this.parentCell = cell0;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getParentCell()
     */
    @Override
    public DragCell getParentCell() {
        return this.parentCell;
    }

    //========= Populate helper methods

    /** Adds phase indicator labels to phase area JPanel container. */
    private void populatePhase() {
        // Constraints string, set once
        final String constraints = "w 94%!, h 7.2%, gaptop 1%, gapleft 3%";

        VField.this.lblUpkeep.setToolTipText("<html>Phase: Upkeep<br>Click to toggle.</html>");
        phaseArea.add(VField.this.lblUpkeep, constraints);

        VField.this.lblDraw.setToolTipText("<html>Phase: Draw<br>Click to toggle.</html>");
        phaseArea.add(VField.this.lblDraw, constraints);

        VField.this.lblMain1.setToolTipText("<html>Phase: Main 1<br>Click to toggle.</html>");
        phaseArea.add(VField.this.lblMain1, constraints);

        VField.this.lblBeginCombat.setToolTipText("<html>Phase: Begin Combat<br>Click to toggle.</html>");
        phaseArea.add(VField.this.lblBeginCombat, constraints);

        VField.this.lblDeclareAttackers.setToolTipText("<html>Phase: Declare Attackers<br>Click to toggle.</html>");
        phaseArea.add(VField.this.lblDeclareAttackers, constraints);

        VField.this.lblDeclareBlockers.setToolTipText("<html>Phase: Declare Blockers<br>Click to toggle.</html>");
        phaseArea.add(VField.this.lblDeclareBlockers, constraints);

        VField.this.lblFirstStrike.setToolTipText("<html>Phase: First Strike Damage<br>Click to toggle.</html>");
        phaseArea.add(VField.this.lblFirstStrike, constraints);

        VField.this.lblCombatDamage.setToolTipText("<html>Phase: Combat Damage<br>Click to toggle.</html>");
        phaseArea.add(VField.this.lblCombatDamage, constraints);

        VField.this.lblEndCombat.setToolTipText("<html>Phase: End Combat<br>Click to toggle.</html>");
        phaseArea.add(VField.this.lblEndCombat, constraints);

        VField.this.lblMain2.setToolTipText("<html>Phase: Main 2<br>Click to toggle.</html>");
        phaseArea.add(VField.this.lblMain2, constraints);

        VField.this.lblEndTurn.setToolTipText("<html>Phase: End Turn<br>Click to toggle.</html>");
        phaseArea.add(VField.this.lblEndTurn, constraints);

        VField.this.lblCleanup.setToolTipText("<html>Phase: Cleanup<br>Click to toggle.</html>");
        phaseArea.add(VField.this.lblCleanup, constraints);
    }

    /** Adds various labels to pool area JPanel container. */
    private void populateDetails() {
        final JPanel row1 = new JPanel(new MigLayout("insets 0, gap 0"));
        final JPanel row2 = new JPanel(new MigLayout("insets 0, gap 0"));
        final JPanel row3 = new JPanel(new MigLayout("insets 0, gap 0"));
        final JPanel row4 = new JPanel(new MigLayout("insets 0, gap 0"));
        final JPanel row5 = new JPanel(new MigLayout("insets 0, gap 0"));
        final JPanel row6 = new JPanel(new MigLayout("insets 0, gap 0"));

        row1.setBackground(FSkin.getColor(FSkin.Colors.CLR_ZEBRA));
        row2.setOpaque(false);
        row3.setBackground(FSkin.getColor(FSkin.Colors.CLR_ZEBRA));
        row4.setOpaque(false);
        row5.setBackground(FSkin.getColor(FSkin.Colors.CLR_ZEBRA));
        row6.setOpaque(false);

        // Hand, library, graveyard, exile, flashback, poison labels
        final String constraintsCell = "w 45%!, h 100%!, gap 0 5% 2px 2px";

        row1.add(lblHand, constraintsCell);
        row1.add(lblLibrary, constraintsCell);

        row2.add(lblGraveyard, constraintsCell);
        row2.add(lblExile, constraintsCell);

        row3.add(lblFlashback, constraintsCell);
        row3.add(lblPoison, constraintsCell);

        row4.add(lblBlack, constraintsCell);
        row4.add(lblBlue, constraintsCell);

        row5.add(lblGreen, constraintsCell);
        row5.add(lblRed, constraintsCell);

        row6.add(lblWhite, constraintsCell);
        row6.add(lblColorless, constraintsCell);

        final String constraintsRow = "w 100%!, h 16%!";
        pnlDetails.add(row1, constraintsRow + ", gap 0 0 4% 0");
        pnlDetails.add(row2, constraintsRow);
        pnlDetails.add(row3, constraintsRow);
        pnlDetails.add(row4, constraintsRow);
        pnlDetails.add(row5, constraintsRow);
        pnlDetails.add(row6, constraintsRow);
    }

    private FLabel getBuiltFLabel(SkinProp p0, String s0, String s1) {
        return new FLabel.Builder().icon(new ImageIcon(FSkin.getImage(p0)))
            .opaque(false).fontSize(14)
            .fontStyle(Font.BOLD).iconAlpha(0.6f).iconInBackground(true)
            .text(s0).tooltip(s1).fontAlign(SwingConstants.RIGHT).build();
    }

    // ========== Observer update methods
    /**
     * Handles observer update of player Zones - hand, graveyard, etc.
     * 
     * @param p0 &emsp; {@link forge.game.player.Player}
     */
    public void updateZones(final Player p0) {
        this.getLblHand().setText("" + p0.getZone(ZoneType.Hand).size());
        this.getLblGraveyard().setText("" + p0.getZone(ZoneType.Graveyard).size());
        this.getLblLibrary().setText("" + p0.getZone(ZoneType.Library).size());
        this.getLblFlashback().setText("" + CardFactoryUtil.getExternalZoneActivationCards(p0).size());
        this.getLblExile().setText("" + p0.getZone(ZoneType.Exile).size());
    }

    /**
     * Handles observer update of non-Zone details - life, poison, etc. Also
     * updates "players" panel in tabber for this player.
     * 
     * @param p0 &emsp; {@link forge.game.player.Player}
     */
    public void updateDetails(final Player p0) {
        // "Players" panel update
        CPlayers.SINGLETON_INSTANCE.update();

        // Mana pool update
        updateManaPool(p0);

        // Poison/life
        this.getLblLife().setText("" + p0.getLife());
        this.getLblPoison().setText("" + p0.getPoisonCounters());

        if (p0.getLife() <= 5) {
            this.getLblLife().setForeground(Color.red);
        }
        else {
            this.getLblLife().setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT));
        }

        if (p0.getPoisonCounters() >= 8) {
            this.getLblPoison().setForeground(Color.red);
        }
        else {
            this.getLblPoison().setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT));
        }
    }

    /**
     * Handles observer update of the mana pool.
     * 
     * @param p0 &emsp; {@link forge.game.player.Player}
     */
    public void updateManaPool(final Player p0) {
        ManaPool m = p0.getManaPool();
        this.lblBlack.setText("" + m.getAmountOfColor(forge.Constant.Color.BLACK));
        this.lblBlue.setText("" + m.getAmountOfColor(forge.Constant.Color.BLUE));
        this.lblGreen.setText("" + m.getAmountOfColor(forge.Constant.Color.GREEN));
        this.lblRed.setText("" + m.getAmountOfColor(forge.Constant.Color.RED));
        this.lblWhite.setText("" + m.getAmountOfColor(forge.Constant.Color.WHITE));
        this.lblColorless.setText("" + m.getAmountOfColor(forge.Constant.Color.COLORLESS));
    }

    //========= Retrieval methods
    /**
     * Gets the player currently associated with this field.
     * @return {@link forge.game.player.Player}
     */
    public Player getPlayer() {
        return this.player;
    }

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

    /** @return {@link javax.swing.JLabel} */
    public FLabel getLblAvatar() {
        return this.lblAvatar;
    }

    /** @return {@link javax.swing.JLabel} */
    public FLabel getLblLife() {
        return this.lblLife;
    }

    /** @return {@link javax.swing.JLabel} */
    public FLabel getLblHand() {
        return this.lblHand;
    }

    /** @return {@link javax.swing.JLabel} */
    public FLabel getLblLibrary() {
        return this.lblLibrary;
    }

    /** @return  {@link javax.swing.JLabel} */
    public JLabel getLblGraveyard() {
        return this.lblGraveyard;
    }

    /** @return  {@link javax.swing.JLabel} */
    public JLabel getLblExile() {
        return this.lblExile;
    }

    /** @return {@link javax.swing.JLabel} */
    public JLabel getLblFlashback() {
        return this.lblFlashback;
    }

    /** @return {@link javax.swing.JLabel} */
    public JLabel getLblPoison() {
        return this.lblPoison;
    }

    /** @return {@link javax.swing.JLabel} */
    public JLabel getLblColorless() {
        return this.lblColorless;
    }

    /** @return {@link javax.swing.JLabel} */
    public JLabel getLblBlack() {
        return this.lblBlack;
    }

    /** @return {@link javax.swing.JLabel} */
    public JLabel getLblBlue() {
        return this.lblBlue;
    }

    /** @return {@link javax.swing.JLabel} */
    public JLabel getLblGreen() {
        return this.lblGreen;
    }

    /** @return {@link javax.swing.JLabel} */
    public JLabel getLblRed() {
        return this.lblRed;
    }

    /** @return {@link javax.swing.JLabel} */
    public JLabel getLblWhite() {
        return this.lblWhite;
    }

    // Phases
    /** @return {@link javax.swing.JLabel} */
    public PhaseLabel getLblUpkeep() {
        return this.lblUpkeep;
    }

    /** @return {@link javax.swing.JLabel} */
    public PhaseLabel getLblDraw() {
        return this.lblDraw;
    }

    /** @return {@link javax.swing.JLabel} */
    public PhaseLabel getLblMain1() {
        return this.lblMain1;
    }

    /** @return {@link javax.swing.JLabel} */
    public PhaseLabel getLblBeginCombat() {
        return this.lblBeginCombat;
    }

    /** @return {@link javax.swing.JLabel} */
    public PhaseLabel getLblDeclareAttackers() {
        return this.lblDeclareAttackers;
    }

    /** @return {@link javax.swing.JLabel} */
    public PhaseLabel getLblDeclareBlockers() {
        return this.lblDeclareBlockers;
    }

    /** @return {@link javax.swing.JLabel} */
    public PhaseLabel getLblCombatDamage() {
        return this.lblCombatDamage;
    }

    /** @return {@link javax.swing.JLabel} */
    public PhaseLabel getLblFirstStrike() {
        return this.lblFirstStrike;
    }

    /** @return {@link javax.swing.JLabel} */
    public PhaseLabel getLblEndCombat() {
        return this.lblEndCombat;
    }

    /** @return {@link javax.swing.JLabel} */
    public PhaseLabel getLblMain2() {
        return this.lblMain2;
    }

    /** @return {@link javax.swing.JLabel} */
    public PhaseLabel getLblEndTurn() {
        return this.lblEndTurn;
    }

    /** @return {@link javax.swing.JLabel} */
    public PhaseLabel getLblCleanup() {
        return this.lblCleanup;
    }

    //========== Custom class handling

    public PhaseLabel getLabelFor(final PhaseType s) {
        switch (s) {
        case UPKEEP:
            return this.getLblUpkeep();
        case DRAW:
            return this.getLblDraw();
        case MAIN1:
            return this.getLblMain1();
        case COMBAT_BEGIN:
            return this.getLblBeginCombat();
        case COMBAT_DECLARE_ATTACKERS:
            return this.getLblDeclareAttackers();
        case COMBAT_DECLARE_BLOCKERS:
            return this.getLblDeclareBlockers();
        case COMBAT_DAMAGE:
            return this.getLblCombatDamage();
        case COMBAT_FIRST_STRIKE_DAMAGE:
            return this.getLblFirstStrike();
        case COMBAT_END:
            return this.getLblEndCombat();
        case MAIN2:
            return this.getLblMain2();
        case END_OF_TURN:
            return this.getLblEndTurn();
        case CLEANUP:
            return this.getLblCleanup();
        default:
            return null;
        }
    }

    /**
     * Shows phase labels, handles repainting and on/off states. A PhaseLabel
     * has "skip" and "active" states, meaning "this phase is (not) skipped" and
     * "this is the current phase".
     */
    @SuppressWarnings("serial")
    public class PhaseLabel extends JLabel {
        private boolean enabled = true;
        private boolean active = false;
        private boolean hover = false;


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
                    PhaseLabel.this.repaintOnlyThisLabel();
                }

                @Override
                public void mouseExited(final MouseEvent e) {
                    PhaseLabel.this.hover = false;
                    PhaseLabel.this.repaintOnlyThisLabel();
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
         * Makes this phase the current phase (or not).
         * 
         * @param b
         *            &emsp; boolean, true if phase is current
         */
        public void setActive(final boolean b) {
            this.active = b;
            this.repaintOnlyThisLabel();
        }

        /**
         * Determines if this phase is the current phase (or not).
         * 
         * @return boolean
         */
        public boolean getActive() {
            return this.active;
        }

        /** Prevent label from repainting the whole screen. */
        public void repaintOnlyThisLabel() {
            final Dimension d = PhaseLabel.this.getSize();
            repaint(0, 0, d.width, d.height);
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
                c = FSkin.getColor(FSkin.Colors.CLR_HOVER);
            } else if (this.active && this.enabled) {
                c = FSkin.getColor(FSkin.Colors.CLR_PHASE_ACTIVE_ENABLED);
            } else if (!this.active && this.enabled) {
                c = FSkin.getColor(FSkin.Colors.CLR_PHASE_INACTIVE_ENABLED);
            } else if (this.active && !this.enabled) {
                c = FSkin.getColor(FSkin.Colors.CLR_PHASE_ACTIVE_DISABLED);
            } else {
                c = FSkin.getColor(FSkin.Colors.CLR_PHASE_INACTIVE_DISABLED);
            }

            // Center vertically and horizontally. Show border if active.
            g.setColor(c);
            g.fillRoundRect(1, 1, w - 2, h - 2, 5, 5);
            super.paintComponent(g);
        }
    }
}
