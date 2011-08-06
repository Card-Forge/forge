package forge;
import java.util.*;

public class GenerateDraftDeck
{
  private static Map<String,String> map = new HashMap<String,String>();

  static
  {
    map.put(Constant.Color.Black , "Swamp");
    map.put(Constant.Color.Blue  , "Island");
    map.put(Constant.Color.Green , "Forest");
    map.put(Constant.Color.Red   , "Mountain");
    map.put(Constant.Color.White , "Plains");
  }

  public static void main(String[] args)
  {
    CardList cardPool = new CardList();
    ReadBoosterPack booster = new ReadBoosterPack();

    for(int outer = 0; outer < 10000; outer++)
    {
      cardPool.clear();
      for(int i = 0; i < 21; i++)
        cardPool.addAll(booster.getBoosterPack().toArray());

      makeDeck7(cardPool);
      if(cardPool.size() != 205)
      {
        System.out.println(cardPool.size() +" should be ");
        break;
      }
    }//for outer
  }

  //Deck[] size is 7
  public static Deck[] makeDeck7(CardList cardPool)
  {
    Deck[] out = new Deck[5];
    //int index = 0; // unused

    for(int i = 0; i < out.length; i++)
      out[i] = makeAndTestDeck(cardPool);

    test(out);
    return out;
  }//makeDeck7()

  public static void test(Deck[] deck)
  {
    for(int i = 0; i < deck.length; i++)
    {
      if(deck[i] == null || deck[i].countMain() != 40)
        throw new RuntimeException("GenerateDraftDeck : test() error, deck is wrong");
    }
  }


  //returned Deck may be null
  //this method changes cardPool, and takes cards out of it
  //the remaining cards in cardPool are unused
  public static Deck makeAndTestDeck(CardList cardPool)
  {
    CardList test = makeDeck(cardPool, 11, Constant.GameType.Draft);
    if(test == null)
    {
      for(int i = 0; i < 100; i++)
        test = makeDeck(cardPool, 11, Constant.GameType.Draft);
    }

    if(test == null)
      return null;

    //convert CardList into Deck
    //remove cards from cardPool
    Deck out = new Deck(Constant.GameType.Draft);
    for(int i = 0; i < test.size(); i++)
    {
      cardPool.remove(test.get(i));
      out.addMain(test.get(i).getName());
    }

    return out;
  }//makeAndTestDeck()

  public static CardList makeDeck(CardList cardPool, int minCreatureCount, String gameType)
  {
    CardList test = null;
    boolean pass = false;
    for(int i = 0; i < 5; i++)
    {
      test = make2ColorDeck(new CardList(cardPool.toArray()));
      if(minCreatureCount <= test.getType("Creature").size())
      {
        pass = true;
        break;
      }
    }//for
    if((! pass) || (test.size() != 40))
      return null;

    return test;
  }//makeDeck()


  private static CardList make2ColorDeck(CardList in)
  {
    final ArrayList<String> remove = new ArrayList<String>();
    remove.add("Force of Savagery");
    remove.add("Darksteel Colossus");
    remove.add("Jokulhaups");
    remove.add("Steel Wall");
    remove.add("Ornithopter");
    remove.add("Sarcomite Myr");

    in = in.filter(new CardListFilter()
    {
      public boolean addCard(Card c)
      {
        return CardUtil.getColors(c).size() == 1 && !remove.contains(c.getName());
      }
    });

    String[] color = get2Colors();
    CardList out = new CardList();

    out.addAll(CardListUtil.getColor(in, color[0]).toArray());
    out.addAll(CardListUtil.getColor(in, color[1]).toArray());

    CardList trim = new CardList();
    for(int i = 0; i < 22 && i < out.size(); i++)
      trim.add(out.get(i));

    addLand(trim, color);

    return trim;
  }

  //the 2 colors will be different
  private static String[] get2Colors()
  {
    int a;
    int b;

    do{
      a = CardUtil.getRandomIndex(Constant.Color.onlyColors);
      b = CardUtil.getRandomIndex(Constant.Color.onlyColors);
    }while(a == b);//do not want to get the same color twice

    String color1 = Constant.Color.onlyColors[a];
    String color2 = Constant.Color.onlyColors[b];

    return new String[] {color1, color2};
  }//get2Colors()

  private static void addLand(CardList list, String[] color)
  {
    Card land;
    for(int i = 0; i < 9; i++)
    {
      land = AllZone.CardFactory.getCard(map.get(color[0]).toString(), AllZone.ComputerPlayer);
      list.add(land);

      land = AllZone.CardFactory.getCard(map.get(color[1]).toString(), AllZone.ComputerPlayer);
      list.add(land);
    }
  }//addLand()
}

/*
import java.util.*;

public class GenerateSealedDeck
{
  private String color1;
  private String color2;

  public CardList generateDeck()
  {
    CardList deck;
    do{
      deck = get2ColorDeck();
    }while(deck.getType("Creature").size() < 16);

    addLand(deck);

    if(deck.size() != 40)
      throw new RuntimeException("GenerateSealedDeck() : generateDeck() error, deck size it not 40, deck size is " +deck.size());

    return deck;
  }
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
    for(int i = 0; i < 22; i++)
      out.add(deck.get(i));

    return out;
  }
}


*/