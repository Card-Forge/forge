package forge.ai.ability;

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
    protected boolean canPlayAI(Player ai, SpellAbility sa) {
        final String logic = sa.getParam("AILogic");
        final Game game = sa.getActivatingPlayer().getGame();
        if (logic == null) {
            return false;
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
                return aiValue <= leftValue && aiValue <= rightValue;
            }
        }
        return true;
    }

    @Override
    public boolean chkAIDrawback(SpellAbility sa, Player ai) {
        return canPlayAI(ai, sa);
    }

    @Override
    protected boolean doTriggerAINoCost(Player ai, SpellAbility sa, boolean mandatory) {
        return mandatory || canPlayAI(ai, sa);
    }
}
