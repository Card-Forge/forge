package forge.card.ability.effects;

import java.util.List;

import forge.card.ability.SpellAbilityEffect;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.SpellAbilityStackInstance;
import forge.card.spellability.TargetChoices;
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
        final List<SpellAbility> sas = getTargetSpells(sa);
        final boolean remember = sa.hasParam("RememberTargetedCard");
        
        final MagicStack stack = sa.getActivatingPlayer().getGame().getStack();
        for (final SpellAbility tgtSA : sas) {
            SpellAbilityStackInstance si = stack.getInstanceFromSpellAbility(tgtSA);
            if (si == null) {
                // If there isn't a Stack Instance, there isn't really a target
                continue;
            }

            SpellAbilityStackInstance changingTgtSI = si;
            while(changingTgtSI != null) {
                // Update targets, with a potential new target
                SpellAbility changingTgtSA = changingTgtSI.getSpellAbility();
                TargetChoices newTarget = sa.getActivatingPlayer().getController().chooseTargets(changingTgtSA);
                changingTgtSI.updateTarget(newTarget);
                changingTgtSI = changingTgtSI.getSubInstace();
            }

            if (remember) {
                sa.getSourceCard().addRemembered(tgtSA.getSourceCard());
            }
        }
    }
}
