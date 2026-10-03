package forge.game.ability;

/**
 * Implemented by classes that declare the card-script params they read, in public static final fields:
 * <ul>
 *   <li>{@code String[] OPTIONAL_PARAMS}: params a script may set;</li>
 *   <li>{@code String[][] REQUIRED_PARAMS} (effects): groups of params, one of each group must be set;</li>
 *   <li>{@code String[] INTERNAL_PARAMS}: params the engine sets itself (keyword abilities and the
 *       like), which a script shouldn't.</li>
 * </ul>
 * The framework classes' declarations apply to every ability; an effect's add to them. The card-script
 * linter reads them, and CardScriptParamDeclarationTest keeps them in sync with the code.
 *
 * A declaration belongs to the class that makes it: the accessors don't look at superclasses, and
 * return null for a class that declares nothing of that kind.
 */
public interface IHasForgeParams {

    static String[] optionalParams(Class<?> c) {
        return declared(c, "OPTIONAL_PARAMS", String[].class);
    }

    static String[][] requiredParams(Class<?> c) {
        return declared(c, "REQUIRED_PARAMS", String[][].class);
    }

    static String[] internalParams(Class<?> c) {
        return declared(c, "INTERNAL_PARAMS", String[].class);
    }

    private static <T> T declared(Class<?> c, String name, Class<T> type) {
        try {
            return type.cast(c.getDeclaredField(name).get(null));
        } catch (NoSuchFieldException e) {
            return null;
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(c.getName() + "." + name + " must be public", e);
        }
    }
}
