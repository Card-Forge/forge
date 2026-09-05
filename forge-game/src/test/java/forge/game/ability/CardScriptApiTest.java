package forge.game.ability;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.testng.annotations.Test;

import forge.game.replacement.ReplacementType;
import forge.game.trigger.TriggerType;

/**
 * Advisory: validates each card script against the engine's own definitions and
 * writes findings to {@code target/card-script-findings.json} for the review
 * workflow to post. Two engine-sourced checks, neither hard-coded here:
 * <ul>
 *   <li>ability/trigger/replacement API names against {@link ApiType}/
 *       {@link TriggerType}/{@link ReplacementType} (the same {@code smartValueOf}
 *       the engine runs at card load);</li>
 *   <li>sub-ability params in {@link AbilityFactory#additionalAbilityKeys} must
 *       reference a defined SVar. {@code Execute} is left to the linter, which
 *       reports it as a hard load failure.</li>
 * </ul>
 * The test never fails, so it stays out of the build's pass/fail signal.
 */
public class CardScriptApiTest {

    private static final Pattern KEY = Pattern.compile("([A-Za-z][A-Za-z0-9]*)\\$(.*)");
    private static final Pattern SVAR_NAME = Pattern.compile("[A-Za-z0-9_]+");
    private static final Set<String> REF_KEYS = refKeys();

    private static Set<String> refKeys() {
        Set<String> keys = new HashSet<>(AbilityFactory.additionalAbilityKeys);
        keys.remove("Execute");   // the linter owns Execute (reports a hard load failure)
        return keys;
    }

    @Test
    public void writeApiFindings() throws IOException {
        Path root = locateRoot();
        Path corpus = root.resolve("forge-gui/res/cardsfolder");
        List<String> findings = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(corpus)) {
            paths.filter(p -> p.toString().endsWith(".txt")).sorted().forEach(p -> {
                String rel = root.relativize(p).toString().replace('\\', '/');
                try {
                    lintFile(p, rel, findings);
                } catch (IOException e) {
                    // unreadable card is the build's problem, not the linter's
                }
            });
        }
        Path out = Paths.get("target");
        Files.createDirectories(out);
        Files.write(out.resolve("card-script-findings.json"),
                toJson(findings).getBytes(StandardCharsets.UTF_8));
    }

    private static void lintFile(Path file, String rel, List<String> findings) throws IOException {
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        Set<String> svars = new HashSet<>();
        for (String line : lines) {
            if (line.startsWith("SVar:")) {
                String[] p = line.split(":", 3);
                if (p.length == 3) {
                    svars.add(p[1]);
                }
            }
        }
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            int ln = i + 1;
            String body = null;
            if (line.startsWith("A:")) {
                body = line.substring(2);
                checkAbility(body, rel, ln, findings);
            } else if (line.startsWith("T:")) {
                body = line.substring(2);
                checkNamed(body, "Mode", "trigger", rel, ln, findings);
            } else if (line.startsWith("R:")) {
                body = line.substring(2);
                checkNamed(body, "Event", "replacement", rel, ln, findings);
            } else if (line.startsWith("S:")) {
                body = line.substring(2);
            } else if (line.startsWith("SVar:")) {
                String[] p = line.split(":", 3);
                if (p.length == 3) {
                    body = p[2];
                    checkAbility(body, rel, ln, findings);
                }
            }
            if (body != null) {
                checkRefs(body, svars, rel, ln, findings);
            }
        }
    }

    private static void checkAbility(String body, String rel, int ln, List<String> findings) {
        Matcher m = KEY.matcher(firstSegment(body));
        if (!m.matches()) {
            return;
        }
        String key = m.group(1);
        if (!(key.equals("AB") || key.equals("SP") || key.equals("ST") || key.equals("DB"))) {
            return;
        }
        String api = m.group(2).trim();
        try {
            ApiType.smartValueOf(api);
        } catch (RuntimeException e) {
            findings.add(finding(rel, ln, "API-UNKNOWN", key + "$ '" + api + "' is not a known API"));
        }
    }

    private static void checkNamed(String body, String param, String kind, String rel, int ln,
            List<String> findings) {
        for (String seg : body.split("\\|")) {
            Matcher m = KEY.matcher(seg.trim());
            if (!m.matches() || !m.group(1).equals(param)) {
                continue;
            }
            String api = m.group(2).trim();
            try {
                if (param.equals("Mode")) {
                    TriggerType.smartValueOf(api);
                } else {
                    ReplacementType.smartValueOf(api);
                }
            } catch (RuntimeException e) {
                findings.add(finding(rel, ln, "API-UNKNOWN",
                        param + "$ '" + api + "' is not a known " + kind + " type"));
            }
            return;
        }
    }

    private static void checkRefs(String body, Set<String> svars, String rel, int ln,
            List<String> findings) {
        for (String seg : body.split("\\|")) {
            int d = seg.indexOf('$');
            if (d < 0) {
                continue;
            }
            String key = seg.substring(0, d).trim();
            String val = seg.substring(d + 1).trim();
            if (REF_KEYS.contains(key) && SVAR_NAME.matcher(val).matches() && !svars.contains(val)) {
                findings.add(finding(rel, ln, "REF-UNDEF", key + "$ '" + val + "' undefined SVar"));
            }
        }
    }

    private static String firstSegment(String body) {
        int bar = body.indexOf('|');
        return (bar < 0 ? body : body.substring(0, bar)).trim();
    }

    private static String finding(String path, int line, String code, String message) {
        return "{\"path\":\"" + path + "\",\"line\":" + line
                + ",\"code\":\"" + code + "\",\"body\":\"" + escape(message) + "\"}";
    }

    private static String toJson(List<String> findings) {
        return "[" + String.join(",", findings) + "]";
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static Path locateRoot() {
        Path dir = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        while (dir != null) {
            if (Files.isDirectory(dir.resolve("forge-gui/res/cardsfolder"))) {
                return dir;
            }
            dir = dir.getParent();
        }
        throw new IllegalStateException("repo root not found from " + System.getProperty("user.dir"));
    }
}
