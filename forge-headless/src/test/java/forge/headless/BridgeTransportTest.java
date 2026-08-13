package forge.headless;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class BridgeTransportTest {
    @Test
    public void preservesOneJsonMessagePerLineAndLogsBothDirections() throws Exception {
        Path log = Files.createTempFile("forge-bridge-transport", ".jsonl");
        ByteArrayInputStream input = new ByteArrayInputStream(
                "{\"jsonrpc\":\"2.0\",\"id\":7,\"method\":\"decision\",\"params\":{}}\n"
                        .getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        try (BridgeTransport transport = new BridgeTransport(input,
                new PrintStream(output, true, StandardCharsets.UTF_8.name()), log)) {
            JsonNode request = transport.receive();
            assertEquals("decision", request.path("method").asText());
            ObjectNode response = BridgeTransport.JSON.createObjectNode();
            response.put("jsonrpc", "2.0");
            response.put("id", 7);
            response.putObject("result").put("type", "pass");
            transport.send(response);
        }

        List<String> lines = Files.readAllLines(log, StandardCharsets.UTF_8);
        assertEquals(2, lines.size());
        assertEquals("in", BridgeTransport.JSON.readTree(lines.get(0)).path("direction").asText());
        assertEquals("out", BridgeTransport.JSON.readTree(lines.get(1)).path("direction").asText());
        assertEquals(1, output.toString(StandardCharsets.UTF_8.name()).lines().count());
        Files.deleteIfExists(log);
    }
}
