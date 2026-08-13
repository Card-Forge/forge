package forge.game.decision;

import forge.ai.AITest;
import forge.game.Game;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerType;
import forge.game.trigger.WrappedAbility;
import forge.game.zone.ZoneType;
import org.testng.annotations.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;
import java.lang.reflect.Modifier;
import java.util.concurrent.atomic.AtomicInteger;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

public class BloodOperativeConfirmationDecisionProviderTest extends AITest {
    private final ConfirmationDecisionProvider provider = new ConfirmationDecisionProvider();

    @Test
    public void bloodProfileUsesDedicatedEtbConfirmationTraceLabel() {
        assertEquals(ConfirmationTriggerProfile.BLOOD_OPERATIVE_ETB_EXILE_GRAVEYARD_CARD.getTraceLabel(),
                "BLOOD_ETB_CONFIRMATION");
    }

    @Test
    public void gelectrodeProfileKeepsExistingConfirmationTraceLabel() {
        assertEquals(ConfirmationTriggerProfile.GELECTRODE_SPELL_CAST_UNTAP_SELF.getTraceLabel(),
                "GELECTRODE_CONFIRMATION");
    }

    @Test
    public void bloodExternalResolverOwnsConfirmationWithoutNativeFallback() {
        final ConfirmationDecisionProvider localProvider = new ConfirmationDecisionProvider();
        final AtomicInteger resolverCalls = new AtomicInteger();
        final AtomicInteger nativeCalls = new AtomicInteger();
        localProvider.setResolver(request -> {
            resolverCalls.incrementAndGet();
            return request.getCandidates().stream()
                    .filter(candidate -> candidate.getConfirmationKind() == ConfirmationCandidateKind.ACCEPT)
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("expected ACCEPT candidate"));
        });

        try {
            final ExactBloodFixture fixture = exactBloodFixture();
            final ConfirmationDecisionProvider.Generation generation =
                    localProvider.generate(fixture.wrapper, fixture.decider);

            assertEquals(generation.getStatus(), ConfirmationDecisionProvider.Status.ADMITTED);
            assertEquals(generation.getProfile(),
                    ConfirmationTriggerProfile.BLOOD_OPERATIVE_ETB_EXILE_GRAVEYARD_CARD);
            final DecisionRequest request = generation.getRequest();
            final LegalCandidate selected = localProvider.choose(request, () -> {
                nativeCalls.incrementAndGet();
                return false;
            });

            assertEquals(selected.getConfirmationKind(), ConfirmationCandidateKind.ACCEPT);
            assertEquals(resolverCalls.get(), 1);
            assertEquals(nativeCalls.get(), 0);
            assertTrue(localProvider.apply(request, selected, fixture.wrapper));
        } finally {
            localProvider.setResolver(null);
        }
    }

    @Test
    public void bloodNativeTeacherMapsBooleanExactlyOnce() {
        final ConfirmationDecisionProvider localProvider = new ConfirmationDecisionProvider();
        localProvider.setResolver(null);
        final AtomicInteger nativeCalls = new AtomicInteger();

        try {
            final ExactBloodFixture fixture = exactBloodFixture();
            final ConfirmationDecisionProvider.Generation generation =
                    localProvider.generate(fixture.wrapper, fixture.decider);

            assertEquals(generation.getStatus(), ConfirmationDecisionProvider.Status.ADMITTED);
            final DecisionRequest request = generation.getRequest();
            final LegalCandidate selected = localProvider.choose(request, () -> {
                nativeCalls.incrementAndGet();
                return true;
            });

            assertEquals(selected.getConfirmationKind(), ConfirmationCandidateKind.ACCEPT);
            assertEquals(nativeCalls.get(), 1);
            assertTrue(localProvider.apply(request, selected, fixture.wrapper));
        } finally {
            localProvider.setResolver(null);
        }
    }

    @Test
    public void bloodApplyRequiresTheCapturedDecisionResult() {
        final ConfirmationDecisionProvider localProvider = new ConfirmationDecisionProvider();
        localProvider.setResolver(request -> request.getCandidates().get(0));

        try {
            final ExactBloodFixture fixture = exactBloodFixture();
            final ConfirmationDecisionProvider.Generation generation =
                    localProvider.generate(fixture.wrapper, fixture.decider);

            assertEquals(generation.getStatus(), ConfirmationDecisionProvider.Status.ADMITTED);
            final DecisionRequest request = generation.getRequest();
            assertNotNull(request);
            if (request == null) {
                return;
            }

            boolean rejected = false;
            try {
                localProvider.apply(request, request.getCandidates().get(0), fixture.wrapper);
                fail("apply must not bypass the resolver or native confirmation callback");
            } catch (final IllegalStateException ex) {
                rejected = true;
            }
            assertTrue(rejected);

            try {
                localProvider.choose(request, () -> {
                    fail("an apply failure must invalidate the active request");
                    return false;
                });
                fail("a failed apply must not leave a retryable confirmation request");
            } catch (final RuntimeException ex) {
                // Expected: the apply failure clears the request-local ownership state.
            }
        } finally {
            localProvider.setResolver(null);
        }
    }

    @Test
    public void unsupportedConfirmationExceptionUsesProfileNeutralMessage() {
        final ConfirmationDecisionProvider localProvider = new ConfirmationDecisionProvider();
        localProvider.setResolver(request -> null);

        try {
            final ExactBloodFixture fixture = exactBloodFixture();
            final ConfirmationDecisionProvider.Generation generation =
                    localProvider.generate(fixture.wrapper, fixture.decider);

            assertEquals(generation.getStatus(), ConfirmationDecisionProvider.Status.ADMITTED);
            final DecisionRequest request = generation.getRequest();
            assertNotNull(request);
            if (request == null) {
                return;
            }

            UnsupportedConfirmationDecisionException observed = null;
            try {
                localProvider.choose(request, () -> {
                    fail("external confirmation must not fall back to native Forge");
                    return false;
                });
                fail("expected invalid external confirmation candidate");
            } catch (final UnsupportedConfirmationDecisionException ex) {
                observed = ex;
            } catch (final RuntimeException ex) {
                fail("unexpected confirmation exception: " + ex.getClass().getSimpleName());
            }

            assertNotNull(observed);
            if (observed == null) {
                return;
            }
            assertEquals(observed.getStatus(), ConfirmationDecisionProvider.Status.INVALID_EXTERNAL_CANDIDATE);
            final String message = observed.getMessage();
            assertNotNull(message);
            if (message == null) {
                return;
            }
            assertFalse(message.contains("FRL-02K-B1"));
            assertTrue(message.toLowerCase(Locale.ROOT).contains("confirmation decision"));
        } finally {
            localProvider.setResolver(null);
        }
    }

    @Test
    public void bloodExternalResolverThrowingOrReturningForeignCandidateFailsClosed() {
        final AtomicInteger nativeCalls = new AtomicInteger();
        final ExactBloodFixture fixture = exactBloodFixture();
        final ConfirmationDecisionProvider localProvider = new ConfirmationDecisionProvider();
        localProvider.setResolver(request -> {
            throw new IllegalStateException("resolver failure must be sanitized");
        });

        try {
            final DecisionRequest request = localProvider.generate(fixture.wrapper, fixture.decider).getRequest();
            assertNotNull(request);
            if (request == null) {
                return;
            }
            try {
                localProvider.choose(request, () -> {
                    nativeCalls.incrementAndGet();
                    return true;
                });
                fail("a throwing external resolver must not fall back to native Forge");
            } catch (final UnsupportedConfirmationDecisionException ex) {
                assertEquals(ex.getStatus(), ConfirmationDecisionProvider.Status.INVALID_EXTERNAL_CANDIDATE);
            }
            assertEquals(nativeCalls.get(), 0);
        } finally {
            localProvider.setResolver(null);
        }

        final ConfirmationDecisionProvider foreignProvider = new ConfirmationDecisionProvider();
        foreignProvider.setResolver(request -> LegalCandidate.pass(99));
        try {
            final DecisionRequest request = foreignProvider.generate(fixture.wrapper, fixture.decider).getRequest();
            assertNotNull(request);
            if (request == null) {
                return;
            }
            try {
                foreignProvider.choose(request, () -> {
                    nativeCalls.incrementAndGet();
                    return true;
                });
                fail("a foreign external candidate must not fall back to native Forge");
            } catch (final UnsupportedConfirmationDecisionException ex) {
                assertEquals(ex.getStatus(), ConfirmationDecisionProvider.Status.INVALID_EXTERNAL_CANDIDATE);
            }
            assertEquals(nativeCalls.get(), 0);
        } finally {
            foreignProvider.setResolver(null);
        }
    }

    @Test
    public void bloodApplyValidationFailureInvalidatesRequest() {
        final ConfirmationDecisionProvider localProvider = new ConfirmationDecisionProvider();
        localProvider.setResolver(request -> request.getCandidates().stream()
                .filter(candidate -> candidate.getConfirmationKind() == ConfirmationCandidateKind.ACCEPT)
                .findFirst()
                .orElse(null));

        try {
            final ExactBloodFixture fixture = exactBloodFixture();
            final ConfirmationDecisionProvider.Generation generation =
                    localProvider.generate(fixture.wrapper, fixture.decider);

            assertEquals(generation.getStatus(), ConfirmationDecisionProvider.Status.ADMITTED);
            final DecisionRequest request = generation.getRequest();
            assertNotNull(request);
            if (request == null) {
                return;
            }
            final LegalCandidate selected = localProvider.choose(request, () -> {
                fail("external confirmation must not invoke the native callback");
                return false;
            });
            assertNotNull(selected);
            if (selected == null) {
                return;
            }

            final List<Player> players = fixture.decider.getGame().getPlayers();
            assertNotNull(players);
            assertTrue(players != null && players.size() > 1);
            if (players == null || players.size() < 2) {
                return;
            }
            final Player wrongDecider = players.get(0);
            assertNotNull(wrongDecider);
            if (wrongDecider == null) {
                return;
            }
            final WrappedAbility wrongWrapper = new WrappedAbility(
                    fixture.wrapper.getTrigger(), fixture.wrapper.getWrappedAbility(), wrongDecider);
            assertNotNull(wrongWrapper);
            if (wrongWrapper == null) {
                return;
            }
            boolean firstApplyFailed = false;
            try {
                localProvider.apply(request, selected, wrongWrapper);
                fail("expected mismatched Blood wrapper to be rejected");
            } catch (final RuntimeException ex) {
                firstApplyFailed = true;
            }
            assertTrue(firstApplyFailed);

            try {
                localProvider.apply(request, selected, fixture.wrapper);
                fail("a failed Blood apply must invalidate the request and forbid retry");
            } catch (final RuntimeException ex) {
                // Expected: the failed integrity validation must clear the active request.
            }
        } finally {
            localProvider.setResolver(null);
        }
    }

    @Test
    public void exactBloodEtbConfirmationIsNotYetAdmittedOnTheBaseline() {
        final Game game = initAndCreateGame();
        final Player decider = game.getPlayers().get(1);
        final Player opponent = game.getPlayers().get(0);
        final Card source = addCardToZone("Blood Operative", decider, ZoneType.Battlefield);
        final Card legalCard = addCardToZone("Runeclaw Bear", opponent, ZoneType.Graveyard);
        assertEquals(source.getName(), "Blood Operative");
        assertTrue(decider.getZone(ZoneType.Battlefield).contains(source));
        assertEquals(source.getController(), decider);
        assertEquals(opponent.getZone(ZoneType.Graveyard).size(), 1);
        assertTrue(opponent.getZone(ZoneType.Graveyard).contains(legalCard));

        final Trigger trigger = source.getTriggers().stream()
                .filter(candidate -> candidate.getMode() == TriggerType.ChangesZone)
                .filter(candidate -> "Any".equals(candidate.getParam("Origin")))
                .filter(candidate -> "Battlefield".equals(candidate.getParam("Destination")))
                .filter(candidate -> "Card.Self".equals(candidate.getParam("ValidCard")))
                .filter(candidate -> "You".equals(candidate.getParam("OptionalDecider")))
                .filter(candidate -> "TrigChangeZone".equals(candidate.getParam("Execute")))
                .filter(candidate -> candidate.isIntrinsic())
                .filter(candidate -> !candidate.isStatic())
                .filter(candidate -> candidate.getSpawningAbility() == null)
                .findFirst()
                .orElseThrow(() -> new AssertionError("expected Blood Operative ETB trigger is unavailable"));
        assertEquals(trigger.getMode(), TriggerType.ChangesZone);
        assertFalse(trigger.isStatic());
        assertTrue(trigger.isIntrinsic());
        assertNull(trigger.getSpawningAbility());
        assertEquals(trigger.getParam("Origin"), "Any");
        assertEquals(trigger.getParam("Destination"), "Battlefield");
        assertEquals(trigger.getParam("ValidCard"), "Card.Self");
        assertEquals(trigger.getParam("OptionalDecider"), "You");
        assertEquals(trigger.getParam("Execute"), "TrigChangeZone");

        final SpellAbility ability = trigger.ensureAbility();
        assertNotNull(ability);
        ability.setActivatingPlayer(decider);
        assertEquals(ability.getApi(), ApiType.ChangeZone);
        assertEquals(ability.getParam("Origin"), "Graveyard");
        assertEquals(ability.getParam("Destination"), "Exile");
        assertEquals(ability.getParam("ValidTgts"), "Card");
        assertFalse(ability.hasParam("Optional"));
        ability.setOptionalTrigger(true);
        assertTrue(ability.isOptionalTrigger());
        assertTrue(ability.isIntrinsic());
        final TargetRestrictions restrictions = ability.getTargetRestrictions();
        assertNotNull(restrictions);
        assertTrue(ability.usesTargeting());
        assertEquals(restrictions.getZone(), List.of(ZoneType.Graveyard));
        assertFalse(restrictions.isRandomTarget());
        assertFalse(restrictions.isRandomNumTargets());
        assertEquals(ability.getMinTargets(), 1);
        assertEquals(ability.getMaxTargets(), 1);
        assertEquals(ability.getTargets().size(), 0);
        assertTrue(ability.canTarget(legalCard));

        final WrappedAbility wrapper = new WrappedAbility(trigger, ability, decider);
        assertEquals(wrapper.getDecider(), decider);
        assertTrue(wrapper.isOptionalTrigger());
        assertTrue(wrapper.isIntrinsic());

        ability.getTargets().add(legalCard);

        final ConfirmationDecisionProvider.Generation generation = provider.generate(wrapper, decider);

        assertEquals(generation.getStatus(), ConfirmationDecisionProvider.Status.ADMITTED);
        final DecisionRequest request = generation.getRequest();
        assertNotNull(request);
        assertEquals(request.getDecisionType(), DecisionType.CONFIRMATION);
        assertFalse(request.isForced());
        assertEquals(request.getCandidates().stream().map(LegalCandidate::getSemanticKey).toList(),
                List.of("ACCEPT", "DECLINE"));

        final Object context = request.getConfirmationContext();
        assertNotNull(context);
        assertEquals(enumName(readRequiredGetter(context, "getProfile"), "profile"),
                "BLOOD_OPERATIVE_ETB_EXILE_GRAVEYARD_CARD");
        assertEquals(enumName(readRequiredGetter(context, "getEvent"), "event"), "CHANGES_ZONE");

        final Object targetPublicIdentity = readRequiredGetter(context, "getTargetPublicIdentity");
        assertNotNull(targetPublicIdentity);
        if (targetPublicIdentity == null) {
            return;
        }
        assertFalse(targetPublicIdentity instanceof Card, "targetPublicIdentity must be a value projection");
        assertPublicCardProjection(targetPublicIdentity, legalCard);

        final Field triggeringPlayerId = requiredField(context, "triggeringPlayerId");
        assertNotNull(triggeringPlayerId);
        if (triggeringPlayerId == null) {
            return;
        }
        assertEquals(triggeringPlayerId.getType(), Integer.class);
        assertNull(readRequiredField(context, triggeringPlayerId));
        assertNull(readRequiredGetter(context, "getTriggeringPlayerId"));
        assertNumericEquals(readRequiredGetter(context, "getDeciderPlayerId"), decider.getId(),
                "deciderPlayerId");
        assertValueOnlyFields(context);
    }

    @Test
    public void unsupportedBloodGenerationRetainsItsTypedProfile() {
        final ExactBloodFixture fixture = exactBloodFixture();
        fixture.wrapper.getWrappedAbility().getTargets().clear();

        final ConfirmationDecisionProvider.Generation generation =
                provider.generate(fixture.wrapper, fixture.decider);

        assertEquals(generation.getStatus(), ConfirmationDecisionProvider.Status.UNSUPPORTED_PROFILE);
        assertEquals(generation.getProfile(),
                ConfirmationTriggerProfile.BLOOD_OPERATIVE_ETB_EXILE_GRAVEYARD_CARD);
        assertNull(generation.getRequest());
    }

    private ExactBloodFixture exactBloodFixture() {
        final Game game = initAndCreateGame();
        final Player decider = game.getPlayers().get(1);
        final Player opponent = game.getPlayers().get(0);
        final Card source = addCardToZone("Blood Operative", decider, ZoneType.Battlefield);
        final Card legalCard = addCardToZone("Runeclaw Bear", opponent, ZoneType.Graveyard);

        final Trigger trigger = source.getTriggers().stream()
                .filter(candidate -> candidate.getMode() == TriggerType.ChangesZone)
                .filter(candidate -> "Any".equals(candidate.getParam("Origin")))
                .filter(candidate -> "Battlefield".equals(candidate.getParam("Destination")))
                .filter(candidate -> "Card.Self".equals(candidate.getParam("ValidCard")))
                .filter(candidate -> "You".equals(candidate.getParam("OptionalDecider")))
                .filter(candidate -> "TrigChangeZone".equals(candidate.getParam("Execute")))
                .filter(Trigger::isIntrinsic)
                .filter(candidate -> !candidate.isStatic())
                .filter(candidate -> candidate.getSpawningAbility() == null)
                .findFirst()
                .orElseThrow(() -> new AssertionError("expected Blood Operative ETB trigger is unavailable"));

        final SpellAbility ability = trigger.ensureAbility();
        assertNotNull(ability);
        ability.setActivatingPlayer(decider);
        ability.setOptionalTrigger(true);
        assertEquals(ability.getApi(), ApiType.ChangeZone);
        assertEquals(ability.getParam("Origin"), "Graveyard");
        assertEquals(ability.getParam("Destination"), "Exile");
        assertEquals(ability.getParam("ValidTgts"), "Card");
        assertEquals(ability.getTargets().size(), 0);
        assertTrue(ability.canTarget(legalCard));
        ability.getTargets().add(legalCard);

        return new ExactBloodFixture(decider, new WrappedAbility(trigger, ability, decider));
    }

    private static final class ExactBloodFixture {
        private final Player decider;
        private final WrappedAbility wrapper;

        private ExactBloodFixture(final Player decider0, final WrappedAbility wrapper0) {
            decider = decider0;
            wrapper = wrapper0;
        }
    }

    private static void assertPublicCardProjection(final Object projection, final Card expectedCard) {
        assertNotNull(expectedCard.getZone());
        assertNotNull(expectedCard.getOwner());
        assertNotNull(expectedCard.getController());
        if (expectedCard.getZone() == null || expectedCard.getOwner() == null
                || expectedCard.getController() == null) {
            return;
        }
        assertValueOnlyFields(projection);
        assertNumericEquals(readRequiredGetter(projection, "getCardId"), expectedCard.getId(), "cardId");
        assertNumericEquals(readRequiredGetter(projection, "getGameTimestamp"), expectedCard.getGameTimestamp(),
                "gameTimestamp");
        assertEquals(readRequiredGetter(projection, "getVisibleName"), expectedCard.getName());
        assertEquals(readRequiredGetter(projection, "getZone"), expectedCard.getZone().getZoneType());
        assertNumericEquals(readRequiredGetter(projection, "getOwnerId"), expectedCard.getOwner().getId(),
                "ownerId");
        assertNumericEquals(readRequiredGetter(projection, "getControllerId"), expectedCard.getController().getId(),
                "controllerId");
    }

    private static void assertNumericEquals(final Object actual, final long expected, final String fieldName) {
        assertNotNull(actual, fieldName + " is missing");
        assertTrue(actual instanceof Number, fieldName + " must be numeric");
        if (actual instanceof Number number) {
            assertEquals(number.longValue(), expected, fieldName + " mismatch");
        }
    }

    private static String enumName(final Object value, final String fieldName) {
        assertNotNull(value, fieldName + " is missing");
        assertTrue(value instanceof Enum<?>, fieldName + " must be an enum");
        if (value instanceof Enum<?> enumValue) {
            return enumValue.name();
        }
        return null;
    }

    private static Object readRequiredGetter(final Object target, final String getterName) {
        assertNotNull(target, "cannot read " + getterName + " from null");
        if (target == null) {
            return null;
        }
        try {
            final Method getter = target.getClass().getMethod(getterName);
            return getter.invoke(target);
        } catch (ReflectiveOperationException | RuntimeException ex) {
            fail("missing or unreadable required getter " + getterName + " ("
                    + ex.getClass().getSimpleName() + ")");
            return null;
        }
    }

    private static Field requiredField(final Object target, final String fieldName) {
        assertNotNull(target, "cannot read " + fieldName + " from null");
        if (target == null) {
            return null;
        }
        try {
            return target.getClass().getDeclaredField(fieldName);
        } catch (ReflectiveOperationException | RuntimeException ex) {
            fail("missing required field " + fieldName + " (" + ex.getClass().getSimpleName() + ")");
            return null;
        }
    }

    private static Object readRequiredField(final Object target, final Field field) {
        assertNotNull(target, "cannot read a field from null");
        assertNotNull(field, "required field is missing");
        if (target == null || field == null) {
            return null;
        }
        try {
            field.setAccessible(true);
            return field.get(target);
        } catch (ReflectiveOperationException | RuntimeException ex) {
            fail("unreadable required field " + field.getName() + " ("
                    + ex.getClass().getSimpleName() + ")");
            return null;
        }
    }

    private static void assertValueOnlyFields(final Object value) {
        assertNotNull(value, "value-only object is missing");
        if (value == null) {
            return;
        }
        for (final Field field : value.getClass().getDeclaredFields()) {
            if (field.isSynthetic() || Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            final String fieldName = field.getName().toLowerCase(Locale.ROOT);
            assertFalse(fieldName.contains("occurrence"), "raw occurrence field: " + field.getName());
            assertFalse(fieldName.contains("continuation"), "raw continuation field: " + field.getName());
            assertTrue(isValueOnlyType(field.getType()), "raw Forge object field: " + field.getName());
        }
    }

    private static boolean isValueOnlyType(final Class<?> type) {
        return type.isPrimitive()
                || type.isEnum()
                || type == String.class
                || Number.class.isAssignableFrom(type)
                || type == Boolean.class
                || type == Character.class
                || "forge.game.decision.CardSelectionCard".equals(type.getName());
    }
}
