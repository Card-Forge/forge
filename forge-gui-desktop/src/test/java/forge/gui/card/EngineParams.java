package forge.gui.card;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.google.common.reflect.ClassPath;

import forge.ai.SpellAbilityAi;
import forge.ai.SpellApiToAi;
import forge.game.ability.AbilityFactory;
import forge.game.ability.ApiType;
import forge.game.ability.SpellAbilityEffect;
import forge.game.replacement.ReplacementEffect;
import forge.game.replacement.ReplacementType;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerType;
import forge.gui.card.ParamReadScanner.Read;

/**
 * The params the engine reads, scanned from its bytecode and sorted into layers:
 * <ul>
 *   <li>handlers (an effect or its AI, a trigger, a replacement) own the reads made in their classes;</li>
 *   <li>framework: ability params read anywhere else, which the framework classes declare;</li>
 *   <li>trigger, replacement and static layers: their params read outside a handler.</li>
 * </ul>
 */
final class EngineParams {

    private static final String[] PACKAGES = {"forge.game.", "forge.ai.", "forge.player."};
    private static EngineParams instance;

    /** Ability params read outside effects and their AI, with the classes reading each. */
    final Map<String, Set<String>> framework = new TreeMap<>();
    /** Params of any trait read outside handlers (a CardTraitBase could be anything). */
    final Set<String> general = new TreeSet<>();
    final Set<String> trigger = new TreeSet<>(), replacement = new TreeSet<>(), statics = new TreeSet<>();
    final Set<String> valid = new TreeSet<>(), svars = new TreeSet<>(), engineWritten = new TreeSet<>();
    /** Reads made in each handler class (nested classes included), by outer class name. */
    private final Map<String, Set<String>> handlers = new HashMap<>();

    static synchronized EngineParams get() {
        if (instance == null) {
            instance = new EngineParams();
        }
        return instance;
    }

    private EngineParams() {
        ClassLoader loader = EngineParams.class.getClassLoader();
        ParamReadScanner scanner = new ParamReadScanner();
        try {
            for (ClassPath.ClassInfo info : ClassPath.from(loader).getAllClasses()) {
                if (Arrays.stream(PACKAGES).noneMatch(info.getName()::startsWith)
                        || info.url().toString().contains("/test-classes/")) {
                    continue; // test code reads and writes params too, but isn't the engine
                }
                try (InputStream in = info.asByteSource().openStream()) {
                    scanner.add(in);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        List<Read> reads = scanner.result();
        Map<String, Set<String>> origins = origins(scanner.callGraph());
        Set<String> declaring = new TreeSet<>();
        CardScriptParams.FRAMEWORK.forEach(c -> declaring.add(c.getName().replace('.', '/')));
        // AI helpers read effect params of other abilities too (e.g. the stack); only AI-only hints count
        Set<String> effectOwn = new TreeSet<>();
        for (Read r : reads) {
            String cls = outer(r.cls());
            if (r.kind() == ParamReadScanner.Kind.READ && handlerBase(cls) == SpellAbilityEffect.class && !isAi(cls)) {
                effectOwn.add(r.name());
            }
        }

        for (Read r : reads) {
            String cls = outer(r.cls());
            switch (r.kind()) {
                case WRITE -> engineWritten.add(r.name());
                case SVAR -> svars.add(r.name());
                case VALID -> valid.add(r.name());
                default -> { }
            }
            if (r.kind() != ParamReadScanner.Kind.READ && r.kind() != ParamReadScanner.Kind.VALID) {
                continue;
            }
            if (r.context() == ParamReadScanner.Context.ABILITY && isAi(cls) && handlerBase(cls) == null
                    && !declaring.contains(cls) && effectOwn.contains(r.name())) {
                continue;
            }
            Class<?> base = handlerBase(cls);
            boolean ownContext = switch (r.context()) {
                case ABILITY -> base == SpellAbilityEffect.class;
                case TRIGGER -> base == Trigger.class;
                case REPLACEMENT -> base == ReplacementEffect.class;
                case STATIC -> false;
                case ANY -> base != null;
            };
            if (ownContext) {
                handlers.computeIfAbsent(cls, c -> new TreeSet<>()).add(r.name());
                continue;
            }
            Set<String> layer = switch (r.context()) {
                case ABILITY -> null;
                case TRIGGER -> trigger;
                case REPLACEMENT -> replacement;
                case STATIC -> statics;
                case ANY -> cls.startsWith("forge/game/trigger/") ? trigger
                    : cls.startsWith("forge/game/replacement/") ? replacement
                    : cls.startsWith("forge/game/staticability/") || cls.startsWith("forge/game/StaticEffect") ? statics : null;
            };
            if (layer != null) {
                layer.add(r.name());
                continue;
            }
            // a helper only effects reach reads their params; one shared code reaches, every ability's
            Set<String> from = origins.get(r.method());
            if (from != null && from.stream().allMatch(h -> handlerBase(h) == SpellAbilityEffect.class)) {
                from.forEach(h -> handlers.computeIfAbsent(h, c -> new TreeSet<>()).add(r.name()));
            } else {
                framework.computeIfAbsent(r.name(), k -> new TreeSet<>()).add(cls);
                if (r.context() == ParamReadScanner.Context.ANY) {
                    general.add(r.name());
                }
            }
        }
        // AbilityFactory reads its sub-ability keys by looping over this list, which the scan can't follow
        for (String key : AbilityFactory.additionalAbilityKeys) {
            framework.computeIfAbsent(key, k -> new TreeSet<>()).add("forge/game/ability/AbilityFactory");
        }
    }

    private static final String GENERIC = "*";

    /**
     * Which handler classes each method runs on behalf of, following the call graph from its roots: a
     * handler's own methods run for that handler, a method nothing calls (an entry point, or one only
     * reached through an interface) for everything (GENERIC), and any other method for its callers.
     */
    private Map<String, Set<String>> origins(Map<String, Set<String>> callees) {
        Map<String, Set<String>> callers = new HashMap<>();
        callees.forEach((m, ts) -> ts.forEach(t -> callers.computeIfAbsent(t, k -> new HashSet<>()).add(m)));
        Map<String, Set<String>> origins = new HashMap<>();
        Deque<String> work = new ArrayDeque<>();
        for (String m : callees.keySet()) {
            String cls = outer(m.substring(0, m.lastIndexOf('.', m.indexOf('('))));
            if (handlerBase(cls) != null) {
                origins.put(m, Set.of(cls));
                work.add(m);
            } else if (!callers.containsKey(m)) {
                origins.put(m, Set.of(GENERIC));
                work.add(m);
            }
        }
        while (!work.isEmpty()) {
            String m = work.poll();
            Set<String> from = origins.get(m);
            for (String t : callees.getOrDefault(m, Set.of())) {
                String cls = outer(t.substring(0, t.lastIndexOf('.', t.indexOf('('))));
                if (handlerBase(cls) != null) {
                    continue;
                }
                Set<String> to = origins.computeIfAbsent(t, k -> new HashSet<>());
                if (to.contains(GENERIC)) {
                    continue;
                }
                boolean changed;
                if (from.contains(GENERIC)) {
                    to.clear();
                    changed = to.add(GENERIC);
                } else {
                    changed = to.addAll(from);
                }
                if (changed) {
                    work.add(t);
                }
            }
        }
        return origins;
    }

    /** Everything an ability of this API reads beyond the framework: its effect and AI classes. */
    Set<String> apiReads(ApiType api) {
        Set<String> out = hierarchyReads(api.getSpellEffect().getClass(), SpellAbilityEffect.class);
        Class<?> ai = SpellApiToAi.Converter.getAiClass(api);
        if (ai != null) {
            out.addAll(hierarchyReads(ai, SpellAbilityAi.class));
        }
        return out;
    }

    /** What CardScriptParams needs to check every line, declared or not. */
    CardScriptParams.Scanned scanned() {
        Map<ApiType, Set<String>> apis = new EnumMap<>(ApiType.class);
        for (ApiType api : ApiType.values()) {
            apis.put(api, apiReads(api));
        }
        Map<TriggerType, Set<String>> triggers = new EnumMap<>(TriggerType.class);
        for (TriggerType mode : TriggerType.values()) {
            triggers.put(mode, hierarchyReads(mode.getTriggerClass(), Trigger.class));
        }
        Map<ReplacementType, Set<String>> replacements = new EnumMap<>(ReplacementType.class);
        for (ReplacementType event : ReplacementType.values()) {
            replacements.put(event, hierarchyReads(event.getReplacementClass(), ReplacementEffect.class));
        }
        return new CardScriptParams.Scanned(framework.keySet(), apis, triggers, replacements, general, trigger,
            replacement, statics, valid, svars);
    }

    private Set<String> hierarchyReads(Class<?> cls, Class<?> stop) {
        Set<String> out = new TreeSet<>();
        for (Class<?> c = cls; c != null && c != stop && c != Object.class; c = c.getSuperclass()) {
            out.addAll(handlers.getOrDefault(c.getName().replace('.', '/'), Set.of()));
        }
        return out;
    }

    private final Map<String, Class<?>> handlerBases = new HashMap<>();

    /** The handler base class this class extends (effects and AI both count as SpellAbilityEffect), or null. */
    private Class<?> handlerBase(String cls) {
        if (cls.equals(GENERIC)) {
            return null;
        }
        return handlerBases.computeIfAbsent(cls, EngineParams::loadHandlerBase);
    }

    private static boolean isAi(String cls) {
        return cls.startsWith("forge/ai/");
    }

    private static Class<?> loadHandlerBase(String cls) {
        try {
            Class<?> c = Class.forName(cls.replace('/', '.'), false, EngineParams.class.getClassLoader());
            for (Class<?> base : new Class<?>[] {SpellAbilityEffect.class, Trigger.class, ReplacementEffect.class}) {
                if (base.isAssignableFrom(c) && c != base && c != SpellAbilityAi.class) {
                    return base;
                }
            }
        } catch (ReflectiveOperationException | LinkageError e) {
            // not a loadable class; not a handler
        }
        return null;
    }

    private static String outer(String cls) {
        int nested = cls.indexOf('$');
        return nested < 0 ? cls : cls.substring(0, nested);
    }
}
