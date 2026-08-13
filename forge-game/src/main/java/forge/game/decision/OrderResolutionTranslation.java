package forge.game.decision;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Pure translation between semantic resolve-first order and native insertion order. */
public final class OrderResolutionTranslation {
    private OrderResolutionTranslation() {
    }

    public static <T> List<T> toSemanticResolveFirst(final List<T> nativeInsertionOrder) {
        return reverse(nativeInsertionOrder);
    }

    public static <T> List<T> toNativeInsertion(final List<T> semanticResolveFirstOrder) {
        return reverse(semanticResolveFirstOrder);
    }

    private static <T> List<T> reverse(final List<T> source) {
        final List<T> copy = new ArrayList<>(Objects.requireNonNull(source));
        Collections.reverse(copy);
        return List.copyOf(copy);
    }
}
