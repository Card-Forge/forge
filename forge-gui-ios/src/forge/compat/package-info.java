/**
 * Bytecode-rewrite targets for the iOS MobiVM build pipeline (tmp/jvmdg).
 *
 * <p>MobiVM's runtime library is Java-7-era: it lacks java.util.function,
 * java.util.stream, Optional, java.time, java.nio.file, and the Java 8
 * default/static members on Map, List, Collection, Comparator, String, etc.
 * The pipeline (JvmDowngrader -c 52 + MobiVmBridge) rewrites call sites in
 * every app jar to the classes in this package (and to streamsupport /
 * app-supplied java.* classes). Nothing references these classes from source;
 * desktop and Android builds never load them.</p>
 *
 * <p>Bodies must be self-implemented: they must not call the very APIs they
 * replace, since this module's classes are themselves bridged.</p>
 */
package forge.compat;
