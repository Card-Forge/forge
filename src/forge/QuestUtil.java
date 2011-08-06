package forge;

import java.util.ArrayList;

public class QuestUtil {
	
	public static int getLife(QuestData qd)
	{
		return qd.getLife();
	}
	
	public static CardList getComputerCreatures(QuestData qd)
	{
		return new CardList();
	}
	
	public static CardList getComputerCreatures(QuestData qd, Quest_Assignment qa)
	{
		CardList list = new CardList();
		if (qa!=null)
		{
			ArrayList<String> compCards = qa.getCompy();
			
			for (String s:compCards)
			{
				Card c = AllZone.CardFactory.getCard(s, Constant.Player.Computer);
				list.add(c);
			}
		}
		return list;
	}
	
	public static CardList getHumanPlantAndPet(QuestData qd)
	{
		CardList list = new CardList();
		
		if (qd.getSelectedPet() != null)
		{
			if (qd.getSelectedPet().equals("No Plant/Pet"))
				return list;
			
			if (qd.getSelectedPet().equals("Wolf") && qd.getWolfPetLevel() > 0)
				list.add(getWolfPetToken(qd.getWolfPetLevel()));
			else if (qd.getSelectedPet().equals("Croc") && qd.getCrocPetLevel() > 0)
				list.add(getCrocPetToken(qd.getCrocPetLevel()));
		}
		
		if (qd.getPlantLevel() > 0) 
			list.add(getPlantToken(qd.getPlantLevel()));

		return list;
	}
	
	public static CardList getHumanPlantAndPet(QuestData qd, Quest_Assignment qa)
	{
		CardList list = getHumanPlantAndPet(qd);
		
		if (qa!=null)
			list.addAll(qa.getHuman().toArray());
		
		return list;
	}
	
	//makeToken(String name, String imageName, Card source, String manaCost, String[] types, int baseAttack, int baseDefense, String[] intrinsicKeywords) {
	
	public static Card getPlantToken(int level)
	{
		String imageName = "";
		int baseAttack = 0;
		int baseDefense = 0;
		
		String keyword = "";
		
		if (level == 1)
		{
			imageName = "G 0 1 Plant Wall";
			baseDefense = 1;
		}
		else if (level == 2)
		{
			imageName = "G 0 2 Plant Wall";
			baseDefense = 2;
		}
		else if (level == 3)
		{
			imageName = "G 0 3 Plant Wall";
			baseDefense = 3;
		}
		else if (level == 4)
		{
			imageName = "G 1 3 Plant Wall";
			baseDefense = 3;
			baseAttack = 1;
		}
		else if (level == 5)
		{
			imageName = "G 1 3 Plant Wall Deathtouch";
			baseDefense = 3;
			baseAttack = 1;
			keyword = "Deathtouch";
		}
		
		else if (level == 6)
		{
			imageName = "G 1 4 Plant Wall";
			baseDefense = 4;
			baseAttack = 1;
			keyword = "Deathtouch";
		}
		
		

        Card c = new Card();
        c.setName("Plant Wall");
        
        c.setImageName(imageName);
        
        c.setController(Constant.Player.Human);
        c.setOwner(Constant.Player.Human);
        
        c.setManaCost("G");
        c.setToken(true);

        c.addType("Creature");
        c.addType("Plant");
        c.addType("Wall");
        
        c.addIntrinsicKeyword("Defender");
        if (!keyword.equals(""))
        	c.addIntrinsicKeyword("Deathtouch");
        
        c.setBaseAttack(baseAttack);
        c.setBaseDefense(baseDefense);
        
        if (level == 6)
        {
        	final Card crd = c;
        	final Ability_Tap ability = new Ability_Tap(c) {
				private static final long serialVersionUID = 7546242087593613719L;

				@Override
                public boolean canPlayAI() {
                    return AllZone.Phase.getPhase().equals(Constant.Phase.Main2);
                }
                
                @Override
                public void resolve() {
                    AllZone.GameAction.getPlayerLife(crd.getController()).addLife(1);
                }
            };
            c.addSpellAbility(ability);
        	ability.setDescription("tap: You gain 1 life.");
            ability.setStackDescription("Plant Wall - " + c.getController() + " gains 1 life.");
            c.setText("tap: You gain 1 life.");
        }
        
        
        return c;
	}//getPlantToken
	
	public static Card getWolfPetToken(int level)
	{
		String imageName = "";
		int baseAttack = 0;
		int baseDefense = 0;
		
		if (level == 1)
		{
			imageName = "G 1 1 Wolf Pet";
			baseDefense = 1;
			baseAttack = 1;
		}
		else if (level == 2)
		{
			imageName = "G 1 2 Wolf Pet";
			baseDefense = 2;
			baseAttack = 1;
		}
		else if (level == 3)
		{
			imageName = "G 2 2 Wolf Pet";
			baseDefense = 2;
			baseAttack = 2;
		}
		else if (level == 4)
		{
			imageName = "G 2 2 Wolf Pet Flanking";
			baseDefense = 2;
			baseAttack = 2;
		}
		

        Card c = new Card();
        c.setName("Wolf Pet");
        
        c.setImageName(imageName);
        
        c.setController(Constant.Player.Human);
        c.setOwner(Constant.Player.Human);
        
        c.setManaCost("G");
        c.setToken(true);

        c.addType("Creature");
        c.addType("Wolf");
        c.addType("Pet");
        
        if (level >= 4)
        	c.addIntrinsicKeyword("Flanking");
        
        c.setBaseAttack(baseAttack);
        c.setBaseDefense(baseDefense);
        
        return c;
	}//getWolfPetToken
	
	public static Card getCrocPetToken(int level)
	{
		String imageName = "";
		int baseAttack = 0;
		int baseDefense = 0;
		
		if (level == 1)
		{
			imageName = "B 1 1 Crocodile Pet";
			baseDefense = 1;
			baseAttack = 1;
		}
		else if (level == 2)
		{
			imageName = "B 2 1 Crocodile Pet";
			baseDefense = 1;
			baseAttack = 2;
		}
		else if (level == 3)
		{
			imageName = "B 3 1 Crocodile Pet";
			baseDefense = 1;
			baseAttack = 3;
		}
		else if (level == 4)
		{
			imageName = "B 3 1 Crocodile Pet Swampwalk";
			baseDefense = 1;
			baseAttack = 3;
		}
		

        Card c = new Card();
        c.setName("Crocodile Pet");
        
        c.setImageName(imageName);
        
        c.setController(Constant.Player.Human);
        c.setOwner(Constant.Player.Human);
        
        c.setManaCost("B");
        c.setToken(true);

        c.addType("Creature");
        c.addType("Crocodile");
        c.addType("Pet");
        
        if (level >= 4)
        	c.addIntrinsicKeyword("Swampwalk");
        
        c.setBaseAttack(baseAttack);
        c.setBaseDefense(baseDefense);
        
        return c;
	}//getCrocPetToken
	
	public static ArrayList<String> getPetNames(QuestData questData)
	{
		ArrayList<String> list = new ArrayList<String>();
		if (questData.getWolfPetLevel() > 0)
			list.add("Wolf");
		if (questData.getCrocPetLevel() > 0)
			list.add("Croc");
		return list;
	}
	
	public static void setupQuest(Quest_Assignment qa)
	{
		/*
		 *  Gold = 0
		 *  Colorless = 1
		 *  Black = 2
		 *  Blue = 3
		 *  Green = 4
		 *  Red = 5
		 *  White = 6
		 */
		
		QuestData_BoosterPack pack = new QuestData_BoosterPack(); 
		qa.clearCompy();
		
		int id = qa.getId();
		if (id == 1) //White Dungeon
		{
			CardList humanList = new CardList();
			Card c = AllZone.CardFactory.getCard("Adventuring Gear", Constant.Player.Human);
			humanList.add(c);
			
			qa.setHuman(humanList);

			for (int i=0;i<2;i++)
				qa.addCompy("Savannah Lions");
			
			qa.setCardRewardList(pack.getRare(3, 6));
		}
		else if (id == 2) //Blue Dungeon
		{
			qa.setCardRewardList(pack.getRare(3, 3));
		}
		else if (id == 3) //Black Dungeon
		{
			qa.setCardRewardList(pack.getRare(3, 2));
		}
		else if (id == 4) //Red Dungeon
		{
			qa.setCardRewardList(pack.getRare(3, 5));
		}
		else if (id == 5) //Green Dungeon
		{
			qa.setCardRewardList(pack.getRare(3, 4));
		}
		else if (id == 6) //Colorless Dungeon
		{
			for (int i=0;i<2;i++)
				qa.addCompy("Ornithopter");
			qa.setCardRewardList(pack.getRare(3, 1));
		}
		else if (id == 7) //Gold Dungeon
		{
			CardList humanList = new CardList();
			Card c = AllZone.CardFactory.getCard("Trailblazer's Boots", Constant.Player.Human);
			humanList.add(c);
			
			qa.setHuman(humanList);
			qa.setCardRewardList(pack.getRare(3, 0));			
		}
		else if (id == 8)
		{
			CardList humanList = new CardList();
			for (int i=0;i<3;i++)
			{
				Card c = CardFactoryUtil.makeToken("Sheep", "G 0 1 Sheep", Constant.Player.Human, "G", 
													new String[] {"Creature","Sheep"}, 0, 1, new String[]{""}).get(0);
				humanList.add(c);
			}
			qa.setHuman(humanList);
			qa.setCardRewardList(pack.getRare(3));			
		}
		else if (id == 9)
		{
			CardList humanList = new CardList();
			Card c = AllZone.CardFactory.getCard("Trusty Machete", Constant.Player.Human);
			humanList.add(c);
			
			qa.setHuman(humanList);
			
			for (int i=0;i<3;i++)
				qa.addCompy("Wall of Wood");
			
			qa.setCardRewardList(pack.getRare(4, 4));			
		}
		else if (id == 10)
		{
			CardList humanList = new CardList();
			
			Card c = AllZone.CardFactory.getCard("Wall of Spears", Constant.Player.Human);
			humanList.add(c);
			
			for (int i=0;i<3;i++)
			{
				c = CardFactoryUtil.makeToken("Citizen", "W 1 1 Citizen", Constant.Player.Human, "W", 
													new String[] {"Creature","Citizen"}, 1, 1, new String[]{""}).get(0);
				humanList.add(c);
			}
			
			qa.setHuman(humanList);
			
			for (int i=0;i<3;i++)
				qa.addCompy("Scathe Zombies");
			qa.addCompy("Mass of Ghouls");
			
			qa.setCardRewardList(pack.getRare(4, 2));	
		}
		else if (id == 11)  // The King's Contest
		{
			CardList humanList = new CardList();
			Card c = AllZone.CardFactory.getCard("Seal of Cleansing", Constant.Player.Human);
			humanList.add(c);
			
			qa.setHuman(humanList);
			
			qa.addCompy("Loyal Retainers");
			
			qa.setCardRewardList(pack.getRare(3));
		}
		else if (id == 12)  // Barroom Brawl
		{
			CardList humanList = new CardList();
			for (int i = 0; i < 3; i ++)
			{
				Card c = CardFactoryUtil.makeToken("Soldier Ally", "W 1 1 Soldier Ally", Constant.Player.Human, "W", 
							new String[] {"Creature", "Soldier", "Ally"}, 1, 1, new String[] {""}).get(0);

				humanList.add(c);
			}
			qa.setHuman(humanList);
			
			
			qa.addCompy("Lowland Giant");
			
			qa.setCardRewardList(pack.getRare(4));
		}
		else if (id == 13)  // The Court Jester
		{
			CardList humanList = new CardList();
			Card c = AllZone.CardFactory.getCard("Sensei's Divining Top", Constant.Player.Human);
			humanList.add(c);
			
			qa.setHuman(humanList);
			
			qa.addCompy("Teferi's Puzzle Box");
			
			qa.setCardRewardList(pack.getRare(4, 0));
		}
		else if (id == 14)  // Ancient Battlefield
		{
			CardList humanList = new CardList();
			String cardsInPlay[] = {"Glasses of Urza", "Blight Sickle"};
			
			for (int i = 0; i < 2; i ++)
			{
				Card c = AllZone.CardFactory.getCard(cardsInPlay[i], Constant.Player.Human);
				humanList.add(c);
			}
			qa.setHuman(humanList);
			
			qa.addCompy("Bad Moon");
			qa.addCompy("Wall of Brambles");
			
			qa.setCardRewardList(pack.getRare(4));
		}
		else if (id == 15)  // Don't Play With Matches
		{
			CardList humanList = new CardList();
			String cardsInPlay[] = {"Mudbutton Torchrunner", "Scuzzback Scrapper"};
			
			for (int i = 0; i < 2; i ++)
			{
				Card c = AllZone.CardFactory.getCard(cardsInPlay[i], Constant.Player.Human);
				humanList.add(c);
			}
			qa.setHuman(humanList);
			
			qa.addCompy("Heedless One");
			qa.addCompy("Norwood Archers");
			qa.addCompy("Wildslayer Elves");
			
			qa.setCardRewardList(pack.getRare(4, 5));
		}
		else if (id == 16)  // Mines of Kazum Durl
		{
			CardList humanList = new CardList();
			String cardsInPlay[] = {"Dwarven Demolition Team", "Dwarven Pony", "Dwarven Trader"};
			
			for (int i = 0; i < 3; i ++)
			{
				Card c = AllZone.CardFactory.getCard(cardsInPlay[i], Constant.Player.Human);
				humanList.add(c);
			}
			qa.setHuman(humanList);
			
			qa.addCompy("Wall of Earth");
			qa.addCompy("Wall of Air");
			qa.addCompy("Wall of Ice");
			qa.addCompy("Wall of Light");
			qa.addCompy("Carrion Wall");
			qa.addCompy("Steel Wall");
			
			qa.setCardRewardList(pack.getRare(4, 4));
		}
			
	}
	
}//QuestUtil
