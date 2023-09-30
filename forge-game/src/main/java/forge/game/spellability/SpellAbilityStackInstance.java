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

import java.util.Map;
import java.util.Set;

import com.google.common.collect.Sets;

import forge.game.GameObject;
import forge.game.IIdentifiable;
import forge.game.ability.AbilityKey;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardView;
import forge.game.card.IHasCardView;
import forge.game.player.Player;
import forge.game.trigger.TriggerType;
import forge.game.trigger.WrappedAbility;
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

    private String stackDescription = null;

    private final StackItemView view;

    public SpellAbilityStackInstance(final SpellAbility sa) {
        // Base SA info
        id = nextId();
        ability = sa;
        stackDescription = sa.getStackDescription();
        activatingPlayer = sa.getActivatingPlayer();

        subInstance = ability.getSubAbility() == null ? null : new SpellAbilityStackInstance(ability.getSubAbility());

        final Map<String, String> sVars = (ability.isWrapper() ? ((WrappedAbility) ability).getWrappedAbility() : ability).getDirectSVars();
        if (ApiType.SetState == sa.getApi() && !sVars.containsKey("StoredTransform")) {
            // Record current state of Transformation if the ability might change state
            sVars.put("StoredTransform", String.valueOf(ability.getHostCard().getTransformedTimestamp()));
        }

        if (sa.getApi() == ApiType.Charm && sa.hasParam("ChoiceRestriction")) {
            // Remember the Choice here for later handling
            sa.getHostCard().addChosenModes(sa, sa.getSubAbility().getDescription(), sa.getHostCard().getGame().getPhaseHandler().inCombat());
        }

        view = new StackItemView(this);
    }

    @Override
    public int getId() {
        return id;
    }

    public final SpellAbility getSpellAbility() {
        return ability;
    }

    // A bit of SA shared abilities to restrict conflicts
    public final String getStackDescription() {
        return stackDescription.replaceAll("\\\\r\\\\n", "").replaceAll("\\.\u2022", ";").replaceAll("\u2022", "");
    }

    public final Card getSourceCard() {
        return ability.getHostCard();
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
        return ability.getTargets();
    }

    public void updateTarget(TargetChoices target, Card cause) {
        if (target != null) {
            TargetChoices oldTarget = ability.getTargets();
            ability.setTargets(target);
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
        if (!ability.hasTriggeringObject(trigObj)) {
            ability.setTriggeringObject(trigObj, value);
            return true;
        }
        return false;
    }
    public boolean updateTriggeringObject(AbilityKey trigObj, Object value) {
        if (ability.hasTriggeringObject(trigObj)) {
            ability.setTriggeringObject(trigObj, value);
            return true;
        }
        return false;
    }
    public Object getTriggeringObject(AbilityKey trigObj) {
        return ability.getTriggeringObject(trigObj);
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
