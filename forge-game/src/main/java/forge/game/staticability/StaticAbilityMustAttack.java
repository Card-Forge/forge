package forge.game.staticability;

import forge.game.Game;
import forge.game.GameEntity;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

import java.util.ArrayList;
import java.util.List;

public class StaticAbilityMustAttack {

    static String MODE = "MustAttack";

    public static List<GameEntity> entitiesMustAttack(final Card attacker) {
        final List<GameEntity> entityList = new ArrayList<>();
        final Game game = attacker.getGame();
        for (final Card ca : game.getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : ca.getStaticAbilities()) {
                if (!stAb.getParam("Mode").equals(MODE) || stAb.isSuppressed() || !stAb.checkConditions()) {
                    continue;
                }
                if (stAb.matchesValidParam("ValidCreature", attacker)) {
                    if (stAb.hasParam("MustAttack")) {
                        List<GameEntity> def = AbilityUtils.getDefinedEntities(stAb.getHostCard(),
                                stAb.getParam("MustAttack"), stAb);
                        for (GameEntity e : def) {
                            if (e instanceof Player) {
                                Player attackPl = (Player) e;
                                if (!game.getPhaseHandler().isPlayerTurn(attackPl)) { // CR 506.2
                                    entityList.add(e);
                                }
                            } else if (e instanceof Card) {
                                Card attackPW = (Card) e;
                                if (!game.getPhaseHandler().isPlayerTurn(attackPW.getController())) { // CR 506.2
                                    entityList.add(e);
                                }
                            }
                        }
                    } else { // if the list is only the attacker, the attacker must attack, but no specific entity
                        entityList.add(attacker);
                    }
                }
            }
        }
        return entityList;
    }
}
