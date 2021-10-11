package forge.ai.ability;

import com.google.common.base.Predicate;

import forge.ai.ComputerUtil;
import forge.ai.SpellAbilityAi;
import forge.game.ability.AbilityUtils;
import forge.game.card.CounterEnumType;
import forge.game.card.CounterType;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.PlayerCollection;
import forge.game.player.PlayerPredicates;
import forge.game.spellability.SpellAbility;

public class PoisonAi extends SpellAbilityAi {

    /*
     * (non-Javadoc)
     * 
     * @see
     * forge.ai.SpellAbilityAi#checkPhaseRestrictions(forge.game.player.Player,
     * forge.game.spellability.SpellAbility, forge.game.phase.PhaseHandler)
     */
    @Override
    protected boolean checkPhaseRestrictions(final Player ai, final SpellAbility sa, final PhaseHandler ph) {
        return !ph.getPhase().isBefore(PhaseType.MAIN2)
                || sa.hasParam("ActivationPhases");
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.ai.SpellAbilityAi#checkApiLogic(forge.game.player.Player,
     * forge.game.spellability.SpellAbility)
     */
    @Override
    protected boolean checkApiLogic(Player ai, SpellAbility sa) {
        // Don't tap creatures that may be able to block
        if (ComputerUtil.waitForBlocking(sa)) {
            return false;
        }

        if (sa.usesTargeting()) {
            sa.resetTargets();
            return tgtPlayer(ai, sa, true);
        }

        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.ai.SpellAbilityAi#doTriggerAINoCost(forge.game.player.Player,
     * forge.game.spellability.SpellAbility, boolean)
     */
    @Override
    protected boolean doTriggerAINoCost(Player ai, SpellAbility sa, boolean mandatory) {
        if (sa.usesTargeting()) {
            return tgtPlayer(ai, sa, mandatory);
        } else if (mandatory || !ai.canReceiveCounters(CounterType.get(CounterEnumType.POISON))) {
            // mandatory or ai is uneffected
            return true;
        } else {
            // currently there are no optional Trigger
            final PlayerCollection players = AbilityUtils.getDefinedPlayers(sa.getHostCard(), sa.getParam("Defined"),
                    sa);
            if (players.isEmpty()) {
                return false;
            }
            // not affected, don't care
            if (!players.contains(ai)) {
                return true;
            }

            Player max = players.max(PlayerPredicates.compareByPoison());
            if (ai.getPoisonCounters() == max.getPoisonCounters()) {
                // ai is one of the max
                return false;
            }
        }
        return true;
    }

    private boolean tgtPlayer(Player ai, SpellAbility sa, boolean mandatory) {
        PlayerCollection tgts = ai.getOpponents().filter(PlayerPredicates.isTargetableBy(sa));
        if (!tgts.isEmpty()) {
            // try to select a opponent that can lose through poison counters
            PlayerCollection betterTgts = tgts.filter(new Predicate<Player>() {
                @Override
                public boolean apply(Player input) {
                    if (input.cantLose()) {
                        return false;
                    } else if (!input.canReceiveCounters(CounterType.get(CounterEnumType.POISON))) {
                        return false;
                    }
                    return true;
                }

            });

            if (!betterTgts.isEmpty()) {
                tgts = betterTgts;
            } else if (mandatory) {
                // no better choice but better than hitting himself
                sa.getTargets().add(tgts.getFirst());
                return true;
            }
        }

        // no opponent can be killed with that
        if (tgts.isEmpty()) {
            if (mandatory) {
                // AI is uneffected
                if (ai.canBeTargetedBy(sa) && ai.canReceiveCounters(CounterType.get(CounterEnumType.POISON))) {
                    sa.getTargets().add(ai);
                    return true;
                }
                // need to target something, try to target allies
                PlayerCollection allies = ai.getAllies().filter(PlayerPredicates.isTargetableBy(sa));
                if (!allies.isEmpty()) {
                    // some ally would be unaffected
                    PlayerCollection betterAllies = allies.filter(new Predicate<Player>() {
                        @Override
                        public boolean apply(Player input) {
                            if (input.cantLose()) {
                                return true;
                            }
                            return !input.canReceiveCounters(CounterType.get(CounterEnumType.POISON));
                        }

                    });
                    if (!betterAllies.isEmpty()) {
                        allies = betterAllies;
                    }

                    Player min = allies.min(PlayerPredicates.compareByPoison());
                    sa.getTargets().add(min);
                    return true;
                } else if (ai.canBeTargetedBy(sa)) {
                    // need to target himself
                    sa.getTargets().add(ai);
                    return true;
                }
            }
            return false;
        }

        // find player with max poison to kill
        Player max = tgts.max(PlayerPredicates.compareByPoison());
        sa.getTargets().add(max);
        return true;
    }
}
