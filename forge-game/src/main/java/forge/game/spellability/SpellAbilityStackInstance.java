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
package forge.game.spellability;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.collect.TreeBasedTable;

import forge.game.GameObject;
import forge.game.IIdentifiable;
import forge.game.ability.AbilityKey;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardView;
import forge.game.card.IHasCardView;
import forge.game.mana.Mana;
import forge.game.player.Player;
import forge.game.trigger.TriggerType;
import forge.game.trigger.WrappedAbility;
import forge.game.zone.ZoneType;
import forge.util.TextUtil;

/**
 * <p>
 * SpellAbility_StackInstance class.
 * </p>
 *
 * @author Forge
 * @version $Id$
 */
public class SpellAbilityStackInstance implements IIdentifiable, IHasCardView {
    private static int maxId = 0;
    private static int nextId() { return ++maxId; }

    // At some point I want this functioning more like Target/Target Choices
    // where the SA has an "active"
    // Stack Instance, and instead of having duplicate parameters, it adds
    // changes directly to the "active" one
    // When hitting the Stack, the active SI gets "applied" to the Stack and
    // gets cleared from the base SI
    // Coming off the Stack would work similarly, except it would just add the
    // full active SI instead of each of the parts

    private final int id;
    private final SpellAbility ability;

    private final SpellAbilityStackInstance subInstance;
    private Player activatingPlayer;

    // When going to a SubAbility that SA has a Instance Choice object
    private TargetChoices tc = new TargetChoices();
    private CardCollection splicedCards = null;

    private String stackDescription = null;

    // Adjusted Mana Cost
    // private String adjustedManaCost = "";

    // Paid Mana Cost
    private List<Mana> payingMana;
    // private ArrayList<AbilityMana> paidAbilities = new ArrayList<AbilityMana>();
    private Integer xManaPaid = null;

    // Other Paid things
    private final TreeBasedTable<String, Boolean, CardCollection> paidHash;

    // Additional info
    // is Kicked, is Buyback

    // Triggers
    private final Map<AbilityKey, Object> triggeringObjects;
    private final List<Object> triggerRemembered;

    private final Map<String, String> storedSVars = Maps.newHashMap();

    private final Map<Player, Object> playersWithValidTargets;

    private final StackItemView view;

    public SpellAbilityStackInstance(final SpellAbility sa) {
        // Base SA info
        id = nextId();
        ability = sa;
        stackDescription = sa.getStackDescription();
        activatingPlayer = sa.getActivatingPlayer();

        // Payment info
        paidHash = TreeBasedTable.create(ability.getPaidHash());
        ability.resetPaidHash();
        splicedCards = sa.getSplicedCards();

        xManaPaid = sa.getXManaCostPaid();
        payingMana = Lists.newArrayList(sa.getPayingMana());

        // Triggering info
        triggeringObjects = sa.getTriggeringObjects();
        triggerRemembered = sa.getTriggerRemembered();

        subInstance = ability.getSubAbility() == null ? null : new SpellAbilityStackInstance(ability.getSubAbility());

        // Targeting info -- 29/06/11 Moved to after taking care of SubAbilities
        // because otherwise AF_DealDamage SubAbilities that use Defined$
        // Targeted breaks (since it's parents target is reset)
        if (sa.usesTargeting()) {
            tc = ability.getTargets();
            ability.resetTargets();
        }

        final Card source = ability.getHostCard();

        // We probably should be storing SA svars too right?
        if (!sa.isWrapper()) {
            for (final Entry<String, String> e : sa.getDirectSVars().entrySet()) {
                final String value = e.getValue();
                if (!StringUtils.isEmpty(value)) {
                    storedSVars.put(e.getKey(), value);
                }
            }
        }

        if (ApiType.SetState == sa.getApi() && !storedSVars.containsKey("StoredTransform")) {
            // Record current state of Transformation if the ability might change state
            storedSVars.put("StoredTransform", String.valueOf(source.getTransformedTimestamp()));
        }

        //store zones to open and players to open them for at the time the SpellAbility first goes on the stack based on the selected targets
        if (tc == null) {
            playersWithValidTargets = null;
        } else {
            playersWithValidTargets = Maps.newHashMap();
            for (Card card : tc.getTargetCards()) {
                ZoneType zoneType = card.getZone() != null ? card.getZone().getZoneType() : null;
                if (zoneType != ZoneType.Battlefield) { //don't need to worry about targets on battlefield
                    playersWithValidTargets.put(card.getController(), null);
                }
            }
        }

        view = new StackItemView(this);
    }

    @Override
    public int getId() {
        return id;
    }

    //TODO: See if refresh actually needed for most places this is being called
    //      Perhaps lets move the refresh logic to a separate function called only when necessary
    public final SpellAbility getSpellAbility(boolean refresh) {
        if (refresh) {
            ability.setTargets(tc);
            ability.setActivatingPlayer(activatingPlayer);

            // Saved sub-SA needs to be reset on the way out
            if (subInstance != null) {
                ability.setSubAbility((AbilitySub) subInstance.getSpellAbility(true));
            }

            // Set Cost specific things here
            ability.setPaidHash(paidHash);
            ability.setSplicedCards(splicedCards);
            ability.setXManaCostPaid(xManaPaid);
            ability.setPayingMana(payingMana);

            // Triggered
            ability.setTriggeringObjects(triggeringObjects);
            ability.setTriggerRemembered(triggerRemembered);

            // Add SVars back in
            final SpellAbility sa = ability.isWrapper() ? ((WrappedAbility) ability).getWrappedAbility() : ability;
            for (final String store : storedSVars.keySet()) {
                final String value = storedSVars.get(store);

                if (!StringUtils.isEmpty(value)) {
                    sa.setSVar(store, value);
                }
            }
        }
        return ability;
    }

    // A bit of SA shared abilities to restrict conflicts
    public final String getStackDescription() {
        return stackDescription.replaceAll("\\\\r\\\\n", "").replaceAll("\\.\u2022", ";").replaceAll("\u2022", "");
    }

    public final Card getSourceCard() {
        return ability.getHostCard();
    }
    
    public final int getXManaPaid() {
    	return xManaPaid == null ? 0 : xManaPaid;
    }
    public final void setXManaPaid(int x) {
        xManaPaid = x;
    }

    public final boolean isSpell() {
        return ability.isSpell();
    }

    public final boolean isAbility() {
        return ability.isAbility();
    }

    public final boolean isTrigger() {
        return ability.isTrigger();
    }

    public final boolean isStateTrigger(final int id) {
        return ability.getSourceTrigger() == id;
    }

    public final boolean isOptionalTrigger() {
        return ability.isOptionalTrigger();
    }

    public final SpellAbilityStackInstance getSubInstance() {
        return subInstance;
    }

    public final TargetChoices getTargetChoices() {
        return tc;
    }

    public final Map<Player, Object> getPlayersWithValidTargets() {
        return playersWithValidTargets;
    }

    public void updateTarget(TargetChoices target, Card cause) {
        if (target != null) {
            TargetChoices oldTarget = tc;
            tc = target;
            ability.setTargets(tc);
            stackDescription = ability.getStackDescription();
            view.updateTargetCards(this);
            view.updateTargetPlayers(this);
            view.updateText(this);
            
            // Run BecomesTargetTrigger
            Map<AbilityKey, Object> runParams = AbilityKey.newMap();
            runParams.put(AbilityKey.SourceSA, ability);
            Set<GameObject> distinctObjects = Sets.newHashSet();
            for (final GameObject tgt : target) {
                if (oldTarget != null && oldTarget.contains(tgt)) {
                    // it was an old target, so don't trigger becomes target
                    continue;
                }
                if (!distinctObjects.add(tgt)) {
                    continue;
                }

                if (tgt instanceof Card && !((Card) tgt).hasBecomeTargetThisTurn()) {
                    runParams.put(AbilityKey.FirstTime, null);
                    ((Card) tgt).setBecameTargetThisTurn(true);
                }
                runParams.put(AbilityKey.Target, tgt);
                getSourceCard().getGame().getTriggerHandler().runTrigger(TriggerType.BecomesTarget, runParams, false);
            }
            // Only run BecomesTargetOnce when at least one target is changed
            if (!distinctObjects.isEmpty()) {
                runParams = AbilityKey.newMap();
                runParams.put(AbilityKey.SourceSA, ability);
                runParams.put(AbilityKey.Targets, distinctObjects);
                runParams.put(AbilityKey.Cause, cause);
                getSourceCard().getGame().getTriggerHandler().runTrigger(TriggerType.BecomesTargetOnce, runParams, false);
            }
        }
    }

    public boolean addTriggeringObject(AbilityKey trigObj, Object value) {
        if (!triggeringObjects.containsKey(trigObj)) {
            triggeringObjects.put(trigObj, value);
            return true;
        }
        return false;
    }

    public boolean updateTriggeringObject(AbilityKey trigObj, Object value) {
        if (triggeringObjects.containsKey(trigObj)) {
            triggeringObjects.replace(trigObj, value);
            return true;
        }
        return false;
    }

    public Object getTriggeringObject(AbilityKey trigObj) {
        if (triggeringObjects.containsKey(trigObj)) {
            return triggeringObjects.get(trigObj);
        }
        return null;
    }

    public Player getActivatingPlayer() {
        return activatingPlayer;
    }
    public void setActivatingPlayer(Player activatingPlayer0) {
        if (activatingPlayer == activatingPlayer0) { return; }
        activatingPlayer = activatingPlayer0;
        view.updateActivatingPlayer(this);
        if (subInstance != null) {
            subInstance.setActivatingPlayer(activatingPlayer0);
        }
    }

    public List<Mana> getPayingMana() {
        return payingMana;
    }

    @Override
    public String toString() {
        return TextUtil.concatNoSpace(getSourceCard().toString(), "->", getStackDescription());
    }

    public StackItemView getView() {
        return view;
    }

    @Override
    public CardView getCardView() {
        return CardView.get(getSourceCard());
    }
}
