package forge.deck.generate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import forge.AllZone;
import forge.Card;
import forge.CardList;
import forge.CardListFilter;
import forge.CardListUtil;
import forge.CardUtil;
import forge.Constant;
import forge.Gui_NewGame;

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
	    
		      land = AllZone.CardFactory.getCard(map.get(color1).toString(), AllZone.ComputerPlayer);
		      list.add(land);
		
		      land = AllZone.CardFactory.getCard(map.get(color2).toString(), AllZone.ComputerPlayer);
		      list.add(land);
		      
		      land = AllZone.CardFactory.getCard(map.get(color3).toString(), AllZone.ComputerPlayer);
		      list.add(land);
	    }
	    
	    int numberDual = 4;
	    for (int i = 0; i < numberDual;i++)
	    {
	    	land = AllZone.CardFactory.getCard(multiMap.get(color1+color2)[0], AllZone.ComputerPlayer);
		    list.add(land);
		    	
		    land = AllZone.CardFactory.getCard(multiMap.get(color1+color3)[0], AllZone.ComputerPlayer);
		    list.add(land);
		    
		    land = AllZone.CardFactory.getCard(multiMap.get(color2+color3)[0], AllZone.ComputerPlayer);
		    list.add(land);
	    }
	    for (int i=0; i<2;i++)
	    {
		    land = AllZone.CardFactory.getCard(multiMap.get(color1+color2)[1], AllZone.ComputerPlayer);
		    list.add(land);
		    	
		    land = AllZone.CardFactory.getCard(multiMap.get(color1+color3)[1], AllZone.ComputerPlayer);
		    list.add(land);
		    
		    land = AllZone.CardFactory.getCard(multiMap.get(color2+color3)[1], AllZone.ComputerPlayer);
		    list.add(land);
	    }
	}
	else if (colors == 5)
	{
		int numberBasic = 1;
	    Card land;
	    for(int i = 0; i < numberBasic; i++)
	    {
	    
		      land = AllZone.CardFactory.getCard(map.get(color1).toString(), AllZone.ComputerPlayer);
		      list.add(land);
		
		      land = AllZone.CardFactory.getCard(map.get(color2).toString(), AllZone.ComputerPlayer);
		      list.add(land);
		      
		      land = AllZone.CardFactory.getCard(map.get(color3).toString(), AllZone.ComputerPlayer);
		      list.add(land);
		      
		      land = AllZone.CardFactory.getCard(map.get(color4).toString(), AllZone.ComputerPlayer);
		      list.add(land);
		      
		      land = AllZone.CardFactory.getCard(map.get(color5).toString(), AllZone.ComputerPlayer);
		      list.add(land);
	    }
	    
		
		int numberDual = 2;
	    for (int i = 0; i < numberDual;i++)
	    {
	    	land = AllZone.CardFactory.getCard(multiMap.get(color1+color2)[0], AllZone.ComputerPlayer);
		    list.add(land);
		    	
		    land = AllZone.CardFactory.getCard(multiMap.get(color1+color3)[0], AllZone.ComputerPlayer);
		    list.add(land);
		    
		    land = AllZone.CardFactory.getCard(multiMap.get(color1+color4)[0], AllZone.ComputerPlayer);
		    list.add(land);
		    
		    land = AllZone.CardFactory.getCard(multiMap.get(color1+color5)[0], AllZone.ComputerPlayer);
		    list.add(land);
		    
		    land = AllZone.CardFactory.getCard(multiMap.get(color2+color3)[0], AllZone.ComputerPlayer);
		    list.add(land);
		    
		    land = AllZone.CardFactory.getCard(multiMap.get(color2+color4)[0], AllZone.ComputerPlayer);
		    list.add(land);
		    
		    land = AllZone.CardFactory.getCard(multiMap.get(color2+color5)[0], AllZone.ComputerPlayer);
		    list.add(land);
		    
		    land = AllZone.CardFactory.getCard(multiMap.get(color3+color4)[0], AllZone.ComputerPlayer);
		    list.add(land);
		    
		    land = AllZone.CardFactory.getCard(multiMap.get(color3+color5)[0], AllZone.ComputerPlayer);
		    list.add(land);
		    
		    land = AllZone.CardFactory.getCard(multiMap.get(color4+color5)[0], AllZone.ComputerPlayer);
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
    out.addAll(CardListUtil.getColor(in, color1));
    out.addAll(CardListUtil.getColor(in, color2));
    out.addAll(CardListUtil.getColor(in, color3));
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
    out.addAll(artifact);
   
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
    out.addAll(CardListUtil.getColor(in, color1));
    out.addAll(CardListUtil.getColor(in, color2));
    out.addAll(CardListUtil.getColor(in, color3));
    out.addAll(CardListUtil.getColor(in, color4));
    out.addAll(CardListUtil.getColor(in, color5));
    */
    out.addAll(CardListUtil.getGoldCards(in));
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
    out.addAll(artifact);
   
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
	               !c.getSVar("RemRandomDeck").equals("True") &&
	               !c.getSVar("RemAIDeck").equals("True")    || //OR very important
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
	               !c.getSVar("RemRandomDeck").equals("True") &&
	               !c.getSVar("RemAIDeck").equals("True")     || //OR very important
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