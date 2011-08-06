package forge.quest.data;

import forge.properties.ForgeProps;
import forge.properties.NewConstants.QUEST;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.Serializable;

public class QuestPreferences implements Serializable {
	private static final long serialVersionUID = 3266336025656577905L;

	private static int numDiff = 4; 
	
	// Descriptive difficulty names
	private static String[] sDifficulty = { "Easy", "Normal", "Hard", "Very Hard" };
	
	// Default match wins it takes to gain a booster
	private static int[] winsForBooster = { 1, 1, 2, 2 };
	private static int[] winsForRankIncrease = { 1, 2, 3, 4 };
	private static int[] winsForMediumAI = { 6, 6, 11, 11 };
	private static int[] winsForHardAI = { 9, 9, 21, 21 };
	private static int[] winsForVeryHardAI = { 29, 29, 31, 31 };
	
	// Default starting land for a quest
	private static int startingBasicLand = 20;
	private static int startingSnowBasicLand = 20;

	// Default starting amount of each rarity
	private static int[] startingCommons = {45, 40, 40, 40};
	private static int[] startingUncommons = {20, 15, 15, 15};
	private static int[] startingRares = {10, 10, 10, 10};
	
	private static int startingCredits = 250;

	private static int boosterPackRare = 1;
	private static int boosterPackUncommon = 3;
	private static int boosterPackCommon = 9;

	private static int matchRewardBase = 10;
	private static double matchRewardTotalWins = 0.3;
	private static int matchRewardNoLosses = 10;
	
	private static int matchRewardPoisonWinBonus = 50;
	private static int matchRewardMilledWinBonus = 40;
	private static int matchRewardAltWinBonus = 100;
	
	private static int matchRewardWinOnFirstTurn = 1500;
	private static int matchRewardWinByTurnFive = 250;
	private static int matchRewardWinByTurnTen = 50;
	private static int matchRewardWinByTurnFifteen = 5;
	private static int matchRewardMullToZero = 500;
	
	
	static {
		// if quest.prefs exists
		grabPrefsFromFile();
	}
	
	public static void grabPrefsFromFile(){
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
	
	public static String[] getDifficulty(){
		return sDifficulty;
	}
	
	public static String getDifficulty(int index){
		return sDifficulty[index];
	}
	
	public static int getWinsForBooster(int index){
		return winsForBooster[index];
	}
	
	public static int getWinsForRankIncrease(int index){
		return winsForRankIncrease[index];
	}
	
	public static int getWinsForMediumAI(int index){
		return winsForMediumAI[index];
	}
	
	public static int getWinsForHardAI(int index){
		return winsForHardAI[index];
	}
	
	public static int getWinsForVeryHardAI(int index){
		return winsForVeryHardAI[index];
	}
	
	public static int getStartingBasic(){
		return startingBasicLand;
	}
	
	public static int getStartingSnowBasic(){
		return startingSnowBasicLand;
	}
	
	public static int getStartingCommons(int index){
		return startingCommons[index];
	}
	
	public static int getStartingUncommons(int index){
		return startingUncommons[index];
	}
	
	public static int getStartingRares(int index){
		return startingRares[index];
	}
	
	public static int getStartingCredits(){
		return startingCredits;
	}
	
	public static int getNumCommon(){
		return boosterPackCommon;
	}
	
	public static int getNumUncommon(){
		return boosterPackUncommon;
	}
	
	public static int getNumRare(){
		return boosterPackRare;
	}
	
	
	public static int getMatchRewardBase(){
		return matchRewardBase;
	}
	
	public static double getMatchRewardTotalWins(){
		return matchRewardTotalWins;
	}
	
	public static int getMatchRewardNoLosses(){
		return matchRewardNoLosses;
	}
	
	public static int getMatchRewardPoisonWinBonus(){
		return matchRewardPoisonWinBonus;
	}
	
	public static int getMatchRewardMilledWinBonus(){
		return matchRewardMilledWinBonus;
	}
	
	public static int getMatchRewardAltWinBonus(){
		return matchRewardAltWinBonus;
	}
	
	
	public static int getMatchRewardWinFirst(){
		return matchRewardWinOnFirstTurn;
	}
	
	public static int getMatchRewardWinByFifth(){
		return matchRewardWinByTurnFive;
	}
	
	public static int getMatchRewardWinByTen(){
		return matchRewardWinByTurnTen;
	}
	
	public static int getMatchRewardWinByFifteen(){
		return matchRewardWinByTurnFifteen;
	}
	
	public static int getMatchMullToZero(){
		return matchRewardMullToZero;
	}
	
	
	// setters
	public static void setDifficulty(String diff){
		sDifficulty = diff.split(",");
	}
	
	public static void setWinsForBooster(String wins){
		String[] winsStr = wins.split(",");
		
		for(int i = 0; i < numDiff; i++)
			winsForBooster[i] = Integer.parseInt(winsStr[i]);
	}
	
	public static void setWinsForRank(String wins){
		String[] winsStr = wins.split(",");
		
		for(int i = 0; i < numDiff; i++)
			winsForRankIncrease[i] = Integer.parseInt(winsStr[i]);
	}
	
	public static void setWinsForMediumAI(String wins){
		String[] winsStr = wins.split(",");
		
		for(int i = 0; i < numDiff; i++)
			winsForMediumAI[i] = Integer.parseInt(winsStr[i]);
	}
	
	public static void setWinsForHardAI(String wins){
		String[] winsStr = wins.split(",");
		
		for(int i = 0; i < numDiff; i++)
			winsForHardAI[i] = Integer.parseInt(winsStr[i]);
	}
	
	public static void setStartingBasic(String land){
		startingBasicLand = Integer.parseInt(land);
	}
	
	public static void setStartingSnowBasic(String land){
		startingSnowBasicLand = Integer.parseInt(land);
	}
	
	public static void setStartingCommons(String rarity){
		String[] splitStr = rarity.split(",");
		
		for(int i = 0; i < numDiff; i++)
			startingCommons[i] = Integer.parseInt(splitStr[i]);
	}
	
	public static void setStartingUncommons(String rarity){
		String[] splitStr = rarity.split(",");
		
		for(int i = 0; i < numDiff; i++)
			startingUncommons[i] = Integer.parseInt(splitStr[i]);
	}
	
	public static void setStartingRares(String rarity){
		String[] splitStr = rarity.split(",");
		
		for(int i = 0; i < numDiff; i++)
			startingRares[i] = Integer.parseInt(splitStr[i]);
	}
	
	public static void setStartingCredits(String credits){
		startingCredits = Integer.parseInt(credits);
	}
	
	public static void setNumCommon(String pack){
		boosterPackCommon = Integer.parseInt(pack);
	}
	
	public static void setNumUncommon(String pack){
		boosterPackUncommon = Integer.parseInt(pack);
	}
	
	public static void setNumRares(String pack){
		boosterPackRare = Integer.parseInt(pack);
	}
	
	
	public static void setMatchRewardBase(String match){
		matchRewardBase = Integer.parseInt(match);
	}
	
	public static void setMatchRewardTotalWins(String match){
		matchRewardTotalWins = Double.parseDouble(match);
	}
	
	public static void setMatchRewardNoLosses(String match){
		matchRewardNoLosses = Integer.parseInt(match);
	}
	
	public static void setMatchRewardPoisonWinBonus(String match){
		matchRewardPoisonWinBonus = Integer.parseInt(match);
	}
	
	public static void setMatchRewardMilledWinBonus(String match){
		matchRewardMilledWinBonus = Integer.parseInt(match);
	}
	
	public static void setMatchRewardAltWinBonus(String match){
		matchRewardAltWinBonus = Integer.parseInt(match);
	}
	
	
	public static void setMatchRewardWinFirst(String match){
		matchRewardWinOnFirstTurn = Integer.parseInt(match);
	}
	
	public static void setMatchRewardWinByFifth(String match){
		matchRewardWinByTurnFive = Integer.parseInt(match);
	}
	
	public static void setMatchRewardWinByTen(String match){
		matchRewardWinByTurnTen = Integer.parseInt(match);
	}
	
	public static void setMatchRewardWinByFifteen(String match){
		matchRewardWinByTurnFifteen = Integer.parseInt(match);
	}
	
	public static void setMatchMullToZero(String match){
		matchRewardMullToZero = Integer.parseInt(match);
	}
}
