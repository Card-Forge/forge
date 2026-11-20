package forge.web.images;

import forge.gui.FThreads;
import forge.gui.download.CdnUuidCache;
import forge.gui.download.ScryfallBulkDataSync;

import java.io.File;
import java.util.function.Consumer;

/**
 * Downloads Scryfall's card data file. It holds the direct address of every card picture on Scryfall's image servers. With it Forge
 * fetches from those servers, which have no rate limit, instead of from Scryfall's API, which has one.
 */
public final class ImageIndex {
    /** percent is how far a download has got. It is -1 when no download is running or its size is not yet known. */
    public record State(boolean downloading, int percent, boolean failed) { }

    private final Consumer<State> listener;
    private volatile boolean cancelled;
    private volatile boolean downloading;

    /** The listener is told of every change. The start is reported on the thread that calls download, the rest on the download's thread. */
    public ImageIndex(final Consumer<State> listener) {
        this.listener = listener;
    }

    /** When the data was last downloaded, read from its newest set file. 0 if it was never downloaded. */
    public static long updated() {
        // Only the set files count. The folder also holds a marker that records the answer to the download offer
        final File[] files = new File(CdnUuidCache.cacheDir()).listFiles((dir, name) -> name.endsWith(".json.gz"));
        long newest = 0;
        for (final File f : files == null ? new File[0] : files) {
            newest = Math.max(newest, f.lastModified());
        }
        return newest;
    }

    public void download() {
        if (downloading) {
            return;
        }
        downloading = true;
        cancelled = false;
        listener.accept(new State(true, -1, false));
        FThreads.invokeInBackgroundThread(() -> {
            final int sets = ScryfallBulkDataSync.sync(ScryfallBulkDataSync.BULK_TYPE_DEFAULT_CARDS, null,
                    (message, fraction) -> listener.accept(new State(true, fraction < 0 ? -1 : (int) (fraction * 100), false)),
                    () -> cancelled);
            downloading = false;
            listener.accept(new State(false, -1, sets < 0 && !cancelled));
        });
    }

    public void cancel() {
        cancelled = true;
    }
}
