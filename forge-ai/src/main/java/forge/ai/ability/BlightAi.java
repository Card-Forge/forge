package forge.ai.ability;

import java.util.Map;
import java.util.Optional;

import forge.ai.ComputerUtilCard;
import forge.ai.SpellAbilityAi;
import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.util.StreamUtil;

public class BlightAi extends SpellAbilityAi {

    protected Card chooseSingleCard(Player ai, SpellAbility sa, Iterable<Card> options, boolean isOptional, Player targetedPlayer, Map<String, Object> params) {
        Optional<Card> filtered = StreamUtil.stream(options).filter(c -> !c.canReceiveCounters(CounterEnumType.M1M1)).findAny();
        if (filtered.isPresent()) {
            return filtered.get();
        }
        return ComputerUtilCard.getWorstCreatureAI(options);
    }
}
