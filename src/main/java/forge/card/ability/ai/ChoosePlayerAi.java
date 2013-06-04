package forge.card.ability.ai;

import java.util.List;

import forge.card.ability.SpellAbilityAi;
import forge.card.spellability.SpellAbility;
import forge.game.player.Player;

public class ChoosePlayerAi extends SpellAbilityAi {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#canPlayAI(forge.game.player.Player, java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected boolean canPlayAI(Player ai, SpellAbility sa) {
        return true;
    }

    @Override
    public boolean chkAIDrawback(SpellAbility sa, Player ai) {
        return canPlayAI(ai, sa);
    }

    @Override
    protected boolean doTriggerAINoCost(Player ai, SpellAbility sa, boolean mandatory) {
        return canPlayAI(ai, sa);
    }

    /* (non-Javadoc)
     * @see forge.card.ability.SpellAbilityAi#chooseSinglePlayer(forge.game.player.Player, forge.card.spellability.SpellAbility, java.util.List)
     */
    @Override
    public Player chooseSinglePlayer(Player ai, SpellAbility sa, List<Player> choices) {
        Player chosen = null;
        if ("Curse".equals(sa.getParam("AILogic"))) {
            for (Player pc : choices) {
                if (pc.isOpponentOf(ai)) {
                    chosen = pc;
                    break;
                }
            }
            if (chosen == null) {
                System.out.println("No good curse choices. Picking first available: " + choices.get(0));
                chosen = choices.get(0);
            }
        } else if ("Pump".equals(sa.getParam("AILogic"))) {
            chosen = choices.contains(ai) ? ai : choices.get(0);
        } else {
            System.out.println("Default player choice logic.");
            chosen = choices.contains(ai) ? ai : choices.get(0);
        }
        return chosen;
    }
}
