package forge.ai.ability;

import java.util.Map;

import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.SpellAbilityAi;
import forge.game.ability.AbilityUtils;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.spellability.SpellAbility;

/**
 * GameAction.becomeMonarch returns immediately when the chosen player is already the monarch, so
 * an ability that would only crown the current monarch again does nothing at all. Cards such as
 * Throne of the High City and King Solomon's Frogs sacrifice or exile themselves as part of the
 * cost, so activating one redundantly throws a permanent away for no effect.
 */
public class BecomeMonarchAi extends SpellAbilityAi {

    @Override
    protected AiAbilityDecision canPlay(Player aiPlayer, SpellAbility sa) {
        if (changesNothing(aiPlayer, sa)) {
            return new AiAbilityDecision(0, AiPlayDecision.DoesntImpactGame);
        }
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    @Override
    public boolean confirmAction(Player player, SpellAbility sa, PlayerActionConfirmMode mode, String message,
            Map<String, Object> params) {
        return !changesNothing(player, sa);
    }

    /**
     * True only when every player the ability would crown is already the monarch. Targeted
     * abilities are left alone, since some of them deliberately hand the crown to an opponent to
     * enable another effect on the same card.
     */
    private static boolean changesNothing(Player aiPlayer, SpellAbility sa) {
        if (sa.usesTargeting()) {
            return false;
        }
        final Player monarch = aiPlayer.getGame().getMonarch();
        if (monarch == null) {
            return false;
        }
        boolean anyAffected = false;
        // mirrors how SpellAbilityEffect resolves "Defined" for this effect
        for (String def : sa.getParamOrDefault("Defined", "You").split(" & ")) {
            for (Player p : AbilityUtils.getDefinedPlayers(sa.getHostCard(), def, sa)) {
                anyAffected = true;
                if (!monarch.equals(p)) {
                    return false;
                }
            }
        }
        return anyAffected;
    }
}
