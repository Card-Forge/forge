package forge.game.decision;

import forge.ai.AITest;
import forge.game.Game;
import forge.game.GameStage;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

public class MulliganDiagnosticsTest extends AITest {
    @Test
    public void keepAndRedrawReturnTheNativeBooleanAndRecordSeparatePolicyActions() {
        final MulliganDiagnostics diagnostics = new MulliganDiagnostics(true);
        final Game keepGame = mulliganGame();
        final Player keepActing = keepGame.getPlayers().get(1);
        final Player keepStarting = keepGame.getPlayers().get(0);
        addCardToZone("Island", keepActing, ZoneType.Hand);
        final MulliganDiagnostics.KeepCapture keepCapture = diagnostics.captureKeepOrRedraw(keepActing,
                keepStarting, 0);

        assertTrue(diagnostics.recordKeepOrRedraw(keepCapture, true, 17L));
        assertTrue(diagnostics.events().stream().anyMatch(line -> line.contains("MULLIGAN|KEEP")));

        final Game redrawGame = mulliganGame();
        final Player redrawActing = redrawGame.getPlayers().get(1);
        final Player redrawStarting = redrawGame.getPlayers().get(0);
        addCardToZone("Mountain", redrawActing, ZoneType.Hand);
        final MulliganDiagnostics.KeepCapture redrawCapture = diagnostics.captureKeepOrRedraw(redrawActing,
                redrawStarting, 0);

        assertFalse(diagnostics.recordKeepOrRedraw(redrawCapture, false, 19L));
        assertTrue(diagnostics.events().stream().anyMatch(line -> line.contains("MULLIGAN|REDRAW")));
    }

    @Test
    public void bottomDiagnosticsReturnTheExactControllerCollectionAndPreserveOrder() {
        final MulliganDiagnostics diagnostics = new MulliganDiagnostics(true);
        final Game game = mulliganGame();
        final Player acting = game.getPlayers().get(1);
        final Card first = addCardToZone("Island", acting, ZoneType.Hand);
        final Card second = addCardToZone("Mountain", acting, ZoneType.Hand);
        final Card third = addCardToZone("Forest", acting, ZoneType.Hand);
        final CardCollection callbackHand = new CardCollection(List.of(first, second, third));
        final CardCollectionView controllerResult = new CardCollection(List.of(second, first));
        final MulliganDiagnostics.BottomCapture capture = diagnostics.captureBottom(acting, callbackHand, 2);

        final CardCollectionView returned = diagnostics.recordBottom(capture, controllerResult, 23L);

        assertSame(returned, controllerResult);
        assertTrue(diagnostics.events().stream().anyMatch(line -> line.startsWith("\"CARD_SELECTION_CALLBACK\",")));
        assertTrue(diagnostics.events().stream().anyMatch(line -> line.contains("\"MULLIGAN_BOTTOM\"")));
        assertTrue(diagnostics.events().stream().map(line -> line.split(",", -1))
                .anyMatch(fields -> fields.length > 20 && fields[20].equals("1")));
    }

    @Test
    public void forcedKeepIsRecordedWithoutPolicyInference() {
        final MulliganDiagnostics diagnostics = new MulliganDiagnostics(true);
        final Game game = mulliganGame();
        final Player acting = game.getPlayers().get(1);
        final Player starting = game.getPlayers().get(0);
        addCardToZone("Island", acting, ZoneType.Hand);

        diagnostics.recordForcedKeep(acting, starting, 0);

        assertTrue(diagnostics.events().stream().anyMatch(line -> line.startsWith("\"MULLIGAN_CALLBACK\",")));
        assertTrue(diagnostics.events().stream().noneMatch(line -> line.startsWith("\"MULLIGAN\",")));
    }

    @Test
    public void bottomMappingFailureIsFailOpenAndDoesNotReplaceControllerResult() {
        final MulliganDiagnostics diagnostics = new MulliganDiagnostics(true);
        final Game game = mulliganGame();
        final Player acting = game.getPlayers().get(1);
        final Card callbackCard = addCardToZone("Island", acting, ZoneType.Hand);
        final Card unknown = addCardToZone("Forest", acting, ZoneType.Hand);
        final CardCollectionView controllerResult = new CardCollection(unknown);
        final MulliganDiagnostics.BottomCapture capture = diagnostics.captureBottom(acting,
                new CardCollection(callbackCard), 1);

        final CardCollectionView returned = diagnostics.recordBottom(capture, controllerResult, 29L);

        assertSame(returned, controllerResult);
        assertTrue(diagnostics.events().stream().anyMatch(line -> line.startsWith("\"MULLIGAN_STATE\",")
                && line.contains("\"MAPPING_FAILED\"")));
    }

    @Test
    public void diagnosticsDoNotExportOpponentPrivateCards() {
        final MulliganDiagnostics diagnostics = new MulliganDiagnostics(true);
        final Game game = mulliganGame();
        final Player acting = game.getPlayers().get(1);
        final Player starting = game.getPlayers().get(0);
        addCardToZone("Island", acting, ZoneType.Hand);
        addCardToZone("Mountain", starting, ZoneType.Hand);
        final MulliganDiagnostics.KeepCapture capture = diagnostics.captureKeepOrRedraw(acting, starting, 0);

        diagnostics.recordKeepOrRedraw(capture, true, 31L);

        assertTrue(diagnostics.events().stream().noneMatch(line -> line.contains("Mountain")));
    }

    private Game mulliganGame() {
        final Game game = initAndCreateGame();
        game.setAge(GameStage.Mulligan);
        return game;
    }
}
