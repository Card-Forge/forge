package forge.card.ability.effects;

import java.util.List;

import forge.Card;
import forge.Singletons;
import forge.card.ability.SpellAbilityEffect;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.SpellAbilityStackInstance;
import forge.card.spellability.Target;
import forge.card.spellability.TargetSelection;
import forge.game.zone.MagicStack;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class ChangeTargetsEffect extends SpellAbilityEffect {

    /* (non-Javadoc)
     * @see forge.card.ability.SpellAbilityEffect#resolve(forge.card.spellability.SpellAbility)
     */
    @Override
    public void resolve(SpellAbility sa) {
        final List<SpellAbility> sas = getTargetSpellAbilities(sa);
        
        final MagicStack stack = sa.getActivatingPlayer().getGame().getStack();
        for (final SpellAbility tgtSA : sas) {
            SpellAbilityStackInstance si = stack.getInstanceFromSpellAbility(tgtSA);
            if (si == null) {
                // If there isn't a Stack Instance, there isn't really a target
                continue;
            }
            // Update targets, with a potential new target
            si.updateTarget(sa.getActivatingPlayer().getController().chooseTargets(tgtSA));
        }
    }
}
