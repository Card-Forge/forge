package forge.game.keyword;

public class Suspend extends KeywordWithCostAndAmount {

    boolean withoutCostAndAmount = false;

    @Override
    protected void parse(String details) {
        if ("".equals(details)) {
            withoutCostAndAmount = true;
        } else {
            super.parse(details);
        }
    }

    @Override
    protected String formatReminderText(String reminderText) {
        if (withoutCostAndAmount) {
            return "At the beginning of its owner's upkeep, remove a time counter from that card. When the last is removed, the player plays it without paying its mana cost. If it's a creature, it has haste.";
        } else {
            return super.formatReminderText(reminderText);
        }
    }
}
