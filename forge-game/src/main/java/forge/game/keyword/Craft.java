package forge.game.keyword;

import forge.card.CardType;
import forge.game.cost.CostExile;

import forge.util.Lang;

public class Craft extends KeywordWithCost {

    String desc;
    String reminder;

    @Override
    public String getTitle() {
        StringBuilder sb = new StringBuilder();
        sb.append(getTitleWithoutCost());
        sb.append(" ");
        sb.append(cost.getCostMana());
        return sb.toString();
    }

    @Override
    public String getTitleWithoutCost() {
        return "Craft with " + desc;
    }

    @Override
    protected void parse(String details) {
        String[] k = details.split(":");
        super.parse(k[0]);

        if (k.length > 2) {
            desc = k[1];
            final StringBuilder sb = new StringBuilder();
            sb.append("Exile ").append(k[2]).append(" from among permanents you control and/or cards in your graveyard");
            reminder = sb.toString();
        } else {
            CostExile exile = cost.getCostPartByType(CostExile.class);
            final StringBuilder sb = new StringBuilder();
            int xmin = cost.getCostMana().getXMin();
            if (xmin > 0) {
                sb.append(Lang.getNumeral(xmin)).append(" or more");
            } else if (!"1".equals(exile.getAmount())) {
                sb.append(Lang.getNumeral(Integer.parseInt(exile.getAmount())));
            }
            if (exile.getTypeDescription() != null) {
                // permanent are skipped in desc
                if (!"permanent".equals(exile.getTypeDescription())) {
                    sb.append(" ");
                    sb.append(exile.getTypeDescription());
                }
            } else {
                sb.append(" ");
                String partType = exile.getType();
                //consume .Other from most partTypes
                if (partType.contains(".Other")) partType = partType.replace(".Other", "");
                // try to guess plural
                if (xmin > 0 || !"1".equals(exile.getAmount())) {
                    sb.append(CardType.getPluralType(partType));
                } else {
                    sb.append(Lang.getInstance().formatValidDesc(partType));
                }
            }
            desc = sb.toString();
            reminder = exile.exileMultiZoneCostString(true, xmin);
        }
    }

    @Override
    protected String formatReminderText(String reminderText) {
        return String.format(reminderText, cost.getCostMana(), reminder);
    }
}
