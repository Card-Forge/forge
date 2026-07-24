import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * MobiVM bridge pass: makes JvmDowngrader(-c 52) output link against MobiVM's
 * Java-7-era runtime by (a) whole-type remapping of classes absent from the
 * runtime to their streamsupport/backport mirrors (java.util.function.* ->
 * java8.util.function.*, java.util.stream.* -> java8.util.stream.*, Optional,
 * CompletableFuture, ...), and (b) hierarchy-aware member redirection of
 * Java-8 default/static members on classes that DO exist (Map.getOrDefault ->
 * java8.util.Maps.getOrDefault(map, ...), Comparator.comparing ->
 * java8.util.Comparators.comparing, ...). Also rewrites method-reference
 * Handles inside invokedynamic bootstrap args with the same member table.
 *
 * A member is only redirected if it does NOT resolve against the index
 * (runtime + supplies + all app jars), so app classes that define their own
 * getOrDefault etc. are left untouched. Unresolved references with no rule
 * are collected and printed (the residual porting workload; final gate is
 * MobiVmLinkAudit).
 *
 * Rules file format (# comments, blank lines ok):
 *   type <internalNamePrefix> <replacementPrefix>
 *   member <declaringOwner> <name> <targetOwner> [PREPEND <receiverDesc>]
 *
 * Usage:
 *   MobiVmBridge --rules bridge.cfg --index rt.jar,supply1.jar,... \
 *                --in in.jar --out out.jar
 * Multiple --in/--out pairs allowed (order-paired); index should include ALL
 * app jars so cross-jar overrides resolve.
 */
public class MobiVmBridge {

    // ---- index (same resolution model as MobiVmLinkAudit) ----
    static class ClassInfo {
        String superName;
        String[] interfaces;
        final Set<String> methods = new HashSet<>();
        final Set<String> fields = new HashSet<>();
    }
    static final Map<String, ClassInfo> INDEX = new HashMap<>();

    // ---- rules ----
    static final List<String[]> TYPE_PREFIXES = new ArrayList<>(); // [from, to], longest-first
    // declaringOwner -> name -> {targetOwner, prependDescOrNull}
    static final Map<String, Map<String, String[]>> MEMBER_RULES = new HashMap<>();

    static final Map<String, List<String>> UNRESOLVED = new TreeMap<>();

    // jvmdg stub members reachable from forge app code that are verified MobiVM-safe (no broken
    // <clinit> that loads a MobiVM-absent class) and so intentionally left unbridged. Any OTHER
    // forge-reachable unbridged jvmdg stub member trips the build gate below — it is the
    // J_U_S_Stream.toList class of latent iOS crash. Audited 2026-07-10. Add here only after
    // decompiling the stub's <clinit>/impl and confirming it cannot throw on MobiVM.
    static final Set<String> GATE_ALLOWLIST = new HashSet<>(Arrays.asList(
        "xyz/wagyourtail/jvmdg/j11/stub/java_base/J_U_F_Predicate.not",
        "xyz/wagyourtail/jvmdg/j11/stub/java_base/J_U_Optional.isEmpty",
        "xyz/wagyourtail/jvmdg/j9/stub/java_base/J_U_Objects.requireNonNullElseGet",
        "xyz/wagyourtail/jvmdg/j9/stub/java_base/J_U_C_CompletableFuture.completeOnTimeout"
    ));

    static int redirects = 0;
    static int handleRedirects = 0;
    static int serializableLambdaFixes = 0;
    static int servicesRemapped = 0;

    public static void main(String[] args) throws Exception {
        List<String> ins = new ArrayList<>();
        List<String> outs = new ArrayList<>();
        String rules = null;
        List<String> index = new ArrayList<>();
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--rules": rules = args[++i]; break;
                case "--index": for (String s : args[++i].split(",")) index.add(s); break;
                case "--in": ins.add(args[++i]); break;
                case "--out": outs.add(args[++i]); break;
                default: throw new IllegalArgumentException(args[i]);
            }
        }
        if (rules != null) loadRules(rules);
        for (String jar : index) indexJar(jar);

        for (int i = 0; i < ins.size(); i++) {
            transform(Paths.get(ins.get(i)), Paths.get(outs.get(i)));
        }
        System.out.println("BRIDGE: " + redirects + " member redirects, " + handleRedirects
                + " handle redirects, " + serializableLambdaFixes + " serializable-lambda markers, "
                + servicesRemapped + " services entries remapped across " + ins.size() + " jar(s)");
        if (!UNRESOLVED.isEmpty()) {
            System.out.println("BRIDGE-UNRESOLVED (" + UNRESOLVED.size() + ") — no rule, left as-is:");
            for (Map.Entry<String, List<String>> e : UNRESOLVED.entrySet()) {
                System.out.println("  " + e.getKey() + "  <- " + e.getValue().size()
                        + " refs, e.g. " + e.getValue().get(0));
            }
        }

        // Build gate: a jvmdg stub METHOD reachable from forge app code with no bridge rule is the
        // J_U_S_Stream.toList class of latent iOS crash — the stub executes on device and its
        // <clinit>/impl may load a MobiVM-absent class (e.g. java.util.stream.ReferencePipeline) and
        // throw. Flag these with a distinct marker; the pipeline greps for it and fails the build.
        List<String> gateFails = new ArrayList<>();
        for (Map.Entry<String, List<String>> e : UNRESOLVED.entrySet()) {
            String key = e.getKey();
            if (!key.contains("xyz/wagyourtail/jvmdg/") || !key.startsWith("METHOD ")) {
                continue;
            }
            String forgeRef = firstForgeRef(e.getValue());
            if (forgeRef == null) {
                continue; // only jvmdg-internal referrers -> inert on device
            }
            boolean allowed = false;
            for (String a : GATE_ALLOWLIST) {
                if (key.contains(a)) { allowed = true; break; }
            }
            if (!allowed) {
                gateFails.add(key + "  <- e.g. " + forgeRef);
            }
        }
        if (!gateFails.isEmpty()) {
            System.out.println("BRIDGE-GATE-FAIL (" + gateFails.size()
                    + ") — unbridged jvmdg stub member(s) reachable from forge code (would crash on device like J_U_S_Stream.toList):");
            for (String k : gateFails) {
                System.out.println("  " + k);
            }
            System.out.println("Fix each: add a bridge.cfg 'member' rule redirecting it to a forge.compat helper"
                    + " (see forge.compat.JStream8), OR add it to MobiVmBridge.GATE_ALLOWLIST after decompiling"
                    + " its <clinit>/impl and confirming it cannot throw on MobiVM.");
        }
    }

    /** First forge/* referrer in the list, or null if none (jvmdg-internal referrers are inert on device). */
    static String firstForgeRef(List<String> refs) {
        for (String r : refs) {
            if (r.startsWith("forge/")) {
                return r;
            }
        }
        return null;
    }

    static void loadRules(String path) throws Exception {
        for (String line : Files.readAllLines(Paths.get(path), StandardCharsets.UTF_8)) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            String[] p = line.split("\\s+");
            if ("type".equals(p[0])) {
                TYPE_PREFIXES.add(new String[]{p[1], p[2]});
            } else if ("member".equals(p[0])) {
                String prepend = null;
                if (p.length >= 6 && "PREPEND".equals(p[4])) prepend = p[5];
                MEMBER_RULES.computeIfAbsent(p[1], k -> new HashMap<>())
                        .put(p[2], new String[]{p[3], prepend});
            } else {
                throw new IllegalArgumentException("bad rule: " + line);
            }
        }
        // longest prefix first so java/util/OptionalInt beats java/util/Optional if both listed
        TYPE_PREFIXES.sort((a, b) -> b[0].length() - a[0].length());
    }

    static void indexJar(String jarPath) throws Exception {
        try (ZipFile zf = new ZipFile(jarPath)) {
            java.util.Enumeration<? extends ZipEntry> en = zf.entries();
            while (en.hasMoreElements()) {
                ZipEntry e = en.nextElement();
                if (!e.getName().endsWith(".class") || e.getName().startsWith("META-INF/versions/")) continue;
                try (InputStream is = zf.getInputStream(e)) {
                    ClassReader cr = new ClassReader(is);
                    final ClassInfo ci = new ClassInfo();
                    cr.accept(new ClassVisitor(Opcodes.ASM9) {
                        @Override
                        public void visit(int v, int a, String name, String sig, String sup, String[] ifs) {
                            ci.superName = sup;
                            ci.interfaces = ifs;
                            INDEX.put(name, ci);
                        }
                        @Override
                        public MethodVisitor visitMethod(int a, String name, String desc, String sig, String[] ex) {
                            ci.methods.add(name + desc);
                            return null;
                        }
                        @Override
                        public FieldVisitor visitField(int a, String name, String desc, String sig, Object val) {
                            ci.fields.add(name);
                            return null;
                        }
                    }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                }
            }
        }
    }

    static boolean isTracked(String owner) {
        return owner.startsWith("java/") || owner.startsWith("javax/");
    }

    static boolean methodResolves(String owner, String nameDesc) {
        if (owner.startsWith("[")) return true;
        Set<String> seen = new HashSet<>();
        List<String> work = new ArrayList<>();
        work.add(owner);
        while (!work.isEmpty()) {
            String c = work.remove(work.size() - 1);
            if (c == null || !seen.add(c)) continue;
            ClassInfo ci = INDEX.get(c);
            if (ci == null) continue;
            if (ci.methods.contains(nameDesc)) return true;
            if (ci.superName != null) work.add(ci.superName);
            if (ci.interfaces != null) for (String i : ci.interfaces) work.add(i);
        }
        return false;
    }

    /** Walk owner's hierarchy for a member rule matching this method name. */
    static String[] findRule(String owner, String name) {
        Set<String> seen = new HashSet<>();
        List<String> work = new ArrayList<>();
        work.add(owner);
        while (!work.isEmpty()) {
            String c = work.remove(work.size() - 1);
            if (c == null || !seen.add(c)) continue;
            Map<String, String[]> byName = MEMBER_RULES.get(c);
            if (byName != null) {
                String[] r = byName.get(name);
                if (r != null) return r;
            }
            ClassInfo ci = INDEX.get(c);
            if (ci != null) {
                if (ci.superName != null) work.add(ci.superName);
                if (ci.interfaces != null) for (String i : ci.interfaces) work.add(i);
            }
        }
        return null;
    }

    static void reportUnresolved(String key, String where) {
        List<String> l = UNRESOLVED.get(key);
        if (l == null) {
            l = new ArrayList<>();
            UNRESOLVED.put(key, l);
        }
        l.add(where);
    }

    // ---- the type remapper ----
    static final Remapper TYPE_REMAPPER = new Remapper() {
        @Override
        public String map(String internalName) {
            for (String[] p : TYPE_PREFIXES) {
                if (internalName.startsWith(p[0])) {
                    return p[1] + internalName.substring(p[0].length());
                }
            }
            return internalName;
        }
    };

    // ---- transform one jar ----
    static void transform(Path in, Path out) throws Exception {
        try (ZipFile zin = new ZipFile(in.toFile());
             ZipOutputStream zout = new ZipOutputStream(Files.newOutputStream(out))) {
            java.util.Enumeration<? extends ZipEntry> en = zin.entries();
            while (en.hasMoreElements()) {
                ZipEntry e = en.nextElement();
                if (e.isDirectory()) continue;
                String n = e.getName();
                // module descriptors are useless to RoboVM; stale jar signatures
                // would be invalidated by the rewrite anyway
                if (n.endsWith("module-info.class") || n.startsWith("META-INF/versions/")
                        || (n.startsWith("META-INF/") && (n.endsWith(".SF") || n.endsWith(".RSA") || n.endsWith(".DSA") || n.endsWith(".EC")))) {
                    continue;
                }
                try (InputStream is = zin.getInputStream(e)) {
                    if (e.getName().endsWith(".class") && !e.getName().startsWith("META-INF/versions/")) {
                        byte[] rewritten = rewriteClass(is);
                        // the entry path must track the (possibly type-remapped)
                        // class name — RoboVM locates classes by jar entry path
                        String cls = new ClassReader(rewritten).getClassName();
                        zout.putNextEntry(new ZipEntry(cls + ".class"));
                        zout.write(rewritten);
                    } else if (n.startsWith("META-INF/services/") && n.length() > "META-INF/services/".length()) {
                        // ServiceLoader registrations: the entry NAME is the service
                        // interface and each content line is an impl class — both are
                        // dotted class names that must track the type-remap rules.
                        // (Miss this and e.g. the relocated ThreeTen ZoneRulesProvider
                        // never registers: "No time-zone data files registered".)
                        String svc = n.substring("META-INF/services/".length());
                        String newSvc = TYPE_REMAPPER.map(svc.replace('.', '/')).replace('/', '.');
                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        byte[] buf = new byte[65536];
                        int r;
                        while ((r = is.read(buf)) > 0) bos.write(buf, 0, r);
                        String content = new String(bos.toByteArray(), StandardCharsets.UTF_8);
                        StringBuilder sb = new StringBuilder();
                        for (String line : content.split("\n", -1)) {
                            String t = line.trim();
                            if (!t.isEmpty() && !t.startsWith("#")) {
                                t = TYPE_REMAPPER.map(t.replace('.', '/')).replace('/', '.');
                            }
                            sb.append(t).append('\n');
                        }
                        String newContent = sb.toString();
                        if (!newSvc.equals(svc) || !newContent.equals(content)) {
                            servicesRemapped++;
                        }
                        zout.putNextEntry(new ZipEntry("META-INF/services/" + newSvc));
                        zout.write(newContent.getBytes(StandardCharsets.UTF_8));
                    } else {
                        zout.putNextEntry(new ZipEntry(e.getName()));
                        byte[] buf = new byte[65536];
                        int r;
                        while ((r = is.read(buf)) > 0) zout.write(buf, 0, r);
                    }
                }
                zout.closeEntry();
            }
        }
    }

    static byte[] rewriteClass(InputStream is) throws Exception {
        ClassReader cr = new ClassReader(is);
        ClassWriter cw = new ClassWriter(0);
        // chain: reader -> member redirector -> type remapper -> writer
        ClassVisitor remap = new ClassRemapper(cw, TYPE_REMAPPER);
        ClassVisitor redirect = new MemberRedirector(remap, cr.getClassName());
        cr.accept(redirect, ClassReader.EXPAND_FRAMES);
        return cw.toByteArray();
    }

    /**
     * RoboVM's AOT lambda plugin (LambdaPlugin/LambdaClassGenerator) implements
     * only FLAG_MARKERS(2) and FLAG_BRIDGES(4) of LambdaMetafactory.altMetafactory
     * — FLAG_SERIALIZABLE(1) is silently discarded, so a serializable lambda
     * comes out NOT implementing Serializable and the compiler-emitted
     * `checkcast java/io/Serializable` throws at runtime (first hit: jgrapht
     * SupplierUtil.<clinit> via GameAction.findStaticAbilityToApply — killed
     * every game). Convert the ignored bit into an explicit marker interface,
     * which RoboVM demonstrably honors. Bit 1 is left set: RoboVM ignores it
     * either way and flags=7 + explicit marker is the (spec-valid) historical
     * ECJ encoding, so the transformed jars still run on real JVMs.
     *
     * Arg layout: [samMT, implMH, instMT, flags,
     *              (markerCount, markers...)?, (bridgeCount, bridges...)?]
     *
     * Note: this runs BEFORE the ClassRemapper in the visitor chain, so a
     * marker matching a type rule would be remapped downstream — that would
     * be correct behavior, not a bug.
     */
    static Object[] fixSerializableLambda(Handle bsm, Object[] args) {
        if (!"java/lang/invoke/LambdaMetafactory".equals(bsm.getOwner())
                || !"altMetafactory".equals(bsm.getName())
                || args.length < 4 || !(args[3] instanceof Integer)) {
            return args;
        }
        int flags = (Integer) args[3];
        if ((flags & 1) == 0) { // FLAG_SERIALIZABLE not set
            return args;
        }
        Type ser = Type.getObjectType("java/io/Serializable");
        List<Object> a = new ArrayList<>(Arrays.asList(args));
        if ((flags & 2) != 0) { // FLAG_MARKERS present (javac-style encoding)
            int mc = (Integer) a.get(4);
            if (a.subList(5, 5 + mc).contains(ser)) {
                return args; // already explicit
            }
            a.set(4, mc + 1);
            a.add(5 + mc, ser);
        } else { // ECJ-style: bit 1 only — splice markers before the bridge payload
            a.set(3, flags | 2);
            a.add(4, 1);
            a.add(5, ser);
        }
        serializableLambdaFixes++;
        return a.toArray();
    }

    static class MemberRedirector extends ClassVisitor {
        final String self;

        MemberRedirector(ClassVisitor cv, String self) {
            super(Opcodes.ASM9, cv);
            this.self = self;
        }

        @Override
        public MethodVisitor visitMethod(int acc, String mname, String mdesc, String sig, String[] ex) {
            MethodVisitor mv = super.visitMethod(acc, mname, mdesc, sig, ex);
            final String where = self + "." + mname;
            return new MethodVisitor(Opcodes.ASM9, mv) {
                @Override
                public void visitMethodInsn(int op, String owner, String name, String desc, boolean itf) {
                    String[] rule = null;
                    if (INDEX.containsKey(owner)) {
                        // ANY indexed owner (incl. app classes): a call to a member
                        // that no longer resolves is usually a Java 8 default method
                        // inherited from a java.util interface (e.g.
                        // forge CardCollection.forEach) — the hierarchy walk finds
                        // the interface's rule
                        if (!methodResolves(owner, name + desc)) {
                            rule = findRule(owner, name);
                            if (rule == null) {
                                reportUnresolved("METHOD " + owner + "." + name + desc, where);
                            }
                        }
                    } else if (isTracked(owner)) {
                        // owner class absent from the index (whole-type-remapped,
                        // e.g. java.util.stream.Stream): interface statics live in
                        // companion classes (RefStreams etc.) — exact-owner rules
                        Map<String, String[]> byName = MEMBER_RULES.get(owner);
                        if (byName != null) {
                            rule = byName.get(name);
                        }
                    }
                    if (rule != null) {
                        redirects++;
                        String newDesc = (op == Opcodes.INVOKESTATIC || rule[1] == null)
                                ? desc : "(" + rule[1] + desc.substring(1);
                        super.visitMethodInsn(Opcodes.INVOKESTATIC, rule[0], name, newDesc, false);
                        return;
                    }
                    super.visitMethodInsn(op, owner, name, desc, itf);
                }

                @Override
                public void visitInvokeDynamicInsn(String name, String desc, Handle bsm, Object... args) {
                    Object[] newArgs = new Object[args.length];
                    for (int i = 0; i < args.length; i++) {
                        Object a = args[i];
                        if (a instanceof Handle) {
                            Handle h = (Handle) a;
                            String hOwner = h.getOwner();
                            boolean hKnown = INDEX.containsKey(hOwner);
                            if ((hKnown && !methodResolves(hOwner, h.getName() + h.getDesc()))
                                    || (!hKnown && isTracked(hOwner))) {
                                String[] rule;
                                if (hKnown) {
                                    rule = findRule(hOwner, h.getName());
                                } else {
                                    Map<String, String[]> byName = MEMBER_RULES.get(hOwner);
                                    rule = byName == null ? null : byName.get(h.getName());
                                }
                                if (rule != null) {
                                    handleRedirects++;
                                    String hDesc = (h.getTag() == Opcodes.H_INVOKESTATIC || rule[1] == null)
                                            ? h.getDesc() : "(" + rule[1] + h.getDesc().substring(1);
                                    a = new Handle(Opcodes.H_INVOKESTATIC, rule[0], h.getName(), hDesc, false);
                                } else if (hKnown) {
                                    reportUnresolved("HANDLE " + hOwner + "." + h.getName() + h.getDesc(), where);
                                }
                            }
                        }
                        newArgs[i] = a;
                    }
                    Object[] outArgs = fixSerializableLambda(bsm, newArgs);
                    super.visitInvokeDynamicInsn(name, desc, bsm, outArgs);
                }
            };
        }
    }
}
