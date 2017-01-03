package forge.ai.ability;

import com.google.common.base.Predicates;
import forge.ai.AiPlayDecision;
import forge.ai.ComputerUtil;
import forge.ai.ComputerUtilAbility;
import forge.ai.ComputerUtilCard;
import forge.ai.ComputerUtilMana;
import forge.ai.PlayerControllerAi;
import forge.ai.SpellAbilityAi;
import forge.card.MagicColor;
import forge.card.mana.ManaCost;
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
import java.util.List;

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

        if ("Nykthos, Shrine to Nyx".equals(source.getName())) {
            PhaseHandler ph = game.getPhaseHandler();
            if (!ph.isPlayerTurn(ai) || ph.getPhase().isBefore(PhaseType.MAIN2)) {
                // TODO: currently limited to Main 2, somehow improve to let the AI use Nykthos at other time?
                return false;
            }
            String prominentColor = ComputerUtilCard.getMostProminentColor(ai.getCardsIn(ZoneType.Battlefield));
            int devotion = CardFactoryUtil.xCount(source, "Count$Devotion." + prominentColor);

            // do not use Nykthos if devotion to most prominent color is less than 4 (since {2} is paid to activate Nykthos, and Nykthos itself is tapped too)
            if (devotion < 4) {
                return false;
            }

            final CardCollectionView cards = ai.getCardsIn(new ZoneType[] {ZoneType.Hand, ZoneType.Battlefield, ZoneType.Command});
            List<SpellAbility> all = ComputerUtilAbility.getSpellAbilities(cards, ai);

            // TODO: this is inexact for the purpose, assuming that each land can serve as at least some form of mana source.
            // Improve somehow to only account for mana-producing lands and also possibly for other (non-land) mana sources.
            int numLands = CardLists.filter(ai.getCardsIn(ZoneType.Battlefield), Predicates.and(CardPredicates.Presets.LANDS, CardPredicates.Presets.UNTAPPED)).size();

            for (final SpellAbility testSa : ComputerUtilAbility.getOriginalAndAltCostAbilities(all, ai)) {
                ManaCost cost = testSa.getPayCosts().getTotalMana();
                byte colorProfile = cost.getColorProfile();
                
                if ((cost.getCMC() == 0)) {
                    // no mana cost, no need to tap Nykthos to activate it
                    continue;
                } else if (colorProfile != 0 && (cost.getColorProfile() & MagicColor.fromName(prominentColor)) == 0) {
                    // does not feature prominent color, won't be able to pay for it with Nykthos activated for this color
                    continue;
                } else if ((testSa.getPayCosts().getTotalMana().getCMC() > devotion + numLands - 3)) {
                    // the cost may be too high even if we tap Nykthos
                    continue;
                }

                if (testSa.getHostCard().getName().equals("Nykthos, Shrine to Nyx")) {
                    // prevent infinitely recursing Nykthos's own ability when testing AI play decision
                    continue;
                }

                testSa.setActivatingPlayer(ai);
                if (((PlayerControllerAi)ai.getController()).getAi().canPlaySa(testSa) == AiPlayDecision.WillPlay) {
                    // the AI is willing to play the spell
                    System.out.println("Willing to play " + testSa + " for " + testSa.getHostCard());
                    return true;
                }
            }

            return false; // haven't found anything to play with the excess generated mana
        }

        if ("Oona, Queen of the Fae".equals(source.getName())) {
            PhaseHandler ph = game.getPhaseHandler();
        	if (ph.isPlayerTurn(ai) || ph.getPhase().isBefore(PhaseType.COMBAT_DECLARE_ATTACKERS)) {
        		return false;
        	}
            // Set PayX here to maximum value.
            int x = ComputerUtilMana.determineLeftoverMana(sa, ai);
            source.setSVar("PayX", Integer.toString(x));
            return true;
        }
        
        if ("Addle".equals(source.getName())) {
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
