package forge.ai.ability;

import java.util.List;

import forge.ai.ComputerUtilCard;
import forge.ai.ComputerUtilCombat;
import forge.ai.SpellAbilityAi;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

public class GoadAi extends SpellAbilityAi {

    @Override
    protected boolean checkApiLogic(final Player ai, final SpellAbility sa) {
        final Card source = sa.getHostCard();
        final Game game = source.getGame();

        // use this part only for targeting
        if (sa.usesTargeting()) {
            // get all possible targets
            List<Card> list = CardLists.getTargetableCards(game.getCardsIn(ZoneType.Battlefield), sa);

            if (list.isEmpty())
                return false;

            if (game.getPlayers().size() > 2) {
                // use this part only in multiplayer
                CardCollection betterList = CardLists.filter(list, c -> {
                    // filter only creatures which can attack
                    if (ComputerUtilCard.isUselessCreature(ai, c)) {
                        return false;
                    }
                    // useless
                    if (c.isGoadedBy(ai)) {
                        return false;
                    }
                    // select creatures which can attack an Opponent other than ai
                    for (Player o : ai.getOpponents()) {
                        if (ComputerUtilCombat.canAttackNextTurn(c, o)) {
                            return true;
                        }
                    }
                    return false;
                });

                // if better list is not empty, use that one instead
                if (!betterList.isEmpty()) {
                    list = betterList;
                    sa.getTargets().add(ComputerUtilCard.getBestCreatureAI(list));
                    return true;
                }
            } else {
                // single Player, goaded creature would attack ai
                CardCollection betterList = CardLists.filter(list, c -> {
                    // filter only creatures which can attack
                    if (ComputerUtilCard.isUselessCreature(ai, c)) {
                        return false;
                    }
                    // useless
                    if (c.isGoadedBy(ai)) {
                        return false;
                    }
                    // select only creatures AI can block
                    return ComputerUtilCard.canBeBlockedProfitably(ai, c, false);
                });

                // if better list is not empty, use that one instead
                if (!betterList.isEmpty()) {
                    list = betterList;
                    sa.getTargets().add(ComputerUtilCard.getBestCreatureAI(list));
                    return true;
                }
            }

            // AI does not find a good creature to goad.
            // because if it would goad a creature it would attack AI.
            // AI might not have enough information to block it
            return false;
        }
        return true;
    }

    @Override
    protected boolean doTriggerAINoCost(Player ai, SpellAbility sa, boolean mandatory) {
        if (checkApiLogic(ai, sa)) {
            return true;
        }
        if (!mandatory) {
            return false;
        }
        if (sa.usesTargeting()) {
            if (sa.getTargetRestrictions().canTgtPlayer()) {
                for (Player opp : ai.getOpponents()) {
                    if (sa.canTarget(opp)) {
                        sa.getTargets().add(opp);
                        return true;
                    }
                }
                if (sa.canTarget(ai)) {
                    sa.getTargets().add(ai);
                    return true;
                }
            } else {
                List<Card> list = CardLists.getTargetableCards(ai.getGame().getCardsIn(ZoneType.Battlefield), sa);

                if (list.isEmpty())
                    return false;

                sa.getTargets().add(ComputerUtilCard.getWorstCreatureAI(list));
                return true;
            }
        }
        return false;
    }

}
