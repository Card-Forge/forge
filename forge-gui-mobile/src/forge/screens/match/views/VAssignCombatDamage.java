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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.badlogic.gdx.utils.Align;

import forge.Forge;
import forge.Graphics;
import forge.assets.FImage;
import forge.assets.FSkinColor;
import forge.assets.FSkinColor.Colors;
import forge.assets.FSkinFont;
import forge.assets.FSkinImage;
import forge.card.CardZoom;
import forge.game.GameEntityView;
import forge.game.card.CardView;
import forge.game.card.CounterEnumType;
import forge.game.player.PlayerView;
import forge.screens.match.MatchController;
import forge.toolbox.FCardPanel;
import forge.toolbox.FContainer;
import forge.toolbox.FDialog;
import forge.toolbox.FDisplayObject;
import forge.toolbox.FEvent;
import forge.toolbox.FEvent.FEventHandler;
import forge.toolbox.FLabel;
import forge.toolbox.FOptionPane;
import forge.toolbox.FScrollPane;
import forge.util.Callback;
import forge.util.CardTranslation;
import forge.util.Localizer;
import forge.util.TextUtil;
import forge.util.Utils;
import forge.util.WaitCallback;

public class VAssignCombatDamage extends FDialog {
    private static final float CARD_GAP_X = Utils.scale(10);
    private static final float ADD_BTN_HEIGHT = Utils.AVG_FINGER_HEIGHT * 0.75f;

    private final Callback<Map<CardView, Integer>> callback;
    private final int totalDamageToAssign;

    private boolean attackerHasDeathtouch = false;
    private boolean attackerHasDivideDamage = false;
    private boolean attackerHasTrample = false;
    private boolean attackerHasInfect = false;
    private boolean overrideCombatantOrder = false;

    private final GameEntityView defender;

    private final FLabel lblTotalDamage = add(new FLabel.Builder().text(Localizer.getInstance().getMessage("lblTotalDamageText")).align(Align.center).build());
    private final FLabel lblAssignRemaining = add(new FLabel.Builder().text(Localizer.getInstance().getMessage("lblAssignRemainingText")).align(Align.center).build());

    private final AttDefCardPanel pnlAttacker;
    private final DefendersPanel pnlDefenders;

    // Indexes of defenders correspond to their indexes in the damage list and labels.
    private final List<DamageTarget> defenders = new ArrayList<>(); // NULL in this map means defender
    private final Map<CardView, DamageTarget> damage = new HashMap<>();  // NULL in this map means defender

    private boolean canAssignTo(CardView card) {
        for (DamageTarget dt : defenders) {
            if (dt.card == card) { return true; }
            if (getDamageToKill(dt.card) > dt.damage) {
                return false;
            }
        }
        throw new RuntimeException("Asking to assign damage to object which is not present in defenders list");
    }

    /** Constructor.
     * 
     * @param attacker0 {@link forge.game.card.Card}
     * @param blockers List<{@link forge.game.card.Card}>
     * @param damage0 int
     * @param defender GameEntity that's bein attacked
     * @param overrideOrder override combatant order
     */
    public VAssignCombatDamage(final CardView attacker, final List<CardView> blockers, final int damage0, final GameEntityView defender0, boolean overrideOrder, final WaitCallback<Map<CardView, Integer>> waitCallback) {
        super(Localizer.getInstance().getMessage("lbLAssignDamageDealtBy").replace("%s",CardTranslation.getTranslatedName(attacker.getName())) , 3);

        callback = waitCallback;
        totalDamageToAssign = damage0;
        defender = defender0;
        attackerHasDeathtouch = attacker.getCurrentState().hasDeathtouch();
        attackerHasDivideDamage = attacker.getCurrentState().hasDivideDamage();
        attackerHasInfect = attacker.getCurrentState().hasInfect();
        attackerHasTrample = defender != null && attacker.getCurrentState().hasTrample();
        overrideCombatantOrder = overrideOrder;

        pnlAttacker = add(new AttDefCardPanel(attacker));
        pnlDefenders = add(new DefendersPanel(blockers));

        initButton(0, Localizer.getInstance().getMessage("lblAuto"), new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                resetAssignedDamage();
                initialAssignDamage(true);
                finish();
            }
        });
        initButton(1, Localizer.getInstance().getMessage("lblOK"), new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                finish();
            }
        });
        initButton(2, Localizer.getInstance().getMessage("lblReset"), new FEventHandler() {
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
        float w = width - 2 * padding;

        float x = padding;
        float labelHeight = lblAssignRemaining.getAutoSizeBounds().height;
        float y = maxHeight - labelHeight + padding;
        lblAssignRemaining.setBounds(x, y, w, labelHeight);

        float dtOffset = ADD_BTN_HEIGHT + defenders.get(0).label.getAutoSizeBounds().height;
        float cardPanelHeight = (y - dtOffset - labelHeight - 3 * padding) / 2;
        float cardPanelWidth = cardPanelHeight / FCardPanel.ASPECT_RATIO;

        y = padding;
        pnlAttacker.setBounds(x + (w - cardPanelWidth) / 2, y, cardPanelWidth, cardPanelHeight);

        y += cardPanelHeight + padding;
        lblTotalDamage.setBounds(x, y, w, labelHeight);

        y += labelHeight + padding;
        pnlDefenders.setBounds(0, y, width, cardPanelHeight + dtOffset);

        return maxHeight;
    }

    private class DefendersPanel extends FScrollPane {
        private DefendersPanel(final List<CardView> defenderCards) {
            for (final CardView c : defenderCards) {
                addDamageTarget(c);
            }

            if (attackerHasTrample || (attackerHasDivideDamage && overrideCombatantOrder)) {
                //add damage target for target of attack that trample damage will go through to
                addDamageTarget(null);
            }
        }

        private void addDamageTarget(CardView card) {
            DamageTarget dt = add(new DamageTarget(card));
            damage.put(card, dt);
            defenders.add(dt);
        }

        @Override
        protected ScrollBounds layoutAndGetScrollBounds(float visibleWidth, float visibleHeight) {
            float cardPanelHeight = visibleHeight - ADD_BTN_HEIGHT - defenders.get(0).label.getAutoSizeBounds().height;
            float width = cardPanelHeight / FCardPanel.ASPECT_RATIO;
            float dx = width + CARD_GAP_X;

            float x = (visibleWidth - defenders.size() * dx + CARD_GAP_X) / 2;
            if (x < FOptionPane.PADDING) {
                x = FOptionPane.PADDING;
            }

            for (DamageTarget dt : defenders) {
                dt.setBounds(x, 0, width, visibleHeight);
                x += dx;
            }
            return new ScrollBounds(x - CARD_GAP_X + FOptionPane.PADDING, visibleHeight);
        }
    }

    private class DamageTarget extends FContainer {
        private final CardView card;
        private final FDisplayObject obj;
        private final FLabel label, btnSubtract, btnAdd;
        private int damage;

        public DamageTarget(CardView card0) {
            card = card0;
            if (card != null) {
                obj = add(new AttDefCardPanel(card));
            }
            else if (defender instanceof CardView) {
                obj = add(new AttDefCardPanel((CardView)defender));
            }
            else if (defender instanceof PlayerView) {
                PlayerView player = (PlayerView)defender;
                obj = add(new MiscAttDefPanel(player.getName(), MatchController.getPlayerAvatar(player)));
            }
            else {
                obj = add(new MiscAttDefPanel(defender.toString(), FSkinImage.UNKNOWN));
            }
            label = add(new FLabel.Builder().text("0").font(FSkinFont.get(18)).align(Align.center).build());
            btnSubtract = add(new FLabel.ButtonBuilder().icon(FSkinImage.MINUS).command(new FEventHandler() {
                @Override
                public void handleEvent(FEvent e) {
                    assignDamageTo(card, false);
                }
            }).build());
            btnAdd = add(new FLabel.ButtonBuilder().icon(Forge.hdbuttons ? FSkinImage.HDPLUS : FSkinImage.PLUS).command(new FEventHandler() {
                @Override
                public void handleEvent(FEvent e) {
                    assignDamageTo(card, true);
                }
            }).build());
        }

        @Override
        protected void doLayout(float width, float height) {
            float y = 0;
            obj.setBounds(0, y, width, FCardPanel.ASPECT_RATIO * width);
            y += obj.getHeight();

            label.setBounds(0, y, width, label.getAutoSizeBounds().height);
            y += label.getHeight();

            float buttonSize = (width - FOptionPane.PADDING) / 2;
            btnSubtract.setBounds(0, y, buttonSize, ADD_BTN_HEIGHT);
            btnAdd.setBounds(width - buttonSize, y, buttonSize, ADD_BTN_HEIGHT);
        }
    }

    private static class AttDefCardPanel extends FCardPanel {
        private AttDefCardPanel(CardView card) {
            super(card);
        }

        @Override
        public boolean tap(float x, float y, int count) {
            CardZoom.show(getCard());
            return true;
        }

        @Override
        public boolean longPress(float x, float y) {
            CardZoom.show(getCard());
            return true;
        }

        @Override
        protected float getPadding() {
            return 0;
        }
    }

    private static class MiscAttDefPanel extends FDisplayObject {
        private static final FSkinFont FONT = FSkinFont.get(18);
        private static final FSkinColor FORE_COLOR = FSkinColor.get(Colors.CLR_TEXT);
        private final String name;
        private final FImage image;

        private MiscAttDefPanel(String name0, FImage image0) {
            name = name0;
            image = image0;
        }

        @Override
        public void draw(Graphics g) {
            float w = getWidth();
            float h = getHeight();
            g.drawImage(image, 0, 0, w, w);
            g.drawText(name, FONT, FORE_COLOR, 0, w, w, h - w, false, Align.center, true);
        }
    }

    private void assignDamageTo(CardView source, boolean isAdding) {
        if (!damage.containsKey(source)) {
            source = null;
        }

        // If trying to assign to the defender, follow the normal assignment rules
        // No need to check for "active" creature assignee when overiding combatant order
        if (!attackerHasDivideDamage) { // Creatures with this can assign to defender
            if ((source == null || source == defender || !overrideCombatantOrder) && isAdding && !canAssignTo(source)) {
                return;
            }
        }

        // If lethal damage has already been assigned just act like it's 0.
        int lethalDamage = getDamageToKill(source);
        int damageItHad = damage.get(source).damage;

        int damageToAdd = isAdding ? 1 : -1;

        int leftToAssign = getRemainingDamage();

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
        if (overrideCombatantOrder && attackerHasDivideDamage) {
            return;
        }
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
    
    private void addDamage(final CardView card, int addedDamage) {
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
                sb.append(" (" + Localizer.getInstance().getMessage("lblLethal"));
                if(overkill > 0) 
                    sb.append(" +").append(overkill);
                sb.append(")");
            }
            allHaveLethal &= dmg >= lethal;
            dt.label.setText(sb.toString());
        }

        lblTotalDamage.setText(TextUtil.concatNoSpace(Localizer.getInstance().getMessage("lblAvailableDamagePoints") + ": ",
                String.valueOf(damageLeft), " (of ", String.valueOf(totalDamageToAssign), ")"));
        setButtonEnabled(1, damageLeft == 0);
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

    private int getDamageToKill(CardView source) {
        int lethalDamage = 0;
        if (source == null) {
            if (defender instanceof PlayerView) {
                PlayerView p = (PlayerView)defender;
                lethalDamage = attackerHasInfect ? MatchController.instance.getGameView().getPoisonCountersToLose() - p.getCounters(CounterEnumType.POISON) : p.getLife();
            }
            else if (defender instanceof CardView) { // planeswalker
                CardView pw = (CardView)defender;
                lethalDamage = Integer.valueOf(pw.getCurrentState().getLoyalty());
            }
        }
        else {
            lethalDamage = Math.max(0, source.getLethalDamage());
            if (source.getCurrentState().getType().isPlaneswalker()) {
                lethalDamage = Integer.valueOf(source.getCurrentState().getLoyalty());
            } else if (attackerHasDeathtouch) {
                lethalDamage = Math.min(lethalDamage, 1);
            }
        }
        return lethalDamage;
    }

    public Map<CardView, Integer> getDamageMap() {
        Map<CardView, Integer> result = new HashMap<>();
        for (DamageTarget dt : defenders) {
            result.put(dt.card, dt.damage);
        }
        return result;
    }
}
