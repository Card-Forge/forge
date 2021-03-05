package forge.gamemodes.limited;

import forge.deck.DeckFormat;
import forge.deck.io.Archetype;
import forge.game.GameFormat;
import forge.item.PaperCard;

import java.util.List;

public class ArchetypeDeckBuilder extends CardThemedDeckBuilder{

    private Archetype archetype;

    public ArchetypeDeckBuilder(Archetype archetype0, PaperCard keyCard0, final List<PaperCard> dList, GameFormat format, boolean isForAI){
        super(keyCard0,null, dList, format, isForAI, DeckFormat.Constructed);
        archetype = archetype0;
    }

    /**
     * Generate a descriptive name.
     *
     * @return name
     */
    protected String generateName() {
        return archetype.getName() + " Generated Deck";
    }



}
