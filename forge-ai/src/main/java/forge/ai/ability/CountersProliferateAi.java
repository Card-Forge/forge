package forge.ai.ability;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import forge.ai.AiProps;
import forge.ai.ComputerUtil;
import forge.ai.ComputerUtilCard;
import forge.ai.PlayerControllerAi;
import forge.ai.SpellAbilityAi;
import forge.game.GameEntity;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.card.CardUtil;
import forge.game.card.CounterEnumType;
import forge.game.card.CounterType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

public class CountersProliferateAi extends SpellAbilityAi {

    @Override
    protected boolean checkApiLogic(Player ai, SpellAbility sa) {
        final List<Card> cperms = Lists.newArrayList();
        final List<Player> allies = ai.getAllies();
        allies.add(ai);
        boolean allyExpOrEnergy = false;

        for (final Player p : allies) {
        	// player has experience or energy counter
            if (p.getCounters(CounterEnumType.EXPERIENCE) + p.getCounters(CounterEnumType.ENERGY) >= 1) {
                allyExpOrEnergy = true;
            }
            cperms.addAll(CardLists.filter(p.getCardsIn(ZoneType.Battlefield), new Predicate<Card>() {
                @Override
                public boolean apply(final Card crd) {
                    if (!crd.hasCounters()) {
                        return false;
                    }

                    if (crd.isPlaneswalker()) {
                        return true;
                    }

                    // iterate only over existing counters
                    for (final Map.Entry<CounterType, Integer> e : crd.getCounters().entrySet()) {
                        if (e.getValue() >= 1 && !ComputerUtil.isNegativeCounter(e.getKey(), crd)) {
                            return true;
                        }
                    }
                    return false;
                }
            }));
        }

        final List<Card> hperms = Lists.newArrayList();
        boolean opponentPoison = false;

        for (final Player o : ai.getOpponents()) {
            opponentPoison |= o.getPoisonCounters() > 0 && o.canReceiveCounters(CounterEnumType.POISON);
            hperms.addAll(CardLists.filter(o.getCardsIn(ZoneType.Battlefield), new Predicate<Card>() {
                @Override
                public boolean apply(final Card crd) {
                    if (!crd.hasCounters()) {
                        return false;
                    }

                    if (crd.isPlaneswalker()) {
                        return false;
                    }

                    // iterate only over existing counters
                    for (final Map.Entry<CounterType, Integer> e : crd.getCounters().entrySet()) {
                        if (e.getValue() >= 1 && ComputerUtil.isNegativeCounter(e.getKey(), crd)) {
                            return true;
                        }
                    }
                    return false;
                }
            }));
        }

        return !cperms.isEmpty() || !hperms.isEmpty() || opponentPoison || allyExpOrEnergy;
    }

    @Override
    protected boolean doTriggerAINoCost(Player aiPlayer, SpellAbility sa, boolean mandatory) {
        boolean chance = true;

        // TODO Make sure Human has poison counters or there are some counters
        // we want to proliferate
        return chance;
    }

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#chkAIDrawback(java.util.Map, forge.card.spellability.SpellAbility, forge.game.player.Player)
     */
    @Override
    public boolean chkAIDrawback(SpellAbility sa, Player ai) {
        if ("Always".equals(sa.getParam("AILogic"))) {
            return true;
        }

        return checkApiLogic(ai, sa);
    }

    /*
     * (non-Javadoc)
     * @see forge.ai.SpellAbilityAi#chooseSingleEntity(forge.game.player.Player, forge.game.spellability.SpellAbility, java.util.Collection, boolean, forge.game.player.Player)
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T extends GameEntity> T chooseSingleEntity(Player ai, SpellAbility sa, Collection<T> options, boolean isOptional, Player targetedPlayer, Map<String, Object> params) {
        // Proliferate is always optional for all, no need to select best

        final CounterType poison = CounterType.get(CounterEnumType.POISON);

        boolean aggroAI = (((PlayerControllerAi) ai.getController()).getAi()).getBooleanProperty(AiProps.PLAY_AGGRO);
        // because countertype can't be chosen anymore, only look for poison counters
        for (final Player p : Iterables.filter(options, Player.class)) {
            if (p.isOpponentOf(ai)) {
                if (p.getCounters(poison) > 0 && p.canReceiveCounters(poison)) {
                    return (T)p;
                }
            } else {
                // poison is risky, should not proliferate them in most cases
                if ((((p.getCounters(poison) <= 5 && aggroAI) || (p.getCounters(poison) == 0)) && p.getCounters(CounterEnumType.EXPERIENCE) + p.getCounters(CounterEnumType.ENERGY) >= 1) || !p.canReceiveCounters(poison)) {
                    return (T)p;
                }
            }
        }

        for (final Card c : Iterables.filter(options, Card.class)) {
            // AI planeswalker always, opponent planeswalkers never
            if (c.isPlaneswalker()) {
                if (c.getController().isOpponentOf(ai)) {
                    continue;
                } else {
                    return (T)c;
                }
            }

            final Card lki = CardUtil.getLKICopy(c);
            // update all the counters there
            boolean hasNegative = false;
            for (final CounterType ct : c.getCounters().keySet()) {
                hasNegative = hasNegative || ComputerUtil.isNegativeCounter(ct, c);
                lki.setCounters(ct, lki.getCounters(ct) + 1);
            }

            // TODO need more logic there?
            // it tries to evaluate the creatures
            if (c.isCreature()) {
                if (c.getController().isOpponentOf(ai) ==
                        (ComputerUtilCard.evaluateCreature(lki, true, false)
                                < ComputerUtilCard.evaluateCreature(c, true, false))) {
                    return (T)c;
                }
            } else {
                if (!c.getController().isOpponentOf(ai) && !hasNegative) {
                    return (T)c;
                }
            }
        }

        return null;
    }
}
