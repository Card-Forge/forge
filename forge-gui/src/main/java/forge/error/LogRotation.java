/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge.error;

import forge.localinstance.properties.ForgeConstants;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;

/**
 * Rotation and pruning for {@code forge.log}.
 *
 * <p>{@link ExceptionHandler#registerErrorHandling()} renames the previous
 * {@code forge.log} to {@code forge.<timestamp>.log} at startup so its content
 * survives the restart (important on Android, where users must restart the app
 * to export logs after a crash).
 *
 * <p>Pruning the rotated backups is preference-driven and runs separately, after
 * preferences load — see {@link #pruneForgeLogs(int)}.
 *
 * <p>Network logs are pruned separately by
 * {@link forge.gamemodes.net.NetworkLogConfig#cleanupOldLogs} — the policies
 * differ (grace period, current-batch protection) so they don't share a helper.
 */
public final class LogRotation {

    private static final String ROTATED_PREFIX = "forge.";
    private static final String ROTATED_SUFFIX = ".log";
    private static final String TIMESTAMP_FORMAT = "yyyyMMdd-HHmmss";

    private LogRotation() {
    }

    /**
     * Build the rotated-backup file for {@code forge.log} at startup.
     * Callers should rename the existing {@code forge.log} to this path before
     * opening a fresh one.
     *
     * @param previousLog the existing {@code forge.log} (must exist)
     * @return target file in the same directory, named
     *         {@code forge.<lastModifiedTimestamp>.log}
     */
    public static File buildRotatedTarget(File previousLog) {
        String stamp = new SimpleDateFormat(TIMESTAMP_FORMAT).format(new Date(previousLog.lastModified()));
        return new File(previousLog.getParentFile(), ROTATED_PREFIX + stamp + ROTATED_SUFFIX);
    }

    /**
     * Delete oldest {@code forge.<timestamp>.log} files in the user dir until
     * at most {@code maxFiles} remain. The active {@code forge.log} is never a
     * candidate (its name has no second dot, so it doesn't match the rotated
     * pattern).
     *
     * <p>Best-effort: any I/O failures are swallowed so log cleanup never
     * affects gameplay.
     */
    public static void pruneForgeLogs(int maxFiles) {
        if (maxFiles <= 0) {
            return;
        }
        try {
            File logFile = new File(ForgeConstants.LOG_FILE);
            File dir = logFile.getParentFile();
            if (dir == null || !dir.isDirectory()) {
                return;
            }
            File[] rotated = dir.listFiles(f ->
                    f.isFile()
                    && f.getName().startsWith(ROTATED_PREFIX)
                    && f.getName().endsWith(ROTATED_SUFFIX)
                    && !f.getName().equals(logFile.getName()));
            if (rotated == null || rotated.length <= maxFiles) {
                return;
            }
            Arrays.sort(rotated, Comparator.comparingLong(File::lastModified));
            int toDelete = rotated.length - maxFiles;
            for (int i = 0; i < toDelete; i++) {
                rotated[i].delete();
            }
        } catch (Exception ignored) {
            // non-critical — never fail the app over log cleanup
        }
    }
}
