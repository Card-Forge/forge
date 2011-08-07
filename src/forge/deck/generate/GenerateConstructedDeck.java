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

public class GenerateConstructedDeck
{
  private String color1;
  private String color2;

  private Map<String,String> map = new HashMap<String,String>();

  public GenerateConstructedDeck() {setupMap();}

  private void setupMap()
  {
    map.put(Constant.Color.Black , "Swamp");
    map.put(Constant.Color.Blue  , "Island");
    map.put(Constant.Color.Green , "Forest");
    map.put(Constant.Color.Red   , "Mountain");
    map.put(Constant.Color.White , "Plains");
  }

  public CardList generateDeck()
  {
    CardList deck;

    int check;

    do{
      deck = get2ColorDeck();
      check = deck.getType("Creature").size();

    }while(check < 16 || 24 < check);

    addLand(deck);

    if(deck.size() != 60)
      throw new RuntimeException("GenerateConstructedDeck() : generateDeck() error, deck size it not 60, deck size is " +deck.size());

    return deck;
  }
  //25 lands
  private void addLand(CardList list)
  {
    Card land;
    for(int i = 0; i < 13; i++)
    {
      land = AllZone.CardFactory.getCard(map.get(color1).toString(), AllZone.ComputerPlayer);
      list.add(land);

      land = AllZone.CardFactory.getCard(map.get(color2).toString(), AllZone.ComputerPlayer);
      list.add(land);
    }
  }//addLand()
  
  private CardList getCards()
  {
    return filterBadCards(AllZone.CardFactory.getAllCards());
  }//getCards()
  
  private CardList get2ColorDeck()
  {
    CardList deck = get2Colors(getCards());

    CardList out = new CardList();
    deck.shuffle();

    //trim deck size down to 34 cards, presumes 26 land, for a total of 60 cards
    for(int i = 0; i < 34 && i < deck.size(); i++)
      out.add(deck.get(i));

    return out;
  }
  
  private CardList get2Colors(CardList in)
  {
    int a;
    int b;

    do{
      a = CardUtil.getRandomIndex(Constant.Color.onlyColors);
      b = CardUtil.getRandomIndex(Constant.Color.onlyColors);
    }while(a == b);//do not want to get the same color twice

    color1 = Constant.Color.onlyColors[a];
    color2 = Constant.Color.onlyColors[b];

    CardList out = new CardList();
    out.addAll(CardListUtil.getColor(in, color1));
    out.addAll(CardListUtil.getColor(in, color2));
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
   
    out = filterBadCards(out);
    return out;
  }

  private CardList filterBadCards(CardList list)
  {
    
    final ArrayList<Card> goodLand = new ArrayList<Card>();

    CardList out = list.filter(new CardListFilter()
    {
      public boolean addCard(Card c)
      {
    	ArrayList<String> list = CardUtil.getColors(c);
    	if (list.size() == 2)
      	{	
      		if (!(list.contains(color1) && list.contains(color2)))
      		   return false;
      	}
        return CardUtil.getColors(c).size() <= 2 && //only dual colored gold cards
               !c.isLand()                       && //no land
               !c.getSVar("RemRandomDeck").equals("True") &&
               !c.getSVar("RemAIDeck").equals("True")     || //OR very important
               goodLand.contains(c.getName());
      }
    });

    return out;
  }//filterBadCards()
  
  public static void main(String[] args)
  {
    GenerateConstructedDeck g = new GenerateConstructedDeck();

    for(int i = 0; i < 50; i++)
    {
      CardList c = g.generateDeck();
      System.out.println(c.getType("Creature").size() +" - " +c.size());
    }
    System.exit(1);

  }//main
}