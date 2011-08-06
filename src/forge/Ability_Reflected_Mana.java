package forge;

import java.util.ArrayList;

public class Ability_Reflected_Mana extends Ability_Mana {

	private static final long serialVersionUID = 7050614528410938233L;

	String colorChosen;
	String colorOrType;
	String who;
	boolean choiceWasMade = false;

	public Ability_Reflected_Mana(final Card card, String abString, String colorOrType, String who) {
		super(card, abString);
		this.colorOrType = new String(colorOrType);
		this.who = new String(who);
		this.setReflectedMana(true);
		this.colorChosen = "0"; // Default -- add no mana
		this.undoable = true;
	}
	
	public Player getTargetPlayer() {
		Player targetPlayer;
		if (this.who.startsWith("Opp")) {
			targetPlayer = this.getSourceCard().getController().getOpponent();
		} else {
			targetPlayer = this.getSourceCard().getController();
		}
		return targetPlayer;
	}
	
	public void undo() {
		this.reset();
		super.undo();
	}

	public ArrayList<String> getPossibleColors() {
		Player targetPlayer;
		if (this.who.startsWith("Opp")) {
			targetPlayer = this.getSourceCard().getController().getOpponent();
		} else {
			targetPlayer = this.getSourceCard().getController();
		}

		ArrayList<String> possibleColors = getManaProduceList(targetPlayer, this.colorOrType);
		return possibleColors;
	}

	public boolean canPlay() {
		ArrayList<String> possibleColors = this.getPossibleColors();
		if (possibleColors.isEmpty()) {
			// Can't use these cards if there are no mana-producing lands in play
			return false;
		} else {
			return super.canPlay();
		}
	}
	
	public void resolve() {
		if (!this.choiceWasMade)
			this.chooseManaColor();
		if (this.choiceWasMade)
			super.resolve();
	}

	public String mana() {
		return this.colorChosen;
	}
	
	public void reset() {
		this.colorChosen = "0";
		this.choiceWasMade = false;
	}
	
	public boolean wasCancelled() {
		return !this.choiceWasMade; 
	}


	public void chooseManaColor() {
		ArrayList<String> possibleColors = this.getPossibleColors();
		if (possibleColors.isEmpty()) {
			// No mana available: card doesn't tap and nothing happens
			this.colorChosen = "0";
		} else if (possibleColors.size() == 1)  {
			// Card taps for the only mana available
			this.colorChosen = 
				Input_PayManaCostUtil.getShortColorString(possibleColors.get(0));
			this.choiceWasMade = true;
		}
		else {
			// Choose a color of mana to produce.
			Object o = AllZone.Display.getChoiceOptional("Select Mana to Produce", possibleColors.toArray());
			if (o == null) {
				// User hit cancel
				this.colorChosen = "0";
				this.choiceWasMade = false; 
			} else {
				this.colorChosen = 
					Input_PayManaCostUtil.getShortColorString((String) o);
				this.choiceWasMade = true;
			}
		}
	}

    // Return the list of mana types or colors that the target player's land can produce
    // This is used by the mana abilities created by the abReflectedMana keyword
    public static ArrayList<String> getManaProduceList(Player player, String colorOrType) {
		ArrayList<String> colorsPlayerCanProduce = new ArrayList<String>();
		ArrayList<String> colorsToLookFor = new ArrayList<String>();
		
		if (colorOrType.startsWith("Type")) {
			// Includes colorless (like Reflecting Pool)
			for (int ic = 0; ic < Constant.Color.Colors.length; ic++) {
				colorsToLookFor.add(Constant.Color.Colors[ic]);
			}
		} else {
			// Excludes colorless (like Exotic Orchard)
			for (int ic = 0; ic < Constant.Color.onlyColors.length; ic++) {
				colorsToLookFor.add(Constant.Color.onlyColors[ic]);
			}
		}

		// Build the list of cards to search for mana colors
		// First, add all the cards owned by the target player and sort out non-lands
    	CardList cl = new CardList();
    	cl.addAll(AllZone.getZone(Constant.Zone.Battlefield,player).getCards());
    	cl = cl.getType("Land");
    	
		// Narrow down the card list to only non-reflected lands
		// If during this search we find another reflected land, and it targets a different player
		// than this land, then we have to search that player's lands as well
    	boolean addOtherPlayerLands = false;
		int ix = 0;
		while (ix < cl.size()) {
    		Card otherCard = cl.get(ix);
			if (otherCard.isReflectedLand() && !addOtherPlayerLands) {    				
				ArrayList<Ability_Mana> amList = otherCard.getManaAbility();
				// We assume reflected lands have only one mana ability
				// Find out which player it targets
				Ability_Mana am = amList.get(0);
				Player otherTargetPlayer = am.getTargetPlayer();
				
				// If the target player of the other land isn't the same as the target player
				// of this land, we need to search the sets of mana he can produce as well.
				if (!otherTargetPlayer.isPlayer(player)) {
					addOtherPlayerLands = true; // We only need to record this decision once
				}
				// Don't keep reflected lands in the list of lands
				cl.remove(ix);
			} else {
				// Other card is a land but not a reflected land
				ix++; // leave in list & look at next card
			}
    	} // while ix < cl.size

		getManaFromCardList(cl, colorsPlayerCanProduce, colorsToLookFor);
		if (addOtherPlayerLands) {
			cl.clear();
			cl.addAll(AllZone.getZone(Constant.Zone.Battlefield,player.getOpponent()).getCards());
			cl = cl.filter(new CardListFilter() {
				public boolean addCard(Card c) {
					return c.isLand() && !c.isReflectedLand();
				}
			});
			
			// Exotic Orchard, which is the only way to get colors from another
			// player's lands, looks for colors. Therefore, we should not look
			// through another player's lands for colorless mana. This is true
			// even if the original card happens to have been a reflecting pool.
			if (colorsToLookFor.contains(Constant.Color.Colorless)) {
				colorsToLookFor.remove(Constant.Color.Colorless);
			}
			if (!colorsToLookFor.isEmpty()) {
				getManaFromCardList(cl, colorsPlayerCanProduce, colorsToLookFor);
			}
		}
		return colorsPlayerCanProduce;
    }
    
    private static void getManaFromCardList(CardList cl, ArrayList<String> colorsPlayerCanProduce, ArrayList<String>colorsToLookFor) {
    	int ix;
    	// In this routine, the list cl must be a list of lands that are not reflected lands
    	// Otherwise if both players had Exotic Orchards we might keep searching
    	// their lands forever.
    	for (ix = 0; ix < cl.size(); ix++) {
    		Card otherCard = cl.get(ix);
    		ArrayList<Ability_Mana> amList = otherCard.getManaAbility();
    		for (int im = 0; im < amList.size(); im++) {
    			// Search all the mana abilities and add colors of mana
    			Ability_Mana am = amList.get(im);
    			String newMana = otherCard.getReflectableMana(); 
    			if (newMana == "")
    				newMana = am.mana(); // This call would break for a reflected mana ability

    			int ic = 0;
    			// Check if any of the remaining colors are in this mana ability
    			while (ic < colorsToLookFor.size()) {
    				if (newMana.contains(Input_PayManaCostUtil.getShortColorString(colorsToLookFor.get(ic)))) {
    					colorsPlayerCanProduce.add(colorsToLookFor.remove(ic));
    					continue; // Don't increment index -- list got smaller
    				}
    				ic++; // Only increment if nothing was found
    			}

    			// If the search list is empty stop
    			if (colorsToLookFor.isEmpty()) {
    				break; // No point in continuing
    			}
    		} // Loop over mana abilities

    		if (colorsToLookFor.isEmpty()) {
    			break;  
    		}
    	} // loop over list of lands
    		    	
    }
    
 

} // end of Ability_Reflected_Mana

