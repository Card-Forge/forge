package forge.game.keyword;

import forge.card.mana.ManaCost;
import forge.game.cost.Cost;

public class Mayhem extends KeywordWithCost {


    @Override
    protected void parse(String details) {
        if (!details.isEmpty()) {
            super.parse(details);
        } else {
            this.cost = new Cost(ManaCost.NO_COST, true);
        }
    }

    @Override
    protected String formatReminderText(String reminderText) {
        if (this.cost.getTotalMana().isNoCost()) {
            return "You may play this card from your graveyard if you discarded it this turn. Timing rules still apply.";
        }
        return super.formatReminderText(reminderText);
    }
}
