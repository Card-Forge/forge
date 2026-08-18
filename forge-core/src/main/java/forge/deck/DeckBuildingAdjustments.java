package forge.deck;

public class DeckBuildingAdjustments {
    boolean allowance;
    boolean restriction;
    String matchType;
    String valid;
    String description;

    public DeckBuildingAdjustments(String part, String part1, String part2, String part3, String _part4) {
        allowance = part.equals("Allowance");
        restriction = !allowance;
        matchType = part1;
        valid = part2;
        description = part3;
    }


}
