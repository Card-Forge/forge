package forge.game.keyword;

public class Emerge extends KeywordWithCostAndType {
    protected void parse(String details) {
        final String[] k = details.split(":");
        if (k.length < 2) {
            super.parse("Creature:" + k[0]);
        } else {
            // Flip parameters
            super.parse(k[1] + ":" + k[0]);
        }
    }
}
