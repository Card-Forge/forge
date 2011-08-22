package forge.card.trigger;

import forge.AllZone;
import forge.Card;
import forge.Player;
import forge.card.cost.Cost;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.SpellAbility_StackInstance;

import java.util.HashMap;

/**
 * <p>Trigger_SpellAbilityCast class.</p>
 *
 * @author Forge
 * @version $Id$
 */
public class Trigger_SpellAbilityCast extends Trigger {

    /**
     * <p>Constructor for Trigger_SpellAbilityCast.</p>
     *
     * @param params a {@link java.util.HashMap} object.
     * @param host a {@link forge.Card} object.
     */
    public Trigger_SpellAbilityCast(HashMap<String, String> params, Card host, boolean intrinsic) {
        super(params, host, intrinsic);
    }

    /** {@inheritDoc} */
    @Override
    public boolean performTest(java.util.Map<String, Object> runParams) {
        SpellAbility SA = (SpellAbility) runParams.get("CastSA");
        Card cast = SA.getSourceCard();
        SpellAbility_StackInstance si = AllZone.getStack().getInstanceFromSpellAbility(SA);

        if (mapParams.get("Mode").equals("SpellCast")) {
            if (!SA.isSpell()) {
                return false;
            }
        } else if (mapParams.get("Mode").equals("AbilityCast")) {
            if (!SA.isAbility()) {
                return false;
            }
        } else if (mapParams.get("Mode").equals("SpellAbilityCast")) {
            //Empty block for readability.
        }

        if (mapParams.containsKey("ValidControllingPlayer")) {
            if (!matchesValid(cast.getController(), mapParams.get("ValidControllingPlayer").split(","), hostCard)) {
                return false;
            }
        }

        if (mapParams.containsKey("ValidActivatingPlayer")) {
            if (!matchesValid(si.getActivatingPlayer(), mapParams.get("ValidActivatingPlayer").split(","), hostCard)) {
                return false;
            }
        }

        if (mapParams.containsKey("ValidCard")) {
            if (!matchesValid(cast, mapParams.get("ValidCard").split(","), hostCard)) {
                return false;
            }
        }

        if (mapParams.containsKey("TargetsValid")) {
            SpellAbility sa = si.getSpellAbility();
            if (sa.getTarget() == null) {
                if (sa.getTargetCard() == null) {
                    if (sa.getTargetList() == null) {
                        if (sa.getTargetPlayer() == null) {
                            return false;
                        } else {
                            if (!matchesValid(sa.getTargetPlayer(), mapParams.get("TargetsValid").split(","), hostCard)) {
                                return false;
                            }
                        }
                    } else {
                        boolean validTgtFound = false;
                        for (Card tgt : sa.getTargetList()) {
                            if (matchesValid(tgt, mapParams.get("TargetsValid").split(","), hostCard)) {
                                validTgtFound = true;
                                break;
                            }
                        }
                        if (!validTgtFound) {
                            return false;
                        }
                    }
                } else {
                    if (!matchesValid(sa.getTargetCard(), mapParams.get("TargetsValid").split(","), hostCard)) {
                        return false;
                    }
                }
            } else {
                if (sa.getTarget().doesTarget()) {
                    boolean validTgtFound = false;
                    for (Card tgt : sa.getTarget().getTargetCards()) {
                        if (tgt.isValidCard(mapParams.get("TargetsValid").split(","), hostCard.getController(), hostCard)) {
                            validTgtFound = true;
                            break;
                        }
                    }

                    for (Player p : sa.getTarget().getTargetPlayers()) {
                        if (matchesValid(p, mapParams.get("TargetsValid").split(","), hostCard)) {
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

        if (mapParams.containsKey("NonTapCost")) {
            Cost cost = (Cost) (runParams.get("Cost"));
            if (cost.getTap()) return false;
        }

        return true;
    }

    /** {@inheritDoc} */
    @Override
    public Trigger getCopy() {
        Trigger copy = new Trigger_SpellAbilityCast(mapParams, hostCard, isIntrinsic);
        if (overridingAbility != null) {
            copy.setOverridingAbility(overridingAbility);
        }
        copy.setName(name);
        copy.setID(ID);

        return copy;
    }

    /** {@inheritDoc} */
    @Override
    public void setTriggeringObjects(SpellAbility sa) {
        sa.setTriggeringObject("Card", ((SpellAbility) runParams.get("CastSA")).getSourceCard());
        sa.setTriggeringObject("SpellAbility", runParams.get("CastSA"));
        sa.setTriggeringObject("Player", runParams.get("Player"));
        sa.setTriggeringObject("Activator", runParams.get("Activator"));
    }
}
