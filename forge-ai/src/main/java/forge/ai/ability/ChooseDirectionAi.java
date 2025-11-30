package forge.ai.ability;

import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.SpellAbilityAi;
import forge.game.Direction;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.Aggregates;

public class ChooseDirectionAi extends SpellAbilityAi {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#canPlayAI(forge.game.player.Player, java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected AiAbilityDecision canPlay(Player ai, SpellAbility sa) {
        final String logic = sa.getParam("AILogic");
        final Game game = sa.getActivatingPlayer().getGame();
        if (logic == null) {
            return new AiAbilityDecision(0, AiPlayDecision.MissingLogic);
        } else {
            if ("Aminatou".equals(logic)) {
                CardCollection all = CardLists.filter(game.getCardsIn(ZoneType.Battlefield), CardPredicates.NONLAND_PERMANENTS);
                CardCollection aiPermanent = CardLists.filterControlledBy(all, ai);
                aiPermanent.remove(sa.getHostCard());
                int aiValue = Aggregates.sum(aiPermanent, Card::getCMC);
                CardCollection left = CardLists.filterControlledBy(all, game.getNextPlayerAfter(ai, Direction.Left));
                CardCollection right = CardLists.filterControlledBy(all, game.getNextPlayerAfter(ai, Direction.Right));
                int leftValue = Aggregates.sum(left, Card::getCMC);
                int rightValue = Aggregates.sum(right, Card::getCMC);
                if (aiValue <= leftValue && aiValue <= rightValue) {
                    return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
                }
            }
        }
        return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
    }

    @Override
    public AiAbilityDecision chkDrawback(Player ai, SpellAbility sa) {
        return canPlay(ai, sa);
    }

    @Override
    protected AiAbilityDecision doTriggerNoCost(Player ai, SpellAbility sa, boolean mandatory) {
        if (mandatory) {
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        }
        return canPlay(ai, sa);
    }
}
