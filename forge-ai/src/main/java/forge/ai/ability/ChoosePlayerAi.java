package forge.ai.ability;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import forge.ai.ComputerUtil;
import forge.ai.SpellAbilityAi;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

import java.util.List;

public class ChoosePlayerAi extends SpellAbilityAi {
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

    @Override
    public Player chooseSinglePlayer(Player ai, SpellAbility sa, Iterable<Player> choices) {
        Player chosen = null;
        if ("Curse".equals(sa.getParam("AILogic"))) {
            for (Player pc : choices) {
                if (pc.isOpponentOf(ai)) {
                    chosen = pc;
                    break;
                }
            }
            if (chosen == null) {
                chosen = Iterables.getFirst(choices, null);
                System.out.println("No good curse choices. Picking first available: " + chosen);
            }
        }
        else if ("Pump".equals(sa.getParam("AILogic"))) {
            chosen = Iterables.contains(choices, ai) ? ai : Iterables.getFirst(choices, null);
        }
        else if ("BestAllyBoardPosition".equals(sa.getParam("AILogic"))) {
            List<Player> prefChoices = Lists.newArrayList(choices);
            prefChoices.removeAll(ai.getOpponents());
            if (!prefChoices.isEmpty()) {
                chosen = ComputerUtil.evaluateBoardPosition(prefChoices);
            }
            if (chosen == null) {
                chosen = Iterables.getFirst(choices, null);
                System.out.println("No good curse choices. Picking first available: " + chosen);
            }
        } else if ("MostCardsInHand".equals(sa.getParam("AILogic"))) {
            int cardsInHand = 0;
            for (final Player p : choices) {
                int hand = p.getCardsIn(ZoneType.Hand).size();
                if (hand >= cardsInHand) {
                    chosen = p;
                    cardsInHand = hand;
                }
            }
        } else if ("LeastCreatures".equals(sa.getParam("AILogic"))) {
            int creats = 50;
            for (final Player p : choices) {
                int curr = p.getCreaturesInPlay().size();
                if (curr <= creats) {
                    chosen = p;
                    creats = curr;
                }
            }
        } else {
            System.out.println("Default player choice logic.");
            chosen = Iterables.contains(choices, ai) ? ai : Iterables.getFirst(choices, null);
        }
        return chosen;
    }
}
