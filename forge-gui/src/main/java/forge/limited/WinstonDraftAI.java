package forge.limited;

import java.util.ArrayList;
import java.util.List;

import forge.deck.CardPool;
import forge.item.PaperCard;
import forge.util.MyRandom;

public class WinstonDraftAI extends BoosterDraftAI{

    private WinstonDraft draft = null;
    private static final int N_DECKS = 1;
    private List<Byte> colorPreference = new ArrayList<>();

    public WinstonDraft getDraft() {
        return draft;
    }

    public void setDraft(WinstonDraft draft) {
        this.draft = draft;
    }

    public WinstonDraftAI() {
        this.decks.clear();
        this.playerColors.clear();
        for (int i = 0; i < N_DECKS; i++) {
            this.decks.add(new ArrayList<PaperCard>());
            this.playerColors.add(new DeckColors());
        }
    }

    public void determineDeckColor() {
        // Turn colorPreference into playerColors
        //int count[] = tallyDeckColors();
        // Just take the three best colors, but maybe sometimes only take best two
        this.playerColors.get(0).setColorsByList(colorPreference.subList(0, 3));
    }

    public void choose() {
        boolean takenPile = true;
        CardPool acquire = null;
        while(takenPile) {
            CardPool pool = draft.getActivePool();
            // Determine if the current pool is worth taking
            // For now, each card in a pile is worth 10 points. Compare versus a d100 roll
            String desc = "Pile " + (draft.getCurrentBoosterIndex()+1);
            int value = pool.countAll() * 10;
            // If this is the last pile, and the deck is empty, definitely take the pile!
            boolean takePile = MyRandom.percentTrue(value) || draft.isLastPileAndEmptyDeck(draft.getCurrentBoosterIndex());

            if (takePile) {
                acquire = draft.takeActivePile(false);
            } else {
                acquire = draft.passActivePile(false);
                desc = "Top of Deck";
            }
            if (acquire != null) {
                System.out.println("AI taking " + desc + "(" + acquire.countAll() + " cards).");
                takenPile = false;
            }
        }
        if (acquire != null) {
            this.decks.get(0).addAll(acquire.toFlatList());
            //tallyDeckColors();
        }
    }

    /*private int[] tallyDeckColors() {
        int[] colorCount = new int[5];

        for(PaperCard pc : this.deck.get(0)) {
            ColorSet colors = pc.getRules().getColor();
            if (colors.hasWhite())
                colorCount[0]++;
            if (colors.hasBlue())
                colorCount[1]++;
            if (colors.hasBlack())
                colorCount[2]++;
            if (colors.hasRed())
                colorCount[3]++;
            if (colors.hasGreen())
                colorCount[4]++;
        }

        // Just keep order or should I keep order/value pairs?
        colorPreference.clear();
        for(int i = 0; i < colorCount.length; i++) {
            // Sort colors in order.
            Byte col = MagicColor.WUBRG[i];
            int spot = 0;
            for (int j = 0; j < i; j++) {
                if (colorCount[i] > colorCount[j]) {
                    // If new color has more than the current slot, we need to add into that slot
                    // And push all remaining colors further down
                    break;
                }
                spot++;
            }
            colorPreference.add(spot, col);
        }
        // Is this the right order?
        return colorCount;
    }*/

    public int getAIDraftSize() {
        return this.decks.get(0).size();
    }
}
