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
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;

import forge.AllZone;
import forge.Card;
import forge.CardList;
import forge.GameEntity;
import forge.error.ErrorViewer;
import forge.gui.match.CMatchUI;

// TODO CardPanel doesn't show the image properly. Everything else seems to work

/**
 * <p>Constructor for GuiAssignDamageFrame.</p>
 *
 * @author Forge
 * @version $Id$
 */
public class GuiAssignDamageFrame extends JFrame {
    // Card (attacker)
    // Card (defender)
    // Remaining Damage
    // List of remaining blockers/defender if Trample
    
    /** Constant <code>serialVersionUID=7622818310877381045L</code>. */
    private static final long serialVersionUID = 7622818310877381045L;

    private int assignDamage;
    private int assignedToActive = 0;
    private Card damageAssigner;
    private Card activeRecipient;
    private CardList recipients;
    private boolean deathtouch = false;
    
    private final BorderLayout borderLayout1 = new BorderLayout();
    private final JPanel mainPanel = new JPanel();
    private final JScrollPane jScrollPane1 = new JScrollPane();
    private final JLabel numberLabel = new JLabel();
    private final JPanel jPanel3 = new JPanel();
    private final BorderLayout borderLayout3 = new BorderLayout();
    private final JPanel creaturePanel = new JPanel();
    private final JPanel buttonPanel = new JPanel();
    private final BoxLayout buttonLayout = new BoxLayout(buttonPanel, BoxLayout.X_AXIS);
    
    private UnsortedListModel recipientsListModel = new UnsortedListModel();
    private final JList recipientsList = new JList(recipientsListModel);
    
    private final JButton oneDamageButton = new JButton("1");
    private final JButton remainingDamageButton = new JButton("Remaining");
    private final JButton nextButton = new JButton("Next");

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
    public GuiAssignDamageFrame(final Card attacker, final CardList creatureList, final int damage) {
        this();
        this.assignDamage = damage;
        this.damageAssigner = attacker;
        this.recipients = creatureList;
        this.deathtouch = attacker.hasKeyword("Deathtouch");

        this.activeRecipient = recipients.get(0);
        this.creaturePanel.add(new CardPanel(damageAssigner));
        this.creaturePanel.add(new CardPanel(activeRecipient));
        
        for(Card c : creatureList) {
            if (!activeRecipient.equals(c)) {
                this.recipientsListModel.add(c);
            }
        }
        
        if (this.damageAssigner.hasKeyword("Trample")) {
            GameEntity pl = AllZone.getCombat().getDefendingEntity(damageAssigner);
            this.recipientsListModel.add(pl);
        }

        this.assignMinimumDamage();
        this.update();
        
        final JDialog dialog = new JDialog(this, true);
        dialog.setTitle("Assign Damage Panel");
        dialog.setContentPane(this.mainPanel);
        dialog.setSize(470, 410);
        dialog.setVisible(true); 
    }

    /**
     * <p>
     * Constructor for Gui_MultipleBlockers4.
     * </p>
     */
    public GuiAssignDamageFrame() {
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
        this.setTitle("Assign Damage Panel");
        this.mainPanel.setLayout(null);
        this.numberLabel.setHorizontalAlignment(SwingConstants.CENTER);
        this.numberLabel.setHorizontalTextPosition(SwingConstants.CENTER);
        this.numberLabel.setText("Assign Damage:");
        this.numberLabel.setBounds(new Rectangle(52, 10, 343, 24));
        this.jPanel3.setLayout(this.borderLayout3);
        this.jPanel3.setBounds(new Rectangle(15, 40, 430, 235));
        
        this.buttonPanel.setBounds(30, 30, 50, 235);
        this.buttonPanel.setLayout(buttonLayout);
        oneDamageButton.setToolTipText("Assign 1 Damage");
        this.buttonPanel.add(oneDamageButton);
        remainingDamageButton.setToolTipText("Assign Remaining Damage");
        this.buttonPanel.add(remainingDamageButton);
        this.buttonPanel.add(nextButton);
        
        this.jPanel3.add(this.buttonPanel, BorderLayout.SOUTH);
        
        this.oneDamageButton.addActionListener(new ActionListener() {            
            @Override
            public void actionPerformed(ActionEvent arg) {
                GuiAssignDamageFrame.this.assignDamageToActiveCreature(1);
            }
        });

        this.remainingDamageButton.addActionListener(new ActionListener() {            
            @Override
            public void actionPerformed(ActionEvent arg) {
                GuiAssignDamageFrame.this.assignDamageToActiveCreature(GuiAssignDamageFrame.this.assignDamage);
            }
        });
        
        this.nextButton.addActionListener(new ActionListener() {            
            @Override
            public void actionPerformed(ActionEvent arg) {
                GuiAssignDamageFrame.this.nextButtonActionPerformed();
            }
        });

        this.creaturePanel.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override
            public void mouseMoved(final MouseEvent e) {
                GuiAssignDamageFrame.this.creaturePanelMouseMoved(e);
            }
        });
        this.mainPanel.add(this.jPanel3, null);
        this.jPanel3.add(this.jScrollPane1, BorderLayout.CENTER);
        this.mainPanel.add(this.numberLabel, null);
        this.jScrollPane1.getViewport().add(this.creaturePanel, null);
        this.getContentPane().add(this.mainPanel, BorderLayout.CENTER);

        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setSize(300, 300);
        rightPanel.add(new JScrollPane(recipientsList), BorderLayout.CENTER);
        this.jPanel3.add(rightPanel, BorderLayout.EAST); 
    }

    /**
     * <p>
     * okButton_actionPerformed.
     * </p>
     * 
     * @param e
     *            a {@link java.awt.event.ActionEvent} object.
     */
    void nextButtonActionPerformed() {
        GameEntity entity = (GameEntity)this.recipientsListModel.getElementAt(0);
        this.recipientsListModel.removeElement(entity);
        if (this.recipientsListModel.getSize() == 0){
            assignDamageToEntity(this.assignDamage, entity);
            this.dispose();
            return;
        }
        // Remove the recipient
        this.creaturePanel.remove(1);
        this.activeRecipient = (Card)entity;
        this.creaturePanel.add(new CardPanel(this.activeRecipient));
        this.assignedToActive = 0;
        this.assignMinimumDamage();
        this.update();
    }
    
    void assignMinimumDamage() {
        int lethal = 1;
        if (!this.deathtouch) {
            Card c = this.activeRecipient;
            lethal = c.getLethalDamage();
        }
        
        if (!this.assignDamageToActiveCreature(lethal)) {
            this.assignDamageToActiveCreature(this.assignDamage);
        }
    }

    void assignDamageToEntity(int damage, GameEntity entity) {
        if (entity instanceof Card) {
            ((Card)entity).addAssignedDamage(damage, this.damageAssigner);
        }
        else {
            AllZone.getCombat().addDefendingDamage(damage, this.damageAssigner);
        }
        this.assignDamage -= damage;
        this.assignedToActive += damage;

        // Update button availability
        if (this.assignDamage == 0) {
            this.dispose();
        }
        else {
            this.update();

            CMatchUI.SINGLETON_INSTANCE.setCard(this.activeRecipient);
        }
    }
    
    boolean assignDamageToActiveCreature(int damage) {
        if (damage > this.assignDamage) {
            return false;
        }
        assignDamageToEntity(damage, this.activeRecipient);
        return true;
    }

    void update() {
        this.updateDamageLabel();
        this.updateButtons();
        this.mainPanel.invalidate();
    }
    
    /**
     * <p>
     * updateButtons.
     * </p>
     */
    void updateButtons() {
        int lethal = this.activeRecipient.getLethalDamage();
        if (this.deathtouch && lethal > 0) {
            lethal = 1;
        }

        this.remainingDamageButton.setText("Remaining("+String.valueOf(this.assignDamage)+")");
        this.nextButton.setEnabled(lethal == 0);
    }
    
    /**
     * <p>
     * updateDamageLabel.
     * </p>
     */
    void updateDamageLabel() {
        StringBuilder sb = new StringBuilder();
        sb.append("Damage Left(");
        sb.append(this.assignDamage);
        sb.append("). Assigned to ");
        sb.append(this.activeRecipient.isFaceDown() ? "Morph" : this.activeRecipient.getName());
        sb.append("(");
        sb.append(this.assignedToActive);
        sb.append(")");
        
        this.numberLabel.setText( sb.toString());
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
