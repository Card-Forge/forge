import java.util.*;

public class Input_PayManaCostUtil
{
  //all mana abilities start with this and typical look like "tap: add G"
  //mana abilities are Strings and are retreaved by calling card.getKeyword()
  //taps any card that has mana ability, not just land
  public static ManaCost tapCard(Card card, ManaCost manaCost)
  {
    if(card instanceof ManaPool) return ((ManaPool)card).subtractMana(manaCost);
	ArrayList<Ability_Mana> abilities = getManaAbilities(card);

    String cneeded="";
    for(String color : Constant.Color.Colors)
    	if(manaCost.isNeeded(color))
    		cneeded+=getColor2(color);
    Iterator<Ability_Mana> it = abilities.iterator();//you can't remove unneded abilitie inside a for(am:abilities) loop :(
    while(it.hasNext())
    {
    	Ability_Mana ma = it.next();
    	if (!ma.canPlay()){ it.remove(); continue;}
    	if (cneeded.contains("1")) break;
    	boolean needed = false;
    	String[] canMake = ManaPool.getManaParts(ma);    	
    	for(String color : canMake)
    		if(cneeded.contains(color)) needed = true;
    	if (!needed) it.remove();
    }
    if(abilities.isEmpty())
    	return manaCost;
    //String color;
    Ability_Mana chosen = abilities.get(0);
    if(1 < abilities.size())
    {
      HashMap<String, Ability_Mana> ability = new HashMap<String, Ability_Mana>();
      for(Ability_Mana am : abilities)
    	  ability.put(am.toString(), am);
      chosen = (Ability_Mana) AllZone.Display.getChoice("Choose mana ability", abilities.toArray());
    }
    {
    	AllZone.GameAction.playSpellAbility(chosen);
    	manaCost = AllZone.ManaPool.subtarctMana(manaCost, chosen);
    	AllZone.Human_Play.updateObservers();//DO NOT REMOVE THIS, otherwise the cards don't always tap (copied)
		return manaCost;	
    }
  }
  public static ArrayList<Ability_Mana> getManaAbilities(Card card)
  {return card.getManaAbility();}
  //color is like "G", returns "Green"
  public static String getColor(String color)
  {
    Map<String, String> m = new HashMap<String, String>();
    m.put("G", Constant.Color.Green);
    m.put("R", Constant.Color.Red);
    m.put("U", Constant.Color.Blue);
    m.put("B", Constant.Color.Black);
    m.put("W", Constant.Color.White);

    Object o = m.get(color);

    if(o == null)
      o = Constant.Color.Colorless;


    return o.toString();
  }
  
  public static String getColor2(String color)
  {
     Map<String, String> m = new HashMap<String, String>();
     m.put(Constant.Color.Green, "G");
     m.put(Constant.Color.Red, "R");
     m.put(Constant.Color.Blue, "U");
     m.put(Constant.Color.Black, "B");
     m.put(Constant.Color.White, "W");
     m.put(Constant.Color.Colorless, "1");
      
     Object o = m.get(color);
    
     return o.toString();
  }
  
}
