package forge.game.staticability;

import forge.game.Game;
import forge.game.GameEntity;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.util.collect.FCollectionView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StaticAbilityMustAttack {

    static String MODE_Creature = "MustAttack";
    static String MODE_Player = "PlayerMustAttack";

    public static List<GameEntity> entitiesMustAttack(final Card attacker) {
        final List<GameEntity> entityList = new ArrayList<>();
        final Game game = attacker.getGame();
        for (final Card ca : game.getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : ca.getStaticAbilities()) {
                if (!stAb.checkConditions(MODE_Creature)) {
                    continue;
                }
                if (stAb.matchesValidParam("ValidCreature", attacker)) {
                    if (stAb.hasParam("MustAttack")) {
                        List<GameEntity> def = AbilityUtils.getDefinedEntities(stAb.getHostCard(), stAb.getParam("MustAttack"), stAb);
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

    public static List<Set<GameEntity>> mustAttackSpecific(final Player attackingPlayer, final FCollectionView<GameEntity> possibleDefenders) {
        List<Set<GameEntity>> defToAtt = new ArrayList<>();
        for (final Card ca : attackingPlayer.getGame().getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : ca.getStaticAbilities()) {
                if (!stAb.checkConditions(MODE_Player)) {
                    continue;
                }
                if (!stAb.matchesValidParam("ValidPlayer", attackingPlayer)) {
                    continue;
                }
                Set<GameEntity> attackWithOne = new HashSet<>();
                for (GameEntity ge : possibleDefenders) {
                    if (stAb.matchesValidParam("MustAttack", ge)) {
                        attackWithOne.add(ge);
                    }
                }
                defToAtt.add(attackWithOne);
            }
        }
        return defToAtt;
    }
}
