package forge.ai.ability;

import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.SpellAbilityAi;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

/**
 * The AI has no logic for choosing to remove all counters of a kind, so it still will not activate
 * one on purpose. As a sub-ability it is a consequence of an effect the AI has already decided it
 * wants - usually bookkeeping, like Oblivion Stone clearing the fate counters it just checked - so
 * it must not veto its own parent.
 */
public class CountersRemoveAllAi extends SpellAbilityAi {

    @Override
    protected AiAbilityDecision canPlay(Player aiPlayer, SpellAbility sa) {
        return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
    }

    @Override
    public AiAbilityDecision chkDrawback(Player aiPlayer, SpellAbility sa) {
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }
}
