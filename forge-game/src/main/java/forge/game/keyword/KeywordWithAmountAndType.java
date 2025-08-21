package forge.game.keyword;

import forge.util.TextUtil;

public class KeywordWithAmountAndType extends KeywordInstance<KeywordWithAmountAndType> {
    protected int amount;
    private boolean withX;
    protected String type;

    @Override
    public int getAmount() {
        return amount;
    }

    @Override
    protected void parse(String details) {
        String[] d = details.split(":");
        if (details.startsWith("X")) {
            withX = true;
        } else {
            amount = Integer.parseInt(d[0]);
        }
        if (d.length > 1) {
            type = TextUtil.fastReplace(d[1], ",", " and/or ");
        }
    }

    @Override
    protected String formatReminderText(String reminderText) {
        if (withX) {
            return String.format(reminderText.replaceAll("\\%(\\d+\\$)?d", "%$1s"), "X", type);
        }
        return String.format(reminderText, amount, type);
    }
}
