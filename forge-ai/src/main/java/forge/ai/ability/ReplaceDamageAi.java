package forge.ai.ability;

import java.util.Map;

import com.google.common.collect.Iterables;

import forge.ai.SpellAbilityAi;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

public class ReplaceDamageAi extends SpellAbilityAi {

    @Override
    protected Card chooseSingleCard(Player ai, SpellAbility sa, Iterable<Card> options, boolean isOptional, Player targetedPlayer, Map<String, Object> params) {

        return Iterables.getFirst(options, null);
    }
}
