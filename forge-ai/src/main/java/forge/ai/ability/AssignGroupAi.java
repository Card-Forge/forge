package forge.ai.ability;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Iterables;

import forge.ai.SpellAbilityAi;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

public class AssignGroupAi extends SpellAbilityAi {

    public AssignGroupAi() {
        // TODO Auto-generated constructor stub
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
