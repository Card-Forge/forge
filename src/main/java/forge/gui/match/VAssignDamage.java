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
package forge.gui.match;

import java.awt.Dialog.ModalityType;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.LineBorder;

import net.miginfocom.swing.MigLayout;
import forge.AllZone;
import forge.Card;
import forge.Command;
import forge.GameEntity;
import forge.gui.SOverlayUtils;
import forge.gui.toolbox.FLabel;
import forge.gui.toolbox.FScrollPane;
import forge.gui.toolbox.FSkin;
import forge.view.arcane.CardPanel;

/**
 * Assembles Swing components of assign damage dialog.
 * 
 * This needs a JDialog to maintain a modal state.
 * Without the modal state, the PhaseHandler automatically
 * moves forward to phase Main2 without assigning damage.
 *
 * <br><br><i>(V at beginning of class name denotes a view class.)</i>
 */
@SuppressWarnings("serial")
public class VAssignDamage {
    // Width and height of blocker dialog
    private final int wDlg = 700;
    private final int hDlg = 500;
    private final JDialog dlg = new JDialog();

    // Damage storage
    private final int totalDamageToAssign;
    private int damageLeftToAssign;
    private boolean deathtouch = false;
    private boolean trample = false;
    private int damageToOpponent = 0;
    private final Card attacker;
    private final GameEntity defender;
    private Integer activeIndex = 0;

    private final JLabel lblTotalDamage = new FLabel.Builder().text("Available damage points: Unknown").build();
    private final FLabel lblOK = new FLabel.Builder().text("OK").hoverable(true).opaque(true).fontSize(16).build();
    
    // TODO Add Auto and Reset Buttons and hook them up to created functions

    // Indexes of defenders correspond to their indexes in the damage list and labels.
    private final List<Card> lstDefenders = new ArrayList<Card>();
    private final List<Integer> lstDamage = new ArrayList<Integer>();
    private final List<JLabel> lstDamageLabels = new ArrayList<JLabel>();

    private boolean canAssignToIndex(Integer selectedIndex) {
        Integer active = this.activeIndex;
        
        if (selectedIndex == null) {
            // Trying to assign to the opponent
            if (active == null) {
                return true;
            }
            
            if (active != this.lstDamage.size() -1) {
                return false;
            }
            
            int activeLethal = this.deathtouch ? 1 : lstDefenders.get(active).getLethalDamage();
            int assignedToActive = lstDamage.get(active); 
            
            if (assignedToActive < activeLethal) {
                return false;
            }
        }
        else {
            // Trying to assign to a combatant
            if (active == null) {
                return false;
            }
            
            if (active == selectedIndex) {
                return true;
            }
            
            int activeLethal = this.deathtouch ? 1 : lstDefenders.get(active).getLethalDamage();
            int assignedToActive = lstDamage.get(active); 
            
            if (active != selectedIndex - 1 || assignedToActive < activeLethal) {
                return false;
            }
        }
        
        return true;
    }
    
    // Mouse actions
    private final MouseAdapter madDefender = new MouseAdapter() {
        @Override
        public void mouseEntered(final MouseEvent evt) {
            Card source = ((CardPanel) evt.getSource()).getCard();
            int index = lstDefenders.indexOf(source);
            FSkin.Colors brdrColor = FSkin.Colors.CLR_INACTIVE;
            if (VAssignDamage.this.canAssignToIndex(index)) {
                brdrColor = FSkin.Colors.CLR_ACTIVE;
            }
            ((CardPanel) evt.getSource()).setBorder(new LineBorder(FSkin.getColor(brdrColor), 2));
        }

        @Override
        public void mouseExited(final MouseEvent evt) {
            ((CardPanel) evt.getSource()).setBorder(null);
        }

        @Override
        public void mousePressed(final MouseEvent evt) {
            Card source = ((CardPanel) evt.getSource()).getCard();
            int index = lstDefenders.indexOf(source);
            
            // Allow click if this is active, or next to active and active has lethal
            if (!VAssignDamage.this.canAssignToIndex(index)) {
                return;
            }

            boolean meta = evt.isControlDown();
            int alreadyAssignDamage = lstDamage.get(index);
            int lethal = VAssignDamage.this.deathtouch ? 1 : source.getLethalDamage();
            int assignedDamage = 1;
            
            // Add damage for left clicks, as much as we can for ctrl clicking
            if (SwingUtilities.isLeftMouseButton(evt)) {
                if (meta) {
                    assignedDamage = VAssignDamage.this.damageLeftToAssign;
                }
                else if (alreadyAssignDamage == 0) {
                    assignedDamage = lethal;
                }
   

                assignCombatantDamage(source, assignedDamage);
            }
            
            // Remove damage for right clicks, as much as we can for ctrl clicking
            else if (SwingUtilities.isRightMouseButton(evt)) {
                if (meta) {
                    if (index == 0) {
                        assignedDamage = lethal - alreadyAssignDamage;
                    }
                    else {
                        assignedDamage = -alreadyAssignDamage;
                    }
                }
                else {
                    if (alreadyAssignDamage == 0) {
                        return;
                    }
                    if (alreadyAssignDamage == lethal) {
                        if (index == 0) {
                            return;
                        }
                        assignedDamage = -lethal;
                    }
                    else {
                        assignedDamage = -1;
                    }
                }
                assignCombatantDamage(source, assignedDamage);
            }
        }
    };

    private final MouseAdapter madOpponent = new MouseAdapter() {
        @Override
        public void mouseEntered(final MouseEvent evt) {
            FSkin.Colors brdrColor = FSkin.Colors.CLR_INACTIVE;
            if (VAssignDamage.this.canAssignToIndex(null)) {
                brdrColor = FSkin.Colors.CLR_ACTIVE;
            }
            ((CardPanel) evt.getSource()).setBorder(new LineBorder(FSkin.getColor(brdrColor), 2));
        }

        @Override
        public void mouseExited(final MouseEvent evt) {
            ((CardPanel) evt.getSource()).setBorder(null);
        }

        @Override
        public void mousePressed(final MouseEvent evt) {
            if (!VAssignDamage.this.canAssignToIndex(null)) {
                return;
            }

            int assignedDamage = 0;
            boolean meta = evt.isControlDown();
            if (SwingUtilities.isLeftMouseButton(evt)) {
                if (meta) {
                    assignedDamage = VAssignDamage.this.damageLeftToAssign;
                }
                else {
                    assignedDamage = 1;
                }
                assignOpponentDamage(assignedDamage);
            }
            else if (SwingUtilities.isRightMouseButton(evt)) {
                int alreadyAssignDamage = VAssignDamage.this.damageToOpponent;
                if (meta) {
                    assignedDamage = -alreadyAssignDamage;
                }
                else {
                    if (alreadyAssignDamage == 0) {
                        return;
                    }
                    assignedDamage = -1;
                }
                assignOpponentDamage(assignedDamage);
            }
        }
    };

    private final Command cmdOK = new Command() { @Override
        public void execute() { finish(); } };

    /** Constructor.
     * 
     * @param attacker0 {@link forge.Card}
     * @param defenders0 List<{@link forge.Card}>
     * @param damage0 int
     * @param defender GameEntity that's bein attacked
     */
    public VAssignDamage(final Card attacker0, final List<Card> defenders0, final int damage0, final GameEntity defender) {
        // Set damage storage vars
        this.totalDamageToAssign = damage0;
        this.damageLeftToAssign = damage0;
        this.defender = defender;
        this.deathtouch = attacker0.hasKeyword("Deathtouch");
        this.trample = defender != null && attacker0.hasKeyword("Trample");
        this.attacker = attacker0;

        for (final Card c : defenders0) {
            this.lstDefenders.add(c);
            this.lstDamage.add(0);
            this.lstDamageLabels.add(new FLabel.Builder().text("0").fontSize(18).fontAlign(SwingConstants.CENTER).build());
        }

        // Top-level UI stuff
        final JPanel overlay = SOverlayUtils.genericOverlay();
        final JPanel pnlMain = new JPanel();
        pnlMain.setBackground(FSkin.getColor(FSkin.Colors.CLR_THEME2));

        // Attacker area
        final CardPanel pnlAttacker = new CardPanel(attacker0);
        pnlAttacker.setOpaque(false);
        pnlAttacker.setCardBounds(0, 0, 105, 150);

        final JPanel pnlInfo = new JPanel(new MigLayout("insets 0, gap 0, wrap"));
        pnlInfo.setOpaque(false);
        pnlInfo.add(lblTotalDamage, "gap 0 0 20px 5px");
        pnlInfo.add(new FLabel.Builder().text("Left click: Assign 1 damage. (Left Click + Control): Assign remaining damage").build(), "gap 0 0 0 5px");
        pnlInfo.add(new FLabel.Builder().text("Right click: Unassign 1 damage. (Right Click + Control): Unassign all damage.").build(), "gap 0 0 0 5px");

        // Defenders area
        final JPanel pnlDefenders = new JPanel();
        pnlDefenders.setOpaque(false);
        final String wrap = (trample ? "wrap " + (lstDefenders.size() + 1) : "wrap " + lstDefenders.size());
        pnlDefenders.setLayout(new MigLayout("insets 0, gap 0, ax center, " + wrap));

        final FScrollPane scrDefenders = new FScrollPane(pnlDefenders);
        scrDefenders.setBorder(null);

        // Top row of cards...
        for (final Card c : lstDefenders) {
            final CardPanel cp = new CardPanel(c);
            cp.setCardBounds(0, 0, 105, 150);
            cp.setOpaque(true);
            pnlDefenders.add(cp, "w 145px!, h 170px!, gap 5px 5px 3px 3px, ax center");
            cp.addMouseListener(madDefender);
        }

        // Add "opponent placeholder" card if trample allowed
        // If trample allowed, make card placeholder
        if (trample) {
            CardPanel defPanel;
            if (this.defender instanceof Card) {
                defPanel = new CardPanel((Card) this.defender);
            }
            else {
                final Card crdOpponentPlaceholder = new Card();
                crdOpponentPlaceholder.setName(this.defender.getName());
                defPanel = new CardPanel(crdOpponentPlaceholder);
            }

            defPanel.setCardBounds(0, 0, 105, 150);
            defPanel.addMouseListener(madOpponent);
            defPanel.setOpaque(true);
            pnlDefenders.add(defPanel, "w 145px!, h 170px!, gap 5px 5px 3px 3px, ax center");
            lstDamageLabels.add(new FLabel.Builder().text("0").fontSize(18).fontAlign(SwingConstants.CENTER).build());
        }

        // ... bottom row of labels.
        for (int i = 0; i < lstDamageLabels.size(); i++) {
            pnlDefenders.add(lstDamageLabels.get(i), "w 145px!, h 30px!, gap 5px 5px 0 5px");
        }

        lblOK.setCommand(cmdOK);

        // Final UI layout
        pnlMain.setLayout(new MigLayout("insets 0, gap 0, wrap 2, ax center"));
        pnlMain.add(pnlAttacker, "w 125px!, h 160px!, gap 50px 0 0 15px");
        pnlMain.add(pnlInfo, "gap 20px 0 0 15px");
        pnlMain.add(scrDefenders, "w 96%!, gap 2% 0 0 0, pushy, growy, ax center, span 2");
        pnlMain.add(lblOK, "w 100px!, h 30px!, gap 0 0 5px 10px, ax center, span 2");

        overlay.add(pnlMain);

        initialAssignDamage();
        SOverlayUtils.showOverlay();

        this.dlg.setUndecorated(true);
        this.dlg.setContentPane(pnlMain);
        this.dlg.setSize(new Dimension(wDlg, hDlg));
        this.dlg.setLocation((overlay.getWidth() - wDlg) / 2, (overlay.getHeight() - hDlg) / 2);
        this.dlg.setModalityType(ModalityType.APPLICATION_MODAL);
        this.dlg.setVisible(true);
    }

    private void initialAssignDamage() {
        // Assign "1" damage to first combatant (it will really assign lethal damage)
        int lethalDamage = this.deathtouch ? 1 : lstDefenders.get(0).getLethalDamage();
        int damage = Math.min(lethalDamage, this.damageLeftToAssign);
        assignCombatantDamage(0, damage);
    }

    /** Reset Assign Damage back to how it was at the beginning. */
    private void resetAssignDamage() {
        // Functions for two new buttons that I'll try to add to the Dialog soon
        int size = lstDefenders.size();
        for (int i = 0; i < size; i++) {
            lstDamage.set(i, 0);
        }
        this.damageToOpponent = 0;
        this.damageLeftToAssign = this.totalDamageToAssign;
        initialAssignDamage();
    }

    /** Goes through defenders assigning lethal damage until exhausted,
     * then overflows extra onto opponent or last defender card. */
    private void autoAssignDamage() {
        // Assign lethal damage to each combatant
        int size = lstDefenders.size();
        for (int i = 0; i < size; i++) {
            int lethalDamage = this.deathtouch ? 1 : lstDefenders.get(i).getLethalDamage();
            int damage = Math.min(lethalDamage, this.damageLeftToAssign);
            if (damage == 0) {
                break;
            }

            if (!this.trample && size - 1 == i) {
                damage = this.damageLeftToAssign;
            }

            lstDamage.set(i, damage);
            this.damageLeftToAssign -= damage;
        }

        if (this.trample) {
            damageToOpponent = this.damageLeftToAssign;
            this.damageLeftToAssign = 0;
        }
        // Should we just finish, or update and then let them say ok?
        update(null);
        //finish();
    }

    private void assignCombatantDamage(final Card card, int damage) {
        int index = lstDefenders.indexOf(card);
        assignCombatantDamage(index, damage);
    }

    private void assignCombatantDamage(int index, int damage) {
        // If we don't have enough left or we're trying to unassign too much return
        if (this.damageLeftToAssign < damage) {
            return;
        }

        if (this.damageLeftToAssign - damage > this.totalDamageToAssign) {
            return;
        }

        int newDamage = lstDamage.get(index) + damage;
        lstDamage.set(index, newDamage);

        this.damageLeftToAssign -= damage;

        update(index);
    }

    private void assignOpponentDamage(int damage) {
        damage = Math.min(damage, this.damageLeftToAssign);
        if (damage == 0) {
            return;
        }
        damageToOpponent += damage;
        this.damageLeftToAssign -= damage;
        update(null);
    }

    /** Updates labels and other UI elements.
     * @param index index of the last assigned damage*/
    private void update(Integer index) {
        StringBuilder damageLeft = new StringBuilder("Available damage points: ");
        damageLeft.append(this.damageLeftToAssign);
        damageLeft.append(" (of ");
        damageLeft.append(this.totalDamageToAssign);
        damageLeft.append(")");

        this.lblTotalDamage.setText(damageLeft.toString());

        int dmg;
        for (int i = 0; i < lstDefenders.size(); i++) {
            dmg = lstDamage.get(i);
            StringBuilder sb = new StringBuilder();
            sb.append(dmg);

            if ((this.deathtouch && dmg > 0) || (dmg >= lstDefenders.get(i).getLethalDamage())) {
                sb.append(" (Lethal)");
            }
            this.lstDamageLabels.get(i).setText(sb.toString());
        }

        // If there's an opponent, update their label.
        if (trample) {
            this.lstDamageLabels.get(lstDefenders.size()).setText(String.valueOf(damageToOpponent));
        }

        if (index != null) {
            int newDamage = lstDamage.get(index);
            //int lethal = this.deathtouch ? 1 : lstDefenders.get(index).getLethalDamage();

            // Update Listeners
            if (newDamage == 0) {
                this.activeIndex = Math.max(0, index - 1);
            }
            else {
                this.activeIndex = index;
            }
        }
        else {
            int newDamage = this.damageToOpponent;
            if (newDamage == 0) {
                this.activeIndex = lstDamage.size() - 1;
            }
            else {
                this.activeIndex = null;
            }
        }
    }

    // Dumps damage onto cards. Damage must be stored first, because if it is
    // assigned dynamically, the cards die off and further damage to them can't
    // be modified.
    private void finish() {
        if (trample) {
            AllZone.getCombat().addDefendingDamage(this.damageToOpponent, this.attacker);
        }

        for (int i = 0; i < lstDefenders.size(); i++) {
            lstDefenders.get(i).addAssignedDamage(lstDamage.get(i), this.attacker);
            lstDefenders.get(i).updateObservers();
        }

        dlg.dispose();
        SOverlayUtils.hideOverlay();
    }
}
