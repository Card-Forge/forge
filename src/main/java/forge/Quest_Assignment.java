package forge;

import java.util.ArrayList;

import forge.card.CardPrinted;

/**
 * <p>Quest_Assignment class.</p>
 *
 * @author Forge
 * @version $Id$
 */
public class Quest_Assignment {
    // ID (default -1, should be explicitly set at later time.)
    private int id = -1;
    
    // Default vals if none provided for this ID in quests.txt.
    private int     requiredNumberWins  = 20;
    private int     computerLife        = 25;
    private long    creditsReward       = 100;
    private String  name                = "Mystery Quest";
    private String  desc                = "";
    private String  difficulty          = "Medium";
    private String  cardReward          = "1 colorless rare";
    private String  iconName            = "Unknown.jpg";
    private boolean repeatable          = false;

    // Other cards used in assignment: starting, and reward.
    private CardList humanExtraCards    = new CardList();
    private CardList aiExtraCards       = new CardList();
    private ArrayList<CardPrinted> cardRewardList = new ArrayList<CardPrinted>(); 
    
    /**
     * <p>Setter for the field <code>id</code>.</p>
     *
     * @param idIn a int.
     */
    public final void setId(final int idIn) {
        this.id = idIn;
    }

    /**
     * <p>Getter for the field <code>id</code>.</p>
     *
     * @return a int.
     */
    public final int getId() {
        return id;
    }

    /**
     * <p>Setter for the field <code>creditsReward</code>.</p>
     *
     * @param creditsRewardIn a long.
     */
    public final void setCreditsReward(final long creditsRewardIn) {
        this.creditsReward = creditsRewardIn;
    }

    /**
     * <p>Getter for the field <code>creditsReward</code>.</p>
     *
     * @return a long.
     */
    public final long getCreditsReward() {
        return creditsReward;
    }

    /**
     * <p>Setter for the field <code>name</code>.</p>
     *
     * @param nameIn a {@link java.lang.String} object.
     */
    public final void setName(final String nameIn) {
        this.name = nameIn;
    }

    /**
     * <p>Getter for the field <code>name</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public final String getName() {
        return name;
    }

    /**
     * <p>Setter for the field <code>desc</code>.</p>
     *
     * @param descIn a {@link java.lang.String} object.
     */
    public final void setDesc(final String descIn) {
        this.desc = descIn;
    }

    /**
     * <p>Getter for the field <code>desc</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public final String getDesc() {
        return desc;
    }

    /**
     * <p>Setter for the field <code>difficulty</code>.</p>
     *
     * @param difficultyIn a {@link java.lang.String} object.
     */
    public final void setDifficulty(final String difficultyIn) {
        this.difficulty = difficultyIn;
    }

    /**
     * <p>Getter for the field <code>difficulty</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public final String getDifficulty() {
        return difficulty;
    }

    /**
     * <p>Setter for the field <code>repeatable</code>.</p>
     *
     * @param repeatableIn a boolean.
     */
    public final void setRepeatable(final boolean repeatableIn) {
        this.repeatable = repeatableIn;
    }

    /**
     * <p>isRepeatable.</p>
     *
     * @return a boolean.
     */
    public final boolean isRepeatable() {
        return repeatable;
    }

    /**
     * <p>Setter for the field <code>requiredNumberWins</code>.</p>
     *
     * @param requiredNumberWinsIn a int.
     */
    public final void setRequiredNumberWins(final int requiredNumberWinsIn) {
        this.requiredNumberWins = requiredNumberWinsIn;
    }

    /**
     * <p>Getter for the field <code>requiredNumberWins</code>.</p>
     *
     * @return a int.
     */
    public final int getRequiredNumberWins() {
        return requiredNumberWins;
    }

    /**
     * <p>Setter for the field <code>computerLife</code>.</p>
     *
     * @param computerLifeIn a int.
     */
    public final void setComputerLife(final int computerLifeIn) {
        this.computerLife = computerLifeIn;
    }

    /**
     * <p>Getter for the field <code>computerLife</code>.</p>
     *
     * @return a int.
     */
    public final int getComputerLife() {
        return computerLife;
    }

    /**
     * <p>Setter for the field <code>cardReward</code>.</p>
     *
     * @param cardRewardIn a {@link java.lang.String} object.
     */
    public final void setCardReward(final String cardRewardIn) {
        this.cardReward = cardRewardIn;
    }

    /**
     * <p>Getter for the field <code>cardReward</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public final String getCardReward() {
        return cardReward;
    }

    /**
     * <p>Setter for the field <code>iconName</code>.</p>
     *
     * @param s a {@link java.lang.String} object.
     */
    public final void setIconName(final String s) {
        iconName = s;
    }

    /**
     * <p>Getter for the field <code>iconName</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public final String getIconName() {
        return iconName;
    }

    /**
     * <p>Setter for the field <code>humanExtraCards</code>.</p>
     *
     * @param s a CardList object.
     */
    public final void setHumanExtraCards(final CardList cl) {
        this.humanExtraCards = cl;
    }
    
    /**
     * <p>Getter for the field <code>humanExtraCards</code>.</p>
     *
     * @return a {@link forge.CardList} object.
     */
    public final CardList getHumanExtraCards() {
        return humanExtraCards;
    }

    /**
     * <p>Setter for the field <code>aiExtraCards</code>.</p>
     *
     * @param s a CardList object.
     */
    public final void setAIExtraCards(final CardList cl) {
        this.aiExtraCards = cl;
    }
    
    /**
     * <p>Getter for the field <code>compy</code>.</p>
     *
     * @return a {@link java.util.ArrayList} object.
     */
    public final CardList getAIExtraCards() {
        return aiExtraCards;
    }

    /**
     * <p>Setter for the field <code>cardRewardList</code>.</p>
     *
     * @return a {@link java.util.ArrayList} object.
     */
    public void setCardRewardList(final ArrayList<CardPrinted> cp) {
        this.cardRewardList = cp;
    }
    
    /**
     * <p>Getter for the field <code>cardRewardList</code>.</p>
     *
     * @return a {@link java.util.ArrayList} object.
     */
    public final ArrayList<CardPrinted> getCardRewardList() {
        return cardRewardList;
    }
    
}
