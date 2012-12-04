/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Nate
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
package forge.gui.match.views;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.EmptyBorder;

import net.miginfocom.swing.MigLayout;
import forge.gui.MultiLineLabelUI;
import forge.gui.framework.DragCell;
import forge.gui.framework.DragTab;
import forge.gui.framework.EDocID;
import forge.gui.framework.IVDoc;
import forge.gui.match.controllers.CDev;
import forge.gui.toolbox.FSkin;

/** 
 * Assembles Swing components of players report.
 *
 * <br><br><i>(V at beginning of class name denotes a view class.)</i>
 */
public enum VDev implements IVDoc<CDev> {
    /** */
    SINGLETON_INSTANCE;

    // Fields used with interface IVDoc
    private DragCell parentCell;
    private final DragTab tab = new DragTab("Dev Mode");

    // Other fields
    private List<JLabel> devLBLs = new ArrayList<JLabel>();

    // Top-level containers
    private final JPanel viewport = new JPanel(new MigLayout("wrap, insets 0"));
    private final JScrollPane scroller = new JScrollPane(viewport,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

    // Dev labels
    private final DevLabel lblMilling = new DevLabel("Loss by Milling: Enabled", "Loss by Milling: Disabled");
    private final DevLabel lblUnlimitedLands = new DevLabel("Play Unlimited Lands This Turn: Enabled",
            "Play Unlimited Lands This Turn: Disabled");
    private final DevLabel lblGenerateMana = new DevLabel("Generate Mana");
    private final DevLabel lblSetupGame = new DevLabel("Setup Game State");
    private final DevLabel lblTutor = new DevLabel("Tutor for Card");
    private final DevLabel lblCounterPermanent = new DevLabel("Add Counter to Permanent");
    private final DevLabel lblTapPermanent = new DevLabel("Tap Permanent");
    private final DevLabel lblUntapPermanent = new DevLabel("Untap Permanent");
    private final DevLabel lblSetLife = new DevLabel("Set Player Life");
    private final DevLabel lblCardToBattlefield = new DevLabel("Add card to play");
    private final DevLabel lblCardToHand = new DevLabel("Add card to hand");
    private final DevLabel lblBreakpoint = new DevLabel("Trigger breakpoint");

    //========= Constructor

    private VDev() {
        devLBLs.add(lblMilling);
        devLBLs.add(lblUnlimitedLands);
        devLBLs.add(lblGenerateMana);
        devLBLs.add(lblSetupGame);
        devLBLs.add(lblTutor);
        devLBLs.add(lblCardToHand);
        devLBLs.add(lblCardToBattlefield);
        devLBLs.add(lblCounterPermanent);
        devLBLs.add(lblTapPermanent);
        devLBLs.add(lblUntapPermanent);
        devLBLs.add(lblSetLife);
        devLBLs.add(lblBreakpoint);

        scroller.setBorder(null);
        scroller.setOpaque(false);
        scroller.getViewport().setOpaque(false);
        viewport.setOpaque(false);

        final String constraints = "w 95%!, gap 0 0 5px 0";
        viewport.add(this.lblMilling, constraints);
        viewport.add(this.lblUnlimitedLands, constraints);
        viewport.add(this.lblGenerateMana, constraints);
        viewport.add(this.lblSetupGame, constraints);
        viewport.add(this.lblTutor, constraints);
        viewport.add(this.lblCardToHand, constraints);
        viewport.add(this.lblCardToBattlefield, constraints);
        viewport.add(this.lblCounterPermanent, constraints);
        viewport.add(this.lblTapPermanent, constraints);
        viewport.add(this.lblUntapPermanent, constraints);
        viewport.add(this.lblSetLife, constraints);
        viewport.add(this.lblBreakpoint, constraints);
    }

    //========= Overridden methods

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#populate()
     */
    @Override
    public void populate() {
        parentCell.getBody().setLayout(new MigLayout("insets 0, gap 0"));
        parentCell.getBody().add(scroller, "w 100%, h 100%!");
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

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getDocumentID()
     */
    @Override
    public EDocID getDocumentID() {
        return EDocID.DEV_MODE;
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
    public CDev getLayoutControl() {
        return CDev.SINGLETON_INSTANCE;
    }

    //========= Retrieval methods

    /** @return {@link forge.gui.match.views.VDev.DevLabel} */
    public DevLabel getLblMilling() {
        return this.lblMilling;
    }

    /** @return {@link forge.gui.match.views.VDev.DevLabel} */
    public DevLabel getLblGenerateMana() {
        return this.lblGenerateMana;
    }

    /** @return {@link forge.gui.match.views.VDev.DevLabel} */
    public DevLabel getLblSetupGame() {
        return this.lblSetupGame;
    }

    /** @return {@link forge.gui.match.views.VDev.DevLabel} */
    public DevLabel getLblTutor() {
        return this.lblTutor;
    }

    /** @return {@link forge.gui.match.views.VDev.DevLabel} */
    public DevLabel getLblCardToHand() {
        return this.lblCardToHand;
    }

    /** @return {@link forge.gui.match.views.VDev.DevLabel} */
    public final DevLabel getLblCardToBattlefield() {
        return lblCardToBattlefield;
    }

    /** @return {@link forge.gui.match.views.VDev.DevLabel} */
    public DevLabel getLblCounterPermanent() {
        return this.lblCounterPermanent;
    }

    /** @return {@link forge.gui.match.views.VDev.DevLabel} */
    public DevLabel getLblTapPermanent() {
        return this.lblTapPermanent;
    }

    /** @return {@link forge.gui.match.views.VDev.DevLabel} */
    public DevLabel getLblUntapPermanent() {
        return this.lblUntapPermanent;
    }

    /** @return {@link forge.gui.match.views.VDev.DevLabel} */
    public DevLabel getLblUnlimitedLands() {
        return this.lblUnlimitedLands;
    }

    /** @return {@link forge.gui.match.views.VDev.DevLabel} */
    public DevLabel getLblSetLife() {
        return this.lblSetLife;
    }

    public DevLabel getLblBreakpoint() {
        return this.lblBreakpoint;
    }

    /**
     * Labels that act as buttons which control dev mode functions. Labels are
     * used to support multiline text.
     */
    public class DevLabel extends JLabel {
        private static final long serialVersionUID = 7917311680519060700L;

        private Color defaultBG = FSkin.getColor(FSkin.Colors.CLR_ACTIVE);
        private final Color hoverBG = FSkin.getColor(FSkin.Colors.CLR_HOVER);
        private final Color pressedBG = FSkin.getColor(FSkin.Colors.CLR_INACTIVE);
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
                this.defaultBG = FSkin.getColor(FSkin.Colors.CLR_ACTIVE);
                s = this.enabledText;
            } else {
                this.defaultBG = FSkin.getColor(FSkin.Colors.CLR_INACTIVE);
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
}
