package forge.ai.ability;

import com.google.common.collect.Iterables;
import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.SpellAbilityAi;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

import java.util.List;
import java.util.Map;

public class AssignGroupAi extends SpellAbilityAi {

    @Override
    protected AiAbilityDecision canPlay(Player ai, SpellAbility sa) {
        // TODO: Currently this AI relies on the card-specific limiting hints (NeedsToPlay / NeedsToPlayVar),
        // otherwise the AI considers the card playable.
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    public SpellAbility chooseSingleSpellAbility(Player player, SpellAbility sa, List<SpellAbility> spells, Map<String, Object> params) {
        final String logic = sa.getParamOrDefault("AILogic", "");
        
        if (logic.equals("FriendOrFoe")) {
            if (params.containsKey("Affected") && spells.size() >= 2) {
                Player t = (Player) params.get("Affected");
                return spells.get(player.isOpponentOf(t) ? 1 : 0);
            }
        }

        return Iterables.getFirst(spells, null);
    }
}
