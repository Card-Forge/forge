package forge.game.ability.effects;

import java.util.List;

import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.spellability.SpellAbility;

public class ChangeXEffect extends SpellAbilityEffect {

    /* (non-Javadoc)
     * @see forge.card.ability.SpellAbilityEffect#resolve(forge.card.spellability.SpellAbility)
     */
    @Override
    public void resolve(SpellAbility sa) {
        // can't get the SpellAbilityStackInstances directly from the Stack,
        // even if they are in the Triggered Objects
        final List<SpellAbility> sas = getTargetSpells(sa);

        for (final SpellAbility tgtSA : sas) {
            int value = AbilityUtils.calculateAmount(sa.getHostCard(), sa.getParam("Value"), sa);
            // for Unbound Flourishing, can't go over SpellAbilityStackInstances because the x is in cast SA copy
            SpellAbility castSA = tgtSA.getHostCard().getCastSA();
            if (tgtSA.equals(castSA) && castSA.getXManaCostPaid() != null) {
                castSA.setXManaCostPaid(value);
            }
            if (tgtSA.getXManaCostPaid() != null) {
                tgtSA.setXManaCostPaid(value);
            }
        }
    }
}
