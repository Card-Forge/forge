package forge.ai.ability;

import forge.ai.*;
import forge.card.MagicColor;
import forge.game.Game;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

public class ChooseColorAi extends SpellAbilityAi {

    @Override
    protected AiAbilityDecision checkApiLogic(Player ai, SpellAbility sa) {
        final Game game = ai.getGame();
        final String sourceName = ComputerUtilAbility.getAbilitySourceName(sa);
        final PhaseHandler ph = game.getPhaseHandler();

        if (!sa.hasParam("AILogic")) {
            return new AiAbilityDecision(0, AiPlayDecision.MissingLogic);
        }
        final String logic = sa.getParam("AILogic");

        if ("Nykthos, Shrine to Nyx".equals(sourceName)) {
            if (SpecialCardAi.NykthosShrineToNyx.consider(ai, sa)) {
                return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
            } else {
                return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            }
        }

        if ("Oona, Queen of the Fae".equals(sourceName)) {
            if (ph.isPlayerTurn(ai) || ph.getPhase().isBefore(PhaseType.COMBAT_DECLARE_ATTACKERS)) {
                return new AiAbilityDecision(0, AiPlayDecision.AnotherTime);
            }
            // Set PayX here to maximum value.
            sa.setXManaCostPaid(ComputerUtilCost.getMaxXValue(sa, ai, false));
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        }

        if ("Addle".equals(sourceName)) {
            // TODO Why is this not in the AI logic?
            // Why are we specifying the weakest opponent?
            if (!ph.getPhase().isBefore(PhaseType.COMBAT_DECLARE_ATTACKERS) && !ai.getWeakestOpponent().getCardsIn(ZoneType.Hand).isEmpty()) {
                return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
            } else {
                return new AiAbilityDecision(0, AiPlayDecision.AnotherTime);
            }
        }

        if (logic.equals("MostExcessOpponentControls")) {
            for (byte color : MagicColor.WUBRG) {
                CardCollectionView ailist = ai.getColoredCardsInPlay(color);
                CardCollectionView opplist = ai.getStrongestOpponent().getColoredCardsInPlay(color);

                int excess = ComputerUtilCard.evaluatePermanentList(opplist) - ComputerUtilCard.evaluatePermanentList(ailist);
                if (excess > 4) {
                    return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
                }
            }
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        } else if (logic.equals("MostProminentInComputerDeck")) {
            if ("Astral Cornucopia".equals(sourceName)) {
                // activate in Main 2 hoping that the extra mana surplus will make a difference
                // if there are some nonland permanents in hand
                CardCollectionView permanents = CardLists.filter(ai.getCardsIn(ZoneType.Hand), 
                        CardPredicates.NONLAND_PERMANENTS);

                if (!permanents.isEmpty() && ph.is(PhaseType.MAIN2, ai)) {
                    return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
                } else {
                    return new AiAbilityDecision(0, AiPlayDecision.WaitForMain2);
                }
            }
        } else if (logic.equals("HighestDevotionToColor")) {
            // currently only works more or less reliably in Main2 to cast own spells
            if (!ph.is(PhaseType.MAIN2, ai)) {
                return new AiAbilityDecision(0, AiPlayDecision.WaitForMain2);
            }
        }

        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    @Override
    protected AiAbilityDecision doTriggerNoCost(Player ai, SpellAbility sa, boolean mandatory) {
        if (mandatory) {
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        }
        return canPlay(ai, sa);
    }

}
