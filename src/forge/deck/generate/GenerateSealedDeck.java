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
import forge.ReadBoosterPack;

public class GenerateSealedDeck
{
  private String color1;
  private String color2;

  private Map<String,String> map = new HashMap<String,String>();

  public GenerateSealedDeck() {setupMap();}

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
    do{
      deck = get2ColorDeck();
    }while(deck.getType("Creature").size() < 12 || 16 < deck.getType("Creature").size());

    addLand(deck);

    if(deck.size() != 40)
      throw new RuntimeException("GenerateSealedDeck() : generateDeck() error, deck size it not 40, deck size is " +deck.size());

    return deck;
  }
  private void addLand(CardList list)
  {
    Card land;
    for(int i = 0; i < 9; i++)
    {
      land = AllZone.CardFactory.getCard(map.get(color1).toString(), AllZone.ComputerPlayer);
      list.add(land);

      land = AllZone.CardFactory.getCard(map.get(color2).toString(), AllZone.ComputerPlayer);
      list.add(land);
    }
  }//addLand()
  private CardList get2ColorDeck()
  {
    ReadBoosterPack booster = new ReadBoosterPack();
    CardList deck;
    do{
      deck = get2Colors(booster.getBoosterPack5());
    }while(deck.size() < 22);

    CardList out = new CardList();
    deck.shuffle();

    //trim deck size down to 22 cards, presumes 18 land
    for(int i = 0; i < 22 && i < deck.size(); i++)
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
    //remove "bad" and multi-colored cards
    final ArrayList<String> remove = new ArrayList<String>();
    remove.add("Force of Savagery");
    remove.add("Darksteel Colossus");
    remove.add("Jokulhaups");
    remove.add("Steel Wall");
    remove.add("Ornithopter");
    remove.add("Sarcomite Myr");

    CardList out = list.filter(new CardListFilter()
    {
      public boolean addCard(Card c)
      {
        return CardUtil.getColors(c).size() == 1 && !remove.contains(c.getName());
      }
    });

    return out;
  }//filterBadCards()
  public static void main(String[] args)
  {
    GenerateSealedDeck g = new GenerateSealedDeck();

    for(int i = 0; i < 100; i++)
    {
      System.out.println(i);
      g.generateDeck();
    }

/*
    for(int i = 0; i < 10; i++)
    {
      CardList c = g.generateDeck();
      System.out.println(c.getType("Creature").size());
    }
*/
  }
}