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
package forge.game.trigger;

import java.util.List;

import forge.game.Game;
import forge.game.GameObject;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.card.CardUtil;
import forge.game.cost.Cost;
import forge.game.player.Player;
import forge.game.spellability.OptionalCost;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbilityStackInstance;
import forge.game.spellability.TargetChoices;
import forge.game.zone.ZoneType;
import forge.util.Expressions;
import java.util.HashSet;
import java.util.Set;

/**
 * <p>
 * Trigger_SpellAbilityCast class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class TriggerSpellAbilityCast extends Trigger {

    /**
     * <p>
     * Constructor for Trigger_SpellAbilityCast.
     * </p>
     * 
     * @param params
     *            a {@link java.util.HashMap} object.
     * @param host
     *            a {@link forge.game.card.Card} object.
     * @param intrinsic
     *            the intrinsic
     */
    public TriggerSpellAbilityCast(final java.util.Map<String, String> params, final Card host, final boolean intrinsic) {
        super(params, host, intrinsic);
    }

    /** {@inheritDoc} */
    @Override
    public final boolean performTest(final java.util.Map<String, Object> runParams2) {
        final SpellAbility spellAbility = (SpellAbility) runParams2.get("CastSA");
        if (spellAbility == null) {
            System.out.println("TriggerSpellAbilityCast performTest encountered spellAbility == null. runParams2 = " + runParams2);
            return false;
        }
        final Card cast = spellAbility.getHostCard();
        final Game game = cast.getGame();
        final SpellAbilityStackInstance si = game.getStack().getInstanceFromSpellAbility(spellAbility);

        if (this.getMode() == TriggerType.SpellCast) {
            if (!spellAbility.isSpell()) {
                return false;
            }
        } else if (this.getMode() == TriggerType.AbilityCast) {
            if (!spellAbility.isAbility()) {
                return false;
            }
        } else if (this.getMode() == TriggerType.SpellAbilityCast) {
            // Empty block for readability.
        }

        if (this.mapParams.containsKey("ActivatedOnly")) {
            if (spellAbility.isTrigger()) {
                return false;
            }
        }

        if (this.mapParams.containsKey("ValidControllingPlayer")) {
            if (!matchesValid(cast.getController(), this.mapParams.get("ValidControllingPlayer").split(","),
                    this.getHostCard())) {
                return false;
            }
        }

        if (this.mapParams.containsKey("ValidActivatingPlayer")) {
            if (si == null || !matchesValid(si.getSpellAbility(true).getActivatingPlayer(), this.mapParams.get("ValidActivatingPlayer")
                    .split(","), this.getHostCard())) {
                return false;
            }
            if (this.mapParams.containsKey("ActivatorThisTurnCast")) {
                String compare = this.mapParams.get("ActivatorThisTurnCast");
                List<Card> thisTurnCast = CardUtil.getThisTurnCast(this.mapParams.containsKey("ValidCard") ? this.mapParams.get("ValidCard") : "Card",
                        this.getHostCard());
                thisTurnCast = CardLists.filterControlledBy(thisTurnCast, si.getSpellAbility(true).getActivatingPlayer());
                int left = thisTurnCast.size();
                int right = Integer.parseInt(compare.substring(2));
                if (!Expressions.compare(left, compare, right)) {
                    return false;
                }
            }
        }

        if (this.mapParams.containsKey("ValidCard")) {
            if (!matchesValid(cast, this.mapParams.get("ValidCard").split(","), this.getHostCard())) {
                return false;
            }
        }

        if (this.mapParams.containsKey("TargetsValid")) {
            SpellAbility sa = spellAbility;
            if (si != null) {
                sa = si.getSpellAbility(true);
            }
           
            boolean validTgtFound = false;
            while (sa != null && !validTgtFound) {
                for (final Card tgt : sa.getTargets().getTargetCards()) {
                    if (tgt.isValid(this.mapParams.get("TargetsValid").split(","), this.getHostCard()
                            .getController(), this.getHostCard(), null)) {
                        validTgtFound = true;
                        break;
                    }
                }

                for (final Player p : sa.getTargets().getTargetPlayers()) {
                    if (matchesValid(p, this.mapParams.get("TargetsValid").split(","), this.getHostCard())) {
                        validTgtFound = true;
                        break;
                    }
                }
                sa = sa.getSubAbility();
            }
            if (!validTgtFound) {
                 return false;
            }
        }

        if (this.mapParams.containsKey("NonTapCost")) {
            final Cost cost = (Cost) (runParams2.get("Cost"));
            if (cost.hasTapCost()) {
                return false;
            }
        }

        if (this.mapParams.containsKey("Conspire")) {
            if (!spellAbility.isOptionalCostPaid(OptionalCost.Conspire)) {
                return false;
            }
            if (spellAbility.getConspireInstances() == 0) {
                return false;
            } else {
                spellAbility.subtractConspireInstance();
                //System.out.println("Conspire instances left = " + spellAbility.getConspireInstances());
            }
        }

        if (this.mapParams.containsKey("Outlast")) {
            if (!spellAbility.isOutlast()) {
                return false;
            }
        }

        if (this.mapParams.containsKey("IsSingleTarget")) {
            Set<GameObject> targets = new HashSet<>();
            for (TargetChoices tc : spellAbility.getAllTargetChoices()) {
                targets.addAll(tc.getTargets());
                if (targets.size() > 1) {
                    return false;
                }
            }
            if (targets.size() != 1) {
                return false;
            }
        }

        if (this.mapParams.containsKey("SpellSpeed")) {
            if (this.mapParams.get("SpellSpeed").equals("NotSorcerySpeed")) {
                if (this.getHostCard().getController().couldCastSorcery(spellAbility)) {
                    return false;
                }
                if (this.getHostCard().hasKeyword("You may cast CARDNAME as though it had flash. If you cast it any time a "
                        + "sorcery couldn't have been cast, the controller of the permanent it becomes sacrifices it at the beginning"
                        + " of the next cleanup step.")) {
                    // for these cards the trigger must only fire if using their own ability to cast at instant speed
                    if (this.getHostCard().hasKeyword("Flash")
                            || this.getHostCard().getController().hasKeyword("You may cast nonland cards as though they had flash.")) {
                        return false;
                    }
                }
                return true;
            }
        }

        if (this.mapParams.containsKey("SharesNameWithActivatorsZone")) {
        	String zones = this.mapParams.get("SharesNameWithActivatorsZone");
            if (si == null) {
                return false;
            }
            boolean sameNameFound = false;
            for (Card c: si.getSpellAbility(true).getActivatingPlayer().getCardsIn(ZoneType.listValueOf(zones))) {
            	if (cast.getName().equals(c.getName())) {
            		sameNameFound = true;
            		break;
                }
            }
            if (!sameNameFound) {
                return false;
           }
        }
        return true;
    }


    /** {@inheritDoc} */
    @Override
    public final void setTriggeringObjects(final SpellAbility sa) {
        final SpellAbility castSA = (SpellAbility) this.getRunParams().get("CastSA");
        final SpellAbilityStackInstance si = sa.getHostCard().getGame().getStack().getInstanceFromSpellAbility(castSA);
        sa.setTriggeringObject("Card", castSA.getHostCard());
        sa.setTriggeringObject("SpellAbility", castSA);
        sa.setTriggeringObject("SpellAbilityTargetingCards", (si != null ? si.getSpellAbility(true) : castSA).getTargets().getTargetCards());
        sa.setTriggeringObject("Player", this.getRunParams().get("Player"));
        sa.setTriggeringObject("Activator", this.getRunParams().get("Activator"));
        sa.setTriggeringObject("CurrentStormCount", this.getRunParams().get("CurrentStormCount"));
        sa.setTriggeringObject("CastSACMC", this.getRunParams().get("CastSACMC"));
    }

    @Override
    public String getImportantStackObjects(SpellAbility sa) {
        StringBuilder sb = new StringBuilder();
        sb.append("Card: ").append(sa.getTriggeringObject("Card")).append(", ");
        sb.append("Activator: ").append(sa.getTriggeringObject("Activator")).append(", ");
        sb.append("SpellAbility: ").append(sa.getTriggeringObject("SpellAbility"));
        return sb.toString();
    }
}
