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
    private int id;
    private int requiredNumberWins;
    private int computerLife;

    private long creditsReward;

    private String name;
    private String desc;
    private String difficulty;
    private String cardReward;
    private String iconName;

    private boolean repeatable;

    private ArrayList<CardPrinted> cardRewardList = new ArrayList<CardPrinted>();

    private CardList human = new CardList();
    private ArrayList<String> compy = new ArrayList<String>();

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
     * <p>Setter for the field <code>human</code>.</p>
     *
     * @param humanIn a {@link forge.CardList} object.
     */
    public final void setHuman(final CardList humanIn) {
        this.human = humanIn;
    }

    /**
     * <p>Getter for the field <code>human</code>.</p>
     *
     * @return a {@link forge.CardList} object.
     */
    public final CardList getHuman() {
        return human;
    }

    /**
     * <p>addCompy.</p>
     *
     * @param s a {@link java.lang.String} object.
     */
    public final void addCompy(final String s) {
        this.compy.add(s);
    }

    /**
     * <p>clearCompy.</p>
     */
    public final void clearCompy() {
        this.compy.clear();
    }


    /**
     * <p>Getter for the field <code>compy</code>.</p>
     *
     * @return a {@link java.util.ArrayList} object.
     */
    public final ArrayList<String> getCompy() {
        return compy;
    }

    /**
     * <p>Setter for the field <code>cardRewardList</code>.</p>
     *
     * @param cardRewardListIn a {@link java.util.ArrayList} object.
     */
    public final void setCardRewardList(final ArrayList<CardPrinted> cardRewardListIn) {
        this.cardRewardList = cardRewardListIn;
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
