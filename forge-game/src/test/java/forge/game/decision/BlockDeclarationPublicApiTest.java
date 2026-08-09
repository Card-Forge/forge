package forge.game.decision;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

public class BlockDeclarationPublicApiTest {

    @Test
    public void blockDecisionAndPublicTypesExposeOnlyNeutralValues() throws Exception {
        AssertJUnit.assertTrue(Arrays.asList(DecisionType.values()).contains(DecisionType.valueOf("BLOCK")));

        final String[] publicTypeNames = {
                "forge.game.decision.BlockDeclarationCard",
                "forge.game.decision.BlockDeclarationAssignment",
                "forge.game.decision.BlockDeclarationContext",
                "forge.game.decision.LegalCandidate"
        };
        for (final String typeName : publicTypeNames) {
            final Class<?> type = loadType(typeName);
            for (final Field field : type.getFields()) {
                AssertJUnit.assertFalse(typeName + " exposes " + field.getType(),
                        Modifier.isPublic(field.getModifiers()) && isLiveForgeType(field.getType()));
            }
            for (final Method method : type.getMethods()) {
                if (!Modifier.isPublic(method.getModifiers())) {
                    continue;
                }
                AssertJUnit.assertFalse(typeName + " exposes " + method.getReturnType() + " from " + method,
                        isLiveForgeType(method.getReturnType()));
                for (final Class<?> parameterType : method.getParameterTypes()) {
                    AssertJUnit.assertFalse(typeName + " accepts " + parameterType + " from " + method,
                            isLiveForgeType(parameterType));
                }
            }
        }
    }

    private static Class<?> loadType(final String typeName) {
        try {
            return Class.forName(typeName);
        } catch (final ClassNotFoundException ex) {
            AssertJUnit.fail("Missing public BLOCK type: " + typeName);
            return Object.class;
        }
    }

    private static boolean isLiveForgeType(final Class<?> type) {
        final Package typePackage = type.getPackage();
        if (typePackage == null || !typePackage.getName().startsWith("forge.")) {
            return false;
        }
        final String name = type.getName();
        return name.equals("forge.game.card.Card")
                || name.equals("forge.game.player.Player")
                || name.equals("forge.game.combat.Combat")
                || name.equals("forge.game.GameEntity")
                || name.equals("forge.game.entity.GameEntity")
                || name.equals("forge.game.spellability.SpellAbility");
    }
}
