package forge;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class GenerateConstructedMultiColorDeck
{
  private String color1;
  private String color2;
  private String color3;
  private String color4;
  private String color5;

  private Map<String,String> map = new HashMap<String,String>();
  private Map<String, String[]> multiMap = new HashMap<String, String[]>();
  
  public GenerateConstructedMultiColorDeck() 
  {
	  setupBasicLandMap();
	  setupMultiMap();
  }

  private void setupBasicLandMap()
  {
    map.put(Constant.Color.Black , "Swamp");
    map.put(Constant.Color.Blue  , "Island");
    map.put(Constant.Color.Green , "Forest");
    map.put(Constant.Color.Red   , "Mountain");
    map.put(Constant.Color.White , "Plains");
  }
  
  private void setupMultiMap()
  {
	  multiMap.put(Constant.Color.Black + Constant.Color.Blue, new String[] {"Underground Sea", "Watery Grave"});
	  multiMap.put(Constant.Color.Black + Constant.Color.Green, new String[] {"Bayou", "Overgrown Tomb"});
	  multiMap.put(Constant.Color.Black + Constant.Color.Red , new String[]{"Badlands", "Blood Crypt"});
	  multiMap.put(Constant.Color.Black + Constant.Color.White, new String[] {"Scrubland", "Godless Shrine"});
	  multiMap.put(Constant.Color.Blue  + Constant.Color.Black, new String[] {"Underground Sea", "Watery Grave"});
	  multiMap.put(Constant.Color.Blue  + Constant.Color.Green, new String[] {"Tropical Island", "Breeding Pool"});
	  multiMap.put(Constant.Color.Blue +Constant.Color.Red,  new String[]{"Volcanic Island", "Steam Vents"});
	  multiMap.put(Constant.Color.Blue +Constant.Color.White, new String[] {"Tundra", "Hallowed Fountain"});
	  multiMap.put(Constant.Color.Green +Constant.Color.Black, new String[] {"Bayou", "Overgrown Tomb"});
	  multiMap.put(Constant.Color.Green +Constant.Color.Blue, new String[] {"Tropical Island", "Breeding Pool"});
	  multiMap.put(Constant.Color.Green +Constant.Color.Red ,  new String[]{"Taiga", "Stomping Ground"});
	  multiMap.put(Constant.Color.Green +Constant.Color.White, new String[] {"Savannah", "Temple Garden"});
	  multiMap.put(Constant.Color.Red +Constant.Color.Black, new String[] {"Badlands", "Blood Crypt"});
	  multiMap.put(Constant.Color.Red +Constant.Color.Blue, new String[] {"Volcanic Island", "Steam Vents"});
	  multiMap.put(Constant.Color.Red +Constant.Color.Green,  new String[]{"Taiga", "Stomping Ground"});
	  multiMap.put(Constant.Color.Red +Constant.Color.White, new String[] {"Plateau", "Sacred Foundry"});
	  multiMap.put(Constant.Color.White +Constant.Color.Black, new String[] {"Scrubland", "Godless Shrine"});
	  multiMap.put(Constant.Color.White +Constant.Color.Blue, new String[] {"Tundra", "Hallowed Fountain"});
	  multiMap.put(Constant.Color.White +Constant.Color.Green,  new String[]{"Savannah", "Temple Garden"});
	  multiMap.put(Constant.Color.White +Constant.Color.Red , new String[] {"Plateau", "Sacred Foundry"});
  }
 
  
  public CardList generate3ColorDeck()
  {
    CardList deck;

    int check;

    do{
      deck = get3ColorDeck();
      check = deck.getType("Creature").size();

    }while(check < 16 || 24 < check);

    addLand(deck, 3);

    if(deck.size() != 60)
      throw new RuntimeException("GenerateConstructedDeck() : generateDeck() error, deck size it not 60, deck size is " +deck.size());

    return deck;
  }
  
  public CardList generate5ColorDeck()
  {
    CardList deck;

    /*
    int check;

    do{
      deck = get5ColorDeck();
      check = deck.getType("Creature").size();

    }while(check < 15 || 25 < check);
	*/

    deck = get5ColorDeck();
    
    addLand(deck, 5);

    if(deck.size() != 61)
      throw new RuntimeException("GenerateConstructedDeck() : generateDeck() error, deck size it not 60, deck size is " +deck.size());

    return deck;
  }
  
  private void addLand(CardList list, int colors)
  {
	if (colors==3)
	{
		int numberBasic = 2;
	    Card land;
	    for(int i = 0; i < numberBasic; i++)
	    {
	    
		      land = AllZone.CardFactory.getCard(map.get(color1).toString(), Constant.Player.Computer);
		      list.add(land);
		
		      land = AllZone.CardFactory.getCard(map.get(color2).toString(), Constant.Player.Computer);
		      list.add(land);
		      
		      land = AllZone.CardFactory.getCard(map.get(color3).toString(), Constant.Player.Computer);
		      list.add(land);
	    }
	    
	    int numberDual = 4;
	    for (int i = 0; i < numberDual;i++)
	    {
	    	land = AllZone.CardFactory.getCard(multiMap.get(color1+color2)[0], Constant.Player.Computer);
		    list.add(land);
		    	
		    land = AllZone.CardFactory.getCard(multiMap.get(color1+color3)[0], Constant.Player.Computer);
		    list.add(land);
		    
		    land = AllZone.CardFactory.getCard(multiMap.get(color2+color3)[0], Constant.Player.Computer);
		    list.add(land);
	    }
	    for (int i=0; i<2;i++)
	    {
		    land = AllZone.CardFactory.getCard(multiMap.get(color1+color2)[1], Constant.Player.Computer);
		    list.add(land);
		    	
		    land = AllZone.CardFactory.getCard(multiMap.get(color1+color3)[1], Constant.Player.Computer);
		    list.add(land);
		    
		    land = AllZone.CardFactory.getCard(multiMap.get(color2+color3)[1], Constant.Player.Computer);
		    list.add(land);
	    }
	}
	else if (colors == 5)
	{
		int numberBasic = 1;
	    Card land;
	    for(int i = 0; i < numberBasic; i++)
	    {
	    
		      land = AllZone.CardFactory.getCard(map.get(color1).toString(), Constant.Player.Computer);
		      list.add(land);
		
		      land = AllZone.CardFactory.getCard(map.get(color2).toString(), Constant.Player.Computer);
		      list.add(land);
		      
		      land = AllZone.CardFactory.getCard(map.get(color3).toString(), Constant.Player.Computer);
		      list.add(land);
		      
		      land = AllZone.CardFactory.getCard(map.get(color4).toString(), Constant.Player.Computer);
		      list.add(land);
		      
		      land = AllZone.CardFactory.getCard(map.get(color5).toString(), Constant.Player.Computer);
		      list.add(land);
	    }
	    
		
		int numberDual = 2;
	    for (int i = 0; i < numberDual;i++)
	    {
	    	land = AllZone.CardFactory.getCard(multiMap.get(color1+color2)[0], Constant.Player.Computer);
		    list.add(land);
		    	
		    land = AllZone.CardFactory.getCard(multiMap.get(color1+color3)[0], Constant.Player.Computer);
		    list.add(land);
		    
		    land = AllZone.CardFactory.getCard(multiMap.get(color1+color4)[0], Constant.Player.Computer);
		    list.add(land);
		    
		    land = AllZone.CardFactory.getCard(multiMap.get(color1+color5)[0], Constant.Player.Computer);
		    list.add(land);
		    
		    land = AllZone.CardFactory.getCard(multiMap.get(color2+color3)[0], Constant.Player.Computer);
		    list.add(land);
		    
		    land = AllZone.CardFactory.getCard(multiMap.get(color2+color4)[0], Constant.Player.Computer);
		    list.add(land);
		    
		    land = AllZone.CardFactory.getCard(multiMap.get(color2+color5)[0], Constant.Player.Computer);
		    list.add(land);
		    
		    land = AllZone.CardFactory.getCard(multiMap.get(color3+color4)[0], Constant.Player.Computer);
		    list.add(land);
		    
		    land = AllZone.CardFactory.getCard(multiMap.get(color3+color5)[0], Constant.Player.Computer);
		    list.add(land);
		    
		    land = AllZone.CardFactory.getCard(multiMap.get(color4+color5)[0], Constant.Player.Computer);
		    list.add(land);
	    }

	}
  }//addLand()
  private CardList getCards(int colors)
  {
    return filterBadCards(AllZone.CardFactory.getAllCards(), colors);
  }//getCards()
  
  private CardList get3ColorDeck()
  {
    CardList deck = get3Colors(getCards(3));

    CardList out = new CardList();
    deck.shuffle();

    //trim deck size down to 36 cards, presumes 24 land, for a total of 60 cards
    for(int i = 0; i < 36 && i < deck.size(); i++)
      out.add(deck.get(i));

    return out;
  }
  
  private CardList get5ColorDeck()
  {
    CardList deck = get5Colors(getCards(5));

    CardList out = new CardList();
    deck.shuffle();

    //trim deck size down to 36 cards, presumes 24 land, for a total of 60 cards
    for(int i = 0; i < 36 && i < deck.size(); i++)
      out.add(deck.get(i));

    return out;
  }
  
  private CardList get3Colors(CardList in)
  {
    int a;
    int b;
    int c;

    a = CardUtil.getRandomIndex(Constant.Color.onlyColors);
    do{
      b = CardUtil.getRandomIndex(Constant.Color.onlyColors);
      c = CardUtil.getRandomIndex(Constant.Color.onlyColors);
    }while(a == b || a == c || b == c );//do not want to get the same color thrice

    color1 = Constant.Color.onlyColors[a];
    color2 = Constant.Color.onlyColors[b];
    color3 = Constant.Color.onlyColors[c];

    CardList out = new CardList();
    out.addAll(CardListUtil.getColor(in, color1).toArray());
    out.addAll(CardListUtil.getColor(in, color2).toArray());
    out.addAll(CardListUtil.getColor(in, color3).toArray());
    out.shuffle();

    CardList artifact = in.filter(new CardListFilter()
    {
      public boolean addCard(Card c)
      {         
        //is this really a colorless artifact and not something
        //wierd like Sarcomite Myr which is a colored artifact
        return c.isArtifact() &&
         CardUtil.getColors(c).contains(Constant.Color.Colorless) &&
         !Gui_NewGame.removeArtifacts.isSelected();
      }
    });
    out.addAll(artifact.toArray());
   
    out = out.filter(new CardListFilter()
    {
      public boolean addCard(Card c)
      {
         if(c.isCreature() &&
            c.getNetAttack() <= 1 &&
            Gui_NewGame.removeSmallCreatures.isSelected())
         {
           return false;
         }
            
         return true;
      }
    });
   
    out = filterBadCards(out, 3);
    return out;
  }
  
  private CardList get5Colors(CardList in)
  {
	    
    color1 = Constant.Color.Black;
    color2 = Constant.Color.Blue;
    color3 = Constant.Color.Green;
    color4 = Constant.Color.Red;
    color5 = Constant.Color.White;

    CardList out = new CardList();
    /*
    out.addAll(CardListUtil.getColor(in, color1).toArray());
    out.addAll(CardListUtil.getColor(in, color2).toArray());
    out.addAll(CardListUtil.getColor(in, color3).toArray());
    out.addAll(CardListUtil.getColor(in, color4).toArray());
    out.addAll(CardListUtil.getColor(in, color5).toArray());
    */
    out.addAll(CardListUtil.getGoldCards(in).toArray());
    out.shuffle();

    CardList artifact = in.filter(new CardListFilter()
    {
      public boolean addCard(Card c)
      {         
        //is this really a colorless artifact and not something
        //wierd like Sarcomite Myr which is a colored artifact
        return c.isArtifact() &&
         CardUtil.getColors(c).contains(Constant.Color.Colorless) &&
         !Gui_NewGame.removeArtifacts.isSelected();
      }
    });
    out.addAll(artifact.toArray());
   
    out = out.filter(new CardListFilter()
    {
      public boolean addCard(Card c)
      {
         if(c.isCreature() &&
            c.getNetAttack() <= 1 &&
            Gui_NewGame.removeSmallCreatures.isSelected())
         {
           return false;
         }
            
         return true;
      }
    });
   
    out = filterBadCards(out, 3);
    return out;
  }
  

  private CardList filterBadCards(CardList list, int colors)
  {
    //remove "bad" and multi-colored cards
    final ArrayList<String> remove = new ArrayList<String>();
    remove.add("Sarcomite Myr");
    remove.add("Force of Savagery");
    remove.add("Darksteel Colossus");
    remove.add("Jokulhaups");
    remove.add("Steel Wall");
    remove.add("Ornithopter");
    remove.add("Amnesia");
    remove.add("Battle of Wits");
    remove.add("Ashes to Ashes");
    remove.add("Haunted Angel");
    remove.add("Sky Swallower");
    remove.add("Magus of the Library");
    remove.add("The Unspeakable");
    remove.add("Wall of Kelp");

    remove.add("Incendiary Command");
    remove.add("Memnarch");
    remove.add("Plague Wind");
    remove.add("Klaas, Elf Friend");
    remove.add("Delirium Skeins");

    remove.add("Undying Beast");
    remove.add("Wit's End");

    remove.add("Blinding Light");
    remove.add("Hymn to Tourach");
    
    //cards that slow the computer down
    
    remove.add("Anger");
    remove.add("Brawn");
    remove.add("Valor");
    remove.add("Wonder");
    
    //not fully implemented:
    
    remove.add("Aether Membrane");
    remove.add("Arashi, the Sky Asunder");
    remove.add("Hand of Cruelty");
    remove.add("Hand of Honor");
    
    //useless, or combo cards:
    
    remove.add("Aluren");
    remove.add("Conspiracy");
    remove.add("Crucible of Fire");
    remove.add("Verduran Enchantress");
    remove.add("Enchantress's Presence");
    remove.add("Mesa Enchantress");
    remove.add("Moat");
    remove.add("Magus of the Moat");
    remove.add("Relentless Rats");
    remove.add("Vedalken Archmage");
    remove.add("Hatching Plans");
    remove.add("Sensation Gorger");
    remove.add("Time Elemental");
    
    //semi useless
    
    remove.add("Wren's Run Packmaster");
    remove.add("Nova Chaser");
    remove.add("Supreme Exemplar");
    remove.add("Goblin Ringleader");
    remove.add("Sylvan Messenger");
    remove.add("Tromp the Domains");
    remove.add("Legacy Weapon");
      
    //cards the AI cannot play (effectively):
    
    remove.add("Necropotence");
    remove.add("Yawgmoth's Bargain");
    remove.add("Sensei's Divining Top");
    remove.add("Standstill");
    //remove.add("Counterspell");
    //remove.add("Exclude");
    //remove.add("False Summoning");
    //remove.add("Essence Scatter");
    //remove.add("Preemptive Strike");
    //remove.add("Punish Ignorance");
    //remove.add("Remand");
    //remove.add("Mystic Snake");
    //remove.add("Absorb");
    //remove.add("Undermine");
    //remove.add("Overwhelming Intellect");
    remove.add("AEther Vial");
    remove.add("Covetous Dragon");
    remove.add("Terramorphic Expanse");
    remove.add("Earthcraft");
    remove.add("Burst of Speed");
    remove.add("Magnify");
    remove.add("Nature's Cloak");
    remove.add("Resuscitate");
    remove.add("Shield Wall");
    remove.add("Solidarity");
    remove.add("Steadfastness");
    remove.add("Tortoise Formation");
    
    //manapool stuff:
    remove.add("Dark Ritual");
    remove.add("Seething Song");
    remove.add("Sol Ring");
    remove.add("Gaea's Cradle");
    remove.add("Priest of Titania");
    remove.add("Tolarian Academy");
    remove.add("Serra's Sanctum");
    remove.add("Basalt Monolith");
    remove.add("Grim Monolith");
    remove.add("Black Lotus");
    remove.add("Composite Golem");
    remove.add("Thran Dynamo");
    remove.add("Elvish Archdruid");
    remove.add("Sunglasses of Urza");
    remove.add("Ceta Discple");
    remove.add("Agent of Stromgald");
    remove.add("Apprentice Wizard");
    remove.add("Azorius Signet");
    remove.add("Bog Initiate");
    remove.add("Boros Signet");
    remove.add("Celestial Prism");
    remove.add("Dimir Signet");
    remove.add("Fire Sprites");
    remove.add("Fyndhorn Elder");
    remove.add("Gilded Lotus");
    remove.add("Golgari Signet");
    remove.add("Greenweaver Druid");
    remove.add("Gruul Signet");
    remove.add("Helionaut");
    remove.add("Izzet Signet");
    remove.add("Knotvine Mystic");
    remove.add("Mana Cylix");
    remove.add("Mana Prism");
    remove.add("Nantuko Elder");
    remove.add("Nomadic Elf");
    remove.add("Orochi Leafcaller");
    remove.add("Orzhov Signet");
    remove.add("Prismatic Lens");
    remove.add("Rakdos Signet");
    remove.add("Sea Scryer");
    remove.add("Selesnya Signet");
    remove.add("Simic Signet");
    remove.add("Sisay's Ring");
    remove.add("Skyshroud Elf");
    remove.add("Ur-Golem's Eye");
    remove.add("Viridian Acolyte");
    remove.add("Worn Powerstone");
    remove.add("Mana Vault");
   
    
    final ArrayList<Card> goodLand = new ArrayList<Card>();
    //goodLand.add("Faerie Conclave");
    //goodLand.add("Forbidding Watchtower");
    //goodLand.add("Treetop Village");

    CardList out = new CardList();
    if (colors == 3)
    {
	    
	    out = list.filter(new CardListFilter()
	    {
	      public boolean addCard(Card c)
	      {
	    	ArrayList<String> list = CardUtil.getColors(c);
	
	    	if (list.size() == 3)
	    	{
	    		if (!list.contains(color1) || !list.contains(color2) || !list.contains(color3))
	    			return false;
	    	}
	
	    	
	    	else if (list.size() == 2)
	    	{	
	    		if (!(list.contains(color1) && list.contains(color2)) &&
	    		    !(list.contains(color1) && list.contains(color3)) &&
	    		    !(list.contains(color2) && list.contains(color3)))
	    		   	return false;
	    	}
	    	  
	        return CardUtil.getColors(c).size() <= 3 &&
	               !c.isLand()                       && //no land
	               !remove.contains(c.getName())     || //OR very important
	               goodLand.contains(c.getName());
	      }
	    });
    }
    
    else if (colors == 5)
    {
    	out = list.filter(new CardListFilter()
	    {
	      public boolean addCard(Card c)
	      {  
	        return CardUtil.getColors(c).size() >= 2 && //only get multicolored cards
	               !c.isLand()                       && //no land
	               !remove.contains(c.getName())     || //OR very important
	               goodLand.contains(c.getName());
	      }
	    });
    	
    }

    return out;
  }//filterBadCards()
  public static void main(String[] args)
  {
    GenerateConstructedMultiColorDeck g = new GenerateConstructedMultiColorDeck();

    for(int i = 0; i < 10; i++)
    {
      System.out.println("***GENERATING DECK***");
      CardList c = g.generate3ColorDeck();
      System.out.println(c.getType("Creature").size() +" - " +c.size());
      for (int j=0; j<c.size();j++ ){  
    	  System.out.println(c.get(j).getName());
      }
      System.out.println("***DECK GENERATED***");
      
    }
    System.exit(1);

  }//main
}