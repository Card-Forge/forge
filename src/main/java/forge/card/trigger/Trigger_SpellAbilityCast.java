package forge.card.trigger;

import java.util.HashMap;

import forge.AllZone;
import forge.Card;
import forge.Player;
import forge.card.cost.Cost;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.SpellAbility_StackInstance;

/**
 * <p>
 * Trigger_SpellAbilityCast class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class Trigger_SpellAbilityCast extends Trigger {

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
    public Trigger_SpellAbilityCast(final HashMap<String, String> params, final Card host, final boolean intrinsic) {
        super(params, host, intrinsic);
    }

    /** {@inheritDoc} */
    @Override
    public final boolean performTest(final java.util.Map<String, Object> runParams2) {
        final SpellAbility spellAbility = (SpellAbility) runParams2.get("CastSA");
        final Card cast = spellAbility.getSourceCard();
        final SpellAbility_StackInstance si = AllZone.getStack().getInstanceFromSpellAbility(spellAbility);

        if (this.getMapParams().get("Mode").equals("SpellCast")) {
            if (!spellAbility.isSpell()) {
                return false;
            }
        } else if (this.getMapParams().get("Mode").equals("AbilityCast")) {
            if (!spellAbility.isAbility()) {
                return false;
            }
        } else if (this.getMapParams().get("Mode").equals("SpellAbilityCast")) {
            // Empty block for readability.
        }

        if (this.getMapParams().containsKey("ActivatedOnly")) {
            if (spellAbility.isTrigger()) {
                return false;
            }
        }

        if (this.getMapParams().containsKey("ValidControllingPlayer")) {
            if (!this.matchesValid(cast.getController(), this.getMapParams().get("ValidControllingPlayer").split(","),
                    this.getHostCard())) {
                return false;
            }
        }

        if (this.getMapParams().containsKey("ValidActivatingPlayer")) {
            if (!this.matchesValid(si.getActivatingPlayer(), this.getMapParams().get("ValidActivatingPlayer")
                    .split(","), this.getHostCard())) {
                return false;
            }
        }

        if (this.getMapParams().containsKey("ValidCard")) {
            if (!this.matchesValid(cast, this.getMapParams().get("ValidCard").split(","), this.getHostCard())) {
                return false;
            }
        }

        if (this.getMapParams().containsKey("TargetsValid")) {
            final SpellAbility sa = si.getSpellAbility();
            if (sa.getTarget() == null) {
                if (sa.getTargetCard() == null) {
                    if (sa.getTargetList() == null) {
                        if (sa.getTargetPlayer() == null) {
                            return false;
                        } else {
                            if (!this.matchesValid(sa.getTargetPlayer(),
                                    this.getMapParams().get("TargetsValid").split(","), this.getHostCard())) {
                                return false;
                            }
                        }
                    } else {
                        boolean validTgtFound = false;
                        for (final Card tgt : sa.getTargetList()) {
                            if (this.matchesValid(tgt, this.getMapParams().get("TargetsValid").split(","),
                                    this.getHostCard())) {
                                validTgtFound = true;
                                break;
                            }
                        }
                        if (!validTgtFound) {
                            return false;
                        }
                    }
                } else {
                    if (!this.matchesValid(sa.getTargetCard(), this.getMapParams().get("TargetsValid").split(","),
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
                        if (this.matchesValid(p, this.getMapParams().get("TargetsValid").split(","), this.getHostCard())) {
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

        return true;
    }

    /** {@inheritDoc} */
    @Override
    public final Trigger getCopy() {
        final Trigger copy = new Trigger_SpellAbilityCast(this.getMapParams(), this.getHostCard(), this.isIntrinsic());
        if (this.getOverridingAbility() != null) {
            copy.setOverridingAbility(this.getOverridingAbility());
        }
        copy.setName(this.getName());
        copy.setID(this.getId());

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
