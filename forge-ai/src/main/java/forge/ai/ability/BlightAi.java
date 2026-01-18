package forge.ai.ability;

import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.ComputerUtilCard;
import forge.ai.SpellAbilityAi;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.card.CounterEnumType;
import forge.game.player.Player;
import forge.game.player.PlayerCollection;
import forge.game.player.PlayerPredicates;
import forge.game.spellability.SpellAbility;
import forge.util.StreamUtil;

import java.util.Map;
import java.util.Optional;

public class BlightAi extends SpellAbilityAi {

    @Override
    protected AiAbilityDecision checkApiLogic(Player ai, SpellAbility sa) {
        return canPlayWithTargeting(ai, sa, /*mandatory=*/ false);
    }

    @Override
    protected AiAbilityDecision doTriggerNoCost(final Player ai, final SpellAbility sa, final boolean mandatory) {
        return canPlayWithTargeting(ai, sa, mandatory);
    }

    private AiAbilityDecision canPlayWithTargeting(Player ai, SpellAbility sa, boolean mandatory) {
        if (!sa.usesTargeting()) {
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        }

        sa.resetTargets();
        PlayerCollection opponents = ai.getOpponents().filter(PlayerPredicates.isTargetableBy(sa));
        int blightAmount = AbilityUtils.calculateAmount(
                sa.getHostCard(), sa.getParamOrDefault("Num", "1"), sa);

        // Prioritize opponents whose worst blightable creature would die
        Optional<Player> target = opponents.stream()
                .filter(this::hasBlightableCreatures).min((a, b) -> Boolean.compare(
                        canKillWorstCreature(b, blightAmount),
                        canKillWorstCreature(a, blightAmount)));

        if (target.isPresent()) {
            sa.getTargets().add(target.get());
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        }

        if (mandatory && !opponents.isEmpty()) {
            sa.getTargets().add(opponents.getFirst());
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        }

        return new AiAbilityDecision(0, AiPlayDecision.TargetingFailed);
    }

    private boolean hasBlightableCreatures(Player player) {
        return player.getCreaturesInPlay().stream()
                .anyMatch(c -> c.canReceiveCounters(CounterEnumType.M1M1));
    }

    private boolean canKillWorstCreature(Player player, int blightAmount) {
        Card worst = ComputerUtilCard.getWorstCreatureAI(
                CardLists.filter(player.getCreaturesInPlay(),
                        c -> c.canReceiveCounters(CounterEnumType.M1M1)));
        return worst != null && worst.getNetToughness() <= blightAmount;
    }

    @Override
    protected Card chooseSingleCard(
            Player ai,
            SpellAbility sa,
            Iterable<Card> options,
            boolean isOptional,
            Player targetedPlayer,
            Map<String, Object> params
    ) {
        Optional<Card> filtered = StreamUtil.stream(options).filter(
                c -> !c.canReceiveCounters(CounterEnumType.M1M1)).findAny();
        return filtered.orElseGet(() -> ComputerUtilCard.getWorstCreatureAI(options));
    }
}
