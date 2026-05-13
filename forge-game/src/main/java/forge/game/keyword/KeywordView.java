package forge.game.keyword;

import java.io.Serializable;

public interface KeywordView extends Serializable {
    String original();
    Keyword keyword();

    String title();
    String reminderText();
}
