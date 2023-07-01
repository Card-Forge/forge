package forge.gamemodes.limited;

import java.util.List;

import forge.card.MagicColor;
import forge.deck.DeckFormat;
import forge.deck.io.Archetype;
import forge.game.GameFormat;
import forge.item.PaperCard;
import forge.model.FModel;

import org.apache.commons.lang3.tuple.Pair;

public class ArchetypeDeckBuilder extends CardThemedDeckBuilder{

    private Archetype archetype;
    private boolean needsSnowLands = false;

    public ArchetypeDeckBuilder(Archetype archetype0, PaperCard keyCard0, final List<PaperCard> dList, GameFormat format, boolean isForAI){
        super(keyCard0,null, dList, format, isForAI, DeckFormat.Constructed);
        archetype = archetype0;
        for(Pair<String, Double> pair : archetype.getCardProbabilities()){
            for(int i=0;i<5;++i){
                if (pair.getLeft().equals(MagicColor.Constant.SNOW_LANDS.get(i)) && pair.getRight() > 0.04) {
                    needsSnowLands=true;
                    return;
                }
            }
        }
    }

    /**
     * Generate a descriptive name.
     *
     * @return name
     */
    protected String generateName() {
        return archetype.getName() + " Generated Deck";
    }

    /**
     * Get basic land.
     *
     * @param basicLand
     *             the set to take basic lands from (pass 'null' for random).
     * @return card
     */
    protected PaperCard getBasicLand(final int basicLand) {
        if(needsSnowLands){
            for(String landSet : setsWithBasicLands){
                PaperCard land = FModel.getMagicDb().getCommonCards().getCard(MagicColor.Constant.SNOW_LANDS.get(basicLand), landSet);
                if(land!=null){
                    return land;
                }
            }
        }
        return super.getBasicLand(basicLand);
    }

}
