package forge.gamemodes.rogue.effect;

import forge.deck.Deck;
import forge.deck.DeckSection;
import forge.gamemodes.rogue.RoguePlanebound;
import forge.gamemodes.rogue.RogueRun;
import forge.item.PaperCard;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Mutable context for effect result handling.
 * Set by effects that trigger follow-up actions or card effects.
 */
public class EffectResultContext {
    public enum ActionTriggerType { BAZAAR, PLANEBOUND, CARD_REMOVAL, CARD_ADDITION, CARD_REWARD, MYTHIC_CARD_REWARD, CHEST, SANCTUM, MOVE }
    public record CarryCardReward(PaperCard card, RogueRun.CarryCardType type, String sourceId) {}

    /** Set by effects that trigger another action (e.g., "opens a bazaar"). Null = no follow-up. */
    public ActionTriggerType trigger;

    /** Set by PLANEBOUND trigger to pass the picked planebound to the UI layer. */
    public RoguePlanebound planebound;

    /** Set by MOVE trigger to pass the chosen destination node index to the UI layer. */
    public int moveNodeIndex = -1;

    /** Set by BAZAAR trigger: custom Bazaar-style shopping configuration. Null = ordinary Bazaar setup. */
    public BazaarContext bazaarContext;

    /** Set by CARD_REWARD / MYTHIC_CARD_REWARD trigger: custom card reward configuration. Null = ordinary reward setup. */
    public CardRewardContext cardRewardContext;

    /** Set by card-selection triggers: */
    public List<PaperCard> candidateCards;
    public int cardSelectionMinCount;
    public int cardSelectionMaxCount;
    public String cardSelectionSubtitle;
    /** Optional deck preview built from the current selection. */
    public Function<List<PaperCard>, Deck> buildDeckPreview;
    /** Optional CARD_ADDITION handler that applies the selection and records the grants in addedCards. */
    public Consumer<List<PaperCard>> onCardsSelected;
    public int cardSelectionCopyCount = 1;
    public DeckSection addSection = DeckSection.Main;
    public boolean replaceCurrentCardsInAddSection;
    public List<PaperCard> replacementCards;
    public CarryCardReward replacementCarryCard;

    /** Cards removed/added by the effect, for result display. Null = not applicable. */
    public List<PaperCard> removedCards;
    public List<PaperCard> addedCards;

    /** Wound gained from this effect, for result display. Null = not applicable. */
    public WoundEffect gainedWoundEffect;

    /** Optional replacement result text supplied by the effect itself. Null = use the effect result text. */
    public String resultTextOverride;

    public EffectResultContext() {
        removedCards = new ArrayList<>();
        addedCards = new ArrayList<>();
    }
}
