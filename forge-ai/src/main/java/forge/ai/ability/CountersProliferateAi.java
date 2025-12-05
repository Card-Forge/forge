package forge.ai.ability;

import com.google.common.collect.Lists;
import forge.ai.*;
import forge.game.GameEntity;
import forge.game.card.*;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.IterableUtil;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class CountersProliferateAi extends SpellAbilityAi {

    @Override
    protected AiAbilityDecision checkApiLogic(Player ai, SpellAbility sa) {
        final List<Card> cperms = Lists.newArrayList();
        boolean allyExpOrEnergy = false;

        for (final Player p : ai.getYourTeam()) {
        	// player has experience or energy counter
            if (p.getCounters(CounterEnumType.EXPERIENCE) + p.getCounters(CounterEnumType.ENERGY) >= 1) {
                allyExpOrEnergy = true;
            }
            cperms.addAll(CardLists.filter(p.getCardsIn(ZoneType.Battlefield), crd -> {
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
            }));
        }

        final List<Card> hperms = Lists.newArrayList();
        boolean opponentPoison = false;

        for (final Player o : ai.getOpponents()) {
            opponentPoison |= o.getPoisonCounters() > 0 && o.canReceiveCounters(CounterEnumType.POISON);
            hperms.addAll(CardLists.filter(o.getCardsIn(ZoneType.Battlefield), crd -> {
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
            }));
        }

        if (!cperms.isEmpty() || !hperms.isEmpty() || opponentPoison || allyExpOrEnergy) {
            // AI will play it if there are any counters to proliferate
            // or if there are no counters, but AI has experience or energy counters
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        }

        return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
    }

    @Override
    protected AiAbilityDecision doTriggerNoCost(Player aiPlayer, SpellAbility sa, boolean mandatory) {
        boolean chance = true;

        // TODO Make sure Human has poison counters or there are some counters
        // we want to proliferate
        return new AiAbilityDecision(
            chance ? 100 : 0,
            chance ? AiPlayDecision.WillPlay : AiPlayDecision.CantPlayAi
        );
    }

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#chkAIDrawback(java.util.Map, forge.card.spellability.SpellAbility, forge.game.player.Player)
     */
    @Override
    public AiAbilityDecision chkDrawback(Player ai, SpellAbility sa) {
        if ("Always".equals(sa.getParam("AILogic"))) {
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
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

        final CounterType poison = CounterEnumType.POISON;

        boolean aggroAI = AiProfileUtil.getBoolProperty(ai, AiProps.PLAY_AGGRO);
        // because countertype can't be chosen anymore, only look for poison counters
        for (final Player p : IterableUtil.filter(options, Player.class)) {
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

        for (final Card c : IterableUtil.filter(options, Card.class)) {
            // AI planeswalker always, opponent planeswalkers never
            if (c.isPlaneswalker()) {
                if (c.getController().isOpponentOf(ai)) {
                    continue;
                }
                return (T)c;
            }

            if (c.isBattle()) {
                if (c.getProtectingPlayer().isOpponentOf(ai)) {
                    // TODO in multiplayer we might sometimes want to do it anyway?
                    continue;
                }
                return (T)c;
            }

            final Card lki = CardCopyService.getLKICopy(c);
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
