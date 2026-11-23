package forge.web.match;

import com.google.gson.JsonObject;
import forge.deck.Deck;
import forge.gamemodes.match.LobbySlot;
import forge.gamemodes.match.LobbySlotType;
import forge.gamemodes.net.server.ServerGameLobby;

import java.util.Map;

/** Gives tests outside this package access to match internals that the classes keep out of their public API. */
public final class MatchProbe {
    private MatchProbe() {
    }

    /** The number of properties the match dropped because they have no JSON form. Tests check that it stays at zero. */
    public static int skippedProperties(final WebGuiGame gui) {
        return gui.skippedProperties();
    }

    /** The model a browser would have if it were sent the whole game now. Call it on the match's dispatch thread. */
    public static BrowserModel freshSnapshot(final WebGuiGame gui) {
        final BrowserModel fresh = new BrowserModel();
        fresh.applyStateMessage(gui.fullState());
        return fresh;
    }

    /** Opens a table for the web seat and one computer seat, and starts the match at once. Call it on the host UI thread. */
    public static void startMatch(final NetplaySeat local, final Deck webDeck, final Deck aiDeck, final WebGuiGame gui) {
        local.openHost("Web Player", gui, () -> { }, (from, text) -> { });
        final ServerGameLobby table = local.hostedLobby();
        while (table.getNumberOfSlots() < 2) {
            table.addSlot();
        }
        final LobbySlot ai = table.getSlot(local.webSeat() == 0 ? 1 : 0);
        ai.setType(LobbySlotType.AI);
        ai.setName("AI");
        ai.setDeck(aiDeck);
        ai.setIsReady(true);
        table.getSlot(local.webSeat()).setDeck(webDeck);
        table.getSlot(local.webSeat()).setIsReady(true);
        final Runnable begin = local.prepare();
        if (begin == null) {
            throw new IllegalStateException("The table refused to start the match");
        }
        begin.run();
    }

    public static Map<Integer, JsonObject> objects(final BrowserModel model) {
        return model.objectsCopy();
    }
}
