package forge.game.keyword;

import forge.util.TextUtil;

public class KeywordWithAmountAndType extends KeywordInstance<KeywordWithAmountAndType> {
    private int amount;
    private String type;

    @Override
    public int getAmount() {
        return amount;
    }

    @Override
    protected void parse(String details) {
        String[] d = details.split(":");
        amount = Integer.parseInt(d[0]);
        type = TextUtil.fastReplace(d[1], ",", " and/or ");
    }

    @Override
    protected String formatReminderText(String reminderText) {
        return String.format(reminderText, amount, type);
    }
}
