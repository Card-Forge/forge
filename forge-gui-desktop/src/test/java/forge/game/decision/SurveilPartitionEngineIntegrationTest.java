package forge.game.decision;

import forge.ai.AITest;
import forge.ai.PlayerControllerAi;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CounterEnumType;
import forge.game.event.GameEventSurveil;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import com.google.common.eventbus.Subscribe;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.expectThrows;

public class SurveilPartitionEngineIntegrationTest extends AITest {
    @Test
    public void playerControllerOwnsSurveilPartitionCoordinator() {
        final Player player = initAndCreateGame().getPlayers().get(1);
        assertNotNull(player.getController().getSurveilPartitionDecisionProvider());
        assertNotNull(player.getController().getSurveilPartitionDecisionCoordinator());
        assertSame(player.getController().getSurveilPartitionDecisionProvider(),
                player.getController().getSurveilPartitionDecisionCoordinator().provider());
    }

    @Test
    public void playerSurveilKeepsNativeMovementFlagsEventAndTriggerBoundary() {
        final var game = initAndCreateGame();
        final Player player = game.getPlayers().get(1);
        final Card source = addCardToZone("Dimir Spybug", player, ZoneType.Battlefield);
        final Card graveCard = addCardToZone("Island", player, ZoneType.Library);
        final Card retainedCard = addCardToZone("Forest", player, ZoneType.Library);
        final SpellAbility cause = addCardToZone("Opt", player, ZoneType.Hand).getFirstSpellAbility();
        final SurveilObserver observer = new SurveilObserver();
        game.subscribeToEvents(observer);

        final PlayerControllerAi nativeController = new PlayerControllerAi(game, player,
                player.getLobbyPlayer()) {
            @Override
            public ImmutablePair<CardCollection, CardCollection> arrangeForSurveil(final CardCollection topN) {
                assertEquals(topN.size(), 2);
                return ImmutablePair.of(new CardCollection(retainedCard), new CardCollection(graveCard));
            }
        };
        final long controllerTimestamp = game.getNextTimestamp();
        player.addController(controllerTimestamp, player, nativeController, false);

        game.getAction().checkStateEffects(true);
        game.getTriggerHandler().registerActiveTrigger(source, false);
        player.surveil(2, cause, new HashMap<>());
        game.getStack().addAllTriggeredAbilitiesToStack();
        if (!game.getStack().isEmpty()) {
            game.getStack().resolveStack();
        }

        assertTrue(graveCard.isInZone(ZoneType.Graveyard));
        assertTrue(retainedCard.isInZone(ZoneType.Library));
        assertTrue(player.getZone(ZoneType.Graveyard).getCards().stream()
                .filter(card -> card.getName().equals(graveCard.getName()))
                .anyMatch(Card::wasSurveilled));
        assertEquals(observer.eventCount, 1);
        assertEquals(observer.toLibrary, 1);
        assertEquals(observer.toGraveyard, 1);
        assertEquals(source.getCounters(CounterEnumType.P1P1), 1);
        assertEquals(player.getSurveilThisTurn(), 1);
    }

    @Test
    public void coordinatorReturnsBeforePlayerMovementAndNativePairDrivesBothDestinations() {
        final var game = initAndCreateGame();
        final Player player = game.getPlayers().get(1);
        final Card graveCard = addCardToZone("Island", player, ZoneType.Library);
        final Card retainedCard = addCardToZone("Forest", player, ZoneType.Library);
        final SpellAbility cause = addCardToZone("Opt", player, ZoneType.Hand).getFirstSpellAbility();
        final AtomicBoolean callbackReturnedBeforeMovement = new AtomicBoolean();
        final PlayerControllerAi nativeController = new PlayerControllerAi(game, player,
                player.getLobbyPlayer()) {
            @Override
            public ImmutablePair<CardCollection, CardCollection> arrangeForSurveil(final CardCollection topN) {
                assertTrue(graveCard.isInZone(ZoneType.Library));
                assertTrue(retainedCard.isInZone(ZoneType.Library));
                callbackReturnedBeforeMovement.set(true);
                return ImmutablePair.of(new CardCollection(retainedCard), new CardCollection(graveCard));
            }
        };
        player.addController(game.getNextTimestamp(), player, nativeController, false);

        player.surveil(2, cause, new HashMap<>());

        assertTrue(callbackReturnedBeforeMovement.get());
        assertTrue(graveCard.isInZone(ZoneType.Graveyard));
        assertTrue(retainedCard.isInZone(ZoneType.Library));
    }

    @Test
    public void playerSurveilRethrowsNativeCallbackWithoutRetryOrZoneMutation() {
        final var game = initAndCreateGame();
        final Player player = game.getPlayers().get(1);
        final Card first = addCardToZone("Island", player, ZoneType.Library);
        final Card second = addCardToZone("Forest", player, ZoneType.Library);
        final SpellAbility cause = addCardToZone("Opt", player, ZoneType.Hand).getFirstSpellAbility();
        final RuntimeException nativeFailure = new RuntimeException("native-surveil-failure");
        final AtomicInteger callbackCalls = new AtomicInteger();

        final PlayerControllerAi nativeController = new PlayerControllerAi(game, player,
                player.getLobbyPlayer()) {
            @Override
            public ImmutablePair<CardCollection, CardCollection> arrangeForSurveil(final CardCollection topN) {
                callbackCalls.incrementAndGet();
                assertEquals(topN.size(), 2);
                assertTrue(first.isInZone(ZoneType.Library));
                assertTrue(second.isInZone(ZoneType.Library));
                throw nativeFailure;
            }
        };
        player.addController(game.getNextTimestamp(), player, nativeController, false);

        final RuntimeException actual = expectThrows(RuntimeException.class,
                () -> player.surveil(2, cause, new HashMap<>()));

        assertSame(actual, nativeFailure);
        assertEquals(callbackCalls.get(), 1);
        assertTrue(first.isInZone(ZoneType.Library));
        assertTrue(second.isInZone(ZoneType.Library));
        assertEquals(player.getSurveilThisTurn(), 0);
        assertEquals(nativeController.getSurveilPartitionDecisionCoordinator().activeSessionCount(), 0);
    }

    private static final class SurveilObserver {
        private int eventCount;
        private int toLibrary;
        private int toGraveyard;

        @Subscribe
        public void receive(final GameEventSurveil event) {
            eventCount++;
            toLibrary = event.toLibrary();
            toGraveyard = event.toGraveyard();
        }
    }
}
