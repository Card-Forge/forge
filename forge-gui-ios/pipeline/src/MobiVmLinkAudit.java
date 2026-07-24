import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * MobiVM linkage audit: scans app jars (AFTER pre-pass + JvmDowngrader) for
 * references to java/* / javax/* members that do NOT exist in MobiVM's
 * runtime (robovm-rt.jar) or in the app-supplied java.* classes
 * (java-function-stubs, java.time supply). Every hit is a guaranteed
 * NoClassDefFoundError / NoSuchMethodError if that code path executes on iOS.
 *
 * This replaces the manual CLAUDE.md grep scan: run it after every upstream
 * merge and the output IS the remaining porting workload (usually empty).
 *
 * Usage: MobiVmLinkAudit --rt rt1.jar[,rt2.jar,...] --scan app1.jar[,app2.jar,...]
 * Output: one line per missing member: KIND owner.member(desc) <- count refs [sample referencer]
 */
public class MobiVmLinkAudit {

    static class ClassInfo {
        String superName;
        String[] interfaces;
        final Set<String> methods = new HashSet<>(); // name + desc
        final Set<String> fields = new HashSet<>();  // name
    }

    static final Map<String, ClassInfo> INDEX = new HashMap<>();
    static final Map<String, List<String>> MISSING = new TreeMap<>();
    static final Set<String> SCANNED_CLASSES = new HashSet<>();
    // jar!entry -> class names referenced (service interface + impl lines)
    static final Map<String, List<String>> SERVICES = new TreeMap<>();

    public static void main(String[] args) throws Exception {
        List<String> rtJars = new ArrayList<>();
        List<String> scanJars = new ArrayList<>();
        for (int i = 0; i < args.length; i++) {
            if ("--rt".equals(args[i])) {
                for (String s : args[++i].split(",")) rtJars.add(s);
            } else if ("--scan".equals(args[i])) {
                for (String s : args[++i].split(",")) scanJars.add(s);
            }
        }
        for (String jar : rtJars) index(jar);
        for (String jar : scanJars) scan(jar);
        checkServices();

        if (MISSING.isEmpty()) {
            System.out.println("AUDIT CLEAN: no unresolved java/* references");
            return;
        }
        System.out.println("AUDIT: " + MISSING.size() + " unresolved java/javax references:");
        for (Map.Entry<String, List<String>> e : MISSING.entrySet()) {
            List<String> refs = e.getValue();
            System.out.println(e.getKey() + "  <- " + refs.size() + " refs, e.g. " + refs.get(0));
        }
        System.exit(2);
    }

    // ---- indexing the runtime ----

    static void index(String jarPath) throws Exception {
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
                        public void visit(int v, int acc, String name, String sig, String sup, String[] ifs) {
                            ci.superName = sup;
                            ci.interfaces = ifs;
                            INDEX.put(name, ci);
                        }
                        @Override
                        public MethodVisitor visitMethod(int acc, String name, String desc, String sig, String[] ex) {
                            ci.methods.add(name + desc);
                            return null;
                        }
                        @Override
                        public FieldVisitor visitField(int acc, String name, String desc, String sig, Object val) {
                            ci.fields.add(name);
                            return null;
                        }
                    }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                }
            }
        }
    }

    // ---- resolution ----

    static boolean isTracked(String owner) {
        // java8/* (streamsupport), forge/compat/* and jvmdg stubs are bridge
        // targets: verifying them catches redirects to nonexistent members
        // (e.g. interface statics absent from streamsupport interfaces)
        return owner.startsWith("java/") || owner.startsWith("javax/")
                || owner.startsWith("java8/") || owner.startsWith("forge/compat/")
                || owner.startsWith("xyz/wagyourtail/jvmdg/")
                || owner.startsWith("io/sentry/");
    }

    static boolean classExists(String name) {
        return INDEX.containsKey(name);
    }

    static boolean methodExists(String owner, String nameDesc) {
        if (owner.startsWith("[")) return true; // arrays: clone() etc.
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

    static boolean fieldExists(String owner, String name) {
        Set<String> seen = new HashSet<>();
        List<String> work = new ArrayList<>();
        work.add(owner);
        while (!work.isEmpty()) {
            String c = work.remove(work.size() - 1);
            if (c == null || !seen.add(c)) continue;
            ClassInfo ci = INDEX.get(c);
            if (ci == null) continue;
            if (ci.fields.contains(name)) return true;
            if (ci.superName != null) work.add(ci.superName);
            if (ci.interfaces != null) for (String i : ci.interfaces) work.add(i);
        }
        return false;
    }

    static void report(String key, String referencer) {
        List<String> l = MISSING.get(key);
        if (l == null) {
            l = new ArrayList<>();
            MISSING.put(key, l);
        }
        l.add(referencer);
    }

    /**
     * A META-INF/services registration naming a class that exists nowhere on
     * the final classpath is a silently-broken service: ServiceLoader finds
     * zero providers (wrong entry NAME, e.g. unremapped after a relocation) or
     * throws ServiceConfigurationError (wrong impl line). First real instance:
     * the relocated ThreeTen zone-rules provider — java.time worked until the
     * first ZoneId.systemDefault(), which threw "No time-zone data files
     * registered" because the services entry still used org.threeten.bp names.
     */
    static void checkServices() {
        for (Map.Entry<String, List<String>> e : SERVICES.entrySet()) {
            for (String dotted : e.getValue()) {
                String internal = dotted.replace('.', '/');
                if (!INDEX.containsKey(internal) && !SCANNED_CLASSES.contains(internal)) {
                    report("SERVICE-UNRESOLVED " + dotted, e.getKey());
                }
            }
        }
    }

    // ---- scanning app jars ----

    static void scan(String jarPath) throws Exception {
        try (ZipFile zf = new ZipFile(jarPath)) {
            java.util.Enumeration<? extends ZipEntry> en = zf.entries();
            while (en.hasMoreElements()) {
                ZipEntry e = en.nextElement();
                String entryName = e.getName();
                if (entryName.startsWith("META-INF/services/")
                        && entryName.length() > "META-INF/services/".length() && !e.isDirectory()) {
                    // collect ServiceLoader registrations; verified after the full
                    // scan (a registration naming a class that exists nowhere on the
                    // final classpath = silently-broken service, e.g. an unremapped
                    // provider name after a relocation)
                    List<String> names = new ArrayList<>();
                    names.add(entryName.substring("META-INF/services/".length()));
                    try (InputStream is = zf.getInputStream(e)) {
                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        byte[] buf = new byte[8192];
                        int r;
                        while ((r = is.read(buf)) > 0) bos.write(buf, 0, r);
                        for (String line : new String(bos.toByteArray(), StandardCharsets.UTF_8).split("\n")) {
                            String t = line.trim();
                            if (!t.isEmpty() && !t.startsWith("#")) names.add(t);
                        }
                    }
                    SERVICES.put(jarPath + "!" + entryName, names);
                    continue;
                }
                if (!entryName.endsWith(".class") || entryName.startsWith("META-INF/versions/")) continue;
                try (InputStream is = zf.getInputStream(e)) {
                    ClassReader cr = new ClassReader(is);
                    final String self = cr.getClassName();
                    SCANNED_CLASSES.add(self);
                    cr.accept(new ClassVisitor(Opcodes.ASM9) {
                        @Override
                        public MethodVisitor visitMethod(int acc, String mname, String mdesc, String sig, String[] ex) {
                            final String where = self + "." + mname;
                            return new MethodVisitor(Opcodes.ASM9) {
                                @Override
                                public void visitMethodInsn(int op, String owner, String name, String desc, boolean itf) {
                                    if (classExists(owner)) {
                                        // ANY indexed owner: unresolved member = will
                                        // NoSuchMethodError if executed (e.g. Java 8
                                        // default inherited through an app class)
                                        if (!methodExists(owner, name + desc)) {
                                            report("METHOD " + owner + "." + name + desc, where);
                                        }
                                    } else if (isTracked(owner)) {
                                        report("CLASS " + owner, where);
                                        // member-level demand: sizes the supply needed
                                        report("MISSING-MEMBER " + owner + "." + name + desc, where);
                                    }
                                }
                                @Override
                                public void visitFieldInsn(int op, String owner, String name, String desc) {
                                    if (isTracked(owner)) {
                                        if (!classExists(owner)) {
                                            report("CLASS " + owner, where);
                                        } else if (!fieldExists(owner, name)) {
                                            report("FIELD " + owner + "." + name, where);
                                        }
                                    }
                                }
                                @Override
                                public void visitTypeInsn(int op, String type) {
                                    if (isTracked(type) && !type.startsWith("[") && !classExists(type)) {
                                        report("CLASS " + type, where);
                                    }
                                }
                                @Override
                                public void visitLdcInsn(Object v) {
                                    if (v instanceof Type) {
                                        Type t = (Type) v;
                                        if (t.getSort() == Type.OBJECT && isTracked(t.getInternalName())
                                                && !classExists(t.getInternalName())) {
                                            report("CLASS " + t.getInternalName(), where);
                                        }
                                    }
                                }
                                @Override
                                public void visitInvokeDynamicInsn(String iname, String idesc, Handle bsm, Object... bsmArgs) {
                                    // RoboVM's lambda plugin ignores altMetafactory
                                    // FLAG_SERIALIZABLE(1); MobiVmBridge compensates by
                                    // adding an explicit java/io/Serializable marker under
                                    // FLAG_MARKERS(2). Any site reaching the final jars
                                    // with bit 1 set but no marker WILL throw CCE on the
                                    // compiler-emitted `checkcast java/io/Serializable`.
                                    if ("java/lang/invoke/LambdaMetafactory".equals(bsm.getOwner())
                                            && "altMetafactory".equals(bsm.getName())
                                            && bsmArgs.length >= 4 && bsmArgs[3] instanceof Integer) {
                                        int flags = (Integer) bsmArgs[3];
                                        if ((flags & 1) != 0) {
                                            boolean hasMarker = false;
                                            if ((flags & 2) != 0 && bsmArgs.length >= 5 && bsmArgs[4] instanceof Integer) {
                                                int mc = (Integer) bsmArgs[4];
                                                for (int m = 0; m < mc && 5 + m < bsmArgs.length; m++) {
                                                    if (bsmArgs[5 + m] instanceof Type
                                                            && "java/io/Serializable".equals(((Type) bsmArgs[5 + m]).getInternalName())) {
                                                        hasMarker = true;
                                                    }
                                                }
                                            }
                                            if (!hasMarker) {
                                                report("SERIALIZABLE-LAMBDA (FLAG_SERIALIZABLE without explicit "
                                                        + "Serializable marker; RoboVM drops it -> runtime CCE)", where);
                                            }
                                        }
                                    }
                                }
                            };
                        }
                    }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                }
            }
        }
    }
}
