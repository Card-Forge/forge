package forge.ai.ability;

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
    protected boolean canPlayAI(Player aiPlayer, SpellAbility sa) {
        // TODO: Extend this if possible for cards that have this as an activated ability
        return false;
    }

    @Override
    protected boolean doTriggerAINoCost(Player aiPlayer, SpellAbility sa, boolean mandatory) {
        return mandatory || canPlayAI(aiPlayer, sa);
    }

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#chkAIDrawback(java.util.Map, forge.card.spellability.SpellAbility, forge.game.player.Player)
     */
    @Override
    public boolean chkAIDrawback(SpellAbility sa, Player aiPlayer) {
        final String logic = sa.getParamOrDefault("AILogic", "");

        if (logic.equals("WeakestOppExceptCtrl")) {
            PlayerCollection targetableOpps = aiPlayer.getOpponents();
            targetableOpps.remove(sa.getHostCard().getController());
            if (targetableOpps.isEmpty()) {
                return false;
            }

            return true;
        }

        return false;
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

