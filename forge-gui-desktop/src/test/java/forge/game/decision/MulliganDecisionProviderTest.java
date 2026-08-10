package forge.game.decision;

import forge.MulliganDefs;
import forge.ai.AITest;
import forge.game.Game;
import forge.game.GameStage;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotSame;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

public class MulliganDecisionProviderTest extends AITest {
    private final MulliganDecisionProvider provider = new MulliganDecisionProvider();

    @Test
    public void ordinaryCallbackProducesKeepAndRedrawWithSeparateStartingPlayer() {
        final Game game = mulliganGame();
        final Player acting = game.getPlayers().get(1);
        final Player starting = game.getPlayers().get(0);
        final Card first = addCardToZone("Island", acting, ZoneType.Hand);
        final Card second = addCardToZone("Mountain", acting, ZoneType.Hand);

        final MulliganDecisionProvider.SessionStart start = provider.beginCallback(acting, starting,
                new CardCollection(List.of(first, second)), 0);
        final MulliganDecisionProvider.Generation generation = provider.generateNext(start.getSession());

        assertEquals(start.getStatus(), MulliganDecisionProvider.Status.READY);
        assertEquals(generation.getStatus(), MulliganDecisionProvider.Status.DECISION);
        assertEquals(generation.getRequest().getDecisionType(), DecisionType.MULLIGAN);
        assertEquals(generation.getRequest().getCandidates().stream().map(LegalCandidate::getSemanticKey).toList(),
                List.of("MULLIGAN|KEEP", "MULLIGAN|REDRAW"));
        assertFalse(generation.getRequest().isForced());

        final MulliganContext context = generation.getRequest().getMulliganContext();
        assertEquals(context.getGameId(), game.getId());
        assertEquals(context.getActingPlayerId(), acting.getId());
        assertEquals(context.getStartingPlayerId(), starting.getId());
        assertEquals(context.getCardsToReturn(), 0);
        assertEquals(context.getHandSize(), 2);
        assertEquals(context.getStage(), MulliganStage.KEEP_OR_REDRAW);
        assertEquals(context.getHandCards().size(), 2);
    }

    @Test
    public void keepConsumesTheRequestAndTerminatesTheParentSession() {
        final Game game = mulliganGame();
        final Player acting = game.getPlayers().get(1);
        final Player starting = game.getPlayers().get(0);
        final Card card = addCardToZone("Island", acting, ZoneType.Hand);
        final MulliganDecisionProvider.SessionStart start = provider.beginCallback(acting, starting,
                new CardCollection(card), 0);
        final MulliganDecisionProvider.Generation initial = provider.generateNext(start.getSession());
        final LegalCandidate keep = candidate(initial.getRequest(), "MULLIGAN|KEEP");

        final MulliganDecisionProvider.Generation complete = provider.apply(initial.getRequest(), keep);
        final MulliganDecisionProvider.Generation after = provider.generateNext(start.getSession());
        final MulliganDecisionProvider.Generation reapplied = provider.apply(initial.getRequest(), keep);

        assertEquals(complete.getStatus(), MulliganDecisionProvider.Status.COMPLETE);
        assertEquals(complete.getSelectedKind(), MulliganCandidateKind.KEEP);
        assertTrue(start.getSession().isTerminal());
        assertEquals(after.getStatus(), MulliganDecisionProvider.Status.COMPLETE);
        assertNull(after.getRequest());
        assertEquals(reapplied.getStatus(), MulliganDecisionProvider.Status.STALE_MULLIGAN);
    }

    @Test
    public void redrawWaitsForTheNextRealForgeCallbackAndKeepsTheParentSession() {
        final Game game = mulliganGame();
        final Player acting = game.getPlayers().get(1);
        final Player starting = game.getPlayers().get(0);
        final Card first = addCardToZone("Island", acting, ZoneType.Hand);
        final MulliganDecisionProvider.SessionStart firstStart = provider.beginCallback(acting, starting,
                new CardCollection(first), 0);
        final MulliganDecisionProvider.Generation firstRequest = provider.generateNext(firstStart.getSession());
        final MulliganDecisionProvider.Generation redraw = provider.apply(firstRequest.getRequest(),
                candidate(firstRequest.getRequest(), "MULLIGAN|REDRAW"));

        assertEquals(redraw.getStatus(), MulliganDecisionProvider.Status.AWAITING_FORGE_CALLBACK);
        assertEquals(provider.generateNext(firstStart.getSession()).getStatus(),
                MulliganDecisionProvider.Status.AWAITING_FORGE_CALLBACK);

        game.getAction().moveToLibrary(first, null);
        final Card replacement = addCardToZone("Mountain", acting, ZoneType.Hand);
        final MulliganDecisionProvider.SessionStart secondStart = provider.beginCallback(acting, starting,
                new CardCollection(replacement), 1);
        final MulliganDecisionProvider.Generation secondRequest = provider.generateNext(secondStart.getSession());

        assertSame(secondStart.getSession(), firstStart.getSession());
        assertEquals(secondRequest.getStatus(), MulliganDecisionProvider.Status.DECISION);
        assertEquals(secondRequest.getRequest().getMulliganContext().getMulliganRoundIndex(), 1);
        assertEquals(secondRequest.getRequest().getMulliganContext().getCardsToReturn(), 1);
        assertEquals(secondRequest.getRequest().getMulliganContext().getHandCards().get(0).getCardId(),
                replacement.getId());
        assertFalse(secondRequest.getRequest().getMulliganContext().getHandCards().get(0).getCardId()
                == first.getId());
    }

    @Test
    public void changedGameStageRejectsTheOldRequestAsStale() {
        final Game game = mulliganGame();
        final Player acting = game.getPlayers().get(1);
        final Player starting = game.getPlayers().get(0);
        final Card card = addCardToZone("Island", acting, ZoneType.Hand);
        final MulliganDecisionProvider.SessionStart start = provider.beginCallback(acting, starting,
                new CardCollection(card), 0);
        final MulliganDecisionProvider.Generation initial = provider.generateNext(start.getSession());
        game.setAge(GameStage.Play);

        final MulliganDecisionProvider.Generation stale = provider.apply(initial.getRequest(),
                candidate(initial.getRequest(), "MULLIGAN|KEEP"));

        assertEquals(stale.getStatus(), MulliganDecisionProvider.Status.STALE_MULLIGAN);
    }

    @Test
    public void addingAHandCardAfterCaptureMakesTheRequestStale() {
        final Game game = mulliganGame();
        final Player acting = game.getPlayers().get(1);
        final Player starting = game.getPlayers().get(0);
        final Card original = addCardToZone("Island", acting, ZoneType.Hand);
        final MulliganDecisionProvider.SessionStart start = provider.beginCallback(acting, starting,
                new CardCollection(original), 0);
        final MulliganDecisionProvider.Generation initial = provider.generateNext(start.getSession());
        addCardToZone("Mountain", acting, ZoneType.Hand);

        final MulliganDecisionProvider.Generation stale = provider.apply(initial.getRequest(),
                candidate(initial.getRequest(), "MULLIGAN|KEEP"));

        assertEquals(stale.getStatus(), MulliganDecisionProvider.Status.STALE_MULLIGAN);
    }

    @Test
    public void emptyHandIsExplicitlyUnsupportedAndDoesNotCreateAPolicyRequest() {
        final Game game = mulliganGame();
        final Player acting = game.getPlayers().get(1);
        final Player starting = game.getPlayers().get(0);

        final MulliganDecisionProvider.SessionStart start = provider.beginCallback(acting, starting,
                new CardCollection(), 0);

        assertEquals(start.getStatus(), MulliganDecisionProvider.Status.UNSUPPORTED_EMPTY_HAND_MULLIGAN);
        assertNull(start.getSession());
    }

    @Test
    public void twoPlayersInOneGameReceiveIndependentParentSessions() {
        final Game game = mulliganGame();
        final Player first = game.getPlayers().get(0);
        final Player second = game.getPlayers().get(1);
        final Card firstCard = addCardToZone("Island", first, ZoneType.Hand);
        final Card secondCard = addCardToZone("Mountain", second, ZoneType.Hand);

        final MulliganDecisionProvider.SessionStart firstStart = provider.beginCallback(first, first,
                new CardCollection(firstCard), 0);
        final MulliganDecisionProvider.SessionStart secondStart = provider.beginCallback(second, first,
                new CardCollection(secondCard), 0);

        assertNotSame(firstStart.getSession(), secondStart.getSession());
        assertEquals(firstStart.getSession().getActingPlayerId(), first.getId());
        assertEquals(secondStart.getSession().getActingPlayerId(), second.getId());
    }

    @Test
    public void outstandingRequestIsScopedToTheCorrectPlayerWhenSessionsInterleave() {
        final Game game = mulliganGame();
        final Player first = game.getPlayers().get(0);
        final Player second = game.getPlayers().get(1);
        final Card firstCard = addCardToZone("Island", first, ZoneType.Hand);
        final Card secondCard = addCardToZone("Mountain", second, ZoneType.Hand);
        final MulliganDecisionProvider.SessionStart firstStart = provider.beginCallback(first, first,
                new CardCollection(firstCard), 0);
        final MulliganDecisionProvider.SessionStart secondStart = provider.beginCallback(second, first,
                new CardCollection(secondCard), 0);
        provider.generateNext(firstStart.getSession());
        provider.generateNext(secondStart.getSession());

        assertEquals(provider.generateNext(firstStart.getSession()).getStatus(),
                MulliganDecisionProvider.Status.REQUEST_OUTSTANDING);
    }

    @Test
    public void changedStartingPlayerIsRejectedOnTheNextRealCallback() {
        final Game game = mulliganGame();
        final Player acting = game.getPlayers().get(1);
        final Player starting = game.getPlayers().get(0);
        final Card first = addCardToZone("Island", acting, ZoneType.Hand);
        final MulliganDecisionProvider.SessionStart firstStart = provider.beginCallback(acting, starting,
                new CardCollection(first), 0);
        final MulliganDecisionProvider.Generation initial = provider.generateNext(firstStart.getSession());
        provider.apply(initial.getRequest(), candidate(initial.getRequest(), "MULLIGAN|REDRAW"));

        final Card replacement = addCardToZone("Mountain", acting, ZoneType.Hand);
        final MulliganDecisionProvider.SessionStart changed = provider.beginCallback(acting, acting,
                new CardCollection(replacement), 1);

        assertEquals(changed.getStatus(), MulliganDecisionProvider.Status.STALE_MULLIGAN);
        assertNull(changed.getSession());
    }

    @Test
    public void malformedReturnCountIsUnsupportedBeforePolicyGeneration() {
        final Game game = mulliganGame();
        final Player acting = game.getPlayers().get(1);
        final Player starting = game.getPlayers().get(0);
        final Card card = addCardToZone("Island", acting, ZoneType.Hand);

        assertEquals(provider.beginCallback(acting, starting, new CardCollection(card), -1).getStatus(),
                MulliganDecisionProvider.Status.UNSUPPORTED_MULLIGAN_STATE);
        assertEquals(provider.beginCallback(acting, starting, new CardCollection(card), 2).getStatus(),
                MulliganDecisionProvider.Status.UNSUPPORTED_MULLIGAN_STATE);
    }

    @Test
    public void terminalSessionIsRemovedFromRegistryAndCannotRestart() {
        final Game game = mulliganGame();
        final Player acting = game.getPlayers().get(1);
        final Player starting = game.getPlayers().get(0);
        final Card card = addCardToZone("Island", acting, ZoneType.Hand);
        final MulliganDecisionProvider localProvider = new MulliganDecisionProvider();
        final MulliganDecisionProvider.SessionStart start = localProvider.beginCallback(acting, starting,
                new CardCollection(card), 0);
        final MulliganDecisionProvider.Generation generation = localProvider.generateNext(start.getSession());
        localProvider.apply(generation.getRequest(), candidate(generation.getRequest(), "MULLIGAN|KEEP"));

        assertEquals(localProvider.activeSessionCount(), 0);
        assertEquals(localProvider.beginCallback(acting, starting, new CardCollection(card), 0).getStatus(),
                MulliganDecisionProvider.Status.STALE_MULLIGAN);
    }

    @Test
    public void gameCleanupClosesAllPlayerSessions() {
        final Game game = mulliganGame();
        final Player first = game.getPlayers().get(0);
        final Player second = game.getPlayers().get(1);
        final Card firstCard = addCardToZone("Island", first, ZoneType.Hand);
        final Card secondCard = addCardToZone("Mountain", second, ZoneType.Hand);
        final MulliganDecisionProvider localProvider = new MulliganDecisionProvider();
        final MulliganDecisionProvider.SessionStart firstStart = localProvider.beginCallback(first, first,
                new CardCollection(firstCard), 0);
        final MulliganDecisionProvider.SessionStart secondStart = localProvider.beginCallback(second, first,
                new CardCollection(secondCard), 0);

        localProvider.endGame(game);

        assertEquals(localProvider.activeSessionCount(), 0);
        assertEquals(localProvider.generateNext(firstStart.getSession()).getStatus(),
                MulliganDecisionProvider.Status.COMPLETE);
        assertEquals(localProvider.generateNext(secondStart.getSession()).getStatus(),
                MulliganDecisionProvider.Status.COMPLETE);
    }

    @Test
    public void multiplayerGameIsOutsideTheOrdinaryV0Admission() {
        final Game game = initAndCreateThreePlayerGame();
        game.setAge(GameStage.Mulligan);
        final Player acting = game.getPlayers().get(1);
        final Player starting = game.getPlayers().get(0);
        final Card card = addCardToZone("Island", acting, ZoneType.Hand);

        assertEquals(provider.beginCallback(acting, starting, new CardCollection(card), 0).getStatus(),
                MulliganDecisionProvider.Status.UNSUPPORTED_MULLIGAN_STATE);
    }

    @Test
    public void nonLondonRuleIsRejectedBeforeNeutralAdmission() {
        final Game game = mulliganGame();
        final Player acting = game.getPlayers().get(1);
        final Player starting = game.getPlayers().get(0);
        final Card card = addCardToZone("Island", acting, ZoneType.Hand);
        final MulliganDecisionProvider nonLondon = new MulliganDecisionProvider(MulliganDefs.MulliganRule.Vancouver);

        final MulliganDecisionProvider.SessionStart start = nonLondon.beginCallback(acting, starting,
                new CardCollection(card), 0);

        assertEquals(start.getStatus(), MulliganDecisionProvider.Status.UNSUPPORTED_MULLIGAN_RULE);
        assertNull(start.getSession());
    }

    @Test
    public void knownSpecialPregameMulliganMechanicsAreUnsupported() {
        final Game serumGame = mulliganGame();
        final Player serumActing = serumGame.getPlayers().get(1);
        final Player serumStarting = serumGame.getPlayers().get(0);
        final Card serum = addCardToZone("Island", serumActing, ZoneType.Hand);
        serum.setName("Serum Powder");
        assertEquals(provider.beginCallback(serumActing, serumStarting, new CardCollection(serum), 0).getStatus(),
                MulliganDecisionProvider.Status.UNSUPPORTED_MULLIGAN_STATE);

        final Game backupGame = mulliganGame();
        final Player backupActing = backupGame.getPlayers().get(1);
        final Player backupStarting = backupGame.getPlayers().get(0);
        addCardToZone("Island", backupActing, ZoneType.Hand);
        final Card backupPlan = addCardToZone("Island", backupActing, ZoneType.Command);
        backupPlan.setName("Backup Plan");
        assertEquals(provider.beginCallback(backupActing, backupStarting,
                new CardCollection(backupActing.getCardsIn(ZoneType.Hand)), 0).getStatus(),
                MulliganDecisionProvider.Status.UNSUPPORTED_MULLIGAN_STATE);
    }

    private Game mulliganGame() {
        final Game game = initAndCreateGame();
        game.setAge(GameStage.Mulligan);
        return game;
    }

    private static LegalCandidate candidate(final DecisionRequest request, final String semanticKey) {
        return request.getCandidates().stream()
                .filter(item -> item.getSemanticKey().equals(semanticKey))
                .findFirst()
                .orElseThrow();
    }
}
