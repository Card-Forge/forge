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
package forge.screens.match.views;

import forge.Forge.KeyInputAdapter;
import forge.assets.FSkinBorder;
import forge.assets.FSkinColor;
import forge.assets.FSkinColor.Colors;
import forge.game.GameEntity;
import forge.game.card.Card;
import forge.game.card.CounterType;
import forge.game.player.Player;
import forge.toolbox.FButton;
import forge.toolbox.FCardPanel;
import forge.toolbox.FDialog;
import forge.toolbox.FEvent;
import forge.toolbox.FEvent.FEventHandler;
import forge.toolbox.FLabel;
import forge.toolbox.FList;
import forge.toolbox.FOptionPane;
import forge.toolbox.FScrollPane;
import forge.util.Callback;
import forge.util.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;


public class VAssignDamage extends FDialog {
    private final Callback<Map<Card, Integer>> callback;
    private final int totalDamageToAssign;

    private boolean attackerHasDeathtouch = false;
    private boolean attackerHasTrample = false;
    private boolean attackerHasInfect = false;
    private boolean overrideCombatantOrder = false;

    private final GameEntity defender;

    private final FLabel lblTotalDamage = add(new FLabel.Builder().text("Available damage points: Unknown").build());
    private final FLabel lblAssignRemaining = add(new FLabel.Builder().text("Distribute the remaining damage points among lethally wounded entities").build());

    private final FButton btnOK    = add(new FButton("OK"));
    private final FButton btnReset = add(new FButton("Reset"));
    private final FButton btnAuto  = add(new FButton("Auto"));

    private final AttackersPanel pnlAttackers;
    private final DefendersPanel pnlDefenders;

    private static class DamageTarget {
        public final Card card;
        public final FLabel label;
        public int damage;

        public DamageTarget(Card entity0, FLabel lbl) {
            card = entity0;
            label = lbl;
        }
    }

    // Indexes of defenders correspond to their indexes in the damage list and labels.
    private final List<DamageTarget> defenders = new ArrayList<DamageTarget>(); // NULL in this map means defender
    private final Map<Card, DamageTarget> damage = new HashMap<Card, DamageTarget>();  // NULL in this map means defender

    private boolean canAssignTo(Card card) {
        for (DamageTarget dt : defenders) {
            if (dt.card == card) return true;
            if (getDamageToKill(dt.card) > dt.damage) {
                return false;
            }
        }
        throw new RuntimeException("Asking to assign damage to object which is not present in defenders list");
    }

    /** Constructor.
     * 
     * @param attacker0 {@link forge.game.card.Card}
     * @param defenderCards List<{@link forge.game.card.Card}>
     * @param damage0 int
     * @param defender GameEntity that's bein attacked
     * @param overrideOrder override combatant order
     */
    public VAssignDamage(final Card attacker0, final List<Card> defenderCards, final int damage0, final GameEntity defender0, boolean overrideOrder, final Callback<Map<Card, Integer>> callback0) {
        super("Assign Damage");

        callback = callback0;
        totalDamageToAssign = damage0;
        defender = defender0;
        attackerHasDeathtouch = attacker0.hasKeyword("Deathtouch");
        attackerHasInfect = attacker0.hasKeyword("Infect");
        attackerHasTrample = defender != null && attacker0.hasKeyword("Trample");
        overrideCombatantOrder = overrideOrder;

        pnlAttackers = add(new AttackersPanel());
        pnlDefenders = add(new DefendersPanel(defenderCards));

        btnAuto.setCommand(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                resetAssignedDamage();
                initialAssignDamage(true);
                finish();
            }
        });
        btnOK.setCommand(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                finish();
            }
        });
        btnReset.setCommand(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                resetAssignedDamage();
                initialAssignDamage(false);
            }
        });

        initialAssignDamage(false);
    }

    @Override
    protected float layoutAndGetHeight(float width, float maxHeight) {
        float padding = FOptionPane.PADDING;
        float x = padding;
        float y = padding;
        float w = width - 2 * padding;

        //layout buttons
        float buttonGap = Utils.scaleX(10);
        float buttonWidth = (w - buttonGap * 2) / 3;
        float buttonHeight = FOptionPane.BUTTON_HEIGHT;
        x = padding;
        y = maxHeight - FOptionPane.GAP_BELOW_BUTTONS - buttonHeight;
        btnAuto.setBounds(x, y, buttonWidth, buttonHeight);
        x += buttonWidth + buttonGap;
        btnOK.setBounds(x, y, buttonWidth, buttonHeight);
        x += buttonWidth + buttonGap;
        btnReset.setBounds(x, y, buttonWidth, buttonHeight);

        x = padding;
        float panelDefendersHeight = y - padding;
        pnlDefenders.setBounds(x, y - panelDefendersHeight - padding, w, panelDefendersHeight);

        return maxHeight;
    }

    private class AttackersPanel extends FScrollPane {
        @Override
        protected ScrollBounds layoutAndGetScrollBounds(float visibleWidth, float visibleHeight) {

            return new ScrollBounds(visibleWidth, visibleHeight);
        }
    }

    private class DefendersPanel extends FScrollPane {
        private DefendersPanel(final List<Card> defenderCards) {
            int cols = attackerHasTrample ? defenderCards.size() + 1 : defenderCards.size();

            // Top row of cards...
            for (final Card c : defenderCards) {
                DamageTarget dt = new DamageTarget(c, new FLabel.Builder().text("0").fontSize(18).align(HAlignment.CENTER).build());
                damage.put(c, dt);
                defenders.add(dt);
                add(new AssignDamageCardPanel(c));
            }

            if (attackerHasTrample) {
                DamageTarget dt = new DamageTarget(null, new FLabel.Builder().text("0").fontSize(18).align(HAlignment.CENTER).build());
                damage.put(null, dt);
                defenders.add(dt);
                Card fakeCard; 
                if (defender instanceof Card) {
                    fakeCard = (Card)defender;
                }
                else if (defender instanceof Player) { 
                    fakeCard = new Card(-1);
                    fakeCard.setName(defender.getName());
                    fakeCard.setOwner((Player)defender);
                    Player p = (Player)defender;
                    //fakeCard.setImageKey(FControl.avatarImages.get(p.getOriginalLobbyPlayer()));
                }
                else {
                    fakeCard = new Card(-2);
                    fakeCard.setName(defender.getName());
                }
                add(new AssignDamageCardPanel(fakeCard));
            }        

            // Add "opponent placeholder" card if trample allowed
            // If trample allowed, make card placeholder

            // ... bottom row of labels.
            for (DamageTarget dt : defenders) {
                add(dt.label);
            }
        }
        @Override
        protected ScrollBounds layoutAndGetScrollBounds(float visibleWidth, float visibleHeight) {
            float x = 0;
            float y = 0;
            float labelWidth = Utils.AVG_FINGER_WIDTH;
            float labelHeight = Utils.scaleY(30);
            float padding = FList.PADDING;
            float dx = labelWidth + padding;

            for (DamageTarget dt : defenders) {
                dt.label.setBounds(x, y, labelWidth, labelHeight);
                x += dx;
            }
            return new ScrollBounds(x, visibleHeight);
        }
    }

    private class AssignDamageCardPanel extends FCardPanel {
        private FSkinColor borderColor;

        private AssignDamageCardPanel(Card card0) {
            super(card0);
        }

        @Override
        public boolean press(float x, float y) {
            Card source = getCard();
            if (!damage.containsKey(source)) {
                source = null; // to get player instead of fake card
            }

            borderColor = canAssignTo(source) ? FSkinColor.get(Colors.CLR_ACTIVE) : FSkinColor.get(Colors.CLR_INACTIVE);
            return true;
        }

        @Override
        public boolean release(float x, float y) {
            borderColor = null;
            return true;
        }

        @Override
        public boolean tap(float x, float y, int count) {
            assignDamageTo(getCard(), KeyInputAdapter.isCtrlKeyDown(), true);
            return true;
        }

        @Override
        protected FSkinBorder getBorder() {
            if (borderColor == null) {
                return null;
            }
            return new FSkinBorder(borderColor, Utils.scaleMin(2));
        }
    }

    private void assignDamageTo(Card source, boolean meta, boolean isAdding) {
        if (!damage.containsKey(source)) {
            source = null;
        }

        // If trying to assign to the defender, follow the normal assignment rules
        // No need to check for "active" creature assignee when overiding combatant order
        if ((source == null || source == defender || !overrideCombatantOrder) && isAdding && 
                !canAssignTo(source)) {
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
        if (meta)  {
            if (isAdding) {
                damageToAdd = leftToKill > 0 ? leftToKill : leftToAssign;
            }
            else {
                damageToAdd = damageItHad > lethalDamage ? lethalDamage - damageItHad : -damageItHad;
            }
        }

        if (damageToAdd > leftToAssign) {
            damageToAdd = leftToAssign;
        }

        // cannot assign first blocker less than lethal damage except when overriding order
        boolean isFirstBlocker = defenders.get(0).card == source;
        if (!overrideCombatantOrder && isFirstBlocker && damageToAdd + damageItHad < lethalDamage) {
            return;
        }

        if (damageToAdd == 0 || damageToAdd + damageItHad < 0) {
            return;
        }
        
        addDamage(source, damageToAdd);
        checkDamageQueue();
        updateLabels();
    }

    private void checkDamageQueue() {
        // Clear out any Damage that shouldn't be assigned to other combatants
        boolean hasAliveEnemy = false;
        for (DamageTarget dt : defenders) {
            int lethal = getDamageToKill(dt.card);
            int damage = dt.damage;
            // If overriding combatant order, make sure everything has lethal if defender has damage assigned to it
            // Otherwise, follow normal combatant order
            if (hasAliveEnemy && (!overrideCombatantOrder || dt.card == null || dt.card == defender)) {
                dt.damage = 0;
            }
            else {
                hasAliveEnemy |= damage < lethal;
            }
        }
    }

    // will assign all damage to defenders and rest to player, if present
    private void initialAssignDamage(boolean toAllBlockers) {
        if (!toAllBlockers && overrideCombatantOrder) {
            // Don't auto assign the first damage when overriding combatant order
            updateLabels();
            return;
        }

        int dmgLeft = totalDamageToAssign;
        DamageTarget dtLast = null;
        for(DamageTarget dt : defenders) { // MUST NOT RUN WITH EMPTY collection
            int lethal = getDamageToKill(dt.card);
            int damage = Math.min(lethal, dmgLeft);
            addDamage(dt.card, damage);
            dmgLeft -= damage;
            dtLast = dt;
            if (dmgLeft <= 0 || !toAllBlockers) { break; }
        }
        if (dmgLeft < 0) {
            throw new RuntimeException("initialAssignDamage managed to assign more damage than it could");
        }
        if (toAllBlockers && dmgLeft > 0) { 
            // flush the remaining damage into last defender if assigning all damage
            addDamage(dtLast.card, dmgLeft);
        }
        updateLabels();
    }

    /** Reset Assign Damage back to how it was at the beginning. */
    private void resetAssignedDamage() {
        for (DamageTarget dt : defenders) {
            dt.damage = 0;
        }
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

    private int getRemainingDamage() {
        int spent = 0;
        for (DamageTarget dt : defenders) {
            spent += dt.damage;
        }
        return totalDamageToAssign - spent;
    }

    /** Updates labels and other UI elements.
     * @param index index of the last assigned damage*/
    private void updateLabels() {
        int damageLeft = totalDamageToAssign;
        boolean allHaveLethal = true;
        
        for (DamageTarget dt : defenders) {
            int dmg = dt.damage;
            damageLeft -= dmg;
            int lethal = getDamageToKill(dt.card);
            int overkill = dmg - lethal;
            StringBuilder sb = new StringBuilder();
            sb.append(dmg);
            if(overkill >= 0) { 
                sb.append(" (Lethal");
                if(overkill > 0) 
                    sb.append(" +").append(overkill);
                sb.append(")");
            }
            allHaveLethal &= dmg >= lethal;
            dt.label.setText(sb.toString());
        }

        lblTotalDamage.setText(String.format("Available damage points: %d (of %d)", damageLeft, totalDamageToAssign));
        btnOK.setEnabled(damageLeft == 0);
        lblAssignRemaining.setVisible(allHaveLethal && damageLeft > 0);
    }

    // Dumps damage onto cards. Damage must be stored first, because if it is
    // assigned dynamically, the cards die off and further damage to them can't
    // be modified.
    private void finish() {
        if (getRemainingDamage() > 0) {
            return;
        }
        hide();
        callback.run(getDamageMap());
    }

    private int getDamageToKill(Card source) {
        int lethalDamage = 0;
        if (source == null) {
            if (defender instanceof Player) {
                Player p = (Player)defender;
                lethalDamage = attackerHasInfect ? p.getGame().getRules().getPoisonCountersToLose() - p.getPoisonCounters() : p.getLife();
            }
            else if (defender instanceof Card) { // planeswalker
                Card pw = (Card)defender;
                lethalDamage = pw.getCounters(CounterType.LOYALTY);
            }
        }
        else {
            lethalDamage = attackerHasDeathtouch ? 1 : Math.max(0, source.getLethalDamage());
        }
        return lethalDamage;
    }

    public Map<Card, Integer> getDamageMap() {
        Map<Card, Integer> result = new HashMap<Card, Integer>();
        for (DamageTarget dt : defenders) {
            result.put(dt.card, dt.damage);
        }
        return result;
    }
}
