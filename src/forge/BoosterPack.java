package forge;


public class BoosterPack
{
  public static CardList getBoosterPack(int number)
  {
    CardList all = new CardList();
    for(int i = 0; i < number; i++)
      all.addAll(getBoosterPack());

    return all;
  }

  public static CardList getBoosterPack()
  {
    CardList all = AllZone.CardFactory.getAllCards();
    CardList pack = new CardList();

    for(int i = 0; i < 10; i++)
      pack.add(all.get(MyRandom.random.nextInt(all.size())));

    for(int i = 0; i < 5; i++)
      pack.add(AllZone.CardFactory.copyCard(pack.get(i)));

    return pack;
  }//getBoosterPack()
}