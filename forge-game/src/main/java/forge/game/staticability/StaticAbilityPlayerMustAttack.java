package forge.game.staticability;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

import java.util.ArrayList;
import java.util.List;

public class StaticAbilityPlayerMustAttack {

    static String MODE = "PlayerMustAttack";

    public static int numMustAttackWith(final Player attackingPlayer) {
        final Game game = attackingPlayer.getGame();
        List<Integer> amounts = new ArrayList<>();
        for (final Card ca : game.getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : ca.getStaticAbilities()) {
                if (!stAb.getParam("Mode").equals(MODE) || stAb.isSuppressed() || !stAb.checkConditions()) {
                    continue;
                }
                if (stAb.matchesValidParam("ValidPlayer", attackingPlayer)) {
                    amounts.add(AbilityUtils.calculateAmount(ca, stAb.getParamOrDefault("NumCreatures", "1"), stAb));
                }
            }
        }
        int amount = -1;
        for (int i : amounts) {
            amount = Math.max(i, amount);
        }
        return amount;
    }
}
