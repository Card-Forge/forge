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
				Card c = AllZone.CardFactory.getCard(s, AllZone.ComputerPlayer);
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
			else if (qd.getSelectedPet().equals("Bird") && qd.getBirdPetLevel() > 0)
				list.add(getBirdPetToken(qd.getBirdPetLevel()));
			else if (qd.getSelectedPet().equals("Hound") && qd.getHoundPetLevel() > 0)
				list.add(getHoundPetToken(qd.getHoundPetLevel()));
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
        
        c.setController(AllZone.HumanPlayer);
        c.setOwner(AllZone.HumanPlayer);
        
        c.setManaCost("G");
        c.addColor("G");
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
                    //AllZone.GameAction.gainLife(crd.getController(), 1);
                	crd.getController().gainLife(1);
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
        
        c.setController(AllZone.HumanPlayer);
        c.setOwner(AllZone.HumanPlayer);
        
        c.setManaCost("G");
        c.addColor("G");
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
        
        c.setController(AllZone.HumanPlayer);
        c.setOwner(AllZone.HumanPlayer);
        
        c.setManaCost("B");
        c.addColor("B");
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
	
	public static Card getBirdPetToken(int level)
	{
		String imageName = "";
		int baseAttack = 0;
		int baseDefense = 0;
		
		if (level == 1)
		{
			imageName = "W 0 1 Bird Pet";
			baseDefense = 1;
			baseAttack = 0;
		}
		else if (level == 2)
		{
			imageName = "W 1 1 Bird Pet";
			baseDefense = 1;
			baseAttack = 1;
		}
		else if (level == 3)
		{
			imageName = "W 2 1 Bird Pet";
			baseDefense = 1;
			baseAttack = 2;
		}
		else if (level == 4)
		{
			imageName = "W 2 1 Bird Pet First Strike";
			baseDefense = 1;
			baseAttack = 2;
		}
		

        Card c = new Card();
        c.setName("Bird Pet");
        
        c.setImageName(imageName);
        
        c.setController(AllZone.HumanPlayer);
        c.setOwner(AllZone.HumanPlayer);
        
        c.setManaCost("W");
        c.addColor("W");
        c.setToken(true);

        c.addType("Creature");
        c.addType("Bird");
        c.addType("Pet");
        
        c.addIntrinsicKeyword("Flying");
        
        if (level >= 4)
        	c.addIntrinsicKeyword("First Strike");
        
        c.setBaseAttack(baseAttack);
        c.setBaseDefense(baseDefense);
        
        return c;
	}
	
	public static Card getHoundPetToken(int level)
	{
		String imageName = "";
		int baseAttack = 0;
		int baseDefense = 0;
		
		if (level == 1)
		{
			imageName = "R 1 1 Hound Pet";
			baseDefense = 1;
			baseAttack = 0;
		}
		else if (level == 2)
		{
			imageName = "R 1 1 Hound Pet Haste";
			baseDefense = 1;
			baseAttack = 1;
		}
		else if (level == 3)
		{
			imageName = "R 2 1 Hound Pet";
			baseDefense = 1;
			baseAttack = 2;
		}
		else if (level == 4)
		{
			imageName = "R 2 1 Hound Pet Alone";
			baseDefense = 1;
			baseAttack = 2;
		}
		

        Card c = new Card();
        c.setName("Hound Pet");
        
        c.setImageName(imageName);
        
        c.setController(AllZone.HumanPlayer);
        c.setOwner(AllZone.HumanPlayer);
        
        c.setManaCost("R");
        c.addColor("R");
        c.setToken(true);

        c.addType("Creature");
        c.addType("Hound");
        c.addType("Pet");
        
        if (level >= 2)
        	c.addIntrinsicKeyword("Haste");
        
        if (level >= 4)
        	c.addIntrinsicKeyword("Whenever this creature attacks alone, it gets +2/+0 until end of turn.");
        
        c.setBaseAttack(baseAttack);
        c.setBaseDefense(baseDefense);
        
        return c;
	}
	
	public static ArrayList<String> getPetNames(QuestData questData)
	{
		ArrayList<String> list = new ArrayList<String>();
		if (questData.getWolfPetLevel() > 0)
			list.add("Wolf");
		if (questData.getCrocPetLevel() > 0)
			list.add("Croc");
		if (questData.getBirdPetLevel() > 0)
			list.add("Bird");
		if (questData.getHoundPetLevel() > 0)
			list.add("Hound");
		
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
			Card c = AllZone.CardFactory.getCard("Adventuring Gear", AllZone.HumanPlayer);
			humanList.add(c);
			
			qa.setHuman(humanList);

			for (int i=0;i<2;i++)
				qa.addCompy("Savannah Lions");
			
			qa.setCardRewardList(pack.getRare(3, 6));
		}
		
		else if (id == 2) //Blue Dungeon
		{
			for (int i=0;i<3;i++)
				qa.addCompy("Merfolk of the Pearl Trident");
			
			qa.setCardRewardList(pack.getRare(3, 3));
		}
		
		else if (id == 3) //Black Dungeon
		{
			qa.setCardRewardList(pack.getRare(3, 2));
		}
		
		else if (id == 4) //Red Dungeon
		{
			for (int i=0;i<3;i++)
				qa.addCompy("Mons's Goblin Raiders");
			
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
			Card c = AllZone.CardFactory.getCard("Trailblazer's Boots", AllZone.HumanPlayer);
			humanList.add(c);
			
			qa.setHuman(humanList);
			qa.setCardRewardList(pack.getRare(3, 0));			
		}
		
		else if (id == 8)
		{
			CardList humanList = new CardList();
			for (int i=0;i<3;i++)
			{
				//CANNOT use makeToken because of WheneverKeyword
				Card c = new Card();
		        c.setName("Sheep");
		        c.setImageName("G 0 1 Sheep");
		        
		        c.setController(AllZone.HumanPlayer);
		        c.setOwner(AllZone.HumanPlayer);
		        
		        c.setManaCost("G");
		        c.addColor("G");
		        c.setToken(true);
		        
		        c.addType("Creature");
		        c.addType("Sheep");
		        
		        c.setBaseAttack(0);
		        c.setBaseDefense(1);
			
				humanList.add(c);
			}
			qa.setHuman(humanList);
			qa.setCardRewardList(pack.getRare(3));			
		}
		
		else if (id == 9)
		{
			CardList humanList = new CardList();
			Card c = AllZone.CardFactory.getCard("Trusty Machete", AllZone.HumanPlayer);
			humanList.add(c);
			
			qa.setHuman(humanList);
			
			for (int i=0;i<3;i++)
				qa.addCompy("Wall of Wood");
			
			qa.setCardRewardList(pack.getRare(4, 4));			
		}
		
		else if (id == 10)
		{
			CardList humanList = new CardList();
			
			Card crd = AllZone.CardFactory.getCard("Wall of Spears", AllZone.HumanPlayer);
			humanList.add(crd);
			
			for (int i=0;i<3;i++)
			{
				Card c = new Card();
		        c.setName("Citizen");
		        c.setImageName("W 1 1 Citizen");
		        
		        c.setController(AllZone.HumanPlayer);
		        c.setOwner(AllZone.HumanPlayer);
		        
		        c.setManaCost("W");
		        c.addColor("W");
		        c.setToken(true);
		        
		        c.addType("Creature");
		        c.addType("Citizen");
		        
		        c.setBaseAttack(1);
		        c.setBaseDefense(1);
				
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
			Card c = AllZone.CardFactory.getCard("Seal of Cleansing", AllZone.HumanPlayer);
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
				Card c = new Card();
		        c.setName("Soldier Ally");
		        c.setImageName("W 1 1 Soldier Ally");
		        
		        c.setController(AllZone.HumanPlayer);
		        c.setOwner(AllZone.HumanPlayer);
		        
		        c.setManaCost("W");
		        c.addColor("W");
		        c.setToken(true);
		        
		        c.addType("Creature");
		        c.addType("Soldier");
		        c.addType("Ally");
		        
		        c.setBaseAttack(1);
		        c.setBaseDefense(1);
			
				
				humanList.add(c);
			}
			qa.setHuman(humanList);
			
			
			qa.addCompy("Lowland Giant");
			
			qa.setCardRewardList(pack.getRare(4));
		}
		
		else if (id == 13)  // The Court Jester
		{
			CardList humanList = new CardList();
			Card c = AllZone.CardFactory.getCard("Sensei's Divining Top", AllZone.HumanPlayer);
			humanList.add(c);
			
			qa.setHuman(humanList);
			
			qa.addCompy("Teferi's Puzzle Box");
			
			qa.setCardRewardList(pack.getRare(4, 0));
		}
		
		else if (id == 14)  // Ancient Battlefield
		{
			CardList humanList = new CardList();
			String humanSetupCards[] = {"Glasses of Urza", "Blight Sickle"};
			
			for (int i = 0; i < 2; i ++)
			{
				Card c = AllZone.CardFactory.getCard(humanSetupCards[i], AllZone.HumanPlayer);
				humanList.add(c);
			}
			qa.setHuman(humanList);
			
			// qa.addCompy("Bad Moon");
			// qa.addCompy("Wall of Brambles");
			
            String compySetupCards[] = {"Bad Moon", "Wall of Brambles"};
            
            for (int i = 0; i < 2; i ++)
            {
                qa.addCompy(compySetupCards[i]);
            }
			
			qa.setCardRewardList(pack.getRare(4));
		}
		
		else if (id == 15)  // Don't Play With Matches
		{
			CardList humanList = new CardList();
			String humanSetupCards[] = {"Mudbutton Torchrunner", "Scuzzback Scrapper"};
			
			for (int i = 0; i < 2; i ++)
			{
				Card c = AllZone.CardFactory.getCard(humanSetupCards[i], AllZone.HumanPlayer);
				humanList.add(c);
			}
			qa.setHuman(humanList);
			
			// qa.addCompy("Heedless One");
			// qa.addCompy("Norwood Archers");
			// qa.addCompy("Wildslayer Elves");
			
			String compySetupCards[] = {"Heedless One", "Norwood Archers", "Wildslayer Elves"};
            
            for (int i = 0; i < 3; i ++)
            {
                qa.addCompy(compySetupCards[i]);
            }
			
			qa.setCardRewardList(pack.getRare(4, 5));
		}
		
		else if (id == 16)  // Mines of Kazum Durl
		{
			CardList humanList = new CardList();
			String humanSetupCards[] = {"Dwarven Demolition Team", "Dwarven Pony", "Dwarven Trader"};
			
			for (int i = 0; i < 3; i ++)
			{
				Card c = AllZone.CardFactory.getCard(humanSetupCards[i], AllZone.HumanPlayer);
				humanList.add(c);
			}
			qa.setHuman(humanList);
			
			// qa.addCompy("Wall of Earth");
			// qa.addCompy("Wall of Air");
			// qa.addCompy("Wall of Ice");
			// qa.addCompy("Wall of Light");
			// qa.addCompy("Carrion Wall");
			// qa.addCompy("Steel Wall");
			
			String compySetupCards[] = 
				{"Wall of Earth", "Wall of Air", "Wall of Ice", "Wall of Light", "Carrion Wall" ,"Steel Wall"};
            
            for (int i = 0; i < 6; i ++)
            {
                qa.addCompy(compySetupCards[i]);
            }
			
			qa.setCardRewardList(pack.getRare(4, 4));
		}
		
		else if (id == 17)  // House Party
		{
			CardList humanList = new CardList();
			String humanSetupCards[] = {"Hopping Automaton", "Honden of Life's Web", "Forbidden Orchard"};
			
			for (int i = 0; i < 3; i ++)
			{
				Card c = AllZone.CardFactory.getCard(humanSetupCards[i], AllZone.HumanPlayer);
				humanList.add(c);
			}
			qa.setHuman(humanList);
			
			String compySetupCards[] = {"Honden of Infinite Rage", "Mikokoro, Center of the Sea", "Tidehollow Strix"};
			
			for (int i = 0; i < 3; i ++)
			{
				qa.addCompy(compySetupCards[i]);
			}
			
			qa.setCardRewardList(pack.getRare(4, 1));
		}
		
		else if (id == 18)  // Crows in the Field
		{
			CardList humanList = new CardList();
			String humanSetupCards[] = {"Straw Soldiers", "Femeref Archers", "Moonglove Extract"};
			
			for (int i = 0; i < 3; i ++)
			{
				Card c = AllZone.CardFactory.getCard(humanSetupCards[i], AllZone.HumanPlayer);
				humanList.add(c);
			}
			qa.setHuman(humanList);
			
			String compySetupCards[] = {"Defiant Falcon", "Soulcatcher", "Storm Crow", "Hypnotic Specter"};
			
			for (int i = 0; i < 4; i ++)
			{
				qa.addCompy(compySetupCards[i]);
			}
			
			qa.setCardRewardList(pack.getRare(5));
		}
		
		else if (id == 19)  // The Desert Caravan
		{
			CardList humanList = new CardList();
			String humanSetupCards[] = {"Spidersilk Net", "Dromad Purebred"};
			
			for (int i = 0; i < 2; i ++)
			{
				Card c = AllZone.CardFactory.getCard(humanSetupCards[i], AllZone.HumanPlayer);
				humanList.add(c);
			}
			qa.setHuman(humanList);
			
			String compySetupCards[] = {"Ambush Party", "Ambush Party", "Gnat Alley Creeper", "Ambush Party", "Ambush Party"};
			
			for (int i = 0; i < 5; i ++)
			{
				qa.addCompy(compySetupCards[i]);
			}
			
			qa.setCardRewardList(pack.getRare(5));
		}
		
		else if (id == 20)  // Blood Oath
		{
			CardList humanList = new CardList();
			String humanSetupCards[] = {"Counterbalance", "Hatching Plans", "Ley Druid"};
			
			for (int i = 0; i < 3; i ++)
			{
				Card c = AllZone.CardFactory.getCard(humanSetupCards[i], AllZone.HumanPlayer);
				humanList.add(c);
			}
			qa.setHuman(humanList);
			
			String compySetupCards[] = {"Ior Ruin Expedition", "Oversold Cemetery", "Trapjaw Kelpie"};
			
			for (int i = 0; i < 3; i ++)
			{
				qa.addCompy(compySetupCards[i]);
			}
			
			qa.setCardRewardList(pack.getRare(5, 1));
		}
		
		else if (id == 21) // Private Domain
		{
			CardList humanList = new CardList();

			humanList.add(AllZone.CardFactory.getCard("Strip Mine", AllZone.HumanPlayer));
			
			qa.setHuman(humanList);
			
			String compySetupCards[] = {"Plains", "Island", "Swamp", "Mountain", "Forest"};
			
			for (int i = 0; i < 5; i ++)
				qa.addCompy(compySetupCards[i]);
			
			qa.setCardRewardList(pack.getRare(6));
		}

		else if (id == 22) // Pied Piper
		{
			CardList humanList = new CardList();
			String humanSetupCards[] = {"Volunteer Militia", "Land Tax", "Elvish Farmer", "An-Havva Township"};
			
			for (int i = 0; i < 4; i ++)
			{
				Card c = AllZone.CardFactory.getCard(humanSetupCards[i], AllZone.HumanPlayer);
				humanList.add(c);
			}
			qa.setHuman(humanList);
			
			String compySetupCards[] = {"Darksteel Citadel", "Relentless Rats"};
			
			for (int i = 0; i < 2; i ++)
				qa.addCompy(compySetupCards[i]);
			
			qa.setCardRewardList(pack.getRare(3));
		}
			
	}
	
}//QuestUtil
