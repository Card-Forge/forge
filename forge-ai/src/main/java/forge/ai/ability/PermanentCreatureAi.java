package forge.ai.ability;

import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;

import forge.ai.ComputerUtil;
import forge.ai.ComputerUtilCard;
import forge.ai.ComputerUtilCost;
import forge.ai.SpellAbilityAi;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.card.CardFactory;
import forge.game.card.CardPredicates;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/** 
 * AbilityFactory for Creature Spells.
 *
 */
public class PermanentCreatureAi extends SpellAbilityAi {
    @Override
    protected boolean canPlayAI(Player aiPlayer, SpellAbility sa) {
        String logic = sa.getParam("AILogic");
        Game game = aiPlayer.getGame();
        final PhaseHandler ph = game.getPhaseHandler();

        if ("ZeroToughness".equals(logic)) {
            // If Creature has Zero Toughness, make sure some static ability is in play
            // That will grant a toughness bonus

            final CardCollectionView list = aiPlayer.getCardsIn(ZoneType.Battlefield);
            if (!Iterables.any(list, Predicates.or(CardPredicates.nameEquals("Glorious Anthem"),
                    CardPredicates.nameEquals("Gaea's Anthem")))) {
                return false;
            }

            // TODO See if card ETB will survive after Static Effects
            /*
            List<Card> cards = game.getCardsIn(ZoneType.Battlefield);

            for(Card c : cards) {
                ArrayList<StaticAbility> statics = c.getStaticAbilities();
                for(StaticAbility s : statics) {
                    final Map<String, String> stabMap = s.parseParams();

                    if (!stabMap.get("Mode").equals("Continuous")) {
                        continue;
                    }

                    final String affected = stabMap.get("Affected");

                    if (affected == null) {
                        continue;
                    }
                }
            }
            */
        }
        
        // FRF Dash Keyword
        if (sa.isDash()) {
            //only checks that the dashed creature will attack
            if (ph.isPlayerTurn(aiPlayer) && ph.getPhase().isBefore(PhaseType.COMBAT_DECLARE_ATTACKERS)) {
                if (ComputerUtilCost.canPayCost(sa.getHostCard().getSpellPermanent(),aiPlayer)) {
                    //do not dash if creature can be played normally
                    return false;
                }
                Card dashed = CardFactory.copyCard(sa.getHostCard(), true);
                dashed.setSickness(false);
                return ComputerUtilCard.doesSpecifiedCreatureAttackAI(aiPlayer, dashed);
            } else {
                return false;
            }
        }

        // Wait for Main2 if possible
        if (ph.is(PhaseType.MAIN1)
                && !ComputerUtil.castPermanentInMain1(aiPlayer, sa)) {
            return false;
        }

        // AI shouldn't be retricted all that much for Creatures for now
        return true;
    }

}
