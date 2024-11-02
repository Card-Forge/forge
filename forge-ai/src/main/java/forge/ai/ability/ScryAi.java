package forge.ai.ability;

import java.util.Map;

import com.google.common.base.Predicates;

import forge.ai.ComputerUtilCost;
import forge.ai.ComputerUtilMana;
import forge.ai.SpecialCardAi;
import forge.ai.SpellAbilityAi;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
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
        if (sa.usesTargeting()) {
            // ability is targeted
            sa.resetTargets();

            if (sa.canTarget(ai)) {
                sa.getTargets().add(ai);
            } else {
                for (Player p : ai.getAllies()) {
                    if (sa.canTarget(p)) {
                        sa.getTargets().add(p);
                        break;
                    }
                }
                if (mandatory && !sa.isTargetNumberValid()) {
                    for (Player p : ai.getOpponents()) {
                        if (sa.canTarget(p)) {
                            sa.getTargets().add(p);
                            break;
                        }
                    }
                }
            }

            if ("X".equals(sa.getParam("ScryNum")) && sa.getSVar("X").equals("Count$xPaid")) {
                int xPay = ComputerUtilCost.getMaxXValue(sa, ai, sa.isTrigger());
                if (xPay == 0) {
                    return false;
                }
                sa.getRootAbility().setXManaCostPaid(xPay);
            }

            return mandatory || sa.isTargetNumberValid();
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
        String logic = sa.getParamOrDefault("AILogic", "");

        // For Brain in a Jar, avoid competing against the other ability in the opponent's EOT.
        if ("BrainJar".equals(logic)) {
            return ph.getPhase().isAfter(PhaseType.MAIN2);
        }

        // if the Scry ability requires tapping and has a mana cost, it's best done at the end of opponent's turn
        // and right before the beginning of AI's turn, if possible, to avoid mana locking the AI and also to
        // try to scry right before drawing a card. Also, avoid tapping creatures in the AI's turn, if possible,
        // even if there's no mana cost.
        if (logic.equals("AtOppEOT") || (sa.getPayCosts().hasTapCost()
                && (sa.getPayCosts().hasManaCost() || (sa.getHostCard() != null && sa.getHostCard().isCreature()))
                && !isSorcerySpeed(sa, ai))) {
            return ph.getNextTurn() == ai && ph.is(PhaseType.END_OF_TURN);
        }

        // AI logic to scry in Main 1 if there is no better option, otherwise scry at opponent's EOT
        // (e.g. Glimmer of Genius)
        if ("BestOpportunity".equals(logic)) {
            return doBestOpportunityLogic(ai, sa, ph);
        }

        // in the playerturn Scry should only be done in Main1 or in upkeep if able
        if (ph.isPlayerTurn(ai)) {
            if (isSorcerySpeed(sa, ai)) {
                return ph.is(PhaseType.MAIN1) || sa.isPwAbility();
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
            return SpecialCardAi.BrainInAJar.consider(ai, sa);
        } else if ("MultipleChoice".equals(aiLogic)) {
            return SpecialCardAi.MultipleChoice.consider(ai, sa);
        }
        return true;
    }
    
    @Override
    protected boolean checkApiLogic(Player ai, SpellAbility sa) {
        // does Scry make sense with no Library cards?
        if (ai.getCardsIn(ZoneType.Library).isEmpty()) {
            return false;
        }

        double chance = .4; // 40 percent chance of milling with instant speed stuff
        if (isSorcerySpeed(sa, ai)) {
            chance = .667; // 66.7% chance for sorcery speed (since it will never activate EOT)
        }
        boolean randomReturn = MyRandom.getRandom().nextFloat() <= Math.pow(chance, sa.getActivationsThisTurn() + 1);

        if (playReusable(ai, sa)) {
            randomReturn = true;
        }

        if (sa.usesTargeting()) {
            sa.resetTargets();
            if (sa.canTarget(ai)) {
                sa.getTargets().add(ai);
            } else {
                for (Player p : ai.getAllies()) {
                    if (sa.canTarget(p)) {
                        sa.getTargets().add(p);
                        break;
                    }
                }
            }
            randomReturn = sa.isTargetNumberValid();
        }

        if ("X".equals(sa.getParam("ScryNum")) && sa.getSVar("X").equals("Count$xPaid")) {
            int xPay = ComputerUtilCost.getMaxXValue(sa, ai, sa.isTrigger());
            if (xPay == 0) {
                return false;
            }
            sa.getRootAbility().setXManaCostPaid(xPay);
        }

        return randomReturn;
    }

    @Override
    public boolean confirmAction(Player player, SpellAbility sa, PlayerActionConfirmMode mode, String message, Map<String, Object> params) {
        return true;
    }
}
