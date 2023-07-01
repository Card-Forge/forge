package forge.game.keyword;

import org.apache.commons.lang3.StringUtils;

import forge.card.CardType;
import forge.game.cost.Cost;

public class KeywordWithCostAndType extends KeywordInstance<KeywordWithCostAndType> {
    private Cost cost;
    private String type;
    private String strType = null;

    @Override
    protected void parse(String details) {
        final String[] k = details.split(":");
        type = k[0];
        cost = new Cost(k[1], false);
        if (k.length > 2) {
            strType = k[2];
        } else {
            String[] n = type.split(",");
            for (int i = 0; i < n.length; i++) {
                if (CardType.isACardType(n[i])) {
                    n[i] = n[i].toLowerCase();
                } else if (n[i].equals("Basic")) {
                    n[i] = "basic land";
                } else if (n[i].equals("Land.Artifact")) {
                    n[i] = "artifact land";
                }
            }

            strType = StringUtils.join(n, " or ");
        }
    }

    @Override
    protected String formatReminderText(String reminderText) {
        return String.format(reminderText, cost.toSimpleString(), strType);
    }
}
