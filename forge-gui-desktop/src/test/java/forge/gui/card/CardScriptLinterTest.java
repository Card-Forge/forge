package forge.gui.card;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.testng.annotations.Test;

import forge.game.ability.ApiType;
import forge.game.trigger.TriggerType;
import forge.gui.card.CardScriptLinter.Finding;
import forge.gui.card.CardScriptLinter.Severity;

public class CardScriptLinterTest {

    /** Declarations, with the params of everything not declared yet filled in from the bytecode scan. */
    private static final CardScriptParams PARAMS = new CardScriptParams(EngineParams.get().scanned());
    private final CardScriptLinter linter = new CardScriptLinter(PARAMS);

    private List<String> codes(String... lines) {
        return linter.lint(String.join("\n", lines)).stream().map(Finding::code).collect(Collectors.toList());
    }

    private static final String HEAD = "Name:Test\nManaCost:1 R\nTypes:Instant\n";

    @Test
    public void paramsComeFromTheEngine() {
        assertTrue(PARAMS.forApi(ApiType.Draw).contains("NumCards"));
        assertTrue(PARAMS.forApi(ApiType.DealDamage).contains("NumDmg"));
        assertTrue(PARAMS.forApi(ApiType.Draw).contains("SpellDescription"), "framework params apply to every API");
        assertTrue(!PARAMS.forApi(ApiType.LoseLife).contains("NumCards"), "NumCards belongs to Draw & co, not LoseLife");
        assertTrue(PARAMS.isInverted("InvertValidCard"), "Invert + a param matched with matchesValidParam");
        assertTrue(!PARAMS.isInverted("InvertFlying"));
        assertTrue(!PARAMS.isKnown("numcards"), "params are case-sensitive");
        assertEquals(PARAMS.correctCase("PreCostDesc"), "PrecostDesc");
        assertTrue(PARAMS.forTrigger(TriggerType.ChangesZone).contains("Destination"),
            "AbilityUtils reads Destination on a trigger, so it's a trigger param");
    }

    /** At runtime only declarations count: an undeclared API isn't checked, a declared one is. */
    @Test
    public void runtimeUsesDeclarations() {
        CardScriptParams declared = CardScriptParams.get();
        assertTrue(declared.isDeclared(ApiType.DealDamage));
        assertEquals(declared.requiredParams(ApiType.DealDamage), List.of(List.of("NumDmg")));
        CardScriptLinter runtime = new CardScriptLinter(declared);
        assertEquals(runtime.lint(HEAD + "A:SP$ DealDamage | ValidTgts$ Any").get(0).code(), "MISSING-KEY");
    }

    @Test
    public void flagsScriptMistakes() {
        assertEquals(codes(HEAD + "A:SP$ DealDamge | ValidTgts$ Any | NumDmg$ 1"), List.of("API-UNKNOWN"));
        assertEquals(codes(HEAD + "A:SP$ DealDamage | ValidTgts$ Any | NumDmgg$ 1"), List.of("KEY-TYPO", "MISSING-KEY"));
        assertEquals(codes(HEAD + "A:SP$ DealDamage | ValidTgts$ Any | Numdmg$ 1"), List.of("KEY-CASE", "MISSING-KEY"));
        assertEquals(codes(HEAD + "A:SP$ Draw | Frobnicate$ 1"), List.of("UNKNOWN-KEY"));
        assertEquals(codes(HEAD + "A:SP$ Pump | Defined$ Self | CumulativeUpkeep$ True"), List.of("INTERNAL-KEY"));
        assertEquals(codes(HEAD + "A:SP$ LoseLife | Defined$ You | LifeAmount$ 1 | NumCards$ 1"), List.of("WRONG-KEY"));
        assertEquals(codes(HEAD + "A:SP$ Animate | Defined$ Self | Keyword$ Flying"), List.of("KEY-TYPO"));
        assertEquals(codes(HEAD + "A:SP$ Draw | NumCards$ 1 | NumCards$ 2"), List.of("DUP-PARAM"));
        assertEquals(linter.lint(HEAD + "A:SP$ Draw | NumCards$ 1 | NumCards$ 1").get(0).severity(), Severity.WARN);
        assertEquals(codes(HEAD + "A:SP$ Draw | SubAbility$ DBNope"), List.of("REF-UNDEF"));
        assertEquals(codes(HEAD + "A:SP$ Draw\nSVar:DBUnused:DB$ Draw"), List.of("ORPHAN"));
        assertEquals(codes(HEAD + "A:SP$ Draw|NumCards$ 1"), List.of("LEX-PIPE"));
        assertEquals(codes("Name:Test\nManaCost:1 Q\nTypes:Instant"), List.of("MANA"));
        assertEquals(codes("Name:Test\nManaCost:WW\nTypes:Instant"), List.of("MANA"));
        assertEquals(codes("Name:Test\nTypes:Instant\nA:SP$ Draw"), List.of("NO-MANACOST"));
        assertEquals(codes(HEAD + "A:AB$ Draw | Cost$ Tapp"), List.of("COST"));
        assertEquals(codes(HEAD + "A:SP$ Draw\nOracel:Draw a card."), List.of("LEX-PREFIX"));
        assertEquals(codes(HEAD + "A:SP$ Draw\nOracle::Draw a card."), List.of("LEX-PREFIX"));
        assertEquals(codes(HEAD + "A:SP$ Charm | Choices$ DBA,DBB\nSVar:DBA:DB$ Draw"), List.of("REF-UNDEF"));
        // triggers, statics and replacements held in SVars are linted too
        assertEquals(codes(HEAD + "A:SP$ Effect | Triggers$ TrigX\nSVar:TrigX:Mode$ ChangesZonee | Execute$ DBNope"),
            List.of("API-UNKNOWN", "REF-UNDEF"));
        assertEquals(codes(HEAD + "A:SP$ Effect | StaticAbilities$ STX\nSVar:STX:Mode$ Continuous | Affected$ Creature | AddPowr$ 1"),
            List.of("KEY-TYPO"));
        // SVars belong to their face
        assertEquals(codes(HEAD + "SVar:DBX:DB$ Draw\nA:SP$ Draw | SubAbility$ DBX\nALTERNATE\nName:Back\nA:SP$ Draw | SubAbility$ DBX"),
            List.of("REF-UNDEF"));
    }

    @Test
    public void avoidsKnownFalsePositives() {
        // SVars defined on Variant: lines count
        assertEquals(codes(HEAD + "Variant:A:A:SP$ Draw | SubAbility$ DBX\nVariant:A:SVar:DBX:DB$ Draw"), List.of());
        // no "did you mean" unless the suggestion is a param this ability reads
        assertEquals(codes(HEAD + "A:SP$ Draw | Host$ True"), List.of("UNKNOWN-KEY"));
    }

    @Test
    public void messagesReadAsWrongToRight() {
        assertEquals(linter.lint(HEAD + "A:SP$ DealDamage | ValidTgts$ Any | NumDmg$ 1 | SubABility$ DBX").get(0).message(),
            "`SubABility$` → `SubAbility$` (params are case-sensitive)");
        assertEquals(linter.lint(HEAD + "A:SP$ DealDamage | ValidTgts$ Any").get(0).message(),
            "`DealDamage` needs `NumDmg$`");
    }

    @Test
    public void readsTheLinesADiffChanges() throws IOException {
        Path diff = Files.createTempFile("cardscript", ".diff");
        Files.writeString(diff, String.join("\n",
            "diff --git a/forge-gui/res/cardsfolder/a/a.txt b/forge-gui/res/cardsfolder/a/a.txt",
            "--- a/forge-gui/res/cardsfolder/a/a.txt",
            "+++ b/forge-gui/res/cardsfolder/a/a.txt\t",
            "@@ -3 +3,2 @@",
            "-old",
            "+new",
            "+new",
            "@@ -9,2 +10,0 @@",
            "-gone"));
        try {
            assertEquals(changedLines(diff.toString()), Map.of("forge-gui/res/cardsfolder/a/a.txt", Set.of(3, 4)));
        } finally {
            Files.delete(diff);
        }
    }

    @Test
    public void acceptsWellFormedScripts() {
        assertEquals(codes(
            "Name:Test",
            "ManaCost:1 R",
            "Types:Creature Goblin",
            "PT:1/1",
            "T:Mode$ ChangesZone | Origin$ Any | Destination$ Battlefield | ValidCard$ Card.Self | Execute$ TrigDraw | TriggerDescription$ When CARDNAME enters, draw a card.",
            "SVar:TrigDraw:DB$ Draw | NumCards$ 1 | SubAbility$ DBDamage",
            "SVar:DBDamage:DB$ DealDamage | ValidTgts$ Any | NumDmg$ 1",
            "A:AB$ Pump | Cost$ R T Sac<1/Creature> | Defined$ Self | NumAtt$ +1 | SpellDescription$ CARDNAME gets +1/+0.",
            "A:SP$ ImmediateTrigger | Execute$ TrigDraw | AfterReplacement$ True | TriggerDescription$ Draw."), List.of());
    }

    /**
     * Lints every card and token script, reports what was and wasn't checked, and writes every finding
     * to forge-gui-desktop/target/card-script-findings.json for the PR review workflow.
     *
     * Which ERRORs fail the build:
     * <ul>
     *   <li>-Dcardscript.diff=file (a {@code git diff -U0}): those on lines the diff adds or changes. CI
     *       passes a PR's diff, so a PR answers only for what it changed.</li>
     *   <li>-Dcardscript.gate: {@code none} (the default) on nothing else; {@code upcoming} also on
     *       cardsfolder/upcoming, where new scripts land; {@code all} on every ERROR.</li>
     * </ul>
     * Everything else is only reported, so the corpus's backlog can't block unrelated work.
     * -Dcardscript.path=folder-or-file prints every finding for the scripts there, in or outside the repo.
     * -Dcardscript.selfcheck=true also adds known mistakes to in-memory copies of real scripts and reports
     * how many the linter finds.
     */
    @Test
    public void lintCorpus() throws IOException {
        Path root = locateRoot();
        Map<String, Set<Integer>> changed = changedLines(System.getProperty("cardscript.diff"));
        String gate = System.getProperty("cardscript.gate", "none");

        Map<String, Integer> counts = new TreeMap<>(), lineKinds = new TreeMap<>();
        List<String> json = new ArrayList<>();
        List<String> blocking = new ArrayList<>();
        List<Path> scripts = scripts(root);
        for (Path p : scripts) {
            String rel = root.relativize(p).toString().replace('\\', '/');
            String text = Files.readString(p, StandardCharsets.UTF_8);
            text.lines().forEach(l -> lineKinds.merge(lineKind(l), 1, Integer::sum));
            for (Finding f : linter.lint(text)) {
                counts.merge(f.severity() + " " + f.code(), 1, Integer::sum);
                json.add("{\"path\":" + quote(rel) + ",\"line\":" + f.line() + ",\"severity\":" + quote(f.severity().name())
                    + ",\"code\":" + quote(f.code()) + ",\"body\":" + quote(f.message()) + "}");
                boolean gated = gate.equals("all") || gate.equals("upcoming") && rel.contains("/upcoming/")
                    || changed.getOrDefault(rel, Set.of()).contains(f.line());
                if (f.severity() == Severity.ERROR && gated) {
                    blocking.add(rel + ":" + f.toString().replace("→", "->"));
                }
            }
        }

        Path out = root.resolve("forge-gui-desktop/target/card-script-findings.json");
        Files.createDirectories(out.getParent());
        Files.writeString(out, "[" + String.join(",\n", json) + "]\n", StandardCharsets.UTF_8);

        System.out.println("Card-script lint: " + scripts.size() + " scripts");
        System.out.println("  lines checked:        " + pick(lineKinds, "A", "T", "S", "R", "SVar ability", "SVar trigger/static", "SVar replacement", "ManaCost", "K Chapter"));
        System.out.println("  lines not checked:    " + pick(lineKinds, "K", "SVar value", "other"));
        System.out.println("  not checked anywhere: param values other than SVar references, Cost$ and"
            + " Defined/Origin/Destination case; required params beyond the declared REQUIRED_PARAMS; Oracle text");
        System.out.println("  findings:             " + counts);
        String path = System.getProperty("cardscript.path");
        if (path != null && !path.isBlank()) {
            printFindings(root, path);
        }
        assertTrue(blocking.isEmpty(), "card-script errors:\n  " + String.join("\n  ", blocking));

        if (Boolean.getBoolean("cardscript.selfcheck")) {
            int[] typos = typoRecall(scripts);
            System.out.println("  typo self-check:      " + typos[0] + " of " + typos[1] + " misspelt params found");
            System.out.println("  wrong-API self-check: " + wrongApiRecall(scripts));
            assertTrue(typos[0] >= typos[1] * 0.99, "the linter misses more than 1% of misspelt params");
        }
    }

    /** Prints one line per finding for the scripts in a folder or file, relative to the repo or absolute. */
    private void printFindings(Path root, String path) throws IOException {
        Path target = root.resolve(path).normalize();
        if (!Files.exists(target)) {
            System.out.println("cardscript.path: " + target + " does not exist");
            return;
        }
        List<Path> files;
        try (Stream<Path> walk = Files.walk(target)) {
            files = walk.filter(p -> p.toString().endsWith(".txt")).sorted().collect(Collectors.toList());
        }
        int errors = 0, warnings = 0;
        System.out.println("Findings in " + target + ":");
        for (Path p : files) {
            String name = (p.equals(target) ? p.getFileName() : target.relativize(p)).toString().replace('\\', '/');
            for (Finding f : linter.lint(Files.readString(p, StandardCharsets.UTF_8))) {
                // the arrow is for PR comments; many consoles can't print it
                System.out.println("  " + name + ":" + f.toString().replace("→", "->"));
                if (f.severity() == Severity.ERROR) {
                    errors++;
                } else {
                    warnings++;
                }
            }
        }
        System.out.println(files.size() + " script(s), " + errors + " error(s), " + warnings + " warning(s)");
    }

    /** Swaps two letters of a param in copies of real scripts; counts how many the linter finds: {found, tried}. */
    private int[] typoRecall(List<Path> scripts) throws IOException {
        Pattern key = Pattern.compile("\\| ([A-Z][A-Za-z]{4,})\\$");
        int planted = 0, caught = 0;
        for (int i = 0; i < scripts.size() && planted < 2000; i += 17) {
            String text = Files.readString(scripts.get(i), StandardCharsets.UTF_8);
            Matcher m = key.matcher(text);
            if (!m.find()) {
                continue;
            }
            String k = m.group(1);
            String typo = k.substring(0, 2) + k.charAt(3) + k.charAt(2) + k.substring(4);
            if (typo.equals(k)) {
                continue;
            }
            planted++;
            String mutated = text.substring(0, m.start(1)) + typo + text.substring(m.end(1));
            if (linter.lint(mutated).stream().anyMatch(f -> f.severity() == Severity.ERROR && typo.equals(f.token()))) {
                caught++;
            }
        }
        return new int[] {caught, planted};
    }

    /**
     * Adds, to copies of real abilities, params that cards use with other APIs but never with this one,
     * and reports how many the linter flags. It accepts the rest because shared code reads them for any
     * ability.
     */
    private String wrongApiRecall(List<Path> scripts) throws IOException {
        Pattern ability = Pattern.compile("^(A:|SVar:[^:]+:)(SP|AB|DB)\\$ (\\w+)");
        Pattern key = Pattern.compile("\\| *([A-Za-z]+)\\$");
        Map<String, Set<String>> used = new TreeMap<>();
        Map<String, List<String>> sample = new TreeMap<>();
        for (Path p : scripts) {
            List<String> lines = Files.readAllLines(p, StandardCharsets.UTF_8);
            for (int i = 0; i < lines.size(); i++) {
                Matcher a = ability.matcher(lines.get(i));
                if (!a.find()) {
                    continue;
                }
                Set<String> keys = used.computeIfAbsent(a.group(3), k -> new HashSet<>());
                Matcher m = key.matcher(lines.get(i));
                while (m.find()) {
                    keys.add(m.group(1));
                }
                List<String> s = sample.computeIfAbsent(a.group(3), k -> new ArrayList<>());
                if (s.isEmpty()) {
                    s.add(String.join("\n", lines) + "\n@" + i);
                }
            }
        }
        Set<String> common = new TreeSet<>();
        used.values().forEach(common::addAll);
        int planted = 0, caught = 0;
        for (Map.Entry<String, List<String>> e : sample.entrySet()) {
            String[] parts = e.getValue().get(0).split("\n@");
            String[] lines = parts[0].split("\n", -1);
            int at = Integer.parseInt(parts[1]);
            List<String> candidates = new ArrayList<>();
            for (String k : common) {
                if (!used.get(e.getKey()).contains(k) && PARAMS.isKnown(k)) {
                    candidates.add(k);
                }
            }
            for (int j = 0; j < 10 && j < candidates.size(); j++) {
                String k = candidates.get(j * candidates.size() / 10); // spread over the alphabet
                planted++;
                String[] copy = lines.clone();
                copy[at] = copy[at] + " | " + k + "$ 1";
                if (linter.lint(String.join("\n", copy)).stream().anyMatch(f -> k.equals(f.token()) && f.line() == at + 1)) {
                    caught++;
                }
            }
        }
        return caught + " of " + planted + " params on the wrong API found";
    }

    private static String lineKind(String l) {
        if (l.startsWith("SVar:")) {
            String[] p = l.split(":", 3);
            String body = p.length == 3 ? p[2] : "";
            return body.matches("\\s*(AB|SP|DB|ST)\\$.*|.*\\| *(AB|SP|DB)\\$.*") ? "SVar ability"
                : body.contains("Event$") ? "SVar replacement"
                : body.startsWith("Mode$") ? "SVar trigger/static" : "SVar value";
        }
        if (l.startsWith("K:")) {
            return l.startsWith("K:Chapter:") ? "K Chapter" : "K";
        }
        int c = l.indexOf(':');
        String prefix = c > 0 ? l.substring(0, c) : "";
        return Set.of("A", "T", "S", "R", "ManaCost").contains(prefix) ? prefix : "other";
    }

    private static String pick(Map<String, Integer> m, String... keys) {
        List<String> out = new ArrayList<>();
        for (String k : keys) {
            out.add(k + "=" + m.getOrDefault(k, 0));
        }
        return String.join(", ", out);
    }

    /** The lines a {@code git diff -U0} file adds or changes, by repo-relative path. */
    static Map<String, Set<Integer>> changedLines(String diff) throws IOException {
        Map<String, Set<Integer>> out = new TreeMap<>();
        if (diff == null || diff.isBlank()) {
            return out;
        }
        Pattern hunk = Pattern.compile("^@@ -\\S+ \\+(\\d+)(?:,(\\d+))? @@");
        String path = null;
        for (String line : Files.readAllLines(Paths.get(diff), StandardCharsets.UTF_8)) {
            if (line.startsWith("+++ ")) {
                // git ends the path with a tab when it contains a space
                path = line.startsWith("+++ b/") ? line.substring(6).replaceAll("\t$", "") : null;
            } else if (path != null) {
                Matcher m = hunk.matcher(line);
                if (m.find()) {
                    int start = Integer.parseInt(m.group(1));
                    int count = m.group(2) == null ? 1 : Integer.parseInt(m.group(2));
                    for (int i = start; i < start + count; i++) {
                        out.computeIfAbsent(path, k -> new HashSet<>()).add(i);
                    }
                }
            }
        }
        return out;
    }

    private static String quote(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                default -> sb.append(c < 0x20 ? String.format("\\u%04x", (int) c) : String.valueOf(c));
            }
        }
        return sb.append('"').toString();
    }

    static List<Path> scripts(Path root) throws IOException {
        List<Path> out = new ArrayList<>();
        for (String dir : new String[] {"cardsfolder", "tokenscripts"}) {
            try (Stream<Path> walk = Files.walk(root.resolve("forge-gui/res").resolve(dir))) {
                walk.filter(p -> p.toString().endsWith(".txt")).sorted().forEach(out::add);
            }
        }
        return out;
    }

    static Path locateRoot() {
        for (Path d = Paths.get("").toAbsolutePath(); d != null; d = d.getParent()) {
            if (Files.isDirectory(d.resolve("forge-gui/res/cardsfolder"))) {
                return d;
            }
        }
        throw new IllegalStateException("repo root not found");
    }
}
