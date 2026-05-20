package forge.game.staticability;

import forge.game.Game;
import forge.game.GameEntity;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.util.collect.FCollectionView;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;

public class StaticAbilityMustAttack {

    public static List<GameEntity> entitiesMustAttack(final Card attacker) {
        final List<GameEntity> entityList = new ArrayList<>();
        final Game game = attacker.getGame();
        for (final Card ca : game.getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : ca.getStaticAbilities()) {
                if (!stAb.checkConditions(StaticAbilityMode.MustAttack)) {
                    continue;
                }
                if (stAb.matchesValidParam("ValidCreature", attacker)) {
                    if (stAb.hasParam("MustAttack")) {
                        List<GameEntity> def = AbilityUtils.getDefinedEntities(stAb.getHostCard(), stAb.getParam("MustAttack"), stAb);
                        for (GameEntity e : def) {
                            if ((e instanceof Player attackPl && game.getPhaseHandler().isPlayerTurn(attackPl)) ||
                                    ((e instanceof Card attackPw && game.getPhaseHandler().isPlayerTurn(attackPw.getController())))) {
                                // CR 506.2
                                continue;
                            }
                            entityList.add(e);
                        }
                    } else {
                        // if the list is only the attacker, the attacker must attack, but no specific entity
                        entityList.add(attacker);
                    }
                }
            }
        }
        return entityList;
    }

    public static Multimap<GameEntity, StaticAbility> mustAttackSpecific(final Player attackingPlayer, final FCollectionView<GameEntity> possibleDefenders) {
        Multimap<GameEntity, StaticAbility> result = MultimapBuilder.hashKeys(possibleDefenders.size()).arrayListValues().build();
        for (final Card ca : attackingPlayer.getGame().getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : ca.getStaticAbilities()) {
                if (!stAb.checkConditions(StaticAbilityMode.PlayerMustAttack)) {
                    continue;
                }
                if (!stAb.matchesValidParam("ValidPlayer", attackingPlayer)) {
                    continue;
                }
                for (GameEntity ge : possibleDefenders) {
                    if (stAb.matchesValidParam("MustAttack", ge)) {
                        result.put(ge, stAb);
                    }
                }
            }
        }
        return result;
    }

    public static Multimap<Card, StaticAbility> getAttackRequirements(final Card card, Iterable<Card> other) {
        Multimap<Card, StaticAbility> result = MultimapBuilder.hashKeys().arrayListValues().build();
        for (final Card ca : card.getGame().getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : ca.getStaticAbilities()) {
                if (!stAb.checkConditions(StaticAbilityMode.AttackRequirement)) {
                    continue;
                }

                if (!stAb.matchesValidParam("ValidCard", card)) {
                    continue;
                }
                for (final Card co : other) {
                    if (stAb.matchesValidParam("ValidAttacker", co)) {
                        result.put(co, stAb);
                    }
                }
            }
        }

        return result;
    }
}
