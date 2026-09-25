package forge.gui.card;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Finds the card-script params the engine reads by looking at its bytecode, without loading any class.
 * {@code sa.getParam("NumCards")} compiles to {@code ldc "NumCards"} followed by the call, so the
 * constant in the key position of a param accessor call is a param. The call also names the receiver's
 * type, which says whose param it is: {@code t.getParam("Destination")} on a Trigger reads a trigger
 * param even inside AbilityUtils. Methods that pass one of their own String arguments on to the key
 * position, such as {@code getDefinedPlayersOrTargeted(sa, "TokenOwner")}, become accessors too.
 *
 * A call's String arguments are taken from the constants (and String parameters) pending since the
 * last expression boundary, counted back from the call by its descriptor. That is exact for the usual
 * one-call-per-expression code; keys held in non-constant fields or built at runtime are missed.
 */
final class ParamReadScanner {

    enum Kind { READ, VALID, WRITE, SVAR }

    /** Whose param a read is, from the receiver's type. ANY: a CardTraitBase or similar, could be any. */
    enum Context { ABILITY, TRIGGER, STATIC, REPLACEMENT, ANY }

    /**
     * One param name passed to an accessor by a method ("forge/game/Foo.bar(desc)") of a class. {@code own}
     * is false for {@code x.getParam("X")} on a local or a returned value, such as another ability on the
     * stack or a sub-ability, rather than on one of the method's parameters (or a SAME_ABILITY call on one).
     */
    record Read(String cls, String method, Kind kind, Context context, String name, boolean own) { }

    /** Engine methods whose first String argument is a param name (or, for SVAR, an SVar name). */
    private static final Map<String, Kind> ACCESSORS = Map.ofEntries(
        Map.entry("getParam", Kind.READ), Map.entry("hasParam", Kind.READ),
        Map.entry("getParamOrDefault", Kind.READ), Map.entry("getOriginalParam", Kind.READ),
        Map.entry("getAdditionalAbility", Kind.READ), Map.entry("getAdditionalAbilityList", Kind.READ),
        Map.entry("hasAdditionalAbility", Kind.READ),
        Map.entry("matchesValidParam", Kind.VALID),
        Map.entry("putParam", Kind.WRITE), Map.entry("removeParam", Kind.WRITE),
        Map.entry("getSVar", Kind.SVAR), Map.entry("hasSVar", Kind.SVAR), Map.entry("getSVarInt", Kind.SVAR));
    /** Lookups on a param map itself, e.g. {@code sa.getMapParams().get("X")}. */
    private static final Set<String> MAP_READS = Set.of("get", "containsKey", "getOrDefault");
    private static final Set<String> MAP_WRITES = Set.of("put", "remove");
    /** Calls that can return the ability they are made on, e.g. a root ability or the one that targets. */
    private static final Set<String> SAME_ABILITY = Set.of("getRootAbility", "getSATargetingCard");
    private static final Pattern NAME = Pattern.compile("[A-Za-z][A-Za-z0-9]*");
    /** A param inside an ability the engine writes itself, e.g. {@code "DB$ Pump | CumulativeUpkeep$ True"}. */
    private static final Pattern SCRIPT_KEY = Pattern.compile("(?:^|\\|)\\s*([A-Za-z][A-Za-z0-9]*)\\$");

    /**
     * A call: what it can take a key from, a constant (String) or the caller's n-th String parameter
     * (Integer), and whose param map it's made on, if it's made on one.
     */
    private record Call(String target, List<Object> args, List<Boolean> afterParam, Context paramMap) { }
    private record Method(String owner, String key, List<Call> calls) { }
    private record Accessor(Kind kind, int keyArg, Context context) { }

    private final List<Method> methods = new ArrayList<>();
    private final Map<String, String> superclasses = new HashMap<>();
    private final List<Read> scriptWrites = new ArrayList<>();
    /** Accessors found by forwarding, by "owner.name(desc)". */
    private final Map<String, Set<Accessor>> derived = new HashMap<>();

    /** Adds one class file. */
    void add(InputStream classFile) throws IOException {
        DataInputStream in = new DataInputStream(classFile);
        in.skipBytes(8);
        int count = in.readUnsignedShort();
        Object[] cp = new Object[count];
        int[][] refs = new int[count][];
        for (int i = 1; i < count; i++) {
            int tag = in.readUnsignedByte();
            switch (tag) {
                case 1 -> cp[i] = in.readUTF();
                case 7, 8 -> refs[i] = new int[] {tag, in.readUnsignedShort()};
                case 16, 19, 20 -> in.skipBytes(2);
                case 15 -> in.skipBytes(3);
                case 9, 10, 11, 12 -> refs[i] = new int[] {tag, in.readUnsignedShort(), in.readUnsignedShort()};
                case 3, 4, 17, 18 -> in.skipBytes(4);
                case 5, 6 -> { in.skipBytes(8); i++; }
                default -> throw new IOException("bad constant pool tag " + tag);
            }
        }
        in.skipBytes(2);
        String self = (String) cp[refs[in.readUnsignedShort()][1]];
        int superIndex = in.readUnsignedShort();
        if (superIndex != 0) {
            superclasses.put(self, (String) cp[refs[superIndex][1]]);
        }
        for (int i = 1; i < count; i++) {
            if (refs[i] != null && refs[i][0] == 8) {
                Matcher m = SCRIPT_KEY.matcher((String) cp[refs[i][1]]);
                while (m.find()) {
                    scriptWrites.add(new Read(self, null, Kind.WRITE, Context.ANY, m.group(1), true));
                }
            }
        }
        in.skipBytes(2 * in.readUnsignedShort());
        skipMembers(in);
        int methodCount = in.readUnsignedShort();
        for (int m = 0; m < methodCount; m++) {
            int access = in.readUnsignedShort();
            String name = (String) cp[in.readUnsignedShort()];
            String desc = (String) cp[in.readUnsignedShort()];
            int attrs = in.readUnsignedShort();
            byte[] code = null;
            String signature = null;
            for (int a = 0; a < attrs; a++) {
                String attr = (String) cp[in.readUnsignedShort()];
                int len = in.readInt();
                if (attr.equals("Code")) {
                    in.skipBytes(4);
                    code = new byte[in.readInt()];
                    in.readFully(code);
                    in.skipBytes(len - 8 - code.length);
                } else if (attr.equals("Signature")) {
                    signature = (String) cp[in.readUnsignedShort()];
                } else {
                    in.skipBytes(len);
                }
            }
            if (code != null) {
                boolean isStatic = (access & 0x0008) != 0;
                methods.add(new Method(self, self + "." + name + desc, calls(code, cp, refs,
                    slots(desc, isStatic, "Ljava/lang/String;"), paramMapSlots(desc, signature, isStatic),
                    slots(desc, isStatic, null))));
            }
        }
    }

    /**
     * Map parameters that can hold a trait's params: a Map<String, String>. Other maps, such as the AI's
     * Map<String, Object> of options, don't hold script params.
     */
    private static List<Integer> paramMapSlots(String desc, String signature, boolean isStatic) {
        List<Integer> maps = slots(desc, isStatic, "Ljava/util/Map;");
        if (signature == null || maps.isEmpty()) {
            return maps;
        }
        List<String> erased = paramTypes(desc);
        List<String> generic = genericParamTypes(signature);
        if (generic.size() != erased.size()) {
            return maps; // e.g. an inner class constructor, whose signature leaves out the outer instance
        }
        List<Integer> out = new ArrayList<>();
        int slot = isStatic ? 0 : 1;
        for (int i = 0; i < erased.size(); i++) {
            if (generic.get(i).equals("Ljava/util/Map<Ljava/lang/String;Ljava/lang/String;>;")) {
                out.add(slot);
            }
            slot += erased.get(i).equals("J") || erased.get(i).equals("D") ? 2 : 1;
        }
        return out;
    }

    /** The parameter types of a generic method signature (JVMS 4.7.9.1), with their type arguments. */
    private static List<String> genericParamTypes(String signature) {
        List<String> out = new ArrayList<>();
        int i = signature.indexOf('(') + 1;
        while (signature.charAt(i) != ')') {
            int start = i;
            while (signature.charAt(i) == '[') {
                i++;
            }
            char c = signature.charAt(i);
            if (c == 'L') {
                for (int depth = 0; signature.charAt(i) != ';' || depth > 0; i++) {
                    depth += signature.charAt(i) == '<' ? 1 : signature.charAt(i) == '>' ? -1 : 0;
                }
            } else if (c == 'T') {
                i = signature.indexOf(';', i);
            }
            out.add(signature.substring(start, ++i));
        }
        return out;
    }

    /** Every name passed to an accessor, plus every param in an ability string the engine writes. */
    List<Read> result() {
        // a method forwarding one of its String parameters into an accessor's key is an accessor
        for (boolean changed = true; changed; ) {
            changed = false;
            for (Method m : methods) {
                if (ACCESSORS.containsKey(simpleName(m.key()))) {
                    continue;
                }
                for (Call c : m.calls()) {
                    for (Accessor a : accessors(c)) {
                        if (key(c, a) instanceof Integer param) {
                            changed |= derived.computeIfAbsent(m.key(), x -> new HashSet<>())
                                .add(new Accessor(a.kind(), param, a.context()));
                        }
                    }
                }
            }
        }
        List<Read> out = new ArrayList<>(scriptWrites);
        for (Method m : methods) {
            for (Call c : m.calls()) {
                for (Accessor a : accessors(c)) {
                    if (key(c, a) instanceof String s && NAME.matcher(s).matches()) {
                        out.add(new Read(m.owner(), m.key(), a.kind(), a.context(), s, onParameter(c)));
                    }
                }
            }
        }
        return out;
    }

    /** Which scanned methods each scanned method calls, inherited calls resolved to where they're declared. */
    Map<String, Set<String>> callGraph() {
        Set<String> known = new HashSet<>();
        methods.forEach(m -> known.add(m.key()));
        Map<String, Set<String>> out = new HashMap<>();
        for (Method m : methods) {
            Set<String> callees = out.computeIfAbsent(m.key(), k -> new HashSet<>());
            for (Call c : m.calls()) {
                String target = c.target();
                String owner = target.substring(0, target.lastIndexOf('.', target.indexOf('(')));
                String member = target.substring(owner.length());
                for (String cls = owner; cls != null; cls = superclasses.get(cls)) {
                    if (known.contains(cls + member)) {
                        callees.add(cls + member);
                        break;
                    }
                }
            }
        }
        return out;
    }

    private Set<Accessor> accessors(Call c) {
        String target = c.target();
        String owner = target.substring(0, target.lastIndexOf('.', target.indexOf('(')));
        String name = simpleName(target);
        Kind k = owner.startsWith("forge/") ? ACCESSORS.get(name) : null;
        if (k != null) {
            return Set.of(new Accessor(k, 0, contextOf(owner)));
        }
        if (c.paramMap() != null && owner.equals("java/util/Map")) {
            if (MAP_READS.contains(name)) {
                return Set.of(new Accessor(Kind.READ, 0, c.paramMap()));
            } else if (MAP_WRITES.contains(name)) {
                return Set.of(new Accessor(Kind.WRITE, 0, c.paramMap()));
            }
        }
        // an inherited method is called through the subclass: look it up where it's declared
        String member = target.substring(owner.length());
        for (String cls = owner; cls != null; cls = superclasses.get(cls)) {
            Set<Accessor> found = derived.get(cls + member);
            if (found != null) {
                return found;
            }
        }
        return Set.of();
    }

    private static Context contextOf(String owner) {
        if (owner.startsWith("forge/game/trigger/")) {
            return Context.TRIGGER;
        } else if (owner.startsWith("forge/game/staticability/") || owner.startsWith("forge/game/StaticEffect")) {
            // StaticEffect holds the params of the static ability it applies
            return Context.STATIC;
        } else if (owner.startsWith("forge/game/replacement/")) {
            return Context.REPLACEMENT;
        } else if (owner.startsWith("forge/game/spellability/")) {
            return Context.ABILITY;
        }
        return Context.ANY;
    }

    /** The key argument of a call: counted back from the call over its String parameters (a Map's Object key). */
    private static Object key(Call c, Accessor a) {
        int strings = stringArgs(c.target());
        if (c.args().isEmpty() || strings == 0) {
            return null;
        }
        return c.args().get(Math.max(0, c.args().size() - strings + a.keyArg()));
    }

    /**
     * Whether a direct accessor call is made on one of the method's parameters: the receiver is loaded just
     * before the key. A helper passed the key, such as getDefinedCards(host, "Defined", sa), counts as one.
     */
    private static boolean onParameter(Call c) {
        String target = c.target();
        if (!target.startsWith("forge/") || !ACCESSORS.containsKey(simpleName(target)) || c.args().isEmpty()) {
            return true;
        }
        return c.afterParam().get(Math.max(0, c.args().size() - stringArgs(target)));
    }

    private static int stringArgs(String target) {
        boolean map = target.startsWith("java/util/Map.");
        int strings = 0;
        for (String type : paramTypes(target.substring(target.indexOf('(')))) {
            if (type.equals("Ljava/lang/String;") || map && type.equals("Ljava/lang/Object;")) {
                strings++;
            }
        }
        return strings;
    }

    /**
     * Walks the bytecode, collecting what each call can take a key from. The pending values stand in for
     * the operand stack: a call takes its String arguments off the end, and a store, branch or return
     * (the end of an expression) clears them.
     */
    private static List<Call> calls(byte[] code, Object[] cp, int[][] refs, List<Integer> stringSlots, List<Integer> mapSlots,
            List<Integer> paramSlots) {
        List<Call> calls = new ArrayList<>();
        List<Object> args = new ArrayList<>();
        // for each pending value: whether it was loaded straight after one of the method's parameters
        List<Boolean> afterParam = new ArrayList<>();
        boolean paramJustLoaded = false;
        // parameters, and locals holding one of them or what a SAME_ABILITY call on one returned
        Set<Integer> ownSlots = new HashSet<>(paramSlots);
        Map<Integer, String> constLocals = new HashMap<>();
        // a Map parameter, or a local holding a trait's param map, and whose params it holds
        Map<Integer, Context> mapLocals = new HashMap<>();
        mapSlots.forEach(slot -> mapLocals.put(slot, Context.ANY));
        String lastConst = null;
        Context paramMap = null;
        int pc = 0;
        while (pc < code.length) {
            int op = code[pc] & 0xff;
            String konst = null;
            boolean paramLoaded = false;
            switch (op) {
                case 0x12, 0x13 -> {                                         // ldc, ldc_w
                    int[] r = refs[op == 0x12 ? code[pc + 1] & 0xff : u2(code, pc + 1)];
                    if (r != null && r[0] == 8) {
                        konst = (String) cp[r[1]];
                        args.add(konst);
                        afterParam.add(paramJustLoaded);
                    }
                }
                case 0x19, 0x2a, 0x2b, 0x2c, 0x2d -> {                       // aload
                    int slot = op == 0x19 ? code[pc + 1] & 0xff : op - 0x2a;
                    if (constLocals.containsKey(slot)) {
                        args.add(constLocals.get(slot));
                        afterParam.add(paramJustLoaded);
                    } else if (stringSlots.contains(slot)) {
                        args.add(stringSlots.indexOf(slot));
                        afterParam.add(paramJustLoaded);
                    }
                    paramLoaded = ownSlots.contains(slot);
                    if (mapLocals.containsKey(slot)) {
                        paramMap = mapLocals.get(slot);
                    }
                }
                case 0x3a, 0x4b, 0x4c, 0x4d, 0x4e -> {                       // astore: String key = "X";
                    int slot = op == 0x3a ? code[pc + 1] & 0xff : op - 0x4b;
                    if (paramJustLoaded) {
                        ownSlots.add(slot);
                    } else if (!paramSlots.contains(slot)) {
                        ownSlots.remove(slot);
                    }
                    if (lastConst != null) {
                        constLocals.put(slot, lastConst);
                    } else {
                        constLocals.remove(slot);
                    }
                    if (paramMap != null) {
                        mapLocals.put(slot, paramMap);
                    } else if (!mapSlots.contains(slot)) {
                        mapLocals.remove(slot);
                    }
                    args.clear();
                    afterParam.clear();
                    paramMap = null;
                }
                case 0xb4 -> {                                               // getfield: this.mapParams
                    int[] fref = refs[u2(code, pc + 1)];
                    String field = (String) cp[refs[fref[2]][1]];
                    if (field.toLowerCase(Locale.ROOT).endsWith("params")) {
                        paramMap = contextOf((String) cp[refs[fref[1]][1]]);
                    }
                }
                case 0xb6, 0xb7, 0xb8, 0xb9 -> {                             // invoke*
                    int[] mref = refs[u2(code, pc + 1)];
                    int[] nat = refs[mref[2]];
                    String name = (String) cp[nat[1]];
                    String target = cp[refs[mref[1]][1]] + "." + name + cp[nat[2]];
                    calls.add(new Call(target, List.copyOf(args), List.copyOf(afterParam), paramMap));
                    paramLoaded = paramJustLoaded && SAME_ABILITY.contains(name);
                    // runParams.get(AbilityKey.X) takes an enum key, not the pending name of an enclosing
                    // matchesValidParam("X", ...): only a param map's lookup takes a pending name
                    boolean otherMap = target.startsWith("java/util/Map.") && paramMap == null;
                    int consumed = otherMap ? 0 : Math.min(stringArgs(target), args.size());
                    args.subList(args.size() - consumed, args.size()).clear();
                    afterParam.subList(afterParam.size() - consumed, afterParam.size()).clear();
                    // a trait's param map (a StaticAbility's holds static params), or a copy of one
                    if (name.equals("getMapParams") || name.equals("getOriginalMapParams")) {
                        paramMap = contextOf(target.substring(0, target.lastIndexOf('.', target.indexOf('('))));
                    } else if (!target.endsWith("Map;")) {
                        paramMap = null;
                    }
                    if (target.endsWith(")V")) {
                        args.clear();
                        afterParam.clear();
                    }
                }
                case 0xba, 0x57, 0x58, 0xa8, 0xbf, 0xb3, 0xb5 -> {           // indy, pop, jsr, athrow, put*
                    args.clear();
                    afterParam.clear();
                    paramMap = null;
                }
                default -> {
                    if (op >= 0x36 && op <= 0x56 || op >= 0x99 && op <= 0xa7 || op >= 0xac && op <= 0xb1
                            || op == 0xc6 || op == 0xc7) {
                        args.clear();                                        // store, branch, return
                        afterParam.clear();
                        paramMap = null;
                    }
                }
            }
            lastConst = konst;
            paramJustLoaded = paramLoaded;
            pc += length(code, pc);
        }
        return calls;
    }

    /** Local-variable slots holding the method's parameters of this type (null: all, and this), in order. */
    private static List<Integer> slots(String desc, boolean isStatic, String wanted) {
        List<Integer> slots = new ArrayList<>();
        if (wanted == null && !isStatic) {
            slots.add(0);
        }
        int slot = isStatic ? 0 : 1;
        for (String type : paramTypes(desc)) {
            if (wanted == null || type.equals(wanted)) {
                slots.add(slot);
            }
            slot += type.equals("J") || type.equals("D") ? 2 : 1;
        }
        return slots;
    }

    private static List<String> paramTypes(String desc) {
        List<String> out = new ArrayList<>();
        for (int i = 1; desc.charAt(i) != ')'; i++) {
            int start = i;
            while (desc.charAt(i) == '[') {
                i++;
            }
            if (desc.charAt(i) == 'L') {
                i = desc.indexOf(';', i);
            }
            out.add(desc.substring(start, i + 1));
        }
        return out;
    }

    /** "owner.name(desc)" -> "name" */
    private static String simpleName(String target) {
        int paren = target.indexOf('(');
        return target.substring(target.lastIndexOf('.', paren) + 1, paren);
    }

    /** Instruction length (JVMS 6.5). */
    private static int length(byte[] code, int pc) {
        int op = code[pc] & 0xff;
        switch (op) {
            case 0x10, 0x12, 0x15, 0x16, 0x17, 0x18, 0x19, 0x36, 0x37, 0x38, 0x39, 0x3a, 0xa9, 0xbc:
                return 2;
            case 0x11, 0x13, 0x14, 0x84, 0xb2, 0xb3, 0xb4, 0xb5, 0xb6, 0xb7, 0xb8, 0xbb, 0xbd, 0xc0, 0xc1, 0xc6, 0xc7:
                return 3;
            case 0xc5:
                return 4;
            case 0xb9, 0xba, 0xc8, 0xc9:
                return 5;
            case 0xc4:
                return (code[pc + 1] & 0xff) == 0x84 ? 6 : 4;
            case 0xaa: {
                int p = (pc + 4) & ~3;
                return p + 12 + 4 * (s4(code, p + 8) - s4(code, p + 4) + 1) - pc;
            }
            case 0xab: {
                int p = (pc + 4) & ~3;
                return p + 8 + 8 * s4(code, p + 4) - pc;
            }
            default:
                return op >= 0x99 && op <= 0xa8 ? 3 : 1;
        }
    }

    private static void skipMembers(DataInputStream in) throws IOException {
        int n = in.readUnsignedShort();
        for (int i = 0; i < n; i++) {
            in.skipBytes(6);
            int attrs = in.readUnsignedShort();
            for (int a = 0; a < attrs; a++) {
                in.skipBytes(2);
                in.skipBytes(in.readInt());
            }
        }
    }

    private static int u2(byte[] b, int i) {
        return ((b[i] & 0xff) << 8) | (b[i + 1] & 0xff);
    }

    private static int s4(byte[] b, int i) {
        return (b[i] << 24) | ((b[i + 1] & 0xff) << 16) | ((b[i + 2] & 0xff) << 8) | (b[i + 3] & 0xff);
    }
}
