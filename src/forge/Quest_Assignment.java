package forge;

import java.util.ArrayList;

/**
 * <p>Quest_Assignment class.</p>
 *
 * @author Forge
 * @version $Id: $
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

    private ArrayList<String> cardRewardList = new ArrayList<String>();

    private CardList human = new CardList();
    private ArrayList<String> compy = new ArrayList<String>();

    /**
     * <p>Setter for the field <code>id</code>.</p>
     *
     * @param id a int.
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * <p>Getter for the field <code>id</code>.</p>
     *
     * @return a int.
     */
    public int getId() {
        return id;
    }

    /**
     * <p>Setter for the field <code>creditsReward</code>.</p>
     *
     * @param creditsReward a long.
     */
    public void setCreditsReward(long creditsReward) {
        this.creditsReward = creditsReward;
    }

    /**
     * <p>Getter for the field <code>creditsReward</code>.</p>
     *
     * @return a long.
     */
    public long getCreditsReward() {
        return creditsReward;
    }

    /**
     * <p>Setter for the field <code>name</code>.</p>
     *
     * @param name a {@link java.lang.String} object.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * <p>Getter for the field <code>name</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getName() {
        return name;
    }

    /**
     * <p>Setter for the field <code>desc</code>.</p>
     *
     * @param desc a {@link java.lang.String} object.
     */
    public void setDesc(String desc) {
        this.desc = desc;
    }

    /**
     * <p>Getter for the field <code>desc</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getDesc() {
        return desc;
    }

    /**
     * <p>Setter for the field <code>difficulty</code>.</p>
     *
     * @param difficulty a {@link java.lang.String} object.
     */
    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    /**
     * <p>Getter for the field <code>difficulty</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getDifficulty() {
        return difficulty;
    }

    /**
     * <p>Setter for the field <code>repeatable</code>.</p>
     *
     * @param repeatable a boolean.
     */
    public void setRepeatable(boolean repeatable) {
        this.repeatable = repeatable;
    }

    /**
     * <p>isRepeatable.</p>
     *
     * @return a boolean.
     */
    public boolean isRepeatable() {
        return repeatable;
    }

    /**
     * <p>Setter for the field <code>requiredNumberWins</code>.</p>
     *
     * @param requiredNumberWins a int.
     */
    public void setRequiredNumberWins(int requiredNumberWins) {
        this.requiredNumberWins = requiredNumberWins;
    }

    /**
     * <p>Getter for the field <code>requiredNumberWins</code>.</p>
     *
     * @return a int.
     */
    public int getRequiredNumberWins() {
        return requiredNumberWins;
    }

    /**
     * <p>Setter for the field <code>computerLife</code>.</p>
     *
     * @param computerLife a int.
     */
    public void setComputerLife(int computerLife) {
        this.computerLife = computerLife;
    }

    /**
     * <p>Getter for the field <code>computerLife</code>.</p>
     *
     * @return a int.
     */
    public int getComputerLife() {
        return computerLife;
    }

    /**
     * <p>Setter for the field <code>cardReward</code>.</p>
     *
     * @param cardReward a {@link java.lang.String} object.
     */
    public void setCardReward(String cardReward) {
        this.cardReward = cardReward;
    }

    /**
     * <p>Getter for the field <code>cardReward</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getCardReward() {
        return cardReward;
    }

    /**
     * <p>Setter for the field <code>iconName</code>.</p>
     *
     * @param s a {@link java.lang.String} object.
     */
    public void setIconName(String s) {
        iconName = s;
    }

    /**
     * <p>Getter for the field <code>iconName</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getIconName() {
        return iconName;
    }

    /**
     * <p>Setter for the field <code>human</code>.</p>
     *
     * @param human a {@link forge.CardList} object.
     */
    public void setHuman(CardList human) {
        this.human = human;
    }

    /**
     * <p>Getter for the field <code>human</code>.</p>
     *
     * @return a {@link forge.CardList} object.
     */
    public CardList getHuman() {
        return human;
    }

    /**
     * <p>addCompy.</p>
     *
     * @param s a {@link java.lang.String} object.
     */
    public void addCompy(String s) {
        this.compy.add(s);
    }

    /**
     * <p>clearCompy.</p>
     */
    public void clearCompy() {
        this.compy.clear();
    }


    /**
     * <p>Getter for the field <code>compy</code>.</p>
     *
     * @return a {@link java.util.ArrayList} object.
     */
    public ArrayList<String> getCompy() {
        return compy;
    }

    /**
     * <p>Setter for the field <code>cardRewardList</code>.</p>
     *
     * @param cardRewardList a {@link java.util.ArrayList} object.
     */
    public void setCardRewardList(ArrayList<String> cardRewardList) {
        this.cardRewardList = cardRewardList;
    }

    /**
     * <p>Getter for the field <code>cardRewardList</code>.</p>
     *
     * @return a {@link java.util.ArrayList} object.
     */
    public ArrayList<String> getCardRewardList() {
        return cardRewardList;
    }
}
