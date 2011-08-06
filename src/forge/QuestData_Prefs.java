package forge;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.Serializable;

import forge.properties.ForgeProps;
import forge.properties.NewConstants.QUEST;

public class QuestData_Prefs implements Serializable {
	private static final long serialVersionUID = 3266336025656577905L;

	private int numDiff = 4; 
	
	// Descriptive difficulty names
	private String[] sDifficulty = { "Easy", "Normal", "Hard", "Very Hard" };
	
	// Default match wins it takes to gain a booster
	private int[] winsForBooster = { 1, 1, 2, 2 };
	private int[] winsForRankIncrease = { 1, 2, 3, 4 };
	private int[] winsForMediumAI = { 6, 6, 11, 11 };
	private int[] winsForHardAI = { 9, 9, 21, 21 };
	
	// Default starting land for a quest
	private int startingBasicLand = 20;
	private int startingSnowBasicLand = 20;

	// Default starting amount of each rarity
	private int[] startingCommons = {45, 40, 40, 40};
	private int[] startingUncommons = {20, 15, 15, 15};
	private int[] startingRares = {10, 10, 10, 10};
	
	private int startingCredits = 250;

	private int boosterPackRare = 1;
	private int boosterPackUncommon = 3;
	private int boosterPackCommon = 9;

	private int matchRewardBase = 10;
	private double matchRewardTotalWins = 0.3;
	private int matchRewardNoLosses = 10;
	
	private int matchRewardPoisonWinBonus = 50;
	private int matchRewardMilledWinBonus = 40;
	private int matchRewardAltWinBonus = 100;
	
	private int matchRewardWinOnFirstTurn = 1500;
	private int matchRewardWinByTurnFive = 250;
	private int matchRewardWinByTurnTen = 50;
	private int matchRewardWinByTurnFifteen = 5;
	private int matchRewardMullToZero = 500;
	
	public QuestData_Prefs()
	{
		// if quest.prefs exists
		grabPrefsFromFile();
	}
	
	public void grabPrefsFromFile(){
		try{
			BufferedReader input = new BufferedReader(new FileReader(ForgeProps.getFile(QUEST.PREFS)));
			String line = null;
			while((line = input.readLine()) != null){
				if (line.startsWith("#") || line.length() == 0)
					continue;
				String[] split = line.split("=");
				
				if (split[0].equals("difficultyString"))
					setDifficulty(split[1]);
				else if (split[0].equals("winsForBooster"))
					setWinsForBooster(split[1]);
				else if (split[0].equals("winsForRankIncrease"))
					setWinsForRank(split[1]);
				else if (split[0].equals("winsForMediumAI"))
					setWinsForMediumAI(split[1]);
				else if (split[0].equals("winsForHardAI"))
					setWinsForHardAI(split[1]);
				else if (split[0].equals("startingBasicLand"))
					setStartingBasic(split[1]);
				else if (split[0].equals("startingSnowBasicLand"))
					setStartingSnowBasic(split[1]);
				else if (split[0].equals("startingCommons"))
					setStartingCommons(split[1]);
				else if (split[0].equals("startingUncommons"))
					setStartingUncommons(split[1]);
				else if (split[0].equals("startingRares"))
					setStartingRares(split[1]);
				else if (split[0].equals("startingCredits"))
					setStartingCredits(split[1]);
				else if (split[0].equals("boosterPackCommon"))
					setNumCommon(split[1]);
				else if (split[0].equals("boosterPackUncommon"))
					setNumUncommon(split[1]);
				else if (split[0].equals("boosterPackRare"))
					setNumRares(split[1]);
				else if (split[0].equals("matchRewardBase"))
					setMatchRewardBase(split[1]);
				else if (split[0].equals("matchRewardTotalWins"))
					setMatchRewardTotalWins(split[1]);
				else if (split[0].equals("matchRewardNoLosses"))
					setMatchRewardNoLosses(split[1]);
				else if (split[0].equals("matchRewardMilledWinBonus"))
					setMatchRewardMilledWinBonus(split[1]);
				else if (split[0].equals("matchRewardPoisonWinBonus"))
					setMatchRewardPoisonWinBonus(split[1]);
				else if (split[0].equals("matchRewardAltWinBonus"))
					setMatchRewardAltWinBonus(split[1]);
				else if (split[0].equals("matchRewardWinOnFirstTurn"))
					setMatchRewardWinFirst(split[1]);
				else if (split[0].equals("matchRewardWinByTurnFive"))
					setMatchRewardWinByFifth(split[1]);
				else if (split[0].equals("matchRewardWinByTurnTen"))
					setMatchRewardWinByTen(split[1]);
				else if (split[0].equals("matchRewardWinByTurnFifteen"))
					setMatchRewardWinByFifteen(split[1]);
				else if (split[0].equals("matchRewardMullToZero"))
					setMatchMullToZero(split[1]);
			}
		}
		catch(Exception e)
		{
			System.out.println("Trouble grabbing quest data preferences. Using default values.");
		}    
	}
	
	// getters
	public String[] getDifficulty(){
		return sDifficulty;
	}
	
	public String getDifficulty(int index){
		return sDifficulty[index];
	}
	
	public int getWinsForBooster(int index){
		return winsForBooster[index];
	}
	
	public int getWinsForRankIncrease(int index){
		return winsForRankIncrease[index];
	}
	
	public int getWinsForMediumAI(int index){
		return winsForMediumAI[index];
	}
	
	public int getWinsForHardAI(int index){
		return winsForHardAI[index];
	}
	
	public int getStartingBasic(){
		return startingBasicLand;
	}
	
	public int getStartingSnowBasic(){
		return startingSnowBasicLand;
	}
	
	public int getStartingCommons(int index){
		return startingCommons[index];
	}
	
	public int getStartingUncommons(int index){
		return startingUncommons[index];
	}
	
	public int getStartingRares(int index){
		return startingRares[index];
	}
	
	public int getStartingCredits(){
		return startingCredits;
	}
	
	public int getNumCommon(){
		return boosterPackCommon;
	}
	
	public int getNumUncommon(){
		return boosterPackUncommon;
	}
	
	public int getNumRare(){
		return boosterPackRare;
	}
	
	
	public int getMatchRewardBase(){
		return matchRewardBase;
	}
	
	public double getMatchRewardTotalWins(){
		return matchRewardTotalWins;
	}
	
	public int getMatchRewardNoLosses(){
		return matchRewardNoLosses;
	}
	
	public int getMatchRewardPoisonWinBonus(){
		return matchRewardPoisonWinBonus;
	}
	
	public int getMatchRewardMilledWinBonus(){
		return matchRewardMilledWinBonus;
	}
	
	public int getMatchRewardAltWinBonus(){
		return matchRewardAltWinBonus;
	}
	
	
	public int getMatchRewardWinFirst(){
		return matchRewardWinOnFirstTurn;
	}
	
	public int getMatchRewardWinByFifth(){
		return matchRewardWinByTurnFive;
	}
	
	public int getMatchRewardWinByTen(){
		return matchRewardWinByTurnTen;
	}
	
	public int getMatchRewardWinByFifteen(){
		return matchRewardWinByTurnFifteen;
	}
	
	public int getMatchMullToZero(){
		return matchRewardMullToZero;
	}
	
	
	// setters
	public void setDifficulty(String diff){
		this.sDifficulty = diff.split(",");
	}
	
	public void setWinsForBooster(String wins){
		String[] winsStr = wins.split(",");
		
		for(int i = 0; i < numDiff; i++)
			this.winsForBooster[i] = Integer.parseInt(winsStr[i]);
	}
	
	public void setWinsForRank(String wins){
		String[] winsStr = wins.split(",");
		
		for(int i = 0; i < numDiff; i++)
			this.winsForRankIncrease[i] = Integer.parseInt(winsStr[i]);
	}
	
	public void setWinsForMediumAI(String wins){
		String[] winsStr = wins.split(",");
		
		for(int i = 0; i < numDiff; i++)
			this.winsForMediumAI[i] = Integer.parseInt(winsStr[i]);
	}
	
	public void setWinsForHardAI(String wins){
		String[] winsStr = wins.split(",");
		
		for(int i = 0; i < numDiff; i++)
			this.winsForHardAI[i] = Integer.parseInt(winsStr[i]);
	}
	
	public void setStartingBasic(String land){
		this.startingBasicLand = Integer.parseInt(land);
	}
	
	public void setStartingSnowBasic(String land){
		this.startingSnowBasicLand = Integer.parseInt(land);
	}
	
	public void setStartingCommons(String rarity){
		String[] splitStr = rarity.split(",");
		
		for(int i = 0; i < numDiff; i++)
			this.startingCommons[i] = Integer.parseInt(splitStr[i]);
	}
	
	public void setStartingUncommons(String rarity){
		String[] splitStr = rarity.split(",");
		
		for(int i = 0; i < numDiff; i++)
			this.startingUncommons[i] = Integer.parseInt(splitStr[i]);
	}
	
	public void setStartingRares(String rarity){
		String[] splitStr = rarity.split(",");
		
		for(int i = 0; i < numDiff; i++)
			this.startingRares[i] = Integer.parseInt(splitStr[i]);
	}
	
	public void setStartingCredits(String credits){
		this.startingCredits = Integer.parseInt(credits);
	}
	
	public void setNumCommon(String pack){
		this.boosterPackCommon = Integer.parseInt(pack);
	}
	
	public void setNumUncommon(String pack){
		this.boosterPackUncommon = Integer.parseInt(pack);
	}
	
	public void setNumRares(String pack){
		this.boosterPackRare = Integer.parseInt(pack);
	}
	
	
	public void setMatchRewardBase(String match){
		this.matchRewardBase = Integer.parseInt(match);
	}
	
	public void setMatchRewardTotalWins(String match){
		this.matchRewardTotalWins = Double.parseDouble(match);
	}
	
	public void setMatchRewardNoLosses(String match){
		this.matchRewardNoLosses = Integer.parseInt(match);
	}
	
	public void setMatchRewardPoisonWinBonus(String match){
		this.matchRewardPoisonWinBonus = Integer.parseInt(match);
	}
	
	public void setMatchRewardMilledWinBonus(String match){
		this.matchRewardMilledWinBonus = Integer.parseInt(match);
	}
	
	public void setMatchRewardAltWinBonus(String match){
		this.matchRewardAltWinBonus = Integer.parseInt(match);
	}
	
	
	public void setMatchRewardWinFirst(String match){
		this.matchRewardWinOnFirstTurn = Integer.parseInt(match);
	}
	
	public void setMatchRewardWinByFifth(String match){
		this.matchRewardWinByTurnFive = Integer.parseInt(match);
	}
	
	public void setMatchRewardWinByTen(String match){
		this.matchRewardWinByTurnTen = Integer.parseInt(match);
	}
	
	public void setMatchRewardWinByFifteen(String match){
		this.matchRewardWinByTurnFifteen = Integer.parseInt(match);
	}
	
	public void setMatchMullToZero(String match){
		this.matchRewardMullToZero = Integer.parseInt(match);
	}
}
