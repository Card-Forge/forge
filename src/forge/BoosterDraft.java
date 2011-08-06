package forge;


public interface BoosterDraft
{
  public CardList nextChoice();
  public void setChoice(Card c);
  public boolean hasNextChoice();
  public Deck[] getDecks(); //size 7, all the computers decks
}

class BoosterDraft_1 implements BoosterDraft
{
  private final BoosterDraftAI draftAI = new BoosterDraftAI();
  private static final int nPlayers = 8;
  private static final int boosterPackSize = 15;
  private static final int stopCount = boosterPackSize * 3;//should total of 45

  private int currentCount = 0;
  private CardList[] pack;//size 8

  //helps the computer choose which booster packs to pick from
  //the first row says "pick from boosters 1-7, skip 0" since the players picks from 0
  //the second row says "pick from 0 and 2-7 boosters, skip 1" - player chooses from 1
  private final int computerChoose[][] = {
    {  1,2,3,4,5,6,7},
    {0,  2,3,4,5,6,7},
    {0,1,  3,4,5,6,7},
    {0,1,2,  4,5,6,7},
    {0,1,2,3,  5,6,7},
    {0,1,2,3,4,  6,7},
    {0,1,2,3,4,5,  7},
    {0,1,2,3,4,5,6  }
  };

  public static void main(String[] args)
  {
    BoosterDraft_1 draft = new BoosterDraft_1();
    while(draft.hasNextChoice())
    {
      CardList list = draft.nextChoice();
      System.out.println(list.size());
      draft.setChoice(list.get(0));
    }
  }
  BoosterDraft_1() {pack = get8BoosterPack();}

  public CardList nextChoice()
  {
    if(pack[getMod()].size() == 0)
      pack = get8BoosterPack();

    computerChoose();
    CardList list = pack[getMod()];
    return list;
  }
  public CardList[] get8BoosterPack()
  {
    CardList[] list = new CardList[]
    {//nPlayers is 8
      new CardList(),
      new CardList(),
      new CardList(),
      new CardList(),

      new CardList(),
      new CardList(),
      new CardList(),
      new CardList(),
    };
    ReadDraftBoosterPack pack = new ReadDraftBoosterPack();

    for(int i = 0; i < list.length; i++)
      list[i].addAll(pack.getBoosterPack().toArray());

    return list;
  }//get8BoosterPack()

  //size 7, all the computers decks
  public Deck[] getDecks() {return draftAI.getDecks();}

  private void computerChoose()
  {
    int row[] = computerChoose[getMod()];

    for(int i = 0; i < row.length; i++)
      draftAI.choose(pack[row[i]]);
  }//computerChoose()

  private int getMod() {return currentCount % nPlayers;}
  public boolean hasNextChoice() {return currentCount < stopCount;}

  public void setChoice(Card c)
  {
    CardList list = pack[getMod()];
    currentCount++;

    if(! list.contains(c))
      throw new RuntimeException("BoosterDraft : setChoice() error - card not found - " +c +" - booster pack = " +list);

    list.remove(c);
  }//setChoice()
}

class BoosterDraftTest implements BoosterDraft
{
  int n = 3;
  public Deck[] getDecks() {return null;}

  public CardList nextChoice()
  {
    n--;
    ReadDraftBoosterPack pack = new ReadDraftBoosterPack();
    return pack.getBoosterPack();
  }
  public void setChoice(Card c) {System.out.println(c.getName());}
  public boolean hasNextChoice()
  {
    return n > 0;
  }
  public CardList getChosenCards() {return null;}
  public CardList getUnchosenCards() {return null;}
}
