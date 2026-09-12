package forge.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import forge.error.ExceptionHandler;
import forge.localinstance.properties.ForgeConstants;
import forge.localinstance.properties.ForgeProfileProperties;

public final class LogExporter {
    private LogExporter() { }

    /**
     * Bundle the forge and network debug logs into a timestamped zip in destDir.
     * @return the created zip file, or null if no log files were found
     */
    public static File exportLogs(File destDir) throws IOException {
        List<File> files = collectLogFiles();
        if (files.isEmpty()) {
            return null;
        }
        // The active log is locked for the process lifetime; on Windows it can't be read directly,
        // so substitute an in-process snapshot read through the owning channel.
        File snapshotDir = null;
        File activeLog = ExceptionHandler.getActiveLogFile();
        try {
            if (activeLog != null) {
                snapshotDir = Files.createTempDirectory("forge-logs").toFile();
                File snapshot = new File(snapshotDir, activeLog.getName());
                if (ExceptionHandler.snapshotActiveLog(snapshot)) {
                    String activeName = activeLog.getName();
                    files.replaceAll(f -> f.getName().equals(activeName) ? snapshot : f);
                }
            }
            String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
            File zipFile = new File(destDir, "forge-logs-" + stamp + ".zip");
            ZipUtil.zipFiles(files, zipFile);
            return zipFile;
        } finally {
            if (snapshotDir != null) {
                File[] contents = snapshotDir.listFiles();
                if (contents != null) {
                    for (File f : contents) {
                        f.delete();
                    }
                }
                snapshotDir.delete();
            }
        }
    }

    private static List<File> collectLogFiles() {
        List<File> files = new ArrayList<>();
        File userDir = new File(ForgeProfileProperties.getUserDir());
        if (userDir.isDirectory()) {
            File[] forgeLogs = userDir.listFiles((d, n) -> n.startsWith("forge") && n.endsWith(".log"));
            if (forgeLogs != null) {
                Collections.addAll(files, forgeLogs);
            }
        }
        File netDir = new File(ForgeConstants.NETWORK_LOGS_DIR);
        if (netDir.isDirectory()) {
            File[] netLogs = netDir.listFiles((d, n) -> n.startsWith("network-debug-") && n.endsWith(".log"));
            if (netLogs != null) {
                Collections.addAll(files, netLogs);
            }
        }
        return files;
    }
}
