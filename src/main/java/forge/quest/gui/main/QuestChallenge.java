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
    private int id = -1;

    // Default vals if none provided for this ID
    /** The ai life. */
    private int aiLife = 25;

    /** The credits reward. */
    private int creditsReward = 100;

    /** The card reward. */
    private String cardReward = "1 colorless rare";

    /** The repeatable. */
    private boolean repeatable = false;

    /** The wins reqd. */
    private int winsReqd = 20;

    // Other cards used in assignment: starting, and reward.
    /** The human extra cards. */
    private List<String> humanExtraCards = new ArrayList<String>();

    /** The ai extra cards. */
    private List<String> aiExtraCards = new ArrayList<String>();

    /** The card reward list. */
    private List<CardPrinted> cardRewardList = new ArrayList<CardPrinted>();

    /**
     * Instantiates a new quest challenge.
     */
    public QuestChallenge() {
        super();
        this.setEventType("challenge");
    }

    /**
     * <p>
     * getAILife.
     * </p>
     * 
     * @return {@link java.lang.Integer}.
     */
    public final int getAILife() {
        return this.getAiLife();
    }

    /**
     * <p>
     * getCardReward.
     * </p>
     * 
     * @return {@link java.lang.String}.
     */
    public final String getCardReward() {
        return this.cardReward;
    }

    /**
     * <p>
     * getCreditsReward.
     * </p>
     * 
     * @return {@link java.lang.Integer}.
     */
    public final int getCreditsReward() {
        return this.creditsReward;
    }

    /**
     * <p>
     * getId.
     * </p>
     * 
     * @return {@link java.lang.Integer}.
     */
    public final int getId() {
        return this.id;
    }

    /**
     * <p>
     * getRepeatable.
     * </p>
     * 
     * @return {@link java.lang.Boolean}.
     */
    public final boolean getRepeatable() {
        return this.isRepeatable();
    }

    /**
     * <p>
     * getWinsReqd.
     * </p>
     * 
     * @return {@link java.lang.Integer}.
     */
    public final int getWinsReqd() {
        return this.winsReqd;
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
        return this.getAiExtraCards();
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
        return this.humanExtraCards;
    }

    /**
     * <p>
     * getCardRewardList.
     * </p>
     * 
     * @return the card reward list
     */
    public final List<CardPrinted> getCardRewardList() {
        return this.cardRewardList;
    }

    /**
     * @return the aiExtraCards
     */
    public List<String> getAiExtraCards() {
        return aiExtraCards;
    }

    /**
     * @param aiExtraCards the aiExtraCards to set
     */
    public void setAiExtraCards(List<String> aiExtraCards) {
        this.aiExtraCards = aiExtraCards; // TODO: Add 0 to parameter's name.
    }

    /**
     * @param id the id to set
     */
    public void setId(int id) {
        this.id = id; // TODO: Add 0 to parameter's name.
    }

    /**
     * @return the repeatable
     */
    public boolean isRepeatable() {
        return repeatable;
    }

    /**
     * @param repeatable the repeatable to set
     */
    public void setRepeatable(boolean repeatable) {
        this.repeatable = repeatable; // TODO: Add 0 to parameter's name.
    }

    /**
     * @return the aiLife
     */
    public int getAiLife() {
        return aiLife;
    }

    /**
     * @param aiLife the aiLife to set
     */
    public void setAiLife(int aiLife) {
        this.aiLife = aiLife; // TODO: Add 0 to parameter's name.
    }

    /**
     * @param winsReqd the winsReqd to set
     */
    public void setWinsReqd(int winsReqd) {
        this.winsReqd = winsReqd; // TODO: Add 0 to parameter's name.
    }

    /**
     * @param creditsReward the creditsReward to set
     */
    public void setCreditsReward(int creditsReward) {
        this.creditsReward = creditsReward; // TODO: Add 0 to parameter's name.
    }

    /**
     * @param cardReward the cardReward to set
     */
    public void setCardReward(String cardReward) {
        this.cardReward = cardReward; // TODO: Add 0 to parameter's name.
    }

    /**
     * @param cardRewardList the cardRewardList to set
     */
    public void setCardRewardList(List<CardPrinted> cardRewardList) {
        this.cardRewardList = cardRewardList; // TODO: Add 0 to parameter's name.
    }

    /**
     * @param humanExtraCards the humanExtraCards to set
     */
    public void setHumanExtraCards(List<String> humanExtraCards) {
        this.humanExtraCards = humanExtraCards; // TODO: Add 0 to parameter's name.
    }
}
