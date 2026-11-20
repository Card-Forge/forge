package forge.web;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import forge.card.CardStateName;
import forge.deck.Deck;
import forge.game.Game;
import forge.game.GameState;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.gamemodes.net.DeltaPacket;
import forge.web.match.MatchProbe;
import forge.web.match.WebGuiGame;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class ActiveClientTest {
    @BeforeClass
    public void setUp() {
        WebTestSupport.initModel();
    }

    // Basic lands only, so the AI does nothing. The cards a test needs are placed with a GameState, never drawn
    private static Deck plains() {
        return TestDecks.of("Plains", "Plains", 40);
    }

    private static Deck forests() {
        return TestDecks.of("Forests", "Forest", 40);
    }

    // GameState calls the first player "human" and the second "ai", whoever controls them
    private static void giveWebSeat(final Game game, final String hand, final String battlefield, final String library) throws InterruptedException {
        giveWebSeat(game, hand, battlefield, library, "", "");
    }

    private static void giveWebSeat(final Game game, final String hand, final String battlefield, final String library, final String aiBattlefield,
            final String aiHand) throws InterruptedException {
        final boolean webFirst = WebTestSupport.isWebSeat(game.getPlayers().get(0));
        final String web = webFirst ? "human" : "ai";
        final String ai = webFirst ? "ai" : "human";
        final GameState state = new GameState();
        state.parse(List.of(
                "activeplayer=" + web,
                "activephase=MAIN1",
                "removesummoningsickness=true",
                web + "life=20",
                web + "hand=" + hand,
                web + "battlefield=" + battlefield,
                web + "library=" + library,
                ai + "life=20",
                ai + "hand=" + aiHand,
                ai + "battlefield=" + aiBattlefield,
                ai + "library=Forest;Forest;Forest;Forest;Forest"));
        TestMatch.apply(state, game);
    }

    // Fails if the web seat never gets the Temple's scry or Evolving Wilds' library search, including after a reload while a request is open, or if a property has no JSON form
    @Test(timeOut = 180000)
    public void actingSeatReachesScryAndLibrarySearch() throws Exception {
        TestMatch.play(plains(), forests(), gui -> TestBrowser.held(gui, 50), (local, gui, browser) -> {
            Assert.assertTrue(browser.atOwnMain.await(120, TimeUnit.SECONDS), "the web seat never reached its main phase");

            // The Temple's enter trigger scrys. Evolving Wilds searches the library
            final Game game = local.hostedMatch().getGame();
            giveWebSeat(game, "Temple of Enlightenment", "Evolving Wilds", "Plains;Plains;Plains;Plains;Plains");
            // Placing cards fires no game event. The host's resync sends them while the engine waits for this seat
            WebTestSupport.remoteGui(local.hostedMatch()).updateGameView();
            // A released browser skips a card it does not have yet, and then passes priority
            final int temple = cardKey(game, ZoneType.Hand, true, "Temple of Enlightenment", false);
            final int wilds = cardKey(game, ZoneType.Battlefield, true, "Evolving Wilds", false);
            WebHubTest.awaitTrue(() -> MatchProbe.objects(browser.model).keySet().containsAll(List.of(temple, wilds)),
                    "the browser never got the cards");
            // Release one card at a time, so the Wilds activation cannot interleave with the Temple's trigger
            browser.release("Hand:Temple of Enlightenment");
            WebHubTest.awaitTrue(() -> asked(browser, "manipulate"), "no scry reached the browser");

            browser.release("Battlefield:Evolving Wilds");
            WebHubTest.awaitTrue(() -> shown(browser, "Library"), "no library search reached the browser");
            Assert.assertTrue(browser.reloaded, "no reload happened mid-request");
            Assert.assertEquals(MatchProbe.skippedProperties(gui), 0);
            TestBrowser.command(gui, TestBrowser.action("concede"));
        });
    }

    private static boolean asked(final TestBrowser browser, final String kind) {
        return browser.all("request").stream().anyMatch(m -> kind.equals(m.get("kind").getAsString()));
    }

    private static boolean shown(final TestBrowser browser, final String zone) {
        return browser.all("zones").stream().flatMap(m -> m.getAsJsonArray("show").asList().stream())
                .anyMatch(z -> zone.equals(z.getAsJsonObject().get("zone").getAsString()));
    }

    private static boolean atPriority(final TestBrowser browser) {
        final JsonObject prompt = browser.last("prompt");
        return prompt.has("priority") && prompt.get("priority").getAsBoolean();
    }

    private static boolean okEnabled(final TestBrowser browser) {
        return browser.last("prompt").getAsJsonObject("ok").get("enabled").getAsBoolean();
    }

    // Answers the pre-game prompts with OK, then waits at the first priority prompt and returns that turn's number
    private static int keepAndHoldPriority(final WebGuiGame gui, final TestBrowser browser) throws InterruptedException {
        for (int i = 0; i < 6000 && !(browser.turn() > 0 && okEnabled(browser) && atPriority(browser)); i++) {
            if (browser.turn() == 0 && okEnabled(browser)) {
                // A press sent before the host registers the input is dropped, so press again if no new prompt follows
                final int prompts = browser.all("prompt").size();
                TestBrowser.command(gui, TestBrowser.action("ok"));
                for (int j = 0; j < 300 && browser.all("prompt").size() == prompts; j++) {
                    Thread.sleep(10);
                }
            } else {
                Thread.sleep(10);
            }
        }
        final int held = browser.turn();
        Assert.assertTrue(held > 0, "the web seat never got priority; last prompt " + browser.last("prompt"));
        return held;
    }

    private static int cardKey(final Game game, final ZoneType zone, final boolean webSeat, final String name, final boolean faceDown) {
        for (final Player p : game.getPlayers()) {
            if (WebTestSupport.isWebSeat(p) != webSeat) {
                continue;
            }
            for (final Card c : p.getCardsIn(zone)) {
                if (c.isFaceDown() == faceDown && name.equals(c.getState(CardStateName.Original).getName())) {
                    return DeltaPacket.makeDeltaKey(DeltaPacket.TYPE_CARD_VIEW, c.getId());
                }
            }
        }
        throw new AssertionError("no " + name + " in " + zone);
    }

    private static JsonArray faces(final WebGuiGame gui, final TestBrowser browser, final int key) throws InterruptedException {
        final JsonObject request = TestBrowser.action("detail");
        request.addProperty("key", key);
        TestBrowser.command(gui, request);
        for (int i = 0; i < 500; i++) {
            for (final JsonObject m : browser.all("detail")) {
                if (m.get("key").getAsInt() == key) {
                    return m.getAsJsonArray("faces");
                }
            }
            Thread.sleep(10);
        }
        throw new AssertionError("no detail for card " + key);
    }

    // Fails if card detail leaves out a face of a double-faced, split or own face-down card, shows another face or the name of an opponent's face-down card, or the visible list holds the opponent's hand card or leaves out the player's own hand or face-down card
    @Test(timeOut = 180000)
    public void cardDetailShowsOtherFacesOnlyToThoseWhoMaySeeThem() throws Exception {
        TestMatch.play(plains(), forests(), gui -> new TestBrowser(gui, false), (local, gui, browser) -> {
            keepAndHoldPriority(gui, browser);
            final Game game = local.hostedMatch().getGame();
            giveWebSeat(game, "Fire // Ice", "Delver of Secrets;Grizzly Bears|FaceDown", "Plains;Plains;Plains", "Grizzly Bears|FaceDown", "Llanowar Elves");
            WebTestSupport.remoteGui(local.hostedMatch()).updateGameView();
            final int delver = cardKey(game, ZoneType.Battlefield, true, "Delver of Secrets", false);
            final int split = cardKey(game, ZoneType.Hand, true, "Fire // Ice", false);
            final int ownFaceDown = cardKey(game, ZoneType.Battlefield, true, "Grizzly Bears", true);
            final int theirFaceDown = cardKey(game, ZoneType.Battlefield, false, "Grizzly Bears", true);
            final int theirHand = cardKey(game, ZoneType.Hand, false, "Llanowar Elves", false);
            WebHubTest.awaitTrue(() -> MatchProbe.objects(browser.model).keySet().containsAll(List.of(delver, split, ownFaceDown, theirFaceDown)),
                    "the browser never got the cards");
            // Each state lists the visible cards among all those sent so far, so the latest state covers these cards
            final List<Integer> visible = browser.last("state").getAsJsonArray("visible").asList().stream().map(JsonElement::getAsInt).toList();
            Assert.assertTrue(visible.containsAll(List.of(split, ownFaceDown)), "the player's own hand or face-down card is not visible: " + visible);
            Assert.assertFalse(visible.contains(theirHand), "the opponent's hand card is visible: " + visible);

            Assert.assertEquals(faces(gui, browser, delver).size(), 2, "a double-faced card shows both faces");
            Assert.assertEquals(faces(gui, browser, split).size(), 3, "a split card shows the whole card and each half");
            Assert.assertEquals(faces(gui, browser, ownFaceDown).size(), 2, "the owner sees the face of their face-down card");
            final JsonArray hidden = faces(gui, browser, theirFaceDown);
            Assert.assertEquals(hidden.size(), 1, "an opponent's face-down card offers another face");
            Assert.assertFalse(hidden.toString().contains("Grizzly"), "an opponent's face-down card revealed its name: " + hidden);
            TestBrowser.command(gui, TestBrowser.action("concede"));
        });
    }

    // Fails if the player the web seat picks for Jötun Grunt's "whose graveyard" choice does not map back across netplay to that player, so the Grunt is sacrificed
    @Test(timeOut = 180000)
    public void playerChoicePromptIsAnsweredFromTheWebSeat() throws Exception {
        // Passes priority, takes the first option of optional choices and presses OK to pay costs
        TestMatch.play(plains(), forests(), gui -> TestBrowser.held(gui, 50), (local, gui, browser) -> {
            Assert.assertTrue(browser.atOwnMain.await(120, TimeUnit.SECONDS), "the web seat never reached its main phase");
            final Game game = local.hostedMatch().getGame();
            final boolean webFirst = WebTestSupport.isWebSeat(game.getPlayers().get(0));
            final String web = webFirst ? "human" : "ai";
            final String ai = webFirst ? "ai" : "human";
            final GameState state = new GameState();
            // The computer's turn is ending, so the web seat's upkeep comes next
            state.parse(List.of(
                    "activeplayer=" + ai,
                    "activephase=END_OF_TURN",
                    web + "life=20",
                    ai + "life=20",
                    web + "battlefield=J\u00f6tun Grunt",
                    web + "graveyard=Plains;Plains;Plains",
                    web + "hand=",
                    web + "library=Plains;Plains;Plains;Plains;Plains",
                    ai + "battlefield=",
                    ai + "graveyard=",
                    ai + "hand=",
                    ai + "library=Forest;Forest;Forest;Forest;Forest"));
            TestMatch.apply(state, game);
            WebTestSupport.remoteGui(local.hostedMatch()).updateGameView();
            final Player seat = game.getPlayers().get(webFirst ? 0 : 1);
            final int turn = game.getPhaseHandler().getTurn();
            browser.release();
            // Wait for the web seat's next turn, past the upkeep that asks for the payment
            WebHubTest.awaitTrue(() -> game.getPhaseHandler().getTurn() > turn && game.getPhaseHandler().getPhase().isAfter(PhaseType.UPKEEP),
                    () -> "the next turn's upkeep never ended: turn " + game.getPhaseHandler().getTurn() + " in " + game.getPhaseHandler().getPhase());
            Assert.assertEquals(game.getPhaseHandler().getPlayerTurn(), seat, "the web seat's next turn never came: turn "
                    + game.getPhaseHandler().getTurn() + " in " + game.getPhaseHandler().getPhase());
            boolean gruntSurvived = false;
            for (final Card c : seat.getCardsIn(ZoneType.Battlefield)) {
                gruntSurvived |= c.getName().startsWith("J");
            }
            Assert.assertTrue(gruntSurvived, "the cumulative upkeep went unpaid and the Grunt was sacrificed");
            Assert.assertEquals(seat.getCardsIn(ZoneType.Graveyard).size(), 1, "two cards should have left the graveyard");
            TestBrowser.command(gui, TestBrowser.action("concede"));
        });
    }
}
