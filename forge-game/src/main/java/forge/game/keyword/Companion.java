package forge.game.keyword;

public class Companion extends SimpleKeyword {

    private String deckRestriction = null;
    private String description = null;
    private String specialRules = null;

    public Companion() { }

    @Override
    protected void parse(String details) {
        String[] splitString = details.split(":");
        int descriptionIndex = splitString.length - 1;

        if (splitString.length < 2) {
            System.out.println("Did not parse a long enough value for Companion.");
            return;
        }

        deckRestriction = splitString[0];

        if (deckRestriction.equals("Special")) {
            specialRules = splitString[1];
        }
        description = splitString[descriptionIndex];
    }

    public String getDeckRestriction() {
        return deckRestriction;
    }

    public boolean hasSpecialRestriction() {
        return specialRules != null;
    }

    public String getDescription() {
        return description;
    }

    public String getSpecialRules() {
        return specialRules;
    }
}
