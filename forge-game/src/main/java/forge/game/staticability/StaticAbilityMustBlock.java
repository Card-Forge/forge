package forge.game.staticability;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.zone.ZoneType;

public class StaticAbilityMustBlock {

    public static boolean blocksEachCombatIfAble(final Card creature)  {
        return findBlocksEachCombatIfAble(creature) != null;
    }

    /**
     * @return the first MustBlock static ability requiring the creature to block, or null
     */
    public static StaticAbility findBlocksEachCombatIfAble(final Card creature)  {
        final Game game = creature.getGame();
        for (final Card ca : game.getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : ca.getStaticAbilities()) {
                if (!stAb.checkConditions(StaticAbilityMode.MustBlock)) {
                    continue;
                }
                if (applyBlocksEachCombatIfAble(stAb, creature)) {
                    return stAb;
                }
            }
        }
        return null;
    }

    public static boolean applyBlocksEachCombatIfAble(final StaticAbility stAb, final Card creature) {
        if (!stAb.matchesValidParam("ValidCreature", creature)) {
            return false;
        }
        return true;
    }
}
