package forge.game.keyword;

import forge.game.cost.Cost;
import forge.game.cost.CostExile;
import forge.game.cost.CostPart;
import forge.game.cost.CostPartMana;

public class Craft extends KeywordWithCost {

    String manaString = "Mana?";
    String exileString = "Exile?";


    @Override
    protected void parse(String details) {
        String[] k = details.split(":");
        super.parse(k[0]);

        final Cost kCost = new Cost(k[0], true);
        for (CostPart part : kCost.getCostParts()) {
            if (part instanceof CostPartMana) {
                manaString = part.toString();
                break;
            }
        }

        final StringBuilder sb = new StringBuilder();
        if (k.length > 2) {
            sb.append("Exile ").append(k[2]).append(" from among permanents you control and/or cards in your graveyard");
        } else for (CostPart part : kCost.getCostParts()) {
            if (part instanceof CostExile) {
                int xMin = 0;
                if (k[0].contains("XMin")) {
                    String cutString = k[0].substring(k[0].indexOf("XMin") + 4);
                    xMin = Integer.parseInt(cutString.substring(0, cutString.indexOf(" ")));
                }
                sb.append(((CostExile) part).exileMultiZoneCostString(true, xMin));
                break;
            }
        }
        exileString = sb.toString();
    }


    @Override
    protected String formatReminderText(String reminderText) {
        return String.format(reminderText, manaString + ", Exile this artifact, " + exileString);
    }
}
