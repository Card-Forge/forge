package forge.game.ability.effects;

import java.util.List;

import forge.game.ability.SpellAbilityEffect;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbilityStackInstance;
import forge.game.zone.MagicStack;

public class ChangeXEffect extends SpellAbilityEffect {

    /* (non-Javadoc)
     * @see forge.card.ability.SpellAbilityEffect#resolve(forge.card.spellability.SpellAbility)
     */
    @Override
    public void resolve(SpellAbility sa) {
        // can't get the SpellAbilityStackInstances directly from the Stack,
        // even if they are in the Triggered Objects
        final List<SpellAbility> sas = getTargetSpells(sa);

        final Player activator = sa.getActivatingPlayer();

        final MagicStack stack = activator.getGame().getStack();
        for (final SpellAbility tgtSA : sas) {
            // for Unbound Flourishing, can't go over SpellAbilityStackInstances because the x is in cast SA copy
            SpellAbility castSA = tgtSA.getHostCard().getCastSA();
            if (castSA != null && tgtSA.equals(castSA)) {
                castSA.setXManaCostPaid(castSA.getXManaCostPaid() * 2);
            }
            // fall back to other potential cards
            SpellAbilityStackInstance si = stack.getInstanceFromSpellAbility(tgtSA);
            if (si != null) {
                // currently hard coded, no nicer way to get the xManaPaid from that Spell/Card
                si.setXManaPaid(si.getXManaPaid() * 2);
            }
        }
    }
}
