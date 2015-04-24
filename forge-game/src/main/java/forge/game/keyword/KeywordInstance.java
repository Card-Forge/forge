package forge.game.keyword;

import java.io.Serializable;

public abstract class KeywordInstance<T extends KeywordInstance<?>> implements Serializable {
    private static final long serialVersionUID = -8515507185693606389L;

    private Keyword keyword;

    public Keyword getKeyword() {
        return keyword;
    }
    public String getReminderText() {
        return formatReminderText(keyword.reminderText);
    }
    public int getAmount() {
        return 1;
    }
    protected void initialize(Keyword keyword0, String details) {
        keyword = keyword0;
        parse(details);
    }
    protected abstract void parse(String details);
    protected abstract String formatReminderText(String reminderText);
}
