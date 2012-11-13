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
package forge.card.trigger;

import forge.Singletons;

import forge.Card;
import forge.card.cost.Cost;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.SpellAbilityStackInstance;
import forge.game.player.Player;

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
     *            a {@link forge.Card} object.
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
        final Card cast = spellAbility.getSourceCard();
        final SpellAbilityStackInstance si = Singletons.getModel().getGame().getStack().getInstanceFromSpellAbility(spellAbility);

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

        if (this.getMapParams().containsKey("ActivatedOnly")) {
            if (spellAbility.isTrigger()) {
                return false;
            }
        }

        if (this.getMapParams().containsKey("ValidControllingPlayer")) {
            if (!matchesValid(cast.getController(), this.getMapParams().get("ValidControllingPlayer").split(","),
                    this.getHostCard())) {
                return false;
            }
        }

        if (this.getMapParams().containsKey("ValidActivatingPlayer")) {
            if (si == null || !matchesValid(si.getActivatingPlayer(), this.getMapParams().get("ValidActivatingPlayer")
                    .split(","), this.getHostCard())) {
                return false;
            }
        }

        if (this.getMapParams().containsKey("ValidCard")) {
            if (!matchesValid(cast, this.getMapParams().get("ValidCard").split(","), this.getHostCard())) {
                return false;
            }
        }

        if (this.getMapParams().containsKey("TargetsValid")) {
            final SpellAbility sa = si.getSpellAbility();
            if (sa.getTarget() == null) {
                if (sa.getTargetCard() == null) {
                    if (sa.getTargetPlayer() == null) {
                        return false;
                    } else {
                        if (!matchesValid(sa.getTargetPlayer(),
                                this.getMapParams().get("TargetsValid").split(","), this.getHostCard())) {
                            return false;
                        }
                    }
                } else {
                    if (!matchesValid(sa.getTargetCard(), this.getMapParams().get("TargetsValid").split(","),
                            this.getHostCard())) {
                        return false;
                    }
                }
            } else {
                if (sa.getTarget().doesTarget()) {
                    boolean validTgtFound = false;
                    for (final Card tgt : sa.getTarget().getTargetCards()) {
                        if (tgt.isValid(this.getMapParams().get("TargetsValid").split(","), this.getHostCard()
                                .getController(), this.getHostCard())) {
                            validTgtFound = true;
                            break;
                        }
                    }

                    for (final Player p : sa.getTarget().getTargetPlayers()) {
                        if (matchesValid(p, this.getMapParams().get("TargetsValid").split(","), this.getHostCard())) {
                            validTgtFound = true;
                            break;
                        }
                    }

                    if (!validTgtFound) {
                        return false;
                    }
                } else {
                    return false;
                }
            }
        }

        if (this.getMapParams().containsKey("NonTapCost")) {
            final Cost cost = (Cost) (runParams2.get("Cost"));
            if (cost.getTap()) {
                return false;
            }
        }

        if (this.getMapParams().containsKey("Conspire")) {
            if (!spellAbility.isOptionalAdditionalCostPaid("Conspire")) {
                return false;
            }
        }

        if (this.getMapParams().containsKey("SpellSpeed")) {
            if (this.getMapParams().get("SpellSpeed").equals("NotSorcerySpeed")) {
                boolean notSorcerySpeed = true;
                if (this.getHostCard().getController().couldCastSorcery(spellAbility)) {
                    notSorcerySpeed = false;
                } else if (this.getHostCard().hasKeyword("You may cast CARDNAME as though it had flash. If you cast it any time a "
                        + "sorcery couldn't have been cast, the controller of the permanent it becomes sacrifices it at the beginning"
                        + " of the next cleanup step.") && !this.getHostCard().getController().couldCastSorcery(spellAbility)) {
                    boolean instantmentCast = true;
                    // for these cards the trigger must only fire if using their own ability to cast at instant speed
                    if (this.getHostCard().hasKeyword("Flash") || this.getHostCard().hasKeyword("HIDDEN Flash")
                            || this.getHostCard().getController().hasKeyword("You may cast nonland cards as though they had flash.")) {
                        instantmentCast = false;
                    }
                    notSorcerySpeed = instantmentCast;
                }
                return notSorcerySpeed;
            }
        }

        return true;
    }

    /** {@inheritDoc} */
    @Override
    public final Trigger getCopy() {
        final Trigger copy = new TriggerSpellAbilityCast(this.getMapParams(), this.getHostCard(), this.isIntrinsic());
        if (this.getOverridingAbility() != null) {
            copy.setOverridingAbility(this.getOverridingAbility());
        }

        copyFieldsTo(copy);
        return copy;
    }

    /** {@inheritDoc} */
    @Override
    public final void setTriggeringObjects(final SpellAbility sa) {
        sa.setTriggeringObject("Card", ((SpellAbility) this.getRunParams().get("CastSA")).getSourceCard());
        sa.setTriggeringObject("SpellAbility", this.getRunParams().get("CastSA"));
        sa.setTriggeringObject("Player", this.getRunParams().get("Player"));
        sa.setTriggeringObject("Activator", this.getRunParams().get("Activator"));
    }
}
