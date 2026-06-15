package forge.game.ability;

/**
 * Implemented by classes that declare the card-script parameters they consume: the optional params
 * in a {@code public static final String[] OPTIONAL_PARAMS} field, and (for effects) mutually-required
 * groups in a {@code String[][] REQUIRED_PARAMS} field. The accessors expose those fields so the
 * card-script linter can ask a class which params it accepts; CardScriptParamDeclarationTest discovers
 * implementors by classpath scan and guards the declarations against drift.
 *
 * The accessors read the field of the runtime class ({@code getClass()}) rather than returning a field
 * directly. Because the fields are {@code static}, a direct {@code return OPTIONAL_PARAMS} inherited by a
 * subclass would return the superclass's field; reading {@code getClass()}'s field returns the subclass's
 * own declaration (or the inherited one when it declares none), so no per-class override is needed.
 */
public interface IHasForgeParams {
    default String[] getOptionalParams() {
        try {
            return (String[]) getClass().getField("OPTIONAL_PARAMS").get(null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            return new String[0];
        }
    }

    default String[][] getRequiredParams() {
        try {
            return (String[][]) getClass().getField("REQUIRED_PARAMS").get(null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            return new String[0][];
        }
    }
}
