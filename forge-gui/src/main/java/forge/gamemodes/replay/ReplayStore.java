package forge.gamemodes.replay;

import forge.localinstance.properties.ForgeConstants;
import forge.util.BuildInfo;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Where replay files live and how they start.
 *
 * <p>A replay file is a small header followed by frames. Each frame is a network message exactly as it would travel to a
 * remote spectator (the same bytes {@code CompatibleObjectEncoder} puts on the wire), with the time it was produced:
 * <pre>
 *   int    MAGIC
 *   int    FORMAT
 *   UTF    Forge version that wrote the file
 *   long   start time (epoch millis)
 *   UTF    title (the players)
 *   frames: { int millisSinceStart, int length, byte[length] encodedMessage } ...
 * </pre>
 * A file cut short (the app was closed mid-game) is still readable up to its last complete frame.
 */
public final class ReplayStore {
    static final int MAGIC = 0x46525031; // "FRP1"
    static final int FORMAT = 2;
    static final String EXTENSION = ".frp";

    /** Replays kept on disk; the oldest go first, so storage stays small on a phone. */
    private static final int KEEP = 20;

    private ReplayStore() {
    }

    public static File directory() {
        final File dir = new File(ForgeConstants.USER_REPLAYS_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    /** New, empty file for a game starting now (older replays beyond the limit are removed first). */
    static File newFile() {
        prune(KEEP - 1);
        final String stamp = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.ROOT).format(new Date());
        File f = new File(directory(), "replay-" + stamp + EXTENSION);
        for (int i = 2; f.exists(); i++) {
            f = new File(directory(), "replay-" + stamp + "-" + i + EXTENSION);
        }
        return f;
    }

    private static void prune(final int keep) {
        final File[] files = directory().listFiles((dir, name) -> name.endsWith(EXTENSION));
        if (files == null || files.length <= keep) {
            return;
        }
        Arrays.sort(files, Comparator.comparingLong(File::lastModified).reversed());
        for (int i = keep; i < files.length; i++) {
            files[i].delete();
        }
    }

    /** What the list of replays shows for one file. */
    public static final class Info {
        public final File file;
        public final String title;
        public final long startMillis;
        public final String forgeVersion;

        Info(final File file0, final String title0, final long startMillis0, final String forgeVersion0) {
            file = file0;
            title = title0;
            startMillis = startMillis0;
            forgeVersion = forgeVersion0;
        }

        @Override
        public String toString() {
            final String when = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ROOT).format(new Date(startMillis));
            final boolean sameBuild = forgeVersion.equals(BuildInfo.getVersionString());
            return when + "  " + title + (sameBuild ? "" : "  (Forge " + forgeVersion + ")");
        }
    }

    /** Newest first. Files that are not replays, or are unreadable, are left out. */
    public static List<Info> list() {
        final List<Info> out = new ArrayList<>();
        final File[] files = directory().listFiles((dir, name) -> name.endsWith(EXTENSION));
        if (files == null) {
            return out;
        }
        Arrays.sort(files, Comparator.comparingLong(File::lastModified).reversed());
        for (final File f : files) {
            final Info info = readInfo(f);
            if (info != null) {
                out.add(info);
            }
        }
        return out;
    }

    private static Info readInfo(final File f) {
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(f), 1024))) {
            if (in.readInt() != MAGIC || in.readInt() != FORMAT) {
                return null;
            }
            final String version = in.readUTF();
            final long start = in.readLong();
            final String title = in.readUTF();
            return new Info(f, title, start, version);
        } catch (final IOException e) {
            return null;
        }
    }
}
