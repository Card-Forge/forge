package forge.quest.gui.main;

import java.util.ArrayList;
import java.util.List;

import forge.item.CardPrinted;

/**
 * <p>
 * QuestQuest class.
 * </p>
 * 
 * MODEL - A single quest event data instance, including meta, deck, and
 * quest-specific properties.
 * 
 */
public class QuestChallenge extends QuestEvent {
    // ID (default -1, should be explicitly set at later time.)
    /** The id. */
    public int id = -1;

    // Default vals if none provided for this ID
    /** The ai life. */
    public int aiLife = 25;

    /** The credits reward. */
    public int creditsReward = 100;

    /** The card reward. */
    public String cardReward = "1 colorless rare";

    /** The repeatable. */
    public boolean repeatable = false;

    /** The wins reqd. */
    public int winsReqd = 20;

    // Other cards used in assignment: starting, and reward.
    /** The human extra cards. */
    public List<String> humanExtraCards = new ArrayList<String>();

    /** The ai extra cards. */
    public List<String> aiExtraCards = new ArrayList<String>();

    /** The card reward list. */
    public List<CardPrinted> cardRewardList = new ArrayList<CardPrinted>();

    /**
     * Instantiates a new quest challenge.
     */
    public QuestChallenge() {
        super();
        eventType = "challenge";
    }

    /**
     * <p>
     * getAILife.
     * </p>
     * 
     * @return {@link java.lang.Integer}.
     */
    public final int getAILife() {
        return aiLife;
    }

    /**
     * <p>
     * getCardReward.
     * </p>
     * 
     * @return {@link java.lang.String}.
     */
    public final String getCardReward() {
        return cardReward;
    }

    /**
     * <p>
     * getCreditsReward.
     * </p>
     * 
     * @return {@link java.lang.Integer}.
     */
    public final int getCreditsReward() {
        return creditsReward;
    }

    /**
     * <p>
     * getId.
     * </p>
     * 
     * @return {@link java.lang.Integer}.
     */
    public final int getId() {
        return id;
    }

    /**
     * <p>
     * getRepeatable.
     * </p>
     * 
     * @return {@link java.lang.Boolean}.
     */
    public final boolean getRepeatable() {
        return repeatable;
    }

    /**
     * <p>
     * getWinsReqd.
     * </p>
     * 
     * @return {@link java.lang.Integer}.
     */
    public final int getWinsReqd() {
        return winsReqd;
    }

    /**
     * <p>
     * getAIExtraCards.
     * </p>
     * Retrieves list of cards AI has in play at the beginning of this quest.
     * 
     * @return the aI extra cards
     */
    public final List<String> getAIExtraCards() {
        return aiExtraCards;
    }

    /**
     * <p>
     * getHumanExtraCards.
     * </p>
     * Retrieves list of cards human has in play at the beginning of this quest.
     * 
     * @return the human extra cards
     */
    public final List<String> getHumanExtraCards() {
        return humanExtraCards;
    }

    /**
     * <p>
     * getCardRewardList.
     * </p>
     * 
     * @return the card reward list
     */
    public final List<CardPrinted> getCardRewardList() {
        return cardRewardList;
    }
}
