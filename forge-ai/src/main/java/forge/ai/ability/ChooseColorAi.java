package forge.ai.ability;

import forge.ai.ComputerUtil;
import forge.ai.ComputerUtilCard;
import forge.ai.ComputerUtilMana;
import forge.ai.SpellAbilityAi;
import forge.card.MagicColor;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.card.CardFactoryUtil;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.MyRandom;

public class ChooseColorAi extends SpellAbilityAi {

    @Override
    protected boolean canPlayAI(Player ai, SpellAbility sa) {
        final Card source = sa.getHostCard();
        final Game game = ai.getGame();
        
        if (!sa.hasParam("AILogic")) {
            return false;
        }
        final String logic = sa.getParam("AILogic");

        if (ComputerUtil.preventRunAwayActivations(sa)) {
            return false;
        }

        if ("Nykthos, Shrine to Nyx".equals(sa.getHostCard().getName())) {
            PhaseHandler ph = game.getPhaseHandler();
            if (!ph.isPlayerTurn(ai) || ph.getPhase().isBefore(PhaseType.MAIN2)) {
                return false;
            }
            String prominentColor = ComputerUtilCard.getMostProminentColor(ai.getCardsIn(ZoneType.Battlefield));
            int devotion = CardFactoryUtil.xCount(sa.getHostCard(), "Count$Devotion." + prominentColor);
            //int numLands = CardLists.filter(ai.getCardsIn(ZoneType.Battlefield), CardPredicates.nameEquals(MagicColor.Constant.BASIC_LANDS.get(MagicColor.getIndexOfFirstColor(MagicColor.fromName(prominentColor))))).size(); // TODO: maybe this logic also has to take the number of available lands of most prominent color type into account

            // do not use Nykthos if devotion to most prominent color is less than 4 (since {2} is paid to activate Nykthos, and Nykthos itself is tapped too)
            if (devotion < 4) {
                return false;
            }
        }

        if ("Oona, Queen of the Fae".equals(sa.getHostCard().getName())) {
            PhaseHandler ph = game.getPhaseHandler();
        	if (ph.isPlayerTurn(ai) || ph.getPhase().isBefore(PhaseType.COMBAT_DECLARE_ATTACKERS)) {
        		return false;
        	}
            // Set PayX here to maximum value.
            int x = ComputerUtilMana.determineLeftoverMana(sa, ai);
            source.setSVar("PayX", Integer.toString(x));
            return true;
        }
        
        if ("Addle".equals(sa.getHostCard().getName())) {
            PhaseHandler ph = game.getPhaseHandler();
        	if (ph.getPhase().isBefore(PhaseType.COMBAT_DECLARE_ATTACKERS) || ai.getOpponent().getCardsIn(ZoneType.Hand).isEmpty()) {
        		return false;
        	}
            return true;
        }
        
        if (logic.equals("MostExcessOpponentControls")) {
        	for (byte color : MagicColor.WUBRG) {
        		CardCollectionView ailist = ai.getCardsIn(ZoneType.Battlefield);
        		CardCollectionView opplist = ai.getOpponent().getCardsIn(ZoneType.Battlefield);
        		
        		ailist = CardLists.filter(ailist, CardPredicates.isColor(color));
        		opplist = CardLists.filter(opplist, CardPredicates.isColor(color));

                int excess = ComputerUtilCard.evaluatePermanentList(opplist) - ComputerUtilCard.evaluatePermanentList(ailist);
                if (excess > 4) {
                	return true;
                }
            }
        	return false;
        }

        boolean chance = MyRandom.getRandom().nextFloat() <= Math.pow(.6667, sa.getActivationsThisTurn());
        return chance;
    }

    @Override
    protected boolean doTriggerAINoCost(Player ai, SpellAbility sa, boolean mandatory) {
        return mandatory || canPlayAI(ai, sa);
    }

}
