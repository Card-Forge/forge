package forge.web;

import forge.ImageKeys;
import forge.gui.GuiBase;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.util.IHasForgeLog;
import forge.util.ImageFetcher;
import forge.web.images.CardThumbnails;
import forge.web.images.Foil;
import forge.web.images.ImageFiles;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpResponseStatus;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static forge.web.WebServer.KEEP_FOREVER;
import static forge.web.WebServer.notFound;
import static forge.web.WebServer.respond;

/**
 * Answers the requests for card pictures and sleeve art. A picture that is not saved yet is answered as missing, and
 * downloaded meanwhile. When the download arrives, every browser is told, and the page asks for the picture again, as
 * desktop and mobile draw a stand-in and repaint when the fetcher calls back.
 */
final class ImageReplies implements IHasForgeLog {
    /** Pictures answered as missing and not yet arrived. The fetcher calls back once for each request, but each arrival is announced once. */
    private static final Set<String> AWAITED = ConcurrentHashMap.newKeySet();

    private ImageReplies() {
    }

    /** When auto is true the page asked without a player's action, and uncached art is downloaded only when online image fetching is on. */
    static void serveSleeveArt(final ChannelHandlerContext ctx, final String asked, final boolean auto, final Runnable arrived) {
        final String key = ImageFiles.artKey(asked);
        final File cached = ImageFiles.sleeveArt(key);
        if (cached != null) {
            respondImage(ctx, cached);
            return;
        }
        notFound(ctx);
        // Only a card has art to crop, not a token or an effect such as the Ring
        if (key.startsWith(ImageKeys.CARD_PREFIX) && (!auto || FModel.getPreferences().getPrefBoolean(FPref.UI_ENABLE_ONLINE_IMAGE_FETCHER))) {
            fetch(true, key, arrived);
        }
    }

    /** small asks for the card at the size the board draws it. */
    static void serveImage(final ChannelHandlerContext ctx, final String key, final boolean small, final Runnable arrived) {
        final File file = ImageFiles.card(key);
        if (file != null) {
            respondCardImage(ctx, file, small, Foil.seed(key));
            return;
        }
        notFound(ctx);
        if (ImageFiles.safeKey(key) && FModel.getPreferences().getPrefBoolean(FPref.UI_ENABLE_ONLINE_IMAGE_FETCHER)) {
            fetch(false, key, () -> {
                // Forge remembers a picture it could not find, and without this it would still report the picture missing now that it exists
                ImageKeys.clearMissingCards();
                arrived.run();
            });
        }
    }

    /** Starts a download on the UI thread. The fetcher calls back only when the picture arrives. */
    private static void fetch(final boolean sleeveArt, final String key, final Runnable arrived) {
        final String awaited = (sleeveArt ? "art:" : "card:") + key;
        AWAITED.add(awaited);
        final ImageFetcher.Callback once = () -> {
            if (AWAITED.remove(awaited)) {
                arrived.run();
            }
        };
        GuiBase.getInterface().invokeInEdtLater(() -> {
            if (sleeveArt) {
                GuiBase.getInterface().getImageFetcher().fetchSleeveArt(key, once);
            } else {
                GuiBase.getInterface().getImageFetcher().fetchImage(key, once);
            }
        });
    }

    private static void respondCardImage(final ChannelHandlerContext ctx, final File file, final boolean small, final int foil) {
        if (!small && foil == 0) {
            respondImage(ctx, file);
            return;
        }
        CardThumbnails.of(file, small, foil).whenComplete((bytes, failed) -> {
            if (bytes != null) {
                respond(ctx, HttpResponseStatus.OK, bytes, ImageFiles.type(file), null, KEEP_FOREVER, null);
                return;
            }
            netLog.warn("Could not draw {}: {}", file, failed.getMessage());
            respondImage(ctx, file);
        });
    }

    private static void respondImage(final ChannelHandlerContext ctx, final File file) {
        try {
            respond(ctx, HttpResponseStatus.OK, Files.readAllBytes(file.toPath()), ImageFiles.type(file), null, KEEP_FOREVER, null);
        } catch (final IOException e) {
            netLog.warn("Could not send {}: {}", file, e.getMessage());
            notFound(ctx);
        }
    }
}
