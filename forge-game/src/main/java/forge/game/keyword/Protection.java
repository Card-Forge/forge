package forge.game.keyword;

public class Protection extends KeywordWithType {

    @Override
    public String getTitle() {
        return "Protection from " + getTypeDescription();
    }

    public static String getProtectionValid(final String kw, final boolean damage) {
        String validSource = "";

        if (kw.startsWith("Protection:")) {
            final String[] kws = kw.split(":");
            String characteristic = kws[1];
            if (characteristic.startsWith("Player")) {
                validSource = "ControlledBy " + characteristic;
            } else {
                if (damage && (characteristic.endsWith("White") || characteristic.endsWith("Blue")
                        || characteristic.endsWith("Black") || characteristic.endsWith("Red")
                        || characteristic.endsWith("Green") || characteristic.endsWith("Colorless")
                        || characteristic.endsWith("MonoColor") || characteristic.endsWith("MultiColor")
                        || characteristic.endsWith("EnemyColor"))) {
                    characteristic += "Source";
                }
                return characteristic;
            }
        } else if (kw.startsWith("Protection from ")) {
            String protectType = kw.substring("Protection from ".length());
            if (protectType.equals("white")) {
                validSource = "White" + (damage ? "Source" : "");
            } else if (protectType.equals("blue")) {
                validSource = "Blue" + (damage ? "Source" : "");
            } else if (protectType.equals("black")) {
                validSource = "Black" + (damage ? "Source" : "");
            } else if (protectType.equals("red")) {
                validSource = "Red" + (damage ? "Source" : "");
            } else if (protectType.equals("green")) {
                validSource = "Green" + (damage ? "Source" : "");
            } else if (protectType.equals("colorless")) {
                validSource = "Colorless" + (damage ? "Source" : "");
            } else if (protectType.equals("each color")) {
                validSource = "nonColorless" + (damage ? "Source" : "");
            } else if (protectType.equals("everything")) {
                return "";
            } else {
                throw new RuntimeException("unknown protection keyword: " + kw);
            }
        }
        if (validSource.isEmpty()) {
            return validSource;
        }
        return "Card." + validSource + ",Emblem." + validSource;
    }
}
