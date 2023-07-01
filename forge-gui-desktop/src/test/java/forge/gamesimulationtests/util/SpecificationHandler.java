package forge.gamesimulationtests.util;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Iterables;

public abstract class SpecificationHandler<TYPE, SPECIFICATION extends Specification<TYPE>> {
    public final TYPE find(Iterable<TYPE> items, final SPECIFICATION specification) {
        return find(items, specification, IntegerConstraint.ONE);
    }

    public final TYPE find(Iterable<TYPE> items, final SPECIFICATION specification, final IntegerConstraint expectedNumberOfResults) {
        Iterable<TYPE> matches = findMatches(items, specification, expectedNumberOfResults);
        return Iterables.getFirst(matches, null);
    }

    public final Iterable<TYPE> findMatches(Iterable<TYPE> items, final SPECIFICATION specification) {
        return findMatches(items, specification, IntegerConstraint.ZERO_OR_MORE);
    }

    public final Iterable<TYPE> findMatches(Iterable<TYPE> items, final SPECIFICATION specification, final IntegerConstraint expectedNumberOfResults) {
        List<TYPE> matches = new ArrayList<>();
        for (TYPE item : items) {
            if (matches(item, specification)) {
                matches.add(item);
            }
        }
        if (!expectedNumberOfResults.matches(matches.size())) {
            throw new IllegalStateException("Expected " + expectedNumberOfResults + " results but got " + matches.size() + " in [" + StringUtils.join(items, ", ") + "]");
        }
        return matches;
    }
    
    public abstract boolean matches(TYPE item, final SPECIFICATION specification);
}
