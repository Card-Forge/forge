package forge.ai.ability;


import forge.ai.ComputerUtil;
import forge.ai.SpellAbilityAi;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;

public class RearrangeTopOfLibraryAi extends SpellAbilityAi {
    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#canPlayAI(forge.game.player.Player, java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected boolean canPlayAI(Player aiPlayer, SpellAbility sa) {
        return sa.isMandatory(); // AI doesn't do anything with this SA yet, but at least it shouldn't miss mandatory triggers
    }

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#doTriggerAINoCost(forge.game.player.Player, java.util.Map, forge.card.spellability.SpellAbility, boolean)
     */
    @Override
    protected boolean doTriggerAINoCost(Player ai, SpellAbility sa, boolean mandatory) {

        final TargetRestrictions tgt = sa.getTargetRestrictions();

        if (tgt != null) {
            // ability is targeted
            sa.resetTargets();

            Player opp = ComputerUtil.getOpponentFor(ai);
            final boolean canTgtHuman = opp.canBeTargetedBy(sa);

            if (!canTgtHuman) {
                return false;
            } else {
                sa.getTargets().add(opp);
            }
        } else {
            // if it's just defined, no big deal
        }

        // TODO: the AI currently doesn't do anything with this ability, consider improving.
        // For now, "true" is returned (without any action) if the SA is mandatory in order
        // not to miss triggers.
        return sa.isMandatory();
    }
}
