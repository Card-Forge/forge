package forge.gui.card;

import static org.testng.Assert.assertTrue;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.testng.annotations.Test;

import forge.game.ability.ApiType;
import forge.game.ability.IHasForgeParams;
import forge.game.ability.SpellAbilityEffect;

/**
 * Keeps the {@link IHasForgeParams} declarations in sync with the params the engine reads, as scanned
 * from its bytecode ({@link EngineParams}):
 * <ul>
 *   <li>the framework classes ({@link CardScriptParams#FRAMEWORK}) together declare every ability param
 *       read outside effects and their AI, each param once;</li>
 *   <li>an effect that declares lists exactly what it and its AI read beyond that, and may mark some
 *       REQUIRED_PARAMS;</li>
 *   <li>INTERNAL_PARAMS are params the engine sets itself, which scripts shouldn't.</li>
 * </ul>
 * Effects that don't declare yet are left alone; run with -DsuggestParams=true to print the arrays for
 * them (and for the framework), ready to paste.
 */
public class CardScriptParamDeclarationTest {

    private static final String[] FIELDS = {"OPTIONAL_PARAMS", "REQUIRED_PARAMS", "INTERNAL_PARAMS"};
    private static final Pattern KEY = Pattern.compile("(?:^|\\|)\\s*([A-Za-z][A-Za-z0-9]*)\\$");

    private final EngineParams engine = EngineParams.get();

    @Test
    public void declarationsMatchTheCode() throws IOException {
        List<String> errors = new ArrayList<>();
        checkFramework(errors);
        checkEffects(engine.framework.keySet(), errors);
        checkInternal(errors);
        if (Boolean.getBoolean("suggestParams")) {
            printSuggestions(engine.framework.keySet());
        }
        assertTrue(errors.isEmpty(), "card-script param declarations are out of sync with the code:\n  "
            + String.join("\n  ", errors) + "\n(run with -DsuggestParams=true for the arrays to paste)");
    }

    /**
     * Every param a framework class reads for any ability is declared once, by a framework class that
     * reads it. Params other shared code reads (GameAction, CardFactory, ...) wait for that code's batch.
     */
    private void checkFramework(List<String> errors) {
        Map<String, String> declaredBy = new TreeMap<>();
        for (Class<?> c : CardScriptParams.FRAMEWORK) {
            checkFields(c, errors);
            String self = c.getName().replace('.', '/');
            for (String p : declared(c)) {
                String other = declaredBy.put(p, c.getSimpleName());
                if (other != null) {
                    errors.add(c.getSimpleName() + ": '" + p + "' is already declared by " + other);
                }
                Set<String> readers = engine.framework.get(p);
                if (readers == null) {
                    errors.add(c.getSimpleName() + ": declares '" + p + "', which only effects read"
                        + " (declare it in their OPTIONAL_PARAMS)");
                } else if (!readers.contains(self)) {
                    errors.add(c.getSimpleName() + ": declares '" + p + "' but doesn't read it; it's read in "
                        + simple(readers));
                }
            }
        }
        for (String p : engine.framework.keySet()) {
            String home = home(p);
            if (home != null && !declaredBy.containsKey(p)) {
                errors.add(simpleName(home) + ": reads '" + p + "' but doesn't declare it");
            }
        }
    }

    private void checkEffects(Set<String> framework, List<String> errors) {
        for (Map.Entry<Class<?>, List<ApiType>> e : effectClasses().entrySet()) {
            Class<?> effect = e.getKey();
            if (!declares(effect)) {
                continue;
            }
            Set<String> reads = new TreeSet<>();
            e.getValue().forEach(api -> reads.addAll(engine.apiReads(api)));
            Set<String> expected = new TreeSet<>(reads);
            expected.removeAll(framework);

            Set<String> own = new TreeSet<>();
            Set<String> required = new TreeSet<>();
            for (Class<?> c = effect; c != SpellAbilityEffect.class; c = c.getSuperclass()) {
                checkFields(c, errors);
                own.addAll(declared(c));
                for (String[] group : required(c)) {
                    if (group.length == 0) {
                        errors.add(c.getSimpleName() + ": REQUIRED_PARAMS has an empty group");
                    }
                    required.addAll(List.of(group));
                }
            }
            String name = effect.getSimpleName();
            for (String p : required) {
                if (!reads.contains(p) && !framework.contains(p)) {
                    errors.add(name + ": REQUIRED_PARAMS names '" + p + "', which it never reads");
                }
            }
            own.removeAll(required);
            int before = errors.size();
            for (String p : own) {
                if (framework.contains(p)) {
                    errors.add(name + ": re-declares framework param '" + p + "'");
                } else if (!expected.contains(p)) {
                    errors.add(name + ": declares '" + p + "', which neither it nor its AI reads");
                }
            }
            Set<String> missing = new TreeSet<>(expected);
            missing.removeAll(own);
            missing.removeAll(required);
            if (!missing.isEmpty()) {
                errors.add(name + ": reads " + missing + " without declaring them");
            }
            if (errors.size() > before) {
                errors.add(name + " wants\n" + array(expected, required));
            }
        }
    }

    /** INTERNAL_PARAMS, on a framework class or a declaring effect, must be params the engine really sets. */
    private void checkInternal(List<String> errors) {
        Set<Class<?>> declarers = new LinkedHashSet<>(CardScriptParams.FRAMEWORK);
        for (Class<?> effect : effectClasses().keySet()) {
            for (Class<?> c = effect; c != SpellAbilityEffect.class; c = c.getSuperclass()) {
                declarers.add(c);
            }
        }
        for (Class<?> c : declarers) {
            for (String p : internal(c)) {
                if (!engine.engineWritten.contains(p)) {
                    errors.add(c.getSimpleName() + ": INTERNAL_PARAMS names '" + p + "', which the engine never sets");
                }
            }
        }
    }

    /** Declaration fields: public static final (so IHasForgeParams can read them), no duplicates. */
    private static void checkFields(Class<?> c, List<String> errors) {
        boolean any = false;
        for (String name : FIELDS) {
            Field f;
            try {
                f = c.getDeclaredField(name);
            } catch (NoSuchFieldException e) {
                continue;
            }
            any = true;
            int mod = f.getModifiers();
            if (!Modifier.isPublic(mod) || !Modifier.isStatic(mod) || !Modifier.isFinal(mod)) {
                errors.add(c.getSimpleName() + "." + name + " must be public static final");
                return;
            }
        }
        if (any && !IHasForgeParams.class.isAssignableFrom(c)) {
            errors.add(c.getSimpleName() + " declares params but doesn't implement IHasForgeParams");
        }
        List<String> all = new ArrayList<>(List.of(optional(c)));
        for (String[] g : required(c)) {
            all.addAll(List.of(g));
        }
        all.addAll(List.of(internal(c)));
        Set<String> seen = new HashSet<>();
        for (String p : all) {
            if (!seen.add(p)) {
                errors.add(c.getSimpleName() + ": '" + p + "' is declared twice");
            }
            if (!p.matches("[A-Za-z][A-Za-z0-9]*")) {
                errors.add(c.getSimpleName() + ": '" + p + "' is not a param name");
            }
        }
    }

    private void printSuggestions(Set<String> framework) throws IOException {
        Map<String, Set<String>> byHome = new TreeMap<>();
        Map<String, Set<String>> later = new TreeMap<>();
        for (Map.Entry<String, Set<String>> e : engine.framework.entrySet()) {
            String home = home(e.getKey());
            if (home != null) {
                byHome.computeIfAbsent(home, k -> new TreeSet<>()).add(e.getKey());
            } else {
                e.getValue().forEach(r -> later.computeIfAbsent(r, k -> new TreeSet<>()).add(e.getKey()));
            }
        }
        System.out.println("=== framework OPTIONAL_PARAMS, by class");
        byHome.forEach((cls, params) -> System.out.println(cls + ":\n" + array(params, Set.of())));
        System.out.println("=== shared code that doesn't declare yet (a later batch), and what it reads");
        later.forEach((cls, params) -> System.out.println("  " + simpleName(cls) + ": " + params));
        System.out.println("=== undeclared effects");
        for (Map.Entry<Class<?>, List<ApiType>> e : effectClasses().entrySet()) {
            if (!declares(e.getKey())) {
                Set<String> expected = new TreeSet<>();
                e.getValue().forEach(api -> expected.addAll(engine.apiReads(api)));
                expected.removeAll(framework);
                System.out.println(e.getKey().getSimpleName() + ":\n" + array(expected, Set.of()));
            }
        }
        System.out.println("=== REQUIRED_PARAMS candidates (on 99.5%+ of an API's 30+ abilities; confirm in the code)");
        requiredCandidates().forEach(System.out::println);
        Set<String> internal = new TreeSet<>(engine.engineWritten);
        internal.retainAll(engine.framework.keySet());
        internal.removeAll(scriptKeys());
        System.out.println("=== INTERNAL_PARAMS candidates (set by the engine, read, used by no script): " + internal);
    }

    /** Params nearly every ability of an API sets in the corpus: likely required. */
    private static List<String> requiredCandidates() throws IOException {
        Pattern api = Pattern.compile("(?:AB|SP|DB)\\$\\s*([A-Za-z]+)");
        Map<String, Integer> uses = new TreeMap<>();
        Map<String, Map<String, Integer>> withKey = new TreeMap<>();
        for (Path p : CardScriptLinterTest.scripts(CardScriptLinterTest.locateRoot())) {
            for (String line : Files.readAllLines(p, StandardCharsets.UTF_8)) {
                Matcher a = api.matcher(line);
                if (!a.find()) {
                    continue;
                }
                uses.merge(a.group(1), 1, Integer::sum);
                Set<String> seen = new HashSet<>();
                Matcher m = KEY.matcher(line);
                while (m.find()) {
                    if (seen.add(m.group(1))) {
                        withKey.computeIfAbsent(a.group(1), k -> new TreeMap<>()).merge(m.group(1), 1, Integer::sum);
                    }
                }
            }
        }
        List<String> out = new ArrayList<>();
        uses.forEach((a, n) -> withKey.getOrDefault(a, Map.of()).forEach((k, c) -> {
            if (n >= 30 && c >= n * 0.995 && !k.matches("AB|SP|DB")) {
                out.add("  " + a + ": " + k + " (" + c + "/" + n + ")");
            }
        }));
        return out;
    }

    /** The framework class that should declare a param: the first one reading it, or null if none does. */
    private String home(String param) {
        Set<String> readers = engine.framework.getOrDefault(param, Set.of());
        for (Class<?> c : CardScriptParams.FRAMEWORK) {
            if (readers.contains(c.getName().replace('.', '/'))) {
                return c.getName().replace('.', '/');
            }
        }
        return null;
    }

    private static String simpleName(String cls) {
        return cls.substring(cls.lastIndexOf('/') + 1);
    }

    /** Effect classes by the APIs they serve. */
    private static Map<Class<?>, List<ApiType>> effectClasses() {
        Map<Class<?>, List<ApiType>> out = new LinkedHashMap<>();
        for (ApiType api : ApiType.values()) {
            out.computeIfAbsent(api.getSpellEffect().getClass(), k -> new ArrayList<>()).add(api);
        }
        return out;
    }

    /** True if the effect or one of its superclasses below SpellAbilityEffect declares. */
    private static boolean declares(Class<?> effect) {
        for (Class<?> c = effect; c != SpellAbilityEffect.class; c = c.getSuperclass()) {
            if (IHasForgeParams.optionalParams(c) != null || IHasForgeParams.requiredParams(c) != null
                    || IHasForgeParams.internalParams(c) != null) {
                return true;
            }
        }
        return false;
    }

    /** Everything a class declares itself: optional, required and internal. */
    private static Set<String> declared(Class<?> c) {
        Set<String> out = new TreeSet<>(List.of(optional(c)));
        for (String[] g : required(c)) {
            out.addAll(List.of(g));
        }
        out.addAll(List.of(internal(c)));
        return out;
    }

    private static String[] optional(Class<?> c) {
        String[] a = IHasForgeParams.optionalParams(c);
        return a != null ? a : new String[0];
    }

    private static String[][] required(Class<?> c) {
        String[][] a = IHasForgeParams.requiredParams(c);
        return a != null ? a : new String[0][];
    }

    private static String[] internal(Class<?> c) {
        String[] a = IHasForgeParams.internalParams(c);
        return a != null ? a : new String[0];
    }

    /** A paste-ready OPTIONAL_PARAMS array. */
    private static String array(Set<String> params, Set<String> except) {
        StringBuilder sb = new StringBuilder("    public static final String[] OPTIONAL_PARAMS = {\n");
        StringBuilder line = new StringBuilder("        ");
        for (String p : params) {
            if (except.contains(p)) {
                continue;
            }
            String item = "\"" + p + "\", ";
            if (line.length() + item.length() > 100) {
                sb.append(line.toString().stripTrailing()).append('\n');
                line = new StringBuilder("        ");
            }
            line.append(item);
        }
        return sb.append(line.toString().stripTrailing()).append("\n    };").toString();
    }

    private static List<String> simple(Set<String> classes) {
        List<String> out = new ArrayList<>();
        classes.forEach(c -> out.add(c.substring(c.lastIndexOf('/') + 1)));
        return out;
    }

    private static Set<String> scriptKeys() throws IOException {
        Set<String> keys = new HashSet<>();
        for (Path p : CardScriptLinterTest.scripts(CardScriptLinterTest.locateRoot())) {
            for (String line : Files.readAllLines(p, StandardCharsets.UTF_8)) {
                Matcher m = KEY.matcher(line);
                while (m.find()) {
                    keys.add(m.group(1));
                }
            }
        }
        return keys;
    }
}
