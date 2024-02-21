package forge.ai.ability;


import com.google.common.collect.Iterables;
import forge.ai.ComputerUtil;
import forge.ai.SpellAbilityAi;
import forge.game.card.*;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.player.PlayerController;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

import java.util.Map;

public class TimeTravelAi extends SpellAbilityAi {
    @Override
    protected boolean canPlayAI(Player aiPlayer, SpellAbility sa) {
        boolean hasSuspendedCards = Iterables.any(aiPlayer.getCardsIn(ZoneType.Exile), CardPredicates.hasSuspend());
        boolean hasRelevantCardsOTB = Iterables.any(aiPlayer.getCardsIn(ZoneType.Battlefield), CardPredicates.hasCounter(CounterEnumType.TIME));

        // TODO: add more logic for cards which may need it
        return hasSuspendedCards || hasRelevantCardsOTB;
    }

    @Override
    public boolean chooseBinary(PlayerController.BinaryChoiceType kindOfChoice, SpellAbility sa, Map<String, Object> params) {
        // Returning true means "add counter", false means "remove counter"

        // TODO: extend this (usually, stuff in exile such as Suspended cards with Time counters is played once no Time counters are left,
        // so removing them is good; stuff on the battlefield is usually stuff like Vanishing or As Foretold, which favors adding Time
        // counters for better effect, but exceptions should be added here).
        Card target = (Card)params.get("Target");
        return !ComputerUtil.isNegativeCounter(CounterType.get(CounterEnumType.TIME), target);
    }

    @Override
    protected Card chooseSingleCard(Player ai, SpellAbility sa, Iterable<Card> options, boolean isOptional, Player targetedPlayer, Map<String, Object> params) {
        return Iterables.getFirst(options, null);
    }

    @Override
    public boolean confirmAction(Player player, SpellAbility sa, PlayerActionConfirmMode mode, String message, Map<String, Object> params) {
        return true;
    }
}
