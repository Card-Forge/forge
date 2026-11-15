package forge.web.images;

import forge.gui.FThreads;
import forge.gui.GuiBase;
import forge.localinstance.properties.ForgeConstants;
import forge.util.IHasForgeLog;
import forge.util.ImageFetcher;
import forge.util.ScryfallRateLimiter;
import forge.util.TextUtil;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * Downloads missing card images into the shared image cache, like the desktop and mobile fetchers. It saves the bytes as they
 * arrive, because the browser decodes the picture itself, so neither desktop's ImageIO re-encoding nor mobile's FileHandle writing fits.
 */
public final class WebImageFetcher extends ImageFetcher implements IHasForgeLog {
    @Override
    protected Runnable getDownloadTask(final String[] downloadUrls, final String destPath, final Runnable notifyObservers) {
        return () -> {
            // As in desktop and mobile, observers hear only of a picture that arrived
            for (final String url : downloadUrls) {
                if (fetch(url.replace("PLANECHASEBG:", ""), destPath)) {
                    FThreads.invokeInEdtLater(notifyObservers);
                    return;
                }
            }
        };
    }

    /** Like desktop, a token address that fails to connect is tried again without its set code. */
    private boolean fetch(final String url, final String destPath) {
        try {
            return download(url, savedAt(url, destPath));
        } catch (final IOException e) {
            netLog.warn("Card image download failed for {}: {}", url, e.getMessage());
        }
        final int set = url.lastIndexOf('_');
        final int type = url.lastIndexOf('.');
        if (!url.contains("tokens") || set < 0 || set > type) {
            return false;
        }
        final String setless = url.substring(0, set) + url.substring(type);
        try {
            return download(setless, savedAt(setless, destPath));
        } catch (final IOException e) {
            netLog.warn("Card image download failed for {}: {}", setless, e.getMessage());
            return false;
        }
    }

    /** Saves the picture at url to path. Returns false when the address is skipped, refused or empty. */
    private static boolean download(final String url, final String path) throws IOException {
        if ((disableHostedDownload && url.startsWith(ForgeConstants.URL_CARDFORGE)) || ScryfallRateLimiter.shouldSkip(url)) {
            return false;
        }
        ScryfallRateLimiter.acquire(url);
        final HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        try {
            c.setRequestProperty("Accept", "*/*");
            c.setRequestProperty("User-Agent", GuiBase.getInterface().getUserAgent());
            c.setConnectTimeout(10000);
            c.setReadTimeout(30000);
            final int code = c.getResponseCode();
            if (code != HttpURLConnection.HTTP_OK) {
                ScryfallRateLimiter.noteIfRateLimited(code, url, c.getHeaderField("Retry-After"));
                return false;
            }
            // Written to a temporary file first, so nothing reads a partial file
            final Path target = Paths.get(path);
            final Path tmp = Paths.get(path + ".tmp");
            Files.createDirectories(target.getParent());
            try (InputStream in = c.getInputStream()) {
                Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
            }
            if (Files.size(tmp) == 0) {
                Files.delete(tmp);
                return false;
            }
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
            return true;
        } finally {
            c.disconnect();
        }
    }

    /** Scryfall's pictures have a border, so they are saved under the .fullborder name that Forge looks for first. */
    private static String savedAt(final String url, final String destPath) {
        final boolean scryfall = url.startsWith(ForgeConstants.URL_PIC_SCRYFALL_DOWNLOAD) || url.startsWith(ForgeConstants.URL_SCRYFALL_CDN);
        final String path = url.contains(".fullborder.") || scryfall ? TextUtil.fastReplace(destPath, ".full.", ".fullborder.") : destPath;
        // An art crop keeps its name, because renaming it to .fullborder hides it from the sleeve cache
        if (!path.contains(".full") && !path.contains(".artcrop") && scryfall
                && !destPath.startsWith(ForgeConstants.CACHE_TOKEN_PICS_DIR) && !destPath.startsWith(ForgeConstants.CACHE_PLANECHASE_PICS_DIR)) {
            return path.replace(".jpg", ".fullborder.jpg");
        }
        return path;
    }
}
