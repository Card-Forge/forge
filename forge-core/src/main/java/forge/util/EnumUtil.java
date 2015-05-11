package forge.util;

import com.google.common.collect.ImmutableList;

public final class EnumUtil {

    private EnumUtil() {
    }

    /**
     * Get the names of the values of an enum type.
     * 
     * @param enumType
     *            an {@link Enum} type.
     * @return an {@link ImmutableList} of strings representing the names of the
     *         enum's values.
     */
    public static ImmutableList<String> getNames(final Class<? extends Enum<?>> enumType) {
        final ImmutableList.Builder<String> builder = ImmutableList.builder();
        for (final Enum<?> type : enumType.getEnumConstants()) {
            builder.add(type.name());
        }
        return builder.build();
    }
}
