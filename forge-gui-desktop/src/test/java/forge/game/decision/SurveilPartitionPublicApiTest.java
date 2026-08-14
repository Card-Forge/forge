package forge.game.decision;

import forge.ai.AITest;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CardView;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import org.testng.annotations.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.expectThrows;

public class SurveilPartitionPublicApiTest extends AITest {
    @Test
    public void publicCardContainsOnlyOpaqueItemIdAndVisibleName() {
        assertPublicMethodNames(SurveilPartitionCard.class,
                Set.of("getItemId", "getVisibleName"));
        assertPublicMethod(SurveilPartitionCard.class, "getItemId", long.class);
        assertPublicMethod(SurveilPartitionCard.class, "getVisibleName", String.class);
        assertNoPublicFields(SurveilPartitionCard.class);
        assertDeclaredFields(SurveilPartitionCard.class,
                Map.of("itemId", long.class, "visibleName", String.class));
        assertFalse(hasFieldOfType(SurveilPartitionCard.class,
                Card.class, CardView.class));
        assertFalse(hasFieldWithTypeName(SurveilPartitionCard.class, "CardLKI"));
    }

    @Test
    public void contextContainsExactlyTheApprovedPublicFields() {
        assertPublicMethodNames(SurveilPartitionContext.class,
                Set.of("getProfile", "getSurveilSessionId", "getDecisionStepIndex",
                        "getChoosingPlayerId", "getOriginalItemCount",
                        "getVisibleItems", "getCurrentItemId"));
        assertPublicMethod(SurveilPartitionContext.class, "getProfile",
                SurveilPartitionProfile.class);
        assertPublicMethod(SurveilPartitionContext.class, "getSurveilSessionId", long.class);
        assertPublicMethod(SurveilPartitionContext.class, "getDecisionStepIndex", int.class);
        assertPublicMethod(SurveilPartitionContext.class, "getChoosingPlayerId", int.class);
        assertPublicMethod(SurveilPartitionContext.class, "getOriginalItemCount", int.class);
        assertPublicMethod(SurveilPartitionContext.class, "getVisibleItems", List.class);
        assertPublicMethod(SurveilPartitionContext.class, "getCurrentItemId", long.class);
        assertNoPublicFields(SurveilPartitionContext.class);
        assertDeclaredFields(SurveilPartitionContext.class,
                Map.of("profile", SurveilPartitionProfile.class,
                        "surveilSessionId", long.class,
                        "decisionStepIndex", int.class,
                        "choosingPlayerId", int.class,
                        "originalItemCount", int.class,
                        "visibleItems", List.class,
                        "currentItemId", long.class));
        assertFalse(hasFieldWithTypeName(SurveilPartitionContext.class, "CardLKI"));
    }

    @Test
    public void duplicateLookingCardsRemainDistinct() {
        final SurveilPartitionCard first = card(1L, "Island");
        final SurveilPartitionCard second = card(2L, "Island");

        assertNotEquals(first.getItemId(), second.getItemId());
        assertEquals(first.getVisibleName(), second.getVisibleName());
    }

    @Test
    public void cardRejectsNullVisibleName() {
        expectThrows(NullPointerException.class, () -> card(1L, null));
    }

    @Test
    public void contextRejectsNullProfileAndVisibleItems() {
        expectThrows(NullPointerException.class,
                () -> new SurveilPartitionContext(null, 7L, 0, 11, 1,
                        List.of(card(1L, "Island")), 1L));
        expectThrows(NullPointerException.class,
                () -> new SurveilPartitionContext(SurveilPartitionProfile.SURVEIL_PARTITION,
                        7L, 0, 11, 1, null, 1L));
    }

    @Test
    public void contextRejectsNullVisibleItemElements() {
        expectThrows(NullPointerException.class,
                () -> new SurveilPartitionContext(SurveilPartitionProfile.SURVEIL_PARTITION,
                        7L, 0, 11, 2, Arrays.asList(card(1L, "Island"), null), 1L));
    }

    @Test
    public void contextRejectsNegativeCountAndOutOfRangeStep() {
        expectThrows(IllegalArgumentException.class,
                () -> new SurveilPartitionContext(SurveilPartitionProfile.SURVEIL_PARTITION,
                        7L, 0, 11, -1, List.of(), 1L));
        expectThrows(IllegalArgumentException.class,
                () -> new SurveilPartitionContext(SurveilPartitionProfile.SURVEIL_PARTITION,
                        7L, -1, 11, 2, visibleItems(), 1L));
        expectThrows(IllegalArgumentException.class,
                () -> new SurveilPartitionContext(SurveilPartitionProfile.SURVEIL_PARTITION,
                        7L, 2, 11, 2, visibleItems(), 1L));
    }

    @Test
    public void contextRejectsVisibleItemCountMismatch() {
        expectThrows(IllegalArgumentException.class,
                () -> new SurveilPartitionContext(SurveilPartitionProfile.SURVEIL_PARTITION,
                        7L, 0, 11, 2, List.of(card(1L, "Island")), 1L));
    }

    @Test
    public void contextRequiresCurrentItemIdExactlyOnce() {
        expectThrows(IllegalArgumentException.class,
                () -> new SurveilPartitionContext(SurveilPartitionProfile.SURVEIL_PARTITION,
                        7L, 0, 11, 2, visibleItems(), 9L));
        expectThrows(IllegalArgumentException.class,
                () -> new SurveilPartitionContext(SurveilPartitionProfile.SURVEIL_PARTITION,
                        7L, 0, 11, 2,
                        List.of(card(1L, "Island"), card(1L, "Island")), 1L));
    }

    @Test
    public void contextCopiesAndFreezesVisibleItems() {
        final List<SurveilPartitionCard> source =
                new java.util.ArrayList<>(visibleItems());
        final SurveilPartitionContext context = new SurveilPartitionContext(
                SurveilPartitionProfile.SURVEIL_PARTITION, 7L, 0, 11, 2, source, 1L);

        source.clear();
        assertEquals(context.getVisibleItems().size(), 2);
        expectThrows(UnsupportedOperationException.class,
                () -> context.getVisibleItems().add(card(3L, "Mountain")));
    }

    @Test
    public void coordinatorPublicContextUsesCanonicalChooserOrderAndForbidsEnginePayload() {
        final Fixture fixture = fixture("Mountain", "Island", "Forest");
        final SurveilPartitionDecisionProvider provider = new SurveilPartitionDecisionProvider();
        final SurveilPartitionSession session = provider.admit(fixture.chooser(), fixture.cards());
        session.recordNativeMembershipVector(Arrays.asList(
                SurveilPartitionCandidateKind.CLASSIFY_RETAIN,
                SurveilPartitionCandidateKind.CLASSIFY_RETAIN,
                SurveilPartitionCandidateKind.CLASSIFY_RETAIN));
        final DecisionRequest firstRequest = provider.createMembershipRequest(session);
        final SurveilPartitionContext context = firstRequest.getSurveilPartitionContext();

        assertEquals(context.getVisibleItems().stream()
                .map(SurveilPartitionCard::getVisibleName).toList(),
                List.of("Forest", "Island", "Mountain"));
        assertEquals(context.getOriginalItemCount(), 3);
        assertEquals(context.getDecisionStepIndex(), 0);
        assertEquals(context.getVisibleItems().stream()
                .map(SurveilPartitionCard::getItemId).collect(Collectors.toSet()).size(), 3);
        for (final SurveilPartitionCard item : context.getVisibleItems()) {
            assertFalse(item.getVisibleName().contains(Long.toString(item.getItemId())));
            assertFalse(item.getVisibleName().contains("950"));
        }

        for (final LegalCandidate candidate : firstRequest.getCandidates()) {
            assertEquals(candidate.getSemanticKey(),
                    "SURVEIL_PARTITION|" + candidate.getSurveilPartitionCandidateKind().name()
                            + "|" + context.getCurrentItemId());
            assertEquals(candidate.getSourceCardId(), -1);
            assertEquals(candidate.getSourceName(), "");
            assertEquals(candidate.getSourceZone(), null);
            assertEquals(candidate.getSpellAbility(), null);
            assertEquals(candidate.getTarget(), null);
            assertEquals(candidate.getTargetName(), "");
            assertEquals(candidate.getTargetZone(), null);
            assertEquals(candidate.getAbilityDescription(), "");
            assertEquals(candidate.getSurveilPartitionCard().getVisibleName(),
                    context.getVisibleItems().stream()
                            .filter(item -> item.getItemId() == context.getCurrentItemId())
                            .findFirst().orElseThrow().getVisibleName());
        }

        for (int step = 0; step < 3; step++) {
            final DecisionRequest request = step == 0 ? firstRequest : provider.createMembershipRequest(session);
            provider.applyMembershipCandidate(session, request.getCandidates().get(1));
        }
        final Map<?, ?> symmetryLabels = (Map<?, ?>) fieldValue(session, "symmetryLabels");
        assertEquals(new HashSet<>(symmetryLabels.keySet()),
                new HashSet<>(List.of("Forest", "Island", "Mountain")));
        for (final Object key : symmetryLabels.keySet()) {
            assertFalse(String.valueOf(key).contains("1"));
            assertFalse(String.valueOf(key).contains("2"));
            assertFalse(String.valueOf(key).contains("3"));
        }
        provider.closeSession(session);
    }

    @Test
    public void publicProjectionContainsNoForbiddenIdentityOrderingOrPrivateEngineTypes() {
        for (final Class<?> type : List.of(SurveilPartitionCard.class, SurveilPartitionContext.class)) {
            for (final Field field : type.getDeclaredFields()) {
                final String name = field.getName().toLowerCase();
                assertFalse(name.contains("card"), type.getSimpleName() + " exposes card field " + field.getName());
                assertFalse(name.contains("native"), type.getSimpleName() + " exposes native field " + field.getName());
                assertFalse(name.contains("timestamp"), type.getSimpleName() + " exposes timestamp field " + field.getName());
                assertFalse(name.contains("ordinal"), type.getSimpleName() + " exposes ordinal field " + field.getName());
                assertFalse(name.contains("zone"), type.getSimpleName() + " exposes zone field " + field.getName());
                assertFalse(name.contains("owner"), type.getSimpleName() + " exposes owner field " + field.getName());
                assertFalse(name.contains("controller"), type.getSimpleName() + " exposes controller field " + field.getName());
                assertFalse(name.contains("rng"), type.getSimpleName() + " exposes RNG field " + field.getName());
                assertFalse(name.contains("ai"), type.getSimpleName() + " exposes AI field " + field.getName());
                assertFalse(hasForbiddenEngineType(field.getType()),
                        type.getSimpleName() + " exposes engine type " + field.getType());
            }
        }
        assertFalse(Arrays.stream(SurveilPartitionContext.class.getDeclaredMethods())
                .anyMatch(method -> method.getName().toLowerCase().contains("feature")
                        || method.getName().toLowerCase().contains("symmetry")
                        || method.getName().toLowerCase().contains("native")
                        || method.getName().toLowerCase().contains("retained")));
    }

    @Test
    public void duplicatePublicSymmetryKeyDoesNotContainItemIdOrNativeStableTuple() {
        final Fixture fixture = fixture("Island", "Island");
        final SurveilPartitionDecisionProvider provider = new SurveilPartitionDecisionProvider();
        final SurveilPartitionSession session = provider.admit(fixture.chooser(), fixture.cards());
        session.recordNativeMembershipVector(Arrays.asList(
                SurveilPartitionCandidateKind.CLASSIFY_RETAIN,
                SurveilPartitionCandidateKind.CLASSIFY_RETAIN));
        final DecisionRequest firstRequest = provider.createMembershipRequest(session);
        final Set<Long> itemIds = firstRequest.getSurveilPartitionContext().getVisibleItems().stream()
                .map(SurveilPartitionCard::getItemId).collect(Collectors.toSet());
        for (int step = 0; step < 2; step++) {
            final DecisionRequest request = step == 0 ? firstRequest : provider.createMembershipRequest(session);
            provider.applyMembershipCandidate(session, request.getCandidates().get(1));
        }
        final Map<?, ?> symmetryLabels = (Map<?, ?>) fieldValue(session, "symmetryLabels");
        assertEquals(symmetryLabels.keySet(), Set.of("Island"));
        for (final Long itemId : itemIds) {
            assertFalse(symmetryLabels.keySet().stream().anyMatch(key -> String.valueOf(key).contains(
                    Long.toString(itemId))));
        }
        provider.closeSession(session);
    }

    private static SurveilPartitionCard card(final long itemId, final String visibleName) {
        return new SurveilPartitionCard(itemId, visibleName);
    }

    private static List<SurveilPartitionCard> visibleItems() {
        return List.of(card(1L, "Island"), card(2L, "Forest"));
    }

    private Fixture fixture(final String... names) {
        final Game game = initAndCreateGame();
        final Player chooser = game.getPlayers().get(1);
        final List<Card> cards = new ArrayList<>();
        for (final String name : names) {
            cards.add(addCardToZone(name, chooser, ZoneType.Hand));
        }
        return new Fixture(game, chooser, cards);
    }

    private static Object fieldValue(final Object target, final String name) {
        try {
            final Field field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return field.get(target);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }

    private static boolean hasForbiddenEngineType(final Class<?> type) {
        final String simpleName = type.getSimpleName();
        return Card.class.isAssignableFrom(type)
                || CardView.class.isAssignableFrom(type)
                || simpleName.equals("CardLKI")
                || simpleName.equals("SpellAbility")
                || simpleName.equals("Player")
                || simpleName.equals("Game")
                || java.util.Random.class.isAssignableFrom(type)
                || simpleName.equals("ZoneType");
    }

    private record Fixture(Game game, Player chooser, List<Card> cards) {
    }

    private static void assertPublicMethodNames(final Class<?> type,
            final Set<String> expectedNames) {
        final Method[] methods = Arrays.stream(type.getDeclaredMethods())
                .filter(method -> Modifier.isPublic(method.getModifiers()))
                .toArray(Method[]::new);
        assertEquals(methods.length, expectedNames.size());
        assertEquals(Arrays.stream(methods).map(Method::getName).collect(Collectors.toSet()),
                expectedNames);
    }

    private static void assertPublicMethod(final Class<?> type, final String name,
            final Class<?> returnType, final Class<?>... parameterTypes) {
        try {
            final Method method = type.getDeclaredMethod(name, parameterTypes);
            assertTrue(Modifier.isPublic(method.getModifiers()));
            assertFalse(Modifier.isStatic(method.getModifiers()));
            assertEquals(method.getReturnType(), returnType);
            assertEquals(Arrays.asList(method.getParameterTypes()),
                    Arrays.asList(parameterTypes));
        } catch (NoSuchMethodException exception) {
            throw new AssertionError(exception);
        }
    }

    private static void assertNoPublicFields(final Class<?> type) {
        assertEquals(type.getFields().length, 0);
        assertEquals(Arrays.stream(type.getDeclaredFields())
                .filter(field -> Modifier.isPublic(field.getModifiers())).count(), 0L);
    }

    private static void assertDeclaredFields(final Class<?> type,
            final Map<String, Class<?>> expectedFields) {
        final Field[] fields = type.getDeclaredFields();
        assertEquals(fields.length, expectedFields.size());
        assertEquals(Arrays.stream(fields).map(Field::getName).collect(Collectors.toSet()),
                expectedFields.keySet());
        for (final Map.Entry<String, Class<?>> expected : expectedFields.entrySet()) {
            try {
                assertEquals(type.getDeclaredField(expected.getKey()).getType(),
                        expected.getValue());
            } catch (NoSuchFieldException exception) {
                throw new AssertionError(exception);
            }
        }
    }

    private static boolean hasFieldOfType(final Class<?> type, final Class<?>... forbiddenTypes) {
        return Arrays.stream(type.getDeclaredFields())
                .map(Field::getType)
                .anyMatch(fieldType -> Arrays.stream(forbiddenTypes)
                        .anyMatch(fieldType::equals));
    }

    private static boolean hasFieldWithTypeName(final Class<?> type, final String forbiddenTypeName) {
        return Arrays.stream(type.getDeclaredFields())
                .map(Field::getType)
                .anyMatch(fieldType -> fieldType.getSimpleName().equals(forbiddenTypeName));
    }

}
