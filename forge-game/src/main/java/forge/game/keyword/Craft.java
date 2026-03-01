package forge.game.keyword;

import forge.card.CardType;
import forge.game.cost.Cost;
import forge.game.cost.CostExile;
import forge.game.cost.CostPart;
import forge.game.cost.CostPartMana;
import forge.util.Lang;

public class Craft extends KeywordWithCost {

    String manaString = "Mana?";
    String exileString = "Exile?";
    private String withDescription = "";

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

        // Build "with" description from all CostExile parts
        final java.util.List<String> withParts = new java.util.ArrayList<>();
        int exilePartCount = 0;
        for (CostPart part : kCost.getCostParts()) {
            if (part instanceof CostExile) {
                exilePartCount++;
                String partType = part.getType().replace(".Other", "");
                String type = part.getTypeDescription() != null
                        ? part.getTypeDescription()
                        : (CardType.CoreType.isValidEnum(partType) || "Permanent".equals(partType))
                                ? partType.toLowerCase() : partType;
                String amount = part.getAmount();
                if ("1".equals(amount)) {
                    withParts.add(Lang.nounWithNumeralExceptOne(1, type));
                } else {
                    withParts.add(Lang.getPlural(type));
                }
            }
        }
        if (!withParts.isEmpty()) {
            withDescription = Lang.joinHomogenous(withParts);
        }

        // Build exile string for reminder text
        if (exilePartCount > 1) {
            exileString = "Exile " + withDescription
                    + " from among permanents you control and/or cards in your graveyard";
        } else {
            final StringBuilder sb = new StringBuilder();
            for (CostPart part : kCost.getCostParts()) {
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
    }

    @Override
    public String getTitle() {
        if (!withDescription.isEmpty()) {
            return "Craft with " + withDescription + " " + manaString;
        }
        return super.getTitle();
    }


    @Override
    protected String formatReminderText(String reminderText) {
        return String.format(reminderText, manaString + ", Exile this artifact, " + exileString);
    }
}
