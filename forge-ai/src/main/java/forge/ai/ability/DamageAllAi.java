package forge.ai.ability;

import com.google.common.base.Predicate;

import forge.ai.*;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.card.CounterType;
import forge.game.cost.Cost;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import forge.game.zone.ZoneType;
import forge.util.MyRandom;

import java.util.Random;

public class  DamageAllAi extends SpellAbilityAi {
    @Override
    protected boolean canPlayAI(Player ai, SpellAbility sa) {
        // AI needs to be expanded, since this function can be pretty complex
        // based on what the expected targets could be
        final Card source = sa.getHostCard();

        // prevent run-away activations - first time will always return true
        final Random r = MyRandom.getRandom();
        if (r.nextFloat() > Math.pow(.9, sa.getActivationsThisTurn())) {
            return false;
        }
        // abCost stuff that should probably be centralized...
        final Cost abCost = sa.getPayCosts();
        if (abCost != null) {
            // AI currently disabled for some costs
            if (!ComputerUtilCost.checkLifeCost(ai, abCost, source, 4, null)) {
                return false;
            }
        }
        // wait until stack is empty (prevents duplicate kills)
        if (!ai.getGame().getStack().isEmpty()) {
            return false;
        }
        
        int x = -1;
        final String damage = sa.getParam("NumDmg");
        int dmg = AbilityUtils.calculateAmount(sa.getHostCard(), damage, sa);
        if (damage.equals("X") && sa.getSVar(damage).equals("Count$Converge")) {
        	dmg = ComputerUtilMana.getConvergeCount(sa, ai);
        }
        if (damage.equals("X") && sa.getSVar(damage).equals("Count$xPaid")) {
            x = ComputerUtilMana.determineLeftoverMana(sa, ai);
        }
        if (damage.equals("ChosenX")) {
            x = source.getCounters(CounterType.LOYALTY);
        }
        if (x == -1) {
            return evaluateDamageAll(ai, sa, source, dmg) > 0;
        } else {
            int best = -1, best_x = -1;
            for (int i = 0; i < x; i++) {
                final int value = evaluateDamageAll(ai, sa, source, i);
                if (value > best) {
                    best = value;
                    best_x = i;
                }
            }
            if (best_x > 0) {
                if (sa.getSVar(damage).equals("Count$xPaid")) {
                    source.setSVar("PayX", Integer.toString(best_x));
                }
                if (damage.equals("ChosenX")) {
                    source.setSVar("ChosenX", "Number$" + best_x);
                }
                return true;
            }
            return false;
        }
    }

    private int evaluateDamageAll(Player ai, SpellAbility sa, final Card source, int dmg) {
        Player opp = ai.getOpponent();
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

        // if we can kill human, do it
        if ((validP.equals("Player") || validP.contains("Opponent"))
                && (opp.getLife() <= ComputerUtilCombat.predictDamageTo(opp, dmg, source, false))) {
            return 1;
        }

        int minGain = 200; // The minimum gain in destroyed creatures
        if (sa.getPayCosts() != null && sa.getPayCosts().isReusuableResource()) {
        	if (computerList.isEmpty()) {
        		minGain = 10; // nothing to lose
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
        int dmg = AbilityUtils.calculateAmount(sa.getHostCard(), damage, sa);

        if (damage.equals("X") && sa.getSVar(damage).equals("Count$xPaid")) {
            // Set PayX here to maximum value.
            dmg = ComputerUtilMana.determineLeftoverMana(sa, ai);
            source.setSVar("PayX", Integer.toString(dmg));
        }

        if (sa.hasParam("ValidPlayers")) {
            validP = sa.getParam("ValidPlayers");
        }

        // Evaluate creatures getting killed
        Player enemy = ai.getOpponent();
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

        list = CardLists.getNotKeyword(list, "Indestructible");
        list = CardLists.filter(list, filterKillable);

        return list;
    }

    @Override
    protected boolean doTriggerAINoCost(Player ai, SpellAbility sa, boolean mandatory) {
        final Card source = sa.getHostCard();
        String validP = "";

        final String damage = sa.getParam("NumDmg");
        int dmg = AbilityUtils.calculateAmount(sa.getHostCard(), damage, sa);

        if (damage.equals("X") && sa.getSVar(damage).equals("Count$xPaid")) {
            // Set PayX here to maximum value.
            dmg = ComputerUtilMana.determineLeftoverMana(sa, ai);
            source.setSVar("PayX", Integer.toString(dmg));
        }

        if (sa.hasParam("ValidPlayers")) {
            validP = sa.getParam("ValidPlayers");
        }

        // Evaluate creatures getting killed
        Player enemy = ai.getOpponent();
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
