package forge.game.ability;

import static org.testng.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.testng.annotations.Test;

/**
 * Guards the card-script parameter declarations against drift. A framework or effect class
 * that reads card-script parameters declares them in OPTIONAL_PARAMS; effects may also group
 * mutually-required params in REQUIRED_PARAMS. This test fails the build if such a class reads
 * a parameter it does not declare, or declares one that appears nowhere in its source.
 *
 * Reads are detected from literal string arguments -- getParam("X"), the defined-resolution
 * helpers, map lookups, and so on. A parameter read through a non-literal argument cannot be
 * detected, so a passing run means "no undeclared literal read", not a proof of completeness.
 *
 * The check is opt-in: a class that declares neither field is skipped, so declarations can be
 * added a few classes at a time. Once a class declares, removing it from the check requires
 * deleting its whole declaration.
 */
public class CardScriptParamDeclarationTest {

    // The literal forms a card-script parameter read takes in source
    private static final Pattern GETPARAM = Pattern.compile(
        "(?<![A-Za-z0-9_])(?:getParam|hasParam|getParamOrDefault|getParamOrDefaultBoolean)\\(\\s*\"([^\"]+)\"");
    private static final Pattern DEFINED = Pattern.compile(
        "(?:getDefinedPlayersOrTargeted|getDefinedCardsOrTargeted|getTargetCards|getTargetPlayers"
        + "|getTargetEntities|getTargetSpells|getCards|getPlayers)\\([^;]*?\"([A-Z]\\w+)\"");
    private static final Pattern MAPGET = Pattern.compile(
        "\\.(?:get|containsKey|getOrDefault)\\(\\s*\"([A-Z][A-Za-z0-9]+)\"");
    private static final Pattern KEYVAR = Pattern.compile(
        "\\b\\w*[Kk]ey\\w*\\s*=\\s*\"([A-Za-z][A-Za-z0-9]+)\"");
    private static final Pattern ADD_TO_COMBAT = Pattern.compile("\\baddToCombat\\(([^)]*)\\)");
    private static final Pattern STRING_LITERAL = Pattern.compile("\"([A-Za-z][A-Za-z0-9]*)\"");

    // The ability declarator prefixes are not params
    private static final Set<String> MARKERS = Set.of("AB", "SP", "ST", "DB");

    // Module source roots walked to find declaring classes -- no per-file path list to maintain.
    private static final String[] SOURCE_ROOTS = {
        "forge-game/src/main/java",
        "forge-ai/src/main/java",
    };

    // Effects own their own params; everything else that declares forms the shared base layer.
    private static final String EFFECTS_DIR = "/ability/effects/";

    // Assignment of an array initializer to a field -- tolerates an explicit "= new String[]{".
    // Shared by discovery and parsing so the two can't drift apart.
    private static final String ASSIGN = "\\s*=\\s*[^;]*?\\{";

    // A class declares params iff one of these fields is assigned an array initializer.
    private static final Pattern DECLARES = Pattern.compile("(?:OPTIONAL_PARAMS|REQUIRED_PARAMS)" + ASSIGN);

    // Cross-cutting readers (they read params owned by effects/triggers, so reads != owned):
    // declared but NOT reads-gated. They still contribute their declared params to the base
    // and are subject to the structural checks.
    private static final Set<String> READ_GATE_EXEMPT = Set.of("SpellAbilityAi");

    @Test
    public void declarationsDoNotDrift() throws IOException {
        Path root = locateRoot();
        List<String> errors = new ArrayList<>();

        // Discover every declaring class by walking the source tree.
        List<Path> declarers = new ArrayList<>();
        for (String rel : SOURCE_ROOTS) {
            Path src = root.resolve(rel);
            if (!Files.isDirectory(src)) {
                continue;
            }
            try (Stream<Path> walk = Files.walk(src)) {
                for (Path p : (Iterable<Path>) walk.filter(f -> f.toString().endsWith(".java"))::iterator) {
                    if (declares(read(p))) {
                        declarers.add(p);
                    }
                }
            }
        }

        // Guard against a broken walk passing vacuously: the framework classes always declare.
        assertTrue(!declarers.isEmpty(), "no param-declaring classes found -- source-tree discovery is broken");

        // Base = declarers that aren't effects; their optional params are inherited by every effect.
        Set<String> base = new TreeSet<>();
        for (Path f : declarers) {
            if (!isEffect(f)) {
                base.addAll(declared(read(f), "OPTIONAL_PARAMS"));
            }
        }

        for (Path f : declarers) {
            checkClass(f, base, isEffect(f), errors);
        }

        assertTrue(errors.isEmpty(),
            "Card-script param declarations are out of sync with the code:\n  " + String.join("\n  ", errors));
    }

    private void checkClass(Path file, Set<String> base, boolean isEffect, List<String> errors) throws IOException {
        String src = read(file);
        String name = file.getFileName().toString().replace(".java", "");
        Set<String> optional = declared(src, "OPTIONAL_PARAMS");
        List<Set<String>> requiredGroups = requiredGroups(src);
        Set<String> requiredFlat = new TreeSet<>();
        requiredGroups.forEach(requiredFlat::addAll);
        Set<String> own = new TreeSet<>(optional);
        own.addAll(requiredFlat);

        // (1) every param the class reads must be declared (own) or inherited (base)
        if (!READ_GATE_EXEMPT.contains(name)) {
            Set<String> allowed = new TreeSet<>(own);
            allowed.addAll(base);
            for (String p : reads(src)) {
                if (!allowed.contains(p)) {
                    errors.add(name + ": reads '" + p + "$' but it is not declared (add to OPTIONAL_PARAMS)");
                }
            }
        }
        // (2) every declared param must appear as a literal somewhere in the source (no typos)
        Set<String> literals = literals(src);
        for (String p : own) {
            if (!literals.contains(p)) {
                errors.add(name + ": declares '" + p + "$' but it appears nowhere in the source (typo?)");
            }
        }
        // (3) effects must not re-declare base params
        if (isEffect) {
            for (String p : own) {
                if (base.contains(p)) {
                    errors.add(name + ": re-declares base param '" + p + "$' (it is already inherited)");
                }
            }
        }
        // (4) required and optional are disjoint
        for (String p : requiredFlat) {
            if (optional.contains(p)) {
                errors.add(name + ": '" + p + "$' is in both REQUIRED_PARAMS and OPTIONAL_PARAMS");
            }
        }
        // (5) no duplicate optional entries
        List<String> rawOpt = declaredList(src, "OPTIONAL_PARAMS");
        if (rawOpt.size() != new LinkedHashSet<>(rawOpt).size()) {
            errors.add(name + ": OPTIONAL_PARAMS has duplicate entries");
        }
        // (6) required groups are non-empty
        for (Set<String> g : requiredGroups) {
            if (g.isEmpty()) {
                errors.add(name + ": REQUIRED_PARAMS has an empty one-of group");
            }
        }
        // (7) every declared entry has a valid param-token shape
        for (String p : own) {
            if (!p.matches("[A-Za-z][A-Za-z0-9]*")) {
                errors.add(name + ": '" + p + "' is not a valid param token");
            }
        }
    }

    private boolean declares(String src) {
        return DECLARES.matcher(src).find();
    }

    private boolean isEffect(Path file) {
        return file.toString().replace('\\', '/').contains(EFFECTS_DIR);
    }

    private Set<String> declared(String src, String field) {
        return new TreeSet<>(declaredList(src, field));
    }

    private List<String> declaredList(String src, String field) {
        List<String> out = new ArrayList<>();
        Matcher decl = Pattern.compile(field + ASSIGN).matcher(src);
        if (!decl.find()) {
            return out;
        }
        int open = decl.end() - 1;
        int close = src.indexOf("};", open);
        if (close < 0) {
            return out;
        }
        Matcher m = STRING_LITERAL.matcher(src.substring(open, close));
        while (m.find()) {
            out.add(m.group(1));
        }
        return out;
    }

    /** REQUIRED_PARAMS is String[][]; return one set per inner { } group. */
    private List<Set<String>> requiredGroups(String src) {
        List<Set<String>> groups = new ArrayList<>();
        Matcher decl = Pattern.compile("REQUIRED_PARAMS" + ASSIGN).matcher(src);
        if (!decl.find()) {
            return groups;
        }
        int open = decl.end() - 1;
        int close = src.indexOf("};", open);
        if (close < 0) {
            return groups;
        }
        Matcher inner = Pattern.compile("\\{([^{}]*)\\}").matcher(src.substring(open + 1, close));
        while (inner.find()) {
            Set<String> g = new TreeSet<>();
            Matcher m = STRING_LITERAL.matcher(inner.group(1));
            while (m.find()) {
                g.add(m.group(1));
            }
            groups.add(g);
        }
        return groups;
    }

    private Set<String> reads(String src) {
        Set<String> out = new TreeSet<>();
        collect(GETPARAM, src, out);
        collect(DEFINED, src, out);
        collect(MAPGET, src, out);
        collect(KEYVAR, src, out);
        Matcher c = ADD_TO_COMBAT.matcher(src);
        while (c.find()) {
            Matcher m = STRING_LITERAL.matcher(c.group(1));
            while (m.find()) {
                out.add(m.group(1));
            }
        }
        out.removeIf(p -> MARKERS.contains(p) || !p.matches("[A-Za-z][A-Za-z0-9]*"));
        return out;
    }

    private Set<String> literals(String src) {
        Set<String> out = new TreeSet<>();
        collect(STRING_LITERAL, src, out);
        return out;
    }

    private void collect(Pattern p, String src, Set<String> out) {
        Matcher m = p.matcher(src);
        while (m.find()) {
            out.add(m.group(1));
        }
    }

    private static Path locateRoot() {
        Path d = Paths.get("").toAbsolutePath();
        for (int i = 0; i < 8 && d != null; i++) {
            if (Files.isDirectory(d.resolve("forge-game/src/main/java"))) {
                return d;
            }
            d = d.getParent();
        }
        throw new IllegalStateException("could not locate repo root from " + Paths.get("").toAbsolutePath());
    }

    private static String read(Path p) throws IOException {
        return new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
    }
}
