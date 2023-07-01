package forge.gamemodes.planarconquest;

import forge.item.PaperCard;

public class ConquestReward {
    private final PaperCard card;
    private final int replacementShards;

    public ConquestReward(PaperCard card0, int replacementShards0) {
        card = card0;
        replacementShards = replacementShards0;
    }

    public PaperCard getCard() {
        return card;
    }

    public boolean isDuplicate() {
        return replacementShards > 0;
    }

    public int getReplacementShards() {
        return replacementShards;
    }
}
