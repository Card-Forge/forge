package forge.compat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Bytecode-rewrite target for BufferedReader.lines() (Java 8), missing from
 * MobiVM's Java-7-era runtime. Returns a streamsupport stream — call-site
 * descriptors match after the bridge pass remaps java.util.stream.
 */
public final class JIo8 {
    private JIo8() { }

    public static java8.util.stream.Stream<String> lines(BufferedReader reader) {
        // Faithful enough for Forge's uses (parse whole files); reads eagerly.
        List<String> lines = new ArrayList<>();
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return java8.util.stream.StreamSupport.stream(lines);
    }
}
