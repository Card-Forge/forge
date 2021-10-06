package forge.ai.ability;

import java.util.Map;

import forge.ai.ComputerUtilCard;
import forge.ai.SpellAbilityAi;
import forge.game.card.Card;
import forge.game.keyword.Keyword;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

public class ReplaceDamageAi extends SpellAbilityAi {

    @Override
    protected Card chooseSingleCard(Player ai, SpellAbility sa, Iterable<Card> options, boolean isOptional, Player targetedPlayer, Map<String, Object> params) {
        for (Card c : options) {
            // TODO check if enough shields to prevent trigger
            if (c.hasSVar("MustBeBlocked")) {
                return c;
            }
            // TODO check if target can receive counters
            if (c.hasKeyword(Keyword.INFECT)) {
                return c;
            }
            if (c.isCommander()) {
                return c;
            }
            if (c.hasKeyword(Keyword.LIFELINK) || c.hasSVar("LikeLifeLink")) {
                return c;
            }
        }
        return ComputerUtilCard.getBestAI(options);
    }
}
