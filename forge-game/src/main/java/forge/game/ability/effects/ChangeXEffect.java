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
            SpellAbilityStackInstance si = stack.getInstanceFromSpellAbility(tgtSA);
            if (si == null) {
                // If there isn't a Stack Instance, then this doesn't matter
                continue;
            }

            // currently hard coded, no nicer way to get the xManaPaid from that Spell/Card
            si.setXManaPaid(si.getXManaPaid() * 2);
        }
    }
}
