package forge.ai.ability;

import forge.ai.ComputerUtilCard;
import forge.ai.SpellAbilityAi;
import forge.game.card.*;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

public class AmassAi extends SpellAbilityAi {
    @Override
    protected boolean checkApiLogic(Player ai, final SpellAbility sa) {
        CardCollection aiArmies = CardLists.filter(ai.getCardsIn(ZoneType.Battlefield), CardPredicates.isType("Army"));

        if (!aiArmies.isEmpty()) {
            boolean canAcceptCounters = false;
            for (Card army : aiArmies) {
                if (army.canReceiveCounters(CounterType.P1P1)) {
                    canAcceptCounters = true;
                    break;
                }
            }

            if (!canAcceptCounters) {
                return false;
            }
        } else {
            // TODO: treat this as a Token creation logic, check in the AI will actually get a living token
        }

        return true;
    }

    @Override
    protected boolean doTriggerAINoCost(Player ai, SpellAbility sa, boolean mandatory) {
        return mandatory || checkApiLogic(ai, sa);
    }

    @Override
    public boolean confirmAction(Player player, SpellAbility sa, PlayerActionConfirmMode mode, String message) {
        return true;
    }

    @Override
    protected Card chooseSingleCard(Player ai, SpellAbility sa, Iterable<Card> options, boolean isOptional, Player targetedPlayer) {
        return ComputerUtilCard.getBestAI(options);
    }
}

