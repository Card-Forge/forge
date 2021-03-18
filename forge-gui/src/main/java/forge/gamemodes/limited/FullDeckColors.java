package forge.gamemodes.limited;

import forge.card.ColorSet;
import forge.card.MagicColor;
import forge.item.IPaperCard;

/**
 * Created by maustin on 11/05/2017.
 */
public class FullDeckColors extends DeckColors {
    public FullDeckColors(){
        MAX_COLORS = 5;
    }

    public void addColorsOf(final IPaperCard pickedCard) {
        final ColorSet colorsCanAdd = chosen.inverse();
        final ColorSet toAdd = colorsCanAdd.getSharedColors(pickedCard.getRules().getColorIdentity());

        int cntColorsAssigned = getChosenColors().countColors();
        final boolean haveSpace = cntColorsAssigned < MAX_COLORS;
        if (!haveSpace || toAdd.isColorless()) {
            return;
        }

        for (final byte color : MagicColor.WUBRG) {
            if (toAdd.hasAnyColor(color)) {
                colorMask |= color;
                chosen = null; // invalidate color set
                cntColorsAssigned++;
            }
            if (cntColorsAssigned >= MAX_COLORS) {
                break;
            }
        }
    }
}
