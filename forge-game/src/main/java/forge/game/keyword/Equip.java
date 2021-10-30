package forge.game.keyword;

public class Equip extends KeywordWithCost {

    String type = "creature";

    public Equip() {
    }

    @Override
    protected void parse(String details) {
        String[] k = details.split(":");
        super.parse(k[0]);
        if (k.length > 2) {
            type = k[2];
        }
    }

    @Override
    protected String formatReminderText(String reminderText) {
        return String.format(reminderText, cost.toSimpleString(), type);
    }
}
