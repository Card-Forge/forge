package forge.ai.ability;

import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.ComputerUtil;
import forge.ai.SpellAbilityAi;
import forge.game.ability.AbilityUtils;
import forge.game.card.CounterEnumType;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.GameLossReason;
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
        return !ph.getPhase().isBefore(PhaseType.MAIN2) || sa.hasParam("ActivationPhases");
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.ai.SpellAbilityAi#checkApiLogic(forge.game.player.Player,
     * forge.game.spellability.SpellAbility)
     */
    @Override
    protected AiAbilityDecision checkApiLogic(Player ai, SpellAbility sa) {
        // Don't tap creatures that may be able to block
        if (ComputerUtil.waitForBlocking(sa)) {
            return new AiAbilityDecision(0, AiPlayDecision.WaitForCombat);
        }

        if (sa.usesTargeting()) {
            sa.resetTargets();
            if (tgtPlayer(ai, sa, true)) {
                return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
            } else {
                return new AiAbilityDecision(0, AiPlayDecision.TargetingFailed);
            }
        }

        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.ai.SpellAbilityAi#doTriggerAINoCost(forge.game.player.Player,
     * forge.game.spellability.SpellAbility, boolean)
     */
    @Override
    protected AiAbilityDecision doTriggerNoCost(Player ai, SpellAbility sa, boolean mandatory) {
        boolean result;
        if (sa.usesTargeting()) {
            result = tgtPlayer(ai, sa, mandatory);
        } else if (mandatory || !ai.canReceiveCounters(CounterEnumType.POISON)) {
            // mandatory or ai is uneffected
            result = true;
        } else {
            // currently there are no optional Trigger
            final PlayerCollection players = AbilityUtils.getDefinedPlayers(sa.getHostCard(), sa.getParam("Defined"), sa);
            if (players.isEmpty()) {
                result = false;
            } else if (!players.contains(ai)) {
                result = true;
            } else {
                Player max = players.max(PlayerPredicates.compareByPoison());
                result = ai.getPoisonCounters() != max.getPoisonCounters();
            }
        }
        return result ? new AiAbilityDecision(100, AiPlayDecision.WillPlay) : new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
    }

    private boolean tgtPlayer(Player ai, SpellAbility sa, boolean mandatory) {
        PlayerCollection tgts = ai.getOpponents().filter(PlayerPredicates.isTargetableBy(sa));
        if (!tgts.isEmpty()) {
            // try to select a opponent that can lose through poison counters
            PlayerCollection betterTgts = tgts.filter(input -> {
                if (input.cantLoseCheck(GameLossReason.Poisoned)) {
                    return false;
                } else if (!input.canReceiveCounters(CounterEnumType.POISON)) {
                    return false;
                }
                return true;
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
                if (ai.canBeTargetedBy(sa) && !ai.canReceiveCounters(CounterEnumType.POISON)) {
                    sa.getTargets().add(ai);
                    return true;
                }
                // need to target something, try to target allies
                PlayerCollection allies = ai.getAllies().filter(PlayerPredicates.isTargetableBy(sa));
                if (!allies.isEmpty()) {
                    // some ally would be unaffected
                    PlayerCollection betterAllies = allies.filter(input -> {
                        if (input.cantLoseCheck(GameLossReason.Poisoned)) {
                            return true;
                        }
                        return !input.canReceiveCounters(CounterEnumType.POISON);
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
