package forge.gui.card;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import forge.card.mana.ManaCostShard;
import forge.game.ability.AbilityFactory;
import forge.game.ability.ApiType;
import forge.game.cost.Cost;
import forge.game.cost.CostPartMana;
import forge.game.replacement.ReplacementType;
import forge.game.staticability.StaticAbilityMode;
import forge.game.trigger.TriggerType;
import forge.util.TextUtil;

/**
 * Lints a card script against the engine's own definitions: the API, trigger, replacement and static
 * enums, the params the engine reads ({@link CardScriptParams}), {@link AbilityFactory}'s sub-ability
 * keys, and the cost and mana parsers.
 *
 * ERROR means the engine ignores or fails on something; WARN means it is probably a mistake. Messages
 * are short "wrong → right" lines in Markdown, since the review workflow posts them as PR comments.
 */
public final class CardScriptLinter {

    private static final String TO = " → ";

    public enum Severity { ERROR, WARN }

    /** A problem on a line; {@code token} is the offending text on that line, if any. */
    public record Finding(int line, Severity severity, String code, String message, String token) {
        @Override
        public String toString() {
            return line + ": [" + severity + "] " + code + " " + message;
        }
    }

    /** Line prefixes CardRules.Reader#parseLine understands. */
    private static final Set<String> PREFIXES = Set.of("Name", "ManaCost", "Types", "PT", "Loyalty",
        "Defense", "Colors", "Text", "Oracle", "K", "A", "T", "S", "R", "SVar", "AI", "DeckHints",
        "DeckNeeds", "DeckHas", "DeckRule", "AlternateMode", "Variant", "HandLifeModifier",
        "Draft", "CopyFaceFrom", "MeldPair", "FlavorName", "Lights", "SETCOLORID");
    private static final Map<String, String> PREFIX_BY_LOWER = new HashMap<>();
    static {
        PREFIXES.forEach(p -> PREFIX_BY_LOWER.put(p.toLowerCase(Locale.ROOT), p));
    }

    private enum Kind { ABILITY, TRIGGER, STATIC, REPLACEMENT }

    /** Params naming SVars that hold abilities, triggers, ...: comma lists. */
    private static final Map<String, Kind> SVAR_LISTS = Map.of(
        "Abilities", Kind.ABILITY, "Triggers", Kind.TRIGGER, "AddTriggers", Kind.TRIGGER,
        "StaticAbilities", Kind.STATIC, "AddStaticAbilities", Kind.STATIC, "ReplacementEffects", Kind.REPLACEMENT);
    /** The same, split on " & " by StaticAbilityContinuous. */
    private static final Map<String, Kind> SVAR_AMP_LISTS = Map.of(
        "AddAbility", Kind.ABILITY, "AddTrigger", Kind.TRIGGER, "AddStaticAbility", Kind.STATIC,
        "AddReplacementEffect", Kind.REPLACEMENT);
    private static final Set<String> AMP_LISTS = Set.of("AddAbility", "AddTrigger", "AddStaticAbility",
        "AddReplacementEffect", "AddTypes", "AddKeyword", "AddKeywords", "RemoveKeywords");
    private static final Set<String> DESC_KEYS = Set.of("SpellDescription", "TriggerDescription",
        "StackDescription", "Description");
    private static final Set<String> ZONES = Set.of("Battlefield", "Exile", "Graveyard", "Hand", "Library",
        "Command", "Stack");
    /** Values whose case the engine matches exactly, by the param they belong to. */
    private static final Map<String, Set<String>> CANONICAL = Map.of(
        "Defined", Set.of("Self", "Targeted", "Remembered", "Imprinted"), "Origin", ZONES, "Destination", ZONES);

    private static final Pattern DECLARER = Pattern.compile("AB|SP|DB|ST", Pattern.CASE_INSENSITIVE);
    private static final Pattern SVAR_NAME = Pattern.compile("[A-Za-z0-9_]+");
    private static final Pattern TRIGGERED = Pattern.compile("\\bTriggered[A-Za-z]+\\b");

    private final CardScriptParams params;

    public CardScriptLinter() {
        this(CardScriptParams.get());
    }

    public CardScriptLinter(CardScriptParams params) {
        this.params = params;
    }

    public List<Finding> lint(String script) {
        List<Finding> out = new ArrayList<>();
        String[] lines = script.split("\r?\n", -1);
        // SVars belong to one face; ALTERNATE and SPECIALIZE start the next
        int start = 0;
        for (int i = 0; i <= lines.length; i++) {
            if (i == lines.length || lines[i].equals("ALTERNATE") || lines[i].startsWith("SPECIALIZE")) {
                new Face(lines, start, i, start == 0, out).lint();
                start = i + 1;
            }
        }
        out.sort(Comparator.comparingInt(Finding::line));
        return out;
    }

    /** One face of a card: lines [from, to). */
    private final class Face {
        private final String[] lines;
        private final int from, to;
        private final boolean front;
        private final List<Finding> out;
        private final Map<String, Integer> svars = new LinkedHashMap<>();
        private final Map<String, Kind> svarKinds = new HashMap<>();

        Face(String[] lines, int from, int to, boolean front, List<Finding> out) {
            this.lines = lines;
            this.from = from;
            this.to = to;
            this.front = front;
            this.out = out;
        }

        void lint() {
            for (int i = from; i < to; i++) {
                String line = unwrapVariant(lines[i]);
                if (line.startsWith("SVar:")) {
                    String[] p = line.split(":", 3);
                    if (p.length == 3) {
                        svars.put(p[1], i + 1);
                    }
                }
                // an SVar named in Triggers$ holds a trigger, in StaticAbilities$ a static, ...
                for (Map.Entry<String, String> p : parse(bodyOf(line)).entrySet()) {
                    Kind k = SVAR_LISTS.getOrDefault(p.getKey(), SVAR_AMP_LISTS.get(p.getKey()));
                    if (k != null) {
                        for (String ref : p.getValue().split(",|\\s&\\s")) {
                            svarKinds.put(ref.trim(), k);
                        }
                    }
                }
            }

            boolean hasManaCost = false, isLand = false, hasTypes = false;
            for (int i = from; i < to; i++) {
                String line = lines[i];
                if (line.startsWith("ManaCost:")) {
                    hasManaCost = true;
                } else if (line.startsWith("Types:")) {
                    hasTypes = true;
                    isLand = line.contains("Land");
                }
                lintLine(line, i + 1);
            }
            if (front && hasTypes && !hasManaCost && !isLand) {
                add(from + 1, Severity.ERROR, "NO-MANACOST", "missing a `ManaCost` line", null);
            }

            for (Map.Entry<String, Integer> e : svars.entrySet()) {
                String body = unwrapVariant(lines[e.getValue() - 1]).split(":", 3)[2];
                if (kindOf(e.getKey(), body) != null && !isReferenced(e.getKey(), e.getValue() - 1)
                        && !params.isEngineSVar(e.getKey())) {
                    add(e.getValue(), Severity.WARN, "ORPHAN", "SVar `" + e.getKey() + "` is never referenced", e.getKey());
                }
            }
        }

        private void lintLine(String line, int ln) {
            if (line.isBlank() || line.startsWith("#")) {
                return;
            }
            int colon = line.indexOf(':');
            String prefix = colon > 0 ? line.substring(0, colon) : line.trim();
            String value = colon > 0 ? line.substring(colon + 1) : "";
            if (colon > 0 && value.startsWith(":")) {
                add(ln, Severity.ERROR, "LEX-PREFIX", "`" + prefix + "::`" + TO + "`" + prefix + ":`", prefix);
                return;
            }
            if (!PREFIXES.contains(prefix)) {
                String want = PREFIX_BY_LOWER.get(prefix.toLowerCase(Locale.ROOT));
                add(ln, Severity.ERROR, "LEX-PREFIX", want != null
                    ? "`" + prefix + ":`" + TO + "`" + want + ":`"
                    : "`" + prefix + ":`" + TO + "not a line the card reader knows", prefix);
                return;
            }
            if (line.contains("’")) {
                add(ln, Severity.WARN, "LEX-CURLY", "curly apostrophe `’`" + TO + "`'`", "’");
            }
            switch (prefix) {
                case "ManaCost" -> checkMana(value.trim(), ln);
                case "Variant" -> {
                    int c = value.indexOf(':');
                    if (c > 0) {
                        lintLine(value.substring(c + 1), ln);
                    }
                }
                case "A" -> checkParams(value, ln, Kind.ABILITY, true);
                case "T" -> checkParams(value, ln, Kind.TRIGGER, false);
                case "S" -> checkParams(value, ln, Kind.STATIC, false);
                case "R" -> checkParams(value, ln, Kind.REPLACEMENT, false);
                case "SVar" -> {
                    String[] p = value.split(":", 2);
                    Kind k = p.length == 2 ? kindOf(p[0], p[1]) : null;
                    if (k != null) {
                        checkParams(p[1], ln, k, false);
                    }
                }
                case "K" -> {
                    if (value.startsWith("Chapter:")) {
                        String[] p = value.split(":");
                        for (String ch : p.length > 2 ? p[2].split(",") : new String[0]) {
                            checkRef("Chapter", ch.trim(), ln);
                        }
                    }
                }
                default -> { }
            }
        }

        /** What an SVar holds, or null for a plain value such as {@code Count$...}. */
        private Kind kindOf(String svar, String body) {
            Map<String, String> p = parse(body);
            if (p.keySet().stream().anyMatch(k -> DECLARER.matcher(k).matches())) {
                return Kind.ABILITY;
            } else if (p.containsKey("Event")) {
                return Kind.REPLACEMENT;
            } else if (p.containsKey("Mode")) {
                Kind referenced = svarKinds.get(svar);
                if (referenced == Kind.TRIGGER || referenced == Kind.STATIC) {
                    return referenced;
                }
                // unreferenced and no Execute: settings the engine looks up by name (AIRollPlanarDieParams)
                return p.containsKey("Execute") ? Kind.TRIGGER : null;
            }
            return null;
        }

        private void checkParams(String body, int ln, Kind kind, boolean activated) {
            if (Pattern.compile("\\S\\||\\|\\S").matcher(body).find()) {
                add(ln, Severity.WARN, "LEX-PIPE", "add a space around the `|` separator", "|");
            }
            if (body.contains("$  ")) {
                add(ln, Severity.WARN, "LEX-DBLSPACE", "double space after `$`", "$  ");
            }
            Map<String, String> seen = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            Map<String, String> keys = new LinkedHashMap<>();
            for (String piece : body.split("\\|")) {
                int d = piece.indexOf('$');
                String key = (d < 0 ? piece : piece.substring(0, d)).trim();
                String val = d < 0 ? "" : piece.substring(d + 1).trim();
                if (key.isEmpty()) {
                    continue;
                }
                if (seen.containsKey(key)) {
                    boolean same = seen.get(key).equals(val);
                    add(ln, same ? Severity.WARN : Severity.ERROR, "DUP-PARAM", same
                        ? "duplicate `" + key + "$`"
                        : "duplicate `" + key + "$`" + TO + "the engine keeps `" + val + "`, drops `" + seen.get(key) + "`", key);
                }
                seen.put(key, val);
                keys.put(key, val);
                if (d >= 0 && DESC_KEYS.contains(key) && !val.isEmpty() && !piece.substring(d + 1).startsWith(" ")) {
                    add(ln, Severity.WARN, "LEX-NOSPACE", "add a space after `" + key + "$`", key);
                }
            }

            // what handles the line: an API, a trigger mode, a replacement event or static modes
            String declarer = keys.keySet().stream().filter(k -> DECLARER.matcher(k).matches()).findFirst().orElse(null);
            String mode = seen.getOrDefault("Mode", "");
            ApiType api = null;
            TriggerType trigger = null;
            ReplacementType event = null;
            if (kind == Kind.ABILITY) {
                if (declarer == null) {
                    add(ln, Severity.ERROR, "API-UNKNOWN", "ability has no `AB$`, `SP$` or `DB$`", null);
                } else {
                    String name = keys.get(declarer);
                    api = parseEnum(() -> ApiType.smartValueOf(name), ln, declarer + "$ " + name);
                }
                if (api == ApiType.ImmediateTrigger) {
                    trigger = TriggerType.Immediate;
                } else if (api == ApiType.DelayedTrigger && !mode.isEmpty()) {
                    trigger = parseEnum(() -> TriggerType.smartValueOf(mode), ln, "Mode$ " + mode);
                }
            } else if (kind == Kind.TRIGGER) {
                trigger = parseEnum(() -> TriggerType.smartValueOf(mode), ln, "Mode$ " + mode);
            } else if (kind == Kind.REPLACEMENT) {
                String name = seen.getOrDefault("Event", "");
                event = parseEnum(() -> ReplacementType.smartValueOf(name), ln, "Event$ " + name);
            } else if (!mode.isEmpty()) {
                for (String m : mode.split("[, ]+")) {
                    parseEnum(() -> StaticAbilityMode.smartValueOf(m), ln, "Mode$ " + m);
                }
            }

            for (Map.Entry<String, String> p : keys.entrySet()) {
                if (!p.getKey().equals(declarer)) {
                    checkKey(p.getKey(), ln, kind, api, trigger, event);
                    checkValue(p.getKey(), p.getValue(), ln, api);
                }
            }

            if (api != null) {
                for (List<String> group : params.requiredParams(api)) {
                    if (group.stream().noneMatch(keys::containsKey)) {
                        add(ln, Severity.ERROR, "MISSING-KEY", "`" + api + "` needs `" + String.join("$` or `", group) + "$`", null);
                    }
                }
            }
            if (activated && TRIGGERED.matcher(body).find()) {
                add(ln, Severity.ERROR, "TRIG-CTX", "trigger-only reference on an `A:` line (an activated ability has no trigger)", null);
            }
            if (activated && seen.getOrDefault("Cost", "").contains("LOYALTY")
                    && !"True".equalsIgnoreCase(seen.get("Planeswalker"))) {
                add(ln, Severity.ERROR, "LOYALTY", "loyalty ability needs `Planeswalker$ True`", null);
            }
        }

        private void checkKey(String key, int ln, Kind kind, ApiType api, TriggerType trigger, ReplacementType event) {
            Set<String> accepted = switch (kind) {
                case ABILITY -> api == null ? null : withTrigger(params.forApi(api), trigger);
                case TRIGGER -> trigger == null ? null : params.forTrigger(trigger);
                case REPLACEMENT -> event == null ? null : params.forReplacement(event);
                case STATIC -> params.forStatic();
            };
            if (accepted == null) {
                return; // this handler doesn't declare its params yet
            }
            String by = api != null ? api.toString() : trigger != null ? "trigger " + trigger
                : event != null ? "replacement " + event : "static abilities";
            String k = "`" + key + "$`";
            if (accepted.contains(key) || params.isInverted(key)) {
                if (params.isInternal(key)) {
                    add(ln, Severity.WARN, "INTERNAL-KEY", k + TO + "set by the engine, not by scripts", key);
                }
            } else if (params.isKnown(key) || !params.isComplete()) {
                add(ln, Severity.WARN, "WRONG-KEY", k + TO + "not used by " + by, key);
            } else if (params.correctCase(key) != null) {
                add(ln, Severity.ERROR, "KEY-CASE", k + TO + "`" + params.correctCase(key) + "$` (params are case-sensitive)", key);
            } else if (suggestion(key, accepted) != null) {
                add(ln, Severity.ERROR, "KEY-TYPO", k + TO + "`" + suggestion(key, accepted) + "$`", key);
            } else {
                add(ln, Severity.ERROR, "UNKNOWN-KEY", k + TO + "not a param the engine reads", key);
            }
        }

        /**
         * A param one edit away that this line's handler reads and that starts the same: a typo rarely
         * hits the first letter, so Host$ isn't taken for Cost$.
         */
        private String suggestion(String key, Set<String> accepted) {
            return params.similar(key).stream()
                .filter(p -> accepted.contains(p) && p.charAt(0) == key.charAt(0))
                .findFirst().orElse(null);
        }

        /** DelayedTrigger and ImmediateTrigger abilities also carry the params of the trigger they create. */
        private Set<String> withTrigger(Set<String> accepted, TriggerType trigger) {
            Set<String> triggerParams = trigger == null ? null : params.forTrigger(trigger);
            if (accepted == null || triggerParams == null) {
                return accepted;
            }
            Set<String> out = new HashSet<>(accepted);
            out.addAll(triggerParams);
            return out;
        }

        private void checkValue(String key, String val, int ln, ApiType api) {
            if (AbilityFactory.additionalAbilityKeys.contains(key) || key.equals("SubAbility") || key.equals("ReplaceWith")) {
                checkRef(key, val, ln);
            }
            if (key.equals("Choices") && AbilityFactory.CHOICE_APIS.contains(api)) {
                for (String c : val.split(",")) {
                    checkRef(key, c.trim(), ln);
                }
            }
            if (SVAR_LISTS.containsKey(key)) {
                if (val.contains(" & ")) {
                    add(ln, Severity.ERROR, "LEX-DELIM", "`" + key + "$` is split on `,`, not ` & `", val);
                }
                for (String r : val.split(",")) {
                    checkRef(key, r.trim(), ln);
                }
            }
            if (SVAR_AMP_LISTS.containsKey(key)) {
                for (String r : val.split(" & ")) {
                    checkRef(key, r.trim(), ln);
                }
            }
            // a ':' means a parameterized keyword (Protection:...), whose value carries commas itself
            if (AMP_LISTS.contains(key) && val.contains(",") && !val.contains(":")) {
                add(ln, Severity.ERROR, "LEX-DELIM", "`" + key + "$` is split on ` & `, not `,`", val);
            }
            if (CANONICAL.containsKey(key)) {
                String head = val.split(" ")[0];
                for (String c : CANONICAL.get(key)) {
                    if (c.equalsIgnoreCase(head) && !c.equals(head)) {
                        add(ln, Severity.ERROR, "CASE", "`" + key + "$ " + head + "`" + TO + "`" + key + "$ " + c + "` (case-sensitive)", head);
                    }
                }
            }
            if (key.equals("SpellDescription") && val.matches("\\{[^}]+\\}:.*")) {
                add(ln, Severity.WARN, "DESC-COST", "`SpellDescription$` restates the activation cost", null);
            }
            if (key.equals("Cost")) {
                checkCost(val, ln);
            }
        }

        private void checkRef(String key, String val, int ln) {
            if (SVAR_NAME.matcher(val).matches() && !svars.containsKey(val)) {
                add(ln, Severity.ERROR, "REF-UNDEF", "`" + key + "$ " + val + "`" + TO + "no such SVar on this face", val);
            }
        }

        /** Each part must be a cost the engine parses, or mana; anything else silently becomes free. */
        private void checkCost(String cost, int ln) {
            for (String part : TextUtil.splitWithParenthesis(cost, ' ', '<', '>')) {
                if (part.isEmpty() || part.equals("Mandatory") || part.startsWith("XMin") || svars.containsKey(part)) {
                    continue; // an SVar name is an amount of generic mana
                }
                boolean isCostPart;
                try {
                    // an unknown part falls through to plain CostPartMana; Mana<> and subclasses are real parts
                    isCostPart = part.startsWith("Mana<") || new Cost(part, true).getCostParts().stream()
                        .anyMatch(cp -> cp.getClass() != CostPartMana.class);
                } catch (RuntimeException e) {
                    add(ln, Severity.ERROR, "COST", "`" + part + "`" + TO + "the cost doesn't parse", part);
                    continue;
                }
                if (!isCostPart && !isManaToken(part)) {
                    add(ln, Severity.ERROR, "COST", "`" + part + "`" + TO + "not a cost the engine knows (paid as nothing)", part);
                }
            }
        }

        private void checkMana(String cost, int ln) {
            if (cost.equalsIgnoreCase("no cost")) {
                return;
            }
            for (String tok : cost.split(" +")) {
                if (!isManaToken(tok)) {
                    add(ln, Severity.ERROR, "MANA", "`" + tok + "`" + TO + "not a mana symbol", tok);
                }
            }
        }

        /** Referenced from some other line of this face. */
        private boolean isReferenced(String svar, int ownLine) {
            Pattern word = Pattern.compile("\\b" + Pattern.quote(svar) + "\\b");
            for (int i = from; i < to; i++) {
                if (i != ownLine && word.matcher(lines[i]).find()) {
                    return true;
                }
            }
            return false;
        }

        private <T> T parseEnum(Supplier<T> parser, int ln, String what) {
            try {
                T t = parser.get();
                if (t != null) {
                    return t;
                }
            } catch (RuntimeException e) {
                // reported below
            }
            add(ln, Severity.ERROR, "API-UNKNOWN", "`" + what + "`" + TO + "unknown API or mode", what.substring(what.indexOf(' ') + 1));
            return null;
        }

        private void add(int ln, Severity s, String code, String msg, String token) {
            out.add(new Finding(ln, s, code, msg, token));
        }
    }

    /** The line a {@code Variant:Name:} line wraps, or the line itself. */
    private static String unwrapVariant(String line) {
        if (!line.startsWith("Variant:")) {
            return line;
        }
        int c = line.indexOf(':', "Variant:".length());
        return c < 0 ? line : line.substring(c + 1);
    }

    /** The key/value body of an A/T/S/R/SVar line, or "". */
    private static String bodyOf(String line) {
        if (line.startsWith("SVar:")) {
            String[] p = line.split(":", 3);
            return p.length == 3 ? p[2] : "";
        }
        return line.length() > 2 && line.charAt(1) == ':' && "ATSR".indexOf(line.charAt(0)) >= 0 ? line.substring(2) : "";
    }

    private static Map<String, String> parse(String body) {
        Map<String, String> out = new TreeMap<>();
        for (String piece : body.split("\\|")) {
            int d = piece.indexOf('$');
            if (d > 0) {
                out.put(piece.substring(0, d).trim(), piece.substring(d + 1).trim());
            }
        }
        return out;
    }

    /**
     * A generic amount, or a shard the engine knows. The shard parser ORs the letters together, so a
     * repeated letter ('WW') would silently collapse into one shard.
     */
    static boolean isManaToken(String tok) {
        if (tok.matches("\\d+")) {
            return true;
        }
        if (!tok.matches("[WUBRGCSXP2/]+") || ManaCostShard.parseNonGeneric(tok) == null) {
            return false;
        }
        Set<Character> seen = new HashSet<>();
        for (char c : tok.toCharArray()) {
            if (c != '/' && !seen.add(c)) {
                return false;
            }
        }
        return true;
    }
}
