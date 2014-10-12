package forge.ai.ability;

import com.google.common.base.Predicate;

import forge.ai.ComputerUtilCard;
import forge.ai.SpellAbilityAi;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates.Presets;
import forge.game.card.CounterType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

import java.util.List;


public class RepeatEachAi extends SpellAbilityAi {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#canPlayAI(forge.game.player.Player, forge.card.spellability.SpellAbility)
     */
    @Override
    protected boolean canPlayAI(Player aiPlayer, SpellAbility sa) {
        String logic = sa.getParam("AILogic");

        if ("CloneMyTokens".equals(logic)) {
            if (CardLists.filter(aiPlayer.getCreaturesInPlay(), Presets.TOKEN).size() < 2) {
                return false;
            }
        } else if ("CloneAllTokens".equals(logic)) {
            final Player opp = aiPlayer.getOpponent();
            List<Card> humTokenCreats = CardLists.filter(opp.getCreaturesInPlay(), Presets.TOKEN);
            List<Card> compTokenCreats = CardLists.filter(aiPlayer.getCreaturesInPlay(), Presets.TOKEN);

            if (compTokenCreats.size() <= humTokenCreats.size()) {
                return false;
            }
        } else if ("DoubleCounters".equals(logic)) {
            // TODO Improve this logic, double Planeswalker counters first, then +1/+1 on Useful creatures
            // Then Charge Counters, then -1/-1 on Opposing Creatures
            CardCollection perms = new CardCollection(aiPlayer.getCardsIn(ZoneType.Battlefield));
            perms = CardLists.filter(CardLists.getTargetableCards(perms, sa), new Predicate<Card>() {
                @Override
                public boolean apply(final Card c) {
                    return c.hasCounters();
                }
            });
            if (perms.isEmpty()) {
                return false;
            }
            CardLists.shuffle(perms);
            sa.setTargetCard(perms.get(0));
        } else if ("RemoveAllCounters".equals(logic)) {
            // Break Dark Depths
            CardCollectionView depthsList = aiPlayer.getCardsIn(ZoneType.Battlefield, "Dark Depths");
            depthsList = CardLists.filter(depthsList, new Predicate<Card>() {
                @Override
                public boolean apply(final Card crd) {
                    return crd.getCounters(CounterType.ICE) >= 3;
                }
            });

            if (depthsList.size() > 0) {
                sa.getTargets().add(depthsList.getFirst());
                return true;
            }

            // Get rid of Planeswalkers:
            CardCollectionView list = aiPlayer.getOpponent().getCardsIn(ZoneType.Battlefield);
            list = CardLists.filter(list, new Predicate<Card>() {
                @Override
                public boolean apply(final Card crd) {
                    return crd.isPlaneswalker() && (crd.getCounters(CounterType.LOYALTY) >= 5);
                }
            });

            if (list.isEmpty()) {
                return false;
            }

            sa.getTargets().add(list.getFirst());
        } else if ("BalanceLands".equals(logic)) {
            if (CardLists.filter(aiPlayer.getCardsIn(ZoneType.Battlefield), Presets.LANDS).size() >= 5) {
                return false;
            }

            List<Player> opponents = aiPlayer.getOpponents();
            for(Player opp : opponents) {
                if (CardLists.filter(opp.getCardsIn(ZoneType.Battlefield), Presets.LANDS).size() < 4) {
                    return false;
                }
            }
        } else if ("GainControlOwns".equals(logic)) {
            List<Card> list = CardLists.filter(aiPlayer.getGame().getCardsIn(ZoneType.Battlefield), new Predicate<Card>() {
                @Override
                public boolean apply(final Card crd) {
                    return crd.isCreature() && !crd.getController().equals(crd.getOwner());
                }
            });
            if (list.isEmpty()) {
                return false;
            }
            for (final Card c : list) {
                if (aiPlayer.equals(c.getController())) {
                    return false;
                }
            }
        } else if ("OpponentHasCreatures".equals(logic)) {
        	for (Player opp : aiPlayer.getOpponents()) {
        		if (!opp.getCreaturesInPlay().isEmpty()){
        			return true;
        		}
        	}
        	return false;
        }

        // TODO Add some normal AI variability here

        return true;
    }

    @Override
    protected Card chooseSingleCard(Player ai, SpellAbility sa, Iterable<Card> options, boolean isOptional, Player targetedPlayer) {
        return ComputerUtilCard.getBestCreatureAI(options);
    }
}
