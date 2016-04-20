package forge.ai.ability;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

import forge.ai.*;
import forge.game.ability.AbilityUtils;
import forge.game.card.*;
import forge.game.combat.CombatUtil;
import forge.game.cost.Cost;
import forge.game.cost.CostPart;
import forge.game.cost.CostRemoveCounter;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import forge.game.zone.ZoneType;
import forge.util.Aggregates;
import forge.util.MyRandom;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CountersPutAi extends SpellAbilityAi {
    @Override
    protected boolean canPlayAI(Player ai, final SpellAbility sa) {
        // AI needs to be expanded, since this function can be pretty complex
        // based on
        // what the expected targets could be
        final Random r = MyRandom.getRandom();
        final Cost abCost = sa.getPayCosts();
        final TargetRestrictions abTgt = sa.getTargetRestrictions();
        final Card source = sa.getHostCard();
        CardCollection list;
        Card choice = null;
        final String type = sa.getParam("CounterType");
        final String amountStr = sa.getParam("CounterNum");
        final boolean divided = sa.hasParam("DividedAsYouChoose");

        final Player player = sa.isCurse() ? ai.getOpponent() : ai;

        if (ComputerUtil.preventRunAwayActivations(sa)) {
            return false;
        }

        if ("Never".equals(sa.getParam("AILogic"))) {
            return false;
        }

        if (sa.getConditions() != null && !sa.getConditions().areMet(sa) && sa.getSubAbility() == null) {
        	return false;
        }
        
        if (abCost != null) {
            // AI currently disabled for these costs
            if (!ComputerUtilCost.checkLifeCost(ai, abCost, source, 4, null)) {
                return false;
            }

            if (!ComputerUtilCost.checkDiscardCost(ai, abCost, source)) {
                return false;
            }

            // disable moving counters
            for (final CostPart part : abCost.getCostParts()) {
                if (part instanceof CostRemoveCounter) {
                    final CostRemoveCounter remCounter = (CostRemoveCounter) part;
                    final CounterType counterType = remCounter.counter;
                    if (counterType.name().equals(type)) {
                        return false;
                    }
                    if (!part.payCostFromSource()) {
                        if (counterType.name().equals("P1P1")) {
                            return false;
                        }
                        continue;
                    }
                    //don't kill the creature
                    if (counterType.name().equals("P1P1") && source.getLethalDamage() <= 1) {
                        return false;
                    }
                }
            }
        }
        
        if (source.getName().equals("Feat of Resistance")) {    // sub-ability should take precedence
            CardCollection prot = ProtectAi.getProtectCreatures(ai, sa.getSubAbility());
            if (!prot.isEmpty()) {
                sa.getTargets().add(prot.get(0));
                return true;
            }
        }

        if (sa.hasParam("Bolster")) {
            CardCollection creatsYouCtrl = CardLists.filter(ai.getCardsIn(ZoneType.Battlefield), CardPredicates.Presets.CREATURES);
            CardCollection leastToughness = new CardCollection(Aggregates.listWithMin(creatsYouCtrl, CardPredicates.Accessors.fnGetDefense));
            if (leastToughness.isEmpty()) {
                return false;
            }
            // TODO If Creature that would be Bolstered for some reason is useless, also return False
        }

        if (sa.hasParam("Monstrosity") && source.isMonstrous()) {
            return false;
        }

        if (sa.hasParam("LevelUp")) {
        	 // creatures enchanted by curse auras have low priority
        	if (source.getGame().getPhaseHandler().getPhase().isBefore(PhaseType.MAIN2)) {
                for (Card aura : source.getEnchantedBy(false)) {
                    if (aura.getController().isOpponentOf(ai)) {
                        return false;
                    }
                }
            }
        	int maxLevel = Integer.parseInt(sa.getParam("MaxLevel"));
            return source.getCounters(CounterType.LEVEL) < maxLevel;
        }

        // TODO handle proper calculation of X values based on Cost
        int amount = AbilityUtils.calculateAmount(source, amountStr, sa);

        if ("Fight".equals(sa.getParam("AILogic"))) {
        	int nPump = 0;
        	if (type.equals("P1P1")) {
        		nPump = amount;
        	}
        	return FightAi.canFightAi(ai, sa, nPump, nPump);
        }
        
        if (amountStr.equals("X") && source.getSVar(amountStr).equals("Count$xPaid")) {
            // Set PayX here to maximum value.
            amount = ComputerUtilMana.determineLeftoverMana(sa, ai);
            source.setSVar("PayX", Integer.toString(amount));
        }

        // don't use it if no counters to add
        if (amount <= 0) {
            return false;
        }

        if ("Polukranos".equals(sa.getParam("AILogic"))) {
            CardCollection humCreatures = ai.getOpponent().getCreaturesInPlay();
            final CardCollection targets = CardLists.filter(humCreatures, new Predicate<Card>() {
                @Override
                public boolean apply(final Card c) {
                    return !(c.hasProtectionFrom(source) || c.hasKeyword("Shroud") || c.hasKeyword("Hexproof"));
                }
            });
            if (!targets.isEmpty()){
                boolean canSurvive = false;
                for (Card humanCreature : targets) {
                    if (!FightAi.canKill(humanCreature, source, 0)){
                        canSurvive = true;
                    }
				}
                if (!canSurvive){
                    return false;
                }
            }
        }
        
        PhaseHandler ph = ai.getGame().getPhaseHandler();
        if (sa.isOutlast()) {
            if (ph.is(PhaseType.MAIN2, ai)) {   //applicable to non-attackers only
                float chance = 0.8f;
                if (ComputerUtilCard.doesSpecifiedCreatureBlock(ai, source)) {
                    return false;
                }
                return chance > r.nextFloat();
            } else {
                return false;
            }
        }
        
        if (!ai.getGame().getStack().isEmpty() && !SpellAbilityAi.isSorcerySpeed(sa)) {
            // only evaluates case where all tokens are placed on a single target
            if (abTgt != null && abTgt.getMinTargets(source, sa) < 2) {
                if (ComputerUtilCard.canPumpAgainstRemoval(ai, sa)) {
                    Card c = sa.getTargets().getFirstTargetedCard();
                    if (sa.getTargets().getNumTargeted() > 1) {
                        sa.resetTargets();
                        sa.getTargets().add(c);
                    }
                    abTgt.addDividedAllocation(sa.getTargetCard(), amount);
                    return true;
                } else {
                    return false;
                }
            }
        }

        // Targeting
        if (abTgt != null) {
            sa.resetTargets();            
            
            final boolean sacSelf = ComputerUtilCost.isSacrificeSelfCost(abCost);

            list = CardLists.filter(player.getCardsIn(ZoneType.Battlefield), new Predicate<Card>() {
                @Override
                public boolean apply(final Card c) {
                	
                	// don't put the counter on the dead creature
                	if (sacSelf && c.equals(source)) {
                		return false;
                	}
                    return c.canBeTargetedBy(sa) && c.canReceiveCounters(CounterType.valueOf(type));
                }
            });

            list = CardLists.getValidCards(list, abTgt.getValidTgts(), source.getController(), source, sa);

            if (list.size() < abTgt.getMinTargets(source, sa)) {
                return false;
            }

            if (source.getName().equals("Abzan Charm")) {
                // specific AI for instant with distribute two +1/+1 counters
                ComputerUtilCard.sortByEvaluateCreature(list);
                // maximise the number of targets
                for (int i = 1; i < amount + 1; i++) {
                    int left = amount;
                    for (Card c : list) {
                        if (ComputerUtilCard.shouldPumpCard(ai, sa, c, i, i,
                                new ArrayList<String>())) {
                            sa.getTargets().add(c);
                            abTgt.addDividedAllocation(c, i);
                            left -= i;
                        }
                        if (left < i || sa.getTargets().getNumTargeted() == abTgt.getMaxTargets(source, sa)) {
                            abTgt.addDividedAllocation(sa.getTargets().getFirstTargetedCard(), left + i);
                            left = 0;
                            break;
                        }
                    }
                    if (left == 0) {
                        return true;
                    }
                    sa.resetTargets();
                }
                return false;
            }
            
            // target loop
            while (sa.getTargets().getNumTargeted() < abTgt.getMaxTargets(sa.getHostCard(), sa)) {
                if (list.isEmpty()) {
                    if ((sa.getTargets().getNumTargeted() < abTgt.getMinTargets(sa.getHostCard(), sa))
                            || (sa.getTargets().getNumTargeted() == 0)) {
                        sa.resetTargets();
                        return false;
                    } else {
                        // TODO is this good enough? for up to amounts?
                        break;
                    }
                }

                if (sa.isCurse()) {
                    choice = CountersAi.chooseCursedTarget(list, type, amount);
                } else {
                    if (type.equals("P1P1") && !SpellAbilityAi.isSorcerySpeed(sa)) {
                        for (Card c : list) {
                            if (ComputerUtilCard.shouldPumpCard(ai, sa, c, amount, amount, new ArrayList<String>())) {
                                choice = c;
                                break;
                            }
                        }
                        if (!source.isSpell()) {    // does not cost a card
                            if (choice == null) {   // find generic target
                                if (abCost == null || ph.is(PhaseType.END_OF_TURN, ai.getOpponent())) {
                                    // only use at opponent EOT unless it is free
                                    choice = CountersAi.chooseBoonTarget(list, type);
                                }
                            }
                        }
                        if (sa.getHostCard().getName().equals("Dromoka's Command")) {
                            choice = CountersAi.chooseBoonTarget(list, type);
                        }
                    } else {
                        choice = CountersAi.chooseBoonTarget(list, type);
                    }
                }

                if (choice == null) { // can't find anything left
                    if (sa.getTargets().getNumTargeted() < abTgt.getMinTargets(sa.getHostCard(), sa)
                            || sa.getTargets().getNumTargeted() == 0) {
                        sa.resetTargets();
                        return false;
                    } else {
                        // TODO is this good enough? for up to amounts?
                        break;
                    }
                }

                list.remove(choice);
                sa.getTargets().add(choice);
                choice = null;
                if (divided) {
                    abTgt.addDividedAllocation(choice, amount);
                    break;
                }
            }
            if (sa.getTargets().isEmpty()) {
                return false;
            }
        } else {
            final List<Card> cards = AbilityUtils.getDefinedCards(sa.getHostCard(), sa.getParam("Defined"), sa);
            // Don't activate Curse abilities on my cards and non-curse abilites
            // on my opponents
            if (cards.isEmpty() || !cards.get(0).getController().equals(player)) {
                return false;
            }

            final int currCounters = cards.get(0).getCounters(CounterType.valueOf(type));
            // each non +1/+1 counter on the card is a 10% chance of not
            // activating this ability.

            if (!(type.equals("P1P1") || type.equals("M1M1") || type.equals("ICE")) && (r.nextFloat() < (.1 * currCounters))) {
                return false;
            }
        }

        boolean immediately = ComputerUtil.playImmediately(ai, sa);
        
        if (abCost != null && !ComputerUtilCost.checkSacrificeCost(ai, abCost, source, immediately)) {
            return false;
        }
        
        if (immediately) {
            return true;
        }

        if (!type.equals("P1P1") && !type.equals("M1M1") && !sa.hasParam("ActivationPhases")) {
	        // Don't use non P1P1/M1M1 counters before main 2 if possible
	        if (ph.getPhase().isBefore(PhaseType.MAIN2)
	                && !ComputerUtil.castSpellInMain1(ai, sa)) {
	            return false;
	        }	        
	        if (ph.isPlayerTurn(ai) && !isSorcerySpeed(sa)) {
	            return false;
	        }
        }

        if (ComputerUtil.waitForBlocking(sa)) {
            return false;
        }
        
        return true;
    }

    @Override
    public boolean chkAIDrawback(final SpellAbility sa, Player ai) {
        boolean chance = true;
        final TargetRestrictions abTgt = sa.getTargetRestrictions();
        final Card source = sa.getHostCard();
        Card choice = null;
        final String type = sa.getParam("CounterType");
        final String amountStr = sa.getParam("CounterNum");
        final boolean divided = sa.hasParam("DividedAsYouChoose");
        final int amount = AbilityUtils.calculateAmount(sa.getHostCard(), amountStr, sa);

        final Player player = sa.isCurse() ? ai.getOpponent() : ai;

        if (abTgt != null) {
            CardCollection list =
                    CardLists.getValidCards(player.getCardsIn(ZoneType.Battlefield), abTgt.getValidTgts(), source.getController(), source, sa);

            if (list.size() == 0) {
                return false;
            }

            sa.resetTargets();
            // target loop
            while (sa.getTargets().getNumTargeted() < abTgt.getMaxTargets(sa.getHostCard(), sa)) {
                list = CardLists.filter(list, new Predicate<Card>() {
                    @Override
                    public boolean apply(final Card c) {
                        return sa.canTarget(c);
                    }
                });
                if (list.size() == 0) {
                    if ((sa.getTargets().getNumTargeted() < abTgt.getMinTargets(sa.getHostCard(), sa))
                            || (sa.getTargets().getNumTargeted() == 0)) {
                        sa.resetTargets();
                        return false;
                    } else {
                        break;
                    }
                }

                if (sa.isCurse()) {
                    choice = CountersAi.chooseCursedTarget(list, type, amount);
                } else {
                	String txt = source.getAbilityText();
                	if (txt != null && txt.contains("Awaken ")) {
                		choice = ComputerUtilCard.getWorstLand(list);
                	} else {
                		choice = CountersAi.chooseBoonTarget(list, type);
                	}
                }

                if (choice == null) { // can't find anything left
                    if ((sa.getTargets().getNumTargeted() < abTgt.getMinTargets(sa.getHostCard(), sa))
                            || (sa.getTargets().getNumTargeted() == 0)) {
                        sa.resetTargets();
                        return false;
                    } else {
                        // TODO is this good enough? for up to amounts?
                        break;
                    }
                }
                list.remove(choice);
                sa.getTargets().add(choice);
                if (divided) {
                    abTgt.addDividedAllocation(choice, amount);
                    break;
                }
            }
        }

        return chance;
    }

    @Override
    protected boolean doTriggerAINoCost(Player ai, SpellAbility sa, boolean mandatory) {
        final TargetRestrictions abTgt = sa.getTargetRestrictions();
        final Card source = sa.getHostCard();
        // boolean chance = true;
        boolean preferred = true;
        CardCollection list;
        boolean isCurse = sa.isCurse();
        final Player player = isCurse ? ai.getOpponent() : ai;
        final String type = sa.getParam("CounterType");
        final String amountStr = sa.getParam("CounterNum");
        final boolean divided = sa.hasParam("DividedAsYouChoose");
        final int amount = AbilityUtils.calculateAmount(sa.getHostCard(), amountStr, sa);
        int left = amount;
        
        if (abTgt == null) {
            // No target. So must be defined
            list = new CardCollection(AbilityUtils.getDefinedCards(source, sa.getParam("Defined"), sa));
            
            if (amountStr.equals("X") && ((sa.hasParam(amountStr) && sa.getSVar(amountStr).equals("Count$xPaid")) || source.getSVar(amountStr).equals("Count$xPaid") )) {
                // Spend all remaining mana to add X counters (eg. Hero of Leina Tower)
                source.setSVar("PayX", Integer.toString(ComputerUtilMana.determineLeftoverMana(sa, ai)));
            }
            
            if (!mandatory) {
                // TODO - If Trigger isn't mandatory, when wouldn't we want to
                // put a counter?
                // things like Powder Keg, which are way too complex for the AI
            }
        } else {
            list = CardLists.getTargetableCards(player.getCardsIn(ZoneType.Battlefield), sa);
            list = CardLists.getValidCards(list, abTgt.getValidTgts(), source.getController(), source, sa);

            int totalTargets = list.size();

            while (sa.getTargets().getNumTargeted() < abTgt.getMaxTargets(sa.getHostCard(), sa)) {
                if (mandatory) {
                    // When things are mandatory, gotta handle a little differently
                    if ((list.isEmpty() || !preferred) && sa.getTargets().getNumTargeted() >= abTgt.getMinTargets(sa.getHostCard(), sa)) {
                        return true;
                    }

                    if (list.isEmpty() && preferred) {
                        // If it's required to choose targets and the list is empty, get a new list
                        list = CardLists.getTargetableCards(player.getOpponent().getCardsIn(ZoneType.Battlefield), sa);
                        list = CardLists.getValidCards(list, abTgt.getValidTgts(), source.getController(), source, sa);
                        preferred = false;
                    }
                }

                if (list.isEmpty()) {
                    // Not mandatory, or the the list was regenerated and is still empty,
                    // so return whether or not we found enough targets
                    return sa.getTargets().getNumTargeted() >= abTgt.getMinTargets(sa.getHostCard(), sa);
                }

                Card choice = null;

                // Choose targets here:
                if (isCurse) {
                    if (preferred) {
                        choice = CountersAi.chooseCursedTarget(list, type, amount);
                        if (choice == null && mandatory) {
                            choice = Aggregates.random(list);
                        }
                    } else {
                        if (type.equals("M1M1")) {
                            choice = ComputerUtilCard.getWorstCreatureAI(list);
                        } else {
                            choice = Aggregates.random(list);
                        }
                    }
                } else {
                    if (preferred) {
                        list = ComputerUtil.getSafeTargets(ai, sa, list);
                        choice = CountersAi.chooseBoonTarget(list, type);
                        if (choice == null && mandatory) {
                            choice = Aggregates.random(list);
                        }
                    } else {
                        if (type.equals("P1P1")) {
                            choice = ComputerUtilCard.getWorstCreatureAI(list);
                        } else {
                            choice = Aggregates.random(list);
                        }
                    }
                    if (choice != null && divided) {
                        int alloc = Math.max(amount / totalTargets, 1);
                        if (sa.getTargets().getNumTargeted() == Math.min(totalTargets, abTgt.getMaxTargets(sa.getHostCard(), sa)) - 1) {
                            abTgt.addDividedAllocation(choice, left);
                        } else {
                            abTgt.addDividedAllocation(choice, alloc);
                            left -= alloc;
                        }
                    }
                }

                if (choice != null) {
                    sa.getTargets().add(choice);
                    list.remove(choice);
                } else {
                    // Didn't want to choose anything?
                    list.clear();
                }

            }
        }
        return true;
    }

    @Override
    public boolean confirmAction(Player player, SpellAbility sa, PlayerActionConfirmMode mode, String message) {
        final Card source = sa.getHostCard();
        if (mode == PlayerActionConfirmMode.Tribute) {
            // add counter if that opponent has a giant creature
            final List<Card> creats = player.getCreaturesInPlay();
            final int tributeAmount = source.getKeywordMagnitude("Tribute");
            final boolean isHaste = source.hasKeyword("Haste");
            List<Card> threatening = CardLists.filter(creats, new Predicate<Card>() {
                @Override
                public boolean apply(Card c) {
                    return CombatUtil.canBlock(source, c, !isHaste) 
                            && (c.getNetToughness() > source.getNetPower() + tributeAmount || c.hasKeyword("DeathTouch"));
                }
            });
            if (!threatening.isEmpty()) {
                return true;
            }
            if (source.hasSVar("TributeAILogic")) {
                final String logic = source.getSVar("TributeAILogic");
                if (logic.equals("Always")) {
                    return true;
                } else if (logic.equals("Never")) {
                    return false;
                } else if (logic.equals("CanBlockThisTurn")) {
                    // pump haste
                    List<Card> canBlock = CardLists.filter(creats, new Predicate<Card>() {
                        @Override
                        public boolean apply(Card c) {
                            return CombatUtil.canBlock(source, c) && (c.getNetToughness() > source.getNetPower() || c.hasKeyword("DeathTouch"));
                        }
                    });
                    if (!canBlock.isEmpty()) {
                        return false;
                    }
                } else if (logic.equals("DontControlCreatures")) {
                    return !creats.isEmpty();
                } else if (logic.equals("OppHasCardsInHand")) {
                    return !player.getOpponent().getCardsIn(ZoneType.Hand).isEmpty();
                }
            }
        }
        return MyRandom.getRandom().nextBoolean();
    }

    @Override
    public Player chooseSinglePlayer(Player ai, SpellAbility sa, Iterable<Player> options) {
        // logic?
        return Iterables.getFirst(options, null);
    }
}
