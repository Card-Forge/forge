package forge.gui.card;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import forge.ai.SpellAbilityAi;
import forge.game.CardTraitBase;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.ability.IHasForgeParams;
import forge.game.ability.SpellAbilityEffect;
import forge.game.replacement.ReplacementType;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbilityCondition;
import forge.game.spellability.SpellAbilityRestriction;
import forge.game.spellability.TargetRestrictions;
import forge.game.trigger.TriggerType;

/**
 * The card-script params the engine accepts, from the {@link IHasForgeParams} declarations: the
 * framework classes' params apply to every ability, and each effect adds its own. CardScriptParamDeclarationTest
 * keeps the declarations in sync with the params the code reads.
 *
 * Until every effect declares, an undeclared API answers null: its params can't be checked at runtime.
 * Tests pass in the params scanned from the engine bytecode, which fill every gap. Params are
 * case-sensitive, as the engine reads them.
 */
public final class CardScriptParams {

    /**
     * The classes, outside effects, that declare the params they read: params every ability may use.
     * Other classes reading params (GameAction, CardFactory, ...) join in later batches.
     */
    public static final List<Class<?>> FRAMEWORK = List.of(AbilityFactory.class, AbilityUtils.class,
        CardTraitBase.class, SpellAbility.class, SpellAbilityCondition.class, SpellAbilityRestriction.class,
        TargetRestrictions.class, SpellAbilityEffect.class, SpellAbilityAi.class);

    /** Params scanned from the engine bytecode, by handler. */
    record Scanned(Set<String> framework, Map<ApiType, Set<String>> apis, Map<TriggerType, Set<String>> triggers,
                   Map<ReplacementType, Set<String>> replacements, Set<String> general, Set<String> trigger,
                   Set<String> replacement, Set<String> statics, Set<String> valid, Set<String> svars) { }

    private static CardScriptParams instance;

    private final Scanned scanned;
    private final Set<String> internal = new TreeSet<>();
    private final Set<ApiType> declared = new TreeSet<>();
    private final Map<ApiType, Set<String>> accepted = new EnumMap<>(ApiType.class);
    private final Map<ApiType, List<List<String>>> required = new EnumMap<>(ApiType.class);
    private final Map<TriggerType, Set<String>> triggers = new EnumMap<>(TriggerType.class);
    private final Map<ReplacementType, Set<String>> replacements = new EnumMap<>(ReplacementType.class);
    private Set<String> statics;
    private final Set<String> known = new TreeSet<>();
    private final Map<String, String> byLowerCase = new HashMap<>();

    /** The declared params. */
    public static synchronized CardScriptParams get() {
        if (instance == null) {
            instance = new CardScriptParams(null);
        }
        return instance;
    }

    CardScriptParams(Scanned scanned) {
        this.scanned = scanned;
        Set<String> shared = new TreeSet<>();
        for (Class<?> c : FRAMEWORK) {
            shared.addAll(List.of(orEmpty(IHasForgeParams.optionalParams(c))));
            internal.addAll(List.of(orEmpty(IHasForgeParams.internalParams(c))));
        }
        if (scanned != null) {
            shared.addAll(scanned.framework()); // read by classes that don't declare yet
        }
        Map<ApiType, Set<String>> own = new EnumMap<>(ApiType.class);
        for (ApiType api : ApiType.values()) {
            Set<String> params = new TreeSet<>();
            List<List<String>> req = new ArrayList<>();
            for (Class<?> c = api.getSpellEffect().getClass(); c != SpellAbilityEffect.class; c = c.getSuperclass()) {
                String[] opt = IHasForgeParams.optionalParams(c);
                String[][] groups = IHasForgeParams.requiredParams(c);
                String[] intern = IHasForgeParams.internalParams(c);
                if (opt != null || groups != null || intern != null) {
                    declared.add(api);
                }
                params.addAll(List.of(orEmpty(opt)));
                params.addAll(List.of(orEmpty(intern)));
                internal.addAll(List.of(orEmpty(intern)));
                for (String[] g : groups != null ? groups : new String[0][]) {
                    req.add(List.of(g));
                    params.addAll(List.of(g));
                }
            }
            if (declared.contains(api)) {
                own.put(api, params);
                required.put(api, req);
            } else if (scanned != null) {
                own.put(api, scanned.apis().getOrDefault(api, Set.of()));
            }
        }
        own.forEach((api, params) -> {
            Set<String> all = new TreeSet<>(shared);
            all.addAll(internal);
            all.addAll(params);
            accepted.put(api, all);
            known.addAll(all);
        });
        known.addAll(shared);
        known.addAll(internal);
        if (scanned != null) {
            for (TriggerType mode : TriggerType.values()) {
                triggers.put(mode, union(scanned.general(), scanned.trigger(), scanned.triggers().getOrDefault(mode, Set.of())));
            }
            for (ReplacementType event : ReplacementType.values()) {
                replacements.put(event, union(scanned.general(), scanned.replacement(),
                    scanned.replacements().getOrDefault(event, Set.of())));
            }
            statics = union(scanned.general(), scanned.statics());
            triggers.values().forEach(known::addAll);
            replacements.values().forEach(known::addAll);
            known.addAll(statics);
        }
        for (String k : known) {
            byLowerCase.putIfAbsent(k.toLowerCase(Locale.ROOT), k);
        }
    }

    /** True when every param the engine reads is known, so an unknown one is certainly a mistake. */
    public boolean isComplete() {
        return scanned != null || declared.size() == ApiType.values().length;
    }

    public boolean isDeclared(ApiType api) {
        return declared.contains(api);
    }

    /** The params an ability of this API accepts, or null if that isn't known. */
    public Set<String> forApi(ApiType api) {
        return accepted.get(api);
    }

    /** The params of this trigger mode, or null: only known from a scan until triggers declare. */
    public Set<String> forTrigger(TriggerType mode) {
        return triggers.get(mode);
    }

    public Set<String> forReplacement(ReplacementType event) {
        return replacements.get(event);
    }

    public Set<String> forStatic() {
        return statics;
    }

    /** Set by the engine itself (keyword abilities etc.), so wrong in a script. */
    public boolean isInternal(String param) {
        return internal.contains(param);
    }

    /** The groups of params this API needs; each group is satisfied by any one of its params. */
    public List<List<String>> requiredParams(ApiType api) {
        return required.getOrDefault(api, List.of());
    }

    /** True if some known handler reads this param (exact case). */
    public boolean isKnown(String param) {
        return known.contains(param) || isInverted(param);
    }

    /** The exact-case name of a known param that differs only in case, or null. */
    public String correctCase(String param) {
        return known.contains(param) ? null : byLowerCase.get(param.toLowerCase(Locale.ROOT));
    }

    /** Known params within one edit of this one, for "did you mean" hints. */
    public List<String> similar(String param) {
        List<String> out = new ArrayList<>();
        for (String k : known) {
            if (oneEditApart(param, k)) {
                out.add(k);
            }
        }
        return out;
    }

    /** An SVar the engine looks up by name, so it needs no reference from the script. */
    public boolean isEngineSVar(String name) {
        return scanned != null && scanned.svars().contains(name);
    }

    /** {@code "Invert" + param}, which CardTraitBase#matchesValidParam also reads. */
    public boolean isInverted(String param) {
        if (!param.startsWith("Invert") || param.length() == "Invert".length()) {
            return false;
        }
        String rest = param.substring("Invert".length());
        return scanned != null ? scanned.valid().contains(rest) : known.contains(rest);
    }

    @SafeVarargs
    private static Set<String> union(Set<String>... sets) {
        Set<String> out = new TreeSet<>();
        for (Set<String> s : sets) {
            out.addAll(s);
        }
        return out;
    }

    private static String[] orEmpty(String[] a) {
        return a != null ? a : new String[0];
    }

    private static boolean oneEditApart(String a, String b) {
        if (a.equals(b) || Math.abs(a.length() - b.length()) > 1) {
            return false;
        }
        if (a.length() > b.length()) {
            String t = a;
            a = b;
            b = t;
        }
        int i = 0;
        while (i < a.length() && a.charAt(i) == b.charAt(i)) {
            i++;
        }
        return a.length() == b.length()
            ? a.substring(i + 1).equals(b.substring(i + 1))
            : a.substring(i).equals(b.substring(i + 1));
    }
}
