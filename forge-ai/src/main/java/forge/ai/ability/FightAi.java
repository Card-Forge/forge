package forge.ai.ability;

import forge.ai.*;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.player.Player;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.SpellAbility;
import forge.game.staticability.StaticAbilityMustTarget;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerType;
import forge.util.MyRandom;

import java.util.List;

public class FightAi extends SpellAbilityAi {
    @Override
    protected boolean checkAiLogic(final Player ai, final SpellAbility sa, final String aiLogic) {
        return super.checkAiLogic(ai, sa, aiLogic);
    }

    @Override
    protected boolean checkApiLogic(final Player ai, final SpellAbility sa) {
        sa.resetTargets();
        final Card source = sa.getHostCard();

        // everything is defined or targeted above, can't do anything there unless a specific logic is set
        if (sa.hasParam("Defined") && !sa.usesTargeting()) {
            return true;
        }

        // Get creature lists
        CardCollectionView aiCreatures = ai.getCreaturesInPlay();
        aiCreatures = CardLists.getTargetableCards(aiCreatures, sa);
        aiCreatures = ComputerUtil.getSafeTargets(ai, sa, aiCreatures);
        List<Card> humCreatures = ai.getOpponents().getCreaturesInPlay();
        humCreatures = CardLists.getTargetableCards(humCreatures, sa);
        // Filter MustTarget requirements
        StaticAbilityMustTarget.filterMustTargetCards(ai, humCreatures, sa);

        if (humCreatures.isEmpty())
            return false; //prevent IndexOutOfBoundsException on MOJHOSTO variant

        // assumes the triggered card belongs to the ai
        if (sa.hasParam("Defined")) {
            CardCollection fighter1List = AbilityUtils.getDefinedCards(source, sa.getParam("Defined"), sa);
            if ("ChosenAsTgt".equals(sa.getParam("AILogic")) && sa.getRootAbility().getTargetCard() != null) {
                if (fighter1List.isEmpty()) {
                    fighter1List.add(sa.getRootAbility().getTargetCard());
                }
            }
            if (fighter1List.isEmpty()) {
                return false;
            }
            Card fighter1 = fighter1List.get(0);
            for (Card humanCreature : humCreatures) {
                if (canKill(fighter1, humanCreature, 0)
                        && !canKill(humanCreature, fighter1, 0)) {
                    // todo: check min/max targets; see if we picked the best matchup
                    sa.getTargets().add(humanCreature);
                    return true;
                }
            }
            return false; // bail at this point, otherwise the AI will overtarget and waste the activation
        }

        if (sa.hasParam("TargetsFromDifferentZone")) {
            if (!(humCreatures.isEmpty() && aiCreatures.isEmpty())) {
                for (Card humanCreature : humCreatures) {
                    for (Card aiCreature : aiCreatures) {
                        if (canKill(aiCreature, humanCreature, 0)
                                && !canKill(humanCreature, aiCreature, 0)) {
                            // todo: check min/max targets; see if we picked the best matchup
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
                if (canKill(creature1, creature2, 0)
                        && canKill(creature2, creature1, 0)) {
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
    public boolean chkAIDrawback(final SpellAbility sa, final Player aiPlayer) {
        if ("Always".equals(sa.getParam("AILogic"))) {
            return true; // e.g. Hunt the Weak, the AI logic was already checked through canFightAi
        }

        return checkApiLogic(aiPlayer, sa);
    }

    @Override
    protected boolean doTriggerAINoCost(Player ai, SpellAbility sa, boolean mandatory) {
        final String aiLogic = sa.getParamOrDefault("AILogic", "");
        if (aiLogic.equals("Grothama")) {
            return mandatory ? true : SpecialCardAi.GrothamaAllDevouring.consider(ai, sa);
        }

        if (checkApiLogic(ai, sa)) {
            return true;
        }
        if (!mandatory) {
            return false;
        }

        //try to make a good trade or no trade
        final Card source = sa.getHostCard();
        List<Card> humCreatures = ai.getOpponents().getCreaturesInPlay();
        humCreatures = CardLists.getTargetableCards(humCreatures, sa);
        if (humCreatures.isEmpty()) {
            return false;
        }
        //assumes the triggered card belongs to the ai
        if (sa.hasParam("Defined")) {
            Card aiCreature = AbilityUtils.getDefinedCards(source, sa.getParam("Defined"), sa).get(0);
            for (Card humanCreature : humCreatures) {
                if (canKill(aiCreature, humanCreature, 0)
                        && ComputerUtilCard.evaluateCreature(humanCreature) > ComputerUtilCard.evaluateCreature(aiCreature)) {
                    sa.getTargets().add(humanCreature);
                    return true;
                }
            }
            for (Card humanCreature : humCreatures) {
                if (!canKill(humanCreature, aiCreature, 0)) {
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
        final String sourceName = ComputerUtilAbility.getAbilitySourceName(sa);
        AbilitySub tgtFight = sa.getSubAbility();
        while (tgtFight != null && tgtFight.getApi() != ApiType.Fight && tgtFight.getApi() != ApiType.DealDamage) {
            // Search for the Fight/DealDamage subability (matters e.g. for Ent's Fury where the Fight SA is not an immediate child of Pump)
            tgtFight = tgtFight.getSubAbility();
        }
        if (tgtFight == null) {
            System.out.println("Warning: couldn't find a Fight/DealDamage subability from FightAi.canFightAi for card " + source.toString());
            tgtFight = sa.getSubAbility(); // at least avoid a NPE, although this will most likely fail
        }
        final boolean isChandrasIgnition = "Chandra's Ignition".equals(sourceName); // TODO: generalize this for other "fake Fight" cases that do not target
        if ("Savage Punch".equals(sourceName) && !ai.hasFerocious()) {
            power = 0;
            toughness = 0;
        }
        // Get sorted creature lists
        CardCollection aiCreatures = ai.getCreaturesInPlay();
        CardCollection humCreatures = ai.getOpponents().getCreaturesInPlay();
        if ("Time to Feed".equals(sourceName)) { // flip sa
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
                if (source.isSpell()) { // heroic triggers adding counters and prowess
                    final int bonus = getSpellBonus(aiCreature);
                    power += bonus;
                    toughness += bonus;
                }
                if ("PowerDmg".equals(sa.getParam("AILogic"))) {
                    if ("2".equals(sa.getParam("TargetMax"))) {
                        // Band Together, uses up to two targets to deal damage to a single target
                        // TODO: Generalize this so that other TargetMax values can be properly accounted for
                        CardCollection aiCreaturesByPower = new CardCollection(aiCreatures);
                        CardLists.sortByPowerDesc(aiCreaturesByPower);
                        Card maxPower = aiCreaturesByPower.getFirst();
                        if (maxPower != aiCreature) {
                            power += maxPower.getNetPower(); // potential bonus from adding a second target
                        }
                        else if ("2".equals(sa.getParam("TargetMin"))) {
                            continue;
                        }
                        if (canKill(aiCreature, humanCreature, power)) {
                            sa.getTargets().add(aiCreature);
                            sa.getTargets().add(maxPower);
                            if (!isChandrasIgnition) {
                                tgtFight.resetTargets();
                                tgtFight.getTargets().add(humanCreature);
                            }
                            return true;
                        }
                    } else {
                        // Other cards that use AILogic PowerDmg and a single target
                        if (canKill(aiCreature, humanCreature, power)) {
                            sa.getTargets().add(aiCreature);
                            if (!isChandrasIgnition) {
                                tgtFight.resetTargets();
                                tgtFight.getTargets().add(humanCreature);
                            }
                            return true;
                        }
                    }
                } else {
                    if (shouldFight(aiCreature, humanCreature, power, toughness)) {
                    	if ("Time to Feed".equals(sourceName)) { // flip targets
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
                SpellAbility sa = t.ensureAbility();
                if (sa == null) {
                    continue;
                }
                if (ApiType.PutCounter.equals(sa.getApi())) {
                    if ("Card.Self".equals(t.getParam("TargetsValid")) && "You".equals(t.getParam("ValidActivatingPlayer"))) {
                        if ("Self".equals(sa.getParam("Defined")) && "P1P1".equals(sa.getParam("CounterType"))) {
                            return AbilityUtils.calculateAmount(aiCreature, sa.getParam("CounterNum"), sa);
                        }
                        break;
                    }
                } else if (ApiType.Pump.equals(sa.getApi())) {
                    // TODO add prowess boost
                }
            }
        }
        return 0;
    }

    private static boolean shouldFight(Card fighter, Card opponent, int pumpAttack, int pumpDefense) {
    	if (canKill(fighter, opponent, pumpAttack)) {
    		if (!canKill(opponent, fighter, -pumpDefense)) { // can survive
    		    return true;
    		}
    		if (MyRandom.getRandom().nextInt(20) < (opponent.getCMC() - fighter.getCMC())) { // trade
    		    return true;
    		}
    	}
    	return false;
    }

    public static boolean canKill(Card fighter, Card opponent, int pumpAttack) {
        if (opponent.getSVar("Targeting").equals("Dies")) {
            return true;
        }
        // the damage prediction is later
        int damage = fighter.getNetPower() + pumpAttack;
        if (damage <= 0 || opponent.getShieldCount() > 0 || ComputerUtil.canRegenerate(opponent.getController(), opponent)) {
            return false;
        }
        // try to predict the damage that fighter would deal to opponent
        // this should also handle if the opponents creature can be destroyed or not
        if (ComputerUtilCombat.getEnoughDamageToKill(opponent, damage, fighter, false) <= damage) {
            return true;
        }
        return false;
    }
}
