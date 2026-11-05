/**
 * Whole-type remap targets for java.lang.management (absent on MobiVM).
 * The iOS bridge pass (tmp/jvmdg/bridge.cfg) remaps the entire
 * java.lang.management package prefix here; apfloat's ApfloatContext and
 * tinylog's runtime provider probe these at boot.
 */
package forge.compat.mgmt;
