package forge.ai.ability;

import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.ComputerUtilCard;
import forge.ai.SpellAbilityAi;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

import java.util.List;

public class HauntAi extends SpellAbilityAi {

    @Override
    protected AiAbilityDecision doTriggerNoCost(Player ai, SpellAbility sa, boolean mandatory) {
        final Card card = sa.getHostCard();
        final Game game = ai.getGame();
        if (sa.usesTargeting() && !card.isToken()) {
            final List<Card> creats = CardLists.filter(game.getCardsIn(ZoneType.Battlefield),
                    CardPredicates.CREATURES);

            // nothing to haunt
            if (creats.isEmpty()) {
                return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            }

            final List<Card> oppCreats = CardLists.filterControlledBy(creats, ai.getOpponents());
            sa.getTargets().add(ComputerUtilCard.getWorstCreatureAI(oppCreats.isEmpty() ? creats : oppCreats));
        }
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }
}