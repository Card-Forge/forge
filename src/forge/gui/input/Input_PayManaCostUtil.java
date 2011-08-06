package forge.gui.input;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import forge.AllZone;
import forge.Card;
import forge.Constant;
import forge.card.mana.ManaCost;
import forge.card.mana.ManaPool;
import forge.card.spellability.Ability_Mana;
import forge.card.spellability.SpellAbility;
import forge.gui.GuiUtils;

public class Input_PayManaCostUtil
{
  //all mana abilities start with this and typical look like "tap: add G"
  //mana abilities are Strings and are retrieved by calling card.getKeyword()
  //taps any card that has mana ability, not just land
  public static ManaCost activateManaAbility(SpellAbility sa, Card card, ManaCost manaCost)
  {
	//make sure computer's lands aren't selected
	if(card.getController().equals(AllZone.ComputerPlayer))
		return manaCost;
	
    if(card instanceof ManaPool) 
    	return ((ManaPool)card).subtractMana(sa, manaCost);

	ArrayList<Ability_Mana> abilities = getManaAbilities(card);
    StringBuilder cneeded = new StringBuilder();
    boolean choice = true;
    boolean skipExpress = false;
    
    for(String color : Constant.Color.ManaColors)
    	if(manaCost.isNeeded(color))
    		cneeded.append(getShortColorString(color));
    
    Iterator<Ability_Mana> it = abilities.iterator();//you can't remove unneeded abilities inside a for(am:abilities) loop :(
    while(it.hasNext())
    {
    	Ability_Mana ma = it.next();
    	ma.setActivatingPlayer(AllZone.HumanPlayer);
    	if (!ma.canPlay()) it.remove();
    	else if (!canMake(ma, cneeded.toString())) it.remove();
    	
    	if (!skipExpress){
    		// skip express mana if the ability is not undoable
	    	if (!ma.isUndoable()){
	    		skipExpress = true;
	    		continue;
	    	}	
    	}
    }
    if(abilities.isEmpty())
    	return manaCost;
    
    // TODO when implementing sunburst 
    // If the card has sunburst or any other ability that tracks mana spent, skip express Mana choice
    // if (card.getTrackManaPaid()) skipExpress = true;

	if (!skipExpress){
		// express Mana Choice
		ArrayList<Ability_Mana> colorMatches = new ArrayList<Ability_Mana>();

		for(Ability_Mana am : abilities){
			String[] m = ManaPool.formatMana(am);
			for(String color : m)
				if(manaCost.isColor(color)) // convert to long before checking if color
					colorMatches.add(am);
		}
		
		if (colorMatches.size() == 0 || colorMatches.size() == abilities.size()) 
			// can only match colorless just grab the first and move on.
			choice = false;
		else if (colorMatches.size() < abilities.size()){
			// leave behind only color matches
			abilities = colorMatches;
		}
	}

    Ability_Mana chosen = abilities.get(0);
    if(1 < abilities.size() && choice)
    {
    	HashMap<String, Ability_Mana> ability = new HashMap<String, Ability_Mana>();
    	for(Ability_Mana am : abilities)
    		ability.put(am.toString(), am);
    	chosen = (Ability_Mana) GuiUtils.getChoice("Choose mana ability", abilities.toArray());
    }
    
	AllZone.GameAction.playSpellAbility(chosen);

 	manaCost = AllZone.ManaPool.subtractMana(sa, manaCost, chosen);

 	AllZone.Human_Battlefield.updateObservers();//DO NOT REMOVE THIS, otherwise the cards don't always tap (copied)
 	return manaCost;	

  }
  public static ArrayList<Ability_Mana> getManaAbilities(Card card)
  {
	  return card.getManaAbility();
  }
  //color is like "G", returns "Green"
  public static boolean canMake(Ability_Mana am, String mana)
  {
	  if(mana.contains("1")) return true;
	  if(mana.contains("S") && am.isSnow()) return true;

	  for(String color : ManaPool.formatMana(am))
  		if(mana.contains(color)) return true;
  	  return false;
  }
  

  public static String getLongColorString(String color)
  {
    Map<String, String> m = new HashMap<String, String>();
    m.put("G", Constant.Color.Green);
    m.put("R", Constant.Color.Red);
    m.put("U", Constant.Color.Blue);
    m.put("B", Constant.Color.Black);
    m.put("W", Constant.Color.White);
    m.put("S", Constant.Color.Snow);

    Object o = m.get(color);

    if(o == null)
      o = Constant.Color.Colorless;


    return o.toString();
  }
  
  public static String getShortColorString(String color)
  {
     Map<String, String> m = new HashMap<String, String>();
     m.put(Constant.Color.Green, "G");
     m.put(Constant.Color.Red, "R");
     m.put(Constant.Color.Blue, "U");
     m.put(Constant.Color.Black, "B");
     m.put(Constant.Color.White, "W");
     m.put(Constant.Color.Colorless, "1");
     m.put(Constant.Color.Snow, "S");
      
     Object o = m.get(color);
    
     return o.toString();
  }
  
}
