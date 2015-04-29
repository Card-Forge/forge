package forge.util;

import com.google.common.collect.ImmutableList;

/**
 * Static library containing methods related to enums.
 */
public final class EnumUtil {

    /**
     * Private constructor to prevent instantiation.
     */
    private EnumUtil() {
    }

    /**
     * Get the names of an enum's values.
     *
     * @param enumType
     *            the enum class.
     * @return an {@link ImmutableList} containing the names of the values of
     *         {@code enumType}, in the order they were declared.
     * @see Enum#name()
     */
    public static ImmutableList<String> getNames(final Class<? extends Enum<?>> enumType) {
        final ImmutableList.Builder<String> builder = ImmutableList.builder();
        for (final Enum<?> type : enumType.getEnumConstants()) {
            builder.add(type.name());
        }
        return builder.build();
    }
}
