package forge;
import java.util.*;

public class BoosterDraftAI
{
  //once a deck has this number of creatures the computer randomly
  //picks a card, so the final computer deck has 12-20 creatures
  //minimum of creatures per deck
  private static final int nCreatures = 16;
  private static final int nDecks = 7;

  //holds all the cards for each of the computer's decks
  private CardList[] deck = new CardList[nDecks];
  private String[][] deckColor = new String[nDecks][];

  private static Map<String, String> colorToLand = new HashMap<String, String>();

  public static void main(String[] args)
  {
    BoosterDraftAI ai = new BoosterDraftAI();
    ai.runTestPrint();
  }
  public void runTestPrint()
  {
    BoosterDraftAI ai = new BoosterDraftAI();
    ai.runTest(ai);

    Deck[] deck = ai.getDecks();

    for(int outer = 0; outer < 7; outer++)
    {
      System.out.print(deck[outer].countMain() +" - ");

      for(int i = 0; i < 16; i++)
        System.out.print(deck[outer].getMain(i) +", ");

      System.out.println("");

      for(int i = 16; i < 22; i++)
        System.out.print(deck[outer].getMain(i) +", ");

      System.out.println("\n");
    }//for outer
  }//runTestPrint()

  //throws Exception if error
  public void runTest(BoosterDraftAI ai)
  {
    ReadDraftBoosterPack booster = new ReadDraftBoosterPack();
    for(int outer = 0; outer < 1; outer++)
    {
      CardList allBooster = new CardList();
      for(int i = 0; i < 21; i++)
        allBooster.addAll(booster.getBoosterPack().toArray());

      int stop = allBooster.size();
      for(int i = 0; i < stop; i++)
      {
        ai.choose(allBooster);
      }
      //ai.checkDeckList(ai.deck);
    }
  }//runTest()

  //picks one Card from augment, removes that card, and returns the list
  //returns the cards not picked
  public CardList choose(final CardList in_choose)
  {
    //in_choose should ONLY be on the RIGHT side of any equal sign
    //only 1 card should be removed from in_choose

    CardList list = new CardList();
    boolean hasPicked = false;

    //try to choose a creature that has one of the colors that are in the deck
    for(int i = 0; i < nDecks; i++)
    {
      if(countCreatures(deck[i]) < nCreatures)
      {
        list.clear();

        //get creature cards that match the decks 2 colors
        list.addAll(CardListUtil.getColor(in_choose.getType("Creature"), deckColor[i][0]).toArray());
        list.addAll(CardListUtil.getColor(in_choose.getType("Creature"), deckColor[i][1]).toArray());

        for(int j=0;j<list.size();j++)
        {
      	  //go through list and remove gold cards:
      	  if(CardUtil.getColors(list.get(j)).size() > 1)
      	  {
      		  //TODO maybe keep in cards that have 2 colors, and match the colors 
  			  list.remove(list.get(j));
      	  }
        }
        
        list.shuffle();

        if(! list.isEmpty())
        {
          
          deck[i].add(list.get(0));
          hasPicked = true;
          break;
        }
      }//if
    }//for

    //no card was chosen
    if(! hasPicked)
    {
      //TODO: remove cards that the computer can't play
      //holds instants and sorceries
      CardList spell = new CardList();
      spell.addAll(in_choose.getType("Instant").toArray());
      spell.addAll(in_choose.getType("Sorcery").toArray());

      //choose any card that matches the decks color
      for(int i = 0; i < nDecks; i++)
      {
        list.clear();

        list.addAll(deck[i].getType("Instant").toArray());
        list.addAll(deck[i].getType("Sorcery").toArray());

        //does the deck need more spells?
        //16 creatures, 6 spells per deck
        if(list.size() < 6)
        {
          list.clear();

          //get any cards that match the decks 2 colors
          list.addAll(CardListUtil.getColor(new CardList(spell.toArray()), deckColor[i][0]).toArray());
          list.addAll(CardListUtil.getColor(new CardList(spell.toArray()), deckColor[i][1]).toArray());
                    
          list.shuffle();
          
          
          
          if(! list.isEmpty())
          {
            deck[i].add(list.get(0));
            hasPicked = true;
            break;
          }
        }//if - deck[i].size() < 22
      }//for
    }//if - choose any card

    if(hasPicked)
      in_choose.remove(list.get(0));
    else//computer didn't choose a card, just remove any card - all computer decks are done
    {
      in_choose.remove(0);
    }

    return in_choose;
  }//choose()

/*
  I get some wierd error when I have this method, I don't know whats wrong

  private void checkDeckList(CardList[] deck)
  {
    if(deck.length != nDecks)
      throw new RuntimeException("BoosterDraftAI : checkDeckList() error, deck list size is not 7 - " +deck.length);

    for(int i = 0; i < nDecks; i++)
    {
      if(deck[i].size() != 22)
      {
        throw new RuntimeException("BoosterDraftAI : checkDeckList() error, deck list size is not 22 - " +deck[i].size() +" - " +deck.toString());
      }
      if(countCreatures(deck[i]) < nCreatures)
        throw new RuntimeException("BoosterDraftAI : checkDeckList() error, deck needs more creatures - " +countCreatures(deck[i]));

      for(int inner = 0; inner < 22; inner++)
        if(! CardUtil.getColors(deck[i].getCard(inner)).contains(deckColor[i][0]) &&
           ! CardUtil.getColors(deck[i].getCard(inner)).contains(deckColor[i][1]))
          throw new RuntimeException("BoosterDraftAI : checkDeckList() error, deck has different card colors");
    }//for
  }//checkDeckList()
*/

  private int countCreatures(CardList list) {return list.getType("Creature").size();}

  private void testColors(int[] n)
  {
    if(n.length != nDecks)
      throw new RuntimeException("BoosterDraftAI : testColors error, numbers array length does not equal 7");

    HashSet<Integer> set = new HashSet<Integer>();
    for(int i = 0; i < nDecks; i++)
      set.add(Integer.valueOf(n[i]));

    if(set.size() != nDecks)
      throw new RuntimeException("BoosterDraftAI : testColors error, numbers not unique");

    for(int i = 0; i < nDecks; i++)
      if(n[i] < 0 || deckColorChoices.length <= n[i])
        throw new RuntimeException("BoosterDraftAI : testColors error, index out of range - " +n[i]);
  }//testColors()

  public Deck[] getDecks()
  {
    //check CardList[] deck for errors
    //checkDeckList(deck);

    Deck[] out = new Deck[deck.length];

    for(int i = 0; i < deck.length; i++)
    {
      addLand(deck[i], deckColor[i]);
      out[i] = getDeck(deck[i]);
    }
    return out;
  }//getDecks()

  private Deck getDeck(CardList list)
  {
    Deck out = new Deck(Constant.GameType.Draft);
    for(int i = 0; i < list.size(); i++)
      out.addMain(list.get(i).getName());

    return out;
  }//getDeck()

  //add Land to list argument
  private void addLand(CardList list, String[] color)
  {
    Card land;
    for(int i = 0; i < 9; i++)
    {
      land = AllZone.CardFactory.getCard(colorToLand.get(color[0]).toString(), AllZone.ComputerPlayer);
      list.add(land);

      land = AllZone.CardFactory.getCard(colorToLand.get(color[1]).toString(), AllZone.ComputerPlayer);
      list.add(land);
    }

    if(list.getType("Land").size() != 18)
      throw new RuntimeException("BoosterDraftAI : addLand() error, deck does not have 18 lands - " +list.getType("Land").size());

    if(list.size() != 40)
      throw new RuntimeException("BoosterDraftAI : addLand() error, deck is not 40 cards - " +list.size());
  }//addLand()


  //returns 7 different ints, within the range of 0-9
  private int[] getDeckColors()
  {
    int[] out = new int[nDecks];
    int start = MyRandom.random.nextInt(10);

    for(int i = 0; i < out.length; i++)
    {
      //% to get an index between 0 and deckColorChoices.length
      out[i] = start % deckColorChoices.length;
      start++;
    }
    testColors(out);

    return out;
  }//getDeckColors()

  public BoosterDraftAI()
  {
    //choose colors for decks
    int[] n = getDeckColors();
    for(int i = 0; i < n.length; i++)
      deckColor[i] = deckColorChoices[n[i]];

    //initilize color map
    colorToLand.put(Constant.Color.Black , "Swamp");
    colorToLand.put(Constant.Color.Blue  , "Island");
    colorToLand.put(Constant.Color.Green , "Forest");
    colorToLand.put(Constant.Color.Red   , "Mountain");
    colorToLand.put(Constant.Color.White , "Plains");

    //initilize deck array
    for(int i = 0; i < deck.length; i++)
      deck[i] = new CardList();
  }//BoosterDraftAI()


  //all 10 two color combinations
  private String[][] deckColorChoices =
  {
    {Constant.Color.Black, Constant.Color.Blue},
    {Constant.Color.Black, Constant.Color.Green},
    {Constant.Color.Black, Constant.Color.Red},
    {Constant.Color.Black, Constant.Color.White},

    {Constant.Color.Blue,  Constant.Color.Green},
    {Constant.Color.Blue,  Constant.Color.Red},
    {Constant.Color.Blue,  Constant.Color.White},

    {Constant.Color.Green, Constant.Color.Red},
    {Constant.Color.Green, Constant.Color.White},

    {Constant.Color.Red,   Constant.Color.White}
  };
}//BoosterDraftAI()