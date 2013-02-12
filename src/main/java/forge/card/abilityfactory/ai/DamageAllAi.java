package forge.card.abilityfactory.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.google.common.base.Predicate;

import forge.Card;
import forge.CardLists;
import forge.Singletons;
import forge.card.abilityfactory.AbilityUtils;
import forge.card.abilityfactory.SpellAiLogic;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.cost.Cost;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.ai.ComputerUtilCombat;
import forge.game.ai.ComputerUtilCost;
import forge.game.ai.ComputerUtilMana;
import forge.game.player.AIPlayer;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.util.MyRandom;

public class  DamageAllAi extends SpellAiLogic {

    @Override
    protected boolean canPlayAI(AIPlayer ai, SpellAbility sa) {
        // AI needs to be expanded, since this function can be pretty complex
        // based on what the expected targets could be
        final Random r = MyRandom.getRandom();
        final Cost abCost = sa.getPayCosts();
        final Card source = sa.getSourceCard();

        String validP = "";

        final String damage = sa.getParam("NumDmg");
        int dmg = AbilityUtils.calculateAmount(sa.getSourceCard(), damage, sa);


        if (damage.equals("X") && sa.getSVar(damage).equals("Count$xPaid")) {
            // Set PayX here to maximum value.
            dmg = ComputerUtilMana.determineLeftoverMana(sa, ai);
            source.setSVar("PayX", Integer.toString(dmg));
        }

        if (sa.hasParam("ValidPlayers")) {
            validP = sa.getParam("ValidPlayers");
        }

        Player opp = ai.getOpponent();
        final List<Card> humanList = this.getKillableCreatures(sa, opp, dmg);
        List<Card> computerList = this.getKillableCreatures(sa, ai, dmg);

        final Target tgt = sa.getTarget();
        if (tgt != null && sa.canTarget(opp)) {
            tgt.resetTargets();
            sa.getTarget().addTarget(opp);
            computerList = new ArrayList<Card>();
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
        if (!Singletons.getModel().getGame().getStack().isEmpty()) {
            return false;
        }

        int minGain = 200; // The minimum gain in destroyed creatures
        if (sa.getPayCosts().isReusuableResource()) {
            minGain = 100;
        }

        // evaluate both lists and pass only if human creatures are more valuable
        if ((CardFactoryUtil.evaluateCreatureList(computerList) + minGain) >= CardFactoryUtil
                .evaluateCreatureList(humanList)) {
            return false;
        }

        return true;
    }

    @Override
    public boolean chkAIDrawback(SpellAbility sa, AIPlayer ai) {
        final Card source = sa.getSourceCard();
        String validP = "";

        final String damage = sa.getParam("NumDmg");
        int dmg = AbilityUtils.calculateAmount(sa.getSourceCard(), damage, sa);

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
        final List<Card> humanList = this.getKillableCreatures(sa, enemy, dmg);
        List<Card> computerList = this.getKillableCreatures(sa, ai, dmg);
        final Target tgt = sa.getTarget();

        if (tgt != null && sa.canTarget(enemy)) {
            tgt.resetTargets();
            sa.getTarget().addTarget(enemy);
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

        if (!computerList.isEmpty() && CardFactoryUtil.evaluateCreatureList(computerList) > CardFactoryUtil
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
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param player
     *            a {@link forge.game.player.Player} object.
     * @param dmg
     *            a int.
     * @return a {@link forge.CardList} object.
     */
    private List<Card> getKillableCreatures(final SpellAbility sa, final Player player, final int dmg) {
        final Card source = sa.getSourceCard();
        String validC = sa.hasParam("ValidCards") ? sa.getParam("ValidCards") : "";

        // TODO: X may be something different than X paid
        List<Card> list =
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
    protected boolean doTriggerAINoCost(AIPlayer ai, SpellAbility sa, boolean mandatory) {
        final Card source = sa.getSourceCard();
        String validP = "";

        final String damage = sa.getParam("NumDmg");
        int dmg = AbilityUtils.calculateAmount(sa.getSourceCard(), damage, sa);

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
        final List<Card> humanList = this.getKillableCreatures(sa, enemy, dmg);
        List<Card> computerList = this.getKillableCreatures(sa, ai, dmg);
        final Target tgt = sa.getTarget();

        if (tgt != null && sa.canTarget(enemy)) {
            tgt.resetTargets();
            sa.getTarget().addTarget(enemy);
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

        if (!computerList.isEmpty() && CardFactoryUtil.evaluateCreatureList(computerList) + 50 >= CardFactoryUtil
                .evaluateCreatureList(humanList)) {
            return false;
        }

        return true;
    }
}
