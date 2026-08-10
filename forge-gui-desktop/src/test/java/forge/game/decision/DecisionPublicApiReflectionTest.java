package forge.game.decision;

import forge.ai.AITest;
import forge.game.Game;
import forge.game.Match;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.player.Player;
import forge.game.zone.PlayerZone;
import forge.game.spellability.SpellAbility;
import org.testng.annotations.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Set;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

public class DecisionPublicApiReflectionTest extends AITest {
    private static final Set<Class<?>> FORBIDDEN_TYPES = Set.of(Card.class, Player.class, Game.class,
            Match.class, PlayerZone.class, CardCollection.class, SpellAbility.class);

    @Test
    public void publicNeutralDecisionDtosExposeOnlyValueTypes() {
        for (final Class<?> dto : Set.of(MulliganContext.class, CardSelectionContext.class,
                CardSelectionCard.class, LegalCandidate.class)) {
            for (final Method method : dto.getMethods()) {
                assertFalse(FORBIDDEN_TYPES.contains(method.getReturnType()),
                        dto.getSimpleName() + " exposes " + method.getName());
                for (final Class<?> parameter : method.getParameterTypes()) {
                    assertFalse(FORBIDDEN_TYPES.contains(parameter),
                            dto.getSimpleName() + " accepts " + method.getName());
                }
            }
            for (final Field field : dto.getFields()) {
                assertFalse(FORBIDDEN_TYPES.contains(field.getType()),
                        dto.getSimpleName() + " exposes field " + field.getName());
            }
        }
    }

    @Test
    public void sourceAbsentCardSelectionUsesNullableMetadataAndNoSpellAbilityGetter() {
        final Game game = initAndCreateGame();
        final Player chooser = game.getPlayers().get(1);
        final Card card = addCardToZone("Island", chooser, forge.game.zone.ZoneType.Hand);
        final CardSelectionDecisionProvider provider = new CardSelectionDecisionProvider();
        final CardSelectionDecisionProvider.SessionStart start = provider.beginSession(
                chooser, chooser, CardSelectionAdapter.MULLIGAN_BOTTOM, new CardCollection(card), 1, 1,
                new CardCollection(card));
        final CardSelectionContext context = provider.generateNext(start.getSession(), null)
                .getRequest().getCardSelectionContext();

        assertTrue(context.getSelectionAdapter() == CardSelectionAdapter.MULLIGAN_BOTTOM);
        assertNull(context.getSourceCardId());
        assertNull(context.getSourceCardTimestamp());
        assertTrue(CardSelectionContext.class.getMethods().length > 0);
        assertFalse(java.util.Arrays.stream(CardSelectionContext.class.getMethods())
                .anyMatch(method -> method.getName().equals("getSource")));
    }
}
