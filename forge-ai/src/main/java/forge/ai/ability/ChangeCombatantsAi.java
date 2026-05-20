package forge.ai.ability;

import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.SpellAbilityAi;
import forge.game.GameEntity;
import forge.game.player.Player;
import forge.game.player.PlayerCollection;
import forge.game.player.PlayerPredicates;
import forge.game.spellability.SpellAbility;

import java.util.Collection;
import java.util.Map;

public class ChangeCombatantsAi extends SpellAbilityAi {
    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#canPlayAI(forge.game.player.Player, java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected AiAbilityDecision canPlay(Player aiPlayer, SpellAbility sa) {
        // TODO: Extend this if possible for cards that have this as an activated ability
        return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
    }

    @Override
    protected AiAbilityDecision doTriggerNoCost(Player aiPlayer, SpellAbility sa, boolean mandatory) {
        if (mandatory) {
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        }
        return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
    }

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#chkAIDrawback(java.util.Map, forge.card.spellability.SpellAbility, forge.game.player.Player)
     */
    @Override
    public AiAbilityDecision chkDrawback(Player aiPlayer, SpellAbility sa) {
        final String logic = sa.getParamOrDefault("AILogic", "");

        if (logic.equals("WeakestOppExceptCtrl")) {
            PlayerCollection targetableOpps = aiPlayer.getOpponents();
            targetableOpps.remove(sa.getHostCard().getController());
            if (targetableOpps.isEmpty()) {
                return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            }
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        }

        return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
    }

    @Override
    public <T extends GameEntity> T chooseSingleEntity(Player ai, SpellAbility sa, Collection<T> options, boolean isOptional, Player targetedPlayer, Map<String, Object> params) {
        PlayerCollection targetableOpps = new PlayerCollection();
        for (GameEntity p : options) {
            if (p instanceof Player && !p.equals(sa.getHostCard().getController())) {
                Player pp = (Player)p;
                if (pp.isOpponentOf(ai)) {
                    targetableOpps.add(pp);
                }
            }
        }

        Player weakestTargetableOpp = targetableOpps.filter(PlayerPredicates.isTargetableBy(sa))
                .min(PlayerPredicates.compareByLife());

        return (T)weakestTargetableOpp;
    }
}
