package forge.game.staticability;

import forge.game.Game;
import forge.game.GameEntity;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

public class StaticAbilityMustAttack {

    static String MODE = "MustAttack";

    public static GameEntity mustAttackSpecific(final Card attacker) {
        final Game game = attacker.getGame();
        for (final Card ca : game.getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : ca.getStaticAbilities()) {
                if (!stAb.getParam("Mode").equals(MODE) || stAb.isSuppressed() || !stAb.checkConditions()) {
                    continue;
                }
                if (stAb.matchesValid(attacker, stAb.getParam("Affected").split(","))) {
                    if (stAb.hasParam("MustAttack")) {
                        GameEntity e = AbilityUtils.getDefinedEntities(attacker, stAb.getParam("MustAttack"),
                                stAb).get(0);
                        if (e instanceof Player) {
                            Player attackPl = (Player) e;
                            if (!game.getPhaseHandler().isPlayerTurn(attackPl)) { // CR 506.2
                                return attackPl;
                            }
                        } else if (e instanceof Card) {
                            Card attackPW = (Card) e;
                            if (!game.getPhaseHandler().isPlayerTurn(attackPW.getController())) { // CR 506.2
                                return attackPW;
                            }
                        }
                    } else { // return attacker to indicate that attacker must attack, but no specific entity
                        return attacker;
                    }
                }
            }
        }
        return null; // null return indicates that attacker is not affected
    }
}
