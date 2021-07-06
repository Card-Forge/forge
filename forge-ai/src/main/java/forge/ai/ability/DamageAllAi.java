package forge.ai.ability;

import com.google.common.base.Predicate;

import forge.ai.ComputerUtilCard;
import forge.ai.ComputerUtilCombat;
import forge.ai.ComputerUtilCost;
import forge.ai.ComputerUtilMana;
import forge.ai.SpellAbilityAi;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.cost.Cost;
import forge.game.keyword.Keyword;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import forge.game.zone.ZoneType;
import forge.util.MyRandom;

public class  DamageAllAi extends SpellAbilityAi {
    @Override
    protected boolean canPlayAI(Player ai, SpellAbility sa) {
        // AI needs to be expanded, since this function can be pretty complex
        // based on what the expected targets could be
        final Card source = sa.getHostCard();

        // prevent run-away activations - first time will always return true
        if (MyRandom.getRandom().nextFloat() > Math.pow(.9, sa.getActivationsThisTurn())) {
            return false;
        }
        // abCost stuff that should probably be centralized...
        final Cost abCost = sa.getPayCosts();
        if (abCost != null) {
            // AI currently disabled for some costs
            if (!ComputerUtilCost.checkLifeCost(ai, abCost, source, 4, sa)) {
                return false;
            }
        }
        // wait until stack is empty (prevents duplicate kills)
        if (!ai.getGame().getStack().isEmpty()) {
            return false;
        }

        int x = -1;
        final String damage = sa.getParam("NumDmg");
        int dmg = AbilityUtils.calculateAmount(source, damage, sa);
        if (damage.equals("X") && sa.getSVar(damage).equals("Count$Converge")) {
        	dmg = ComputerUtilMana.getConvergeCount(sa, ai);
        }
        if (damage.equals("X") && sa.getSVar(damage).equals("Count$xPaid")) {
            x = ComputerUtilCost.getMaxXValue(sa, ai);
        }
        if (x == -1) {
            if (determineOppToKill(ai, sa, source, dmg) != null) {
                // we already know we can kill a player, so go for it
                return true;
            }
            // look for other value in this (damaging creatures or
            // creatures + player, e.g. Pestilence, etc.)
            return evaluateDamageAll(ai, sa, source, dmg) > 0;
        } else {
            int best = -1, best_x = -1;
            Player bestOpp = determineOppToKill(ai, sa, source, x);
            if (bestOpp != null) {
                // we can finish off a player, so go for it

                // TODO: improve this by possibly damaging more creatures
                // on the battlefield belonging to other opponents at the same
                // time, if viable
                best_x = bestOpp.getLife();
            } else {
                // see if it's possible to get value from killing off creatures
                for (int i = 0; i <= x; i++) {
                    final int value = evaluateDamageAll(ai, sa, source, i);
                    if (value > best) {
                        best = value;
                        best_x = i;
                    }
                }
            }

            if (best_x > 0) {
                if (sa.getSVar(damage).equals("Count$xPaid")) {
                    sa.setXManaCostPaid(best_x);
                }
                return true;
            }
            return false;
        }
    }

    private Player determineOppToKill(Player ai, SpellAbility sa, Card source, int x) {
        // Attempt to determine which opponent can be finished off such that the most players
        // are killed at the same time, given X damage tops
        final String validP = sa.hasParam("ValidPlayers") ? sa.getParam("ValidPlayers") : "";
        int aiLife = ai.getLife();
        Player bestOpp = null; // default opponent, if all else fails

        for (int dmg = 1; dmg <= x; dmg++) {
            // Don't kill yourself in the process
            if (validP.equals("Player") && aiLife <= ComputerUtilCombat.predictDamageTo(ai, dmg, source, false)) {
                break;
            }
            for (Player opp : ai.getOpponents()) {
                if ((validP.equals("Player") || validP.contains("Opponent"))
                        && (opp.getLife() <= ComputerUtilCombat.predictDamageTo(opp, dmg, source, false))) {
                    bestOpp = opp;
                }
            }
        }

        return bestOpp;
    }

    private int evaluateDamageAll(Player ai, SpellAbility sa, final Card source, int dmg) {
        final Player opp = ai.getWeakestOpponent();
        final CardCollection humanList = getKillableCreatures(sa, opp, dmg);
        CardCollection computerList = getKillableCreatures(sa, ai, dmg);

        final TargetRestrictions tgt = sa.getTargetRestrictions();
        if (tgt != null && sa.canTarget(opp)) {
            sa.resetTargets();
            sa.getTargets().add(opp);
            computerList.clear();
        }

        final String validP = sa.hasParam("ValidPlayers") ? sa.getParam("ValidPlayers") : "";
        // TODO: if damage is dependant on mana paid, maybe have X be human's max life
        // Don't kill yourself
        if (validP.equals("Player") && (ai.getLife() <= ComputerUtilCombat.predictDamageTo(ai, dmg, source, false))) {
            return -1;
        }

        int minGain = 200; // The minimum gain in destroyed creatures
        if (sa.getPayCosts().isReusuableResource()) {
            if (computerList.isEmpty()) {
                minGain = 10; // nothing to lose
                // no creatures to lose and player can be damaged
                // so do it if it's helping!
                // ----------------------------
                // needs future improvement on pestilence :
                // what if we lose creatures but can win by repeated activations?
                // that tactic only works if there are creatures left to keep pestilence in play
                // and can kill the player in a reasonable amount of time (no more than 2-3 turns?)
                if (validP.equals("Player")) {
                    if (ComputerUtilCombat.predictDamageTo(opp, dmg, source, false) > 0) {
                        // When using Pestilence to hurt players, do it at
                        // the end of the opponent's turn only
                        if ((!"DmgAllCreaturesAndPlayers".equals(sa.getParam("AILogic")))
                                || ((ai.getGame().getPhaseHandler().is(PhaseType.END_OF_TURN)
                                && (ai.getGame().getNonactivePlayers().contains(ai)))))
                        // Need further improvement : if able to kill immediately with repeated activations, do not wait
                        // for phases! Will also need to implement considering repeated activations for killed creatures!
                        // || (ai.sa.getPayCosts(). ??? )
                        {
                            // would take zero damage, and hurt opponent, do it!
                            if (ComputerUtilCombat.predictDamageTo(ai, dmg, source, false)<1) {
                                return 1;
                            }
                            // enemy is expected to die faster than AI from damage if repeated
                            if (ai.getLife() > ComputerUtilCombat.predictDamageTo(ai, dmg, source, false)
                                    * ((opp.getLife() + ComputerUtilCombat.predictDamageTo(opp, dmg, source, false) - 1)
                                    / ComputerUtilCombat.predictDamageTo(opp, dmg, source, false))) {
                                // enemy below 10 life, go for it!
                                if ((opp.getLife() < 10)
                                        && (ComputerUtilCombat.predictDamageTo(opp, dmg, source, false) >= 1)) {
                                    return 1;
                                }
                                // At least half enemy remaining life can be removed in one go
                                // worth doing even if enemy still has high health - one more copy of spell to win!
                                if (opp.getLife() <= 2 * ComputerUtilCombat.predictDamageTo(opp, dmg, source, false)) {
                                    return 1;
                                }
                            }
                        }
                    }
                }
            } else {
                minGain = 100; // safety for errors in evaluate creature
            }
        } else if (sa.getSubAbility() != null && ai.getGame().getPhaseHandler().isPreCombatMain() && computerList.isEmpty()
                && opp.getCreaturesInPlay().size() > 1 && !ai.getCreaturesInPlay().isEmpty()) {
            minGain = 126; // prepare for attack
        }

        return ComputerUtilCard.evaluateCreatureList(humanList) - ComputerUtilCard.evaluateCreatureList(computerList)
                - minGain;
    }

    @Override
    public boolean chkAIDrawback(SpellAbility sa, Player ai) {
        final Card source = sa.getHostCard();
        String validP = "";

        final String damage = sa.getParam("NumDmg");
        int dmg;
        if (damage.equals("X") && sa.getSVar(damage).equals("Count$xPaid")) {
            // Set PayX here to maximum value.
            dmg = ComputerUtilCost.getMaxXValue(sa, ai);
            sa.setXManaCostPaid(dmg);
        } else {
            dmg = AbilityUtils.calculateAmount(source, damage, sa);
        }

        if (sa.hasParam("ValidPlayers")) {
            validP = sa.getParam("ValidPlayers");
        }

        // Evaluate creatures getting killed
        Player enemy = ai.getWeakestOpponent();
        final CardCollection humanList = getKillableCreatures(sa, enemy, dmg);
        CardCollection computerList = getKillableCreatures(sa, ai, dmg);
        final TargetRestrictions tgt = sa.getTargetRestrictions();

        if (tgt != null && sa.canTarget(enemy)) {
            sa.resetTargets();
            sa.getTargets().add(enemy);
            computerList.clear();
        }
        // Don't get yourself killed
        if (validP.equals("Player") && (ai.getLife() <= ComputerUtilCombat.predictDamageTo(ai, dmg, source, false))) {
            return false;
        }

        // if we can kill human, do it
        if ((validP.equals("Player") || validP.equals("Opponent") || validP.contains("Targeted"))
                && (enemy.getLife() <= ComputerUtilCombat.predictDamageTo(enemy, dmg, source, false))) {
            return true;
        }

        if (!computerList.isEmpty() && ComputerUtilCard.evaluateCreatureList(computerList) > ComputerUtilCard
                .evaluateCreatureList(humanList)) {
            return false;
        }

        return true;
    }

    /**
     * <p>
     * getKillableCreatures.
     * </p>
     *
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @param player
     *            a {@link forge.game.player.Player} object.
     * @param dmg
     *            a int.
     * @return a {@link forge.game.card.CardCollection} object.
     */
    private CardCollection getKillableCreatures(final SpellAbility sa, final Player player, final int dmg) {
        final Card source = sa.getHostCard();
        String validC = sa.hasParam("ValidCards") ? sa.getParam("ValidCards") : "";

        // TODO: X may be something different than X paid
        CardCollection list =
                CardLists.getValidCards(player.getCardsIn(ZoneType.Battlefield), validC.split(","), source.getController(), source, sa);

        final Predicate<Card> filterKillable = new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                return (ComputerUtilCombat.predictDamageTo(c, dmg, source, false) >= ComputerUtilCombat.getDamageToKill(c));
            }
        };

        list = CardLists.getNotKeyword(list, Keyword.INDESTRUCTIBLE);
        list = CardLists.filter(list, filterKillable);

        return list;
    }

    @Override
    protected boolean doTriggerAINoCost(Player ai, SpellAbility sa, boolean mandatory) {
        final Card source = sa.getHostCard();
        String validP = "";

        final String damage = sa.getParam("NumDmg");
        int dmg;

        if (damage.equals("X") && sa.getSVar(damage).equals("Count$xPaid")) {
            // Set PayX here to maximum value.
            dmg = ComputerUtilCost.getMaxXValue(sa, ai);
            sa.setXManaCostPaid(dmg);
        } else {
            dmg = AbilityUtils.calculateAmount(source, damage, sa);
        }

        if (sa.hasParam("ValidPlayers")) {
            validP = sa.getParam("ValidPlayers");
        }

        // Evaluate creatures getting killed
        Player enemy = ai.getWeakestOpponent();
        final CardCollection humanList = getKillableCreatures(sa, enemy, dmg);
        CardCollection computerList = getKillableCreatures(sa, ai, dmg);
        final TargetRestrictions tgt = sa.getTargetRestrictions();

        if (tgt != null && sa.canTarget(enemy)) {
            sa.resetTargets();
            sa.getTargets().add(enemy);
            computerList.clear();
        }

        // If it's not mandatory check a few things
        if (mandatory) {
            return true;
        }
        // Don't get yourself killed
        if (validP.equals("Player") && (ai.getLife() <= ComputerUtilCombat.predictDamageTo(ai, dmg, source, false))) {
            return false;
        }

        // if we can kill human, do it
        if ((validP.equals("Player") || validP.contains("Opponent") || validP.contains("Targeted"))
                && (enemy.getLife() <= ComputerUtilCombat.predictDamageTo(enemy, dmg, source, false))) {
            return true;
        }

        if (!computerList.isEmpty() && ComputerUtilCard.evaluateCreatureList(computerList) + 50 >= ComputerUtilCard
                .evaluateCreatureList(humanList)) {
            return false;
        }

        return true;
    }
}
