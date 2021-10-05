package forge.ai.ability;

import java.util.List;
import java.util.Map;

import forge.ai.ComputerUtil;
import forge.ai.ComputerUtilAbility;
import forge.ai.ComputerUtilCard;
import forge.ai.ComputerUtilCombat;
import forge.ai.SpellAbilityAi;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.keyword.Keyword;
import forge.game.player.Player;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.SpellAbility;
import forge.game.staticability.StaticAbilityMustTarget;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerType;
import forge.util.MyRandom;

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

        // everything is defined or targeted above, can't do anything there?
        if (sa.hasParam("Defined") && !sa.usesTargeting()) {
            // TODO extend Logic for cards like Arena or Grothama
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
                return true; // FIXME: shouldn't this return "false" if nothing found?
            }
            Card fighter1 = fighter1List.get(0);
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
            return false; // bail at this point, otherwise the AI will overtarget and waste the activation
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
                if (sa.hasParam("TargetsWithoutSameCreatureType") && creature1.sharesCreatureTypeWith(creature2)) {
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
    public boolean chkAIDrawback(final SpellAbility sa, final Player aiPlayer) {
        if ("Always".equals(sa.getParam("AILogic"))) {
            return true; // e.g. Hunt the Weak, the AI logic was already checked through canFightAi
        }

        return checkApiLogic(aiPlayer, sa);
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
        List<Card> humCreatures = ai.getOpponents().getCreaturesInPlay();
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
        final String sourceName = ComputerUtilAbility.getAbilitySourceName(sa);
        final AbilitySub tgtFight = sa.getSubAbility();
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
                final Map<String, String> params = t.getMapParams();
                if (sa == null) {
                    continue;
                }
                if (ApiType.PutCounter.equals(sa.getApi())) {
                    if ("Card.Self".equals(params.get("TargetsValid")) && "You".equals(params.get("ValidActivatingPlayer"))) {
                        SpellAbility heroic = AbilityFactory.getAbility(aiCreature.getSVar(params.get("Execute")),aiCreature);
                        if ("Self".equals(heroic.getParam("Defined")) && "P1P1".equals(heroic.getParam("CounterType"))) {
                            return AbilityUtils.calculateAmount(aiCreature, heroic.getParam("CounterNum"), heroic);
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
    		} else {
                if (MyRandom.getRandom().nextInt(20) < (opponent.getCMC() - fighter.getCMC())) { // trade
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
        if (opponent.hasProtectionFrom(fighter) || !opponent.canBeDestroyed() || opponent.getShieldCount() > 0
                || ComputerUtil.canRegenerate(opponent.getController(), opponent)) {
            return false;
        }
        if (fighter.hasKeyword(Keyword.DEATHTOUCH)
                || ComputerUtilCombat.getDamageToKill(opponent) <= fighter.getNetPower() + pumpAttack) {
            return true;
        }
        return false;
    }
}
