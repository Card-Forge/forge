package forge.gamemodes.net;

import forge.gui.GuiBase;
import org.tinylog.core.LogEntry;
import org.tinylog.core.LogEntryValue;
import org.tinylog.writers.AbstractFormatPatternWriter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Custom tinylog writer that routes NETWORK-tagged log entries to per-instance files
 * based on the {@code logfileKey} thread context value. Replaces logback's SiftingAppender.
 *
 * <p>Registered via {@code META-INF/services/org.tinylog.writers.Writer}.
 * Configuration name derived from class name: {@code network log}.
 */
public class NetworkLogWriter extends AbstractFormatPatternWriter {

    private static final String LOG_PREFIX = "network-debug-";
    private static final String DEFAULT_KEY = "default";

    private final ConcurrentHashMap<String, BufferedWriter> writers = new ConcurrentHashMap<>();

    public NetworkLogWriter(Map<String, String> properties) {
        super(properties);
    }

    @Override
    public Collection<LogEntryValue> getRequiredLogEntryValues() {
        Collection<LogEntryValue> values = EnumSet.copyOf(super.getRequiredLogEntryValues());
        values.add(LogEntryValue.CONTEXT);
        return values;
    }

    @Override
    public void write(LogEntry logEntry) throws IOException {
        String key = DEFAULT_KEY;
        Map<String, String> context = logEntry.getContext();
        if (context != null) {
            String contextKey = context.get("logfileKey");
            if (contextKey != null && !contextKey.isEmpty()) {
                key = contextKey;
            }
        }
        // Fallback: if thread has no logfileKey, use global key from NetworkLogConfig
        if (DEFAULT_KEY.equals(key)) {
            String globalKey = NetworkLogConfig.getGlobalLogfileKey();
            if (globalKey != null) {
                key = globalKey;
            }
        }

        String rendered = render(logEntry);
        BufferedWriter writer = writers.computeIfAbsent(key, this::createWriter);
        if (writer != null) {
            synchronized (writer) {
                writer.write(rendered);
                writer.newLine();
                writer.flush();
            }
        }
    }

    @Override
    public void flush() throws IOException {
        for (BufferedWriter writer : writers.values()) {
            synchronized (writer) {
                writer.flush();
            }
        }
    }

    @Override
    public void close() throws IOException {
        for (BufferedWriter writer : writers.values()) {
            synchronized (writer) {
                writer.close();
            }
        }
        writers.clear();
    }

    private BufferedWriter createWriter(String key) {
        try {
            File dir = NetworkLogConfig.getLogDirectory();
            if (!dir.exists()) {
                dir.mkdirs();
            }
            File file = new File(dir, LOG_PREFIX + key + ".log");
            boolean isNew = !file.exists() || file.length() == 0;
            BufferedWriter writer = new BufferedWriter(new FileWriter(file, true));
            if (isNew) {
                writeSystemInfoHeader(writer, key);
            }
            return writer;
        } catch (IOException e) {
            return null;
        }
    }

    private void writeSystemInfoHeader(BufferedWriter writer, String key) {
        try {
            long pid = ProcessHandle.current().pid();
            StringBuilder sb = new StringBuilder();
            sb.append("=".repeat(80)).append("\n");
            sb.append("Network Debug Log Started\n");
            sb.append("Log file key: ").append(key).append("\n");
            sb.append("PID: ").append(pid).append("\n");
            try {
                String hwInfo = GuiBase.getHWInfo()
                        .replace("##########################################", "");
                sb.append(hwInfo.strip()).append("\n");
            } catch (Exception e) {
                sb.append("(hardware info unavailable)\n");
            }
            sb.append("=".repeat(80)).append("\n");
            writer.write(sb.toString());
            writer.flush();
        } catch (IOException e) {
            // Non-critical
        }
    }
}
