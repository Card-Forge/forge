package forge.ai.ability;

import com.google.common.base.Predicates;
import forge.ai.ComputerUtilMana;
import forge.ai.SpellAbilityAi;
import forge.game.ability.ApiType;
import forge.game.card.*;
import forge.game.card.Card.SplitCMCMode;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.MyRandom;

public class ScryAi extends SpellAbilityAi {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#doTriggerAINoCost(forge.game.player.Player, java.util.Map, forge.card.spellability.SpellAbility, boolean)
     */
    @Override
    protected boolean doTriggerAINoCost(Player ai, SpellAbility sa, boolean mandatory) {

        if (sa.usesTargeting()) { // It doesn't appear that Scry ever targets
            // ability is targeted
            sa.resetTargets();

            sa.getTargets().add(ai);
        }

        return true;
    } // scryTargetAI()

    @Override
    public boolean chkAIDrawback(SpellAbility sa, Player ai) {
        return doTriggerAINoCost(ai, sa, false);
    }
    
    /**
     * Checks if the AI will play a SpellAbility based on its phase restrictions
     */
    @Override
    protected boolean checkPhaseRestrictions(final Player ai, final SpellAbility sa, final PhaseHandler ph) {
        // if the Scry ability requires tapping and has a mana cost, it's best done at the end of opponent's turn
        // and right before the beginning of AI's turn, if possible, to avoid mana locking the AI and also to
        // try to scry right before drawing a card. Also, avoid tapping creatures in the AI's turn, if possible,
        // even if there's no mana cost.
        if (sa.getPayCosts().hasTapCost()
                && (sa.getPayCosts().hasManaCost() || (sa.getHostCard() != null && sa.getHostCard().isCreature()))
                && !SpellAbilityAi.isSorcerySpeed(sa)) {
            return ph.getNextTurn() == ai && ph.is(PhaseType.END_OF_TURN);
        }

        // AI logic to scry in Main 1 if there is no better option, otherwise scry at opponent's EOT
        // (e.g. Glimmer of Genius)
        if ("BestOpportunity".equals(sa.getParam("AILogic"))) {
            return doBestOpportunityLogic(ai, sa, ph);
        }

        // in the playerturn Scry should only be done in Main1 or in upkeep if able
        if (ph.isPlayerTurn(ai)) {
            if (SpellAbilityAi.isSorcerySpeed(sa)) {
                return ph.is(PhaseType.MAIN1) || sa.hasParam("Planeswalker");
            } else {
                return ph.is(PhaseType.UPKEEP);
            }
        }
        return true;
    }

    private boolean doBestOpportunityLogic(Player ai, SpellAbility sa, PhaseHandler ph) {
        // Check to see if there are any cards in hand that may be worth casting
        boolean hasSomethingElse = false;
        for (Card c : CardLists.filter(ai.getCardsIn(ZoneType.Hand), Predicates.not(CardPredicates.Presets.LANDS))) {
            for (SpellAbility ab : c.getAllSpellAbilities()) {
                if (ab.getPayCosts().hasManaCost()
                        && ComputerUtilMana.hasEnoughManaSourcesToCast(ab, ai)) {
                    // TODO: currently looks for non-Scry cards, can most certainly be made smarter.
                    if (ab.getApi() != ApiType.Scry) {
                        hasSomethingElse = true;
                        break;
                    }
                }
            }
        }

        return (!hasSomethingElse && ph.getPlayerTurn() == ai && ph.getPhase().isAfter(PhaseType.DRAW))
                || (ph.getNextTurn() == ai && ph.is(PhaseType.END_OF_TURN));
    }

    /**
     * Checks if the AI will play a SpellAbility with the specified AiLogic
     */
    @Override
    protected boolean checkAiLogic(final Player ai, final SpellAbility sa, final String aiLogic) {
        if ("Never".equals(aiLogic)) {
            return false;
        } else if ("BrainJar".equals(aiLogic)) {
            final Card source = sa.getHostCard();
            
            int counterNum = source.getCounters(CounterEnumType.CHARGE);
            // no need for logic
            if (counterNum == 0) {
                return false;
            }
            int libsize = ai.getCardsIn(ZoneType.Library).size();
            
            final CardCollection hand = CardLists.filter(ai.getCardsIn(ZoneType.Hand), Predicates.or( 
                    CardPredicates.isType("Instant"), CardPredicates.isType("Sorcery")));
            if (!hand.isEmpty()) {
                // has spell that can be cast in hand with put ability
                if (!CardLists.filter(hand, CardPredicates.hasCMC(counterNum + 1)).isEmpty()) {
                    return false;
                }
                // has spell that can be cast if one counter is removed
                if (!CardLists.filter(hand, CardPredicates.hasCMC(counterNum)).isEmpty()) {
                    sa.setSVar("ChosenX", "Number$1");
                    return true;
                }
            }
            final CardCollection library = CardLists.filter(ai.getCardsIn(ZoneType.Library), Predicates.or( 
                    CardPredicates.isType("Instant"), CardPredicates.isType("Sorcery")));
            if (!library.isEmpty()) {
                // get max cmc of instant or sorceries in the libary 
                int maxCMC = 0;
                for (final Card c : library) {
                    int v = c.getCMC(); 
                    if (c.isSplitCard()) {
                        v = Math.max(c.getCMC(SplitCMCMode.LeftSplitCMC), c.getCMC(SplitCMCMode.RightSplitCMC));
                    }
                    if (v > maxCMC) {
                        maxCMC = v;
                    }
                }
                // there is a spell with more CMC, no need to remove counter
                if (counterNum + 1 < maxCMC) {
                    return false;
                }
                int maxToRemove = counterNum - maxCMC + 1;
                // no Scry 0, even if its catched from later stuff 
                if (maxToRemove <= 0) {
                	return false;
                }
                sa.setSVar("ChosenX", "Number$" + maxToRemove);
            } else {
                // no Instant or Sorceries anymore, just scry
                sa.setSVar("ChosenX", "Number$" + Math.min(counterNum, libsize));
            }
        }
        return true;
    }
    
    @Override
    protected boolean checkApiLogic(Player ai, SpellAbility sa) {
        // does Scry make sense with no Library cards?
        if (ai.getCardsIn(ZoneType.Library).isEmpty()) {
            return false;
        }

        double chance = .4; // 40 percent chance of milling with instant speed
                            // stuff
        if (SpellAbilityAi.isSorcerySpeed(sa)) {
            chance = .667; // 66.7% chance for sorcery speed (since it will
                           // never activate EOT)
        }
        boolean randomReturn = MyRandom.getRandom().nextFloat() <= Math.pow(chance, sa.getActivationsThisTurn() + 1);

        if (SpellAbilityAi.playReusable(ai, sa)) {
            randomReturn = true;
        }

        return randomReturn;
    }

    @Override
    public boolean confirmAction(Player player, SpellAbility sa, PlayerActionConfirmMode mode, String message) {
        return true;
    }
}
