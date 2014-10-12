package forge.ai.ability;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

import forge.ai.*;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.card.CounterType;
import forge.game.combat.CombatUtil;
import forge.game.cost.Cost;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerType;
import forge.game.zone.ZoneType;
import forge.util.Aggregates;
import forge.util.MyRandom;

import java.util.List;
import java.util.Map;
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
        
        if (abCost != null) {
            // AI currently disabled for these costs
            if (!ComputerUtilCost.checkLifeCost(ai, abCost, source, 4, null)) {
                return false;
            }

            if (!ComputerUtilCost.checkDiscardCost(ai, abCost, source)) {
                return false;
            }

            if (!ComputerUtilCost.checkRemoveCounterCost(abCost, source)) {
                return false;
            }
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
        	final AbilitySub tgtFight = sa.getSubAbility();
            CardCollection aiCreatures = ai.getCreaturesInPlay();
            aiCreatures = CardLists.getTargetableCards(aiCreatures, sa);
            aiCreatures =  ComputerUtil.getSafeTargets(ai, sa, aiCreatures);
            ComputerUtilCard.sortByEvaluateCreature(aiCreatures);

            CardCollection humCreatures = ai.getOpponent().getCreaturesInPlay();
            humCreatures = CardLists.getTargetableCards(humCreatures, tgtFight);
            ComputerUtilCard.sortByEvaluateCreature(humCreatures);
            if (humCreatures.isEmpty() || aiCreatures.isEmpty()) {
            	return false;
            }
            for (Card humanCreature : humCreatures) {
            	for (Card aiCreature : aiCreatures) {
            	    if (sa.isSpell()) {   //heroic triggers adding counters
                        for (Trigger t : aiCreature.getTriggers()) {
                            if (t.getMode() == TriggerType.SpellCast) {
                                final Map<String, String> params = t.getMapParams();
                                if ("Card.Self".equals(params.get("TargetsValid")) && "You".equals(params.get("ValidActivatingPlayer")) 
                                		&& params.containsKey("Execute")) {
                                    SpellAbility heroic = AbilityFactory.getAbility(aiCreature.getSVar(params.get("Execute")),aiCreature);
                                    if ("Self".equals(heroic.getParam("Defined")) && "P1P1".equals(heroic.getParam("CounterType"))) {
                                        int n = AbilityUtils.calculateAmount(aiCreature, heroic.getParam("CounterNum"), heroic);
                                        nPump += n;
                                    }
                                    break;
                                }
                            }
                        }
                    }
            		if (FightAi.shouldFight(aiCreature, humanCreature, nPump, nPump)) {
            			sa.getTargets().add(aiCreature);
            			tgtFight.getTargets().add(humanCreature);
            			return true;
            		}
            	}
            }
        }
        
        if (amountStr.equals("X") && source.getSVar(amountStr).equals("Count$xPaid")) {
            // Set PayX here to maximum value.
            amount = ComputerUtilMana.determineLeftoverMana(sa, ai);
            source.setSVar("PayX", Integer.toString(amount));
        }
        if ("Polukranos".equals(sa.getParam("AILogic"))) {
            List<Card> humCreatures = ai.getOpponent().getCreaturesInPlay();
            //TODO how to grab target restrictions from subsequent triggered ability?
            if (!humCreatures.isEmpty()){
                boolean canSurvive = false;
                for (Card humanCreature : humCreatures) {
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

        // don't use it if no counters to add
        if (amount <= 0) {
            return false;
        }

        // Targeting
        if (abTgt != null) {
            sa.resetTargets();
            // target loop

            list = CardLists.filter(player.getCardsIn(ZoneType.Battlefield), new Predicate<Card>() {
                @Override
                public boolean apply(final Card c) {
                    return c.canBeTargetedBy(sa) && c.canReceiveCounters(CounterType.valueOf(type));
                }
            });

            list = CardLists.getValidCards(list, abTgt.getValidTgts(), source.getController(), source);

            if (list.size() < abTgt.getMinTargets(source, sa)) {
                return false;
            }
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
                    choice = CountersAi.chooseBoonTarget(list, type);
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

        // Don't use non P1P1/M1M1 counters before main 2 if possible
        if (ai.getGame().getPhaseHandler().getPhase().isBefore(PhaseType.MAIN2)
                && !sa.hasParam("ActivationPhases")
                && !(type.equals("P1P1") || type.equals("M1M1"))
                && !ComputerUtil.castSpellInMain1(ai, sa)) {
            return false;
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
                    CardLists.getValidCards(player.getCardsIn(ZoneType.Battlefield), abTgt.getValidTgts(), source.getController(), source);

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
                    choice = CountersAi.chooseBoonTarget(list, type);
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
            list = CardLists.getValidCards(list, abTgt.getValidTgts(), source.getController(), source);

            if (list.isEmpty() && mandatory) {
                // If there isn't any prefered cards to target, gotta choose
                // non-preferred ones
                list = CardLists.getTargetableCards(player.getOpponent().getCardsIn(ZoneType.Battlefield), sa);
                list = CardLists.getValidCards(list, abTgt.getValidTgts(), source.getController(), source);
                preferred = false;
            }
            // Not mandatory, or the the list was regenerated and is still
            // empty,
            // so return false since there are no targets
            if (list.isEmpty()) {
                return false;
            }

            Card choice = null;

            // Choose targets here:
            if (isCurse) {
                if (preferred) {
                    choice = CountersAi.chooseCursedTarget(list, type, amount);
                }

                else {
                    if (type.equals("M1M1")) {
                        choice = ComputerUtilCard.getWorstCreatureAI(list);
                    } else {
                        choice = Aggregates.random(list);
                    }
                }
            }
            else {
                if (preferred) {
                    choice = CountersAi.chooseBoonTarget(list, type);
                }
                else {
                    if (type.equals("P1P1")) {
                        choice = ComputerUtilCard.getWorstCreatureAI(list);
                    } else {
                        choice = Aggregates.random(list);
                    }
                }
                if (choice !=  null && divided) {
                    abTgt.addDividedAllocation(choice, amount);
                }
            }

            // TODO - I think choice can be null here. Is that ok for
            // addTarget()?
            sa.getTargets().add(choice);
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
                            && (c.getNetDefense() > source.getNetAttack() + tributeAmount || c.hasKeyword("DeathTouch"));
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
                            return CombatUtil.canBlock(source, c) && (c.getNetDefense() > source.getNetAttack() || c.hasKeyword("DeathTouch"));
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
