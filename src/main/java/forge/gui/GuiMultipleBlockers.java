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
package forge.gui;

import java.awt.BorderLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;

import forge.AllZone;
import forge.Card;
import forge.CardList;
import forge.error.ErrorViewer;
import forge.gui.match.CMatchUI;

/**
 * <p>Constructor for Gui_MultipleBlockers4.</p>
 *
 * @author Forge
 * @version $Id$
 */

/**
 * very hacky.
 * 
 */
public class GuiMultipleBlockers extends JFrame {
    /** Constant <code>serialVersionUID=7622818310877381045L</code>. */
    private static final long serialVersionUID = 7622818310877381045L;

    private int assignDamage;
    private Card att;
    private CardList blockers;
    private CardContainer guiDisplay;

    private final BorderLayout borderLayout1 = new BorderLayout();
    private final JPanel mainPanel = new JPanel();
    private final JScrollPane jScrollPane1 = new JScrollPane();
    private final JLabel numberLabel = new JLabel();
    private final JPanel jPanel3 = new JPanel();
    private final BorderLayout borderLayout3 = new BorderLayout();
    private final JPanel creaturePanel = new JPanel();

    /**
     * <p>
     * Constructor for Gui_MultipleBlockers4.
     * </p>
     * 
     * @param attacker
     *            a {@link forge.Card} object.
     * @param creatureList
     *            a {@link forge.CardList} object.
     * @param damage
     *            a int.
     */
    public GuiMultipleBlockers(final Card attacker, final CardList creatureList, final int damage) {
        this();
        this.assignDamage = damage;
        this.updateDamageLabel(); // update user message about assigning damage
        this.att = attacker;
        this.blockers = creatureList;

        for (int i = 0; i < creatureList.size(); i++) {
            this.creaturePanel.add(new CardPanel(creatureList.get(i)));
        }

        if (this.att.hasKeyword("Trample")) {
            final Card player = new Card();
            player.setName("Player");
            player.addIntrinsicKeyword("Shroud");
            player.addIntrinsicKeyword("Indestructible");
            this.creaturePanel.add(new CardPanel(player));
        }

        final JDialog dialog = new JDialog(this, true);
        dialog.setTitle("Multiple Blockers");
        dialog.setContentPane(this.mainPanel);
        dialog.setSize(470, 310);
        dialog.setVisible(true);
    }

    /**
     * <p>
     * Constructor for Gui_MultipleBlockers4.
     * </p>
     */
    public GuiMultipleBlockers() {
        try {
            this.jbInit();
        } catch (final Exception ex) {
            ErrorViewer.showError(ex);
        }
        // setSize(470, 280);
        // show();
    }

    /**
     * <p>
     * jbInit.
     * </p>
     * 
     * @throws java.lang.Exception
     *             if any.
     */
    private void jbInit() throws Exception {
        this.getContentPane().setLayout(this.borderLayout1);
        this.setTitle("Multiple Blockers");
        this.mainPanel.setLayout(null);
        this.numberLabel.setHorizontalAlignment(SwingConstants.CENTER);
        this.numberLabel.setHorizontalTextPosition(SwingConstants.CENTER);
        this.numberLabel.setText("Assign");
        this.numberLabel.setBounds(new Rectangle(52, 10, 343, 24));
        this.jPanel3.setLayout(this.borderLayout3);
        this.jPanel3.setBounds(new Rectangle(15, 40, 430, 235));
        this.creaturePanel.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(final MouseEvent e) {
                GuiMultipleBlockers.this.creaturePanelMousePressed(e);
            }
        });
        this.creaturePanel.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override
            public void mouseMoved(final MouseEvent e) {
                GuiMultipleBlockers.this.creaturePanelMouseMoved(e);
            }
        });
        this.mainPanel.add(this.jPanel3, null);
        this.jPanel3.add(this.jScrollPane1, BorderLayout.CENTER);
        this.mainPanel.add(this.numberLabel, null);
        this.jScrollPane1.getViewport().add(this.creaturePanel, null);
        this.getContentPane().add(this.mainPanel, BorderLayout.CENTER);
    }

    /**
     * <p>
     * okButton_actionPerformed.
     * </p>
     * 
     * @param e
     *            a {@link java.awt.event.ActionEvent} object.
     */
    void okButtonActionPerformed(final ActionEvent e) {
        this.dispose();
    }

    /**
     * <p>
     * creaturePanel_mousePressed.
     * </p>
     * 
     * @param e
     *            a {@link java.awt.event.MouseEvent} object.
     */
    void creaturePanelMousePressed(final MouseEvent e) {
        final Object o = this.creaturePanel.getComponentAt(e.getPoint());
        if (o instanceof CardPanel) {

            boolean assignedDamage = true;

            final CardContainer cardPanel = (CardContainer) o;
            final Card c = cardPanel.getCard();
            // c.setAssignedDamage(c.getAssignedDamage() + 1);
            final CardList cl = new CardList();
            cl.add(this.att);

            boolean assignedLethalDamageToAllBlockers = true;
            for (final Card crd : this.blockers) {
                if ((crd.getLethalDamage() > 0)
                        && (!this.att.hasKeyword("Deathtouch") || (crd.getTotalAssignedDamage() < 1))) {
                    assignedLethalDamageToAllBlockers = false;
                }
            }

            if (c.getName().equals("Player") && this.att.hasKeyword("Trample") && assignedLethalDamageToAllBlockers) {
                AllZone.getCombat().addDefendingDamage(1, this.att);
                c.addAssignedDamage(1, this.att);
            } else if (!c.getName().equals("Player")) {
                c.addAssignedDamage(1, this.att);
            } else {
                assignedDamage = false;
            }

            if (assignedDamage) {
                this.assignDamage--;
                this.updateDamageLabel();
                if (this.assignDamage == 0) {
                    this.dispose();
                }
            }

            if (this.guiDisplay != null) {
                this.guiDisplay.setCard(c);
            }
        }
        // reduce damage, show new user message, exit if necessary

    } // creaturePanel_mousePressed()

    /**
     * <p>
     * updateDamageLabel.
     * </p>
     */
    void updateDamageLabel() {
        this.numberLabel.setText("Assign " + this.assignDamage + " damage - click on card to assign damage");
    }

    /**
     * <p>
     * creaturePanel_mouseMoved.
     * </p>
     * 
     * @param e
     *            a {@link java.awt.event.MouseEvent} object.
     */
    void creaturePanelMouseMoved(final MouseEvent e) {
        final Object o = this.creaturePanel.getComponentAt(e.getPoint());
        if (o instanceof CardPanel) {
            final CardContainer cardPanel = (CardContainer) o;
            final Card c = cardPanel.getCard();

            CMatchUI.SINGLETON_INSTANCE.setCard(c);
        }
    }
}
