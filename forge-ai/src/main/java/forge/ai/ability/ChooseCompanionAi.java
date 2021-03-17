package forge.ai.ability;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;

import forge.ai.SpellAbilityAi;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

public class ChooseCompanionAi extends SpellAbilityAi {

    /* (non-Javadoc)
     * @see forge.card.ability.SpellAbilityAi#chooseSingleCard(forge.card.spellability.SpellAbility, java.util.List, boolean)
     */
    @Override
    public Card chooseSingleCard(final Player ai, final SpellAbility sa, Iterable<Card> options, boolean isOptional, Player targetedPlayer, Map<String, Object> params) {
        List<Card> cards = Lists.newArrayList(options);
        if (cards.isEmpty()) {
            return null;
        }

        Collections.shuffle(cards);
        return cards.get(0);
    }
}

