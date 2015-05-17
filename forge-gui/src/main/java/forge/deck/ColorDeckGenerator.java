package forge.deck;

import java.util.ArrayList;
import java.util.List;

import forge.itemmanager.IItemManager;

public class ColorDeckGenerator extends DeckProxy implements Comparable<ColorDeckGenerator> {
    public static List<DeckProxy> getColorDecks(final IItemManager<DeckProxy> lstDecks0, final boolean isAi0) {
        final String[] colors = new String[] { "Random 1", "Random 2", "Random 3",
                "White", "Blue", "Black", "Red", "Green" };
        final List<DeckProxy> decks = new ArrayList<DeckProxy>();
        for (int i = 0; i < colors.length; i++) {
            decks.add(new ColorDeckGenerator(colors[i], i, lstDecks0, isAi0));
        }
        return decks;
    }

    private final String name;
    private final int index;
    private final IItemManager<DeckProxy> lstDecks;
    private final boolean isAi;

    private ColorDeckGenerator(final String name0, final int index0, final IItemManager<DeckProxy> lstDecks0, final boolean isAi0) {
        super();
        name = name0;
        index = index0;
        lstDecks = lstDecks0;
        isAi = isAi0;
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
    public int compareTo(final ColorDeckGenerator d) {
        return Integer.compare(index, d.index);
    }

    @Override
    public Deck getDeck() {
        final List<String> selection = new ArrayList<String>();
        for (final DeckProxy deck : lstDecks.getSelectedItems()) {
            selection.add(deck.getName());
        }
        if (DeckgenUtil.colorCheck(selection)) {
            return DeckgenUtil.buildColorDeck(selection, isAi);
        }
        return null;
    }

    @Override
    public boolean isGeneratedDeck() {
        return true;
    }
}