package forge.deck;

import java.util.*;

public class DeckBuildingAdjustments {
    boolean allowance;
    boolean restriction;
    String matchType;
    String valid;
    String description;
    Map<String, String> allowanceMap = new HashMap<String, String>();

    public DeckBuildingAdjustments(String part, String part1, String part2, String part3, String _part4) {
        allowance = part.equals("Allowance");
        restriction = !allowance;
        matchType = part1;
        valid = part2;
        description = part3;

        parseValidAllowance();
    }

    public void parseValidAllowance() {
        if (restriction) {
            return;
        }

        // Valid is formatted like: SubType-Phyrexian,Land-Basic:
        // I'd like to split these and put them into a map
        for(String s : valid.split(",")) {
            String[] parts = s.split("-");
            if(parts.length == 2) {
                allowanceMap.put(parts[0], parts[1]);
            }
        }
    }

    public Map<String, String> getValidAllowances() {
        return allowanceMap;
    }

    public String getMatchType() {
        return matchType;
    }
}
