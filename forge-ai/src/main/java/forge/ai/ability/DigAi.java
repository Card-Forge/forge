package forge.ai.ability;

import java.util.Map;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

import forge.ai.AiAttackController;
import forge.ai.ComputerUtil;
import forge.ai.ComputerUtilAbility;
import forge.ai.ComputerUtilCard;
import forge.ai.ComputerUtilCost;
import forge.ai.SpecialCardAi;
import forge.ai.SpellAbilityAi;
import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.keyword.Keyword;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.TextUtil;


public class DigAi extends SpellAbilityAi {
    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#canPlayAI(forge.game.player.Player, java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected boolean canPlayAI(Player ai, SpellAbility sa) {
        final Game game = ai.getGame();
        Player opp = AiAttackController.choosePreferredDefenderPlayer(ai);
        final Card host = sa.getHostCard();
        Player libraryOwner = ai;

        if (!willPayCosts(ai, sa, sa.getPayCosts(), host)) {
            return false;
        }

        if (sa.usesTargeting()) {
            sa.resetTargets();
            if (!opp.canBeTargetedBy(sa)) {
                return false;
            } else {
                sa.getTargets().add(opp);
            }
            libraryOwner = opp;
        }

        // return false if nothing to dig into
        if (libraryOwner.getCardsIn(ZoneType.Library).isEmpty()) {
            return false;
        }

        if ("Never".equals(sa.getParam("AILogic"))) {
            return false;
        } else if ("AtOppEndOfTurn".equals(sa.getParam("AILogic"))) {
            if (!(game.getPhaseHandler().getNextTurn() == ai && game.getPhaseHandler().is(PhaseType.END_OF_TURN))) {
                return false;
            }
        }

        // don't deck yourself
        if (sa.hasParam("DestinationZone2") && !"Library".equals(sa.getParam("DestinationZone2"))) {
            int numToDig = AbilityUtils.calculateAmount(host, sa.getParam("DigNum"), sa);
            if (libraryOwner == ai && ai.getCardsIn(ZoneType.Library).size() <= numToDig + 2) {
                return false;
            }
        }

        // Don't use draw abilities before main 2 if possible
        if (game.getPhaseHandler().getPhase().isBefore(PhaseType.MAIN2) && !sa.hasParam("ActivationPhases")
                && !sa.hasParam("DestinationZone") && !ComputerUtil.castSpellInMain1(ai, sa)) {
            return false;
        }

        final String num = sa.getParam("DigNum");
        final boolean payXLogic = sa.hasParam("AILogic") && sa.getParam("AILogic").startsWith("PayX");
        if (num != null && (num.equals("X") && sa.getSVar(num).equals("Count$xPaid")) || payXLogic) {
            // By default, set PayX here to maximum value.
            SpellAbility root = sa.getRootAbility();
            if (root.getXManaCostPaid() == null) {
                int manaToSave = 0;

                // Special logic that asks the AI to conserve a certain amount of mana when paying X
                if (sa.hasParam("AILogic") && sa.getParam("AILogic").startsWith("PayXButSaveMana")) {
                    manaToSave = Integer.parseInt(TextUtil.split(sa.getParam("AILogic"), '.')[1]);
                }

                int numCards = ComputerUtilCost.getMaxXValue(sa, ai) - manaToSave;
                if (numCards <= 0) {
                    return false;
                }
                root.setXManaCostPaid(numCards);
            }
        }

        if (SpellAbilityAi.playReusable(ai, sa)) {
            return true;
        }

        if ((!game.getPhaseHandler().getNextTurn().equals(ai)
                || game.getPhaseHandler().getPhase().isBefore(PhaseType.END_OF_TURN))
            && !sa.hasParam("PlayerTurn") && !SpellAbilityAi.isSorcerySpeed(sa)
            && (ai.getCardsIn(ZoneType.Hand).size() > 1 || game.getPhaseHandler().getPhase().isBefore(PhaseType.DRAW))
            && !ComputerUtil.activateForCost(sa, ai)) {
        	return false;
        }

        if ("MadSarkhanDigDmg".equals(sa.getParam("AILogic"))) {
            return SpecialCardAi.SarkhanTheMad.considerDig(ai, sa);
        }
        
        return !ComputerUtil.preventRunAwayActivations(sa);
    }

    @Override
    protected boolean doTriggerAINoCost(Player ai, SpellAbility sa, boolean mandatory) {
        final SpellAbility root = sa.getRootAbility();
        final Player opp = AiAttackController.choosePreferredDefenderPlayer(ai);
        if (sa.usesTargeting()) {
            sa.resetTargets();
            if (mandatory && sa.canTarget(opp)) {
                sa.getTargets().add(opp);
            } else if (mandatory && sa.canTarget(ai)) {
                sa.getTargets().add(ai);
            }
        }

        // Triggers that ask to pay {X} (e.g. Depala, Pilot Exemplar).
        if (sa.hasParam("AILogic") && sa.getParam("AILogic").startsWith("PayXButSaveMana")) {
            int manaToSave = Integer.parseInt(TextUtil.split(sa.getParam("AILogic"), '.')[1]);
            int numCards = ComputerUtilCost.getMaxXValue(sa, ai) - manaToSave;
            if (numCards <= 0) {
                return mandatory;
            }
            root.setXManaCostPaid(numCards);
        }

        return true;
    }
    
    @Override
    public Card chooseSingleCard(Player ai, SpellAbility sa, Iterable<Card> valid, boolean isOptional, Player relatedPlayer, Map<String, Object> params) {
        if ("DigForCreature".equals(sa.getParam("AILogic"))) {
            Card bestChoice = ComputerUtilCard.getBestCreatureAI(valid);
            if (bestChoice == null) {
                // no creatures, but maybe there's a morphable card that can be played as a creature?
                CardCollection morphs = CardLists.filter(valid, new Predicate<Card>() {
                    @Override
                    public boolean apply(Card card) {
                        return card.hasKeyword(Keyword.MORPH);
                    }
                });
                if (!morphs.isEmpty()) {
                    bestChoice = ComputerUtilCard.getBestAI(morphs);
                }
            }

            // still nothing, so return the worst card since it'll be unplayable from exile (e.g. Vivien, Champion of the Wilds)
            return bestChoice != null ? bestChoice : ComputerUtilCard.getWorstAI(valid);
        }

        if (sa.getActivatingPlayer().isOpponentOf(ai) && relatedPlayer.isOpponentOf(ai)) {
            return ComputerUtilCard.getWorstPermanentAI(valid, false, true, false, false);
        } else {
            return ComputerUtilCard.getBestAI(valid);
        }
    }

    /* (non-Javadoc)
     * @see forge.card.ability.SpellAbilityAi#chooseSinglePlayer(forge.game.player.Player, forge.card.spellability.SpellAbility, java.util.List)
     */
    @Override
    public Player chooseSinglePlayer(Player ai, SpellAbility sa, Iterable<Player> options, Map<String, Object> params) {
        // an opponent choose a card from
        return Iterables.getFirst(options, null);
    }

    /* (non-Javadoc)
     * @see forge.card.ability.SpellAbilityAi#confirmAction(forge.card.spellability.SpellAbility, forge.game.player.PlayerActionConfirmMode, java.lang.String)
     */
    @Override
    public boolean confirmAction(Player player, SpellAbility sa, PlayerActionConfirmMode mode, String message, Map<String, Object> params) {
        Card topc = player.getZone(ZoneType.Library).get(0);

        // AI actions for individual cards (until this AI can be generalized)
        if (sa.getHostCard() != null) {
            if (ComputerUtilAbility.getAbilitySourceName(sa).equals("Explorer's Scope")) {
                // for Explorer's Scope, always put a land on the battlefield tapped
                // (TODO: might not always be a good idea, e.g. when a land ETBing can have detrimental effects)
                return true;
            } else if ("AlwaysConfirm".equals(sa.getParam("AILogic"))) {
                return true;
            }
        }

        // looks like perfect code for Delver of Secrets, but what about other cards? 
        return topc.isInstant() || topc.isSorcery();
    }
}
