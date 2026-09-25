package forge.gui.card;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.testng.SkipException;
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
        assertTrue(!PARAMS.forApi(ApiType.Animate).contains("SacValid"), "AnimateAi reads SacValid on the ability on the stack");
        assertTrue(!PARAMS.forApi(ApiType.Dig).contains("Attacker"), "DigAi reads Attacker from its options map");
        assertTrue(PARAMS.forApi(ApiType.Untap).contains("Radiance"), "read on sa.getSATargetingCard(), often sa itself");
        assertTrue(PARAMS.forTrigger(TriggerType.AttackersDeclared).contains("AttackingPlayer"),
            "matchesValidParam(\"AttackingPlayer\", runParams.get(...))");
    }

    /** At runtime only declarations count: an undeclared API isn't checked, a declared one is. */
    @Test
    public void runtimeUsesDeclarations() {
        CardScriptParams declared = CardScriptParams.get();
        assertTrue(declared.isDeclared(ApiType.DealDamage));
        assertEquals(declared.requiredParams(ApiType.DealDamage), List.of(List.of("NumDmg")));
        CardScriptLinter runtime = new CardScriptLinter(declared);
        assertEquals(runtime.lint(HEAD + "A:SP$ DealDamage | ValidTgts$ Any").get(0).code(), "MISSING-KEY");
        // params only the AI reads are declared on the AI class
        assertEquals(runtime.lint(HEAD + "A:SP$ DealDamage | ValidTgts$ Any | NumDmg$ 1 | AIExpectAmount$ 1"), List.of());
    }

    @Test
    public void flagsScriptMistakes() {
        assertEquals(codes(HEAD + "A:SP$ DealDamge | ValidTgts$ Any | NumDmg$ 1"), List.of("API-UNKNOWN"));
        assertEquals(codes(HEAD + "A:SP$ DealDamage | ValidTgts$ Any | NumDmgg$ 1"), List.of("KEY-TYPO", "MISSING-KEY"));
        assertEquals(codes(HEAD + "A:SP$ DealDamage | ValidTgts$ Any | Numdmg$ 1"), List.of("KEY-CASE", "MISSING-KEY"));
        assertEquals(codes(HEAD + "A:SP$ Draw | Frobnicate$ 1"), List.of("UNKNOWN-KEY"));
        assertEquals(codes(HEAD + "A:SP$ Pump | Defined$ Self | CumulativeUpkeep$ True"), List.of("INTERNAL-KEY"));
        assertEquals(codes(HEAD + "A:SP$ LoseLife | Defined$ You | LifeAmount$ 1 | NumCards$ 1"), List.of("WRONG-KEY"));
        // a sub-ability key belongs to the effects that run it
        assertEquals(codes(HEAD + "A:SP$ Draw | WinSubAbility$ DBX\nSVar:DBX:DB$ Draw"), List.of("WRONG-KEY"));
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
        // a case fix is only suggested from the values of that param
        assertEquals(codes(HEAD + "A:SP$ Pump | Defined$ hand"), List.of());
        assertEquals(codes(HEAD + "A:SP$ Pump | Defined$ self"), List.of("CASE"));
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
     * The PR gate. CI passes a PR's {@code git diff -U0} as -Dcardscript.diff; this lints every card and token
     * script, fails on ERRORs on the lines the diff adds or changes, and writes every finding to
     * forge-gui-desktop/target/card-script-findings.json for the PR review workflow. Errors elsewhere are only
     * reported, so the corpus's backlog can't block unrelated work.
     */
    @Test
    public void lintCorpus() throws IOException {
        String diff = System.getProperty("cardscript.diff");
        if (diff == null || diff.isBlank()) {
            throw new SkipException("runs on PR builds; to check scripts, run checkCards with -Dcardscript.path");
        }
        Path root = locateRoot();
        Map<String, Set<Integer>> changed = changedLines(diff);

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
                if (f.severity() == Severity.ERROR && changed.getOrDefault(rel, Set.of()).contains(f.line())) {
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
        assertTrue(blocking.isEmpty(), "card-script errors:\n  " + String.join("\n  ", blocking));
    }

    /**
     * Checks the scripts in -Dcardscript.path, a folder or file relative to the repo or absolute: prints every
     * finding, and fails if any is an ERROR.
     */
    @Test
    public void checkCards() throws IOException {
        String path = System.getProperty("cardscript.path");
        if (path == null || path.isBlank()) {
            throw new SkipException("pass -Dcardscript.path=folder-or-file to check scripts");
        }
        Path target = locateRoot().resolve(path).normalize();
        assertTrue(Files.exists(target), target + " does not exist");
        List<Path> files;
        try (Stream<Path> walk = Files.walk(target)) {
            files = walk.filter(p -> p.toString().endsWith(".txt")).sorted().collect(Collectors.toList());
        }
        List<String> errors = new ArrayList<>();
        int warnings = 0;
        System.out.println("Findings in " + target + ":");
        for (Path p : files) {
            String name = (p.equals(target) ? p.getFileName() : target.relativize(p)).toString().replace('\\', '/');
            for (Finding f : linter.lint(Files.readString(p, StandardCharsets.UTF_8))) {
                // the arrow is for PR comments; many consoles can't print it
                String line = name + ":" + f.toString().replace("→", "->");
                System.out.println("  " + line);
                if (f.severity() == Severity.ERROR) {
                    errors.add(line);
                } else {
                    warnings++;
                }
            }
        }
        System.out.println(files.size() + " script(s), " + errors.size() + " error(s), " + warnings + " warning(s)");
        if (!errors.isEmpty()) {
            fail("card-script errors:\n  " + String.join("\n  ", errors));
        }
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
