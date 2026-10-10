package forge.deck;

import forge.card.CardEdition;

public abstract class GeneratedDeckProxy extends DeckProxy {
    private final String name;
    private final int mainSize;

    protected GeneratedDeckProxy() {
        this(null, -1);
    }

    protected GeneratedDeckProxy(final String name0) {
        this(name0, -1);
    }

    protected GeneratedDeckProxy(final int mainSize0) {
        this(null, mainSize0);
    }

    protected GeneratedDeckProxy(final String name0, final int mainSize0) {
        super();
        name = name0;
        mainSize = mainSize0;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public CardEdition getEdition() {
        return CardEdition.UNKNOWN;
    }

    @Override
    public int getMainSize() {
        return mainSize;
    }

    @Override
    public boolean isGeneratedDeck() {
        return true;
    }
}
