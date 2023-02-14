package forge.game.staticability;

import com.google.common.collect.Lists;
import forge.game.GameEntity;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.util.collect.FCollectionView;

import java.util.List;

public class StaticAbilityPlayerMustAttack {

    static String MODE = "PlayerMustAttack";

    public static List<GameEntity> mustAttackSpecific(final Player attackingPlayer, final FCollectionView<GameEntity> possibleDefenders) {
        List<GameEntity> defToAtt = Lists.newArrayList();
        for (final Card ca : attackingPlayer.getGame().getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : ca.getStaticAbilities()) {
                List<GameEntity> defined = Lists.newArrayList();
                if (!stAb.getParam("Mode").equals(MODE) || stAb.isSuppressed() || !stAb.checkConditions()
                        || !stAb.matchesValidParam("ValidPlayer", attackingPlayer)
                        || !stAb.hasParam("MustAttack")) {
                    continue;
                }
                for (GameEntity ge : possibleDefenders) {
                    if (stAb.matchesValidParam("MustAttack", ge)) {
                        defToAtt.add(ge);
                    }
                }
            }
        }
        return defToAtt;
    }

    public static boolean mustAttack(final Player attackingPlayer) {
        for (final Card ca : attackingPlayer.getGame().getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : ca.getStaticAbilities()) {
                if (!stAb.getParam("Mode").equals(MODE) || stAb.isSuppressed() || !stAb.checkConditions()
                        || !stAb.matchesValidParam("ValidPlayer", attackingPlayer)
                        || stAb.hasParam("MustAttack")) {
                    continue;
                }
                return true;
            }
        }
        return false;
    }

}
