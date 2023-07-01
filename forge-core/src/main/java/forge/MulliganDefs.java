package forge;

import java.util.List;

import com.google.common.collect.Lists;

/*
 * A class that contains definitions of available Mulligan rule variants and helper methods to access them
 */
public class MulliganDefs {
    public enum MulliganRule {
        Original,
        Paris,
        Vancouver,
        London
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
