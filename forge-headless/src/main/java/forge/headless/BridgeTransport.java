package forge.headless;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/** Newline-delimited JSON transport with a deterministic full-duplex JSONL audit log. */
final class BridgeTransport implements Closeable {
    static final ObjectMapper JSON = new ObjectMapper();

    private final BufferedReader input;
    private final PrintStream output;
    private final BufferedWriter messageLog;

    BridgeTransport(InputStream input, PrintStream output, Path logPath) throws IOException {
        this.input = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
        this.output = output;
        Path absoluteLogPath = logPath.toAbsolutePath();
        Path parent = absoluteLogPath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        this.messageLog = Files.newBufferedWriter(absoluteLogPath, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    JsonNode receive() throws IOException {
        String line = input.readLine();
        if (line == null) {
            return null;
        }
        JsonNode message = JSON.readTree(line);
        appendLog("in", message);
        return message;
    }

    void send(JsonNode message) throws IOException {
        String line = JSON.writeValueAsString(message);
        appendLog("out", message);
        output.println(line);
        output.flush();
        if (output.checkError()) {
            throw new IOException("Failed to write JSON-RPC message to stdout");
        }
    }

    private void appendLog(String direction, JsonNode message) throws IOException {
        ObjectNode entry = JSON.createObjectNode();
        entry.put("direction", direction);
        entry.set("message", message);
        messageLog.write(JSON.writeValueAsString(entry));
        messageLog.newLine();
        messageLog.flush();
    }

    @Override
    public void close() throws IOException {
        messageLog.close();
    }
}
