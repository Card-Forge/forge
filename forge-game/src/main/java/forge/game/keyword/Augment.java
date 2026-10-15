package forge.game.keyword;

public class Augment extends KeywordWithCost {
    protected String powerModifier = "0";
    protected String toughnessModifier = "0";
    protected String lefSideSVar = "";

    public String getPowerModifier() {
        return powerModifier;
    }

    public String getToughnessModifier() {
        return toughnessModifier;
    }

    public String getLefSideSVar() {
        return lefSideSVar;
    }

    @Override
    protected void parse(String details) {
        String[] k = details.split(":");
        super.parse(k[0]);
        String[] ptModifier = k[1].split("/");
        powerModifier = ptModifier[0];
        if (ptModifier.length > 1) {
            toughnessModifier = ptModifier[1];
        }
        if (k.length > 2) {
            lefSideSVar = k[2];
        }
    }
}
