package forge;
//handles mana costs like 2/R or 2/B
//for cards like Flame Javelin (Shadowmoor)
public class Mana_PartSplit extends Mana_Part
{
  private Mana_Part manaPart;
  private String originalCost;

  public Mana_PartSplit(String manaCost)
  {
    //is mana cost like "2/R"
    if(manaCost.length() != 3)
      throw new RuntimeException("Mana_PartSplit : constructor() error, bad mana cost parameter - " +manaCost);

    originalCost = manaCost;
  }

  private boolean isFirstTime()
  {
    return manaPart == null;
  }
  private void setup(String manaToPay)
  {
    //get R out of "2/R"
    String color = originalCost.substring(2, 3);

    //is manaToPay the one color we want or do we
    //treat it like colorless?
    //if originalCost is 2/R and is color W (treated like colorless)
    //or R?  if W use Mana_PartColorless, if R use Mana_PartColor
    //does manaToPay contain color?
    if(0 <= manaToPay.indexOf(color))
    {
      manaPart = new Mana_PartColor(color);
    }
    else
    {
      //get 2 out of "2/R"
      manaPart = new Mana_PartColorless(originalCost.substring(0, 1));
    }
  }//setup()

  public void reduce(String mana)
  {
    if(isFirstTime())
      setup(mana);

    manaPart.reduce(mana);
  }
  public boolean isNeeded(String mana)
  {
    if(isFirstTime())
    {
      //always true because any mana can pay the colorless part of 2/G
      return true;
    }

    return manaPart.isNeeded(mana);
  }//isNeeded()

  public String toString()
  {
    if(isFirstTime())
      return originalCost;

    return manaPart.toString();
  }

  public boolean isPaid()
  {
    if(isFirstTime())
      return false;

    return manaPart.isPaid();
  }
}