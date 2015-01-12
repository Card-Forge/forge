package forge.ai.ability;

import forge.ai.ComputerUtil;
import forge.ai.ComputerUtilCard;
import forge.ai.ComputerUtilCombat;
import forge.ai.SpellAbilityAi;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.util.MyRandom;

import java.util.List;
import java.util.Random;

public class FightAi extends SpellAbilityAi {
    @Override
    protected boolean canPlayAI(Player ai, SpellAbility sa) {
        sa.resetTargets();
        final Card source = sa.getHostCard();

        CardCollectionView aiCreatures = ai.getCreaturesInPlay();
        aiCreatures = CardLists.getTargetableCards(aiCreatures, sa);
        aiCreatures = ComputerUtil.getSafeTargets(ai, sa, aiCreatures);

        List<Card> humCreatures = ai.getOpponent().getCreaturesInPlay();
        humCreatures = CardLists.getTargetableCards(humCreatures, sa);

        final Random r = MyRandom.getRandom();
        if (r.nextFloat() > Math.pow(.6667, sa.getActivationsThisTurn())) {
            return false;
        }
        if (sa.hasParam("FightWithToughness")) {
            // TODO: add ailogic
            return false;
        }

        //assumes the triggered card belongs to the ai
        if (sa.hasParam("Defined")) {
            Card fighter1 = AbilityUtils.getDefinedCards(source, sa.getParam("Defined"), sa).get(0);
            for (Card humanCreature : humCreatures) {
                if (ComputerUtilCombat.getDamageToKill(humanCreature) <= fighter1.getNetPower()
                        && humanCreature.getNetPower() < ComputerUtilCombat.getDamageToKill(fighter1)) {
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
            if (!(humCreatures.isEmpty() && aiCreatures.isEmpty())) {
                for (Card humanCreature : humCreatures) {
                    for (Card aiCreature : aiCreatures) {
                        if (ComputerUtilCombat.getDamageToKill(humanCreature) <= aiCreature.getNetPower()
                                && humanCreature.getNetPower() < ComputerUtilCombat.getDamageToKill(aiCreature)) {
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
                if (ComputerUtilCombat.getDamageToKill(creature1) <= creature2.getNetPower()
                        && creature1.getNetPower() >= ComputerUtilCombat.getDamageToKill(creature2)) {
                    // todo: check min/max targets; see if we picked the best matchup
                    sa.getTargets().add(creature1);
                    sa.getTargets().add(creature2);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    protected boolean doTriggerAINoCost(Player ai, SpellAbility sa, boolean mandatory) {
        if (canPlayAI(ai, sa)) {
            return true;
        }
        if (!mandatory) {
            return false;
        }
        
        //try to make a good trade or no trade
        final Card source = sa.getHostCard();
        List<Card> humCreatures = ai.getOpponent().getCreaturesInPlay();
        humCreatures = CardLists.getTargetableCards(humCreatures, sa);
        if (humCreatures.isEmpty()) {
            return false;
        }
        //assumes the triggered card belongs to the ai
        if (sa.hasParam("Defined")) {
            Card aiCreature = AbilityUtils.getDefinedCards(source, sa.getParam("Defined"), sa).get(0);
            for (Card humanCreature : humCreatures) {
                if (ComputerUtilCombat.getDamageToKill(humanCreature) <= aiCreature.getNetPower()
                        && ComputerUtilCard.evaluateCreature(humanCreature) > ComputerUtilCard.evaluateCreature(aiCreature)) {
                    sa.getTargets().add(humanCreature);
                    return true;
                }
            }
            for (Card humanCreature : humCreatures) {
                if (ComputerUtilCombat.getDamageToKill(aiCreature) > humanCreature.getNetPower()) {
                    sa.getTargets().add(humanCreature);
                    return true;
                }
            }
            sa.getTargets().add(humCreatures.get(0));
            return true;
        }
        return true;
    }
    public static boolean shouldFight(Card fighter, Card opponent, int pumpAttack, int pumpDefense) {
    	if (canKill(fighter, opponent, pumpAttack)) {
    		if (!canKill(opponent, fighter, -pumpDefense)) {
    			return true;
    		} else {
    			final Random r = MyRandom.getRandom();
    			if (r.nextInt(20)<(opponent.getCMC() - fighter.getCMC())) {
    				return true;
    			}
    		}
    	}
    	return false;
    }
    public static boolean canKill(Card fighter, Card opponent, int pumpAttack) {
    	if (opponent.getSVar("Targeting").equals("Dies")) {
    		return true;
    	}
    	if (opponent.hasProtectionFrom(fighter) || !opponent.canBeDestroyed() 
    	        || opponent.getShieldCount() > 0 || ComputerUtil.canRegenerate(opponent.getController(), opponent)) {
    		return false;
    	}
    	if (fighter.hasKeyword("Deathtouch") || ComputerUtilCombat.getDamageToKill(opponent) <= fighter.getNetPower() + pumpAttack) {
    		return true;
    	}
    	return false;
    }
}
