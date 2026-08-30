package forge.util;

public interface ITranslatable extends IHasName {
    default String getTranslationKey() {
        return getName();
    }

    //Fallback methods - used if no translation is found for the given key.

    default String getUntranslatedName() {
        return getName();
    }
    default String getTranslatedName() {
        return getName();
    }

    default String getUntranslatedType() {
        return "";
    }
}
