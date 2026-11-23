package forge.web;

import com.google.gson.JsonObject;
import forge.deck.Deck;
import forge.gamemodes.match.HostedMatch;
import forge.gamemodes.net.server.RemoteClientGuiGame;
import forge.gui.GuiBase;
import forge.web.match.BrowserModel;
import forge.web.match.NetplaySeat;
import forge.web.match.MatchProbe;
import forge.web.match.WebGuiGame;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/** Plays a whole game on the web client's real path: loopback host, netplay client, dispatch executor and JSON forwarding. */
public class LoopbackGameTest {
    @BeforeClass
    public void setUp() {
        WebTestSupport.initModel();
    }

    private static void awaitGameStarted(final NetplaySeat local, final TestBrowser browser) {
        WebHubTest.awaitTrue(() -> {
            final HostedMatch match = local.hostedMatch();
            return match != null && match.getGame() != null && browser.all("state").size() > 1;
        }, "game did not start");
    }

    // Fails if a game on the real web path does not finish with a score, the client cannot answer while the host waits, the browser's model built from updates differs from a fresh snapshot, the log or land sound is missing, or a second match cannot start
    @Test(timeOut = 360000)
    public void passiveGameCompletesAndIncrementalMatchesFull() throws Exception {
        final Deck bears = TestDecks.of("Bears", "Grizzly Bears", 20, "Forest", 20);
        // The web seat loses by drawing from an empty library a few turns in
        final Deck islands = TestDecks.of("Islands", "Island", 8);
        TestMatch.play(islands, bears, gui -> new TestBrowser(gui, true), (local, gui, browser) -> {
            awaitGameStarted(local, browser);

            // The host UI thread blocks while it waits for a client reply. The client must answer without that thread
            final RemoteClientGuiGame remote = WebTestSupport.remoteGui(local.hostedMatch());
            final CompletableFuture<Boolean> probe = new CompletableFuture<>();
            GuiBase.getInterface().invokeInEdtLater(() -> probe.complete(remote.showConfirmDialog("probe", "probe", "Yes", "No", true)));
            Assert.assertTrue(probe.get(20, TimeUnit.SECONDS));

            Assert.assertTrue(browser.gameOver.await(300, TimeUnit.SECONDS), "game did not finish");
            // The score refers to both players by reference, and credits the one game to its winner
            final JsonObject scored = browser.awaitMatching("gameOver", m -> !m.getAsJsonArray("score").isEmpty(), "the game ended with no score");
            Assert.assertEquals(scored.getAsJsonArray("score").size(), 2);
            Assert.assertEquals(scored.getAsJsonArray("score").asList().stream().mapToInt(e -> e.getAsJsonObject().get("won").getAsInt()).sum(), 1);
            final int root = browser.last("state").get("root").getAsInt();
            final int turns = MatchProbe.objects(browser.model).get(root).get("Turn").getAsInt();
            Assert.assertTrue(turns > 2, "the game ended on turn " + turns);
            final BrowserModel fresh = CompletableFuture.supplyAsync(() -> MatchProbe.freshSnapshot(gui), gui.dispatchExecutor()).get(10, TimeUnit.SECONDS);
            WebTestSupport.assertSameIgnoringZone(MatchProbe.objects(browser.model), MatchProbe.objects(fresh));
            Assert.assertEquals(MatchProbe.skippedProperties(gui), 0, "properties with no JSON form");
            // The client builds its log from forwarded game events
            final long turnEntries = browser.all("log").stream()
                    .flatMap(m -> m.getAsJsonArray("entries").asList().stream())
                    .filter(e -> "TURN".equals(e.getAsJsonObject().get("type").getAsString())).count();
            Assert.assertTrue(turnEntries >= turns - 1, turnEntries + " turn log entries for " + turns + " turns");
            // Land plays name their card, so the log can show it
            Assert.assertTrue(browser.all("log").stream().flatMap(m -> m.getAsJsonArray("entries").asList().stream())
                    .anyMatch(e -> "LAND".equals(e.getAsJsonObject().get("type").getAsString()) && e.getAsJsonObject().has("imageKey")),
                    "no land entry carried its card");
            Assert.assertTrue(browser.all("sound").stream().anyMatch(m -> m.get("name").getAsString().endsWith("_land")),
                    "no land made its sound");

            // A second match reuses the running loopback host
            final WebGuiGame second = new WebGuiGame(PlayerSettings.saved());
            final TestBrowser secondBrowser = new TestBrowser(second, true);
            second.attach(secondBrowser);
            WebHubTest.onUi(() -> MatchProbe.startMatch(local, islands, bears, second));
            awaitGameStarted(local, secondBrowser);
        });
    }
}
