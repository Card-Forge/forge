package forge.gamemodes.net;

import forge.util.IHasForgeLog;

import java.io.InvalidClassException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Allowlist for class names arriving on the multiplayer wire. The protocol is
 * Java native serialization, so without one a peer's frame can name any class
 * on the classpath.
 *
 * <p>Deliberately not an {@code ObjectInputFilter}: JEP 290 reached Android at
 * API 33 and {@code forge-gui-android} declares {@code minSdkVersion=26}, so
 * {@code setObjectInputFilter} would compile for desktop and break mobile at
 * class-load. A name check inside {@link CObjectInputStream} needs no JDK 9+
 * API, and covers both wire streams — the per-frame one and the inner one in
 * {@link TrackableSerializer#unwrapEvents} — because every class resolved
 * during deserialization passes through it.
 *
 * <p>The prefixes below are derived from measured traffic; see the commit
 * message for the derivation and the gadget-library survey behind it.
 */
final class WireClassFilter implements IHasForgeLog {

    /** {@code =off} disables enforcement, so a false reject cannot strand a game. */
    private static final String ENFORCE_PROPERTY = "forge.net.classFilter";

    /** Package prefixes accepted wholesale. */
    private static final String[] ALLOWED_PREFIXES = {
            "forge.",
            "java.util.",
            "com.google.common.collect.",
    };

    /** Simple names under {@code java.lang} we accept, plus their nested classes. */
    private static final Set<String> ALLOWED_JAVA_LANG = unmodifiableSetOf(
            "Boolean", "Byte", "Character", "Short", "Integer", "Long", "Float", "Double",
            "Number", "String", "Enum", "Object", "Void", "StringBuffer", "StringBuilder");

    /**
     * The filter's one deliberate soft spot. {@code ProtocolMethod.getChoices}
     * ships an {@code FSerializableFunction}, so real traffic carries lambdas;
     * removing this means changing that method to send rendered strings.
     */
    private static final Set<String> ALLOWED_EXACT = unmodifiableSetOf(
            "java.lang.invoke.SerializedLambda");

    private static final String JAVA_LANG = "java.lang.";
    private static final String PRIMITIVE_DESCRIPTORS = "BCDFIJSZ";

    private WireClassFilter() {
    }

    private static Set<String> unmodifiableSetOf(final String... names) {
        return Collections.unmodifiableSet(new HashSet<>(Arrays.asList(names)));
    }

    /** Whether the filter refuses disallowed classes, or merely reports them. */
    static boolean isEnforcing() {
        return !"off".equalsIgnoreCase(System.getProperty(ENFORCE_PROPERTY, "on"));
    }

    /**
     * Reject a class the protocol has no business carrying.
     *
     * @throws InvalidClassException before the class is resolved, so a gadget's
     *         static initialiser and constructor never run
     */
    static void checkAllowed(final String rawName) throws InvalidClassException {
        if (isAllowed(rawName)) {
            return;
        }
        if (!isEnforcing()) {
            netLog.warn("Wire class {} is not on the allowlist (filter disabled, allowing)", rawName);
            return;
        }
        netLog.error("Rejected wire class {} — not on the multiplayer allowlist. "
                + "If this is legitimate traffic, please report it; "
                + "-D{}=off disables enforcement.", rawName, ENFORCE_PROPERTY);
        throw new InvalidClassException(rawName, "not permitted by the multiplayer class filter");
    }

    static boolean isAllowed(final String rawName) {
        if (rawName == null || rawName.isEmpty()) {
            return false;
        }
        final String type = elementType(rawName);

        // An array of primitives, e.g. "[B" reduces to "B".
        if (type.length() == 1 && PRIMITIVE_DESCRIPTORS.indexOf(type.charAt(0)) >= 0) {
            return true;
        }
        if (ALLOWED_EXACT.contains(type)) {
            return true;
        }
        for (final String prefix : ALLOWED_PREFIXES) {
            if (type.startsWith(prefix)) {
                return true;
            }
        }
        if (type.startsWith(JAVA_LANG)) {
            String simple = type.substring(JAVA_LANG.length());
            final int nested = simple.indexOf('$');
            if (nested >= 0) {
                simple = simple.substring(0, nested);
            }
            // Reject deeper packages such as java.lang.reflect.* — only the
            // top level of java.lang is in scope here.
            return simple.indexOf('.') < 0 && ALLOWED_JAVA_LANG.contains(simple);
        }
        return false;
    }

    /**
     * Reduce an array name to the type it is an array of, so that
     * {@code "[[Ljava.io.File;"} is judged as {@code java.io.File} rather than
     * slipping through for matching no prefix in its raw descriptor form.
     */
    static String elementType(final String name) {
        int depth = 0;
        while (depth < name.length() && name.charAt(depth) == '[') {
            depth++;
        }
        if (depth == 0) {
            return name;
        }
        final String rest = name.substring(depth);
        if (rest.length() > 2 && rest.charAt(0) == 'L' && rest.endsWith(";")) {
            return rest.substring(1, rest.length() - 1);
        }
        return rest;
    }
}
