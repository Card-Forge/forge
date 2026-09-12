package forge.game.keyword;

import forge.game.card.Card;

public interface IKeywordsChange {
    void applyKeywords(KeywordCollection list);
    IKeywordsChange copy(final Card host, final boolean lki);
}
