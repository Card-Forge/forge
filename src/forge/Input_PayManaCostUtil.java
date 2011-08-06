package forge;
import java.util.*;

public class Input_PayManaCostUtil
{
  //all mana abilities start with this and typical look like "tap: add G"
  //mana abilities are Strings and are retrieved by calling card.getKeyword()
  //taps any card that has mana ability, not just land
  public static ManaCost tapCard(Card card, ManaCost manaCost)
  {
	//make sure computer's lands aren't selected
	if(card.getController().equals(AllZone.ComputerPlayer))
		return manaCost;
	
    if(card instanceof ManaPool) 
    	return ((ManaPool)card).subtractMana(manaCost);

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
    	if (!ma.canPlay()) it.remove();
    	else if (!canMake(ma, cneeded.toString())) it.remove();
    	
    	if (!skipExpress){
    		// skip express mana if there's a a sacrifice or the ability is not undoable
	    	if (ma.isSacrifice() || !ma.undoable()){
	    		skipExpress = true;
	    		continue;
	    	}	
    	}
    }
    if(abilities.isEmpty())
    	return manaCost;
    
    // todo when implementing sunburst 
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
    	chosen = (Ability_Mana) AllZone.Display.getChoice("Choose mana ability", abilities.toArray());
    }
    {
 	   if (chosen.isReflectedMana()) {
 		   // Choose the mana color
 		   Ability_Reflected_Mana arm = (Ability_Reflected_Mana) chosen;
 		   arm.chooseManaColor();

 		   // Only resolve if the choice wasn't cancelled and the mana was actually needed
 		   if (arm.wasCancelled()) {
 			   return manaCost;
 		   } else {
 			   String color = chosen.mana();
 			   if (!manaCost.isNeeded(color)) {
 				   // Don't tap the card if the user chose something invalid
 				   arm.reset(); // Invalidate the choice
 				   return manaCost;
 			   }
 		   }
 		   // A valid choice was made -- resolve the ability and tap the card
 		   arm.resolve();
 		   arm.getSourceCard().tap();
 	   } else {
 		   AllZone.GameAction.playSpellAbility(chosen);
 	   }
     	manaCost = AllZone.ManaPool.subtractMana(manaCost, chosen);
     	
     	// Nirkana Revenant Code
        Card mp = AllZone.ManaPool;
        if(card.getType().contains("Swamp") && card.getController().equals(AllZone.HumanPlayer)) {
         	CardList Nirkana_Human = AllZoneUtil.getPlayerCardsInPlay(AllZone.HumanPlayer, "Nirkana Revenant");
         	for(int x = 0; x < Nirkana_Human.size(); x++) {
        		for(int i = 0; i < abilities.size(); i++) {
        			if(abilities.get(i).mana().contains("B")) {
		         		if(!card.isSnow()) {
		         			chosen = abilities.get(i);	
		         			manaCost = AllZone.ManaPool.subtractMana(manaCost, chosen);
		         		} 
		         		else {
		         			if(manaCost.toString().trim() != "") {
			         			if(manaCost.toString().contains("B"))  {
			         				manaCost.payMana("B");
			         				if(AllZone.ManaPool.isEmpty()) {
			         					AllZone.ManaPool.removeManaFromPaying(new ManaCost("B"), Nirkana_Human.get(x));
			         					mp.removeExtrinsicKeyword("ManaPool:B");
			         				}
			         				else  {
			         					AllZone.ManaPool.removeManaFromFloating(new ManaCost("B"),  Nirkana_Human.get(x));
			         				}
			         			}
			         			else {
			         				if(manaCost.toString().length() > 0) {
				         				manaCost.payMana("1");
				         				
				         				if(AllZone.ManaPool.isEmpty()) {
				         					AllZone.ManaPool.removeManaFromPaying(new ManaCost("B"),  Nirkana_Human.get(x));
				         					mp.removeExtrinsicKeyword("ManaPool:B");
				         				}
				         				else {
				         					AllZone.ManaPool.removeManaFromFloating(new ManaCost("B"),  Nirkana_Human.get(x));
				         				}
			         				}
			         			}
		         			}
		         		}
        			}      		
        		}
         	} 
         }

       	// High Tide Code
         if(Phase.HighTideCount > 0 && card.getType().contains("Island") && card.getController().equals(AllZone.HumanPlayer)) {
         	for(int x = 0; x < Phase.HighTideCount; x++) {
        		for(int i = 0; i < abilities.size(); i++) {
        			if(abilities.get(i).mana().contains("U") == true) {
        				chosen = abilities.get(i);	
	         		if(card.isSnow() == false) {
	         			manaCost = AllZone.ManaPool.subtractMana(manaCost, chosen);
	         		} else { 
	         			
	         			if(manaCost.toString().trim() != "") {
	         			if(manaCost.toString().contains("U"))  {
	         				manaCost.payMana("U");
	         				if(AllZone.ManaPool.isEmpty()) {
	         					AllZone.ManaPool.removeManaFromPaying(new ManaCost("U"), chosen.getSourceCard());
	         					mp.removeExtrinsicKeyword("ManaPool:U");
	         				}
	         				else  {
	         					AllZone.ManaPool.removeManaFromFloating(new ManaCost("U"), chosen.getSourceCard());
	         				}
	         			}
         			else {
         				if(manaCost.toString().length() > 0) {
         				manaCost.payMana("1");
         				
         				if(AllZone.ManaPool.isEmpty()) {
         					AllZone.ManaPool.removeManaFromPaying(new ManaCost("U"), chosen.getSourceCard());
         					mp.removeExtrinsicKeyword("ManaPool:U");
         				}
         				else {
         					AllZone.ManaPool.removeManaFromFloating(new ManaCost("U"), chosen.getSourceCard());
         					
         				}
         			}
         			}
         			
         			}
         		}       		
         	}      		
         	}
         } 
         }
         	// High Tide Code
     	AllZone.Human_Play.updateObservers();//DO NOT REMOVE THIS, otherwise the cards don't always tap (copied)
     	return manaCost;	
     }

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
	  if(am.isReflectedMana()) {
		  for( String color:((Ability_Reflected_Mana)am).getPossibleColors()) {
			  if (mana.contains(getShortColorString(color))) {
				  return true;
			  }
		  }
		  return false;
	  }
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
