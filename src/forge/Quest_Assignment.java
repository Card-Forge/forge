package forge;

import java.util.ArrayList;

public class Quest_Assignment {
	private int 			id;
	private int 			requiredNumberWins;
	private int				computerLife;
	
	private long			creditsReward;
	
	private String 			name;
	private String			desc;
	private String 			difficulty;
	private String 			cardReward;
	private String			iconName;
	
	private boolean 		repeatable;
	
	private ArrayList<String> cardRewardList = new ArrayList<String>();
	
	private CardList		human = new CardList();
	private ArrayList<String> compy = new ArrayList<String>();

	public void setId(int id) {
		this.id = id;
	}

	public int getId() {
		return id;
	}

	public void setCreditsReward(long creditsReward) {
		this.creditsReward = creditsReward;
	}

	public long getCreditsReward() {
		return creditsReward;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}
	
	public void setDesc(String desc) {
		this.desc = desc;
	}

	public String getDesc() {
		return desc;
	}

	public void setDifficulty(String difficulty) {
		this.difficulty = difficulty;
	}

	public String getDifficulty() {
		return difficulty;
	}

	public void setRepeatable(boolean repeatable) {
		this.repeatable = repeatable;
	}

	public boolean isRepeatable() {
		return repeatable;
	}

	public void setRequiredNumberWins(int requiredNumberWins) {
		this.requiredNumberWins = requiredNumberWins;
	}

	public int getRequiredNumberWins() {
		return requiredNumberWins;
	}
	
	public void setComputerLife(int computerLife) {
		this.computerLife = computerLife;
	}

	public int getComputerLife() {
		return computerLife;
	}

	public void setCardReward(String cardReward) {
		this.cardReward = cardReward;
	}

	public String getCardReward() {
		return cardReward;
	}
	
	public void setIconName(String s)
	{
		iconName = s;
	}
	
	public String getIconName()
	{
		return iconName;
	}

	public void setHuman(CardList human) {
		this.human = human;
	}

	public CardList getHuman() {
		return human;
	}

	public void addCompy(String s) {
		this.compy.add(s);
	}
	
	public void clearCompy()
	{
		this.compy.clear();
	}
	

	public ArrayList<String> getCompy() {
		return compy;
	}
	
	public void setCardRewardList(ArrayList<String> cardRewardList) {
		this.cardRewardList = cardRewardList;
	}

	public ArrayList<String> getCardRewardList() {
		return cardRewardList;
	}
}
