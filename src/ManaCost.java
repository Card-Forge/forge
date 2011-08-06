//import java.util.*; //unused


//this is just a wrapper for Mana_PayCost
//it was easier to change this than to update a bunch of stuff
class ManaCost
{
  private Mana_PayCost m;

  public ManaCost(String cost)
  {
    m = new Mana_PayCost(cost);
  }

  public boolean isPaid()
  {
    return m.isPaid();
  }
  public String toString()
  {
    return m.toString();
  }

  public void subtractMana(String color)
  {
    color = getShortManaString(color);
    m.addMana(color);
  }

  //convert "white" to "W"
  //convert "colorless" to "1" for Mana_PartColorless
  private String getShortManaString(String longManaString)
  {
    String s = longManaString;
    if(s.equals(Constant.Color.White))
      return "W";
    if(s.equals(Constant.Color.Black))
      return "B";
    if(s.equals(Constant.Color.Blue))
      return "U";
    if(s.equals(Constant.Color.Green))
      return "G";
    if(s.equals(Constant.Color.Red))
      return "R";
    if(s.equals(Constant.Color.Colorless))
      return "1";
    else if((s.equals("G") || s.equals("U") || s.equals("W") ||
            s.equals("B") || s.equals("R") || s.equals("1")))
    	return s;

    throw new RuntimeException("ManaCost : getShortManaString() invalid argument - " +longManaString);
  }//getShortManaString()

  public boolean isNeeded(String color)
  {
    color = getShortManaString(color);
    return m.isNeeded(color);
  }

/*
  public int getColor(String color)
  {
    return (int)Math.abs(manaPool.getColor(color));
  }
  public boolean isNeeded(String color)
  {
    int a = getColor(color);
    int b = getColor(Constant.Color.Colorless);

    if(0 < a || 0 < b)
      return true;

    return false;
  }
  */
}//ManaCost
