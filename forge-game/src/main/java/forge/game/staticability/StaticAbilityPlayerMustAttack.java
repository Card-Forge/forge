package forge.game.staticability;

import com.google.common.collect.Lists;
import forge.game.Game;
import forge.game.GameEntity;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StaticAbilityPlayerMustAttack {

    static String MODE = "PlayerMustAttack";

    public static Map<Integer, List<GameEntity>> mustAttack(final Player attackingPlayer) {
        final Game game = attackingPlayer.getGame();
        Map<Integer, List<GameEntity>> requirements = new HashMap<>();
        for (final Card ca : game.getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : ca.getStaticAbilities()) {
                int amount = 0;
                List<GameEntity> defined = Lists.newArrayList();
                if (!stAb.getParam("Mode").equals(MODE) || stAb.isSuppressed() || !stAb.checkConditions()) {
                    continue;
                }
                if (stAb.matchesValidParam("ValidPlayer", attackingPlayer)) {
                    amount = AbilityUtils.calculateAmount(ca, stAb.getParamOrDefault("NumCreatures", "1"), stAb);
                }
                if (stAb.hasParam("MustAttack")) {
                    defined.addAll(AbilityUtils.getDefinedEntities(ca, "MustAttack", stAb));
                }
                requirements.put(amount, defined);
            }
        }
        return requirements;
    }
}
