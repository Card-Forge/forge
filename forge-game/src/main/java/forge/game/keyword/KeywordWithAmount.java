package forge.game.keyword;

public class KeywordWithAmount extends KeywordInstance<KeywordWithAmount> {
    private int amount;
    private boolean withX;
    private String extra = "";

    @Override
    public int getAmount() {
        return amount;
    }

    @Override
    protected void parse(String details) {
        if (details.startsWith("X")) {
            withX = true;
            if (details.contains(":")) {
                extra = details.split(":")[1];
            }
        } else if (!details.isEmpty()) {
            amount = details.contains(":") ? Integer.parseInt(details.split(":")[0]) : Integer.parseInt(details);
        }
    }

    @Override
    protected String formatReminderText(String reminderText) {
        if (withX) {
            StringBuilder result = new StringBuilder(
                String.format(reminderText.replaceAll("\\%(\\d+\\$)?d", "%$1s"), "X")
            );
            if (!extra.isEmpty() && !extra.contains("$")) {
                result.insert(result.length()-1, extra);
            }
            return result.toString();
        } else {
            return String.format(reminderText, amount);
        }
    }
}
