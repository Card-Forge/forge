package forge.util;

import com.google.common.collect.ImmutableList;

public final class EnumUtil {

    private EnumUtil() {
    }

    public static ImmutableList<String> getNames(final Class<? extends Enum<?>> enumType) {
        final ImmutableList.Builder<String> builder = ImmutableList.builder();
        for (final Enum<?> type : enumType.getEnumConstants()) {
            builder.add(type.name());
        }
        return builder.build();
    }
}
