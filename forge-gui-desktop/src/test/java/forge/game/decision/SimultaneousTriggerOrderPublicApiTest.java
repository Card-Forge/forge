package forge.game.decision;

import forge.ai.AITest;
import forge.game.Game;
import forge.game.GameObject;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.TriggerType;
import org.testng.annotations.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Set;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;

public class SimultaneousTriggerOrderPublicApiTest extends AITest {
    private static final Set<Class<?>> FORBIDDEN_TYPES = Set.of(
            Card.class, Player.class, Game.class, GameObject.class, SpellAbility.class,
            ActionContinuation.class, forge.game.trigger.WrappedAbility.class);

    @Test
    public void exposesOnlyTheApprovedProfileAndEnums() {
        assertEquals(DecisionType.ORDER.name(), "ORDER");
        assertEquals(OrderDirection.values(), new OrderDirection[] {OrderDirection.RESOLVE_FIRST});
        assertEquals(OrderCandidateKind.values(),
                new OrderCandidateKind[] {OrderCandidateKind.SELECT_RESOLVE_FIRST});
        assertEquals(SimultaneousTriggerOrderProfile.values(),
                new SimultaneousTriggerOrderProfile[] {
                        SimultaneousTriggerOrderProfile.SIMULTANEOUS_TRIGGER_ORDER
                });
    }

    @Test
    public void itemAndContextAreStableValueOnlyDtos() {
        final Game game = initAndCreateGame();
        final Player chooser = game.getPlayers().get(0);
        final CardSelectionCard source = new CardSelectionCard(addCard("Island", chooser));
        final SimultaneousTriggerOrderItem first = new SimultaneousTriggerOrderItem(
                1L, source, TriggerType.AbilityCast, ApiType.Effect);
        final SimultaneousTriggerOrderItem duplicateLooking = new SimultaneousTriggerOrderItem(
                2L, source, TriggerType.AbilityCast, ApiType.Effect);

        assertEquals(first.getItemId(), 1L);
        assertEquals(duplicateLooking.getItemId(), 2L);
        assertEquals(first.getSource(), duplicateLooking.getSource());
        assertEquals(first.getTriggerType(), duplicateLooking.getTriggerType());
        assertEquals(first.getEffectApi(), duplicateLooking.getEffectApi());
        assertFalse(first.equals(duplicateLooking));

        final SimultaneousTriggerOrderContext context = new SimultaneousTriggerOrderContext(
                SimultaneousTriggerOrderProfile.SIMULTANEOUS_TRIGGER_ORDER,
                OrderDirection.RESOLVE_FIRST, 7L, 0, 2, chooser.getId());
        assertEquals(context.getOrderSessionId(), 7L);
        assertEquals(context.getStepIndex(), 0);
        assertEquals(context.getOriginalItemCount(), 2);
        assertEquals(context.getChoosingPlayerId(), chooser.getId());
        assertNull(context.getDecisionSequenceId());
        assertNull(context.getSubdecisionIndex());
        assertFalse(java.util.Arrays.stream(SimultaneousTriggerOrderContext.class.getMethods())
                .anyMatch(method -> method.getName().equals("getRemainingItems")));
    }

    @Test
    public void orderCandidatesCarryOnlyTheTypedItemProjection() {
        final Game game = initAndCreateGame();
        final Player chooser = game.getPlayers().get(0);
        final SimultaneousTriggerOrderItem first = new SimultaneousTriggerOrderItem(
                1L, new CardSelectionCard(addCard("Island", chooser)),
                TriggerType.AbilityCast, ApiType.Effect);
        final SimultaneousTriggerOrderItem second = new SimultaneousTriggerOrderItem(
                2L, new CardSelectionCard(addCard("Mountain", chooser)),
                TriggerType.AbilityCast, ApiType.Effect);
        final DecisionRequest request = new DecisionRequest(3L, DecisionType.ORDER,
                java.util.List.of(
                        LegalCandidate.order(0, OrderCandidateKind.SELECT_RESOLVE_FIRST, first),
                        LegalCandidate.order(1, OrderCandidateKind.SELECT_RESOLVE_FIRST, second)),
                new SimultaneousTriggerOrderContext(
                        SimultaneousTriggerOrderProfile.SIMULTANEOUS_TRIGGER_ORDER,
                        OrderDirection.RESOLVE_FIRST, 8L, 0, 2, chooser.getId()));

        assertEquals(request.getDecisionType(), DecisionType.ORDER);
        assertEquals(request.getCandidates().size(), 2);
        assertEquals(request.getOrderContext().getDirection(), OrderDirection.RESOLVE_FIRST);
        assertEquals(request.getCandidates().get(0).getOrderKind(),
                OrderCandidateKind.SELECT_RESOLVE_FIRST);
        assertEquals(request.getCandidates().get(0).getOrderItem(), first);
        assertEquals(request.getCandidates().get(0).getSemanticKey(), "RESOLVE_FIRST|1");
    }

    @Test
    public void newPublicDtosExposeNoNativeEngineTypes() {
        for (final Class<?> dto : Set.of(
                SimultaneousTriggerOrderItem.class,
                SimultaneousTriggerOrderContext.class)) {
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
}
