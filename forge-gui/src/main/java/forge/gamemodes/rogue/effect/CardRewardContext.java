package forge.gamemodes.rogue.effect;

import forge.item.PaperCard;
import java.util.ArrayList;
import java.util.List;

/**
 * Mutable context for onCardReward triggers.
 * Effects accumulate into this object; the caller reads the final state.
 */
public class CardRewardContext {
    public int maxPicks;
    public int nonMythicCardCountAdjustment;
    public int additionalNonMythicCards;
    public int nonMythicCardReplacementCount;
    public String title;
    public List<PaperCard> rewardCards = new ArrayList<>();
    public List<PaperCard> nonMythicCardReplacementCandidates = new ArrayList<>();

    public CardRewardContext(int basePicks) {
        this.maxPicks = basePicks;
    }
}
