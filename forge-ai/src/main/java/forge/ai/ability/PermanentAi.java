package forge.ai.ability;

import forge.ai.ComputerUtil;
import forge.ai.ComputerUtilCost;
import forge.ai.ComputerUtilMana;
import forge.ai.SpellAbilityAi;
import forge.card.CardType.Supertype;
import forge.card.mana.ManaCost;
import forge.game.Game;
import forge.game.GlobalRuleChange;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.cost.Cost;
import forge.game.mana.ManaCostBeingPaid;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

public class PermanentAi extends SpellAbilityAi {

    /**
     * Checks if the AI will play a SpellAbility based on its phase restrictions
     */
    @Override
    protected boolean checkPhaseRestrictions(final Player ai, final SpellAbility sa, final PhaseHandler ph) {

        final Card card = sa.getHostCard();

        if (card.hasStartOfKeyword("You may cast CARDNAME as though it had flash. If") && !ai.couldCastSorcery(sa)) {
            // AiPlayDecision.AnotherTime
            return false;
        }

        // Wait for Main2 if possible
        if (ph.is(PhaseType.MAIN1) && ph.isPlayerTurn(ai) && !ComputerUtil.castPermanentInMain1(ai, sa)) {
            return false;
        }
        return true;
    }

    /**
     * The rest of the logic not covered by the canPlayAI template is defined
     * here
     */
    @Override
    protected boolean checkApiLogic(final Player ai, final SpellAbility sa) {

        final Card card = sa.getHostCard();
        final Game game = ai.getGame();

        // check on legendary
        if (card.getType().isLegendary()
                && !game.getStaticEffects().getGlobalRuleChange(GlobalRuleChange.noLegendRule)) {
            if (ai.isCardInPlay(card.getName())) {
                // AiPlayDecision.WouldDestroyLegend
                return false;
            }
        }
        if (card.isPlaneswalker()) {
            CardCollection list = CardLists.filter(ai.getCardsIn(ZoneType.Battlefield),
                    CardPredicates.Presets.PLANEWALKERS);
            for (String type : card.getType().getSubtypes()) { // determine
                                                               // planewalker
                                                               // subtype
                final CardCollection cl = CardLists.getType(list, type);
                if (!cl.isEmpty()) {
                    // AiPlayDecision.WouldDestroyOtherPlaneswalker
                    return false;
                }
                break;
            }
        }

        if (card.getType().hasSupertype(Supertype.World)) {
            CardCollection list = CardLists.getType(ai.getCardsIn(ZoneType.Battlefield), "World");
            if (!list.isEmpty()) {
                // AiPlayDecision.WouldDestroyWorldEnchantment
                return false;
            }
        }

        ManaCost mana = sa.getPayCosts().getTotalMana();
        if (mana.countX() > 0) {
            // Set PayX here to maximum value.
            final int xPay = ComputerUtilMana.determineLeftoverMana(sa, ai);
            final Card source = sa.getHostCard();
            if (source.hasConverge()) {
                card.setSVar("PayX", Integer.toString(0));
                int nColors = ComputerUtilMana.getConvergeCount(sa, ai);
                for (int i = 1; i <= xPay; i++) {
                    card.setSVar("PayX", Integer.toString(i));
                    int newColors = ComputerUtilMana.getConvergeCount(sa, ai);
                    if (newColors > nColors) {
                        nColors = newColors;
                    } else {
                        card.setSVar("PayX", Integer.toString(i - 1));
                        break;
                    }
                }
            } else {
                // AiPlayDecision.CantAffordX
                if (xPay <= 0) {
                    return false;
                }
                card.setSVar("PayX", Integer.toString(xPay));
            }
        } else if (mana.isZero()) {
            // if mana is zero, but card mana cost does have X, then something
            // is wrong
            ManaCost cardCost = card.getManaCost();
            if (cardCost != null && cardCost.countX() > 0) {
                // AiPlayDecision.CantPlayAi
                return false;
            }
        }

        if (sa.hasParam("Announce") && sa.getParam("Announce").startsWith("Multikicker")) {
            // String announce = sa.getParam("Announce");
            ManaCost mkCost = sa.getMultiKickerManaCost();
            ManaCost mCost = sa.getPayCosts().getTotalMana();
            for (int i = 0; i < 10; i++) {
                mCost = ManaCost.combine(mCost, mkCost);
                ManaCostBeingPaid mcbp = new ManaCostBeingPaid(mCost);
                if (!ComputerUtilMana.canPayManaCost(mcbp, sa, ai)) {
                    card.setKickerMagnitude(i);
                    break;
                }
                card.setKickerMagnitude(i + 1);
            }
        }

        // don't play cards without being able to pay the upkeep for
        for (String ability : card.getKeywords()) {
            if (ability.startsWith("At the beginning of your upkeep, sacrifice CARDNAME unless you pay")) {
                final String[] k = ability.split(" pay ");
                final String costs = k[1].replaceAll("[{]", "").replaceAll("[}]", " ");

                final SpellAbility emptyAbility = new SpellAbility.EmptySa(card, ai);
                emptyAbility.setPayCosts(new Cost(costs, true));
                emptyAbility.setTargetRestrictions(sa.getTargetRestrictions());

                emptyAbility.setActivatingPlayer(ai);
                if (!ComputerUtilCost.canPayCost(emptyAbility, ai)) {
                    // AiPlayDecision.AnotherTime
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    protected boolean doTriggerAINoCost(Player ai, SpellAbility sa, boolean mandatory) {
        final Card source = sa.getHostCard();
        final Cost cost = sa.getPayCosts();

        if (sa.getConditions() != null && !sa.getConditions().areMet(sa)) {
            return false;
        }

        if (sa.hasParam("AILogic") && !checkAiLogic(ai, sa, sa.getParam("AILogic"))) {
            return false;
        }
        if (cost != null && !willPayCosts(ai, sa, cost, source)) {
            return false;
        }
        if (!checkPhaseRestrictions(ai, sa, ai.getGame().getPhaseHandler())) {
            return false;
        }
        return checkApiLogic(ai, sa);
    }

}
