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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.LineBorder;

import net.miginfocom.swing.MigLayout;
import forge.Card;
import forge.CounterType;
import forge.GameEntity;
import forge.Singletons;
import forge.game.MatchController;
import forge.game.player.Player;
import forge.gui.SOverlayUtils;
import forge.gui.toolbox.FButton;
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
public class VAssignDamage {
    // Width and height of blocker dialog
    private final int wDlg = 700;
    private final int hDlg = 500;
    private final JDialog dlg = new JDialog();

    // Damage storage
    private final int totalDamageToAssign;

    private final Card attacker;
    private boolean attackerHasDeathtouch = false;
    private boolean attackerHasTrample = false;
    private boolean attackerHasInfect = false;

    private final GameEntity defender;

    private final JLabel lblTotalDamage = new FLabel.Builder().text("Available damage points: Unknown").build();

    //  Label Buttons
    private final FButton btnOK    = new FButton("OK");
    private final FButton btnReset = new FButton("Reset");
    private final FButton btnAuto  = new FButton("Auto");

    
    private static class DamageTarget {
        public final Card card;
        public final JLabel label;
        public int damage;

        public DamageTarget(Card entity0, JLabel lbl) {
            card = entity0;
            label = lbl;
        }
    }

    // Indexes of defenders correspond to their indexes in the damage list and labels.
    private final List<DamageTarget> defenders = new ArrayList<DamageTarget>(); // NULL in this map means defender
    private final Map<Card, DamageTarget> damage = new HashMap<Card, DamageTarget>();  // NULL in this map means defender

    private boolean canAssignTo(Card card) {
        for(DamageTarget dt : defenders) {
            if ( dt.card == card ) return true;
            if ( getDamageToKill(dt.card) > dt.damage )
                return false;
        }
        throw new RuntimeException("Asking to assign damage to object which is not present in defenders list");
    }

    // Mouse actions
    private final MouseAdapter madDefender = new MouseAdapter() {
        @Override
        public void mouseEntered(final MouseEvent evt) {
            Card source = ((CardPanel) evt.getSource()).getCard();
            if (!damage.containsKey(source))
                source = null;
            
            FSkin.Colors brdrColor = VAssignDamage.this.canAssignTo(source) ? FSkin.Colors.CLR_ACTIVE : FSkin.Colors.CLR_INACTIVE;
            ((CardPanel) evt.getSource()).setBorder(new LineBorder(FSkin.getColor(brdrColor), 2));
        }

        @Override
        public void mouseExited(final MouseEvent evt) {
            ((CardPanel) evt.getSource()).setBorder(null);
        }

        @Override
        public void mousePressed(final MouseEvent evt) {
            Card source = ((CardPanel) evt.getSource()).getCard(); // will be NULL for player

            boolean meta = evt.isControlDown();
            boolean isLMB = SwingUtilities.isLeftMouseButton(evt);
            boolean isRMB = SwingUtilities.isRightMouseButton(evt);
            
            if ( isLMB || isRMB)
                assignDamageTo(source, meta, isLMB);
        }
    };

    /** Constructor.
     * 
     * @param attacker0 {@link forge.Card}
     * @param defenders0 List<{@link forge.Card}>
     * @param damage0 int
     * @param defender GameEntity that's bein attacked
     */
    public VAssignDamage(final Card attacker0, final List<Card> defenderCards, final int damage0, final GameEntity defender) {
        // Set damage storage vars
        this.totalDamageToAssign = damage0;
        this.defender = defender;
        this.attackerHasDeathtouch = attacker0.hasKeyword("Deathtouch");
        this.attackerHasInfect = attacker0.hasKeyword("Infect");
        this.attackerHasTrample = defender != null && attacker0.hasKeyword("Trample");
        this.attacker = attacker0;

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
        pnlInfo.add(new FLabel.Builder().text("Left click: Assign 1 damage. (Left Click + Control): Assign remaining damage up to lethal").build(), "gap 0 0 0 5px");
        pnlInfo.add(new FLabel.Builder().text("Right click: Unassign 1 damage. (Right Click + Control): Unassign all damage.").build(), "gap 0 0 0 5px");

        // Defenders area
        final JPanel pnlDefenders = new JPanel();
        pnlDefenders.setOpaque(false);
        final String wrap = "wrap " + (attackerHasTrample ? defenderCards.size() + 1 : defenderCards.size());
        pnlDefenders.setLayout(new MigLayout("insets 0, gap 0, ax center, " + wrap));

        final FScrollPane scrDefenders = new FScrollPane(pnlDefenders);
        scrDefenders.setBorder(null);

        // Top row of cards...
        for (final Card c : defenderCards) {
            DamageTarget dt = new DamageTarget(c, new FLabel.Builder().text("0").fontSize(18).fontAlign(SwingConstants.CENTER).build());
            this.damage.put(c, dt);
            this.defenders.add(dt);
            addPanelForDefender(pnlDefenders, c);
        }

        if (attackerHasTrample) {
            DamageTarget dt = new DamageTarget(null, new FLabel.Builder().text("0").fontSize(18).fontAlign(SwingConstants.CENTER).build());
            this.damage.put(null, dt);
            this.defenders.add(dt);
            Card fakeCard; 
            if( defender instanceof Card ) 
                fakeCard = (Card)defender;
            else { 
                fakeCard = new Card();
                fakeCard.setName(this.defender.getName());
            }            
            addPanelForDefender(pnlDefenders, fakeCard);
        }        

        // Add "opponent placeholder" card if trample allowed
        // If trample allowed, make card placeholder

        // ... bottom row of labels.
        for (DamageTarget l : defenders) {
            pnlDefenders.add(l.label, "w 145px!, h 30px!, gap 5px 5px 0 5px");
        }

        btnOK.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent arg0) { finish(); } });
        btnReset.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent arg0) { resetAssignDamage(); } });
        btnAuto.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent arg0) { resetAssignDamage(); finish(); } });

        // Final UI layout
        pnlMain.setLayout(new MigLayout("insets 0, gap 0, wrap 2, ax center"));
        pnlMain.add(pnlAttacker, "w 125px!, h 160px!, gap 50px 0 0 15px");
        pnlMain.add(pnlInfo, "gap 20px 0 0 15px");
        pnlMain.add(scrDefenders, "w 96%!, gap 2% 0 0 0, pushy, growy, ax center, span 2");

        JPanel pnlButtons = new JPanel(new MigLayout("insets 0, gap 0, ax center"));
        pnlButtons.setOpaque(false);
        pnlButtons.add(btnAuto, "w 110px!, h 30px!, gap 0 10px 0 0");
        pnlButtons.add(btnOK, "w 110px!, h 30px!, gap 0 10px 0 0");
        pnlButtons.add(btnReset, "w 110px!, h 30px!");

        pnlMain.add(pnlButtons, "ax center, w 350px!, gap 10px 10px 10px 10px, span 2");
        overlay.add(pnlMain);
        
        pnlMain.getRootPane().setDefaultButton(btnOK);
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                btnAuto.requestFocusInWindow();
            }
        });

        initialAssignDamage();
        SOverlayUtils.showOverlay();

        this.dlg.setUndecorated(true);
        this.dlg.setContentPane(pnlMain);
        this.dlg.setSize(new Dimension(wDlg, hDlg));
        this.dlg.setLocation((overlay.getWidth() - wDlg) / 2, (overlay.getHeight() - hDlg) / 2);
        this.dlg.setModalityType(ModalityType.APPLICATION_MODAL);
        this.dlg.setVisible(true);
    }

    /**
     * TODO: Write javadoc for this method.
     * @param pnlDefenders
     * @param defender
     */
    private void addPanelForDefender(final JPanel pnlDefenders, final Card defender) {
        final CardPanel cp = new CardPanel(defender);
        cp.setCardBounds(0, 0, 105, 150);
        cp.setOpaque(true);
        pnlDefenders.add(cp, "w 145px!, h 170px!, gap 5px 5px 3px 3px, ax center");
        cp.addMouseListener(madDefender);
    }

    /**
     * TODO: Write javadoc for this method.
     * @param source
     * @param meta
     * @param isLMB
     */
    private void assignDamageTo(Card source, boolean meta, boolean isAdding) {
        if ( !damage.containsKey(source) ) 
            source = null;

        // Allow click if this is active, or next to active and active has lethal
        if (isAdding && !VAssignDamage.this.canAssignTo(source)) {
            return;
        }

        // If lethal damage has already been assigned just act like it's 0.
        int lethalDamage = getDamageToKill(source);
        int damageItHad = damage.get(source).damage;
        int leftToKill = Math.max(0, lethalDamage - damageItHad);
    
        int damageToAdd = isAdding ? 1 : -1;
    
        int leftToAssign = getRemainingDamage();
        // Left click adds damage, right click substracts damage.
        // Hold Ctrl to assign lethal damage, Ctrl-click again on a creature with lethal damage to assign all available damage to it
        if ( meta )  {
            if (isAdding) {
                damageToAdd = leftToKill > 0 ? leftToKill : leftToAssign;
            } else {
                damageToAdd = damageItHad > lethalDamage ? lethalDamage - damageItHad : -damageItHad;
            }
        }
        
        if ( damageToAdd > leftToAssign )
            damageToAdd = leftToAssign;
        
        if ( 0 == damageToAdd || damageToAdd + damageItHad < 0) 
            return;
        
        addDamage(source, damageToAdd);
        checkDamageQueue();
        updateLabels();
    }

    /**
     * TODO: Write javadoc for this method.
     */
    private void checkDamageQueue() {
        boolean hasAliveEnemy = false;
        for(DamageTarget dt : defenders) {
            int lethal = getDamageToKill(dt.card);
            int damage = dt.damage;
            if ( hasAliveEnemy )
                dt.damage = 0;
            else
                hasAliveEnemy = damage < lethal;
        }
    }

    // will assign all damage to defenders and rest to player, if present
    private void initialAssignDamage() {
        int dmgLeft = totalDamageToAssign;
        for(DamageTarget dt : defenders) {
            int lethal = getDamageToKill(dt.card);
            int damage = Math.min(lethal, dmgLeft);
            addDamage(dt.card, damage);
            dmgLeft -= damage;
            if ( dmgLeft <= 0 ) break;
        }
        if ( dmgLeft < 0 )
            throw new RuntimeException("initialAssignDamage managed to assign more damage than it could");
        if ( dmgLeft > 0 ) { // flush the remaining damage into last defender
            DamageTarget dt = defenders.get(defenders.size()-1);
            addDamage(dt.card, dmgLeft );
        }
        updateLabels();
    }

    /** Reset Assign Damage back to how it was at the beginning. */
    private void resetAssignDamage() {
        for(DamageTarget dt : defenders)
            dt.damage = 0;

        initialAssignDamage();
    }

    private void addDamage(final Card card, int addedDamage) {
        // If we don't have enough left or we're trying to unassign too much return
        int canAssign = getRemainingDamage();
        if (canAssign < addedDamage) {
            addedDamage = canAssign;
        }

        DamageTarget dt = damage.get(card);
        dt.damage = Math.max(0, addedDamage + dt.damage); 
    }


    /**
     * TODO: Write javadoc for this method.
     * @return
     */
    private int getRemainingDamage() {
        int spent = 0;
        for(DamageTarget dt : defenders) {
            spent += dt.damage;
        }
        return totalDamageToAssign - spent;
    }

    /** Updates labels and other UI elements.
     * @param index index of the last assigned damage*/
    private void updateLabels() {

        int damageLeft = totalDamageToAssign;
        for ( DamageTarget dt : defenders )
        {
            int dmg = dt.damage;
            damageLeft -= dmg;
            int lethal = getDamageToKill(dt.card);
            String text = dmg >= lethal ? Integer.toString(dmg) + " (Lethal)" : Integer.toString(dmg);
            dt.label.setText(text);
        }

        this.lblTotalDamage.setText(String.format("Available damage points: %d (of %d)", damageLeft, this.totalDamageToAssign));
        btnOK.setEnabled(damageLeft == 0);

    }

    // Dumps damage onto cards. Damage must be stored first, because if it is
    // assigned dynamically, the cards die off and further damage to them can't
    // be modified.
    private void finish() {
        if ( getRemainingDamage() > 0 ) 
            return;
        
        for (DamageTarget dt : defenders) {
            if( dt.card == null && attackerHasTrample ) {
                Singletons.getModel().getGame().getCombat().addDefendingDamage(dt.damage, this.attacker);
                continue;
            }
            dt.card.addAssignedDamage(dt.damage, this.attacker);
            dt.card.updateObservers();
        }

        dlg.dispose();
        SOverlayUtils.hideOverlay();
    }
    
    /**
     * TODO: Write javadoc for this method.
     * @param source
     * @return
     */
    private int getDamageToKill(Card source) {
        int lethalDamage = 0;
        if ( source == null ) {
            if ( defender instanceof Player ) {
                Player p = (Player)defender;
                lethalDamage = attackerHasInfect ? MatchController.getPoisonCountersAmountToLose() - p.getPoisonCounters() : p.getLife();
            } else if ( defender instanceof Card ) { // planeswalker
                Card pw = (Card)defender;
                lethalDamage = pw.getCounters(CounterType.LOYALTY);
            }
        } else {
            lethalDamage = VAssignDamage.this.attackerHasDeathtouch ? 1 : Math.max(0, source.getLethalDamage());
        }
        return lethalDamage;
    }

    public Map<Card, Integer> getDamageMap() {
        Map<Card, Integer> result = new HashMap<Card, Integer>();
        for(DamageTarget dt : defenders)
            result.put(dt.card, dt.damage);
        return result;
    }
}
