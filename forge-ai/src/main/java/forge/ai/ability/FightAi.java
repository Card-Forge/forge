package forge.ai.ability;

import forge.ai.ComputerUtil;
import forge.ai.ComputerUtilCard;
import forge.ai.ComputerUtilCombat;
import forge.ai.SpellAbilityAi;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.player.Player;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerType;
import forge.util.MyRandom;

import java.util.List;
import java.util.Map;
import java.util.Random;

public class FightAi extends SpellAbilityAi {
    @Override
    protected boolean checkAiLogic(final Player ai, final SpellAbility sa, final String aiLogic) {
        if (sa.hasParam("FightWithToughness")) {
            // TODO: add ailogic
            return false;
        }
        return super.checkAiLogic(ai, sa, aiLogic);
    }

    @Override
    protected boolean checkApiLogic(final Player ai, final SpellAbility sa) {
        sa.resetTargets();
        final Card source = sa.getHostCard();
        
        // Get creature lists
        CardCollectionView aiCreatures = ai.getCreaturesInPlay();
        aiCreatures = CardLists.getTargetableCards(aiCreatures, sa);
        aiCreatures = ComputerUtil.getSafeTargets(ai, sa, aiCreatures);
        List<Card> humCreatures = ai.getOpponent().getCreaturesInPlay();
        humCreatures = CardLists.getTargetableCards(humCreatures, sa);

        // assumes the triggered card belongs to the ai
        if (sa.hasParam("Defined")) {
            Card fighter1 = AbilityUtils.getDefinedCards(source, sa.getParam("Defined"), sa).get(0);
            for (Card humanCreature : humCreatures) {
                if (ComputerUtilCombat.getDamageToKill(humanCreature) <= fighter1.getNetPower()
                        && humanCreature.getNetPower() < ComputerUtilCombat.getDamageToKill(fighter1)) {
                    // todo: check min/max targets; see if we picked the best
                    // matchup
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
                            // todo: check min/max targets; see if we picked the
                            // best matchup
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
                if (sa.hasParam("TargetsWithoutSameCreatureType") && creature1.sharesCreatureTypeWith(creature2)) {
                    continue;
                }
                if (ComputerUtilCombat.getDamageToKill(creature1) <= creature2.getNetPower()
                        && creature1.getNetPower() >= ComputerUtilCombat.getDamageToKill(creature2)) {
                    // todo: check min/max targets; see if we picked the best
                    // matchup
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
    
    /**
     * Logic for evaluating fight effects
     * @param ai controlling player
     * @param sa host SpellAbility
     * @param toughness bonus to toughness
     * @param power	bonus to power
     * @return true if fight effect should be played, false otherwise
     */
    public static boolean canFightAi(final Player ai, final SpellAbility sa, int power, int toughness) {
    	final Card source = sa.getHostCard();
        final AbilitySub tgtFight = sa.getSubAbility();
        if ("Savage Punch".equals(source.getName()) && !ai.hasFerocious()) {
            power = 0;
            toughness = 0;
        }
        // Get sorted creature lists
        CardCollection aiCreatures = ai.getCreaturesInPlay();
        CardCollection humCreatures = ai.getOpponent().getCreaturesInPlay();
		if ("Time to Feed".equals(source.getName())) {	// flip sa
			aiCreatures = CardLists.getTargetableCards(aiCreatures, tgtFight);
			aiCreatures = ComputerUtil.getSafeTargets(ai, tgtFight, aiCreatures);
			humCreatures = CardLists.getTargetableCards(humCreatures, sa);
		} else {
			aiCreatures = CardLists.getTargetableCards(aiCreatures, sa);
			aiCreatures = ComputerUtil.getSafeTargets(ai, sa, aiCreatures);
			humCreatures = CardLists.getTargetableCards(humCreatures, tgtFight);
		}
        ComputerUtilCard.sortByEvaluateCreature(aiCreatures);
        ComputerUtilCard.sortByEvaluateCreature(humCreatures);
        if (humCreatures.isEmpty() || aiCreatures.isEmpty()) {
            return false;
        }
        // Evaluate creature pairs
        for (Card humanCreature : humCreatures) {
            for (Card aiCreature : aiCreatures) {
                if (source.isSpell()) {   // heroic triggers adding counters and prowess
                	final int bonus = getSpellBonus(aiCreature);
                    power += bonus;
                    toughness += bonus;
                }
                if ("PowerDmg".equals(sa.getParam("AILogic"))) {
                    if (FightAi.canKill(aiCreature, humanCreature, power)) {
                        sa.getTargets().add(aiCreature);
                        tgtFight.resetTargets();
                        tgtFight.getTargets().add(humanCreature);
                        return true;
                    }
                } else {
                    if (FightAi.shouldFight(aiCreature, humanCreature, power, toughness)) {
                    	if ("Time to Feed".equals(source.getName())) {	// flip targets
                    		final Card tmp = aiCreature;
                    		aiCreature = humanCreature;
                    		humanCreature = tmp;
                    	}
                        sa.getTargets().add(aiCreature);
                        tgtFight.resetTargets();
                        tgtFight.getTargets().add(humanCreature);
                        return true;
                    }
                }
            }
        }
        return false;
    }

	/**
	 * Compute the bonus from Heroic +1/+1 counters or Prowess
	 */
	private static int getSpellBonus(final Card aiCreature) {
		for (Trigger t : aiCreature.getTriggers()) {
		    if (t.getMode() == TriggerType.SpellCast) {
		        final Map<String, String> params = t.getMapParams();
		        if ("Card.Self".equals(params.get("TargetsValid")) && "You".equals(params.get("ValidActivatingPlayer")) 
		                && params.containsKey("Execute")) {
		            SpellAbility heroic = AbilityFactory.getAbility(aiCreature.getSVar(params.get("Execute")),aiCreature);
		            if ("Self".equals(heroic.getParam("Defined")) && "P1P1".equals(heroic.getParam("CounterType"))) {
		                return AbilityUtils.calculateAmount(aiCreature, heroic.getParam("CounterNum"), heroic);
		            }
		            break;
		        }
		        if ("ProwessPump".equals(params.get("Execute"))) {
		        	return 1;
		        }
		    }
		}
		return 0;
	}
    
    private static boolean shouldFight(Card fighter, Card opponent, int pumpAttack, int pumpDefense) {
    	if (canKill(fighter, opponent, pumpAttack)) {
    		if (!canKill(opponent, fighter, -pumpDefense)) {	// can survive
    			return true;
    		} else {
    			final Random r = MyRandom.getRandom();
    			if (r.nextInt(20)<(opponent.getCMC() - fighter.getCMC())) {	// trade
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
