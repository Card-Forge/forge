package forge.game.decision;

import forge.ai.AITest;
import forge.ai.PlayerControllerAi;
import forge.game.Game;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbilityStackInstance;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerType;
import forge.game.trigger.WrappedAbility;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

/**
 * Locks the production route after ORDER returns: AI controller, native insertion,
 * MagicStack push, and actual resolution must preserve RESOLVE_FIRST semantics.
 */
public class SimultaneousTriggerOrderEngineIntegrationTest extends AITest {
    @Test
    public void externalTwoItemRouteResolvesSelectedItemFirstOnMagicStack() {
        final RouteResult result = runExternal(List.of(1L, 2L));

        assertEquals(result.resolverCalls, 1);
        assertEquals(result.insertedTopFirst, List.of("A", "B"));
        assertEquals(result.resolutionOrder, List.of("A", "B"));
    }

    @Test
    public void externalThreeItemRouteResolvesEachSelectedItemInOrder() {
        final RouteResult result = runExternal(List.of(1L, 3L, 2L));

        assertEquals(result.resolverCalls, 2);
        assertEquals(result.insertedTopFirst, List.of("A", "C", "B"));
        assertEquals(result.resolutionOrder, List.of("A", "C", "B"));
    }

    @Test
    public void nativeAndExternalRoutesHaveEquivalentStackAndResolutionOrder() {
        final RouteResult nativeTwo = runNative(2);
        final RouteResult externalTwo = runExternal(List.of(2L, 1L));
        assertEquals(nativeTwo.insertedTopFirst, externalTwo.insertedTopFirst);
        assertEquals(nativeTwo.resolutionOrder, externalTwo.resolutionOrder);

        final RouteResult nativeThree = runNative(3);
        final RouteResult externalThree = runExternal(List.of(3L, 2L, 1L));
        assertEquals(nativeThree.insertedTopFirst, externalThree.insertedTopFirst);
        assertEquals(nativeThree.resolutionOrder, externalThree.resolutionOrder);
    }

    private RouteResult runExternal(final List<Long> selectedItemIds) {
        final Game game = initAndCreateGame();
        final Player player = game.getPlayers().get(0);
        assertTrue(player.getController() instanceof PlayerControllerAi);

        final List<String> resolutionOrder = new ArrayList<>();
        final List<SpellAbility> entries = entries(player, resolutionOrder, selectedItemIds.size());
        final AtomicInteger resolverCalls = new AtomicInteger();
        player.getController().setSimultaneousTriggerOrderResolver(request -> {
            resolverCalls.incrementAndGet();
            final int step = request.getOrderContext().getStepIndex();
            final long selectedItemId = selectedItemIds.get(step);
            return request.getCandidates().stream()
                    .filter(candidate -> candidate.getOrderItem().getItemId() == selectedItemId)
                    .findFirst()
                    .orElseThrow();
        });

        player.getController().orderAndPlaySimultaneousSa(entries);
        assertEquals(game.getStack().size(), entries.size());
        assertSame(game.getStack().peekAbility(), entries.get(selectedItemIds.get(0).intValue() - 1));

        final List<String> insertedTopFirst = stackLabels(game);
        resolveStack(game);
        return new RouteResult(resolverCalls.get(), insertedTopFirst, resolutionOrder);
    }

    private RouteResult runNative(final int count) {
        final Game game = initAndCreateGame();
        final Player player = game.getPlayers().get(0);
        assertTrue(player.getController() instanceof PlayerControllerAi);

        final List<String> resolutionOrder = new ArrayList<>();
        final List<SpellAbility> entries = entries(player, resolutionOrder, count);
        player.getController().orderAndPlaySimultaneousSa(entries);
        assertEquals(game.getStack().size(), entries.size());

        final List<String> insertedTopFirst = stackLabels(game);
        resolveStack(game);
        return new RouteResult(0, insertedTopFirst, resolutionOrder);
    }

    private List<SpellAbility> entries(final Player player, final List<String> resolutionOrder,
            final int count) {
        final List<SpellAbility> entries = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            final String label = String.valueOf((char) ('A' + index));
            final Card source = addCard("Island", player);
            final Trigger trigger = TriggerType.Always.createTrigger(new HashMap<>(), source, true);
            final RecordingSpellAbility effect = new RecordingSpellAbility(source, player, label,
                    resolutionOrder);
            entries.add(new WrappedAbility(trigger, effect, null));
        }
        return entries;
    }

    private List<String> stackLabels(final Game game) {
        final List<String> labels = new ArrayList<>();
        for (SpellAbilityStackInstance instance : game.getStack()) {
            final WrappedAbility wrapper = (WrappedAbility) instance.getSpellAbility();
            labels.add(((RecordingSpellAbility) wrapper.getWrappedAbility()).label);
        }
        return labels;
    }

    private void resolveStack(final Game game) {
        while (!game.getStack().isEmpty()) {
            game.getStack().resolveStack();
        }
    }

    private static final class RecordingSpellAbility extends SpellAbility.EmptySa {
        private final String label;
        private final List<String> resolutionOrder;

        private RecordingSpellAbility(final Card source, final Player player, final String label,
                final List<String> resolutionOrder) {
            super(ApiType.Untap, source, player);
            this.label = label;
            this.resolutionOrder = resolutionOrder;
            setDescription(label);
        }

        @Override
        public void resolve() {
            resolutionOrder.add(label);
        }
    }

    private static final class RouteResult {
        private final int resolverCalls;
        private final List<String> insertedTopFirst;
        private final List<String> resolutionOrder;

        private RouteResult(final int resolverCalls, final List<String> insertedTopFirst,
                final List<String> resolutionOrder) {
            this.resolverCalls = resolverCalls;
            this.insertedTopFirst = insertedTopFirst;
            this.resolutionOrder = resolutionOrder;
        }
    }
}
