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
    private boolean deathtouch = false;
    private boolean trample = false;
    private int damageToOpponent = 0;
    private final Card attacker;

    private final JLabel lblTotalDamage = new FLabel.Builder().text("Available damage points: Unknown").build();
    private final FLabel lblOK = new FLabel.Builder().text("OK").hoverable(true).opaque(true).fontSize(16).build();

    // Indexes of defenders correspond to their indexes in the damage list and labels.
    private final List<Card> lstDefenders = new ArrayList<Card>();
    private final List<Integer> lstDamage = new ArrayList<Integer>();
    private final List<JLabel> lstDamageLabels = new ArrayList<JLabel>();

    // Mouse actions
    private final MouseAdapter madDefender = new MouseAdapter() {
        @Override
        public void mouseEntered(final MouseEvent evt) {
            ((CardPanel) evt.getSource()).setBorder(new LineBorder(FSkin.getColor(FSkin.Colors.CLR_ACTIVE), 2));
        }

        @Override
        public void mouseExited(final MouseEvent evt) {
            ((CardPanel) evt.getSource()).setBorder(null);
        }

        @Override
        public void mousePressed(final MouseEvent evt) {
            if (SwingUtilities.isLeftMouseButton(evt)) {
                takeDefenderDamage(((CardPanel) evt.getSource()).getCard());
            }
            else if (SwingUtilities.isRightMouseButton(evt)) {
                giveDefenderDamage(((CardPanel) evt.getSource()).getCard());
            }
        }
    };

    private final MouseAdapter madOpponent = new MouseAdapter() {
        @Override
        public void mouseEntered(final MouseEvent evt) {
            ((CardPanel) evt.getSource()).setBorder(new LineBorder(FSkin.getColor(FSkin.Colors.CLR_ACTIVE), 2));
        }

        @Override
        public void mouseExited(final MouseEvent evt) {
            ((CardPanel) evt.getSource()).setBorder(null);
        }

        @Override
        public void mousePressed(final MouseEvent evt) {
            if (SwingUtilities.isLeftMouseButton(evt)) {
                takeOpponentDamage();
            }
            else if (SwingUtilities.isRightMouseButton(evt)) {
                giveOpponentDamage();
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
     */
    public VAssignDamage(final Card attacker0, final List<Card> defenders0, final int damage0) {
        // Set damage storage vars
        this.totalDamageToAssign = damage0;
        this.deathtouch = attacker0.hasKeyword("Deathtouch");
        this.trample = attacker0.hasKeyword("Trample");
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
        pnlInfo.add(new FLabel.Builder().text("Left click: Take 1 damage from first defender.").build(), "gap 0 0 0 5px");
        pnlInfo.add(new FLabel.Builder().text("Right click: Give 1 damage to first defender.").build(), "gap 0 0 0 5px");

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

            // If deathtouch, everyone is dead, damage cannot be spread further
            if (!this.deathtouch) { cp.addMouseListener(madDefender); }

        }

        // Add "opponent placeholder" card if trample allowed
        // If trample allowed, make card placeholder
        if (trample) {
            final Card crdOpponentPlaceholder = new Card();
            crdOpponentPlaceholder.setName("Opponent");
            final CardPanel cp = new CardPanel(crdOpponentPlaceholder);
            cp.setCardBounds(0, 0, 105, 150);
            cp.addMouseListener(madOpponent);
            cp.setOpaque(true);
            pnlDefenders.add(cp, "w 145px!, h 170px!, gap 5px 5px 3px 3px, ax center");
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

        autoAssignDamage();
        update();
        SOverlayUtils.showOverlay();

        this.dlg.setUndecorated(true);
        this.dlg.setContentPane(pnlMain);
        this.dlg.setSize(new Dimension(wDlg, hDlg));
        this.dlg.setLocation((overlay.getWidth() - wDlg) / 2, (overlay.getHeight() - hDlg) / 2);
        this.dlg.setModalityType(ModalityType.APPLICATION_MODAL);
        this.dlg.setVisible(true);
    }

    /** Goes through defenders assigning lethal damage until exhausted,
     * then overflows extra onto opponent or first defender card. */
    private void autoAssignDamage() {
        int remainingDamage = this.totalDamageToAssign;

        // If deathtouch, kill everyone and overflow extra onto player.
        if (this.deathtouch) {
            // Assign lethal damage to everyone. Each "touch" costs one damage.
            for (int i = 0; i < lstDefenders.size(); i++) {
                remainingDamage -= 1;
                lstDamage.set(i, lstDefenders.get(i).getLethalDamage());
            }

            // Overflow any extra onto opponent.
            if (this.trample && remainingDamage > 0) {
                damageToOpponent = remainingDamage;
            }
        }
        else {
            // Assign regular damage to everyone.
            for (int i = 0; i < lstDefenders.size(); i++) {
                // If not enough to be lethal, assign all and break.
                if (lstDefenders.get(i).getLethalDamage() > remainingDamage) {
                    setDamage(remainingDamage, lstDefenders.get(i));
                    remainingDamage = 0;
                    break;
                }

                // Otherwise, continue assigning lethal amounts of damage.
                lstDamage.set(i, lstDefenders.get(i).getLethalDamage());
                remainingDamage -= lstDefenders.get(i).getLethalDamage();
            }

            // Overflow extra onto opponent in trample case...
            if (this.trample && remainingDamage > 0) {
                damageToOpponent = remainingDamage;
            }
            // ... or just dump onto first in list.
            else if (remainingDamage > 0) {
                lstDamage.set(0, lstDamage.get(0) + remainingDamage);
            }
        }

        update();
    }

    /** Sets (overwrites) damage value for defender card (not opponent!). */
    private void setDamage(final int damage0, final Card c0) {
        lstDamage.set(lstDefenders.indexOf(c0), damage0);
    }

    /** Increments damage value for defender card (not opponent!). */
    private void incDamage(final Card c0) {
        final int temp = lstDamage.get(lstDefenders.indexOf(c0));
        setDamage(temp + 1, c0);
    }

    /** Decrements damage value for defender card (not opponent!). */
    private void decDamage(final Card c0) {
        final int temp = lstDamage.get(lstDefenders.indexOf(c0));
        if (temp > 0) {
            setDamage(temp - 1, c0);
        }
    }

    /** Right-click event for defender card (not opponent!). */
    private void giveDefenderDamage(final Card c0) {
        // Clicking is completely disabled for first defender; it acts as a pivot for damage points.
        if (c0.equals(lstDefenders.get(0))) { return; }
        // If clicked defender has 0 damage, no need to continue.
        if (lstDamage.get(lstDefenders.indexOf(c0)) < 1) { return; }

        incDamage(lstDefenders.get(0));
        decDamage(c0);

        update();
    }

    /** Left-click event for defender card (not opponent!). */
    private void takeDefenderDamage(final Card c0) {
        // Clicking is completely disabled for first defender; it acts as a pivot for damage points.
        if (c0.equals(lstDefenders.get(0))) { return; }
        // If first defender has 0 damage, no need to continue.
        if (lstDamage.get(0) < 1) { return; }

        decDamage(lstDefenders.get(0));
        incDamage(c0);

        update();
    }

    /** Gives 1 damage to opponent if all defenders are dead. */
    private void giveOpponentDamage() {
        // Everyone dead?
        for (int i = 0; i < lstDefenders.size(); i++) {
            if (lstDefenders.get(i).getLethalDamage() > lstDamage.get(i)) {
                return;
            }
        }

        // First defender has extra points available, and will still be dead?
        if (lstDefenders.get(0).getLethalDamage() >= lstDamage.get(0)) {
            return;
        }

        // Everything OK, continue with damage.
        damageToOpponent += 1;
        decDamage(lstDefenders.get(0));

        update();
    }

    /** Removes one damage from opponent and adds to first defender card. */
    private void takeOpponentDamage() {
        if (damageToOpponent < 1) { return; }

        damageToOpponent -= 1;
        incDamage(lstDefenders.get(0));

        update();
    }

    private void update() {
        this.lblTotalDamage.setText("Available damage points: " + String.valueOf(totalDamageToAssign));

        int dmg;
        for (int i = 0; i < lstDefenders.size(); i++) {
            dmg = lstDamage.get(i);
            if (this.deathtouch) {
                this.lstDamageLabels.get(i).setText("1 (Dead)");
            }
            else if (lstDefenders.get(i).getLethalDamage() <= dmg) {
                this.lstDamageLabels.get(i).setText(String.valueOf(dmg) + " (Dead)");
            }
            else {
                this.lstDamageLabels.get(i).setText(String.valueOf(dmg));
            }
        }

        // If there's an opponent, update their label.
        if (lstDamageLabels.size() > lstDefenders.size()) {
            this.lstDamageLabels.get(lstDefenders.size()).setText(String.valueOf(damageToOpponent));
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
