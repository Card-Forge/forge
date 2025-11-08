package forge;

import com.google.common.collect.Lists;

import java.util.List;

/*
 * A class that contains definitions of available Mulligan rule variants and helper methods to access them
 */
public class MulliganDefs {
    public enum MulliganRule {
        Original {
            @Override
            public int getModifiedHandSize(int startingHandSize) {
                return startingHandSize;
            }
        },
        Paris {
            @Override
            public int getModifiedHandSize(int startingHandSize) {
                return startingHandSize;
            }
        },
        Vancouver {
            @Override
            public int getModifiedHandSize(int startingHandSize) {
                return startingHandSize;
            }
        },
        London {
            @Override
            public int getModifiedHandSize(int startingHandSize) {
                return startingHandSize;
            }
        },
        Houston {
            @Override
            public int getModifiedHandSize(int startingHandSize) {
                // The Houston rule always sets the hand size to 10, regardless of the starting size
                // or how many times the player has mulliganed.
                return 10;
            }
        };

        public abstract int getModifiedHandSize(int startingHandSize);
    }
    private static MulliganRule defaultRule = MulliganRule.London;

    public static MulliganRule getDefaultRule() {
        return defaultRule;
    }

    public static String[] getMulliganRuleNames() {
        List<String> names = Lists.newArrayList();
        for (MulliganRule mr : MulliganRule.values()) {
            names.add(mr.name());
        }
        return names.toArray(new String[0]);
    }

    public static MulliganRule GetRuleByName(String rule) {
        MulliganRule r;
        try {
            r = MulliganRule.valueOf(rule);
        } catch (IllegalArgumentException ex) {
            System.err.println("Warning: illegal Mulligan rule specified: " + rule + ", defaulting to " + getDefaultRule().name());
            r = getDefaultRule();
        }

        return r;
    }
}
