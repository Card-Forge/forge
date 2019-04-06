package forge.ai.ability;

import forge.ai.ComputerUtilCard;
import forge.ai.SpellAbilityAi;
import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.*;
import forge.game.card.token.TokenInfo;
import forge.game.phase.PhaseHandler;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.TriggerType;
import forge.game.zone.ZoneType;

public class AmassAi extends SpellAbilityAi {
    @Override
    protected boolean checkApiLogic(Player ai, final SpellAbility sa) {
        CardCollection aiArmies = CardLists.filter(ai.getCardsIn(ZoneType.Battlefield), CardPredicates.isType("Army"));
        Card host = sa.getHostCard();

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
            final String tokenScript = "b_0_0_zombie_army";
            final int amount = AbilityUtils.calculateAmount(host, sa.getParamOrDefault("Num", "1"), sa);

            Card result = TokenInfo.getProtoType(tokenScript, sa);

            if (result == null) {
                return false;
            }

            result.setController(ai, 0);

            host.getGame().getTriggerHandler().suppressMode(TriggerType.CounterAdded);
            host.getGame().getTriggerHandler().suppressMode(TriggerType.CounterAddedOnce);
            result.addCounter(CounterType.P1P1, amount, ai, true);
            host.getGame().getTriggerHandler().clearSuppression(TriggerType.CounterAdded);
            host.getGame().getTriggerHandler().clearSuppression(TriggerType.CounterAddedOnce);

            final Game game = ai.getGame();
            ComputerUtilCard.applyStaticContPT(game, result, null);
            if (result.isCreature() && result.getNetToughness() < 1) {
                return false;
            } else {
                return true;
            }
        }

        return true;
    }

    @Override
    protected boolean checkPhaseRestrictions(final Player ai, final SpellAbility sa, final PhaseHandler ph) {
        // TODO: Special check for instant speed logic? Something like Lazotep Plating.
        /*
        boolean isInstant = sa.getRestrictions().isInstantSpeed();
        CardCollection aiArmies = CardLists.filter(ai.getCardsIn(ZoneType.Battlefield), CardPredicates.isType("Army"));

        if (isInstant) {

        }
        */

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

