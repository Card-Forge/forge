package forge.ai.ability;

import com.google.common.base.Predicate;

import forge.ai.*;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
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
        final Random r = MyRandom.getRandom();
        final Cost abCost = sa.getPayCosts();
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

        Player opp = ai.getOpponent();
        final CardCollection humanList = getKillableCreatures(sa, opp, dmg);
        CardCollection computerList = getKillableCreatures(sa, ai, dmg);

        final TargetRestrictions tgt = sa.getTargetRestrictions();
        if (tgt != null && sa.canTarget(opp)) {
            sa.resetTargets();
            sa.getTargets().add(opp);
            computerList.clear();
        }

        // abCost stuff that should probably be centralized...
        if (abCost != null) {
            // AI currently disabled for some costs
            if (!ComputerUtilCost.checkLifeCost(ai, abCost, source, 4, null)) {
                return false;
            }
        }

        // TODO: if damage is dependant on mana paid, maybe have X be human's max life
        // Don't kill yourself
        if (validP.contains("Each") && (ai.getLife() <= ComputerUtilCombat.predictDamageTo(ai, dmg, source, false))) {
            return false;
        }

        // prevent run-away activations - first time will always return true
        if (r.nextFloat() > Math.pow(.9, sa.getActivationsThisTurn())) {
            return false;
        }

        // if we can kill human, do it
        if ((validP.contains("Each") || validP.contains("EachOpponent"))
                && (opp.getLife() <= ComputerUtilCombat.predictDamageTo(opp, dmg, source, false))) {
            return true;
        }

        // wait until stack is empty (prevents duplicate kills)
        if (!ai.getGame().getStack().isEmpty()) {
            return false;
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

        // evaluate both lists and pass only if human creatures are more valuable
        if ((ComputerUtilCard.evaluateCreatureList(computerList) + minGain) >= ComputerUtilCard
                .evaluateCreatureList(humanList)) {
            return false;
        }

        return true;
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
        if (validP.contains("Each") && (ai.getLife() <= ComputerUtilCombat.predictDamageTo(ai, dmg, source, false))) {
            return false;
        }

        // if we can kill human, do it
        if ((validP.contains("Each") || validP.contains("EachOpponent") || validP.contains("Targeted"))
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
     * @param af
     *            a {@link forge.game.ability.AbilityFactory} object.
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @param player
     *            a {@link forge.game.player.Player} object.
     * @param dmg
     *            a int.
     * @return a {@link forge.CardList} object.
     */
    private CardCollection getKillableCreatures(final SpellAbility sa, final Player player, final int dmg) {
        final Card source = sa.getHostCard();
        String validC = sa.hasParam("ValidCards") ? sa.getParam("ValidCards") : "";

        // TODO: X may be something different than X paid
        CardCollection list =
                CardLists.getValidCards(player.getCardsIn(ZoneType.Battlefield), validC.split(","), source.getController(), source);

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
        if (validP.contains("Each") && (ai.getLife() <= ComputerUtilCombat.predictDamageTo(ai, dmg, source, false))) {
            return false;
        }

        // if we can kill human, do it
        if ((validP.contains("Each") || validP.contains("EachOpponent") || validP.contains("Targeted"))
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
