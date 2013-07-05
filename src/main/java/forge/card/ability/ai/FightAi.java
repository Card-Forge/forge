package forge.card.ability.ai;

import java.util.List;
import java.util.Random;

import forge.Card;
import forge.CardLists;
import forge.card.ability.AbilityUtils;
import forge.card.ability.SpellAbilityAi;
import forge.card.spellability.SpellAbility;
import forge.game.ai.ComputerUtil;
import forge.game.ai.ComputerUtilCard;
import forge.game.ai.ComputerUtilCombat;
import forge.game.player.Player;
import forge.util.MyRandom;

public class FightAi extends SpellAbilityAi {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#canPlayAI(forge.game.player.Player, java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected boolean canPlayAI(Player ai, SpellAbility sa) {
        sa.resetTargets();
        final Card source = sa.getSourceCard();

        List<Card> aiCreatures = ai.getCreaturesInPlay();
        aiCreatures = CardLists.getTargetableCards(aiCreatures, sa);
        aiCreatures =  ComputerUtil.getSafeTargets(ai, sa, aiCreatures);

        List<Card> humCreatures = ai.getOpponent().getCreaturesInPlay();
        humCreatures = CardLists.getTargetableCards(humCreatures, sa);

        final Random r = MyRandom.getRandom();
        if (r.nextFloat() > Math.pow(.6667, sa.getActivationsThisTurn())) {
            return false;
        }
        
        //assumes the triggered card belongs to the ai
        if (sa.hasParam("Defined")) {
            Card fighter1 = AbilityUtils.getDefinedCards(source, sa.getParam("Defined"), sa).get(0);
            for (Card humanCreature : humCreatures) {
                if (ComputerUtilCombat.getDamageToKill(humanCreature) <= fighter1.getNetAttack()
                        && humanCreature.getNetAttack() < ComputerUtilCombat.getDamageToKill(fighter1)) {
                    // todo: check min/max targets; see if we picked the best matchup
                    sa.getTargets().add(humanCreature);
                    return true;
                } else if (humanCreature.getSVar("Targeting").equals("Dies")) {
                    sa.getTargets().add(humanCreature);
                    return true;
                }
            }
        }

        if (sa.hasParam("TargetsFromDifferentZone")) {
            if (humCreatures.isEmpty() && aiCreatures.isEmpty()) {
                for (Card humanCreature : humCreatures) {
                    for (Card aiCreature : aiCreatures) {
                        if (ComputerUtilCombat.getDamageToKill(humanCreature) <= aiCreature.getNetAttack()
                                && humanCreature.getNetAttack() < ComputerUtilCombat.getDamageToKill(aiCreature)) {
                            // todo: check min/max targets; see if we picked the best matchup
                            sa.getTargets().add(humanCreature);
                            sa.getTargets().add(aiCreature);
                            return true;
                        } else if (humanCreature.getSVar("Targeting").equals("Dies")) {
                            sa.getTargets().add(humanCreature);
                            sa.getTargets().add(aiCreature);
                            return true;
                        }
                    }
                }
            }
            return false;
        }
        for (Card creature1 : humCreatures) {
            for (Card creature2 : humCreatures) {
                if (creature1.equals(creature2)) {
                    continue;
                }
                if (sa.hasParam("TargetsWithoutSameCreatureType")
                        && creature1.sharesCreatureTypeWith(creature2)) {
                    continue;
                }
                if (ComputerUtilCombat.getDamageToKill(creature1) <= creature2.getNetAttack()
                        && creature1.getNetAttack() >= ComputerUtilCombat.getDamageToKill(creature2)) {
                    // todo: check min/max targets; see if we picked the best matchup
                    sa.getTargets().add(creature1);
                    sa.getTargets().add(creature2);
                    return true;
                }
            }
        }

        return false;
    }

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#doTriggerAINoCost(forge.game.player.Player, java.util.Map, forge.card.spellability.SpellAbility, boolean)
     */
    @Override
    protected boolean doTriggerAINoCost(Player ai, SpellAbility sa, boolean mandatory) {
        if (canPlayAI(ai, sa)) {
            return true;
        }
        if (!mandatory) {
            return false;
        }
        
        //try to make a good trade or no trade
        final Card source = sa.getSourceCard();
        List<Card> humCreatures = ai.getOpponent().getCreaturesInPlay();
        humCreatures = CardLists.getTargetableCards(humCreatures, sa);
        if (humCreatures.isEmpty()) {
            return false;
        }
        //assumes the triggered card belongs to the ai
        if (sa.hasParam("Defined")) {
            Card aiCreature = AbilityUtils.getDefinedCards(source, sa.getParam("Defined"), sa).get(0);
            for (Card humanCreature : humCreatures) {
                if (ComputerUtilCombat.getDamageToKill(humanCreature) <= aiCreature.getNetAttack()
                        && ComputerUtilCard.evaluateCreature(humanCreature) > ComputerUtilCard.evaluateCreature(aiCreature)) {
                    sa.getTargets().add(humanCreature);
                    return true;
                }
            }
            for (Card humanCreature : humCreatures) {
                if (ComputerUtilCombat.getDamageToKill(aiCreature) > humanCreature.getNetAttack()) {
                    sa.getTargets().add(humanCreature);
                    return true;
                }
            }
            sa.getTargets().add(humCreatures.get(0));
            return true;
        }
        
        return true;
    }

}
