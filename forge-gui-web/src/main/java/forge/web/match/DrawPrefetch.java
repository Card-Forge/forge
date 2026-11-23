package forge.web.match;

import com.google.common.eventbus.Subscribe;
import forge.ImageKeys;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CardView;
import forge.game.event.GameEvent;
import forge.game.player.Player;
import forge.game.zone.PlayerZone;
import forge.game.zone.ZoneType;
import forge.gui.GuiBase;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.web.WebGuiBase;
import forge.web.images.CardThumbnails;
import forge.web.images.Foil;
import forge.web.images.ImageFiles;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.Executor;

/**
 * Readies the picture of the top card of each library before it is drawn, so the browser gets it at once. The host downloads it if it is
 * missing and makes the sizes the browser asks for. The browser is never told which card it is, because the library is hidden.
 */
final class DrawPrefetch {
    private static final Executor WORK = WebGuiBase.worker("DrawPrefetch");
    private static final Set<Game> WATCHED = Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));

    private final Game game;
    /** The top card last readied for each player. Only the game thread reads or changes it. */
    private final Map<Player, Card> readied = new HashMap<>();

    private DrawPrefetch(final Game game) {
        this.game = game;
    }

    /** Starts watching a game's libraries. A game already watched is left as it is. */
    static void watch(final Game game) {
        if (game != null && WATCHED.add(game)) {
            game.subscribeToEvents(new DrawPrefetch(game));
        }
    }

    // Events are posted on the game thread, which is the only thread that changes a library
    @Subscribe
    public void receive(final GameEvent event) {
        for (final Player p : game.getPlayers()) {
            final PlayerZone library = p.getZone(ZoneType.Library);
            final Card top = library == null || library.isEmpty() ? null : library.get(0);
            if (top != null && readied.put(p, top) != top) {
                final CardView view = top.getView();
                final String key = Foil.key(view, view.getCurrentState());
                if (key != null) {
                    WORK.execute(() -> ready(key));
                }
            }
        }
    }

    /** Downloads the picture if it is missing, then makes the sizes the server would make when the browser asks. */
    private static void ready(final String key) {
        final File file = ImageFiles.card(key);
        if (file != null) {
            thumbnails(file, key);
        } else if (ImageFiles.safeKey(key) && FModel.getPreferences().getPrefBoolean(FPref.UI_ENABLE_ONLINE_IMAGE_FETCHER)) {
            GuiBase.getInterface().invokeInEdtLater(() -> GuiBase.getInterface().getImageFetcher().fetchImage(key, () -> {
                // Forge remembers a picture it could not find. Without this it would still report the picture as missing
                ImageKeys.clearMissingCards();
                final File fetched = ImageFiles.card(key);
                if (fetched != null) {
                    thumbnails(fetched, key);
                }
            }));
        }
    }

    // The board asks for the small size. A foil is painted at both sizes, because its preview is also drawn by the server
    private static void thumbnails(final File file, final String key) {
        final int foil = Foil.seed(key);
        CardThumbnails.of(file, true, foil);
        if (foil > 0) {
            CardThumbnails.of(file, false, foil);
        }
    }
}
