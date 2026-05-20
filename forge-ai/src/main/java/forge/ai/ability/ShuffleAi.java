package forge.ai.ability;

import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.SpellAbilityAi;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.spellability.SpellAbility;

import java.util.Map;

public class ShuffleAi extends SpellAbilityAi {
    @Override
    protected AiAbilityDecision canPlay(Player aiPlayer, SpellAbility sa) {
        // TODO Does the AI know what's on top of the deck and is it something useful?

        String logic = sa.getParamOrDefault("AILogic", "");
        if (logic.equals("Always")) {
            // We may want to play this for the subability, e.g. Mind's Desire
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        } else if (logic.equals("OwnMain2")) {
            if (aiPlayer.getGame().getPhaseHandler().is(PhaseType.MAIN2, aiPlayer)) {
                // We may want to play this for the subability, e.g. Mind's Desire
                return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
            } else {
                return new AiAbilityDecision(0, AiPlayDecision.WaitForMain2);
            }
        }

        // not really sure when the compy would use this; maybe only after a human
        // deliberately put a card on top of their library
        return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
    }

    @Override
    public AiAbilityDecision chkDrawback(Player aiPlayer, SpellAbility sa) {
        return shuffleTargetAI(sa);
    }

    private AiAbilityDecision shuffleTargetAI(final SpellAbility sa) {
        /*
         *  Shuffle at the end of some other effect where we'd usually shuffle
         *  inside that effect, but can't for some reason.
         */
        if (sa.getParent() != null) {
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        } else {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
    } // shuffleTargetAI()

    @Override
    protected AiAbilityDecision doTriggerNoCost(Player aiPlayer, SpellAbility sa, boolean mandatory) {
        return shuffleTargetAI(sa);
    }  

    @Override
    public boolean confirmAction(Player player, SpellAbility sa, PlayerActionConfirmMode mode, String message, Map<String, Object> params) {
        // ai could analyze parameter denoting the player to shuffle
        return true;
    }
}
