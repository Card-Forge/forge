package forge.deck;

import java.util.ArrayList;
import java.util.List;

import forge.itemmanager.IItemManager;


public class ColorDeckGenerator extends DeckProxy implements Comparable<ColorDeckGenerator> {
    public static List<DeckProxy> getColorDecks(IItemManager<DeckProxy> lstDecks0, boolean isAi0) {
        String[] colors = new String[] { "Random 1", "Random 2", "Random 3",
                "White", "Blue", "Black", "Red", "Green" };
        ArrayList<DeckProxy> decks = new ArrayList<DeckProxy>();
        for (int i = 0; i < colors.length; i++) {
            decks.add(new ColorDeckGenerator(colors[i], i, lstDecks0, isAi0));
        }
        return decks;
    }

    private String name;
    private int index;
    private final IItemManager<DeckProxy> lstDecks;
    private final boolean isAi;

    private ColorDeckGenerator(String name0, int index0, IItemManager<DeckProxy> lstDecks0, boolean isAi0) {
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
        return d instanceof ColorDeckGenerator ? Integer.compare(index, ((ColorDeckGenerator)d).index) : 1;
    }

    @Override
    public Deck getDeck() {
        List<String> selection = new ArrayList<String>();
        for (DeckProxy deck : lstDecks.getSelectedItems()) {
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