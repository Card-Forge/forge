package forge.web;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import forge.deck.Deck;
import forge.game.Game;
import forge.game.GameState;
import forge.web.match.BrowserModel;
import forge.web.match.MatchProbe;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/** Tests what one message to a client may contain. It reads every message from ten turns of the computer playing itself. */
public class BatchBoundaryTest {
    private static final String SPARK = "Spark Elemental";

    @BeforeClass
    public void setUp() {
        WebTestSupport.initModel();
    }

    private static Set<Integer> onBattlefields(final BrowserModel model) {
        final Set<Integer> cards = new HashSet<>();
        for (final JsonObject object : MatchProbe.objects(model).values()) {
            final JsonElement zone = object.get("Battlefield");
            if (zone != null && zone.isJsonArray()) {
                zone.getAsJsonArray().forEach(card -> cards.add(card.getAsJsonObject().get("ref").getAsInt()));
            }
        }
        return cards;
    }

    private static long named(final BrowserModel model, final Set<Integer> cards, final String name) {
        final Map<Integer, JsonObject> objects = MatchProbe.objects(model);
        return cards.stream().map(objects::get).filter(card -> card != null && card.has("CurrentState"))
                .map(card -> objects.get(card.getAsJsonObject("CurrentState").get("ref").getAsInt()))
                .filter(state -> state != null && state.has("Name") && name.equals(state.get("Name").getAsString())).count();
    }

    private static String zone(final JsonObject move, final String end) {
        final JsonElement place = move.get(end);
        return place == null || place.isJsonNull() ? "" : place.getAsJsonObject().get("zone").getAsString();
    }

    // Fails if one message to a client splits an action, such as a card leaving a battlefield before its move, holds a cast with its resolution, a land with a later cast, or a resolution with the next turn, or the browser's model built from the messages differs from a fresh snapshot
    @Test(timeOut = 360000)
    public void aMessageHoldsWholeActionsAndNoMoreThanOne() throws Exception {
        final Deck red = TestDecks.of("Red", "Mountain", 22, "Shock", 10, "Goblin Piker", 14, "Raging Goblin", 14);
        TestMatch.play(red, red, gui -> TestBrowser.held(gui, 0), (local, gui, browser) -> {
            // The game waits on the web seat in its first main phase, so the state can be placed while nothing else changes the game
            Assert.assertTrue(browser.atOwnMain.await(120, TimeUnit.SECONDS), "the web seat never reached its main phase");
            final Game played = local.hostedMatch().getGame();
            // GameState calls the first player "human" and the second "ai", whoever controls them
            final String web = WebTestSupport.isWebSeat(played.getPlayers().get(0)) ? "human" : "ai";
            // Each side has a Spark Elemental, which is sacrificed at every end step, so a card is sure to leave a battlefield
            final GameState state = new GameState();
            final String hand = "Mountain;Shock;Raging Goblin;Goblin Piker";
            final String library = String.join(";", Collections.nCopies(6, "Mountain;Shock;Raging Goblin;Goblin Piker"));
            state.parse(List.of("activeplayer=" + web, "activephase=MAIN1", "removesummoningsickness=true", "humanlife=20", "ailife=20",
                    "humanbattlefield=Mountain;Mountain;Mountain;" + SPARK, "humanhand=" + hand, "humanlibrary=" + library,
                    "aibattlefield=Mountain;Mountain;Mountain;" + SPARK, "aihand=" + hand, "ailibrary=" + library));
            TestMatch.apply(state, played);
            // The computer takes the web seat, so both sides cast spells and nobody is asked anything
            local.spectate();
            browser.release();
            // Ten turns of this deck cover every case below many times, and a whole game takes much longer
            for (int i = 0; i < 30000 && browser.gameOver.getCount() > 0 && browser.turn() < 10; i++) {
                Thread.sleep(10);
            }

            final BrowserModel replay = new BrowserModel();
            Set<Integer> before = Set.of();
            boolean placed = false;
            final List<String> leftUnsaid = new ArrayList<>();
            int left = 0, castThenResolved = 0, resolvedIntoNextTurn = 0, landThenCast = 0, resolutions = 0, lands = 0, casts = 0;
            for (final JsonObject message : browser.all("state")) {
                replay.applyStateMessage(message);
                final Set<Integer> after = onBattlefields(replay);
                // Placing the state removes whatever was played before it without a move, so the checks start once both Elementals are in play
                if (!placed) {
                    placed = named(replay, after, SPARK) == 2;
                    before = after;
                    continue;
                }
                if (message.get("full").getAsBoolean()) {
                    before = after;
                    continue;
                }
                final Set<Integer> movedOff = new HashSet<>();
                boolean cast = false, land = false, resolved = false;
                final JsonArray events = message.getAsJsonArray("events");
                for (final JsonElement e : events) {
                    final JsonObject event = e.getAsJsonObject();
                    switch (event.get("kind").getAsString()) {
                        case "cardMoved" -> {
                            if ("Battlefield".equals(zone(event, "from"))) {
                                movedOff.add(event.getAsJsonObject("card").get("ref").getAsInt());
                            }
                            if ("Hand".equals(zone(event, "from")) && "Battlefield".equals(zone(event, "to"))) {
                                land = true;
                                lands++;
                            }
                        }
                        case "stackAdded" -> {
                            if (land) landThenCast++;
                            cast = true;
                            casts++;
                        }
                        case "stackResolved" -> {
                            if (cast) castThenResolved++;
                            resolved = true;
                            resolutions++;
                        }
                        default -> { }
                    }
                }
                for (final Integer card : before) {
                    if (!after.contains(card)) {
                        left++;
                        if (!movedOff.contains(card)) leftUnsaid.add(card + " in message " + message.get("seq"));
                    }
                }
                final JsonObject game = message.getAsJsonObject("deltas").getAsJsonObject(message.get("root").getAsString());
                if (resolved && game != null && game.has("Turn")) resolvedIntoNextTurn++;
                before = after;
            }
            // Fails if the game never did what the checks below test, which would make them pass on nothing
            Assert.assertTrue(left > 0 && resolutions > 0 && lands > 0 && casts > 0,
                    left + " cards left a battlefield, " + resolutions + " resolutions, " + lands + " lands, " + casts + " casts");
            // Each check runs even when an earlier one fails, so one run reports every problem
            final SoftAssert pins = new SoftAssert();
            // Fails if a card leaves a battlefield in one message but its move comes in a later one. The client then cannot animate the move
            pins.assertEquals(leftUnsaid, List.of(), "cards that left a battlefield without their move");
            // Fails if a spell is cast and resolves in one message, so a client never sees it on the stack
            pins.assertEquals(castThenResolved, 0, "messages holding a cast and then a resolution");
            // Fails if a land play is held until the player's next action, so others see it late
            pins.assertEquals(landThenCast, 0, "messages holding a land and then a cast");
            // Fails if a resolution arrives with the next turn, so a client shows the new turn with the spell still on the stack
            pins.assertEquals(resolvedIntoNextTurn, 0, "messages holding a resolution and the next turn");
            pins.assertAll();
            // The game goes on, so the browser's model and a fresh snapshot are read together on the dispatch thread, between two messages
            final List<Map<Integer, JsonObject>> both = CompletableFuture.supplyAsync(() -> List.of(MatchProbe.objects(browser.model),
                    MatchProbe.objects(MatchProbe.freshSnapshot(gui))), gui.dispatchExecutor()).get(10, TimeUnit.SECONDS);
            WebTestSupport.assertSameIgnoringZone(both.get(0), both.get(1));
        });
    }
}
