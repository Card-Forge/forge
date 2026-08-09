package forge.game.decision;

import forge.game.GameEntity;
import forge.game.card.Card;
import forge.game.combat.Combat;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import org.testng.annotations.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import static org.testng.Assert.assertFalse;

public class AttackDeclarationPublicApiTest {
    private static final List<Class<?>> FORGE_LIVE_TYPES = List.of(Card.class, Player.class, GameEntity.class,
            Combat.class, SpellAbility.class);

    @Test
    public void publicAttackIdentityDtosContainOnlyNeutralValues() {
        assertNeutral(AttackDeclarationCard.class);
        assertNeutral(AttackDeclarationDefender.class);
        assertNeutral(AttackDeclarationAssignment.class);
        assertNeutral(AttackDeclarationContext.class);
    }

    @Test
    public void publicAttackIdentityDtosHaveNoLiveAccessors() {
        assertFalse(hasPublicMethod(AttackDeclarationCard.class, "getLiveCard"));
        assertFalse(hasPublicMethod(AttackDeclarationDefender.class, "getLiveEntity"));
    }

    private static void assertNeutral(final Class<?> dtoType) {
        for (final Field field : dtoType.getDeclaredFields()) {
            assertFalse(isForgeLiveType(field.getType()),
                    dtoType.getSimpleName() + " contains live field " + field.getName());
        }
        for (final Method method : dtoType.getMethods()) {
            assertFalse(isForgeLiveType(method.getReturnType()),
                    dtoType.getSimpleName() + " exposes live return type " + method.getName());
        }
    }

    private static boolean hasPublicMethod(final Class<?> type, final String name) {
        for (final Method method : type.getMethods()) {
            if (method.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isForgeLiveType(final Class<?> type) {
        for (final Class<?> liveType : FORGE_LIVE_TYPES) {
            if (liveType.isAssignableFrom(type)) {
                return true;
            }
        }
        return false;
    }
}
