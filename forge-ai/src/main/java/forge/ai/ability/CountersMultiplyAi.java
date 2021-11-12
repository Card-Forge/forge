package forge.ai.ability;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.base.Predicate;
import com.google.common.collect.Lists;

import forge.ai.ComputerUtil;
import forge.ai.ComputerUtilCost;
import forge.ai.SpellAbilityAi;
import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.card.CounterEnumType;
import forge.game.card.CounterType;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

public class CountersMultiplyAi extends SpellAbilityAi {

    @Override
    protected boolean checkApiLogic(Player ai, SpellAbility sa) {
        final CounterType counterType = getCounterType(sa);

        if (!sa.usesTargeting()) {
            // defined are mostly Self or Creatures you control
            CardCollection list = AbilityUtils.getDefinedCards(sa.getHostCard(), sa.getParam("Defined"), sa);

            list = CardLists.filter(list, new Predicate<Card>() {

                @Override
                public boolean apply(Card c) {
                    if (!c.hasCounters()) {
                        return false;
                    }

                    if (counterType != null) {
                        if (c.getCounters(counterType) <= 0) {
                            return false;
                        }
                        if (!c.canReceiveCounters(counterType)) {
                            return false;
                        }
                    } else {
                        for (Map.Entry<CounterType, Integer> e : c.getCounters().entrySet()) {
                            // has negative counter it would double
                            if (ComputerUtil.isNegativeCounter(e.getKey(), c)) {
                                return false;
                            }
                        }
                    }

                    return true;
                }

            });

            if (list.isEmpty()) {
                return false;
            }
        } else {
            return setTargets(ai, sa);
        }

        return super.checkApiLogic(ai, sa);
    }

    @Override
    protected boolean checkPhaseRestrictions(final Player ai, final SpellAbility sa, final PhaseHandler ph) {
        final CounterType counterType = getCounterType(sa);

        if (counterType != null && !counterType.is(CounterEnumType.P1P1)) {
            if (!sa.hasParam("ActivationPhases")) {
                // Don't use non P1P1/M1M1 counters before main 2 if possible
                if (ph.getPhase().isBefore(PhaseType.MAIN2) && !ComputerUtil.castSpellInMain1(ai, sa)) {
                    return false;
                }
                if (ph.isPlayerTurn(ai) && !isSorcerySpeed(sa)) {
                    return false;
                }
            }
        }
        if (ComputerUtil.waitForBlocking(sa)) {
            return false;
        }

        return true;
    }

    @Override
    protected boolean doTriggerAINoCost(Player ai, SpellAbility sa, boolean mandatory) {
        return !sa.usesTargeting() || setTargets(ai, sa) || mandatory;
    }

    private CounterType getCounterType(SpellAbility sa) {
        if (sa.hasParam("CounterType")) {
            try {
                return AbilityUtils.getCounterType(sa.getParam("CounterType"), sa);
            } catch (Exception e) {
                System.out.println("Counter type doesn't match, nor does an SVar exist with the type name.");
                return null;
            }
        }
        return null;
    }

    private boolean setTargets(Player ai, SpellAbility sa) {
        final CounterType counterType = getCounterType(sa);

        final Game game = ai.getGame();

        CardCollection list = CardLists.getTargetableCards(game.getCardsIn(ZoneType.Battlefield), sa);

        // pre filter targetable cards with counters and can receive one of them
        list = CardLists.filter(list, new Predicate<Card>() {

            @Override
            public boolean apply(Card c) {
                if (!c.hasCounters()) {
                    return false;
                }

                if (counterType != null) {
                    if (c.getCounters(counterType) <= 0) {
                        return false;
                    }
                    if (!c.canReceiveCounters(counterType)) {
                        return false;
                    }
                }

                return true;
            }

        });

        CardCollection aiList = CardLists.filterControlledBy(list, ai);
        if (!aiList.isEmpty()) {
            // counter type list to check
            // first loyalty, then P1P1, then Charge Counter
            List<CounterEnumType> typeList = Lists.newArrayList(CounterEnumType.LOYALTY, CounterEnumType.P1P1, CounterEnumType.CHARGE);
            for (CounterEnumType type : typeList) {
                // enough targets
                if (!sa.canAddMoreTarget()) {
                    break;
                }

                if (counterType == null || counterType.is(type)) {
                    addTargetsByCounterType(ai, sa, aiList, CounterType.get(type));
                }
            }
        }

        CardCollection oppList = CardLists.filterControlledBy(list, ai.getOpponents());
        if (!oppList.isEmpty()) {
            // not enough targets
            if (sa.canAddMoreTarget()) {
                final CounterType type = CounterType.get(CounterEnumType.M1M1);
                if (counterType == null || counterType == type) {
                    addTargetsByCounterType(ai, sa, oppList, type);
                }
            }
        }

        // targeting does failed
        if (!sa.isTargetNumberValid() || sa.getTargets().size() == 0) {
            sa.resetTargets();
            return false;
        }

        return true;
    }

    private void addTargetsByCounterType(final Player ai, final SpellAbility sa, final CardCollection list,
            final CounterType type) {
        CardCollection newList = CardLists.filter(list, CardPredicates.hasCounter(type));
        if (newList.isEmpty()) {
            return;
        }

        newList.sort(Collections.reverseOrder(CardPredicates.compareByCounterType(type)));
        while (sa.canAddMoreTarget()) {
            if (newList.isEmpty()) {
                break;
            }

            Card c = newList.remove(0);
            sa.getTargets().add(c);

            // check if Spell with Strive is still playable
            if (sa.isSpell() && sa.getHostCard().hasStartOfKeyword("Strive")) {
                // if not remove target again and break list
                if (!ComputerUtilCost.canPayCost(sa, ai)) {
                    sa.getTargets().remove(c);
                    break;
                }
            }
        }
    }
}
